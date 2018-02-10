# Copyright (C) 2013 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

hb-framework-sdk-jar-path := $(TARGET_OUT_COMMON_INTERMEDIATES)/JAVA_LIBRARIES/hb-framework-sdk_intermediates/classes.jar
hb-framework-jar-path := $(TARGET_OUT_COMMON_INTERMEDIATES)/JAVA_LIBRARIES/hb-framework_intermediates/classes.jar
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
$(shell cp  $(hb-framework-sdk-jar-path) $(hb-framework-jar-path))

LOCAL_SRC_FILES := $(call all-java-files-under, src) $(call all-subdir-Iaidl-files)

LOCAL_MODULE := HMBKeyguard

LOCAL_CERTIFICATE := platform

LOCAL_JAVA_LIBRARIES += telephony-common
LOCAL_JAVA_LIBRARIES += SettingsLib

#hb: tangjun add for hb-framework begin
LOCAL_JAVA_LIBRARIES += hb-framework
#hb: tangjun add for hb-framework end

LOCAL_STATIC_JAVA_LIBRARIES := com.mediatek.keyguard.ext.hb

LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res
LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/res_ext
LOCAL_AAPT_FLAGS := --auto-add-overlay

#hb: tangjun add for hb-framework begin
LOCAL_AAPT_FLAGS += -I out/target/common/obj/APPS/hb-framework-res_intermediates/package-export.apk
#hb: tangjun add for hb-framework end

include $(BUILD_STATIC_JAVA_LIBRARY)

#hb: tangjun mod begin
#include $(call all-makefiles-under,$(LOCAL_PATH))
include $(LOCAL_PATH)/ext/Android.mk
#hb: tangjun mod end
