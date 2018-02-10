package cn.com.protruly.filemanager.utils;

import android.util.Log;
import android.widget.AbsListView;
import java.util.List;
import java.util.ArrayList;

public class MatchupRecorder {
    private AbsListView mCurrentListView;
    private List<PathListPositionMatchup> mMatchupList;
    private String mPreviousPath;

    public MatchupRecorder(AbsListView listView) {
        mCurrentListView = listView;
        mMatchupList = new ArrayList();
    }

    public int computeScrollPosition(String path) {
        int pos = 0;
        if(mPreviousPath != null) {
            if((path != null) && (path.startsWith(mPreviousPath))) {
                pos = recordPosition();
            } else {
                pos = getPosition(path);
            }
        }
        mPreviousPath = path;
        return pos;
    }

    private int recordPosition() {
        int pos = 0;
        int firstVisiblePosition = mCurrentListView.getFirstVisiblePosition();
        if((mMatchupList.size() != 0) && (mPreviousPath.equals(mMatchupList.get((mMatchupList.size() - 1)).mPath))) {
            firstVisiblePosition = mMatchupList.get((mMatchupList.size() - 1)).mPosition;
            return firstVisiblePosition;
        }
        mMatchupList.add(new PathListPositionMatchup(this, mPreviousPath, firstVisiblePosition));
        pos = firstVisiblePosition;
        return pos;
    }

    private int getPosition(String path) {
        int pos = 0;
        int i;
        for(i = 0; i < mMatchupList.size(); i++) {
            if(!path.startsWith(mMatchupList.get(i).mPath)) {
                break;
            }
        }
        if(i>0) {
            pos = mMatchupList.get((i - 1)).mPosition;
        }
        for(int j = (mMatchupList.size() - 1); (j >= (i - 1))&&(j >= 0); j--) {
            mMatchupList.remove(j);
        }
        return pos;
    }

    class PathListPositionMatchup {
        public String mPath;
        public int mPosition;

        public PathListPositionMatchup(MatchupRecorder p1, String path, int position) {
            mPath = path;
            mPosition = position;
        }
    }
}