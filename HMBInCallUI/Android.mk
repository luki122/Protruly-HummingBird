LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

dialer_dir := Dialer
contacts_common_dir := ContactsCommon
phone_common_dir := ../HMBPhoneCommon

ifeq ($(TARGET_BUILD_APPS),)
support_library_root_dir := frameworks/support
else
support_library_root_dir := prebuilts/sdk/current/support
endif

src_dirs := src \
    $(dialer_dir)/src \
    $(contacts_common_dir)/src \
    $(phone_common_dir)/src \
    common/src

M: Add ContactsCommon ext
src_dirs += $(contacts_common_dir)/ext

res_dirs := res \
    $(dialer_dir)/res \
    $(contacts_common_dir)/res \
    $(phone_common_dir)/res

res_dirs += res_ext
res_dirs += $(contacts_common_dir)/res_ext
res_dirs += $(dialer_dir)/res_ext
# M: [InCallUI]needed by AddMemberEditView who extends MTKRecipientEditTextView
# M: [InCallUI]FIXME: should replace this with google default RecipientEditTextView
res_dirs += ../../../frameworks/ex/chips/res


#src_dirs += \
#    src-N \
#    $(dialer_dir)/src-N \
#    $(contacts_common_dir)/src-N \
 #   $(phone_common_dir)/src-N

# M: Add ext resources
# res_dirs += $(contacts_common_dir)/res_ext

LOCAL_SRC_FILES := $(call all-java-files-under, $(src_dirs))
LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dirs)) \
    $(support_library_root_dir)/v7/cardview/res \
    $(support_library_root_dir)/v7/recyclerview/res \
    $(support_library_root_dir)/v7/appcompat/res \
    $(support_library_root_dir)/design/res


LOCAL_SRC_FILES += \
        src/com/mediatek/telecom/recording/IPhoneRecorder.aidl\
        src/com/mediatek/telecom/recording/IPhoneRecordStateListener.aidl

LOCAL_SRC_FILES += src/com/hmb/manager/aidl/ICspService.aidl

LOCAL_AAPT_FLAGS := \
    --auto-add-overlay \
    --extra-packages android.support.v7.cardview \
    --extra-packages android.support.v7.cardview \
    --extra-packages android.support.v7.recyclerview \
    --extra-packages android.support.design \
    --extra-packages com.android.dialer \
    --extra-packages com.android.contacts.common \
    --extra-packages com.android.phone.common \
    --extra-packages com.android.mtkex.chips

LOCAL_JAVA_LIBRARIES := telephony-common \
                        ims-common

# M: [InCallUI]additional libraries
LOCAL_JAVA_LIBRARIES += mediatek-framework
# M: Add for ContactsCommon
LOCAL_JAVA_LIBRARIES += voip-common

# 引用hb-framework的资源
LOCAL_AAPT_FLAGS += -I out/target/common/obj/APPS/hb-framework-res_intermediates/package-export.apk
 
# 引用hb-framework的类
LOCAL_JAVA_LIBRARIES += hb-framework

LOCAL_STATIC_JAVA_LIBRARIES := \
    android-common \
    android-support-v13 \
    android-support-v4 \
    android-support-v7-cardview \
    android-support-v7-recyclerview \
    com.android.vcard \
    guava \
    libphonenumber

# M: add mtk-ex
LOCAL_STATIC_JAVA_LIBRARIES += com.mediatek.dialer.ext

# M: add for WFC support
LOCAL_STATIC_JAVA_LIBRARIES += wfo-common

# M: add for mtk-tatf case
#LOCAL_STATIC_JAVA_LIBRARIES += com.mediatek.tatf.common

# M: [InCallUI]ext library
LOCAL_STATIC_JAVA_LIBRARIES += com.mediatek.incallui.ext
# M: [InCallUI]added for MTKRecipientEditTextView
# M: [InCallUI]FIXME: should replace this with google default RecipientEditTextView
LOCAL_STATIC_JAVA_LIBRARIES += android-common-chips


LOCAL_PACKAGE_NAME := HMBInCallUI
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true
LOCAL_JACK_ENABLED := disabled  

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

# Uncomment the following line to build against the current SDK
# This is required for building an unbundled app.
#LOCAL_SDK_VERSION := current

include $(BUILD_PACKAGE)


# Use the following include to make our test apk.
# include $(call all-makefiles-under,$(LOCAL_PATH))
