package cn.com.protruly.filemanager.operation;

import android.content.Context;
import android.database.Cursor;

import cn.com.protruly.filemanager.categorylist.SelectionManager;
import cn.com.protruly.filemanager.utils.SingleMediaScanner;

/**
 * Created by sqf on 17-5-16.
 */

public class CategoryCopyTask extends BaseOperationTask {

    private Cursor mCursor;
    private SelectionManager mSelectionManager;
    private String mDestinationDirectory;

    public CategoryCopyTask(Context context, FileDbManager fileDbManager, FileOperationTaskListener listener,
                            Cursor cursor, SelectionManager selectionManager, String destinationDirectory) {
        super(context, fileDbManager, 0, listener);
        mCursor = cursor;
        mSelectionManager = selectionManager;
        mDestinationDirectory = destinationDirectory;
    }

    @Override
    protected FileOperationResult doOperation() {
        /*
        int selectedCount = mSelectionManager.getSelectedCount();
        int totalCount = mCursor.getCount();
        int finishedCount = 0;
        for(int i = 0; i < totalCount; i ++) {
            mCursor.moveToPosition(i);
            final String filePath = mCursor.getString(CategoryFactory.COLUMN_INDEX_DISPLAY_DATA);
            if(mSelectionManager.isSelected(filePath)) {
                FileOperationResult result = FileOperationUtil.copySingleFile(filePath, mDestinationDirectory);
                if(!result.isSucceeded()) {
                    return result;
                }
                ++finishedCount;
                publishProgress((float) finishedCount * 100 / selectedCount);
            }
        }
        */
        return null;
    }
}
