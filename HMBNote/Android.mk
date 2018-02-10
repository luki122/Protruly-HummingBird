LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_STATIC_JAVA_LIBRARIES := glide android-support-v4

LOCAL_PACKAGE_NAME := HMBNote
LOCAL_CERTIFICATE := platform

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

# 引用hb-framework的类
LOCAL_JAVA_LIBRARIES += hb-framework
# 引用hb-framework的资源
LOCAL_AAPT_FLAGS += -I out/target/common/obj/APPS/hb-framework-res_intermediates/package-export.apk

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := glide:libs/glide-full-4.0.0.jar
include $(BUILD_MULTI_PREBUILT)
