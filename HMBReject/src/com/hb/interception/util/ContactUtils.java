package com.hb.interception.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.hb.interception.InterceptionApplication;
import com.hb.interception.database.BlackItem;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.Contacts.Entity;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import com.hb.interception.R;

public class ContactUtils {
    public static String getContactNameByPhoneNumber(Context context, String address) {
        if (TextUtils.isEmpty(address) || address.length() <= 3) {
            return "";
        }
        String[] projection = {ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER};
        Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                ContactsContract.CommonDataKinds.Phone.NUMBER + " = '" + address + "'",
                null,
                null);
        if (cursor == null) {
            return null;
        }
        try {
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToPosition(i);
                int nameFieldColumnIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
                String name = cursor.getString(nameFieldColumnIndex);
                return name;
            }
            return null;
        } finally {
            cursor.close();
        }
    }

    public static List<BlackItem> getBlackItemListByDataId(Context context, long[] dataIds, List<String> blacklist) {
        if (dataIds == null || dataIds.length == 0) {
            return null;
        }
        String[] projection = {ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER,"_id","raw_contact_id"};
        StringBuilder extraSel = new StringBuilder();
        for(long dataId : dataIds) {
            extraSel.append(dataId + ",");
        }
        extraSel.deleteCharAt(extraSel.length() - 1);
        Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                ContactsContract.CommonDataKinds.Phone._ID + " IN (" + extraSel.toString() + ")",
                null,
                null);
        if (cursor == null) {
            return null;
        }
        ArrayList<BlackItem> blackItemList = new ArrayList<>();
    	List<String> nums = new ArrayList<String>();
        try {
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToPosition(i);
                int numberFieldColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                int nameFieldColumnIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME); 
                String number = cursor.getString(numberFieldColumnIndex);
                String name = cursor.getString(nameFieldColumnIndex);
                //
                number = number.replace("-", "").replace(" ", "");
                name = name.replace("-", "").replace(" ", "");
                long mDataId=cursor.getLong(cursor.getColumnIndex("_id"));
                long mRawContactId=cursor.getLong(cursor.getColumnIndex("raw_contact_id"));
                if (!TextUtils.isEmpty(number)) {
                    if (InterceptionUtils.isNoneDigit(number)) {
                        continue;
                    }
//                    if (blacklist != null && blacklist.contains(number)) {
//                        continue;
//                    } 
                 if (nums != null && nums.contains(getChineseVaildNumStr(number))) {
                      continue;
                  } 
                }
                nums.add(getChineseVaildNumStr(number));
                BlackItem blackItem = new BlackItem();
                blackItem.setmDataId(mDataId);
                blackItem.setmRawContactId(mRawContactId);
                blackItem.setmNumber(number);
                blackItem.setmBlackName(name);
                blackItemList.add(blackItem);
            }
            return blackItemList;
        } finally {
            cursor.close();
        }
    }
    
    public static List<BlackItem> getWhiteItemListByDataId(Context context, long[] dataIds, List<String> whitelist) {
    	return getBlackItemListByDataId(context, dataIds, whitelist);
    }
    
    public static boolean isSimContact(Context context, long rawContactId) {
    	Cursor cursor = null;
		try{
			cursor = context.getContentResolver()
					.query(RawContacts.CONTENT_URI,
							new String[] {"indicate_phone_or_sim_contact"}, "_id=" + rawContactId, null, null);
			if(cursor != null && cursor.moveToFirst()){
				int indicate_phone_or_sim_contact = cursor.getInt(0);
				if(indicate_phone_or_sim_contact >= 0){
					return true;
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(cursor!=null){
				cursor.close();
				cursor=null;
			}
		}
		return false;
	}
    
    public static List<BlackItem> getBlackItemListByNumber(Context context, List<String> selectNumbers, List<String> existBlacklist) {    	
        if (selectNumbers == null || selectNumbers.size() == 0) {
            return null;
        }
        if(existBlacklist != null) {
        	selectNumbers.removeAll(existBlacklist);
        }
        //like number is mail ...@qq.com sql will error replaceAll
        List<String> globalPhoneList = new ArrayList<String>();
        for(String number : selectNumbers) {
        	if(PhoneNumberUtils.isGlobalPhoneNumber(number)){
        		globalPhoneList.add(number);
        	}
        }
        ArrayList<BlackItem> blackItemList = new ArrayList<>();
        List<String> nums = new ArrayList<String>();
        if(globalPhoneList != null &&  globalPhoneList.size() >= 1) {
        	String[] projection = {ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER,"_id","raw_contact_id"};
        	StringBuilder extraSel = new StringBuilder();
        	for(String number : selectNumbers) {
        		if(PhoneNumberUtils.isGlobalPhoneNumber(number)){
        			extraSel.append(number + ",");
        		}
        	}
        	extraSel.deleteCharAt(extraSel.length() - 1);
        	String Text = BlackUtils.getPhoneNumberEqualString(extraSel.toString());
        	Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        			projection,
        			ContactsContract.CommonDataKinds.Phone.NUMBER + " IN (" + extraSel.toString() + ")",
        			null,
        			null);
        	if (cursor != null) {
        		try {
        			for (int i = 0; i < cursor.getCount(); i++) {
        				cursor.moveToPosition(i);
        				int numberFieldColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
        				int nameFieldColumnIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME); 
        				String number = cursor.getString(numberFieldColumnIndex);
        				selectNumbers.remove(number);
        				String name = cursor.getString(nameFieldColumnIndex);
        				number = number.replace("-", "").replace(" ", "");
        				name = name.replace("-", "").replace(" ", "");
        				long mDataId=cursor.getLong(cursor.getColumnIndex("_id"));
        				long mRawContactId=cursor.getLong(cursor.getColumnIndex("raw_contact_id"));
        				if (!TextUtils.isEmpty(number)) {
        					if (InterceptionUtils.isNoneDigit(number)) {
        						continue;
        					}
        				}
        				if (nums != null && nums.contains(getChineseVaildNumStr(number))) {
                            continue;
                        } 
        			    nums.add(getChineseVaildNumStr(number));
        				BlackItem blackItem = new BlackItem();
        				blackItem.setmDataId(mDataId);
        				blackItem.setmRawContactId(mRawContactId);
        				blackItem.setmNumber(number);
        				blackItem.setmBlackName(name);
        				blackItemList.add(blackItem);
        			}
        		} finally {
        			cursor.close();
        		}
        	}
        }
        for(String number : selectNumbers) {
            BlackItem blackItem = new BlackItem();
            blackItem.setmNumber(number);
            blackItem.setmBlackName(context.getString(R.string.sms_note));        	
            blackItemList.add(blackItem);
        }
        return blackItemList;
    }
    
	private static String getChineseVaildNumStr(String str) {       
    	if(str.length() >= 11) {      
    		return str.substring(str.length()-11,str.length()) ; 
    	} else     {        
    		return str;     
    		} 
    	}
}