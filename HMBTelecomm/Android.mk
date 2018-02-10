LOCAL_PATH:= $(call my-dir)

# Build the Telecom service.
include $(CLEAR_VARS)

LOCAL_JAVA_LIBRARIES := telephony-common
# Add for MMI, include the account widget framework
LOCAL_JAVA_LIBRARIES += mediatek-framework
LOCAL_STATIC_JAVA_LIBRARIES := \
        guava 

phone_common_dir := ../HMBPhoneCommon

res_dirs := res \
            res_ext \
            $(phone_common_dir)/res

LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dirs))

LOCAL_AAPT_FLAGS := \
    --auto-add-overlay \
    --extra-packages com.android.phone.common

src_dirs := src ext/src

LOCAL_SRC_FILES := $(call all-java-files-under, $(src_dirs))
LOCAL_SRC_FILES += \
        src/com/mediatek/telecom/recording/IPhoneRecorder.aidl\
        src/com/mediatek/telecom/recording/IPhoneRecordStateListener.aidl

LOCAL_SRC_FILES += src/com/hmb/manager/aidl/ICspService.aidl

# 引用hb-framework的资源
LOCAL_AAPT_FLAGS += -I out/target/common/obj/APPS/hb-framework-res_intermediates/package-export.apk
 
# 引用hb-framework的类
LOCAL_JAVA_LIBRARIES += hb-framework
                        
LOCAL_PACKAGE_NAME := HMBTelecom

LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

include $(BUILD_PACKAGE)

# Build the test package.
#include $(call all-makefiles-under,$(LOCAL_PATH))

# Build Plug in jar
include $(LOCAL_PATH)/ext/Android.mk
