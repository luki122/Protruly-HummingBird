package cn.com.protruly.filemanager.operation;

import android.content.Context;
import android.media.MediaScannerConnection;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;

import cn.com.protruly.filemanager.enums.FileInfo;
import cn.com.protruly.filemanager.utils.DebugUtil;
import cn.com.protruly.filemanager.utils.FilePathUtil;
import cn.com.protruly.filemanager.utils.LogUtil;
import cn.com.protruly.filemanager.utils.Util;

/**
 * Created by sqf on 17-5-25.
 */

public class MoveTask extends BaseOperationTask implements CopyHelper.CopyListener {

    private static final String TAG = "MoveTask";

    private HashSet<FileInfo> mFileInfos;
    private String mDestinationDirectory;
    private boolean mHasJustOneFile;
    private String mDiskPath;

    private int mCopiedCount;
    private int mTotalFileCount;

    public MoveTask(Context context, FileDbManager fileDbManager, FileOperationTaskListener listener, HashSet<FileInfo> fileInfos, String destinationDirectory) {
        super(context, fileDbManager, OperationType.CUT, listener);
        mFileInfos = fileInfos;
        mDestinationDirectory = destinationDirectory;
        mDiskPath = FilePathUtil.getCurrentDisk(context, destinationDirectory);
        mHasJustOneFile = hasJustOneFile();
        mCopiedCount = 0;
    }

    private boolean hasJustOneFile() {
        if(mFileInfos.size() != 1) return false;
        Iterator<FileInfo> it = mFileInfos.iterator();
        while (it.hasNext()) {
            FileInfo fileInfo = it.next();
            return fileInfo.isFile();
        }
        return false;
    }

    @Override
    protected FileOperationResult doOperation() {
        //DebugUtil.debugFileInfos(mFileInfos);
        FileOperationResult result = new FileOperationResult();
        mTotalFileCount = FilePathUtil.getAllFileAndDirectoryNum(mFileInfos);
        //LogUtil.i(TAG, "doOperation mTotalFileCount:" + mTotalFileCount);
        if(0 == mTotalFileCount) {
            //if all FileInfo does not exist
            return result.set(FileOperationResult.FORC_FILE_NOT_EXISTS, null);
        }
        for(FileInfo fileInfo : mFileInfos) {
            if(isCancelled()) {
                return result.set(FileOperationResult.FORC_USER_CANCELLED, mDestinationDirectory);
            }
            //if file to move is in the destination directory, just reture FORC_SUCCEEDED.
            if(mDestinationDirectory.equals(fileInfo.getParent())) {
                //NOT ALLOWED
                //LogUtil.i(TAG, "Copy or move into self's directory, return FORC_SUCCEEDED directly.");
                mCopiedCount += FilePathUtil.getAllFileAndDirectoryNum(fileInfo.getFile());
                result = result.set(FileOperationResult.FORC_SUCCEEDED, null);
                publishProgress(mTotalFileCount, mCopiedCount, null, -1 , -1);
                continue;
            }
            if (fileInfo.isFile()) {
                result = moveSingleFile(fileInfo.getFile(), mDestinationDirectory, result);
            } else if (fileInfo.isDirectory()) {
                if(FilePathUtil.isSelfOrDescendantPath(fileInfo.getPath(), mDestinationDirectory)) {
                    //move to self's sub directory NOT ALLOWED
                    /* LogUtil.i(TAG, "Move into subdirectory not allowed." + "file to move:" + fileInfo.getPath() + " destination directory:" + mDestinationDirectory);*/
                    result.set(FileOperationResult.FORC_COPY_OR_MOVE_INTO_SUBDIRECTORY_NOT_ALLOWED, null);
                    mCopiedCount += FilePathUtil.getAllFileAndDirectoryNum(fileInfo.getFile());
                    continue;
                }
                result = moveDirectory(fileInfo.getFile(), mDestinationDirectory, result);
            }
        }
        //LogUtil.i(TAG, "doOperation--> deleteFromDeleteList");
        mFileDbManager.deleteFromDeleteList();
        //LogUtil.i(TAG, "doOperation--> insertFromContentValueList");
        mFileDbManager.insertFromContentValueList();
        return result;
    }

