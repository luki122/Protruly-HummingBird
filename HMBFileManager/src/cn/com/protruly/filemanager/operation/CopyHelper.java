package cn.com.protruly.filemanager.operation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import cn.com.protruly.filemanager.utils.FilePathUtil;
import cn.com.protruly.filemanager.utils.LogUtil;

/**
 * Created by sqf on 17-7-11.
 */

public class CopyHelper {

    private static final String TAG = "CopyHelper";
    public static final int EOF = -1;

    public interface CopyListener {
        boolean isCancelledByUser();
        void onPartialCopied(long alreadyRead, long total, String destinationFilePath);
    }

    /**
     * copy a file and call copy callback
     * @param srcFile file to read
     * @param destinationFilePath destination file to write
     * @param listener copy progress listener
     * @return FORC_XXXXX
     */
    public static int copySingleFile(File srcFile, String destinationFilePath, FileDbManager fileDbManager, final CopyListener listener) {
        /*
        if(listener == null) {
            throw new IllegalArgumentException("CopyHelper::copySingleFile listener MUST NOT be null");
        }
        */
        FileInputStream inputStream = null;
        FileOutputStream outputStream = null;
        FileChannel inputChannel = null;
        FileChannel outputChannel = null;
        try {
            inputStream = new FileInputStream(srcFile);
            LogUtil.i(TAG, "srcFile:" + srcFile.getAbsolutePath());
            outputStream = new FileOutputStream(destinationFilePath);
            inputChannel = inputStream.getChannel();
            outputChannel = outputStream.getChannel();
            ByteBuffer buffer = ByteBuffer.allocateDirect(BaseOperationTask.BUFF_SIZE);
            buffer.clear();
            //inputChannel.transferTo(0, inputChannel.size(), outputChannel);
            long totalCount = srcFile.length();
            if(0 == totalCount) {
                //LogUtil.i(TAG, "copySingleFile srcFile:" + srcFile.getAbsolutePath() + " 0 bytes , we see it as FORC_SUCCEEDED");
                listener.onPartialCopied(1, 1, destinationFilePath);
                //return FileOperationResult.FORC_FILE_ZERO_BYTE;
                //if file is zero byte, we see it as succeeded.
                return FileOperationResult.FORC_SUCCEEDED;
            }
            long readCount;
            long alreadyRead = 0;
            while(alreadyRead < totalCount) {
                boolean isCancelled = listener.isCancelledByUser();
                //LogUtil.i(TAG, "CopyHelper::copySingleFile isCancelled:" + isCancelled + " ----- ");
                if(isCancelled) {
                    LogUtil.i(TAG, "copySingleFile canncelllllllled......");
                    return FileOperationResult.FORC_USER_CANCELLED;
                }
                readCount = inputChannel.read(buffer);
                //LogUtil.i(TAG, "readCount:" + readCount);
                if(readCount <= 0) {
                    LogUtil.i(TAG, "EOF");
                    break;
                }
                //LogUtil.i(TAG, "readCount :" + readCount + System.currentTimeMillis());
                buffer.flip();
                outputChannel.write(buffer);
                buffer.clear();
                alreadyRead += readCount;
                listener.onPartialCopied(alreadyRead, totalCount, destinationFilePath);
            }
            //fileDbManager.insert(srcFile.getAbsolutePath(), destinationFilePath, false);
            fileDbManager.addToInsertList(srcFile.getAbsolutePath(), destinationFilePath, false);
        } catch (FileNotFoundException e) {
            LogUtil.e(TAG, e.getMessage());
            LogUtil.i(TAG, "copySingleFile FileNotFoundException......");
            return FileOperationResult.FORC_FILE_NOT_EXISTS;
        } catch (IOException e) {
            LogUtil.e(TAG, e.getMessage());
            LogUtil.i(TAG, "copySingleFile IOException......");
            return FileOperationResult.FORC_UNKNOWN_ERROR;
        } finally {
            //LogUtil.i(TAG, "copySingleFile finally......");
            FilePathUtil.closeSilently(outputChannel);
            FilePathUtil.closeSilently(inputChannel);
            FilePathUtil.closeSilently(inputStream);
            FilePathUtil.closeSilently(outputStream);
        }
        //LogUtil.i(TAG, "copySingleFile FileOperationResult.FORC_SUCCEEDED;......");
        return FileOperationResult.FORC_SUCCEEDED;
    }
}
