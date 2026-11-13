#!/bin/bash
# 在 GitHub Actions 的 Linux runner 上交叉编译 wolfSSL + JNI 桥。
#
# 用法：
#   NDK=/path/to/android-ndk-r10e bash scripts/build_wolfssl_ci.sh legacy
#   NDK=/path/to/android-ndk-r27d bash scripts/build_wolfssl_ci.sh riscv
#
# 产物：out/<abi>/libwolfssl_bridge.so
#
# 静态链接 wolfSSL，运行时只需要一个 .so，不依赖 libwolfssl.so / libstdc++。
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/.." && pwd)
MODE=${1:-legacy}
OUT="$ROOT/out"
WOLFSSL_VERSION="5.7.4-stable"
WOLFSSL_TGZ="/tmp/wolfssl-${WOLFSSL_VERSION}.zip"
WOLFSSL_SRC="/tmp/wolfssl-${WOLFSSL_VERSION}"
JOBS=${JOBS:-$(nproc 2>/dev/null || echo 2)}

if [ -z "${NDK:-}" ] || [ ! -d "$NDK" ]; then
    echo "NDK 未设置或不存在" >&2
    exit 2
fi

# ---------------------------------------------------------------- 拉取源码

if [ ! -x "$WOLFSSL_SRC/configure" ]; then
    echo ">> 下载 wolfSSL ${WOLFSSL_VERSION}"
    rm -rf "$WOLFSSL_SRC" /tmp/wolfssl-unzip
    curl -fL --retry 3 --connect-timeout 20 \
        -o "$WOLFSSL_TGZ" \
        "https://github.com/wolfSSL/wolfssl/archive/refs/tags/v${WOLFSSL_VERSION}.zip"
    mkdir -p /tmp/wolfssl-unzip
    unzip -q "$WOLFSSL_TGZ" -d /tmp/wolfssl-unzip
    SRC=$(find /tmp/wolfssl-unzip -maxdepth 1 -mindepth 1 -type d -name 'wolfssl-*' | head -1)
    mv "$SRC" "$WOLFSSL_SRC"
    cd "$WOLFSSL_SRC"
    echo ">> 生成 configure"
    ./autogen.sh >/tmp/wolfssl-autogen.log 2>&1 || {
        echo "autogen 失败，最后 80 行：" >&2
        tail -80 /tmp/wolfssl-autogen.log >&2
        exit 1
    }
    echo ">> wolfSSL configure 就绪"
fi

mkdir -p "$OUT"

# ---------------------------------------------------------------- 公共参数

CONFIG_FLAGS=(
    --disable-shared
    --enable-static
    --disable-examples
    --disable-crypttests
    --disable-benchmark
    --disable-asm
    --enable-sni
)

# ---------------------------------------------------------------- 旧版 NDK 7 ABI

build_legacy_one() {
    ABI=$1
    TRIPLE=$2
    ARCH=$3
    PLATFORM=$4
    TOOLCHAIN_NAME=$5
    EXTRA_CFLAGS=$6

    echo "================================================================"
    echo ">> 构建 $ABI ($TRIPLE, $PLATFORM)"

    TOOLCHAIN="$NDK/toolchains/$TOOLCHAIN_NAME/prebuilt/linux-x86_64"
    SYSROOT="$NDK/platforms/$PLATFORM/arch-$ARCH"
    CC_BIN="$TOOLCHAIN/bin/${TRIPLE}-gcc"
    AR_BIN="$TOOLCHAIN/bin/${TRIPLE}-ar"
    RANLIB_BIN="$TOOLCHAIN/bin/${TRIPLE}-ranlib"
    STRIP_BIN="$TOOLCHAIN/bin/${TRIPLE}-strip"

    if [ ! -x "$CC_BIN" ]; then
        echo "缺少编译器：$CC_BIN" >&2
        return 1
    fi
    if [ ! -d "$SYSROOT" ]; then
        echo "缺少 sysroot：$SYSROOT" >&2
        return 1
    fi

    WORK="/tmp/cybuild-$ABI"
    rm -rf "$WORK"
    mkdir -p "$WORK"
    cp -R "$WOLFSSL_SRC" "$WORK/src"
    mkdir -p "$WORK/prefix" "$OUT/$ABI"

    cat >"$WORK/cc" <<EOF
#!/bin/sh
exec "$CC_BIN" --sysroot="$SYSROOT" "\$@"
EOF
    chmod +x "$WORK/cc"

    cd "$WORK/src"
    echo ">> configure $ABI"
    CC="$WORK/cc" \
    AR="$AR_BIN" \
    RANLIB="$RANLIB_BIN" \
    CFLAGS="-fPIC -O2 $EXTRA_CFLAGS" \
    LDFLAGS="-Wl,-z,max-page-size=4096" \
    ./configure \
        --host="$TRIPLE" \
        --build=x86_64-pc-linux-gnu \
        --prefix="$WORK/prefix" \
        "${CONFIG_FLAGS[@]}" \
        >"$WORK/configure.log" 2>&1 || {
        echo "configure 失败，最后 100 行：" >&2
        tail -100 "$WORK/configure.log" >&2
        return 1
    }

    echo ">> make $ABI"
    make -j"$JOBS" >"$WORK/make.log" 2>&1 || {
        echo "make 失败，最后 140 行：" >&2
        tail -140 "$WORK/make.log" >&2
        return 1
    }
    make install >"$WORK/install.log" 2>&1 || {
        echo "make install 失败，最后 80 行：" >&2
        tail -80 "$WORK/install.log" >&2
        return 1
    }

    echo ">> 链接 libwolfssl_bridge.so ($ABI)"
    "$WORK/cc" -shared -fPIC -O2 \
        -Wl,-soname,libwolfssl_bridge.so \
        -o "$OUT/$ABI/libwolfssl_bridge.so" \
        "$ROOT/app/src/main/jni/wolfssl_bridge.c" \
        -I"$WORK/prefix/include" \
        "$WORK/prefix/lib/libwolfssl.a" \
        -lm -llog || {
        echo "JNI 桥链接失败 ($ABI)" >&2
        return 1
    }

    "$STRIP_BIN" --strip-unneeded "$OUT/$ABI/libwolfssl_bridge.so" 2>/dev/null || true
    echo ">> 完成 $ABI：$(du -h "$OUT/$ABI/libwolfssl_bridge.so" | cut -f1)"
}

