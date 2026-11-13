#!/bin/sh
# 一键生成 wolfssl_bridge.so（需要 Android NDK r10e 或更高）
#
# 用法：
#   export NDK=/path/to/android-ndk
#   sh build_native.sh
#
# 脚本会：
#   1. 下载 wolfSSL 源码到 jni/wolfssl
#   2. 使用 ndk-build 生成 armeabi / armeabi-v7a / x86 三套 .so
#   3. 拷贝到 src/main/jniLibs/<abi>/libwolfssl_bridge.so
set -e
cd "$(dirname "$0")"

if [ ! -d wolfssl ]; then
  echo ">> 下载 wolfSSL 5.7.4"
  curl -sSL -o wolfssl.tar.gz https://www.wolfssl.com/wolfssl-5.7.4-stable.zip || \
  curl -sSL -o wolfssl.zip https://github.com/wolfSSL/wolfssl/archive/refs/tags/v5.7.4-stable.zip
  mkdir -p wolfssl
  unzip -q -o wolfssl.zip -d tmp_wolf
  cp -R tmp_wolf/wolfssl-*/[!.]* wolfssl/ 2>/dev/null || true
  rm -rf tmp_wolf wolfssl.zip wolfssl.tar.gz
fi

if [ -z "$NDK" ]; then
  echo "请先设置 NDK 环境变量，例如 export NDK=\$HOME/Android/Sdk/ndk/21.4.7075529"
  exit 1
fi

"$NDK/ndk-build" NDK_PROJECT_PATH=. APP_BUILD_SCRIPT=Android.mk NDK_APPLICATION_MK=Application.mk

for abi in armeabi armeabi-v7a x86; do
  mkdir -p ../jniLibs/$abi
  cp -f libs/$abi/libwolfssl_bridge.so ../jniLibs/$abi/ 2>/dev/null || true
done

echo ">> 完成，.so 已拷贝到 src/main/jniLibs/"
