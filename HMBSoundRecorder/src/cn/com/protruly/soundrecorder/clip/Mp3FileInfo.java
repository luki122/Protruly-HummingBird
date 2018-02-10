package cn.com.protruly.soundrecorder.clip;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaPlayer;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedHashMap;

import cn.com.protruly.soundrecorder.util.LogUtil;

/**
 * Created by sqf on 17-8-29.
 */

public class Mp3FileInfo {

    private static final String TAG = "Mp3FileInfo";

    private static final int DEFAULT_MAX_INPUT_SIZE = 4096;

    private int mSoundTrackCount;
    private MediaExtractor mMediaExtractor;

    public class TrackFormatInfo {
        public MediaFormat mediaFormat;
        public int trackIndex;
        public String mime;
        public long duration;
        public int channelCount;
        public int sampleRate;
        public int bitRate;
        public long oneFrameDuration;
        public int maxInputSize;
    }

    private HashMap<Integer, TrackFormatInfo> mTrackFormatInfos = new LinkedHashMap<>();

    public Mp3FileInfo(MediaExtractor extractor) {
        mMediaExtractor = extractor;
        mSoundTrackCount = extractor.getTrackCount();
        LogUtil.i(TAG, "mSoundTrackCount:" + mSoundTrackCount);
        if(mSoundTrackCount == 0) return;

        // find and select the first audio track present in the file.
        int i = 0;
        for (i = 0; i < mSoundTrackCount; i ++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            LogUtil.i(TAG, "mime:" + mime);
            if (mime.startsWith("audio/")) {
                extractor.selectTrack(i);
                TrackFormatInfo trackFormatInfo = new TrackFormatInfo();
                trackFormatInfo.mediaFormat = format;
                trackFormatInfo.trackIndex = i;
                trackFormatInfo.duration = format.getLong(MediaFormat.KEY_DURATION);//in us
                trackFormatInfo.mime = mime;
                trackFormatInfo.channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                trackFormatInfo.sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                //trackFormatInfo.oneFrameDuration =

                try {
                    trackFormatInfo.maxInputSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                } catch (Exception e) {
                    //no such key
                    LogUtil.i(TAG, "no KEY_MAX_INPUT_SIZE in MediaFormat");
                }
                //trackFormatInfo.bitRate = format.getInteger(MediaFormat.KEY_BIT_RATE);

                mTrackFormatInfos.put(i, trackFormatInfo);
                extractor.unselectTrack(i);
            }
        }
        /*
        MediaFormat format = null;
        for (i=0; i < mSoundTrackCount; i++) {
            format = extractor.getTrackFormat(i);
            mime = format.getString(MediaFormat.KEY_MIME);
            LogUtil.i(TAG, "mime:" + mime);
            if (mime.startsWith("audio/")) {
                mCurrentTrack = i;
                extractor.selectTrack(mCurrentTrack);
                break;
            }
        }

        mMediaFormat = format;
        mMime = format.getString(MediaFormat.KEY_MIME);
        mDuration = format.getLong(MediaFormat.KEY_DURATION);//in us
        mSoundChannelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        mSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        //mBitRate = format.getInteger(MediaFormat.KEY_BIT_RATE);

        LogUtil.i(TAG, "mDuration:" + mDuration +
                " mSampleRate:" + mSampleRate +
                " mSoundTrackCount:" + mSoundTrackCount +
                " mSoundChannelCount:" + mSoundChannelCount +
                " mBitRate:" + mBitRate);
                */
    }

    public long getFrameDuration(int trackIndex) {
        ByteBuffer inputBuffer = ByteBuffer.allocate(getMaxInputSize(trackIndex));
        mMediaExtractor.selectTrack(trackIndex);
        mMediaExtractor.readSampleData(inputBuffer, 0);
        //skip first sample
        if (mMediaExtractor.getSampleTime() == 0) {
            mMediaExtractor.advance();
        }
        mMediaExtractor.readSampleData(inputBuffer, 0);
        long firstSampleTime = mMediaExtractor.getSampleTime();
        mMediaExtractor.advance();
        mMediaExtractor.readSampleData(inputBuffer, 0);
        long secondSampleTime = mMediaExtractor.getSampleTime();
        long frameDuration = Math.abs(secondSampleTime - firstSampleTime);
        LogUtil.i(TAG, "Frame Duration is " + frameDuration + " in track " + trackIndex);
        mMediaExtractor.unselectTrack(trackIndex);
        return frameDuration;
    }


    public int getSoundTrackCount() {
        return mSoundTrackCount;
    }

    public long getTrackDuration(int trackIndex) {
        return mTrackFormatInfos.get(trackIndex).duration;
    }

    public String getTrackMime(int trackIndex) {
        return mTrackFormatInfos.get(trackIndex).mime;
    }

    public MediaFormat getMediaFormat(int trackIndex) {
        return mTrackFormatInfos.get(trackIndex).mediaFormat;
    }

    public int getMaxInputSize(int trackIndex) {
        int maxInputSize = mTrackFormatInfos.get(trackIndex).maxInputSize;
        if(maxInputSize == 0) {
            return DEFAULT_MAX_INPUT_SIZE;
        }
        return maxInputSize;
    }

    //functions below get the first sound track of a audio file
    public long getDuration() {
        return getTrackDuration(0);
    }
    public String getMime() { return getTrackMime(0); }
    public MediaFormat getMediaFormat() { return getMediaFormat(0); }
    public int getMaxInputSize() { return getMaxInputSize(0); }
    public long getFrameDuration() { return getFrameDuration(0); }


    public TrackFormatInfo getTrackFormatInfo(int trackIndex) {
        return mTrackFormatInfos.get(trackIndex);
    }
}
