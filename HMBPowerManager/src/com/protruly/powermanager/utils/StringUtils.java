package com.protruly.powermanager.utils;

import java.util.ArrayList;
import java.util.List;

public class StringUtils {

	public static boolean isEmpty(String s) {
		return s == null || s.length() == 0 || s.equals("null");
	}

	public static boolean notEmpty(final String str) {
		return !isEmpty(str);
	}

	public static String str2HexStr(String str) {
		char[] chars = "0123456789ABCDEF".toCharArray();
		StringBuilder sb = new StringBuilder("");
		byte[] bs = str.getBytes();
		int bit;
		for (int i = 0; i < bs.length; i++) {
			bit = (bs[i] & 0x0f0) >> 4;
			sb.append(chars[bit]);
			bit = bs[i] & 0x0f;
			sb.append(chars[bit]);
		}
		return sb.toString();
	}

	public static byte[] hexStringToByte(String hex) {
		int len = (hex.length() / 2);
		byte[] result = new byte[len];
		char[] achar = hex.toCharArray();
		for (int i = 0; i < len; i++) {
			int pos = i * 2;
			result[i] = (byte) (toByte(achar[pos]) << 4 | toByte(achar[pos + 1]));
		}
		return result;
	}

	private static int toByte(char c) {
		byte b = (byte) "0123456789ABCDEF".indexOf(c);
		return b;
	}

	// ASCII字符串转换为十六进制字节数组
	public static byte[] convert2HexArray(String apdu) {
		int len = apdu.length() / 2;
		char[] chars = apdu.toCharArray();
		String[] hexes = new String[len];
		byte[] bytes = new byte[len];
		for (int i = 0, j = 0; j < len; i = i + 2, j++) {
			hexes[j] = "" + chars[i] + chars[i + 1];
			bytes[j] = (byte) Integer.parseInt(hexes[j], 16);
		}
		return bytes;
	}

	public static String str_replace(String strSource, String strFrom,
									 String strTo) {
		if (strSource == null) {
			return null;
		}
		int i = 0;
		if ((i = strSource.indexOf(strFrom, i)) >= 0) {
			char[] cSrc = strSource.toCharArray();
			char[] cTo = strTo.toCharArray();
			int len = strFrom.length();
			StringBuffer buf = new StringBuffer(cSrc.length);
			buf.append(cSrc, 0, i).append(cTo);
			i += len;
			int j = i;
			while ((i = strSource.indexOf(strFrom, i)) > 0) {
				buf.append(cSrc, j, i - j).append(cTo);
				i += len;
				j = i;
			}
			buf.append(cSrc, j, cSrc.length - j);
			return buf.toString();
		}
		return strSource;
	}

	public static String[] StringToArray(String splitStr, String sourceStr,
										 String end) {
		String[] targetStr = new String[sourceStr.length()];
		int i = 0;
		int j = 0;
		while ((i = sourceStr.indexOf(splitStr)) >= 0) {
			targetStr[j] = sourceStr.substring(0, i) + end;
			sourceStr = sourceStr.substring(i + splitStr.length(), sourceStr
					.length());
			j++;
		}
		if (sourceStr.length() > 0) {
			targetStr[j] = sourceStr + end;
		}
		return targetStr;
	}

	public static List<String> splitStrByat(String str){
		List<String> strList = new ArrayList<String>();
		if(str == null){
			return null;
		}
		
		while(true){
			int index = str.indexOf("@");
			if(index == -1){
				strList.add(str);
				break;
			}else{
				strList.add(str.substring(0,index));
				str = str.substring(index+1);
			}
		}		
		return strList;		
	}
}