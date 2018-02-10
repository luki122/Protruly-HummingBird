LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src) \
    src/com/android/systemui/EventLogTags.logtags

LOCAL_STATIC_JAVA_LIBRARIES := HMBKeyguard
LOCAL_STATIC_JAVA_LIBRARIES += com.mediatek.systemui.ext.hb 
#hb: add by chenhl start
LOCAL_STATIC_JAVA_LIBRARIES += android-support-v4
#hb: add by chenhl end
LOCAL_JAVA_LIBRARIES := telephony-common
LOCAL_JAVA_LIBRARIES += mediatek-framework
LOCAL_JAVA_LIBRARIES += ims-common

#hb: tangjun add for hb-framework begin
LOCAL_JAVA_LIBRARIES += hb-framework
#hb: tangjun add for hb-framework end

LOCAL_PACKAGE_NAME := HMBSystemUI
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

LOCAL_RESOURCE_DIR := \
    ${LOCAL_PATH}/../HMBKeyguard/res \
    ${LOCAL_PATH}/../HMBKeyguard/res_ext \
    $(LOCAL_PATH)/res \
    $(LOCAL_PATH)/res_ext
LOCAL_AAPT_FLAGS := --auto-add-overlay --extra-packages com.android.keyguard

#hb: tangjun add for hb-framework begin
LOCAL_AAPT_FLAGS += -I out/target/common/obj/APPS/hb-framework-res_intermediates/package-export.apk
#hb: tangjun add for hb-framework end

ifneq ($(SYSTEM_UI_INCREMENTAL_BUILDS),)
    LOCAL_PROGUARD_ENABLED := disabled
    LOCAL_JACK_ENABLED := incremental
endif

include frameworks/base/packages/SettingsLib/common.mk

#hb add by tangjun begin
LOCAL_MULTILIB := 64
LOCAL_PREBUILT_JNI_LIBS := jni/lib64/libgenius_graphics.so
#hb add by tangjun end

include $(BUILD_PACKAGE)

#hb tangjun mod for don't do tests Android.mk begin
#ifeq ($(EXCLUDE_SYSTEMUI_TESTS),)
#    include $(call all-makefiles-under,$(LOCAL_PATH))
#endif
include $(LOCAL_PATH)/ext/Android.mk
#hb tangjun mod for don't do tests Android.mk end
