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

package com.android.mms.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract.Profile;
import android.provider.Telephony.Sms;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.LineHeightSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import android.provider.Telephony;
import android.provider.Telephony.Mms;

import com.android.mms.LogTag;
import com.android.mms.MmsApp;
import com.android.mms.R;
import com.android.mms.data.Contact;
import com.android.mms.data.WorkingMessage;
import com.android.mms.model.SlideModel;
import com.android.mms.model.SlideshowModel;
import com.android.mms.transaction.Transaction;
import com.android.mms.transaction.TransactionBundle;
import com.android.mms.transaction.TransactionService;
import com.android.mms.util.DownloadManager;
import com.android.mms.util.ItemLoadedCallback;
import com.android.mms.util.ThumbnailManager.ImageLoaded;
import com.android.mms.util.MmsContentType;
import com.google.android.mms.pdu.PduHeaders;


/// M: @{
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.MailTo;
import android.provider.Browser;
import android.text.style.LeadingMarginSpan;
import android.text.Spannable;
import android.widget.CheckBox;
import android.widget.Toast;

import com.android.internal.telephony.PhoneConstants;
import com.android.mms.MmsConfig;
import com.google.android.mms.util.SqliteWrapper;
import com.android.mms.util.FeatureOption;
import com.android.mms.util.MmsLog;
import com.android.mms.util.MessageResource;
import com.mediatek.ipmsg.util.IpMessageUtils;
import com.mediatek.mms.callback.IFileAttachmentModelCallback;
import com.mediatek.mms.ext.IOpMessageListItemExt;
import com.mediatek.mms.callback.IMessageUtilsCallback;
import com.mediatek.mms.model.FileAttachmentModel;
import com.mediatek.mms.model.VCardModel;
import com.mediatek.mms.util.VCardUtils;
import com.mediatek.opmsg.util.OpMessageUtils;
import com.mediatek.storage.StorageManagerEx;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/// @}
import android.app.Activity;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.provider.MediaStore.Video.Thumbnails;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.ProgressBar;
/// M: @{

//add for attachment enhance
import java.io.FileInputStream;
import java.io.FileOutputStream;

import com.mediatek.mms.ext.IOpMessageItemExt;
import android.content.ContentResolver;
import android.os.Environment;

import com.google.android.mms.pdu.PduBody;
import com.google.android.mms.pdu.PduPart;
import com.google.android.mms.MmsException;

import android.graphics.drawable.Drawable;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;

import com.mediatek.mms.ipmessage.IIpMessageItemExt;
import com.mediatek.mms.ipmessage.IIpMessageListItemExt;
import com.mediatek.mms.callback.IMessageListItemCallback;
import com.mediatek.mms.util.DrmUtilsEx;
//tangyisen
import hb.app.dialog.AlertDialog;

/// @}
/**
 * This class provides view of a message in the messages list.
 */
