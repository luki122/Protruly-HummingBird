package com.hmb.manager.sms;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hmb.manager.HMBManagerApplication;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.text.TextUtils;

public class KeyWordFilter {
	private static final String TAG = "KeyWordFilter";
	private static Pattern pattern = null;
	private static Uri KEYWORD_URI = Uri.parse("content://com.hb.reject/keyword");
	
	private static void initPattern(Context context) {
		StringBuffer patternBuf = new StringBuffer("");
		Cursor cursor = context.getContentResolver().query(KEYWORD_URI,
				null, null, null, null);
		List<String> mKeywordStringList = new ArrayList<String>();
		if (cursor != null) {
			mKeywordStringList.clear();
			if (cursor.moveToFirst()) {
				do {
					mKeywordStringList.add(cursor.getString(cursor
							.getColumnIndex("word")));
				} while (cursor.moveToNext());
			}
			cursor.close();
		}
					
		patternBuf.append("(");
		for(String word:mKeywordStringList) {
			patternBuf.append(escapeExprSpecialWord(word) + "|");
		}
		patternBuf.deleteCharAt(patternBuf.length() - 1);
		patternBuf.append(")");
        Log.d(TAG, "initPattern =  " + patternBuf);
		// unix换成UTF-8
		// pattern = Pattern.compile(new
		// String(patternBuf.toString().getBytes("ISO-8859-1"), "UTF-8"));
		pattern = Pattern.compile(patternBuf.toString());
	}

	public static boolean doFilter(String str) {
		initPattern(HMBManagerApplication.getInstance());
		Matcher m = pattern.matcher(str);
		return m.find();
	}
	
	/**
	 * 转义正则特殊字符 （$()*+.[]?\^{},|）
	 * 
	 * @param keyword
	 * @return
	 */
	public static String escapeExprSpecialWord(String keyword) {
	    if (!TextUtils.isEmpty(keyword)) {
	        String[] fbsArr = { "\\", "$", "(", ")", "*", "+", ".", "[", "]", "?", "^", "{", "}", "|" };
	        for (String key : fbsArr) {
	            if (keyword.contains(key)) {
	                keyword = keyword.replace(key, "\\" + key);
	            }
	        }
	    }
	    return keyword;
	}
}