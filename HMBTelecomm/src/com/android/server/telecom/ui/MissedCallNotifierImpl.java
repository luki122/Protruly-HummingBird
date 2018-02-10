/*
 * Copyright 2014, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.telecom.ui;

import java.util.HashMap;
import java.util.List;

import com.android.server.telecom.Call;
import com.android.server.telecom.CallState;
import com.android.server.telecom.CallerInfoAsyncQueryFactory;
import com.android.server.telecom.CallsManager;
import com.android.server.telecom.CallsManagerListenerBase;
import com.android.server.telecom.Constants;
import com.android.server.telecom.ContactsAsyncHelper;
import com.android.server.telecom.Log;
import com.android.server.telecom.MissedCallNotifier;
import com.android.server.telecom.R;
import com.android.server.telecom.TelecomBroadcastIntentProcessor;
import com.android.server.telecom.TelecomSystem;
import com.android.server.telecom.components.TelecomBroadcastReceiver;
import com.hb.utils.ContactsUtils;
import com.hb.utils.SubUtils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.CallLog.Calls;
import android.telecom.DisconnectCause;
import android.telecom.PhoneAccount;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.text.TextUtils;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionManager;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.RemoteViews;
import android.provider.ContactsContract.QuickContact;
import android.app.hb.TMSManager;
// TODO: Needed for move to system service: import com.android.internal.R;

/**
 * Creates a notification for calls that the user missed (neither answered nor rejected).
 *
 * TODO: Make TelephonyManager.clearMissedCalls call into this class.
 *
 * TODO: Reduce dependencies in this implementation; remove the need to create a new Call
 *     simply to look up caller metadata, and if possible, make it unnecessary to get a
 *     direct reference to the CallsManager. Try to make this class simply handle the UI
 *     and Android-framework entanglements of missed call notification.
 */
public class MissedCallNotifierImpl extends CallsManagerListenerBase implements MissedCallNotifier {

    private static final String[] CALL_LOG_PROJECTION = new String[] {
        Calls._ID,
        Calls.NUMBER,
        Calls.NUMBER_PRESENTATION,
        Calls.DATE,
        Calls.DURATION,
        Calls.TYPE,
        Calls.PHONE_ACCOUNT_ID //add by lgy for 3429114
    };

    private static final int CALL_LOG_COLUMN_ID = 0;
    private static final int CALL_LOG_COLUMN_NUMBER = 1;
    private static final int CALL_LOG_COLUMN_NUMBER_PRESENTATION = 2;
    private static final int CALL_LOG_COLUMN_DATE = 3;
    private static final int CALL_LOG_COLUMN_DURATION = 4;
    private static final int CALL_LOG_COLUMN_TYPE = 5;
    private static final int CALL_LOG_COLUMN_PHONE_ACCOUNT_ID = 6;     //add by lgy for 3429114

    private static final int MISSED_CALL_NOTIFICATION_ID = 1;

    private final Context mContext;
    private final NotificationManager mNotificationManager;

    // Used to track the number of missed calls.
    private int mMissedCallCount = 0;

