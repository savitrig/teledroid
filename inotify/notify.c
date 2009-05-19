/** the tag of the android log entries */
#define LOG_TAG "inotify jni library"

#include <utils/Log.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/inotify.h>
#include <errno.h>
#include "jni.h"

int res = 0;
int event_pos = 0;
char event_buf[512];
struct inotify_event *event;
static const char *classPathName = "net/solarvistas/android/Notify";

JNIEXPORT jint JNICALL Java_net_solarvistas_android_Notify_initNotify
  (JNIEnv *env, jclass clazz){
	return inotify_init();
}

JNIEXPORT jint JNICALL Java_net_solarvistas_android_Notify_registerFile
  (JNIEnv *env, jclass clazz, jint nfd, jstring file, jint mask) {
	int wd;
	const jbyte *filename;
    filename = (*env)->GetStringUTFChars(env, file, NULL);
	if (filename == NULL) {
		return -1; /* OutOfMemoryError already thrown */
	}
	wd = inotify_add_watch(nfd, filename, mask);
	(*env)->ReleaseStringUTFChars(env, file, filename);
	return wd;
}

JNIEXPORT jlong JNICALL Java_net_solarvistas_android_Notify_nextEvent
  (JNIEnv *env, jclass clazz){
	if(res < (int)sizeof(*event)){
		LOGD("ERROR: event buffer overflow");
		return -1;
	}

	int event_size;
	long event_long;
	event = (struct inotify_event *)(event_buf + event_pos);		
	event_long = event->wd * 0x100000000 + event->mask;
	event_size = sizeof(*event) + event->len;
	res -= event_size;
	event_pos += event_size;
}

JNIEXPORT jboolean JNICALL Java_net_solarvistas_android_Notify_hasNext
  (JNIEnv *env, jclass clazz, jint nfd){
	if(res >= (int)sizeof(*event))
		return JNI_TRUE;
	event_pos = 0;
	res = read(nfd, event_buf, sizeof(event_buf));
	if(res < (int)sizeof(*event)) {
		if(errno != EINTR)
			LOGD("ERROR: could not get event");
		return JNI_FALSE;
    }
	return JNI_TRUE;
}

static JNINativeMethod sMethods[] = {
     /* name, signature, funcPtr */
	{"initNotify", "()I", (void*)Java_net_solarvistas_android_Notify_initNotify}, 
	{"registerFile", "(ILjava/lang/String;I)I", (void*)Java_net_solarvistas_android_Notify_registerFile}, 
	{"hasNext", "(I)Z", (void*)Java_net_solarvistas_android_Notify_hasNext}, 
	{"nextEvent","()J", (void*)Java_net_solarvistas_android_Notify_nextEvent}, 
};

/*
 * Register several native methods for one class.
 */
static int registerNativeMethods(JNIEnv* env, const char* className,
    JNINativeMethod* gMethods, int numMethods)
{
    jclass clazz;

    clazz = env->FindClass(className);
    if (clazz == NULL) {
        LOGE("Native registration unable to find class '%s'", className);
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        LOGE("RegisterNatives failed for '%s'", className);
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

/*
 * Register native methods for all classes we know about.
 *
 * returns JNI_TRUE on success.
 */
static int registerNatives(JNIEnv* env)
{
  if (!registerNativeMethods(env, classPathName,
                 methods, sizeof(methods) / sizeof(methods[0]))) {
    return JNI_FALSE;
  }

  return JNI_TRUE;
}


// ----------------------------------------------------------------------------

/*
 * This is called by the VM when the shared library is first loaded.
 */
 
typedef union {
    JNIEnv* env;
    void* venv;
} UnionJNIEnvToVoid;

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    UnionJNIEnvToVoid uenv;
    uenv.venv = NULL;
    jint result = -1;
    JNIEnv* env = NULL;
    
    LOGI("JNI_OnLoad");

    if (vm->GetEnv(&uenv.venv, JNI_VERSION_1_4) != JNI_OK) {
        LOGE("ERROR: GetEnv failed");
        goto bail;
    }
    env = uenv.env;

    if (registerNatives(env) != JNI_TRUE) {
        LOGE("ERROR: registerNatives failed");
        goto bail;
    }
    
    result = JNI_VERSION_1_4;
    
bail:
    return result;
}
