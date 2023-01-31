package com.OxGames.OxShell;

import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.OxGames.OxShell.Adapters.DetailAdapter;
import com.OxGames.OxShell.Data.DetailItem;
import com.OxGames.OxShell.Data.SettingsKeeper;
import com.OxGames.OxShell.Data.ShortcutsCache;
import com.OxGames.OxShell.Helpers.ActivityManager;
import com.OxGames.OxShell.Helpers.AndroidHelpers;
import com.OxGames.OxShell.Helpers.LogcatHelper;
import com.OxGames.OxShell.Interfaces.InputReceiver;
import com.OxGames.OxShell.Interfaces.PermissionsListener;
import com.OxGames.OxShell.Interfaces.Refreshable;
import com.OxGames.OxShell.Views.SlideTouchListView;
import com.appspell.shaderview.ShaderView;
import com.appspell.shaderview.gl.params.ShaderParamsBuilder;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

public class PagedActivity extends AppCompatActivity {
    protected Hashtable<ActivityManager.Page, View> allPages = new Hashtable<>();
    //    private static PagedActivity instance;
//    public static DisplayMetrics displayMetrics;
    private List<PermissionsListener> permissionListeners = new ArrayList<>();
    protected ActivityManager.Page currentPage;

    private static boolean startTimeSet;
    private static long startTime;
    //private static long prevFrameTime;

    //private ShaderView shaderView;
    private SlideTouchListView settingsDrawer;
    private boolean settingsDrawerOpen = false;
    private int settingsDrawerWidth = 512;
    //private static final float SETTINGS_DRAWER_OPEN_X = 50;
    private static final float SETTINGS_DRAWER_OPEN_Y = 0;
    //private static final float SETTINGS_DRAWER_CLOSED_X = 0;
    private static final float SETTINGS_DRAWER_CLOSED_Y = 0;
    private static final long SETTINGS_DRAWER_ANIM_TIME = 300;

    private int systemUIVisibility = View.SYSTEM_UI_FLAG_VISIBLE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        trySetStartTime();
        super.onCreate(savedInstanceState);
//        instance = this;

        ActivityManager.init();
        ActivityManager.instanceCreated(this);

        SettingsKeeper.loadOrCreateSettings();
        if (SettingsKeeper.fileDidNotExist()) {
            ShortcutsCache.createAndStoreDefaults();
            Log.i("PagedActivity", "Settings did not exist, first time launch");
        } else {
            ShortcutsCache.readIntentsFromDisk();
            int timesLoaded = 0;
            if (SettingsKeeper.hasValue(SettingsKeeper.TIMES_LOADED))
                timesLoaded = (int) SettingsKeeper.getValue(SettingsKeeper.TIMES_LOADED);
            Log.i("PagedActivity", "Settings existed, activity launched " + timesLoaded + " time(s)");
        }

        LogcatHelper.getInstance(this).start();
        //int mPId = android.os.Process.myPid();
        //Log.d("PagedActivity", "pid: " + mPId);

        //HideActionBar();

//        InitViewsTable();

//        RefreshDisplayMetrics();

//        HomeManager.Init();
        Log.i("PagedActivity", "OnCreate " + this);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        //initBackground();
    }

    @Override
    protected void onResume() {
        ActivityManager.setCurrent(currentPage);
        goTo(currentPage);
        super.onResume();

        settingsDrawer = findViewById(R.id.settings_drawer);
        fixDrawerLayout();
        showContextDrawer(isContextDrawerOpen());
        //settingsDrawer.setX(settingsDrawerWidth);
        //Add an if statement later to have a setting for hiding status bar
        hideActionBar();
        //setFullscreen(true);
        //setNavBarHidden(true);
        //setStatusBarHidden(true);
        //resumeBackground();
//        final WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
//        if (AndroidHelpers.hasReadStoragePermission()) {
//            //Log.d("PagedActivity", "Getting wallpaper drawable");
//            WallpaperInfo info = wallpaperManager.getWallpaperInfo();
//            Log.d("PagedActivity", "Wallpaper info: " + info);
//            final Drawable wallpaperDrawable;
//            if (info != null) {
//                wallpaperDrawable = info.loadIcon(getPackageManager());
//            } else
//                wallpaperDrawable = wallpaperManager.getDrawable();
//
//            ImageView bg = findViewById(R.id.bgView);
//            bg.setBackground(wallpaperDrawable);
//        } else {
//            final Drawable wallpaperDrawable = wallpaperManager.getBuiltInDrawable();
//            ImageView bg = findViewById(R.id.bgView);
//            bg.setBackground(wallpaperDrawable);
//        }

        Log.i("PagedActivity", "OnResume " + this);
    }
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        // TODO: store non-persistent data (like where we are in the app and other stuff) for when leaving the app temporarily
        // use outState.putFloat, putInt, putString...
        super.onSaveInstanceState(outState);
    }
    @Override
    protected void onPause() {
        Log.i("PagedActivity", "OnPause " + this);
        //pauseBackground();
        super.onPause();
    }
    @Override
    protected void onStop() {
        Log.i("PagedActivity", "OnStop " + this);
        super.onStop();
    }
    @Override
    protected void onDestroy() {
        Log.i("PagedActivity", "OnDestroy " + this);
        LogcatHelper.getInstance(this).stop();
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        fixDrawerLayout();
        showContextDrawer(isContextDrawerOpen());
    }
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        Log.i("PagedActivity", "OnWindowFocusChanged " + this);
        super.onWindowFocusChanged(hasFocus);
        hideActionBar();
        //setFullscreen(true);
        //setNavBarHidden(true);
        //setStatusBarHidden(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (PermissionsListener pl : permissionListeners) {
            pl.onPermissionResponse(requestCode, permissions, grantResults);
        }
    }

    // TODO: Add more customizability for controls
