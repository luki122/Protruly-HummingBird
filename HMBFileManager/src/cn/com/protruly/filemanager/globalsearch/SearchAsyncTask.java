package cn.com.protruly.filemanager.globalsearch;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import hb.app.dialog.AlertDialog;
import android.os.Environment;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;


import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.com.protruly.filemanager.categorylist.SelectionManager;
import cn.com.protruly.filemanager.enums.Category;
import cn.com.protruly.filemanager.enums.FileInfo;
import cn.com.protruly.filemanager.utils.GlobalConstants;
import cn.com.protruly.filemanager.utils.Util;
import cn.com.protruly.filemanager.R;


public class SearchAsyncTask extends AsyncTask<Integer,Long,Boolean> {

    private List<FileInfo> picfilelists;
    private List<FileInfo> musicfilelist;
    private List<FileInfo> videofilelists;
    private List<FileInfo> otherfilelists;
    private long searchCount;
    private long resultCount;
    private Timer timer;
    private Boolean hidenFile = true;

    private List<FileInfo> filelists;

    private TextView tv;
    private Context mcontex;
    private String path;
    private String query;
    private GlobalSearchReaultAdapter fileAdapter;
    private SearchAsyncTaskListener listener;
    private SearchSectionManager mSearchSectionManager;
    //private ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
    private ExecutorService cachedThreadPool = Executors.newFixedThreadPool(6);
    private List<SearchTaskForThread> SearchTaskList = new ArrayList<>();
    private List<Future<Integer>> SearchTaskFutures;
    private List<String> storagePathList;



    long starttime = 0;
    private final ForegroundColorSpan mTextColor= new ForegroundColorSpan(Color.RED);

    // AlertDialog dialog;
    public SearchAsyncTask(Context contex, String path, String query, GlobalSearchReaultAdapter fileAdapter /*AlertDialog dialog*/) {
        //this.tv = tv;
        this.mcontex = contex;
        this.path = path;
        this.query = query;
        this.fileAdapter = fileAdapter;
        // this.dialog = dialog;
    }

    public SearchAsyncTask(Context contex, String path, String query, GlobalSearchReaultAdapter fileAdapter,
                           SearchSectionManager mSearchSectionManager) {
        //this.tv = tv;
        this.mcontex = contex;
        this.path = path;
        this.query = query;
        this.fileAdapter = fileAdapter;
        this.mSearchSectionManager = mSearchSectionManager;
        this.picfilelists = mSearchSectionManager.picfilelists;
        this.musicfilelist = mSearchSectionManager.musicfilelist;
        this.videofilelists = mSearchSectionManager.videofilelists;
        this.otherfilelists = mSearchSectionManager.otherfilelists;
        this.filelists = mSearchSectionManager.fileinfolist;

        timer = new Timer();
        SearchTaskFutures = new ArrayList<Future<Integer>>();

    }

    void setSearchMessageText(TextView tv) {
        this.tv = tv;
    }
    void setHidenStatus(boolean hiden) {
        this.hidenFile = hiden;
    }
    void setStoragePathList(List list) {
        this.storagePathList = list;
    }
    void setKeyWord(String st) {
        this.query = st;
    }


    @Override
    protected Boolean doInBackground(Integer... params) {

        query = escapeExprSpecialWord(query);

        starttime = System.currentTimeMillis();

        if(storagePathList==null)return false;

        if(SearchTaskFutures!=null && SearchTaskFutures.size()>0){
            for(Future<Integer> future:SearchTaskFutures){
                future.cancel(true);
            }
        }
        if(SearchTaskList!=null)SearchTaskList.clear();
        if(SearchTaskFutures!=null)SearchTaskFutures.clear();


        for(String storagepath:storagePathList){
                SearchTaskList.addAll((new SearchTaskForStorageValue(storagepath,query)).getSearchTaskList());
        }

        if(SearchTaskFutures==null)SearchTaskFutures = new ArrayList<>();
        for(SearchTaskForThread task:SearchTaskList){
            SearchTaskFutures.add(cachedThreadPool.submit(task));
            android.util.Log.d("wenwenchao","new SearchTask begin;   path="+ task.path +"  keyword="+query);
        }

        if(timer==null)timer = new Timer();
        updateSearchCountbyTime(timer);
        while(!isCancelled() && !isAllDown(SearchTaskFutures)){}

        if(SearchTaskList!=null) {
            for (SearchTaskForThread t : SearchTaskList) {
                t.addAllSearchData();
            }
        }
        if(SearchTaskFutures!=null) {
            for (Future<Integer> future : SearchTaskFutures) {
                future.cancel(true);
            }
            if(timer!=null){
                timer.cancel();
            }
        }

        android.util.Log.d("wenwenchao", "Search Time = " + (System.currentTimeMillis() - starttime));

        return true;
    }


