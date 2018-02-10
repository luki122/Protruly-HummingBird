package com.android.launcher3.appwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.RemoteViews;

import com.android.launcher3.R;
import com.android.launcher3.colors.ColorManager;

import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Implementation of App Widget functionality.
 */
public class LauncherClock extends AppWidgetProvider {
    public static final String UPDATECLOCK = "com.android.launcher3.LauncherClock.updateClock";
    public static final String UPDATECOLOR = "com.android.launcher3.LauncherClock.updateColor";
    public static final String TAG = "LauncherClock";
    private static int mTextColor;
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        Date date = new Date();

        SimpleDateFormat forma = new SimpleDateFormat(context.getResources().getString(R.string.launcher_clock_time_format));
        String format = forma.format(date);
        String[] strings = format.split("/");
        // Construct the RemoteViews object
        if (strings != null && strings.length > 3) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.launcher_clock);
            //setTimeFormat(context,views,R.id.appwidget_time,date,strings[1]);
            //views.setTextViewText(R.id.appwidget_time, strings[1]);
            views.setTextViewText(R.id.appwidget_date, strings[0]);
            views.setTextViewText(R.id.appwidget_week, strings[2]);
            if(DateFormat.is24HourFormat(context)){
                views.setTextViewText(R.id.appwidget_aa, "");
            }else {
                views.setTextViewText(R.id.appwidget_aa, strings[3]);
            }
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.android.deskclock",
                    "com.android.deskclock.DeskClock"));
            views.setOnClickPendingIntent(R.id.appwidget_time, PendingIntent.getActivity(context, 0, intent, 0));
            views.setOnClickPendingIntent(R.id.appwidget_date, PendingIntent.getActivity(context, 0, intent, 0));
            views.setOnClickPendingIntent(R.id.appwidget_week, PendingIntent.getActivity(context, 0, intent, 0));
            Log.d(TAG, forma.format(date));
            // Instruct the widget manager to update the widget
            updateTextColor(views,context,appWidgetId,appWidgetManager);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }

    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        Log.d(TAG,"onUpdate ");
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        Log.d(TAG,"onEnabled ");
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        Log.d(TAG,"onDisabled ");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Log.d(TAG,"onReceive : "+intent.getAction());
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        if(manager==null)return;
        ComponentName reminderProvider = new ComponentName(context, LauncherClock.class);
        int[] appWidgetIds = manager.getAppWidgetIds(reminderProvider);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.launcher_clock);
        try {
            for (int appWidgetId : appWidgetIds) {
                updateAppWidget(context, manager, appWidgetId);
            }
      /*  if (UPDATECOLOR.equals(intent.getAction())) {

                for (int appWidgetId : appWidgetIds) {
                    updateTextColor(views, context, appWidgetId, manager);
                    //updateAppWidget(context, manager, appWidgetId);
                }
        } else if (UPDATECLOCK.equals(intent.getAction())){
            if(mCalendar!=null&&mCalendar.yearDay!=new Time().yearDay) {
                for (int appWidgetId : appWidgetIds) {
                    updateTextColor(views, context, appWidgetId, manager);
                    updateAppWidget(context, manager, appWidgetId);
                }
            }
            if(mCalendar!=null)
                mCalendar.setToNow();
        }else {
            for (int appWidgetId : appWidgetIds) {
                updateTextColor(views, context, appWidgetId, manager);
                updateAppWidget(context, manager, appWidgetId);
            }
        }*/
        } catch (IllegalStateException e) {
            Log.e(TAG,"onReceive : "+e.getMessage());
        }
    }

    private static void updateTextColor(RemoteViews views, Context context, int appWidgetId, AppWidgetManager manager) {
        Resources res = context.getResources();
        int colorTextBlack = res.getColor(R.color.launcher_clock_text_color_black);
        int colorTextWhite = res.getColor(R.color.launcher_clock_text_color_white);
        int colorText ;
        if (ColorManager.getInstance().isBlackText()) {
            mTextColor = colorTextBlack;
            colorText =res.getColor(R.color.launcher_clock_text_aa_color_black);
        } else  {
            mTextColor = colorTextWhite;
            colorText =res.getColor(R.color.launcher_clock_text_aa_color_white);
        }
        views.setTextColor(R.id.appwidget_time, mTextColor);
        views.setTextColor(R.id.appwidget_date, mTextColor);
        views.setTextColor(R.id.appwidget_week, mTextColor);
        views.setTextColor(R.id.appwidget_aa, colorText);
    }
    public  static void setTimeFormat(Context context, RemoteViews clock,
                                      int clockId, Date date, String string24) {
        if (clock != null) {
            SimpleDateFormat format12 = new SimpleDateFormat("hh:mm");
            String format = format12.format(date);

            // Set the best format for 12 hours mode according to the locale
            clock.setCharSequence(clockId, "setFormat12Hour",
                    format);
            // Set the best format for 24 hours mode according to the locale
            clock.setCharSequence(clockId, "setFormat24Hour", string24);
        }
    }
}

