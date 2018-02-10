package cn.com.protruly.filemanager.enums;




import android.os.Parcel;
import android.os.Parcelable;
import android.text.SpannableString;
import android.text.TextUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.Serializable;

import cn.com.protruly.filemanager.utils.MediaFileUtil;

/**
 * Created by liushitao on 17-4-19.
 */

public class FileInfo implements Parcelable , Serializable {

    public String filePath;
    public String fileName;
    public long modifiedTime = -1;
    public boolean isFile;
    public long fileSize = -1;
    public int childFileNum = -1;
    public String mimeType;
    public int sectionType;              //add by wwc --用于分组ListView悬停标签
    public SpannableString spannableName; //add by wwc --用于搜索关键字颜色标签
    public File file;

    public FileInfo(String filePath) {
        this(new File(filePath));
    }

    public FileInfo(File file) {
        this.file = file;
        isFile = file.isFile();
        fileName = file.getName();
        modifiedTime = file.lastModified();
        filePath = file.getPath();
        if(isFile) {
            fileSize = file.length();
        }
    }

    public FileInfo(String path,boolean isHistory) {
        this.file = new File(path);
    }

    public File getFile() {
        return this.file;
    }

    protected FileInfo(Parcel in) {
        this.filePath = in.readString();
        this.fileName = in.readString();
        this.modifiedTime = in.readLong();
        this.isFile = in.readByte() != 0;
        this.fileSize = in.readLong();
        this.childFileNum = in.readInt();
        this.mimeType = in.readString();
        this.sectionType = in.readInt();
        this.file = (File)in.readSerializable();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(filePath);
        dest.writeString(fileName);
        dest.writeLong(modifiedTime);
        dest.writeByte((byte) (isFile ? 1 : 0));
        dest.writeLong(fileSize);
        dest.writeInt(childFileNum);
        dest.writeString(mimeType);
        dest.writeInt(sectionType);
        dest.writeSerializable(file);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<FileInfo> CREATOR = new Creator<FileInfo>() {
        @Override
        public FileInfo createFromParcel(Parcel in) {
            return new FileInfo(in);
        }

        @Override
        public FileInfo[] newArray(int size) {
            return new FileInfo[size];
        }
    };

    public String getPath(){
        return file.getPath();
    }

    public String getName(){
        return file.getName();
    }

    public String getParent(){
        return file.getParent();
    }

    public boolean exists(){
        return file.exists();
    }

    public long length(){
        return file.length();
    }

    public long lastModified(){
        return file.lastModified();
    }

    public boolean isFile(){
        return file.isFile();
    }

    public boolean isDirectory(){
        return file.isDirectory();
    }

    public boolean isHidden(){
        return file.isHidden();
    }

    public boolean renameTo(File f){
        return file.renameTo(f);
    }

    public boolean delete(){
        return file.delete();
    }

    public File[] listFiles(){
        return file.listFiles();
    }

    public File[] listFiles(FileFilter filter){
        return file.listFiles(filter);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("-- filePath:" + filePath + "\n");
        sb.append("   fileName:" + fileName + "\n");
        sb.append("   modifiedTime:" + modifiedTime + "\n");
        sb.append("   isFile:" + isFile + "\n");
        sb.append("   fileSize:" + fileSize + "\n");
        sb.append("   childFileNum:" + childFileNum + "\n");
        sb.append("   mimeType:" + mimeType + "\n");
        return sb.toString();
    }

}
