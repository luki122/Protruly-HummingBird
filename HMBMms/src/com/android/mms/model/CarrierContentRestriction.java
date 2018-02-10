/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.mms.model;

import android.content.ContentResolver;

import com.android.mms.ContentRestrictionException;
import com.android.mms.ExceedMessageSizeException;
import com.android.mms.MmsConfig;
import com.android.mms.ResolutionException;
import com.android.mms.UnsupportContentTypeException;
import com.android.mms.data.WorkingMessage;
import com.android.mms.util.MmsContentType;
import com.android.mms.util.MmsLog;
import com.google.android.mms.ContentType;

import java.util.ArrayList;

/// M: Code analyze 001, new feature, Restricted mode check @{
/// @}

public class CarrierContentRestriction implements ContentRestriction {
    private static ArrayList<String> sSupportedImageTypes;
    private static ArrayList<String> sSupportedAudioTypes;
    private static ArrayList<String> sSupportedVideoTypes;
    private static final String IMAGE_BMP = "image/bmp";
    private static final String AUDIO_WAVE_2CH_LPCM = "audio/wave_2ch_lpcm";

    private static int mCreationMode;
    private static final boolean DEBUG = false;

    static {
        sSupportedImageTypes = MmsContentType.getImageTypes();
        // Add a ContentType for bmp
        if (!sSupportedImageTypes.contains(IMAGE_BMP)) {
            sSupportedImageTypes.add(IMAGE_BMP);
        }
        sSupportedAudioTypes = MmsContentType.getAudioTypes();
        if (!sSupportedAudioTypes.contains(AUDIO_WAVE_2CH_LPCM)) {
            sSupportedAudioTypes.add(AUDIO_WAVE_2CH_LPCM);
        }
        sSupportedVideoTypes = MmsContentType.getVideoTypes();
        /// M: Code analyze 002, new feature, add vCard support @{
        SUPPORTED_TYPES      = MmsContentType.getSupportedTypes();
        /// @}
    }

    public CarrierContentRestriction() {
    }

    //from qcmms
    public CarrierContentRestriction(int creationMode) {
        mCreationMode = creationMode;
        switch (creationMode) {
        case MmsConfig.CREATIONMODE_RESTRICTED:
        case MmsConfig.CREATIONMODE_WARNING:
            sSupportedImageTypes = new ArrayList<String>();
            sSupportedImageTypes.add(ContentType.IMAGE_JPEG);
            sSupportedImageTypes.add(ContentType.IMAGE_GIF);
            sSupportedImageTypes.add(ContentType.IMAGE_WBMP);

            sSupportedAudioTypes = new ArrayList<String>();
            sSupportedAudioTypes.add(ContentType.AUDIO_AMR);

            sSupportedVideoTypes = new ArrayList<String>();
            sSupportedVideoTypes.add(ContentType.VIDEO_3GPP);
            sSupportedVideoTypes.add(ContentType.VIDEO_H263);
            break;
        case MmsConfig.CREATIONMODE_FREE:
        default:
            sSupportedAudioTypes = ContentType.getAudioTypes();
            if (!sSupportedAudioTypes.contains(AUDIO_WAVE_2CH_LPCM)) {
                sSupportedAudioTypes.add(AUDIO_WAVE_2CH_LPCM);
            }
            break;
        }
    }
	
    public static ArrayList<String> getSupportedAudioTypes() {
        return sSupportedAudioTypes;
    }

