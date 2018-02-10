package com.android.providers.applications;

import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Created by lijun on 17-6-5.
 */

public class Utilities {
    public static ArrayList<String> getFullPinYinList(String displayName) {
        return getFullPinYinListNew(displayName);
//        if (displayName == null) return null;
//        ArrayList<HanziToPinyin.Token> tokens = HanziToPinyin.getInstance().get(displayName);
//        if (tokens != null && tokens.size() > 0) {
//            ArrayList<String> pinyinList = new ArrayList<String>();
//            for (HanziToPinyin.Token token : tokens) {
//                if (HanziToPinyin.Token.PINYIN == token.type) {
//                    pinyinList.add(token.target);
//                }
//            }
//            return pinyinList;
//        }
//        return null;
    }

    public static ArrayList<String> getFullPinYinListNew(String displayName) {
        if (displayName == null) return null;
        ArrayList<HanziToPinyinNew.Token> tokens = HanziToPinyinNew.getInstance().getTokens(displayName);
        if (tokens != null && tokens.size() > 0) {
            ArrayList<String> pinyinList = new ArrayList<String>();
            for (HanziToPinyinNew.Token token : tokens) {
                if (HanziToPinyinNew.Token.PINYIN == token.type) {
                    pinyinList.add(token.target);
                }
            }
            return pinyinList;
        }
        return null;
    }

    public static String getRegExpBySpChinese(CharSequence spChinese) {
        ArrayList<String> pinyinList = getFullPinYinList(spChinese.toString());
        if (pinyinList != null && pinyinList.size() > 0) {
            int pinyinCount = pinyinList.size();
            ArrayList<String> regExpList = new ArrayList<String>();
            StringBuilder finalRegExpBuilder = new StringBuilder();
            finalRegExpBuilder.append("(");
            for (int current = 0; current < pinyinCount; current++) {
                String pinyin = pinyinList.get(current);
                int size = pinyin.length();
                if (size > 0) {
                    StringBuilder regExpBuilder = new StringBuilder();
                    pinyin = pinyin.toLowerCase();
                    regExpBuilder.append("(");
                    for (int i = 1; i <= size; i++) {
                        regExpBuilder.append(pinyin.substring(0, i));
                        if (i < size) {
                            regExpBuilder.append("|");
                        }
                    }
                    regExpBuilder.append(")");
                    regExpList.add(regExpBuilder.toString());

                    finalRegExpBuilder.append(regExpBuilder.toString());
                    finalRegExpBuilder.append("|");
                    int regExpSize = regExpList.size();
                    int end = current + 1;
                    if (regExpSize > 1) {
                        for (int index = 0; index < end - 1; index++) {
                            finalRegExpBuilder.append("(");
                            finalRegExpBuilder.append(getStringByList(regExpList, index, end));
                            finalRegExpBuilder.append(")");
                            finalRegExpBuilder.append("|");
                        }
                    }
                }
            }
            finalRegExpBuilder.deleteCharAt(finalRegExpBuilder.length() - 1);
            finalRegExpBuilder.append(")");

            return finalRegExpBuilder.toString();
        }

        return null;
    }

    public static String getStringByList(ArrayList<String> regExpList, int start, int end) {

        StringBuilder subBuilder = new StringBuilder();
        for (int i = start; i < end; i++) {
            subBuilder.append(regExpList.get(i));
        }
        return subBuilder.toString();
    }

    // 根据Unicode编码完美的判断中文汉字和符号
    private static boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION) {
            return true;
        }
        return false;
    }

    // 完整的判断中文汉字和符号
    public static boolean isChinese(String strName) {
        char[] ch = strName.toCharArray();
        for (int i = 0; i < ch.length; i++) {
            char c = ch[i];
            if (isChinese(c)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isLetter(String str) {
        if (str == null || str.length() == 0) return false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(0);
            int a = (int) c;
            if ((a >= 65 && a <= 90) || (a >= 97 && a <= 122)) {

            } else {
                return false;
            }
        }
        return true;
    }
}
