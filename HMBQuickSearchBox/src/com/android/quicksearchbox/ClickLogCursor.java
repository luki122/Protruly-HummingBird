package com.android.quicksearchbox;

import android.util.Log;

import com.android.quicksearchbox.util.QuietlyCloseable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by lijun on 17-5-19.
 */

public class ClickLogCursor extends ListSuggestionCursor{

    private SuggestionCursor mClickLogs;
    private Map<Integer,String> querys = new HashMap<Integer,String>();

    public ClickLogCursor(String userQuery, SuggestionCursor mClickLogs) {
        super(userQuery);
        initClickLogs(mClickLogs);
    }

    public ClickLogCursor(String userQuery, SuggestionCursor mClickLogs, Suggestion... suggestions) {
        super(userQuery, suggestions);
        initClickLogs(mClickLogs);
    }

    public ClickLogCursor(String userQuery, int capacity, SuggestionCursor mClickLogs) {
        super(userQuery, capacity);
        initClickLogs(mClickLogs);
    }

    private void initClickLogs(SuggestionCursor mClickLogs){
        this.mClickLogs = mClickLogs;
        querys.clear();
        int count = mClickLogs.getCount();
        ShortcutRepositoryImplLog.SuggestionCursorImpl suggestionCursor = (ShortcutRepositoryImplLog.SuggestionCursorImpl) mClickLogs;
        for (int i = 0; i < count; i++) {
            mClickLogs.moveTo(i);
            add(new SuggestionPosition(mClickLogs,i));
            querys.put(i,suggestionCursor.getQueryString());
        }
    }

    ArrayList<String> queryStrings ;

    @Override
    public boolean isHistory() {
        return true;
    }

    @Override
    public Suggestion current() {
        return super.current();
    }

    public String getCurrentClickText(){
        return querys.get(getPosition());
    }
}
