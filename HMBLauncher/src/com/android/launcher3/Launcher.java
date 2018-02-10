/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks2;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.support.v4.app.ActivityCompat;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.TextKeyListener;
import android.util.Log;
import android.view.Display;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Advanceable;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.dlauncher.badge.BadgeController;
import com.android.dlauncher.badge.BadgeInfo;
import com.android.dlauncher.badge.LauncherBadgeProvider;
import com.android.launcher3.DropTarget.DragObject;
import com.android.launcher3.LauncherSettings.Favorites;
import com.android.launcher3.accessibility.LauncherAccessibilityDelegate;
import com.android.launcher3.allapps.AllAppsContainerView;
import com.android.launcher3.allapps.AllAppsTransitionController;
import com.android.launcher3.allapps.DefaultAppSearchController;
import com.android.launcher3.appwidget.LauncherClock;
import com.android.launcher3.colors.ColorManager;
import com.android.launcher3.compat.AppWidgetManagerCompat;
import com.android.launcher3.compat.LauncherActivityInfoCompat;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.compat.UserHandleCompat;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.config.ProviderConfig;
import com.android.launcher3.dragndrop.DragController;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.dragndrop.DragOptions;
import com.android.launcher3.dragndrop.DragView;
import com.android.launcher3.dynamicui.DynamicProvider;
import com.android.launcher3.dynamicui.ExtractedColors;
import com.android.launcher3.dynamicui.IDynamicIcon;
import com.android.launcher3.folder.Folder;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.keyboard.ViewGroupFocusHelper;
import com.android.launcher3.logging.FileLog;
import com.android.launcher3.logging.UserEventDispatcher;
import com.android.launcher3.model.WidgetsModel;
import com.android.launcher3.pageindicators.PageIndicator;
import com.android.launcher3.pageindicators.PageIndicatorDiagitalImagview;
import com.android.launcher3.shortcuts.DeepShortcutManager;
import com.android.launcher3.shortcuts.DeepShortcutsContainer;
import com.android.launcher3.shortcuts.ShortcutKey;
import com.android.launcher3.theme.IconManager;
import com.android.launcher3.theme.blur.BlurFactory;
import com.android.launcher3.userevent.nano.LauncherLogProto;
import com.android.launcher3.util.ActivityResultInfo;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.GridOccupancy;
import com.android.launcher3.util.ItemInfoMatcher;
import com.android.launcher3.util.MultiHashMap;
import com.android.launcher3.util.PackageManagerHelper;
import com.android.launcher3.util.PendingRequestArgs;
import com.android.launcher3.util.TestingUtils;
import com.android.launcher3.util.Thunk;
import com.android.launcher3.util.ViewOnDrawExecutor;
import com.android.launcher3.wallpaperpicker.WallpaperPagedViewContainer;
import com.android.launcher3.widget.BaseWidgetsContainerView;
import com.android.launcher3.widget.PendingAddWidgetInfo;
import com.android.launcher3.widget.WidgetHostViewLoader;
import com.android.launcher3.widget.WidgetsContainerPagedView;
import com.android.launcher3.widget.WidgetsContainerView;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.android.launcher3.specialeffectpreview.PreviewContainer;

/**
 * Default launcher application.
 */
public class Launcher extends Activity
        implements LauncherExterns, View.OnClickListener, OnLongClickListener,
                   LauncherModel.Callbacks, View.OnTouchListener, LauncherProviderChangeListener,
                   AccessibilityManager.AccessibilityStateChangeListener ,ColorManager.IWallpaperChange ,LauncherBadgeProvider.BadgeChangedCallBack{//lijun add ColorManager.IWallpaperChange LauncherBadgeProvider.BadgeChangedCallBack
    public static final String TAG = "Launcher";
    static final boolean LOGD = true;

    static final boolean DEBUG_WIDGETS = false;
    static final boolean DEBUG_STRICT_MODE = false;
    static final boolean DEBUG_RESUME_TIME = false;

    private static final int REQUEST_CREATE_SHORTCUT = 1;
    private static final int REQUEST_CREATE_APPWIDGET = 5;
    private static final int REQUEST_PICK_APPWIDGET = 9;
    private static final int REQUEST_PICK_WALLPAPER = 10;

    private static final int REQUEST_BIND_APPWIDGET = 11;
    private static final int REQUEST_BIND_PENDING_APPWIDGET = 14;
    private static final int REQUEST_RECONFIGURE_APPWIDGET = 12;

    private static final int REQUEST_PERMISSION_CALL_PHONE = 13;

    public static final int REQUEST_PERMISSION_ALL = 0;//lijun add for checkAllPermission

    private static final float BOUNCE_ANIMATION_TENSION = 1.3f;

    /**
     * IntentStarter uses request codes starting with this. This must be greater than all activity
     * request codes used internally.
     */
    protected static final int REQUEST_LAST = 100;

    // To turn on these properties, type
    // adb shell setprop logTap.tag.PROPERTY_NAME [VERBOSE | SUPPRESS]
    static final String DUMP_STATE_PROPERTY = "launcher_dump_state";

    // The Intent extra that defines whether to ignore the launch animation
    static final String INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION =
            "com.android.launcher3.intent.extra.shortcut.INGORE_LAUNCH_ANIMATION";

    public static final String ACTION_APPWIDGET_HOST_RESET =
            "com.android.launcher3.intent.ACTION_APPWIDGET_HOST_RESET";

    // Type: int
    private static final String RUNTIME_STATE_CURRENT_SCREEN = "launcher.current_screen";
    // Type: int
    private static final String RUNTIME_STATE = "launcher.state";
    // Type: PendingRequestArgs
    private static final String RUNTIME_STATE_PENDING_REQUEST_ARGS = "launcher.request_args";
    // Type: ActivityResultInfo
    private static final String RUNTIME_STATE_PENDING_ACTIVITY_RESULT = "launcher.activity_result";

    static final String APPS_VIEW_SHOWN = "launcher.apps_view_shown";

    /** The different states that Launcher can be in. */
    public enum State { NONE, WORKSPACE, WORKSPACE_SPRING_LOADED, APPS, APPS_SPRING_LOADED,
        WIDGETS, WIDGETS_SPRING_LOADED,FOLDER_IMPORT,WALLPAPER ,ICONARRANGE,SPECIALEFFECT}//lijun add UNINSTALL_NORMAL mode

    public State getState() {
        return mState;
    }

    @Thunk State mState = State.WORKSPACE;
    @Thunk LauncherStateTransitionAnimation mStateTransitionAnimation;

    private boolean mIsSafeModeEnabled;

    static final int APPWIDGET_HOST_ID = 1024;
    public static final int EXIT_SPRINGLOADED_MODE_SHORT_TIMEOUT = 500;
    private static final int ON_ACTIVITY_RESULT_ANIMATION_DELAY = 500;
    private static final int ACTIVITY_START_DELAY = 1000;

    // How long to wait before the new-shortcut animation automatically pans the workspace
    private static int NEW_APPS_PAGE_MOVE_DELAY = 500;
    private static int NEW_APPS_ANIMATION_INACTIVE_TIMEOUT_SECONDS = 5;
    @Thunk static int NEW_APPS_ANIMATION_DELAY = 500;

    //lijun add for permission check
    public static String[] sAllPermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE};//,Manifest.permission.READ_SMS,Manifest.permission.READ_CALL_LOG

    private final BroadcastReceiver mUiBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_APPWIDGET_HOST_RESET.equals(intent.getAction())) {
                if (mAppWidgetHost != null) {
                    mAppWidgetHost.startListening();
                }
            }
        }
    };

    @Thunk public Workspace mWorkspace;
    private View mLauncherView;
    @Thunk DragLayer mDragLayer;
    private DragController mDragController;
    private View mQsbContainer;

    public View mWeightWatcher;

    private AppWidgetManagerCompat mAppWidgetManager;
    private LauncherAppWidgetHost mAppWidgetHost;

    private int[] mTmpAddItemCellCoordinates = new int[2];

    @Thunk Hotseat mHotseat;
    private ViewGroup mOverviewPanel;

    private View mAllAppsButton;
    private View mWidgetsButton;
    private View mWallpaperButton;//lijun add for wallpaper
    private View mVulvanClearBuuton;
    private WallpaperPagedViewContainer mWallpaperPicker;//lijun add for wallpaper
    private View mAlineButton;

    //liuzuo add the background of workspace when opening folder
    ImageView mWorkspaceBg;
    Animator mAniWorkspaceBg;
    Bitmap mBlur;

    //lijun add
    ThemeChangedLoadingView mThemeChangedLoadingView;

    private boolean exitImportModeInHomeKey;
    private static final float WORKSPACE_ALPHA_BG = 0.3f;


    private DropTargetBar mDropTargetBar;

    // Main container view for the all apps screen.
    @Thunk AllAppsContainerView mAppsView;
    AllAppsTransitionController mAllAppsController;

    // Main container view and the model for the widget tray screen.
    //lijun modify for WidgetsContainerPageView