    /**
     * move a single file(.mp3, *jpg, *.png ...) into a destination directory.
     * @param fileToMove
     * @param destinationDir
     * @param result
     * @return
     */
    private FileOperationResult moveSingleFile(File fileToMove, String destinationDir, FileOperationResult result) {
        result = moveSingleFileByRename(fileToMove, destinationDir, result);
        if(!result.isSucceeded()) {
            result = copyAndDeleteSingleFile(fileToMove, destinationDir, result);
        }
        return result;
    }

    private FileOperationResult moveDirectory(File dirToMove, String destinationDir, FileOperationResult result) {
        //LogUtil.i(TAG, "moveDirectory --> dirToMove:" + dirToMove + " destinationDir:" + destinationDir);
        result = moveSingleDirectoryByRename(dirToMove, destinationDir, result);
        if(!result.isSucceeded()) {
            result = copyAndDeleteDirectory(dirToMove, destinationDir, result);
        }
        return result;
    }

    private FileOperationResult moveSingleFileByRename(File fileToMove, String destinationDir, FileOperationResult result) {
        //LogUtil.i(TAG, "moveSingleFileByRename --> fileToMove:" + fileToMove + " destinationDir:" + destinationDir);
        String fileName = fileToMove.getName();
        String destinationFilePath = destinationDir + FilePathUtil.PATH_SEPARATOR + fileName;
        //LogUtil.i(TAG, "moveSingleFileByRename --> destinationFilePath:" + destinationFilePath + " before generateFilePathWhenExists");
        destinationFilePath = FilePathUtil.generateFilePathWhenExists(destinationFilePath);
        FilePathUtil.mkdirs(destinationDir);
        //LogUtil.i(TAG, "moveSingleFileByRename --> destinationFilePath:" + destinationFilePath + " after generateFilePathWhenExists");
        boolean succeeded = fileToMove.renameTo(new File(destinationFilePath));
        if(!succeeded) {
            //LogUtil.i(TAG, "moveSingleFileByRename --> fileToMove renameTo " + destinationFilePath + " FAILED ");
            return result.set(FileOperationResult.FORC_RENAME_TO_FAILED_WHEN_MOVING, null);
        }
        //LogUtil.i(TAG, "moveSingleFileByRename --> fileToMove renameTo " + destinationFilePath + " SUCCEEDED ");
        //mFileDbManager.insert(fileToMove.getAbsolutePath(), destinationFilePath, false);
        mFileDbManager.addToInsertList(fileToMove.getAbsolutePath(), destinationFilePath, false);
        //mFileDbManager.delete(mContext, fileToMove.getAbsolutePath(), false);
        mFileDbManager.addToDeleteList(fileToMove.getAbsolutePath());
        mCopiedCount ++;
        publishProgress(mTotalFileCount, mCopiedCount, destinationFilePath, -1 , -1);
        return result.set(FileOperationResult.FORC_SUCCEEDED, null);
    }

    private FileOperationResult moveSingleDirectoryByRename(File dirToMove, String destinationDir, FileOperationResult result) {
        //LogUtil.i(TAG, "moveSingleDirectoryByRename --> dirToMove:" + dirToMove + " destinationDir:" + destinationDir);
        String name = dirToMove.getName();
        String destinationPath = destinationDir + FilePathUtil.PATH_SEPARATOR + name;
        //LogUtil.i(TAG, "moveSingleDirectoryByRename --> destinationPath:" + destinationPath + " before generateFilePathWhenExists");
        destinationPath = FilePathUtil.generateFilePathWhenExists(destinationPath);
        int countToMove = FilePathUtil.getAllFileAndDirectoryNum(dirToMove);
        //LogUtil.i(TAG, "moveSingleDirectoryByRename --> countToMove:" + countToMove);
        boolean renameResult = dirToMove.renameTo(new File(destinationPath));
        if(!renameResult) {
            //LogUtil.i(TAG, "moveSingleDirectoryByRename --> dirToMove renameTo " + destinationPath + " FAILED ");
            return result.set(FileOperationResult.FORC_RENAME_TO_FAILED_WHEN_MOVING, null);
        } else {
            mFileDbManager.batchDeleteFileStartWithPathPrefix(mContext, dirToMove.getPath());
        }
        //LogUtil.i(TAG, "moveSingleDirectoryByRename --> dirToMove renameTo " + destinationPath + " SUCCEEDED ");
        mCopiedCount += countToMove;
        publishProgress(mTotalFileCount, mCopiedCount, destinationPath, -1 , -1);
        //MediaScannerConnection.scanFile(mContext, new String[]{ destinationPath }, null, null);
        //mFileDbManager.addToInsertList(destinationDir, true);
        mFileDbManager.scanFileInfo(new FileInfo(destinationDir));
        return result.set(FileOperationResult.FORC_SUCCEEDED, null);
    }

