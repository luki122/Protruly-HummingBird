LOCAL_PATH		:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := libgenius_graphics
LOCAL_SRC_FILES := stackblur.c net_qiujuer_genius_grahpics_Blur.c load.c clipblur.c
LOCAL_LDLIBS    := -lm -llog -ljnigraphics

include $(BUILD_SHARED_LIBRARY)
