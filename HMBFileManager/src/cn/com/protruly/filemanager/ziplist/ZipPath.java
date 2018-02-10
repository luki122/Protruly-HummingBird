package cn.com.protruly.filemanager.ziplist;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
//import java.util.zip.ZipEntry;
import org.apache.tools.zip.ZipEntry;

import cn.com.protruly.filemanager.utils.GlobalConstants;
import cn.com.protruly.filemanager.utils.LogUtil;
import cn.com.protruly.filemanager.utils.PinyinCompareUtil;
import cn.com.protruly.filemanager.utils.Util;

/**
 * Created by sqf on 17-5-17.
 *
 * a .zip file is formed like below when using ZipFile.
 * entry: META-INF/MANIFEST.MF
 * entry: META-INF/IFLYTEKI.SF
 * entry: META-INF/IFLYTEKI.RSA
 * entry: lib/
 * entry: res/
 * entry: res/240/
 * entry: res/240/abc/
 * entry: res/240/checkboxchecked.png
 * entry: res/320/checkboxchecked.png
 */
public class ZipPath implements Parcelable {

    public static final String TAG = "ZipPath";
    public static final boolean DEBUG = GlobalConstants.DEBUG;
    public static final String PATH_SEPARATOR = "/";
    public static final int TYPE_FILE = 1;
    public static final int TYPE_DIRECTORY = 2;

    /**
     * if root, mParent is null
     */
    private ZipPath mParent;
    private String mName;
    private long mTime;
    private long mSize;
    //private boolean mIsDirectory;
    private ArrayList<ZipPath> mChildren;
    //mType indicates whether ZipPath is a directory or a file
    private int mType;


    public ZipPath(String name, int type) {
        mName = name;
        mType = type;
    }

    protected ZipPath(Parcel in) {
        mParent = in.readParcelable(ZipPath.class.getClassLoader());
        mName = in.readString();
        mTime = in.readLong();
        mSize = in.readLong();
        mChildren = in.createTypedArrayList(ZipPath.CREATOR);
        mType = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mParent, flags);
        dest.writeString(mName);
        dest.writeLong(mTime);
        dest.writeLong(mSize);
        dest.writeTypedList(mChildren);
        dest.writeInt(mType);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ZipPath> CREATOR = new Creator<ZipPath>() {
        @Override
        public ZipPath createFromParcel(Parcel in) {
            return new ZipPath(in);
        }

        @Override
        public ZipPath[] newArray(int size) {
            return new ZipPath[size];
        }
    };

    public ZipPath getParent() {
        return mParent;
    }

    private void setParent(ZipPath zipPath) {
        this.mParent = zipPath;
    }

    public String getName() {
        return mName;
    }

    public int getType() {
        return mType;
    }

    public void setType(int type) {
        mType = type;
    }

    public long getTime() {
        return mTime;
    }

    public void setTime(long time) {
        mTime = time;
    }

    public long getSize() {
        return mSize;
    }

    public void setSize(long size) {
        mSize = size;
    }

    public int getLevelCount() {
        int levelCount = 0;
        ZipPath current = this;
        while(current.getParent() != null) {
            ++ levelCount;
            current = current.getParent();
        }
        return levelCount;
    }

    public boolean isRoot() {
        return mParent == null && mName.equals("");
    }