    /**
     * move a single file(.mp3, *jpg, *.png ...) into a destination directory.
     * @param fileToMove
     * @param destinationDir
     * @param result
     * @return
     */
    private FileOperationResult copyAndDeleteSingleFile(File fileToMove, String destinationDir, FileOperationResult result) {
        //judge whether has enough space for copying
        long totalFileSize = FilePathUtil.getFileSize(fileToMove);
        long currentDiskFreeSize = Util.getFreeSize(mDiskPath);
        //LogUtil.i(TAG, "doOperation currentDiskFreeSize:" + currentDiskFreeSize + " totalFileSize:" + totalFileSize + " testCurrentDiskFreeSize:" + testCurrentDiskFreeSize);
        if(totalFileSize > currentDiskFreeSize) {
            //LogUtil.i(TAG, "doOperation insufficient space: currentDiskFreeSize:" + currentDiskFreeSize + " totalFileSize:" + totalFileSize);
            return result.set(FileOperationResult.FORC_INSUFFICIENT_SPACE, null);
        }

        String fileName = fileToMove.getName();
        String destinationFilePath = destinationDir + FilePathUtil.PATH_SEPARATOR + fileName;
        destinationFilePath = FilePathUtil.generateFilePathWhenExists(destinationFilePath);
        FilePathUtil.createNewFile(destinationFilePath);

        long freeSize = Util.getFreeSize(mDiskPath);
        if(fileToMove.length() > freeSize) {
            return result.set(FileOperationResult.FORC_INSUFFICIENT_SPACE, null);
        }

        //firstly, copy
        int copyResult = CopyHelper.copySingleFile(fileToMove, destinationFilePath, mFileDbManager, this);
        if(copyResult != FileOperationResult.FORC_SUCCEEDED) {
            //LogUtil.i(TAG, "copyAndDeleteSingleFile NOT SUCCEEDED copyResult:" + copyResult);
            return result.set(copyResult, destinationFilePath);
        }
        //LogUtil.i(TAG, "copyAndDeleteSingleFile copy SUCCEEDED -----:");
        //secondly, delete original;
        if(fileToMove.exists()) {
            //LogUtil.i(TAG, "delete --> " + fileToMove.getPath());
            fileToMove.delete();
        }
        //mFileDbManager.delete(mContext, fileToMove.getAbsolutePath(), false);
        mFileDbManager.addToDeleteList(fileToMove.getAbsolutePath());

        //LogUtil.i(TAG, "copyAndDeleteSingleFile delete SUCCEEDED -----:");

        mCopiedCount ++;
        //LogUtil.i(TAG, "copyAndDeleteSingleFile SUCCEEDED mCopiedCount:" + mCopiedCount + " mTotalFileCount:" + mTotalFileCount);

        if(!mHasJustOneFile) {
            publishProgress(mTotalFileCount, mCopiedCount, destinationFilePath, -1 , -1);
        }
        return result.set(FileOperationResult.FORC_SUCCEEDED, destinationFilePath);

    }


