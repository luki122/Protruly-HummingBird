LOCAL_PATH:= $(call my-dir)

# Build the Phone app which includes the emergency dialer. See Contacts
# for the 'other' dialer.
include $(CLEAR_VARS)

phone_common_dir := ../HMBPhoneCommon

src_dirs := src $(phone_common_dir)/src sip/src ext/src common/src
res_dirs := res res_ext $(phone_common_dir)/res sip/res

LOCAL_JAVA_LIBRARIES := telephony-common voip-common ims-common

# Add for Plug-in, include the plug-in framework
LOCAL_JAVA_LIBRARIES += mediatek-framework

LOCAL_STATIC_JAVA_LIBRARIES := guava        

LOCAL_SRC_FILES := $(call all-java-files-under, $(src_dirs))
LOCAL_SRC_FILES += \
        src/com/android/phone/EventLogTags.logtags \
        src/com/android/phone/INetworkQueryService.aidl \
        src/com/android/phone/INetworkQueryServiceCallback.aidl
LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dirs))

LOCAL_AAPT_FLAGS := \
    --auto-add-overlay \
    --extra-packages com.android.phone.common \
    --extra-packages com.android.services.telephony.sip

LOCAL_PACKAGE_NAME := HMBTeleService

LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_FLAG_FILES := proguard.flags sip/proguard.flags

# 引用hb-framework的资源
LOCAL_AAPT_FLAGS += -I out/target/common/obj/APPS/hb-framework-res_intermediates/package-export.apk
# 引用hb-framework的类
LOCAL_JAVA_LIBRARIES += hb-framework

include $(BUILD_PACKAGE)

# Build the test package
#include $(call all-makefiles-under,$(LOCAL_PATH))
