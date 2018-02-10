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

public class NoteStyleAdapter extends BaseAdapter {

    private static final int[] RES_IDS = new int[] {
            R.drawable.ic_style_white,
            R.drawable.ic_style_blue,
            R.drawable.ic_style_green,
            R.drawable.ic_style_pink,
    };

    private int mCheckedPosition;
    private List<String> mLabels;
    private LayoutInflater mLayoutInflater;

    public NoteStyleAdapter(Context context) {
        String[] labels = context.getResources().getStringArray(R.array.style_labels);
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
            convertView = mLayoutInflater.inflate(R.layout.note_menu_style_item, null);
        }

        TextView label = (TextView) convertView.findViewById(R.id.label);
        label.setText(mLabels.get(position));
        label.setCompoundDrawablesWithIntrinsicBounds(RES_IDS[position], 0, 0, 0);

        ImageView checked = (ImageView) convertView.findViewById(R.id.checked);
        if (mCheckedPosition == position) {
            checked.setVisibility(View.VISIBLE);
        } else {
            checked.setVisibility(View.GONE);
        }

        return convertView;
    }
}
