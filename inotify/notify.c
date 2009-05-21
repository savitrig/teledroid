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
int	event_mask = 0;
char event_buf[1024];
char file_name[FILENAME_MAX];
struct inotify_event *event;

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
	LOGD("native registering %s at pos: %d", filename, wd);
	(*env)->ReleaseStringUTFChars(env, file, filename);
	return wd;
}

JNIEXPORT jint JNICALL Java_net_solarvistas_android_Notify_unregisterFile
  (JNIEnv *env, jclass clazz, jint nfd, jint wd) {
	LOGD("native unregistering pos: %d", wd);
	return inotify_rm_watch(nfd, wd);
}

JNIEXPORT jint JNICALL Java_net_solarvistas_android_Notify_nextEvent
  (JNIEnv *env, jclass clazz){
	if(res < (int)sizeof(*event)){
		LOGE("ERROR: event buffer overflow");
		return -1;
	}

	int event_size;
	event = (struct inotify_event *)(event_buf + event_pos);		
	LOGV("Returning Event %d at pos: %d length: %d|\t(res=%d, pos=%d)", event->mask, event->wd, event->len, res, event_pos);
	if (event->len)
		strncpy (file_name, event->name, FILENAME_MAX - 1);
	event_mask = event->mask;
	event_size = sizeof(*event) + event->len;
	res -= event_size;
	event_pos += event_size;
	return event->wd;
}

JNIEXPORT jint JNICALL Java_net_solarvistas_android_Notify_eventMask
  (JNIEnv *env, jclass clazz){
	int mask = event_mask;
	event_mask = 0;
	return mask;
}

JNIEXPORT jstring JNICALL Java_net_solarvistas_android_Notify_newFile
  (JNIEnv *env, jclass clazz){
	return (*env)->NewStringUTF(env, file_name);
}

JNIEXPORT jboolean JNICALL Java_net_solarvistas_android_Notify_hasNext
  (JNIEnv *env, jclass clazz, jint nfd){
	if(res >= (int)sizeof(*event))
		return JNI_TRUE;
	event_pos = 0;
	res = read(nfd, event_buf, sizeof(event_buf));
	if(res < (int)sizeof(*event)) {
		if(errno != EINTR)
			LOGE("ERROR: could not get event");
		return JNI_FALSE;
    }
	return JNI_TRUE;
}

static JNINativeMethod sMethods[] = {
     /* name, signature, funcPtr */
	{"initNotify", "()I", (void*)Java_net_solarvistas_android_Notify_initNotify}, 
	{"registerFile", "(ILjava/lang/String;I)I", (void*)Java_net_solarvistas_android_Notify_registerFile}, 
	{"unregisterFile", "(II)I", (void*)Java_net_solarvistas_android_Notify_unregisterFile}, 
	{"hasNext", "(I)Z", (void*)Java_net_solarvistas_android_Notify_hasNext}, 
	{"nextEvent","()I", (void*)Java_net_solarvistas_android_Notify_nextEvent}, 
	{"eventMask","()I", (void*)Java_net_solarvistas_android_Notify_eventMask}, 
	{"newFile","()Ljava/lang/String;", (void*)Java_net_solarvistas_android_Notify_newFile}, 
};

JNIEXPORT void JNICALL Java_net_solarvistas_android_Notify_registerNativeMethod
  (JNIEnv *env, jclass clazz){
	jniRegisterNativeMethods(env, "net/solarvistas/android/Notify", sMethods, 1);    	
}

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv* env = NULL;
    
	/*jint result = -1;
	
    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        return result;
    }
    jniRegisterNativeMethods(env, "net/solarvistas/android/Notify", sMethods, 1);    
	*/
	
    return JNI_VERSION_1_4;
}

int jniRegisterNativeMethods(JNIEnv* env, const char* className,
    const JNINativeMethod* gMethods, int numMethods)
{
    jclass clazz;

    LOGV("Registering %s natives\n", className);
    clazz = (*env)->FindClass(env, className);
    if (clazz == NULL) {
        LOGE("Native registration unable to find class '%s'\n", className);
        return -1;
    }
    if ((*env)->RegisterNatives(env, clazz, gMethods, numMethods) < 0) {
        LOGE("RegisterNatives failed for '%s'\n", className);
        return -1;
    }
    return 0;
}
