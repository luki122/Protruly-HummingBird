package com.android.launcher3.dynamicui;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.text.format.Time;
import android.util.Log;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.FastBitmapDrawable;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.R;
import com.android.launcher3.theme.IconProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by liuzuo on 17-5-25.
 */

public class WeatherDynamic implements IDynamicIcon {

    private Context mContext;
    private int mColor;
    private int mIconId = -1;
    private int mIconSize;
    private String mTemp;
    private int mTextSize;
    private int mUnitSize;
    private int mHour = -1;

    private final String TAG = "WeatherDynamic";
    private boolean mIsRegister;
    private BubbleTextView mView;
    private static String SPLIT = " ";
    private WeatherObserver mContentResolver;
    private String AUTOHORITY = "com.moji.daling.weather.provider";
    private String FROM = "/from/dynamic_icon";
    private String DYM_WEATHER_TEXT = "com.android.launcher3$dym_weather_text";
    private Time mCalendar;
    private String[] mIconArray;

    private final Uri URI = Uri.parse("content://" + AUTOHORITY + FROM + "/" + "location");


    public WeatherDynamic() {
    }

    @Override
    public boolean init(Context context, BubbleTextView bubbleTextView, ItemInfo info) {
        final Resources r = context.getResources();
        mContext = context;
        mView = bubbleTextView;

        if (mView != null)
            mIconSize = mView.getIconSize();
        IconProvider provider = LauncherAppState.getInstance().getIconCache().getIconProvider();
        mColor = provider.getColor(DYM_WEATHER_TEXT, R.color.dym_weather_text_color, r);/* r.getColor(R.color.dym_weather_text_color);*/
        mTextSize = (int) r.getDimension(R.dimen.dym_calender_date_text_size);
        mUnitSize = (int) r.getDimension(R.dimen.dym_weather_unit_size);
        mContentResolver = new WeatherObserver(null);
        mIconArray = r.getStringArray(R.array.dym_weather_icon);

        mCalendar = new Time();
        registerReceiver();
        return updateDynamicIcon(true);
    }

    @Override
    public void removeDynamicReceiver() {

        mIntentReceiver.setUpdate(false);

    }

    @Override
    public boolean updateDynamicIcon(boolean register) {
        mIntentReceiver.setUpdate(true);
        boolean weatherChanged = onWeatherChanged(false);

        if (register) {
            registerReceiver();
        }
        return weatherChanged;
    }

    @Override
    public void clearDynamicIcon() {
        unregisterReceiver();
    }


    public boolean updateDynamicIcon() {
        return onWeatherChanged(false);
    }

    private boolean onWeatherChanged(boolean isFromBroadcast) {

        return updateWeatherIcon(isFromBroadcast);
    }

