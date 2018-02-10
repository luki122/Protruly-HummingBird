package com.hb.thememanager.ui.fragment;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.app.Dialog;
import android.app.Fragment;
import android.content.pm.PackageInfo;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.hb.thememanager.R;
import com.hb.thememanager.job.loader.ImageLoader;
import com.hb.thememanager.ui.adapter.AbsHomeThemeListAdapter;
import com.hb.thememanager.utils.ToastUtils;
import com.hb.thememanager.views.ThemeListView;
/**
 *主界面每个Tab对应的界面需要继承这个类去实现自己的内容 
 *
 */
public abstract class AbsRingTongFragment extends AbsHomeFragment{

	public WebView mWebView;
	private View mContentView;
	protected boolean searchStart = false;

	public AbsRingTongFragment(){}
	public AbsRingTongFragment(CharSequence title){
		mTitle = title;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);
	}
	
	public CharSequence getTitle(){
		return mTitle;
	}
	
	public void setTitle(CharSequence title){
		mTitle = title;
	}

	@Override
	public View onCreateNormalView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
//        hookWebView();
		Log.e("huliang", "!!!create mWebView");
		// TODO Auto-generated method stub
		if(mContentView == null) {
			mContentView = inflater.inflate(R.layout.ringtong_home_fragment, container, false);
		}
		mWebView = (WebView) mContentView.findViewById(R.id.webView);
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.getSettings().setAppCacheEnabled(true);
		mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
		mWebView.getSettings().setTextZoom(100);
		mWebView.setWebViewClient(new WebViewClient(){
			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				if(searchStart){
					searchStart = false;
					mWebView.clearHistory();
				}
			}
		});
		return mContentView;
	}
	
	/**
	 * 显示Toast提示
	 * @param msg
	 */
	public void showToast(String msg){
		ToastUtils.showShortToast(getContext(), msg);
	}
	
	/**
	 * 显示网络错误或者无网络视图
	 * @param show
	 */
	public void showNetworkErrorView(boolean show){
		setState(EMPTY_STATE_NO_NETWORK);
	}

	/**
	 * 显示空视图
	 * @param show
	 */
	public void showEmptyView(boolean show){
		
	}
	
	/**
	 * 显示指定ID的Dialog
	 * @param dialogId
	 */
	public void showDialog(int dialogId){
		
		dismissDialog();
		
		mDialog = onCreateDialog(dialogId);
		if(mDialog != null){
			mDialog.show();
		}
	}
	
	/**
	 * 隐藏Dialog
	 */
	public void dismissDialog(){
		if(mDialog != null && mDialog.isShowing()){
			mDialog.dismiss();
		}
	}
	
	/**
	 * 根据不同的ID去创建不同作用的Dialog
	 * @param dialogId
	 * @return
	 */
	protected Dialog onCreateDialog(int dialogId){
		return null;
	}

	private void hookWebView(){
        Class<?> factoryClass = null;
        try {
            factoryClass = Class.forName("android.webkit.WebViewFactory");
            Method getProviderClassMethod = null;
            Object sProviderInstance = null;

            if (Build.VERSION.SDK_INT == 23) {
                getProviderClassMethod = factoryClass.getDeclaredMethod("getProviderClass");
                getProviderClassMethod.setAccessible(true);
                Class<?> providerClass = (Class<?>) getProviderClassMethod.invoke(factoryClass);
                Class<?> delegateClass = Class.forName("android.webkit.WebViewDelegate");
                Constructor<?> constructor = providerClass.getConstructor(delegateClass);
                if (constructor != null) {
                    constructor.setAccessible(true);
                    Constructor<?> constructor2 = delegateClass.getDeclaredConstructor();
                    constructor2.setAccessible(true);
                    sProviderInstance = constructor.newInstance(constructor2.newInstance());
                }
            } else if (Build.VERSION.SDK_INT == 22) {
                getProviderClassMethod = factoryClass.getDeclaredMethod("getFactoryClass");
                getProviderClassMethod.setAccessible(true);
                Class<?> providerClass = (Class<?>) getProviderClassMethod.invoke(factoryClass);
                Class<?> delegateClass = Class.forName("android.webkit.WebViewDelegate");
                Constructor<?> constructor = providerClass.getConstructor(delegateClass);
                if (constructor != null) {
                    constructor.setAccessible(true);
                    Constructor<?> constructor2 = delegateClass.getDeclaredConstructor();
                    constructor2.setAccessible(true);
                    sProviderInstance = constructor.newInstance(constructor2.newInstance());
                }
            } else if (Build.VERSION.SDK_INT == 21) {//Android 21无WebView安全限制
                getProviderClassMethod = factoryClass.getDeclaredMethod("getFactoryClass");
                getProviderClassMethod.setAccessible(true);
                Class<?> providerClass = (Class<?>) getProviderClassMethod.invoke(factoryClass);
                sProviderInstance = providerClass.newInstance();
            }
            if (sProviderInstance != null) {
                Log.i("huliang", sProviderInstance.toString());
                Field field = factoryClass.getDeclaredField("sProviderInstance");
                field.setAccessible(true);
                field.set("sProviderInstance", sProviderInstance);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

	
	
}