//    @Thunk WidgetsContainerView mWidgetsView;
    @Thunk
    BaseWidgetsContainerView mWidgetsView;//lijun modify WidgetsContainerView to BaseWidgetsContainerView
    //lijun modify end

    @Thunk WidgetsModel mWidgetsModel;

    private Bundle mSavedState;
    // We set the state in both onCreate and then onNewIntent in some cases, which causes both
    // scroll issues (because the workspace may not have been measured yet) and extra work.
    // Instead, just save the state that we need to restore Launcher to, and commit it in onResume.
    private State mOnResumeState = State.NONE;

    private SpannableStringBuilder mDefaultKeySsb = null;

    @Thunk boolean mWorkspaceLoading = true;

    private boolean mPaused = true;
    private boolean mOnResumeNeedsLoad;

    private ArrayList<Runnable> mBindOnResumeCallbacks = new ArrayList<Runnable>();
    private ArrayList<Runnable> mOnResumeCallbacks = new ArrayList<Runnable>();
    private ViewOnDrawExecutor mPendingExecutor;

    private LauncherModel mModel;
    private IconCache mIconCache;
    private ExtractedColors mExtractedColors;
    private LauncherAccessibilityDelegate mAccessibilityDelegate;
    private boolean mIsResumeFromActionScreenOff;
    @Thunk boolean mUserPresent = true;
    private boolean mVisible;
    private boolean mHasFocus;
    private boolean mAttached;

    /** Maps launcher activity components to their list of shortcut ids. */
    private MultiHashMap<ComponentKey, String> mDeepShortcutMap = new MultiHashMap<>();

    private View.OnTouchListener mHapticFeedbackTouchListener;

    // Related to the auto-advancing of widgets
    private final int ADVANCE_MSG = 1;
    private final int FULLSCREEN_DELAY_MSG = 2;//lijun add for fullscreen
    private final int THEME_CHANGED_DELAY_MSG = 3;//lijun add for theme changed
    private static final int ADVANCE_INTERVAL = 20000;
    private static final int ADVANCE_STAGGER = 250;

    private boolean mAutoAdvanceRunning = false;
    private long mAutoAdvanceSentTime;
    private long mAutoAdvanceTimeLeft = -1;
    @Thunk HashMap<View, AppWidgetProviderInfo> mWidgetsToAdvance = new HashMap<>();

    // Determines how long to wait after a rotation before restoring the screen orientation to
    // match the sensor state.
    private static final int RESTORE_SCREEN_ORIENTATION_DELAY = 500;

    private final ArrayList<Integer> mSynchronouslyBoundPages = new ArrayList<Integer>();

    // We only want to get the SharedPreferences once since it does an FS stat each time we get
    // it from the context.
    private SharedPreferences mSharedPrefs;

    // Holds the page that we need to animate to, and the icon views that we need to animate up
    // when we scroll to that page on resume.
    @Thunk ImageView mFolderIconImageView;
    private Bitmap mFolderIconBitmap;
    private Canvas mFolderIconCanvas;
    private Rect mRectForFolderAnimation = new Rect();

    private DeviceProfile mDeviceProfile;

    //M:liuzuo add for addIcon begin
    private boolean isMoveToDefaultScreen;
    private boolean mOpenFolder;
    private FolderIcon mEditFolderIcon;
    private Button mFolderImportButton;
    private FolderInfo mEditFolderInfo;
    LinearLayout mFolderImportContainer;
    public ArrayList<ShortcutInfo> mCheckedShortcutInfos = new ArrayList<ShortcutInfo>();
    public ArrayList<BubbleTextView> mCheckedBubbleTextViews = new ArrayList<BubbleTextView>();
    public HashSet<FolderInfo> mCheckedFolderInfos = new HashSet<FolderInfo>();
    public HashSet<FolderIcon> mCheckedFolderIcons = new HashSet<FolderIcon>();
    Animator animImportButton;


    private boolean isSuccessAddIcon;
    //M:liuzuo add for addIcon end


    private boolean mMoveToDefaultScreenFromNewIntent;

    //lijun add for pageindicator begin
    PageIndicatorDiagitalImagview mPageIndicatorDiagital;
    //lijun add for pageindicator end

    // This is set to the view that launched the activity that navigated the user away from
    // launcher. Since there is no callback for when the activity has finished launching, enable
    // the press state and keep this reference to reset the press state when we return to launcher.
    private BubbleTextView mWaitingForResume;

    protected static HashMap<String, CustomAppWidget> sCustomAppWidgets =
            new HashMap<String, CustomAppWidget>();

    static {
        if (TestingUtils.ENABLE_CUSTOM_WIDGET_TEST) {
            TestingUtils.addDummyWidget(sCustomAppWidgets);
        }
    }

    // Exiting spring loaded mode happens with a delay. This runnable object triggers the
    // state transition. If another state transition happened during this delay,
    // simply unregister this runnable.
    private Runnable mExitSpringLoadedModeRunnable;

    @Thunk Runnable mBuildLayersRunnable = new Runnable() {
        public void run() {
            if (mWorkspace != null) {
                mWorkspace.buildPageHardwareLayers();
            }
        }
    };

    // Activity result which needs to be processed after workspace has loaded.
    private ActivityResultInfo mPendingActivityResult;
    /**
     * Holds extra information required to handle a result from an external call, like
     * {@link #startActivityForResult(Intent, int)} or {@link #requestPermissions(String[], int)}
     */
    private PendingRequestArgs mPendingRequestArgs;

    private UserEventDispatcher mUserEventDispatcher;

    public ViewGroupFocusHelper mFocusHandler;
    private boolean mRotationEnabled = false;


    //uninstallMode   add by liuzuo
    public boolean isUninstallMode = false;
    private boolean comfirmingUninstall;
    @Thunk void setOrientation() {
        if (mRotationEnabled) {
            unlockScreenOrientation(true);
        } else {
            setRequestedOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        }
    }

    private RotationPrefChangeHandler mRotationPrefChangeHandler;

	private PreviewContainer mPreviewContainer; // cyl add for special effect 

    private BadgeController mBadgeController;//lijun add for unread

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (DEBUG_STRICT_MODE) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }
        if (LauncherAppState.PROFILE_STARTUP) {
            Trace.beginSection("Launcher-onCreate");
        }

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.preOnCreate();
        }

        super.onCreate(savedInstanceState);

        checkPermission();//lijun add for checkAllPermission

        LauncherAppState app = LauncherAppState.getInstance();
        ColorManager.getInstance().addWallpaperCallback(this);//lijun add for wallpaper change
        fullscreenOrNot(false);
        // Load configuration-specific DeviceProfile
        mDeviceProfile = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE ?
                app.getInvariantDeviceProfile().landscapeProfile
                : app.getInvariantDeviceProfile().portraitProfile;

        mSharedPrefs = Utilities.getPrefs(this);
        mIsSafeModeEnabled = getPackageManager().isSafeMode();
        mModel = app.setLauncher(this);
        mBadgeController = app.initBadgeProvider(this);//lijun add for unread
        mIconCache = app.getIconCache();
        mAccessibilityDelegate = new LauncherAccessibilityDelegate(this);

        mDragController = new DragController(this);
        mAllAppsController = new AllAppsTransitionController(this);
        mStateTransitionAnimation = new LauncherStateTransitionAnimation(this, mAllAppsController);

        mAppWidgetManager = AppWidgetManagerCompat.getInstance(this);

        mAppWidgetHost = new LauncherAppWidgetHost(this, APPWIDGET_HOST_ID);
        mAppWidgetHost.startListening();

        // If we are getting an onCreate, we can actually preempt onResume and unset mPaused here,
        // this also ensures that any synchronous binding below doesn't re-trigger another
        // LauncherModel load.
        mPaused = false;

        setContentView(R.layout.launcher);

        setupViews();
        mDeviceProfile.layout(this, false /* notifyListeners */);
        mExtractedColors = new ExtractedColors();
        loadExtractedColorsAndColorItems();

        ((AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE))
                .addAccessibilityStateChangeListener(this);

        lockAllApps();

        mSavedState = savedInstanceState;
        restoreState(mSavedState);

        if (LauncherAppState.PROFILE_STARTUP) {
            Trace.endSection();
        }

        // We only load the page synchronously if the user rotates (or triggers a
        // configuration change) while launcher is in the foreground
        setWorkspaceLoading(true);//lijun modify for bug:211
        if (!mModel.startLoader(mWorkspace.getRestorePage())) {
            // If we are not binding synchronously, show a fade in animation when
            // the first page bind completes.
            mDragLayer.setAlpha(0);
        } else {
//            setWorkspaceLoading(true);//lijun modify for bug:211
        }

        // For handling default keys
        mDefaultKeySsb = new SpannableStringBuilder();
        Selection.setSelection(mDefaultKeySsb, 0);

        IntentFilter filter = new IntentFilter(ACTION_APPWIDGET_HOST_RESET);
        registerReceiver(mUiBroadcastReceiver, filter);

        mRotationEnabled = getResources().getBoolean(R.bool.allow_rotation);
        // In case we are on a device with locked rotation, we should look at preferences to check
        // if the user has specifically allowed rotation.
        if (!mRotationEnabled) {
            mRotationEnabled = Utilities.isAllowRotationPrefEnabled(getApplicationContext());
            mRotationPrefChangeHandler = new RotationPrefChangeHandler();
            mSharedPrefs.registerOnSharedPreferenceChangeListener(mRotationPrefChangeHandler);
        }

        // On large interfaces, or on devices that a user has specifically enabled screen rotation,
        // we want the screen to auto-rotate based on the current orientation
        setOrientation();

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onCreate(savedInstanceState);
        }
    }

    @Override
    public void onExtractedColorsChanged() {
        loadExtractedColorsAndColorItems();
    }

    private void loadExtractedColorsAndColorItems() {
        // TODO: do this in pre-N as well, once the extraction part is complete.
        if (Utilities.isNycOrAbove()) {
            mExtractedColors.load(this);
            mHotseat.updateColor(mExtractedColors, !mPaused);
            mWorkspace.getPageIndicator().updateColor(mExtractedColors);
            // It's possible that All Apps is visible when this is run,
            // so always use light status bar in that case.
            activateLightStatusBar(isAllAppsVisible());
        }
    }

    /**
     * Sets the status bar to be light or not. Light status bar means dark icons.
     * @param activate if true, make sure the status bar is light, otherwise base on wallpaper.
     */
    public void activateLightStatusBar(boolean activate) {
        boolean lightStatusBar = activate || (FeatureFlags.LIGHT_STATUS_BAR
                && mExtractedColors.getColor(ExtractedColors.STATUS_BAR_INDEX,
                ExtractedColors.DEFAULT_DARK) == ExtractedColors.DEFAULT_LIGHT);
        int oldSystemUiFlags = getWindow().getDecorView().getSystemUiVisibility();
        int newSystemUiFlags = oldSystemUiFlags;
        if (lightStatusBar) {
            newSystemUiFlags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        } else {
            newSystemUiFlags &= ~(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        if (newSystemUiFlags != oldSystemUiFlags) {
            getWindow().getDecorView().setSystemUiVisibility(newSystemUiFlags);
        }
    }

    private LauncherCallbacks mLauncherCallbacks;

    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onPostCreate(savedInstanceState);
        }
    }

    public void onInsetsChanged(Rect insets) {
        mDeviceProfile.updateInsets(insets);
        mDeviceProfile.layout(this, true /* notifyListeners */);
    }

    /**
     * Call this after onCreate to set or clear overlay.
     */
    public void setLauncherOverlay(LauncherOverlay overlay) {
        if (overlay != null) {
            overlay.setOverlayCallbacks(new LauncherOverlayCallbacksImpl());
        }
        mWorkspace.setLauncherOverlay(overlay);
    }

    public boolean setLauncherCallbacks(LauncherCallbacks callbacks) {
        mLauncherCallbacks = callbacks;
        mLauncherCallbacks.setLauncherSearchCallback(new Launcher.LauncherSearchCallbacks() {
            private boolean mWorkspaceImportanceStored = false;
            private boolean mHotseatImportanceStored = false;
            private int mWorkspaceImportanceForAccessibility =
                    View.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
            private int mHotseatImportanceForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO;

            @Override
            public void onSearchOverlayOpened() {
                if (mWorkspaceImportanceStored || mHotseatImportanceStored) {
                    return;
                }
                // The underlying workspace and hotseat are temporarily suppressed by the search
                // overlay. So they shouldn't be accessible.
                if (mWorkspace != null) {
                    mWorkspaceImportanceForAccessibility =
                            mWorkspace.getImportantForAccessibility();
                    mWorkspace.setImportantForAccessibility(
                            View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
                    mWorkspaceImportanceStored = true;
                }
                if (mHotseat != null) {
                    mHotseatImportanceForAccessibility = mHotseat.getImportantForAccessibility();
                    mHotseat.setImportantForAccessibility(
                            View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
                    mHotseatImportanceStored = true;
                }
            }

            @Override
            public void onSearchOverlayClosed() {
                if (mWorkspaceImportanceStored && mWorkspace != null) {
                    mWorkspace.setImportantForAccessibility(mWorkspaceImportanceForAccessibility);
                }
                if (mHotseatImportanceStored && mHotseat != null) {
                    mHotseat.setImportantForAccessibility(mHotseatImportanceForAccessibility);
                }
                mWorkspaceImportanceStored = false;
                mHotseatImportanceStored = false;
            }
        });
        return true;
    }

    @Override
    public void onLauncherProviderChange() {
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onLauncherProviderChange();
        }
    }

    /** To be overridden by subclasses to hint to Launcher that we have custom content */
    protected boolean hasCustomContentToLeft() {
        if (mLauncherCallbacks != null) {
            return mLauncherCallbacks.hasCustomContentToLeft();
        }
        return false;
    }

    /**
     * To be overridden by subclasses to populate the custom content container and call
     * {@link #addToCustomContentPage}. This will only be invoked if
     * {@link #hasCustomContentToLeft()} is {@code true}.
     */
    protected void populateCustomContentContainer() {
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.populateCustomContentContainer();
        }
    }

    /**
     * Invoked by subclasses to signal a change to the {@link #addCustomContentToLeft} value to
     * ensure the custom content page is added or removed if necessary.
     */
    protected void invalidateHasCustomContentToLeft() {
        if (mWorkspace == null || mWorkspace.getScreenOrder().isEmpty()) {
            // Not bound yet, wait for bindScreens to be called.
            return;
        }

        if (!mWorkspace.hasCustomContent() && hasCustomContentToLeft()) {
            // Create the custom content page and call the subclass to populate it.
            mWorkspace.createCustomContentContainer();
            populateCustomContentContainer();
        } else if (mWorkspace.hasCustomContent() && !hasCustomContentToLeft()) {
            mWorkspace.removeCustomContentPage();
        }
    }

    public UserEventDispatcher getUserEventDispatcher() {
        if (mLauncherCallbacks != null) {
            UserEventDispatcher dispatcher = mLauncherCallbacks.getUserEventDispatcher();
            if (dispatcher != null) {
                return dispatcher;
            }
        }

        // Logger object is a singleton and does not have to be coupled with the foreground
        // activity. Since most user event logging is done on the UI, the object is retrieved
        // from the callback for convenience.
        if (mUserEventDispatcher == null) {
            mUserEventDispatcher = new UserEventDispatcher();
        }
        return mUserEventDispatcher;
    }

    public boolean isDraggingEnabled() {
        // We prevent dragging when we are loading the workspace as it is possible to pick up a view
        // that is subsequently removed from the workspace in startBinding().
        return !isWorkspaceLoading();
    }

    public int getViewIdForItem(ItemInfo info) {
        // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
        // This cast is safe as long as the id < 0x00FFFFFF
        // Since we jail all the dynamically generated views, there should be no clashes
        // with any other views.
        return (int) info.id;
    }

    /**
     * Returns whether we should delay spring loaded mode -- for shortcuts and widgets that have
     * a configuration step, this allows the proper animations to run after other transitions.
     */
    private long completeAdd(
            int requestCode, Intent intent, int appWidgetId, PendingRequestArgs info) {
        long screenId = info.screenId;
        if (info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
            // When the screen id represents an actual screen (as opposed to a rank) we make sure
            // that the drop page actually exists.
            screenId = ensurePendingDropLayoutExists(info.screenId);
        }

        switch (requestCode) {
            case REQUEST_CREATE_SHORTCUT:
                completeAddShortcut(intent, info.container, screenId, info.cellX, info.cellY, info);
                break;
            case REQUEST_CREATE_APPWIDGET:
                completeAddAppWidget(appWidgetId, info, null, null);
                break;
            case REQUEST_RECONFIGURE_APPWIDGET:
                completeRestoreAppWidget(appWidgetId, LauncherAppWidgetInfo.RESTORE_COMPLETED);
                break;
            case REQUEST_BIND_PENDING_APPWIDGET: {
                int widgetId = appWidgetId;
                LauncherAppWidgetInfo widgetInfo =
                        completeRestoreAppWidget(widgetId, LauncherAppWidgetInfo.FLAG_UI_NOT_READY);
                if (widgetInfo != null) {
                    // Since the view was just bound, also launch the configure activity if needed
                    LauncherAppWidgetProviderInfo provider = mAppWidgetManager
                            .getLauncherAppWidgetInfo(widgetId);
                    if (provider != null && provider.configure != null) {
                        startRestoredWidgetReconfigActivity(provider, widgetInfo);
                    }
                }
                break;
            }
        }

        return screenId;
    }

    private void handleActivityResult(
            final int requestCode, final int resultCode, final Intent data) {
        if (isWorkspaceLoading()) {
            // process the result once the workspace has loaded.
            mPendingActivityResult = new ActivityResultInfo(requestCode, resultCode, data);
            return;
        }
        mPendingActivityResult = null;

        // Reset the startActivity waiting flag
        final PendingRequestArgs requestArgs = mPendingRequestArgs;
        setWaitingForResult(null);
        if (requestArgs == null) {
            return;
        }

        final int pendingAddWidgetId = requestArgs.getWidgetId();

        Runnable exitSpringLoaded = new Runnable() {
            @Override
            public void run() {
                exitSpringLoadedDragModeDelayed((resultCode != RESULT_CANCELED),
                        EXIT_SPRINGLOADED_MODE_SHORT_TIMEOUT, null);
            }
        };

        if (requestCode == REQUEST_BIND_APPWIDGET) {
            // This is called only if the user did not previously have permissions to bind widgets
            final int appWidgetId = data != null ?
                    data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) : -1;
            if (resultCode == RESULT_CANCELED) {
                completeTwoStageWidgetDrop(RESULT_CANCELED, appWidgetId, requestArgs);
                mWorkspace.removeExtraEmptyScreenDelayed(true, exitSpringLoaded,
                        ON_ACTIVITY_RESULT_ANIMATION_DELAY, false);
            } else if (resultCode == RESULT_OK) {
                addAppWidgetImpl(
                        appWidgetId, requestArgs, null,
                        requestArgs.getWidgetProvider(),
                        ON_ACTIVITY_RESULT_ANIMATION_DELAY);
            }
            return;
        } else if (requestCode == REQUEST_PICK_WALLPAPER) {
            if (resultCode == RESULT_OK && mWorkspace.isInOverviewMode()) {
                // User could have free-scrolled between pages before picking a wallpaper; make sure
                // we move to the closest one now.
                mWorkspace.setCurrentPage(mWorkspace.getPageNearestToCenterOfScreen());
                showWorkspace(false);
            }
            return;
        }

        boolean isWidgetDrop = (requestCode == REQUEST_PICK_APPWIDGET ||
                requestCode == REQUEST_CREATE_APPWIDGET);

        // We have special handling for widgets
        if (isWidgetDrop) {
            final int appWidgetId;
            int widgetId = data != null ? data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                    : -1;
            if (widgetId < 0) {
                appWidgetId = pendingAddWidgetId;
            } else {
                appWidgetId = widgetId;
            }

            final int result;
            if (appWidgetId < 0 || resultCode == RESULT_CANCELED) {
                Log.e(TAG, "Error: appWidgetId (EXTRA_APPWIDGET_ID) was not " +
                        "returned from the widget configuration activity.");
                result = RESULT_CANCELED;
                completeTwoStageWidgetDrop(result, appWidgetId, requestArgs);
                final Runnable onComplete = new Runnable() {
                    @Override
                    public void run() {
                        exitSpringLoadedDragModeDelayed(false, 0, null);
                    }
                };

                mWorkspace.removeExtraEmptyScreenDelayed(true, onComplete,
                        ON_ACTIVITY_RESULT_ANIMATION_DELAY, false);
            } else {
                if (requestArgs.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                    // When the screen id represents an actual screen (as opposed to a rank)
                    // we make sure that the drop page actually exists.
                    requestArgs.screenId =
                            ensurePendingDropLayoutExists(requestArgs.screenId);
                }
                final CellLayout dropLayout =
                        mWorkspace.getScreenWithId(requestArgs.screenId);

                dropLayout.setDropPending(true);
                final Runnable onComplete = new Runnable() {
                    @Override
                    public void run() {
                        completeTwoStageWidgetDrop(resultCode, appWidgetId, requestArgs);
                        dropLayout.setDropPending(false);
                    }
                };
                mWorkspace.removeExtraEmptyScreenDelayed(true, onComplete,
                        ON_ACTIVITY_RESULT_ANIMATION_DELAY, false);
            }
            return;
        }

        if (requestCode == REQUEST_RECONFIGURE_APPWIDGET
                || requestCode == REQUEST_BIND_PENDING_APPWIDGET) {
            if (resultCode == RESULT_OK) {
                // Update the widget view.
                completeAdd(requestCode, data, pendingAddWidgetId, requestArgs);
            }
            // Leave the widget in the pending state if the user canceled the configure.
            return;
        }

        if (requestCode == REQUEST_CREATE_SHORTCUT) {
            // Handle custom shortcuts created using ACTION_CREATE_SHORTCUT.
            if (resultCode == RESULT_OK && requestArgs.container != ItemInfo.NO_ID) {
                completeAdd(requestCode, data, -1, requestArgs);
                mWorkspace.removeExtraEmptyScreenDelayed(true, exitSpringLoaded,
                        ON_ACTIVITY_RESULT_ANIMATION_DELAY, false);

            } else if (resultCode == RESULT_CANCELED) {
                mWorkspace.removeExtraEmptyScreenDelayed(true, exitSpringLoaded,
                        ON_ACTIVITY_RESULT_ANIMATION_DELAY, false);
            }
        }
        mDragLayer.clearAnimatedView();
    }

    @Override
    protected void onActivityResult(
            final int requestCode, final int resultCode, final Intent data) {
        handleActivityResult(requestCode, resultCode, data);
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onActivityResult(requestCode, resultCode, data);
        }
        //lijun add for wallpaper
        if (mWallpaperPicker != null) {
            mWallpaperPicker.onActivityResult(requestCode, resultCode, data);
        }
        //lijun add end
    }

    /** @Override for MNC */
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] grantResults) {
        PendingRequestArgs pendingArgs = mPendingRequestArgs;
        if (requestCode == REQUEST_PERMISSION_CALL_PHONE && pendingArgs != null
                && pendingArgs.getRequestCode() == REQUEST_PERMISSION_CALL_PHONE) {
            setWaitingForResult(null);

            View v = null;
            CellLayout layout = getCellLayout(pendingArgs.container, pendingArgs.screenId);
            if (layout != null) {
                v = layout.getChildAt(pendingArgs.cellX, pendingArgs.cellY);
            }
            Intent intent = pendingArgs.getPendingIntent();

            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startActivitySafely(v, intent, null);
            } else {
                // TODO: Show a snack bar with link to settings
                Toast.makeText(this, getString(R.string.msg_no_phone_permission,
                        getString(R.string.derived_app_name)), Toast.LENGTH_SHORT).show();
            }
        }else if(requestCode == REQUEST_PERMISSION_ALL){//lijun add for checkAllPermission
            if (grantResults.length > 0) {
//                updateSDCache();
            }
        }
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onRequestPermissionsResult(requestCode, permissions,
                    grantResults);
        }
    }

    /**
     * Check to see if a given screen id exists. If not, create it at the end, return the new id.
     *
     * @param screenId the screen id to check
     * @return the new screen, or screenId if it exists
     */
    private long ensurePendingDropLayoutExists(long screenId) {
        CellLayout dropLayout = mWorkspace.getScreenWithId(screenId);
        if (dropLayout == null) {
            // it's possible that the add screen was removed because it was
            // empty and a re-bind occurred
            mWorkspace.addExtraEmptyScreen();
            return mWorkspace.commitExtraEmptyScreen();
        } else {
            return screenId;
        }
    }

    @Thunk void completeTwoStageWidgetDrop(
            final int resultCode, final int appWidgetId, final PendingRequestArgs requestArgs) {
        CellLayout cellLayout = mWorkspace.getScreenWithId(requestArgs.screenId);
        Runnable onCompleteRunnable = null;
        int animationType = 0;

        AppWidgetHostView boundWidget = null;
        if (resultCode == RESULT_OK) {
            animationType = Workspace.COMPLETE_TWO_STAGE_WIDGET_DROP_ANIMATION;
            final AppWidgetHostView layout = mAppWidgetHost.createView(this, appWidgetId,
                    requestArgs.getWidgetProvider());
            boundWidget = layout;
            onCompleteRunnable = new Runnable() {
                @Override
                public void run() {
                    completeAddAppWidget(appWidgetId, requestArgs, layout, null);
                    exitSpringLoadedDragModeDelayed((resultCode != RESULT_CANCELED),
                            EXIT_SPRINGLOADED_MODE_SHORT_TIMEOUT, null);
                }
            };
        } else if (resultCode == RESULT_CANCELED) {
            mAppWidgetHost.deleteAppWidgetId(appWidgetId);
            animationType = Workspace.CANCEL_TWO_STAGE_WIDGET_DROP_ANIMATION;
        }
        if (mDragLayer.getAnimatedView() != null) {
            mWorkspace.animateWidgetDrop(requestArgs, cellLayout,
                    (DragView) mDragLayer.getAnimatedView(), onCompleteRunnable,
                    animationType, boundWidget, true);
        } else if (onCompleteRunnable != null) {
            // The animated view may be null in the case of a rotation during widget configuration
            onCompleteRunnable.run();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        FirstFrameAnimatorHelper.setIsVisible(false);

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onStop();
        }

        if (Utilities.isNycMR1OrAbove()) {
            mAppWidgetHost.stopListening();
        }
        updateDynamicStatus(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirstFrameAnimatorHelper.setIsVisible(true);

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onStart();
        }

        if (Utilities.isNycMR1OrAbove()) {
            mAppWidgetHost.startListening();
        }
        updateDynamicStatus(true);//liuzuo add
    }

    @Override
    protected void onResume() {
        Log.e("IconManager1","Launcher onResume");
        long startTime = 0;
        if (DEBUG_RESUME_TIME) {
            startTime = System.currentTimeMillis();
            Log.v(TAG, "Launcher.onResume()");
        }

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.preOnResume();
        }

        super.onResume();
        getUserEventDispatcher().resetElapsedSessionMillis();

        // Restore the previous launcher state
        if (mOnResumeState == State.WORKSPACE) {
            showWorkspace(false);
        } else if (mOnResumeState == State.APPS) {
            boolean launchedFromApp = (mWaitingForResume != null);
            // Don't update the predicted apps if the user is returning to launcher in the apps
            // view after launching an app, as they may be depending on the UI to be static to
            // switch to another app, otherwise, if it was
            showAppsView(false /* animated */, !launchedFromApp /* updatePredictedApps */,
                    mAppsView.shouldRestoreImeState() /* focusSearchBar */);
        } else if (mOnResumeState == State.WIDGETS) {
            showWidgetsView(false, false);
        }
        //lijun add for wallpaper start
        else if (mOnResumeState == State.WALLPAPER) {
            showWallpaperPanel(true);
        }
        //lijun add end
        mOnResumeState = State.NONE;

        //lijun add for theme changed start
        if(mModel.reloadForThemechanged && mModel.getCallback() == this){
            IconManager.getInstance(this);
            mModel.reloadForThemechanged = false;
            showThemeChangingDialog();
        }
        //lijun add for theme changed end
        mPaused = false;
        if (mOnResumeNeedsLoad) {
            setWorkspaceLoading(true);
            mModel.startLoader(getCurrentWorkspaceScreen());
            mOnResumeNeedsLoad = false;
        }
        if (mBindOnResumeCallbacks.size() > 0) {
            // We might have postponed some bind calls until onResume (see waitUntilResume) --
            // execute them here
            long startTimeCallbacks = 0;
            if (DEBUG_RESUME_TIME) {
                startTimeCallbacks = System.currentTimeMillis();
            }

            for (int i = 0; i < mBindOnResumeCallbacks.size(); i++) {
                mBindOnResumeCallbacks.get(i).run();
            }
            mBindOnResumeCallbacks.clear();
            if (DEBUG_RESUME_TIME) {
                Log.d(TAG, "Time spent processing callbacks in onResume: " +
                    (System.currentTimeMillis() - startTimeCallbacks));
            }
        }
        if (mOnResumeCallbacks.size() > 0) {
            for (int i = 0; i < mOnResumeCallbacks.size(); i++) {
                mOnResumeCallbacks.get(i).run();
            }
            mOnResumeCallbacks.clear();
        }

        // Reset the pressed state of icons that were locked in the press state while activities
        // were launching
        if (mWaitingForResume != null) {
            // Resets the previous workspace icon press state
            mWaitingForResume.setStayPressed(false);
        }

        // It is possible that widgets can receive updates while launcher is not in the foreground.
        // Consequently, the widgets will be inflated in the orientation of the foreground activity
        // (framework issue). On resuming, we ensure that any widgets are inflated for the current
        // orientation.
        if (!isWorkspaceLoading()) {
            getWorkspace().reinflateWidgetsIfNecessary();
        }

        if (DEBUG_RESUME_TIME) {
            Log.d(TAG, "Time spent in onResume: " + (System.currentTimeMillis() - startTime));
        }

        // We want to suppress callbacks about CustomContent being shown if we have just received
        // onNewIntent while the user was present within launcher. In that case, we post a call
        // to move the user to the main screen (which will occur after onResume). We don't want to
        // have onHide (from onPause), then onShow, then onHide again, which we get if we don't
        // suppress here.
        if (mWorkspace.getCustomContentCallbacks() != null
                && !mMoveToDefaultScreenFromNewIntent) {
            // If we are resuming and the custom content is the current page, we call onShow().
            // It is also possible that onShow will instead be called slightly after first layout
            // if PagedView#setRestorePage was set to the custom content page in onCreate().
            if (mWorkspace.isOnOrMovingToCustomContent()) {
                mWorkspace.getCustomContentCallbacks().onShow(true);
            }
        }
        mMoveToDefaultScreenFromNewIntent = false;
        updateInteraction(Workspace.State.NORMAL, mWorkspace.getState());
        mWorkspace.onResume();

        if (!isWorkspaceLoading()) {
            // Process any items that were added while Launcher was away.
            InstallShortcutReceiver.disableAndFlushInstallQueue(this);

            // Refresh shortcuts if the permission changed.
            mModel.refreshShortcutsIfRequired();
        }

        if (shouldShowDiscoveryBounce() && FeatureFlags.LAUNCHER3_ALL_APPS_PULL_UP) {//lijun add && FeatureFlags.LAUNCHER3_ALL_APPS_PULL_UP
            mAllAppsController.showDiscoveryBounce();
        }
        mIsResumeFromActionScreenOff = false;
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onResume();
        }
		mWorkspace.setCycleSlideFlag(); // cyl add for cycle slide
    }

    @Override
    protected void onPause() {
        // Ensure that items added to Launcher are queued until Launcher returns
        InstallShortcutReceiver.enableInstallQueue();

        super.onPause();
        mPaused = true;
        mDragController.cancelDrag();
        mDragController.resetLastGestureUpTime();

        // We call onHide() aggressively. The custom content callbacks should be able to
        // debounce excess onHide calls.
        if (mWorkspace.getCustomContentCallbacks() != null) {
            mWorkspace.getCustomContentCallbacks().onHide();
        }

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onPause();
        }
        cancelToast();//lijun add
        //liuzuo add
        if(mEditFolderIcon!=null&&getImportMode()) {
            forceExitImportMode();
        }
    }

    public interface CustomContentCallbacks {
        // Custom content is completely shown. {@code fromResume} indicates whether this was caused
        // by a onResume or by scrolling otherwise.
        public void onShow(boolean fromResume);

        // Custom content is completely hidden
        public void onHide();

        // Custom content scroll progress changed. From 0 (not showing) to 1 (fully showing).
        public void onScrollProgressChanged(float progress);

        // Indicates whether the user is allowed to scroll away from the custom content.
        boolean isScrollingAllowed();
    }

    public interface LauncherOverlay {

        /**
         * Touch interaction leading to overscroll has begun
         */
        public void onScrollInteractionBegin();

        /**
         * Touch interaction related to overscroll has ended
         */
        public void onScrollInteractionEnd();

        /**
         * Scroll progress, between 0 and 100, when the user scrolls beyond the leftmost
         * screen (or in the case of RTL, the rightmost screen).
         */
        public void onScrollChange(float progress, boolean rtl);

        /**
         * Called when the launcher is ready to use the overlay
         * @param callbacks A set of callbacks provided by Launcher in relation to the overlay
         */
        public void setOverlayCallbacks(LauncherOverlayCallbacks callbacks);
    }

    public interface LauncherSearchCallbacks {
        /**
         * Called when the search overlay is shown.
         */
        public void onSearchOverlayOpened();

        /**
         * Called when the search overlay is dismissed.
         */
        public void onSearchOverlayClosed();
    }

    public interface LauncherOverlayCallbacks {
        public void onScrollChanged(float progress);
    }

    class LauncherOverlayCallbacksImpl implements LauncherOverlayCallbacks {

        public void onScrollChanged(float progress) {
            if (mWorkspace != null) {
                mWorkspace.onOverlayScrollChanged(progress);
            }
        }
    }

    protected boolean hasSettings() {
        //lijun modify to remove settings
        /*if (mLauncherCallbacks != null) {
            return mLauncherCallbacks.hasSettings();
        } else {
            // On devices with a locked orientation, we will at least have the allow rotation
            // setting.
            return !getResources().getBoolean(R.bool.allow_rotation);
        }*/
        return false;
        //lijun modify end
    }

    public void addToCustomContentPage(View customContent,
            CustomContentCallbacks callbacks, String description) {
        mWorkspace.addToCustomContentPage(customContent, callbacks, description);
    }

    // The custom content needs to offset its content to account for the QSB
    public int getTopOffsetForCustomContent() {
        return mWorkspace.getPaddingTop();
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        // Flag the loader to stop early before switching
        if (mModel.isCurrentCallbacks(this)) {
            mModel.stopLoader();
        }
        //TODO(hyunyoungs): stop the widgets loader when there is a rotation.

        return Boolean.TRUE;
    }

    // We can't hide the IME if it was forced open.  So don't bother
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        mHasFocus = hasFocus;

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onWindowFocusChanged(hasFocus);
        }
        if (isFullScreenMode()) {
            fullscreenOrNot(true);
        } else {
            fullscreenOrNot(false);
        }
    }

    private boolean acceptFilter() {
        final InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        return !inputManager.isFullscreenMode();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        final int uniChar = event.getUnicodeChar();
        final boolean handled = super.onKeyDown(keyCode, event);
        final boolean isKeyNotWhitespace = uniChar > 0 && !Character.isWhitespace(uniChar);
        if (!handled && acceptFilter() && isKeyNotWhitespace) {
            boolean gotKey = TextKeyListener.getInstance().onKeyDown(mWorkspace, mDefaultKeySsb,
                    keyCode, event);
            if (gotKey && mDefaultKeySsb != null && mDefaultKeySsb.length() > 0) {
                // something usable has been typed - start a search
                // the typed text will be retrieved and cleared by
                // showSearchDialog()
                // If there are multiple keystrokes before the search dialog takes focus,
                // onSearchRequested() will be called for every keystroke,
                // but it is idempotent, so it's fine.
                return onSearchRequested();
            }
        }

        // Eat the long press event so the keyboard doesn't come up.
        if (keyCode == KeyEvent.KEYCODE_MENU && event.isLongPress()) {
            return true;
        }

        return handled;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            // Ignore the menu key if we are currently dragging or are on the custom content screen
            if (!isOnCustomContent() && !mDragController.isDragging()) {
                // Close any open folders
                closeFolder();

                // Close any shortcuts containers
                closeShortcutsContainer();

                // Stop resizing any widgets
                mWorkspace.exitWidgetResizeMode();

                // Show the overview mode if we are on the workspace
                if (mState == State.WORKSPACE && !mWorkspace.isInOverviewMode() &&
                        !mWorkspace.isSwitchingState() && mWorkspace.getState()!= Workspace.State.OVERVIEW_HIDDEN) {//lijun add '&& mWorkspace.getState()!= Workspace.State.OVERVIEW_HIDDEN' for bug:257
                    mOverviewPanel.requestFocus();
                    showOverviewMode(true, true /* requestButtonFocus */);
                }
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private String getTypedText() {
        return mDefaultKeySsb.toString();
    }

    @Override
    public void clearTypedText() {
        mDefaultKeySsb.clear();
        mDefaultKeySsb.clearSpans();
        Selection.setSelection(mDefaultKeySsb, 0);
    }

    /**
     * Given the integer (ordinal) value of a State enum instance, convert it to a variable of type
     * State
     */
    private static State intToState(int stateOrdinal) {
        State state = State.WORKSPACE;
        final State[] stateValues = State.values();
        for (int i = 0; i < stateValues.length; i++) {
            if (stateValues[i].ordinal() == stateOrdinal) {
                state = stateValues[i];
                break;
            }
        }
        return state;
    }

    /**
     * Restores the previous state, if it exists.
     *
     * @param savedState The previous state.
     */
    private void restoreState(Bundle savedState) {
        if (savedState == null) {
            return;
        }

        State state = intToState(savedState.getInt(RUNTIME_STATE, State.WORKSPACE.ordinal()));
        if (state == State.APPS || state == State.WIDGETS || state == State.WALLPAPER) {//lijun add WALLPAPER
            mOnResumeState = state;
        }

        int currentScreen = savedState.getInt(RUNTIME_STATE_CURRENT_SCREEN,
                PagedView.INVALID_RESTORE_PAGE);
        if (currentScreen != PagedView.INVALID_RESTORE_PAGE) {
            mWorkspace.setRestorePage(currentScreen);
        }

        PendingRequestArgs requestArgs = savedState.getParcelable(RUNTIME_STATE_PENDING_REQUEST_ARGS);
        if (requestArgs != null) {
            setWaitingForResult(requestArgs);
        }

        mPendingActivityResult = savedState.getParcelable(RUNTIME_STATE_PENDING_ACTIVITY_RESULT);
    }

    /**
     * Finds all the views we need and configure them properly.
     */
    private void setupViews() {
        mLauncherView = findViewById(R.id.launcher);
        mDragLayer = (DragLayer) findViewById(R.id.drag_layer);
        mFocusHandler = mDragLayer.getFocusIndicatorHelper();
        mWorkspace = (Workspace) mDragLayer.findViewById(R.id.workspace);
        if(FeatureFlags.LAUNCHER3_ENABLE_QUICKSEARCHBAR){
            mQsbContainer = mDragLayer.findViewById(mDeviceProfile.isVerticalBarLayout()
                    ? R.id.workspace_blocked_row : R.id.qsb_container);
        }else{
            mQsbContainer = mDragLayer.findViewById(mDeviceProfile.isVerticalBarLayout()
                    ? R.id.workspace_blocked_row : R.id.qsb_container);
            mDragLayer.removeView(mQsbContainer);
        }

        mWorkspace.initParentViews(mDragLayer);

        mLauncherView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        mThemeChangedLoadingView = (ThemeChangedLoadingView) findViewById(R.id.theme_changed_view);//lijun add for theme loading

        // Setup the drag layer
        mDragLayer.setup(this, mDragController, mAllAppsController);

        // Setup the hotseat
        mHotseat = (Hotseat) findViewById(R.id.hotseat);
        if (mHotseat != null) {
            mHotseat.setOnLongClickListener(this);
        }

        // Setup the overview panel
        setupOverviewPanel();

        // Setup the workspace
        mWorkspace.setHapticFeedbackEnabled(false);
        mWorkspace.setOnLongClickListener(this);
        mWorkspace.setup(mDragController);
        // Until the workspace is bound, ensure that we keep the wallpaper offset locked to the
        // default state, otherwise we will update to the wrong offsets in RTL
        mWorkspace.lockWallpaperToDefaultPage();
        mWorkspace.bindAndInitFirstWorkspaceScreen(null /* recycled qsb */);
        mDragController.addDragListener(mWorkspace);
        if(FeatureFlags.LAUNCHER3_LEGACY_DELETEDROPTARGET){
            // Get the search/delete/uninstall bar
            mDropTargetBar = (DropTargetBar) mDragLayer.findViewById(R.id.drop_target_bar);
        }else{
            mDropTargetBar = (DropTargetBar) mDragLayer.findViewById(R.id.drop_target_bar);
            mDragLayer.removeView(mDropTargetBar);
        }

        // Get the search/delete/uninstall bar
        mDropTargetBar = (DropTargetBar) mDragLayer.findViewById(R.id.drop_target_bar);

        // Setup Apps and Widgets
        mAppsView = (AllAppsContainerView) findViewById(R.id.apps_view);
        //lijun modify for WIDGETS_CONTAINER_PAGE
        BaseWidgetsContainerView tempWidgetsPagedView = (WidgetsContainerPagedView) findViewById(R.id.widgets_paged_view);;
        BaseWidgetsContainerView tempWidgetsdView = (WidgetsContainerView) findViewById(R.id.widgets_view);;
        if(FeatureFlags.WIDGETS_CONTAINER_PAGE) {
            mWidgetsView = tempWidgetsPagedView;
            tempWidgetsdView.setVisibility(View.GONE);
        }else {
            mWidgetsView = tempWidgetsdView;
            tempWidgetsPagedView.setVisibility(View.GONE);
        }
        //lijun modify end
        if (mLauncherCallbacks != null && mLauncherCallbacks.getAllAppsSearchBarController() != null) {
            mAppsView.setSearchBarController(mLauncherCallbacks.getAllAppsSearchBarController());
        } else {
            mAppsView.setSearchBarController(new DefaultAppSearchController());
        }

        // Setup the drag controller (drop targets have to be added in reverse order in priority)
        mDragController.setDragScoller(mWorkspace);
        mDragController.setScrollView(mDragLayer);
        mDragController.setMoveTarget(mWorkspace);
        mDragController.addDropTarget(mWorkspace);
        if(FeatureFlags.LAUNCHER3_LEGACY_DELETEDROPTARGET) {
            mDropTargetBar.setup(mDragController);
        }

        if (FeatureFlags.LAUNCHER3_ALL_APPS_PULL_UP) {
            mAllAppsController.setupViews(mAppsView, mHotseat, mWorkspace);
        }

        if (TestingUtils.MEMORY_DUMP_ENABLED) {
            TestingUtils.addWeightWatcher(this);
        }

        //lijun add for pageIndicator
        mPageIndicatorDiagital = (PageIndicatorDiagitalImagview) findViewById(R.id.page_indicator_digital);
        if(FeatureFlags.SHOW_PAGEINDICATOR_CUBE && mPageIndicatorDiagital != null){
            mPageIndicatorDiagital.setVisibility(View.GONE);
        }
        //lijun add end


        //liuzuo add for addIcon
        mFolderImportContainer = (LinearLayout) findViewById(R.id.folder_importmode_button_container);
        mFolderImportButton=(Button)findViewById(R.id.folder_importmode_button);
        mFolderImportButton.setOnClickListener(this);
        mFolderImportButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    v.getBackground().setAlpha(127);
                }else if(event.getAction() == MotionEvent.ACTION_UP){
                    v.getBackground().setAlpha(226);
                }else if(event.getAction() == MotionEvent.ACTION_CANCEL){
                    v.getBackground().setAlpha(226);
                }
                return false;

            }
        });
        //liuzuo end
        //liuzuo add the background of workspace when opening folder begin
        mWorkspaceBg = (ImageView) findViewById(R.id.img_workspace_bg);
         setWallPaperBlur();
        //liuzuo end

        //lijun add for wallpaper
        mWallpaperPicker = (WallpaperPagedViewContainer) findViewById(R.id.wallpaper_picker);
        mVulvanClearBuuton =findViewById(R.id.valcants_clear_button);
        mVulvanClearBuuton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickVacantsClearButton(v);
            }
        });

        //lijun add for alinebutton
        mAlineButton = findViewById(R.id.aline);
        mAlineButton.setAlpha(0.0f);
        mAlineButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmVacantsClear();
            }
        });
        //Icon Arrange begin
        mNavigationbar = (ArrangeNavigationBar) findViewById(R.id.navigationbar);
        //Icon Arrange begin

     // cyl add for special effect start
        mPreviewContainer = (PreviewContainer)findViewById(R.id.specialeffect_container);      
	 // cyl add for special effect end
    }

    private void setupOverviewPanel() {
        mOverviewPanel = (ViewGroup) findViewById(R.id.overview_panel);

        // Long-clicking buttons in the overview panel does the same thing as clicking them.
        OnLongClickListener performClickOnLongClick = new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return v.performClick();
            }
        };

        // Bind wallpaper button actions
        mWallpaperButton = findViewById(R.id.wallpaper_button);
        mWallpaperButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mWorkspace.rejectClickOnMenuButton()) {
                    //lijun modify for wallpaper
//                    onClickWallpaperPicker(view);
                    onClickWallpaperPickerNew(view);
                    //lijun modify end
                }
            }
        });
        mWallpaperButton.setOnLongClickListener(performClickOnLongClick);
        mWallpaperButton.setOnTouchListener(getHapticFeedbackTouchListener());

        // Bind widget button actions
        mWidgetsButton = findViewById(R.id.widget_button);
        mWidgetsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mWorkspace.rejectClickOnMenuButton()) {
                    onClickAddWidgetButton(view);
                }
            }
        });
        mWidgetsButton.setOnLongClickListener(performClickOnLongClick);
        mWidgetsButton.setOnTouchListener(getHapticFeedbackTouchListener());

        // Bind settings actions
        View settingsButton = findViewById(R.id.settings_button);
		/* // cyl add for special effect  
        boolean hasSettings = hasSettings();
        if (hasSettings) { */
            settingsButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!mWorkspace.rejectClickOnMenuButton()) {
					   // cyl modify for special effect start
						showSpecialEffectPreview(true); 
                        //onClickSettingsButton(view);
                       // cyl modify for special effect end
                    }
                }
            });
            settingsButton.setOnLongClickListener(performClickOnLongClick);
            settingsButton.setOnTouchListener(getHapticFeedbackTouchListener());
		/* 	// cyl add for special effect  
        } else {
            settingsButton.setVisibility(View.GONE);
        } */

        mOverviewPanel.setAlpha(0f);
    }

    /**
     * Sets the all apps button. This method is called from {@link Hotseat}.
     * TODO: Get rid of this.
     */
    public void setAllAppsButton(View allAppsButton) {
        mAllAppsButton = allAppsButton;
    }

    public View getStartViewForAllAppsRevealAnimation() {
        return FeatureFlags.NO_ALL_APPS_ICON ? mWorkspace.getPageIndicator() : mAllAppsButton;
    }

    public View getWidgetsButton() {
        return mWidgetsButton;
    }

    /**
     * Creates a view representing a shortcut.
     *
     * @param info The data structure describing the shortcut.
     */
    View createShortcut(ShortcutInfo info) {
        return createShortcut((ViewGroup) mWorkspace.getChildAt(mWorkspace.getCurrentPage()), info);
    }

    /**
     * Creates a view representing a shortcut inflated from the specified resource.
     *
     * @param parent The group the shortcut belongs to.
     * @param info The data structure describing the shortcut.
     *
     * @return A View inflated from layoutResId.
     */
    public View createShortcut(ViewGroup parent, ShortcutInfo info) {
        BubbleTextView favorite = (BubbleTextView) getLayoutInflater().inflate(R.layout.app_icon,
                parent, false);
        favorite.applyFromShortcutInfo(info, mIconCache);
        favorite.setCompoundDrawablePadding(mDeviceProfile.iconDrawablePaddingPx);
        favorite.setOnClickListener(this);
        favorite.setOnFocusChangeListener(mFocusHandler);
        //lijun add for ColorManager
        favorite.setTextColor(ColorManager.getInstance().getColors()[0]);
        //lijun add end
        return favorite;
    }

    /**
     * Add a shortcut to the workspace.
     *
     * @param data The intent describing the shortcut.
     */
    private void completeAddShortcut(Intent data, long container, long screenId, int cellX,
            int cellY, PendingRequestArgs args) {
        int[] cellXY = mTmpAddItemCellCoordinates;
        CellLayout layout = getCellLayout(container, screenId);

        ShortcutInfo info = InstallShortcutReceiver.fromShortcutIntent(this, data);
        if (info == null || args.getRequestCode() != REQUEST_CREATE_SHORTCUT ||
                args.getPendingIntent().getComponent() == null) {
            return;
        }
        if (!PackageManagerHelper.hasPermissionForActivity(
                this, info.intent, args.getPendingIntent().getComponent().getPackageName())) {
            // The app is trying to add a shortcut without sufficient permissions
            Log.e(TAG, "Ignoring malicious intent " + info.intent.toUri(0));
            return;
        }
        final View view = createShortcut(info);

        boolean foundCellSpan = false;
        // First we check if we already know the exact location where we want to add this item.
        if (cellX >= 0 && cellY >= 0) {
            cellXY[0] = cellX;
            cellXY[1] = cellY;
            foundCellSpan = true;

            // If appropriate, either create a folder or add to an existing folder
            if (mWorkspace.createUserFolderIfNecessary(view, container, layout, cellXY, 0,
                    true, null,null)) {
                return;
            }
            DragObject dragObject = new DragObject();
            dragObject.dragInfo = info;
            if (mWorkspace.addToExistingFolderIfNecessary(view, layout, cellXY, 0, dragObject,
                    true)) {
                return;
            }
        } else {
            foundCellSpan = layout.findCellForSpan(cellXY, 1, 1);
        }

        if (!foundCellSpan) {
            showOutOfSpaceMessage(isHotseatLayout(layout));
            return;
        }

        LauncherModel.addItemToDatabase(this, info, container, screenId, cellXY[0], cellXY[1]);

        mWorkspace.addInScreen(view, container, screenId, cellXY[0], cellXY[1], 1, 1,
                isWorkspaceLocked());
    }

    /**
     * Add a widget to the workspace.
     *
     * @param appWidgetId The app widget id
     */
    @Thunk void completeAddAppWidget(int appWidgetId, ItemInfo itemInfo,
            AppWidgetHostView hostView, LauncherAppWidgetProviderInfo appWidgetInfo) {

        if (appWidgetInfo == null) {
            appWidgetInfo = mAppWidgetManager.getLauncherAppWidgetInfo(appWidgetId);
        }

        if (appWidgetInfo.isCustomWidget) {
            appWidgetId = LauncherAppWidgetInfo.CUSTOM_WIDGET_ID;
        }

        LauncherAppWidgetInfo launcherInfo;
        launcherInfo = new LauncherAppWidgetInfo(appWidgetId, appWidgetInfo.provider);
        launcherInfo.spanX = itemInfo.spanX;
        launcherInfo.spanY = itemInfo.spanY;
        launcherInfo.minSpanX = itemInfo.minSpanX;
        launcherInfo.minSpanY = itemInfo.minSpanY;
        launcherInfo.user = mAppWidgetManager.getUser(appWidgetInfo);

        LauncherModel.addItemToDatabase(this, launcherInfo,
                itemInfo.container, itemInfo.screenId, itemInfo.cellX, itemInfo.cellY);

        if (hostView == null) {
            // Perform actual inflation because we're live
            hostView = mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo);
        }
        hostView.setVisibility(View.VISIBLE);
        addAppWidgetToWorkspace(hostView, launcherInfo, appWidgetInfo, isWorkspaceLocked());
    }

    private void addAppWidgetToWorkspace(
            AppWidgetHostView hostView, LauncherAppWidgetInfo item,
            LauncherAppWidgetProviderInfo appWidgetInfo, boolean insert) {
        hostView.setTag(item);
        item.onBindAppWidget(this, hostView);

        hostView.setFocusable(true);
        hostView.setOnFocusChangeListener(mFocusHandler);

        mWorkspace.addInScreen(hostView, item.container, item.screenId,
                item.cellX, item.cellY, item.spanX, item.spanY, insert);

        if (!item.isCustomWidget()) {
            addWidgetToAutoAdvanceIfNeeded(hostView, appWidgetInfo);
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                mUserPresent = false;
                mDragLayer.clearAllResizeFrames();
                updateAutoAdvanceState();
                //updateDynamicStatus(false);
                // Reset AllApps to its initial state only if we are not in the middle of
                // processing a multi-step drop
                //lijun remove
                /*if (mAppsView != null && mWidgetsView != null && mPendingRequestArgs == null) {
                    if (!showWorkspace(false)) {
                        // If we are already on the workspace, then manually reset all apps
                        mAppsView.reset();
                    }
                }*/
                mIsResumeFromActionScreenOff = true;
            } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                mUserPresent = true;
                updateAutoAdvanceState();
            }else if(Intent.ACTION_SCREEN_ON.equals(action)){
                //updateDynamicStatus(true);
            }
        }
    };

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        // Listen for broadcasts related to user-presence
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);//liuzuo add
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(mReceiver, filter);
        FirstFrameAnimatorHelper.initializeDrawListener(getWindow().getDecorView());
        mAttached = true;
        mVisible = true;

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onAttachedToWindow();
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mVisible = false;

        if (mAttached) {
            unregisterReceiver(mReceiver);
            mAttached = false;
        }
        updateAutoAdvanceState();

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onDetachedFromWindow();
        }
    }

    public void onWindowVisibilityChanged(int visibility) {
        mVisible = visibility == View.VISIBLE;
        updateAutoAdvanceState();
        // The following code used to be in onResume, but it turns out onResume is called when
        // you're in All Apps and click home to go to the workspace. onWindowVisibilityChanged
        // is a more appropriate event to handle
        if (mVisible) {
            if (!mWorkspaceLoading) {
                final ViewTreeObserver observer = mWorkspace.getViewTreeObserver();
                // We want to let Launcher draw itself at least once before we force it to build
                // layers on all the workspace pages, so that transitioning to Launcher from other
                // apps is nice and speedy.
                observer.addOnDrawListener(new ViewTreeObserver.OnDrawListener() {
                    private boolean mStarted = false;
                    public void onDraw() {
                        if (mStarted) return;
                        mStarted = true;
                        // We delay the layer building a bit in order to give
                        // other message processing a time to run.  In particular
                        // this avoids a delay in hiding the IME if it was
                        // currently shown, because doing that may involve
                        // some communication back with the app.
                        mWorkspace.postDelayed(mBuildLayersRunnable, 500);
                        final ViewTreeObserver.OnDrawListener listener = this;
                        mWorkspace.post(new Runnable() {
                            public void run() {
                                if (mWorkspace != null &&
                                        mWorkspace.getViewTreeObserver() != null) {
                                    mWorkspace.getViewTreeObserver().
                                            removeOnDrawListener(listener);
                                }
                            }
                        });
                        return;
                    }
                });
            }
            clearTypedText();
        }
    }

    @Thunk void sendAdvanceMessage(long delay) {
        mHandler.removeMessages(ADVANCE_MSG);
        Message msg = mHandler.obtainMessage(ADVANCE_MSG);
        mHandler.sendMessageDelayed(msg, delay);
        mAutoAdvanceSentTime = System.currentTimeMillis();
    }

    @Thunk void updateAutoAdvanceState() {
        boolean autoAdvanceRunning = mVisible && mUserPresent && !mWidgetsToAdvance.isEmpty();
        if (autoAdvanceRunning != mAutoAdvanceRunning) {
            mAutoAdvanceRunning = autoAdvanceRunning;
            if (autoAdvanceRunning) {
                long delay = mAutoAdvanceTimeLeft == -1 ? ADVANCE_INTERVAL : mAutoAdvanceTimeLeft;
                sendAdvanceMessage(delay);
            } else {
                if (!mWidgetsToAdvance.isEmpty()) {
                    mAutoAdvanceTimeLeft = Math.max(0, ADVANCE_INTERVAL -
                            (System.currentTimeMillis() - mAutoAdvanceSentTime));
                }
                mHandler.removeMessages(ADVANCE_MSG);
                mHandler.removeMessages(0); // Remove messages sent using postDelayed()
            }
        }
    }

    @Thunk final Handler mHandler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == ADVANCE_MSG) {
                int i = 0;
                for (View key: mWidgetsToAdvance.keySet()) {
                    final View v = key.findViewById(mWidgetsToAdvance.get(key).autoAdvanceViewId);
                    final int delay = ADVANCE_STAGGER * i;
                    if (v instanceof Advanceable) {
                        mHandler.postDelayed(new Runnable() {
                           public void run() {
                               ((Advanceable) v).advance();
                           }
                       }, delay);
                    }
                    i++;
                }
                sendAdvanceMessage(ADVANCE_INTERVAL);
            }
            //lijun add for fullscreen
            else if(msg.what == FULLSCREEN_DELAY_MSG){
                if(isFullScreenMode()) {
                    fullscreenOrNot(true);
                }
            }
            //lijun add end
            //lijun add for fullscreen
            else if(msg.what == THEME_CHANGED_DELAY_MSG){
                hideThemeChangingDialog();
            }
            //lijun add end
            return true;
        }
    });

    private void addWidgetToAutoAdvanceIfNeeded(View hostView, AppWidgetProviderInfo appWidgetInfo) {
        if (appWidgetInfo == null || appWidgetInfo.autoAdvanceViewId == -1) return;
        View v = hostView.findViewById(appWidgetInfo.autoAdvanceViewId);
        if (v instanceof Advanceable) {
            mWidgetsToAdvance.put(hostView, appWidgetInfo);
            ((Advanceable) v).fyiWillBeAdvancedByHostKThx();
            updateAutoAdvanceState();
        }
    }

    private void removeWidgetToAutoAdvance(View hostView) {
        if (mWidgetsToAdvance.containsKey(hostView)) {
            mWidgetsToAdvance.remove(hostView);
            updateAutoAdvanceState();
        }
    }

    public void showOutOfSpaceMessage(boolean isHotseatLayout) {
        int strId = (isHotseatLayout ? R.string.hotseat_out_of_space : R.string.out_of_space);
        //lijun modify start
//        Toast.makeText(this, getString(strId), Toast.LENGTH_SHORT).show();
        showToast(strId);
        //lijun modify end
    }

    public DragLayer getDragLayer() {
        return mDragLayer;
    }

    public AllAppsContainerView getAppsView() {
        return mAppsView;
    }

    public BaseWidgetsContainerView getWidgetsView() {//lijun modify WidgetsContainerView to BaseWidgetsContainerView
        return mWidgetsView;
    }

    public Workspace getWorkspace() {
        return mWorkspace;
    }

    public View getQsbContainer() {
        return mQsbContainer;
    }

    public Hotseat getHotseat() {
        return mHotseat;
    }

    public ViewGroup getOverviewPanel() {
        return mOverviewPanel;
    }

    /**
     * lijun add for WIDGETS_CONTAINER_PAGE
     */
    public View getWidgetsPanel() {
        return mWidgetsView;
    }

    /**
     * lijun add for wallpaper
     */
    public WallpaperPagedViewContainer getmWallpaperPicker() {
        return mWallpaperPicker;
    }

    /**
     * lijun add for wallpaper
     */
    public View getmWallpaperButton() {
        return mWallpaperButton;
    }

    public DropTargetBar getDropTargetBar() {
        return mDropTargetBar;
    }

    public LauncherAppWidgetHost getAppWidgetHost() {
        return mAppWidgetHost;
    }

    public LauncherModel getModel() {
        return mModel;
    }

    public SharedPreferences getSharedPrefs() {
        return mSharedPrefs;
    }

    public DeviceProfile getDeviceProfile() {
        return mDeviceProfile;
    }

    public void closeSystemDialogs() {
        getWindow().closeAllPanels();

        // Whatever we were doing is hereby canceled.
        setWaitingForResult(null);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        long startTime = 0;
        if (DEBUG_RESUME_TIME) {
            startTime = System.currentTimeMillis();
        }

        //add by lijun start for WIDGETS_CONTAINER_PAGE
        if (FeatureFlags.WIDGETS_CONTAINER_PAGE && (mWorkspace.isSwitchingState() || mStateTransitionAnimation.beingAnimaBetweenOverViewAndWidgets)) {
            return;
        }
        //and by lijun end

        //lijun add for theme loading start
        if(isShowThemeChang()) {
            mOnResumeState = State.WORKSPACE;
            return;
        }
        //lijun add for theme loading end

        //lijun add for wallpaper
        if (isWallpaperMode()) {
            mWallpaperPicker.setVisibility(View.GONE);
        }

        super.onNewIntent(intent);

        boolean alreadyOnHome = mHasFocus && ((intent.getFlags() &
                Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
                != Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);

        // Check this condition before handling isActionMain, as this will get reset.
        boolean shouldMoveToDefaultScreen = alreadyOnHome &&
                mState == State.WORKSPACE && getTopFloatingView() == null &&
                mWorkspace.getState() != Workspace.State.OVERVIEW && mWorkspace.getState() != Workspace.State.OVERVIEW_HIDDEN;

        boolean isActionMain = Intent.ACTION_MAIN.equals(intent.getAction());
        if (isActionMain) {
            // also will cancel mWaitingForResult.
            closeSystemDialogs();

            if (mWorkspace == null) {
                // Can be cases where mWorkspace is null, this prevents a NPE
                return;
            }
            // In all these cases, only animate if we're already on home
            mWorkspace.exitWidgetResizeMode();
            //liuzuo add for addIcon begin
            if(isLauncherArrangeMode()){
                Folder openFolder = mWorkspace.getOpenFolder();
                if(openFolder!=null) {
                    if (openFolder.isEditingName()) {
                        openFolder.dismissEditingName();
                    } else {
                        closeFolder();
                    }
                }else{
                    getArrangeNavigationBar().onBackPressed();
                    showOverviewMode(true);
                    mNavigationbar.setVisibility(View.GONE);
                    getDragController().removeDropTarget(mNavigationbar);//liuzuo add
                }
                return;
            }else if(mEditFolderIcon!=null&&getImportMode()) {
                forceExitImportMode();
                return;
            }else {
                closeFolder();
            }
            //liuzuo add for addIcon end
            closeFolder(alreadyOnHome);
            //liuzuo add for  close worjspacebg unexpected begin
            if(mWorkspaceBg!=null&&mAniWorkspaceBg==null&&mWorkspaceBg.getVisibility()==View.VISIBLE){
                openOrCloseFolderAnimation(false);
            }
            //liuzuo add for  close worjspacebg unexpected end
            closeShortcutsContainer(alreadyOnHome);
            exitSpringLoadedDragMode();

            //lijun add start
            if(isUnInstallMode()){
                exitUnInstallNormalMode();
                return;
            }
            //lijun add end

            // cyl add for special effect start
			if(mState == State.SPECIALEFFECT){
			  dismissSpecialEffectPreview();
			}
		   // cyl add for special effect end
		   
            // If we are already on home, then just animate back to the workspace,
            // otherwise, just wait until onResume to set the state back to Workspace
            if (alreadyOnHome) {
                showWorkspace(true);
            } else {
                mOnResumeState = State.WORKSPACE;
            }

            final View v = getWindow().peekDecorView();
            if (v != null && v.getWindowToken() != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(
                        INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }

            // Reset the apps view
            if (!alreadyOnHome && mAppsView != null) {
                mAppsView.scrollToTop();
            }

            // Reset the widgets view
            if (!alreadyOnHome && mWidgetsView != null) {
                mWidgetsView.scrollToTop();
            }

            if (mLauncherCallbacks != null) {
                mLauncherCallbacks.onHomeIntent();
            }
        }

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onNewIntent(intent);
        }

        // Defer moving to the default screen until after we callback to the LauncherCallbacks
        // as slow logic in the callbacks eat into the time the scroller expects for the snapToPage
        // animation.
        if (isActionMain) {
            boolean callbackAllowsMoveToDefaultScreen = mLauncherCallbacks != null ?
                    mLauncherCallbacks.shouldMoveToDefaultScreenOnHomeIntent() : true;
            if (shouldMoveToDefaultScreen && !mWorkspace.isTouchActive()
                    && callbackAllowsMoveToDefaultScreen) {

                // We use this flag to suppress noisy callbacks above custom content state
                // from onResume.
                mMoveToDefaultScreenFromNewIntent = true;
                mWorkspace.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mWorkspace != null) {
                            mWorkspace.moveToDefaultScreen(true);
                        }
                    }
                });
            }
        }

        if (DEBUG_RESUME_TIME) {
            Log.d(TAG, "Time spent in onNewIntent: " + (System.currentTimeMillis() - startTime));
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        for (int page: mSynchronouslyBoundPages) {
            mWorkspace.restoreInstanceStateForChild(page);
        }
        //lijun add for wallpaper start
        if (isWallpaperMode()) {
            mWallpaperPicker.onRestoreInstanceState(state);
        }
        //lijun add for wallpaper end
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mWorkspace.getChildCount() > 0) {
            outState.putInt(RUNTIME_STATE_CURRENT_SCREEN,
                    mWorkspace.getCurrentPageOffsetFromCustomContent());

        }
        super.onSaveInstanceState(outState);

        outState.putInt(RUNTIME_STATE, mState.ordinal());
        // We close any open folder since it will not be re-opened, and we need to make sure
        // this state is reflected.
        // TODO: Move folderInfo.isOpened out of the model and make it a UI state.
        //UnistallMode add by liuzuo begin
        if(!comfirmingUninstall&&isUninstallMode&&mWorkspace!=null){
//            mWorkspace.exitUninstallMode();
            exitUnInstallNormalMode();
        }
        if(!comfirmingUninstall) {
            closeFolder(false);
        }else {
            comfirmingUninstall=false;
        }
         //UnistallMode add by liuzuo end
        closeShortcutsContainer(false);

        if (mPendingRequestArgs != null) {
            outState.putParcelable(RUNTIME_STATE_PENDING_REQUEST_ARGS, mPendingRequestArgs);
        }
        if (mPendingActivityResult != null) {
            outState.putParcelable(RUNTIME_STATE_PENDING_ACTIVITY_RESULT, mPendingActivityResult);
        }

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onSaveInstanceState(outState);
        }
        //lijun add for wallpaper start
        if (isWallpaperMode()) {
            mWallpaperPicker.onSaveInstanceState(outState);
        }
        //lijun add for wallpaper end
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //lijun add for themechanged start
        hideThemeChangingDialog();
        mModel.reloadForThemechanged = false;
        //lijun add for themechanged end

        // Remove all pending runnables
        mHandler.removeMessages(ADVANCE_MSG);
        mHandler.removeMessages(0);
        mWorkspace.removeCallbacks(mBuildLayersRunnable);
        mWorkspace.removeFolderListeners();

        // Stop callbacks from LauncherModel
        // It's possible to receive onDestroy after a new Launcher activity has
        // been created. In this case, don't interfere with the new Launcher.
        if (mModel.isCurrentCallbacks(this)) {
            mModel.stopLoader();
            LauncherAppState.getInstance().setLauncher(null);
        }

        if (mRotationPrefChangeHandler != null) {
            mSharedPrefs.unregisterOnSharedPreferenceChangeListener(mRotationPrefChangeHandler);
        }

        try {
            mAppWidgetHost.stopListening();
        } catch (NullPointerException ex) {
            Log.w(TAG, "problem while stopping AppWidgetHost during Launcher destruction", ex);
        }
        mAppWidgetHost = null;

        mWidgetsToAdvance.clear();

        TextKeyListener.getInstance().release();

        ((AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE))
                .removeAccessibilityStateChangeListener(this);

        unregisterReceiver(mUiBroadcastReceiver);

        LauncherAnimUtils.onDestroyActivity();

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onDestroy();
        }
        LauncherAppState app = (LauncherAppState.getInstance());
        ColorManager.getInstance().removeWallpaperCallback(this);//lijun add for wallpaper change
        //liuzuo add start
        ArrayList<IDynamicIcon> iDynamicIcons = DynamicProvider.getInstance(this).getAllDynamicIcon();
        for (IDynamicIcon icon:iDynamicIcons){
             icon.clearDynamicIcon();
            }
        //liuzuo add end
    }

    public LauncherAccessibilityDelegate getAccessibilityDelegate() {
        return mAccessibilityDelegate;
    }

    public DragController getDragController() {
        return mDragController;
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
        super.startActivityForResult(intent, requestCode, options);
    }

    @Override
    public void startIntentSenderForResult (IntentSender intent, int requestCode,
            Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags, Bundle options) {
        try {
            super.startIntentSenderForResult(intent, requestCode,
                fillInIntent, flagsMask, flagsValues, extraFlags, options);
        } catch (IntentSender.SendIntentException e) {
            throw new ActivityNotFoundException();
        }
    }

    /**
     * Indicates that we want global search for this activity by setting the globalSearch
     * argument for {@link #startSearch} to true.
     */
    @Override
    public void startSearch(String initialQuery, boolean selectInitialQuery,
            Bundle appSearchData, boolean globalSearch) {

        if (initialQuery == null) {
            // Use any text typed in the launcher as the initial query
            initialQuery = getTypedText();
        }
        if (appSearchData == null) {
            appSearchData = new Bundle();
            appSearchData.putString("source", "launcher-search");
        }

        if (mLauncherCallbacks == null ||
                !mLauncherCallbacks.startSearch(initialQuery, selectInitialQuery, appSearchData)) {
            // Starting search from the callbacks failed. Start the default global search.
            startGlobalSearch(initialQuery, selectInitialQuery, appSearchData, null);
        }

        // We need to show the workspace after starting the search
        showWorkspace(true);
    }

    /**
     * Starts the global search activity. This code is a copied from SearchManager
     */
    public void startGlobalSearch(String initialQuery,
            boolean selectInitialQuery, Bundle appSearchData, Rect sourceBounds) {
        final SearchManager searchManager =
            (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        ComponentName globalSearchActivity = searchManager.getGlobalSearchActivity();
        if (globalSearchActivity == null) {
            Log.w(TAG, "No global search activity found.");
            return;
        }
        Intent intent = new Intent(SearchManager.INTENT_ACTION_GLOBAL_SEARCH);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setComponent(globalSearchActivity);
        // Make sure that we have a Bundle to put source in
        if (appSearchData == null) {
            appSearchData = new Bundle();
        } else {
            appSearchData = new Bundle(appSearchData);
        }
        // Set source to package name of app that starts global search if not set already.
        if (!appSearchData.containsKey("source")) {
            appSearchData.putString("source", getPackageName());
        }
        intent.putExtra(SearchManager.APP_DATA, appSearchData);
        if (!TextUtils.isEmpty(initialQuery)) {
            intent.putExtra(SearchManager.QUERY, initialQuery);
        }
        if (selectInitialQuery) {
            intent.putExtra(SearchManager.EXTRA_SELECT_QUERY, selectInitialQuery);
        }
        intent.setSourceBounds(sourceBounds);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Log.e(TAG, "Global search activity not found: " + globalSearchActivity);
        }
    }

    public boolean isOnCustomContent() {
        return mWorkspace.isOnOrMovingToCustomContent();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (mLauncherCallbacks != null) {
            return mLauncherCallbacks.onPrepareOptionsMenu(menu);
        }
        return false;
    }

    @Override
    public boolean onSearchRequested() {
        startSearch(null, false, null, true);
        // Use a custom animation for launching search
        return true;
    }

    public boolean isWorkspaceLocked() {
        return mWorkspaceLoading || mPendingRequestArgs != null;
    }

    public boolean isWorkspaceLoading() {
        return mWorkspaceLoading;
    }

    private void setWorkspaceLoading(boolean value) {
        boolean isLocked = isWorkspaceLocked();
        mWorkspaceLoading = value;
        if (isLocked != isWorkspaceLocked()) {
            onWorkspaceLockedChanged();
        }
    }

    private void setWaitingForResult(PendingRequestArgs args) {
        boolean isLocked = isWorkspaceLocked();
        mPendingRequestArgs = args;
        if (isLocked != isWorkspaceLocked()) {
            onWorkspaceLockedChanged();
        }
    }

    protected void onWorkspaceLockedChanged() {
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onWorkspaceLockedChanged();
        }
    }

    void addAppWidgetFromDropImpl(int appWidgetId, ItemInfo info, AppWidgetHostView boundWidget,
            LauncherAppWidgetProviderInfo appWidgetInfo) {
        if (LOGD) {
            Log.d(TAG, "Adding widget from drop");
        }
        addAppWidgetImpl(appWidgetId, info, boundWidget, appWidgetInfo, 0);
    }

    void addAppWidgetImpl(int appWidgetId, ItemInfo info,
            AppWidgetHostView boundWidget, LauncherAppWidgetProviderInfo appWidgetInfo,
            int delay) {
        if (appWidgetInfo.configure != null) {
            setWaitingForResult(PendingRequestArgs.forWidgetInfo(appWidgetId, appWidgetInfo, info));

            // Launch over to configure widget, if needed
            mAppWidgetManager.startConfigActivity(appWidgetInfo, appWidgetId, this,
                    mAppWidgetHost, REQUEST_CREATE_APPWIDGET);
        } else {
            // Otherwise just add it
            Runnable onComplete = new Runnable() {
                @Override
                public void run() {
                    // Exit spring loaded mode if necessary after adding the widget
                    exitSpringLoadedDragModeDelayed(true, EXIT_SPRINGLOADED_MODE_SHORT_TIMEOUT,
                            null);
                }
            };
            completeAddAppWidget(appWidgetId, info, boundWidget, appWidgetInfo);
            mWorkspace.removeExtraEmptyScreenDelayed(true, onComplete, delay, false);
        }
    }

    protected void moveToCustomContentScreen(boolean animate) {
        // Close any folders that may be open.
        closeFolder();
        mWorkspace.moveToCustomContentScreen(animate);
    }

    public void addPendingItem(PendingAddItemInfo info, long container, long screenId,
            int[] cell, int spanX, int spanY) {
        info.container = container;
        info.screenId = screenId;
        if (cell != null) {
            info.cellX = cell[0];
            info.cellY = cell[1];
        }
        info.spanX = spanX;
        info.spanY = spanY;

        switch (info.itemType) {
            case LauncherSettings.Favorites.ITEM_TYPE_CUSTOM_APPWIDGET:
            case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                addAppWidgetFromDrop((PendingAddWidgetInfo) info);
                break;
            case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                processShortcutFromDrop(info);
                break;
            default:
                throw new IllegalStateException("Unknown item type: " + info.itemType);
            }
    }

    /**
     * Process a shortcut drop.
     */
    private void processShortcutFromDrop(PendingAddItemInfo info) {
        Intent intent = new Intent(Intent.ACTION_CREATE_SHORTCUT).setComponent(info.componentName);
        setWaitingForResult(PendingRequestArgs.forIntent(REQUEST_CREATE_SHORTCUT, intent, info));
        Utilities.startActivityForResultSafely(this, intent, REQUEST_CREATE_SHORTCUT);
    }

    /**
     * Process a widget drop.
     */
    private void addAppWidgetFromDrop(PendingAddWidgetInfo info) {
        AppWidgetHostView hostView = info.boundWidget;
        int appWidgetId;
        if (hostView != null) {
            // In the case where we've prebound the widget, we remove it from the DragLayer
            if (LOGD) {
                Log.d(TAG, "Removing widget view from drag layer and setting boundWidget to null");
            }
            getDragLayer().removeView(hostView);

            appWidgetId = hostView.getAppWidgetId();
            addAppWidgetFromDropImpl(appWidgetId, info, hostView, info.info);

            // Clear the boundWidget so that it doesn't get destroyed.
            info.boundWidget = null;
        } else {
            // In this case, we either need to start an activity to get permission to bind
            // the widget, or we need to start an activity to configure the widget, or both.
            appWidgetId = getAppWidgetHost().allocateAppWidgetId();
            Bundle options = info.bindOptions;

            boolean success = mAppWidgetManager.bindAppWidgetIdIfAllowed(
                    appWidgetId, info.info, options);
            if (success) {
                addAppWidgetFromDropImpl(appWidgetId, info, null, info.info);
            } else {
                setWaitingForResult(PendingRequestArgs.forWidgetInfo(appWidgetId, info.info, info));
                Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.componentName);
                mAppWidgetManager.getUser(info.info)
                    .addToIntent(intent, AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE);
                // TODO: we need to make sure that this accounts for the options bundle.
                // intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, options);
                startActivityForResult(intent, REQUEST_BIND_APPWIDGET);
            }
        }
    }

    FolderIcon addFolder(CellLayout layout, long container, final long screenId, int cellX,
            int cellY) {
        final FolderInfo folderInfo = new FolderInfo();
        folderInfo.title = getText(R.string.folder_name);

        // Update the model
        LauncherModel.addItemToDatabase(Launcher.this, folderInfo, container, screenId,
                cellX, cellY);

        // Create the view
        FolderIcon newFolder =
            FolderIcon.fromXml(R.layout.folder_icon, this, layout, folderInfo, mIconCache);
	   // cyl add for hotseat icon center start
		if(container == LauncherSettings.Favorites.CONTAINER_HOTSEAT){
			mWorkspace.addInHotseat(newFolder, container, screenId,cellX, cellY, 1, 1, cellX);
		}else{
		// cyl add for hotseat icon center end
            mWorkspace.addInScreen(newFolder, container, screenId, cellX, cellY, 1, 1,
                isWorkspaceLocked());
		}
        newFolder.mFolder.firstCreateAddSharePreferences();//liuzuo add for folder editTitle guide
        // Force measure the new folder icon
        CellLayout parent = mWorkspace.getParentCellLayoutForView(newFolder);
        parent.getShortcutsAndWidgets().measureChild(newFolder);
        return newFolder;
    }

    /**
     * Unbinds the view for the specified item, and removes the item and all its children.
     *
     * @param v the view being removed.
     * @param itemInfo the {@link ItemInfo} for this view.
     * @param deleteFromDb whether or not to delete this item from the db.
     */
    public boolean  removeItem(View v, final ItemInfo itemInfo, boolean deleteFromDb) {
        if (itemInfo instanceof ShortcutInfo) {
            // Remove the shortcut from the folder before removing it from launcher
            View folderIcon = mWorkspace.getHomescreenIconByItemId(itemInfo.container);
            if (folderIcon instanceof FolderIcon) {
                ((FolderInfo) folderIcon.getTag()).remove((ShortcutInfo) itemInfo, true);
            } else {
                mWorkspace.removeWorkspaceItem(v);
            }
            if (deleteFromDb) {
                LauncherModel.deleteItemFromDatabase(this, itemInfo);
            }
        } else if (itemInfo instanceof FolderInfo) {
            final FolderInfo folderInfo = (FolderInfo) itemInfo;
            if (v instanceof FolderIcon) {
                ((FolderIcon) v).removeListeners();
                ((FolderIcon) v).mFolder.cleanSharedPreferences();//liuzuo add for folder editTitle guide
            }
            mWorkspace.removeWorkspaceItem(v);
            if (deleteFromDb) {
                LauncherModel.deleteFolderAndContentsFromDatabase(this, folderInfo);
            }
        } else if (itemInfo instanceof LauncherAppWidgetInfo) {
            final LauncherAppWidgetInfo widgetInfo = (LauncherAppWidgetInfo) itemInfo;
            mWorkspace.removeWorkspaceItem(v);
            removeWidgetToAutoAdvance(v);
            if (deleteFromDb) {
                deleteWidgetInfo(widgetInfo);
            }
        } else {
            return false;
        }
        return true;
    }

    /**
     * Deletes the widget info and the widget id.
     */
    private void deleteWidgetInfo(final LauncherAppWidgetInfo widgetInfo) {
        final LauncherAppWidgetHost appWidgetHost = getAppWidgetHost();
        if (appWidgetHost != null && !widgetInfo.isCustomWidget() && widgetInfo.isWidgetIdAllocated()) {
            // Deleting an app widget ID is a void call but writes to disk before returning
            // to the caller...
            new AsyncTask<Void, Void, Void>() {
                public Void doInBackground(Void ... args) {
                    appWidgetHost.deleteAppWidgetId(widgetInfo.appWidgetId);
                    return null;
                }
            }.executeOnExecutor(Utilities.THREAD_POOL_EXECUTOR);
        }
        LauncherModel.deleteItemFromDatabase(this, widgetInfo);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_HOME:
                    return true;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    if (Utilities.isPropertyEnabled(DUMP_STATE_PROPERTY)) {
                        dumpState();
                        return true;
                    }
                    break;
            }
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_HOME:
                    return true;
            }
        }

        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onBackPressed() {
        if (mLauncherCallbacks != null && mLauncherCallbacks.handleBackPressed()) {
            return;
        }

        //lijun add for theme loading start
        if(isShowThemeChang()) {
            return;
        }
        //lijun add for theme loading end

        if (mDragController.isDragging()) {
            mDragController.cancelDrag();
            //lijun add start
            if(isUnInstallMode()){
                exitUnInstallNormalMode();
                return;
            }
            //lijun add end
            return;
        }

        if (getOpenShortcutsContainer() != null) {
            closeShortcutsContainer();
        } else if (isAppsViewVisible()) {
            showWorkspace(true);
        } else if (isWidgetsViewVisible() || isWallpaperMode()||isLauncherArrangeMode() ||isSpecialEffectMode()) {//lijun add isWallpaperMode()
          // cyl add for special effect :isSpecialEffectMode()
            //Icon Arrange begin
            if(isLauncherArrangeMode()){
                Folder openFolder = mWorkspace.getOpenFolder();
                if(openFolder!=null) {
                    if (openFolder.isEditingName()) {
                        openFolder.dismissEditingName();
                    } else {
                        closeFolder();
                    }
                }else{
                    ((ArrangeNavigationBar)getArrangeNavigationBar()).onBackPressed();

                    showOverviewMode(true);
                    //mNavigationbar.setVisibility(View.GONE);
                }
            }else{
                Folder.ICONARRANGING = false;
                showOverviewMode(true);
            }
            //Icon Arrange begin

        } else if (mWorkspace.isInOverviewMode()) {
            //liuzuo add begin
            if (mWorkspace.getOpenFolder() != null) {
                Folder openFolder = mWorkspace.getOpenFolder();
                if (openFolder.isEditingName()) {
                    openFolder.dismissEditingName();
                } else {
                    closeFolder();
                }
                return;
            }
            //liuzuo add end
            showWorkspace(true);
        } else if (mWorkspace.getOpenFolder() != null) {
            Folder openFolder = mWorkspace.getOpenFolder();
            if (openFolder.isEditingName()) {
                openFolder.dismissEditingName();
            } else {
                closeFolder();
            }
        }else if(mEditFolderIcon!=null&&getImportMode()){
            if(mEditFolderIcon.getFolder()!=null&&mEditFolderIcon.getFolder().isAnimating()){
                return;
            }
            showFolderIcon();
            mOpenFolder =true;
            if(mCheckedBubbleTextViews!=null){
                for (BubbleTextView bv:mCheckedBubbleTextViews
                        ) {
                    bv.setChecked(false);
                }
            }
            mEditFolderIcon.mFolder.setImportMode(false);
            mCheckedBubbleTextViews.clear();
            mCheckedShortcutInfos.clear();
            exitEditModeAndOpenFolder();
            //UnistallMode add by liuzuo begin
        }else if(isUninstallMode) {
            exitUnInstallNormalMode();//lijun add
             //UnistallMode add by liuzuo end
        }else {
            mWorkspace.exitWidgetResizeMode();

            // Back button is a no-op here, but give at least some feedback for the button press
            mWorkspace.showOutlinesTemporarily();
            //liuzuo add for  close worjspacebg unexpected begin
            if(mWorkspaceBg!=null&&mWorkspaceBg.getVisibility()==View.VISIBLE){
                openOrCloseFolderAnimation(false);
            }
            //liuzuo add for  close worjspacebg unexpected end
        }

    }

    /**
     * Launches the intent referred by the clicked shortcut.
     *
     * @param v The view representing the clicked shortcut.
     */
    public void onClick(View v) {
        // Make sure that rogue clicks don't get through while allapps is launching, or after the
        // view has detached (it's possible for this to happen if the view is removed mid touch).
        if (v.getWindowToken() == null) {
            return;
        }

        if (!mWorkspace.isFinishedSwitchingState()) {
            return;
        }

        if (v instanceof Workspace) {
            //lijun add start for WIDGETS_CONTAINER_PAGE
            if(FeatureFlags.WIDGETS_CONTAINER_PAGE && mState == State.WIDGETS){
                showOverviewModeFromOverviewHidenMode(State.WORKSPACE,true);
                return;
            }else if(isWallpaperMode()){
                showOverviewModeFromOverviewHidenMode(State.WORKSPACE,true);
                return;
            }else if(isUnInstallMode()){
                exitUnInstallNormalMode();
                return;
            }
            //lijun add end
            if (mWorkspace.isInOverviewMode()) {
                if(FeatureFlags.WIDGETS_CONTAINER_PAGE){//lijun add for WIDGETS_CONTAINER_PAGE
                    showOverviewModeFromOverviewHidenMode(State.WORKSPACE,true);
                }
                showWorkspace(true);
            }
            return;
        }

        if (v instanceof CellLayout) {
            //lijun add start for WIDGETS_CONTAINER_PAGE
            if(FeatureFlags.WIDGETS_CONTAINER_PAGE && mState == State.WIDGETS){
                showOverviewModeFromOverviewHidenMode(State.WORKSPACE,true);
                return;
            }else if(isWallpaperMode()){
                showOverviewModeFromOverviewHidenMode(State.WORKSPACE,true);
                return;
			// cyl add for special effect start
            }else if(mState == State.SPECIALEFFECT){
                showOverviewModeFromOverviewHidenMode(State.WORKSPACE,true); 
                return;
			// cyl add for special effect end
            }else if(isUnInstallMode()) {
                CellLayout widgetView = (CellLayout) v;
                LauncherAppWidgetHostView onClickWidgetView = widgetView.getOnClickWidgetView();
                if (onClickWidgetView != null && onClickWidgetView.isUninstallRect() && onClickWidgetView.getTag() instanceof ItemInfo) {
                    Log.d(TAG, "removeWorkspaceOrFolderItem");
                    removeItem(onClickWidgetView, (ItemInfo) onClickWidgetView.getTag(), true /* deleteFromDb */);
                    widgetView.setOnClickWidgetView(null);
                    mWorkspace.setmDelayedResizeRunnable(null);
//                    mWorkspace.removeExtraEmptyScreen(true, true);
                } else {
                    exitUnInstallNormalMode();
                }
                return;
            }
            //lijun add end
            if (mWorkspace.isInOverviewMode()) {
                mWorkspace.snapToPageFromOverView(mWorkspace.indexOfChild(v));
                if(FeatureFlags.WIDGETS_CONTAINER_PAGE){//lijun add for WIDGETS_CONTAINER_PAGE
                    showOverviewModeFromOverviewHidenMode(State.WORKSPACE,true);
                }
                showWorkspace(true);
	//UninstallMode add by liuzuo begin
            }else if (isUninstallMode) {

                CellLayout widgetView = (CellLayout) v;
                LauncherAppWidgetHostView onClickWidgetView = widgetView.getOnClickWidgetView();
                if(onClickWidgetView!=null) {
                    Log.d(TAG, "startUninstallActivity" + onClickWidgetView.getTag());
                }else {
                    Log.d(TAG, "startUninstallActivity  null" );
                }
                if (onClickWidgetView!=null&&onClickWidgetView.isUninstallRect()&&onClickWidgetView.getTag() instanceof ItemInfo) {
                    Log.d(TAG, "removeWorkspaceOrFolderItem");
                    removeItem(onClickWidgetView, (ItemInfo) onClickWidgetView.getTag(), true /* deleteFromDb */);
                    widgetView.setOnClickWidgetView(null);
                    mWorkspace.setmDelayedResizeRunnable(null);
//                    mWorkspace.removeExtraEmptyScreen(true,true);
                }
//UninstallMode add by liuzuo end
            }
            return;
        }

        if(isEditorMode()){
//            if(mState == State.WIDGETS){
//                showOverviewModeFromWidgetMode(State.WORKSPACE,true);
//                return;
//            }
//            if (mWorkspace.isInOverviewMode()) {
//                //lijun add
//                showOverviewModeFromWidgetMode(State.WORKSPACE,true);
//                showWorkspace(true);
//            }
//            if (v instanceof FolderIcon) {
//                onClickFolderIcon(v);
//            }
            return;
        }

        Object tag = v.getTag();
        if(isLauncherArrangeMode()){
            if (v instanceof FolderIcon) {
                onClickFolderIcon(v);
            }else if (tag instanceof ShortcutInfo) {
                if(v.getParent()!=null){
                    getArrangeNavigationBar().addIconIntoNavigationbar(v);
                }
            }
            return;
        }
        if (tag instanceof ShortcutInfo) {
//UninstallMode add by liuzuo begin
            if(isUninstallMode&&v instanceof UninstallRect ){
                UninstallRect textView = (UninstallRect) v;
                if(textView.isUninstallRect()){
                    Log.d(TAG,"startUninstallActivity");
                    if( UninstallDropTarget.getAppInfoFlags((ItemInfo) tag)!=null){
                    UninstallDropTarget.startUninstallActivity(this, (ItemInfo) tag);
                        comfirmingUninstall = true;
                    }else{
                        DeleteDropTarget.removeWorkspaceOrFolderItem(this, (ItemInfo) tag,v);
                        deleteParallelShortcut((ItemInfo) tag);
                    }
                }
//UninstallMode add by liuzuo end

                //M:liuzuo add for addIcon begin
            }else if(mEditFolderIcon!=null&&getImportMode()){
                BubbleTextView bv=  (BubbleTextView)v;
                if(bv.isChecked()){
                    bv.setChecked(false);
                    mCheckedShortcutInfos.remove((ShortcutInfo)tag);
                    mCheckedBubbleTextViews.remove(bv);
                    updateImportButton();
                }else {
                    bv.setChecked(true);
                    Log.d(TAG,"info="+tag.toString());
                    mCheckedShortcutInfos.add((ShortcutInfo)tag);
                    mCheckedBubbleTextViews.add(bv);
                    updateImportButton();
                }
            }else {
                onClickAppShortcut(v);
            }
            //M:liuzuo add for addIcon end
        } else if (tag instanceof FolderInfo) {
            //M:liuzuo add for addIcon begin
            if(v==mEditFolderIcon)return;
            //M:liuzuo add for addIcon end
            if (v instanceof FolderIcon) {
                onClickFolderIcon(v);
            }
        } else if ((FeatureFlags.LAUNCHER3_ALL_APPS_PULL_UP && v instanceof PageIndicator) ||
                (v == mAllAppsButton && mAllAppsButton != null)) {
            onClickAllAppsButton(v);
        } else if (tag instanceof AppInfo) {
            startAppShortcutOrInfoActivity(v);
        } else if (tag instanceof LauncherAppWidgetInfo) {
           //UnistallMode add by liuzuo begin
            if (isUninstallMode && v instanceof UninstallRect) {
                UninstallRect widgetView = (UninstallRect) v;
                Log.d(TAG, "startUninstallWidget");
                if (widgetView.isUninstallRect()) {
                    DeleteDropTarget.removeWorkspaceOrFolderItem(this, (ItemInfo) tag, v);
		//UnistallMode add by liuzuo end
                } else if (v instanceof PendingAppWidgetHostView) {
                    onClickPendingWidget((PendingAppWidgetHostView) v);
                }
            }
        }
        //M:liuzuo add the folderImportMode begin
        else if (v==mFolderImportButton){
            closeFolder();
            setImportMode(false);
            int i=0;
            for(ShortcutInfo info :mCheckedShortcutInfos){
                View view=mWorkspace.getParentCellLayoutForView(mCheckedBubbleTextViews.get(i));
                Log.d(TAG,"info.screenId="+info.toString());
                if(view instanceof CellLayout){
                    mCheckedBubbleTextViews.get(i).setChecked(false);
                    ((CellLayout) view).removeView(mCheckedBubbleTextViews.get(i));
                }
                i++;
            }
            Log.d(TAG,"mCheckedFolderInfos="+mCheckedFolderInfos.toString());
            Iterator<FolderInfo> iterator = mCheckedFolderInfos.iterator();
            while(iterator.hasNext()){
                FolderInfo info = iterator.next();
                info.removeInfo();
            }
            Iterator<FolderIcon> iteratorIcon = mCheckedFolderIcons.iterator();
            while(iteratorIcon.hasNext()){
                FolderIcon icon = iteratorIcon.next();
                icon.removeInfo();
            }
            mEditFolderInfo.addInfo(mCheckedShortcutInfos);
            mOpenFolder = true;
            showFolderIcon();
            exitEditModeAndOpenFolder();
        }
        //M:liuzuo add the folderImportMode end
    }

    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }

    /**
     * Event handler for the app widget view which has not fully restored.
     */
    public void onClickPendingWidget(final PendingAppWidgetHostView v) {
        if (mIsSafeModeEnabled) {
            Toast.makeText(this, R.string.safemode_widget_error, Toast.LENGTH_SHORT).show();
            return;
        }

        final LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) v.getTag();
        if (v.isReadyForClickSetup()) {
            if (info.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_ID_NOT_VALID)) {
                if (!info.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_ID_ALLOCATED)) {
                    // This should not happen, as we make sure that an Id is allocated during bind.
                    return;
                }
                LauncherAppWidgetProviderInfo appWidgetInfo =
                        mAppWidgetManager.findProvider(info.providerName, info.user);
                if (appWidgetInfo != null) {
                    setWaitingForResult(PendingRequestArgs
                            .forWidgetInfo(info.appWidgetId, appWidgetInfo, info));

                    Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, info.appWidgetId);
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, appWidgetInfo.provider);
                    mAppWidgetManager.getUser(appWidgetInfo)
                            .addToIntent(intent, AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE);
                    startActivityForResult(intent, REQUEST_BIND_PENDING_APPWIDGET);
                }
            } else {
                LauncherAppWidgetProviderInfo appWidgetInfo =
                        mAppWidgetManager.getLauncherAppWidgetInfo(info.appWidgetId);
                if (appWidgetInfo != null) {
                    startRestoredWidgetReconfigActivity(appWidgetInfo, info);
                }
            }
        } else if (info.installProgress < 0) {
            // The install has not been queued
            final String packageName = info.providerName.getPackageName();
            showBrokenAppInstallDialog(packageName,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivitySafely(v, LauncherModel.getMarketIntent(packageName), info);
                    }
                });
        } else {
            // Download has started.
            final String packageName = info.providerName.getPackageName();
            startActivitySafely(v, LauncherModel.getMarketIntent(packageName), info);
        }
    }

    private void startRestoredWidgetReconfigActivity(
            LauncherAppWidgetProviderInfo provider, LauncherAppWidgetInfo info) {
        setWaitingForResult(PendingRequestArgs.forWidgetInfo(info.appWidgetId, provider, info));
        mAppWidgetManager.startConfigActivity(provider,
                info.appWidgetId, this, mAppWidgetHost, REQUEST_RECONFIGURE_APPWIDGET);
    }

    /**
     * Event handler for the "grid" button that appears on the home screen, which
     * enters all apps mode.
     *
     * @param v The view that was clicked.
     */
    protected void onClickAllAppsButton(View v) {
        if (LOGD) Log.d(TAG, "onClickAllAppsButton");
        if (!isAppsViewVisible()) {
            getUserEventDispatcher().logActionOnControl(LauncherLogProto.Action.TAP,
                    LauncherLogProto.ALL_APPS_BUTTON);
            showAppsView(true /* animated */, true /* updatePredictedApps */,
                    false /* focusSearchBar */);
        }
    }

    protected void onLongClickAllAppsButton(View v) {
        if (LOGD) Log.d(TAG, "onLongClickAllAppsButton");
        if (!isAppsViewVisible()) {
            getUserEventDispatcher().logActionOnControl(LauncherLogProto.Action.LONGPRESS,
                    LauncherLogProto.ALL_APPS_BUTTON);
            showAppsView(true /* animated */,
                    true /* updatePredictedApps */, true /* focusSearchBar */);
        }
    }

    private void showBrokenAppInstallDialog(final String packageName,
            DialogInterface.OnClickListener onSearchClickListener) {
        new AlertDialog.Builder(this)
            .setTitle(R.string.abandoned_promises_title)
            .setMessage(R.string.abandoned_promise_explanation)
            .setPositiveButton(R.string.abandoned_search, onSearchClickListener)
            .setNeutralButton(R.string.abandoned_clean_this,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        final UserHandleCompat user = UserHandleCompat.myUserHandle();
                        mWorkspace.removeAbandonedPromise(packageName, user);
                    }
                })
            .create().show();
        return;
    }

    /**
     * Event handler for an app shortcut click.
     *
     * @param v The view that was clicked. Must be a tagged with a {@link ShortcutInfo}.
     */
    protected void onClickAppShortcut(final View v) {
        if (LOGD) Log.d(TAG, "onClickAppShortcut");
        Object tag = v.getTag();
        if (!(tag instanceof ShortcutInfo)) {
            throw new IllegalArgumentException("Input must be a Shortcut");
        }

        // Open shortcut
        final ShortcutInfo shortcut = (ShortcutInfo) tag;

        if (shortcut.isDisabled != 0) {
            if ((shortcut.isDisabled &
                    ~ShortcutInfo.FLAG_DISABLED_SUSPENDED &
                    ~ShortcutInfo.FLAG_DISABLED_QUIET_USER) == 0) {
                // If the app is only disabled because of the above flags, launch activity anyway.
                // Framework will tell the user why the app is suspended.
            } else {
                if (!TextUtils.isEmpty(shortcut.disabledMessage)) {
                    // Use a message specific to this shortcut, if it has one.
                    Toast.makeText(this, shortcut.disabledMessage, Toast.LENGTH_SHORT).show();
                    return;
                }
                // Otherwise just use a generic error message.
                int error = R.string.activity_not_available;
                if ((shortcut.isDisabled & ShortcutInfo.FLAG_DISABLED_SAFEMODE) != 0) {
                    error = R.string.safemode_shortcut_error;
                } else if ((shortcut.isDisabled & ShortcutInfo.FLAG_DISABLED_BY_PUBLISHER) != 0 ||
                        (shortcut.isDisabled & ShortcutInfo.FLAG_DISABLED_LOCKED_USER) != 0) {
                    error = R.string.shortcut_not_available;
                }
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Check for abandoned promise
        if ((v instanceof BubbleTextView)
                && shortcut.isPromise()
                && !shortcut.hasStatusFlag(ShortcutInfo.FLAG_INSTALL_SESSION_ACTIVE)) {
            showBrokenAppInstallDialog(
                    shortcut.getTargetComponent().getPackageName(),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            startAppShortcutOrInfoActivity(v);
                        }
                    });
            return;
        }

        // Start activities
        startAppShortcutOrInfoActivity(v);
    }

    private void startAppShortcutOrInfoActivity(View v) {
        ItemInfo item = (ItemInfo) v.getTag();
        Intent intent = item.getIntent();
        if (intent == null) {
            throw new IllegalArgumentException("Input must have a valid intent");
        }
        boolean success = startActivitySafely(v, intent, item);
        getUserEventDispatcher().logAppLaunch(v, intent);

        if (success && v instanceof BubbleTextView) {
            mWaitingForResume = (BubbleTextView) v;
            mWaitingForResume.setStayPressed(true);
        }
    }

    /**
     * Event handler for a folder icon click.
     *
     * @param v The view that was clicked. Must be an instance of {@link FolderIcon}.
     */
    protected void onClickFolderIcon(View v) {
        if (LOGD) Log.d(TAG, "onClickFolder"+"  "+v.getTag());
        if (!(v instanceof FolderIcon)){
            throw new IllegalArgumentException("Input must be a FolderIcon");
        }

        FolderIcon folderIcon = (FolderIcon) v;
        if (!folderIcon.getFolderInfo().opened && !folderIcon.getFolder().isDestroyed()) {
            if(mAniWorkspaceBg == null || !mAniWorkspaceBg.isRunning()) {//liuzuo add
                // Open the requested folder
                openFolder(folderIcon);
            }
        }
    }
    public boolean isInVacantsClear=false;
    protected void onClickVacantsClearButton(View view) {
        if(!mModel.isAllAppsLoaded() || mWorkspaceLoading || mWorkspace.rejectClickOnMenuButton()){
            return;
        }
        if (mIsSafeModeEnabled) {
            Toast.makeText(this, R.string.safemode_wallpaper_error, Toast.LENGTH_SHORT).show();
        } else {
            showArrangeNavigationBar(true);
        }
    }

    private void confirmVacantsClear(){
        int currPageIndex = getCurrentWorkspaceScreen();
        CellLayout currCellLayout = (CellLayout)mWorkspace.getPageAt(currPageIndex);
        if (currCellLayout == null) return;
        LauncherAppState app = LauncherAppState.getInstance();
        InvariantDeviceProfile profile = app.getInvariantDeviceProfile();
        final int xCount =  profile.numColumns;
        final int yCount =  profile.numRows;
        //boolean[][] occupied = new boolean[xCount][yCount];
        GridOccupancy gridOccupancy = new GridOccupancy(xCount,yCount);
        int[] vacant = new int[2];
        final ArrayList<ItemInfo> reorderItems = new ArrayList<>();
        final ArrayList<View> reorderViews = new ArrayList<>();
        Log.v("reorder", "onClickVacantsClearButton--------------");
        for(int y=0; y<yCount; y++){
            for(int x=0; x<xCount; x++){
                if(gridOccupancy.isOcupied(x,y)){
                    continue;
                }
                View child = currCellLayout.getChildAt(x,y);
                if(child != null) {
                    CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
                    if (child instanceof LauncherAppWidgetHostView){
                        gridOccupancy.markCells(0,lp.cellY,xCount,lp.cellVSpan,true);
                        continue;
                    }
                    if(child.getTag() instanceof ItemInfo) {
                        ItemInfo info = (ItemInfo)child.getTag();
                        Log.v("reorder", "child---info.title="+info.title+", lp.x="+lp.x+", lp.y="+lp.y);
                        gridOccupancy.findVacantCell(vacant,1,1);
                        if(info.cellX == vacant[0] && info.cellY == vacant[1]){
                            gridOccupancy.markCells(info.cellX,info.cellY,1,1,true);
                            continue;
                        }
                        lp.tmpCellX = info.cellX = vacant[0];
                        lp.tmpCellY = info.cellY = vacant[1];
                        gridOccupancy.markCells(info.cellX,info.cellY,1,1,true);
                        reorderItems.add(info);
                        reorderViews.add(child);
                    }
                }
            }
        }
        if(!reorderItems.isEmpty()) {
            Log.v("reorder", "reorderItems.size="+(reorderItems.size()));
            for(View child : reorderViews){
                ItemInfo info = (ItemInfo)child.getTag();
                currCellLayout.animateChildToPosition(child, info.cellX, info.cellY, 160, 0, true, true);
            }
            LauncherModel.sWorker.post(new Runnable() {
                @Override
                public void run() {
                    long screenId = reorderItems.get(0).screenId;
                    LauncherModel.moveItemsInDatabase(Launcher.this, reorderItems, LauncherSettings.Favorites.CONTAINER_DESKTOP, (int) screenId);
                }
            });
        }else {
            showToast(R.string.aline_toast_text);
        }
    }


    /**
     * Event handler for the (Add) Widgets button that appears after a long press
     * on the home screen.
     */
    public void onClickAddWidgetButton(View view) {
        if (LOGD) Log.d(TAG, "onClickAddWidgetButton");
        if (mIsSafeModeEnabled) {
            Toast.makeText(this, R.string.safemode_widget_error, Toast.LENGTH_SHORT).show();
        } else {
            showWidgetsView(true /* animated */, true /* resetPageToZero */);
        }
    }

    /**
     * Event handler for the wallpaper picker button that appears after a long press
     * on the home screen.
     */
    public void onClickWallpaperPicker(View v) {
        if (!Utilities.isWallapaperAllowed(this)) {
            Toast.makeText(this, R.string.msg_disabled_by_admin, Toast.LENGTH_SHORT).show();
            return;
        }

        String pickerPackage = getString(R.string.wallpaper_picker_package);
        if (TextUtils.isEmpty(pickerPackage)) {
            pickerPackage =  PackageManagerHelper.getWallpaperPickerPackage(getPackageManager());
        }

        int pageScroll = mWorkspace.getScrollForPage(mWorkspace.getPageNearestToCenterOfScreen());
        float offset = mWorkspace.mWallpaperOffset.wallpaperOffsetForScroll(pageScroll);

        setWaitingForResult(new PendingRequestArgs(new ItemInfo()));
        Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER)
                .setPackage(pickerPackage)
                .putExtra(Utilities.EXTRA_WALLPAPER_OFFSET, offset);
        intent.setSourceBounds(getViewBounds(v));
        startActivityForResult(intent, REQUEST_PICK_WALLPAPER, getActivityLaunchOptions(v));
    }

    //lijun add for wallpaper start
    public void onClickWallpaperPickerNew(View v){
        if (LOGD) Log.d(TAG, "onClickWallpaperPickerNew");
        if (mIsSafeModeEnabled) {
            Toast.makeText(this, R.string.safemode_wallpaper_error, Toast.LENGTH_SHORT).show();
        } else {
            showWallpaperPanel(true);
        }
    }

    private void showWallpaperPanel(boolean animated){
        mState = State.WALLPAPER;
        mStateTransitionAnimation.startAnimationBetweenOverviewAndOverviewHiden(Workspace.State.OVERVIEW_HIDDEN,State.WALLPAPER, animated);
        mWallpaperPicker.requestFocus();
    }
    public void refreshWallPaperList(final boolean comeIn){
        new Thread(new Runnable() {
            @Override
            public void run() {
                mWallpaperPicker.refresh(comeIn);
            }
        }).start();
    }
    //lijun add for wallpaper end

    /**
     * Event handler for a click on the settings button that appears after a long press
     * on the home screen.
     */
    public void onClickSettingsButton(View v) {
        if (LOGD) Log.d(TAG, "onClickSettingsButton");
        Intent intent = new Intent(Intent.ACTION_APPLICATION_PREFERENCES)
                .setPackage(getPackageName());
        intent.setSourceBounds(getViewBounds(v));
        startActivity(intent, getActivityLaunchOptions(v));
    }

    public View.OnTouchListener getHapticFeedbackTouchListener() {
        if (mHapticFeedbackTouchListener == null) {
            mHapticFeedbackTouchListener = new View.OnTouchListener() {
                @SuppressLint("ClickableViewAccessibility")
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
                        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    }
                    return false;
                }
            };
        }
        return mHapticFeedbackTouchListener;
    }

    @Override
    public void onAccessibilityStateChanged(boolean enabled) {
        mDragLayer.onAccessibilityStateChanged(enabled);
    }

    public void onDragStarted() {
        if (isOnCustomContent()) {
            // Custom content screen doesn't participate in drag and drop. If on custom
            // content screen, move to default.
            moveWorkspaceToDefaultScreen();
        }
    }

    /**
     * Called when the user stops interacting with the launcher.
     * This implies that the user is now on the homescreen and is not doing housekeeping.
     */
    protected void onInteractionEnd() {
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onInteractionEnd();
        }
    }

    /**
     * Called when the user starts interacting with the launcher.
     * The possible interactions are:
     *  - open all apps
     *  - reorder an app shortcut, or a widget
     *  - open the overview mode.
     * This is a good time to stop doing things that only make sense
     * when the user is on the homescreen and not doing housekeeping.
     */
    protected void onInteractionBegin() {
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onInteractionBegin();
        }
    }

    /** Updates the interaction state. */
    public void updateInteraction(Workspace.State fromState, Workspace.State toState) {
        // Only update the interacting state if we are transitioning to/from a view with an
        // overlay
        boolean fromStateWithOverlay = fromState != Workspace.State.NORMAL;
        boolean toStateWithOverlay = toState != Workspace.State.NORMAL;
        if (toStateWithOverlay) {
            onInteractionBegin();
        } else if (fromStateWithOverlay) {
            onInteractionEnd();
        }
    }

    private void startShortcutIntentSafely(Intent intent, Bundle optsBundle, ItemInfo info) {
        try {
            StrictMode.VmPolicy oldPolicy = StrictMode.getVmPolicy();
            try {
                // Temporarily disable deathPenalty on all default checks. For eg, shortcuts
                // containing file Uri's would cause a crash as penaltyDeathOnFileUriExposure
                // is enabled by default on NYC.
                StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll()
                        .penaltyLog().build());

                if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT) {
                    String id = ((ShortcutInfo) info).getDeepShortcutId();
                    String packageName = intent.getPackage();
                    LauncherAppState.getInstance().getShortcutManager().startShortcut(
                            packageName, id, intent.getSourceBounds(), optsBundle, info.user);
                } else {
                    // Could be launching some bookkeeping activity
                    startActivity(intent, optsBundle);
                }
            } finally {
                StrictMode.setVmPolicy(oldPolicy);
            }
        } catch (SecurityException e) {
            // Due to legacy reasons, direct call shortcuts require Launchers to have the
            // corresponding permission. Show the appropriate permission prompt if that
            // is the case.
            if (intent.getComponent() == null
                    && Intent.ACTION_CALL.equals(intent.getAction())
                    && checkSelfPermission(Manifest.permission.CALL_PHONE) !=
                    PackageManager.PERMISSION_GRANTED) {

                setWaitingForResult(PendingRequestArgs
                        .forIntent(REQUEST_PERMISSION_CALL_PHONE, intent, info));
                requestPermissions(new String[]{Manifest.permission.CALL_PHONE},
                        REQUEST_PERMISSION_CALL_PHONE);
            } else {
                // No idea why this was thrown.
                throw e;
            }
        }
    }

    private Bundle getActivityLaunchOptions(View v) {
        if (Utilities.ATLEAST_MARSHMALLOW) {
            int left = 0, top = 0;
            int width = v.getMeasuredWidth(), height = v.getMeasuredHeight();
            if (v instanceof TextView) {
                // Launch from center of icon, not entire view
                Drawable icon = Workspace.getTextViewIcon((TextView) v);
                if (icon != null) {
                    Rect bounds = icon.getBounds();
                    left = (width - bounds.width()) / 2;
                    top = v.getPaddingTop();
                    width = bounds.width();
                    height = bounds.height();
                }
            }
            return ActivityOptions.makeClipRevealAnimation(v, left, top, width, height).toBundle();
        } else if (Utilities.ATLEAST_LOLLIPOP_MR1) {
            // On L devices, we use the device default slide-up transition.
            // On L MR1 devices, we use a custom version of the slide-up transition which
            // doesn't have the delay present in the device default.
            return ActivityOptions.makeCustomAnimation(
                    this, R.anim.task_open_enter, R.anim.no_anim).toBundle();
        }
        return null;
    }

    private Rect getViewBounds(View v) {
        int[] pos = new int[2];
        v.getLocationOnScreen(pos);
        return new Rect(pos[0], pos[1], pos[0] + v.getWidth(), pos[1] + v.getHeight());
    }

    public boolean startActivitySafely(View v, Intent intent, ItemInfo item) {
        if (mIsSafeModeEnabled && !Utilities.isSystemApp(this, intent)) {
            Toast.makeText(this, R.string.safemode_shortcut_error, Toast.LENGTH_SHORT).show();
            return false;
        }
        // Only launch using the new animation if the shortcut has not opted out (this is a
        // private contract between launcher and may be ignored in the future).
        boolean useLaunchAnimation = (v != null) &&
                !intent.hasExtra(INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION);
        Bundle optsBundle = useLaunchAnimation ? getActivityLaunchOptions(v) : null;

        UserHandleCompat user = null;
        if (intent.hasExtra(AppInfo.EXTRA_PROFILE)) {
            long serialNumber = intent.getLongExtra(AppInfo.EXTRA_PROFILE, -1);
            user = UserManagerCompat.getInstance(this).getUserForSerialNumber(serialNumber);
        }

        // Prepare intent
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (v != null) {
            intent.setSourceBounds(getViewBounds(v));
        }
        Log.i("Launcher","startActivitySafely : intent : "+intent+" , \nitem :"+item);
        try {
            if (Utilities.ATLEAST_MARSHMALLOW && item != null
                    && (item.itemType == Favorites.ITEM_TYPE_SHORTCUT
                    || item.itemType == Favorites.ITEM_TYPE_DEEP_SHORTCUT)
                    && ((ShortcutInfo) item).promisedIntent == null) {
                // Shortcuts need some special checks due to legacy reasons.
                startShortcutIntentSafely(intent, optsBundle, item);
            } else if (user == null || user.equals(UserHandleCompat.myUserHandle())) {
                // Could be launching some bookkeeping activity
                startActivity(intent, optsBundle);
            } else {
                LauncherAppsCompat.getInstance(this).startActivityForProfile(
                        intent.getComponent(), user, intent.getSourceBounds(), optsBundle);
            }
            return true;
        } catch (ActivityNotFoundException|SecurityException e) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Unable to launch. tag=" + item + " intent=" + intent, e);
        }
        return false;
    }

    /**
     * This method draws the FolderIcon to an ImageView and then adds and positions that ImageView
     * in the DragLayer in the exact absolute location of the original FolderIcon.
     */
    private void copyFolderIconToImage(FolderIcon fi) {
        final int width = fi.getMeasuredWidth();
        final int height = fi.getMeasuredHeight();

        // Lazy load ImageView, Bitmap and Canvas
        if (mFolderIconImageView == null) {
            mFolderIconImageView = new ImageView(this);
        }
        if (mFolderIconBitmap == null || mFolderIconBitmap.getWidth() != width ||
                mFolderIconBitmap.getHeight() != height) {
            mFolderIconBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            mFolderIconCanvas = new Canvas(mFolderIconBitmap);
        }

        DragLayer.LayoutParams lp;
        if (mFolderIconImageView.getLayoutParams() instanceof DragLayer.LayoutParams) {
            lp = (DragLayer.LayoutParams) mFolderIconImageView.getLayoutParams();
        } else {
            lp = new DragLayer.LayoutParams(width, height);
        }

        // The layout from which the folder is being opened may be scaled, adjust the starting
        // view size by this scale factor.
        float scale = mDragLayer.getDescendantRectRelativeToSelf(fi, mRectForFolderAnimation);
        lp.customPosition = true;
        lp.x = mRectForFolderAnimation.left;
        lp.y = mRectForFolderAnimation.top;
        lp.width = (int) (scale * width);
        lp.height = (int) (scale * height);

        mFolderIconCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        fi.draw(mFolderIconCanvas);
        mFolderIconImageView.setImageBitmap(mFolderIconBitmap);
        if (fi.getFolder() != null) {
            mFolderIconImageView.setPivotX(fi.getFolder().getPivotXForIconAnimation());
            mFolderIconImageView.setPivotY(fi.getFolder().getPivotYForIconAnimation());
        }
        // Just in case this image view is still in the drag layer from a previous animation,
        // we remove it and re-add it.
        if (mDragLayer.indexOfChild(mFolderIconImageView) != -1) {
            mDragLayer.removeView(mFolderIconImageView);
        }
        mDragLayer.addView(mFolderIconImageView, lp);
        if (fi.getFolder() != null) {
            fi.getFolder().bringToFront();
        }
    }

    private void growAndFadeOutFolderIcon(FolderIcon fi) {
        if (fi == null) return;
        FolderInfo info = (FolderInfo) fi.getTag();
       /* cyl del for bug:2529
         if (info.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            CellLayout cl = (CellLayout) fi.getParent().getParent();
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) fi.getLayoutParams();
            cl.setFolderLeaveBehindCell(lp.cellX, lp.cellY);
        } */

        // Push an ImageView copy of the FolderIcon into the DragLayer and hide the original
        copyFolderIconToImage(fi);
        fi.setVisibility(View.INVISIBLE);

        ObjectAnimator oa = LauncherAnimUtils.ofViewAlphaAndScale(
                mFolderIconImageView, 0, 1f, 1f);//liuzuo add 2f >> 1.5f
        if (Utilities.ATLEAST_LOLLIPOP) {
            oa.setInterpolator(new LogDecelerateInterpolator(100, 0));
        }
        oa.setDuration(10/*getResources().getInteger(R.integer.config_folderExpandDuration)*/);
        oa.start();
    }

    private void shrinkAndFadeInFolderIcon(final FolderIcon fi, boolean animate) {
        if (fi == null) return;
        final CellLayout cl = (CellLayout) fi.getParent().getParent();

        // We remove and re-draw the FolderIcon in-case it has changed
        mDragLayer.removeView(mFolderIconImageView);
        copyFolderIconToImage(fi);

        if (cl != null) {
            cl.clearFolderLeaveBehind();
        }

        ObjectAnimator oa = LauncherAnimUtils.ofViewAlphaAndScale(mFolderIconImageView, 1, 1, 1);
        oa.setDuration(getResources().getInteger(R.integer.config_folderExpandDuration));
        oa.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (cl != null) {
                    // Remove the ImageView copy of the FolderIcon and make the original visible.
                    mDragLayer.removeView(mFolderIconImageView);
                    fi.setVisibility(View.VISIBLE);
                }
            }
        });
        oa.start();
        if (!animate) {
            oa.end();
        }
    }

    /**
     * Opens the user folder described by the specified tag. The opening of the folder
     * is animated relative to the specified View. If the View is null, no animation
     * is played.
     *
     * @param folderIcon The FolderIcon describing the folder to open.
     */
    public void openFolder(FolderIcon folderIcon) {
        hideImportMode();//liuzuo add for addIcon
        Folder folder = folderIcon.getFolder();
        Folder openFolder = mWorkspace != null ? mWorkspace.getOpenFolder() : null;
        if (openFolder != null && openFolder != folder) {
            // Close any open folder before opening a folder.
            closeFolder();
        }
        mWorkspace.getPageIndicator().setAlpha(0f);
        mWorkspace.getPageIndicator().setVisibility(View.INVISIBLE);//liuzuo add  for hide the indicator of workspace
        FolderInfo info = folder.mInfo;

        info.opened = true;

        // While the folder is open, the position of the icon cannot change.
        ((CellLayout.LayoutParams) folderIcon.getLayoutParams()).canReorder = false;

        // Just verify that the folder hasn't already been added to the DragLayer.
        // There was a one-off crash where the folder had a parent already.
        if (folder.getParent() == null) {
            mDragLayer.addView(folder);
            mDragController.addDropTarget(folder);
        } else {
            Log.w(TAG, "Opening folder (" + folder + ") which already has a parent (" +
                    folder.getParent() + ").");
        }
/*        //liuzuo add begin
        if(isLauncherArrangeMode()){
            folder.beginExternalDrag();
        }
        //liuzuo add end*/
        folder.animateOpen();

        growAndFadeOutFolderIcon(folderIcon);//liuzuo remove folderIcon scale animation

        // Notify the accessibility manager that this folder "window" has appeared and occluded
        // the workspace items
        folder.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        getDragLayer().sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
        //liuzuo add the background of workspace when opening folder begin
        openOrCloseFolderAnimation(true);
        //copyFolderIconToImage(folderIcon);
        //liuzuo add the background of workspace when opening folder end

        //lijun add start
        if(isLauncherArrangeMode() && mNavigationbar!=null){
            mNavigationbar.enableAddFolder(false);
        }
        //lijun add end
    }

    public void closeFolder() {
        closeFolder(true);
    }

    public void closeFolder(boolean animate) {
        Folder folder = mWorkspace != null ? mWorkspace.getOpenFolder() : null;
        if (folder != null) {
            if (folder.isEditingName()) {
                folder.dismissEditingName();
            }
            closeFolder(folder, animate);
        }
        if(isUnInstallMode()) {
            if(mState != State.WORKSPACE_SPRING_LOADED) {
                enterSpringLoadedDragMode();
            }
        }else {
            mWorkspace.getPageIndicator().setAlpha(1f);
            mWorkspace.getPageIndicator().setVisibility(View.VISIBLE);//liuzuo add  for hide the indicator of workspace
        }

        //lijun add start
        if(isLauncherArrangeMode() && mNavigationbar!=null){
            mNavigationbar.enableAddFolder(true);
        }
        //lijun add end
    }

    public void closeFolder(Folder folder, boolean animate) {
        animate &= !Utilities.isPowerSaverOn(this);

        folder.getInfo().opened = false;
        FolderIcon fi = (FolderIcon) mWorkspace.getViewForTag(folder.mInfo);
        ViewGroup parent = (ViewGroup) folder.getParent().getParent();
        if (parent != null) {
            //M:liuzuo add addIcon  begin
            folder.hideAddView();
            //M:liuzuo add addIcon  end
            if (fi != null) {
                ((CellLayout.LayoutParams) fi.getLayoutParams()).canReorder = true;
            }
        }
        shrinkAndFadeInFolderIcon(fi, false);// liuzuo false >> animate
        if (animate) {
            folder.animateClosed();
        } else {
            folder.close(false);
        }

        // Notify the accessibility manager that this folder "window" has disappeared and no
        // longer occludes the workspace items
        getDragLayer().sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        //liuzuo add the background of workspace when opening folder begin
        openOrCloseFolderAnimation(false);
        //liuzuo add the background of workspace when opening folder end
        //M:liuzuo add the addIcon begin
        if(getImportMode()){
            animationCloseFolder(folder);
        }else if(getHotseatVisible()){
            mWorkspace.showWorkspace();
        }
        //M:liuzuo add the addIcon end
    }
    //liuzuo add for visible of hotseat begin
    private boolean getHotseatVisible(){
        return getHotseat().getVisibility()==View.INVISIBLE||getHotseat().getAlpha()==0;
    }

    //liuzuo add for visible of hotseat end
    public void closeShortcutsContainer() {
        closeShortcutsContainer(true);
    }

    public void closeShortcutsContainer(boolean animate) {
        DeepShortcutsContainer deepShortcutsContainer = getOpenShortcutsContainer();
        if (deepShortcutsContainer != null) {
            if (animate) {
                deepShortcutsContainer.animateClose();
            } else {
                deepShortcutsContainer.close();
            }
        }
    }

    public View getTopFloatingView() {
        View topView = getOpenShortcutsContainer();
        if (topView == null) {
            topView = getWorkspace().getOpenFolder();
        }
        return topView;
    }

    /**
     * @return The open shortcuts container, or null if there is none
     */
    public DeepShortcutsContainer getOpenShortcutsContainer() {
        // Iterate in reverse order. Shortcuts container is added later to the dragLayer,
        // and will be one of the last views.
        for (int i = mDragLayer.getChildCount() - 1; i >= 0; i--) {
            View child = mDragLayer.getChildAt(i);
            if (child instanceof DeepShortcutsContainer
                    && ((DeepShortcutsContainer) child).isOpen()) {
                return (DeepShortcutsContainer) child;
            }
        }
        return null;
    }

    @Override
    public boolean onLongClick(View v) {
        if (!isDraggingEnabled()) return false;
        if (isWorkspaceLocked()) return false;
        //liuzuo add begin
        if(mDragLayer.getActiveController() instanceof  PinchToOverviewListener){
            PinchToOverviewListener controller = (PinchToOverviewListener) mDragLayer.getActiveController();
            if(controller.isPinchStarted()) {
                return false;
            }
        }
        //liuzuo add end
        if (mState != State.WORKSPACE && mState!= State.WORKSPACE_SPRING_LOADED&&mState!=State.ICONARRANGE) return false;//liuzuo add for iconarrange

        //lijun add start
        if (isUnInstallMode() && ((v instanceof Workspace) || (v instanceof CellLayout))) {
            return true;
        }
        //lijun add end

        if ((FeatureFlags.LAUNCHER3_ALL_APPS_PULL_UP && v instanceof PageIndicator) ||
                (v == mAllAppsButton && mAllAppsButton != null)) {
            onLongClickAllAppsButton(v);
            return true;
        }

        if (v instanceof Workspace) {
            if (!mWorkspace.isInOverviewMode()) {
                if (!mWorkspace.isTouchActive()) {
                    //showOverviewMode(true);
                    //mWorkspace.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS,
                    //        HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }

        CellLayout.CellInfo longClickCellInfo = null;
        View itemUnderLongClick = null;
        if (v.getTag() instanceof ItemInfo) {
            ItemInfo info = (ItemInfo) v.getTag();
            longClickCellInfo = new CellLayout.CellInfo(v, info);
            itemUnderLongClick = longClickCellInfo.cell;
            mPendingRequestArgs = null;
        }

        //lijun add start
        if(v.getTag() instanceof ItemInfo && isEditorMode())itemUnderLongClick = null;
        //lijun add end

        // The hotseat touch handling does not go through Workspace, and we always allow long press
        // on hotseat items.
        if (!mDragController.isDragging()) {
            if (itemUnderLongClick == null) {
                // User long pressed on empty space
                if (mWorkspace.isInOverviewMode()) {
                    mWorkspace.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS,
                            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
                    //lijun add start
                    View parentView = v;
                    if (v.getTag() instanceof ItemInfo && v.getParent() != null && v.getParent().getParent() != null && v.getParent().getParent() instanceof CellLayout) {
                        parentView = (View) v.getParent().getParent();
                    }
                    //lijun add end
                    mWorkspace.startReordering(parentView);
                } else {
                    //showOverviewMode(true);
                }
            } else {
                final boolean isAllAppsButton =
                        !FeatureFlags.NO_ALL_APPS_ICON && isHotseatLayout(v) &&
                                mDeviceProfile.inv.isAllAppsButtonRank(mHotseat.getOrderInHotseat(
                                        longClickCellInfo.cellX, longClickCellInfo.cellY));
                if (!(itemUnderLongClick instanceof Folder || isAllAppsButton)) {
                    // User long pressed on an item
                    DragOptions dragOptions = new DragOptions();
                    if (itemUnderLongClick instanceof BubbleTextView) {
                        BubbleTextView icon = (BubbleTextView) itemUnderLongClick;
                        if (icon.hasDeepShortcuts()) {
                            DeepShortcutsContainer dsc = DeepShortcutsContainer.showForIcon(icon);
                            if (dsc != null) {
                                dragOptions.deferDragCondition = dsc.createDeferDragCondition(null);
                            }
                        }
                    }
					getHotseat().initViewCacheList(); // cyl add for hotseat icon center
//liuzuo add for arrangeMode begin
                    if(isLauncherArrangeMode()&&v.getParent().getParent() instanceof  ArrangeNavigationBar){
                        getArrangeNavigationBar(). startDrag(v,dragOptions);
                    }else if(isArrangeBarShowing()&&(v.getTag() instanceof  FolderInfo||v.getTag() instanceof LauncherAppWidgetInfo)){
                        return false;
                    }else {
//liuzuo add for arrangeMode end
                        mWorkspace.startDrag(longClickCellInfo, dragOptions);
                    }
                }
            }
        }
        return true;
    }

    public boolean isHotseatLayout(View layout) {
        return mHotseat != null && layout != null &&
                (layout instanceof CellLayout) && (layout == mHotseat.getLayout());
    }

    /**
     * Returns the CellLayout of the specified container at the specified screen.
     */
    public CellLayout getCellLayout(long container, long screenId) {
        if (container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            if (mHotseat != null) {
                return mHotseat.getLayout();
            } else {
                return null;
            }
        } else {
            return mWorkspace.getScreenWithId(screenId);
        }
    }

    /**
     * For overridden classes.
     */
    public boolean isAllAppsVisible() {
        return isAppsViewVisible();
    }

    public boolean isAppsViewVisible() {
        return (mState == State.APPS) || (mOnResumeState == State.APPS);
    }

    public boolean isWidgetsViewVisible() {
        return (mState == State.WIDGETS) || (mOnResumeState == State.WIDGETS) || (mState == State.WIDGETS_SPRING_LOADED) || (mOnResumeState == State.WIDGETS_SPRING_LOADED);
    }

    public boolean isWidgetsPanelShowing() {
        if (mWidgetsView.getAlpha() > 0.5 && mWidgetsView.getVisibility() == View.VISIBLE) {
            return true;
        }
        return false;
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            // The widget preview db can result in holding onto over
            // 3MB of memory for caching which isn't necessary.
            SQLiteDatabase.releaseMemory();

            // This clears all widget bitmaps from the widget tray
            // TODO(hyunyoungs)
        }
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.onTrimMemory(level);
        }
    }

    public boolean showWorkspace(boolean animated) {
        return showWorkspace(animated, null);
    }

    public boolean showWorkspace(boolean animated, Runnable onCompleteRunnable) {
        boolean changed = mState != State.WORKSPACE ||
                mWorkspace.getState() != Workspace.State.NORMAL;
        if (changed || mAllAppsController.isTransitioning()) {
            fullscreenOrNot(false);//lijun add for hide statusbar
            mWorkspace.setVisibility(View.VISIBLE);
            mStateTransitionAnimation.startAnimationToWorkspace(mState, mWorkspace.getState(),
                    Workspace.State.NORMAL, animated, onCompleteRunnable);

            // Set focus to the AppsCustomize button
            if (mAllAppsButton != null) {
                mAllAppsButton.requestFocus();
            }
        }

        checkAndResetForState(State.WORKSPACE);//lijun add

        // Change the state *after* we've called all the transition code
        mState = State.WORKSPACE;

        // Resume the auto-advance of widgets
        mUserPresent = true;
        updateAutoAdvanceState();

        if (changed) {
            // Send an accessibility event to announce the context change
            getWindow().getDecorView()
                    .sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        }
        mWorkspace.refreashUnread();//liuzuo add
        return changed;
    }

    /**
     * Shows the overview button.
     */
    public void showOverviewMode(boolean animated) {
        showOverviewMode(animated, false);
    }

    /**
     * Shows the overview button, and if {@param requestButtonFocus} is set, will force the focus
     * onto one of the overview panel buttons.
     */
    void showOverviewMode(boolean animated, final boolean requestButtonFocus) {
        Runnable postAnimRunnable = null;

            postAnimRunnable = new Runnable() {
                @Override
                public void run() {
                    // Hitting the menu button when in touch mode does not trigger touch mode to
                    // be disabled, so if requested, force focus on one of the overview panel
                    // buttons.
                    if (requestButtonFocus) {
                        mOverviewPanel.requestFocusFromTouch();
                    }
                    mWorkspace.updateFoldersUnread();//liuzuo add for update unread in folder
                }
            };
        fullscreenOrNot(true);
        mWorkspace.setDefaultScreen(mWorkspace.defaultScreenId);//lijun add for homebutton
        mWorkspace.setVisibility(View.VISIBLE);
        //lijun modify for overviewhiden start
//        mStateTransitionAnimation.startAnimationToWorkspace(mState, mWorkspace.getState(),
//                Workspace.State.OVERVIEW, animated, postAnimRunnable);
      // cyl add for special effect :State.SPECIALEFFECT
        if (mState == State.WIDGETS || mState == State.WALLPAPER||mState == State.ICONARRANGE || mState == State.SPECIALEFFECT) {
            Folder.ICONARRANGING = false;
            showOverviewModeFromOverviewHidenMode(State.WORKSPACE, true);
        } else {
            mStateTransitionAnimation.startAnimationToWorkspace(mState, mWorkspace.getState(),
                    Workspace.State.OVERVIEW, animated, postAnimRunnable);
            mWorkspace.exitWidgetResizeMode();//liuzuo add for hide apphostview in the wrong situation
        }
        //lijun modify end
        mState = State.WORKSPACE;
        // If animated from long press, then don't allow any of the controller in the drag
        // layer to intercept any remaining touch.
        mWorkspace.requestDisallowInterceptTouchEvent(animated);
//UnistallMode add by liuzuo begin
        if(isUnInstallMode()) {
            exitUnInstallNormalMode();
        }
//UnistallMode add by liuzuo end
    }

    /**
     * Shows the apps view.
     */
    public void showAppsView(boolean animated, boolean updatePredictedApps,
            boolean focusSearchBar) {
        markAppsViewShown();
        if (updatePredictedApps) {
            tryAndUpdatePredictedApps();
        }
        showAppsOrWidgets(State.APPS, animated, focusSearchBar);
    }

    /**
     * Shows the widgets view.
     */
    void showWidgetsView(boolean animated, boolean resetPageToZero) {
        if (LOGD) Log.d(TAG, "showWidgetsView:" + animated + " resetPageToZero:" + resetPageToZero);
        if (resetPageToZero) {
            mWidgetsView.scrollToTop();
        }
        showAppsOrWidgets(State.WIDGETS, animated, false);

        mWidgetsView.post(new Runnable() {
            @Override
            public void run() {
                mWidgetsView.requestFocus();
            }
        });
    }

    /**
     * lijun add to hide Widgets PageView
     */
    private boolean showOverviewModeFromOverviewHidenMode(State toState, boolean animated){
        // cyl add for special effect :State.SPECIALEFFECT
        if (mState != State.WIDGETS && mState != State.WALLPAPER&&mState!=State.ICONARRANGE && mState != State.SPECIALEFFECT) {
            return false;
        }
        if (toState != State.WORKSPACE) {
            return false;
        }
        mStateTransitionAnimation.startAnimationBetweenOverviewAndOverviewHiden(Workspace.State.OVERVIEW,mState, animated);

        // Change the state *after* we've called all the transition code
        mState = toState;

        return true;
    }

    /**
     * Sets up the transition to show the apps/widgets view.
     *
     * @return whether the current from and to state allowed this operation
     */
    // TODO: calling method should use the return value so that when {@code false} is returned
    // the workspace transition doesn't fall into invalid state.
    private boolean showAppsOrWidgets(State toState, boolean animated, boolean focusSearchBar) {
        if (!(mState == State.WORKSPACE ||
                mState == State.APPS_SPRING_LOADED ||
                mState == State.WIDGETS_SPRING_LOADED ||
                (mState == State.APPS && mAllAppsController.isTransitioning()))) {
            return false;
        }
        if (toState != State.APPS && toState != State.WIDGETS) {
            return false;
        }

        // This is a safe and supported transition to bypass spring_loaded mode.
        if (mExitSpringLoadedModeRunnable != null) {
            mHandler.removeCallbacks(mExitSpringLoadedModeRunnable);
            mExitSpringLoadedModeRunnable = null;
        }

        if (toState == State.APPS) {
            mStateTransitionAnimation.startAnimationToAllApps(mWorkspace.getState(), animated,
                    focusSearchBar);
        } else {
            //lijun modify start for WIDGETS_CONTAINER_PAGE
//            mStateTransitionAnimation.startAnimationToWidgets(mWorkspace.getState(), animated);
            if(FeatureFlags.WIDGETS_CONTAINER_PAGE) {
                mStateTransitionAnimation.startAnimationBetweenOverviewAndOverviewHiden(Workspace.State.OVERVIEW_HIDDEN,toState, animated);
            }else {
                mStateTransitionAnimation.startAnimationToWidgets(mWorkspace.getState(), animated);
            }
            //lijun modify end
        }

        // Change the state *after* we've called all the transition code
        mState = toState;

        // Pause the auto-advance of widgets until we are out of AllApps
        mUserPresent = false;
        updateAutoAdvanceState();
        closeFolder();
        closeShortcutsContainer();

        // Send an accessibility event to announce the context change
        getWindow().getDecorView()
                .sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        return true;
    }

    /**
     * Updates the workspace and interaction state on state change, and return the animation to this
     * new state.
     */
    public Animator startWorkspaceStateChangeAnimation(Workspace.State toState,
            boolean animated, HashMap<View, Integer> layerViews) {
        Workspace.State fromState = mWorkspace.getState();
        Animator anim = mWorkspace.setStateWithAnimation(toState, animated, layerViews);
        updateInteraction(fromState, toState);
        return anim;
    }

    public void enterSpringLoadedDragMode() {
        if (LOGD) Log.d(TAG, String.format("enterSpringLoadedDragMode [mState=%s", mState.name()));
        if (isStateSpringLoaded()) {
            return;
        }

        //lijun modify for widgetmode
//        mStateTransitionAnimation.startAnimationToWorkspace(mState, mWorkspace.getState(),
//                Workspace.State.SPRING_LOADED, true /* animated */,
//                null /* onCompleteRunnable */);
        if (isWidgetsViewVisible()) {
            mWorkspace.setmState(Workspace.State.SPRING_LOADED);
        } else {
            mStateTransitionAnimation.startAnimationToWorkspace(mState, mWorkspace.getState(),
                    Workspace.State.SPRING_LOADED, true /* animated */,
                    null /* onCompleteRunnable */);
        }
        //lijun modify end
        if (isAppsViewVisible()) {
            mState = State.APPS_SPRING_LOADED;
        } else if (isWidgetsViewVisible()) {
            mState = State.WIDGETS_SPRING_LOADED;
        } else if (!FeatureFlags.LAUNCHER3_LEGACY_WORKSPACE_DND) {
            mState = State.WORKSPACE_SPRING_LOADED;
        } else {
            mState = State.WORKSPACE;
        }
    }
    public void enterSpringLoadedDragMode(Runnable onCompleteRunnable) {
        if (LOGD) Log.d(TAG, String.format("enterSpringLoadedDragMode [mState=%s", mState.name()));
        if (isStateSpringLoaded()) {
            return;
        }

        //lijun modify for widgetmode
//        mStateTransitionAnimation.startAnimationToWorkspace(mState, mWorkspace.getState(),
//                Workspace.State.SPRING_LOADED, true /* animated */,
//                null /* onCompleteRunnable */);
        if (isWidgetsViewVisible()) {
            mWorkspace.setmState(Workspace.State.SPRING_LOADED);
        } else {
            mStateTransitionAnimation.startAnimationToWorkspace(mState, mWorkspace.getState(),
                    Workspace.State.SPRING_LOADED, true /* animated */,
                    onCompleteRunnable /* onCompleteRunnable */);
        }
        //lijun modify end
        if (isAppsViewVisible()) {
            mState = State.APPS_SPRING_LOADED;
        } else if (isWidgetsViewVisible()) {
            mState = State.WIDGETS_SPRING_LOADED;
        } else if (!FeatureFlags.LAUNCHER3_LEGACY_WORKSPACE_DND) {
            mState = State.WORKSPACE_SPRING_LOADED;
        } else {
            mState = State.WORKSPACE;
        }
    }
    public void exitSpringLoadedDragModeDelayed(final boolean successfulDrop, int delay,
            final Runnable onCompleteRunnable) {
        if (!isStateSpringLoaded()) return;

        //lijun add for widgetmode
        if(isWidgetsViewVisible()){
            mWorkspace.setmState(Workspace.State.OVERVIEW_HIDDEN);
        }
        //lijun add end

        if (mExitSpringLoadedModeRunnable != null) {
            mHandler.removeCallbacks(mExitSpringLoadedModeRunnable);
        }
        mExitSpringLoadedModeRunnable = new Runnable() {
            @Override
            public void run() {
                if (successfulDrop && !isWidgetsViewVisible()) {//lijun add  && !isWidgetsViewVisible() for widgetmode
                    // TODO(hyunyoungs): verify if this hack is still needed, if not, delete.
                    //
                    // Before we show workspace, hide all apps again because
                    // exitSpringLoadedDragMode made it visible. This is a bit hacky; we should
                    // clean up our state transition functions
                    mWidgetsView.setVisibility(View.GONE);
                    showWorkspace(true, onCompleteRunnable);
                } else {
                    exitSpringLoadedDragMode();
                }
                mExitSpringLoadedModeRunnable = null;
            }
        };
        mHandler.postDelayed(mExitSpringLoadedModeRunnable, delay);
    }

    boolean isStateSpringLoaded() {
        return mState == State.WORKSPACE_SPRING_LOADED || mState == State.APPS_SPRING_LOADED
                || mState == State.WIDGETS_SPRING_LOADED;
    }

    void exitSpringLoadedDragMode() {
        if (mState == State.APPS_SPRING_LOADED) {
            showAppsView(true /* animated */,
                    false /* updatePredictedApps */, false /* focusSearchBar */);
        } else if (mState == State.WIDGETS_SPRING_LOADED) {
            //lijun modify
//            showWidgetsView(true, false);
            mState = State.WIDGETS;
            //lijun modify end
        } else if (mState == State.WORKSPACE_SPRING_LOADED) {
            showWorkspace(true);
        }
    }

    /**
     * Updates the set of predicted apps if it hasn't been updated since the last time Launcher was
     * resumed.
     */
    public void tryAndUpdatePredictedApps() {
        if (mLauncherCallbacks != null) {
            List<ComponentKey> apps = mLauncherCallbacks.getPredictedApps();
            if (apps != null) {
                mAppsView.setPredictedApps(apps);
                getUserEventDispatcher().setPredictedApps(apps);
            }
        }
    }

    void lockAllApps() {
        // TODO
    }

    void unlockAllApps() {
        // TODO
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        final boolean result = super.dispatchPopulateAccessibilityEvent(event);
        final List<CharSequence> text = event.getText();
        text.clear();
        // Populate event with a fake title based on the current state.
        if (mState == State.APPS) {
            text.add(getString(R.string.all_apps_button_label));
        } else if (mState == State.WIDGETS) {
            text.add(getString(R.string.widget_button_text));
        } else if (mWorkspace != null) {
            text.add(mWorkspace.getCurrentPageDescription());
        } else {
            text.add(getString(R.string.all_apps_home_button_label));
        }
        return result;
    }

    /**
     * If the activity is currently paused, signal that we need to run the passed Runnable
     * in onResume.
     *
     * This needs to be called from incoming places where resources might have been loaded
     * while the activity is paused. That is because the Configuration (e.g., rotation)  might be
     * wrong when we're not running, and if the activity comes back to what the configuration was
     * when we were paused, activity is not restarted.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     *
     * @return {@code true} if we are currently paused. The caller might be able to skip some work
     */
    @Thunk boolean waitUntilResume(Runnable run, boolean deletePreviousRunnables) {
        if (mPaused) {
            if (LOGD) Log.d(TAG, "Deferring update until onResume");
            if (deletePreviousRunnables) {
                while (mBindOnResumeCallbacks.remove(run)) {
                }
            }
            mBindOnResumeCallbacks.add(run);
            return true;
        } else {
            return false;
        }
    }

    private boolean waitUntilResume(Runnable run) {
        return waitUntilResume(run, false);
    }

    public void addOnResumeCallback(Runnable run) {
        mOnResumeCallbacks.add(run);
    }

    /**
     * If the activity is currently paused, signal that we need to re-run the loader
     * in onResume.
     *
     * This needs to be called from incoming places where resources might have been loaded
     * while we are paused.  That is becaues the Configuration might be wrong
     * when we're not running, and if it comes back to what it was when we
     * were paused, we are not restarted.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     *
     * @return true if we are currently paused.  The caller might be able to
     * skip some work in that case since we will come back again.
     */
    @Override
    public boolean setLoadOnResume() {
        if (mPaused) {
            if (LOGD) Log.d(TAG, "setLoadOnResume");
            mOnResumeNeedsLoad = true;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Implementation of the method from LauncherModel.Callbacks.
     */
    @Override
    public int getCurrentWorkspaceScreen() {
        if (mWorkspace != null) {
            return mWorkspace.getCurrentPage();
        } else {
            return 0;
        }
    }

    /**
     * Clear any pending bind callbacks. This is called when is loader is planning to
     * perform a full rebind from scratch.
     */
    @Override
    public void clearPendingBinds() {
        mBindOnResumeCallbacks.clear();
        if (mPendingExecutor != null) {
            mPendingExecutor.markCompleted();
            mPendingExecutor = null;
        }
    }

    /**
     * Refreshes the shortcuts shown on the workspace.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void startBinding() {
        if (LauncherAppState.PROFILE_STARTUP) {
            Trace.beginSection("Starting page bind");
        }
        setWorkspaceLoading(true);

        // Clear the workspace because it's going to be rebound
        mWorkspace.clearDropTargets();
        mWorkspace.removeAllWorkspaceScreens();

        mWidgetsToAdvance.clear();
        if (mHotseat != null) {
            mHotseat.resetLayout();
        }
        if (LauncherAppState.PROFILE_STARTUP) {
            Trace.endSection();
        }
    }

    @Override
    public void bindScreens(ArrayList<Long> orderedScreenIds) {
        // Make sure the first screen is always at the start.
        if (FeatureFlags.QSB_ON_FIRST_SCREEN &&
                orderedScreenIds.indexOf(Workspace.FIRST_SCREEN_ID) != 0) {
            orderedScreenIds.remove(Workspace.FIRST_SCREEN_ID);
            orderedScreenIds.add(0, Workspace.FIRST_SCREEN_ID);
            mModel.updateWorkspaceScreenOrder(this, orderedScreenIds);
        } else if (!FeatureFlags.QSB_ON_FIRST_SCREEN && orderedScreenIds.isEmpty()) {
            // If there are no screens, we need to have an empty screen
            mWorkspace.addExtraEmptyScreen();
        }
        bindAddScreens(orderedScreenIds);

        // Create the custom content page (this call updates mDefaultScreen which calls
        // setCurrentPage() so ensure that all pages are added before calling this).
        if (hasCustomContentToLeft()) {
            mWorkspace.createCustomContentContainer();
            populateCustomContentContainer();
        }

        // After we have added all the screens, if the wallpaper was locked to the default state,
        // then notify to indicate that it can be released and a proper wallpaper offset can be
        // computed before the next layout
        mWorkspace.unlockWallpaperFromDefaultPageOnNextLayout();
    }

    private void bindAddScreens(ArrayList<Long> orderedScreenIds) {
        int count = orderedScreenIds.size();
        for (int i = 0; i < count; i++) {
            long screenId = orderedScreenIds.get(i);
            if (!FeatureFlags.QSB_ON_FIRST_SCREEN || screenId != Workspace.FIRST_SCREEN_ID) {
                // No need to bind the first screen, as its always bound.
                mWorkspace.insertNewWorkspaceScreenBeforeEmptyScreen(screenId);
            }
        }
    }

    public void bindAppsAdded(final ArrayList<Long> newScreens,
                              final ArrayList<ItemInfo> addNotAnimated,
                              final ArrayList<ItemInfo> addAnimated,
                              final ArrayList<AppInfo> addedApps) {
        Runnable r = new Runnable() {
            public void run() {
                bindAppsAdded(newScreens, addNotAnimated, addAnimated, addedApps);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }

        // Add the new screens
        if (newScreens != null) {
            bindAddScreens(newScreens);
        }

        // We add the items without animation on non-visible pages, and with
        // animations on the new page (which we will try and snap to).
        if (addNotAnimated != null && !addNotAnimated.isEmpty()) {
            bindItems(addNotAnimated, 0,
                    addNotAnimated.size(), false);
        }
        if (addAnimated != null && !addAnimated.isEmpty()) {
            bindItems(addAnimated, 0,
                    addAnimated.size(), true);
        }

        // Remove the extra empty screen
        mWorkspace.removeExtraEmptyScreen(false, false);

        if (addedApps != null && mAppsView != null) {
            mAppsView.addApps(addedApps);
        }
    }

    /**
     * Bind the items start-end from the list.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    @Override
    public void bindItems(final ArrayList<ItemInfo> shortcuts, final int start, final int end,
                          final boolean forceAnimateIcons) {
        Runnable r = new Runnable() {
            public void run() {
                bindItems(shortcuts, start, end, forceAnimateIcons);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }


        // Get the list of added shortcuts and intersect them with the set of shortcuts here
        final AnimatorSet anim = LauncherAnimUtils.createAnimatorSet();
        final Collection<Animator> bounceAnims = new ArrayList<Animator>();
        final boolean animateIcons = forceAnimateIcons && canRunNewAppsAnimation();
        Workspace workspace = mWorkspace;
        long newShortcutsScreenId = -1;
        for (int i = start; i < end; i++) {
            final ItemInfo item = shortcuts.get(i);

            // Short circuit if we are loading dock items for a configuration which has no dock
            if (item.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT &&
                    mHotseat == null) {
                continue;
            }

            final View view;
            switch (item.itemType) {
                case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                case LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT:
                    ShortcutInfo info = (ShortcutInfo) item;

                    view = createShortcut(info);
                    break;
                case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                    view = FolderIcon.fromXml(R.layout.folder_icon, this,
                            (ViewGroup) workspace.getChildAt(workspace.getCurrentPage()),
                            (FolderInfo) item, mIconCache);
                    break;
                default:
                    throw new RuntimeException("Invalid Item Type");
            }

             /*
             * Remove colliding items.
             */
            if (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                CellLayout cl = mWorkspace.getScreenWithId(item.screenId);
                if (cl != null && cl.isOccupied(item.cellX, item.cellY)) {
                    View v = cl.getChildAt(item.cellX, item.cellY);
                    Object tag = v.getTag();
                    String desc = "Collision while binding workspace item: " + item
                            + ". Collides with " + tag;
                    if (ProviderConfig.IS_DOGFOOD_BUILD) {
                        throw (new RuntimeException(desc));
                    } else {
                        Log.d(TAG, desc);
                        LauncherModel.deleteItemFromDatabase(this, item);
                        continue;
                    }
                }
            }
            workspace.addInScreenFromBind(view, item.container, item.screenId, item.cellX,
                    item.cellY, 1, 1);
            if (animateIcons) {
                // Animate all the applications up now
                view.setAlpha(0f);
                view.setScaleX(0f);
                view.setScaleY(0f);
                ValueAnimator animation = createNewAppBounceAnimation(view, i);
                animation.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                         //UnistallMode add by liuzuo begin
                    if(isUninstallMode&&view instanceof UninstallMode ) {
                        UninstallMode uninstallMode = (UninstallMode) view;
                        uninstallMode.showUninstallApp();
                    }
            //UnistallMode add by liuzuo  end
                    }
                });
                bounceAnims.add(animation);
                newShortcutsScreenId = item.screenId;
            }
        }

        if (animateIcons) {
            // Animate to the correct page
            if (newShortcutsScreenId > -1) {
                long currentScreenId = mWorkspace.getScreenIdForPageIndex(mWorkspace.getNextPage());
                final int newScreenIndex = mWorkspace.getPageIndexForScreenId(newShortcutsScreenId);
                final Runnable startBounceAnimRunnable = new Runnable() {
                    public void run() {
                        anim.playTogether(bounceAnims);
                        anim.start();
                    }
                };
                if (newShortcutsScreenId != currentScreenId) {
                    // We post the animation slightly delayed to prevent slowdowns
                    // when we are loading right after we return to launcher.
                    mWorkspace.postDelayed(new Runnable() {
                        public void run() {
                            if (mWorkspace != null) {
                                mWorkspace.snapToPage(newScreenIndex);
                                mWorkspace.postDelayed(startBounceAnimRunnable,
                                        NEW_APPS_ANIMATION_DELAY);
                            }
                        }
                    }, NEW_APPS_PAGE_MOVE_DELAY);
                } else {
                    mWorkspace.postDelayed(startBounceAnimRunnable, NEW_APPS_ANIMATION_DELAY);
                }
            }
        }
        workspace.requestLayout();
    }

    private void bindSafeModeWidget(LauncherAppWidgetInfo item) {
        PendingAppWidgetHostView view = new PendingAppWidgetHostView(this, item, true);
        view.updateIcon(mIconCache);
        view.updateAppWidget(null);
        view.setOnClickListener(this);
        addAppWidgetToWorkspace(view, item, null, false);
        mWorkspace.requestLayout();
    }

    /**
     * Add the views for a widget to the workspace.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindAppWidget(final LauncherAppWidgetInfo item) {
        Runnable r = new Runnable() {
            public void run() {
                bindAppWidget(item);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }

        if (mIsSafeModeEnabled) {
            bindSafeModeWidget(item);
            return;
        }

        final long start = DEBUG_WIDGETS ? SystemClock.uptimeMillis() : 0;
        if (DEBUG_WIDGETS) {
            Log.d(TAG, "bindAppWidget: " + item);
        }

        final LauncherAppWidgetProviderInfo appWidgetInfo;

        if (item.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_PROVIDER_NOT_READY)) {
            // If the provider is not ready, bind as a pending widget.
            appWidgetInfo = null;
        } else if (item.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_ID_NOT_VALID)) {
            // The widget id is not valid. Try to find the widget based on the provider info.
            appWidgetInfo = mAppWidgetManager.findProvider(item.providerName, item.user);
        } else {
            appWidgetInfo = mAppWidgetManager.getLauncherAppWidgetInfo(item.appWidgetId);
        }

        // If the provider is ready, but the width is not yet restored, try to restore it.
        if (!item.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_PROVIDER_NOT_READY) &&
                (item.restoreStatus != LauncherAppWidgetInfo.RESTORE_COMPLETED)) {
            if (appWidgetInfo == null) {
                if (DEBUG_WIDGETS) {
                    Log.d(TAG, "Removing restored widget: id=" + item.appWidgetId
                            + " belongs to component " + item.providerName
                            + ", as the povider is null");
                }
                LauncherModel.deleteItemFromDatabase(this, item);
                return;
            }

            // If we do not have a valid id, try to bind an id.
            if (item.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_ID_NOT_VALID)) {
                if (!item.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_ID_ALLOCATED)) {
                    // Id has not been allocated yet. Allocate a new id.
                    item.appWidgetId = mAppWidgetHost.allocateAppWidgetId();
                    item.restoreStatus |= LauncherAppWidgetInfo.FLAG_ID_ALLOCATED;

                    // Also try to bind the widget. If the bind fails, the user will be shown
                    // a click to setup UI, which will ask for the bind permission.
                    PendingAddWidgetInfo pendingInfo = new PendingAddWidgetInfo(this, appWidgetInfo);
                    pendingInfo.spanX = item.spanX;
                    pendingInfo.spanY = item.spanY;
                    pendingInfo.minSpanX = item.minSpanX;
                    pendingInfo.minSpanY = item.minSpanY;
                    Bundle options = WidgetHostViewLoader.getDefaultOptionsForWidget(this, pendingInfo);

                    boolean isDirectConfig =
                            item.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_DIRECT_CONFIG);
                    if (isDirectConfig && item.bindOptions != null) {
                        Bundle newOptions = item.bindOptions.getExtras();
                        if (options != null) {
                            newOptions.putAll(options);
                        }
                        options = newOptions;
                    }
                    boolean success = mAppWidgetManager.bindAppWidgetIdIfAllowed(
                            item.appWidgetId, appWidgetInfo, options);

                    // We tried to bind once. If we were not able to bind, we would need to
                    // go through the permission dialog, which means we cannot skip the config
                    // activity.
                    item.bindOptions = null;
                    item.restoreStatus &= ~LauncherAppWidgetInfo.FLAG_DIRECT_CONFIG;

                    // Bind succeeded
                    if (success) {
                        // If the widget has a configure activity, it is still needs to set it up,
                        // otherwise the widget is ready to go.
                        item.restoreStatus = (appWidgetInfo.configure == null) || isDirectConfig
                                ? LauncherAppWidgetInfo.RESTORE_COMPLETED
                                : LauncherAppWidgetInfo.FLAG_UI_NOT_READY;
                    }

                    LauncherModel.updateItemInDatabase(this, item);
                }
            } else if (item.hasRestoreFlag(LauncherAppWidgetInfo.FLAG_UI_NOT_READY)
                    && (appWidgetInfo.configure == null)) {
                // The widget was marked as UI not ready, but there is no configure activity to
                // update the UI.
                item.restoreStatus = LauncherAppWidgetInfo.RESTORE_COMPLETED;
                LauncherModel.updateItemInDatabase(this, item);
            }
        }

        if (item.restoreStatus == LauncherAppWidgetInfo.RESTORE_COMPLETED) {
            if (DEBUG_WIDGETS) {
                Log.d(TAG, "bindAppWidget: id=" + item.appWidgetId + " belongs to component "
                        + appWidgetInfo.provider);
            }

            // Verify that we own the widget
            if (appWidgetInfo == null) {
                FileLog.e(TAG, "Removing invalid widget: id=" + item.appWidgetId);
                deleteWidgetInfo(item);
                return;
            }

            item.minSpanX = appWidgetInfo.minSpanX;
            item.minSpanY = appWidgetInfo.minSpanY;
            addAppWidgetToWorkspace(
                    mAppWidgetHost.createView(this, item.appWidgetId, appWidgetInfo),
                    item, appWidgetInfo, false);
        } else {
            PendingAppWidgetHostView view = new PendingAppWidgetHostView(this, item, false);
            view.updateIcon(mIconCache);
            view.updateAppWidget(null);
            view.setOnClickListener(this);
            addAppWidgetToWorkspace(view, item, null, false);
        }
        mWorkspace.requestLayout();

        if (DEBUG_WIDGETS) {
            Log.d(TAG, "bound widget id="+item.appWidgetId+" in "
                    + (SystemClock.uptimeMillis()-start) + "ms");
        }
    }

    /**
     * Restores a pending widget.
     *
     * @param appWidgetId The app widget id
     */
    private LauncherAppWidgetInfo completeRestoreAppWidget(int appWidgetId, int finalRestoreFlag) {
        LauncherAppWidgetHostView view = mWorkspace.getWidgetForAppWidgetId(appWidgetId);
        if ((view == null) || !(view instanceof PendingAppWidgetHostView)) {
            Log.e(TAG, "Widget update called, when the widget no longer exists.");
            return null;
        }

        LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) view.getTag();
        info.restoreStatus = finalRestoreFlag;

        mWorkspace.reinflateWidgetsIfNecessary();
        LauncherModel.updateItemInDatabase(this, info);
        return info;
    }

    public void onPageBoundSynchronously(int page) {
        mSynchronouslyBoundPages.add(page);
    }

    @Override
    public void executeOnNextDraw(ViewOnDrawExecutor executor) {
        if (mPendingExecutor != null) {
            mPendingExecutor.markCompleted();
        }
        mPendingExecutor = executor;
        executor.attachTo(this);
    }

    public void clearPendingExecutor(ViewOnDrawExecutor executor) {
        if (mPendingExecutor == executor) {
            mPendingExecutor = null;
        }
    }

    @Override
    public void finishFirstPageBind(final ViewOnDrawExecutor executor) {
        Runnable r = new Runnable() {
            public void run() {
                finishFirstPageBind(executor);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }

        Runnable onComplete = new Runnable() {
            @Override
            public void run() {
                if (executor != null) {
                    executor.onLoadAnimationCompleted();
                }
            }
        };
        if (mDragLayer.getAlpha() < 1) {
            mDragLayer.animate().alpha(1).withEndAction(onComplete).start();
        } else {
            onComplete.run();
        }
    }

    /**
     * Callback saying that there aren't any more items to bind.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void finishBindingItems() {
        //lijun add for themechanged start
        hideThemeChangingDialog();
        mModel.reloadForThemechanged = false;
        //lijun add for themechanged end
        Runnable r = new Runnable() {
            public void run() {
                finishBindingItems();
            }
        };
        if (waitUntilResume(r)) {
            return;
        }
        if (LauncherAppState.PROFILE_STARTUP) {
            Trace.beginSection("Page bind completed");
        }
        if (mSavedState != null) {
            if (!mWorkspace.hasFocus()) {
                //lijun modify for nullpointexception
                if(mWorkspace.getPageCount() == 0){
                    mWorkspace.requestFocus();
                }else {
                    int indext = getCurrentWorkspaceScreen();
                    if(indext <0 || indext >= mWorkspace.getPageCount()){
                        indext = 0;
                    }
                    mWorkspace.getChildAt(indext).requestFocus();
                }
                //lijun modify end
            }

            mSavedState = null;
        }

        mWorkspace.initDefaultScreen();//lijun add for homebutton
        mWorkspace.restoreInstanceStateForRemainingPages();

        setWorkspaceLoading(false);

        if (mPendingActivityResult != null) {
            handleActivityResult(mPendingActivityResult.requestCode,
                    mPendingActivityResult.resultCode, mPendingActivityResult.data);
            mPendingActivityResult = null;
        }

        InstallShortcutReceiver.disableAndFlushInstallQueue(this);

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.finishBindingItems(false);
        }
        if (LauncherAppState.PROFILE_STARTUP) {
            Trace.endSection();
        }
		getHotseat().initViewCacheList(); // cyl add for hotseat icon center

        if (FeatureFlags.UNREAD_ENABLE) {
            mBadgeController.reloadBadges();//lijun add for unread
        }
    }

    private boolean canRunNewAppsAnimation() {
        long diff = System.currentTimeMillis() - mDragController.getLastGestureUpTime();
        return diff > (NEW_APPS_ANIMATION_INACTIVE_TIMEOUT_SECONDS * 1000);
    }

    private ValueAnimator createNewAppBounceAnimation(View v, int i) {
        ValueAnimator bounceAnim = LauncherAnimUtils.ofViewAlphaAndScale(v, 1, 1, 1);
        bounceAnim.setDuration(InstallShortcutReceiver.NEW_SHORTCUT_BOUNCE_DURATION);
        bounceAnim.setStartDelay(i * InstallShortcutReceiver.NEW_SHORTCUT_STAGGER_DELAY);
        bounceAnim.setInterpolator(new OvershootInterpolator(BOUNCE_ANIMATION_TENSION));
        return bounceAnim;
    }

    public boolean useVerticalBarLayout() {
        return mDeviceProfile.isVerticalBarLayout();
    }

    public int getSearchBarHeight() {
        if (mLauncherCallbacks != null) {
            return mLauncherCallbacks.getSearchBarHeight();
        }
        return LauncherCallbacks.SEARCH_BAR_HEIGHT_NORMAL;
    }

    /**
     * A runnable that we can dequeue and re-enqueue when all applications are bound (to prevent
     * multiple calls to bind the same list.)
     */
    @Thunk ArrayList<AppInfo> mTmpAppsList;
    private Runnable mBindAllApplicationsRunnable = new Runnable() {
        public void run() {
            bindAllApplications(mTmpAppsList);
            mTmpAppsList = null;
        }
    };

    /**
     * Add the icons for all apps.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindAllApplications(final ArrayList<AppInfo> apps) {
        if (waitUntilResume(mBindAllApplicationsRunnable, true)) {
            mTmpAppsList = apps;
            return;
        }

        if (mAppsView != null) {
            mAppsView.setApps(apps);
        }
        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.bindAllApplications(apps);
        }
    }

    /**
     * Copies LauncherModel's map of activities to shortcut ids to Launcher's. This is necessary
     * because LauncherModel's map is updated in the background, while Launcher runs on the UI.
     */
    @Override
    public void bindDeepShortcutMap(MultiHashMap<ComponentKey, String> deepShortcutMapCopy) {
        mDeepShortcutMap = deepShortcutMapCopy;
        if (LOGD) Log.d(TAG, "bindDeepShortcutMap: " + mDeepShortcutMap);
    }

    public List<String> getShortcutIdsForItem(ItemInfo info) {
        if (!DeepShortcutManager.supportsShortcuts(info)) {
            return Collections.EMPTY_LIST;
        }
        ComponentName component = info.getTargetComponent();
        if (component == null) {
            return Collections.EMPTY_LIST;
        }

        List<String> ids = mDeepShortcutMap.get(new ComponentKey(component, info.user));
        return ids == null ? Collections.EMPTY_LIST : ids;
    }

    /**
     * A package was updated.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindAppsUpdated(final ArrayList<AppInfo> apps) {
        Runnable r = new Runnable() {
            public void run() {
                bindAppsUpdated(apps);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }

        if (mAppsView != null) {
            mAppsView.updateApps(apps);
        }
    }

    @Override
    public void bindWidgetsRestored(final ArrayList<LauncherAppWidgetInfo> widgets) {
        Runnable r = new Runnable() {
            public void run() {
                bindWidgetsRestored(widgets);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }
        mWorkspace.widgetsRestored(widgets);
    }

    /**
     * Some shortcuts were updated in the background.
     * Implementation of the method from LauncherModel.Callbacks.
     *
     * @param updated list of shortcuts which have changed.
     * @param removed list of shortcuts which were deleted in the background. This can happen when
     *                an app gets removed from the system or some of its components are no longer
     *                available.
     */
    @Override
    public void bindShortcutsChanged(final ArrayList<ShortcutInfo> updated,
            final ArrayList<ShortcutInfo> removed, final UserHandleCompat user) {
        Runnable r = new Runnable() {
            public void run() {
                bindShortcutsChanged(updated, removed, user);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }

        if (!updated.isEmpty()) {
            mWorkspace.updateShortcuts(updated);
        }

        if (!removed.isEmpty()) {
            HashSet<ComponentName> removedComponents = new HashSet<>();
            HashSet<ShortcutKey> removedDeepShortcuts = new HashSet<>();

            for (ShortcutInfo si : removed) {
                if (si.itemType == Favorites.ITEM_TYPE_DEEP_SHORTCUT) {
                    removedDeepShortcuts.add(ShortcutKey.fromShortcutInfo(si));
                } else {
                    removedComponents.add(si.getTargetComponent());
                }
            }

            if (!removedComponents.isEmpty()) {
                ItemInfoMatcher matcher = ItemInfoMatcher.ofComponents(removedComponents, user);
                mWorkspace.removeItemsByMatcher(matcher);
                mDragController.onAppsRemoved(matcher);
            }

            if (!removedDeepShortcuts.isEmpty()) {
                ItemInfoMatcher matcher = ItemInfoMatcher.ofShortcutKeys(removedDeepShortcuts);
                mWorkspace.removeItemsByMatcher(matcher);
                mDragController.onAppsRemoved(matcher);
            }
        }
    }

    /**
     * Update the state of a package, typically related to install state.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    @Override
    public void bindRestoreItemsChange(final HashSet<ItemInfo> updates) {
        Runnable r = new Runnable() {
            public void run() {
                bindRestoreItemsChange(updates);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }

        mWorkspace.updateRestoreItems(updates);
    }

    /**
     * A package was uninstalled/updated.  We take both the super set of packageNames
     * in addition to specific applications to remove, the reason being that
     * this can be called when a package is updated as well.  In that scenario,
     * we only remove specific components from the workspace and hotseat, where as
     * package-removal should clear all items by package name.
     */
    @Override
    public void bindWorkspaceComponentsRemoved(
            final HashSet<String> packageNames, final HashSet<ComponentName> components,
            final UserHandleCompat user) {
        Runnable r = new Runnable() {
            public void run() {
                bindWorkspaceComponentsRemoved(packageNames, components, user);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }
        if (!packageNames.isEmpty()) {
            ItemInfoMatcher matcher = ItemInfoMatcher.ofPackages(packageNames, user);
            mWorkspace.removeItemsByMatcher(matcher);
            mDragController.onAppsRemoved(matcher);
            mNavigationbar.removeItemsByMatcher(matcher);

        }
        if (!components.isEmpty()) {
            ItemInfoMatcher matcher = ItemInfoMatcher.ofComponents(components, user);
            mWorkspace.removeItemsByMatcher(matcher);
            mDragController.onAppsRemoved(matcher);
            mNavigationbar.removeItemsByMatcher(matcher);
        }
    }

    @Override
    public void bindAppInfosRemoved(final ArrayList<AppInfo> appInfos) {
        Runnable r = new Runnable() {
            public void run() {
                bindAppInfosRemoved(appInfos);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }

        // Update AllApps
        if (mAppsView != null) {
            mAppsView.removeApps(appInfos);
        }

    }

    private Runnable mBindWidgetModelRunnable = new Runnable() {
            public void run() {
                bindWidgetsModel(mWidgetsModel);
            }
        };

    @Override
    public void bindWidgetsModel(WidgetsModel model) {
        if (waitUntilResume(mBindWidgetModelRunnable, true)) {
            mWidgetsModel = model;
            return;
        }

        if (mWidgetsView != null && model != null) {
            mWidgetsView.addWidgets(model);
            mWidgetsModel = null;
        }
    }

    @Override
    public void notifyWidgetProvidersChanged() {
        if (mWorkspace.getState().shouldUpdateWidget) {
            mModel.refreshAndBindWidgetsAndShortcuts(this, mWidgetsView.isEmpty());
        }
    }

    private int mapConfigurationOriActivityInfoOri(int configOri) {
        final Display d = getWindowManager().getDefaultDisplay();
        int naturalOri = Configuration.ORIENTATION_LANDSCAPE;
        switch (d.getRotation()) {
        case Surface.ROTATION_0:
        case Surface.ROTATION_180:
            // We are currently in the same basic orientation as the natural orientation
            naturalOri = configOri;
            break;
        case Surface.ROTATION_90:
        case Surface.ROTATION_270:
            // We are currently in the other basic orientation to the natural orientation
            naturalOri = (configOri == Configuration.ORIENTATION_LANDSCAPE) ?
                    Configuration.ORIENTATION_PORTRAIT : Configuration.ORIENTATION_LANDSCAPE;
            break;
        }

        int[] oriMap = {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
                ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT,
                ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        };
        // Since the map starts at portrait, we need to offset if this device's natural orientation
        // is landscape.
        int indexOffset = 0;
        if (naturalOri == Configuration.ORIENTATION_LANDSCAPE) {
            indexOffset = 1;
        }
        return oriMap[(d.getRotation() + indexOffset) % 4];
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void lockScreenOrientation() {
        if (mRotationEnabled) {
            if (Utilities.ATLEAST_JB_MR2) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            } else {
                setRequestedOrientation(mapConfigurationOriActivityInfoOri(getResources()
                        .getConfiguration().orientation));
            }
        }
    }

    public void unlockScreenOrientation(boolean immediate) {
        if (mRotationEnabled) {
            if (immediate) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            } else {
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                    }
                }, RESTORE_SCREEN_ORIENTATION_DELAY);
            }
        }
    }

    private void markAppsViewShown() {
        if (mSharedPrefs.getBoolean(APPS_VIEW_SHOWN, false)) {
            return;
        }
        mSharedPrefs.edit().putBoolean(APPS_VIEW_SHOWN, true).apply();
    }

    private boolean shouldShowDiscoveryBounce() {
        if (mState != mState.WORKSPACE) {
            return false;
        }
        if (mLauncherCallbacks != null && mLauncherCallbacks.shouldShowDiscoveryBounce()) {
            return true;
        }
        if (!mIsResumeFromActionScreenOff) {
            return false;
        }
        if (mSharedPrefs.getBoolean(APPS_VIEW_SHOWN, false)) {
            return false;
        }
        return true;
    }

    // TODO: These method should be a part of LauncherSearchCallback
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ItemInfo createAppDragInfo(Intent appLaunchIntent) {
        // Called from search suggestion
        UserHandleCompat user = null;
        if (Utilities.ATLEAST_LOLLIPOP) {
            UserHandle userHandle = appLaunchIntent.getParcelableExtra(Intent.EXTRA_USER);
            if (userHandle != null) {
                user = UserHandleCompat.fromUser(userHandle);
            }
        }
        return createAppDragInfo(appLaunchIntent, user);
    }

    // TODO: This method should be a part of LauncherSearchCallback
    public ItemInfo createAppDragInfo(Intent intent, UserHandleCompat user) {
        if (user == null) {
            user = UserHandleCompat.myUserHandle();
        }

        // Called from search suggestion, add the profile extra to the intent to ensure that we
        // can launch it correctly
        LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(this);
        LauncherActivityInfoCompat activityInfo = launcherApps.resolveActivity(intent, user);
        if (activityInfo == null) {
            return null;
        }
        return new AppInfo(this, activityInfo, user, mIconCache);
    }

    // TODO: This method should be a part of LauncherSearchCallback
    public ItemInfo createShortcutDragInfo(Intent shortcutIntent, CharSequence caption,
            Bitmap icon) {
        return new ShortcutInfo(shortcutIntent, caption, caption, icon,
                UserHandleCompat.myUserHandle());
    }

    protected void moveWorkspaceToDefaultScreen() {
        mWorkspace.moveToDefaultScreen(false);
    }

    /**
     * Returns a FastBitmapDrawable with the icon, accurately sized.
     */
    public FastBitmapDrawable createIconDrawable(Bitmap icon) {
        FastBitmapDrawable d = new FastBitmapDrawable(icon);
        d.setFilterBitmap(true);
        resizeIconDrawable(d);
        return d;
    }

    /**
     * Resizes an icon drawable to the correct icon size.
     */
    public Drawable resizeIconDrawable(Drawable icon) {
        icon.setBounds(0, 0, mDeviceProfile.iconSizePx, mDeviceProfile.iconSizePx);
        return icon;
    }

    /**
     * Prints out out state for debugging.
     */
    public void dumpState() {
        Log.d(TAG, "BEGIN launcher3 dump state for launcher " + this);
        Log.d(TAG, "mSavedState=" + mSavedState);
        Log.d(TAG, "mWorkspaceLoading=" + mWorkspaceLoading);
        Log.d(TAG, "mPendingRequestArgs=" + mPendingRequestArgs);
        Log.d(TAG, "mPendingActivityResult=" + mPendingActivityResult);
        mModel.dumpState();
        // TODO(hyunyoungs): add mWidgetsView.dumpState(); or mWidgetsModel.dumpState();

        Log.d(TAG, "END launcher3 dump state");
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);
        // Dump workspace
        writer.println(prefix + "Workspace Items");
        for (int i = mWorkspace.numCustomPages(); i < mWorkspace.getPageCount(); i++) {
            writer.println(prefix + "  Homescreen " + i);

            ViewGroup layout = ((CellLayout) mWorkspace.getPageAt(i)).getShortcutsAndWidgets();
            for (int j = 0; j < layout.getChildCount(); j++) {
                Object tag = layout.getChildAt(j).getTag();
                if (tag != null) {
                    writer.println(prefix + "    " + tag.toString());
                }
            }
        }

        writer.println(prefix + "  Hotseat");
        ViewGroup layout = mHotseat.getLayout().getShortcutsAndWidgets();
        for (int j = 0; j < layout.getChildCount(); j++) {
            Object tag = layout.getChildAt(j).getTag();
            if (tag != null) {
                writer.println(prefix + "    " + tag.toString());
            }
        }

        try {
            FileLog.flushAll(writer);
        } catch (Exception e) {
            // Ignore
        }

        if (mLauncherCallbacks != null) {
            mLauncherCallbacks.dump(prefix, fd, writer, args);
        }
    }

    public static CustomAppWidget getCustomAppWidget(String name) {
        return sCustomAppWidgets.get(name);
    }

    public static HashMap<String, CustomAppWidget> getCustomAppWidgets() {
        return sCustomAppWidgets;
    }

    public static List<View> getFolderContents(View icon) {
        if (icon instanceof FolderIcon) {
            return ((FolderIcon) icon).getFolder().getItemsInReadingOrder();
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    public static Launcher getLauncher(Context context) {
        if (context instanceof Launcher) {
            return (Launcher) context;
        }
        return ((Launcher) ((ContextWrapper) context).getBaseContext());
    }

    private class RotationPrefChangeHandler implements OnSharedPreferenceChangeListener, Runnable {

        @Override
        public void onSharedPreferenceChanged(
                SharedPreferences sharedPreferences, String key) {
            if (Utilities.ALLOW_ROTATION_PREFERENCE_KEY.equals(key)) {
                mRotationEnabled = Utilities.isAllowRotationPrefEnabled(getApplicationContext());
                if (!waitUntilResume(this, true)) {
                    run();
                }
            }
        }

        @Override
        public void run() {
            setOrientation();
        }
    }

    //lijun add start
    AnimatorSet mPageIndicatorDiagitAnimator = null;
    public void showPageIndicatorDiagital(int index){
        if(FeatureFlags.SHOW_PAGEINDICATOR_CUBE && mPageIndicatorDiagital != null){
            mPageIndicatorDiagital.setIndicatorIndex(index);
            if(mPageIndicatorDiagital.getAlpha()>0.01f){
                mPageIndicatorDiagital.setVisibility(View.VISIBLE);
                mPageIndicatorDiagital.setAlpha(1.0f);
                mPageIndicatorDiagital.invalidate();
                if(mPageIndicatorDiagitAnimator!=null) {
                    mPageIndicatorDiagitAnimator.cancel();
                    mPageIndicatorDiagitAnimator = null;
                }
            }
            LauncherViewPropertyAnimator animation = new LauncherViewPropertyAnimator(mPageIndicatorDiagital);
            animation.alpha(1.0f);
            animation.setDuration(400);
            animation.addListener(new AlphaUpdateListener(mPageIndicatorDiagital, false));
            mPageIndicatorDiagitAnimator = LauncherAnimUtils.createAnimatorSet();
            mPageIndicatorDiagitAnimator.play(animation);
            mPageIndicatorDiagitAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    mPageIndicatorDiagitAnimator = null;
                }
            });
            mPageIndicatorDiagitAnimator.start();
        }
    }
    public void snapToPageIndicatorDiagital(int index){
        if(FeatureFlags.SHOW_PAGEINDICATOR_CUBE && mPageIndicatorDiagital != null){
            mPageIndicatorDiagital.setIndicatorIndex(index);
            mPageIndicatorDiagital.invalidate();
        }
    }
    public void hidePageIndicatorDiagital(){
        if(FeatureFlags.SHOW_PAGEINDICATOR_CUBE && mPageIndicatorDiagital != null){
            if(mPageIndicatorDiagitAnimator!=null){
                mPageIndicatorDiagitAnimator.cancel();
                mPageIndicatorDiagitAnimator = null;
            }
            if (mPageIndicatorDiagital.getAlpha() < 0.01f || mPageIndicatorDiagital.getVisibility() != View.VISIBLE) {
                return;
            }
            LauncherViewPropertyAnimator animation = new LauncherViewPropertyAnimator(mPageIndicatorDiagital);
            animation.alpha(0.0f);
            animation.setDuration(400);
            animation.addListener(new AlphaUpdateListener(mPageIndicatorDiagital, false));
            mPageIndicatorDiagitAnimator = LauncherAnimUtils.createAnimatorSet();
            mPageIndicatorDiagitAnimator.play(animation);
            mPageIndicatorDiagitAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    mPageIndicatorDiagitAnimator = null;
                }
            });
            mPageIndicatorDiagitAnimator.start();
        }
    }
    //lijun add end

    //liuzuo : add for folder addIcon begin
    public void enterImportMode(FolderIcon icon, FolderInfo info){
        mEditFolderIcon = icon;
        mEditFolderInfo = info;
        if(isUninstallMode){
             mWorkspace.exitUninstallMode();
        }
        showImportMode(true);
    }

    private void showImportMode(boolean animated) {
        enterLauncherState(State.FOLDER_IMPORT,Workspace.State.FOLDER_IMPORT);
    }
    private void enterLauncherState(State launcherState,Workspace.State toWorkspaceState) {
        if (LOGD) Log.d(TAG, String.format("enterNormalDrag [mState=%s", mState.name()));
        if (mState == State.APPS || mState == State.WIDGETS || mState == State.WALLPAPER || mState == State.SPECIALEFFECT) {//lijun add WALLPAPER
            return;
        }
        mStateTransitionAnimation.startAnimationToWorkspace(mState, mWorkspace.getState(),
                toWorkspaceState,
                /*WorkspaceStateTransitionAnimation.SCROLL_TO_CURRENT_PAGE,*/ true /* animated */,
                null /* onCompleteRunnable */);
        mState = launcherState;
    }
    private void animationCloseFolder(Folder folder) {
        //mHotseat.setVisibility(View.INVISIBLE);
        //  mFolderImportHintContainer.setVisibility(View.VISIBLE);
        //mFolderImportContainer.setVisibility(View.VISIBLE);
        if(animImportButton!=null)
            animImportButton.cancel();
        animImportButton= showImportButton();
        animImportButton.start();
        // mFolderImportContainer.setTranslationY(mFolderImportContainer.getHeight());
        if(mEditFolderIcon!=null)
            mEditFolderIcon.setAlpha(0.4f);
    }
    private void showFolderIcon(){
        if(mEditFolderIcon != null && mEditFolderIcon.getAlpha() < 1.0f) {
            mEditFolderIcon.setAlpha(1.0f);
        }
    }

    private void exitEditModeAndOpenFolder() {
        clearFolderShortcut();
        if(mOpenFolder && mEditFolderIcon != null ){
            int destScreen = (int) mEditFolderIcon.getFolderInfo().screenId;
            int srcScreen = mWorkspace.getCurrentPage();
            int screenIndex=mWorkspace.getPageIndexForScreenId(destScreen);
            if(destScreen != srcScreen && mEditFolderIcon.getParent() != null
                    && !isHotseatLayout((View)mEditFolderIcon.getParent().getParent())){
                int duration =getResources().getInteger(R.integer.folder_snap_to_page_duration) ;
                mWorkspace.snapToPage(screenIndex, duration, new Runnable() {
                    @Override
                    public void run() {
                        handleFolderClick(mEditFolderIcon);
                        hideImportMode();
                        mEditFolderIcon = null;
                        mWorkspace.stripEmptyScreens();
                    }
                });
            } else {
                hideImportMode();
                handleFolderClick(mEditFolderIcon);
                mEditFolderIcon = null;
                mWorkspace.stripEmptyScreens();
            }
            mOpenFolder=false;
        } else {
            hideImportMode();
            mEditFolderIcon = null;
        }
        isSuccessAddIcon = true;
        exitImportMode(true);
    }
    private void exitEditModeAndCloseFolder() {
        clearFolderShortcut();
        mWorkspace.stripEmptyScreens();
        if(mOpenFolder && mEditFolderIcon != null ){
            int destScreen = (int) mEditFolderIcon.getFolderInfo().screenId;
            int srcScreen = mWorkspace.getCurrentPage();
            int screenIndex=mWorkspace.getPageIndexForScreenId(destScreen);
            Log.d(TAG,"screenIndex="+screenIndex+" destScreen="+destScreen);
            if(destScreen != srcScreen && mEditFolderIcon.getParent() != null
                    && !isHotseatLayout((View)mEditFolderIcon.getParent().getParent())){
                int duration =getResources().getInteger(R.integer.folder_snap_to_page_duration) ;
                mWorkspace.snapToPage(screenIndex, duration, new Runnable() {
                    @Override
                    public void run() {
                        hideImportMode();
                        closeFolder(false);
                        if(mEditFolderIcon.mFolder!=null)
                        mEditFolderIcon.mFolder.close(false);
                        mEditFolderIcon = null;
                    }
                });
            } else {
                hideImportMode();
                closeFolder(false);
                if(mEditFolderIcon!=null&&mEditFolderIcon.mFolder!=null)
                mEditFolderIcon.mFolder.close(false);
                mEditFolderIcon = null;
            }
            mOpenFolder=false;
        } else {
            hideImportMode();
            mEditFolderIcon = null;
        }
        exitImportMode(true);

    }
    public boolean isSuccessAddIcon() {
        return isSuccessAddIcon;
    }
    private void clearFolderShortcut() {
        if(mCheckedBubbleTextViews != null) {
            for(int i=0;i<mCheckedBubbleTextViews.size();i++){
                mCheckedBubbleTextViews.get(i).setChecked(false);
            }
            mCheckedBubbleTextViews.clear();
        }
        if (mCheckedShortcutInfos!=null)
            mCheckedShortcutInfos.clear();
        Iterator<FolderInfo> iterator = mCheckedFolderInfos.iterator();
        while(iterator.hasNext()){
            FolderInfo info = iterator.next();
            info.clearInfo();
        }
        Iterator<FolderIcon> iteratorIcon = mCheckedFolderIcons.iterator();
        while(iteratorIcon.hasNext()){
            FolderIcon icon = iteratorIcon.next();
            icon.onClearIconInfo();
        }
    }

    private void handleFolderClick(FolderIcon editFolderIcon) {
        if(editFolderIcon==null)
            return;
        final FolderInfo info = editFolderIcon.getFolderInfo();
        Folder openFolder = mWorkspace.getFolderForTag(info);
        editFolderIcon.getFolder().mSnapToLastpage=true;
        // If the folder info reports that the associated folder is open, then
        // verify that
        // it is actually opened. There have been a few instances where this
        // gets out of sync.
        if (info.opened && openFolder == null) {
            info.opened = false;
        }

        if (!info.opened && !editFolderIcon.getFolder().isDestroyed()) {
            // Close any open folder
            closeFolder();
            // Open the requested folder
            openFolder(editFolderIcon);
        } else {
            // Find the open folder...
            int folderScreen;
            if (openFolder != null) {
                folderScreen = mWorkspace.getPageForView(openFolder);
                // .. and close it
                closeFolder(openFolder,false);
                if (folderScreen != mWorkspace.getCurrentPage()) {
                    // Close any folder open on the current screen
                    closeFolder();
                    // Pull the folder onto this screen
                    openFolder(editFolderIcon);

                }
            }
        }
    }

    private void hideImportMode() {
        if(animImportButton!=null)
            animImportButton.cancel();
        animImportButton= hideImportButton();
        animImportButton.start();
        //mFolderImportContainer.setVisibility(View.INVISIBLE);
    }
    private void setImportMode(boolean b) {
        if(mEditFolderIcon==null) {
            Log.e(TAG, "Launcher setImportMode is error"+ android.util.Log.getStackTraceString(new Throwable()));
            return;
        }
        mEditFolderIcon.mFolder.setImportMode(b);
    }
    public boolean getImportMode(){
        return mEditFolderIcon!=null&&mEditFolderIcon.mFolder.isImportMode();
    }
    public void addCheckedFolderInfo(FolderInfo info){
        if (!mCheckedFolderInfos.contains(info)) {
            mCheckedFolderInfos.add(info);
        }
    }
    public void addCheckedFolderIcon(FolderIcon icon){
        if (!mCheckedFolderIcons.contains(icon)) {
            mCheckedFolderIcons.add(icon);
        }
    }

    public void updateImportButton() {
        mFolderImportButton.setText(getString(R.string.folder_importmode_button,mCheckedShortcutInfos.size()));
        if(mCheckedShortcutInfos.size() > 0){
            mFolderImportButton.getBackground().setAlpha(226);
            mFolderImportButton.setEnabled(true);
        } else {
            mFolderImportButton.getBackground().setAlpha(127);
            mFolderImportButton.setEnabled(false);
        }
    }
    private void exitImportMode(boolean animated) {
        enterLauncherState(State.WORKSPACE,Workspace.State.NORMAL);
        isSuccessAddIcon = false;
    }
    protected Folder getOpenFolder(){
        if(mWorkspace!=null){
            return mWorkspace.getOpenFolder();
        }
        return null;
    }
    private Animator showImportButton() {
        AnimatorSet ain= LauncherAnimUtils.createAnimatorSet();
        ain.play(createImportButtonAnimation(mFolderImportContainer));
        return ain;
    }
    private Animator hideImportButton() {
        long duration = getResources().getInteger(R.integer.folder_blur_background_duration);
        Interpolator ln = new AccelerateDecelerateInterpolator();
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 1.0f, 0f);
        ObjectAnimator oa = LauncherAnimUtils.ofPropertyValuesHolder(
                mFolderImportContainer, alpha);
        oa.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mFolderImportContainer.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mFolderImportContainer.setLayerType(View.LAYER_TYPE_NONE, null);
                if(mFolderImportContainer!=null){
                    //mFolderBlurBackground.setBackground(null);
                    mFolderImportContainer.setVisibility(View.INVISIBLE);
                }
            }
        });
        oa.setInterpolator(ln);
        oa.setDuration(duration);
        oa.setStartDelay(getResources().getInteger(R.integer.folder_import_button_startdelay));
        return oa;
    }
    private Animator createImportButtonAnimation(final View view){
        long duration = getResources().getInteger(R.integer.folder_blur_background_duration);
        Interpolator ln = new AccelerateDecelerateInterpolator();
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 0f, 1f);
        ObjectAnimator oa = LauncherAnimUtils.ofPropertyValuesHolder(
                view, alpha);
        oa.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                view.setVisibility(View.VISIBLE);
                view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                view.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        });
        oa.setDuration(duration);
        oa.setInterpolator(ln);
        oa.setStartDelay(getResources().getInteger(R.integer.folder_import_button_startdelay));
        return oa;
    }
    //liuzuo : add for folder addIcon end

    //liuzuo add the background of workspace when opening folder begin
    private void openOrCloseFolderAnimation(boolean open){
        if(mAniWorkspaceBg!=null&&mAniWorkspaceBg.isRunning()) {
            mAniWorkspaceBg.end();
        }
        if(open){
            Log.d(TAG,"show blur");
            mAniWorkspaceBg = animateShowWorkspaceBg();
        }else {
            Log.d(TAG,"close blur");
            mAniWorkspaceBg=dismissWorkspaceBg();
        }
        mAniWorkspaceBg.start();

    }
    private Animator animateShowWorkspaceBg() {
        clipBlur();
        AnimatorSet animatorSet =LauncherAnimUtils.createAnimatorSet();
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 0f, 1.0f);
        long duration = getResources().getInteger(R.integer.workspace_bg_ani_duration);


        Interpolator ln = new DecelerateInterpolator(1.5f);
        ObjectAnimator oa = LauncherAnimUtils.ofPropertyValuesHolder(
                mWorkspaceBg, alpha);
        oa.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                if(mWorkspaceBg!=null) {
                    mWorkspaceBg.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                }
                showWorkspaceBackground();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if(mWorkspaceBg!=null) {
                    mWorkspaceBg.setLayerType(View.LAYER_TYPE_NONE, null);
                }
                mAniWorkspaceBg = null;
            }
        });
        oa.setDuration(duration);
        oa.setInterpolator(ln);
        animatorSet.play(oa);
        return animatorSet;
    }

    private void clipBlur() {
        DeviceProfile deviceProfile = getDeviceProfile();
        float ratio = (float) deviceProfile.widthPx / deviceProfile.heightPx;

        if (mBlur != null) {
            int height = mBlur.getHeight();
            int width = mBlur.getWidth();
            float bitmapRatio = (float) width / height;
            if (bitmapRatio > ratio) {
                int clipBitmapWidth = (int) (height * ratio);
                int x = 0;
                if (mWorkspace != null) {
                    int pageIndex = mWorkspace.getCurrentPage();
                    int extWidth = (int) (width * (bitmapRatio - ratio));
                    int pageWidth = extWidth / mWorkspace.getPageCount();
                    x = pageIndex * pageWidth;
                }
                try {
                    Bitmap bitmap = Bitmap.createBitmap(mBlur, x, 0, clipBitmapWidth, height);
                    mWorkspaceBg.setImageBitmap(bitmap);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "create  bitmap error");
                }


            }
        }
    }

    private Animator dismissWorkspaceBg() {
        long duration = getResources().getInteger(R.integer.workspace_bg_ani_duration);
        AnimatorSet animatorSet=LauncherAnimUtils.createAnimatorSet();
        Interpolator ln = new AccelerateInterpolator(0.75f);
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 1.0f, 0f);
        ObjectAnimator oa = LauncherAnimUtils.ofPropertyValuesHolder(
                mWorkspaceBg, alpha);
        oa.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mWorkspaceBg.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mWorkspaceBg.setLayerType(View.LAYER_TYPE_NONE, null);
                if(mWorkspaceBg!=null){
                    mWorkspaceBg.setVisibility(View.INVISIBLE);
                }
                mAniWorkspaceBg = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                if(mWorkspaceBg!=null){
                    mWorkspaceBg.setVisibility(View.INVISIBLE);
                }
            }
        });
        oa.setDuration(duration);
        oa.setInterpolator(ln);
        animatorSet.play(oa);
        return animatorSet;
    }

    private void showWorkspaceBackground() {
        if (mWorkspaceBg != null) {
            mWorkspaceBg.setVisibility(View.VISIBLE);
        }

    }
    //liuzuo add the background of workspace when opening folder end

    /**
     * lijun add
     */
    private void checkPermission(){
        List<String> noOkPermissions = new ArrayList<>();

        for (String permission : sAllPermissions) {
            if (ActivityCompat.checkSelfPermission(this,permission) == PackageManager.PERMISSION_DENIED) {
                noOkPermissions.add(permission);
            }
        }
//        if (noOkPermissions.contains(Manifest.permission.READ_EXTERNAL_STORAGE)) {
//            sRWSDCardPermission = false;
//        } else {
//            sRWSDCardPermission = true;
//        }
        if (noOkPermissions.size() <= 0)
            return ;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(noOkPermissions.toArray(new String[noOkPermissions.size()]), REQUEST_PERMISSION_ALL);
        }
    }

    /**
     * lijun add for pinch
     * @param progress
     */
    public void updatePinchProgress(float progress) {
        if (mOverviewPanel != null) {
//            mOverviewPanel.setVisibility(View.VISIBLE);
//            mOverviewPanel.setAlpha(1.0f - progress);
        }
//        if(mHotseat !=null){
//            mHotseat.setVisibility(View.VISIBLE);
//            mHotseat.setAlpha(progress);
//        }
        if (mWorkspace.getPageIndicator() != null) {
            mWorkspace.getPageIndicator().setTranslationY(mWorkspace.getOverviewModeTranslationYNew() * 1.25f * (1.0f - progress));
        }
        float workspaceTranslationY = mWorkspace.getOverviewModeTranslationYNew() * (1.0f - progress);
        mWorkspace.setTranslationY(workspaceTranslationY);

        final int childCount = mWorkspace.getChildCount();
        float childScale = progress * (1f - CellLayout.CELLLAYOUT_CONTENT_SCALE) + CellLayout.CELLLAYOUT_CONTENT_SCALE;
        for (int i = 0; i < childCount; i++) {
            final CellLayout cl = (CellLayout) mWorkspace.getChildAt(i);
            View homeButton = cl.getmHomeButton();
            ShortcutAndWidgetContainer swc = cl.getShortcutsAndWidgets();
            swc.setScaleX(childScale);
            swc.setScaleY(childScale);
            swc.setTranslationY(cl.getContentTranslationY() * (1.0f - progress));
            if (homeButton != null) {
                homeButton.setAlpha(1.0f - progress);
                homeButton.setVisibility((progress == 1.0f) ? View.GONE : View.VISIBLE);
            }
        }
        if (mAlineButton != null) {
            mAlineButton.setAlpha(1 - progress);
            mAlineButton.setVisibility((progress == 1.0f) ? View.GONE : View.VISIBLE);
        }
    }

    //lijun add for wallpaper
    public boolean isWallpaperMode() {
        return (mState == State.WALLPAPER) || (mOnResumeState == State.WALLPAPER && mWallpaperPicker != null);
    }

    //lijun add for wallpaper
    public boolean isWallpaperPanelShowing() {
        if (mWallpaperPicker.getAlpha() > 0.5 && mWallpaperPicker.getVisibility() == View.VISIBLE) {
            return true;
        }
        return false;
    }

    //lijun add for aline icons
    public View getmAlineButton() {
        return mAlineButton;
    }
    Animator alineIconAnima;
    public void aLineButtonAnimateApha(final float apha) {
        if(mWorkspace.getState() != Workspace.State.OVERVIEW){
            return;
        }
        if (mAlineButton != null) {
            if(alineIconAnima!=null){
                alineIconAnima.cancel();
                alineIconAnima = null;
            }
            alineIconAnima = new LauncherViewPropertyAnimator(mAlineButton)
                    .alpha(apha).withLayer();
            alineIconAnima.addListener(new AlphaUpdateListener(mAlineButton,
                    true));
            alineIconAnima.setDuration(300);
            alineIconAnima.start();
        }
    }

    //liuzuo add for get WallPaperBlur
    private void setWallPaperBlur() {
        Drawable d = WallpaperManager.getInstance(this).getDrawable();
        Bitmap bm = null;
        if(d instanceof BitmapDrawable){
            bm = ((BitmapDrawable)d).getBitmap();
        }
        if(mBlur!=null){
            mBlur.recycle();
        }
        mBlur = BlurFactory.blur(this, bm, 0.1f,10);// radius 1~25
        mWorkspaceBg.setImageBitmap(mBlur);
        Log.d(TAG,"setWallPaperBlur");
    }

    /**
     * lijun add for hide statusbar
     * @param gotoFullscreen
     */
    public void fullscreenOrNot(boolean gotoFullscreen) {
        int flags;
        if (gotoFullscreen) {
            windowChangeListener(true);
            if (ColorManager.getInstance().isBlackText()) {
                flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | 0x00000008|0x00004000 | View.INVISIBLE;
            } else {
                flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.INVISIBLE;
            }

        } else {
            if (ColorManager.getInstance().isBlackText()) {
                flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | 0x00000008 |0x00004000| View.SYSTEM_UI_FLAG_VISIBLE;
            } else {
                flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_VISIBLE;
            }
        }
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    /**
     * lijun add for fullscreen
     */
    private void windowChangeListener(boolean listener) {
        View decorView = getWindow().getDecorView();
        if (listener) {
            decorView.setOnSystemUiVisibilityChangeListener
                    (new View.OnSystemUiVisibilityChangeListener() {
                        @Override
                        public void onSystemUiVisibilityChange(int visibility) {
                            if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                                // TODO: The system bars are visible. Make any desired
                                mHandler.removeMessages(FULLSCREEN_DELAY_MSG);
                                if(isFullScreenMode()){
                                    mHandler.sendEmptyMessageDelayed(FULLSCREEN_DELAY_MSG,2000);
                                }
                            } else {
                                // TODO: The system bars are NOT visible. Make any desired
                            }
                        }
                    });
        } else {
            decorView.setOnSystemUiVisibilityChangeListener(null);
        }
    }

    //lijun add for widgets mode can drag and cannot click
    public boolean isEditorMode() {
        if (mWorkspace != null && (mWorkspace.getState() == Workspace.State.OVERVIEW || mWorkspace.getState() == Workspace.State.OVERVIEW_HIDDEN)
                && (mState == State.WORKSPACE || mState == State.WIDGETS || mState == State.WALLPAPER)) {
            return true;
        }
        return false;
    }

    //lijun add for fullscreen
    public boolean isFullScreenMode() {
        if (isOverViewPanaelShowing() || isWallpaperMode() || isArrangeBarShowing() || isWidgetsViewVisible() || isSpecialEffectMode()) {
            return true;
        }
        return false;
    }

    //lijun add for overview panel button click
    public boolean isOverViewPanaelShowing() {
        if (mOverviewPanel.getAlpha() > 0.5 && mOverviewPanel.getVisibility() == View.VISIBLE) {
            return true;
        }
        return false;
    }

    //lijun add
    public boolean isArrangeBarShowing() {
        if (mNavigationbar.getAlpha() > 0.5 && mNavigationbar.getVisibility() == View.VISIBLE) {
            return true;
        }
        return false;
    }

    //lijun add for wallpaper change start
    @Override
    public void onWallpaperChange() {
        setWallPaperBlur();
    }

    @Override
    public void onColorChange(int[] colors) {
        int flags;
        if(mWorkspace.getState() == Workspace.State.OVERVIEW || mWorkspace.getState() == Workspace.State.OVERVIEW_HIDDEN){
            if (ColorManager.getInstance().isBlackText()) {
                flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | 0x00000008 |0x00004000 | View.INVISIBLE;
            } else {
                flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.INVISIBLE;
            }
        }else {
            if (ColorManager.getInstance().isBlackText()) {
                flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | 0x00000008 |0x00004000 | View.SYSTEM_UI_FLAG_VISIBLE;
            } else {
                flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_VISIBLE;
            }
        }
        getWindow().getDecorView().setSystemUiVisibility(flags);
        mWorkspace.onColorChanged(colors);
        mNavigationbar.onColorChanged(colors);
        mHotseat.getLayout().onColorChanged(colors);
        sendBroadcast(new Intent(LauncherClock.UPDATECOLOR));
    }
    //lijun add end

    //Icon Arrange begin
    public ArrangeNavigationBar mNavigationbar;
    public ArrangeNavigationBar getArrangeNavigationBar(){
        return mNavigationbar;
    }
    public boolean isLauncherArrangeMode() {
        return (mState == State.ICONARRANGE);
    }

    private void showArrangeNavigationBar(boolean animated){
        if(mNavigationbar!=null){
            Folder.ICONARRANGING = true;
            mState = State.ICONARRANGE;
            mStateTransitionAnimation.startAnimationBetweenOverviewAndOverviewHiden(Workspace.State.OVERVIEW_HIDDEN,State.ICONARRANGE, animated);
            getDragController().addDropTarget(mNavigationbar);//liuzuo add
        }
    }
    //Icon Arrange end

    //lijun add start
    private Toast mToast;
    public void showToast(int textId) {
        if(mToast == null) {
            mToast = Toast.makeText(this, textId, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(textId);
            mToast.setDuration(Toast.LENGTH_SHORT);
        }
        mToast.show();
    }

    public void cancelToast() {
        if (mToast != null) {
            mToast.cancel();
        }
    }
    //lijun add end


    //liuzuo add for dynamic
    private void updateDynamicStatus(Boolean status) {
        ArrayList<IDynamicIcon> iDynamicIcons = DynamicProvider.getInstance(this).getAllDynamicIcon();
        for (IDynamicIcon icon:iDynamicIcons){
            if(status){
                if(!Utilities.isScreenOn(this)){

                }else {
                    icon.updateDynamicIcon(false);
                }
            }else {
                icon.removeDynamicReceiver();
            }
        }
        Log.d(TAG,"status="+status+"  iDynamicIcons.size="+iDynamicIcons.size());
}

    //lijun add
    public void exitUnInstallNormalMode(){
        exitSpringLoadedDragModeDelayed(true,0,mWorkspace.getmDelayedResizeRunnable());
        mWorkspace.setmDelayedResizeRunnable(null);
        if(mWorkspace!=null) {
            mWorkspace.exitUninstallMode();
            mWorkspace.resetPageIndicatorCube();
            hidePageIndicatorDiagital();

        }
    }

    public boolean isUnInstallMode() {
        return isUninstallMode;
    }
    public boolean isExitImportModeInHomeKey() {
        return exitImportModeInHomeKey;
    }


    //lijun add for theme changed start
    private long startShowProgressTime = 0;
    private long MIN_SHOW_PROGRESS_TIME = 1200;
    public void showThemeChangingDialog() {
        if (mThemeChangedLoadingView == null || mThemeChangedLoadingView.isThemeLoading()) return;
        startShowProgressTime = System.currentTimeMillis();
        mThemeChangedLoadingView.showLoading();
    }

    public void hideThemeChangingDialog() {
        if (mThemeChangedLoadingView == null || !mThemeChangedLoadingView.isThemeLoading()) return;
        long delay = MIN_SHOW_PROGRESS_TIME - (System.currentTimeMillis() - startShowProgressTime);
        if(delay>0){
            mHandler.sendEmptyMessageDelayed(THEME_CHANGED_DELAY_MSG,delay);
        }else {
            mThemeChangedLoadingView.hideLoading();
        }
    }
    public boolean isShowThemeChang(){
        if(mThemeChangedLoadingView == null)return false;
        return mThemeChangedLoadingView.isThemeLoading();
    }
    //lijun add for theme changed end

    //lijun add
    public void checkAndResetForState(State launcherState) {
        Log.d(TAG,"checkAndResetForState");
        boolean needHideWidgets;
        boolean needHideWallpaper;
        boolean needHideIconarrange;
        boolean needHideSpecialEffect;
		
        if (launcherState == State.WIDGETS) {
            needHideWidgets = false;
            needHideWallpaper = isWallpaperPanelShowing();
            needHideIconarrange = isArrangeBarShowing();
			needHideSpecialEffect = isSpecialEffectShowing();
        } else if (launcherState == State.WALLPAPER) {
            needHideWidgets = isWidgetsPanelShowing();
            needHideWallpaper = false;
            needHideIconarrange = isArrangeBarShowing();
			needHideSpecialEffect = isSpecialEffectShowing();
        } else if (launcherState == State.ICONARRANGE) {
            needHideWidgets = isWidgetsPanelShowing();
            needHideWallpaper = isWallpaperPanelShowing();
            needHideIconarrange = false;
			needHideSpecialEffect = isSpecialEffectShowing();
        } else if (launcherState == State.SPECIALEFFECT) {
            needHideWidgets = isWidgetsPanelShowing();
            needHideWallpaper = isWallpaperPanelShowing();
            needHideIconarrange = isArrangeBarShowing();
			needHideSpecialEffect = false;
        } else if (launcherState == State.WORKSPACE) {
            needHideWidgets = isWidgetsPanelShowing();
            needHideWallpaper = isWallpaperPanelShowing();
            needHideIconarrange = isArrangeBarShowing();
			needHideSpecialEffect = isSpecialEffectShowing();
        } else {
            return;
        }
        if (needHideWidgets) {
            getWidgetsPanel().setVisibility(View.INVISIBLE);
            getWidgetsPanel().setAlpha(0);
        }
        if (needHideWallpaper) {
            getmWallpaperPicker().setVisibility(View.INVISIBLE);
            getmWallpaperPicker().setAlpha(0);
        }
        if (needHideIconarrange) {
            getArrangeNavigationBar().setVisibility(View.INVISIBLE);
            getArrangeNavigationBar().setAlpha(0);
            getArrangeNavigationBar().onBackPressed(false);
            Folder.ICONARRANGING = false;
        }
        if (needHideSpecialEffect) {
            mPreviewContainer.setVisibility(View.INVISIBLE);
            mPreviewContainer.setAlpha(0);
        }		
        if(getOpenFolder()!=null&&mAniWorkspaceBg==null&&mWorkspaceBg.getVisibility()==View.VISIBLE){
            openOrCloseFolderAnimation(false);
        }
    }

    //liuzuo add
    private void forceExitImportMode(){
        isMoveToDefaultScreen=false;
        showFolderIcon();
        mOpenFolder =true;
        if(mCheckedBubbleTextViews!=null){
            for (BubbleTextView bv:mCheckedBubbleTextViews
                    ) {
                bv.setChecked(false);
            }
        }
        mEditFolderIcon.mFolder.setImportMode(false);
        mCheckedBubbleTextViews.clear();
        mCheckedShortcutInfos.clear();
        exitImportModeInHomeKey=true;
        exitEditModeAndCloseFolder();
        exitImportModeInHomeKey=false;
    }

    // cyl add for special effect  start
	public Handler getHandler(){
	   return mHandler;
    }

    private void dismissSpecialEffectPreview(){
		mPreviewContainer.setVisibility(View.GONE);
    }

    private void showSpecialEffectPreview(boolean animated){
		mPreviewContainer.initPagedView();
        mState = State.SPECIALEFFECT;
        mStateTransitionAnimation.startAnimationBetweenOverviewAndOverviewHiden(Workspace.State.OVERVIEW_HIDDEN,State.SPECIALEFFECT, animated);
        mPreviewContainer.requestFocus();
    }

   public PreviewContainer getSpecialEffectPreview(){
   	  return mPreviewContainer;
   	}

    public boolean isSpecialEffectMode() {
        return (mState == State.SPECIALEFFECT);
    }

    public boolean isSpecialEffectShowing() {
        if (mPreviewContainer.getAlpha() > 0.5 && mPreviewContainer.getVisibility() == View.VISIBLE) {
            return true;
        }
        return false;
    }	
   // cyl add for special effect  end


    /**
     * lijun add start for unread
     */
    @Override
    public void bindComponentUnreadChanged(final String packageName, final int count, final String shortcutCustomId) {
        if (!FeatureFlags.UNREAD_ENABLE) return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mWorkspace != null) {
                    mWorkspace.updateComponentUnreadChanged(packageName, count, shortcutCustomId);
                }
            }
        });
    }

    @Override
    public void bindWorkspaceUnreadInfo(final ArrayList<BadgeInfo> unreadApps) {
        if (!FeatureFlags.UNREAD_ENABLE) return;
        mHandler.post(new Runnable() {
            public void run() {
                final long start = System.currentTimeMillis();
                if (mWorkspace != null) {
                    mWorkspace.updateShortcutsAndFoldersUnread(unreadApps);
                }
            }
        });
    }

    public ArrayList<BadgeInfo> getUnreadApps(){
        return mBadgeController.getBadges();
    }

    /**
     * lijun add end for unread
     */

    /**
     * liuzuo add  for unread
     */
    public boolean isShowUnread(){
        return !(getWorkspace().getState()==Workspace.State.OVERVIEW||getWorkspace().getState()==Workspace.State.OVERVIEW_HIDDEN||isLauncherArrangeMode());
    }
    /**
     * liuzuo add  for Application of the double open
     */
    private void deleteParallelShortcut(ItemInfo info) {
        if (info != null && info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT) {
            if (info.title != null && info.title.toString().endsWith("+")) {
                Intent i = info.getIntent();
                if (i != null && "com.lbe.parallel.ACTION_LAUNCH_PACKAGE".equals(i.getAction())) {
                    Log.i(TAG, "tzf isParallelShortcut intent = " + i);
                    Intent intent = new Intent("com.vapp.shortcut.deleted");
                    String packageName = i.getStringExtra("EXTRA_LAUNCH_PACKAGE");
                    intent.putExtra("packageName", packageName);
                    this.sendBroadcast(intent);
                }
            }
        }
    }
}
