package cn.com.protruly.filemanager.globalsearch;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.com.protruly.filemanager.enums.Category;
import cn.com.protruly.filemanager.enums.FileInfo;
import cn.com.protruly.filemanager.utils.FileSortHelper;
import cn.com.protruly.filemanager.utils.GlobalConstants;
import cn.com.protruly.filemanager.R;
import cn.com.protruly.filemanager.utils.Util;

/**
 * Created by wenwenchao on 17-6-20.
 */

public class SearchSectionManager {

    public List<FileInfo> fileinfolist ;
    public List<FileInfo> picfilelists ;
    public List<FileInfo> musicfilelist ;
    public List<FileInfo> videofilelists ;
    public List<FileInfo> otherfilelists ;
    public HashSet<FileInfo> selectedFileSet;
    public HashMap<String,FileInfo> sectionFileMap;
    public HashMap<String,List> sectionFileListMap;
    private Boolean hidenFile = true;

    public final String[] sectionPath = {"picture_fileSection","music_fileSection","vedio_fileSection","other_fileSection"};
    public final int[] sectionNameID = {R.string.category_picture,R.string.category_music,R.string.category_video,R.string.category_other};
    //public List sectionFileInfoList;
   // public int[] sectionFileType = {Category.Picture,Category.Video,Category.Other};

    private FileSortHelper fileSortHelper;
    private Context mContext;

    public SearchSectionManager(Context context){

        this.mContext = context;
        fileinfolist = new ArrayList<FileInfo>();
        selectedFileSet = new HashSet<FileInfo>();

        picfilelists = new ArrayList<FileInfo>();
        musicfilelist = new ArrayList<FileInfo>();
        videofilelists = new ArrayList<FileInfo>();
        otherfilelists = new ArrayList<FileInfo>();

        sectionFileMap = new HashMap<String,FileInfo>();
        sectionFileListMap = new HashMap<String,List>();

        fileSortHelper = new FileSortHelper();

        for(int i=0; i<sectionPath.length;i++){
            FileInfo fileinfo = new FileInfo(sectionPath[i]);
            fileinfo.fileName = context.getString(sectionNameID[i]);
            fileinfo.sectionType = GlobalConstants.SECTION_TYPE;
            sectionFileMap.put(sectionPath[i],fileinfo);
        }
        sectionFileListMap.put("picture_fileSection", picfilelists);
        sectionFileListMap.put("music_fileSection", musicfilelist);
        sectionFileListMap.put("vedio_fileSection", videofilelists);
        sectionFileListMap.put("other_fileSection", otherfilelists);
        refreshFileList();
    }

    public void setHidenFileStatus(Boolean hidenFile) {
        this.hidenFile = hidenFile;
    }

    public void clearAllList(){
        fileinfolist.clear();
        selectedFileSet.clear();
        picfilelists.clear();
        musicfilelist.clear();
        videofilelists.clear();
        otherfilelists.clear();
    }

    public void refreshFileList(){
        fileinfolist.clear();
        updateSectionTitleNum();
        for(int i=0; i< sectionPath.length;i++){
            fileinfolist.add(sectionFileMap.get(sectionPath[i]));
            fileinfolist.addAll(sectionFileListMap.get(sectionPath[i]));
        }
    }

    public List deleteFromFileList(List<FileInfo> fileinfolist,Set<FileInfo> selectedFileSet){

        fileinfolist.removeAll(selectedFileSet);
        for(FileInfo fileInfo:selectedFileSet){
            if(fileInfo.sectionType == Category.Picture){
                picfilelists.remove(fileInfo);
            }else if(fileInfo.sectionType == Category.Music){
                musicfilelist.remove(fileInfo);
            } else if(fileInfo.sectionType == Category.Video){
                videofilelists.remove(fileInfo);
            }else {
                otherfilelists.remove(fileInfo);
            }
        }
        updateSectionTitleNum();
        return fileinfolist;
    }

    public void reNameFromFileList(String keyword,FileInfo fileInfo,String newname){

        SpannableString s = new SpannableString(newname);
        //Pattern p = Pattern.compile(keyword,Pattern.CASE_INSENSITIVE);
        Pattern p = Pattern.compile(keyword);
        Matcher m = p.matcher(s);
        if (!m.find() || hidenFile && newname.startsWith(".")){
            if(fileInfo.sectionType == Category.Picture){
                picfilelists.remove(fileInfo);
            }else if(fileInfo.sectionType == Category.Music){
                musicfilelist.remove(fileInfo);
            }else if(fileInfo.sectionType == Category.Video){
                videofilelists.remove(fileInfo);
            }else {
                otherfilelists.remove(fileInfo);
            }
            refreshFileList();
            return;
        }
        s.setSpan(new ForegroundColorSpan(Color.RED), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        File newFile = new File(fileInfo.getParent(),newname);
        if(!newFile.exists()) return;
        FileInfo newInfo = new FileInfo(newFile);
        newInfo.sectionType = fileInfo.sectionType;
        newInfo.spannableName = s;
        if(fileInfo.sectionType == Category.Picture){
            picfilelists.add(newInfo);
            picfilelists.remove(fileInfo);
        }else if(fileInfo.sectionType == Category.Music){
            musicfilelist.add(newInfo);
            musicfilelist.remove(fileInfo);
        }else if(fileInfo.sectionType == Category.Video){
            videofilelists.add(newInfo);
            videofilelists.remove(fileInfo);
        }else {
            otherfilelists.add(newInfo);
            otherfilelists.remove(fileInfo);
        }
        fileinfolist.add(fileinfolist.indexOf(fileInfo),newInfo);
        fileinfolist.remove(fileInfo);

    }

   public  void updateSectionTitleNum(){

       if(sectionFileMap==null)return;

       sectionFileMap.get("picture_fileSection").childFileNum = picfilelists.size();
       sectionFileMap.get("music_fileSection").childFileNum = musicfilelist.size();
       sectionFileMap.get("vedio_fileSection").childFileNum = videofilelists.size();
       sectionFileMap.get("other_fileSection").childFileNum = otherfilelists.size();
   }

    protected void sortFileList(int sortMode){
        if (fileSortHelper == null) return;

        if(sortMode==2 || sortMode==3) {
            for (FileInfo fileinfo : otherfilelists) {
                if (fileinfo.fileSize == -1) {
                    fileinfo.fileSize = Util.getFileSize(mContext, fileinfo.getFile());
                }
            }
        }

        Comparator comparator = fileSortHelper.getComparator(sortMode);
        Collections.sort(picfilelists,comparator);
        Collections.sort(musicfilelist,comparator);
        Collections.sort(videofilelists,comparator);
        Collections.sort(otherfilelists,comparator);
    }

    public int getAllFileListNum(){
        return picfilelists.size()+musicfilelist.size()+videofilelists.size()+otherfilelists.size();
    }

    public void selectAllFileList(){
        selectedFileSet.clear();
        selectedFileSet.addAll(picfilelists);
        selectedFileSet.addAll(musicfilelist);
        selectedFileSet.addAll(videofilelists);
        selectedFileSet.addAll(otherfilelists);
    }


}
