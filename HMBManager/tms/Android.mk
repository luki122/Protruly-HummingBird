LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
#LOCAL_PREBUILT_JAVA_LIBRARIES := tms.jar
LOCAL_PREBUILT_LIBS := libams-1.2.5-mfr:libams-1.2.5-mfr.so
LOCAL_PREBUILT_LIBS += libdce-1.1.15-mfr:libdce-1.1.15-mfr.so
LOCAL_PREBUILT_LIBS += libTmsdk-2.0.10-mfr:libTmsdk-2.0.10-mfr.so
LOCAL_PREBUILT_LIBS += libbuffalo-1.0.0-mfr:libbuffalo-1.0.0-mfr.so
LOCAL_PREBUILT_LIBS += libbumblebee-1.0.4-mfr:libbumblebee-1.0.4-mfr.so
include $(BUILD_MULTI_PREBUILT)

include $(CLEAR_VARS)
LOCAL_IS_HOST_MODULE =
LOCAL_MODULE = tms
LOCAL_MODULE_CLASS = JAVA_LIBRARIES
LOCAL_MODULE_PATH =
LOCAL_MODULE_RELATIVE_PATH =
LOCAL_MODULE_SUFFIX = .jar
LOCAL_SRC_FILES = tms.jar
include $(BUILD_PREBUILT)
