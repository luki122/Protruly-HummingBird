package com.hb.thememanager.ui.fragment;

import hb.widget.tab.TabLayout;
import hb.widget.toolbar.Toolbar;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;

import com.hb.thememanager.model.Theme;
import com.hb.thememanager.ui.HomePage;
import com.hb.thememanager.ui.adapter.AbsHomeThemeListAdapter;
import com.hb.thememanager.ui.fragment.ringtone.setTo;
import com.hb.thememanager.R;
/**
 * 铃声Tab内容页面
 *
 */
public class HomeRingTongFragment extends AbsRingTongFragment {

    private Toolbar mTabToolbar;
    private Toolbar  mRingToneToolbar;
    private Handler mHandler ;
    private boolean mAllowScroll;
    private boolean mSearch = false; 
    private String searchUri ;
    private static String mSearchBaseUri = "https://iring.diyring.cc/ss/b66f7ea64c1c4a36?word=";
    private static String mRingtoneUri = "https://iring.diyring.cc/friend/b66f7ea64c1c4a36";

    public HomeRingTongFragment(){
    }

    public HomeRingTongFragment(CharSequence title) {
        super(title);
        // TODO Auto-generated constructor stub
    }

    public HomeRingTongFragment(CharSequence title, boolean bSeatch) {
        super(title);
        // TODO Auto-generated constructor stub
        mSearch = bSeatch;
    }
    
    @Override
    protected int getThemeType() {
        return Theme.RINGTONE;
    }

    @Override
    protected AbsHomeThemeListAdapter createAdapter() {
        return null;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onActivityCreated(savedInstanceState);
    }
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onViewCreated(view, savedInstanceState);
        
        if ( false == mSearch) {
        	setHomeView(mRingtoneUri) ;

        	mTabToolbar = ((HomePage) this.getActivity()).getToolbar();
            mRingToneToolbar = ((HomePage) this.getActivity()).getRingtoneToolbar();
            mRingToneToolbar.setNavigationOnClickListener(new View.OnClickListener () {
    			@Override
    			public void onClick(View arg0) {
    				// TODO Auto-generated method stub
    				if (mWebView.canGoBack()) {
    					mWebView.goBack();
    				}
    			}
            });
        } else {
        	setHomeView(searchUri) ;
        }
        
        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg){
                super.handleMessage(msg);
                if(msg.what == 1){
                	 if ( false == mSearch) {
                		 String title = msg.getData().getString("title");
                		 syncTitle(title);
                	 }
                }
            }
        };
    }

    @Override
	public void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		try {
			mWebView.loadUrl("javascript:KY.ine.stopPlay()");
		} catch (Exception e) {
			Log.e("huliang", "", e);
		}
	}

    public void setHomeView(String Uri) {
        if (null != mWebView) {
            Log.e("huliang", "loadUrl");
            mWebView.loadUrl(Uri);
            mWebView.addJavascriptInterface(this, "KuYinExt");
        } else {
            Log.e("huliang", "mWebView is null");
        }
    }
    
    public void searchRingtone(String target) {
        searchStart = true;
        searchUri = mSearchBaseUri + target;
        setHomeView(searchUri);
    }
    
    private void syncTitle(String title) {
        Log.e("huliang", "set Title" + title);
        if (title.equals("铃声")) {
            Log.e("huliang", "show toobar");
            if (null != mTabToolbar) {
                mTabToolbar.setVisibility(View.VISIBLE);
                mRingToneToolbar.setVisibility(View.GONE);
                mTabToolbar.postInvalidate();
                mRingToneToolbar.postInvalidate();
                mAllowScroll = true;
            }
        } else {
            Log.e("huliang", "hide toobar");
            if (null != mTabToolbar) {
                mTabToolbar.setVisibility(View.GONE);
                mRingToneToolbar.setVisibility(View.VISIBLE);
                mRingToneToolbar.setTitle(title);
                mTabToolbar.postInvalidate();
                mRingToneToolbar.postInvalidate();
                mAllowScroll = false;
            }
        }
       ((HomePage) this.getActivity()).getViewPager().allowScroll(mAllowScroll);
    }
    
    @JavascriptInterface
    public  void setTitle(String title) {
        Log.e("huliang", "settitle :" + title);
        if(2 !=  ((HomePage) this.getActivity()).getCurrentPage()) {
            return;
        }
        Message message = new Message();
        message.what = 1;
        Bundle bundle = new Bundle();    
        bundle.putString("title",title);  //往Bundle中存放数据   
        message.setData(bundle);   
        mHandler.sendMessage(message);
    }

    @JavascriptInterface
    public void downloadRing(String wno, String wurl, String wname, String wsinger, String wsize, String wtime) {
        Log.e("huliang", "!!!!!downloadRing wno:" + wno + " wurl:" +wurl + "  wname:"+wname + " wsinger:" + wsinger +"　wsize:" + wsize + " wtime" + wtime);
        // pop menu
        Intent it = new Intent(this.getContext(), setTo.class);
        it.putExtra("wno", wno);
        it.putExtra("uri", wurl);
        it.putExtra("wname", wname);
        it.putExtra("wsinger", wsinger);
        it.putExtra("wsize", wsize);
        it.putExtra("wtime", wtime);
        this.getContext().startActivity(it);
    }

    @JavascriptInterface
    public void disableTouchArea(String top, String bottom) {
    	try {
    		((HomePage) this.getActivity()).getViewPager().setDisableRect(0,  Integer.parseInt(top)*3, 1080,  Integer.parseInt(bottom)*3);
    	} catch (Exception e){
//    		Log.e("huliang", "", e);
    	}
    }
    
    @JavascriptInterface
    public void resetTouchArea(){
    	try {
    		((HomePage) this.getActivity()).getViewPager().setDisableRect(0,  0, 0, 0);
    	} catch (Exception e){
//    		Log.e("huliang", "", e);
    	}
    }

}