    public MissedCallNotifierImpl(Context context) {
        mContext = context;
        mNotificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /** {@inheritDoc} */
    @Override
    public void onCallStateChanged(Call call, int oldState, int newState) {
        if (oldState == CallState.RINGING && newState == CallState.DISCONNECTED &&
                call.getDisconnectCause().getCode() == DisconnectCause.MISSED) {
            showMissedCallNotification(call);
        }
    }

    /** Clears missed call notification and marks the call log's missed calls as read. */
    @Override
    public void clearMissedCalls() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                // Clear the list of new missed calls from the call log.
                ContentValues values = new ContentValues();
                values.put(Calls.NEW, 0);
                values.put(Calls.IS_READ, 1);
                StringBuilder where = new StringBuilder();
                where.append(Calls.NEW);
                where.append(" = 1 AND ");
                where.append(Calls.TYPE);
                where.append(" = ?");
                try {
                    mContext.getContentResolver().update(Calls.CONTENT_URI, values,
                            where.toString(), new String[]{ Integer.toString(Calls.
                            MISSED_TYPE) });
                } catch (IllegalArgumentException e) {
                    Log.w(this, "ContactsProvider update command failed", e);
                }
            }
        });
        cancelMissedCallNotification();
    }

    /**
     * Create a system notification for the missed call.
     *
     * @param call The missed call.
     */
    @Override
    public void showMissedCallNotification(Call call) {
    	//add by lgy
    	if(TelecomSystem.isHbUI) {
    		showMissedCallNotificationHb(call);
    		return;
    	}
        mMissedCallCount++;

        final int titleResId;
        final String expandedText;  // The text in the notification's line 1 and 2.

        // Display the first line of the notification:
        // 1 missed call: <caller name || handle>
        // More than 1 missed call: <number of calls> + "missed calls"
        if (mMissedCallCount == 1) {
            titleResId = R.string.notification_missedCallTitle;
            expandedText = getNameForCall(call);
        } else {
            titleResId = R.string.notification_missedCallsTitle;
            expandedText =
                    mContext.getString(R.string.notification_missedCallsMsg, mMissedCallCount);
        }

        // Create the notification.
        Notification.Builder builder = new Notification.Builder(mContext);
        builder.setSmallIcon(android.R.drawable.stat_notify_missed_call)
                .setColor(mContext.getResources().getColor(R.color.theme_color))
                .setWhen(call.getCreationTimeMillis())
                .setContentTitle(mContext.getText(titleResId))
                .setContentText(expandedText)
                .setContentIntent(createCallLogPendingIntent())
                .setAutoCancel(true)
                .setDeleteIntent(createClearMissedCallsPendingIntent());

        Uri handleUri = call.getHandle();
        String handle = handleUri == null ? null : handleUri.getSchemeSpecificPart();

        // Add additional actions when there is only 1 missed call, like call-back and SMS.
        if (mMissedCallCount == 1) {
            Log.d(this, "Add actions with number %s.", Log.piiHandle(handle));

            if (!TextUtils.isEmpty(handle)
                    && !TextUtils.equals(handle, mContext.getString(R.string.handle_restricted))) {
                String subId = getSubIdFromCall(call);
                builder.addAction(R.drawable.ic_phone_24dp,
                        mContext.getString(R.string.notification_missedCall_call_back),
                        createCallBackPendingIntent(handleUri, subId));

                builder.addAction(R.drawable.ic_message_24dp,
                        mContext.getString(R.string.notification_missedCall_message),
                        createSendSmsFromNotificationPendingIntent(handleUri));
            }

            Bitmap photoIcon = call.getPhotoIcon();
            if (photoIcon != null) {
                builder.setLargeIcon(photoIcon);
            } else {
                Drawable photo = call.getPhoto();
                if (photo != null && photo instanceof BitmapDrawable) {
                    builder.setLargeIcon(((BitmapDrawable) photo).getBitmap());
                }
            }
        } else {
            Log.d(this, "Suppress actions. handle: %s, missedCalls: %d.", Log.piiHandle(handle),
                    mMissedCallCount);
        }

        Notification notification = builder.build();
        configureLedOnNotification(notification);

        Log.i(this, "Adding missed call notification for %s.", call);
        long token = Binder.clearCallingIdentity();
        try {
            mNotificationManager.notifyAsUser(
                    null /* tag */, MISSED_CALL_NOTIFICATION_ID, notification, UserHandle.CURRENT);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /** Cancels the "missed call" notification. */
    private void cancelMissedCallNotification() {
    	//add by lgy
    	if(TelecomSystem.isHbUI) {
    		cancelMissedCallNotificationHb();
    		return;
    	}
        // Reset the number of missed calls to 0.
        mMissedCallCount = 0;
        long token = Binder.clearCallingIdentity();
        try {
            mNotificationManager.cancelAsUser(null, MISSED_CALL_NOTIFICATION_ID,
                    UserHandle.CURRENT);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /**
     * Returns the name to use in the missed call notification.
     */
    private String getNameForCall(Call call) {
        String handle = call.getHandle() == null ? null : call.getHandle().getSchemeSpecificPart();
        String name = call.getName();

        if (!TextUtils.isEmpty(name) && TextUtils.isGraphic(name)) {
            return name;
        } else if (!TextUtils.isEmpty(handle)) {
            // A handle should always be displayed LTR using {@link BidiFormatter} regardless of the
            // content of the rest of the notification.
            // TODO: Does this apply to SIP addresses?
            BidiFormatter bidiFormatter = BidiFormatter.getInstance();
            return bidiFormatter.unicodeWrap(handle, TextDirectionHeuristics.LTR);
        } else {
            // Use "unknown" if the call is unidentifiable.
            return mContext.getString(R.string.unknown);
        }
    }

    /**
     * Creates a new pending intent that sends the user to the call log.
     *
     * @return The pending intent.
     */
    private PendingIntent createCallLogPendingIntent() {
        Intent intent = new Intent(Intent.ACTION_VIEW, null);
        intent.setType(Calls.CONTENT_TYPE);

        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(mContext);
        taskStackBuilder.addNextIntent(intent);

        return taskStackBuilder.getPendingIntent(0, 0, null, UserHandle.CURRENT);
    }

    /**
     * Creates an intent to be invoked when the missed call notification is cleared.
     */
    private PendingIntent createClearMissedCallsPendingIntent() {
        return createTelecomPendingIntent(
                TelecomBroadcastIntentProcessor.ACTION_CLEAR_MISSED_CALLS, null);
    }

    /**
     * Creates an intent to be invoked when the user opts to "call back" from the missed call
     * notification.
     *
     * @param handle The handle to call back.
     */
    //modify by lgy for 3429114 start
    private PendingIntent createCallBackPendingIntent(Uri handle,String subId) {
        return createTelecomPendingIntent(
                TelecomBroadcastIntentProcessor.ACTION_CALL_BACK_FROM_NOTIFICATION, handle, subId);
    }
    //modify by lgy for 3429114 end

    /**
     * Creates an intent to be invoked when the user opts to "send sms" from the missed call
     * notification.
     */
    private PendingIntent createSendSmsFromNotificationPendingIntent(Uri handle) {
        return createTelecomPendingIntent(
                TelecomBroadcastIntentProcessor.ACTION_SEND_SMS_FROM_NOTIFICATION,
                Uri.fromParts(Constants.SCHEME_SMSTO, handle.getSchemeSpecificPart(), null));
    }

    /**
     * Creates generic pending intent from the specified parameters to be received by
     * {@link TelecomBroadcastIntentProcessor}.
     *
     * @param action The intent action.
     * @param data The intent data.
     */
    private PendingIntent createTelecomPendingIntent(String action, Uri data) {
        Intent intent = new Intent(action, data, mContext, TelecomBroadcastReceiver.class);
        return PendingIntent.getBroadcast(mContext, 0, intent, 0);
    }

    /**
     * Configures a notification to emit the blinky notification light.
     */
    private void configureLedOnNotification(Notification notification) {
        notification.flags |= Notification.FLAG_SHOW_LIGHTS;
        notification.defaults |= Notification.DEFAULT_LIGHTS;
    }

    /**
     * Adds the missed call notification on startup if there are unread missed calls.
     */
    @Override
    public void updateOnStartup(
            final TelecomSystem.SyncRoot lock,
            final CallsManager callsManager,
            final ContactsAsyncHelper contactsAsyncHelper,
            final CallerInfoAsyncQueryFactory callerInfoAsyncQueryFactory) {
        Log.d(this, "updateOnStartup()...");

        // instantiate query handler
        AsyncQueryHandler queryHandler = new AsyncQueryHandler(mContext.getContentResolver()) {
            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                Log.d(MissedCallNotifierImpl.this, "onQueryComplete()...");
                if (cursor != null) {
                    try {
                        while (cursor.moveToNext()) {
                            // Get data about the missed call from the cursor
                            final String handleString = cursor.getString(CALL_LOG_COLUMN_NUMBER);
                            final int presentation =
                                    cursor.getInt(CALL_LOG_COLUMN_NUMBER_PRESENTATION);
                            final long date = cursor.getLong(CALL_LOG_COLUMN_DATE);

                            final Uri handle;
                            if (presentation != Calls.PRESENTATION_ALLOWED
                                    || TextUtils.isEmpty(handleString)) {
                                handle = null;
                            } else {
                                handle = Uri.fromParts(PhoneNumberUtils.isUriNumber(handleString) ?
                                        PhoneAccount.SCHEME_SIP : PhoneAccount.SCHEME_TEL,
                                                handleString, null);
                            }

                            synchronized (lock) {

                                // Convert the data to a call object
                                Call call = new Call(mContext, callsManager, lock,
                                        null, contactsAsyncHelper, callerInfoAsyncQueryFactory,
                                        null, null, null, null, true, false);
                                call.setDisconnectCause(
                                        new DisconnectCause(DisconnectCause.MISSED));
                                call.setState(CallState.DISCONNECTED, "throw away call");
                                call.setCreationTimeMillis(date);
                                
                                //add by lgy for 3429114 start
                                call.setTargetPhoneAccount(getPhoneAccountHandle(cursor.getString(CALL_LOG_COLUMN_PHONE_ACCOUNT_ID)));
                                //add by lgy for 3429114 end

                                // Listen for the update to the caller information before posting
                                // the notification so that we have the contact info and photo.
                                call.addListener(new Call.ListenerBase() {
                                    @Override
                                    public void onCallerInfoChanged(Call call) {
                                        call.removeListener(
                                                this);  // No longer need to listen to call
                                        // changes after the contact info
                                        // is retrieved.
                                        showMissedCallNotification(call);
                                    }
                                });
                                // Set the handle here because that is what triggers the contact
                                // info query.
                                call.setHandle(handle, presentation);
                            }
                        }
                    } finally {
                        cursor.close();
                    }
                }
            }
        };

        // setup query spec, look for all Missed calls that are new.
        StringBuilder where = new StringBuilder("type=");
        where.append(Calls.MISSED_TYPE);
        where.append(" AND new=1");
        where.append(" AND is_read=0");

        // start the query
        queryHandler.startQuery(0, null, Calls.CONTENT_URI, CALL_LOG_PROJECTION,
                where.toString(), null, Calls.DEFAULT_SORT_ORDER);
    }
    
    //add by lgy 
    private String getSubIdFromCall(Call call) {
        Log.w(this, "getSubIdFromCall subId call = " + call);
//        String subId = null;
//        if(call.getTargetPhoneAccount() != null) {
//            subId = call.getTargetPhoneAccount().getId();
//            Log.w(this, "getSubIdFromCall subId = " + subId);
//        }
//      return subId;
        return call.getSubId() + "";
    }
    
    private PendingIntent createTelecomPendingIntent(String action, Uri data,  String subId) {
        Intent intent = new Intent(action, data, mContext, TelecomBroadcastReceiver.class);
        if(!TextUtils.isEmpty(subId))  {
            try {
                intent.putExtra("slot", SubscriptionManager.getSlotId(Integer.valueOf(subId)));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        return PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
    
    private PhoneAccountHandle getPhoneAccountHandle(String subId) {
        if(TextUtils.isEmpty(subId)) {
            return null;
        }
        TelecomManager telecomManager = TelecomManager.from(mContext);
        List<PhoneAccountHandle> accounts = telecomManager.getCallCapablePhoneAccounts();
        for (PhoneAccountHandle account : accounts) {
            if (subId.equals(account.getId())) {
                return account;
            }
        }
        return null;
    }
    
    private HashMap<String, Integer> mCallToIdList = new HashMap<String, Integer>();
    private HashMap<String, Integer> mCallCountList = new HashMap<String, Integer>();
    
    private String getNumberForCall(Call call) {
        String handle = call.getHandle() == null ? null : call.getHandle().getSchemeSpecificPart();

       if (!TextUtils.isEmpty(handle)) {
            // A handle should always be displayed LTR using {@link BidiFormatter} regardless of the
            // content of the rest of the notification.
            // TODO: Does this apply to SIP addresses?
//            BidiFormatter bidiFormatter = BidiFormatter.getInstance();
//            return bidiFormatter.unicodeWrap(handle, TextDirectionHeuristics.LTR);
    	    return handle;
        } else {
            // Use "unknown" if the call is unidentifiable.
            return mContext.getString(R.string.unknown);
        }
    }

	private String CaluText(String str, Integer count) {
		TextPaint mTextPaint = new TextPaint();
		DisplayMetrics mDm = mContext.getResources().getDisplayMetrics();
		Configuration mConfiguration = mContext.getResources().getConfiguration();
		int displayWidth =  420 ;
		String nameOrNum = str;
		float expandedTextWidth = mTextPaint.measureText(nameOrNum)* mDm.density * mConfiguration.fontScale;
		StringBuilder titleBuild1 = new StringBuilder();
		String countStr = titleBuild1.append("...").append(" (").append(count.toString()).append(")").toString();
		float countStrTextWidth = mTextPaint.measureText(countStr) * mDm.density
				* mConfiguration.fontScale;
		StringBuilder titleBuild = new StringBuilder();
		if (countStrTextWidth + expandedTextWidth > displayWidth) {
			int textLength = nameOrNum.length();
			for (int i = textLength - 1; i > 1; i--) {
				nameOrNum = nameOrNum.substring(0, i);
				expandedTextWidth = mTextPaint.measureText(nameOrNum)
						* mDm.density * mConfiguration.fontScale;
				if (countStrTextWidth + expandedTextWidth < displayWidth) {
					break;
				}
			}
			titleBuild.append(nameOrNum);
			titleBuild.append("...");
		} else {
			titleBuild.append(nameOrNum);
		}
		titleBuild.append(" (").append(count.toString()).append(")");
		return titleBuild.toString();
	}
    
    private void showMissedCallNotificationHb(final Call call) {  
    	String number = getNumberForCall(call); 
    	Integer count = mCallCountList.get(number);
    	if(count == null) {
    		count = 1;
    		mCallCountList.put(number, 1);
    	} else {
    		count ++;
    		mCallCountList.put(number, count);
    	}
    	
    	Integer id = mCallToIdList.get(number);
    	if(id == null) {
    		id = mCallToIdList.size() +1;
        	mCallToIdList.put(number, id);
    	}
    	
        Log.i(this, "Adding missed call notification number = " + number); 
        Log.i(this, "Adding missed call notification notificationid = " + id); 

        final int titleResId;
        String expandedText; 
        expandedText = getNameForCall(call);
        if (count == 1) {
            titleResId = R.string.notification_missedCallTitle;
        } else {
            titleResId = R.string.notification_missedCallsTitle;
        }
        boolean isNameExit = !call.nameIsNum();
        if (count > 1)  {
        	expandedText = CaluText(expandedText, count);
        }
        
        String title = mContext.getString(titleResId) ;
		if (!isNameExit) {
			String location;
			if (!TextUtils.isEmpty(number)) {
				TMSManager mTMSManager = (TMSManager) mContext
						.getSystemService(TMSManager.TMS_SERVICE);
				if (mTMSManager != null) {
					try {
						location = mTMSManager.getLocation(number);
						StringBuilder build = new StringBuilder(title);
						build.append("   ");
						build.append(location);
						title = build.toString();
					} catch (Exception e) {
						new Handler().postDelayed(new Runnable() {
							public void run() {
								showMissedCallNotification(call);
							}
						}, 15000);
					}
				}
			}
		} else {
			StringBuilder build = new StringBuilder(title);
			build.append("   ");
			build.append(number);
			title = build.toString();
		}
        String subId = getSubIdFromCall(call);
        int smallIconRes = SubUtils.getSimRes(Integer.valueOf(subId));
        if(smallIconRes == 0) {
        	smallIconRes = android.R.drawable.stat_notify_missed_call;
        }

        // Create the notification.
        Notification.Builder builder = new Notification.Builder(mContext);
//        builder.setSmallIcon(android.R.drawable.stat_notify_missed_call)
        builder.setSmallIcon(smallIconRes)
                .setColor(mContext.getResources().getColor(R.color.theme_color))
                .setWhen(call.getCreationTimeMillis())
                .setContentTitle(expandedText)
                .setContentText(title)
                .setContentIntent(createCallLogPendingIntent())
                .setAutoCancel(true)
                .setDeleteIntent(createClearMissedCallsPendingIntentHb(id));

        Uri handleUri = call.getHandle();
        String handle = handleUri == null ? null : handleUri.getSchemeSpecificPart();

        if (!TextUtils.isEmpty(handle)
                && !TextUtils.equals(handle, mContext.getString(R.string.handle_restricted))) {
            //got to call Detail
//            builder.addAction(R.drawable.ic_more_vert_24dp,
        	builder.addAction(0,
                    mContext.getString(R.string.notification_show_missedCall_message),
                    createShowCallDetailIntent(handleUri, id));
            
            //Call back
//            builder.addAction(R.drawable.ic_phone_24dp,
        	builder.addAction(0,
                    mContext.getString(R.string.notification_missedCall_call_back),
                    createCallBackPendingIntentHb(handleUri, id, subId));
            
            //delete call Log
//            builder.addAction(R.drawable.ic_delete,
        	builder.addAction(0,
                    mContext.getString(R.string.notification_delete_missedCall_message),
                    createDeleteCallLogIntent(handleUri, id));            
            
        }
//zhangcj delete for 2839
//        Bitmap photoIcon = call.getPhotoIcon();
//        if (photoIcon != null) {
//            builder.setLargeIcon(photoIcon);
//        } else {
//            Drawable photo = call.getPhoto();
//            if (photo != null && photo instanceof BitmapDrawable) {
//                builder.setLargeIcon(((BitmapDrawable) photo).getBitmap());
//            }
//        }

        Notification notification = builder.build();
        configureLedOnNotification(notification);

        Log.i(this, "Adding missed call notification for %s.", call);
        long token = Binder.clearCallingIdentity();
        try {
            mNotificationManager.notifyAsUser(
                    null /* tag */, id, notification, UserHandle.CURRENT);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }
    
    public void showMissedCallCustomNotificationHb(Call call) {  
    	String number = getNumberForCall(call); 
    	Integer count = mCallCountList.get(number);
    	if(count == null) {
    		mCallCountList.put(number, 1);
    	} else {
    		count ++;
    		mCallCountList.put(number, count);
    	}
    	
    	Integer id = mCallToIdList.get(number);
    	if(id == null) {
    		id = mCallToIdList.size() +1;
        	mCallToIdList.put(number, id);
    	}
    	
        Log.i(this, "Adding missed call notification number = " + number); 
        Log.i(this, "Adding missed call notification notificationid = " + id); 

        final int titleResId;
        final String expandedText; 

        expandedText = getNameForCall(call);
        if (count == 1) {
            titleResId = R.string.notification_missedCallTitle;
        } else {
            titleResId = R.string.notification_missedCallsTitle;
        }
               
        
        Notification.Builder builder = new Notification.Builder(mContext);
		builder.setSmallIcon(android.R.drawable.stat_notify_missed_call)
				.setColor(mContext.getResources().getColor(R.color.theme_color))
				.setWhen(call.getCreationTimeMillis())
				.setContentTitle(mContext.getText(titleResId))
				.setContentText(expandedText).setAutoCancel(true)
				.setDeleteIntent(createClearMissedCallsPendingIntentHb(id));


        Notification notification = builder.getNotification();
        RemoteViews contentView = new RemoteViews(mContext.getPackageName(), R.layout.custom_misscall_notification); 
        Bitmap notifyIcon = BitmapFactory.decodeResource(mContext.getResources(), android.R.drawable.stat_notify_missed_call); 
    	contentView.setImageViewBitmap(R.id.image, notifyIcon);
        contentView.setTextViewText(R.id.title, mContext.getText(titleResId)); 
        contentView.setTextViewText(R.id.text, expandedText);
        String subId = getSubIdFromCall(call);
        
        int slot = SubUtils.getSlotBySubId(Integer.valueOf(subId));
        if(SubUtils.isDoubleCardInsert()) {
             contentView.setImageViewResource(R.id.sim_slot, slot > 0 ? R.drawable.sim_icon_2
 					: R.drawable.sim_icon_1);
		     contentView.setViewVisibility(R.id.sim_slot, View.VISIBLE);
		 } else {
			 contentView.setImageViewResource(R.id.sim_slot, 0);
		     contentView.setViewVisibility(R.id.sim_slot, View.GONE);
		 }
          notification.contentIntent =  createCallLogPendingIntent();
        

        Uri handleUri = call.getHandle();
        String handle = handleUri == null ? null : handleUri.getSchemeSpecificPart();
        if (!TextUtils.isEmpty(handle)
                  && !TextUtils.equals(handle, mContext.getString(R.string.handle_restricted))) {
            contentView.setOnClickPendingIntent(R.id.detail, createShowCallDetailIntent(handleUri, id));
            contentView.setOnClickPendingIntent(R.id.dial, createCallBackPendingIntentHb(handleUri, id, subId));
            contentView.setOnClickPendingIntent(R.id.delete, createDeleteCallLogIntent(handleUri, id));
        } 
        
        notification.contentView = contentView; 
        configureLedOnNotification(notification);
        
        Log.i(this, "Adding missed call notification for %s.", call);
        long token = Binder.clearCallingIdentity();
        try {
            mNotificationManager.notifyAsUser(
                    null /* tag */, id, notification, UserHandle.CURRENT);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }
    
    private void cancelMissedCallNotificationHb() {
    	mCallCountList.clear();
        long token = Binder.clearCallingIdentity();
        try {
        	for (int id : mCallToIdList.values()) {
                mNotificationManager.cancelAsUser(null, id,
                        UserHandle.CURRENT);
        	}
        } finally {
            Binder.restoreCallingIdentity(token);
        }
        mCallToIdList.clear();
    }
    
    private PendingIntent createClearMissedCallsPendingIntentHb(int notificationId) {
        return createTelecomPendingIntentHb(
                TelecomBroadcastIntentProcessor.ACTION_CLEAR_MISSED_CALLS, null, notificationId);
    }
    
    private PendingIntent createCallBackPendingIntentHb(Uri handle, int notificationId, String subId) {
        return createTelecomPendingIntentHb(
                TelecomBroadcastIntentProcessor.ACTION_CALL_BACK_FROM_NOTIFICATION, handle, notificationId, subId);
    }
    
    private PendingIntent createSendSmsFromNotificationPendingIntentHb(Uri handle, int notificationId) {
        return createTelecomPendingIntentHb(
                TelecomBroadcastIntentProcessor.ACTION_SEND_SMS_FROM_NOTIFICATION,
                Uri.fromParts(Constants.SCHEME_SMSTO, handle.getSchemeSpecificPart(), null) , notificationId);
    }
    
    private PendingIntent createDeleteCallLogIntent(Uri handle, int notificationId) {
        return createTelecomPendingIntentHb(
                TelecomBroadcastIntentProcessor.ACTION_DELETE_CALLLOG, null, notificationId);
    }
    
    private PendingIntent createShowCallDetailIntent(Uri handle, int notificationId) {
        return createTelecomPendingIntentHb(
                TelecomBroadcastIntentProcessor.ACTION_SHOW_CALLLOG_DETAIL, null, notificationId);
    }
    
    private PendingIntent createTelecomPendingIntentHb(String action, Uri data, int notificationId) {
          return createTelecomPendingIntentHb(action, data, notificationId, null);
    }
    
    private PendingIntent createTelecomPendingIntentHb(String action, Uri data,  int notificationId, String subId) {
        Intent intent = new Intent(action, data, mContext, TelecomBroadcastReceiver.class);
        intent.putExtra("notificationId", notificationId);
        if(!TextUtils.isEmpty(subId))  {
            try {
                intent.putExtra("slot", SubscriptionManager.getSlotId(Integer.valueOf(subId)));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        return PendingIntent.getBroadcast(mContext, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
    
    @Override
    public void clearMissedCallsById(final int notificationId) {
    	if(notificationId == -1) {
    		clearMissedCalls();
    		return;
    	}
		final String number = getNumberByNotificationId(notificationId);
    	if(TextUtils.isEmpty(number)) return;
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                // Clear the list of new missed calls from the call log.
                ContentValues values = new ContentValues();
                values.put(Calls.NEW, 0);
                values.put(Calls.IS_READ, 1);
                StringBuilder where = new StringBuilder();
                where.append(Calls.NEW);
                where.append(" = 1 AND ");
                where.append(Calls.NUMBER);
                where.append(" = '" + number + "' AND ");
                where.append(Calls.TYPE);
                where.append(" = ?");
                try {
                    mContext.getContentResolver().update(Calls.CONTENT_URI, values,
                            where.toString(), new String[]{ Integer.toString(Calls.
                            MISSED_TYPE) });
                } catch (IllegalArgumentException e) {
                    Log.w(this, "ContactsProvider update command failed", e);
                }
            }
        });
		clearMissedCallHb(number, notificationId);
    }
    
    @Override
    public void  deleteCallLogById(final int notificationId) {
		final String number = getNumberByNotificationId(notificationId);
    	if(TextUtils.isEmpty(number)) return;
        final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                StringBuilder where = new StringBuilder();
                where.append(Calls.NEW);
                where.append(" = 1 AND ");
                where.append(Calls.NUMBER);
                where.append(" = '" + number + "' AND ");
                where.append(Calls.TYPE);
                where.append(" = ?");
                try {
                    mContext.getContentResolver().delete(Calls.CONTENT_URI,
                            where.toString(), new String[]{ Integer.toString(Calls.
                            MISSED_TYPE) });
                } catch (IllegalArgumentException e) {
                    Log.w(this, "ContactsProvider update command failed", e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
        		clearMissedCallHb(number, notificationId);
            }
        };
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    
    @Override
    public void showCallLogDetailById(final int notificationId) {
    	final String number = getNumberByNotificationId(notificationId);
    	if(TextUtils.isEmpty(number)) return;

    	Uri contactUri = ContactsUtils.queryContactUriByPhoneNumber(mContext, number);
		if(contactUri != null){
			Rect v = null;
			QuickContact.showQuickContact(mContext, v, contactUri,
					QuickContact.MODE_LARGE, null);
		} else {
			Intent intent = new Intent();
			intent.setClassName("com.android.dialer", "com.android.dialer.CallDetailActivity");
			intent.putExtra("EXTRA_NUMBER", number);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mContext.startActivity(intent);
		}
    	
    }    
    
    private String getNumberByNotificationId(int notificationId) {
      	for(String key :mCallToIdList.keySet()) {
    		if(mCallToIdList.get(key) == notificationId) {
    			return key;    			
    		}
    	}
      	return null;
    } 
    
    private void clearMissedCallHb(String number, int notificationId) {
        mCallToIdList.remove(number);
        mCallCountList.remove(number);
        long token = Binder.clearCallingIdentity();
        try {
            mNotificationManager.cancelAsUser(null, notificationId,
                    UserHandle.CURRENT);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }
}
