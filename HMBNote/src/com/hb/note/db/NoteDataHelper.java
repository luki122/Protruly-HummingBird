package com.hb.note.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.hb.note.util.Globals;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NoteDataHelper {

    private static final String[] NOTE_COLUMNS = new String[]{
            Notes.ID,
            Notes.UUID,
            Notes.TITLE,
            Notes.CONTENT,
            Notes.CHARACTERS,
            Notes.IMAGE_COUNT,
            Notes.UPDATE_TIME,
            Notes.STICK_TIME
    };

    private static final int INDEX_ID = 0;
    private static final int INDEX_UUID = 1;
    private static final int INDEX_TITLE = 2;
    private static final int INDEX_CONTENT = 3;
    private static final int INDEX_CHARACTERS = 4;
    private static final int INDEX_IMAGE_COUNT = 5;
    private static final int INDEX_UPDATE_TIME = 6;
    private static final int INDEX_STICK_TIME = 7;

    private static final String WHERE_BY_ID = Notes.ID + "=?";
    private static final String ORDER_BY_TIME = Notes.STICK_TIME + " DESC, " +
            Notes.UPDATE_TIME + " DESC";
    private static final String SELECTION_BY_KEY = Notes.CHARACTERS + " LIKE '%?%'";

    private NoteDatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    public NoteDataHelper(Context context) {
        mDbHelper = new NoteDatabaseHelper(context);
        mDb = mDbHelper.getWritableDatabase();
    }

    public void shutdown() {
        if (mDbHelper != null) {
            mDbHelper.close();
            mDbHelper = null;
            mDb = null;
        }
    }

    public static void initPresetData(Context context, String title) {
        NoteData noteData = new NoteData();
        noteData.setTitle(title);
        noteData.setContent(Globals.SPAN_START + Globals.SPAN_TITLE + Globals.SPAN_END + title);
        noteData.setCharacters(title);

        NoteDataHelper noteDataHelper = new NoteDataHelper(context);
        noteDataHelper.insert(noteData);
        noteDataHelper.shutdown();
    }

    private String[] getWhereArgsById(int id) {
        return new String[]{ String.valueOf(id)};
    }

    private ContentValues getContentValues(NoteData noteData) {
        ContentValues values = new ContentValues();
        values.put(Notes.TITLE, noteData.getTitle());
        values.put(Notes.CONTENT, noteData.getContent());
        values.put(Notes.CHARACTERS, noteData.getCharacters());
        values.put(Notes.IMAGE_COUNT, noteData.getImageCount());
        return values;
    }

    private NoteData getNoteData(Cursor cursor) {
        NoteData noteData = new NoteData();
        noteData.setId(cursor.getInt(INDEX_ID));
        noteData.setUuid(cursor.getString(INDEX_UUID));
        noteData.setTitle(cursor.getString(INDEX_TITLE));
        noteData.setContent(cursor.getString(INDEX_CONTENT));
        noteData.setCharacters(cursor.getString(INDEX_CHARACTERS));
        noteData.setImageCount(cursor.getInt(INDEX_IMAGE_COUNT));
        noteData.setUpdateTime(cursor.getLong(INDEX_UPDATE_TIME));
        noteData.setStickTime(cursor.getLong(INDEX_STICK_TIME));
        return noteData;
    }

    private List<NoteData> getNoteDataList(Cursor cursor) {
        List<NoteData> noteDataList = new ArrayList<>();
        while (!cursor.isAfterLast()) {
            noteDataList.add(getNoteData(cursor));
            cursor.moveToNext();
        }
        return noteDataList;
    }

    public long insert(ContentValues values) {
        return mDb.insert(Notes.TABLE_NAME, null, values);
    }

    public long insert(NoteData noteData) {
        ContentValues values = getContentValues(noteData);
        values.put(Notes.UUID, UUID.randomUUID().toString());
        values.put(Notes.UPDATE_TIME, System.currentTimeMillis());

        return insert(values);
    }

    public int bulkInsert(List<NoteData> noteDataList) {
        mDb.beginTransaction();
        try {
            for (NoteData noteData : noteDataList) {
                insert(noteData);
            }
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }
        return noteDataList.size();
    }

    public void delete(int id) {
        mDb.delete(Notes.TABLE_NAME, WHERE_BY_ID, getWhereArgsById(id));
    }

    public int bulkDelete(List<Integer> ids) {
        mDb.beginTransaction();
        try {
            for (Integer id : ids) {
                delete(id);
            }
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }
        return ids.size();
    }

    public void update(ContentValues values) {
        if (!values.containsKey(Notes.ID)) {
            return;
        }

        mDb.update(Notes.TABLE_NAME, values,
                WHERE_BY_ID, getWhereArgsById(values.getAsInteger(Notes.ID)));
    }

    public void update(NoteData noteData) {
        ContentValues values = getContentValues(noteData);
        values.put(Notes.ID, noteData.getId());
        values.put(Notes.UPDATE_TIME, System.currentTimeMillis());

        update(values);
    }

    public void stick(int id, long stickTime) {
        ContentValues values = new ContentValues();
        values.put(Notes.ID, id);
        values.put(Notes.STICK_TIME, stickTime);

        update(values);
    }

    public int bulkStick(List<Integer> ids) {
        long stickTime = System.currentTimeMillis();
        mDb.beginTransaction();
        try {
            for (Integer id : ids) {
                stick(id, stickTime);
            }
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }
        return ids.size();
    }

    public int bulkCancelStick(List<Integer> ids) {
        long stickTime = 0;
        mDb.beginTransaction();
        try {
            for (Integer id : ids) {
                stick(id, stickTime);
            }
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }
        return ids.size();
    }

    public NoteData query(int id) {
        Cursor cursor = mDb.query(Notes.TABLE_NAME, NOTE_COLUMNS,
                WHERE_BY_ID, getWhereArgsById(id), null, null, null);
        if (cursor == null) {
            return null;
        }

        NoteData noteData = null;
        if (cursor.moveToFirst()) {
            noteData = getNoteData(cursor);
        }
        cursor.close();
        return noteData;
    }

    public List<NoteData> query(String selection, String[] selectionArgs) {
        Cursor cursor = mDb.query(Notes.TABLE_NAME, NOTE_COLUMNS,
                selection, selectionArgs, null, null, ORDER_BY_TIME);
        if (cursor == null) {
            return null;
        }

        List<NoteData> noteDataList = null;
        if (cursor.moveToFirst()) {
            noteDataList = getNoteDataList(cursor);
        }
        cursor.close();
        return noteDataList;
    }

    public List<NoteData> queryAll() {
        return query(null, null);
    }

    public List<NoteData> queryByKey(String key) {
        return query(SELECTION_BY_KEY, new String[] { key.replace("'", "''")});
    }
}
