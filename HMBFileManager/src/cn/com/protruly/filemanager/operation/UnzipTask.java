package cn.com.protruly.filemanager.operation;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipException;
//import java.util.zip.ZipFile;
import org.apache.tools.zip.ZipFile;
//import java.util.zip.ZipEntry;
import org.apache.tools.zip.ZipEntry;
import cn.com.protruly.filemanager.utils.FilePathUtil;
import cn.com.protruly.filemanager.utils.LogUtil;
import cn.com.protruly.filemanager.utils.Util;

/**
 * Created by sqf on 17-5-23.
 */

public class UnzipTask extends BaseOperationTask {

    public static final String TAG = "UnzipTask";

    private String mZipFilePath;
    private String mZipEntryName;
    private String mDestinationDirectory;

    public UnzipTask(Context context, FileDbManager fileDbManager, FileOperationTaskListener listener,
                          String zipFilePath, String entryName, String destinationDir) {
        super(context, fileDbManager, OperationType.UNZIP_ENTRY, listener);
        mZipFilePath = zipFilePath;
        mZipEntryName = entryName;
        mDestinationDirectory = destinationDir;
    }

    public UnzipTask(Context context, FileDbManager fileDbManager, FileOperationTaskListener listener,
                     String zipFilePath, String destinationDir) {
        super(context, fileDbManager, OperationType.UNZIP_WHOLE, listener);
        mZipFilePath = zipFilePath;
        mZipEntryName = "";
        mDestinationDirectory = destinationDir;
    }

    @Override
    protected FileOperationResult doOperation() {
        ZipFile zipFile = null;
        FileOperationResult outResult = new FileOperationResult();
        try {
            //String zipFilePath = param.zipFilePath;
            //String zipEntryName = param.zipEntry;
            //String destinationDir = param.destinationDir;
            String destinationDisk = FilePathUtil.getCurrentDisk(mContext, mDestinationDirectory);

            /*
            LogUtil.i(TAG, "doInBackground zipFilePath:" + mZipFilePath + " destinationDir:" + mDestinationDirectory +
                    " destinationDisk:" + destinationDisk + " zipEntryName:" + mZipEntryName);
            */
            switch (mOperationType) {
                case OperationType.UNZIP_ENTRY:
                    zipFile = new ZipFile(mZipFilePath);
                    outResult = unzipZipEntry(zipFile, mZipEntryName, mDestinationDirectory,
                            destinationDisk, true, outResult);
                    break;
                case OperationType.UNZIP_WHOLE:
                    outResult = unzipWholeZip(mZipFilePath, mDestinationDirectory, destinationDisk);
                    break;
            }
            //LogUtil.i(TAG, "do Operation return ....");
            mFileDbManager.insertFromContentValueList();
            return outResult;
        } catch (IOException e) {
            LogUtil.e(TAG, "UnzipTask doOperation ERROR:" + e.getMessage());
            return outResult.set(FileOperationResult.FORC_OPEN_ZIP_FILE_FAILED, null);
        } finally {
            //FilePathUtil.closeSilently(zipFile);
            ZipFile.closeQuietly(zipFile);
        }
    }

