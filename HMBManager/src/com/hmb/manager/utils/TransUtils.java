package com.hmb.manager.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

import com.hmb.manager.bean.StorageSize;

import android.util.Log;

public class TransUtils {

	public static final long ONE_KB = 1024L;
	public static final long ONE_MB = ONE_KB * 1024L;
	public static final long ONE_GB = ONE_MB * 1024L;
	public static final long ONE_TB = ONE_GB * 1024L;

	public static String transformShortType(long bytes, boolean isShortType) {
		long currenUnit = ONE_KB;
		int unitLevel = 0;
		boolean isNegative = false;
		if (bytes < 0) {
			isNegative = true;
			bytes = (-1) * bytes;
		}

		while ((bytes / currenUnit) > 0) {
			unitLevel++;
			currenUnit *= ONE_KB;
		}

		String result_text = null;
		double currenResult = 0;

		switch (unitLevel) {
		case 0:
			currenResult = bytes;
			result_text = getFloatValue(currenResult, 0)+"B";
			break;
		case 1:
			currenResult = bytes / ONE_KB;

			result_text = getFloatValue(currenResult, 2) + "KB";

			break;
		case 2:
			currenResult = bytes * 1.0 / ONE_MB;

			result_text = getFloatValue(currenResult, 2) + "MB";

			break;
		case 3:
			currenResult = bytes * 1.0 / ONE_GB;

			result_text = getFloatValue(currenResult, 2) + "GB";

			break;
		case 4:
			result_text = getFloatValue(bytes * 1.0 / ONE_TB, 2) + "TB";
		}

		if (isNegative) {
			result_text = "-" + result_text;
		}
		return result_text;
	}

	public static long unTransformShortType(String text) {
		long bytes = 0;
		String s=null;
		String mText=text.trim();
		if(mText!=null&&mText.length()>0){
		int length = mText.length();
		s = mText.substring(0, length - 1);
		if (isDigit(s)) {
			bytes = Long.parseLong(s);
		} else {
			s=mText.substring(length - 2, length);
			float mSize = Float.valueOf(text.trim().substring(0, length - 2));
			if (s != null) {
				switch (s) {
				case "KB":
					bytes = Float.valueOf(mSize * ONE_KB).longValue();
					break;
				case "MB":
					bytes = Float.valueOf(mSize * ONE_MB).longValue();
					break;
				case "GB":
					bytes = Float.valueOf(mSize * ONE_GB).longValue();
					break;
				case "TB":
					bytes = Float.valueOf(mSize * ONE_TB).longValue();
					break;
				}
			}
		}
		}
		return bytes;
	}

	public static StorageSize convertStorageSize(long size) {
		long kb = 1024;
		long mb = kb * 1024;
		long gb = mb * 1024;
		StorageSize sto = new StorageSize();
		if (size >= gb) {

			sto.suffix = "GB";
			sto.value = (float) size / gb;
			return sto;
		} else if (size >= mb) {

			sto.suffix = "MB";
			sto.value = (float) size / mb;

			return sto;
		} else if (size >= kb) {

			sto.suffix = "KB";
			sto.value = (float) size / kb;

			return sto;
		} else {
			sto.suffix = "B";
			sto.value = (float) size;

			return sto;
		}

	}

	public static String storageToText(StorageSize storageSize){
		StringBuffer sb=new StringBuffer();
		sb.append(storageSize.value);
		sb.append(storageSize.suffix);
		return sb.toString();
	}

	public static boolean isDigit(String str) {
		boolean isNum = false;
		isNum = str.matches("[0-9]+");
		return isNum;
	}

	public static String getFloatValue(double oldValue, int decimalCount) {
		if (oldValue >= 1000) {
			decimalCount = 0;
		} else if (oldValue >= 100) {
			decimalCount = 1;
		}

		BigDecimal b = new BigDecimal(oldValue);
		try {
			if (decimalCount <= 0) {
				oldValue = b.setScale(0, BigDecimal.ROUND_HALF_UP).floatValue(); 
																				
			} else {
				oldValue = b.setScale(decimalCount, BigDecimal.ROUND_HALF_UP).floatValue(); 
																							
																							
			}
		} catch (ArithmeticException e) {
			Log.w("Unit.getFloatValue", e.getMessage());
		}
		String decimalStr = "";
		if (decimalCount <= 0) {
			decimalStr = "#";
		} else {
			for (int i = 0; i < decimalCount; i++) {
				decimalStr += "#";
			}
		}
		DecimalFormat format = new DecimalFormat("###." + decimalStr);
		return format.format(oldValue);
	}

}
