# Copyright 2007-2008 The Android Open Source Project
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_SRC_FILES += src/com/hmb/manager/aidl/ICspService.aidl
LOCAL_PACKAGE_NAME := HMBReject
LOCAL_CERTIFICATE := platform

LOCAL_JAVA_LIBRARIES := telephony-common

LOCAL_STATIC_JAVA_LIBRARIES := \
    android-common \
    android-support-v4

# 引用hb-framework的资源
LOCAL_AAPT_FLAGS += -I out/target/common/obj/APPS/hb-framework-res_intermediates/package-export.apk
 
# 引用hb-framework的类
LOCAL_JAVA_LIBRARIES += hb-framework

include $(BUILD_PACKAGE)
include $(call all-makefiles-under,$(LOCAL_PATH))
