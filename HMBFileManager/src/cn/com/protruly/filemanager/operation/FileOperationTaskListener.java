package cn.com.protruly.filemanager.operation;

/**
 * Created by sqf on 17-5-13.
 */

public interface FileOperationTaskListener {
    void onOperationStart(int operationType);

    /**
     *
     * @param operationType
     * @param progress indicates how many files have been operated. if 2 files out of ten are operated, the progress is 20%
     * @param filePath
     * @param copiedSize used only in copy or cut mode
     * @param totalSizeToCopy used only in copy or cut mode, the value copiedSize/totalFileSize is the progress of operating a single file.
     */
    void onOperationProgress(int operationType, int progress, String filePath, long copiedSize, long totalSizeToCopy);
    void onOperationCancelled(int operationType, FileOperationResult result);
    void onOperationSucceeded(int operationType, FileOperationResult result);
    void onOperationFailed(int operationType, FileOperationResult result);
}