package com.hb.note.activity;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.hb.note.R;
import com.hb.note.db.ImageData;
import com.hb.note.db.ImageDataHelper;
import com.hb.note.db.NoteData;
import com.hb.note.db.NoteDataHelper;
import com.hb.note.ui.NoteEditText;
import com.hb.note.ui.NoteEditorLayout;
import com.hb.note.ui.NoteFontAdapter;
import com.hb.note.ui.NoteImageSpan;
import com.hb.note.ui.NoteMenuDialog;
import com.hb.note.ui.NoteSpanHelper;
import com.hb.note.ui.NoteStyleAdapter;
import com.hb.note.ui.ToastHelper;
import com.hb.note.util.FileUtils;
import com.hb.note.util.Globals;
import com.hb.note.util.PatternUtils;
import com.hb.note.util.PermissionUtils;
import com.hb.note.util.SystemUtils;
import com.hb.note.util.Utils;

import hb.app.dialog.AlertDialog;
import hb.app.dialog.ProgressDialog;
import hb.widget.toolbar.Toolbar;
import hb.widget.toolbar.Toolbar.OnMenuItemClickListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

public class NoteEditorActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "NoteEditorActivity";

    public static final String NOTE_ID = "note_id";

    private static final String SPAN_SYMBOL_BILL = Globals.SPAN_START +
            Globals.SPAN_SYMBOL_BILL + Globals.SPAN_END;
    private static final String SPAN_SYMBOL_BILL_DONE = Globals.SPAN_START +
            Globals.SPAN_SYMBOL_BILL_DONE + Globals.SPAN_END;
    private static final int SPAN_SYMBOL_BILL_LENGTH = SPAN_SYMBOL_BILL.length();
    private static final int SPAN_SYMBOL_BILL_DONE_LENGTH = SPAN_SYMBOL_BILL_DONE.length();

    private static final int REQUEST_CODE_PICK_IMAGE = 1;
    private static final int MAX_IMAGE_COUNT = 10;
    private static final int MAX_LENGTH = 10000;

    private static final int INDEX_TITLE = 0;
    private static final int INDEX_SUBTITLE = 1;
    private static final int INDEX_CONTENT = 2;
    private static final int INDEX_BULLET = 3;
    private static final int INDEX_UNDERLINE = 4;

    private NoteDataHelper mNoteDataHelper;
    private NoteData mNoteData;

    private NoteEditorLayout mRootView;
    private NoteEditText mContentView;
    private Toolbar mToolbar;
    private TextView mTitleView;
    private TextView mTimeView;
    private View mMenuView;
    private ImageView mBackView;
    private ImageView mBillView;
    private ImageView mFontView;
    private ImageView mImageView;
    private ImageView mStyleView;
    private ProgressDialog mProgressDialog;

    private boolean mDeletingSpan, mSoftInputShowing;
    private SpannableString mContent;

    private NoteHandler mHandler = new NoteHandler(this);
    private Runnable mRunnable;

    private NoteFontAdapter mFontAdapter;
    private NoteStyleAdapter mStyleAdapter;
    private NoteMenuDialog mMenuDialog;
    private View mMenuContentView;

    private OnItemClickListener mFontClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (mMenuDialog != null) {
                mMenuDialog.dismiss();
            }
            setFont(position);
        }
    };

    private OnItemClickListener mStyleClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (mMenuDialog != null) {
                mMenuDialog.dismiss();
            }
            setStyle(position);
        }
    };

    private Runnable mFontRunnable = new Runnable() {
        @Override
        public void run() {
            initFontAdapter();
            showMenuDialog(mFontAdapter, mFontClickListener);
        }
    };

    private Runnable mThemeRunnable = new Runnable() {
        @Override
        public void run() {
            initStyleAdapter();
            showMenuDialog(mStyleAdapter, mStyleClickListener);
        }
    };

    private OnMenuItemClickListener onMenuItemClickListener = new OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_share:
                    new ShareImageTask().execute();
                    break;
                case R.id.action_delete:
                    showDeleteDialog();
                    break;
            }
            return false;
        }
    };

    private static class NoteHandler extends Handler {

        private static final int INIT_CONTENT = 0;
        private static final int UPDATE_CONTENT = 1;

        private WeakReference<NoteEditorActivity> mTarget;

        NoteHandler(NoteEditorActivity activity) {
            mTarget = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final NoteEditorActivity activity = mTarget.get();
            if (activity == null) {
                return;
            }

            switch (msg.what) {
                case INIT_CONTENT:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            activity.initContent();
                            updateContent();
                        }
                    }).start();

                    break;
                case UPDATE_CONTENT:
                    activity.updateContent();

                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }

        void initContent() {
            sendEmptyMessage(INIT_CONTENT);
        }

        void updateContent() {
            sendEmptyMessage(UPDATE_CONTENT);
        }
    }

    private class PickImageTask extends AsyncTask<Uri, Void, String> {
        @Override
        protected void onPreExecute() {
            mProgressDialog = ProgressDialog.show(
                    NoteEditorActivity.this, null, getString(R.string.image_copying));
        }

        @Override
        protected String doInBackground(Uri... params) {
            return getImagePath(params[0]);
        }

        @Override
        protected void onPostExecute(String path) {
            if (path != null) {
                insertImage(path);
            }
            dismissProgressDialog();
        }
    }

    private class ShareImageTask extends AsyncTask<Void, Void, String> {
        @Override
        protected void onPreExecute() {
            clearContentFocus();
            mProgressDialog = ProgressDialog.show(
                    NoteEditorActivity.this, null, getString(R.string.note_sharing));
        }

        @Override
        protected String doInBackground(Void... params) {
            return getShareImagePath();
        }

        @Override
        protected void onPostExecute(String path) {
            if (path != null) {
                scanShareImage(path);
            }
            dismissProgressDialog();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!PermissionUtils.checkAppDefaultPermissions(this)) {
            ToastHelper.show(R.string.permission_denied);
            finish();
            return;
        }

        setContentView(R.layout.note_editor_activity);

        initData();
        initToolbar();
        initViews();
        initListeners();
        updateViews();
    }

    @Override
    public void finish() {
        setResult(saveData() ? RESULT_OK : RESULT_CANCELED);
        closeHelper();

        super.finish();
    }

    @Override
    protected void onDestroy() {
        saveData();
        closeHelper();

        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back_view:
                handleExit();
                break;
            case R.id.bill_view:
                handleOnBillClick();
                break;
            case R.id.font_view:
                handleOnFontClick();
                break;
            case R.id.image_view:
                handleOnImageClick();
                break;
            case R.id.style_view:
                handleOnThemeClick();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (REQUEST_CODE_PICK_IMAGE == requestCode &&
                RESULT_OK == resultCode && data.getData() != null) {
            /*String imagePath = getPath(data.getData());
            if (imagePath != null) {
                insertImage(imagePath);
            }*/
            new PickImageTask().execute(data.getData());
        }
    }

    private void startPickImage() {
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setType("image/*");
        try {
            intent.setAction(Intent.ACTION_PICK);
            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
        } catch (ActivityNotFoundException e) {
            try {
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
            } catch (ActivityNotFoundException e1) {
                Log.e(TAG, "Can't goto pick image!");
            }
        }
    }

    private void startShareImage(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(intent, getString(R.string.app_name)));
    }

    private String getPath(Uri uri) {
        String path = null;
        try {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                path = cursor.getString(1);
            }
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return path;
    }

    private InputStream getInputStream(Uri uri) {
        InputStream is = null;
        try {
            is = getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage());
        }
        return is;
    }

    private String getImagePath(Uri uri) {
        String originalPath = getPath(uri);
        if (TextUtils.isEmpty(originalPath)) {
            return null;
        }

        ImageDataHelper imageDataHelper = new ImageDataHelper(this);
        ImageData imageData = imageDataHelper.query(originalPath);
        if (imageData != null) {
            File file = new File(imageData.getPath());
            if (file.exists()) {
                imageDataHelper.shutdown();
                return imageData.getPath();
            } else {
                imageDataHelper.delete(originalPath);
            }
        }

        String path = FileUtils.getImagePath();
        if (path == null || !FileUtils.copyToFile(getInputStream(uri), new File(path))) {
            imageDataHelper.shutdown();
            return originalPath;
        }

        imageData = new ImageData();
        imageData.setPath(path);
        imageData.setOriginalPath(originalPath);
        imageDataHelper.insert(imageData);
        imageDataHelper.shutdown();

        return path;
    }

    private Bitmap getShareBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(
                mContentView.getWidth(),
                mContentView.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.save();
        mContentView.draw(canvas);
        canvas.restore();
        return bitmap;
    }

    private String getShareImagePath() {
        Bitmap bitmap = null;
        try {
            bitmap = getShareBitmap();
        } catch (OutOfMemoryError error) {
            Log.e(TAG, error.getMessage());
        }

        String path = FileUtils.getShareImagePath();
        if (bitmap != null &&
                path != null &&
                FileUtils.writeToFile(bitmap, new File(path))) {
            return path;
        }

        return null;
    }

    private void scanShareImage(String path) {
        MediaScannerConnection.scanFile(this, new String[]{path}, null,
                new MediaScannerConnection.MediaScannerConnectionClient() {
                    @Override
                    public void onMediaScannerConnected() {

                    }

                    @Override
                    public void onScanCompleted(String path, Uri uri) {
                        startShareImage(uri);
                    }
                });
    }

    public void initContent() {
        mContent = getSpannableString(mNoteData.getContent());
    }

    public void updateContent() {
        mContentView.setText(mContent);
        dismissProgressDialog();
    }

    private void showDeleteDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.note_delete)
                .setPositiveButton(R.string.action_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        handleDeleteAndExit();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void handleDeleteAndExit() {
        mContentView.setText("");
        handleExit();
    }

    private void handleExit() {
        hideSoftInput();
        finish();
    }

    private void hideSoftInput() {
        if (mContentView != null && mContentView.hasFocus()) {
            mContentView.hideSoftInput();
        }
    }

    private boolean saveData() {
        if (mNoteDataHelper == null) {
            return false;
        }

        String content = mContentView.getText().toString().trim();

        if (mNoteData == null && !isContentEmpty(content)) {
            insertNote(content);
            return true;
        }

        if (mNoteData != null && contentChanged(content)) {
            if (isContentEmpty(content)) {
                deleteNote(mNoteData.getId());
            } else {
                updateNote(content, mNoteData.getId());
            }
            return true;
        }

        return false;
    }

    private boolean isContentEmpty(String content) {
        return TextUtils.isEmpty(content) || (hasSpanOnly(content) && !hasImage(content));
    }

    private boolean hasSpanOnly(String content) {
        return TextUtils.isEmpty(PatternUtils.replaceAllSpans(content).trim());
    }

    private boolean hasImage(String content) {
        return PatternUtils.getImageCount(content) > 0;
    }

    private boolean contentChanged(String content) {
        return !content.equals(mNoteData.getContent());
    }

    private String[] getTitleAndCharacters(String content, int imageCount) {
        String[] strings = new String[2];

        String characters = strings[1] = PatternUtils.replaceAllSpans(content).trim();
        String[] lines = characters.split(Globals.NEW_LINE);
        if (lines.length > 0 && lines[0].length() > 0) {
            strings[0] = lines[0];
        } else {
            strings[0] = getString(R.string.title_images, imageCount);
        }

        return strings;
    }

    private NoteData getNoteData(String content) {
        int imageCount = PatternUtils.getImageCount(content);
        String[] strings = getTitleAndCharacters(content, imageCount);

        NoteData noteData = new NoteData();
        noteData.setTitle(strings[0]);
        noteData.setContent(content);
        noteData.setCharacters(strings[1]);
        noteData.setImageCount(imageCount);

        return noteData;
    }

    private void insertNote(String content) {
        mNoteDataHelper.insert(getNoteData(content));
    }

    private void deleteNote(int noteId) {
        mNoteDataHelper.delete(noteId);
    }

    private void updateNote(String content, int noteId) {
        NoteData noteData = getNoteData(content);
        noteData.setId(noteId);
        mNoteDataHelper.update(noteData);
    }

    private void closeHelper() {
        if (mNoteDataHelper != null) {
            mNoteDataHelper.shutdown();
            mNoteDataHelper = null;
        }
    }

    private void initData() {
        mNoteDataHelper = new NoteDataHelper(this);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(NOTE_ID)) {
            int noteId = intent.getIntExtra(NOTE_ID, -1);
            mNoteData = noteId == -1 ? null : mNoteDataHelper.query(noteId);
        }
    }

    private void initToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mNoteData != null) {
            mToolbar.inflateMenu(R.menu.note_editor_menu);
            mToolbar.setOverflowIcon(getDrawable(R.drawable.ic_more));
            mToolbar.setOnMenuItemClickListener(onMenuItemClickListener);
        }
    }

    private void initViews() {
        mRootView = (NoteEditorLayout) findViewById(R.id.root_view);
        mContentView = (NoteEditText) findViewById(R.id.content_view);
        mTitleView = (TextView) findViewById(R.id.title_view);
        mTimeView = (TextView) findViewById(R.id.time_view);
        mMenuView = findViewById(R.id.menu_view);
        mBackView = (ImageView) findViewById(R.id.back_view);
        mBillView = (ImageView) findViewById(R.id.bill_view);
        mFontView = (ImageView) findViewById(R.id.font_view);
        mImageView = (ImageView) findViewById(R.id.image_view);
        mStyleView = (ImageView) findViewById(R.id.style_view);
    }

    private void initListeners() {
        mRootView.setOnSizeChangedListener(new NoteEditorLayout.OnSizeChangedListener() {
            @Override
            public void onSizeChanged(int w, int h, int oldw, int oldh) {
                if (h < oldh - 160) {
                    mSoftInputShowing = true;
                } else if (h > oldh + 160) {
                    mSoftInputShowing = false;
                }

                if (mRunnable != null) {
                    mHandler.postDelayed(mRunnable, 160);
                    mRunnable = null;
                }
            }
        });

        mBackView.setOnClickListener(this);
        mBillView.setOnClickListener(this);
        mFontView.setOnClickListener(this);
        mImageView.setOnClickListener(this);
        mStyleView.setOnClickListener(this);

        SystemUtils.lengthFilter(mContentView, MAX_LENGTH, R.string.length_exceed_limit);

        mContentView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mMenuView.setVisibility(View.VISIBLE);
                } else {
                    mMenuView.setVisibility(View.GONE);
                }
            }
        });

        mContentView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                return handleOnKey(keyCode, event);
            }
        });

        mContentView.setOnSpanClickListener(new NoteEditText.OnSpanClickListener() {
            @Override
            public boolean onSpanClick(Editable editable, NoteImageSpan span, int selStart) {
                if (NoteImageSpan.Type.Symbol == span.getType()) {
                    handleSymbolClick(editable, span, selStart);
                    return true;
                }
                return false;
            }
        });

        mContentView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                handleOnTextChanged(s, start, before, count);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void updateViews() {
        if (!Utils.isDefaultStyle()) {
            updateStyle();
        }

        if (mNoteData == null) {
            mTitleView.setText(R.string.note_edit);
            mTimeView.setVisibility(View.GONE);
        } else {
            preMeasureContent();
            clearContentFocus();

            mTitleView.setText(mNoteData.getTitle());
            mMenuView.setVisibility(View.GONE);

            String date = DateUtils.formatDateTime(this, mNoteData.getUpdateTime(),
                    DateUtils.FORMAT_SHOW_DATE |
                            DateUtils.FORMAT_SHOW_YEAR |
                            DateUtils.FORMAT_ABBREV_MONTH);
            String time = DateUtils.formatDateTime(this, mNoteData.getUpdateTime(),
                    DateUtils.FORMAT_SHOW_TIME);
            mTimeView.setText(date + "  " + time);

            setContent();
        }
    }

    private void clearContentFocus() {
        mRootView.setFocusable(true);
        mRootView.setFocusableInTouchMode(true);
        mRootView.requestFocus();
    }

    private void updateStyle() {
        int[] resIds = Utils.getResIds();

        getWindow().setStatusBarColor(getColor(resIds[0]));
        mRootView.setBackgroundResource(resIds[0]);
        mContentView.setBackgroundResource(resIds[0]);
        mContentView.setTextColor(getColor(resIds[1]));
        mToolbar.setOverflowIcon(getDrawable(resIds[2]));
        mBackView.setImageResource(resIds[3]);
        mBillView.setImageResource(resIds[4]);
        mFontView.setImageResource(resIds[5]);
        mImageView.setImageResource(resIds[6]);
        mStyleView.setImageResource(resIds[7]);
    }

    private void setStyle(int index) {
        String style = Utils.NOTE_STYLES[index];
        if (!Utils.getStylePreference().equals(style)) {
            Utils.setStylePreference(style);
            updateStyle();
            updateSymbolSpan();
        }
    }

    private void setContent() {
        if (hasImage(mNoteData.getContent())) {
            showProgressDialog();
            mHandler.initContent();
        } else {
            initContent();
            updateContent();
        }
    }

    private void showProgressDialog() {
        mProgressDialog = ProgressDialog.show(this, null, getString(R.string.note_loading));
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    private void handleOnBillClick() {
        int selStart = mContentView.getSelectionStart();
        Editable editable = mContentView.getText();
        String content = editable.toString();

        if (!TextUtils.isEmpty(content)) {
            int rowStart = SystemUtils.getRowStart(content, selStart);
            int rowEnd = SystemUtils.getRowEnd(content, selStart);
            String rowStr = null;
            if (rowStart < rowEnd) {
                rowStr = content.substring(rowStart, rowEnd);
            }

            if (TextUtils.isEmpty(rowStr) || !rowStr.startsWith(Globals.SPAN_START)) {
                editable.insert(rowStart, getSpannableString(SPAN_SYMBOL_BILL));
            } else {
                int spanEndIndex = rowStr.indexOf(Globals.SPAN_END);
                if (spanEndIndex == -1) {
                    return;
                }

                String dest = rowStr.substring(Globals.SPAN_START_LENGTH, spanEndIndex);
                if (!dest.startsWith(Globals.SPAN_IMAGE) &&
                        !dest.startsWith(Globals.SPAN_SYMBOL)) {
                    editable.replace(rowStart, rowEnd,
                            getSpannableString(SPAN_SYMBOL_BILL +
                                    rowStr.substring(spanEndIndex + Globals.SPAN_END_LENGTH)));
                }
            }
        } else {
            editable.insert(0, getSpannableString(SPAN_SYMBOL_BILL));
        }
    }

    private void handleOnFontClick() {
        handleRunnable(mFontRunnable);
    }

    private void handleOnImageClick() {
        String content = mContentView.getText().toString();
        if (!TextUtils.isEmpty(content.trim())) {
            int count = PatternUtils.getImageCount(content);
            if (count >= MAX_IMAGE_COUNT) {
                ToastHelper.show(R.string.image_count_exceed_limit);
                return;
            }
        }

        startPickImage();
    }

    private void handleOnThemeClick() {
        handleRunnable(mThemeRunnable);
    }

    private void handleRunnable(Runnable runnable) {
        if (mSoftInputShowing) {
            mContentView.setHandleEvent(true);
            mRunnable = runnable;
            hideSoftInput();
        } else {
            runnable.run();
        }
    }

    private void initFontAdapter() {
        if (mFontAdapter == null) {
            mFontAdapter = new NoteFontAdapter(this);
        }
        mFontAdapter.setCheckedPosition(getFontCheckedPosition());
    }

    private int getFontCheckedPosition() {
        String content = mContentView.getText().toString();
        int selStart = mContentView.getSelectionStart();

        if (TextUtils.isEmpty(content)) {
            return INDEX_CONTENT;
        } else {
            int rowStart = SystemUtils.getRowStart(content, selStart);
            int rowEnd = SystemUtils.getRowEnd(content, selStart);
            String rowStr = null;
            if (rowStart < rowEnd) {
                rowStr = content.substring(rowStart, rowEnd);
            }

            if (TextUtils.isEmpty(rowStr) || !rowStr.startsWith(Globals.SPAN_START)) {
                return INDEX_CONTENT;
            } else {
                int spanEndIndex = rowStr.indexOf(Globals.SPAN_END);
                if (spanEndIndex == -1) {
                    return INDEX_CONTENT;
                }

                String dest = rowStr.substring(Globals.SPAN_START_LENGTH, spanEndIndex);
                if (dest.startsWith(Globals.SPAN_IMAGE) ||
                        dest.startsWith(Globals.SPAN_SYMBOL)) {
                    return INDEX_CONTENT;
                }

                int index = INDEX_CONTENT;
                if (Globals.SPAN_TITLE.equals(dest)) {
                    index = INDEX_TITLE;
                } else if (Globals.SPAN_SUBTITLE.equals(dest)) {
                    index = INDEX_SUBTITLE;
                } else if (Globals.SPAN_BULLET.equals(dest)) {
                    index = INDEX_BULLET;
                } else if (Globals.SPAN_UNDER_LINE.equals(dest)) {
                    index = INDEX_UNDERLINE;
                }
                return index;
            }
        }
    }

    private void setFont(int index) {
        String spanDest = null;
        boolean setBullet = false;
        if (INDEX_TITLE == index) {
            spanDest = Globals.SPAN_TITLE;
        } else if (INDEX_SUBTITLE == index) {
            spanDest = Globals.SPAN_SUBTITLE;
        } else if (INDEX_BULLET == index) {
            spanDest = Globals.SPAN_BULLET;
            setBullet = true;
        } else if (INDEX_UNDERLINE == index) {
            spanDest = Globals.SPAN_UNDER_LINE;
        }

        String spanStr = spanDest == null ? "" : Globals.SPAN_START + spanDest + Globals.SPAN_END;

        Editable editable = mContentView.getText();
        String content = editable.toString();
        int selStart = mContentView.getSelectionStart();
        if (TextUtils.isEmpty(content)) {
            if (setBullet) {
                editable.insert(selStart, getSpannableString(spanStr));
            }
        } else {
            int rowStart = SystemUtils.getRowStart(content, selStart);
            int rowEnd = SystemUtils.getRowEnd(content, selStart);
            String rowStr = null;
            if (rowStart < rowEnd) {
                rowStr = content.substring(rowStart, rowEnd);
            }

            if (TextUtils.isEmpty(rowStr)) {
                if (setBullet) {
                    editable.insert(selStart, getSpannableString(spanStr));
                }
            } else if (!rowStr.startsWith(Globals.SPAN_START)) {
                editable.replace(rowStart, rowEnd, getSpannableString(spanStr + rowStr));
            } else {
                int spanEndIndex = rowStr.indexOf(Globals.SPAN_END);
                if (spanEndIndex == -1) {
                    return;
                }

                String dest = rowStr.substring(Globals.SPAN_START_LENGTH, spanEndIndex);
                if (dest.startsWith(Globals.SPAN_IMAGE) || dest.equals(spanDest)) {
                    return;
                }

                String text = rowStr.substring(spanEndIndex + Globals.SPAN_END_LENGTH);
                if (TextUtils.isEmpty(text) &&
                        Globals.SPAN_BULLET.equals(dest) &&
                        !TextUtils.isEmpty(spanDest)) {
                    return;
                }

                editable.replace(rowStart, rowEnd, getSpannableString(spanStr + text));
            }
        }
    }

    private void initStyleAdapter() {
        if (mStyleAdapter == null) {
            mStyleAdapter = new NoteStyleAdapter(this);
        }
        mStyleAdapter.setCheckedPosition(Utils.getStyleIndex());
    }

    private void showMenuDialog(BaseAdapter adapter, OnItemClickListener listener) {
        if (mMenuContentView == null) {
            mMenuContentView = LayoutInflater.from(this)
                    .inflate(R.layout.note_menu_content_view, null);
        }

        ListView listView = (ListView) mMenuContentView.findViewById(R.id.label_list);
        listView.setOnItemClickListener(listener);
        listView.setAdapter(adapter);

        if (mMenuDialog == null) {
            mMenuDialog = new NoteMenuDialog(this);
            mMenuDialog.setContentView(mMenuContentView);
            mMenuDialog.getWindow().setGravity(Gravity.BOTTOM);
        }
        mMenuDialog.show();

        mContentView.setHandleEvent(false);
    }

    private boolean handleOnKey(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_DEL == keyCode &&
                KeyEvent.ACTION_DOWN == event.getAction()) {
            if (mDeletingSpan) {
                return true;
            }

            Editable editable = mContentView.getText();
            String content = editable.toString();
            if (TextUtils.isEmpty(content)) {
                return false;
            }

            int selStart = mContentView.getSelectionStart();
            String frontStr = content.substring(0, selStart);
            String afterStr = content.substring(selStart, content.length());
            if (TextUtils.isEmpty(frontStr)) {
                return false;
            }

            if (frontStr.endsWith(Globals.SPAN_END + Globals.NEW_LINE)) {
                if (selStart < content.length() &&
                        content.charAt(selStart) != Globals.NEW_LINE_CHAR) {
                    mContentView.setSelection(selStart - 1);
                    return true;
                }
            } else if (frontStr.endsWith(Globals.SPAN_END)) {
                return deleteSpan(editable, frontStr);
            } else if (!TextUtils.isEmpty(afterStr) && afterStr.startsWith(Globals.SPAN_START)) {
                mContentView.setSelection(selStart - 1);
                return true;
            }
        }
        return false;
    }

    private void handleSymbolClick(Editable editable, NoteImageSpan span, int selStart) {
        final String content = editable.toString();
        int rowStart = SystemUtils.getRowStart(content, selStart);
        int rowEnd = SystemUtils.getRowEnd(content, selStart);
        String rowStr = null;
        if (rowStart < rowEnd) {
            rowStr = content.substring(rowStart, rowEnd);
        }

        if (!TextUtils.isEmpty(rowStr)) {
            mContentView.setSelection(rowEnd);

            int spanLength;
            String spanStr;
            if (Globals.SPAN_SYMBOL_BILL.equals(span.getSource())) {
                spanLength = SPAN_SYMBOL_BILL_LENGTH;
                spanStr = SPAN_SYMBOL_BILL_DONE;
            } else {
                spanLength = SPAN_SYMBOL_BILL_DONE_LENGTH;
                spanStr = SPAN_SYMBOL_BILL;
            }

            int spanEnd = rowStart + spanLength;
            if (spanEnd > rowEnd) {
                return;
            }

            removeSpan(editable, spanEnd, rowEnd, StrikethroughSpan.class);

            editable.replace(rowStart, rowEnd,
                    getSpannableString(spanStr + rowStr.substring(spanLength)));
        }
    }

    private void handleOnTextChanged(CharSequence s, int start, int before, int count) {
        Log.d(TAG, "onTextChanged() start = " + start +
                " before = " + before + " count = " + count);

        final String content = s.toString();
        if (before == 0 && count > 0 && (s instanceof SpannableStringBuilder)) {
            int rowStart = SystemUtils.getRowStart(content, start);
            int rowEnd = SystemUtils.getRowEnd(content, start);

            String beforeStr = null;
            String afterStr = null;
            if (start > rowStart) {
                beforeStr = content.substring(rowStart, start);
            } else if (rowEnd > start + count) {
                afterStr = content.substring(start + count, rowEnd);
            }

            if (!TextUtils.isEmpty(beforeStr) && beforeStr.endsWith(Globals.SPAN_END)) {
                int spanStart = beforeStr.lastIndexOf(Globals.SPAN_START);
                if (spanStart == -1) {
                    return;
                }

                String dest = beforeStr.substring(spanStart + Globals.SPAN_START_LENGTH,
                        beforeStr.length() - Globals.SPAN_END_LENGTH);
                if (dest.startsWith(Globals.SPAN_IMAGE) &&
                        content.charAt(start) != Globals.NEW_LINE_CHAR) {
                    ((SpannableStringBuilder) s).insert(start, Globals.NEW_LINE);
                }
            } else if (!TextUtils.isEmpty(afterStr) &&
                    afterStr.startsWith(Globals.SPAN_START)) {
                ((SpannableStringBuilder) s).delete(start, start + count);
            }

            String rowStr = null;
            if (rowStart < rowEnd) {
                rowStr = content.substring(rowStart, rowEnd);
            }

            if (!TextUtils.isEmpty(rowStr) && rowStr.startsWith(Globals.SPAN_START)) {
                int spanEndIndex = rowStr.indexOf(Globals.SPAN_END);
                if (spanEndIndex == -1) {
                    return;
                }

                String dest = rowStr.substring(Globals.SPAN_START_LENGTH, spanEndIndex);
                if (!dest.startsWith(Globals.SPAN_IMAGE) &&
                        !dest.equals(Globals.SPAN_SYMBOL_BILL)) {
                    ((SpannableStringBuilder) s).replace(rowStart, rowEnd,
                            getSpannableString(content.substring(rowStart, rowEnd)));
                }
            }
        }

        if (before > 0 && count == 0 && (s instanceof SpannableStringBuilder)) {
            int rowStart = SystemUtils.getRowStart(content, start);
            int rowEnd = SystemUtils.getRowEnd(content, start);
            String rowStr = null;
            if (rowStart < rowEnd) {
                rowStr = content.substring(rowStart, rowEnd);
            }

            if (!TextUtils.isEmpty(rowStr) &&
                    rowStr.startsWith(Globals.SPAN_START) &&
                    rowStr.endsWith(Globals.SPAN_END)) {
                int spanEndIndex = rowStr.indexOf(Globals.SPAN_END);
                String dest = rowStr.substring(Globals.SPAN_START_LENGTH, spanEndIndex);
                if (dest.equals(Globals.SPAN_TITLE) ||
                        dest.equals(Globals.SPAN_SUBTITLE) ||
                        dest.equals(Globals.SPAN_UNDER_LINE)) {
                    ((SpannableStringBuilder) s).delete(rowStart, rowEnd);
                }
            }
        }
    }

    private void insertImage(String imagePth) {
        Editable editable = mContentView.getText();
        String content = editable.toString();
        int selStart = mContentView.getSelectionStart();

        int insertPos = selStart;
        String spanStr = Globals.SPAN_START + Globals.SPAN_IMAGE +
                Globals.FILE_PROTOCOL + imagePth + Globals.SPAN_END;

        if (!TextUtils.isEmpty(content)) {
            int rowStart = SystemUtils.getRowStart(content, selStart);
            int rowEnd = SystemUtils.getRowEnd(content, selStart);
            String rowStr = null;
            if (rowStart < rowEnd) {
                rowStr = content.substring(rowStart, rowEnd);
            }

            if (!TextUtils.isEmpty(rowStr) && rowStr.startsWith(Globals.SPAN_START)) {
                insertPos = rowEnd;
                spanStr = Globals.NEW_LINE + spanStr;
            } else {
                String beforeStr = content.substring(0, selStart);
                if (!TextUtils.isEmpty(beforeStr) && !beforeStr.endsWith(Globals.NEW_LINE)) {
                    spanStr = Globals.NEW_LINE + spanStr;
                }
                String afterStr = content.substring(selStart, content.length());
                if (!TextUtils.isEmpty(afterStr) && !afterStr.startsWith(Globals.NEW_LINE)) {
                    spanStr += Globals.NEW_LINE;
                }
            }
        }

        editable.insert(insertPos, getSpannableString(spanStr));
        mContentView.setSelection(insertPos + spanStr.length());
    }

    private boolean deleteSpan(Editable editable, String frontStr) {
        int spanStart = frontStr.lastIndexOf(Globals.SPAN_START);
        if (spanStart == -1) {
            return false;
        }

        String source = frontStr.substring(spanStart);
        String dest = source.substring(
                Globals.SPAN_START_LENGTH,
                source.length() - Globals.SPAN_END_LENGTH);

        if (Globals.SPAN_BULLET.equals(dest) ||
                Globals.SPAN_SYMBOL_BILL.equals(dest)) {
            return false;
        }

        mDeletingSpan = true;
        if (dest.startsWith(Globals.SPAN_IMAGE)) {
            showDeleteImageDialog(editable, spanStart, frontStr.length());
        } else {
            deleteSpanWithTextStyle(editable, spanStart, frontStr.length(), dest);
        }

        return true;
    }

    private void deleteSpanWithTextStyle(Editable editable, int spanStart, int spanEnd, String dest) {
        deleteSpan(editable, spanStart, spanEnd);
        deleteTextStyle(editable, spanEnd, dest);
        mDeletingSpan = false;
    }

    private void deleteSpan(Editable editable, int spanStart, int spanEnd) {
        editable.delete(spanStart, spanEnd);
    }

    private void deleteTextStyle(Editable editable, int spanEnd, String dest) {
        String content = editable.toString();
        int rowEnd = SystemUtils.getRowEnd(content, spanEnd);
        if (rowEnd > spanEnd) {
            if (Globals.SPAN_SYMBOL_BILL_DONE.equals(dest)) {
                removeSpan(editable, spanEnd, rowEnd, StrikethroughSpan.class);
            } else if (Globals.SPAN_UNDER_LINE.equals(dest)) {
                removeSpan(editable, spanEnd, rowEnd, UnderlineSpan.class);
            } else {
                removeSpan(editable, spanEnd, rowEnd, AbsoluteSizeSpan.class);
            }
        }
    }

    private <T> void removeSpan(Editable editable, int start, int end, Class<T> type) {
        T[] spans = editable.getSpans(start, end, type);
        if (spans != null && spans.length > 0) {
            editable.removeSpan(spans[0]);
        }
    }

    private void showDeleteImageDialog(
            final Editable editable, final int spanStart, final int spanEnd) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.image_delete)
                .setPositiveButton(R.string.action_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteSpan(editable, spanStart, spanEnd);
                        mDeletingSpan = false;
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        mDeletingSpan = false;
                    }
                })
                .show();
    }

    private void updateSymbolSpan() {
        Editable editable = mContentView.getText();
        String content = editable.toString();

        if (TextUtils.isEmpty(content.trim())) {
            return;
        }

        int pos = 0;
        do {
            int spanStart = content.indexOf(SPAN_SYMBOL_BILL_DONE, pos);
            if (spanStart == -1) {
                break;
            }

            pos = spanStart + SPAN_SYMBOL_BILL_DONE_LENGTH;
            editable.replace(spanStart, pos,
                    getSymbolSpannableString(
                            SPAN_SYMBOL_BILL_DONE, Globals.SPAN_SYMBOL_BILL_DONE));
        } while (pos < content.length());
    }

    private SpannableString getSymbolSpannableString(String source, String dest) {
        return NoteSpanHelper.getSymbolSpan(
                source,
                dest,
                getPaddingLeft(),
                getPaddingTop());
    }

    private SpannableString getSpannableString(String content) {
        return NoteSpanHelper.string2SpannableString(
                content,
                getWidth(),
                getRightExtraSpace(),
                getPaddingLeft(),
                getPaddingTop());
    }

    private int getWidth() {
        int width = mContentView.getMeasuredWidth();
        if (width == 0) {
            preMeasureContent();
            width = mContentView.getMeasuredWidth();
        }
        return width - mContentView.getPaddingLeft() -
                mContentView.getPaddingRight() - getRightExtraSpace();
    }

    private void preMeasureContent() {
        mRootView.measure(
                MeasureSpec.makeMeasureSpec(
                        getResources().getDisplayMetrics().widthPixels, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
    }

    private int getRightExtraSpace() {
        return 6;
    }

    private int getPaddingLeft() {
        return mContentView.getCompoundPaddingLeft();
    }

    private int getPaddingTop() {
        return mContentView.getCompoundPaddingTop();
    }
}
