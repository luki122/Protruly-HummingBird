package cn.com.protruly.soundrecorder.clip;

import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaMuxer;
import android.os.AsyncTask;

import java.io.IOException;
import java.nio.ByteBuffer;

import cn.com.protruly.soundrecorder.util.DatabaseUtil;
import cn.com.protruly.soundrecorder.util.LogUtil;
import cn.com.protruly.soundrecorder.R;

/**
 * Created by sqf on 17-9-13.
 */

public class Mp3ClipperTask extends AsyncTask<Void, Float, Boolean> implements DialogInterface.OnCancelListener {

    private static final String TAG = "Mp3ClipperTask";

    private ProgressDialogManager mProgressDialogManager;

    private Mp3FileInfo mFileInfo;
    private MediaExtractor mMediaExtractor;
    private MediaMuxer mMediaMuxer;

    private float mProgress;

    private float [] mSoundTrackCopyProgress;
    private Context mContext;


    private String mSrcFilePath;
    private long mStartTime;
    private long mEndTime;
    private String mDstFilePath;

    public Mp3ClipperTask(Context context, String srcFilePath, long startTime, long endTime, String dstFilePath) {
        mContext = context;
        mMediaExtractor = new MediaExtractor();
        mSrcFilePath = srcFilePath;
        mStartTime = startTime;
        mEndTime = endTime;
        mDstFilePath = dstFilePath;

        mProgress = 0;

        mProgressDialogManager = new ProgressDialogManager(context);
        mProgressDialogManager.setCancelListener(this);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mProgressDialogManager.showProgressDialog(R.string.clipping);
    }

    @Override
    protected void onProgressUpdate(Float... values) {
        if(mProgressDialogManager != null) {
            mProgressDialogManager.setProgress((int)(values[0] * 100));
        }
    }


    @Override
    protected Boolean doInBackground(Void... params) {
        return clip();
    }

    public boolean clip() {
        try {
            LogUtil.i(TAG, "start:" + mStartTime + " endTime:" + mEndTime + "dstFilePath:" + mDstFilePath);
            mMediaExtractor.setDataSource(mSrcFilePath);
            mMediaMuxer = new MediaMuxer(mDstFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            mFileInfo = new Mp3FileInfo(mMediaExtractor);
            int trackCount = mFileInfo.getSoundTrackCount();

            mSoundTrackCopyProgress = new float[trackCount];

            for(int i = 0; i< trackCount; i++) {
                if(isCancelled()) {
                    break;
                }
                LogUtil.i(TAG, " add track .... " + i);
                Mp3FileInfo.TrackFormatInfo info = mFileInfo.getTrackFormatInfo(i);
                mMediaMuxer.addTrack(info.mediaFormat);
            }
            if(isCancelled()) {
               return false;
            }
            mMediaMuxer.start();
            for(int i = 0; i< trackCount; i++) {
                LogUtil.i(TAG, "copy track:" + i);
                copyTrack(i, mStartTime, mEndTime);
            }
            mMediaMuxer.stop();
            mMediaMuxer.release();
            mMediaExtractor.release();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        super.onPostExecute(aBoolean);
        DatabaseUtil databaseUtil = new DatabaseUtil(mContext);
        databaseUtil.insertRecordFile(mDstFilePath);
        mProgressDialogManager.dismissProgressDialog();
    }

    private void copyTrack(int trackIndex, long startTime, long endTime) {
        long frameDuration = mFileInfo.getFrameDuration(trackIndex);
        int maxInputSize = mFileInfo.getMaxInputSize(trackIndex);
        ByteBuffer inputBuffer = ByteBuffer.allocate(maxInputSize);
        LogUtil.i(TAG, "copyTrack maxInputSize:" + maxInputSize + " in track:" + trackIndex);
        MediaCodec.BufferInfo audioInfo = new MediaCodec.BufferInfo();
        audioInfo.presentationTimeUs = 0;
        mMediaExtractor.selectTrack(trackIndex);
        mMediaExtractor.seekTo(startTime, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);

        while (true) {
            if(isCancelled()) {
                break;
            }
            int sampleSize = mMediaExtractor.readSampleData(inputBuffer, 0);
            if (sampleSize < 0) {
                break;
            }
            long presentationTimeUs = mMediaExtractor.getSampleTime();
            if (presentationTimeUs > endTime + frameDuration) {
                break;
            }
            mMediaExtractor.advance();
            audioInfo.offset = 0;
            audioInfo.size = sampleSize;
            mMediaMuxer.writeSampleData(trackIndex, inputBuffer, audioInfo);

            LogUtil.i(TAG, "copyTrack original trackIndex is " + trackIndex +
                    "; presentationTimeUs is " + presentationTimeUs +
                    " write into:" + audioInfo.presentationTimeUs);

            float trackProgress = (float)(audioInfo.presentationTimeUs + frameDuration) / (float)(endTime - startTime);
            mSoundTrackCopyProgress[trackIndex] = trackProgress;

            mProgress = getProgress();
            publishProgress(mProgress);

            LogUtil.i(TAG, "trackProgress:" + trackProgress + " mProgress:" + mProgress + " mStartTime:" + mStartTime + " mEndTime:" + mEndTime);
            audioInfo.presentationTimeUs += frameDuration;
        }
        mMediaExtractor.unselectTrack(trackIndex);
    }

    private float getProgress() {
        float progress = 0.0f;
        for(int i = 0; i < mSoundTrackCopyProgress.length; i++) {
            progress += mSoundTrackCopyProgress[i];
        }
        LogUtil.i(TAG, "progress:" + progress);
        if(progress > 1.0f) { mProgress = 1.0f; }
        return progress;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        cancel(true);
    }
}
