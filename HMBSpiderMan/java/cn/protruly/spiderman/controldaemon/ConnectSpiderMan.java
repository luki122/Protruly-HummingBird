package cn.protruly.spiderman.controldaemon;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by lijia on 17-5-3.
 */

public class ConnectSpiderMan {

    private final String TAG = "ConnectSpiderMan";
    private final String SOCKET_NAME = "socket_spiderman";

    private InputStream mIn;
    private OutputStream mOut;
    private LocalSocket mSocket;
    private final byte buf[] = new byte[1024];

    public ConnectSpiderMan() {
    }

    public int transact(int tagInt) {
        if (!connect()) {
            Log.v(TAG, "connecting failed");
            return -1;
        }
        if (!writeInt(tagInt)) {
            Log.v(TAG, "write data failed? reconnect");
            if(!connect() || !writeInt(tagInt)){
                return -1;
            }
        }

        final int replyLength = readReply();

        if (replyLength > 0) {
            Log.v(TAG, "replyLength = " + replyLength);
            return 1;
        } else {
            return -1;
        }

    }

    private boolean connect() {

        if (mSocket != null) {
            return true;
        }
        Log.v(TAG, "connecting......");
        try {

            mSocket = new LocalSocket();
            LocalSocketAddress address = new LocalSocketAddress(SOCKET_NAME, LocalSocketAddress.Namespace.RESERVED);
            mSocket.connect(address);

            mIn = mSocket.getInputStream();
            mOut = mSocket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
            disConnect();
            return false;
        }
        return true;
    }

    private boolean writeInt(int tagInt) {

        try {
            mOut.write(tagInt);
        } catch (IOException e) {
            e.printStackTrace();
            disConnect();
            return false;
        }
        return true;
    }

    private int readReply() {

        try {
            return mIn.read();
        } catch (IOException e) {
            e.printStackTrace();
            disConnect();
            return -1;
        }
    }

    public void disConnect() {

        Log.v(TAG, "disconnecting......");
        try {
            if(mSocket != null){
                mSocket.close();
            }
            if(mSocket != null){
                mIn.close();
            }
            if(mSocket != null){
                mOut.close();
            }
            mSocket = null;
            mIn = null;
            mOut = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
