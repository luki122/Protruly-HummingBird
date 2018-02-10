package cn.com.protruly.soundrecorder.clip;

import java.util.ArrayList;

/**
 * Created by sqf on 17-8-29.
 */

public interface WaveFormProcessListener {

    int MSG_REFRESH_UI = 1234;

    void refresh(ArrayList<WaveFormData> data, boolean finished);
    void setMaxAmplitude(float maxAmplitude);
    void onMediaInfoExtracted(Mp3FileInfo mp3FileInfo);
}
