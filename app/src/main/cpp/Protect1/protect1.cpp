#include <jni.h>
#include <string>

//jstring nativeReturn(JNIEnv* env,jobject obj)
extern "C" JNIEXPORT
jstring nativeReturn(JNIEnv* env,jobject obj)
{
    return env->NewStringUTF("动态注册成功");
}

JNINativeMethod nativeMethod[] = {{"returnString","()Ljava/lang/String;",(void*)nativeReturn}};

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* jvm,void* reserved)
{
    JNIEnv *env;
    if(jvm->GetEnv((void**)&env,JNI_VERSION_1_4) != JNI_OK)
    {
        return -1;
    }
    jclass clz = env->FindClass("com/example/jz/testapk/MainActivity");
    env->RegisterNatives(clz,nativeMethod,sizeof(nativeMethod)/sizeof(nativeMethod[0]));
    return JNI_VERSION_1_4;
}

//split apk file from so file
extern "C" JNIEXPORT
jint sub_1(JNIEnv* env,jobject obj)
{
    sub1();
    return 0;
}

string sub1()
{
    string str = "Hello";
    return str;
}