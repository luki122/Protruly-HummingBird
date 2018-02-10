package cn.com.protruly.filemanager.utils;

import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;

import cn.com.protruly.filemanager.enums.FileInfo;

/**
 * Created by liushitao on 17-5-17.
 */

public class FileSortHelper {
    private Collator collator;
    private ArrayList<Comparator<FileInfo>> comparatorArrayList;

    public FileSortHelper(){
        collator = Collator.getInstance(Locale.getDefault());
        comparatorArrayList = new ArrayList<>(2);
        comparatorArrayList.add(0,nameComparator);
        comparatorArrayList.add(1,dateDescComparator);
        comparatorArrayList.add(2,sizeAscComparator);
        comparatorArrayList.add(3,sizeDescComparator);
    }

    public Comparator getComparator(int p){
        return (Comparator) comparatorArrayList.get(p);
    }


    private Comparator<FileInfo> nameComparator = new FileComparator(){

        @Override
        public int compare(FileInfo f1, FileInfo f2) {
            return super.compare(f1, f2);
        }

        @Override
        public int doCompare(FileInfo f1, FileInfo f2) {
            return collator.compare(f1.fileName,f2.fileName);
            /*if(!TextUtils.isEmpty(f1.fileName)&&!TextUtils.isEmpty(f2.fileName)){
                return collator.compare(f1.fileName,f2.fileName);
            }else{
                return collator.compare(f1.getName(),f2.getName());
            }*/
        }
    };

    private Comparator<FileInfo> sizeAscComparator = new FileComparator(){

        @Override
        public int compare(FileInfo f1, FileInfo f2) {
            return super.compare(f1, f2);
        }

        @Override
        public int doCompare(FileInfo f1, FileInfo f2) {
            /*long f1_size;
            long f2_size;
            if(f1.fileSize!=-1){
                f1_size = f1.fileSize;
            }else{
                f1_size = f1.length();
            }
            if(f2.fileSize!=-1){
                f2_size = f2.fileSize;
            }else{
                f2_size = f2.length();
            }
            if(f1_size!=f2_size){
                return f1_size > f2_size ? 1 : -1;
            }
            return 0;*/
            if(f1.fileSize!=f2.fileSize){
                return f1.fileSize > f2.fileSize ? 1 : -1;
            }
            return 0;
        }
    };

    private Comparator<FileInfo> sizeDescComparator = new FileComparator(){

        @Override
        public int compare(FileInfo f1, FileInfo f2) {
            return super.compare(f1, f2);
        }

        @Override
        public int doCompare(FileInfo f1, FileInfo f2) {
            /*long f1_size;
            long f2_size;
            if(f1.fileSize!=-1){
                f1_size = f1.fileSize;
            }else{
                f1_size = f1.length();
            }
            if(f2.fileSize!=-1){
                f2_size = f2.fileSize;
            }else{
                f2_size = f2.length();
            }
            if(f1_size!=f2_size){
                return f1_size > f2_size ? -1 : 1;
            }
            return 0;*/
            if(f1.fileSize!=f2.fileSize){
                return f1.fileSize > f2.fileSize ? -1 : 1;
            }
            return 0;
        }
    };

    private Comparator<FileInfo> dateAscComparator = new FileComparator(){
        @Override
        public int compare(FileInfo f1, FileInfo f2) {
            return super.compare(f1, f2);
        }

        @Override
        public int doCompare(FileInfo f1, FileInfo f2) {
            /*long f1_time;
            long f2_time;
            if(f1.modifiedTime!=-1){
                f1_time = f1.modifiedTime;
            }else{
                f1_time = f1.lastModified();
            }
            if(f2.modifiedTime!=-1){
                f2_time = f2.modifiedTime;
            }else{
                f2_time = f2.lastModified();
            }
            if(f1_time!=f2_time){
                return f1_time > f2_time ? 1 : -1;
            }
            return 0;*/
            if(f1.modifiedTime!=f2.modifiedTime){
                return f1.modifiedTime > f2.modifiedTime ? 1 : -1;
            }
            return 0;
        }
    };

    private Comparator<FileInfo> dateDescComparator = new FileComparator(){
        @Override
        public int compare(FileInfo f1, FileInfo f2) {
            return super.compare(f1, f2);
        }

        @Override
        public int doCompare(FileInfo f1, FileInfo f2) {
            /*long f1_time;
            long f2_time;
            if(f1.modifiedTime!=-1){
                f1_time = f1.modifiedTime;
            }else{
                f1_time = f1.lastModified();
            }
            if(f2.modifiedTime!=-1){
                f2_time = f2.modifiedTime;
            }else{
                f2_time = f2.lastModified();
            }
            if(f1_time!=f2_time){
                return f1_time > f2_time ? -1 : 1;
            }
            return 0;*/
            if(f1.modifiedTime!=f2.modifiedTime){
                return f1.modifiedTime > f2.modifiedTime ? -1 : 1;
            }
            return 0;
        }
    };

    abstract class FileComparator implements Comparator<FileInfo>{
        public abstract int doCompare(FileInfo f1,FileInfo f2);
        public int compare(FileInfo f1,FileInfo f2){
            if(f1.isFile == f2.isFile){
                return doCompare(f1,f2);
            }
            return f1.isFile ? 1:-1;
        }
    }
}
