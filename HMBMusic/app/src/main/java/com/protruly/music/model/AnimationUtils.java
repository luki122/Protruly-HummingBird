package com.protruly.music.model;

import android.content.Context;
import android.content.res.XmlResourceParser;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;

/**
 * Created by hujianwei on 17-8-31.
 */

public class AnimationUtils {
    public static String FRAME_ANIMATION_ITEM = "item";
    public static String FRAME_ANIMATION_DRAWABLE = "drawable";
    public static String FRAME_ANIMATION_DURATION = "duration";

    public ArrayList<Integer> mDrawableIds;
    public  ArrayList<Integer> mDuration;

    // android res xml
    public static String ANDROID_XMLNS = "http://schemas.android.com/apk/res/android";

    private  FrameAnimationItem startParseFrameDataXml(XmlResourceParser xpp) {
        FrameAnimationItem item = new FrameAnimationItem();
        if (AnimationUtils.FRAME_ANIMATION_ITEM.equals(xpp.getName())) {

            String drawableId = xpp.getAttributeValue(AnimationUtils.ANDROID_XMLNS,
                    AnimationUtils.FRAME_ANIMATION_DRAWABLE);
            String duration = xpp.getAttributeValue(AnimationUtils.ANDROID_XMLNS,
                    AnimationUtils.FRAME_ANIMATION_DURATION);
            if (drawableId != null) {
                item.setDrawable((Integer.valueOf(drawableId.replace("@", ""))));// 通过R.string设置
            }
            if (duration != null) {
                item.setDuration(Integer.valueOf(duration));
            }
        }
        return item;
    }

    public void parseFrame(Context context, int xml) {
        mDrawableIds =new ArrayList<Integer>();
        mDuration = new ArrayList<Integer>();
        FrameAnimationItem item = null;
        try {
            XmlResourceParser xpp = context.getResources().getXml(xml);
            xpp.next();
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    item = startParseFrameDataXml(xpp);
                    if(item != null){

                        mDrawableIds.add(item.getDrawable());
                        //mDuration.add(item.getDuration());
                        mDuration.add(8);
                    }
                }
                eventType = xpp.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Integer> getImages(){
        return mDrawableIds;
    }
    public ArrayList<Integer> getDuration(){
        return mDuration;
    }
}