//    @Override
//    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
//        // source: https://stackoverflow.com/questions/34536195/how-to-differentiate-a-d-pad-movement-from-a-joystick-movement/58510631#58510631
//        Log.d("PagedActivity", "x: " + ev.getAxisValue(MotionEvent.AXIS_X) + " y: " + ev.getAxisValue(MotionEvent.AXIS_Y) + " z: " + ev.getAxisValue(MotionEvent.AXIS_Z) + " rz: " + ev.getAxisValue(MotionEvent.AXIS_RZ));
//        return super.dispatchGenericMotionEvent(ev);
//    }
    @Override
    public boolean dispatchKeyEvent(KeyEvent key_event) {
        //Log.d("PagedActivity", key_event.toString());

        if (key_event.getAction() == KeyEvent.ACTION_DOWN) {
            if (key_event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_Y) {
                Log.d("PagedActivity", "Attempting to convert button keycode to app switch");
                AccessService.showRecentApps();
                return true;
            }
        }
//        if (key_event.getAction() == KeyEvent.ACTION_UP) {
//            if (key_event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_Y) {
//                Log.d("PagedActivity", "Attempting to bring up app switcher");
//                //sendKeyEvent(this, KeyEvent.KEYCODE_APP_SWITCH, KeyEvent.ACTION_DOWN, 0);
//                sendKeyEvent(KeyEvent.KEYCODE_APP_SWITCH, KeyEvent.ACTION_UP);
//                return true;
//            }
//        }

        if (isContextDrawerOpen() && settingsDrawer.receiveKeyEvent(key_event))
            return true;

        boolean childsPlay = false;
        View currentView = allPages.get(currentPage);
        if (currentView instanceof InputReceiver)
            childsPlay = ((InputReceiver) currentView).receiveKeyEvent(key_event);
        if (childsPlay)
            return true;

        return super.dispatchKeyEvent(key_event);
    }
    private static void sendKeyEvent(View targetView, KeyEvent keyEvent) {
        //Reference: https://developer.android.com/reference/android/view/inputmethod/InputConnection#sendKeyEvent(android.view.KeyEvent)
        //& https://stackoverflow.com/questions/13026505/how-can-i-send-key-events-in-android
        //long eventTime = SystemClock.uptimeMillis();
        BaseInputConnection mInputConnection = new BaseInputConnection(targetView, true);
        mInputConnection.sendKeyEvent(keyEvent);
        //dispatchKeyEvent(new KeyEvent(eventTime, eventTime, action, keyeventcode, 0));
    }

    public static class ContextBtn {
        String label;
        Callable event;
        public ContextBtn(String label, Callable event) {
            this.label = label;
            this.event = event;
        }
    }
    public void setContextDrawerBtns(ContextBtn... btns) {
        DetailAdapter listAdapter = new DetailAdapter(this);
        for (int i = 0; i < btns.length; i++) {
            ContextBtn btn = btns[i];
            listAdapter.add(new DetailItem(null, btn.label, null, null));
            int btnIndex = i;
            settingsDrawer.addListener(index -> {
                if (btnIndex == index) {
                    try {
                        if (btn.event != null)
                            btn.event.call();
                        else
                            Log.e("PagedActivity", "Button event for " + btn.label + " is null");
                    } catch (Exception ex) {
                        Log.e("PagedActivity", "Failed to call context event: " + ex);
                    }
                }
            });
        }
        settingsDrawer.setAdapter(listAdapter);
    }
    public void showContextDrawer(boolean onOff) {
        settingsDrawerOpen = onOff;
        float settingsDrawerOpenX = OxShellApp.getDisplayWidth() - settingsDrawerWidth;
        float settingsDrawerClosedX = OxShellApp.getDisplayWidth();
        float xDist = (settingsDrawerOpen ? settingsDrawerOpenX : settingsDrawerClosedX) - settingsDrawer.getX();
        float yDist = (settingsDrawerOpen ? SETTINGS_DRAWER_OPEN_Y : SETTINGS_DRAWER_CLOSED_Y) - settingsDrawer.getY();
        float alphaDist = (settingsDrawerOpen ? 1 : 0) - settingsDrawer.getAlpha();
        long duration = Math.round(SETTINGS_DRAWER_ANIM_TIME * (Math.abs(xDist) / Math.abs(settingsDrawerClosedX - settingsDrawerOpenX)));
        //Log.d("PagedActivity", "Settings view open: " + settingsDrawerOpen + " x: " + settingsDrawer.getX() + " xdist: " + xDist + " ydist: " + yDist + " alphadist: " + alphaDist + " duration: " + duration);
        settingsDrawer.animate().setDuration(duration);
        settingsDrawer.animate().xBy(xDist);
        settingsDrawer.animate().yBy(yDist);
        settingsDrawer.animate().alphaBy(alphaDist);
    }
    public boolean isContextDrawerOpen() {
        return settingsDrawerOpen;
    }
    private void fixDrawerLayout() {
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams)settingsDrawer.getLayoutParams();
        //settingsDrawer.setGravity(Gravity.TOP | Gravity.END);
        //layoutParams.gravity = Gravity.TOP | Gravity.END;
        layoutParams.width = settingsDrawerWidth;
        settingsDrawer.setLayoutParams(layoutParams);
        settingsDrawer.setX(OxShellApp.getDisplayWidth());
    }

    private void trySetStartTime() {
        if (!startTimeSet) {
            startTimeSet = true;
            startTime = System.currentTimeMillis();
            //prevFrameTime = startTime;
        }
    }

    public ShaderView getBackground() {
        return null;
        //return findViewById(R.id.bgShader);
    }
    private void pauseBackground() {
        ShaderView shaderView = getBackground();
        shaderView.setUpdateContinuously(false);
    }
    private void resumeBackground() {
        //Later will have more logic based on options set by user
        ShaderView shaderView = getBackground();
        shaderView.setUpdateContinuously(true);
        shaderView.setFramerate(30);
    }
    private void initBackground() {
        ShaderView shaderView = getBackground();
        Log.d("Paged Activity", "Shader view null: " + (shaderView == null));
        if (shaderView != null) {
            shaderView.setFragmentShader(AndroidHelpers.readAssetAsString(this, "xmb.fsh"));
            shaderView.setVertexShader(AndroidHelpers.readAssetAsString(this, "default.vsh"));
            ShaderParamsBuilder paramsBuilder = new ShaderParamsBuilder();
            paramsBuilder.addFloat("iTime", 0f);
            //DisplayMetrics displayMetrics = ActivityManager.getCurrentActivity().getDisplayMetrics();
            paramsBuilder.addVec2i("iResolution", new int[] { OxShellApp.getDisplayWidth(), OxShellApp.getDisplayHeight() });
            shaderView.setShaderParams(paramsBuilder.build());
            shaderView.setOnDrawFrameListener(shaderParams -> {
                //float deltaTime = (System.currentTimeMillis() - prevFrameTime) / 1000f;
                //float fps = 1f / deltaTime;
                //Log.d("FPS", String.valueOf(fps));
                //prevFrameTime = System.currentTimeMillis();
                float secondsElapsed = (System.currentTimeMillis() - startTime) / 1000f;
                shaderParams.updateValue("iTime", secondsElapsed);
                return null;
            });
        }
    }

    public void addPermissionListener(PermissionsListener listener) {
        permissionListeners.add(listener);
    }