    Boolean isAllDown(List<Future<Integer>> SearchTaskFutures){
        for(Future<Integer> future:SearchTaskFutures) {
            if (!future.isDone()) {
                return false;
            }
        }
        return true;
    }

    void updateSearchCountbyTime(Timer timer){
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                searchCount = 0;
                resultCount = 0;
                if(SearchTaskList!=null) {
                    for (SearchTaskForThread t : SearchTaskList) {
                        searchCount += t.mTempHolder.queryCount;
                        resultCount += t.mTempHolder.resultCount;
                    }
                    publishProgress(resultCount,searchCount);
                }
            }
                // TODO Auto-generated method stub
            }, 2000, 2000);
    }


    @Override
    protected void onPostExecute(Boolean data) {

       // fileAdapter.setAdapterData(data);
        mSearchSectionManager.refreshFileList();
        fileAdapter.notifyDataSetChanged(true);
        listener.onEndSearch(mSearchSectionManager.fileinfolist);
    }

    @Override
    protected void onProgressUpdate(Long... values) {
         if(tv==null)return;
         long v1 = values[0];
         long v2 = values[1];
         tv.setText(mcontex.getResources().getString(R.string.global_search_count) + v1+"/"+v2);

    }

    @Override
    protected void onPreExecute() {
        clearList();
        fileAdapter.notifyDataSetChanged(true);
        listener.onStartSearch();
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        if(SearchTaskFutures!=null && SearchTaskFutures.size()>0){
            for(Future<Integer> future:SearchTaskFutures){
                future.cancel(true);
            }
        }
        if(timer!=null){
            timer.cancel();
        }
    }

    private void clearList() {
        filelists.clear();
        picfilelists.clear();
        musicfilelist.clear();
        videofilelists.clear();
        otherfilelists.clear();
        searchCount =0;
        resultCount =0;
    }



    public void searchbypathForThread(File file, MultThreadSearchTempHolder tempHolder) {

        if(Thread.currentThread().isInterrupted())return;
/*        if (!file.exists()) {
            System.out.println("文件不存在!");
            return;
        }*/
        File[] files = file.listFiles();
        if (files == null) {
            return;
        }

        for (File file2 : files) {

            if(hidenFile && file2.getName().startsWith(".")){
                continue;
            }

            if (file2.isDirectory()) {

                matchKeyWordForDir(file2, query, tempHolder);

                searchbypathForThread(file2, tempHolder);
            } else {

                matchKeyWordForFile(file2, query, tempHolder);
            }
        }
    }


    void matchKeyWordForDir(File file, String keyword, MultThreadSearchTempHolder tempHolder){

        SpannableString s = new SpannableString(file.getName());
        //Pattern p = Pattern.compile(keyword,Pattern.CASE_INSENSITIVE);
        Pattern p = Pattern.compile(keyword);
        Matcher m = p.matcher(s);
        tempHolder.queryCount++;
        if (!m.find()) return;
        tempHolder.resultCount++;
        s.setSpan(mTextColor, m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        FileInfo fileinfo = new FileInfo(file);
        fileinfo.spannableName = s;
        fileinfo.sectionType = Category.Other;
        tempHolder.motherfilelists.add(fileinfo);
    }



    void matchKeyWordForFile(File file, String keyword, MultThreadSearchTempHolder tempHolder) {


       // if (!file.exists()) return;
            tempHolder.queryCount++;
            //if(file.length()<10*1024)return;
            SpannableString s = new SpannableString(file.getName());
            //Pattern p = Pattern.compile(keyword,Pattern.CASE_INSENSITIVE);
            Pattern p = Pattern.compile(keyword);
            Matcher m = p.matcher(s);
            if (!m.find()) return;
            tempHolder.resultCount++;
            s.setSpan(mTextColor, m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            FileInfo fileinfo = new FileInfo(file);
            String path = fileinfo.filePath;
            fileinfo.spannableName = s;

            if (Util.isImageFile(path)) {
                fileinfo.sectionType = Category.Picture;
                tempHolder.mpicfilelists.add(fileinfo);

            } else if(Util.isAudioFile(path)) {
                fileinfo.sectionType = Category.Music;
                tempHolder.mmusicfilelists.add(fileinfo);

            } else if(Util.isVideoFile(path)) {
                fileinfo.sectionType = Category.Video;
                tempHolder.mvideofilelists.add(fileinfo);

            } else{
                fileinfo.sectionType = Category.Other;
                tempHolder.motherfilelists.add(fileinfo);
            }


             /*if (Util.isImageFile(path)) {
                fileinfo.sectionType = Category.Picture;
                tempHolder.mpicfilelists.add(fileinfo);

            } else if (Util.isVideoFile(path)) {
                fileinfo.sectionType = Category.Video;
                tempHolder.mvideofilelists.add(fileinfo);

            } else if (Util.isAudioFile(path)) {
                fileinfo.sectionType = Category.Music;
                tempHolder.motherfilelists.add(fileinfo);

            } else if (Util.isZipFile(path)) {
                fileinfo.sectionType = Category.Zip;
                tempHolder.motherfilelists.add(fileinfo);

            } else if (Util.isApkFile(path)) {
                fileinfo.sectionType = Category.Apk;
                tempHolder.motherfilelists.add(fileinfo);

            } else if (Util.istTxtFile(path) || Util.istWordFile(path) || Util.istExcelFile(path) || Util.isPPtFile(path) || Util.isPdfFile(path)) {
                fileinfo.sectionType = Category.Document;
                tempHolder.motherfilelists.add(fileinfo);

            } else {
                fileinfo.sectionType = Category.Other;
                tempHolder.motherfilelists.add(fileinfo);
            }*/

    }


    void matchTypeForThread(File file, String keyword, MultThreadSearchTempHolder tempHolder) {

        // if (!file.exists()) return;
        tempHolder.queryCount++;
        SpannableString s = new SpannableString(file.getName());
        //Pattern p = Pattern.compile(keyword,Pattern.CASE_INSENSITIVE);
        Pattern p = Pattern.compile(keyword);
        Matcher m = p.matcher(s);
        if (!m.find()) return;
        tempHolder.resultCount++;
        s.setSpan(mTextColor, m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        FileInfo fileinfo = new FileInfo(file);
        String path = fileinfo.filePath;
        fileinfo.spannableName = s;

        if(!fileinfo.isFile){
            fileinfo.sectionType = Category.Other;
            tempHolder.motherfilelists.add(fileinfo);
        }else {
            //if(file.length()<10*1024)return;

            if (Util.isImageFile(path)) {
                fileinfo.sectionType = Category.Picture;
                tempHolder.mpicfilelists.add(fileinfo);

            } else if(Util.isAudioFile(path)) {
                fileinfo.sectionType = Category.Music;
                tempHolder.mmusicfilelists.add(fileinfo);

            } else if(Util.isVideoFile(path)) {
                fileinfo.sectionType = Category.Video;
                tempHolder.mvideofilelists.add(fileinfo);

            } else{
                fileinfo.sectionType = Category.Other;
                tempHolder.motherfilelists.add(fileinfo);
            }


            /*if (Util.isImageFile(path)) {
                fileinfo.sectionType = Category.Picture;
                tempHolder.mpicfilelists.add(fileinfo);

            } else if (Util.isVideoFile(path)) {
                fileinfo.sectionType = Category.Video;
                tempHolder.mvideofilelists.add(fileinfo);

            } else if (Util.isAudioFile(path)) {
                fileinfo.sectionType = Category.Music;
                tempHolder.motherfilelists.add(fileinfo);

            } else if (Util.isZipFile(path)) {
                fileinfo.sectionType = Category.Zip;
                tempHolder.motherfilelists.add(fileinfo);

            } else if (Util.isApkFile(path)) {
                fileinfo.sectionType = Category.Apk;
                tempHolder.motherfilelists.add(fileinfo);

            } else if (Util.istTxtFile(path) || Util.istWordFile(path) || Util.istExcelFile(path) || Util.isPPtFile(path) || Util.isPdfFile(path)) {
                fileinfo.sectionType = Category.Document;
                tempHolder.motherfilelists.add(fileinfo);

            } else {
                fileinfo.sectionType = Category.Other;
                tempHolder.motherfilelists.add(fileinfo);
            }*/
        }
    }

    public class SearchTaskForStorageValue{
        private String storagePath;
        private String keyword;
        private Boolean isDividable;
        private List fileList1;
        private List fileList2;
        private ArrayList<SearchTaskForThread> searchTaskList;



        public SearchTaskForStorageValue(String storagePath,String keyword){
            this.storagePath = storagePath;
            this.keyword = keyword;
            this.isDividable = initListForStorageValue(storagePath);
            initSearchTaskForThread();
        }

        public void initSearchTaskForThread(){
            searchTaskList = new ArrayList<>();
            if(!isDividable){
                SearchTaskForThread stft1 = new  SearchTaskForThread(storagePath,keyword,null);
                searchTaskList.add(stft1);
            }else{
                 if(fileList1!=null && fileList1.size()>0){
                     SearchTaskForThread stft1 = new  SearchTaskForThread(storagePath,keyword,fileList1);
                     searchTaskList.add(stft1);
                 }
                if(fileList2!=null && fileList2.size()>0){
                    SearchTaskForThread stft2 = new  SearchTaskForThread(storagePath,keyword,fileList2);
                    searchTaskList.add(stft2);
                }
            }
        }


        public Boolean initListForStorageValue(String storagePath){
            List templist;
            File rootfile = new File(storagePath);
            if (!rootfile.exists()) {
                android.util.Log.d("wenwenchao","the disk isnot exists!");
                return false;
            }
            File[] rootfilelist = rootfile.listFiles();
            if (rootfilelist == null) {
                return false;
            }
            int length = rootfilelist.length;
            if (length < 2) {
                return false;
            }else{
                templist = new ArrayList<File>();
                fileList1 = new ArrayList<File>();
                fileList2 = new ArrayList<File>();
                int count=0;
                for(File f:rootfilelist){
                    if(hidenFile && f.getName().startsWith(".")){
                        continue;
                    }
                    if(f.isDirectory() && "/storage/emulated/0/Android".equals(f.getPath())){
                        continue;
                    }

                    if(f.isFile() || f.isDirectory() && f.list()==null) {
                        templist.add(f);
                    }else {
                        count++;
                        if(count%2==0){
                            fileList2.add(f);
                        }else{
                            fileList1.add(f);
                        }
                    }
                }
                if(templist.size()>0)fileList1.addAll(templist);
            }
            return true;
        }

        public ArrayList<SearchTaskForThread> getSearchTaskList() {
            return searchTaskList;
        }
    }




    public class SearchTaskForThread implements Callable<Integer> {

        public List<File> searchList = null;

        public MultThreadSearchTempHolder mTempHolder;
        private String path;
        private String keyword;

        public SearchTaskForThread(String path, String keyword) {
            this.path = path;
            this.keyword = keyword;
            this.searchList = null;
            initdata();
        }

        public SearchTaskForThread(String path, String keyword, List searchList) {
            this.path = path;
            this.keyword = keyword;
            this.searchList = searchList;
            initdata();
        }

        private void initdata() {
            mTempHolder = new MultThreadSearchTempHolder();
        }

        @Override
        public Integer call() throws Exception {

            mTempHolder.clearAllList();

            if(searchList==null){
                searchbypathForThread(new File(path), mTempHolder);
                return null;
            }

            if(searchList!=null){
                for(File f:searchList){
                    if(hidenFile && f.getName().startsWith(".")){
                        continue;
                    }
                    matchTypeForThread(f, keyword, mTempHolder);
                    searchbypathForThread(f, mTempHolder);
                }
            }
            return null;
        }


        public void addAllSearchData() {
                picfilelists.addAll(mTempHolder.mpicfilelists);
                musicfilelist.addAll(mTempHolder.mmusicfilelists);
                videofilelists.addAll(mTempHolder.mvideofilelists);
                otherfilelists.addAll(mTempHolder.motherfilelists);
        }
    }



    public class MultThreadSearchTempHolder{
        public List<FileInfo> mpicfilelists;
        public List<FileInfo> mmusicfilelists;
        public List<FileInfo> mvideofilelists;
        public List<FileInfo> motherfilelists;
        public long queryCount;
        public long resultCount;

        public MultThreadSearchTempHolder(){
            mpicfilelists = new ArrayList<FileInfo>();
            mmusicfilelists = new ArrayList<FileInfo>();
            mvideofilelists = new ArrayList<FileInfo>();
            motherfilelists = new ArrayList<FileInfo>();
            queryCount = 0;
            resultCount = 0;
        }

        public void clearAllList(){
            mpicfilelists.clear();
            mmusicfilelists.clear();
            mvideofilelists.clear();
            motherfilelists.clear();
            queryCount = 0;
            resultCount = 0;
        }

    }



    public void setSelectionListener(SearchAsyncTask.SearchAsyncTaskListener listener) {
        this.listener = listener;
    }
    public interface  SearchAsyncTaskListener{
        public void onStartSearch();
        public void doInSearch();
        public void onEndSearch(List<FileInfo> data);
    }
    public  String escapeExprSpecialWord(String keyword) {

               String[] fbsArr = { "\\", "$", "(", ")", "*", "+", ".", "[", "]", "?", "^", "{", "}", "|"};
                     for (String key : fbsArr) {
                            if (keyword.contains(key)) {
                                      keyword = keyword.replace(key, "\\" + key);
                                }
                        }
            return keyword;
    }

}
