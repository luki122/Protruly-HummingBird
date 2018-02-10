package cn.com.protruly.soundrecorder.clip;

/**
 * Created by sqf on 17-8-25.
 */

public class WaveFormData {

    public long mPresentationTime;
    public float mAmplitude;

    public WaveFormData(long presentationTime, float amplitude) {
        mPresentationTime = presentationTime;
        mAmplitude = amplitude;
    }

    /**
     * unit is us. 1000 us is 1ms.
     * @return presentation time
     */
    public long getPresentationTime() {
        return mPresentationTime;
    }

    public float getAmplitude() {
        return mAmplitude;
    }

}
