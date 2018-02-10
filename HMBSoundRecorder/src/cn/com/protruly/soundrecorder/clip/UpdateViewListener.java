package cn.com.protruly.soundrecorder.clip;

/**
 * Created by sqf on 17-9-11.
 */

public interface UpdateViewListener {

    void updateTimeInfo(ClipView.TimeInfo timeInfo);
    void notifyProcessFinished();
}
