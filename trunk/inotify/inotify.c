#define INFO_LENGTH (50+FILENAME_MAX)
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


static jint
add(JNIEnv *env, jobject thiz, jint a, jint b) {
int result = a + b;
    LOGI("%d + %d = %d", a, b, result);
    return result;
}

static const char *classPathName = "net/solarvistas/android/Notify";

static JNINativeMethod methods[] = {
  {"add", "(II)I", (void*)add },
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



int main(int argc, char *argv[])
{
    int c;
    int nfd, ffd;
    int res;
	char event_buf[512];
    struct inotify_event *event;
	int event_mask = IN_ALL_EVENTS;
    int event_count = 0;
	int print_files = 0;
	int verbose = 0;
	int width = 80;
	char **file_names;
	int file_count;
	int id_offset = 0;
	int i;
	char *buf;

    do {
        c = getopt(argc, argv, "m:c:pv:w:");
        if (c == EOF)
            break;
        switch (c) {
        case 'm':
            event_mask = strtol(optarg, NULL, 0);
            break;
		case 'c':
            event_count = atoi(optarg);
            break;
		case 'p':
			print_files = 1;
			break;
        case 'v':
            verbose = atoi(optarg);
            break;
        case 'w':
            width = atoi(optarg);
            break;
        case '?':
            printf("%s: invalid option -%c\n",
                argv[0], optopt);
            exit(1);
        }
    } while (1);
	
    if (argc <= optind) {
        printf("Usage: %s [-m eventmask] [-c count] [-p] [-v verbosity] path [path ...]\n", argv[0]);
		return 1;
    }

    nfd = inotify_init();
    if(nfd < 0) {
        printf("inotify_init failed, %s\n", strerror(errno));
        return 1;
    }
	file_names = argv + optind;
	file_count = argc - optind;
	for(i = 0; i < file_count; i++) {
		res = inotify_add_watch(nfd, file_names[i], event_mask);
		if(res < 0) {
	        printf("inotify_add_watch failed for %s, %s\n", file_names[i], strerror(errno));
			return 1;
		}
		if(i == 0)
			id_offset = -res;
		if(res + id_offset != i) {
			printf("%s got unexpected id %d instead of %d\n", file_names[i], res, i);
			return 1;
		}
	}

	buf = malloc(width + 2);
	

    while(1) {
		fflush(stdout);
		
		int event_pos = 0;
        res = read(nfd, event_buf, sizeof(event_buf));
        if(res < (int)sizeof(*event)) {
			if(errno == EINTR)
				continue;
            printf("could not get event, %s\n", strerror(errno));
            return 1;
        }
		//printf("got %d bytes of event information\n", res);
		while(res >= (int)sizeof(*event)) {	
			int event_size;
			event = (struct inotify_event *)(event_buf + event_pos);
			
			char info[INFO_LENGTH];

			if (event->len)
				strncpy (info, event->name, INFO_LENGTH -1);
		    else
				strncpy(info, file_names[event->wd + id_offset], FILENAME_MAX -1);
		
			if (event->mask & IN_ACCESS)
				strncat(info, ", ACCESS", INFO_LENGTH -1);
		    if (event->mask & IN_ATTRIB)
				strncat(info, ", ATTRIB", INFO_LENGTH -1);
		    if (event->mask & IN_CLOSE_WRITE)
				strncat(info, ", CLOSE_WRITE", INFO_LENGTH -1);
		    if (event->mask & IN_CLOSE_NOWRITE)
				strncat(info, ", CLOSE_NOWRITE", INFO_LENGTH -1);
		    if (event->mask & IN_CREATE)
				strncat(info, ", CREATE", INFO_LENGTH -1);
		    if (event->mask & IN_DELETE)
				strncat(info, ", DETELE", INFO_LENGTH -1);
		    if (event->mask & IN_DELETE_SELF)
				strncat(info, ", DELETE_SELF", INFO_LENGTH -1);
		    if (event->mask & IN_MODIFY)
				strncat(info, ", MODIFY", INFO_LENGTH -1);
		    if (event->mask & IN_MOVE_SELF)
				strncat(info, ", MOVE_SELF", INFO_LENGTH -1);
		    if (event->mask & IN_MOVED_FROM)
				strncat(info, ", MOVED_FROM", INFO_LENGTH -1);
		    if (event->mask & IN_MOVED_TO)
				strncat(info, ", MOVED_TO", INFO_LENGTH -1);
		    if (event->mask & IN_OPEN)
				strncat(info, ", OPEN", INFO_LENGTH -1);

			printf("[i]%s\n", info);
			
			if(verbose >= 2)
		        printf("%s: %08x %08x \"%s\"\n", file_names[event->wd + id_offset], event->mask, event->cookie, event->len ? event->name : "");
			else if(verbose >= 2)
		        printf("%s: %08x \"%s\"\n", file_names[event->wd + id_offset], event->mask, event->len ? event->name : "");
			else if(verbose >= 1)
		        printf("%d: %08x \"%s\"\n", event->wd, event->mask, event->len ? event->name : "");
			if(print_files && (event->mask & IN_MODIFY)) {
				char filename[512];
				ssize_t read_len;
				char *display_name;
				int buflen;
				strcpy(filename, file_names[event->wd + id_offset]);
				if(event->len) {
					strcat(filename, "/");
					strcat(filename, event->name);
				}
				ffd = open(filename, O_RDONLY);
				display_name = (verbose >= 2 || event->len == 0) ? filename : event->name;
				buflen = width - strlen(display_name);
				read_len = read(ffd, buf, buflen);
				if(read_len > 0) {
					if(read_len < buflen && buf[read_len-1] != '\n') {
						buf[read_len] = '\n';
						read_len++;
					}
					if(read_len == buflen) {
						buf[--read_len] = '\0';
						buf[--read_len] = '\n';
						buf[--read_len] = '.';
						buf[--read_len] = '.';
						buf[--read_len] = '.';
					}
					else {
						buf[read_len] = '\0';
					}
					printf("%s: %s", display_name, buf);
				}
				close(ffd);
			}
	        if(event_count && --event_count == 0)
	            return 0;
			event_size = sizeof(*event) + event->len;
			res -= event_size;
			event_pos += event_size;
		}
    }

    return 0;
}
