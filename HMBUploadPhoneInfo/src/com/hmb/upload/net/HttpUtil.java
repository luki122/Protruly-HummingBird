package com.hmb.upload.net;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by zhaolaichao on 17-6-20.
 * 网络应用
 */

public class HttpUtil {
    public static final String NET_TYPE_MOBILE = "MOBILE";
    public static final String NET_TYPE_WIFI = "WIFI";

    /**
     * 从输入流读取数据
     * @param inStream
     * @return
     * @throws Exception
     */
    public static byte[] convertStreamToByteArray(InputStream inputStream) {
        // ByteArrayOutputStream相当于内存输出流
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        // 将输入流转移到内存输出流中
        try {
            while ((len = inputStream.read(buffer, 0, buffer.length)) != -1) {
                out.write(buffer, 0, len);
            }
            // 将内存流转换为字符串
            return out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取网络状态，wifi,wap,2g,3g.
     *
     * @param context 上下文
     * @return 联网类型
     *
     */
    public static String getNetWorkType(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            String type = networkInfo.getTypeName();
            Log.v("proxyHost", "proxyHost>>>" + type);
            if (type.equalsIgnoreCase(NET_TYPE_MOBILE)) {
                String proxyHost = System.getProperty("http.proxyHost");
                if(TextUtils.isEmpty(proxyHost)) {
                    return NET_TYPE_MOBILE;
                }
            } else if (type.equalsIgnoreCase(NET_TYPE_WIFI)) {
                return NET_TYPE_WIFI;
            }
        }
        return null;
    }

    /**
     * 检测当的网络（WLAN、3G/2G）状态
     * @param context Context
     * @return true 表示网络可用
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            if (info != null && info.isConnected()){
                // 当前网络是连接的
                if (info.getState() == NetworkInfo.State.CONNECTED){
                    // 当前所连接的网络可用
                    return true;
                }
            }
        }
        return false;
    }
}
