package cn.com.protruly.filemanager.utils;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

import cn.com.protruly.filemanager.enums.FileInfo;

/**
 * Created by sqf on 17-5-11.
 */

public class FilePathUtil {

    private static final String TAG = "FilePathUtil";
    public static final String PATH_SEPARATOR = File.separator;
    /**
     * get file name and extension "111111.jpg" from filePath
     * @param filePath something like "/sdcard/DCIM/Camera/111111.jpg"
     * @return file name and extension, something like 111111.jpg
     */
    public static final String getFileNameAndExtension(String filePath) {
        //LogUtil.i(TAG, "getFileNameAndExtension pass in filePath:" + filePath);
        if(TextUtils.isEmpty(filePath)) return "";
        int indexOfSlash = filePath.lastIndexOf('/');
        if(indexOfSlash == -1) {
            //LogUtil.i(TAG, "getFileNameAndExtension not found indexOfSlash:" + indexOfSlash);
            return "";
        }
        String ret = filePath.substring(indexOfSlash + 1);
        //LogUtil.i(TAG, "getFileNameAndExtension ret string:" + ret);
        return ret;
    }

    /**
     * get file name "111111" from filePath
     * @param filePath something like "/sdcard/DCIM/Camera/111111.jpg"
     * @return file name, something like "111111"
     */
    public static final String getFileName(String filePath) {
        //LogUtil.i(TAG, "getFileName filePath:" + filePath);
        if(TextUtils.isEmpty(filePath)) return "";
        int indexOfSlash = filePath.lastIndexOf('/');
        if(indexOfSlash == -1) {
            //LogUtil.i(TAG, "not found index:" + indexOfSlash);
            return "";
        }
        int indexOfDot = filePath.lastIndexOf('.');
        //LogUtil.i(TAG, "getFileName indexOfDot:" + indexOfDot + " indexOfSlash:" + indexOfSlash);
        if(-1 == indexOfDot) {
            String ret = filePath.substring(indexOfSlash + 1);
            //LogUtil.i(TAG, "getFileName no extension ret:" + ret);
            return ret;
        }
        if(indexOfSlash >= indexOfDot) {
            //if something like "/storage/emulated/0/Android/data/cn.com.protruly.filemanager/cache/ziptest/doc1", return "doc1"
            String ret = filePath.substring(indexOfSlash + 1);
            //LogUtil.i(TAG, "getFileName no extension ret:" + ret);
            return filePath.substring(indexOfSlash + 1);
        }
        return filePath.substring(indexOfSlash + 1, indexOfDot);
    }

    /**
     * get extension "jpg" from filePath
     * @param filePath something like "/sdcard/DCIM/Camera/111111.jpg"
     * @return extension, something like "jpg"
     */
    public static final String getFileExtension(String filePath) {
        if(TextUtils.isEmpty(filePath)) return "";
        int indexOfSlash = filePath.lastIndexOf(PATH_SEPARATOR);
        int indexOfDot = filePath.lastIndexOf('.');
        if(-1 == indexOfDot) {
            return "";
        }
        if(-1 != indexOfSlash && indexOfSlash >= indexOfDot) {
            //if something like "/storage/emulated/0/Android/data/cn.com.protruly.filemanager/cache/ziptest/doc1", return "doc1"
            return "";
        }
        return filePath.substring(indexOfDot + 1);
    }

    public static final String getName(String nameAndExtension) {
        int indexOfDot = nameAndExtension.indexOf(".");
        if(-1 == indexOfDot) return nameAndExtension;
        return nameAndExtension.substring(0, indexOfDot);
    }

    public static final String getExtension(String nameAndExtension) {
        int indexOfDot = nameAndExtension.indexOf(".");
        if(-1 == indexOfDot) return "";
        return nameAndExtension.substring(indexOfDot+1);
    }

    /**
     * get directory "/sdcard/DCIM/Camera" from filePath
     * @param filePath something like "/sdcard/DCIM/Camera/111111.jpg"
     * @return directory path
     */
    public static final String getFileDirectory(String filePath) {
        if(TextUtils.isEmpty(filePath)) return "";
        int index = filePath.lastIndexOf(PATH_SEPARATOR);
        if(-1 == index) return "";
        return filePath.substring(0, index);
    }

