package com.protruly.music.widget;

import android.widget.BaseAdapter;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by hujianwei on 17-8-30.
 */

public class AdapterWrapper extends BaseAdapter implements  StickyListHeadersAdapter{

    public interface OnHeaderClickListener {
        public void onHeaderClick(View header, int itemPosition, long headerId);
    }

    final StickyListHeadersAdapter mDelegate;
    private final List<View> mHeaderCache = new LinkedList<View>();
    private final Context mContext;
    private Drawable mDivider;
    private int mDividerHeight;
    private OnHeaderClickListener mOnHeaderClickListener;
    private DataSetObserver mDataSetObserver = new DataSetObserver() {

        @Override
        public void onInvalidated() {
            mHeaderCache.clear();
            AdapterWrapper.super.notifyDataSetInvalidated();
        }

        @Override
        public void onChanged() {
            AdapterWrapper.super.notifyDataSetChanged();
        }
    };

    AdapterWrapper(Context context, StickyListHeadersAdapter delegate) {
        this.mContext = context;
        this.mDelegate = delegate;
        delegate.registerDataSetObserver(mDataSetObserver);
    }

    void setDivider(Drawable divider) {
        this.mDivider = divider;
    }

    void setDividerHeight(int dividerHeight) {
        this.mDividerHeight = dividerHeight;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return mDelegate.areAllItemsEnabled();
    }

    @Override
    public boolean isEnabled(int position) {
        return mDelegate.isEnabled(position);
    }

    @Override
    public int getCount() {
        return mDelegate.getCount();
    }

    @Override
    public Object getItem(int position) {
        return mDelegate.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        return mDelegate.getItemId(position);
    }

    @Override
    public boolean hasStableIds() {
        return mDelegate.hasStableIds();
    }

    @Override
    public int getItemViewType(int position) {
        return mDelegate.getItemViewType(position);
    }

    @Override
    public int getViewTypeCount() {
        return mDelegate.getViewTypeCount();
    }

    @Override
    public boolean isEmpty() {
        return mDelegate.isEmpty();
    }

    private View popHeader() {
        if (mHeaderCache.size() > 0) {
            return mHeaderCache.remove(0);
        }
        return null;
    }

    /** Returns {@code true} if the previous position has the same header ID. */
    private boolean previousPositionHasSameHeader(int position) {
        return position != 0
                && mDelegate.getHeaderId(position) == mDelegate
                .getHeaderId(position - 1);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = mDelegate.getView(position, convertView, parent);
        return convertView;
    }

    public void setOnHeaderClickListener(
            OnHeaderClickListener onHeaderClickListener) {
        this.mOnHeaderClickListener = onHeaderClickListener;
    }

    @Override
    public boolean equals(Object o) {
        return mDelegate.equals(o);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return ((BaseAdapter) mDelegate).getDropDownView(position, convertView,
                parent);
    }

    @Override
    public int hashCode() {
        return mDelegate.hashCode();
    }

    @Override
    public void notifyDataSetChanged() {
        ((BaseAdapter) mDelegate).notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated() {
        ((BaseAdapter) mDelegate).notifyDataSetInvalidated();
    }

    @Override
    public String toString() {
        return mDelegate.toString();
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        return mDelegate.getHeaderView(position, convertView, parent);
    }

    @Override
    public long getHeaderId(int position) {
        return mDelegate.getHeaderId(position);
    }

}
