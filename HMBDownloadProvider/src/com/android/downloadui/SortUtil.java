package com.android.downloadui;

import android.os.Build;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author wxue
 */
public class SortUtil {

    public static final int TYPE_SPECIAL_CHAR = 1;
    public static final int TYPE_DIGIT = 2;
    public static final int TYPE_LETTER = 3;
    public static final int TYPE_CHINESE = 4;

    //convert Chinese characters into pinyin sequence.
    public static String getSpell(String str) {
        if (Build.VERSION.SDK_INT == 17) {
            StringBuffer buffer = new StringBuffer();
            if (str != null && !str.equals("")) {
                char[] cc = str.toCharArray();
                for (int i = 0; i < cc.length; i++) {
                    ArrayList<HbPinYinUtils.Token> mArrayList = HbPinYinUtils
                            .getInstance().get(String.valueOf(cc[i]));
                    if (mArrayList.size() > 0) {
                        String n = mArrayList.get(0).target;
                        buffer.append(n);
                    }
                }
            }
            String spellStr = buffer.toString();
            return spellStr.toUpperCase();
        } else if (Build.VERSION.SDK_INT > 17) {
            return HanziToPinyin.hanziToPinyin(str);
        }
        return "";
    }

    //judge whether "str" contains Chinese character
    public static boolean containChinese(String str) {
        Pattern p = Pattern.compile("[\u4e00-\u9fa5]");
        Matcher m = p.matcher(str);
        if (m.find()) {
            return true;
        }
        return false;
    }

    public static boolean isSpecialCharacter(String str) {
        String regEx="[` _~!@#$%^&*()+=|{}':;,\\[\\].<>/?！￥…（）—【】‘；：”“’。，、？]";
        return str.matches(regEx);
    }

    public static boolean isDigit(String str) {
        /*
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(str);
        if(m.find()) {
            return true;
        }
        return false;
        */
        String regEx="[0-9]";
        return str.matches(regEx);
    }

    public static boolean isLetter(String str) {
        String regEx = "[a-zA-Z]";
        return str.matches(regEx);
    }

    /*we only compare the first character of string*/
    public static int getCharacterType(String str) {
        if(isLetter(str)) {
            return TYPE_LETTER;
        } else if(isDigit(str)) {
            return TYPE_DIGIT;
        } else if(isSpecialCharacter(str)) {
            return TYPE_SPECIAL_CHAR;
        } else if(containChinese(str)) {
            return TYPE_CHINESE;
        }
        return TYPE_LETTER;
    }

    public static String getCharacterTypeDescription(int type) {
        switch(type) {
            case TYPE_SPECIAL_CHAR:
                return "TYPE_SPECIAL_CHAR";
            case TYPE_DIGIT:
                return "TYPE_DIGIT";
            case TYPE_LETTER:
                return "TYPE_LETTER";
            case TYPE_CHINESE:
                return "TYPE_CHINESE";
        }
        return "";
    }
}
