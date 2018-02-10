package com.hmb.manager.tms;

import java.util.ArrayList;

import com.zzz.provider.Telephony.Mms;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;

/**
 * tangyisen add
 */

public class BlackChangedService extends Service {
    private static final String TAG = "BlackChangedService";
    public final static Uri BLACK_SMS_URI = Uri.parse("content://mms-sms/blackRecoveredSmsmms");
    public final static String INTENT_EXTRA_BLACK_NUMS = "hb_delete_black_nums";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String[] numbers = intent.getStringArrayExtra( INTENT_EXTRA_BLACK_NUMS );
        Thread thread = new Thread( new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                deleteSmsBlack(numbers);
            }
        } );
        thread.start();
        return Service.START_NOT_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void deleteSmsBlack(String[] numbers) {
        ContentResolver cr = getContentResolver();
        Uri.Builder uriBuilder = BLACK_SMS_URI.buildUpon();
        for (String recipient : numbers) {
            if (Mms.isEmailAddress(recipient)) {
                recipient = Mms.extractAddrSpec(recipient);
            }
            uriBuilder.appendQueryParameter("recipient", recipient);
        }
        //uriBuilder.appendQueryParameter("recipient", recipients);
        Uri uri = uriBuilder.build();
        ContentValues cv = new ContentValues();
        cv.put("reject", 0);
        cv.put("reject_tag", "");
        cr.update(uri, cv, null, null);
    }
    /*protected void deleteSingleBlack(String number) {
        Uri.Builder uriBuilder = BLACK_SINGLE_URI.buildUpon();
        ContentResolver cr = getContentResolver();
        uriBuilder.appendQueryParameter("recipient", number);
        Uri uri = uriBuilder.build();
        ContentValues cv = new ContentValues();
        cv.put("reject", 0);
        cv.put("reject_tag", "");
        cr.update(uri, cv, null, null);
    }

    protected void deleteMultiBlack(ArrayList<String> numbers) {

        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        for (String num : numbers) {
            ops.add(getDeleteOperation(num));
        }
        try {
            getContentResolver().applyBatch(BLACK_SMS_URI,
                    ops);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }
    }

    protected ContentProviderOperation getDeleteOperation(String number) {
        return ContentProviderOperation.newUpdate(BLACK_MULTI_URI)
                .withValue("reject", 0)
                .withValue("reject_tag", "")
                .withYieldAllowed(true)
                .build();
    }*/
}
