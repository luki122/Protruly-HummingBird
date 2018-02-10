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

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.net.Uri;
import android.os.Handler;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.mms.MmsApp;
import com.android.mms.R;
import com.android.mms.data.Contact;
import com.android.mms.data.ContactList;
import com.android.mms.data.Conversation;
import com.android.mms.util.MmsLog;
import com.mediatek.cb.cbmsg.CBMessage;
import com.zzz.provider.Telephony;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// M:
/// M: add for ipmessage
//import com.hb.tms.TmsServiceManager;
//import com.hb.tms.MarkResult;

/**
 * This class manages the view for given conversation.
 */
public class ConversationListItem extends RelativeLayout implements Contact.UpdateListener,
//public class ConversationListItem extends LinearLayout implements Contact.UpdateListener,
            Checkable {
    private static final String TAG = "Mms/ConvListItem";
    private static final boolean DEBUG = true;
    private static final boolean BIND_DBG = false;

    private TextView mSubjectView;//just like summary
    private TextView mFromView;//just like title
	//lichao add begin
    private TextView mBlackView;
    private View mCheckBoxStubView;
    private CheckBox mCheckBox;
    private View mUnreadStubView;
    private TextView mUnreadView;
    private View mSimTopStubView;
    private ImageView mSimIconView;
    private View mSummaryLayout;
    private ImageView mTopView;//lichao add in 2017-04-19
    //lichao add end
    private TextView mDateView;
    //private ImageView mAttachmentView;
    //private ImageView mErrorIndicator;
    //private QuickContactBadge mAvatarView;
    //private static Drawable sDefaultContactImage;
    // M: add for multi check begin
    //private ImageView mSelectIcon;
    //private static Drawable sDefaultSelectedImage;
    // M: add for multi check end

    // For posting UI update Runnables from other threads:
    private Handler mHandler = new Handler();

    private Conversation mConversation;

    public static final StyleSpan STYLE_BOLD = new StyleSpan(Typeface.BOLD);

    private Context mContext;
    /// M: add for new common feature.
    //private View mMuteView;
    //private TextView mUnreadView;
    //private static final int MAX_UNREAD_MESSAGES_COUNT = 999;
    //private static final String MAX_UNREAD_MESSAGES_STRING = "999+";
    //private static final int MAX_READ_MESSAGES_COUNT = 9999;
    //private static final String MAX_READ_MESSAGES_STRING = "9999+";

    // M: add for op
    //private IOpConversationListItemExt mOpConversationListItemExt = null;

    /// M: New feature for rcse, adding IntegrationMode. @{
    //private ImageView mFullIntegrationModeView;
    /// @}

    //add for ipmessage
    //public IIpConversationListItemExt mIpConvListItem;

    public ConversationListItem(Context context) {
        super(context);
        mContext = context;
        //mOpConversationListItemExt = OpMessageUtils.getOpMessagePlugin()
        //        .getOpConversationListItemExt();
        //lichao add
        initDimension(context);
    }

    public ConversationListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        /*if (sDefaultContactImage == null) {
            sDefaultContactImage = context.getResources().getDrawable(R.drawable.ic_default_contact);
        }*/
        //if (sDefaultSelectedImage == null) {
        //    sDefaultSelectedImage = context.getResources().getDrawable(R.drawable.ic_selected_item);
        //}
        //mOpConversationListItemExt = OpMessageUtils.getOpMessagePlugin()
        //        .getOpConversationListItemExt();
        //lichao add
        initDimension(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mFromView = (TextView) findViewById(R.id.conv_list_from);
        mBlackView = (TextView) findViewById(R.id.conv_list_black);
        mSubjectView = (TextView) findViewById(R.id.conv_list_subject);
        mSummaryLayout = findViewById(R.id.conv_list_summary_layout);
        mDateView = (TextView) findViewById(R.id.conv_list_date);
        //mAttachmentView = findViewById(R.id.attachment);
        //mErrorIndicator = (ImageView) findViewById(R.id.error);
        //mAvatarView = (QuickContactBadge) findViewById(R.id.avatar);
        //mAvatarView.setOverlay(null);
        //mSelectIcon = (ImageView) findViewById(R.id.selectIcon);
        /// M: add for ipmessage
        //mMuteView = findViewById(R.id.mute);
        //mUnreadView = (TextView) findViewById(R.id.unread);
        /// M: New feature for rcse, adding IntegrationMode. @{
        //mFullIntegrationModeView = (ImageView) findViewById(R.id.fullintegrationmode);

        //mIpConvListItem = IpMessageUtils.getIpMessagePlugin(mContext).getIpConversationListItem();
        //mIpConvListItem.onIpSyncView(mContext, mFullIntegrationModeView, mAvatarView);
        /// @}
        // add for op
        //mOpConversationListItemExt.onFinishInflate(mSubjectView);
    }

    public Conversation getConversation() {
        return mConversation;
    }

    /**
     * Only used for header binding.
     */
	//lichao delete
    /*public void bind(String title, String explain) {
        mFromView.setText(title);
        mSubjectView.setText(explain);
    }*/

    private CharSequence formatMessage() {
        //final int color = android.R.styleable.Theme_textColorSecondary;
        /// M: Code analyze 029, For new feature ALPS00111828, add CellBroadcast feature . @{
        String from = null;
        if (mConversation.getType() == Telephony.Threads.CELL_BROADCAST_THREAD) {
            from = formatCbMessage();
        } else {
            //mtk delete begin
//            if (mConversation.getRecipients().size() == 1) {
//                Contact contact = mConversation.getRecipients().get(0);
//                String name = mIpConvListItem.onIpFormatMessage(contact.getIpContact(mContext), mConversation.getThreadId(), contact.getNumber(), contact.getName());
//                if (name != null) {
//                    contact.setName(name);
//                }
//            }
            //mtk delete end
            //lichao delete
            /*if (mConversation.getRecipients() != null && mConversation.getRecipients().size() > 0) {
                Contact contact = mConversation.getRecipients().get(0);
                from = mIpConvListItem.onIpFormatMessage(contact.getIpContact(mContext), mConversation.getThreadId(), contact.getNumber(), mConversation.getRecipients().formatNames(", "));
            } else {
                from = mIpConvListItem.onIpFormatMessage(null, mConversation.getThreadId(), null, null);
            }*/

            //if (TextUtils.isEmpty(from)) {
                from = mConversation.getRecipients().formatNames(", ");
            //}
        }

        if (TextUtils.isEmpty(from)) {
            from = mContext.getString(android.R.string.unknownName);
        }
        /// @}

        //SpannableStringBuilder buf = new SpannableStringBuilder(from);

        /// M:
        //lichao delete
        /*int before = buf.length();
        if (!mConversation.hasUnreadMessages()) {
            MmsLog.d(TAG, "formatMessage(): Thread " + mConversation.getThreadId() +
                    " has no unread message.");
            int count = mConversation.getMessageCount();
            /// M: add for op
			//lichao delete
            //count = mOpConversationListItemExt.formatMessage(
            //            (ImageView) findViewById(R.id.sim_type_conv),
            //            mConversation.mOpConversationExt, count);
            if (count > 1) {
                if (count > MAX_READ_MESSAGES_COUNT) {
                    buf.append("  " + MAX_READ_MESSAGES_STRING);
                } else {
                    buf.append("  " + count);
                }
                buf.setSpan(new ForegroundColorSpan(mContext.getResources().getColor(R.color.message_count_color)),
                        before, buf.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            }
        }*/

        //lichao delete
        /*if (mConversation.hasDraft()) {
           // buf.append(mContext.getResources().getString(R.string.draft_separator));
            //int before = buf.length();
            int size;
            buf.append(",  " + mContext.getResources().getString(R.string.has_draft));
            size = android.R.style.TextAppearance_Small;
            buf.setSpan(new TextAppearanceSpan(mContext, size, color), before + 1,
                    buf.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            buf.setSpan(new ForegroundColorSpan(
                    mContext.getResources().getColor(R.drawable.text_color_red)),
                    before + 1, buf.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        }*/

 
        // Unread messages are shown in bold
        //lichao delete
        /*if (mConversation.hasUnreadMessages()) {
            buf.setSpan(STYLE_BOLD, 0, buf.length(),
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        }*/

        //return buf;
        return from;
    }

	//lichao delete
	/*
    private void updateAvatarView() {
        Drawable avatarDrawable;
        ConversationList conversationList = (ConversationList) ConversationList.getContext();
        if (conversationList.isActionMode() && mConversation.isChecked()) {
            mSelectIcon.setVisibility(View.VISIBLE);
            mAvatarView.setVisibility(View.GONE);
            mSelectIcon.setImageDrawable(sDefaultSelectedImage);
        } else {
            if (mIpConvListItem.updateIpAvatarView(mConversation.getIpConv(mContext), mAvatarView,
                    mSelectIcon)){
                return;
            }
            Uri photoUri = null;
            if (mConversation.getRecipients().size() == 1) {
                final Contact contact = mConversation.getRecipients().get(0);
                photoUri = contact.getPhotoUri();
                boolean isJoynNumber = mIpConvListItem.updateIpAvatarView(contact.getIpContact(mContext), contact.getNumber(), mAvatarView, contact.getUri());
                avatarDrawable = contact.getAvatar(mContext, sDefaultContactImage, mConversation.getThreadId());

                /// M: fix bug ALPS00400483, same as 319320, clear all data of mAvatarView firstly. @{
                mAvatarView.assignContactUri(null);
                /// @}
                /// M: Code analyze 030, For new feature ALPS00241750, Add email address
                /// to email part in contact . @{
                String number = contact.getNumber();
                // add for joyn converged inbox mode
                if (isJoynNumber) {
                    number = number.substring(4);
                }
                if (Mms.isEmailAddress(number)) {
                    mAvatarView.assignContactFromEmail(number, true);
                } else {
                    if (contact.existsInDatabase()) {
                        mAvatarView.assignContactUri(contact.getUri());
                    } else {
                        mAvatarView.assignContactFromPhone(number, true);
                    }
                    /// @}
                }
            } else {
                // TODO get a multiple recipients asset (or do something else)
                avatarDrawable = sDefaultContactImage;
                mAvatarView.assignContactUri(null);
            }
            ImageView headerView;
            if (conversationList.isActionMode()) {
                mSelectIcon.setVisibility(View.VISIBLE);
                mAvatarView.setVisibility(View.GONE);
                headerView = mSelectIcon;
            } else {
                mSelectIcon.setVisibility(View.GONE);
                mAvatarView.setVisibility(View.VISIBLE);
                headerView = mAvatarView;
            }
            if (avatarDrawable != sDefaultContactImage) {
                ContactPhotoManager contactPhotoManager = ContactPhotoManager.getInstance(mContext);
                final DefaultImageRequest request = new DefaultImageRequest(null, null,
                        ContactPhotoManager.TYPE_DEFAULT, true);
                contactPhotoManager.loadDirectoryPhoto(headerView, photoUri,
                        false, true, request);
            } else {
                headerView.setImageDrawable(avatarDrawable);
            }
        }
    }*/

    private void updateFromView() {
        //if(DEBUG) Log.v(TAG, " updateFromView()");
		mFromView.setText(formatMessage());
        ContactList recipients = mConversation.getRecipients();
        HashSet<String> blackNumSet = MessageUtils.getBlacklistSet(mContext);
        updateBlackView(blackNumSet, recipients);
        updateFromViewMaxWidth(mContext);
        //updateAvatarView();
    }

    private Runnable mUpdateFromViewRunnable = new Runnable() {
        public void run() {
            updateFromView();
        }
    };

    //for Contact.UpdateListener()
    public void onUpdate(Contact updated) {
        //if(DEBUG) Log.v(TAG, "onUpdate(), contact: " + updated);
        /// M: fix blank screen issue. if there are 1000 threads, 1 recipient each thread,
        /// and 8 list items in each screen, onUpdate() will be called 8000 times.
        /// mUpdateFromViewRunnable run in UI thread will blocking the other things.
        /// remove blocked mUpdateFromViewRunnable.
        mHandler.removeCallbacks(mUpdateFromViewRunnable);
        mHandler.post(mUpdateFromViewRunnable);
    }

	//bind Conversation, lichao add blackNumSet
    public final void bind(Context context, final Conversation conversation,
                           HashSet<String> blackNumSet) {
        if (BIND_DBG) Log.v(TAG, "\n\n bind(), getThreadId()=" + conversation.getThreadId());
        mConversation = conversation;
        //lichao add 022026173
        updateCheckBox(context, conversation.isChecked());
        boolean hasUnread = mConversation.hasUnreadMessages();

        /// M: Code analyze 031, For bug ALPS00235723, The crolling performance of message . @{
        //updateBackground(conversation);
        //boolean hasError = conversation.hasError();

        //lichao add begin
		int subId = conversation.getSubId();
        int slotId = SubscriptionManager.getSlotId(subId);
        boolean isIccCardEnabled = MessageUtils.isIccCardEnabled(slotId);
        boolean isTwoSimCardEnabled = MessageUtils.isTwoSimCardEnabled();
        //if isIccCardEnabled, no need to judge slotId >= 0
        boolean isShowSimIcon = isIccCardEnabled && isTwoSimCardEnabled;
        if (BIND_DBG) {
            Log.d(TAG, "bind, subId=" + subId
                    + ", slotId=" + slotId
                    + ", isIccCardEnabled=" + isIccCardEnabled
                    + ", isShowSimIcon=" + isShowSimIcon);
        }
        //boolean isShowSimIcon = hasUnread;//for debug
        updateSimIcon(context, slotId, isShowSimIcon);

        // Subject
        //lichao change hasError() to latestHasError() in 2017-05-27
        CharSequence subject = MessageUtils.formatSubject(context, conversation.getSnippet(),
                conversation.hasDraft(), conversation.latestHasError());
        mSubjectView.setText(subject);
        setSubjectLayoutMargins(context);
		//lichao add end

        //boolean hasAttachment = conversation.hasAttachment();
        //mAttachmentView.setVisibility(hasAttachment ? VISIBLE : GONE);

        // Date
        mDateView.setVisibility(VISIBLE);
        String dateStr = MessageUtils.formatTimeStampStringForItem(context, conversation.getDate());
        //Log.v(TAG, "bind, dateStr = "+ dateStr);
        mDateView.setText(dateStr);
        setDateViewMargins(context);
		
        boolean isTop = conversation.getTop();
        updateTopIcon(context, isTop);
        setSimTopViewMargins(context, isShowSimIcon, isTop);

        setSubjectMargins(context, isShowSimIcon, isTop);


        // From.
        mFromView.setVisibility(VISIBLE);
		mFromView.setText(formatMessage());
        ContactList recipients = conversation.getRecipients();
        updateBlackView(blackNumSet, recipients);

        //updateAvatarView();

        /// M: this local variable has never been used. delete google default code.
        // Register for updates in changes of any of the contacts in this conversation.
        // ContactList contacts = conversation.getRecipients();
        Contact.addListener(this);
        updateFromViewMaxWidth(context);
        setFromMargins(context, hasUnread);

        /// M: Code analyze 031, For bug ALPS00235723, The crolling performance of message .
        mSubjectView.setVisibility(VISIBLE);

        // Transmission error indicator.
        //mErrorIndicator.setVisibility(hasError ? VISIBLE : GONE);
        updateUnreadView(context, hasUnread);
    }

    /*
	//lichao delete mtk codes
    private void updateBackground(Conversation conversation) {
        int backgroundId;
        /// M: fix bug ALPS00998351, solute the issue "All of the threads still
        /// highlight after you back to all thread view". @{
        ConversationList conversationList = (ConversationList) ConversationList.getContext();
        /// @}
        if (conversationList.isActionMode() && conversation.isChecked()) {
            backgroundId = R.drawable.conversation_item_background_unread;
        } else if (conversation.hasUnreadMessages()) {
            backgroundId = R.drawable.conversation_item_background_unread;
        } else {
            backgroundId = R.drawable.conversation_item_background_unread;
        }
        Drawable background = mContext.getResources().getDrawable(backgroundId);
        setBackgroundDrawable(background);
    }*/

    //lichao modify
    //unbindConversation
    public final void unbind(boolean resetCheckBox) {
        /*if (DEBUG) {
            Log.v(TAG, "unbind: contacts.removeListeners " + this);
        }*/

        // Unregister contact update callbacks.
        Contact.removeListener(this);
        //mIpConvListItem.onIpUnbind();
        //mOpConversationListItemExt.unbind();

        //lichao add begin
        if(null != mCheckBox && resetCheckBox){
            mCheckBox.setChecked(false);
        }
        //lichao add end
    }

    public void setChecked(boolean checked) {
	    if(mConversation != null){
            mConversation.setIsChecked(checked);
            //updateBackground(mConversation);
            //updateAvatarView();
		}
    }

    public boolean isChecked() {
        return mConversation != null && mConversation.isChecked();
    }

    public void toggle() {
        mConversation.setIsChecked(!mConversation.isChecked());
    }

    /// M: Code analyze 031, For bug ALPS00235723, The crolling performance of message . @{
    //mtk add
    public void bindDefault(Conversation conversation) {
        MmsLog.d(TAG, "bindDefault().");
        if (conversation  != null) {
            //updateBackground(conversation);
        }
        /*if (null != mAttachmentView) {
            mAttachmentView.setVisibility(GONE);
        }*/
        mDateView.setVisibility(View.GONE);
        mFromView.setText(R.string.refreshing);
        mSubjectView.setVisibility(GONE);
        mSimIconView.setVisibility(GONE);
        mTopView.setVisibility(GONE);
        //mUnreadView.setVisibility(View.GONE);
        //mErrorIndicator.setVisibility(GONE);
        //mAvatarView.setImageDrawable(sDefaultContactImage);
        /// M:
        //mMuteView.setVisibility(View.GONE);
        //mOpConversationListItemExt.bindDefault((ImageView) findViewById(R.id.sim_type_conv));
    }
    /// @}

    /// M: Make sure listeners are removed so that ConversationList instance can be released @{
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if(BIND_DBG) Log.v(TAG, "onDetachedFromWindow!!!");
        Contact.removeListener(this);
        //mIpConvListItem.onIpDetachedFromWindow();
    }
    /// @}
	
	//lichao add
    private void updateBlackView(HashSet<String> blackNumSet, ContactList recipients) {
        boolean isContainBlack = MessageUtils.isContainBlackNumRecipients(blackNumSet, recipients);
        if (isContainBlack) {
            String blackMark = "";
            int recipientCount = recipients.getNumbersList().size();
            if (recipientCount == 1) {
                blackMark = mContext.getString(R.string.mark_block_with_bracket);
            } else if (recipientCount > 1) {
                int blackCount = MessageUtils.getBlackCountOfRecipients(blackNumSet, recipients);
                blackMark = "[" + blackCount + "/" + recipientCount + " " + mContext.getString(R.string.mark_block) + "]";
            }
            if(BIND_DBG) Log.d(TAG, "updateBlackView(), blackMark = "+blackMark);
            mBlackView.setText(blackMark);
            //int color = mContext.getResources().getColor(R.color.prefix_text_color_red);
            //mBlackView.setTextColor(color);
        }
        mBlackView.setVisibility(isContainBlack ? View.VISIBLE : View.GONE);
    }

    /// M:
	//formatCbMessageFrom
    private String formatCbMessage() {
        int channelId = 0;
        String from = null;
        if (mConversation.getRecipients().size() == 0) {
            return null;
        }
        //MmsLog.i(TAG, "recipients = " + mConversation.getRecipients().formatNames(", "));
        String number = mConversation.getRecipients().get(0).getNumber();
        if (!TextUtils.isEmpty(number)) {
            try {
                channelId = Integer.parseInt(number);
            } catch (NumberFormatException e) {
                MmsLog.e(TAG, "format number error!");
            }
        }

        String name = "";
        List<SubscriptionInfo> subInfoList
                = SubscriptionManager.from(mContext).getActiveSubscriptionInfoList();
        int subCount = (subInfoList != null && !subInfoList.isEmpty()) ? subInfoList.size() : 0;
        for (int i = 0; i < subCount; i++) {
            name = CBMessage.getCBChannelName(subInfoList.get(i).getSubscriptionId(), channelId);
            if (name != mContext.getString(R.string.cb_default_channel_name)) {
                break;
            }
        }

        if (TextUtils.isEmpty(name)) {
            name = MmsApp.getApplication().getApplicationContext()
                    .getString(R.string.cb_default_channel_name);
        }
        try {
            from = name + "(" + channelId + ")";
        } catch (NumberFormatException e) {
            MmsLog.e(TAG, "format recipient number error!");
        }
        return from;
    }
    /// @}

    //lichao add in 2016-09-03 begin
    /*private boolean isEnName() {
        String from = mConversation.getRecipients().formatNames(", ");
        if (MessageUtils.isWapPushNumber(from)) {
            String[] mAddresses = from.split(":");
            from = mAddresses[mContext.getResources().getInteger(
                    R.integer.wap_push_address_index)];
        }

        *//**
         * Add boolean to know that the "from" haven't the Arabic and '+'.
         * Make sure the "from" display normally for RTL.
         *//*
        Boolean isEnName = false;
        Boolean isLayoutRtl = (TextUtils.getLayoutDirectionFromLocale(Locale.getDefault())
                == View.LAYOUT_DIRECTION_RTL);
        if (isLayoutRtl && from != null) {
            if (from.length() >= 1) {
                Pattern pattern = Pattern.compile("[^أ-ي]+");
                Matcher matcher = pattern.matcher(from);
                isEnName = matcher.matches();
            }
        }
        return isEnName;
    }*/
    //lichao add in 2016-09-03 end

    //lichao add in 2016-09-22 begin
    private void updateUnreadView(Context context, boolean hasUnread) {
        if (!hasUnread) {
            if (null != mUnreadStubView) {
                MessageUtils.setMargins(context, mUnreadStubView, 0, 0, 0, 0);
            }
            if (null != mUnreadView && mUnreadView.getVisibility() != View.GONE) {
                mUnreadView.setVisibility(GONE);
            }
            return;
        }
        if (null == mUnreadStubView) {
            ViewStub stub = (ViewStub) findViewById(R.id.conv_list_unread_stub);
            mUnreadStubView = stub.inflate();
        }
        MessageUtils.setMargins(context, mUnreadStubView,
                UNREAD_VIEW_MARGIN_LEFT, UNREAD_VIEW_MARGIN_TOP, 0, 0);
        if (null == mUnreadView) {
            mUnreadView = (TextView) mUnreadStubView.findViewById(R.id.list_item_unread1);
            //mUnreadView.setText("");
        }
        mUnreadView.setBackground(getUnreadViewBackground(UNREAD_VIEW_WIDTH / 2));
        mUnreadView.setVisibility(VISIBLE);
    }

    //lichao add
    public ShapeDrawable getUnreadViewBackground(int dipRadius) {
        int bgColor_red = mContext.getResources().getColor(R.color.unread_view_bg_color_green);
        int radius = MessageUtils.dip2Px(mContext, dipRadius);
        float[] radiusArray = new float[]{radius, radius, radius, radius, radius, radius, radius, radius};
        RoundRectShape roundRect = new RoundRectShape(radiusArray, null, null);
        ShapeDrawable bgDrawable = new ShapeDrawable(roundRect);
        bgDrawable.getPaint().setColor(bgColor_red);
        bgDrawable.getPaint().setAntiAlias(true);
        return bgDrawable;
    }

    //lichao add
    private boolean mIsCheckBoxMode = false;
    public void setCheckBoxEnable(boolean flag) {
        mIsCheckBoxMode = flag;
    }
    public boolean getCheckBoxEnable() {
        return mIsCheckBoxMode;
    }

    //lichao add
    private void updateCheckBox(Context context, boolean isChecked) {
        if (!mIsCheckBoxMode) {
            if (null != mCheckBoxStubView) {
                MessageUtils.setMargins(context, mCheckBoxStubView, 0, 0, 0, 0);
            }
            if (null != mCheckBox) {
                mCheckBox.setVisibility(View.GONE);
            }
            return;
        }
        if (null == mCheckBoxStubView) {
            ViewStub stub = (ViewStub) findViewById(R.id.checkbox_stub);
            mCheckBoxStubView = stub.inflate();
        }
        if (null == mCheckBox) {
            mCheckBox = (CheckBox) mCheckBoxStubView.findViewById(R.id.list_item_check_box);
        }
        MessageUtils.setMargins(context, mCheckBoxStubView, 0, 0, CHECK_BOX_MARGIN_RIGHT, 0);
        mCheckBox.setChecked(isChecked);
        mCheckBox.setVisibility(View.VISIBLE);
    }

    //lichao add in 2017-04-19
    private void updateTopIcon(Context context, boolean isTop) {
        if (!isTop) {
            if (null != mTopView && mTopView.getVisibility() != View.GONE) {
                //MessageUtils.setMargins(context, mTopView,0,0,0,0);
                mTopView.setVisibility(View.GONE);
            }
        } else {
            inflateSimTopStubView();
            if (null == mTopView) {
                mTopView = (ImageView) mSimTopStubView.findViewById(R.id.top_icon);
            }
            //MessageUtils.setMargins(context, mTopView,20,0,0,0);
            mTopView.setVisibility(View.VISIBLE);
        }
    }

    //lichao add
    private void updateSimIcon(Context context, int slotId, boolean isShowSimIcon) {
        if (!isShowSimIcon) {
            if (null != mSimIconView && mSimIconView.getVisibility() != View.GONE) {
                //MessageUtils.setMargins(context, mSimIconView, 0, 0, 0, 0);
                mSimIconView.setVisibility(View.GONE);
            }
        } else {
            inflateSimTopStubView();
            if (null == mSimIconView) {
                mSimIconView = (ImageView) mSimTopStubView.findViewById(R.id.sim_indicator_icon);
            }
            Drawable mSimIndicatorIcon = MessageUtils.getMultiSimIcon(context, slotId);
            mSimIconView.setImageDrawable(mSimIndicatorIcon);
            //MessageUtils.setMargins(context, mSimIconView, SIM_ICON_MARGIN_LEFT, 0, 0, 0);
            if (mSimIconView.getVisibility() == View.GONE) {
                mSimIconView.setVisibility(View.VISIBLE);
            }
        }
    }

    //lichao add in 2017-04-19
    private void setSimTopViewMargins(Context context, boolean isShowSimIcon, boolean isTop) {
        if(null == mSimIconView){
            return;
        }
        if (isShowSimIcon && isTop) {
            MessageUtils.setMargins(context, mSimIconView, SIM_ICON_MARGIN_LEFT, 0, 0, 0);
        } else {
            MessageUtils.setMargins(context, mSimIconView, 0, 0, 0, 0);
        }
    }

    //lichao add
    private void inflateSimTopStubView() {
        if (null == mSimTopStubView) {
            ViewStub stub = (ViewStub) findViewById(R.id.sim_top_icon_stub);
            mSimTopStubView = stub.inflate();
        }
    }

    //lichao add
    private void setSubjectMargins(Context context, boolean isShowSimIcon, boolean isTop) {
        if(null == mSubjectView){
            return;
        }
        int marginRight = 0;
        if (isShowSimIcon && isTop) {
            marginRight += SIM_ICON_WIDTH + SIM_ICON_MARGIN_LEFT + TOP_ICON_WIDTH + SIM_STUB_MARGIN_LEFT;
        } else if (isShowSimIcon) {
            marginRight += SIM_ICON_WIDTH + SIM_STUB_MARGIN_LEFT;
        } else if (isTop) {
            marginRight += TOP_ICON_WIDTH + SIM_STUB_MARGIN_LEFT;
        }
        MessageUtils.setMargins(context, mSubjectView, 0, 0, marginRight, 0);
    }

    private void setSubjectLayoutMargins(Context context) {
        if(null == mSummaryLayout){
            return;
        }
        if (mIsCheckBoxMode) {
            MessageUtils.setMargins(context, mSummaryLayout,
                    SUBJECT_MARGIN_LEFT, 0, CHECK_BOX_TOTAL_WIDTH, SUBJECT_MARGIN_BOTTOM);
        }else {
            MessageUtils.setMargins(context, mSummaryLayout,
                    SUBJECT_MARGIN_LEFT, 0, 0, SUBJECT_MARGIN_BOTTOM);
        }
    }

    //lichao add
    private void updateFromViewMaxWidth(Context context) {
        int right_padding = LIST_ITEM_RIGHT_PADDING;
        //left: unread view
        int left_margins = UNREAD_VIEW_TOTAL_WIDTH;
        //right: date, sim, checkbox
        int right_margins = 0;
        if (null != mDateView && mDateView.getVisibility() == View.VISIBLE) {
            int width = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            int height = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            mDateView.measure(width, height);
            int dateViewWidth = mDateView.getMeasuredWidth();
            int dateViewWidthDip = MessageUtils.px2dip(context, dateViewWidth);
            right_margins += ( FROM_MARGIN_RIGHT + dateViewWidthDip);
        }
        if (null != mBlackView && mBlackView.getVisibility() == View.VISIBLE) {
            int width = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            int height = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            mBlackView.measure(width, height);
            int blackViewWidth = mBlackView.getMeasuredWidth();
            int blackViewWidthDip = MessageUtils.px2dip(context, blackViewWidth);
            if(DEBUG) Log.v(TAG, "updateFromViewMaxWidth, blackViewWidthDip = "+ blackViewWidthDip);
            right_margins += BLACK_MARGIN_LEFT + blackViewWidthDip+2;
        }
        if (mIsCheckBoxMode) {
            right_margins += CHECK_BOX_TOTAL_WIDTH;
        }
        int fromMaxWidthDip = mScreenWidthDip - right_padding - left_margins - right_margins;
        //if(DEBUG) Log.v(TAG, "updateFromViewMaxWidth, fromMaxWidthDip = "+ fromMaxWidthDip);
        mFromView.setMaxWidth(MessageUtils.dip2Px(context, fromMaxWidthDip));
    }

    //lichao add
    //delete for set mUnreadView right Margins instead
    private void setFromMargins(Context context, boolean hasUnread) {
        if(null == mFromView){
            return;
        }
        if (hasUnread) {
            MessageUtils.setMargins(context, mFromView, UNREAD_VIEW_MARGIN_RIGHT, 0, 0, 0);
        }else {
            MessageUtils.setMargins(context, mFromView, UNREAD_VIEW_TOTAL_WIDTH, 0, 0, 0);
        }
    }

    //lichao add
    private void setDateViewMargins(Context context) {
        if(null == mDateView){
             return;
        }
        if (mIsCheckBoxMode) {
            MessageUtils.setMargins(context, mDateView, 0, DATE_MARGIN_TOP, CHECK_BOX_TOTAL_WIDTH, 0);
        }else {
            MessageUtils.setMargins(context, mDateView, 0, DATE_MARGIN_TOP, 0, 0);
        }
    }
    //lichao add
    private int mScreenWidthDip = 0;
    public void setScreenWidthDip(int screenWidthDip) {
        mScreenWidthDip = screenWidthDip;
    }

    private int CHECK_BOX_MARGIN_LEFT = 20;
    private int CHECK_BOX_WIDTH = 24;
    private int CHECK_BOX_MARGIN_RIGHT = 4;
    private int CHECK_BOX_TOTAL_WIDTH = 48;

    private int UNREAD_VIEW_WIDTH = 8;
    private int UNREAD_VIEW_MARGIN_LEFT = 9;
    private int UNREAD_VIEW_MARGIN_RIGHT = 7;
    private int UNREAD_VIEW_TOTAL_WIDTH = 24;
    private int UNREAD_VIEW_MARGIN_TOP = 8;

    private int DATE_MARGIN_TOP = 13;
    
    private int SUBJECT_MARGIN_LEFT = 24;
    private int SUBJECT_MARGIN_BOTTOM = 13;

    private int SIM_ICON_WIDTH = 12;
    private int SIM_ICON_MARGIN_LEFT = 4;

    private int TOP_ICON_WIDTH = 12;

    private int SIM_STUB_MARGIN_TOP = 7;
    private int SIM_STUB_MARGIN_LEFT = 8;

    private int LIST_ITEM_RIGHT_PADDING = 16;

    private int FROM_MARGIN_RIGHT = 16;
    private int BLACK_MARGIN_LEFT = 3;

    private void initDimension(Context context){
        //getDimension() will return px value
        //UNREAD_VIEW_WIDTH = (int) context.getResources().getDimension(R.dimen.unread_width);
        //getXmlDef() return dip value
        UNREAD_VIEW_WIDTH = MessageUtils.getXmlDef(context, R.dimen.conv_list_unread_width);
        UNREAD_VIEW_MARGIN_LEFT = MessageUtils.getXmlDef(context, R.dimen.conv_list_unread_margin_left);
        //if(DEBUG) Log.d(TAG, "initDimension(), UNREAD_VIEW_MARGIN_LEFT = "+UNREAD_VIEW_MARGIN_LEFT);
        UNREAD_VIEW_MARGIN_RIGHT = MessageUtils.getXmlDef(context, R.dimen.conv_list_unread_margin_right);
        UNREAD_VIEW_TOTAL_WIDTH = UNREAD_VIEW_WIDTH + UNREAD_VIEW_MARGIN_LEFT + UNREAD_VIEW_MARGIN_RIGHT;
        UNREAD_VIEW_MARGIN_TOP = MessageUtils.getXmlDef(context, R.dimen.conv_list_unread_margin_top);

        DATE_MARGIN_TOP = MessageUtils.getXmlDef(context, R.dimen.conv_list_date_margin_top);

        SUBJECT_MARGIN_LEFT = MessageUtils.getXmlDef(context, R.dimen.conv_list_subject_margin_left);
        SUBJECT_MARGIN_BOTTOM = MessageUtils.getXmlDef(context, R.dimen.conv_list_subject_margin_bottom);

        SIM_ICON_WIDTH = MessageUtils.getXmlDef(context, R.dimen.conv_list_sim_width);
        SIM_ICON_MARGIN_LEFT = MessageUtils.getXmlDef(context, R.dimen.conv_list_sim_margin_left);

        TOP_ICON_WIDTH = MessageUtils.getXmlDef(context, R.dimen.conv_list_top_width);

        SIM_STUB_MARGIN_LEFT = MessageUtils.getXmlDef(context, R.dimen.conv_list_sim_stub_margin_left);
        SIM_STUB_MARGIN_TOP = MessageUtils.getXmlDef(context, R.dimen.conv_list_sim_stub_margin_top);

        CHECK_BOX_MARGIN_LEFT = MessageUtils.getXmlDef(context, R.dimen.conv_list_checkbox_width);
        CHECK_BOX_WIDTH = MessageUtils.getXmlDef(context, R.dimen.conv_list_checkbox_margin_left);
        CHECK_BOX_MARGIN_RIGHT = MessageUtils.getXmlDef(context, R.dimen.conv_list_checkbox_margin_right);
        CHECK_BOX_TOTAL_WIDTH = CHECK_BOX_MARGIN_LEFT + CHECK_BOX_WIDTH + CHECK_BOX_MARGIN_RIGHT;
        LIST_ITEM_RIGHT_PADDING = MessageUtils.getXmlDef(context, R.dimen.conv_list_item_right_padding);

        FROM_MARGIN_RIGHT = MessageUtils.getXmlDef(context, R.dimen.conv_list_from_margin_right);
        BLACK_MARGIN_LEFT = MessageUtils.getXmlDef(context, R.dimen.conv_list_black_margin_left);
    }
    //lichao add end

    //HB. Comments : for fix bug 3991, Engerineer : lichao , Date : 17-7-6 , begin
    public void setItemChecked(boolean selected) {
        if (mCheckBox != null && mCheckBox.getVisibility() == View.VISIBLE) {
            mCheckBox.setChecked(selected);
        }
    }
    //HB. end
}