//    public static PagedActivity GetInstance() {
//        return instance;
//    }

    private void hideActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.hide();
    }
    private void setFullscreen(boolean onOff) {
        if (onOff)
            systemUIVisibility |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        else
            systemUIVisibility &= ~(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        updateSystemUIVisibility();
    }
    private void setStatusBarHidden(boolean onOff) {
        if (onOff)
            systemUIVisibility |= View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        else if ((systemUIVisibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0)
            systemUIVisibility &= ~(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        else
            systemUIVisibility &= ~View.SYSTEM_UI_FLAG_FULLSCREEN;
        updateSystemUIVisibility();
    }
    private void setNavBarHidden(boolean onOff) {
        if (onOff)
            systemUIVisibility |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        else if ((systemUIVisibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0)
            systemUIVisibility &= ~(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        else
            systemUIVisibility &= ~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        updateSystemUIVisibility();
    }
    private void updateSystemUIVisibility() {
        getWindow().getDecorView().setSystemUiVisibility(systemUIVisibility);
    }

//    public void RefreshDisplayMetrics() {
//        displayMetrics = new DisplayMetrics();
//        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
//    }
//    public DisplayMetrics getDisplayMetrics() {
//        DisplayMetrics displayMetrics = new DisplayMetrics();
//        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
//        return displayMetrics;
//    }

    protected void initViewsTable() {
    }
    public View getView(ActivityManager.Page page) {
        return allPages.get(page);
    }
    public void goTo(ActivityManager.Page page) {
        if (currentPage != page) {
            Set<Map.Entry<ActivityManager.Page, View>> entrySet = allPages.entrySet();
            for (Map.Entry<ActivityManager.Page, View> entry : entrySet) {
                entry.getValue().setVisibility(page == entry.getKey() ? View.VISIBLE : View.GONE);
                if (page == entry.getKey()) {
                    View nextPage = entry.getValue();
                    if (nextPage instanceof Refreshable)
                        ((Refreshable)nextPage).refresh();
                    nextPage.requestFocusFromTouch();

                    if (nextPage instanceof Refreshable)
                        ((Refreshable)nextPage).refresh();
                    currentPage = page;
                }
            }
        }
    }
}
