package com.hb.note.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.hb.note.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NoteFontAdapter extends BaseAdapter {

    private int mCheckedPosition;
    private List<String> mLabels;
    private LayoutInflater mLayoutInflater;

    public NoteFontAdapter(Context context) {
        String[] labels = context.getResources().getStringArray(R.array.font_labels);
        mLabels = new ArrayList<>(Arrays.asList(labels));

        mLayoutInflater = LayoutInflater.from(context);
    }

    public void setCheckedPosition(int position) {
        mCheckedPosition = position;
    }

    public void clearCheckedPosition() {
        mCheckedPosition = -1;
    }

    @Override
    public int getCount() {
        return mLabels.size();
    }

    @Override
    public Object getItem(int position) {
        return mLabels.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.note_menu_font_item, null);
        }

        TextView label = (TextView) convertView.findViewById(R.id.label);
        label.setText(mLabels.get(position));

        if (position == 0) {
            label.setTextSize(24);
        } else if (position == 1) {
            label.setTextSize(20);
        } else {
            label.setTextSize(16);
        }

        ImageView checked = (ImageView) convertView.findViewById(R.id.checked);
        if (mCheckedPosition == position) {
            checked.setVisibility(View.VISIBLE);
        } else {
            checked.setVisibility(View.GONE);
        }

        return convertView;
    }
}
