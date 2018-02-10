#
# Copyright (C) 2008 The Android Open Source Project
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

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_DEX_PREOPT := false
LOCAL_MODULE_TAGS := optional

LOCAL_CERTIFICATE := platform
#LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_SRC_FILES := $(call all-java-files-under,$(src_dirs)) \
LOCAL_SRC_FILES += $(call all-java-files-under, hb_imageloader/src)

LOCAL_AAPT_FLAGS += -I out/target/common/obj/APPS/hb-framework-res_intermediates/package-export.apk


LOCAL_JAVA_LIBRARIES += hb-framework 

LOCAL_STATIC_JAVA_LIBRARIES += \
	glide3.6 \
	android-support-v4 \
	sunjce \
	palette \
	fastjson-1.2.32 \
	okhttp-3.7 \
	okhio-1.12 \
	alipaysdk \
	wechatpay

LOCAL_PACKAGE_NAME := HMBThemeManager
LOCAL_PROGUARD_ENABLED := disabled
LOCAL_PROGUARD_FLAG_FILES := proguard.flags

include $(BUILD_PACKAGE)
##################################################
include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := \
          sunjce:libs/sunjce_provider.jar \
          palette:libs/palette.jar \
          glide3.6:libs/glide-3.6.1.jar \
          fastjson-1.2.32:libs/fastjson-1.2.32.jar \
          okhttp-3.7:libs/okhttp-3.7.0.jar \
          okhio-1.12:libs/okio-1.12.0.jar \
          alipaysdk:libs/alipaySdk-20170725.jar \
          wechatpay:libs/wechat-sdk-android-without-mta.jar

include $(BUILD_MULTI_PREBUILT)

# Use the following include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))

