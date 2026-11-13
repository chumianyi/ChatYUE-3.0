#!/bin/sh
# ---------------------------------------------------------------------------
# ChatYUE 手工构建脚本（不依赖 Gradle / AGP）
#
# 为什么要这套脚本：AGP 7.x/8.x 已被 Google 硬编码为「compileSdk 至少 33」，
# 且 Gradle 9 无法运行 AGP 7；而本工程要保证 minSdkVersion=3。
# 直接调用 SDK 自带的 aapt2 / javac / d8 / zipalign / apksigner，
# 能 100% 控制 minSdk、dex 版本（035）与签名方案（v1 SHA1 + v2）。
#
# 用法：
#   ANDROID_SDK=/path/to/android-sdk sh scripts/build_apk.sh
#   ANDROID_SDK 下的要求：build-tools/*/aapt2|d8|zipalign|apksigner
#                        platforms/android-*/android.jar
# ---------------------------------------------------------------------------
set -e

SDK="${ANDROID_SDK:-${ANDROID_HOME:-/opt/android-sdk}}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/build"
MIN_SDK=3
TARGET_SDK=30
VERSION_CODE=5
VERSION_NAME=3.0
APK_NAME="ChatYUE-$VERSION_NAME.apk"

# 优先用 JDK 8：源码是 1.6 语法，用 8 编译可以避免误用新 API；
# 没有 JDK 8 时退回当前 javac（仍然锁定 -source/-target 1.6）。
JDK_HOME="${JDK_HOME:-${JAVA_HOME_8:-}}"
if [ -z "$JDK_HOME" ]; then
    if [ -x "/home/user/jdk-17.0.12+7/bin/javac" ]; then
        JDK_HOME="/home/user/jdk-17.0.12+7"
    elif [ -x "/opt/jdk8" ]; then
        JDK_HOME="/opt/jdk8"
    fi
fi
if [ -n "$JDK_HOME" ] && [ -x "$JDK_HOME/bin/javac" ]; then
    JAVAC="$JDK_HOME/bin/javac"
    echo "==> javac: $JAVAC (JDK $(basename "$JDK_HOME"))"
else
    JAVAC="javac"
    echo "==> javac: $(command -v javac)（使用 PATH 中的 JDK）"
fi

BT="$(ls -d "$SDK"/build-tools/* 2>/dev/null | sort -V | tail -1)"
PLATFORM="$(ls -d "$SDK"/platforms/android-* 2>/dev/null | sort -V | tail -1)"
AJ="$PLATFORM/android.jar"
[ -x "$BT/aapt2" ] || { echo "找不到 aapt2，请检查 ANDROID_SDK=$SDK"; exit 1; }
[ -f "$AJ" ] || { echo "找不到 android.jar，请检查 $PLATFORM"; exit 1; }

echo "==> SDK: $SDK"
echo "==> build-tools: $BT"
echo "==> platform: $PLATFORM"

rm -rf "$OUT"
mkdir -p "$OUT/res" "$OUT/gen" "$OUT/classes" "$OUT/dex"

# 1) 资源
"$BT/aapt2" compile --dir "$ROOT/app/src/main/res" -o "$OUT/res/res.zip"

# 2) 链接：minSdkVersion=3 在这里真正写进清单
"$BT/aapt2" link \
    -o "$OUT/base.apk" \
    -I "$AJ" \
    --manifest "$ROOT/app/src/main/AndroidManifest.xml" \
    -A "$ROOT/app/src/main/assets" \
    -R "$OUT/res/res.zip" \
    --java "$OUT/gen" \
    --min-sdk-version "$MIN_SDK" \
    --target-sdk-version "$TARGET_SDK" \
    --version-code "$VERSION_CODE" \
    --version-name "$VERSION_NAME" \
    --auto-add-overlay

# 3) 编译 Java（1.6 语法范围内的写法，用 8 的字节码便于 d8 处理）
find "$ROOT/app/src/main/java" "$OUT/gen" -name "*.java" > "$OUT/srcs.txt"
if ! "$JAVAC" -nowarn -encoding UTF-8 -source 1.7 -target 1.7 -cp "$AJ" \
        -d "$OUT/classes" @"$OUT/srcs.txt" > "$OUT/javac.log" 2>&1; then
    cat "$OUT/javac.log"
    echo "!! Java 编译失败"
    exit 1
fi
echo "==> Java 编译通过：$(wc -l < "$OUT/srcs.txt") 个源文件"

# 4) dex：优先用 dx（与 API 3 同期的工具链），产出 dex 035；
#    新版 SDK 移除 dx 时退回 d8 --min-api 3（同样是 035）。
find "$OUT/classes" -name "*.class" > "$OUT/classes.txt"
if [ -x "$BT/dx" ]; then
    echo "==> dex: dx（legacy）"
    "$BT/dx" --dex --output="$OUT/dex/classes.dex" "$OUT/classes"
else
    echo "==> dex: d8 --min-api $MIN_SDK"
    "$BT/d8" --release --min-api "$MIN_SDK" --lib "$AJ" --output "$OUT/dex" @"$OUT/classes.txt"
fi

# 5) 打包（含 jniLibs，若存在）
mkdir -p "$OUT/apkroot/lib"
cp "$OUT/dex/classes.dex" "$OUT/apkroot/classes.dex"
if [ -d "$ROOT/app/src/main/jniLibs" ]; then
    for d in "$ROOT"/app/src/main/jniLibs/*; do
        [ -d "$d" ] || continue
        mkdir -p "$OUT/apkroot/lib/$(basename "$d")"
        cp -R "$d/." "$OUT/apkroot/lib/$(basename "$d")/" 2>/dev/null || true
    done
fi
( cd "$OUT/apkroot" && zip -qr "$OUT/base.apk" . )

# 6) 对齐 + 签名（v1 用 SHA1 摘要，Android 1.5 只认这一套）
KS="$ROOT/keystore/debug.keystore"
mkdir -p "$ROOT/keystore"
KEYTOOL="$JDK_HOME/bin/keytool"
[ -x "$KEYTOOL" ] || KEYTOOL=keytool
if [ ! -f "$KS" ]; then
    "$KEYTOOL" -genkeypair -keystore "$KS" -storepass android -keypass android \
        -alias chatyue -keyalg RSA -keysize 2048 -validity 10950 \
        -dname "CN=ChatYUE, OU=Dev, O=ChatYUE, L=Hangzhou, S=Zhejiang, C=CN"
fi

"$BT/zipalign" -p -f 4 "$OUT/base.apk" "$OUT/aligned.apk"
V3_FLAG=""
if "$BT/apksigner" sign --help 2>&1 | grep -q "v3-signing-enabled"; then
    V3_FLAG="--v3-signing-enabled false"
fi
"$BT/apksigner" sign \
    --ks "$KS" --ks-pass pass:android --key-pass pass:android --ks-key-alias chatyue \
    --min-sdk-version "$MIN_SDK" \
    --v1-signing-enabled true --v2-signing-enabled true $V3_FLAG \
    --out "$ROOT/$APK_NAME" "$OUT/aligned.apk"

"$BT/apksigner" verify --min-sdk-version "$MIN_SDK" "$ROOT/$APK_NAME"
echo "==> 产出: $ROOT/$APK_NAME"
ls -lh "$ROOT/$APK_NAME"
