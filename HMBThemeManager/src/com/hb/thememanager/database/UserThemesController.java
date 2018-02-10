package com.hb.thememanager.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.hb.thememanager.model.Theme;
import com.hb.thememanager.utils.Config;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alexluo on 17-9-29.
 */

public class UserThemesController extends ThemeDatabaseController<Theme> {


    public UserThemesController(Context context, int themeType) {
        super(context, themeType);
    }

    @Override
    protected Theme createTypeInstance() {
        return null;
    }


    @Override
    protected int getThemeType() {
        return super.getThemeType();
    }


    @Override
    public Theme getThemeByPath(String filePath) {
        return null;
    }




    @Override
    public List<Theme> getThemes() {
        // TODO Auto-generated method stub
        ArrayList<Theme> themes = new ArrayList<Theme>();
        if (getDatabase() != null) {
            Cursor cursor = getDatabase().query(getTableName(),
                    null, null, null, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    Theme theme = new Theme();
                    theme.id = cursor.getString(cursor.getColumnIndex(_ID));
                    theme.userId = cursor.getInt(cursor.getColumnIndex(USER_ID));
                    theme.type = cursor.getInt(cursor.getColumnIndex(TYPE));
                    themes.add(theme);
                }
            }
            cursor.close();
        }
        return themes;
    }

    @Override
    public List<Theme> getThemesByUser(int userId) {
        ArrayList<Theme> themes = new ArrayList<Theme>();
        if (getDatabase() != null) {
            Cursor cursor = getDatabase().query(getTableName(),
                    null, Config.DatabaseColumns.USER_ID + "=?", new String[]{String.valueOf(userId)}
                    , null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    Theme theme = new Theme();
                    theme.id = cursor.getString(cursor.getColumnIndex(_ID));
                    theme.userId = cursor.getInt(cursor.getColumnIndex(USER_ID));
                    theme.type = cursor.getInt(cursor.getColumnIndex(TYPE));
                    themes.add(theme);
                }
            }
            cursor.close();
        }
        return themes;
    }

    @Override
    public boolean updateTheme(Theme theme) {
        if (getDatabase() != null) {
            ContentValues values = new ContentValues();

            values.put(Config.DatabaseColumns.TYPE, theme.type);
            values.put(Config.DatabaseColumns.USER_ID, theme.userId);
            updateThemeOtherStatus(theme,values);
            update( values, Config.DatabaseColumns._ID + "=?", new String[]{theme.id});
        }
        return true;
    }


    @Override
    public void insertTheme(Theme theme) {
        if (getDatabase() != null) {
            ContentValues values = new ContentValues();
            values.put(Config.DatabaseColumns._ID,theme.id);
            values.put(Config.DatabaseColumns.TYPE, theme.type);
            values.put(Config.DatabaseColumns.USER_ID, theme.userId);
            insertThemeOtherStatus(theme,values);
            long ok = insert(values);
        }
    }

    @Override
    public Theme getThemeById(String themeId) {

        Theme theme = null;
            if (getDatabase() != null) {
                Cursor cursor = getDatabase().query(getTableName(),
                        null, Config.DatabaseColumns._ID + "=?", new String[]{themeId}, null, null, null);
                if (cursor != null && cursor.getCount() > 0) {

                    if (cursor.moveToNext()) {
                        theme = new Theme();
                        theme.id = cursor.getString(cursor.getColumnIndex(_ID));
                        theme.userId = cursor.getInt(cursor.getColumnIndex(USER_ID));
                        theme.type = cursor.getInt(cursor.getColumnIndex(TYPE));
                    }
                }
                cursor.close();
            }
            return theme;
    }
}
