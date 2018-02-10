package com.hb.thememanager.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.RatingBar;

import hb.app.HbActivity;
import hb.widget.ActionMode;
import hb.widget.ActionModeListener;

import com.alibaba.fastjson.JSON;
import com.hb.thememanager.R;
import com.hb.thememanager.http.Http;
import com.hb.thememanager.http.response.IResponseHandler;
import com.hb.thememanager.http.response.RawResponseHandler;
import com.hb.thememanager.http.response.adapter.CommentsBody;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.User;
import com.hb.thememanager.utils.CommonUtil;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.ToastUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by alexluo on 17-8-16.
 */

public class AddCommentActivity extends HbActivity implements ActionModeListener,RatingBar.OnRatingBarChangeListener{

    public static final int ADD_COMMENT_REQUEST_CODE = 0X01;
    public static final int ADD_COMMENT_RESULT_CODE = 0X02;

    private RatingBar mRating;
    private EditText mInputText;
    private Handler mHandler = new Handler();
    private Http mHttp;
    private Theme mCurrentTheme;
    private boolean mSubmitSuccess;
    private boolean mEnablePostButton = false;
    private Runnable mShowActionMode = new Runnable() {
        @Override
        public void run() {
            showActionMode(true);
            getActionMode().enableItem(ActionMode.POSITIVE_BUTTON,false);
        }
    };

    private Runnable mClearCommentsContentAction = new Runnable() {
        @Override
        public void run() {
            if(mSubmitSuccess) {
                mInputText.setText(null);
                mRating.setRating(0f);
                ToastUtils.showShortToast(AddCommentActivity.this, R.string.add_comments_success);
                setResult(ADD_COMMENT_RESULT_CODE);
                finish();

            }else{
                ToastUtils.showShortToast(AddCommentActivity.this, R.string.add_comments_failure);
            }
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHbContentView(R.layout.activity_add_comment);
        mRating = (RatingBar)findViewById(R.id.comment_rating);
        mRating.setOnRatingBarChangeListener(this);
        mCurrentTheme = getIntent().getParcelableExtra(Config.ActionKey.KEY_HANDLE_COMMENTS);
        mHttp = Http.getHttp(getApplicationContext());
        mInputText = (EditText)findViewById(android.R.id.text2);
        mInputText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                enablePostButton();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        mHandler.post(mShowActionMode);
        setActionModeListener(this);
    }


    @Override
    protected void onResume() {
        super.onResume();
        if(mInputText != null){
            mInputText.setFocusable(true);
        }
    }

    private void enablePostButton(){
        getActionMode().enableItem(ActionMode.POSITIVE_BUTTON,
                !TextUtils.isEmpty(mInputText.getText()) && mRating.getRating() != 0.0f);
    }

    @Override
    public void onRatingChanged(RatingBar ratingBar, float rating, boolean b) {
        enablePostButton();
    }

    @Override
    public void onActionItemClicked(ActionMode.Item item) {
        if(item.getItemId() == ActionMode.POSITIVE_BUTTON){
            submitComment();
        }else if(item.getItemId() == ActionMode.NAGATIVE_BUTTON){
            onBackPressed();
        }
    }

    private void submitComment(){
        User user = User.getInstance(getApplicationContext());
        if(!user.isLogin()){
            user.jumpLogin(null,this);
            return;
        }


        Comment ct = new Comment();
        ct.version = CommonUtil.getThemeAppVersion(getApplicationContext());
        ct.deviceId = CommonUtil.getIMEI(getApplicationContext());
        ct.model = CommonUtil.getModel(getApplicationContext());
        ct.romVersion = CommonUtil.getRomVersion();
        ct.body = new CommentBody();
        ct.body.nickname = user.getNickName();
        try {
            ct.body.content = URLEncoder.encode(mInputText.getText().toString(),"UTF-8");
        }catch (UnsupportedEncodingException e){
            ct.body.content = mInputText.getText().toString();
        }
        ct.body.id = mCurrentTheme.id;
        ct.body.type = mCurrentTheme.type;
        ct.body.qlcId = user.getId();
        ct.body.StarLevel = getStartLevel();
        String json = JSON.toJSONString(ct);
        mHttp.post(Config.HttpUrl.ADD_COMMENTS_URL, json, new RawResponseHandler() {

            @Override
            public void onSuccess(int statusCode, String response) {
                mSubmitSuccess = true;
                mHandler.post(mClearCommentsContentAction);

            }

            @Override
            public void onFailure(int statusCode, String error_msg) {
                mSubmitSuccess = false;
                mHandler.post(mClearCommentsContentAction);
            }
        });
    }

    private int getStartLevel(){
        return (int)mRating.getRating();
    }

    @Override
    public void onActionModeShow(ActionMode actionMode) {

    }

    @Override
    public void onActionModeDismiss(ActionMode actionMode) {

    }




    class Comment{
         long version;

         String deviceId;

         String model;

         String romVersion;


        CommentBody body;

        public long getVersion() {
            return version;
        }

        public void setVersion(long version) {
            this.version = version;
        }

        public String getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getRomVersion() {
            return romVersion;
        }

        public void setRomVersion(String romVersion) {
            this.romVersion = romVersion;
        }

        public CommentBody getBody() {
            return body;
        }

        public void setBody(CommentBody body) {
            this.body = body;
        }
    }

    class CommentBody{
        public String nickname;
        int type;
        String id;
        String content;
        int StarLevel;
        String qlcId;

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public int getStarLevel() {
            return StarLevel;
        }

        public void setStarLevel(int starLevel) {
            StarLevel = starLevel;
        }

        public String getQlcId() {
            return qlcId;
        }

        public void setQlcId(String qlcId) {
            this.qlcId = qlcId;
        }
    }


}
