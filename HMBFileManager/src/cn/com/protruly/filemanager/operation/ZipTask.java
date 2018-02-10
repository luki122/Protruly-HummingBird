package cn.com.protruly.filemanager.operation;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import cn.com.protruly.filemanager.enums.FileInfo;
import cn.com.protruly.filemanager.utils.FilePathUtil;
import cn.com.protruly.filemanager.utils.GlobalConstants;
import cn.com.protruly.filemanager.utils.LogUtil;
import cn.com.protruly.filemanager.utils.Util;

/**
 * Created by sqf on 17-5-24.
 */

public class ZipTask extends BaseOperationTask {

    private static final String TAG = "ZipTask";
    private HashSet<String> mZippedEntries = new HashSet<String>();
    private HashSet<FileInfo> mFileInfos;
    private String mDestinationDirectory;
    private String mZippedFileName;
    private int mZippedFileCount;
    private int mTotalFileCount;

    private String mZippedFilePath;

    public ZipTask (Context context, FileDbManager fileDbManager, FileOperationTaskListener listener,
                    HashSet<FileInfo> files, String destinationDirectory, String zippedFileName) {
        super(context, fileDbManager, OperationType.ZIP, listener);

        this.mFileInfos = files;
        this.mDestinationDirectory = destinationDirectory;
        this.mZippedFileName = zippedFileName;
        this.mZippedFileCount = 0;

    }