    public boolean isDirectory() {
        //return mIsDirectory;
        return getType() == TYPE_DIRECTORY;
    }


    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof ZipPath)) {
            return false;
        }
        ZipPath other = (ZipPath)obj;
        return this.getPath().equals(other.getPath());
    }

    public static ZipPath createRoot() {
        ZipPath root = new ZipPath("", TYPE_DIRECTORY);
        return root;
    }

    /**
     * zipEntry is something like "META-INF/IFLYTEKI.SF"
     * We parse param "zipEntry" from front to end.
     * after parsing, a chain of zipPath is added to root.
     * @param zipEntry
     * @return nothing
     */
    public static void parseZipEntry(ZipPath root, org.apache.tools.zip.ZipEntry zipEntry) {
        if(null == zipEntry) return;
        String zipEntryName = zipEntry.getName();

        if(TextUtils.isEmpty(zipEntryName)) return;
        //boolean endsWithPathSeparator = zipEntryName.endsWith(PATH_SEPARATOR);
        String [] directories = zipEntryName.split(PATH_SEPARATOR);
        ZipPath current = root;
        if(DEBUG) {
            LogUtil.i(TAG, "-----------------------------------: ");
        }
        final int len = directories.length;
        for(int i = 0; i < len; i++) {
            String name = directories[i];
            boolean last = i == len - 1;
            boolean isFile = ! zipEntry.isDirectory() && last;
            /*
            ZipPath path = null;
            if(current.containsChild(name)) {
                path = current.findChild(name);
            } else {
                path = new ZipPath(name, isFile ? TYPE_FILE : TYPE_DIRECTORY);
                current.addChild(path);
            }
            */
            ZipPath path = current.findChild(name);
            if(path == null) {
                path = new ZipPath(name, isFile ? TYPE_FILE : TYPE_DIRECTORY);
                current.addChild(path);
            }
            current = path;
            if(last) {
                path.setTime(zipEntry.getTime());
                path.setSize(zipEntry.getSize());
                path.setType(zipEntry.isDirectory() ? TYPE_DIRECTORY : TYPE_FILE);
            }
            if(DEBUG) {
                LogUtil.i(TAG, "directory : " + name);
            }
        }
    }

    /**
     * zipEntry is something like "META-INF/IFLYTEKI.SF"
     * We parse param "zipEntry" from front to end.
     * after parsing, a chain of zipPath is added to root.
     * @param zipEntry
     * @return nothing
     */
    public static void parseZipEntry(ZipPath root, java.util.zip.ZipEntry zipEntry) {
        if(null == zipEntry) return;
        String zipEntryName = zipEntry.getName();

        /*
        String encoding = getEncoding(zipEntryName);
        LogUtil.i(TAG, "parseZipEntry: encoding: " + encoding);
        */

        if(TextUtils.isEmpty(zipEntryName)) return;
        //boolean endsWithPathSeparator = zipEntryName.endsWith(PATH_SEPARATOR);
        String [] directories = zipEntryName.split(PATH_SEPARATOR);
        ZipPath current = root;
        if(DEBUG) {
            LogUtil.i(TAG, "-----------------------------------: ");
        }
        final int len = directories.length;
        for(int i = 0; i < len; i++) {
            String name = directories[i];
            boolean last = i == len - 1;
            boolean isFile = ! zipEntry.isDirectory() && last;
            ZipPath path = current.findChild(name);
            if(path == null) {
                path = new ZipPath(name, isFile ? TYPE_FILE : TYPE_DIRECTORY);
                current.addChild(path);
            }
            current = path;
            if(last) {
                path.setTime(zipEntry.getTime());
                path.setSize(zipEntry.getSize());
                path.setType(zipEntry.isDirectory() ? TYPE_DIRECTORY : TYPE_FILE);
            }
            if(DEBUG) {
                LogUtil.i(TAG, "directory : " + name);
            }
        }
    }


    public ZipPath findChild(String childName) {
        if(null == mChildren) return null;
        for(ZipPath zipPath : mChildren) {
            if(zipPath.getName().equals(childName)) {
                return zipPath;
            }
        }
        return null;
    }

    public boolean containsChild(String childName) {
        return findChild(childName) != null;
    }

    public String getPath() {
        StringBuilder sb = new StringBuilder();
        ZipPath pointer = this;
        ZipPath parent = null;
        while(pointer != null) {
            if(pointer.isDirectory() && ! pointer.isRoot()) {
                sb.insert(0, PATH_SEPARATOR);
            }
            sb.insert(0, pointer.getName());
            pointer = pointer.getParent();
        }
        LogUtil.i(TAG, "ZipPath::getPath return " + sb.toString());
        return sb.toString();
    }

    public String dump() {
        StringBuilder builder = new StringBuilder();
        final String INDENT = "    ";
        int indentTimes = getLevelCount();
        while(indentTimes > 0) {
            builder.append(INDENT);
            -- indentTimes;
        }
        builder.append(getName() + " (" + getLevelCount() + ")\n");
        if(null == mChildren) return builder.toString();
        for(ZipPath zipPath : mChildren) {
            builder.append(zipPath.dump());
        }
        return builder.toString();
    }

    public void addChild(ZipPath child) {
        if(null == mChildren) {
            mChildren = new ArrayList<ZipPath>();
        }
        mChildren.add(child);
        child.setParent(this);
    }

    public List<ZipPath> getSortedChildren() {
        ArrayList<ZipPath> sortedChildren = new ArrayList<ZipPath>();
        ArrayList<ZipPath> childrenDirectories = new ArrayList<ZipPath>();
        ArrayList<ZipPath> childrenFiles = new ArrayList<ZipPath>();
        if(null == mChildren) {
            return new ArrayList<ZipPath>();
            //return null;
        }
        for(ZipPath zipPath : mChildren) {
            final int type = zipPath.getType();
            if(type == TYPE_DIRECTORY) {
                childrenDirectories.add(zipPath);
            } else if(type == TYPE_FILE) {
                childrenFiles.add(zipPath);
            }
        }
        if(!childrenDirectories.isEmpty()) {
            Collections.sort(childrenDirectories, mComparator);
            sortedChildren.addAll(childrenDirectories);
        }
        if(!childrenFiles.isEmpty()) {
            Collections.sort(childrenFiles, mComparator);
            sortedChildren.addAll(childrenFiles);
        }
        return sortedChildren;
    }

    Comparator<ZipPath> mComparator = new Comparator<ZipPath>() {
        @Override
        public int compare(ZipPath o1, ZipPath o2) {
            String name1 = o1.getName();
            String name2 = o2.getName();
            return PinyinCompareUtil.sComparator.compare(name1, name2);
        }
    };

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        builder.append("mName:" + mName + "\n");
        builder.append("mSize:" + mSize + "\n");
        builder.append("mTime:" + Util.formatDateStringThird(mTime) + "\n");
        builder.append("mType:" + (mType == TYPE_DIRECTORY ? "TYPE_DIRECTORY" : "TYPE_FILE"));
        builder.append("]");
        return builder.toString();
    }

    /**
     * This function may takes a long time; use it carefully
     *
     * @return
     */
    public long getAllDescendantsSize() {
        long size = 0;
        if(getType() == TYPE_FILE) {
            size = getSize();
            return size;
        }
        if(null == mChildren) return 0;
        for(ZipPath child : mChildren) {
            if(child.getType() == TYPE_DIRECTORY) {
                size += child.getAllDescendantsSize();
            } else if(child.getType() == TYPE_FILE) {
                size += child.getSize();
            }
        }
        return size;
    }
}