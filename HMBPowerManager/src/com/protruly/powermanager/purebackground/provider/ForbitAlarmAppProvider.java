package com.protruly.powermanager.purebackground.provider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.util.Xml;

import com.android.internal.util.XmlUtils;
import com.protruly.powermanager.purebackground.Config;
import com.protruly.powermanager.utils.LogUtils;

public class ForbitAlarmAppProvider{
    private static final String TAG = "ForbitAlarmAppProvider";

    private final static String xmlNewFilePath = "/data/data/com.protruly.powermanager/files/";
    private final static String xmlNewFileName = "forbitapplist.xml";
    private final static String xmlNewFile = xmlNewFilePath + xmlNewFileName;

    private static List<String> mForbitAlarmAppList = new ArrayList<String>();

    public static List<String> getForbitAlarmAppList(Context context) {
        List<String> forbitAlarmAppList = getForbitAlarmAppsFromXml();

        mForbitAlarmAppList.clear();
        if (forbitAlarmAppList != null) {
            for (String app : forbitAlarmAppList) {
                mForbitAlarmAppList.add(app);
            }
        }

        return mForbitAlarmAppList;
    }

    /**
     * write app package name into xml.
     * 
     * @param context
     * @param pkgName
     */
    public static void addForbitAlarmAppInToXML(Context mContext, String packageName) {
        insertOrUpdateXMLData(mContext, packageName);
    }

    public static void removeForbitAlarmAppFromXML(Context mContext, String packageName) {
        deleteDataFromXML(mContext, packageName);
    }
    
    private static void deleteDataFromXML(Context mContext, String packageName) {
        if (packageName == null) {
            return;
        }

        if (!mForbitAlarmAppList.contains(packageName)) {
            LogUtils.d(TAG, packageName + " is not in xml");
        } else {
            mForbitAlarmAppList.remove(packageName);

            writeForbitAlarmAppsIntoXML(mForbitAlarmAppList);
        }
    }

    /**
     * Whether app is in auto clean app list.
     * 
     * @param context
     * @param pkgName
     * @return
     */
    public static boolean isInForbitAlarmAppList(Context context, String pkgName) {
        return mForbitAlarmAppList.contains(pkgName);
    }

    private static void insertOrUpdateXMLData(Context context, String pkgName) {
        if (context == null || pkgName == null) {
            return;
        }

        if (mForbitAlarmAppList.contains(pkgName)) {
            // do nothing
            LogUtils.d(TAG, "insertOrUpdateData() -> pkgName " + pkgName + " is In XML");
        } else {
            LogUtils.d(TAG, "insertOrUpdateData() -> pkgName = " + pkgName);
            if ((mForbitAlarmAppList == null) || (mForbitAlarmAppList.size() == 0)) {
                mForbitAlarmAppList = getForbitAlarmAppsFromXml();
            }

            mForbitAlarmAppList.add(pkgName);
            writeForbitAlarmAppsIntoXML(mForbitAlarmAppList);
        }
    }

    public static List<String> getForbitAlarmAppsFromXml() {
        FileReader permReader = null;
        List<String> list = null;

        try {
            list = new ArrayList<String>();
            permReader = new FileReader(xmlNewFile);
            LogUtils.d(TAG, "getPackageNameFromXml : read xmlNewFile ");
        } catch (FileNotFoundException e) {
            LogUtils.d(TAG, "getPackageNameFromXml, can not find config xml, try default arrays.");

            for (int i = 0; i < Config.forbitAlarmDefaultList.length; i++) {
                if (Config.forbitAlarmDefaultList[i] != null) {
                    list.add(Config.forbitAlarmDefaultList[i]);
                }
            }
        }

        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(permReader);

            XmlUtils.beginDocument(parser, "channel");

            while (true) {
                XmlUtils.nextElement(parser);
                if (parser.getEventType() == XmlPullParser.END_DOCUMENT) {
                    break;
                }

                String name = parser.getName();
                if ("item".equals(name)) {
                    int id = Integer.parseInt(parser.getAttributeValue(null, "id"));
                    if (id <= 0) {
                         LogUtils.d(TAG, "<item> without id at " + parser.getPositionDescription());
                         XmlUtils.skipCurrentTag(parser);
                         continue;
                     }

                    String packagename = parser.getAttributeValue(null, "name");
                    if (packagename == null) {
                        LogUtils.d(TAG, "<item> without name at " + parser.getPositionDescription());
                        XmlUtils.skipCurrentTag(parser);
                        continue;
                    }
                    LogUtils.d(TAG, "getPackageNameFromXml : id is " + id + "  name is " + packagename);

                    list.add(packagename);

                    XmlUtils.skipCurrentTag(parser);
                } else {
                    XmlUtils.skipCurrentTag(parser);
                    continue;
                }
            }
        } catch (XmlPullParserException e) {
            LogUtils.e(TAG, "Got execption parsing permissions : " + e);
        } catch (IOException e) {
            LogUtils.e(TAG, "Got execption parsing permissions : " + e);
        } finally {
            if(permReader!=null) {
                try {
                    permReader.close();
                } catch (IOException ee){
                    LogUtils.d(TAG, "Got execption parsing permissions." + ee);
                }
            }
        }

        return list;
    }

    public static void writeForbitAlarmAppsIntoXML(final List<String> pkgs) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                FileOutputStream fileos = null;

                File forbitAlarmFile = new File(xmlNewFile);
                if (!forbitAlarmFile.exists()) {
                    LogUtils.d(TAG, "forbit alarm file is not exist, please create new file.");
                    getFilePath(xmlNewFilePath, xmlNewFileName);
                }

                try {
                    fileos = new FileOutputStream(xmlNewFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                XmlSerializer serializer = Xml.newSerializer();
                try {
                    serializer.setOutput(fileos, "UTF-8");
                    serializer.startDocument(null, true);
                    serializer.startTag(null, "channel");

                    for (int i = 0 ; i < pkgs.size() ; i++) {
                        String name = pkgs.get(i);
                        if (name != null && name.length() > 0) {
                            serializer.startTag(null, "item");
                            serializer.attribute(null, "id", String.valueOf(i + 1));
                            serializer.attribute(null, "name", name);
                            serializer.endTag(null, "item");
                        }
                    }
                    serializer.endTag(null, "channel");

                    serializer.endDocument();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    
    public static File getFilePath(String filePath, String fileName) {
        File file = null;
        makeRootDirectory(filePath);
        try {
            file = new File(filePath + fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    public static void makeRootDirectory(String filePath) {
        File file = null;
        try {
            file = new File(filePath);
            if (!file.exists()) {
                file.mkdir();
            }
        } catch (Exception e) {

        }
    }
}
