package com.hb.note.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.hb.note.NoteApplication;
import com.hb.note.R;

public class Utils {

    private static final String SHARED_PREFS_NAME = "com.hb.note_preferences";
    private static final String KEY_NOTE_PRESET = "preferences_note_preset";
    private static final String KEY_NOTE_STYLE = "preferences_note_style";

    private static final int[] RES_IDS_DEFAULT = new int[] {
            R.color.editor_bg_color,
            R.color.editor_content_color,
            R.drawable.ic_more,
            R.drawable.ic_back,
            R.drawable.ic_menu_bill,
            R.drawable.ic_menu_font,
            R.drawable.ic_menu_camera,
            R.drawable.ic_menu_style,
            R.drawable.ic_bill_done,
            R.color.home_text_color,              // 首页按钮颜色
            R.style.NoteActionModeItem,           // 首页标题颜色
            R.drawable.checkbox_selector
    };

    private static final int[] RES_IDS_BLUE = new int[] {
            R.color.editor_bg_color_other,
            R.color.editor_content_color_blue,
            R.drawable.ic_more_blue,
            R.drawable.ic_back_blue,
            R.drawable.ic_menu_bill_blue,
            R.drawable.ic_menu_font_blue,
            R.drawable.ic_menu_camera_blue,
            R.drawable.ic_menu_style_blue,
            R.drawable.ic_bill_done_blue,
            R.color.home_text_color_blue,
            R.style.NoteActionModeItemBlue,
            R.drawable.checkbox_blue_selector
    };

    private static final int[] RES_IDS_GREEN = new int[] {
            R.color.editor_bg_color_other,
            R.color.editor_content_color_green,
            R.drawable.ic_more_green,
            R.drawable.ic_back_green,
            R.drawable.ic_menu_bill_green,
            R.drawable.ic_menu_font_green,
            R.drawable.ic_menu_camera_green,
            R.drawable.ic_menu_style_green,
            R.drawable.ic_bill_done_green,
            R.color.home_text_color_green,
            R.style.NoteActionModeItemGreen,
            R.drawable.checkbox_green_selector
    };

    private static final int[] RES_IDS_PINK = new int[] {
            R.color.editor_bg_color_other,
            R.color.editor_content_color_pink,
            R.drawable.ic_more_pink,
            R.drawable.ic_back_pink,
            R.drawable.ic_menu_bill_pink,
            R.drawable.ic_menu_font_pink,
            R.drawable.ic_menu_camera_pink,
            R.drawable.ic_menu_style_pink,
            R.drawable.ic_bill_done_pink,
            R.color.home_text_color_pink,
            R.style.NoteActionModeItemPink,
            R.drawable.checkbox_pink_selector
    };

    private static final int[][] RES_IDS_ALL = new int[][] {
            RES_IDS_DEFAULT,
            RES_IDS_BLUE,
            RES_IDS_GREEN,
            RES_IDS_PINK
    };

    public static final String[] NOTE_STYLES = new String[] {
            "White",
            "Blue",
            "Green",
            "Pink"
    };

    public static final String NOTE_STYLE_DEFAULT = NOTE_STYLES[0];

    public static SharedPreferences getSharedPreferences() {
        return NoteApplication.getInstance()
                .getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean hasPreset() {
        return getSharedPreferences()
                .getBoolean(KEY_NOTE_PRESET, false);
    }

    public static void setPreset() {
        getSharedPreferences().edit()
                .putBoolean(KEY_NOTE_PRESET, true).apply();
    }

    public static String getStylePreference() {
        return getSharedPreferences()
                .getString(KEY_NOTE_STYLE, NOTE_STYLE_DEFAULT);
    }

    public static void setStylePreference(String noteStyle) {
        getSharedPreferences().edit()
                .putString(KEY_NOTE_STYLE, noteStyle).apply();
    }

    public static boolean isDefaultStyle() {
        return NOTE_STYLE_DEFAULT.equals(getStylePreference());
    }

    public static int getStyleIndex() {
        String noteStyle = getStylePreference();

        int index = 0;
        for (String style : NOTE_STYLES) {
            if (style.equals(noteStyle)) {
                break;
            }
            index ++;
        }
        return index;
    }

    public static int[] getResIds() {
        return RES_IDS_ALL[getStyleIndex()];
    }
}
