# jniLibs

`libwolfssl_bridge.so` 由 GitHub Actions 云端交叉编译，静态链接 wolfSSL 5.7.4：

| ABI | 来源 NDK | 说明 |
| --- | --- | --- |
| armeabi | r10e / android-3 | ARMv5，Android 1.5+ |
| armeabi-v7a | r10e / android-9 | 常见老 ARM 设备 |
| arm64-v8a | r10e / android-21 | 64 位 ARM |
| x86 | r10e / android-9 | 模拟器 / 老 x86 设备 |
| x86_64 | r10e / android-21 | 64 位 x86 |
| mips | r10e / android-9 | 极冷门 MIPS |
| mips64 | r10e / android-21 | 极冷门 MIPS64 |
| riscv64 | r27d / android-35 | 极冷门 RISC-V |

运行时依赖只有 libc / libm / liblog / libdl；不要额外放 `libwolfssl.so`。
重新生成见仓库根目录 `.github/workflows/build-wolfssl.yml` 与
`scripts/build_wolfssl_ci.sh`。
