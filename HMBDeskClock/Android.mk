LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_RESOURCE_DIR := ${LOCAL_PATH}/res
LOCAL_RESOURCE_DIR += frameworks/opt/datetimepicker/res

ifeq ($(TARGET_BUILD_APPS),)
LOCAL_RESOURCE_DIR += frameworks/support/v7/appcompat/res
LOCAL_RESOURCE_DIR += frameworks/support/v7/gridlayout/res
else
LOCAL_RESOURCE_DIR += prebuilts/sdk/current/support/v7/appcompat/res
LOCAL_RESOURCE_DIR += prebuilts/sdk/current/support/v7/gridlayout/res
endif

LOCAL_JAVA_LIBRARIES := framework \
        mediatek-framework \
        mediatek-common

LOCAL_CERTIFICATE := platform

LOCAL_PROGUARD_ENABLED := disabled

LOCAL_MODULE_TAGS := optional
#LOCAL_SDK_VERSION := current

LOCAL_PACKAGE_NAME := HMBDeskClock
LOCAL_OVERRIDES_PACKAGES := AlarmClock

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_STATIC_JAVA_LIBRARIES := android-opt-datetimepicker
LOCAL_STATIC_JAVA_LIBRARIES += android-support-v13
LOCAL_STATIC_JAVA_LIBRARIES += android-support-v7-appcompat
LOCAL_STATIC_JAVA_LIBRARIES += android-support-v7-gridlayout
LOCAL_STATIC_JAVA_LIBRARIES += com.mediatek.deskclock.ext


# 引用hb-framework的类
LOCAL_JAVA_LIBRARIES += hb-framework

LOCAL_AAPT_FLAGS := --auto-add-overlay
LOCAL_AAPT_FLAGS += --extra-packages android.support.v7.appcompat
LOCAL_AAPT_FLAGS += --extra-packages android.support.v7.gridlayout
LOCAL_AAPT_FLAGS += --extra-packages com.android.datetimepicker

LOCAL_JACK_ENABLED := disabled

# 引用hb-framework的资源
LOCAL_AAPT_FLAGS += -I out/target/common/obj/APPS/hb-framework-res_intermediates/package-export.apk

include $(BUILD_PACKAGE)


# Use the following include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))