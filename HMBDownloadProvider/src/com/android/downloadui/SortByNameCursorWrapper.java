package com.android.downloadui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.provider.Downloads;
import android.text.TextUtils;

/**
 * @author wxue
 */
public class SortByNameCursorWrapper extends CursorWrapper implements Comparator<SortEntry>{
	private static final String TAG = "HmbSortByNameCursorWrapper";
	private final Cursor mCursor;
	private List <SortEntry> sortList = new ArrayList<SortEntry>(); 
	private int mPos;
	
	public SortByNameCursorWrapper(Cursor cursor) {
		super(cursor);
		this.mCursor = cursor;
		sortCursor();
	}
	
	private void sortCursor() {
		mCursor.moveToPosition(-1);
        if (mCursor != null && mCursor.getCount() > 0) {  
            int i = 0;  
            int column = mCursor.getColumnIndexOrThrow(Downloads.Impl.COLUMN_TITLE);  
            while(mCursor.moveToNext()){
            	SortEntry sortKey = new SortEntry();  
                sortKey.key = mCursor.getString(column);  
                sortKey.order = i;  
                sortList.add(sortKey);  
                i++;
            }
        }  
        Collections.sort(sortList, this);  
    }

	public boolean moveToPosition(int position) {
		if (position >= 0 && position < sortList.size()) {
			mPos = position;
			int order = sortList.get(position).order;
			return mCursor.moveToPosition(order);
		}
		if (position < 0) {
			mPos = -1;
		}
		if (position >= sortList.size()) {
			mPos = sortList.size();
		}
		return mCursor.moveToPosition(position);
	}

	public boolean moveToFirst() {
		return moveToPosition(0);
	}

	public boolean moveToLast() {
		return moveToPosition(getCount() - 1);
	}

	public boolean moveToNext() {
		return moveToPosition(mPos + 1);
	}

	public boolean moveToPrevious() {
		return moveToPosition(mPos - 1);
	}

	public boolean move(int offset) {
		return moveToPosition(mPos + offset);
	}

	public int getPosition() {
		return mPos;
	}

	@Override
	public int compare(SortEntry obj1, SortEntry obj2) {
		String text1 = obj1.key;
		String text2 = obj2.key;
		 if(TextUtils.isEmpty(text1) || TextUtils.isEmpty(text2)) {
             return -1;
         }
         String c1 = text1.substring(0, 1);
         String c2 = text2.substring(0, 1);
         int characterType1 = SortUtil.getCharacterType(c1);
         int characterType2 = SortUtil.getCharacterType(c2);
         LogUtil.i(TAG, " ------ displayName1:" + text1 + " text2:" + text2 + " c1:" + c1 + " c2:" + c2 +
                 " characterType1:" + SortUtil.getCharacterTypeDescription(characterType1) +
                 " characterType2:" + SortUtil.getCharacterTypeDescription(characterType2));
         if(characterType1 == characterType2) {
             if(characterType1 != SortUtil.TYPE_CHINESE) {
                 LogUtil.i(TAG, "111111 ");
                 return text1.compareTo(text2);
             } else {
                 LogUtil.i(TAG, "222222 pinyin");
                 //we compare pinyin order here
                 String pinyin1 = SortUtil.getSpell(text1);
                 String pinyin2 = SortUtil.getSpell(text2);
                 return pinyin1.compareTo(pinyin2);
             }
         } else {
             if(characterType1 < characterType2) {
                 LogUtil.i(TAG, "333333");
                 return -1;
             } else {
                 LogUtil.i(TAG, "444444");
                 return 1;
             }
         }
     }
}

class SortEntry {  
    public String key;  
    public int order;  
}
