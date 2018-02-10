package cn.com.protruly.soundrecorder.clip;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import cn.com.protruly.soundrecorder.util.LogUtil;

/**
 * Created by sqf on 17-8-14.
 */

public class Mp3Decoder {

    private static final String TAG = "Mp3Decoder";

    private Context mContext;
    private String mFilePath;

    private int mSoundTrackCount;
    private int mSoundChannelCount;
    private int mBitRate;
    private int mSampleRate;

    private ByteBuffer mDecodedBytes;  // Raw audio data
    private ShortBuffer mDecodedSamples;  // shared buffer with mDecodedBytes.

    private int mNumSamples;
    private int mAvgBitRate;

    private long mFileSize;
    private File mFile;

    // Member variables for hack (making it work with old version, until app just uses the samples).
    private int mNumFrames;
    private int[] mFrameGains;
    private int[] mFrameLens;
    private int[] mFrameOffsets;

    private HandlerThread mHandlerThread = new HandlerThread("Mp3Decode");
    private Handler mProcessHandler;
    private DecodedSampleProcessor mDecodedSampleProcessor;// = new DecodedSampleProcessor();

    private int mApproximateSampleTime;

    public Mp3Decoder(Context context, String filePath, DecodedSampleProcessor decodedSampleProcessor) {
        mContext = context;
        mFilePath = filePath;
        mFile = new File(filePath);
        mFileSize = (int)mFile.length();
        mDecodedSampleProcessor = decodedSampleProcessor;
        mHandlerThread.start();
        mProcessHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                LogUtil.i(TAG, "Mp3Decoder :: handleMessage in handler -----------");
            }
        };
        mApproximateSampleTime = ClipView.calculateApproximateSampleTime(context);
    }

    public void parseWaveFormAsynchronously() {
        try {
            final MediaExtractor extractor = new MediaExtractor();
            MediaFormat format = null;
            File file = new File(mFilePath);
            if(!file.exists()) {
                LogUtil.e(TAG, "File not found:" + mFilePath);
                throw new IllegalArgumentException("File not found:" + mFilePath);
            }
            extractor.setDataSource(mFilePath);
            /*
            LogUtil.i(TAG, "setDataSource .... mFilePath:" + mFilePath + " mFileSize:" + mFileSize);
            mSoundTrackCount = extractor.getTrackCount();
            if(mSoundTrackCount == 0) return;

            String mime = "";
            // find and select the first audio track present in the file.
            int i = 0;
            for (i=0; i < mSoundTrackCount; i++) {
                format = extractor.getTrackFormat(i);
                mime = format.getString(MediaFormat.KEY_MIME);
                LogUtil.i(TAG, "mime:" + mime);
                if (mime.startsWith("audio/")) {
                    extractor.selectTrack(i);
                    break;
                }
            }
            mSoundChannelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            mSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            mBitRate = format.getInteger(MediaFormat.KEY_BIT_RATE);
            */
            Mp3FileInfo mp3FileInfo = new Mp3FileInfo(extractor);
            long duration = mp3FileInfo.getDuration();
            int expectedNumSamples = (int)((duration / 1000000.f) * mSampleRate + 0.5f);

            WaveFormProcessListener listener = mDecodedSampleProcessor.getWaveFormProcessListener();
            listener.onMediaInfoExtracted(mp3FileInfo);
            /*
            if (firstSampleData && mime.equals("audio/mp4a-latm") && sampleSize == 2) {
                LogUtil.i(TAG, "firstSampleData is true,  mime: audio/mp4a-latm , sampleSize is 2");
                extractor.advance();
            }
            */
            long frameDuration = mp3FileInfo.getFrameDuration();
            extractor.selectTrack(0);
            MediaCodec codec = MediaCodec.createDecoderByType(mp3FileInfo.getMime());
            SampleCallback callback = new SampleCallback(extractor, codec, mDecodedSampleProcessor, duration, mApproximateSampleTime, frameDuration);
            codec.setCallback(callback, mProcessHandler);
            codec.configure(mp3FileInfo.getMediaFormat(), null, null, 0);
            codec.start();
        } catch (IOException e) {
            LogUtil.e(TAG, " IOException e:" + e.getMessage());
        } catch (MediaCodec.CodecException e) {
            LogUtil.e(TAG, " MediaCodec.CodecException e:" + e.getMessage());
        }
    }

    private int getSamplesPerFrame() {
        return 1024;
    }


    /*
    public void parseWaveFormSynchronously() {
        try {
            MediaExtractor extractor = new MediaExtractor();
            MediaFormat format = null;
            extractor.setDataSource(mFilePath);
            LogUtil.i(TAG, "setDataSource .... mFilePath:" + mFilePath + " mFileSize:" + mFileSize);
            mSoundTrackCount = extractor.getTrackCount();
            if(mSoundTrackCount == 0) return;

            String mime = "";
            // find and select the first audio track present in the file.
            int i = 0;
            for (i=0; i < mSoundTrackCount; i++) {
                format = extractor.getTrackFormat(i);
                mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio/")) {
                    extractor.selectTrack(i);
                    break;
                }
            }
            mSoundChannelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            mSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            long duration = format.getLong(MediaFormat.KEY_DURATION);//in us
            int expectedNumSamples = (int)((duration / 1000000.f) * mSampleRate + 0.5f);
            LogUtil.i(TAG, "duration:" + duration + " expectedNumSamples:" + expectedNumSamples + " mSampleRate:" + mSampleRate +
                    " mSoundTrackCount:" + mSoundTrackCount +
                    " mSoundChannelCount:" + mSoundChannelCount);

            MediaCodec codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
            codec.configure(format, null, null, 0);
            codec.start();

            int decodedSamplesSize = 0;
            byte [] decodeSamples = null;
            ByteBuffer[] inputBuffers = codec.getInputBuffers();
            ByteBuffer[] outputBuffers = codec.getOutputBuffers();
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            boolean firstSampleData = true;
            boolean doneReading = false;
            long presentationTime = 0;
            long totalSizeRead = 0;
            mDecodedBytes = ByteBuffer.allocate(1<<20);
            while(true) {
                int inputBufferIndex = codec.dequeueInputBuffer(100);
                if (!doneReading && inputBufferIndex >= 0) {
                    int sampleSize = extractor.readSampleData(inputBuffers[inputBufferIndex], 0);
                    if (firstSampleData && mime.equals("audio/mp4a-latm") && sampleSize == 2) {
                        LogUtil.i(TAG, "firstSampleData is true,  mime: audio/mp4a-latm , sampleSize is 2");
                        extractor.advance();
                    } else if (sampleSize < 0) {
                        // All samples have been read.
                        codec.queueInputBuffer(inputBufferIndex, 0, 0, -1, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        doneReading = true;
                    } else {
                        presentationTime = extractor.getSampleTime();
                        long cacheDuration = extractor.getCachedDuration();
                        int sampleFlags = extractor.getSampleFlags();
                        LogUtil.i(TAG, "presentationTime:" + presentationTime + " extractor.cacheDuration:" + cacheDuration + " extractor.sampleFlags:" + sampleFlags + " ");
                        codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, presentationTime, 0);
                        extractor.advance();
                        totalSizeRead += sampleSize;
                        LogUtil.i(TAG, "totalSizeRead:" + totalSizeRead + " sampleSize:" + sampleSize + " mFileSize:" + mFileSize);

//                        if(totalSizeRead >= mFileSize) {
//                            LogUtil.i(TAG, "totalSizeRead:" + totalSizeRead + " sampleSize:" + sampleSize + " mFileSize;");
//                            extractor.release();
//                            codec.stop();
//                            codec.release();
//                            return;
//                        }

                    }
                    firstSampleData = false;
                }
                int outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 100);
                if(outputBufferIndex > 0 && bufferInfo.size > 0) {
                    if(decodedSamplesSize < bufferInfo.size) {
                        decodedSamplesSize = bufferInfo.size;
                        decodeSamples = new byte[decodedSamplesSize];
                    }
                    outputBuffers[outputBufferIndex].get(decodeSamples, 0, bufferInfo.size);
                    outputBuffers[outputBufferIndex].clear();
                    //check buffer is big enough. Resize it if it's too small.
                    if(mDecodedBytes.remaining() < bufferInfo.size) {
                        int position = mDecodedBytes.position();
                        int newSize = (int)((position * (1.0 * mFileSize / totalSizeRead)) * 1.2);
                        if (newSize - position < bufferInfo.size + 5 * (1<<20)) {
                            newSize = position + bufferInfo.size + 5 * (1<<20);
                        }
                        ByteBuffer newDecodedBytes = null;
                        int retry = 10;
                        while(retry > 0) {
                            try {
                                newDecodedBytes = ByteBuffer.allocate(newSize);
                                break;
                            } catch (OutOfMemoryError oome) {
                                // setting android:largeHeap="true" in <application> seem to help not
                                // reaching this section.
                                retry--;
                            }
                        }
                        if (retry == 0) {
                            // Failed to allocate memory... Stop reading more data and finalize the
                            // instance with the data decoded so far.
                            break;
                        }
                        //ByteBuffer newDecodedBytes = ByteBuffer.allocate(newSize);
                        mDecodedBytes.rewind();
                        newDecodedBytes.put(mDecodedBytes);
                        mDecodedBytes = newDecodedBytes;
                        mDecodedBytes.position(position);
                    }
                    mDecodedBytes.put(decodeSamples, 0, bufferInfo.size);
                    codec.releaseOutputBuffer(outputBufferIndex, false);
                } else if( outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    outputBuffers = codec.getOutputBuffers();
                } else if(outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // Subsequent data will conform to new format.
                    // We could check that codec.getOutputFormat(), which is the new output format,
                    // is what we expect.
                }
                if((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0 ||
                        (mDecodedBytes.position() / (2 * mSoundChannelCount) >= expectedNumSamples)){
                    break;
                }
            }
            mNumSamples = mDecodedBytes.position() / (mSoundChannelCount * 2);
            mDecodedBytes.rewind();
            mDecodedBytes.order(ByteOrder.LITTLE_ENDIAN);
            mDecodedSamples = mDecodedBytes.asShortBuffer();
            LogUtil.i(TAG, "mNumSamples:" + mNumSamples + " mChannelCount:" + mSoundChannelCount);
            mAvgBitRate = (int)((mFileSize * 8) * ((float)mSampleRate / mNumSamples) / 1000);

            LogUtil.i(TAG, "mAvgBitRate:" + mAvgBitRate + " mFileSize:" + mFileSize + " mSampleRate:" + mSampleRate + " mNumSamples:" + mNumSamples);

            extractor.release();
            extractor = null;
            codec.stop();
            codec.release();
            codec = null;


            // Temporary hack to make it work with the old version.
            mNumFrames = mNumSamples / getSamplesPerFrame();
            if (mNumSamples % getSamplesPerFrame() != 0){
                mNumFrames++;
            }
            mFrameGains = new int[mNumFrames];
            mFrameLens = new int[mNumFrames];
            mFrameOffsets = new int[mNumFrames];
            int j;
            int gain, value;
            int frameLens = (int)((1000 * mAvgBitRate / 8) *
                    ((float)getSamplesPerFrame() / mSampleRate));
            for (i=0; i<mNumFrames; i++){
                gain = -1;
                for(j=0; j<getSamplesPerFrame(); j++) {
                    value = 0;
                    for (int k=0; k<mSoundChannelCount; k++) {
                        if (mDecodedSamples.remaining() > 0) {
                            value += Math.abs(mDecodedSamples.get());
                        }
                    }
                    value /= mSoundChannelCount;
                    if (gain < value) {
                        gain = value;
                    }
                }
                mFrameGains[i] = (int)Math.sqrt(gain);  // here gain = sqrt(max value of 1st channel)...
                mFrameLens[i] = frameLens;  // totally not accurate...
                mFrameOffsets[i] = (int)(i * (1000 * mAvgBitRate / 8) *  //  = i * frameLens
                        ((float)getSamplesPerFrame() / mSampleRate));

                //LogUtil.i(TAG, "mFrameGains [" + i + "]:" + mFrameGains[i]);
            }
            mDecodedSamples.rewind();

        } catch (IOException e) {
            LogUtil.e(TAG, e.getMessage());
        } finally {

        }
    }
    */
}
