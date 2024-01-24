#include <jni.h>

JNIEXPORT jint JNICALL
Java_bench_JniBench_add(JNIEnv* env, jclass unused, jint a, jint b) {
    return a + b;
}

JNIEXPORT jint JNICALL
raw_add(jint a, jint b) {
    return a + b;
}

JNIEXPORT jlong JNICALL
Java_bench_JniBench_max(JNIEnv* env, jclass unused, jlongArray array, jint length) {
    jboolean isCopy;
    jlong* data = (jlong*) (*env)->GetPrimitiveArrayCritical(env, array, &isCopy);

    jlong max = data[0];
    jint i;
    for (i = 1; i < length; i++) {
        if (data[i] > max) max = data[i];
    }
    
    (*env)->ReleasePrimitiveArrayCritical(env, array, data, JNI_ABORT);
    return max;
}

JNIEXPORT jlong JNICALL
raw_max(jlong* data, jint length) {
    jlong max = data[0];
    jint i;
    for (i = 1; i < length; i++) {
        if (data[i] > max) max = data[i];
    }
    return max;
}
