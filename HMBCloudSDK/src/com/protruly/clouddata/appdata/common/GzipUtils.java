 package com.protruly.clouddata.appdata.common;
 
 import java.io.ByteArrayOutputStream;
import java.io.IOException;
 import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPOutputStream;

import android.text.TextUtils;
 
 public class GzipUtils
 {
   public static byte[] gzip(String strContent)
   {
     return gzip(strContent, "utf-8");
   }
   

   public static byte[] gzip(String strContent, String strEncode)
   {
			    if (TextUtils.isEmpty(strContent)) {
			        return null;
			    }
			    try {
			        ByteArrayOutputStream baos = new ByteArrayOutputStream();
			        GZIPOutputStream gzip = new GZIPOutputStream(baos);
			        gzip.write(strContent.getBytes());
			        gzip.close();
			        byte[] encode = baos.toByteArray();
			        baos.flush();
			        baos.close();
			        return encode;
			    } catch (UnsupportedEncodingException e) {
			        e.printStackTrace();
			    } catch (IOException e) {
			        e.printStackTrace();
			    }
			
			    return null;
   }


   public static byte[] readBytes(InputStream in)
     throws IOException
   {
     byte[] temp = new byte[in.available()];
     byte[] result = new byte[0];
     int size = 0;
     while ((size = in.read(temp)) != -1) {
       byte[] readBytes = new byte[size];
       System.arraycopy(temp, 0, readBytes, 0, size);
       result = mergeArray(new byte[][] { result, readBytes });
     }
     return result;
   }
   
   public static byte[] mergeArray(byte[]... a)
   {
     int index = 0;
     int sum = 0;
     for (int i = 0; i < a.length; i++) {
       sum += a[i].length;
     }
     byte[] result = new byte[sum];
     for (int i = 0; i < a.length; i++) {
       int lengthOne = a[i].length;
       if (lengthOne != 0)
       {
 
 
         System.arraycopy(a[i], 0, result, index, lengthOne);
         index += lengthOne;
       } }
     return result;
   }
 }

