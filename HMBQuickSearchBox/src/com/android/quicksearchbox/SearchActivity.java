/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.quicksearchbox;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.android.common.Search;
import com.android.quicksearchbox.hotsearch.HotSearchView;
import com.android.quicksearchbox.ui.SearchActivityView;
import com.android.quicksearchbox.ui.SuggestionClickListener;
import com.android.quicksearchbox.ui.SuggestionsAdapter;
import com.android.quicksearchbox.util.Consumer;
import com.android.quicksearchbox.util.Consumers;
import com.android.quicksearchbox.util.RequestPermissionsActivity;
import com.android.quicksearchbox.util.RequestPermissionsActivityBase;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import com.hb.themeicon.theme.IconManager;//lijun add

/**
 * The main activity for Quick Search Box. Shows the search UI.
 *
 */
public class SearchActivity extends Activity {

    private static final boolean DBG = true;
    private static final String TAG = "QSB.SearchActivity";

    private static final String SCHEME_CORPUS = "qsb.corpus";

    public static final String INTENT_ACTION_QSB_AND_SELECT_CORPUS
            = "com.android.quicksearchbox.action.QSB_AND_SELECT_CORPUS";

    private static final String INTENT_EXTRA_TRACE_START_UP = "trace_start_up";

    private static final int REQUEST_PERMISSION_READ_PHONE_STATE = 0;

    // Keys for the saved instance state.
    private static final String INSTANCE_KEY_CORPUS = "corpus";
    private static final String INSTANCE_KEY_QUERY = "query";

    private static final String ACTIVITY_HELP_CONTEXT = "search";

    private boolean mTraceStartUp;
    // Measures time from for last onCreate()/onNewIntent() call.
    private LatencyTracker mStartLatencyTracker;
    // Measures time spent inside onCreate()
    private LatencyTracker mOnCreateTracker;
    private int mOnCreateLatency;
    // Whether QSB is starting. True between the calls to onCreate()/onNewIntent() and onResume().
    private boolean mStarting;
    // True if the user has taken some action, e.g. launching a search, voice search,
    // or suggestions, since QSB was last started.
    private boolean mTookAction;

    private SearchActivityView mSearchActivityView;

    private CorporaObserver mCorporaObserver;

    private Bundle mAppSearchData;

    private final Handler mHandler = new Handler();
    private final Runnable mUpdateSuggestionsTask = new Runnable() {
        public void run() {
            updateSuggestions();
        }
    };

    private final Runnable mShowInputMethodTask = new Runnable() {
        public void run() {
            mSearchActivityView.showInputMethodForQuery();
        }
    };

    private OnDestroyListener mDestroyListener;

    //lijun add for hot search start
    private WebView mWebview;
    private View mWebViewLoadPanel;
    private View mWebViewLoading;
    private View mWebViewLoadError;
    WebViewState mWebViewState = WebViewState.HIDE;
    boolean clearWebviewCaches = false;
    SharedPreferences webviewSharedPreferences;
    private String cookies = "";
    private String lastcookies = "";
    private static final String WEIBO_HOST = "s.weibo.com";
    public enum WebViewState {
        HIDE,LOADING,LOADED_SUCCESS,LOADED_FAILED
    }

