LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_RESOURCE_DIR := ${LOCAL_PATH}/res

LOCAL_JAVA_LIBRARIES := framework 

LOCAL_CERTIFICATE := platform

ifneq ($(SYSTEM_UI_INCREMENTAL_BUILDS),)
    LOCAL_PROGUARD_ENABLED := disabled
    LOCAL_JACK_ENABLED := incremental
endif

LOCAL_MODULE_TAGS := optional
#LOCAL_SDK_VERSION := current

LOCAL_PACKAGE_NAME := HMBLockScreen
LOCAL_OVERRIDES_PACKAGES := HMBLockScreen

LOCAL_SRC_FILES := $(call all-java-files-under, src)


LOCAL_AAPT_FLAGS := --auto-add-overlay

include $(BUILD_PACKAGE)


# Use the following include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
