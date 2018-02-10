package com.android.settings.utils;

import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import com.android.settings.R;

/**
 * Created by liuqin on 17-6-28.
 *
 * @date Liuqin on 2017-06-28
 */
public class PasswordOnTouchListener implements View.OnTouchListener {
    private EditText passwdView;

    public PasswordOnTouchListener(EditText passwdView) {
        this.passwdView = passwdView;
    }

    @Override

    public boolean onTouch(View view, MotionEvent event) {
        final int DRAWABLE_LEFT = 0;
        final int DRAWABLE_TOP = 1;
        final int DRAWABLE_RIGHT = 2;
        final int DRAWABLE_BOTTOM = 3;

        if (event.getX() >= (passwdView.getRight() -
                passwdView.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width() - passwdView.getLeft())) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                boolean isVisible = !isVisiblePasswordInputType(passwdView.getInputType());
                int resId = isVisible ? R.drawable.visible_password : R.drawable.invisible_password;
                int pos = passwdView.getSelectionEnd();
                passwdView.setCompoundDrawablesWithIntrinsicBounds(0, 0, resId, 0);
                passwdView.setInputType(

                        InputType.TYPE_CLASS_TEXT |
                                (isVisible ?
                                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                                        InputType.TYPE_TEXT_VARIATION_PASSWORD));
                if (pos >= 0) {
                    ((EditText) passwdView).setSelection(pos);
                }
            }
            return true;
        }
        return false;
    }

    private static boolean isVisiblePasswordInputType(int inputType) {
        final int variation =
                inputType & (EditorInfo.TYPE_MASK_CLASS | EditorInfo.TYPE_MASK_VARIATION);
        return variation
                == (EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
    }
}
