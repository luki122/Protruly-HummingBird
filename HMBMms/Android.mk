#
# Copyright (C) 2014 MediaTek Inc.
# Modification based on code covered by the mentioned copyright
# and/or permission notice(s).
#
# Copyright 2007-2008 The Android Open Source Project 

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
ext_dir := ext
callback_dir := callback
# MTK replace opt/chips with ex/chips
# Include build dir from frameworks/ex/chips
ex_chips_dir := ex_chips

src_dirs := src \
    $(ext_dir)/src \
    $(callback_dir)/src \
    common/src

res_dirs := res \
    $(ex_chips_dir)/res

#$(shell rm -rf $(LOCAL_PATH)/chips)

LOCAL_MODULE_TAGS := optional

#LOCAL_SRC_FILES := $(call all-java-files-under, $(src_dirs))

LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dirs))

LOCAL_PACKAGE_NAME := HMBMms
LOCAL_OVERRIDES_PACKAGES := Mms
LOCAL_OVERRIDES_PACKAGES := messaging

# Builds against the public SDK
#LOCAL_SDK_VERSION := current

LOCAL_JAVA_LIBRARIES += mediatek-framework
LOCAL_JAVA_LIBRARIES += telephony-common
LOCAL_JAVA_LIBRARIES += org.apache.http.legacy.boot
LOCAL_JAVA_LIBRARIES += voip-common
LOCAL_JAVA_LIBRARIES += ims-common
LOCAL_STATIC_JAVA_LIBRARIES += android-common jsr305
#frameworks/opt/chips
#LOCAL_STATIC_JAVA_LIBRARIES += libchips
#frameworks/ex/chips
LOCAL_STATIC_JAVA_LIBRARIES += android-common-chips
LOCAL_STATIC_JAVA_LIBRARIES += com.android.vcard
#Mms/ext
#LOCAL_STATIC_JAVA_LIBRARIES += com.mediatek.mms.ext
#Mms/callback
#LOCAL_STATIC_JAVA_LIBRARIES += com.mediatek.mms.callback

LOCAL_STATIC_JAVA_LIBRARIES += guava \
    android-support-v13 \
    android-support-v4 \
    libphonenumber


LOCAL_SRC_FILES := $(call all-java-files-under, $(src_dirs))
#tangyisen add tms begin
LOCAL_SRC_FILES += src/com/hmb/manager/aidl/ICspService.aidl
#tangyisen end
LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dirs))

#Automatically add resources that are only in overlays
LOCAL_AAPT_FLAGS := --auto-add-overlay
#To set an another name for a resource file
LOCAL_AAPT_FLAGS += --extra-packages com.android.phone.common
#LOCAL_AAPT_FLAGS += --extra-packages com.android.ex.chips
LOCAL_AAPT_FLAGS += --extra-packages com.android.mtkex.chips
LOCAL_AAPT_FLAGS += --extra-packages com.android.contacts.common

# lichao add an existing package to base include set
LOCAL_AAPT_FLAGS += -I out/target/common/obj/APPS/hb-framework-res_intermediates/package-export.apk
# lichao add hummingbird framework
LOCAL_JAVA_LIBRARIES += hb-framework

LOCAL_REQUIRED_MODULES := SoundRecorder
ifeq ($(strip $(MTK_RCS_SUPPORT)),yes)
LOCAL_PROGUARD_ENABLED := disabled
#LOCAL_PROGUARD_FLAG_FILES := proguard.flags
else
#LOCAL_PROGUARD_ENABLED := disabled
LOCAL_PROGUARD_FLAG_FILES := proguard.flags
endif

#system/priv-app
LOCAL_PRIVILEGED_MODULE := true

LOCAL_JACK_ENABLED := disabled

#Tell it to build an APK
include $(BUILD_PACKAGE)

# This finds and builds the test apk as well, so a single make does both.
#include $(call all-makefiles-under,$(LOCAL_PATH))
