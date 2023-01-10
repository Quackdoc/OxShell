package com.OxGames.OxShell.Wallpaper;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;

//Source: https://www.learnopengles.com/how-to-use-opengl-es-2-in-an-android-live-wallpaper/
public class GLWallpaperService extends WallpaperService {
    @Override
    public Engine onCreateEngine() {
        return new GLEngine();
    }

    public class GLEngine extends WallpaperService.Engine {
        private WallpaperGLSurfaceView glSurfaceView;
        private boolean rendererHasBeenSet;

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            glSurfaceView = new WallpaperGLSurfaceView(GLWallpaperService.this);
        }
        @Override
        public void onDestroy() {
            super.onDestroy();
            glSurfaceView.onDestroy();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            //Log.d("GLEngine", "Is visible " + visible + " renderer is set " + rendererHasBeenSet);

            if (rendererHasBeenSet) {
                if (visible) {
                    glSurfaceView.onResume();
                } else {
                    glSurfaceView.onPause();
                }
            }
        }

        protected void setRenderer(GLSurfaceView.Renderer renderer) {
            glSurfaceView.setRenderer(renderer);
            rendererHasBeenSet = true;
        }
        protected void setEGLContextClientVersion(int version) {
            glSurfaceView.setEGLContextClientVersion(version);
        }
        protected void setPreserveEGLContextOnPause(boolean preserve) {
            glSurfaceView.setPreserveEGLContextOnPause(preserve);
        }

        public class WallpaperGLSurfaceView extends GLSurfaceView {
            WallpaperGLSurfaceView(Context context) {
                super(context);
            }
            @Override
            public SurfaceHolder getHolder() {
                return getSurfaceHolder();
            }
            public void onDestroy() {
                super.onDetachedFromWindow();
            }
        }
    }
}
