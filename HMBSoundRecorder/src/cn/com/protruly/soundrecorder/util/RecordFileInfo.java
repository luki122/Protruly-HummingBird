package cn.com.protruly.soundrecorder.util;

import android.media.MediaMetadataRetriever;

import java.io.File;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by wenwenchao on 2017/8/20.
 */

public class RecordFileInfo extends FileInfo {
    private long timeLong = -1;
    private String nameLabel;
    private String timeLabel;

    List<Long> markTimeList;
    public RecordFileInfo(File file){
        super(file);
        this.nameLabel = getStringLabel(name);

        markTimeList = new ArrayList<>();
    }
    public RecordFileInfo(String path){
        this(new File(path));
    }

    public void addOneMark(long timepoint){
        markTimeList.add(timepoint);
    }
    public void addMarkList(List<Long> list){
        markTimeList.addAll(list);
    }
    public void removeOneMark(long timepoint){ markTimeList.remove(timepoint);}
    public void clearAllMarks(){
        markTimeList.clear();
    }

    public List<Long> getMarkTimeList() {
        return markTimeList;
    }

    private String getStringLabel(String name){
        int index = name.lastIndexOf(".");
        if(index>0){
            return name.substring(0,index);
        }
        return null;
    }

    public String getNameLabel(){
        if(file == null) return null;
        return this.nameLabel;
    }

    public void setTimeLong(long time){
        if(file==null)return;
        this.timeLong = time;
    }
    public long getTimeLong(){
        if(file==null)return -1;
        return timeLong;
    }

    public String getTimeLabel() {
        return timeLabel;
    }

    public void setTimeLabel(String timeLabel) {
        this.timeLabel = timeLabel;
    }
}
