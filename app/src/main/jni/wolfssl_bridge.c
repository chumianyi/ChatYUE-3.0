/*
 * ChatYUE —— wolfSSL JNI 桥
 *
 * 为 Android 1.5（API 3）这类老系统补上现代 TLS。平台 JSSE 只到 TLS 1.0/1.1，
 * 证书库也早已过期，握手现代站点必然失败；这里用 wolfSSL 自带 TLS 1.2/1.3 实现
 * 与内置 CA 包完成握手，再把明文交给上层的 HTTP/1.1 解析器。
 *
 * 编译：见同目录 build_native.sh（ndk-build，wolfSSL 源码放 jni/wolfssl）。
 */
#include <jni.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netdb.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <arpa/inet.h>
#include <sys/time.h>

#include <wolfssl/options.h>
#include <wolfssl/ssl.h>
#include <wolfssl/error-ssl.h>

#define TAG "WolfSslBridge"

typedef struct {
    WOLFSSL_CTX *ctx;
    WOLFSSL *ssl;
    int fd;
} CYConn;

static char g_lastError[512];
static char g_caPath[1024];

static void set_err(const char *msg) {
    if (msg == NULL) msg = "unknown";
    snprintf(g_lastError, sizeof(g_lastError), "%s", msg);
}

JNIEXPORT jstring JNICALL
Java_com_chatyue_app_net_WolfSsl_version(JNIEnv *env, jclass clazz) {
    return (*env)->NewStringUTF(env, wolfSSL_lib_version());
}

JNIEXPORT void JNICALL
Java_com_chatyue_app_net_WolfSsl_nativeSetCaBundle(JNIEnv *env, jclass clazz, jstring path) {
    const char *p = (*env)->GetStringUTFChars(env, path, NULL);
    if (p != NULL) {
        snprintf(g_caPath, sizeof(g_caPath), "%s", p);
        (*env)->ReleaseStringUTFChars(env, path, p);
    }
}

JNIEXPORT jstring JNICALL
Java_com_chatyue_app_net_WolfSsl_lastError(JNIEnv *env, jclass clazz) {
    return (*env)->NewStringUTF(env, g_lastError);
}

static int tcp_connect(const char *host, int port, int timeoutMs) {
    struct addrinfo hints;
    struct addrinfo *res = NULL;
    char portStr[16];
    memset(&hints, 0, sizeof(hints));
    hints.ai_family = AF_UNSPEC;
    hints.ai_socktype = SOCK_STREAM;
    snprintf(portStr, sizeof(portStr), "%d", port);

    int rc = getaddrinfo(host, portStr, &hints, &res);
    if (rc != 0 || res == NULL) {
        set_err("DNS 解析失败");
        return -1;
    }

    int fd = -1;
    struct addrinfo *it = res;
    for (; it != NULL; it = it->ai_next) {
        fd = socket(it->ai_family, it->ai_socktype, it->ai_protocol);
        if (fd < 0) continue;
        struct timeval tv;
        tv.tv_sec = timeoutMs / 1000;
        tv.tv_usec = (timeoutMs % 1000) * 1000;
        setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(tv));
        setsockopt(fd, SOL_SOCKET, SO_SNDTIMEO, &tv, sizeof(tv));
        int one = 1;
        setsockopt(fd, IPPROTO_TCP, TCP_NODELAY, &one, sizeof(one));
        if (connect(fd, it->ai_addr, it->ai_addrlen) == 0) break;
        close(fd);
        fd = -1;
    }
    freeaddrinfo(res);
    if (fd < 0) set_err("TCP 连接失败");
    return fd;
}

