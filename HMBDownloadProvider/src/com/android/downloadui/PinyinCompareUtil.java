package com.android.downloadui;

import android.text.TextUtils;
import java.util.Comparator;

/**
 * @author wxue
 */
public class PinyinCompareUtil {

    public static final String TAG = "PinyinCompareUtil";

    public static Comparator<String> sComparator = new Comparator<String>() {
        @Override
        public int compare(String text1, String text2) {
            if(TextUtils.isEmpty(text1) || TextUtils.isEmpty(text2)) {
                return -1;
            }
            String c1 = text1.substring(0, 1);
            String c2 = text2.substring(0, 1);
            int characterType1 = SortUtil.getCharacterType(c1);
            int characterType2 = SortUtil.getCharacterType(c2);
            LogUtil.i(TAG, " ------ displayName1:" + text1 + " text2:" + text2 + " c1:" + c1 + " c2:" + c2 +
                    " characterType1:" + SortUtil.getCharacterTypeDescription(characterType1) +
                    " characterType2:" + SortUtil.getCharacterTypeDescription(characterType2));
            if(characterType1 == characterType2) {
                if(characterType1 != SortUtil.TYPE_CHINESE) {
                    LogUtil.i(TAG, "111111 ");
                    return text1.compareTo(text2);
                } else {
                    LogUtil.i(TAG, "222222 pinyin");
                    //we compare pinyin order here
                    String pinyin1 = SortUtil.getSpell(text1);
                    String pinyin2 = SortUtil.getSpell(text2);
                    return pinyin1.compareTo(pinyin2);
                }
            } else {
                if(characterType1 < characterType2) {
                    LogUtil.i(TAG, "333333");
                    return -1;
                } else {
                    LogUtil.i(TAG, "444444");
                    return 1;
                }
            }
        }
    };



}