    public void checkMessageSize(int messageSize, int increaseSize, ContentResolver resolver)
            throws ContentRestrictionException {
        if (DEBUG) {
            MmsLog.d(TAG, "CarrierContentRestriction.checkMessageSize messageSize: " +
                        messageSize + " increaseSize: " + increaseSize +
                        " MmsConfig.getMaxMessageSize: " + MmsConfig.getMaxMessageSize()
                        + " MmsConfig.getUserSetMmsSizeLimit: "
                        + MmsConfig.getUserSetMmsSizeLimit(true));
        }
        if ( (messageSize < 0) || (increaseSize < 0) ) {
            throw new ContentRestrictionException("Negative message size"
                    + " or increase size");
        }
        int newSize = messageSize + increaseSize;

        /// M: Code analyze 003, new feature, user set Mms MaxSizeLimit @{
        if ((newSize < 0) || (newSize > MmsConfig.getUserSetMmsSizeLimit(true))) {
            throw new ExceedMessageSizeException("Exceed message size limitation");
        }
        /// @}
    }

    public void checkResolution(int width, int height) throws ContentRestrictionException {
        MmsLog.d(TAG, "CarrierContentRestriction.checkResolution width = " + String.valueOf(width)
                                    + " Height = " + String.valueOf(height));
        if ( (width > MmsConfig.getMaxImageWidth()) || (height > MmsConfig.getMaxImageHeight()) ) {
            throw new ResolutionException("content resolution exceeds restriction.");
        }
    }

    public void checkImageContentType(String contentType)
            throws ContentRestrictionException {
        MmsLog.d(TAG, "CarrierContentRestriction.checkImageContentType = " + contentType);
        if (null == contentType) {
            throw new ContentRestrictionException("Null content type to be check");
        }

        /// M: Code analyze 001, new feature, Restricted mode check @{
        checkRestrictedContentType(contentType);
        /// @}
        if (!sSupportedImageTypes.contains(contentType)) {
            throw new UnsupportContentTypeException("Unsupported image content type : "
                    + contentType);
        }
    }

    public void checkAudioContentType(String contentType)
            throws ContentRestrictionException {
        MmsLog.d(TAG, "CarrierContentRestriction.checkAudioContentType = " + contentType);
        if (null == contentType) {
            throw new ContentRestrictionException("Null content type to be check");
        }

        /// M: Code analyze 001, new feature, Restricted mode check @{
        checkRestrictedContentType(contentType);
        /// @}
        if (!sSupportedAudioTypes.contains(contentType)) {
            throw new UnsupportContentTypeException("Unsupported audio content type : "
                    + contentType);
        }
    }

    public void checkVideoContentType(String contentType)
            throws ContentRestrictionException {
        MmsLog.d(TAG, "CarrierContentRestriction.checkVideoContentType = " + contentType);
        if (null == contentType) {
            throw new ContentRestrictionException("Null content type to be check");
        }

        /// M: Code analyze 001, new feature, Restricted mode check @{
        checkRestrictedContentType(contentType);
        /// @}

        if (!sSupportedVideoTypes.contains(contentType)) {
            throw new UnsupportContentTypeException("Unsupported video content type : "
                    + contentType);
        }
    }

    private static final String TAG = "MSG/slide";

    /// M: Code analyze 002, new feature, add vCard vCalendar support @{
    private static final ArrayList<String> SUPPORTED_TYPES;

    /// M: Code analyze 002, new feature, add vCard vCalendar support @{
    public void checkFileAttachmentContentType(String contentType) throws ContentRestrictionException {
        MmsLog.d(TAG, "CarrierContentRestriction.checkFileAttachmentContentType = " + contentType);
        if (null == contentType) {
            throw new ContentRestrictionException("Null content type to be check");
        }

        if (!SUPPORTED_TYPES.contains(contentType)) {
            throw new UnsupportContentTypeException("Unsupported content type : " + contentType);
        }
    }
    /// @}

    /// M: Code analyze 001, new feature, Restricted mode check @{
    private void checkRestrictedContentType(String contentType)
    throws ContentRestrictionException {
        MmsLog.d(TAG, "CarrierContentRestriction.checkRestrictedContentType = " + contentType);
        if (WorkingMessage.sCreationMode != 0 && !MmsContentType.isUnrestrictedType(contentType)) {
            throw new ContentRestrictionException("Restricted content type:" + contentType);
        }

    }
    /// @}
}