    public static String getDiskCacheDir(Context context) {
        String cachePath = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || ! Environment.isExternalStorageRemovable()) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        return cachePath;
    }

    public static boolean regExpFound(String text, String regExp) {
        Matcher matcher = Pattern.compile(regExp).matcher(text);
        if(matcher.find()) {
            return true;
        }
        return false;
    }

    /**
     * ATTENTION: if filePath is a directory, it MUST not be ended with "/";
     * if file or directory exists:
     *     if filePath is a directory /sdcard/DCIM/Camera(N), will return /sdcard/DCIM/Camera(N+1)
     *     if filePath is a file "/sdcard/DCIM/Camera/111(N).jpg", will return "/sdcard/DCIM/Camera/111(N+1).jpg"
     * otherwise:
     *     return filePath passed in.
     * @param filePath something like "/sdcard/DCIM/Camera/111.jpg" or "/sdcard/DCIM/Camera";
     * @return newly generated file path
     */
    public static String generateFilePathWhenExists(String filePath) {
        File file = new File(filePath);
        if(!file.exists()) return filePath;
        String regExp = null;
        if(file.isDirectory()) {
            //directory
            regExp = "\\(\\d+\\)$";
            do {
                if (regExpFound(filePath, regExp)) {
                    int leftBracketIndex = filePath.lastIndexOf("(");
                    int rightBracketIndex = filePath.lastIndexOf(")");
                    String sequenceText = filePath.substring(leftBracketIndex + 1, rightBracketIndex);
                    int currentSequence = Integer.valueOf(sequenceText);
                    String front = filePath.substring(0, leftBracketIndex + 1);
                    String end = filePath.substring(rightBracketIndex);
                    filePath = front + (currentSequence + 1) + end;
                } else {
                    filePath = filePath + "(1)";
                }
            } while (exists(filePath));
        } else {
            //file
            String name = getFileName(filePath);
            String extension = getFileExtension(filePath);
            String dir = getFileDirectory(filePath);
            boolean extensionIsEmpty = TextUtils.isEmpty(extension);
            if(extensionIsEmpty) {
                regExp = "\\(\\d+\\)" + "$";
            } else {
                regExp = "\\(\\d+\\)" + "\\." + extension + "$";
            }
            do {
                if (regExpFound(filePath, regExp)) {
                    int leftBracketIndex = filePath.lastIndexOf("(");
                    int rightBracketIndex = filePath.lastIndexOf(")");
                    String sequenceText = filePath.substring(leftBracketIndex + 1, rightBracketIndex);
                    int currentSequence = Integer.valueOf(sequenceText);
                    String front = filePath.substring(0, leftBracketIndex + 1);
                    String end = filePath.substring(rightBracketIndex);
                    filePath = front + (currentSequence + 1) + end;
                } else {
                    LogUtil.i(TAG, "666666 dir dir dir :" + dir );
                    LogUtil.i(TAG, "666666 name name name :" + name );
                    if(extensionIsEmpty) {
                        filePath = dir + PATH_SEPARATOR + name + "(1)";
                    } else {
                        filePath = dir + PATH_SEPARATOR + name + "(1)." + extension;
                    }
                }
            } while (exists(filePath));
        }
        return filePath;
    }

    /**
     * if the destinationDir path is /emulated/storage/0/DCIM/Camera
     * we get disk path "/emulated/storage/0" from destinationDir
     * @param context
     * @param destinationDir
     * @return
     */
    public static String getCurrentDisk(Context context, String destinationDir) {
        ArrayList<String> disks = Util.getStoragePathListSecond(context);
        //TODO: delete log
        for(String disk : disks) {
            LogUtil.i(TAG, "getCurrentDisk disks:" + disks);
        }
        String currentDisk = null;
        for(int i=0; i < disks.size(); i++) {
            String diskPath = disks.get(i);
            if(destinationDir.startsWith(diskPath)) {
                currentDisk = diskPath;
                break;
            }
        }
        LogUtil.i(TAG, "getCurrentDisk currentDisk :" + currentDisk);
        return currentDisk;
    }

    public static boolean exists(String filePath) {
        File f = new File(filePath);
        return f.exists();
    }

    /**
     * MUST pass in directory path
     * @param filePath
     * @return
     */
    public static boolean mkdirs(String filePath) {
        File destination = new File(filePath);
        return mkdirs(destination);
    }

    /**
     * MUST pass in directory path
     * @param file a directory File instance
     * @return
     */
    public static boolean mkdirs(File file) {
        if(!file.exists()) {
            return file.mkdirs();
        }
        return true;
    }

    /**
     * MUST pass in an file path, not a directory.
     * it will delete file when already exists.
     * @param filePath
     * @return
     */
    public static boolean createNewFile(String filePath) {
        File file = new File(filePath);
        return createNewFile(file);
    }

    /**
     * MUST pass in an file path, not a directory.
     * it will delete file when already exists.
     * @param file
     * @return
     */
    public static boolean createNewFile(File file) {
        try {
            if (file.exists()) {
                file.delete();
            }
            if (!mkdirs(file.getParentFile())) {
                LogUtil.i(TAG, "parent file: createNewFile failed");
                return false;
            }
            file.createNewFile();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static long getFileSize(String filePath) {
        File file = new File(filePath);
        return getFileSize(file);
    }

    public static long getFileSize(final File file) {
        if (file.isFile()) {
            return file.length();
        }
        final File[] children = file.listFiles();
        if(children == null) return 0;
        long total = 0;
        for (final File child : children) {
            total += getFileSize(child);
        }
        return total;
    }

    public static long getFileSize(final FileInfo fileInfo) {
        return getFileSize(fileInfo.getFile());
    }

    public static long getFileSize(final HashSet<FileInfo> fileInfos) {
        long fileSize = 0;
        for(FileInfo fileInfo : fileInfos) {
            fileSize += getFileSize(fileInfo);
        }
        return fileSize;
    }

    public static void closeSilently(InputStream is) {
        try {
            if (null != is) {
                is.close();
                is = null;
            }
        } catch (IOException e) {

        }
    }

    public static void closeSilently(OutputStream os) {
        try {
            if(null != os) {
                os.close();
                os = null;
            }
        } catch (IOException e) {

        }
    }

    public static void closeSilently(FileChannel fileChannel) {
        try {
            if(null != fileChannel) {
                fileChannel.close();
                fileChannel = null;
            }
        } catch (IOException e) {

        }
    }

    public static void closeSilently(ZipFile zf) {
        try {
            if(null != zf) {
                zf.close();
                zf = null;
            }
        } catch (IOException e) {

        }
    }

    /**
     * get file count under directory
     * @return
     */
    public static int getAllFileAndDirectoryNum(String directoryPath) {
        //LogUtil.i(TAG, "getAllFileAndDirectoryNum pass in directoryPath:" + directoryPath);
        File direFile = new File(directoryPath);
        return getAllFileAndDirectoryNum(direFile);
    }

    /**
     *
     * @param file maybe a file or a directory
     * @return
     */
    public static int getAllFileAndDirectoryNum(File file) {
        if(file == null || !file.exists()) {
            //LogUtil.i("SSS", "!fileExists: return 0" );
            return 0;
        }
        int count = 1; //count self. so we set count as 1
        if (file.isFile()) {
            //LogUtil.i("SSS", "is File: return 1" );
            return count;
        }
        final File[] children = file.listFiles();
        if(children == null) return count;
        for (final File child : children) {
            count += getAllFileAndDirectoryNum(child);
        }
        return count;
    }

    public static int getAllFileAndDirectoryNum(HashSet<FileInfo> fileInfos) {
        int total = 0;
        for(FileInfo fileInfo : fileInfos) {
            total += FilePathUtil.getAllFileAndDirectoryNum(fileInfo.getFile());
        }
        return total;
    }

    public static void enumerateFile(File file, FileEnumerationListener listener) {
        if(file == null || !file.exists()) {
            return ;
        }
        listener.onFileEnumerated(file);
        if(file.isFile()) return;
        //enumerate directory
        final File[] children = file.listFiles();
        if(children == null) return;
        for (final File child : children) {
            enumerateFile(child, listener);
        }
    }

    public static final String getDefaultZipDirectory(Context context) {
        //String diskCache = getDiskCacheDir(context) + PATH_SEPARATOR + "AndroidZip";
        String diskCache = Environment.getExternalStorageDirectory().getPath() + "/AndroidZip";
        return diskCache;
    }

    /**
     * is otherDirPath a self or descendant of currentDirPath
     * @param currentDirPath a directory path: something like /storage/emulated/0/DCIM/Camera
     * @param otherDirPath a directory path: something like /storage/emulated/0/DCIM/Camera/DirName1/DirName2
     * @return if a
     */
    public static boolean isSelfOrDescendantPath(String currentDirPath, String otherDirPath) {
        String [] current = currentDirPath.split(FilePathUtil.PATH_SEPARATOR);
        String [] other = otherDirPath.split(FilePathUtil.PATH_SEPARATOR);
        if(current == null || other == null) return false;
        int index = 0;
        int currentLen = current.length;
        int otherLen = other.length;
        if(otherLen < currentLen) {
            return false;
        }
        int minLen = Math.min(currentLen, otherLen);
        boolean sameLen = currentLen == otherLen;
        for( ; index < minLen; index++) {
            boolean equals = current[index].equals(other[index]);
            if(!equals) {
                return false;
            }
        }
        return true;
    }
}
