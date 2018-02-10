package com.android.quicksearchbox.util;

import android.content.ComponentName;
import android.database.DataSetObserver;

import com.android.quicksearchbox.Corpus;
import com.android.quicksearchbox.CorpusResult;
import com.android.quicksearchbox.Source;
import com.android.quicksearchbox.SuggestionExtras;

import java.util.Collection;

/**
 * Created by lijun on 17-5-22.
 */

public class ClickLogCorpusResult implements CorpusResult {
    @Override
    public Corpus getCorpus() {
        return null;
    }

    @Override
    public String getUserQuery() {
        return "";
    }

    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public void moveTo(int pos) {

    }

    @Override
    public boolean moveToNext() {
        return false;
    }

    @Override
    public int getPosition() {
        return 0;
    }

    @Override
    public void close() {

    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public Collection<String> getExtraColumns() {
        return null;
    }

    @Override
    public int getLatency() {
        return 0;
    }

    @Override
    public Source getSuggestionSource() {
        return null;
    }

    @Override
    public String getShortcutId() {
        return null;
    }

    @Override
    public boolean isSpinnerWhileRefreshing() {
        return false;
    }

    @Override
    public String getSuggestionFormat() {
        return null;
    }

    @Override
    public String getSuggestionText1() {
        return null;
    }

    @Override
    public String getSuggestionText2() {
        return null;
    }

    @Override
    public String getSuggestionText2Url() {
        return null;
    }

    @Override
    public String getSuggestionIcon1() {
        return null;
    }

    @Override
    public String getSuggestionIcon2() {
        return null;
    }

    @Override
    public String getSuggestionIntentAction() {
        return null;
    }

    @Override
    public ComponentName getSuggestionIntentComponent() {
        return null;
    }

    @Override
    public String getSuggestionIntentExtraData() {
        return null;
    }

    @Override
    public String getSuggestionIntentDataString() {
        return null;
    }

    @Override
    public String getSuggestionQuery() {
        return null;
    }

    @Override
    public String getSuggestionLogType() {
        return null;
    }

    @Override
    public boolean isSuggestionShortcut() {
        return false;
    }

    @Override
    public boolean isWebSearchSuggestion() {
        return false;
    }

    @Override
    public boolean isHistorySuggestion() {
        return false;
    }

    @Override
    public SuggestionExtras getExtras() {
        return null;
    }
}
