package cn.protruly.spiderman.transmitservice;

import android.util.Log;

import org.json.JSONException;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by lijia on 17-5-16.
 */

public class HttpConnectionUtil {

    private static final String TAG = "SpiderMan";
    private final static int CONNECT_OUT_TIME = 5000;
    private String url;
    private File file;
    private HashMap<String, String> params;
    private StringBuffer sb;

    public HttpConnectionUtil(String url, File file, HashMap<String, String> params) {
        this.url = url;
        this.file = file;
        this.params = params;
    }

    public void sendPostUpFile() {

        try {

            String BOUNDARY = "---------------------------7da2137580612";
            String CHARSET = "utf-8";
            String CONTENT_TYPE = "multipart/form-data";
            String PREFIX = "--";
            String LINE_END = "\r\n";

            URL realURL = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) realURL.openConnection();

            conn.setConnectTimeout(CONNECT_OUT_TIME);
            conn.setReadTimeout(CONNECT_OUT_TIME);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Charset", CHARSET);
            conn.setRequestProperty("connection", "keep-alive");
            conn.setRequestProperty("Content-Type", CONTENT_TYPE + ";boundary=" + BOUNDARY);

            if (file != null) {
                OutputStream outputSteam = conn.getOutputStream();
                DataOutputStream dos = new DataOutputStream(outputSteam);
                sb = new StringBuffer();
                if (params != null) {
                    // 根据格式，开始拼接文本参数
                    for (Map.Entry<String, String> entry : params.entrySet()) {
                        sb.append(PREFIX).append(BOUNDARY).append(LINE_END);
                        sb.append("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"" + LINE_END);
                        sb.append("Content-Type: text/plain; charset=" + CHARSET + LINE_END);
                        sb.append("Content-Transfer-Encoding: 8bit" + LINE_END);
                        sb.append(LINE_END);
                        sb.append(entry.getValue());
                        sb.append(LINE_END);
                    }
                }
                // 开始拼接文件参数
                sb.append(PREFIX).append(BOUNDARY).append(LINE_END);
                sb.append("Content-Disposition: form-data; name=\"file\"; filename=\""
                        + file.getName() + "\"" + LINE_END);
                sb.append("Content-Type: application/zip; charset=" + CHARSET + LINE_END);
                sb.append(LINE_END);

                // 写入文件数据
                dos.write(sb.toString().getBytes());

                FileInputStream is = new FileInputStream(file);
                byte[] bytes = new byte[1024 * 1024];
                int len;
                while ((len = is.read(bytes)) != -1) {
                    dos.write(bytes, 0, len);
                }
                is.close();
                dos.write(LINE_END.getBytes());
                byte[] end_data = (PREFIX + BOUNDARY + PREFIX + LINE_END).getBytes();
                dos.write(end_data);
                dos.flush();
                dos.close();

            }

            int code = conn.getResponseCode();
            if (code == 200) {
                Log.v(TAG, "服务器有响应,请求连接成功");
                InputStream is = conn.getInputStream();
                String retrunValue = NetUtils.readString(is);
                Log.v(TAG, retrunValue);
                ArrayList<HashMap<String, Object>> arrayList = NetUtils.readJSONData(retrunValue);
                for (HashMap<String, Object> data : arrayList) {
                    if ((int) data.get("retCode") == 0) {
                        Log.v(TAG, "文件上传成功,删除文件");
                        file.delete();
                    } else {
                        Log.v(TAG, "文件上传失败");
                    }
                }
            } else {
                Log.v(TAG, "服务器无响应,请求连接失败");
            }


        } catch (MalformedURLException eio) {

            eio.printStackTrace();

        } catch (ProtocolException e) {

            e.printStackTrace();

        } catch (IOException e) {

            e.printStackTrace();

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


}
