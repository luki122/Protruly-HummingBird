package com.mediatek.dialer.util;

import com.android.dialer.R;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;

public class PhoneInfoUtils {

	private static String TAG = "PhoneInfoUtils";



	//    //获取sim卡iccid
	//    public String getIccid() {
	//        String iccid = "N/A";
	//        iccid = telephonyManager.getSimSerialNumber();
	//        return iccid;
	//    }

	//获取电话号码
	public static String getNativePhoneNumber(Context context) {
		try{
			return ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number();
		}catch(SecurityException e){
			Log.e(TAG,"e:"+e);
			return "";
		}
	}
	//获取手机服务商信息

	/*    <spnOverride numeric="46000" spn="CHINA MOBILE"/>
    <spnOverride numeric="46001" spn="CHN-UNICOM"/>
    <spnOverride numeric="46002" spn="CHINA MOBILE"/>
    <spnOverride numeric="46003" spn="CHINA TELECOM"/>
    <spnOverride numeric="46007" spn="CHINA MOBILE"/>
    <spnOverride numeric="46008" spn="CHINA MOBILE"/>
    <spnOverride numeric="46009" spn="CHN-UNICOM"/>
    <spnOverride numeric="46011" spn="CHINA TELECOM"/>
    <spnOverride numeric="20404" spn="CHINA TELECOM"/>**/
	public static String getProvidersName(Context context,int slotId) {
		String providersName = "";
		String NetworkOperator = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getSimOperatorNumericForPhone(slotId);
		Log.d(TAG,"slotId:"+slotId+" NetworkOperator:"+NetworkOperator);
		if (NetworkOperator.equals("46000") || NetworkOperator.equals("46002") ||NetworkOperator.equals("46007") ||NetworkOperator.equals("46008")) {
			providersName = context.getString(R.string.hb_china_mobile);//中国移动
		} else if(NetworkOperator.equals("46001") || NetworkOperator.equals("46009")) {
			providersName = context.getString(R.string.hb_china_unicom);//中国联通
		} else if (NetworkOperator.equals("46003") || NetworkOperator.equals("46011") || NetworkOperator.equals("20404")) {
			providersName = context.getString(R.string.hb_china_tele);//中国电信
		} else {
			providersName= ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getSimOperatorNameForPhone(slotId);
			if(providersName.length()==0) return providersName;
			
			StringBuilder sb=new StringBuilder();
		    char[] cs=providersName.toCharArray();
		    int count=0;
		    for(char c:cs){
		    	if(isChinese(c)) count+=2;
		    	else count+=1;
		    	if(count<=6) sb.append(c);
		    	else break;
		    }
			return sb.toString();
		}
		
		return providersName;
		
	}
	
	public static boolean isChinese(char c) {

        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);

        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS

                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS

                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A

                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION

                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION

                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {

            return true;

        }

        return false;

    }

	//获取手机服务商信息
	public static String getSimOperator(Context context) {
		return ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getNetworkOperator();
	}

	//    public String getPhoneInfo() {
	//        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
	//        StringBuffer sb = new StringBuffer();
	//
	//        sb.append("\nLine1Number = " + tm.getLine1Number());
	//        sb.append("\nNetworkOperator = " + tm.getNetworkOperator());//移动运营商编号
	//        sb.append("\nNetworkOperatorName = " + tm.getNetworkOperatorName());//移动运营商名称
	//        sb.append("\nSimCountryIso = " + tm.getSimCountryIso());
	//        sb.append("\nSimOperator = " + tm.getSimOperator());
	//        sb.append("\nSimOperatorName = " + tm.getSimOperatorName());
	//        sb.append("\nSimSerialNumber = " + tm.getSimSerialNumber());
	//        sb.append("\nSubscriberId(IMSI) = " + tm.getSubscriberId());
	//        return  sb.toString();
	//    }

}