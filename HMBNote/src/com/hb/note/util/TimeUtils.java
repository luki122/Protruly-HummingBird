package com.hb.note.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeUtils {

    public static String formatTime(long time, String pattern) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
        return dateFormat.format(new Date(time));
    }

}
