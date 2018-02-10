#
# Copyright (C) 2014 MediaTek Inc.
# Modification based on code covered by the mentioned copyright
# and/or permission notice(s).
#
# Copyright 2012, The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.



LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

#add by liyang begin:MTK和高通不同部分的代码分别放到两个独立的文件夹，在编译之前手动配置src_dirs
mtk_csp_dir:=hb_mtk
qualcomm_csp_dir:=hb_qualcomm
#add by liyang end

phone_common_dir := ../HMBPhoneCommon
src_dirs := src $(phone_common_dir)/src $(mtk_csp_dir)
res_dirs := res $(phone_common_dir)/res


include $(BUILD_MULTI_PREBUILT)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, $(src_dirs))
LOCAL_SRC_FILES += $(call all-java-files-under, ext/src)
LOCAL_SRC_FILES += src/com/hb/tms/ITmsService.aidl
LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dirs))
LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/res_ext

LOCAL_AAPT_FLAGS := \
    --auto-add-overlay \
    --extra-packages com.android.phone.common

LOCAL_STATIC_JAVA_LIBRARIES := \
    com.android.vcard \
    guava \
    android-common \
    android-support-v13 \
    android-support-v4 \
    libphonenumber \


 
# 引用hb-framework的类
LOCAL_JAVA_LIBRARIES += hb-framework
# 引用hb-framework的资源
LOCAL_AAPT_FLAGS += -I out/target/common/obj/APPS/hb-framework-res_intermediates/package-export.apk

LOCAL_JAVA_LIBRARIES += telephony-common
LOCAL_JAVA_LIBRARIES += voip-common
# LOCAL_JAVA_LIBRARIES += telephony-ext

LOCAL_PACKAGE_NAME := HMBContactsCommon

LOCAL_PROGUARD_ENABLED := disabled
LOCAL_PROGUARD_FLAG_FILES := proguard.flags

include $(BUILD_PACKAGE)

# Use the following include to make our test apk.
#include $(call all-makefiles-under,$(LOCAL_PATH))


