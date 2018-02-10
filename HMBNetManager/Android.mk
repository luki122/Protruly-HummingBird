
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_JAVA_LIBRARIES := telephony-common voip-common org.apache.http.legacy
# 引用hb-framework的资源
LOCAL_AAPT_FLAGS += -I out/target/common/obj/APPS/hb-framework-res_intermediates/package-export.apk
# 引用hb-framework的类
LOCAL_JAVA_LIBRARIES += hb-framework
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4
LOCAL_SRC_FILES := $(call all-subdir-java-files)
LOCAL_PACKAGE_NAME := HMBNetManager
LOCAL_PROGUARD_ENABLED := disabled
#LOCAL_PROGUARD_FLAG_FILES := proguard-project.txt
#LOCAL_PRIVILEGED_MODULE := true
LOCAL_CERTIFICATE := platform
LOCAL_SRC_FILES := \
        $(call all-java-files-under, src)
#LOCAL_PROGUARD_FLAG_FILES := proguard.flags
#LOCAL_SRC_FILES += src/com/mst/tms/ITmsService.aidl \
 #                  src/com/mst/tms/ITrafficCorrectListener.aidl
#TARGET_CPU_API := arm64-v8a
#APP_ABI := arm64-v8a
#LOCAL_JNI_SHARED_LIBRARIES := libTmsdk-2.0.9-mfr
#LOCAL_PREBUILT_JNI_LIBS := lib/arm64/libTmsdk-2.0.9-mfr.so         
include $(BUILD_PACKAGE)
############################
include $(CLEAR_VARS)
#LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := tms:libs/tms.jar
#LOCAL_PREBUILT_LIBS := libTmsdk-2.0.9-mfr:libs/arm64-v8a/libTmsdk-2.0.9-mfr.so

include $(BUILD_MULTI_PREBUILT)
include $(call all-makefiles-under,$(LOCAL_PATH))
