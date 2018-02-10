package cn.com.protruly.soundrecorder.util;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.text.TextUtils;
import android.widget.Toast;

import cn.com.protruly.soundrecorder.RecordListActivity;
import cn.com.protruly.soundrecorder.recordlist.FileNameInputDialog;
import hb.app.dialog.AlertDialog;
import cn.com.protruly.soundrecorder.R;

/**
 * Created by sqf on 17-9-19.
 */

public class FileNameDialogUtil {

    private static final String TAG = "FileNameDialogUtil";

    public static boolean startWithDot(String name){
        return !TextUtils.isEmpty(name) && !name.equals(".") && name.startsWith(".");
    }

    public static void createNameInputDialog(final Context context, String title, String name, final String postfix, final FileInfo fileInfo,
                                                 final FileNameInputDialog.OnConfirmedListener onConfirmedListener) {
        FileNameInputDialog.OnFinishFileInputListener finishFileInputListener = new FileNameInputDialog.OnFinishFileInputListener() {
            @Override
            public void onFinishFileNameInput(String str, String prefix) {
                if(startWithDot(str)){
                    showConfirmStartWithDotDialog(context, fileInfo, str + postfix, onConfirmedListener);
                    return;
                }
                //doAfterNewNameConfirmed(fileInfo,str+prefix);
                LogUtil.i(TAG, "call onConfirmed.............str:" + str + " prefix:" + prefix);
                onConfirmedListener.onConfirmed(fileInfo, str + postfix);
            }
        };
        FileNameInputDialog  fileNameInputDialog = new FileNameInputDialog(context, title, name, fileInfo, finishFileInputListener);
        fileNameInputDialog.setIsFile(fileInfo.isFile());
        fileNameInputDialog.show();
    }

    private static void showConfirmStartWithDotDialog(Context context, final cn.com.protruly.soundrecorder.util.FileInfo fileInfo, final String str,
                                                                 final FileNameInputDialog.OnConfirmedListener onConfirmedListener) {
        Resources res = context.getResources();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(res.getString(R.string.confirm_hiden_file_create));
        builder.setPositiveButton(res.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //doAfterNewNameConfirmed(fileInfo,str);
                LogUtil.i(TAG, "showConfirmStartWithDotDialog 222222222" + str) ;
                onConfirmedListener.onConfirmed(fileInfo, str);
            }
        });
        builder.setNegativeButton(res.getString(android.R.string.cancel),null);
        builder.show();
    }
}
