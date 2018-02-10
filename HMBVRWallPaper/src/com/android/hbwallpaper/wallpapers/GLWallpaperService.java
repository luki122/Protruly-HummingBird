package com.android.hbwallpaper.wallpapers;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Build;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;

import com.android.hbwallpaper.renderers.RendererCountry;
import com.android.hbwallpaper.utils.LoggerConfig;

public abstract class GLWallpaperService extends WallpaperService {
	public class GLEngine extends Engine {
		Renderer mRenderer;
		class WallpaperGLSurfaceView extends GLSurfaceView {
			private static final String TAG = "WallpaperGLSurfaceView";

			WallpaperGLSurfaceView(Context context) {
				super(context);

				if (LoggerConfig.ON) {
					Log.d(TAG, "WallpaperGLSurfaceView(" + context + ")");
				}
			}

			@Override
			public SurfaceHolder getHolder() {
				if (LoggerConfig.ON) {
					Log.d(TAG, "getHolder(): returning " + getSurfaceHolder());
				}

				return getSurfaceHolder();
			}

			public void onDestroy() {
				if (LoggerConfig.ON) {
					Log.d(TAG, "onDestroy()");
				}

				super.onDetachedFromWindow();
			}

			@Override
			public void onResume() {
				super.onResume();
				if (LoggerConfig.ON) {
					Log.d(TAG, "onResume()");
				}
			}

			@Override
			public void onPause() {
				super.onPause();
				if (LoggerConfig.ON) {
					Log.d(TAG, "onPause()");
				}
			}
		}

		private static final String TAG = "GLEngine";

		private WallpaperGLSurfaceView glSurfaceView;
		private boolean rendererHasBeenSet;		

		@Override
		public void onCreate(SurfaceHolder surfaceHolder) {
			if (LoggerConfig.ON) {
				Log.d(TAG, "onCreate(" + surfaceHolder + ")");
			}
			super.onCreate(surfaceHolder);

			glSurfaceView = new WallpaperGLSurfaceView(GLWallpaperService.this);
		}

		@Override
		public void onVisibilityChanged(boolean visible) {
			if (LoggerConfig.ON) {
				Log.d(TAG, "onVisibilityChanged(" + visible + ") :: rendererHasBeenSet = "+rendererHasBeenSet);
			}

			super.onVisibilityChanged(visible);

			if (rendererHasBeenSet) {
				if (visible) {
					glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
					glSurfaceView.onResume();
					((RendererCountry)mRenderer).onResume();
				} else {
					glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
					((RendererCountry)mRenderer).onPause();
					//glSurfaceView.onPause();
				}
			}
		}

		@Override
		public void onDestroy() {
			if (LoggerConfig.ON) {
				Log.d(TAG, "onDestroy()");
			}
			super.onDestroy();
			glSurfaceView.onDestroy();
			if(mRenderer!=null){
				((RendererCountry)mRenderer).onDestroy();
			}
		}
		
		protected void setRenderer(Renderer renderer) {
			if (LoggerConfig.ON) {
				Log.d(TAG, "setRenderer(" + renderer + ")");
			}
			mRenderer = renderer;
			glSurfaceView.setRenderer(renderer);
			glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
			rendererHasBeenSet = true;
		}
		
		protected void setPreserveEGLContextOnPause(boolean preserve) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				if (LoggerConfig.ON) {
					Log.d(TAG, "setPreserveEGLContextOnPause(" + preserve + ")");
				}
				glSurfaceView.setPreserveEGLContextOnPause(preserve);
			}
		}		

		protected void setEGLContextClientVersion(int version) {
			if (LoggerConfig.ON) {
				Log.d(TAG, "setEGLContextClientVersion(" + version + ")");
			}
			glSurfaceView.setEGLContextClientVersion(version);
		}
	}
}