    private void requestReadPhonePermission() {
        if (ActivityCompat.checkSelfPermission(this,Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_PERMISSION_READ_PHONE_STATE);
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_READ_PHONE_STATE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    //TODO
                    mSearchActivityView.loadHotsearch();
                }
                break;

            default:
                break;
        }
    }
    //lijun add for hot search end

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (DBG) {  Log.d(TAG, "onCreate()");  }
        super.onCreate(savedInstanceState);
        boolean isIntentFromPermission =
            getIntent().hasExtra(RequestPermissionsActivityBase.IS_FROM_PEMISSION_REQUEST);

        if (!isIntentFromPermission) {
            if (RequestPermissionsActivity.startPermissionActivity(this)) {
                Log.i(TAG, "[onCreate]startPermissionActivity,return.");
                return;
            }
        } else {
            if (DBG) {  Log.d(TAG, "onCreate request from Permissions");  }
        }

        requestReadPhonePermission();//lijun add for hot search

        setTheme(R.style.Theme_QuickSearchBox_Search_HasActionBar);

        mTraceStartUp = getIntent().hasExtra(INTENT_EXTRA_TRACE_START_UP);
        if (mTraceStartUp) {
            String traceFile = new File(getDir("traces", 0), "qsb-start.trace").getAbsolutePath();
            Log.i(TAG, "Writing start-up trace to " + traceFile);
            Debug.startMethodTracing(traceFile);
        }
        recordStartTime();

        // This forces the HTTP request to check the users domain to be
        // sent as early as possible.
        QsbApplication.get(this).getSearchBaseUrlHelper();

        mSearchActivityView = setupContentView();

        if (getConfig().showScrollingSuggestions()) {
            mSearchActivityView.setMaxPromotedSuggestions(getConfig().getMaxPromotedSuggestions());
        } else {
            mSearchActivityView.limitSuggestionsToViewHeight();
        }
        if (getConfig().showScrollingResults()) {
            mSearchActivityView.setMaxPromotedResults(getConfig().getMaxPromotedResults());
        } else {
            mSearchActivityView.limitResultsToViewHeight();
        }

        mSearchActivityView.setSearchClickListener(new SearchActivityView.SearchClickListener() {
            public boolean onSearchClicked(int method) {
                return SearchActivity.this.onSearchClicked(method);
            }
        });

        mSearchActivityView.setQueryListener(new SearchActivityView.QueryListener() {
            public void onQueryChanged() {
                updateSuggestionsBuffered();
            }
        });

        mSearchActivityView.setSuggestionClickListener(new ClickHandler());

        mSearchActivityView.setVoiceSearchButtonClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                onVoiceSearchClicked();
            }
        });

        View.OnClickListener finishOnClick = new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        };
        mSearchActivityView.setExitClickListener(finishOnClick);

        // First get setup from intent
        Intent intent = getIntent();
        setupFromIntent(intent);
        // Then restore any saved instance state
        restoreInstanceState(savedInstanceState);

        // Do this at the end, to avoid updating the list view when setSource()
        // is called.
        mSearchActivityView.start();

        mCorporaObserver = new CorporaObserver();
        setSystemUI();
        getCorpora().registerDataSetObserver(mCorporaObserver);
        recordOnCreateDone();

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.hb.theme.ACTION_THEME_CHANGE");
        registerReceiver(myReceiver,filter);
    }

    private BroadcastReceiver myReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            if("com.hb.theme.ACTION_THEME_CHANGE".equals(intent.getAction())){
                IconManager.getInstance(SearchActivity.this, true, true).clearCaches();
                mSearchActivityView.clearIconCache();
            }
        }
    };

    private void setSystemUI() {
        int flags;
        flags =  View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | 0x00000008 ;
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    /// M:
    private void setActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setCustomView(R.layout.action_bar_custom_view);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        //liuzuo add 
        actionBar.setElevation(0);
    }

    protected SearchActivityView setupContentView() {
        /// M:
        setActionBar();

        setContentView(R.layout.search_activity);

        //lijun add for hot search start
        initWebView();
        //lijun add for hot search end

        return (SearchActivityView) findViewById(R.id.search_activity_view);
    }

    protected SearchActivityView getSearchActivityView() {
        return mSearchActivityView;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (DBG) Log.d(TAG, "onNewIntent()");
        //lijun add start for bug:7839
        if(mSearchActivityView == null){
            finish();
            return;
        }
        //lijun add end
        recordStartTime();
        setIntent(intent);
        setupFromIntent(intent);
    }

    private void recordStartTime() {
        mStartLatencyTracker = new LatencyTracker();
        mOnCreateTracker = new LatencyTracker();
        mStarting = true;
        mTookAction = false;
    }

    private void recordOnCreateDone() {
        mOnCreateLatency = mOnCreateTracker.getLatency();
    }

    protected void restoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState == null) return;
        String corpusName = savedInstanceState.getString(INSTANCE_KEY_CORPUS);
        String query = savedInstanceState.getString(INSTANCE_KEY_QUERY);

        QsbApplication app = QsbApplication.get(SearchActivity.this);
        // This callback will be invoked after make changes in QSB settings.
        // We suppose to check if the current corpus is still enable,
        // otherwise, set the current corpus to ALL.
        if ((null != corpusName)
                && (!app.isCorpusEnabled(corpusName))) {
            if (DBG) Log.d(TAG, "restoreInstanceState(). corpusName = "
                + corpusName + "is no longer enabled.");
            corpusName = null;
        }

        setCorpus(corpusName);
        setQuery(query, false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // We don't save appSearchData, since we always get the value
        // from the intent and the user can't change it.

        outState.putString(INSTANCE_KEY_CORPUS, getCorpusName());
        outState.putString(INSTANCE_KEY_QUERY, getQuery());
    }

    private void setupFromIntent(Intent intent) {
        if (DBG) Log.d(TAG, "setupFromIntent(" + intent.toUri(0) + ")");
        String corpusName = getCorpusNameFromUri(intent.getData());
        String query = intent.getStringExtra(SearchManager.QUERY);
        Bundle appSearchData = intent.getBundleExtra(SearchManager.APP_DATA);
        boolean selectAll = intent.getBooleanExtra(SearchManager.EXTRA_SELECT_QUERY, false);

        setCorpus(corpusName);
        setQuery(query, selectAll);
        mAppSearchData = appSearchData;

        //lijun remove
//        if (startedIntoCorpusSelectionDialog()) {
//            mSearchActivityView.showCorpusSelectionDialog();
//        }
    }

    public boolean startedIntoCorpusSelectionDialog() {
        return INTENT_ACTION_QSB_AND_SELECT_CORPUS.equals(getIntent().getAction());
    }

    /**
     * Removes corpus selector intent action, so that BACK works normally after
     * dismissing and reopening the corpus selector.
     */
    public void clearStartedIntoCorpusSelectionDialog() {
        Intent oldIntent = getIntent();
        if (SearchActivity.INTENT_ACTION_QSB_AND_SELECT_CORPUS.equals(oldIntent.getAction())) {
            Intent newIntent = new Intent(oldIntent);
            newIntent.setAction(SearchManager.INTENT_ACTION_GLOBAL_SEARCH);
            setIntent(newIntent);
        }
    }

    public static Uri getCorpusUri(Corpus corpus) {
        if (corpus == null) return null;
        return new Uri.Builder()
                .scheme(SCHEME_CORPUS)
                .authority(corpus.getName())
                .build();
    }

    private String getCorpusNameFromUri(Uri uri) {
        if (uri == null) return null;
        if (!SCHEME_CORPUS.equals(uri.getScheme())) return null;
        return uri.getAuthority();
    }

    private Corpus getCorpus() {
        return mSearchActivityView.getCorpus();
    }

    private String getCorpusName() {
        return mSearchActivityView.getCorpusName();
    }

    private void setCorpus(String name) {
        mSearchActivityView.setCorpus(name);
    }

    private QsbApplication getQsbApplication() {
        return QsbApplication.get(this);
    }

    private Config getConfig() {
        return getQsbApplication().getConfig();
    }

    protected SearchSettings getSettings() {
        return getQsbApplication().getSettings();
    }

    private Corpora getCorpora() {
        return getQsbApplication().getCorpora();
    }

    private CorpusRanker getCorpusRanker() {
        return getQsbApplication().getCorpusRanker();
    }

    private ShortcutRepository getShortcutRepository() {
        return getQsbApplication().getShortcutRepository();
    }

    private SuggestionsProvider getSuggestionsProvider() {
        return getQsbApplication().getSuggestionsProvider();
    }

    private Logger getLogger() {
        return getQsbApplication().getLogger();
    }

    @VisibleForTesting
    public void setOnDestroyListener(OnDestroyListener l) {
        mDestroyListener = l;
    }

    @Override
    protected void onDestroy() {
        if (DBG) Log.d(TAG, "onDestroy()");

        //lijun add start
        try {
            unregisterReceiver(myReceiver);
        } catch (Exception e) {

        }
        //lijun add end
        if (mCorporaObserver != null) {
            getCorpora().unregisterDataSetObserver(mCorporaObserver);
        }
        if (mSearchActivityView != null) {
            mSearchActivityView.destroy();
        }
        super.onDestroy();
        if (mDestroyListener != null) {
            mDestroyListener.onDestroyed();
        }
    }

    @Override
    protected void onStop() {
        if (DBG) Log.d(TAG, "onStop()");
        if (!mTookAction) {
            // TODO: This gets logged when starting other activities, e.g. by opening the search
            // settings, or clicking a notification in the status bar.
            // TODO we should log both sets of suggestions in 2-pane mode
            getLogger().logExit(getCurrentSuggestions(), getQuery().length());
        }
        // Close all open suggestion cursors. The query will be redone in onResume()
        // if we come back to this activity.
        mSearchActivityView.clearSuggestions();
        getQsbApplication().getShortcutRefresher().reset();
        mSearchActivityView.onStop();
        super.onStop();
    }

    @Override
    protected void onPause() {
        if (DBG) Log.d(TAG, "onPause()");
        mSearchActivityView.onPause();
        super.onPause();
    }

    @Override
    protected void onRestart() {
        if (DBG) Log.d(TAG, "onRestart()");
        super.onRestart();
    }

    @Override
    protected void onResume() {
        if (DBG) Log.d(TAG, "onResume()");
        super.onResume();
        updateSuggestionsBuffered();
        mSearchActivityView.onResume();
        if (mTraceStartUp) Debug.stopMethodTracing();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /// M: moved from onPrepareOptionsMenu, for RTL support
        //liuzuo remove begin
        //createMenuItems(menu, true);
        //liuzuo remove end

        return false;
    }

    public void createMenuItems(Menu menu, boolean showDisabled) {
        getSettings().addMenuItems(menu, showDisabled);
        // as Config.getHelpUrl returns null, so far it does NOT add any menu item
        getQsbApplication().getHelp().addHelpMenuItem(menu, ACTIVITY_HELP_CONTEXT);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        /// M: IME launch again while tap suggestion which comes from File Manager @{
        if (hasFocus && isResumed()) {
        /// @}
            // Launch the IME after a bit
            mHandler.postDelayed(mShowInputMethodTask, 0);
        }
    }

    protected String getQuery() {
        return mSearchActivityView.getQuery();
    }

    protected void setQuery(String query, boolean selectAll) {
        mSearchActivityView.setQuery(query, selectAll);
    }

    public CorpusSelectionDialog getCorpusSelectionDialog() {
        CorpusSelectionDialog dialog = createCorpusSelectionDialog();
        dialog.setOwnerActivity(this);
        dialog.setOnDismissListener(new CorpusSelectorDismissListener());
        return dialog;
    }

    protected CorpusSelectionDialog createCorpusSelectionDialog() {
        return new CorpusSelectionDialog(this, getSettings());
    }

    /**
     * @return true if a search was performed as a result of this click, false otherwise.
     */
    protected boolean onSearchClicked(int method) {
        String query = CharMatcher.WHITESPACE.trimAndCollapseFrom(getQuery(), ' ');
        if (DBG) Log.d(TAG, "Search clicked, query=" + query);

        // Don't do empty queries
        if (TextUtils.getTrimmedLength(query) == 0) return false;

        Corpus searchCorpus = getSearchCorpus();
        if (searchCorpus == null) return false;

        mTookAction = true;

        // Log search start
        getLogger().logSearch(getCorpus(), method, query.length());

        // Start search
        startSearch(searchCorpus, query);

//        getShortcutRepository().reportClickForWebSearch(query);//lijun add
        return true;
    }

    protected void startSearch(Corpus searchCorpus, String query) {
        Intent intent = searchCorpus.createSearchIntent(query, mAppSearchData);
        launchIntent(intent);
    }

    protected void onVoiceSearchClicked() {
        if (DBG) Log.d(TAG, "Voice Search clicked");
        Corpus searchCorpus = getSearchCorpus();
        if (searchCorpus == null) return;

        mTookAction = true;

        // Log voice search start
        getLogger().logVoiceSearch(searchCorpus);

        // Start voice search
        Intent intent = searchCorpus.createVoiceSearchIntent(mAppSearchData);
        launchIntent(intent);
    }

    protected Corpus getSearchCorpus() {
        return mSearchActivityView.getSearchCorpus();
    }

    protected SuggestionCursor getCurrentSuggestions() {
        return mSearchActivityView.getCurrentPromotedSuggestions();
    }

    protected SuggestionPosition getCurrentSuggestions(SuggestionsAdapter<?> adapter, long id) {
        SuggestionPosition pos = adapter.getSuggestion(id);
        if (pos == null) {
            return null;
        }
        SuggestionCursor suggestions = pos.getCursor();
        int position = pos.getPosition();
        if (suggestions == null) {
            return null;
        }
        int count = suggestions.getCount();
        if (position < 0 || position >= count) {
            Log.w(TAG, "Invalid suggestion position " + position + ", count = " + count);
            return null;
        }
        suggestions.moveTo(position);
        return pos;
    }

    protected Set<Corpus> getCurrentIncludedCorpora() {
        Suggestions suggestions = mSearchActivityView.getSuggestions();
        return suggestions == null  ? null : suggestions.getIncludedCorpora();
    }

    protected void launchIntent(Intent intent) {
        if (DBG) Log.d(TAG, "launchIntent " + intent);
        if (intent == null) {
            return;
        }
        try {
            startActivity(intent);
            mSearchActivityView.considerHidingInputMethod();
        } catch (RuntimeException ex) {
            // Since the intents for suggestions specified by suggestion providers,
            // guard against them not being handled, not allowed, etc.
            Log.e(TAG, "Failed to start " + intent.toUri(0), ex);
        }
    }

    private boolean launchSuggestion(SuggestionsAdapter<?> adapter, long id) {
        SuggestionPosition suggestion = getCurrentSuggestions(adapter, id);
        if (suggestion == null) return false;

        //lijun add for click on history
        SuggestionCursor suggestionCursor = suggestion.getCursor();
        if(suggestionCursor instanceof ClickLogCursor){
            setQuery(((ClickLogCursor) suggestionCursor).getCurrentClickText(),false);
            mSearchActivityView.notifyTextChanged();
            return true;
        }
        //lijun add end

        if (DBG) Log.d(TAG, "Launching suggestion " + id);
        mTookAction = true;

        // Log suggestion click
        getLogger().logSuggestionClick(id, suggestion.getCursor(), getCurrentIncludedCorpora(),
                Logger.SUGGESTION_CLICK_TYPE_LAUNCH);

        // Create shortcut
        getShortcutRepository().reportClick(suggestion.getCursor(), suggestion.getPosition());

        // Launch intent
        launchSuggestion(suggestion.getCursor(), suggestion.getPosition());

        return true;
    }

    protected void launchSuggestion(SuggestionCursor suggestions, int position) {
        suggestions.moveTo(position);
        Intent intent = SuggestionUtils.getSuggestionIntent(suggestions, mAppSearchData);
        launchIntent(intent);
    }

    protected void removeFromHistoryClicked(final SuggestionsAdapter<?> adapter,
            final long id) {
        SuggestionPosition suggestion = getCurrentSuggestions(adapter, id);
        if (suggestion == null) return;
        CharSequence title = suggestion.getSuggestionText1();
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(R.string.remove_from_history)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // TODO: what if the suggestions have changed?
                                removeFromHistory(adapter, id);
                            }
                        })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.show();
    }

    protected void removeFromHistory(SuggestionsAdapter<?> adapter, long id) {
        SuggestionPosition suggestion = getCurrentSuggestions(adapter, id);
        if (suggestion == null) return;
        removeFromHistory(suggestion.getCursor(), suggestion.getPosition());
        // TODO: Log to event log?
    }

    protected void removeFromHistory(SuggestionCursor suggestions, int position) {
        removeShortcut(suggestions, position);
        removeFromHistoryDone(true);
    }

    protected void removeFromHistoryDone(boolean ok) {
        Log.i(TAG, "Removed query from history, success=" + ok);
        updateSuggestionsBuffered();
        if (!ok) {
            Toast.makeText(this, R.string.remove_from_history_failed, Toast.LENGTH_SHORT).show();
        }
    }

    protected void removeShortcut(SuggestionCursor suggestions, int position) {
        if (suggestions.isSuggestionShortcut()) {
            if (DBG) Log.d(TAG, "Removing suggestion " + position + " from shortcuts");
            getShortcutRepository().removeFromHistory(suggestions, position);
        }
    }

    protected void clickedQuickContact(SuggestionsAdapter<?> adapter, long id) {
        SuggestionPosition suggestion = getCurrentSuggestions(adapter, id);
        if (suggestion == null) return;

        if (DBG) Log.d(TAG, "Used suggestion " + suggestion.getPosition());
        mTookAction = true;

        // Log suggestion click
        getLogger().logSuggestionClick(id, suggestion.getCursor(), getCurrentIncludedCorpora(),
                Logger.SUGGESTION_CLICK_TYPE_QUICK_CONTACT);

        // Create shortcut
        getShortcutRepository().reportClick(suggestion.getCursor(), suggestion.getPosition());
    }

    protected void refineSuggestion(SuggestionsAdapter<?> adapter, long id) {
        if (DBG) Log.d(TAG, "query refine clicked, pos " + id);
        SuggestionPosition suggestion = getCurrentSuggestions(adapter, id);
        if (suggestion == null) {
            return;
        }
        String query = suggestion.getSuggestionQuery();
        if (TextUtils.isEmpty(query)) {
            return;
        }

        // Log refine click
        getLogger().logSuggestionClick(id, suggestion.getCursor(), getCurrentIncludedCorpora(),
                Logger.SUGGESTION_CLICK_TYPE_REFINE);

        // Put query + space in query text view
        String queryWithSpace = query + ' ';
        setQuery(queryWithSpace, false);
        updateSuggestions();
        mSearchActivityView.focusQueryTextView();
    }

    private void updateSuggestionsBuffered() {
        if (DBG) Log.d(TAG, "updateSuggestionsBuffered()");
        mHandler.removeCallbacks(mUpdateSuggestionsTask);
        long delay = getConfig().getTypingUpdateSuggestionsDelayMillis();
        mHandler.postDelayed(mUpdateSuggestionsTask, delay);
    }

    private void gotSuggestions(Suggestions suggestions) {
        if (mStarting) {
            mStarting = false;
            String source = getIntent().getStringExtra(Search.SOURCE);
            int latency = mStartLatencyTracker.getLatency();
            getLogger().logStart(mOnCreateLatency, latency, source, getCorpus(),
                    suggestions == null ? null : suggestions.getExpectedCorpora());
            getQsbApplication().onStartupComplete();
        }
    }

    private void getCorporaToQuery(Consumer<List<Corpus>> consumer) {
        Corpus corpus = getCorpus();
        if (corpus == null) {
            getCorpusRanker().getCorporaInAll(Consumers.createAsyncConsumer(mHandler, consumer));
        } else {
            List<Corpus> corpora = new ArrayList<Corpus>();
            Corpus searchCorpus = getSearchCorpus();
            if (searchCorpus != null) corpora.add(searchCorpus);
            consumer.consume(corpora);
        }
    }

    protected void getShortcutsForQuery(String query, Collection<Corpus> corporaToQuery,
            final Suggestions suggestions) {
        ShortcutRepository shortcutRepo = getShortcutRepository();
        if (shortcutRepo == null) return;
        if (query.length() == 0 && !getConfig().showShortcutsForZeroQuery()) {
            return;
        }
        Consumer<ShortcutCursor> consumer = Consumers.createAsyncCloseableConsumer(mHandler,
                new Consumer<ShortcutCursor>() {
            public boolean consume(ShortcutCursor shortcuts) {
                suggestions.setShortcuts(shortcuts);
                return true;
            }
        });
        shortcutRepo.getShortcutsForQuery(query, corporaToQuery,
                getSettings().allowWebSearchShortcuts(), consumer);
    }

    public void updateSuggestions() {
        if (DBG) Log.d(TAG, "updateSuggestions()");
        /// M: If activity is not resumed, do not update suggestions.
        /// Otherwise, the screen would refresh twice. @{
        if (!isResumed()) {
            return;
        }
        /// @}
        final String query = CharMatcher.WHITESPACE.trimLeadingFrom(getQuery());
        mSearchActivityView.onClickLogCountChanged(0);
        getQsbApplication().getSourceTaskExecutor().cancelPendingTasks();
        getCorporaToQuery(new Consumer<List<Corpus>>() {
            @Override
            public boolean consume(List<Corpus> corporaToQuery) {
                updateSuggestions(query, corporaToQuery);
                return true;
            }
        });
    }

    protected void updateSuggestions(String query, List<Corpus> corporaToQuery) {
        if (DBG) Log.d(TAG, "updateSuggestions(\"" + query + "\"," + corporaToQuery + ")");
        /// M: Change shortcut mechanism according to mix mode @{
        Suggestions suggestions = null;
        if (QsbApplication.isMixMode()) {
            suggestions = getSuggestionsProvider().getSuggestions(
                    query, corporaToQuery);
            getShortcutsForQuery(query, corporaToQuery, suggestions);
        } else {
            if (query.equals("")) {
                suggestions = new Suggestions(query, corporaToQuery);
                suggestions.done();
                //lijun modify start
//                getShortcutsNoQuery(corporaToQuery, suggestions);
                getClickLogs(getCorpora().getAllCorpora(),suggestions);
                //lijun modify end
            } else {
                suggestions = getSuggestionsProvider().getSuggestions(
                        query, corporaToQuery);
            }
        }
        /// @}

        // Log start latency if this is the first suggestions update
        gotSuggestions(suggestions);

        showSuggestions(suggestions);
    }

    protected void showSuggestions(Suggestions suggestions) {
        mSearchActivityView.setSuggestions(suggestions);
    }

    private class ClickHandler implements SuggestionClickListener {

        public void onSuggestionQuickContactClicked(SuggestionsAdapter<?> adapter, long id) {
            clickedQuickContact(adapter, id);
        }

        public void onSuggestionClicked(SuggestionsAdapter<?> adapter, long id) {
            launchSuggestion(adapter, id);
        }

        public void onSuggestionRemoveFromHistoryClicked(SuggestionsAdapter<?> adapter, long id) {
            /// M: If can remove single shortcut, then show the dialog
            if (QsbApplication.canRemoveShortcut()) {
                removeFromHistoryClicked(adapter, id);
            }
        }

        public void onSuggestionQueryRefineClicked(SuggestionsAdapter<?> adapter, long id) {
            refineSuggestion(adapter, id);
        }

        /**
         * lijun add for history
         * @param adapter
         * @param queryKey
         */
        @Override
        public void onHistorySuggestionDeleteClick(SuggestionsAdapter<?> adapter, String queryKey) {
            deleteHistoryOnClick(adapter,queryKey);
        }
    }

    private class CorpusSelectorDismissListener implements DialogInterface.OnDismissListener {
        public void onDismiss(DialogInterface dialog) {
            if (DBG) Log.d(TAG, "Corpus selector dismissed");
            clearStartedIntoCorpusSelectionDialog();
        }
    }

    private class CorporaObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            String currentCorpusName = getCorpusName();
            QsbApplication app = QsbApplication.get(SearchActivity.this);

            // This callback will be invoked after make changes in QSB settings.
            // We suppose to check if the current corpus is still enable,
            // otherwise, set the current corpus to ALL.
            if ((null != currentCorpusName)
                    && (!app.isCorpusEnabled(currentCorpusName))) {
                currentCorpusName = null;
            }

            setCorpus(currentCorpusName);
            updateSuggestions();
        }
    }

    public interface OnDestroyListener {
        void onDestroyed();
    }

    /**
     * M: Override this methord to resolve issue:
     * java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState.
     */
    @Override
    public void onBackPressed() {
        //lijun add for hot search start
        if (isWebLoadPanelShowing()) {
            hideWebLoadPanel();
            return;
        } else if (isWeiboViewShowing()) {
            if (mWebview.canGoBack()) {
                mWebview.goBack();
            } else {
                mWebview.clearHistory();
                hideWeibo();
            }
            return;
        }
        //lijun add for hot search end
        if (isResumed()) {
            super.onBackPressed();
        }
    }

    /*
     * M: Only get shortcuts when query is null if mix mode is to
     * show suggestions and shortcuts separately.
     */
    protected void getShortcutsNoQuery(Collection<Corpus> corporaToQuery,
            final Suggestions suggestions) {
        ShortcutRepository shortcutRepo = getShortcutRepository();
        if (shortcutRepo == null) return;
        Consumer<ShortcutCursor> consumer = Consumers.createAsyncCloseableConsumer(mHandler,
                new Consumer<ShortcutCursor>() {
            public boolean consume(ShortcutCursor shortcuts) {
                suggestions.setShortcuts(shortcuts);
                return true;
            }
        });
        ((ShortcutRepositoryImplLog) shortcutRepo).getShortcutsNoQuery(corporaToQuery,
                getSettings().allowWebSearchShortcuts(), consumer);
    }

    /**
     * lijun add for history
     */
    public void getClickLogs(Collection<Corpus> corporaToQuery,final Suggestions suggestions){
        ShortcutRepository shortcutRepo = getShortcutRepository();
        if (shortcutRepo == null) return;

        Consumer<ClickLogCursor> consumer = Consumers.createAsyncCloseableConsumer(mHandler,
                new Consumer<ClickLogCursor>() {
                    public boolean consume(ClickLogCursor clickLogCursor) {
                        suggestions.setClickLogCursor(clickLogCursor);
                        return true;
                    }
                });

        ((ShortcutRepositoryImplLog) shortcutRepo).getClickLogs(corporaToQuery,consumer);
    }

    //lijun add for history
    private void deleteHistoryOnClick(SuggestionsAdapter<?> adapter,String queryKey){
        Log.d(TAG,"deleteHistoryOnClick queryKey : " + queryKey);
        ShortcutRepository shortcuts = QsbApplication.get(this).getShortcutRepository();
        Suggestions suggestions = adapter.getSuggestions();
        ClickLogCursor clickLogCursor = suggestions.getClickLogCursor();
        if(clickLogCursor != null){
            shortcuts.removeFromClickLogHistory(clickLogCursor,queryKey);
            mSearchActivityView.onClickLogCountChanged(0);
//            clickLogCursor.removeRow();
//            clickLogCursor.notifyDataSetChanged();
        }
        updateSuggestionsBuffered();
    }

    //lijun add for hot search start

    private void initWebView(){
        mWebview = (WebView) findViewById(R.id.hotsearch_webview);
        webviewSharedPreferences = getSharedPreferences("weibo_webview", MODE_PRIVATE);
        WebSettings webSettings = mWebview.getSettings();
        webSettings.setJavaScriptEnabled(true);
//        webSettings.setAllowFileAccess(true);
//        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
//        webSettings.setAppCacheEnabled(true);
//        webSettings.setDomStorageEnabled(true);
//        webSettings.setDatabaseEnabled(true);

        webSettings.setAllowUniversalAccessFromFileURLs(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(mWebview,true);

        mWebViewLoadPanel = findViewById(R.id.webview_load);
        mWebViewLoading = mWebViewLoadPanel.findViewById(R.id.webview_loading_progress);
        mWebViewLoadError = mWebViewLoadPanel.findViewById(R.id.webview_load_error);
    }

    public void loadWeibo(String url){
        if(mWebViewState == WebViewState.LOADING)return;
        Log.d(HotSearchView.TAG,"loadWeibo url : " +url);
        mWebview.setWebViewClient(webClient);
        clearWebviewCaches = true;
        mWebview.loadUrl(url);
    }

    private void hideWeibo(){
        mWebViewState = WebViewState.HIDE;
        mWebViewLoadPanel.setVisibility(View.GONE);
        mWebview.setVisibility(View.GONE);
        mWebview.onPause();
    }

    private void showWeibo(){
        mWebViewState = WebViewState.LOADED_SUCCESS;
        mWebViewLoadPanel.setVisibility(View.GONE);
        mWebview.setVisibility(View.VISIBLE);
        mWebview.onResume();
    }

    private boolean isWeiboViewShowing(){
        return mWebview.getVisibility() == View.VISIBLE;
    }

    public void hideWebLoadPanel(){
        mWebViewLoadPanel.setVisibility(View.GONE);
        if(mWebViewState == WebViewState.LOADING){
            mWebview.stopLoading();
        }
        mWebViewState = WebViewState.HIDE;
    }

    private void showErrorPage() {
        mWebViewState = WebViewState.LOADED_FAILED;
        mWebViewLoadPanel.setVisibility(View.VISIBLE);
        mWebViewLoading.setVisibility(View.GONE);
        mWebViewLoadError.setVisibility(View.VISIBLE);
        mWebview.setVisibility(View.GONE);
    }

    private void showLoadingPage(){
        mWebViewState = WebViewState.LOADING;
        mWebViewLoadPanel.setVisibility(View.VISIBLE);
        mWebViewLoading.setVisibility(View.VISIBLE);
        mWebViewLoadError.setVisibility(View.GONE);
        mWebview.setVisibility(View.GONE);
    }

    public boolean isWebLoadPanelShowing(){
        return mWebViewLoadPanel.getVisibility() == View.VISIBLE;
    }

    public void hideInputMethod(){
        mSearchActivityView.hideInputMethod();
    }

    public void loadHotSearchSuccess(boolean success){
        mSearchActivityView.updateHotsearchView(success);
    }

    public void updateForQuery(){
        if(isWebLoadPanelShowing() || isWeiboViewShowing()){
            hideWeibo();
        }
    }

    WebViewClient webClient = new WebViewClient(){
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            Log.d(HotSearchView.TAG,"loadWeibo onReceivedError");
            showErrorPage();
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            Log.d(HotSearchView.TAG,"loadWeibo onPageStarted");
            showLoadingPage();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            Log.d(HotSearchView.TAG, "loadWeibo onPageFinished url : " + url);
            if (mWebViewState != WebViewState.LOADED_FAILED) {
                mWebViewState = WebViewState.LOADED_SUCCESS;
                showWeibo();
            }
            if (clearWebviewCaches) {
                mWebview.clearHistory();
                clearWebviewCaches = false;
            }

//            if (url != null && url.contains("passport.weibo.cn/sso/crossdomain")) {
//                CookieManager cookieManager = CookieManager.getInstance();
//                cookieManager.setAcceptCookie(true);
//                cookies = cookieManager.getCookie(url);
//                synchronousWebCookies(SearchActivity.this, url, cookies);
//                Log.e(HotSearchView.TAG, "onPageFinished cook : " + cookies + " url: " + url);
//                mWebview.clearHistory();
//            }
        }
    };

    public void synchronousWebCookies(Context context,String url,String cookies) {
        Log.d(HotSearchView.TAG, "synchronousWebCookies : " + cookies);
        if (cookies != null && !cookies.equals(lastcookies)) {
            if (!TextUtils.isEmpty(cookies)) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    CookieSyncManager.createInstance(context);
                }
                CookieManager cookieManager = CookieManager.getInstance();

                String oldCookie = webviewSharedPreferences.getString("cook", "");
                Log.d(HotSearchView.TAG, "oldCookie : " + oldCookie);


                cookieManager.setAcceptCookie(true);
                cookieManager.removeSessionCookie();
                cookieManager.removeAllCookie();

                String[] cks = cookies.split(";");
                String cookieNames = "";
                for (String item : cks) {
                    int index = item.indexOf("=");
                    String cookieName = item.substring(0, index);
                    String cookieVl = item.substring(index + 1);
                    String coo = cookieName + "=" + cookieVl;
                    cookieManager.setCookie(WEIBO_HOST, coo);
                    cookieNames = cookieNames + cookieName;
                }

                if (oldCookie != null && !oldCookie.equals("")) {
                    String[] cks2 = oldCookie.split(";");
                    for (String item : cks2) {
                        int index = item.indexOf("=");
                        String cookieName = item.substring(0, index);
                        if (cookieNames.contains(cookieName)) continue;
                        String cookieVl = item.substring(index + 1);
                        String coo = cookieName + "=" + cookieVl;
                        cookieManager.setCookie(WEIBO_HOST, coo);
                    }
                }

                CookieSyncManager.getInstance().sync();//同步cookie
                String newCookie = cookieManager.getCookie(WEIBO_HOST);
                webviewSharedPreferences.edit().putString("cook", newCookie).apply();

                lastcookies = cookies;
                mWebview.reload();
                Log.d(HotSearchView.TAG, "newCookie : " + newCookie);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.e("HotSearch","onConfigurationChanged");
    }
    //lijun add for hot search end
}
