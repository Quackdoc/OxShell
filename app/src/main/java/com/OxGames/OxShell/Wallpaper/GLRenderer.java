package com.OxGames.OxShell.Wallpaper;

import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;

import com.OxGames.OxShell.Data.Paths;
import com.OxGames.OxShell.Helpers.AndroidHelpers;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLRenderer implements GLSurfaceView.Renderer {
    private Shader shader;
    private int glVersion;
    private int width, height;

    public GLRenderer(int glVersion) {
        this.glVersion = glVersion;
    }

    public int getGLVersion() {
        return glVersion;
    }

    public void onTouchEvent(MotionEvent ev) {
        //Log.d("GLRenderer", ev.getRawX() + ", " + ev.getRawY());
        shader.setMousePos(ev.getX(), height - ev.getY());
    }
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        //Log.d("GLRenderer", "Surface created");
        //TODO: need to set up shader for when version is less than 2
//        if (glVersion < 0x20000)
//            throw new UnsupportedOperationException("OpenGL version 1 is unsupported");
        //Log.d("GLRenderer", AndroidHelpers.combinePaths(Paths.SHADER_ITEMS_DIR_INTERNAL, "vert.vsh") + ", " + AndroidHelpers.combinePaths(Paths.SHADER_ITEMS_DIR_INTERNAL, "frag.fsh"));
        String vert = null;
        String frag = null;
        String vertPath = AndroidHelpers.combinePaths(Paths.SHADER_ITEMS_DIR_INTERNAL, "vert.vsh");
        String fragPath = AndroidHelpers.combinePaths(Paths.SHADER_ITEMS_DIR_INTERNAL, "frag.fsh");
        if (AndroidHelpers.fileExists(vertPath))
            vert = AndroidHelpers.readFile(vertPath);
        if (AndroidHelpers.fileExists(fragPath))
            frag = AndroidHelpers.readFile(fragPath);
        shader = new Shader(glVersion, vert, frag);

        for (int i = 0; i < Shader.MAX_TEXTURE_COUNT; i++) {
            String currentImagePath = AndroidHelpers.combinePaths(Paths.SHADER_ITEMS_DIR_INTERNAL, "channel" + i + ".png");
            if (AndroidHelpers.fileExists(currentImagePath)) {
                Bitmap bitmap = AndroidHelpers.bitmapFromFile(currentImagePath);
                shader.bindTexture(bitmap, "iChannel" + i);
                bitmap.recycle();
            }
        }
    }
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        //Log.d("GLRenderer", "Surface changed");
        this.width = width;
        this.height = height;
        shader.setViewportSize(width, height);
    }
    @Override
    public void onDrawFrame(GL10 gl) {
        shader.draw();
    }
}
