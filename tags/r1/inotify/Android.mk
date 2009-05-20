ifneq ($(TARGET_SIMULATOR),true)

LOCAL_PATH:= $(call my-dir)


include $(CLEAR_VARS)
LOCAL_SRC_FILES:= inotify.c
LOCAL_MODULE := inotify
#LOCAL_STATIC_LIBRARIES := libcutils libc
LOCAL_CFLAGS += -Werror -Wall
include $(BUILD_EXECUTABLE)

endif  # TARGET_SIMULATOR != true
