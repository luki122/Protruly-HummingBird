package com.hb.thememanager.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Utils {
	
	/**
	 * Gets MD5 code for target file
	 * @param file
	 * @return 
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	public static String getFileMd5Code(File file) throws NoSuchAlgorithmException, IOException{
		  if (!file.isFile()) {
	            return null;
	        }
	        MessageDigest digest;
	        FileInputStream in;
	        byte buffer[] = new byte[1024];
	        int len;
	        digest = MessageDigest.getInstance("MD5");
	        in = new FileInputStream(file);
	        while ((len = in.read(buffer, 0, 1024)) != -1) {
	            digest.update(buffer, 0, len);
	        }
	        in.close();
	        BigInteger bigInt = new BigInteger(1, digest.digest());
	        return bigInt.toString(16);
	}

	
	/**
	 * Encrypt target String to md5 code
	 * @param plaintext target string
	 * @return 
	 */
    public final static String  encryptString(String plaintext) {
        char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'a', 'b', 'c', 'd', 'e', 'f' };
        try {
            byte[] btInput = plaintext.getBytes();
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            mdInst.update(btInput);
            byte[] md = mdInst.digest();
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            return null;
        }
    }
	
}