build_legacy() {
    local failed=0
    # armeabi：ARMv5，API 3 起可用
    build_legacy_one armeabi arm-linux-androideabi arm android-3 arm-linux-androideabi-4.9 \
        "-march=armv5te -mfloat-abi=soft" || failed=1
    # armeabi-v7a：ARMv7，API 9 起；覆盖绝大多数老 ARM 设备
    build_legacy_one armeabi-v7a arm-linux-androideabi arm android-9 arm-linux-androideabi-4.9 \
        "-march=armv7-a -mfloat-abi=softfp -mfpu=vfpv3-d16" || failed=1
    # x86：API 9
    build_legacy_one x86 i686-linux-android x86 android-9 x86-4.9 "" || failed=1
    # mips：API 9
    build_legacy_one mips mipsel-linux-android mips android-9 mipsel-linux-android-4.9 "" || failed=1
    # 64 位：API 21
    build_legacy_one arm64-v8a aarch64-linux-android arm64 android-21 aarch64-linux-android-4.9 "" || failed=1
    build_legacy_one x86_64 x86_64-linux-android x86_64 android-21 x86_64-4.9 "" || failed=1
    build_legacy_one mips64 mips64el-linux-android mips64 android-21 mips64el-linux-android-4.9 "" || failed=1
    return $failed
}

# ---------------------------------------------------------------- RISC-V 64

build_riscv() {
    echo "================================================================"
    echo ">> 构建 riscv64（NDK clang）"
    TOOLCHAIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64"
    SYSROOT="$TOOLCHAIN/sysroot"
    API=35
    CC_BIN="$TOOLCHAIN/bin/clang"
    TARGET="riscv64-linux-android$API"
    AR_BIN="$TOOLCHAIN/bin/llvm-ar"
    RANLIB_BIN="$TOOLCHAIN/bin/llvm-ranlib"
    STRIP_BIN="$TOOLCHAIN/bin/llvm-strip"

    if [ ! -x "$CC_BIN" ]; then
        echo "缺少 clang：$CC_BIN" >&2
        return 1
    fi

    WORK="/tmp/cybuild-riscv64"
    rm -rf "$WORK"
    mkdir -p "$WORK"
    cp -R "$WOLFSSL_SRC" "$WORK/src"
    mkdir -p "$WORK/prefix" "$OUT/riscv64"

    cat >"$WORK/cc" <<EOF
#!/bin/sh
exec "$CC_BIN" --target="$TARGET" --sysroot="$SYSROOT" "\$@"
EOF
    chmod +x "$WORK/cc"

    cd "$WORK/src"
    echo ">> configure riscv64"
    CC="$WORK/cc" \
    AR="$AR_BIN" \
    RANLIB="$RANLIB_BIN" \
    CFLAGS="-fPIC -O2" \
    LDFLAGS="-Wl,-z,max-page-size=16384" \
    ./configure \
        --host=riscv64-linux-android \
        --build=x86_64-pc-linux-gnu \
        --prefix="$WORK/prefix" \
        "${CONFIG_FLAGS[@]}" \
        >"$WORK/configure.log" 2>&1 || {
        echo "configure 失败，最后 100 行：" >&2
        tail -100 "$WORK/configure.log" >&2
        return 1
    }

    echo ">> make riscv64"
    make -j"$JOBS" >"$WORK/make.log" 2>&1 || {
        echo "make 失败，最后 140 行：" >&2
        tail -140 "$WORK/make.log" >&2
        return 1
    }
    make install >"$WORK/install.log" 2>&1 || {
        echo "make install 失败，最后 80 行：" >&2
        tail -80 "$WORK/install.log" >&2
        return 1
    }

    "$WORK/cc" -shared -fPIC -O2 \
        -Wl,-soname,libwolfssl_bridge.so \
        -Wl,-z,max-page-size=16384 \
        -o "$OUT/riscv64/libwolfssl_bridge.so" \
        "$ROOT/app/src/main/jni/wolfssl_bridge.c" \
        -I"$WORK/prefix/include" \
        "$WORK/prefix/lib/libwolfssl.a" \
        -lm -llog || {
        echo "JNI 桥链接失败 (riscv64)" >&2
        return 1
    }

    "$STRIP_BIN" --strip-unneeded "$OUT/riscv64/libwolfssl_bridge.so" 2>/dev/null || true
    echo ">> 完成 riscv64：$(du -h "$OUT/riscv64/libwolfssl_bridge.so" | cut -f1)"
}

case "$MODE" in
    legacy)
        build_legacy
        ;;
    riscv)
        build_riscv
        ;;
    *)
        echo "未知模式：$MODE（legacy / riscv）" >&2
        exit 2
        ;;
esac

echo ">> 产物清单"
find "$OUT" -name 'libwolfssl_bridge.so' -print -exec ls -l {} \;