public class MessageListItem extends LinearLayout implements
 SlideViewInterface, OnClickListener, IMessageListItemCallback {
    public static final String EXTRA_URLS = "com.android.mms.ExtraUrls";

    private static final String TAG = "Mms/MessageListItem";
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_DONT_LOAD_IMAGES = false;

    private static final String M_TAG = "Mms/MessageListItem";
    public static final String TAG_DIVIDER = "Mms/divider";

    private static final StyleSpan STYLE_BOLD = new StyleSpan(Typeface.BOLD);
    private static HashMap<Integer, String> mHashSub = new HashMap<Integer, String>();
    private String mDefualtSubName = "SUB01";

    static final int MSG_LIST_EDIT    = 1;
    static final int MSG_LIST_PLAY    = 2;
    static final int MSG_LIST_DETAILS = 3;
    
    //begin tangyisen add
    public static final int MSG_LIST_SHOW_EDIT = 8;
    public static final int MSG_LIST_RESEND = 9;
    //end tangyisen add

    static final int MSG_LIST_EDIT_MMS   = 1;
    static final int MSG_LIST_EDIT_SMS   = 2;
    private static final int PADDING_LEFT_THR = 3;
    private static final int PADDING_LEFT_TWE = 13;

    public static final int text_view = 1;
    public static final int date_view = 2;
    //public static final int sim_status = 3;//tangyisen sim1 or sim2
//    public static final int account_icon = 4;
    //tangyisen delete
    /*public static final int locked_indicator = 5;
    public static final int delivered_indicator = 6;
    public static final int details_indicator = 7;
    public static final int avatar = 8;*/
    public static final int message_block = 9;
    public static final int select_check_box = 10;
    public static final int download_view = 11;
    public static final int mms_circle_imageview = 12;
    public static final int date_sim_space_view = 13;
    public static final int panel_content_space = 14;
    public static final int group_send_info = 15;
    public static final int group_send_detail = 16;
    public static final int send_status = 17;
    public static final int failed_resend = 18;
    public static final int sim_view = 19;
    public static final int panel_content = 20;
    public static final int mms_layout_view_parent = 21;
    public static final int image_view = 22;
    public static final int play_slideshow_button = 23;
    public static final int mms_view = 24;
    public static final int mms_layout_view_stub = 25;
    
    public boolean mIsShowFileAttachmentView;
    //begin tangyisen add fix bug
    public boolean mIsShowVcardView;
    //end tangyisen add fix bug
    //tangyisen
    /*public static final int time_divider = 11;
    public static final int time_divider_str = 12;
    public static final int on_line_divider = 15;
    public static final int on_line_divider_str = 16;
    public static final int sim_divider = 17;
    public static final int text_expire = 18;
    public static final int sender_name = 19;
    public static final int sender_name_separator = 20;
    public static final int sender_photo = 21;
    public static final int send_time_txt = 22;
    public static final int double_time_layout = 23;
    public static final int mms_file_attachment_view_stub = 24;
    public static final int file_attachment_view = 25;
    public static final int file_attachment_thumbnail = 26;
    public static final int file_attachment_name_info = 27;
    public static final int file_attachment_name_info2 = 28;
    public static final int file_attachment_thumbnail2 = 29;
    public static final int file_attachment_size_info = 30;
    public static final int mms_view = 31;
    public static final int mms_layout_view_stub = 32;
    public static final int image_view = 33;
    public static final int play_slideshow_button = 34;
    public static final int mms_downloading_view_stub = 35;
    public static final int btn_download_msg = 36;
    public static final int label_downloading = 37;
    public static final int mms_download_controls = 38;
    public static final int status_panel = 39;*/

    public static int MSG_LIST_SHOW_MSGITEM_DETAIL = 3600;

    private View mMmsView;
    /// M: add for vcard
    private View mFileAttachmentView;
    private ZzzCircleImageView mImageView;
    //private ImageView mImageView;
    //tangyisen delete begin
    //private ImageView mLockedIndicator;
    //private ImageView mDeliveredIndicator;//tangyisen will can use for status
    //private ImageView mDetailsIndicator;
    private ImageView mSlideShowButton;
    private ImageView mSimCardView;
    private ZzzMessageItemTextView mBodyTextView;
    private TextView mDownloadView;
    private ZzzCircleImageView mMmsCircleImageView;
    private LinearLayout mPanelContent;
    private View mDateSimSpaceView;
    private View mPanelContentSpace;
    private TextView mGroupSendInfo;
    private TextView mGroupSendDetail;
    private TextView mSendStatus;
    private TextView mFailResend;
    private ZzzMmsParentLinearLayout mMmsLayoutParent;
    //private Button mDownloadButton;
    //private TextView mDownloadingLabel;
    //tangyisen delete end
    private Handler mHandler;
    private MessageItem mMessageItem;
    private String mDefaultCountryIso;
    private TextView mDateView;
//    public View mMessageBlock;
    private Path mPathRight;
    private Path mPathLeft;
    private Paint mPaint;
    //tangyisen delete
    //private QuickContactDivot mAvatar;
    private boolean mIsLastItemInList;
    static private Drawable sDefaultContactImage;
    private Presenter mPresenter;
    private int mPosition;      // for debugging
    private ImageLoadedCallback mImageLoadedCallback;

    /// M: google JB.MR1 patch, group mms
    private boolean mIsGroupMms;
    /// M: fix bug ALPS00439894, MTK MR1 new feature: Group Mms
    //tangyisen delete begin
    //private QuickContactBadge mSenderPhoto;
    //private TextView mSenderName;
    //private View mSendNameSeparator;
    //tangyisen delete end

    private Toast mInvalidContactToast;
    private LeadingMarginSpan mLeadingMarginSpan;
    private long mHandlerTime = 0;
    /// M:
    private static  boolean sImageButtonCanClick = true; // this is a hack for quick click.

    //tangyisen delete begin
    /// M: add for time divider
    //private View mTimeDivider; // time_divider
    //private TextView mTimeDividerStr; // time_divider_str
    /// M: add for online divider
    //private View mOnLineDivider; // on_line_divider
    //private TextView mOnLineDividertextView; // on_line_divider_str
    /// M: add for sub message divider
    //private View mSubDivider;
    //private TextView mExpireText;
    //tangyisen delete end

    private MessageListAdapter mMessageListAdapter;

    //tangyisen delete
    /*IIpMessageListItemExt mIpMessageListItem;

    IOpMessageListItemExt mOpMessageListItemExt;*/

    public MessageListItem(Context context, View view) {
        super(context);
        initView( context, view );
    }
    /*public MessageListItem(Context context) {
        super(context);
        init(context);
    }

    public MessageListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    private void init(Context context) {
        mDefaultCountryIso = MmsApp.getApplication().getCurrentCountryIso();
        int color = context.getResources().getColor(R.color.timestamp_color);
        mColorSpan = new ForegroundColorSpan(color);
        if (sDefaultContactImage == null) {
            sDefaultContactImage = context.getResources().getDrawable(R.drawable.ic_contact_picture);
        }
    }*/

    public View findView(int id) {
        return findView(id, null);
    }

    public View findView(int id, View view) {
        View parentView = this;
        if (view != null) {
            parentView = view;
        }
        // add for ipmessage
        /*View find = mIpMessageListItem.findIpView(id, parentView);
        if (find != null) {
            return find;
        }*/

        switch (id) {
            case text_view:
                return parentView.findViewById(R.id.text_view_buttom);
            case date_view:
                return parentView.findViewById(R.id.date_view);
            /*case sim_status:
                return parentView.findViewById(R.id.sim_view);*/
                //tangyisen
           /* case locked_indicator:
                return parentView.findViewById(R.id.locked_indicator);
            case delivered_indicator:
                return parentView.findViewById(R.id.delivered_indicator);
            case details_indicator:
                return parentView.findViewById(R.id.details_indicator);
            case avatar:
                return parentView.findViewById(R.id.avatar);*/
            case select_check_box:
                return parentView.findViewById(R.id.list_item_check_box);
                //tangyisen
            /*case time_divider:
                return parentView.findViewById(R.id.time_divider);
            case time_divider_str:
                return parentView.findViewById(R.id.time_divider_str);
            case on_line_divider:
                return parentView.findViewById(R.id.on_line_divider);
            case on_line_divider_str:
                return parentView.findViewById(R.id.on_line_divider_str);
            case sim_divider:
                return parentView.findViewById(R.id.sim_divider);
            case text_expire:
                return parentView.findViewById(R.id.text_expire);
            case sender_name:
                return parentView.findViewById(R.id.sender_name);
            case sender_name_separator:
                return parentView.findViewById(R.id.sender_name_separator);
            case sender_photo:
                return parentView.findViewById(R.id.sender_photo);
            case send_time_txt:
                return parentView.findViewById(R.id.send_time_txt);
            case double_time_layout:
                return parentView.findViewById(R.id.double_time_layout);
            case mms_file_attachment_view_stub:
                return parentView.findViewById(R.id.mms_file_attachment_view_stub);
            case file_attachment_view:
                return parentView.findViewById(R.id.file_attachment_view);
            case file_attachment_thumbnail:
                return parentView.findViewById(R.id.file_attachment_thumbnail);
            case file_attachment_name_info:
                return parentView.findViewById(R.id.file_attachment_name_info);
            case file_attachment_name_info2:
                return parentView.findViewById(R.id.file_attachment_name_info2);
            case file_attachment_thumbnail2:
                return parentView.findViewById(R.id.file_attachment_thumbnail2);
            case file_attachment_size_info:
                return parentView.findViewById(R.id.file_attachment_size_info);
            case mms_view:
                return parentView.findViewById(R.id.mms_view);
            case mms_layout_view_stub:
                return parentView.findViewById(R.id.mms_layout_view_stub);
            case image_view:
                return parentView.findViewById(R.id.image_view);
            case play_slideshow_button:
                return parentView.findViewById(R.id.play_slideshow_button);
            case mms_downloading_view_stub:
                return parentView.findViewById(R.id.mms_downloading_view_stub);
            case btn_download_msg:
                return parentView.findViewById(R.id.btn_download_msg);
            case label_downloading:
                return parentView.findViewById(R.id.label_downloading);
            case mms_download_controls:
                return parentView.findViewById(R.id.mms_download_controls);
            case status_panel:
                return parentView.findViewById(R.id.status_panel);*/
            case download_view:
                return parentView.findViewById(R.id.mms_download);
            case mms_circle_imageview:
                return null;
            case date_sim_space_view:
                return parentView.findViewById(R.id.date_sim_space);
            case panel_content_space:
                return parentView.findViewById(R.id.panel_content_space);
            case group_send_info:
                return parentView.findViewById(R.id.group_send_info);
            case group_send_detail:
                return parentView.findViewById(R.id.group_send_detail);
            case send_status:
                return parentView.findViewById(R.id.send_status);
            case failed_resend:
                return parentView.findViewById(R.id.failed_resend);
            case sim_view:
                return parentView.findViewById(R.id.sim_view);
            case panel_content:
                return parentView.findViewById(R.id.panel_content);
            case mms_layout_view_parent:
                return parentView.findViewById( R.id.mms_layout_view_parent );
            case image_view:
                return parentView.findViewById(R.id.image_view);
            case play_slideshow_button:
                return parentView.findViewById(R.id.play_slideshow_button);
            case mms_view:
                return parentView.findViewById(R.id.mms_view);
            case mms_layout_view_stub:
                return parentView.findViewById(R.id.mms_layout_view_stub);
            default:
        }
        return null;
    }

    protected void onFinishInflateView() {
        super.onFinishInflate();

        // add for ipmessage
        //mIpMessageListItem.onIpFinishInflate(mContext, mBodyTextView, this, mHandler, this);
        //begin tangyisen
        mMmsLayoutParent = (ZzzMmsParentLinearLayout) findView(mms_layout_view_parent);
        //updateMmsLayoutParentBackground(mMessageItem.mContext, mMessageItem.getSubId());
        if(mMmsLayoutParent != null) {
            mMmsLayoutParent.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    //Log.d(TAG, "mMmsLayoutParent onLongClick");
                    sendMessage(mMessageItem, MSG_LIST_SHOW_EDIT);
                    return true;
                }
            });
        }
        //end tangyisen
        mBodyTextView = (ZzzMessageItemTextView) findView(text_view);
        mBodyTextView.setOnDoubleClickListener(new ZzzMessageItemTextView.OnDoubleClickListener() {
            @Override
            public void onDoubleClick() {
                // TODO Auto-generated method stub
                //if (!mIsCheckBoxMode) {
                    MessageUtils.showSmsMessageContent(getContext(), mBodyTextView.getText().toString());
                //}
            }
        });
        mBodyTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View arg0) {
                //Log.d(TAG, "mBodyTextView onLongClick");
                sendMessage(mMessageItem, MSG_LIST_SHOW_EDIT);
                return true;
            }
        });
        mDateView = (TextView) findView(date_view);
        //tangyisen here only inflate the common view
        mSimCardView = (ImageView) findView(sim_view);
        /// M: @{
        //mSubStatus = (TextView) findView(sim_status);
        /// @}
        //tangyisen
        /*mLockedIndicator = (ImageView) findView(locked_indicator);
        mDeliveredIndicator = (ImageView) findView(delivered_indicator);
        mDetailsIndicator = (ImageView) findView(details_indicator);
        mAvatar = (QuickContactDivot) findView(avatar);*/
        /// M: Remove Google default code
        // mMessageBlock = findView(message_block);
        /// M: @{
        //add for multi-delete
        mSelectedBox = (CheckBox) findView(select_check_box);
        /// @}

        /// M: add for time divider
        //tangyisen
        /*mTimeDivider = (View) findView(time_divider);
        if (null != mTimeDivider) {
            mTimeDividerStr = (TextView) findView(time_divider_str, mTimeDivider);
        }
        mOnLineDivider = (View) findView(on_line_divider);
        if (null != mOnLineDivider) {
            mOnLineDividertextView = (TextView) findView(on_line_divider_str, mOnLineDivider);
        }
        mSubDivider = (View) findView(sim_divider);
        mExpireText = (TextView) findView(text_expire);*/
        //begin tangyisen
        /*mDownloadView = (CheckBox) findView(download_view);
        mMmsCircleImageView = (ZzzCircleImageView) findView(mms_circle_imageview);
        mDateSimSpaceView = findView(date_sim_space_view);
        mPanelContentSpace = findView(panel_content_space);
        mGroupSendInfo = (TextView) findView(group_send_info);
        mGroupSendDetail = (TextView) findView(group_send_detail);
        mSendStatus = (TextView) findView(send_status);
        mFailResend = (ImageView) findView(failed_resend);
        if(null != mFailResend) {
        	mFailResend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    // TODO Auto-generated method stub
                    if(!mIsCheckBoxMode) {
                        if(null != mMessageSendRemind) {
                            sendMessage(mMessageItem, MSG_LIST_RESEND);
                        }
                    }
                }
            });
        }*/
        mMmsCircleImageView = (ZzzCircleImageView) findView(mms_circle_imageview);
        if (mMmsCircleImageView != null) {
            mMmsCircleImageView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View arg0) {
                    //Log.d(TAG, "mMmsCircleImageView onLongClick");
                    sendMessage(mMessageItem, MSG_LIST_SHOW_EDIT);
                    return true;
                }
            });
        }
        if (mMessageItemType == INCOMING_ITEM_TYPE) {
            mDownloadView = (TextView) findView(download_view);
        } else {
            mDateSimSpaceView = (View) findView(date_sim_space_view);
            mPanelContentSpace  = (View) findView(panel_content_space);
            mGroupSendInfo = (TextView) findView(group_send_info);
            mGroupSendDetail = (TextView) findView(group_send_detail);
            if(null != mGroupSendDetail) {
                mGroupSendDetail.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        showGroupDetailActivty( getContext() );
                    }
                });
            }
            mSendStatus = (TextView) findView(send_status);
            mFailResend = (TextView) findView(failed_resend);
            mPanelContent = (LinearLayout) findView(panel_content);
            if(null != mFailResend) {
                Drawable d = mContext.getResources().getDrawable(R.drawable.zzz_ic_message_failed_resend);
                d.setBounds(0, 0, d.getIntrinsicHeight(), d.getIntrinsicHeight());
                //tv.setCompoundDrawablePadding(10);
                mFailResend.setCompoundDrawables(null, null, d, null);
                mFailResend.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        // TODO Auto-generated method stub
                        if (!MmsConfig.isSmsEnabled(mContext)) {
                            return;
                        }
                        if(mIsGroupMms && mMessageItem.isSms()) {
                            showGroupDetailActivty( getContext() );
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                            builder.setCancelable(true);
                            builder.setMessage(getContext().getResources().getString( R.string.besure_resend_text ));
                            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface arg0, int arg1) {
                                    if(!mIsCheckBoxMode) {
                                        sendMessage(mMessageItem, MSG_LIST_RESEND);
                                    }
                                }
                            });
                            builder.setNegativeButton(R.string.no, null);
                            builder.show();
                        }
                    }
                });
            }
        }
        //end tangyisen
    }

    public void bind(MessageItem msgItem, boolean convGroupMms, int position, boolean isDeleteMode) {
        if (msgItem == null) {
            /// M: google jb.mr1 patch, group mms. isLastItem (useless) ? convHasMultiRecipients
            boolean isLastItem = convGroupMms;
            bindDefault(null, isLastItem);
            MmsLog.i(TAG, "bind: msgItem is null, position = " + position);
            return;
        }
        /// M: fix bug ALPS00383381 @{
        if(DEBUG) MmsLog.i(TAG, "MessageListItem.bind() : msgItem.mSubId = " + msgItem.mSubId
                + ", position = " + position +
                "uri = " + msgItem.mMessageUri);
        /// @}
        mMessageItem = msgItem;
        mIsGroupMms = convGroupMms;
        mPosition = position;
        /// fix bug ALPS00400536, set null text to avoiding reuse view @{
        //tangyisen
        mBodyTextView.setContent("", msgItem.isInboxMessage());
        /// @}

        /// M: fix bug ALPS00439894, MTK MR1 new feature: Group Mms
        /// set Gone to avoiding reuse view (Visible)
        //tangyisen
        /*if (!mMessageItem.isMe()) {
            mSenderName = (TextView) findView(sender_name);
            mSendNameSeparator = findView(sender_name_separator);
            mSenderPhoto = (QuickContactBadge) findView(sender_photo);
            if (mSenderName != null && mSenderPhoto != null && mSendNameSeparator != null) {
                mSenderName.setVisibility(View.GONE);
                mSendNameSeparator.setVisibility(View.GONE);
                mSenderPhoto.setVisibility(View.GONE);
            }
        }*/

        //tangyisen begin
        updateSimIcon(msgItem.mContext, SubscriptionManager.getSlotId(mMessageItem.getSubId()));
        //tangyisen end
        /// M: @{
        if (isDeleteMode) {
            mSelectedBox.setVisibility(View.VISIBLE);
           /* if (msgItem.isSelected()) {
                setSelectedBackGroud(true);
            }else {
                setSelectedBackGroud(false);
            }*/
        } else {
            /// M: change for ALPS01899925, set the background as null if not in delete mode. @{
            if (msgItem.isSubMsg()) {
                setSelectedBackGroud(false);
            }
            /// @}
            mSelectedBox.setVisibility(View.GONE);
        }
        /// M: @{

        setLongClickable(false);
        //set item these two false can make listview always get click event.
        setFocusable(false);
        setClickable(false);

        mContext = msgItem.mContext;

        /*if (!msgItem.isSubMsg()) {
            bindDividers(msgItem, isDeleteMode);
            // add for ipmessage
            if (mIpMessageListItem.onIpBind(msgItem.mIpMessageItem, msgItem.mMsgId,
                    msgItem.mIpMessageId, isDeleteMode)) {
                return;
            }
        }*/

        //mOpMessageListItemExt.bind(mMessageItem.mOpMessageItemExt);

        //tangyisen
        /*if (mSubDivider != null) {
            if (msgItem.isSubMsg() && (position > 0)) {
                mSubDivider.setVisibility(View.VISIBLE);
            } else {
                mSubDivider.setVisibility(View.GONE);
            }
        }*/

        switch (msgItem.mMessageType) {
            case PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND:
                bindNotifInd();
                break;
            default:
                bindCommonMessage();
                break;
        }
       // setSubIconAndLabel();
    }

    public void unbind() {
        ///M: fix bug ALPS00383381 @{
        if(DEBUG) MmsLog.i(TAG, "unbind() :  " + " position = " + mPosition + "uri = " +
                (mMessageItem == null ? "" : mMessageItem.mMessageUri));
        /// @}
        // Clear all references to the message item, which can contain attachments and other
        // memory-intensive objects
        mMessageItem = null;
        if (mImageView != null) {
            // Because #setOnClickListener may have set the listener to an object that has the
            // message item in its closure.
            mImageView.setOnClickListener(null);
            //tangyisen add
            //mImageView.setOnLongClickListener( null );
        }
        //tangyisen
        if (mSlideShowButton != null) {
            // Because #drawPlaybackButton sets the tag to mMessageItem
            mSlideShowButton.setTag(null);
        }
        // leave the presenter in case it's needed when rebound to a different MessageItem.
        if (mPresenter != null) {
            mPresenter.cancelBackgroundLoading();
        }
        //mIpMessageListItem.onIpUnbind();
    }

    public MessageItem getMessageItem() {
        return mMessageItem;
    }

    public void setMsgListItemHandler(Handler handler) {
        mHandler = handler;
    }

    private void bindNotifInd() {
        showMmsView(false);
        //tangyisen add
        MessageUtils.hideView(mFailResend);
        //tangyisen
        // add for vcard
        hideFileAttachmentViewIfNeeded();

        /// M: fix bug ALPS00423228, reuse last view when refresh ListView @{
        //tangyisen modify
        //mDateView.setText("");
        mDateView.setVisibility(View.VISIBLE);
        /// M: google jb.mr1 patch, group mms
        String dateStr = buildTimestampLine( mMessageItem.mTimestamp);/*mMessageItem.isSending()
                ? mContext.getResources().getString(R.string.sending_message)
                : buildTimestampLine(mMessageItem.mTimestamp);*/
        mDateView.setText(dateStr);
        /// @}

        String msgSizeText = mContext.getString(R.string.message_size_label)
                                + String.valueOf((mMessageItem.mMessageSize + 1023) / 1024)
                                + mContext.getString(R.string.kilobyte);

        mBodyTextView.setVisibility(View.VISIBLE);
        //tangyisen
        /*mBodyTextView.setContent(formatMessage(mMessageItem, null,
                              mMessageItem.mSubject,
                              mMessageItem.mHighlight,
                              mMessageItem.mTextContentType), mMessageItem.isInboxMessage());*/
        if (null != mBodyTextView) {
            mBodyTextView.setVisibility(View.VISIBLE);
            mBodyTextView.setContent(buildTimestampLine(msgSizeText + "\n"
                + mMessageItem.mExpireOnTimestamp), false);
        }
        //tangyisen end
        /// M:
        //tangyisen
        /*mExpireText.setText(msgSizeText + "\t\n" + mMessageItem.mTimestamp);
        mExpireText.setVisibility(View.VISIBLE);*/
        //MessageUtils.setSubIconAndLabel(mMessageItem.mSubId, null, mSubStatus);
        if(DEBUG) MmsLog.i(TAG, "bindNotifInd: uri = " + mMessageItem.mMessageUri +
                    ", position = " + mPosition + ", downloading Status ="
                    + mMessageItem.getMmsDownloadStatus());
        switch (mMessageItem.getMmsDownloadStatus()) {
            case DownloadManager.STATE_DOWNLOADING:
                showDownloadingAttachment();
                /// M: @{
                //tangyisen delete
                //findView(text_view).setVisibility(GONE);
                /// @}
                break;
            case DownloadManager.STATE_TRANSIENT_FAILURE:
            case DownloadManager.STATE_PERMANENT_FAILURE:
                showDownloadFailedAttachment();
                setDownloadViewClick();
                break;
            case DownloadManager.STATE_UNKNOWN:
            case DownloadManager.STATE_UNSTARTED:
                /** M: comment this code, this code bug fix is not perfect. there is other bigger bugs
                DownloadManager downloadManager = DownloadManager.getInstance();
                boolean autoDownload = downloadManager.isAuto();
                boolean dataSuspended = (MmsApp.getApplication().getTelephonyManager()
                        .getDataState() == TelephonyManager.DATA_SUSPENDED);

                // If we're going to automatically start downloading the mms attachment, then
                // don't bother showing the download button for an instant before the actual
                // download begins. Instead, show downloading as taking place.
                if (autoDownload && !dataSuspended) {
                    showDownloadingAttachment();
                    break;
                }
                */
            default:
                setLongClickable(true);
                //tangyisen
                showDownloadClickAttachment();
                /*inflateDownloadControls();
                mDownloadingLabel.setVisibility(View.GONE);
                mDownloadButton.setVisibility(View.VISIBLE);*/
                /// M: @{
                //findView(text_view).setVisibility(GONE);
                /// @}
                //tangyisen
                setDownloadViewClick();
                //mtk81083 this is a google default bug. it has no this code!
                // When we show the mDownloadButton, this list item's onItemClickListener doesn't
                // get called. (It gets set in ComposeMessageActivity:
                // mMsgListView.setOnItemClickListener) Here we explicitly set the item's
                // onClickListener. It allows the item to respond to embedded html links and at the
                // same time, allows the button to work.
                //tangyisen delete
                /*setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onMessageListItemClick();
                    }
                });*/
                break;
        }

        /*mOpMessageListItemExt.bindNotifInd(this.getContext(),
                msgSizeText, mExpireText, mSubStatus, mSelectedBox, MmsConfig.isSmsEnabled(mContext),
                (TextView) findView(send_time_txt),
                (LinearLayout) findView(double_time_layout));*/
        // Hide the indicators.
        /// M: @{
        //mLockedIndicator.setVisibility(View.GONE);
        //tangyisen
        /*if (mMessageItem.mLocked) {
            mLockedIndicator.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_lock_message_sms));
            mLockedIndicator.setVisibility(View.VISIBLE);
            mSubStatus.setPadding(PADDING_LEFT_THR, 0, 0, 0);
        } else {
            mLockedIndicator.setVisibility(View.GONE);
            mSubStatus.setPadding(PADDING_LEFT_TWE, 0, 0, 0);
        }
        /// @}
        mDeliveredIndicator.setVisibility(View.GONE);
        mDetailsIndicator.setVisibility(View.GONE);*/
        /// M: Remove Google default code
//        updateAvatarView(msgItem.mAddress, false);

    }

    //begin tangyisen
    public void setDownloadViewClick() {
        if(mDownloadView == null) {
            inflateDownloadControls();
        }
        mDownloadView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ///M: When it's not default SMS, do not download
                if (!MmsConfig.isSmsEnabled(mContext)) {
                    Toast.makeText(mContext, R.string.download_disabled_toast, Toast.LENGTH_LONG).show();
                    return;
                }

                ///M: fix bug ALPS00383381 @{
                // avoid mMessageItem is already setted null
                if (mMessageItem == null || mMessageItem.mMessageUri == null) {
                    MmsLog.v(TAG, "downloadButton onClick, mMessageItem or mMessageUri is null");
                    return;
                }
                //@}

                /// M: @{
                //add for multi-delete
                if (mSelectedBox != null && mSelectedBox.getVisibility() == View.VISIBLE) {
                    return;
                }
                if(DEBUG) MmsLog.i(TAG, "bindNotifInd: download button onClick: uri = " + mMessageItem.mMessageUri +
                        ", position = " + mPosition);

                //tangyisen delete
                /*if (mOpMessageListItemExt.onDownloadButtonClick(
                        ComposeMessageActivity.getComposeContext(),
                        MmsConfig.getDeviceStorageFullStatus())) {
                    return;
                }*/

                // add for gemini
                int subId = 0;
                // get sub id by uri
                Cursor cursor = SqliteWrapper.query(mMessageItem.mContext,
                        mMessageItem.mContext.getContentResolver(),
                        mMessageItem.mMessageUri, new String[] {
                            Telephony.Mms.SUBSCRIPTION_ID
                        }, null, null, null);
                if (cursor != null) {
                    try {
                        if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                            subId = cursor.getInt(0);
                        }
                    } finally {
                        cursor.close();
                    }
                }

                /// M: fix bug ALPS00406912
                mMessageItem.mMmsStatus = DownloadManager.STATE_DOWNLOADING;
                DownloadManager.getInstance().markState(mMessageItem.mMessageUri, DownloadManager.STATE_DOWNLOADING);
                if (mMessageListAdapter != null) {
                    mMessageListAdapter.saveNotifIndStatus(mMessageItem.getMessageId(), DownloadManager.STATE_DOWNLOADING);
                }

                //tangyisen
                /*mDownloadingLabel.setVisibility(View.VISIBLE);
                mDownloadButton.setVisibility(View.GONE);*/
                showDownloadingAttachment();
                //end tangyisen
                Intent intent = new Intent(mContext, TransactionService.class);
                intent.putExtra(TransactionBundle.URI, mMessageItem.mMessageUri.toString());
                intent.putExtra(TransactionBundle.TRANSACTION_TYPE,
                        Transaction.RETRIEVE_TRANSACTION);
                // add for gemini
                intent.putExtra(PhoneConstants.SUBSCRIPTION_KEY, subId);
                mContext.startService(intent);
            }
        });
    }
    //end tangyisen

    /// M: google JB.MR1 patch, group mms
    private String buildTimestampLine(String timestamp) {
        if (!mIsGroupMms || mMessageItem.isMe() || TextUtils.isEmpty(mMessageItem.mContact)) {
            // Never show "Me" for messages I sent.
            return timestamp;
        }

        /// M: fix bug ALPS00439894, MTK MR1 new feature: Group Mms
        //tangyisen
        /*if (mSenderName != null && mSenderPhoto != null && mSendNameSeparator != null) {
            mSendNameSeparator.setVisibility(View.VISIBLE);
            mSenderName.setText(mMessageItem.mContact);
            mSenderName.setVisibility(View.VISIBLE);
            Drawable avatarDrawable;
            if (mMessageItem.mGroupContact != null) {
                avatarDrawable = mMessageItem.mGroupContact.getAvatar(mContext, sDefaultContactImage, -1);
            } else {
                avatarDrawable = sDefaultContactImage;
            }
            mSenderPhoto.setImageDrawable(avatarDrawable);
            mSenderPhoto.setVisibility(View.VISIBLE);

            // mSenderPhoto.setClickable(false);
            String number = mMessageItem.mGroupContact.getNumber();
            if (Mms.isEmailAddress(number)) {
                mSenderPhoto.assignContactFromEmail(number, true);
            } else {
                if (mMessageItem.mGroupContact.existsInDatabase()) {
                    mSenderPhoto.assignContactUri(mMessageItem.mGroupContact.getUri());
                } else {
                    mSenderPhoto.assignContactFromPhone(number, true);
                }
            }
        }*/

        // This is a group conversation, show the sender's name on the same line as the timestamp.
        return timestamp;
    }

    //begin tangyisen
    public static final String DOWNLOAD_BTN_DOING = "#3D000000";
    public static final String DOWNLOAD_BTN_FAILED = "#FFDF5252";
    public static final String DOWNLOAD_BTN_NORMAL = "#FF77C926";
    private void showDownloadingAttachment() {
        //tangyisen
        /*inflateDownloadControls();
        mDownloadingLabel.setVisibility(View.VISIBLE);
        mDownloadButton.setVisibility(View.GONE);*/
        inflateDownloadControls();
        mDownloadView.setTextColor( Color.parseColor(DOWNLOAD_BTN_DOING) );
        mDownloadView.setText( R.string.mms_is_downloading);
        mDownloadView.setEnabled( false );
        //mOpMessageListItemExt.showDownloadingAttachment(mSelectedBox);
    }

    private void showDownloadFailedAttachment() {
        //tangyisen
        /*inflateDownloadControls();
        mDownloadingLabel.setVisibility(View.VISIBLE);
        mDownloadButton.setVisibility(View.GONE);*/
        inflateDownloadControls();
        mDownloadView.setTextColor( Color.parseColor(DOWNLOAD_BTN_FAILED) );
        mDownloadView.setText( R.string.download_failed_retry_mms);
        mDownloadView.setEnabled( true );
        //mOpMessageListItemExt.showDownloadingAttachment(mSelectedBox);
    }

    private void showDownloadClickAttachment() {
        //tangyisen
        /*inflateDownloadControls();
        mDownloadingLabel.setVisibility(View.VISIBLE);
        mDownloadButton.setVisibility(View.GONE);*/
        inflateDownloadControls();
        mDownloadView.setTextColor( Color.parseColor(DOWNLOAD_BTN_NORMAL) );
        mDownloadView.setText( R.string.click_to_download_mms);
        mDownloadView.setEnabled( true );
        //mOpMessageListItemExt.showDownloadingAttachment(mSelectedBox);
    }
    //end tangyisen

    private void updateAvatarView(String addr, boolean isSelf) {
        //tangyisen
        /*Drawable avatarDrawable;
        if (isSelf || !TextUtils.isEmpty(addr)) {
            Contact contact = isSelf ? Contact.getMe(false) : Contact.get(addr, false);
            avatarDrawable = contact.getAvatar(mContext, sDefaultContactImage, -1);

            if (isSelf) {
                mAvatar.assignContactUri(Profile.CONTENT_URI);
            } else {
                String number = contact.getNumber();
                if (Mms.isEmailAddress(number)) {
                    mAvatar.assignContactFromEmail(number, true);
                } else {
                    mAvatar.assignContactFromPhone(number, true);
                }
            }
        } else {
            avatarDrawable = sDefaultContactImage;
        }
        mAvatar.setImageDrawable(avatarDrawable);*/
    }

    private void bindCommonMessage() {
        //tangyisen
        updateParentBackground(mMessageItem);//add tangyisen
        /*if (mDownloadButton != null) {
            mDownloadButton.setVisibility(View.GONE);
            mDownloadingLabel.setVisibility(View.GONE);*/
            /// M: @{
        MessageUtils.hideView(mDownloadView);
        mBodyTextView.setVisibility(View.VISIBLE);
            /// @}
        //}

        /// M: only mms notifInd view will use and show this.
            //tangyisen
        /*if (mExpireText != null) {
            mExpireText.setVisibility(View.GONE);
        }*/

        // Since the message text should be concatenated with the sender's
        // address(or name), I have to display it here instead of
        // displaying it by the Presenter.
        mBodyTextView.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
        //tangyisen
        /*if (mAvatar != null) {
            if (mMessageItem.isSubMsg()) {
                mAvatar.setVisibility(View.VISIBLE);
                boolean isSelf = Sms.isOutgoingFolder(mMessageItem.mBoxId);
                String addr = isSelf ? null : mMessageItem.mAddress;
                updateAvatarView(addr, isSelf);
            } else {
                mAvatar.setVisibility(View.GONE);
            }
        }*/

        // Get and/or lazily set the formatted message from/on the
        // MessageItem.  Because the MessageItem instances come from a
        // cache (currently of size ~50), the hit rate on avoiding the
        // expensive formatMessage() call is very high.
        CharSequence formattedMessage = mMessageItem.getCachedFormattedMessage();
        /// M: @{
        // CharSequence formattedTimestamp = mMessageItem.getCachedFormattedTimestamp();
//        CharSequence formattedSubStatus = mMessageItem.getCachedFormattedSubStatus();
        /// @}

        if (formattedMessage == null) {
            formattedMessage = formatMessage(mMessageItem,
                                             mMessageItem.mBody,
                                             mMessageItem.mSubject,
                                             mMessageItem.mHighlight,
                                             mMessageItem.mTextContentType);
            /// M: @{
            // formattedTimestamp = formatTimestamp(mMessageItem, mMessageItem.mTimestamp);
//            formattedSubStatus = formatSubStatus(mMessageItem);
//            mMessageItem.setCachedFormattedSubStatus(formattedSubStatus);
            /// @}
            mMessageItem.setCachedFormattedMessage(formattedMessage);
        }
        /// M:
        if (TextUtils.isEmpty(mMessageItem.mBody) && TextUtils.isEmpty(mMessageItem.mSubject)) {
            mBodyTextView.setVisibility(View.GONE);
        } else {
            mBodyTextView.setVisibility(View.VISIBLE);
            //tangyisen
            mBodyTextView.setContent(formattedMessage, mMessageItem.isInboxMessage());
        }

        // Debugging code to put the URI of the image attachment in the body of the list item.
        if (DEBUG) {
            String debugText = null;
            if (mMessageItem.mSlideshow == null) {
                debugText = "NULL slideshow";
            } else {
                SlideModel slide = ((SlideshowModel) mMessageItem.mSlideshow).get(0);
                if (slide == null) {
                    debugText = "NULL first slide";
                } else if (!slide.hasImage()) {
                    debugText = "Not an image";
                } else {
                    debugText = slide.getImage().getUri().toString();
                }
            }
            //tangyisen
            mBodyTextView.setContent(mPosition + ": " + debugText, mMessageItem.isInboxMessage());
        }

        // If we're in the process of sending a message (i.e. pending), then we show a "SENDING..."
        // string in place of the timestamp.
        /// M: @{
        /*mDateView.setText(msgItem.isSending() ?
                mContext.getResources().getString(R.string.sending_message) :
                    buildTimestampLine(msgItem.mTimestamp));
        */
        //tangyisen it will show the date when failed send
        /*if (mMessageItem.isFailedMessage() || (!mMessageItem.isSending() && TextUtils.isEmpty(mMessageItem.mTimestamp))) {
            mDateView.setVisibility(View.GONE);
        } else {*/
            mDateView.setVisibility(View.VISIBLE);
            /// M: google jb.mr1 patch, group mms
            String dateStr = buildTimestampLine( mMessageItem.mTimestamp);/*mMessageItem.isSending()
                    ? mContext.getResources().getString(R.string.sending_message)
                    : buildTimestampLine(mMessageItem.mTimestamp);*/
            mDateView.setText(dateStr);
        //}
        /// @}

        /// M: @{
        //if (!mMessageItem.isSubMsg() && !TextUtils.isEmpty(formattedSubStatus)) {
        /*if (!mMessageItem.isSubMsg()) {
            MessageUtils.setSubIconAndLabel(mMessageItem.mSubId, null, mSubStatus);
        } else {
            mSubStatus.setVisibility(View.GONE);
        }*/
        /// @}
        //begin tangyisen
        if (mSendStatus != null && !isSmsGroup(mMessageItem)) {
            if (mMessageItem.isSending()) {
                mSendStatus.setVisibility(View.VISIBLE);
                mSendStatus.setText( R.string.message_item_is_sending );
            }else if(mSendStatus.getVisibility() == View.VISIBLE) {
                mSendStatus.setVisibility(View.GONE);
            }
        }
        //end tangyisen

        if (mMessageItem.isSms()) {
            showMmsView(false);
            mMessageItem.setOnPduLoaded(null);
            // add for vcard
            hideFileAttachmentViewIfNeeded();
        } else if (mMessageItem.isMms()){
            if (DEBUG) {
                Log.v(TAG, "bindCommonMessage for item: " + mPosition + " " +
                        mMessageItem.toString() +
                        " mMessageItem.mAttachmentType: " + mMessageItem.mAttachmentType);
            }
            //tangyisen
            //boolean isShowFileAttachmentView = false;
            mIsShowFileAttachmentView = false;
            mIsShowVcardView = false;
            if (mMessageItem.mAttachmentType != WorkingMessage.TEXT &&
                    mMessageItem.mAttachmentType != MessageItem.ATTACHMENT_TYPE_NOT_LOADED) {
                if (mMessageItem.mAttachmentType == WorkingMessage.ATTACHMENT) {
                    mIsShowFileAttachmentView = true;
                    ArrayList<FileAttachmentModel> files = mMessageItem.mSlideshow.getAttachFiles();
                    if(files.size() == 1) {
                        FileAttachmentModel attach = files.get(0);
                        if(attach.isVCard()) {
                            mIsShowVcardView = true;
                        } else {
                            mIsShowVcardView = false;
                        }
                    } else {
                        mIsShowVcardView = false;
                    }
                    showMmsView(true);
                    // show file attachment view
                    //showFileAttachmentView(mMessageItem.mSlideshow.getAttachFiles());
                    mImageView.setType(ZzzCircleImageView.VCARD_TYPE);
                    mImageView.setSrc( null );
                    MessageUtils.showView( mSlideShowButton );
                    //if(mMessageItem.isInboxMessage()) {
                    showVcardView(files, mImageView);
                    //}
                    updateSlideShowButton(mMessageItem);
                } else {
                    /// M: OP01 add for VCard and VCanlendar
                    /*if (!mOpMessageListItemExt.showOrHideFileAttachmentView(
                            mMessageItem.mSlideshow.getAttachFiles())) {*/
                        hideFileAttachmentViewIfNeeded();
                        MmsLog.i(TAG, "mMmsAttachmentEnhancePlugin= null");
                    //}
                    setImage(null, null);

                    drawPlaybackButton(mMessageItem);
                    //tangyisen
                    if (mSlideShowButton.getVisibility() == View.GONE) {
                        setMediaOnClickListener(mMessageItem);
                    }
                }
            } else {
                showMmsView(false);
                /// M:  add for VCard and VCanlendar
                hideFileAttachmentViewIfNeeded();
            }
            if (mMessageItem.mSlideshow == null && !mIsShowFileAttachmentView) {
                mMessageItem.setOnPduLoaded(new MessageItem.PduLoadedCallback() {
                    public void onPduLoaded(MessageItem messageItem) {
                        if (DEBUG) {
                            Log.v(TAG, "PduLoadedCallback in MessageListItem for item: " + mPosition +
                                    " " + (mMessageItem == null ? "NULL" : mMessageItem.toString()) +
                                    " passed in item: " +
                                    (messageItem == null ? "NULL" : messageItem.toString()));
                        }
                        if (messageItem != null && mMessageItem != null &&
                                messageItem.getMessageId() == mMessageItem.getMessageId()) {
                            mMessageItem.setCachedFormattedMessage(null);
                            bindCommonMessage();
                        }
                    }
                });
            } else {
                if (mPresenter == null) {
                    mPresenter = PresenterFactory.getPresenter(
                    "MmsThumbnailPresenter", mContext,
                    this, mMessageItem.mSlideshow);
                } else {
                    mPresenter.setModel(mMessageItem.mSlideshow);
                    mPresenter.setView(this);
                }
                if (mImageLoadedCallback == null) {
                    mImageLoadedCallback = new ImageLoadedCallback(this);
                } else {
                    mImageLoadedCallback.reset(this);
                }
                mPresenter.present(mImageLoadedCallback);
            }
        }
        //drawRightStatusIndicator(mMessageItem);
        //begin tangyisen
        updateSendStatus(mMessageItem);
        updateGroupState(mMessageItem);
        //end tangyisen

        requestLayout();

        //mOpMessageListItemExt.bindCommonMessage(mDownloadButton, mDateView);

    }

    static private class ImageLoadedCallback implements ItemLoadedCallback<ImageLoaded> {
        private long mMessageId;
        private final MessageListItem mListItem;

        public ImageLoadedCallback(MessageListItem listItem) {
            mListItem = listItem;
            mMessageId = listItem.getMessageItem().getMessageId();
        }

        public void reset(MessageListItem listItem) {
            mMessageId = listItem.getMessageItem().getMessageId();
        }

        public void onItemLoaded(ImageLoaded imageLoaded, Throwable exception) {
            if (DEBUG_DONT_LOAD_IMAGES) {
                return;
            }
            // Make sure we're still pointing to the same message. The list item could have
            // been recycled.
            MessageItem msgItem = mListItem.mMessageItem;
            if (msgItem != null && msgItem.getMessageId() == mMessageId) {
                if (imageLoaded.mIsVideo) {
                    mListItem.setVideoThumbnail(null, imageLoaded.mBitmap);
                } else {
                    mListItem.setImage(null, imageLoaded.mBitmap);
                }
            }
        }
    }

    @Override
    public void startAudio() {
        // TODO Auto-generated method stub
    }

    @Override
    public void startVideo() {
        // TODO Auto-generated method stub
    }

    @Override
    public void setAudio(Uri audio, String name, Map<String, ?> extras) {
        // TODO Auto-generated method stub
        mImageView.setType(ZzzCircleImageView.AUDIO_TYPE);
        mImageView.setSrc( null );
        MessageUtils.showView( mSlideShowButton );
        if (mMessageItem.mAttachmentType == WorkingMessage.SLIDESHOW) {
            mSlideShowButton.setImageResource(R.drawable.zzz_ic_message_attachment_edit_voice);
        }
    }

    @Override
    public void setImage(String name, Bitmap bitmap) {
        showMmsView(true);
        if(bitmap == null) {
            return; 
        }
        try {
            /*mImageView.setImageBitmap(bitmap);
            mImageView.setVisibility(VISIBLE);*/
            mImageView.setType(ZzzCircleImageView.IMAGE_TYPE);
            mImageView.setSrc(bitmap);
            mImageView.setVisibility(VISIBLE);
            MessageUtils.hideView( mSlideShowButton );
            setMediaOnClickListener(mMessageItem);//add by tangyisen
            /// M:
            // add for ipmessage
            //mIpMessageListItem.onIpSetMmsImage();
        } catch (java.lang.OutOfMemoryError e) {
            Log.e(TAG, "setImage: out of memory: ", e);
            MessageUtils.writeHprofDataToFile();
        }
    }

    private void showMmsView(boolean visible) {
        if (mMmsView == null) {
            mMmsView = findView(mms_view);
            // if mMmsView is still null here, that mean the mms section hasn't been inflated

            if (visible && mMmsView == null) {
                //inflate the mms view_stub
                View mmsStub = findView(mms_layout_view_stub);
                mmsStub.setVisibility(View.VISIBLE);
                mMmsView = findView(mms_view);
            }
            if(mMmsView != null) {
                mMmsView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View arg0) {
                        //Log.d(TAG, "mMmsView onLongClick");
                        sendMessage(mMessageItem, MSG_LIST_SHOW_EDIT);
                        return true;
                    }
                });
            }
        }
        if (mMmsView != null) {
            if (mImageView == null) {
                mImageView = (ZzzCircleImageView) findView(image_view);
                mImageView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View arg0) {
                        //Log.d(TAG, "mImageView onLongClick");
                        sendMessage(mMessageItem, MSG_LIST_SHOW_EDIT);
                        return true;
                    }
                });
            }
            //tangyisen
            if (mSlideShowButton == null) {
                mSlideShowButton = (ImageView) findView(play_slideshow_button);
                if(null != mSlideShowButton) {
                    mSlideShowButton.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View arg0) {
                            //Log.d(TAG, "mSlideShowButton onLongClick");
                            sendMessage(mMessageItem, MSG_LIST_SHOW_EDIT);
                            return true;
                        }
                    });
                }
            }
            mMmsView.setVisibility(visible ? View.VISIBLE : View.GONE);
            mImageView.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

      private void inflateDownloadControls() {
        //tangyisen
        /*if (mDownloadButton == null) {
            //inflate the download controls
            findView(mms_downloading_view_stub).setVisibility(VISIBLE);
            mDownloadButton = (Button) findView(btn_download_msg);
            mDownloadingLabel = (TextView) findView(label_downloading);
            mOpMessageListItemExt.inflateDownloadControls(
                    ComposeMessageActivity.getComposeContext(),
                    (LinearLayout) findView(mms_download_controls),
                    (TextView) findView(text_expire));
        }*/
         if(null == mDownloadView) {
             mDownloadView = (TextView)findView(download_view);
         }
         mDownloadView.setVisibility( View.VISIBLE );
    }

    private LineHeightSpan mSpan = new LineHeightSpan() {
        @Override
        public void chooseHeight(CharSequence text, int start,
                int end, int spanstartv, int v, FontMetricsInt fm) {
            fm.ascent -= 10;
        }
    };

    TextAppearanceSpan mTextSmallSpan =
        new TextAppearanceSpan(mContext, android.R.style.TextAppearance_Small);

    ForegroundColorSpan mColorSpan = null;  // set in ctor

    private CharSequence formatMessage(MessageItem msgItem, String body,
                                       String subject, Pattern highlight,
                                       String contentType) {
        SpannableStringBuilder buf = new SpannableStringBuilder();

        boolean hasSubject = !TextUtils.isEmpty(subject);
        if (hasSubject) {
            buf.append(mContext.getResources().getString(R.string.zzz_inline_subject, subject));
        }

        if (!TextUtils.isEmpty(body)) {
            // Converts html to spannable if MmsContentType is "text/html".
            if (contentType != null && MmsContentType.TEXT_HTML.equals(contentType)) {
                buf.append("\n");
                buf.append(Html.fromHtml(body));
            } else {
                if (hasSubject) {
                    buf.append(" - ");
                }
                buf.append(body);
            }
        }

        // add for ipmessage
        buf = new SpannableStringBuilder(IpMessageUtils.formatIpMessage(body, true, buf));

        //buf = mOpMessageListItemExt.formatMessage(msgItem.mOpMessageItemExt, buf, body, hasSubject);

        //lichao delete in 2017-05-12 for design
        /*if (highlight != null) {
            Matcher m = highlight.matcher(buf.toString());
            while (m.find()) {
                buf.setSpan(new StyleSpan(Typeface.BOLD), m.start(), m.end(), 0);
            }
        }*/
        /// M: @{
        buf.setSpan(mLeadingMarginSpan, 0, buf.length(), 0);
        /// @}
        return buf;
    }

    private void drawPlaybackButton(MessageItem msgItem) {
        switch (msgItem.mAttachmentType) {
            case WorkingMessage.SLIDESHOW:
            case WorkingMessage.AUDIO:
            case WorkingMessage.VIDEO:
                updateSlideShowButton(msgItem);
                break;
            case WorkingMessage.IMAGE:
                //tangyisen add
                /*if (msgItem.mSlideshow.get(0).hasText()) {
                    MmsLog.d(TAG, "msgItem is image and text");
                    updateSlideShowButton(msgItem);
                }
                 else {*/
                    mSlideShowButton.setVisibility(View.GONE);
                //}
                break;
            default:
                mSlideShowButton.setVisibility(View.GONE);
                break;
        }
    }

    private void updateSlideShowButton(MessageItem msgItem) {
        // Show the 'Play' button and bind message info on it.
        //tangyisen
        mSlideShowButton.setTag(msgItem);
        /// M: @{
        mSlideShowButton.setVisibility(View.GONE);

        /*Bitmap drmBitmap = DrmUtilsEx.getDrmBitmapWithLockIcon(mContext,
                msgItem, R.drawable.mms_play_btn,
                MessageResource.drawable.drm_red_lock);
        if (drmBitmap != null) {
            mSlideShowButton.setImageBitmap(drmBitmap);
        } else {
            Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                                R.drawable.mms_play_btn);
            mSlideShowButton.setImageBitmap(bitmap);
        }*/
        if (mIsShowFileAttachmentView) {
            if(mIsShowVcardView) {
                mSlideShowButton.setImageResource(R.drawable.zzz_ic_message_attchment_edit_vcard);
            } else {
                mSlideShowButton.setImageResource(R.drawable.zzz_ic_message_attchment_edit_common);
            }
        } else if (msgItem.mAttachmentType == WorkingMessage.VIDEO) {
            mSlideShowButton.setImageResource(R.drawable.zzz_ic_message_attachment_edit_video);
        } else if (msgItem.mAttachmentType == WorkingMessage.AUDIO) {
            mSlideShowButton.setImageResource(R.drawable.zzz_ic_message_attachment_edit_voice);
        }
        /// @}

        // Set call-back for the 'Play' button.
        mSlideShowButton.setOnClickListener(this);
        mSlideShowButton.setVisibility(View.VISIBLE);
        //setLongClickable(true);

        // When we show the mSlideShowButton, this list item's onItemClickListener doesn't
        // get called. (It gets set in ComposeMessageActivity:
        // mMsgListView.setOnItemClickListener) Here we explicitly set the item's
        // onClickListener. It allows the item to respond to embedded html links and at the
        // same time, allows the slide show play button to work.
        /*setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onMessageListItemClick();
            }
        });*/
    }

    // OnClick Listener for the playback button
    @Override
    public void onClick(View v) {
        /// M: add for multi-delete @{
        if (mSelectedBox != null && mSelectedBox.getVisibility() == View.VISIBLE) {
            return;
        }
        if (!sImageButtonCanClick) {
            return;
        }
        sImageButtonCanClick = false;
        //begin tangyisen
        if (mIsShowFileAttachmentView) {
            ArrayList<FileAttachmentModel> files = mMessageItem.mSlideshow.getAttachFiles();
            if(files.size() == 1) {
                if(mMessageItem.isInboxMessage()) {
                    clickVcardView(files);
                }
            } else {
                sendMessage(mMessageItem, MSG_LIST_PLAY);
            }
        } else {
        //end tangyisen
        /// @}
            sendMessage(mMessageItem, MSG_LIST_PLAY);
        }
        /// M:
        if (mHandler != null) {
            Runnable run = new Runnable() {
                public void run() {
                    sImageButtonCanClick = true;
                }
            };
            mHandler.postDelayed(run, 200);//tangyisen, 1000 to long ,set 200
        }
    }

    private void sendMessage(MessageItem messageItem, int message) {
        //begin tangyisen fix monkey bugs
        if(messageItem == null) {
            Log.e(TAG, "sendMessage messageItem is null! ");
            return;
        }
        //end tangyisen fix monkey bugs
        if (mHandler != null) {
            Message msg = Message.obtain(mHandler, message);
            //begin tangyisen
            if(messageItem.mAttachmentType == WorkingMessage.AUDIO) {
                if(mSlideShowButton != null) {
                    messageItem.setAudioView(mSlideShowButton);
                }
            }
            //end tangyisen
            msg.obj = messageItem;
            msg.sendToTarget(); // See ComposeMessageActivity.mMessageListItemHandler.handleMessage
        }
    }

    public void onMessageListItemClick() {
        if (mMessageItem == null) {
            MmsLog.e(TAG, "onMessageListItemClick(): Message Item is null!");
            return;
        }
        /*if (mIpMessageListItem.onIpMessageListItemClick()) {
            return;
        }*/
        /// M: add for multi-delete @{
        //tangyisen delete
        /*if (mSelectedBox != null && mSelectedBox.getVisibility() == View.VISIBLE) {
            if (!mSelectedBox.isChecked()) {
                setSelectedBackGroud(true);
            } else {
                setSelectedBackGroud(false);
            }
            if (null != mHandler) {
                Message msg = Message.obtain(mHandler, ITEM_CLICK);
                msg.arg1 = (int) MessageListAdapter.getKey(mMessageItem.mType, mMessageItem.mMsgId);
                msg.arg2 = mMessageItem.mLocked ?
                    MultiDeleteActivity.MESSAGE_STATUS_LOCK : MultiDeleteActivity.MESSAGE_STATUS_NOT_LOCK;
                msg.obj = mMessageItem;
                msg.sendToTarget();
            }
            return;
        }*/
        /// @}
        /*if (mOpMessageListItemExt.onMessageListItemClick()) {
            return;
        }*/

        // If the message is a failed one, clicking it should reload it in the compose view,
        // regardless of whether it has links in it
        if (mMessageItem != null &&
                ((mMessageItem.isOutgoingMessage() &&
                mMessageItem.isFailedMessage()) ||
                mMessageItem.mDeliveryStatus == MessageItem.DeliveryStatus.FAILED)) {
            // Assuming the current message is a failed one, reload it into the
            // compose view so
            // the user can resend it.
            sendMessage(mMessageItem, MSG_LIST_EDIT);
            return;
        }

        // Check for links. If none, do nothing; if 1, open it; if >1, ask user to pick one
        final URLSpan[] spans = mBodyTextView.getUrls();
        /// M: @{
        final java.util.ArrayList<String> urls = MessageUtils.extractUris(spans);
        final String telPrefix = "tel:";
        String url = "";
        boolean isTel = false;
        /// M: fix bug ALPS00367589, uri_size sync according to urls after filter to unique array @{
        for (int i = 0; i < urls.size(); i++) {
            url = urls.get(i);
            if (url.startsWith(telPrefix)) {
                isTel = true;
                if (MmsConfig.isSmsEnabled(mContext)) {
                    urls.add("smsto:" + url.substring(telPrefix.length()));
                }
            }
        }
        /// @}
        if (spans.length == 0) {
            sendMessage(mMessageItem, MSG_LIST_DETAILS);    // show the message details dialog
        /// M: @{
        //} else if (spans.length == 1) {
        } else if (spans.length == 1 && !isTel) {
        /// @}
            /*
            Uri uri = Uri.parse(spans[0].getURL());
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.putExtra(Browser.EXTRA_APPLICATION_ID, mContext.getPackageName());
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            mContext.startActivity(intent);
            */
            //mOpMessageListItemExt.openUrl(mContext, spans[0].getURL());
        /** M: delete google default code @{
        } else if (spans.length == 1) {
            spans[0].onClick(mBodyTextView);
        */
        } else {
            /// M: modify google default Code.@{
            // ArrayAdapter<URLSpan> adapter =
            //      new ArrayAdapter<URLSpan>(mContext, android.R.layout.select_dialog_item, spans) {

            ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(mContext, android.R.layout.select_dialog_item, urls) {
              /// @}
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View v = super.getView(position, convertView, parent);
                    TextView tv = (TextView) v;
                    /// M: move this try into the exact place
                    //try {
                        /// M: modify google default Code @{
                        // URLSpan span = getItem(position);
                        // String url = span.getURL();
                        String url = getItem(position).toString();
                        /// @}
                        Uri uri = Uri.parse(url);

                        final String telPrefix = "tel:";
                        /// M: use default icon to display
                        Drawable d = parseAppIcon(mContext, url);
                        if (d != null) {
                            d.setBounds(0, 0, d.getIntrinsicHeight(), d.getIntrinsicHeight());
                            tv.setCompoundDrawablePadding(10);
                            tv.setCompoundDrawables(d, null, null, null);
                        } else {
                            /// M: currently we only know this one
                            if (url.startsWith(telPrefix)) {
                                d = mContext.getResources().getDrawable(R.drawable.ic_launcher_phone);
                                d.setBounds(0, 0, d.getIntrinsicHeight(), d.getIntrinsicHeight());
                                tv.setCompoundDrawablePadding(10);
                                tv.setCompoundDrawables(d, null, null, null);
                            } else {
                                tv.setCompoundDrawables(null, null, null, null);
                            }
                        }

                        /// M: @{
                        final String smsPrefix = "smsto:";
                        final String mailPrefix = "mailto";
                        /// @}
                        if (url.startsWith(telPrefix)) {
                            url = PhoneNumberUtils.formatNumber(
                                            url.substring(telPrefix.length()), mDefaultCountryIso);
                            if (url == null) {
                                MmsLog.w(TAG, "url turn to null after calling PhoneNumberUtils.formatNumber");
                                url = getItem(position).toString().substring(telPrefix.length());
                            }
                        } else if (url.startsWith(smsPrefix)) { /// M: @{
                            url = PhoneNumberUtils.formatNumber(
                                            url.substring(smsPrefix.length()), mDefaultCountryIso);
                            if (url == null) {
                                MmsLog.w(TAG, "url turn to null after calling PhoneNumberUtils.formatNumber");
                                url = getItem(position).toString().substring(smsPrefix.length());
                            }
                        } else if (url.startsWith(mailPrefix)) {
                            String uu = url.substring(mailPrefix.length() + 1, url.length());
                            uu = Uri.encode(uu);
                            uu = mailPrefix + ":" + uu;
                            MailTo mt = MailTo.parse(uu);
                            url = mt.getTo();
                        }
                        /// @}
                        tv.setText(url);
                    /// M: move this catch to the exact place
                    //} catch (android.content.pm.PackageManager.NameNotFoundException ex) {
                        // it's ok if we're unable to set the drawable for this view - the user
                        // can still use it
                    //    tv.setCompoundDrawables(null, null, null, null);
                    //    return v;
                    //}
                    return v;
                }
            };

            AlertDialog.Builder b = new AlertDialog.Builder(mContext);

            DialogInterface.OnClickListener click = new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialog, int which) {
                    if (which >= 0) {
                        /// M: change google default action to choose how to response click  @{
                        //spans[which].onClick(mBodyTextView);

                        Uri uri = Uri.parse(urls.get(which));
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        intent.putExtra(Browser.EXTRA_APPLICATION_ID, mContext.getPackageName());
                        if (urls.get(which).startsWith("smsto:")) {
                            intent.setClassName(mContext, "com.android.mms.ui.SendMessageToActivity");
                        }
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                        mContext.startActivity(intent);
                        if (urls.get(which).startsWith("smsto:")) {
                            intent.setClassName(mContext, "com.android.mms.ui.SendMessageToActivity");
                        }
                        /// @}

                    }
                    dialog.dismiss();
                }
            };

            b.setTitle(R.string.select_link_title);
            b.setCancelable(true);
            b.setAdapter(adapter, click);

            b.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            b.show();
        }
    }

    public void showMessageDetail() {
        if (mHandler != null) {
            Message msg = Message.obtain(mHandler, MSG_LIST_SHOW_MSGITEM_DETAIL);
            msg.obj = mMessageItem;
            msg.sendToTarget(); // See
                                // ComposeMessageActivity.mMessageListItemHandler.handleMessage
        }
    }

   private void setOnClickListener(final MessageItem msgItem) {
        switch(msgItem.mAttachmentType) {
            case WorkingMessage.IMAGE:
            case WorkingMessage.VIDEO:
                mImageView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendMessage(msgItem, MSG_LIST_PLAY);
                    }
                });
                //tangyisen delete
                /*mImageView.setOnLongClickListener(new OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        return v.showContextMenu();
                    }
                });*/
                break;

            default:
                mImageView.setOnClickListener(null);
                break;
            }
    }

    private void setMediaOnClickListener(final MessageItem msgItem) {
        switch(msgItem.mAttachmentType) {
        case WorkingMessage.IMAGE:
        case WorkingMessage.VIDEO:
        case WorkingMessage.SLIDESHOW://tangyisen add
            mImageView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    /// M: @{
                    //add for multi-delete
                   //tangyisen delete
                   /* if (mSelectedBox != null && mSelectedBox.getVisibility() == View.VISIBLE) {
                        mSelectedBox.setChecked(!mSelectedBox.isChecked());

                        if (mSelectedBox.isChecked()) {
                            setSelectedBackGroud(true);
                        } else {
                            setSelectedBackGroud(false);
                        }

                        if (null != mHandler) {
                            Message msg = Message.obtain(mHandler, ITEM_CLICK);
                            msg.arg1 = (int) (mMessageItem.mType.equals("mms") ? -mMessageItem.mMsgId : mMessageItem.mMsgId);
                            msg.arg2 = mMessageItem.mLocked ? MultiDeleteActivity.MESSAGE_STATUS_LOCK
                                        : MultiDeleteActivity.MESSAGE_STATUS_NOT_LOCK;
                            msg.obj = mMessageItem;
                            msg.sendToTarget();
                        }
                        return;
                    }*/
                    /// @}
                    if (!sImageButtonCanClick) {
                        return;
                    }
                    sImageButtonCanClick = false;
                    /// M: @{
                    /*if (msgItem.mAttachmentType == WorkingMessage.IMAGE && msgItem.mSlideshow.get(0).hasText()) {
                        mImageView.setOnClickListener(null);
                    } else {*///tangyisen delete
                        sendMessage(msgItem, MSG_LIST_PLAY);
                    //}
                    if (mHandler != null) {
                        Runnable run = new Runnable() {
                            public void run() {
                                sImageButtonCanClick = true;
                            }
                        };
                        mHandler.postDelayed(run, 200);//tangyisen 1000 too long,set 200
                    }
                    /// @}
                }
            });
            //tangyisen delete
            /*mImageView.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    return v.showContextMenu();
                }
            });*/
            break;

        default:
            mImageView.setOnClickListener(null);
            break;
        }
    }

    private void drawRightStatusIndicator(MessageItem msgItem) {
        // Locked icon
        //tangyisen
        /*if (msgItem.mLocked) {
            mLockedIndicator.setImageDrawable(mContext.getResources().getDrawable(
                    R.drawable.ic_lock_message_sms));
            mLockedIndicator.setVisibility(View.VISIBLE);
            mSubStatus.setPadding(PADDING_LEFT_THR, 0, 0, 0);
        } else {
            mLockedIndicator.setVisibility(View.GONE);
            mSubStatus.setPadding(PADDING_LEFT_TWE, 0, 0, 0);
        }*/
        //tangyisen
        /*if ((msgItem.isOutgoingMessage() && msgItem.isFailedMessage())
                ||
                msgItem.mDeliveryStatus == MessageItem.DeliveryStatus.FAILED) {
            mDeliveredIndicator.setImageDrawable(mContext.getResources().getDrawable(
                    R.drawable.ic_list_alert_sms_failed));
            mDeliveredIndicator.setVisibility(View.VISIBLE);
        } else if (msgItem.isSms() &&
                msgItem.mDeliveryStatus == MessageItem.DeliveryStatus.RECEIVED) {
            /// M: @{
            mDeliveredIndicator.setClickable(false);
            /// @}
            ///M: modified for new icon
//            mDeliveredIndicator.setImageResource(R.drawable.ic_sms_mms_delivered);
            mDeliveredIndicator.setImageDrawable(mContext.getResources().getDrawable(
                    R.drawable.im_meg_status_reach));
            mDeliveredIndicator.setVisibility(View.VISIBLE);
        } else {
            /// M: Add new status icon for MMS or SMS. @{
            int resId = MessageUtils.getStatusResourceId(mContext, msgItem);
            if (resId > 0) {
                mDeliveredIndicator.setClickable(false);
                mDeliveredIndicator.setImageDrawable(mContext.getResources().getDrawable(resId));
//                mDeliveredIndicator.setImageResource(resId);
                mDeliveredIndicator.setVisibility(View.VISIBLE);
            } else {
                mDeliveredIndicator.setVisibility(View.GONE);
            }
            /// @}
        }*/

        // Message details icon - this icon is shown both for sms and mms messages. For mms,
        // we show the icon if the read report or delivery report setting was set when the
        // message was sent. Showing the icon tells the user there's more information
        // by selecting the "View report" menu.
        //tangyisen delete
        /*if (msgItem.mDeliveryStatus == MessageItem.DeliveryStatus.INFO || msgItem.mReadReport
                || (msgItem.isMms() &&
                        msgItem.mDeliveryStatus == MessageItem.DeliveryStatus.RECEIVED)) {
            mDetailsIndicator.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_sms_mms_details));
            mDetailsIndicator.setVisibility(View.VISIBLE);
        } else {
            mDetailsIndicator.setVisibility(View.GONE);
        }*/

        /*mOpMessageListItemExt.drawRightStatusIndicator(this.getContext(),
                (LinearLayout) findView(status_panel), msgItem.mOpMessageItemExt, mSubStatus,
                (TextView) findView(send_time_txt), (LinearLayout) findView(double_time_layout),
                buildTimestampLine(msgItem.mTimestamp));*/
        /*mIpMessageListItem.drawRightStatusIndicator(mContext, msgItem.mIpMessageItem,
                                            mDeliveredIndicator, mDetailsIndicator);*/
    }

    @Override
    public void setImageRegionFit(String fit) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setImageVisibility(boolean visible) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setText(String name, String text) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setTextVisibility(boolean visible) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setVideo(String name, Uri uri) {
    }

    @Override
    public void setVideoThumbnail(String name, Bitmap bitmap) {
        showMmsView(true);

        try {
            mImageView.setType(ZzzCircleImageView.VIDEO_TYPE);
            mImageView.setImageBitmap(bitmap);
            mImageView.setVisibility(VISIBLE);
            MessageUtils.showView( mSlideShowButton );
            if (mMessageItem.mAttachmentType == WorkingMessage.SLIDESHOW) {
                mSlideShowButton.setImageResource(R.drawable.zzz_ic_message_attachment_edit_video);
            }
        } catch (java.lang.OutOfMemoryError e) {
            Log.e(TAG, "setVideo: out of memory: ", e);
        }
    }

    @Override
    public void setVideoVisibility(boolean visible) {
        // TODO Auto-generated method stub
    }

    @Override
    public void stopAudio() {
        // TODO Auto-generated method stub
    }

    @Override
    public void stopVideo() {
        // TODO Auto-generated method stub
    }

    @Override
    public void reset() {
    }

    @Override
    public void setVisibility(boolean visible) {
        // TODO Auto-generated method stub
    }

    @Override
    public void pauseAudio() {
        // TODO Auto-generated method stub

    }

    @Override
    public void pauseVideo() {
        // TODO Auto-generated method stub

    }

    @Override
    public void seekAudio(int seekTo) {
        // TODO Auto-generated method stub

    }

    @Override
    public void seekVideo(int seekTo) {
        // TODO Auto-generated method stub

    }

    /**
     * Override dispatchDraw so that we can put our own background and border in.
     * This is all complexity to support a shared border from one item to the next.
     */
    /*** M: remove Google default code
    @Override
    public void dispatchDraw(Canvas c) {
        super.dispatchDraw(c);

        // This custom border is causing our scrolling fps to drop from 60+ to the mid 40's.
        // Commenting out for now until we come up with a new UI design that doesn't require
        // the border.
        return;

//        View v = mMessageBlock;
//        int selectBoxWidth = 0;
//        if (mSelectedBox != null && mSelectedBox.getVisibility() == View.VISIBLE) {
//            selectBoxWidth = mSelectedBox.getWidth();
//        }
//        if (v != null) {
//            float l = v.getX() + selectBoxWidth;
//            float t = v.getY();
//            float r = v.getX() + v.getWidth() + selectBoxWidth;
//            float b = v.getY() + v.getHeight();
//
//            Path path = mPath;
//            path.reset();
//
//            super.dispatchDraw(c);
//
//            path.reset();
//
//            r -= 1;
//
//            // This block of code draws the border around the "message block" section
//            // of the layout.  This would normally be a simple rectangle but we omit
//            // the border at the point of the avatar's divot.  Also, the bottom is drawn
//            // 1 pixel below our own bounds to get it to line up with the border of
//            // the next item.
//            //
//            // But for the last item we draw the bottom in our own bounds -- so it will
//            // show up.
//            if (mIsLastItemInList) {
//                b -= 1;
//            }
//            if (mAvatar.getPosition() == Divot.RIGHT_UPPER) {
//                path.moveTo(l, t + mAvatar.getCloseOffset());
//                path.lineTo(l, t);
//                if (selectBoxWidth > 0) {
//                    path.lineTo(l - mAvatar.getWidth() - selectBoxWidth, t);
//                }
//                path.lineTo(r, t);
//                path.lineTo(r, b);
//                path.lineTo(l, b);
//                path.lineTo(l, t + mAvatar.getFarOffset());
//            } else if (mAvatar.getPosition() == Divot.LEFT_UPPER) {
//                path.moveTo(r, t + mAvatar.getCloseOffset());
//                path.lineTo(r, t);
//                path.lineTo(l - selectBoxWidth, t);
//                path.lineTo(l - selectBoxWidth, b);
//                path.lineTo(r, b);
//                path.lineTo(r, t + mAvatar.getFarOffset());
//            }
//
//            Paint paint = mPaint;
////            paint.setColor(0xff00ff00);
//            paint.setColor(0xffcccccc);
//            paint.setStrokeWidth(1F);
//            paint.setStyle(Paint.Style.STROKE);
//            c.drawPath(path, paint);
//        } else {
//            super.dispatchDraw(c);
//        }
    }
*/

    /// M: @{
    public static final int ITEM_CLICK          = 5;
    static final int ITEM_MARGIN         = 50;
    //tangyisen
    //private TextView mSubStatus;
    public CheckBox mSelectedBox;

    private CharSequence formatTimestamp(MessageItem msgItem, String timestamp) {
        SpannableStringBuilder buf = new SpannableStringBuilder();
        if (msgItem.isSending()) {
            timestamp = mContext.getResources().getString(R.string.sending_message);
        }

           buf.append(TextUtils.isEmpty(timestamp) ? " " : timestamp);
           buf.setSpan(mSpan, 1, buf.length(), 0);

        //buf.setSpan(mTextSmallSpan, 0, buf.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        // Make the timestamp text not as dark
        buf.setSpan(mColorSpan, 0, buf.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return buf;
    }

//    private CharSequence formatSubStatus(MessageItem msgItem) {
//        SpannableStringBuilder buffer = new SpannableStringBuilder();
//        // If we're in the process of sending a message (i.e. pending), then we show a "Sending..."
//        // string in place of the timestamp.
//        //Add sub info
//        CharSequence subInfo = MessageUtils.getSubInfo(mContext, msgItem.mSubId);
//        Log.i(TAG,"formatSubStatus::subInfo="+subInfo);
//        if (subInfo.length() > 0) {
//            buffer.append(subInfo);
//        }
//        int subInfoStart = buffer.length();
//
//        //buffer.setSpan(mTextSmallSpan, 0, buffer.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//        // Make the timestamp text not as dark
//        buffer.setSpan(mColorSpan, 0, subInfoStart, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//
//        return buffer;
//    }

    public void setSelectedBackGroud(boolean selected) {
        if (selected) {
            mSelectedBox.setChecked(true);
//            mSelectedBox.setBackgroundDrawable(null);
//            mMessageBlock.setBackgroundDrawable(null);
//            mDateView.setBackgroundDrawable(null);
        } else {
              mSelectedBox.setChecked(false);
//            mSelectedBox.setBackgroundResource(R.drawable.listitem_background);
//            mMessageBlock.setBackgroundResource(R.drawable.listitem_background);
//            mDateView.setBackgroundResource(R.drawable.listitem_background);
        }
        setBackgroundDrawable(null);
    }

    public void bindDefault(MessageItem msgItem, boolean isLastItem) {
        MmsLog.d(M_TAG, "bindDefault()");
        //begin tangyisen
        MessageUtils.hideView(mDownloadView);
        //end tangyisen
        mIsLastItemInList = isLastItem;
        mSelectedBox.setVisibility(View.GONE);
        setLongClickable(false);
        setFocusable(false);
        setClickable(false);
        String msgType = "";
        if (msgItem != null) {
            msgType = msgItem.mType;
        }
        //begin tangyisen add
        updateParentBackground(msgItem);
        //end tangyisen
        if (mMmsView != null) {
            mMmsView.setVisibility(View.GONE);
        }
        if (mFileAttachmentView != null) {
            mFileAttachmentView.setVisibility(View.GONE);
        }
        /// M: fix bug ALPS00435619, show Refreshing string
        /// when we fail to reload item and put in cache during 500 ms
        mBodyTextView.setVisibility(View.VISIBLE);
        // M:for ALPS01065027,just for compose sms messagelist use in scrolling
        if ("sms".equals(msgType)) {
            CharSequence formattedMessage = formatMessage(msgItem, msgItem.mBody, msgItem.mSubject,
                    msgItem.mHighlight, msgItem.mTextContentType);
            msgItem.setCachedFormattedMessage(formattedMessage);

            if (TextUtils.isEmpty(msgItem.mBody) && TextUtils.isEmpty(msgItem.mSubject)) {
                mBodyTextView.setVisibility(View.GONE);
            } else {
                //tangyisen
                mBodyTextView.setContent(formattedMessage, msgItem.isInboxMessage());
            }
        } else {
            //tangyisen
            if(msgItem != null) {
                mBodyTextView.setContent(this.getContext().getString(R.string.refreshing), msgItem.isInboxMessage());
            } else {
                mBodyTextView.setContent(this.getContext().getString(R.string.refreshing), false);
            }
        }

        // if (null != mTimeDivider) {
        // mTimeDivider.setVisibility(View.GONE);
        // }
        //

        // mDateView.setVisibility(View.GONE);
        // mSimStatus.setVisibility(View.GONE);
        //tangyisen
        /*if (mDownloadButton != null) {
            mDownloadingLabel.setVisibility(View.GONE);
            mDownloadButton.setVisibility(View.GONE);
        }*/

        //mOpMessageListItemExt.bindDefault(mDownloadButton);

        //tangyisen
        //mLockedIndicator.setVisibility(View.GONE);
        //mSubStatus.setPadding(PADDING_LEFT_TWE, 0, 0, 0);
//        CharSequence formattedSubStatus = "";
        if (msgItem != null) {
            MmsLog.e(TAG, "message item is not null");
//            formattedSubStatus = msgItem.getCachedFormattedSubStatus();
            //if (!msgItem.isSubMsg() && !TextUtils.isEmpty(formattedSubStatus)) {
            if (!msgItem.isSubMsg() && mMessageItem != null) {
                //MessageUtils.setSubIconAndLabel(mMessageItem.mSubId, null, mSubStatus);
            }
        } else {
            MmsLog.e(TAG, "message item is null");
        }
        // mDeliveredIndicator.setVisibility(View.GONE);
        //tangyisen
        //mDetailsIndicator.setVisibility(View.GONE);
        /// M: fix bug ALPS00432063, check NPE
        //tangyisen
        /*if (mAvatar != null && mAvatar.getVisibility() == View.VISIBLE) {
            mAvatar.setImageDrawable(sDefaultContactImage);
        }*/

        /// M: fix bug ALPS00439894, MTK MR1 new feature: Group Mms
        /// set Gone in bindDefault()
        //tangyisen
        /*if (mMessageItem != null && !mMessageItem.isMe()) {
            mSenderName = (TextView) findView(sender_name);
            mSendNameSeparator = findView(sender_name_separator);
            mSenderPhoto = (QuickContactBadge) findView(sender_photo);
            if (mSenderName != null && mSenderPhoto != null && mSendNameSeparator != null) {
                mSenderName.setVisibility(View.GONE);
                mSendNameSeparator.setVisibility(View.GONE);
                mSenderPhoto.setVisibility(View.GONE);
            }
        }
        if (mOnLineDivider != null) {
            mOnLineDivider.setVisibility(View.GONE);
        }*/
        /// M: add for ALPS01918645, hide deliver status and time because they contains old data.
        //tangyisen
        /*if (mDeliveredIndicator != null) {
            mDeliveredIndicator.setVisibility(View.GONE);
        }*/
        if (mDateView != null) {
            mDateView.setVisibility(View.GONE);
        }
        /// @}
        // add for ipmessage
        //mIpMessageListItem.onIpBindDefault();

        //tangyisen
        /*IIpMessageItemExt ipMsgItem = null;
        if (msgItem != null) {
            ipMsgItem = msgItem.mIpMessageItem;
        }
        MmsLog.d(TAG, "call onIpBindDefault, ipMsgItem = " + ipMsgItem);*/
        //mIpMessageListItem.onIpBindDefault(ipMsgItem);
        
        requestLayout();
    }
    /// @}

    ///M: add for adjust text size
    public void setBodyTextSize(float size) {
        //tangyisen delete
        /*if (mBodyTextView != null && mBodyTextView.getVisibility() == View.VISIBLE) {
            mBodyTextView.setTextSize(size);
        }*/
        // add for ipmessage
        //mIpMessageListItem.onIpSetBodyTextSize(size);
    }

    @Override
    public void setImage(Uri mUri) {
        try {
            Bitmap bitmap = null;
            if (null == mUri) {
                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_missing_thumbnail_picture);
            } else {
                InputStream mInputStream = null;
                try {
                    mInputStream = this.getContext().getContentResolver().openInputStream(mUri);
                    if (mInputStream != null) {
                        bitmap = BitmapFactory.decodeStream(mInputStream);
                    }
                } catch (FileNotFoundException e) {
                    bitmap = null;
                } finally {
                    if (mInputStream != null) {
                        mInputStream.close();
                    }
                }
                setImage("", bitmap);
            }
        } catch (java.lang.OutOfMemoryError e) {
            Log.e(TAG, "setImage(Uri): out of memory: ", e);
        } catch (IOException e) {
            Log.e(TAG, "mInputStream.close() IOException." + e);
        }
    }

    // Add for vCard begin
    private void hideFileAttachmentViewIfNeeded() {
        if (mFileAttachmentView != null) {
            mFileAttachmentView.setVisibility(View.GONE);
        }
    }

    private void showFileAttachmentView(ArrayList<FileAttachmentModel> files) {
        // There should be one and only one file
        if (files == null || files.size() < 1) {
            Log.e(TAG, "showFileAttachmentView, oops no attachment files found");
            return;
        }
        final int filesize = files.size();
        ArrayList<FileAttachmentModel> mfiles = files;

        if (mFileAttachmentView == null) {
            //tangyisen
            /*findView(mms_file_attachment_view_stub).setVisibility(VISIBLE);
            mFileAttachmentView = findView(file_attachment_view);*/
        }
        mFileAttachmentView.setVisibility(View.VISIBLE);

        final FileAttachmentModel attach = files.get(0);
        mFileAttachmentView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mSelectedBox != null && mSelectedBox.getVisibility() == View.VISIBLE) {
                    return;
                }
                /*if (mOpMessageListItemExt.onFileAttachmentViewClick(
                        attach.isVCard(), attach.getSrc())) {
                    return;
                }*/
                if (attach.isVCard()) {
                    VCardUtils.importVCard(mContext, attach);
                } else if (attach.isVCalendar()) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(attach.getUri(), attach.getContentType().toLowerCase());
                        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        mContext.startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        MmsLog.e(TAG, "no activity handle ", e);
                    }
                }
            }
        });

        //tangyisen
        /*ImageView thumb = (ImageView) findView(file_attachment_thumbnail, mFileAttachmentView);
        TextView name = (TextView) findView(file_attachment_name_info, mFileAttachmentView);
        TextView name2 = (TextView) findView(file_attachment_name_info2, mFileAttachmentView);*/
        String nameText = "";
        int thumbResId = -1;
        if (attach.isVCard()) {
            nameText = mContext.getString(R.string.file_attachment_vcard_name, attach.getSrc());
            thumbResId = R.drawable.ic_vcard_attach;
        } else if (attach.isVCalendar()) {
            nameText = mContext.getString(R.string.file_attachment_vcalendar_name, attach.getSrc());
            thumbResId = R.drawable.ic_vcalendar_attach;
        }

        //tangyisen
        /*name.setText(nameText);

        if (thumbResId != -1) {
            thumb.setImageDrawable(mContext.getResources().getDrawable(thumbResId));
        }*/
        //tanyisen
        /*final TextView size = (TextView) findView(file_attachment_size_info, mFileAttachmentView);
        size.setText(MessageUtils.getHumanReadableSize(attach.getAttachSize()));*/

        /*mOpMessageListItemExt.showFileAttachmentView(
                mFileAttachmentView, name2, name, thumb, size, files,
                new Intent(mContext, MultiSaveActivity.class), mMessageItem.mMsgId, attach,
                attach.mOpFileAttachmentModelExt);*/
        /// @}
    }
    // Add for vCard end

  private boolean copyPart(PduPart part, String filename) {
      Uri uri = part.getDataUri();
      if(DEBUG) MmsLog.i(TAG, "copyPart, copy part into sdcard uri " + uri);

      InputStream input = null;
      FileOutputStream fout = null;
      try {
          ContentResolver mContentResolver = mContext.getContentResolver();
          input = mContentResolver.openInputStream(uri);
          if (input instanceof FileInputStream) {
              FileInputStream fin = (FileInputStream) input;
              // Depending on the location, there may be an
              // extension already on the name or not
              String dir = "";
              File file = MessageUtils.getStorageFile(filename, mContext.getApplicationContext());
              if (file == null) {
                  return false;
              }
              fout = new FileOutputStream(file);
              byte[] buffer = new byte[8000];
              int size = 0;
              while ((size = fin.read(buffer)) != -1) {
                  fout.write(buffer, 0, size);
              }

              // Notify other applications listening to scanner events
              // that a media file has been added to the sd card
              mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                      Uri.fromFile(file)));
          }
      } catch (IOException e) {
          // Ignore
          MmsLog.e(TAG, "IOException caught while opening or reading stream", e);
          return false;
      } finally {
          if (null != input) {
              try {
                  input.close();
              } catch (IOException e) {
                  // Ignore
                  MmsLog.e(TAG, "IOException caught while closing stream", e);
                  return false;
              }
          }
          if (null != fout) {
              try {
                  fout.close();
              } catch (IOException e) {
                  // Ignore
                  MmsLog.e(TAG, "IOException caught while closing stream", e);
                  return false;
              }
          }
      }
      return true;
  }

    private void bindDividers(MessageItem msgItem, boolean isDeleteMode) {
        MmsLog.d(TAG_DIVIDER, "listItem.bindDividers(): draw time divider ?= " + msgItem.mIsDrawTimeDivider);
        //tangyisen
        /*if (null != mTimeDivider) {
            if (msgItem.mIsDrawTimeDivider) {
                mTimeDivider.setVisibility(View.VISIBLE);
                mTimeDividerStr.setText(msgItem.mTimeDividerString);
            } else {
                mTimeDivider.setVisibility(View.GONE);
            }
        }*/
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // add for ipmessage
        //mIpMessageListItem.onIpDetachedFromWindow();
    }

    public void setMessageListItemAdapter(MessageListAdapter adapter) {
        mMessageListAdapter = adapter;
        //tangyisen delete
        //mIpMessageListItem.setIpMessageListItemAdapter(mMessageListAdapter.mIpMessageListAdapter);
    }

    /**
     * Init Plugin
     *
     * @param context
     */
    private void initPlugin(Context context) {
        // add for ipmessage
        /*mIpMessageListItem = IpMessageUtils.getIpMessagePlugin(context).getIpMessageListItem();
        mOpMessageListItemExt = OpMessageUtils.getOpMessagePlugin().getOpMessageListItemExt();
        mOpMessageListItemExt.init(this, context);*/
    }

    /**
     * M: BUGFIX: ALPS00515336; Hide all view
     */
    public void hideAllView() {
        MmsLog.d(M_TAG, "hideAllView()");
        mSelectedBox.setVisibility(View.GONE);
        setLongClickable(false);
        setFocusable(false);
        setClickable(false);

        if (mMmsView != null) {
            mMmsView.setVisibility(View.GONE);
        }
        if (mFileAttachmentView != null) {
            mFileAttachmentView.setVisibility(View.GONE);
        }
        /// when we fail to reload item and put in cache during 500 ms
        mBodyTextView.setVisibility(View.GONE);

        //tangyisen
        /*if (null != mTimeDivider) {
            mTimeDivider.setVisibility(View.GONE);
        }*/

        mDateView.setVisibility(View.GONE);
        //mSubStatus.setVisibility(View.GONE);
        //tangyisen
       /* if (mDownloadButton != null) {
            mDownloadingLabel.setVisibility(View.GONE);
            mDownloadButton.setVisibility(View.GONE);
            //mOpMessageListItemExt.hideAllView();
        }*/
        //tangyisen delete
        /*mLockedIndicator.setVisibility(View.GONE);
        mSubStatus.setVisibility(View.GONE);
        mDeliveredIndicator.setVisibility(View.GONE);
        mDetailsIndicator.setVisibility(View.GONE);
        /// check NPE
        if (mAvatar != null && mAvatar.getVisibility() == View.VISIBLE) {
            mAvatar.setVisibility(View.GONE);
        }*/

        /// MTK MR1 new feature: Group Mms
        /// set Gone in bindDefault()
        //tangyisen
        /*if (mMessageItem != null && !mMessageItem.isMe()) {
            mSenderName = (TextView) findView(sender_name);
            if (mSenderName != null) {
                mSenderName.setVisibility(View.GONE);
            }
            mSendNameSeparator = findView(sender_name_separator);
            if (mSendNameSeparator != null) {
                mSendNameSeparator.setVisibility(View.GONE);
            }
            mSenderPhoto = (QuickContactBadge) findView(sender_photo);
            if (mSenderPhoto != null) {
                mSenderPhoto.setVisibility(View.GONE);
            }
        }*/
        requestLayout();
    }

    /**
     * M: Use default icon to display
     */
    private Drawable parseAppIcon(Context context, String url) {
        final String telPrefix = "tel:";
        final String smsPrefix = "smsto:";
        final String mailPrefix = "mailto";
        int drawableId;

        if (url.startsWith(telPrefix)) {
            drawableId = R.drawable.common_phone;
        } else if (url.startsWith(smsPrefix)) {
            drawableId = R.drawable.common_message;
        } else if (url.startsWith(mailPrefix)) {
            drawableId = R.drawable.common_email;
        } else {
            drawableId = R.drawable.common_browser;
        }
        return context.getResources().getDrawable(drawableId);
    }

    // Add for IpMessage callback

    public void setSubDateView(String subName) {
        //MessageUtils.setSubIconAndLabel(mMessageItem.mSubId, subName, mSubStatus);

        if (mMessageItem.isFailedMessage()
                || (!mMessageItem.isSending() && TextUtils.isEmpty(mMessageItem.mTimestamp))) {
            mDateView.setVisibility(View.GONE);
        } else {
            mDateView.setVisibility(View.VISIBLE);
            // / M: google jb.mr1 patch, group mms
            mDateView.setText(mMessageItem.isSending() ? mContext.getResources().getString(
                    R.string.sending_message) : buildTimestampLine(mMessageItem.mTimestamp));
        }
        //drawRightStatusIndicator(mMessageItem);
        requestLayout();
    }

    public void setTextMessage(String body) {
        CharSequence formattedMessage = mMessageItem.getCachedFormattedMessage();
        if (formattedMessage == null) {
            formattedMessage = formatMessage(mMessageItem, body, null, mMessageItem.mHighlight,
                    null);
            mMessageItem.setCachedFormattedMessage(formattedMessage);
        }
        mBodyTextView.setVisibility(View.VISIBLE);
        //tangyisen
        mBodyTextView.setContent(formattedMessage, mMessageItem.isInboxMessage());
    }
    
    public void updateMessageItemState(long msgId) {
        //tangyisen
        /*if (mMessageItem.mIpMessageId <= 0 || mMessageItem.mMsgId != msgId) {
            return;
        }*/
        final long messageId = msgId;
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean deleteMode = mMessageListAdapter == null ? false
                        : mMessageListAdapter.mIsDeleteMode;
                if (deleteMode) {
                    long id = mMessageListAdapter.getKey("sms", messageId);
                    if (mMessageListAdapter.mListItem.get(id) == null) {
                        mMessageListAdapter.mListItem.put(id, false);
                    } else {
                        mMessageItem.setSelectedState(mMessageListAdapter.mListItem.get(id));
                    }
                }
                bind(mMessageItem, false, 0, deleteMode);
            }
        });
    }

    public void showFileAttachmentViewCallback(ArrayList files) {
        showFileAttachmentView((ArrayList<FileAttachmentModel>) files);
    }

    public void hideFileAttachmentViewIfNeededCallback() {
        hideFileAttachmentViewIfNeeded();
    }

    public File getStorageFileCallback(String fileName, Context context) {
        return MessageUtils.getStorageFile(fileName, context);
    }

    public String getHumanReadableSizeCallback(int size) {
        return MessageUtils.getHumanReadableSize(size);
    }

    public boolean copyPartCallback(PduPart part, String filename) {
        return copyPart(part, filename);
    }

    @Override
    public String buildTimestampLineCallback(String time) {
        return buildTimestampLine(time);
    }
    
    //tangyisen begin
    private void updateSimIcon(Context context, int slotId) {
         boolean isShowSimIcon = MessageUtils.isTwoSimCardEnabled() && slotId >= 0;
         if(isShowSimIcon) {
             mSimCardView.setVisibility( View.VISIBLE );
             Drawable mSimIndicatorIcon = MessageUtils.getMultiSimIcon(context, slotId);
             mSimCardView.setImageDrawable(mSimIndicatorIcon);
         }else{
             mSimCardView.setVisibility( View.GONE );
         }
    }
    
    private void updateMmsLayoutParentBackground(Context context, int slotId){
        boolean isShowSimIcon = MessageUtils.isTwoSimCardEnabled() && slotId >= 0;
        if(isShowSimIcon) {
            Drawable mBg = MessageUtils.getMultiSimIcon(context, slotId);
            mMmsLayoutParent.setBackgroundDrawable( mBg );
        }
    }

    private void updateGroupPanel() {
        if (mMessageItemType == INCOMING_ITEM_TYPE) {
            return;
        }
        if (mIsGroupMms) {
            mGroupSendInfo.setVisibility( View.VISIBLE );
            //mGroupSendInfo.setText(R.string.sms_group_summary);
            mGroupSendDetail.setVisibility( View.VISIBLE );
        } else {
            mGroupSendInfo.setVisibility( View.GONE );
            mGroupSendDetail.setVisibility( View.GONE );
        }
   }
    
    int mMessageItemType;
    private static final int INCOMING_ITEM_TYPE = 0;
    private static final int OUTGOING_SIM1_ITEM_TYPE = 1;
    private static final int OUTGOING_SIM2_ITEM_TYPE = 2;
    //can be set in construct fun
    public void bind(MessageItem msgItem, boolean convGroupMms, int position, boolean isDeleteMode, int type) {
        mMessageItemType = type;
        bind(msgItem, convGroupMms, position, isDeleteMode);
    }
    
    public MessageListItem(Context context, View view, int type) {
        super(context);
        mMessageItemType = type;
        initView( context, view );
    }
    
    private void updateSendStatus(MessageItem msgItem) {
        if ((msgItem.isOutgoingMessage() && msgItem.isFailedMessage()) ||
                msgItem.mDeliveryStatus == MessageItem.DeliveryStatus.FAILED) {
            //MessageUtils.showView( mPanelContent );
            MessageUtils.showView(mFailResend);
        } else {
            MessageUtils.hideView(mFailResend);
            //MessageUtils.hideView( mPanelContent );
            if(!isSmsGroup( msgItem ) && null != mSendStatus && mSendStatus.getVisibility() == View.GONE) {
                if (mIsShowSendStatus) {
                    MessageUtils.showView( mSendStatus );
                    mSendStatus.setText( R.string.message_item_sent_sucess );
                }
            }
        }
    }
    
    private void initView(Context context, View view) {
        addView(view);
        mDefaultCountryIso = MmsApp.getApplication().getCurrentCountryIso();
        int color = context.getResources().getColor(R.color.timestamp_color);
        mColorSpan = new ForegroundColorSpan(color);
        if (sDefaultContactImage == null) {
            sDefaultContactImage = context.getResources().getDrawable(R.drawable.ic_contact_picture);
        }
        initPlugin(this.getContext());
        onFinishInflateView();
    }
    
    private boolean mIsCheckBoxMode = false;
    public boolean isEditAble(){
        return mIsCheckBoxMode;
    }
    
    public void setIsCheckBoxMode(boolean isCheckBoxMode) {
        mIsCheckBoxMode = isCheckBoxMode;
        if(mPanelContentSpace != null) {
            mPanelContentSpace.setVisibility(isCheckBoxMode ? View.VISIBLE: View.GONE);
        }
        if(mDateSimSpaceView != null) {
            mDateSimSpaceView.setVisibility(isCheckBoxMode ? View.VISIBLE: View.GONE);
        }
        if (mSelectedBox != null) {
            if(!isCheckBoxMode) {
                mSelectedBox.setChecked( false );
            }
            mSelectedBox.setVisibility(isCheckBoxMode ? View.VISIBLE: View.GONE);
        }
    }

    public boolean getIsCheckBoxMode() {
        return mIsCheckBoxMode;
    }

    public void markAsSelected(boolean selected) {
        if (mSelectedBox != null && mSelectedBox.getVisibility() == View.VISIBLE /*&& mSelectedBox.isChecked() != selected*/) {
            mSelectedBox.setChecked(selected);
        }
    }

    public void updateParentBackground(MessageItem item) {
        if(item == null) {
            return;
        }
        if(item.isInboxMessage() || !MessageUtils.isTwoSimCardEnabled()) {
            return;
        }
        int slotId = SubscriptionManager.getSlotId(item.getSubId());
        if(slotId < 0) {
            return;
        }
        if(mMmsLayoutParent != null) {
            mMmsLayoutParent.setBackgroundDrawable( MessageUtils.getMultiSimItemBackground( item.mContext, slotId) );
        }
    }

    public void updateGroupState(MessageItem item) {
        if(item == null) {
            return;
        }
        if(/*item.mSmsGroupId == -1*/isSmsGroup(item)) {
            MessageUtils.showView( mGroupSendInfo );
            mGroupSendInfo.setText( (item.mSmsNotiCount - item.mSmsFailsCount) + "/" + mRecipientsCount + getContext().getResources().getString( R.string.sms_group_summary ));
            MessageUtils.showView( mGroupSendDetail );
            if(item.mSmsFailsCount > 0) {
                MessageUtils.showView( mFailResend );
                MessageUtils.hideView( mSendStatus );
            } else if(item.mSmsNotiCount == mRecipientsCount) {
                MessageUtils.hideView( mFailResend );
                if (mIsShowSendStatus) {
                    MessageUtils.showView( mSendStatus );
                    mSendStatus.setText( R.string.message_item_sent_sucess );
                } else {
                    MessageUtils.hideView( mSendStatus );
                }
            } else {
                MessageUtils.showView( mSendStatus );
                mSendStatus.setText( R.string.message_item_is_sending );
            }
        } else {
            MessageUtils.hideView( mGroupSendInfo );
            MessageUtils.hideView( mGroupSendDetail );
        }
    }

    private boolean isSmsGroup(MessageItem item) {
        return mIsGroupMms && "sms".equals( item.mType );
    }

    public void showGroupDetailActivty(Context context) {//, long msgId) {
        Intent i = new Intent(context, ZzzMessageItemGroupDetailActivity.class);
        //i.setData(ContentUris.withAppendedId(Sms.CONTENT_URI, msgId));
        i.putExtra(ZzzMessageItemGroupDetailActivity.GROUP_ID_KEY, mMessageItem.mMsgId);
        context.startActivity(i);
    }

    private int mRecipientsCount;
    public void setRecipientsCount(int count) {
        mRecipientsCount = count;
    }

    private boolean mIsShowSendStatus = false;
    public void setIsShowSendStatus(boolean show) {
        mIsShowSendStatus = show;
    }

    private void showVcardView(ArrayList<FileAttachmentModel> files, View vcardView) {
        // There should be one and only one file
        if (files == null || files.size() < 1) {
            Log.e(TAG, "showFileAttachmentView, oops no attachment files found");
            return;
        }
        if(vcardView == null) {
            return;
        }
        final int filesize = files.size();
        ArrayList<FileAttachmentModel> mfiles = files;

        final FileAttachmentModel attach = files.get(0);
        /*vcardView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (attach.isVCard()) {
                    VCardUtils.importVCard(mContext, attach);
                } else if (attach.isVCalendar()) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(attach.getUri(), attach.getContentType().toLowerCase());
                        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        mContext.startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        MmsLog.e(TAG, "no activity handle ", e);
                    }
                }
            }
        });*/
    }

    private void clickVcardView(ArrayList<FileAttachmentModel> files) {
        // There should be one and only one file
        if (files == null || files.size() < 1) {
            Log.e(TAG, "showFileAttachmentView, oops no attachment files found");
            return;
        }
        final int filesize = files.size();
        ArrayList<FileAttachmentModel> mfiles = files;

        final FileAttachmentModel attach = files.get(0);
        if (attach.isVCard()) {
            VCardUtils.importVCard(mContext, attach);
        } else if (attach.isVCalendar()) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(attach.getUri(), attach.getContentType().toLowerCase());
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                mContext.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                MmsLog.e(TAG, "no activity handle ", e);
            }
        }
    }
    //tangyisen end
}
