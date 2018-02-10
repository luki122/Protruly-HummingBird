package com.protruly.music.online;

import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.protruly.music.R;

/**
 * Created by hujianwei on 17-9-1.
 */

public class HBNetTrackDetailFragment extends Fragment{

    private static final String TAG = "HBNetTrackDetailFragment";
    private HBNetTrackDetail mRecommend = null;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mRecommend = new HBNetTrackDetail();
        mRecommend.initview(getView(), getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return LayoutInflater.from(getActivity()).inflate(R.layout.hb_nettrackdetail_fragment,
                null);
    }

    @Override
    public void onPause() {
        super.onPause();
        mRecommend.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mRecommend.onResume();
    }
    public void playAll(int start){
        mRecommend.playAll(start);
    }
    public void showAnimation(){
        mRecommend.showAnimation();
    }
    public void Destroy(){
        mRecommend.Destroy();
    }
    public View getPlaySelect(){
        if(mRecommend!=null){
            return mRecommend.getPlaySelect();
        }
        return null;
    }
}