    private FileOperationResult copyAndDeleteDirectory(File fileToMove, String destinationDir, FileOperationResult result) {
        //LogUtil.i(TAG, "copyAndDeleteDirectory srcDirFile:" + fileToMove.getAbsolutePath() + " destinationDir:" + destinationDir);
        //judge whether has enough space for copying
        long totalFileSize = FilePathUtil.getFileSize(fileToMove);
        long currentDiskFreeSize = Util.getFreeSize(mDiskPath);
        //long testCurrentDiskFreeSize = fileToMove.getFreeSpace();
        //LogUtil.i(TAG, "copyAndDeleteDirectory currentDiskFreeSize:" + currentDiskFreeSize + " totalFileSize:" + totalFileSize + " testCurrentDiskFreeSize:" + testCurrentDiskFreeSize);
        if(totalFileSize > currentDiskFreeSize) {
            //LogUtil.i(TAG, "copyAndDeleteDirectory insufficient space: currentDiskFreeSize:" + currentDiskFreeSize + " totalFileSize:" + totalFileSize);
            return result.set(FileOperationResult.FORC_INSUFFICIENT_SPACE, null);
        }

        //firstly, copy
        String name = fileToMove.getName();
        String destinationPath = destinationDir + FilePathUtil.PATH_SEPARATOR + name;
        destinationPath = FilePathUtil.generateFilePathWhenExists(destinationPath);
        FilePathUtil.mkdirs(destinationPath);
        //mFileDbManager.insert(fileToMove.getAbsolutePath(), destinationPath, true);
        mFileDbManager.addToInsertList(fileToMove.getAbsolutePath(), destinationPath, true);
        mCopiedCount ++;

        publishProgress(mTotalFileCount, mCopiedCount, destinationPath, -1 , -1);
        /*
        LogUtil.i(TAG, "copyAndDeleteDirectory srcDirFile:" + fileToMove.getAbsolutePath() +
                " real destinationPath:" + destinationPath +
                " srcDirFile name:" + name);*/

        File[] files = fileToMove.listFiles();
        if(null != files && 0 != files.length) {
            for (int i = 0; i < files.length; i++) {
                if (isCancelled()) {
                    return result.set(FileOperationResult.FORC_USER_CANCELLED, null);
                }
                File file = files[i];
                if (file.isFile()) {
                    result = copyAndDeleteSingleFile(file, destinationPath, result);
                } else if (file.isDirectory()) {
                    result = copyAndDeleteDirectory(file, destinationPath, result);
                }

                //secondly, delete original;
                if (result.isSucceeded()) {
                    //mFileDbManager.delete(mContext, file.getAbsolutePath(), file.isDirectory());
                    mFileDbManager.addToDeleteList(file.getAbsolutePath());
                    if (file.exists()) {
                        //LogUtil.i(TAG, "copyAndDeleteDirectory delete --> " + file.getPath());
                        file.delete();
                    }
                } else {
                    result.set(result.resultCode, null);
                    //LogUtil.i(TAG, "copyAndDeleteDirectory failed...Directory:" + fileToMove.getAbsolutePath());
                    return result;
                }
            }
        }

        //mFileDbManager.delete(mContext, fileToMove.getAbsolutePath(), true);
        mFileDbManager.addToDeleteList(fileToMove.getAbsolutePath());
        if (fileToMove.exists()) {
            //LogUtil.i(TAG, "copyAndDeleteDirectory delete --> " + fileToMove.getPath());
            fileToMove.delete();
        }
        //LogUtil.i(TAG, "copyAndDeleteDirectory succeeded...");
        return result.set(FileOperationResult.FORC_SUCCEEDED, destinationPath);
    }

    @Override
    public boolean isCancelledByUser() {
        boolean cancelled = isCancelled();
        if(cancelled) {
            LogUtil.i(TAG, "MoveTask :: isCancelledByUser:" + cancelled);
        }
        return cancelled;
    }

    @Override
    public void onPartialCopied(long alreadyRead, long total, String destinationFilePath) {
        try {
            if (mHasJustOneFile) {
                publishProgress(total, alreadyRead, destinationFilePath, -1 , -1);
                if (total < SINGLE_FILE_SLEEP_SIZE_THRESHOLD) {
                    //progress bar will not perform normally, if we don't sleep(0) here
                    Thread.sleep(1);
                }
            }
        } catch (Exception e) {

        }
    }
}
