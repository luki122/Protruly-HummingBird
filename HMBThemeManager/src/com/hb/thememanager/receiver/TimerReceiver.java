package com.hb.thememanager.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

import com.hb.thememanager.ThemeManager;
import com.hb.thememanager.ThemeManagerImpl;
import com.hb.thememanager.database.DatabaseFactory;
import com.hb.thememanager.database.SharePreferenceManager;
import com.hb.thememanager.database.ThemeDatabaseController;
import com.hb.thememanager.listener.OnThemeStateChangeListener;
import com.hb.thememanager.manager.TimerManager;
import com.hb.thememanager.model.Fonts;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.state.StateManager;
import com.hb.thememanager.state.ThemeState;
import com.hb.thememanager.utils.CommonUtil;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.TLog;
import com.hb.thememanager.utils.ToastUtils;
import com.hb.thememanager.R;
import java.util.HashMap;

import hb.preference.PreferenceManager;

/**
 * Created by alexluo on 17-8-21.
 */

public class TimerReceiver extends BroadcastReceiver {
    private static final String TAG = "TryTimer";
    public static final String ACTION = "com.hb.thememanager.ACTION_TIMER_TRY_THEME";
    public static final String ACTION_CANCEL = "com.hb.thememanager.ACTION_CANCEL_TIMER_TRY_THEME";
    private static final String ACTION_START_TIMER = "com.hb.thememanager.start_timer";
    public static final String ACTION_FINISH_TIMER = ACTION_START_TIMER;
    private static final int TRY_TIME_LIMIT = TimerManager.MAX_TRY_TIME;
    public static final String KEY_TIMER_INTENT_ID = "timer_intent_id";
    public static final String KEY_TIMER_INTENT_TYPE = "timer_intent_type";
    @Override
    public void onReceive(final Context context, Intent intent) {
        TLog.d(TAG,"Action Enter->"+intent.getAction());
        final SharedPreferences preferences = hb.preference.PreferenceManager.getDefaultSharedPreferences(context);
        StateManager mStateManager = StateManager.getInstance(context);
        mStateManager.setFromTry(true);
        if(ACTION.equals(intent.getAction())){
            /**
             * 主题或者字体试用逻辑为：
             * 1、如果当前已应用的主题是系统内置或者免费的主题，则先备份当前正常主题的ID。
             * 2、如果当前正在使用的主题是试用的主题，则停止当前正在试用的主题，试用新的主题
             * 3、不管什么时候正在使用（试用）的主题或者字体有且只有一个，通过主题的type来区别试用
             * 计时器
             */
            Theme theme = intent.getParcelableExtra(Config.ActionKey.KEY_APPLY_THEME_IN_SERVICE);
            if(theme != null) {
               final TimerManager.TimerObj timerObj = new TimerManager.TimerObj(theme);
                //先删除正在试用的计时器
                TimerManager.getTimerFromSharedPrefs(preferences,String.valueOf(theme.type));
                startTimer(context,true,timerObj);
                ThemeManager tm = ThemeManagerImpl.getInstance(context);
                mStateManager.setTryStateChangeListener(new OnThemeStateChangeListener() {
                    @Override
                    public void onStateChange(ThemeState state) {
                        if(state == ThemeState.STATE_APPLY_SUCCESS){
                            //开始新的倒计时
                            startTimer(context,false,timerObj);
                        }else if(state == ThemeState.STATE_START_APPLY){
                            ToastUtils.showShortToast(context,R.string.start_try_theme);
                        }
                    }
                });
                //试用目标主题
                tm.applyTheme(theme,context, mStateManager);
            }
        }else if(ACTION_START_TIMER.equals(intent.getAction())){
            TLog.d(TAG,"cancel count->");
            String timerObjId = intent.getStringExtra(KEY_TIMER_INTENT_ID);
            int timerObjType = intent.getIntExtra(KEY_TIMER_INTENT_TYPE,-1);
            final TimerManager.TimerObj finishedTimerObj = TimerManager.getTimerFromSharedPrefs(preferences,String.valueOf(timerObjType));
            ThemeDatabaseController dbController = DatabaseFactory.createDatabaseController(timerObjType,context);
            if(dbController != null){
                Theme lastNormalApplyTheme ;
                String normalThemeId;

                /**
                 * 获取正常使用的主题或者字体
                 */
                if(timerObjType == Theme.THEME_PKG){
                    normalThemeId = SharePreferenceManager.getStringPreference(context,
                            SharePreferenceManager.KEY_APPLIED_NORMAL_THEME_ID,Config.DEFAULT_THEME_ID);
                }else{
                    normalThemeId = SharePreferenceManager.getStringPreference(context,
                            SharePreferenceManager.KEY_APPLIED_NORMAL_FONT_ID,Config.DEFAULT_FONT_ID);
                }
                lastNormalApplyTheme = dbController.getThemeById(normalThemeId);
                if(lastNormalApplyTheme == null){
                    if(timerObjType == Theme.THEME_PKG){
                        lastNormalApplyTheme = new Theme();
                        lastNormalApplyTheme.id = Config.DEFAULT_THEME_ID;
                    }else{
                        lastNormalApplyTheme = new Fonts();
                        lastNormalApplyTheme.id = Config.DEFAULT_FONT_ID;
                    }
                }
                lastNormalApplyTheme.type = timerObjType;
                /*
                 *试用时间到之后恢复到试用之前正常的使用主题
                 */
                if(lastNormalApplyTheme != null){
                    ThemeManager tm = ThemeManagerImpl.getInstance(context);
                    mStateManager.setTryStateChangeListener(new OnThemeStateChangeListener() {
                        @Override
                        public void onStateChange(ThemeState state) {
                            if(state == ThemeState.STATE_APPLY_SUCCESS){
                                startTimer(context,false,finishedTimerObj);
                                TimerManager.deleteFromSharedPref(preferences,finishedTimerObj);
                            }else if(state == ThemeState.STATE_START_APPLY){
                                ToastUtils.showShortToast(context,R.string.end_try_theme);
                            }
                        }
                    });
                    tm.applyTheme(lastNormalApplyTheme,context, mStateManager);
                }
            }

            /*
             *清除对应的主题的试用计时器
             */
            startTimer(context,true,finishedTimerObj);
            TimerManager.deleteFromSharedPref(PreferenceManager
                    .getDefaultSharedPreferences(context),finishedTimerObj);

        }else if(ACTION_CANCEL.equals(intent.getAction())){
            TLog.d(TAG,"cancel count if needed->");
            String timerObjId = intent.getStringExtra(KEY_TIMER_INTENT_ID);
            int timerObjType = intent.getIntExtra(KEY_TIMER_INTENT_TYPE,-1);
            final TimerManager.TimerObj finishedTimerObj = TimerManager.getTimerFromSharedPrefs(preferences,String.valueOf(timerObjType));
            startTimer(context,true,finishedTimerObj);
            TimerManager.deleteFromSharedPref(PreferenceManager
                    .getDefaultSharedPreferences(context),finishedTimerObj);

        }
    }


    private void startTimer(Context context, boolean cancel, TimerManager.TimerObj timer) {
        AlarmManager manager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        long triggerAtTime = SystemClock.elapsedRealtime() + TRY_TIME_LIMIT;
        Intent intent = new Intent();
        intent.setAction(ACTION_START_TIMER);
        intent.setClass(context, TimerReceiver.class);
        if(timer != null){
            intent.putExtra(KEY_TIMER_INTENT_ID,timer.id);
            intent.putExtra(KEY_TIMER_INTENT_TYPE,timer.type);
        }
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);
        if(cancel){
            manager.cancel(pi);
            pi.cancel();
        }else{
            manager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
        }

    }


}
