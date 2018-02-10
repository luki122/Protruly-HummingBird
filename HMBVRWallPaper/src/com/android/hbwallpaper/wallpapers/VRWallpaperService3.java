package com.android.hbwallpaper.wallpapers;

import android.opengl.GLSurfaceView.Renderer;

import com.android.hbwallpaper.renderers.RendererCountry;

public class VRWallpaperService3 extends OpenGLES2WallpaperService {
	@Override
	Renderer getNewRenderer() {
		return new RendererCountry(this.getApplicationContext(),"vr/360sp3.jpg");
	}
}
