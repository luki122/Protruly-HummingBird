package com.android.hbwallpaper;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.Nullable;
import com.android.hbwallpaper.renderers.RendererCountry;

public class VrContextActivity extends Activity{

    private GLSurfaceView mGLView;
    RendererCountry mRendererCountry;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.glview);
        mRendererCountry = new RendererCountry(this,"vr/360sp.jpg");
        mGLView=(GLSurfaceView) findViewById(R.id.mGLView);
        mGLView.setEGLContextClientVersion(2);
        mGLView.setRenderer(mRendererCountry);
        mGLView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRendererCountry.onResume();
        mGLView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mRendererCountry.onPause();
        mGLView.onPause();
    }

    @Override
    protected void onUserLeaveHint() {
       super.onUserLeaveHint();
    }
}
