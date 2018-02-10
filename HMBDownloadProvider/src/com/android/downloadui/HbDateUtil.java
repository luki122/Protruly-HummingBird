package com.android.downloadui;

import java.util.Calendar;
import java.util.TimeZone;
import com.android.providers.downloads.R;
import android.content.Context;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
/**
 * @author wxue
 */
public class HbDateUtil {
   
	public static String formatTime(Context context, long when) {
        // TODO: DateUtils should make this easier
        Time then = new Time();
        then.set(when);
        Time now = new Time();
        now.setToNow();

        int flags = DateUtils.FORMAT_NO_NOON | DateUtils.FORMAT_NO_MIDNIGHT
                | DateUtils.FORMAT_ABBREV_ALL;

        if (then.year != now.year) {
            flags |= DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE;
        } else if (then.yearDay != now.yearDay) {
            flags |= DateUtils.FORMAT_SHOW_DATE;
        } else {
            flags |= DateUtils.FORMAT_SHOW_TIME;
        }
   
        return DateUtils.formatDateTime(context, when, flags);
    }
	
	public static String getFormatTime(Context context, long when){
		Calendar now = Calendar.getInstance();
		Calendar whenCalendar = Calendar.getInstance();
		whenCalendar.setTimeInMillis(when);
		LogUtil.i("---getFormatTime()--now = " + DateFormat.format("yyyy年M月d日,  h:mmaa", now) + " when = " + DateFormat.format("yyyy年M月d日,  h:mmaa", whenCalendar));
		if(now.get(Calendar.YEAR) > whenCalendar.get(Calendar.YEAR)){
			return String.valueOf(DateFormat.format(context.getResources().getString(R.string.hmb_ymd), whenCalendar));
		}
		int nowDayOfYear = now.get(Calendar.DAY_OF_YEAR);
		int whenDayOfYear = whenCalendar.get(Calendar.DAY_OF_YEAR);
		LogUtil.i("---nowDayOfYear = " + nowDayOfYear + "whenDayOfYear = " + whenDayOfYear);
		if(nowDayOfYear - whenDayOfYear >= 7){
			return String.valueOf(DateFormat.format(context.getResources().getString(R.string.hmb_md), whenCalendar));
		}
		if(nowDayOfYear - whenDayOfYear >= 3){
			return String.valueOf(DateFormat.format(context.getResources().getString(R.string.hmb_weekday), whenCalendar));
		}
		if(nowDayOfYear - whenDayOfYear >= 2){
			return context.getResources().getString(R.string.hmb_before_yesterday);
		}
		if(nowDayOfYear - whenDayOfYear >= 1){
			return context.getResources().getString(R.string.hmb_yesterday);
		}
		if(now.getTimeInMillis() - when >= 5 * 60 * 1000){
			if(DateFormat.is24HourFormat(context)){
				return String.valueOf(DateFormat.format(context.getResources().getString(R.string.hmb_hm_24), whenCalendar));
			}else{
				if(whenCalendar.get(Calendar.AM_PM) == 1){
					return context.getResources().getString(R.string.hmb_afternoon,DateFormat.format(context.getResources().getString(R.string.hmb_hm_12), whenCalendar));
				}else{
					return context.getResources().getString(R.string.hmb_morning,DateFormat.format(context.getResources().getString(R.string.hmb_hm_12), whenCalendar));
				}
			}
		}
		return context.getResources().getString(R.string.hmb_just);
	}
}
