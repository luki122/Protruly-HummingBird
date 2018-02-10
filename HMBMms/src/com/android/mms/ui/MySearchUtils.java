package com.android.mms.ui;

import android.content.Context;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

import com.android.mms.data.Contact;
import com.android.mms.data.ContactList;
import com.android.mms.data.Conversation;

//this class add by lichao for Mms Search
public class MySearchUtils {

    private static final String TAG = "Mms/MySearchUtils";
    private static final boolean DEBUG = false;

    //this can get multi recipients String
    public static String getRecipientsStrByThreadId(Context context, long threadId) {
        if (threadId <= 0) {
            Log.w(TAG, "getRecipientsStrByThreadId, invalid threadId, return");
            return "";
        }
        Conversation conv = Conversation.query(context, threadId, true/*allowQuery*/);
        if (null == conv) {
            Log.w(TAG, "getRecipientsStrByThreadId, null conv, return");
            return "";
        }
        //这样“重新获取收信人”就可以获取到该会话的所有联系人，包括群发信息的情况
        //lichao modify in 2017-04-26
        String titleString = getRecipientsStrByContactList(conv.getRecipients());
        if (DEBUG) Log.d(TAG, "getRecipientsStrByThreadId(), titleString: " + titleString);
        return titleString;
    }

    public static ContactList getRecipientsByThreadId(Context context, long threadId) {
        if (threadId <= 0) {
            Log.w(TAG, "getRecipientsStrByThreadId, invalid threadId, return");
            return null;
        }
        Conversation conv = Conversation.query(context, threadId, true/*allowQuery*/);
        if (null == conv) {
            Log.w(TAG, "getRecipientsStrByThreadId, null conv, return");
            return null;
        }
        ContactList recipients = conv.getRecipients();
        if (DEBUG) Log.d(TAG, "getRecipientsByThreadId(), recipients: " + recipients);
        return recipients;
    }

    public static String getRecipientsStrByContactList(ContactList contactList) {
        if (null == contactList) {
            Log.w(TAG, "getRecipientsStrByContactList, null contactList, return");
            return "";
        }
        String namesAndNumbers = contactList.formatNamesAndNumbers(",");
        //namesAndNumbers: 哈哈1116 <18866661116>
        if (DEBUG) Log.d(TAG, "\n\n getRecipientsStrByContactList(), namesAndNumbers: " + namesAndNumbers);

        String replaceNumbers = namesAndNumbers.replace(" ", "");
        //replaceNumbers = 哈哈1116<18866661116>
        if(DEBUG) Log.d(TAG, "getRecipientsStrByContactList(), replaceNumbers = " + replaceNumbers);

        //String strippedNumber = PhoneNumberUtils.stripSeparators(namesAndNumbers);
        ////strippedNumber = 111618866661116
        //if(DEBUG) Log.d(TAG, "getRecipientsStrByContactList(), strippedNumber = " + strippedNumber);

        boolean isNumber = MessageUtils.isNumeric(replaceNumbers);
        if (!isNumber) {
            return replaceNumbers;
        }
        String normalNumbers = PhoneNumberUtils.normalizeNumber(replaceNumbers);
        if(DEBUG) Log.d(TAG, "getRecipientsStrByContactList(), normalNumbers = " + normalNumbers);

        //String phoneNumberNo86 = StringUtils.getPhoneNumberNo86(normalNumber);
        //if (DEBUG) Log.d(TAG, "getRecipientsStrByContactList(), phoneNumberNo86 = " + phoneNumberNo86);
        return normalNumbers;
    }

    //lichao add in 2017-04-26
    public static String getRecipientsNamesByContactList(ContactList contactList) {
        if (null == contactList) {
            Log.w(TAG, "getRecipientsNameByContactList, null contactList, return");
            return "";
        }
        String names = contactList.formatNames(",");
        if (DEBUG) Log.d(TAG, "\n\n getRecipientsNameByContactList(), names: " + names);

        String replaceNames = names.replace(" ", "");
        if(DEBUG) Log.d(TAG, "getRecipientsNameByContactList(), replaceNames = " + replaceNames);

        //only is number if the contact not storaged
        boolean isNumber = MessageUtils.isNumeric(replaceNames);
        if (!isNumber) {
            return replaceNames;
        }
        String normaNames = PhoneNumberUtils.normalizeNumber(replaceNames);
        if(DEBUG) Log.d(TAG, "getRecipientsNameByContactList(), normaNames = " + normaNames);

        return normaNames;
    }

    //lichao add in 2017-04-26
    public static String getRecipientsNumbersByContactList(ContactList contactList) {
        if (null == contactList) {
            Log.w(TAG, "getRecipientsNameByContactList, null contactList, return");
            return "";
        }
        String numbers = contactList.formatNumbers(",");
        if (DEBUG) Log.d(TAG, "\n\n getRecipientsNameByContactList(), numbers: " + numbers);

        String replaceNumbers = numbers.replace(" ", "");
        if(DEBUG) Log.d(TAG, "getRecipientsNameByContactList(), replaceNumbers = " + replaceNumbers);

        //only is number if the contact not storaged
        boolean isNumber = MessageUtils.isNumeric(replaceNumbers);
        if (!isNumber) {
            return replaceNumbers;
        }
        String normaNumbers = PhoneNumberUtils.normalizeNumber(replaceNumbers);
        if(DEBUG) Log.d(TAG, "getRecipientsNameByContactList(), normaNumbers = " + normaNumbers);

        return normaNumbers;
    }

    public static String getNameAndNumberByAddress(String address) {
        if (TextUtils.isEmpty(address)) {
            Log.w(TAG, "getNameAndNumberByAddress, null or empty address, return");
            return "";
        }
        String recipientsString = address;
        Contact contact = Contact.get(address, false);
        //no need hightligt this number,so not normalizeNumber here
        if (contact != null) {
            String contactName = contact.getName();
            //if(DEBUG) Log.d(TAG, "getFullyRecipientsStrByAddress(), contactName = " + contactName);
            String contactNumber = contact.getNumber();
            //if(DEBUG) Log.d(TAG, "getFullyRecipientsStrByAddress(), contactNumber = " + contactNumber);
            //for one contact and have name
            if (!contactName.equals(contactNumber)) {
                recipientsString = contact.getNameAndNumber();
            }
        }
        return recipientsString;
    }

    public static String getNameByAddress(String address) {
        if (TextUtils.isEmpty(address)) {
            Log.w(TAG, "getNameByAddress, null or empty address, return");
            return "";
        }
        String contactName = address;
        Contact contact = Contact.get(address, false);
        if (contact != null) {
            contactName = contact.getName();
        }
        return contactName;
    }

    public static String getNumberByAddress(String address) {
        if (TextUtils.isEmpty(address)) {
            Log.w(TAG, "getNumberByAddress, null or empty address, return");
            return "";
        }
        String contactNumber = address;
        Contact contact = Contact.get(address, false);
        //no need hightligt this number,so not normalizeNumber here
        if (contact != null) {
            contactNumber = contact.getNumber();
        }
        return contactNumber;
    }

}
