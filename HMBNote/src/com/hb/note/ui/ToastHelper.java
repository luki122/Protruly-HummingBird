package com.hb.note.ui;

import android.widget.Toast;

import com.hb.note.NoteApplication;

public class ToastHelper {
    public static void show(int resId) {
        Toast.makeText(NoteApplication.getInstance(), resId, Toast.LENGTH_SHORT).show();
    }
}
