 package com.protruly.clouddata.appdata.common;
 
 import android.util.Base64;
 import java.io.UnsupportedEncodingException;
 
 
 
 
 
 
 
 public class SecurityUtil
 {
//   public static String encrypt(String str)
//   {
//     if ((str != null) && (!"".equals(str))) {
//       return encodeXOR(str);
//     }
//     return str;
//   }
   
 
 
 
 
   public static String encryptLocal(String str)
   {
     if ((str != null) && (!"".equals(str))) {
       return encodeBase64(encodeXOR(str));
     }
     return str;
   }
   
 
 
 
 
//   public static String decrypt(String str)
//   {
//     if ((str != null) && (!"".equals(str))) {
//       return decodeXOR(str);
//     }
//     return str;
//   }
   
/*
 * 字符串加密
 * @param str
 *  待加密字符串
 * @return 加密后的字符串
 */
public static String encrypt(String str) {
	String rstStr = "";
	if (str != null && str.length() > 0) {
		char[] charArray = str.toCharArray();
		int j = 0;
		for (int i = 0; i < charArray.length; i++) {
			charArray[i] = (char) (charArray[i] ^ (666 + j));
			if (j++ > 10000) {
				j = 0;
			}
		}
		rstStr = new String(charArray);
	}
	return rstStr;
}

/**
 * 字符串解密
 * @param str 待解密字符串
 * @return 解密后的字符串
 */
public static String decrypt(String str) {
	return encrypt(str);
}


 
 
   public static String decryptLocal(String str)
   {
     if ((str != null) && (!"".equals(str))) {
       return decodeXOR(decodeBase64(str));
     }
     return str;
   }
   
   public static String decodeXOR(String str) {
     String rst = "";
     if ((str != null) && (!str.equals(""))) {
       char[] array = str.toCharArray();
       for (int i = 0; i < array.length; i++) {
         array[i] = ((char)(array[i] ^ 1000 + i));
       }
       rst = new String(array);
     }
     return rst;
   }
   
   public static String encodeXOR(String str) {
     return decodeXOR(str);
   }
   
   public static String encodeBase64(String str) {
     if ((str != null) && (!str.equals(""))) {
       String rst = "";
       try {
         rst = Base64.encodeToString(str.getBytes("utf-8"), 2);
       } catch (UnsupportedEncodingException e) {
         e.printStackTrace();
       }
       return rst;
     }
     return str;
   }
   
   public static String decodeBase64(String str) {
     if ((str != null) && (!str.equals(""))) {
       String rst = "";
       try {
         rst = new String(Base64.decode(str, 2), "utf-8");
       } catch (UnsupportedEncodingException e) {
         e.printStackTrace();
       }
       return rst;
     }
     return str;
   }
 }
