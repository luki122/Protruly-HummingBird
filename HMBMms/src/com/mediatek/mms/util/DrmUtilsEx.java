package com.mediatek.mms.util;

import java.util.Iterator;
import java.util.List;

import com.android.mms.R;
import com.android.mms.MmsApp;
import com.android.mms.data.WorkingMessage;
import com.android.mms.model.MediaModel;
import com.android.mms.model.SlideModel;
import com.android.mms.model.SlideshowModel;
import com.android.mms.ui.ComposeMessageActivity;
import com.android.mms.ui.MessageItem;
import com.android.mms.util.FeatureOption;
import com.android.mms.util.MessageResource;
import com.android.mms.util.MmsContentType;
import com.android.mms.util.MmsLog;
import com.google.android.mms.pdu.PduPart;
import com.mediatek.drm.OmaDrmClient;
import com.mediatek.drm.OmaDrmStore;
import com.mediatek.drm.OmaDrmUtils;
import com.mediatek.drm.OmaDrmUiUtils;

import hb.app.dialog.AlertDialog;
import hb.app.dialog.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.Telephony.Mms.Part;
import android.view.View;
import android.widget.ImageView;

public class DrmUtilsEx {
    private final static String TAG = "DrmUtilsEx";

    public static boolean checkHasDrmContent(Context context, String src, Uri uri) {
        if (FeatureOption.MTK_DRM_APP) {
            if (src != null ? src.toLowerCase().endsWith(".dcf")
                                : checkUriContainsDrm(context, uri)) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkHasDrmRight(Context context, boolean hasDrmContent, Uri uri) {
        if (FeatureOption.MTK_DRM_APP && hasDrmContent) {
            int right = MmsApp.getApplication().getDrmManagerClient()
                    .checkRightsStatus(uri, OmaDrmStore.Action.DISPLAY);
            if (OmaDrmStore.RightsStatus.RIGHTS_VALID == right) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public static boolean hasDrmMediaInSlide(SlideModel slide) {
        if (!FeatureOption.MTK_DRM_APP) return false;
        for (Iterator<MediaModel> it = slide.iterator(); it.hasNext();) {
            MediaModel media = it.next();
            if (media.hasDrmContent()) {
                return true;
            }
        }
        return false;
    }

    /*
     * If no drm media, return true.
     */
    public static boolean hasDrmMediaRightInSlide(SlideModel slide) {
        if (!FeatureOption.MTK_DRM_APP) return false;
        for (Iterator<MediaModel> it = slide.iterator(); it.hasNext();) {
            MediaModel media = it.next();
            if (media.hasDrmContent() && !media.hasDrmRight()) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasDrmMediaInSlideshow(SlideshowModel slideshow) {
        if (!FeatureOption.MTK_DRM_APP) return false;
        for (Iterator<SlideModel> it = slideshow.iterator(); it.hasNext();) {
            SlideModel slide = it.next();
            if (hasDrmMediaInSlide(slide)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasDrmMediaRightInSlideshow(SlideshowModel slideshow) {
        if (!FeatureOption.MTK_DRM_APP) return false;
        for (Iterator<SlideModel> it = slideshow.iterator(); it.hasNext();) {
            SlideModel slide = it.next();
            if (!hasDrmMediaRightInSlide(slide)) {
                return false;
            }
        }
        return true;
    }

    public static String getDrmContentType(String src, String extention,
            String path, String contentType) {
        if (!FeatureOption.MTK_DRM_APP) return contentType;
        if ((src != null && src.endsWith(".dcf"))
                || (extention != null && extention.equals("dcf"))) {
            return MmsApp.getApplication()
                    .getDrmManagerClient().getOriginalMimeType(path);
        }
        return contentType;
    }

    public static String getDrmContentType(PduPart part, String contentType) {
        if (!FeatureOption.MTK_DRM_APP) return contentType;
        if (MmsContentType.isDrmType(contentType)) {
            contentType = MmsApp.getApplication().getDrmManagerClient()
                    .getOriginalMimeType(part.getDataUri());
        }
        return contentType;
    }

    public static boolean isDrm(String name) {
        if (name != null && name.toLowerCase().endsWith(".dcf")) {
            return true;
        } else {
            return false;
        }
    }

    public static Bitmap getDrmBitmapWithLockIcon(Context context,
            WorkingMessage msg, int drawableId, int iconId) {
        if (msg == null || !msg.hasDrmMedia()) return null;
        return getDrmBitmapWithLockIcon(context, drawableId, iconId);
    }

    public static Bitmap getDrmBitmapWithLockIcon(Context context,
            String src, int drawableId, int iconId) {
        if (src == null || !src.endsWith(".dcf")) return null;
        return getDrmBitmapWithLockIcon(context, drawableId, iconId);
    }

    public static Bitmap getDrmBitmapWithLockIcon(Context context,
            MessageItem mi, int drawableId, int iconId) {
        if (mi == null || !mi.hasDrmMedia()) return null;
        return getDrmBitmapWithLockIcon(context, drawableId, iconId);
    }

    public static Bitmap getDrmBitmapWithLockIcon(Context context,
            int drawableId, int iconId) {
        if (!FeatureOption.MTK_DRM_APP) return null;
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(),
                drawableId);
        Drawable front = context.getResources().getDrawable(iconId);
        Bitmap drmBitmap = OmaDrmUiUtils.overlayBitmap(
                MmsApp.getApplication().getDrmManagerClient(), bitmap, front);
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
            bitmap = null;
        }
        return drmBitmap;
    }

    public static Bitmap getDrmBitmapWithLockIcon(Context context,
            Bitmap bitmap, int iconId) {
        if (!FeatureOption.MTK_DRM_APP) return null;
        Drawable front = context.getResources().getDrawable(iconId);
        Bitmap drmBitmap = OmaDrmUiUtils.overlayBitmap(
                MmsApp.getApplication().getDrmManagerClient(), bitmap, front);
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
            bitmap = null;
        }
        return drmBitmap;
    }

    public static boolean checkUriContainsDrm(Context context, Uri uri) {
        if (uri != null && uri.getAuthority() != null && uri.getAuthority().equals("mms")) {
            Cursor c = context.getContentResolver().query(uri,
                    new String[] {Part._DATA}, null, null, null);
            try {
                if (c.moveToFirst()) {
                    String src = c.getString(c.getColumnIndexOrThrow(Part._DATA));
                    if (src != null && src.endsWith(".dcf")) {
                        MmsLog.d(TAG, "Media src ends with dcf, isDrmContent = true");
                        return true;
                    }
                }
            } finally {
                c.close();
            }
            MmsLog.d(TAG, "Media src does not end with dcf, isDrmContent = false");
            return false;
        }
        try {
            OmaDrmUtils.DrmProfile drmProfile = OmaDrmUtils.getDrmProfile(context, uri,
                    MmsApp.getApplication().getDrmManagerClient());
            MmsLog.d(TAG, "OmaDrmUtils Uri = " + uri + " isDrmContent = " + drmProfile.isDrm());
            return drmProfile.isDrm();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static void showDrmAlertDialog(Context context) {
        Builder builder = new Builder(context);
        builder.setMessage(R.string.drm_not_support);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        if (context instanceof ComposeMessageActivity) {
            ((ComposeMessageActivity) context).setCanResponse(true);
        }
    }

    public static void showDrmLock(Context context, SlideshowModel slideshow,
            int position, ImageView drmImageVideoLock, ImageView drmAudioLock) {
        drmImageVideoLock.setVisibility(View.GONE);
        drmAudioLock.setVisibility(View.GONE);

        if (!FeatureOption.MTK_DRM_APP) return;
            Resources res = context.getResources();
            boolean hasContent = false;
            boolean hasRight = false;
        if (slideshow.get(position) != null) {
            if (slideshow.get(position).hasAudio()) {
                hasContent = slideshow.get(position).getAudio().hasDrmContent();
                hasRight = slideshow.get(position).getAudio().hasDrmRight();
                if (hasContent && hasRight) {
                    drmAudioLock.setImageDrawable(
                            res.getDrawable(MessageResource.drawable.drm_green_lock));
                    drmAudioLock.setVisibility(View.VISIBLE);
                } else if (hasContent) {
                    drmAudioLock.setImageDrawable(
                            res.getDrawable(MessageResource.drawable.drm_red_lock));
                    drmAudioLock.setVisibility(View.VISIBLE);
                }
            }
            if (slideshow.get(position).hasImage()) {
                hasContent = slideshow.get(position).getImage().hasDrmContent();
                hasRight = slideshow.get(position).getImage().hasDrmRight();
                if (hasContent && hasRight) {
                      drmImageVideoLock.setImageDrawable(
                              res.getDrawable(MessageResource.drawable.drm_green_lock));
                      drmImageVideoLock.setVisibility(View.VISIBLE);
                } else if (hasContent) {
                    drmImageVideoLock.setImageDrawable(
                            res.getDrawable(MessageResource.drawable.drm_red_lock));
                    drmImageVideoLock.setVisibility(View.VISIBLE);
                }
            } else if (slideshow.get(position).hasVideo()) {
                hasContent = slideshow.get(position).getVideo().hasDrmContent();
                hasRight = slideshow.get(position).getVideo().hasDrmRight();
                if (hasContent && hasRight) {
                    drmImageVideoLock.setImageDrawable(
                            res.getDrawable(MessageResource.drawable.drm_green_lock));
                    drmImageVideoLock.setVisibility(View.VISIBLE);
                } else if (hasContent) {
                    drmImageVideoLock.setImageDrawable(
                            res.getDrawable(MessageResource.drawable.drm_red_lock));
                    drmImageVideoLock.setVisibility(View.VISIBLE);
                }
            }
        }
    }
}
