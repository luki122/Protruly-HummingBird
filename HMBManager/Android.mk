LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under,src)
LOCAL_SRC_FILES += src/com/hmb/manager/aidl/ICspService.aidl

#LOCAL_STATIC_JAVA_LIBRARIES := gson
LOCAL_STATIC_JAVA_LIBRARIES += android-support-v4
#LOCAL_STATIC_JAVA_LIBRARIES += libTms

LOCAL_PACKAGE_NAME := HMBManager
LOCAL_CERTIFICATE := platform

LOCAL_AAPT_FLAGS += -I out/target/common/obj/APPS/hb-framework-res_intermediates/package-export.apk
LOCAL_JAVA_LIBRARIES += hb-framework
LOCAL_STATIC_JAVA_LIBRARIES += tms

LOCAL_MULTILIB := 64
LOCAL_JNI_SHARED_LIBRARIES := libTmsdk-2.0.10-mfr
LOCAL_JNI_SHARED_LIBRARIES += libams-1.2.5-mfr
LOCAL_JNI_SHARED_LIBRARIES += libdce-1.1.15-mfr
LOCAL_JNI_SHARED_LIBRARIES += libbuffalo-1.0.0-mfr
LOCAL_JNI_SHARED_LIBRARIES += libbumblebee-1.0.4-mfr
LOCAL_PROGUARD_ENABLED := disabled

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)
#LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := gson:libs/gson-2.2.4.jar
#LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES :=libTms:libs/tms.jar
include $(BUILD_MULTI_PREBUILT)

include $(call all-makefiles-under,$(LOCAL_PATH))
