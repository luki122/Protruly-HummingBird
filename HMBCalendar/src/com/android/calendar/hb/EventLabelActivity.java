package com.android.calendar.hb;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.android.calendar.R;
import com.android.calendar.Utils;

import hb.app.dialog.AlertDialog;

import java.util.List;

public class EventLabelActivity extends Activity implements View.OnClickListener {

    public static final int EVENT_LABEL_REQUEST_CODE = 1 << 7;
    public static final String KEY_EVENT_DESCRIPTION = "EVENT_DESCRIPTION";

    private InputMethodManager mInputMethodManager;

    private List<String> mDescriptionValues;
    private List<String> mDescriptionLabels;

    private String mLabel;

    private boolean mDialogShowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setStatusBarColor(getColor(R.color.toolbar_bg_color));
        super.onCreate(savedInstanceState);

        mInputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        setContentView(R.layout.hb_event_label_layout);

        initData();
        initView();
    }

    private void initData() {
        mDescriptionValues = Utils.loadDescriptionValues(getResources());
        mDescriptionLabels = Utils.loadDescriptionLabels(getResources());

        mLabel = getIntent().getStringExtra(KEY_EVENT_DESCRIPTION);
        if (!TextUtils.isEmpty(mLabel) &&
                mDescriptionValues.indexOf(mLabel) != -1) {
            mLabel = "";
        }
    }

    private void initView() {
        findViewById(R.id.back_view).setOnClickListener(this);

        View header =  LayoutInflater.from(this).inflate(R.layout.hb_event_label_header, null);

        if (!TextUtils.isEmpty(mLabel)) {
            ((TextView) header.findViewById(R.id.custom_label)).setText(mLabel);
        }

        ListView labelList = (ListView) findViewById(R.id.label_list);
        labelList.addHeaderView(header);
        labelList.setAdapter(new LabelAdapter(this, mDescriptionLabels));
        labelList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    finishWithLabel(mDescriptionValues.get(position - 1));
                    return;
                }
                if (!mDialogShowing) {
                    showDialog();
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        final int id = v.getId();
        if (id == R.id.back_view) {
            finish();
        }
    }

    private void showDialog() {
        mDialogShowing = true;

        final EditText editText = (EditText) LayoutInflater.from(this)
                .inflate(R.layout.hb_event_label_edit, null);
        if (!TextUtils.isEmpty(mLabel)) {
            editText.setText(mLabel);
            editText.setSelection(mLabel.length());
        }

        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.label_custom_title)
                .setView(editText)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mLabel = editText.getText().toString().trim();
                        if (!TextUtils.isEmpty(mLabel)) {
                            int index = mDescriptionLabels.indexOf(mLabel);
                            if (index != -1) {
                                mLabel = mDescriptionValues.get(index);
                            }
                        }
                        finishWithLabel(mLabel);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mDialogShowing = false;
            }
        });
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                mInputMethodManager.showSoftInput(editText, 0);
            }
        });
        alertDialog.show();
    }

    private void finishWithLabel(String label) {
        Intent intent = new Intent();
        intent.putExtra(KEY_EVENT_DESCRIPTION, label);

        setResult(RESULT_OK, intent);
        finish();
    }

    private class LabelAdapter extends BaseAdapter {

        private LayoutInflater mLayoutInflater;

        private List<String> mLabelList;

        LabelAdapter(Context context, List<String> labelList) {
            mLayoutInflater = LayoutInflater.from(context);
            mLabelList = labelList;
        }

        @Override
        public int getCount() {
            return mLabelList.size();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mLayoutInflater.inflate(R.layout.hb_event_label_item, null);
            }

            TextView labelView = (TextView) convertView;
            labelView.setText(mLabelList.get(position));
            labelView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    Utils.getLabelIconId(position), 0, 0, 0);

            return convertView;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }
    }
}