    @Override
    protected FileOperationResult doOperation() {
        mZippedEntries.clear();
        FileOperationResult result = new FileOperationResult();
        ZipOutputStream zipOutputStream = null;
        //String zippedFilePath = null;
        try {
            //check disk free space
            long totalFileSize = FilePathUtil.getFileSize(mFileInfos);
            String destinationDisk = FilePathUtil.getCurrentDisk(mContext, mDestinationDirectory);
            if(Util.getFreeSize(destinationDisk) < totalFileSize) {
                //LogUtil.i(TAG, "not FORC_INSUFFICIENT_SPACE");
                return result.set(FileOperationResult.FORC_INSUFFICIENT_SPACE, null);
            }
            if(isCancelled()) {
                return result.set(FileOperationResult.FORC_USER_CANCELLED, null);
            }

            //get total file count
            mTotalFileCount = FilePathUtil.getAllFileAndDirectoryNum(mFileInfos);
            if(0 == mTotalFileCount) {
                //LogUtil.e(TAG, "mTotalFileCount calculated to 0, no files exist.");
                return result.set(FileOperationResult.FORC_FILE_NOT_EXISTS, null);
            }
            if(isCancelled()) {
                return result.set(FileOperationResult.FORC_USER_CANCELLED, null);
            }

            mZippedFilePath = mDestinationDirectory + FilePathUtil.PATH_SEPARATOR + mZippedFileName;
            //LogUtil.i(TAG, "do Operation: BEFORE -> " + mZippedFilePath);
            mZippedFilePath = FilePathUtil.generateFilePathWhenExists(mZippedFilePath);
            //LogUtil.i(TAG, "do Operation: AFTER -> " + mZippedFilePath);
            if(!FilePathUtil.createNewFile(mZippedFilePath)) {
                return result.set(FileOperationResult.FORC_CREATE_NEW_FILE_ERROR, null);
            }
            zipOutputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(mZippedFilePath), BUFF_SIZE));
            for (FileInfo fileInfo : mFileInfos) {
                //LogUtil.i(TAG, "do Operation: fileInfo.getPath -> " + fileInfo.getPath());
                if(isCancelled()) {
                    //we delete the half-zipped file if cancelled by user.
                    FileOperationUtil.deleteSingleFile(mZippedFilePath);
                    return result.set(FileOperationResult.FORC_USER_CANCELLED, null);
                }
                File fileToZip = fileInfo.getFile();

                /*
                long currentDiskFreeSize = Util.getFreeSize(mDestinationDirectory);
                long fileSize = FilePathUtil.getFileSize(fileToZip);
                if(fileSize > currentDiskFreeSize) {
                    return result.set(FileOperationResult.FORC_INSUFFICIENT_SPACE, null);
                }
                */

                zipSingleFileIntoZip(fileToZip, zipOutputStream, "");
            }
            MediaScannerConnection.scanFile(mContext, new String[] {mZippedFilePath}, null, null);
            return result.set(FileOperationResult.FORC_SUCCEEDED, mZippedFilePath);
        } catch (IOException e) {
            if(null != e) LogUtil.e(TAG, e.getMessage());
        } finally {
            FilePathUtil.closeSilently(zipOutputStream);
        }
        return result.set(FileOperationResult.FORC_UNKNOWN_ERROR, null);
    }

    /**
     *
     * @param fileToZip may be a file or a directory
     * @param zipOutputStream OutputStream to write in
     * @param zipEntryName is "" if first level of zip path, it indicates the parent of the zipEntry
     * @return if a file is zipped, return true, if a directory is zipped, return false;
     * @throws FileNotFoundException
     * @throws IOException
     */
    private void zipSingleFileIntoZip(File fileToZip, ZipOutputStream zipOutputStream, String zipEntryName) throws FileNotFoundException, IOException {
        if(!fileToZip.exists()) {
            return;
        }
        if(fileToZip.getPath().equals(mZippedFilePath)) {
            // we compress files or directories into /storage/emulated/0/AndroidZip by default
            // but if we compress /storage/emulated/0/AndroidZip into a .zip file, it goes into a loop.
            // so we skip mZippedFilePath to avoid this problem.
            // if mZippedFilePath is /storage/emulated/0/AndroidZip.zip
            LogUtil.i(TAG, "Unfortunately, you are zipping the file you want to generate.");
            return;
        }
        if(zipEntryName.equals("")) {
            zipEntryName = zipEntryName + (zipEntryName.trim().length() == 0 ? "" : File.separator) + fileToZip.getName();
            zipEntryName = generateEntryNameWhenExists(zipEntryName, fileToZip.isDirectory());
            mZippedEntries.add(zipEntryName);
            if(GlobalConstants.DEBUG) {
                LogUtil.i(TAG, "------------------------------------------ ");
                Iterator<String> it = mZippedEntries.iterator();
                while (it.hasNext()) {
                    String string = it.next();
                    LogUtil.i(TAG, " zip --> " + string);
                }
                LogUtil.i(TAG, "------------------------------------------ ");
            }
        } else {
            zipEntryName = zipEntryName + (zipEntryName.trim().length() == 0 ? "" : File.separator) + fileToZip.getName();
        }
        if (fileToZip.isDirectory()) {
            File[] fileList = fileToZip.listFiles();
            //we add FilePathUtil.PATH_SEPARATOR here to indicate that it's a directory.
            ZipEntry entry = new ZipEntry(zipEntryName + FilePathUtil.PATH_SEPARATOR);
            zipOutputStream.putNextEntry(entry);
            for (File file : fileList) {
                if(isCancelled()) return;
                zipSingleFileIntoZip(file, zipOutputStream, zipEntryName);
            }
        } else {
            BufferedInputStream in = null;
            try {
                //long time = System.currentTimeMillis();
                byte buffer[] = new byte[BUFF_SIZE];
                in = new BufferedInputStream(new FileInputStream(fileToZip), BUFF_SIZE);
                ZipEntry entry = new ZipEntry(zipEntryName);
                zipOutputStream.putNextEntry(entry);
                int realLength = 0;
                while ((realLength = in.read(buffer)) != -1) {
                    if (isCancelled()) break;
                    zipOutputStream.write(buffer, 0, realLength);
                }
                zipOutputStream.flush();
                zipOutputStream.closeEntry();
                //LogUtil.i(TAG, "sssss time:" + (System.currentTimeMillis() - time));
            } catch(Exception e) {
                if(e != null) LogUtil.e(TAG, e.getMessage());
            } finally {
                FilePathUtil.closeSilently(in);
            }
        }
        ++ mZippedFileCount;
        if(0 == mTotalFileCount) {
            publishProgress(1, 1, fileToZip.getAbsolutePath(), -1, -1);
        } else {
            publishProgress(mTotalFileCount, mZippedFileCount, fileToZip.getAbsolutePath(), -1, -1);
        }
    }

    private boolean hasEntryName(String entryName) {
        return mZippedEntries.contains(entryName);
    }

    private String generateEntryNameWhenExists(String entryName, boolean isDirectory) {
        if(!hasEntryName(entryName)) return entryName;
        String regExp = null;
        if(isDirectory) {
            //directory
            regExp = "\\(\\d+\\)$";
            do {
                if (FilePathUtil.regExpFound(entryName, regExp)) {
                    int leftBracketIndex = entryName.lastIndexOf("(");
                    int rightBracketIndex = entryName.lastIndexOf(")");
                    String sequenceText = entryName.substring(leftBracketIndex + 1, rightBracketIndex);
                    int currentSequence = Integer.valueOf(sequenceText);
                    String front = entryName.substring(0, leftBracketIndex + 1);
                    String end = entryName.substring(rightBracketIndex);
                    entryName = front + (currentSequence + 1) + end;
                } else {
                    entryName = entryName + "(1)";
                }
            } while (hasEntryName(entryName));
        } else {
            //file
            String name = getFileName(entryName);
            String extension = getFileExtension(entryName);
            //String dir = FilePathUtil.getFileDirectory(entryName);
            boolean extensionIsEmpty = TextUtils.isEmpty(extension);
            if(extensionIsEmpty) {
                regExp = "\\(\\d+\\)" + "$";
            } else {
                regExp = "\\(\\d+\\)" + "\\." + extension + "$";
            }
            do {
                if (FilePathUtil.regExpFound(entryName, regExp)) {
                    int leftBracketIndex = entryName.lastIndexOf("(");
                    int rightBracketIndex = entryName.lastIndexOf(")");
                    String sequenceText = entryName.substring(leftBracketIndex + 1, rightBracketIndex);
                    int currentSequence = Integer.valueOf(sequenceText);
                    String front = entryName.substring(0, leftBracketIndex + 1);
                    String end = entryName.substring(rightBracketIndex);
                    entryName = front + (currentSequence + 1) + end;
                } else {
                    if(extensionIsEmpty) {
                        entryName = name + "(1)";
                    } else {
                        entryName = name + "(1)." + extension;
                    }
                }
            } while (hasEntryName(entryName));
        }
        return entryName;
    }

    /**
     * entry name is something like "abc.jpg" or "abc"
     * @param entryName
     * @return
     */
    private final String getFileName(String entryName) {
        if(TextUtils.isEmpty(entryName)) return "";
        int indexOfSlash = entryName.lastIndexOf('/');
        int indexOfDot = entryName.lastIndexOf('.');
        if(-1 == indexOfDot) {
            String ret = entryName.substring(indexOfSlash + 1);
            return ret;
        }
        return entryName.substring(indexOfSlash + 1, indexOfDot);
    }

    public final String getFileExtension(String entryName) {
        if(TextUtils.isEmpty(entryName)) return "";
        int indexOfSlash = entryName.lastIndexOf(FilePathUtil.PATH_SEPARATOR);
        int indexOfDot = entryName.lastIndexOf('.');
        if(-1 == indexOfDot) {
            return "";
        }
        if(-1 != indexOfSlash && indexOfSlash >= indexOfDot) {
            //if something like "/storage/emulated/0/Android/data/cn.com.protruly.filemanager/cache/ziptest/doc1", return "doc1"
            return "";
        }
        return entryName.substring(indexOfDot + 1);
    }

}
