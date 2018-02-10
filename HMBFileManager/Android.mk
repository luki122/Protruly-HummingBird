LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

$(warning "LocalPath-->" $(LOCAL_PATH))

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

$(warning "LOCAL_RESOURCE_DIR -- " $(LOCAL_RESOURCE_DIR))

ifeq ($(TARGET_BUILD_APPS),)
LOCAL_RESOURCE_DIR += frameworks/support/v7/appcompat/res
LOCAL_RESOURCE_DIR += frameworks/support/v7/gridlayout/res
else
LOCAL_RESOURCE_DIR += prebuilts/sdk/current/support/v7/appcompat/res
LOCAL_RESOURCE_DIR += prebuilts/sdk/current/support/v7/gridlayout/res
endif

$(warning $(LOCAL_RESOURCE_DIR))

LOCAL_JAVA_LIBRARIES := framework \
        mediatek-framework

LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true
#LOCAL_JACK_ENABLED := disabled
#LOCAL_PROGUARD_ENABLED := disabled
LOCAL_PROGUARD_ENABLED := custom
LOCAL_PROGUARD_FLAG_FILES := app/proguard.cfg
LOCAL_PROGUARD_ENABLED := full

LOCAL_MODULE_TAGS := optional
# LOCAL_SDK_VERSION := current

LOCAL_PACKAGE_NAME := HMBFileManager
# LOCAL_OVERRIDES_PACKAGES := FileManager

LOCAL_SRC_FILES := $(call all-java-files-under, src)

# LOCAL_STATIC_JAVA_LIBRARIES := android-opt-datetimepicker
#LOCAL_STATIC_JAVA_LIBRARIES := android-support-v13
LOCAL_STATIC_JAVA_LIBRARIES += android-support-v4
LOCAL_STATIC_JAVA_LIBRARIES += android-support-v7-appcompat
LOCAL_STATIC_JAVA_LIBRARIES += android-support-v7-gridlayout
#LOCAL_STATIC_JAVA_LIBRARIES += com.mediatek.deskclock.ext
#LOCAL_STATIC_JAVA_LIBRARIES += com.mediatek.filemanager.ext
#LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += libs/com.ibm.icu_3.8.jar
LOCAL_STATIC_JAVA_LIBRARIES += pinyin4j

# 引用hb-framework的类
LOCAL_JAVA_LIBRARIES += hb-framework

# 引用hb-framework的资源
LOCAL_AAPT_FLAGS := --auto-add-overlay
LOCAL_AAPT_FLAGS += -I out/target/common/obj/APPS/hb-framework-res_intermediates/package-export.apk
LOCAL_AAPT_FLAGS += --extra-packages android.support.v7.appcompat
LOCAL_AAPT_FLAGS += --extra-packages android.support.v7.gridlayout
#LOCAL_AAPT_FLAGS += --extra-packages com.android.datetimepicker

include $(BUILD_PACKAGE)


# Use the following include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))

#shenqianfeng add begin
include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += pinyin4j:libs/pinyin4j-2.5.0.jar
include $(BUILD_MULTI_PREBUILT)
#include $(BUILD_PREBUILT)
#shenqianfeng add end