JNIEXPORT jlong JNICALL
Java_com_chatyue_app_net_WolfSsl_connect(JNIEnv *env, jclass clazz, jstring jhost,
                                         jint port, jint timeoutMs) {
    const char *host = (*env)->GetStringUTFChars(env, jhost, NULL);
    if (host == NULL) {
        set_err("host 为空");
        return 0;
    }

    wolfSSL_Init();

    WOLFSSL_METHOD *method = wolfTLSv1_2_client_method();
    WOLFSSL_CTX *ctx = wolfSSL_CTX_new(method);
    if (ctx == NULL) {
        set_err("wolfSSL_CTX_new 失败");
        (*env)->ReleaseStringUTFChars(env, jhost, host);
        return 0;
    }
    /* 允许 TLS 1.0/1.1 老站点 + 现代 TLS 1.2/1.3 站点 */
    wolfSSL_CTX_set_min_proto_version(ctx, TLSv1_VERSION);

    if (g_caPath[0] != '\0') {
        if (wolfSSL_CTX_load_verify_locations(ctx, g_caPath, NULL) != WOLFSSL_SUCCESS) {
            set_err("CA 包加载失败");
        }
    }
    wolfSSL_CTX_set_verify(ctx, SSL_VERIFY_PEER, NULL);

    int fd = tcp_connect(host, port, timeoutMs);
    if (fd < 0) {
        wolfSSL_CTX_free(ctx);
        (*env)->ReleaseStringUTFChars(env, jhost, host);
        return 0;
    }

    WOLFSSL *ssl = wolfSSL_new(ctx);
    if (ssl == NULL) {
        set_err("wolfSSL_new 失败");
        close(fd);
        wolfSSL_CTX_free(ctx);
        (*env)->ReleaseStringUTFChars(env, jhost, host);
        return 0;
    }
    wolfSSL_set_fd(ssl, fd);
    wolfSSL_UseSNI(ssl, WOLFSSL_SNI_HOST_NAME, host, (word16) strlen(host));
    wolfSSL_check_domain_name(ssl, host);

    int rc = wolfSSL_connect(ssl);
    if (rc != WOLFSSL_SUCCESS) {
        int err = wolfSSL_get_error(ssl, rc);
        char buf[256];
        snprintf(buf, sizeof(buf), "握手中断 (%d): %s", err, wolfSSL_ERR_reason_error_string(err));
        set_err(buf);
        wolfSSL_free(ssl);
        close(fd);
        wolfSSL_CTX_free(ctx);
        (*env)->ReleaseStringUTFChars(env, jhost, host);
        return 0;
    }

    CYConn *conn = (CYConn *) calloc(1, sizeof(CYConn));
    if (conn == NULL) {
        set_err("内存不足");
        wolfSSL_free(ssl);
        close(fd);
        wolfSSL_CTX_free(ctx);
        (*env)->ReleaseStringUTFChars(env, jhost, host);
        return 0;
    }
    conn->ctx = ctx;
    conn->ssl = ssl;
    conn->fd = fd;
    (*env)->ReleaseStringUTFChars(env, jhost, host);
    set_err("ok");
    return (jlong) (intptr_t) conn;
}

JNIEXPORT jint JNICALL
Java_com_chatyue_app_net_WolfSsl_write(JNIEnv *env, jclass clazz, jlong handle,
                                       jbyteArray data, jint off, jint len) {
    CYConn *conn = (CYConn *) (intptr_t) handle;
    if (conn == NULL || conn->ssl == NULL) return -1;
    jbyte *buf = (*env)->GetByteArrayElements(env, data, NULL);
    if (buf == NULL) return -1;
    int n = wolfSSL_write(conn->ssl, buf + off, len);
    (*env)->ReleaseByteArrayElements(env, data, buf, JNI_ABORT);
    if (n <= 0) {
        int err = wolfSSL_get_error(conn->ssl, n);
        set_err(wolfSSL_ERR_reason_error_string(err));
        return -1;
    }
    return n;
}

JNIEXPORT jint JNICALL
Java_com_chatyue_app_net_WolfSsl_read(JNIEnv *env, jclass clazz, jlong handle,
                                      jbyteArray buf, jint off, jint len) {
    CYConn *conn = (CYConn *) (intptr_t) handle;
    if (conn == NULL || conn->ssl == NULL) return -1;
    jbyte *tmp = (*env)->GetByteArrayElements(env, buf, NULL);
    if (tmp == NULL) return -1;
    int n = wolfSSL_read(conn->ssl, tmp + off, len);
    if (n < 0) {
        int err = wolfSSL_get_error(conn->ssl, n);
        if (err == WOLFSSL_ERROR_WANT_READ || err == WOLFSSL_ERROR_WANT_WRITE) {
            n = 0;
        } else {
            set_err(wolfSSL_ERR_reason_error_string(err));
            n = -1;
        }
    }
    (*env)->ReleaseByteArrayElements(env, buf, tmp, 0);
    return n;
}

JNIEXPORT void JNICALL
Java_com_chatyue_app_net_WolfSsl_close(JNIEnv *env, jclass clazz, jlong handle) {
    CYConn *conn = (CYConn *) (intptr_t) handle;
    if (conn == NULL) return;
    if (conn->ssl != NULL) {
        wolfSSL_shutdown(conn->ssl);
        wolfSSL_free(conn->ssl);
    }
    if (conn->fd >= 0) close(conn->fd);
    if (conn->ctx != NULL) wolfSSL_CTX_free(conn->ctx);
    free(conn);
}
