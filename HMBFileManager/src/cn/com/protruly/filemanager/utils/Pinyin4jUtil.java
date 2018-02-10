package cn.com.protruly.filemanager.utils;

/**
 * Created by sqf on 17-7-24.
 */

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import cn.com.protruly.filemanager.utils.LogUtil;

public class Pinyin4jUtil {

    private static final String TAG = "Pinyin4jUtil";

    public HanyuPinyinOutputFormat mHanyuPinyinOutputFormat;

    private static Pinyin4jUtil mInstance;

    private Pinyin4jUtil() {
        mHanyuPinyinOutputFormat = new HanyuPinyinOutputFormat();
    }

    public static Pinyin4jUtil instance() {
        if(mInstance == null) {
            mInstance = new Pinyin4jUtil();
        }
        return mInstance;
    }

    public String getPinyin(String hanziString){
        mHanyuPinyinOutputFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        //mHanyuPinyinOutputFormat.setToneType(HanyuPinyinToneType.WITH_TONE_MARK);
        mHanyuPinyinOutputFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        mHanyuPinyinOutputFormat.setVCharType(HanyuPinyinVCharType.WITH_U_UNICODE);
        String[] pinyinArray0 = null;
        StringBuilder pinyinArray = new StringBuilder();
        for(char c:hanziString.toCharArray()) {
            try {
                //是否在汉字范围内
                if (c >= 0x4e00 && c <= 0x9fa5) {
                    pinyinArray0 = PinyinHelper.toHanyuPinyinStringArray(c, mHanyuPinyinOutputFormat);
                    pinyinArray.append(pinyinArray0[0]) ;
                } else {
                    pinyinArray.append(c);
                }
            } catch (BadHanyuPinyinOutputFormatCombination e) {
                e.printStackTrace();
            }
        }
        return pinyinArray.toString();
    }
}
