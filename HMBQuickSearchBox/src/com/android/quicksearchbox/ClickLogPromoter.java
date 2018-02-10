package com.android.quicksearchbox;

import com.google.common.collect.HashMultiset;

import java.util.ArrayList;

/**
 * Created by lijun on 17-5-19.
 */

public class ClickLogPromoter implements Promoter {

    protected ClickLogPromoter() {

    }

    protected void doPickPromoted(Suggestions suggestions, int maxPromoted, ListSuggestionCursor promoted) {
        if (QsbApplication.isMixMode()) {
            promoteClickLogs(suggestions.getClickLogCursor(), maxPromoted, promoted);
        } else {
            promoteClickLogs(suggestions.getClickLogCursor(), promoted);
        }
    }

    void promoteClickLogs(ClickLogCursor clicklogs, int maxPromoted,
                          ListSuggestionCursor promoted) {
        int clicklogCount = clicklogs == null ? 0 : clicklogs.getCount();
        if (clicklogCount == 0) return;
//        HashMultiset<Source> sourceClickLogCounts = HashMultiset.create(clicklogCount);
//        ArrayList<String> values = new ArrayList<String>();
//        for (int i = 0; i < clicklogCount && promoted.getCount() < maxPromoted; i++) {
//            clicklogs.moveTo(i);
//            Source source = clicklogs.getSuggestionSource();
//            if (source != null && accept(clicklogs)) {
//                int prevCount = sourceClickLogCounts.add(source, 1);
//                int maxShortcuts = getConfig().getClickLogsOnlyLimitedCount();
//                if (prevCount < maxShortcuts) {
//                    promoted.add(new SuggestionPosition(clicklogs));
//                }
//            }
//        }
    }

    void promoteClickLogs(ClickLogCursor clicklogs,
                          ListSuggestionCursor promoted) {
        int shortcutCount = clicklogs == null ? 0 : clicklogs.getCount();
        if (shortcutCount == 0) return;
        // limit the shortcuts count
        for (int i = 0; i < shortcutCount; i++) {
            clicklogs.moveTo(i);
            promoted.add(new SuggestionPosition(clicklogs));
        }
    }

    @Override
    public void pickPromoted(Suggestions suggestions, int maxPromoted, ListSuggestionCursor promoted) {
        doPickPromoted(suggestions, maxPromoted, promoted);
    }
}
