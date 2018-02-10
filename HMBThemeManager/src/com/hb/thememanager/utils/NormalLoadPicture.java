package com.hb.thememanager.utils;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by TangHui on 2017/6/19.
 */
public class NormalLoadPicture {

    private String uri;
    private ImageView imageView;
    private byte[] picByte;
    private Thread mThread;

    public NormalLoadPicture() {
        super();
    }

    public void getPicture(String uri, ImageView imageView) {
        this.uri = uri;
        this.imageView = imageView;
        mThread = new Thread(runnable);
        mThread.start();
    }

    public void stop() {
        handle.removeCallbacksAndMessages(null);
        handle = null;
        imageView = null;
    }

    @SuppressLint("HandlerLeak")
    Handler handle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                if (picByte != null) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(picByte, 0, picByte.length);
                    if (imageView != null && bitmap != null) imageView.setImageBitmap(bitmap);
                }
            }
        }
    };

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            try {
                URL url = new URL(uri);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setReadTimeout(10000);

                if (conn.getResponseCode() == 200) {
                    InputStream fis = conn.getInputStream();
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] bytes = new byte[1024];
                    int length = -1;
                    while ((length = fis.read(bytes)) != -1) {
                        bos.write(bytes, 0, length);
                    }
                    picByte = bos.toByteArray();
                    bos.close();
                    fis.close();
                    if(handle != null) {
                        Message message = new Message();
                        message.what = 1;
                        handle.sendMessage(message);
                    }
                }


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

}