    private void getDateFromCursor() {
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(URI, null, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToNext();
                mTemp = cursor.getString(cursor.getColumnIndex("tempCurrent"))+"\u00b0";
                mIconId = getIconId(cursor.getInt(cursor.getColumnIndex("weatherIdCurrent")));
                /*mTemp = getTempNow(cursor.getString(cursor.getColumnIndex("hour24")));
                mIconId = getIconId(cursor.getInt(cursor.getColumnIndex("weatherId")));*/
            }
        } catch (Exception e) {

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private Bitmap getDymIcon() {
        Bitmap bitmap = Bitmap.createBitmap(mIconSize, mIconSize, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(bitmap);
        Drawable icon = getWeatherIcon(mContext, mIconId);
        Log.d(TAG, "mIconId=" + mIconId + "  icon=" + icon);
        if (icon == null) {
           /* icon = mContext.getDrawable(R.drawable.dym_weather_default);
            isDefaultIcon=true;*/
            return null;
        }
        if (icon != null) {
            int w = icon.getIntrinsicWidth();
            int h = icon.getIntrinsicHeight();
            int x = mIconSize / 2;
            int y = mIconSize / 2;

            if ((mIconSize < w) || (mIconSize < h)) {
                float scale = Math.min((float) mIconSize / (float) w,
                        (float) mIconSize / (float) h);
                canvas.save();

                canvas.scale(scale, scale, x, y);

            }
            icon.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
            icon.draw(canvas);
            if (mTemp != null && mTemp.length() > 0) {

                Paint mDatePaint = new Paint();
                mDatePaint.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
                mDatePaint.setTextSize(mTextSize);
                mDatePaint.setColor(mColor);
                mDatePaint.setAntiAlias(true);
                Rect rect = new Rect();
                Rect rectUnit = new Rect();

                String temp = "";
                String[] strings = mTemp.split("");
                for (int i = 0; i < strings.length - 1; i++) {
                    temp += strings[i];
                }
                String string = strings[strings.length - 1];
                string += SPLIT;
                mDatePaint.getTextBounds(temp, 0, temp.length(), rect);
                mDatePaint.setTextSize(mUnitSize);
                mDatePaint.getTextBounds(string, 0, string.length(), rectUnit);
                int rectWidth = rect.right - rect.left + rectUnit.right - rectUnit.left;
                int rectHeight = rect.bottom - rect.top;
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                canvas.drawText(string, (width - rectWidth) / 2 - rectUnit.left + rectWidth - 3, height / 2 -
                                rectHeight - rect.top,
                        mDatePaint);
                mDatePaint.setTextSize(mTextSize);
                Log.d(TAG, "drawText= " + string + "  rect=" + temp + "  mTextSize=" + mTextSize);
                canvas.drawText(temp, (width - rectWidth) / 2 - rect.left, (height - rectHeight) / 2 - rect.top, mDatePaint);

            }
            //Bitmap scalebmp = LauncherAppState.getInstance().getIconCache().getIconProvider().normalizeIcons(bitmap);
            return bitmap;
        }
        return null;
    }

    private boolean updateWeatherIcon(boolean isFromBroadcast) {
        if (mIconSize == 0)
            return false;
        mHour = mCalendar.hour;
        DynamicProvider.sWorker.post(new Runnable() {
            @Override
            public void run() {
                getDateFromCursor();
                final Bitmap dymIcon = getDymIcon();
                if(mView!=null){
                    mView.post(new Runnable() {
                        @Override
                        public void run() {
                            updateUI(dymIcon);
                        }
                    });
                }

            }
        });
/*       *//* DynamicProvider.sWorker.*//*post(new Runnable() {
            @Override
            public void run() {
                getDateFromCursor();
                mDymIcon = getDymIcon();
                new MainThreadExecutor().execute(mRunnable);
            }
        });*/
        return false;
    }


    private Drawable getWeatherIcon(Context context, int iconId) {
        if (mIconArray != null && mIconArray.length > 0 && iconId >= 0) {
            return LauncherAppState.getInstance().getIconCache().getIconProvider().getIconFromManager(context, mIconArray[iconId], -1);
        }
        return null;
    }


    private final DymBroadcastReceiver mIntentReceiver = new DymBroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!mIntentReceiver.isUpdate()) {
                return;
            }
            if (intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                updateDynamicIcon();
            } else if (Intent.ACTION_TIME_TICK.equals(intent.getAction())) {
                if (mCalendar == null) mCalendar = new Time();

                mCalendar.setToNow();
                if (mCalendar.hour != mHour) {
                    updateDynamicIcon();
                }

            } else {
                updateDynamicIcon();
            }


        }
    };


    private synchronized void registerReceiver() {
        if (!mIsRegister) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIME_TICK);
            if (mContext != null) {
                mContext.registerReceiver(mIntentReceiver, filter);
                mContext.getContentResolver().registerContentObserver(URI,
                        true, mContentResolver);
            }
            mIsRegister = true;
            Log.d(TAG, "weather registerReceiver");
        }
    }

    private synchronized void unregisterReceiver() {
        try {
            if (mIsRegister) {
                mContext.getContentResolver().unregisterContentObserver(mContentResolver);
                mContext.unregisterReceiver(mIntentReceiver);
                mIsRegister = false;
            }
            Log.e(TAG, "WeatherDynamic is unregistered");
        } catch (Exception e) {
            Log.e(TAG, "WeatherDynamic unregistered failed");
        }
    }

    private String getTempNow(String json) {
        Log.d(TAG, json);
        String tempNow = null;
        if (mCalendar == null) {
            mCalendar = new Time();
        }
        mCalendar.setToNow();
        int hour = mCalendar.minute < 30 ? mCalendar.hour : mCalendar.hour + 1;
        String systemTime;
        if (hour < 10) {
            systemTime = "0" + hour + ":00";
        } else {
            systemTime = hour + ":00";
        }

        try {
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject object = (JSONObject) jsonArray.get(i);
                String time = object.getString("time");
                if (systemTime.equals(time)) {
                    String temp = object.getString("temp");
                    tempNow = temp + "\u00b0";
                }
            }
        } catch (JSONException e) {
            Log.d(TAG, "JSONException=" + e);
        }

        return tempNow;
    }

    private int getIconId(int weatherId) {

        return weatherId;
    }


    private void updateUI(Bitmap dymIcon){
        if (mView != null && dymIcon != null) {
            mView.setIcon(new FastBitmapDrawable(dymIcon));
            mView.updateFolderIcon();
        }
    }

    private class WeatherObserver extends ContentObserver {
        WeatherObserver(Handler handler) {
            super(handler);
        }

        //当ContentProvier数据发生改变，则触发该函数
        @Override
        public void onChange(boolean selfChange) {
            Log.d(TAG, "WeatherObserver    onChange");
            super.onChange(selfChange);
        }
    }

}



