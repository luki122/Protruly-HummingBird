package com.android.launcher3.theme;

import android.graphics.Color;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.util.HashMap;

/**
 * Created by lj on 16-10-24.
 */
public class ZIPThemeConfigParseByPull {
    public static final String TAG = "ZIPThemeConfigParseByPull";

    private HashMap<String, String> mLabel_Icons;
    private HashMap<String, Integer> mLabel_colors;
    private String themeName;
    private String themeVersion;

    public void parse(InputStream is) throws Exception {

        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(is, "UTF-8");
        int eventType = parser.getEventType();
        HashMap<String, String> packageClassMap = null;
        mLabel_colors = new HashMap<String,Integer>();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case XmlPullParser.START_DOCUMENT:
                    break;
                case XmlPullParser.START_TAG:
                    if (parser.getName().equals("string-array")) {
                        String parserName = parser.getAttributeValue(0);
                        if ("icon_array".equals(parserName)) {
                            packageClassMap = new HashMap<String, String>();
                        }
                    } else if (parser.getName().equals("item")) {
                        if (packageClassMap == null) break;
                        eventType = parser.next();
                        String packageClasseIcon = parser.getText();
                        String[] packageClasses_Icon = packageClasseIcon.split("#");
                        if (packageClasses_Icon.length == 2) {
                            String[] packageClasses = packageClasses_Icon[0].split("\\|");
                            for (String s : packageClasses) {
                                packageClassMap.put(s.trim(), packageClasses_Icon[1]);
                            }
                        }
                        Log.d(TAG, "parse icon:" + packageClasseIcon);
                    } else if (parser.getName().equals("dimen")) {
                        String parserName = null;
                        try {
                            parserName = parser.getAttributeValue(0);
                        } catch (Exception e) {
                            break;
                        }
                        eventType = parser.next();
                        String text = parser.getText();
                        text = text.replace("dp", "");
                        int intText = Integer.parseInt(text);
                        Log.d(TAG, "parse dimen " + parserName + ":" + intText);
                    }else if (parser.getName().equals("color")) {
                        String parserName = null;
                        try {
                            parserName = parser.getAttributeValue(0);
                        } catch (Exception e) {
                            break;
                        }
                        eventType = parser.next();
                        String text = parser.getText();
                        Integer color = Color.parseColor(text);
                        mLabel_colors.put(parserName,color);
                        Log.d(TAG, "parse color " + parserName + ":" + color);
                    } else if (parser.getName().equals("string")) {
                        String parserName = parser.getAttributeValue(0);
                        eventType = parser.next();
                        String text = parser.getText();
                        if ("theme_name".equals(parserName)) {
                            themeName = text;
                        } else if ("theme_version".equals(parserName)) {
                            themeVersion = text;
                        }
                        Log.d(TAG, "parse string " + parserName + ":" + text);
                    } else if (parser.getName().equals("bool")) {
                        String parserName = parser.getAttributeValue(0);
                        eventType = parser.next();

                        String text = parser.getText();
                        Log.d(TAG, "parse bool " + parserName + ":" + text);
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (parser.getName().equals("string-array")) {
                        if (packageClassMap == null) break;
                        mLabel_Icons = packageClassMap;
                        packageClassMap = null;
                        Log.d(TAG, "icon_array parse end :");
                    } else if (parser.getName().equals("item")) {
//                        Log.d(TAG,"item parse end :");
                    }
                    break;
            }
            eventType = parser.next();
        }
    }

    public String getThemeName() {
        return themeName;
    }

    public String getThemeVersion() {
        return themeVersion;
    }

    public HashMap<String, String> getmLabel_Icons() {
        return mLabel_Icons;
    }

    public HashMap<String, Integer> getmLabel_colors() {
        return mLabel_colors;
    }

    public boolean hasData() {
        if (mLabel_Icons == null || mLabel_Icons.size() <= 0) {
            return false;
        }
        return true;
    }
}
