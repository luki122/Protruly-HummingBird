package cn.com.protruly.soundrecorder;

import android.os.Handler;

import java.io.File;
import java.util.List;

/**
 * Created by liushitao on 17-9-8.
 */

public interface IRecordService {
    void startRecord();

    void pauseRecord();

    void finishRecord();

    void cancelRecord();

    File getRecordFile();

    File getRecordLastFile();

    String getRecordFileName();

    int getRecordState();

    boolean isProcessResume();

    int getMaxAmplitude();

    void setHandler(Handler handler);

    List<Recorder.FrameInfo> getAmplitudeList();

    void removeFloatWindow();

    void ShowFloatWindow(String timeStr);

    void setRecordTime(String timeStr);

    void updateFloatWindowViewLayout(String timeStr);

    void setIsShowWave(boolean isShowWave);

    void createNotification();

    void clearNotification();

    void updateNotificationTime(String time);

    void updateNotificationStatus(String contentTitle, String intentStr,String buttonStatus);
}