    /**
     * unzip only one .zip ZipEntry(not a directory) into a directory
     * @param zf ZipFile
     * @param zipEntryName one zip entry
     * @param destinationDirectory destination directory
     * @param destinationDisk disk in which destination directory lies
     * @param outResult will return outResult
     * @return outResult
     */
    private FileOperationResult unzipZipEntry(ZipFile zf, String zipEntryName, String destinationDirectory,
                                      String destinationDisk, boolean needPublishProgress, FileOperationResult outResult) {
        InputStream is = null;
        OutputStream os = null;
        try {
            //LogUtil.i(TAG, "unzipZipEntry zipEntryName:" + zipEntryName);
            ZipEntry zipEntry = zf.getEntry(zipEntryName);
            long currentDiskFreeSize = Util.getFreeSize(destinationDisk);
            if(currentDiskFreeSize < zipEntry.getSize()) {
                return outResult.set(FileOperationResult.FORC_INSUFFICIENT_SPACE, null);
            }

            String unzippedFilePath = destinationDirectory + File.separator + zipEntryName;
            //LogUtil.i(TAG, "unzipZipEntry unzippedFilePath:" + unzippedFilePath);
            unzippedFilePath = FilePathUtil.generateFilePathWhenExists(unzippedFilePath);
            if (zipEntry.isDirectory()) {
                //it is a directory, will not be unzipped.
                if(!FilePathUtil.mkdirs(unzippedFilePath)) {
                   return outResult.set(FileOperationResult.FORC_CREATE_NEW_DIRECTORY_ERROR, null);
                }
                return outResult.set(FileOperationResult.FORC_SUCCEEDED, unzippedFilePath);
            }

            is = zf.getInputStream(zipEntry);
            long total = is.available();
            //LogUtil.i(TAG, "unzipZipEntry total:" + total);
            File f = new File(unzippedFilePath);
            boolean created = FilePathUtil.createNewFile(f);
            if (!created) {
                return outResult.set(FileOperationResult.FORC_CREATE_NEW_FILE_ERROR, unzippedFilePath);
            }

            int alreadyRead = 0;
            os = new FileOutputStream(f);
            byte buffer[] = new byte[BUFF_SIZE];
            int realLength;
            while ((realLength = is.read(buffer)) > 0) {
                //LogUtil.i(TAG, "unzipZipEntry readlLength:" + realLength);
                if(isCancelled()) {
                    return outResult.set(FileOperationResult.FORC_USER_CANCELLED, null);
                }
                os.write(buffer, 0, realLength);
                alreadyRead += realLength;
                if(needPublishProgress) {
                    if(total == 0) {
                        // we set progress as 100% directly
                        publishProgress(1, 1, null, -1, -1);
                        //LogUtil.i(TAG, "unzipZipEntry publishProgress 1111:");
                    } else {
                        publishProgress(total, alreadyRead, null, -1 , -1);
                        //LogUtil.i(TAG, "unzipZipEntry publishProgress 2222 total:" + total + " alreadyRead:" + alreadyRead);
                    }
                }
            }
            //LogUtil.i(TAG, "unzipZipEntry after while");
            if (isCancelled()) {
                return outResult.set(FileOperationResult.FORC_USER_CANCELLED, unzippedFilePath);
            }
            //mFileDbManager.insert(unzippedFilePath, false);
            mFileDbManager.addToInsertList(unzippedFilePath, false);
            //LogUtil.i(TAG, "unzipZipEntry return succeeded");
            return outResult.set(FileOperationResult.FORC_SUCCEEDED, unzippedFilePath);
        } catch (ZipException e) {
            LogUtil.e(TAG, e.getMessage());
        } catch (IOException e) {
            LogUtil.e(TAG, e.getMessage());
        } finally {
            FilePathUtil.closeSilently(is);
            FilePathUtil.closeSilently(os);
            LogUtil.i(TAG, "finally ---");
        }
        return outResult.set(FileOperationResult.FORC_UNKNOWN_ERROR, null);
    }

    /**
     * unzip the whole .zip file into a directory
     * @return
     */
    private FileOperationResult unzipWholeZip(String zipFilePath, String destinationDirectory, String destinationDisk) {

        String zipFileName = FilePathUtil.getFileName(zipFilePath);
        String destDirWithZipFileNameDir = destinationDirectory + FilePathUtil.PATH_SEPARATOR + zipFileName;
        destDirWithZipFileNameDir = FilePathUtil.generateFilePathWhenExists(destDirWithZipFileNameDir);
        ZipFile zf = null;
        try {
            zf = new ZipFile(zipFilePath);
            FileOperationResult result = new FileOperationResult();
            //Enumeration<?> entries = zf.entries();//for OS zip api
            Enumeration<?> entries = zf.getEntries();//for apache zip api
            //first, we calculate the total size of file ZipEntry, and ignore directory ZipEntry
            int totalFileCount = 0;
            ZipEntry entry = null;
            while (entries.hasMoreElements()) {
                if(isCancelled()) {
                    return result.set(FileOperationResult.FORC_USER_CANCELLED, null);
                }
                entry = (ZipEntry) entries.nextElement();
                ++totalFileCount;
            }
            //if .zip file contains 0 files inside, just return
            if(totalFileCount == 0) {
                return result.set(FileOperationResult.FORC_EMPTY_ZIP_FILE, "");
            }
            //second, we unzip every ZipEntry from .zip file
            //LogUtil.i(TAG, "unzipWholeZip --  totalFileCount: " + totalFileCount);
            int unzippedCount = 0;
            //entries = zf.entries();//for OS zip api
            entries = zf.getEntries();//for apache zip api
            while(entries.hasMoreElements()) {
                if(isCancelled()) {
                    return result.set(FileOperationResult.FORC_USER_CANCELLED, null);
                }
                entry = ((ZipEntry) entries.nextElement());
                result = unzipZipEntry(zf, entry.getName(), destDirWithZipFileNameDir, destinationDisk, false, result);
                if(!result.isSucceeded()) {
                    break;
                }
                ++ unzippedCount;
                publishProgress(totalFileCount, unzippedCount, "", -1 , -1);
            }

            boolean finished = totalFileCount == unzippedCount;
            if(finished) {
                result.realDestinationPath = destDirWithZipFileNameDir;
                result.resultCode = FileOperationResult.FORC_SUCCEEDED;
            } else {
                result.realDestinationPath = null;
                //the left properties will use unzipZipEntry result code
            }
            return result;
        } catch (Exception e) {
            if(e != null) {
                LogUtil.e(TAG, "unzipWholeZip Failed:" + e.getMessage());
            }
        } finally {
            //FilePathUtil.closeSilently(zf);
            ZipFile.closeQuietly(zf);
        }
        return null;
    }

}