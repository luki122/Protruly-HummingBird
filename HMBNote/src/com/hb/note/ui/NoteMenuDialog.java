package com.hb.note.ui;

import android.app.Dialog;
import android.content.Context;
import android.view.WindowManager.LayoutParams;

import com.hb.note.R;

public class NoteMenuDialog extends Dialog {

    public NoteMenuDialog(Context context) {
        super(context, R.style.MenuDialog);
    }

    @Override
    public void show() {
        super.show();

        LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.width = LayoutParams.MATCH_PARENT;
        getWindow().setAttributes(layoutParams);
    }
}
