LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)


LOCAL_PACKAGE_NAME := HMBDownloadProvider
LOCAL_CERTIFICATE := media
LOCAL_PRIVILEGED_MODULE := true
LOCAL_STATIC_JAVA_LIBRARIES := guava\
                  com.mediatek.downloadmanager.ext
LOCAL_JAVA_LIBRARIES += mediatek-framework
LOCAL_JAVA_LIBRARIES += org.apache.http.legacy

#HB. Comments : add extra res , Engerineer : wxue , Date : 2017年6月9日 ,begin
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res
LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/res_ui
LOCAL_AAPT_FLAGS := --auto-add-overlay 
LOCAL_AAPT_FLAGS += -I out/target/common/obj/APPS/hb-framework-res_intermediates/package-export.apk
LOCAL_JAVA_LIBRARIES += hb-framework ext telephony-common
#HB. end 

# M: add for emma
LOCAL_EMMA_COVERAGE_FILTER := @$(LOCAL_PATH)/downloadprovider-emma-filter.txt
include $(BUILD_PACKAGE)

# build UI + tests
include $(call all-makefiles-under,$(LOCAL_PATH))
