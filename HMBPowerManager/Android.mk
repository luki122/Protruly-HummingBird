LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)


LOCAL_PROGUARD_ENABLED := disabled

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_AAPT_FLAGS += -I out/target/common/obj/APPS/hb-framework-res_intermediates/package-export.apk

LOCAL_JAVA_LIBRARIES += hb-framework

LOCAL_STATIC_JAVA_LIBRARIES := libfastjson

LOCAL_PACKAGE_NAME := HMBPowerManager

LOCAL_CERTIFICATE := platform

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := libfastjson:libs/fastjson-1.1.8.jar
include $(BUILD_MULTI_PREBUILT)
