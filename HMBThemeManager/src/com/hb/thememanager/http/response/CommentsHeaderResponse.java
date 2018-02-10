package com.hb.thememanager.http.response;


import com.hb.thememanager.http.response.adapter.CommentHeaderBody;
import com.hb.thememanager.http.response.adapter.ResponseBody;

/**
 * Created by alexluo on 17-8-3.
 */

public class CommentsHeaderResponse extends Response {


    public CommentHeaderBody body;



    public float getScore(){
        return body.score;
    }

    public int getStart1(){

        return body.star1;

    }

    public int getStart2(){

        return body.star2;

    }

    public int getStart3(){

        return body.star3;

    }

    public int getStart4(){

        return body.star4;

    }

    public int getStart5(){

        return body.star5;

    }
    @Override
    public ResponseBody returnBody() {
        return body;
    }


}
