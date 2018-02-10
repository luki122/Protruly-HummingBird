package com.hb.thememanager.http.request;

import android.content.Context;

import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.User;
import com.hb.thememanager.utils.Config;

/**
 * 
 */
public class FeedbackRequest extends ThemeRequest {

    private Context mContext;
    private User user;
    private String content;

    public FeedbackRequest(Context context) {
        super(context,Theme.THEME_NULL);
        mContext = context;
        user = User.getInstance(mContext);
        setUrl(Config.HttpUrl.FEEDBACK_URL);
    }

    public void setContent(String c) {
    	this.content = c;
    }
    
    @Override
    protected void generateRequestBody() {
    	FeedbackBody body = new FeedbackBody();
        body.setQlyId(user.getId());
        body.setMobile(user.getPhone());
        body.setNickname(user.getNickName());
        body.setContent(content);
        body.setupAvaliableProperties("qlcId","mobile","content","nickname");
        setBody(body);
    }

    public static class FeedbackBody extends RequestBody{
        public String qlcId;
        public String mobile;
        public String content;
        public String nickname;

        public String getQlyId() {
            return qlcId;
        }

        public void setQlyId(String  id) {
            this.qlcId = id;
        }
        
        public String getMobile() {
        	return mobile;
        }
        
        public void setMobile(String mobile) {
        	this.mobile = mobile;
        }
        
        public String getContent() {
        	return content;
        }
        
        public void setContent(String content) {
        	this.content = content;
        }
        
        public String getNickname() {
        	return nickname;
        }
        
        public void setNickname(String name) {
        	this.nickname = name;
        }
    }
    
    @Override
    public Response parseResponse(String responseStr) {
        return null;
    }

}
