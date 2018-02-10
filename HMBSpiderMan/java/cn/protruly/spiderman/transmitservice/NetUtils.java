package cn.protruly.spiderman.transmitservice;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by lijia on 17-5-16.
 */

public class NetUtils {

    private static final String TAG = "SpiderMan";

    /**
     * If there is a wifi network connection connected
     *
     * @param context
     * @return true if there is a wifi network connection connected
     */

    public static boolean isWiFiActive(Context context) {
        Context c = context.getApplicationContext();
        ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isAvailable()
                && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            // 网络连接
            String name = netInfo.getTypeName();
            Log.v(TAG, "连接状态： " + name + ", WiFi可用");
            return true;
        } else {
            Log.v(TAG, "连接状态：WiFi, WiFi不可用");
            return false;
        }

    }

    /**
     * If there is a mobile network connection connected
     *
     * @param context
     * @return true if there is a mobile network connection connected
     */
    public static boolean isMobileOnline(Context context) {
        Context c = context.getApplicationContext();
        ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        if (netInfo != null && netInfo.isAvailable()
                && netInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
            String name = netInfo.getTypeName();
            Log.v(TAG, "连接状态： " + name + ", 数据流量网络可用");
            return true;
        } else {
            Log.v(TAG, "连接状态：数据流量网络, 数据流量网络不可用");
            return false;
        }
    }

    public static String readString(InputStream is) {
        return new String(readBytes(is));
    }

    /**
     * 读取字节数组
     */

    public static byte[] readBytes(InputStream is) {
        try {
            byte[] buffer = new byte[1024];
            int len;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            is.close();
            baos.close();
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 解析
     *
     * @throws JSONException
     */
    public static ArrayList<HashMap<String, Object>> readJSONData(String jsonString) throws JSONException {

        ArrayList<HashMap<String, Object>> list = new ArrayList();
        HashMap<String, Object> map = new HashMap();
        JSONArray jsonArray = new JSONArray("[" + jsonString + "]");
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            map.put("retCode", jsonObject.getInt("retCode"));
            map.put("errorMsg", jsonObject.getString("errorMsg"));
            list.add(map);
        }
        return list;
    }


}
