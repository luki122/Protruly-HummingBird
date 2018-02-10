package com.protruly.clouddata.appdata.common;

import android.text.TextUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Utils
{
   private static char[] hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 
     'c', 'd', 'e', 'f' };
  
   private static MessageDigest mMessageDigest = null;
  
  static {
     try { mMessageDigest = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
       e.printStackTrace();
    }
  }
  
  public static String getMD5(String strContent) {
     StringBuffer localStringBuffer = new StringBuffer();
    try {
       MessageDigest localMessageDigest = MessageDigest.getInstance("MD5");
       localMessageDigest.update(strContent.getBytes());
       byte[] arrayOfByte = localMessageDigest.digest();
       for (int i = 0; i < arrayOfByte.length; i++) {
         int j = 0xFF & arrayOfByte[i];
         if (j < 16)
           localStringBuffer.append(0);
         localStringBuffer.append(Integer.toHexString(j));
      }
    } catch (Exception exception) {
       exception.printStackTrace();
    }
     return localStringBuffer.toString();
  }
  
  public static String getStringMD5Value(String strString)
  {
     return getMD5Value(strString.getBytes());
  }
  
  public static String getMD5Value(byte[] bytes) {
     mMessageDigest.update(bytes);
     return bufferToHex(mMessageDigest.digest());
  }
  
  public static boolean checkStringMd5Value(String strString, String strOrgMd5PwdStr) {
     if ((TextUtils.isEmpty(strString)) || (TextUtils.isEmpty(strOrgMd5PwdStr))) {
       return false;
    }
    
     String strMd5PwdStr = getStringMD5Value(strString);
     return strMd5PwdStr.equals(strOrgMd5PwdStr);
  }
  
  public static String getFileMD5Value(File file)
    throws Exception
  {
     InputStream fis = new FileInputStream(file);
     byte[] buffer = new byte['Ѐ'];
     int numRead = 0;
     while ((numRead = fis.read(buffer)) > 0) {
       mMessageDigest.update(buffer, 0, numRead);
    }
     fis.close();
     return bufferToHex(mMessageDigest.digest());
  }
  
  public static String getFileMD5Value(String strFilePath) {
    try {
       if (TextUtils.isEmpty(strFilePath)) {
         return null;
      }
      
       File file = new File(strFilePath);
       return getFileMD5Value(file);
    } catch (Exception e) {
       e.printStackTrace(); }
     return null;
  }
  
  public static boolean checkFileMd5Value(File file, String strOrgMd5PwdStr) throws Exception
  {
     if ((file == null) || (!file.isFile()) || (!file.exists()) || (TextUtils.isEmpty(strOrgMd5PwdStr))) {
       return false;
    }
    
     String strMd5PwdStr = getFileMD5Value(file);
     return strMd5PwdStr.equals(strOrgMd5PwdStr);
  }
  
  private static String bufferToHex(byte[] bytes) {
     return bufferToHex(bytes, 0, bytes.length);
  }
  
  private static String bufferToHex(byte[] bytes, int m, int n) {
     StringBuffer stringbuffer = new StringBuffer(2 * n);
     int k = m + n;
     for (int l = m; l < k; l++) {
       appendHexPair(bytes[l], stringbuffer);
    }
     return stringbuffer.toString();
  }
  
  private static void appendHexPair(byte bt, StringBuffer stringbuffer) {
     char c0 = hexDigits[((bt & 0xF0) >> 4)];
    
     char c1 = hexDigits[(bt & 0xF)];
     stringbuffer.append(c0);
     stringbuffer.append(c1);
  }
}
