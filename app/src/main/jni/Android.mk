LOCAL_PATH := $(call my-dir)

# wolfSSL 源码需放在 jni/wolfssl（见 build_native.sh 自动拉取）
include $(CLEAR_VARS)
LOCAL_MODULE := wolfssl
LOCAL_SRC_FILES := $(wildcard $(LOCAL_PATH)/wolfssl/src/*.c)
LOCAL_C_INCLUDES := $(LOCAL_PATH)/wolfssl
LOCAL_CFLAGS := -DWOLFSSL_USER_SETTINGS -Os -fPIC
include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := wolfssl_bridge
LOCAL_SRC_FILES := wolfssl_bridge.c
LOCAL_C_INCLUDES := $(LOCAL_PATH)/wolfssl
LOCAL_STATIC_LIBRARIES := wolfssl
LOCAL_LDLIBS := -llog
include $(BUILD_SHARED_LIBRARY)
