package com.hb.thememanager.ui.mvpview;

import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.model.Comments;
import com.hb.thememanager.ui.BaseView;

import java.util.List;

/**
 * Created by alexluo on 17-8-3.
 */

public interface CommentsView extends BaseView{

    public void updateComments(Response comments);

    public void updateCommentsHeader(Response obj);

}
