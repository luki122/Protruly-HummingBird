package cn.com.protruly.soundrecorder.clip;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.support.annotation.NonNull;

import java.nio.ByteBuffer;

import cn.com.protruly.soundrecorder.util.LogUtil;

/**
 * Created by sqf on 17-8-24.
 *
 * As of LOLLIPOP, this SampleCallback works.
 * Use synchronous method before LOLLIPOP.
 */

public class SampleCallback extends MediaCodec.Callback {

    private static final String TAG = "SampleCallback";
    private static final int MAX_SAMPLE_TIMES = 1000;
    private static final int MIN_SAMPLE_TIMES = 16;
    //private static final int MIN_SAMPLE_TIME_INTERVAL = 62500; // 1000 / 16 = 62.5
    private long mSampleTimeInterval;
    private long mSeekTime = 0;
    private long mDuration;// in us
    private MediaExtractor mMediaExtractor;
    private MediaCodec mMediaCodec;
    private MediaFormat mOutputFormat;
    private DecodedSampleProcessor mDecodedSampleProcessor;
    private int mApproximateSampleTimes;
    private long mFrameDuration;

    private boolean mReadEnds;

    public SampleCallback(MediaExtractor extractor, MediaCodec codec, DecodedSampleProcessor decodedSampleProcessor, long duration, int sampleTimes, long frameDuration) {
        super();
        mDuration = duration;
        mMediaExtractor = extractor;
        mMediaCodec = codec;
        mDecodedSampleProcessor = decodedSampleProcessor;
        mApproximateSampleTimes = sampleTimes;
        mFrameDuration= frameDuration;
        calculateSampleTimeInterval();
    }

    private void calculateSampleTimeInterval() {
        mSampleTimeInterval = mDuration / mApproximateSampleTimes;
        LogUtil.i("mSampleTimeInterval", "dddddddddddddddd :" + mSampleTimeInterval + " mDuration:" + mDuration );
        /*
        if(mSampleTimeInterval < MIN_SAMPLE_TIME_INTERVAL) {
            mSampleTimeInterval = MIN_SAMPLE_TIME_INTERVAL;
        }
        */
        if(mSampleTimeInterval < mFrameDuration) {
            mSampleTimeInterval = mFrameDuration;
        }
        //LogUtil.i("mSampleTimeInterval", "sample time interval calculated:" + mSampleTimeInterval);
    }

    @Override
    public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
        if(mReadEnds) return;
        LogUtil.i(TAG, "onInputBufferAvailable index:" + index + " not synchronized  -------- ");
        ByteBuffer inputBuffer = codec.getInputBuffer(index);
        LogUtil.i(TAG, "input buffer capacity: " + inputBuffer.capacity());
        mMediaExtractor.seekTo(mSeekTime, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        LogUtil.i("dddddd", "mSeekTime: " + mSeekTime + " mSampleTimeInterval:" + mSampleTimeInterval);
        int sampleSize = mMediaExtractor.readSampleData(inputBuffer, 0);
        LogUtil.i(TAG, "onInputBufferAvailable sampleSize:" + sampleSize);
        if (sampleSize <= 0 || mSeekTime > mDuration) {
            //LogUtil.i(TAG, "read finished.");
            LogUtil.i(TAG, "read finished. -------------------------");
            codec.queueInputBuffer(index, 0, 0, -1, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            //codec.signalEndOfInputStream();
            mReadEnds = true;
            return;
        }
        long presentationTime = mMediaExtractor.getSampleTime();
        codec.queueInputBuffer(index, 0, sampleSize, presentationTime, 0);
        //long trackIndex = mMediaExtractor.getSampleTrackIndex();
        LogUtil.i(TAG, "presentationTime:" + presentationTime /*+ " trackIndex:" + trackIndex*/);
        mMediaExtractor.advance();
        mSeekTime += mSampleTimeInterval;
    }

    @Override
    public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
        LogUtil.i(TAG, "onOutputBufferAvailable" );
        ByteBuffer outputBuffer = codec.getOutputBuffer(index);
        MediaFormat outputFormat = codec.getOutputFormat(index);
        LogUtil.i(TAG, "outputbuffer index:" + index);
        //process data here
        mDecodedSampleProcessor.process(outputBuffer, info);
        codec.releaseOutputBuffer(index, false);
        if(DecodedSampleProcessor.readEnds(info)) {
            //end ...
            LogUtil.i(TAG, "out put finished...........................................");


            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
            // we just select the first track, if read ends, we need to unselect it.
            mMediaExtractor.unselectTrack(0);
            mMediaExtractor.release();
            mMediaExtractor = null;
        }
    }

    @Override
    public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
        LogUtil.i(TAG, "onError" );
    }

    @Override
    public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
        LogUtil.i(TAG, "onOutputFormatChanged" );
        mOutputFormat = format;
    }
}
