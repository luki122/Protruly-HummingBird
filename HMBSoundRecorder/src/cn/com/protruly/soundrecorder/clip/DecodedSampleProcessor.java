package cn.com.protruly.soundrecorder.clip;

import android.media.MediaCodec;
import android.os.Handler;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import cn.com.protruly.soundrecorder.util.LogUtil;

/**
 * Created by sqf on 17-8-24.
 */

public class DecodedSampleProcessor {

    private static final String TAG = "DecodedSampleProcessor";
    private int mRealSampleTimes;
    private WaveFormProcessListener mWaveFormProcessListener;
    private Handler mUiHandler;
    private float mCurrentMaxAmplitude;

    private ArrayList<WaveFormData> mWaveFormDataList = new ArrayList<>();

    public DecodedSampleProcessor(WaveFormProcessListener listener, Handler uiHandler) {
        mWaveFormProcessListener = listener;
        mUiHandler = uiHandler;
    }

    public WaveFormProcessListener getWaveFormProcessListener() {
        return mWaveFormProcessListener;
    }

    public void process(ByteBuffer decodedSample, MediaCodec.BufferInfo info) {
        //LogUtil.i(TAG, "process: decodedSample limit:" + decodedSample.limit() + " position:" + decodedSample.position());
        //LogUtil.i(TAG, "process: info.size:" + info.size + " info.offset:" + info.offset);
        if(decodedSample.remaining() <= 0) return;
        if(info.size == 0) return;
        mRealSampleTimes++;
        //LogUtil.i(TAG, "mRealSampleTimes:" + mRealSampleTimes);
        //dumpDecodedSampleFromPositionToLimit(decodedSample);
        decodedSample.order(ByteOrder.LITTLE_ENDIAN);
        ShortBuffer shortBuffer = decodedSample.asShortBuffer();
        double tmp = calculateAmplitude(shortBuffer);
        float amplitude = (float)tmp;
        //LogUtil.i(TAG, "tmp:" + tmp + " amplitude:" + amplitude + " --- - -- -- --- --- - --  --  - ----- ");
        mCurrentMaxAmplitude = Math.max(amplitude, mCurrentMaxAmplitude);
        WaveFormData waveFormData = new WaveFormData(info.presentationTimeUs, amplitude);
        mWaveFormDataList.add(waveFormData);
        mWaveFormProcessListener.setMaxAmplitude(mCurrentMaxAmplitude);
        int finished = readEnds(info) ? 1 : 0;
        if(finished == 1) {
            LogUtil.i(TAG, "mRealSampleTimes -----------------------------> " + mRealSampleTimes);
        }
        mUiHandler.removeMessages(WaveFormProcessListener.MSG_REFRESH_UI);
        mUiHandler.obtainMessage(WaveFormProcessListener.MSG_REFRESH_UI, finished, 0, mWaveFormDataList).sendToTarget();
    }

    private void dumpDecodedSampleFromPositionToLimit(ByteBuffer decodedSample) {
        for(int i=decodedSample.position(); i < decodedSample.limit(); i++) {
            LogUtil.i(TAG, "decodeSample data:" + decodedSample.get(i) + " i:" + i);
        }
    }

    public static boolean readEnds(MediaCodec.BufferInfo bufferInfo) {
        return (bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
    }

    private double calculateAmplitude(ShortBuffer decodedSamples) {
        //long time = System.currentTimeMillis();
        int bufferLength = decodedSamples.remaining();
        long squarePlus = 0;
        for(int i = decodedSamples.position(); i < decodedSamples.limit(); i++) {
            short value = decodedSamples.get(i);
            squarePlus = value * value;
            //LogUtil.i(TAG, "squarePlus:" + squarePlus + " value:" + value);
        }
        double amplitude = Math.sqrt(squarePlus / bufferLength);
        //LogUtil.i(TAG, "amplitude:" + amplitude + " time:" + (System.currentTimeMillis() - time));
        return amplitude;
    }

}
