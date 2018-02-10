package com.android.contacts.util;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import android.text.TextUtils;

public class PinyinUtils {
	/**  
	 * 获取汉字串拼音，英文字符不变  
	 * @param chinese 汉字串  
	 * @return 汉语拼音  
	 */   
	private static HanyuPinyinOutputFormat format =null;
	private static StringBuilder quanPinyinBuilder=new StringBuilder();
	private static StringBuilder jianPinyinBuilder=new StringBuilder();
	public static String[] getFullSpell(String chinese) {
		if (null == format) {
			format = new HanyuPinyinOutputFormat();
			format.setCaseType(HanyuPinyinCaseType.LOWERCASE);   
			format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
		}

		if(TextUtils.isEmpty(chinese)) return new String[]{"",""};
		quanPinyinBuilder=new StringBuilder();
		jianPinyinBuilder=new StringBuilder();
		StringBuffer pybf = new StringBuffer();   
		char[] arr = chinese.toCharArray();    
		for (int i = 0; i < arr.length; i++) {   
			if (arr[i] > 128) {   
				try {   
					String[] temp = PinyinHelper.toHanyuPinyinStringArray(arr[i], format);   
					if (temp != null) {   						
						quanPinyinBuilder.append(temp[0]); 
						jianPinyinBuilder.append(temp[0].charAt(0)); 
					}   

				} catch (BadHanyuPinyinOutputFormatCombination e) {   
					e.printStackTrace();   
				}   
			} else {   
				quanPinyinBuilder.append(arr[i]);   
				jianPinyinBuilder.append(arr[i]); 
			}
		}

		String[] stringBuilders=new String[2];
		stringBuilders[0]=quanPinyinBuilder.toString().toUpperCase();
		stringBuilders[1]=jianPinyinBuilder.toString().toUpperCase();
		return stringBuilders;
	}
}
