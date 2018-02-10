package cn.com.protruly.filemanager.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by sqf on 17-7-7.
 */

public class MD5Util {
    public static String stringToMD5(String text) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return MD5(text, md);
    }

    public static String MD5(String strSrc, MessageDigest md) {
        byte[] bt = strSrc.getBytes();
        md.update(bt);
        String strDes = bytes2Hex(md.digest()); // to HexString
        return strDes;
    }

    private static String bytes2Hex(byte[] bts) {
        StringBuffer des = new StringBuffer();
        String tmp = null;
        for (int i = 0; i < bts.length; i++) {
            tmp = (Integer.toHexString(bts[i] & 0xFF));
            if (tmp.length() == 1) {
                des.append("0");
            }
            des.append(tmp);
        }
        return des.toString();
    }
}
