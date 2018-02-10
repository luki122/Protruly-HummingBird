package com.hb.note;

import android.app.Application;

import com.hb.note.db.NoteDataHelper;
import com.hb.note.util.Utils;

public class NoteApplication extends Application {

    private static NoteApplication sNoteApp;

    public static NoteApplication getInstance() {
        return sNoteApp;
    }

    @Override
    public void onCreate() {
        sNoteApp = this;
        super.onCreate();
        initPresetData();
    }

    private void initPresetData() {
        if (!Utils.hasPreset()) {
            Utils.setPreset();

            NoteDataHelper.initPresetData(this, getString(R.string.note_preset_content));
        }
    }
}
