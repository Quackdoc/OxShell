package com.OxGames.OxShell;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.net.Uri;
import android.opengl.GLES32;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.BaseInputConnection;
import android.widget.FrameLayout;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.OxGames.OxShell.Data.KeyComboAction;
import com.OxGames.OxShell.Data.Paths;
import com.OxGames.OxShell.Data.SettingsKeeper;
import com.OxGames.OxShell.Helpers.AndroidHelpers;
import com.OxGames.OxShell.Helpers.LogcatHelper;
import com.OxGames.OxShell.Helpers.MusicPlayer;
import com.OxGames.OxShell.Views.DebugView;
import com.OxGames.OxShell.Views.DynamicInputView;
import com.OxGames.OxShell.Views.PromptView;
import com.OxGames.OxShell.Views.SettingsDrawer;
import com.appspell.shaderview.ShaderView;
import com.appspell.shaderview.gl.params.ShaderParamsBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

public class PagedActivity extends AppCompatActivity {
    private final ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (PagedActivity.this.onReceivedResult != null)
                PagedActivity.this.onReceivedResult.accept(result);
        });
    private final ActivityResultLauncher<String[]> mStartForContent = registerForActivityResult(new ActivityResultContracts.OpenDocument(),
        uri -> {
            Log.i("HomeActivity", "Received result from activity " + uri);
            if (uri != null) {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                if (PagedActivity.this.onReceivedContent != null)
                    PagedActivity.this.onReceivedContent.accept(uri);
            }
        });
    // source: https://stackoverflow.com/a/70933975/5430992
    private final ActivityResultLauncher<Uri> mDirRequest = registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(),
        uri -> {
            Log.i("HomeActivity", "Received result from activity " + uri);
            if (uri != null) {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                //AndroidHelpers.queryUri(uri);
                if (PagedActivity.this.onReceivedDir != null)
                    PagedActivity.this.onReceivedDir.accept(uri);
            }
        });
    private final ActivityResultLauncher<String> mCreateRequest = registerForActivityResult(new ActivityResultContracts.CreateDocument("application/zip"),
            uri -> {
                Log.i("HomeActivity", "Received result from activity " + uri);
                if (uri != null) {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    if (PagedActivity.this.onReceivedContent != null)
                        PagedActivity.this.onReceivedContent.accept(uri);
                }
            });
    private Consumer<ActivityResult> onReceivedResult;
    private Consumer<Uri> onReceivedContent;
    private Consumer<Uri> onReceivedDir;

    public void requestResult(Intent intent, Consumer<ActivityResult> onReceived) {
        this.onReceivedResult = onReceived;
        mStartForResult.launch(intent);
    }
    public void requestContent(Consumer<Uri> onReceived, String... types) {
        // some mime types: https://stackoverflow.com/questions/23385520/android-available-mime-types
        this.onReceivedContent = onReceived;
        mStartForContent.launch(types);
    }
    public void requestDirectoryAccess(Uri initialPath, Consumer<Uri> onReceived) {
        this.onReceivedDir = onReceived;
        mDirRequest.launch(initialPath);
    }
    public void requestCreateZipFile(Consumer<Uri> onReceived, String type) {
        // some mime types: https://stackoverflow.com/questions/23385520/android-available-mime-types
        this.onReceivedContent = onReceived;
        mCreateRequest.launch(type);
    }

    private final HashMap<Integer, List<Consumer<Boolean>>> permissionListeners = new HashMap<>();
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.i("PagedActivity", "Received result for " + requestCode + " permissions: " + Arrays.toString(permissions) + " grantResults: " + Arrays.toString(grantResults));
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissionListeners.containsKey(requestCode)) {
            List<Consumer<Boolean>> listeners = permissionListeners.get(requestCode);
            if (listeners != null) {
                for (Consumer<Boolean> listener : listeners)
                    if (listener != null)
                        listener.accept(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
                listeners.clear();
            }
        }
    }
    public void addOneTimePermissionListener(int requestCode, Consumer<Boolean> onResult) {
        if (!permissionListeners.containsKey(requestCode))
            permissionListeners.put(requestCode, new ArrayList<>());
        permissionListeners.get(requestCode).add(onResult);
    }

    private static final int SETTINGS_DRAWER_ID = View.generateViewId();
    private static final int DYNAMIC_INPUT_ID = View.generateViewId();
    private static final int PROMPT_ID = View.generateViewId();
    private static final int DEBUG_VIEW_ID = View.generateViewId();

    private ShaderView tvShaderBg;
    private FrameLayout tvImageBg;
    private static boolean startTimeSet;
    private static long startTime;
    //private static long prevFrameTime;

    private FrameLayout parentView;
    private DynamicInputView dynamicInput;
    private SettingsDrawer settingsDrawer;
    private PromptView prompt;
    private DebugView debugView;

    //private int systemUIVisibility = View.SYSTEM_UI_FLAG_VISIBLE;

    //private InputHandler inputHandler;
    //private boolean isKeyboardShown;
    //private ViewTreeObserver.OnGlobalLayoutListener keyboardListener;
    private List<KeyComboAction> accessPopupComboActions;
    private List<KeyComboAction> musicPlayerActions;
    KeyComboAction[] showDebugAction;

    private static final String homeAccessMsg = "Please enable Ox Shell as an accessibility service. Ox Shell uses your key presses to let you go home even when the app is not in use.";//"Ox Shell needs accessibility permission in order to go home when pressing this key combo";
    private static final String recentsAccessMsg = "Please enable Ox Shell as an accessibility service. Ox Shell uses your key presses to let you view recent apps even when the app is not in use.";//"Ox Shell needs accessibility permission in order to show recent apps when pressing this key combo";

    private void showAccessibilityPopup(String msg) {
        if (!AccessService.isEnabled() && !prompt.isPromptShown()) {
            PromptView prompt = getPrompt();
            prompt.setCenterOfScreen();
            prompt.setMessage(msg);
            prompt.setStartBtn("Continue", () -> {
                prompt.setShown(false);
                AndroidHelpers.requestAccessibilityService(granted -> {
                    Log.d("PagedActivity", "Accessibility permission granted: " + granted);
                });
            }, SettingsKeeper.getSuperPrimaryInput());
            prompt.setEndBtn("Cancel", () -> {
                prompt.setShown(false);
            }, SettingsKeeper.getCancelInput());
            prompt.setShown(true);
        }
    }
    public void refreshAccessibilityInput() {
        if (accessPopupComboActions != null)
            OxShellApp.getInputHandler().removeKeyComboActions(accessPopupComboActions.toArray(new KeyComboAction[0]));
        KeyComboAction[] recentsComboAction = Arrays.stream(SettingsKeeper.getRecentsCombos()).map(combo -> new KeyComboAction(combo, () -> showAccessibilityPopup(recentsAccessMsg))).toArray(KeyComboAction[]::new);
        KeyComboAction[] homeComboAction = Arrays.stream(SettingsKeeper.getHomeCombos()).map(combo -> new KeyComboAction(combo, () -> showAccessibilityPopup(homeAccessMsg))).toArray(KeyComboAction[]::new);
        accessPopupComboActions = new ArrayList<>();
        Collections.addAll(accessPopupComboActions, recentsComboAction);
        Collections.addAll(accessPopupComboActions, homeComboAction);
        OxShellApp.getInputHandler().addKeyComboActions(recentsComboAction);
        OxShellApp.getInputHandler().addKeyComboActions(homeComboAction);
    }
    public void refreshMusicPlayerInput() {
        if (musicPlayerActions != null)
            OxShellApp.getInputHandler().removeKeyComboActions(musicPlayerActions.toArray(new KeyComboAction[0]));
        KeyComboAction[] musicTogglePlayAction = Arrays.stream(SettingsKeeper.getMusicPlayerTogglePlayInput()).map(combo -> new KeyComboAction(combo, () -> {
            if (!AccessService.isEnabled())
                MusicPlayer.togglePlay();
        })).toArray(KeyComboAction[]::new);
        KeyComboAction[] musicStopAction = Arrays.stream(SettingsKeeper.getMusicPlayerStopInput()).map(combo -> new KeyComboAction(combo, () -> {
            if (!AccessService.isEnabled())
                MusicPlayer.stop();
        })).toArray(KeyComboAction[]::new);
        KeyComboAction[] musicSkipNextAction = Arrays.stream(SettingsKeeper.getMusicPlayerSkipNextInput()).map(combo -> new KeyComboAction(combo, () -> {
            if (!AccessService.isEnabled())
                MusicPlayer.seekToNext();
        })).toArray(KeyComboAction[]::new);
        KeyComboAction[] musicSkipPrevAction = Arrays.stream(SettingsKeeper.getMusicPlayerSkipPrevInput()).map(combo -> new KeyComboAction(combo, () -> {
            if (!AccessService.isEnabled())
                MusicPlayer.seekToPrev();
        })).toArray(KeyComboAction[]::new);
        KeyComboAction[] musicSeekForwardAction = Arrays.stream(SettingsKeeper.getMusicPlayerSeekForwardInput()).map(combo -> new KeyComboAction(combo, () -> {
            if (!AccessService.isEnabled())
                MusicPlayer.seekForward();
        })).toArray(KeyComboAction[]::new);
        KeyComboAction[] musicSeekBackAction = Arrays.stream(SettingsKeeper.getMusicPlayerSeekBackInput()).map(combo -> new KeyComboAction(combo, () -> {
            if (!AccessService.isEnabled())
                MusicPlayer.seekBack();
        })).toArray(KeyComboAction[]::new);
        musicPlayerActions = new ArrayList<>();
        Collections.addAll(musicPlayerActions, musicTogglePlayAction);
        Collections.addAll(musicPlayerActions, musicStopAction);
        Collections.addAll(musicPlayerActions, musicSkipNextAction);
        Collections.addAll(musicPlayerActions, musicSkipPrevAction);
        Collections.addAll(musicPlayerActions, musicSeekForwardAction);
        Collections.addAll(musicPlayerActions, musicSeekBackAction);
        OxShellApp.getInputHandler().addKeyComboActions(musicPlayerActions.toArray(new KeyComboAction[0]));
    }
    public void refreshShowDebugInput() {
        if (showDebugAction != null)
            OxShellApp.getInputHandler().removeKeyComboActions(showDebugAction);
        showDebugAction = Arrays.stream(SettingsKeeper.getShowDebugInput()).map(combo -> new KeyComboAction(combo, () -> getDebugView().setShown(!getDebugView().isDebugShown()))).toArray(KeyComboAction[]::new);
        OxShellApp.getInputHandler().addKeyComboActions(showDebugAction);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        OxShellApp.setCurrentActivity(this);
        refreshAccessibilityInput();
        refreshMusicPlayerInput();
        refreshShowDebugInput();

        //trySetStartTime();
        super.onCreate(savedInstanceState);

        LogcatHelper.getInstance(this).start();

        Log.i("PagedActivity", "OnCreate " + this);
        Log.i("PagedActivity", "Running on a tv: " + AndroidHelpers.isRunningOnTV());
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        prepareOtherViews();
        SettingsKeeper.reloadSystemUiState();
        //initBackground();
    }

    protected void setMarginsFor(boolean statusBar, boolean navBar, int... ids) {
        // sets the provided views' margins to avoid the status bar and navigation bar
        for (int id : ids) {
            View view = findViewById(id);
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)view.getLayoutParams();
            params.setMargins(0, statusBar ? OxShellApp.getStatusBarHeight() : 0, 0, navBar ? OxShellApp.getNavBarHeight() : 0);
            view.setLayoutParams(params);
        }
    }

    @Override
    protected void onResume() {
        OxShellApp.setCurrentActivity(this);
        //ActivityManager.setCurrent(currentPage);
        //goTo(currentPage);
        super.onResume();

        prepareOtherViews();
        //settingsDrawer.setShown(isContextDrawerOpen());
        //settingsDrawer.setX(settingsDrawerWidth);
        //Add an if statement later to have a setting for hiding status bar
        setActionBarHidden(true);
        SettingsKeeper.reloadCurrentSystemUiState();
        //setNavBarHidden(true);
        //setStatusBarHidden(true);
        //resetShaderViewBg();
        applyTvBg();
        resumeBackground();

        // TODO: find less hacky solution to active tag sometimes being null when coming back to Ox Shell
        if (tagFromPause != null) {
            if (OxShellApp.getInputHandler().getActiveTag() == null)
                OxShellApp.getInputHandler().setActiveTag(tagFromPause);
            tagFromPause = null;
        }
        Log.i("PagedActivity", "OnResume " + this);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        // TODO: store non-persistent data (like where we are in the app and other stuff) for when leaving the app temporarily
        // use outState.putFloat, putInt, putString...
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStart() {
        Log.i("PagedActivity", "OnStart " + this);
        //startCheckIfKeyboardOpen();

        super.onStart();
    }
//    private void startCheckIfKeyboardOpen() {
//        View rootView = getWindow().getDecorView();
//        if (keyboardListener != null)
//            rootView.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardListener);
//        keyboardListener = () -> {
//            Rect r = new Rect();
//            rootView.getWindowVisibleDisplayFrame(r);
//
//            int heightDiff = rootView.getHeight() - (r.bottom - r.top);
//            isKeyboardShown = heightDiff > 100;
//        };
//        rootView.getViewTreeObserver().addOnGlobalLayoutListener(keyboardListener);
//    }
    public boolean isKeyboardShown() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            return getWindow().getDecorView().getRootWindowInsets().isVisible(WindowInsets.Type.ime());
//        }
        Rect windowRect = new Rect();
        getWindow().getDecorView().getWindowVisibleDisplayFrame(windowRect);
        //Log.d("PagedActivity", "WindowRect: " + windowRect.bottom + " =? " + OxShellApp.getDisplayHeight());
        return windowRect.bottom < OxShellApp.getDisplayHeight();
        //return isKeyboardShown;
    }

    private String tagFromPause = null;
    @Override
    protected void onPause() {
        Log.i("PagedActivity", "OnPause " + this);
        //OxShellApp.setCurrentActivity(null);
        pauseBackground();
        tagFromPause = OxShellApp.getInputHandler().getActiveTag();
        super.onPause();
    }
    @Override
    protected void onStop() {
        Log.i("PagedActivity", "OnStop " + this);
        //inputHandler.clearKeyComboActions();
        super.onStop();
    }
    @Override
    protected void onDestroy() {
        Log.i("PagedActivity", "OnDestroy " + this);
        LogcatHelper.getInstance(this).stop();
        OxShellApp.getInputHandler().removeKeyComboActions(accessPopupComboActions.toArray(new KeyComboAction[0]));
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        Log.d("PagedActivity", "onConfigurationChanged");
        super.onConfigurationChanged(newConfig);
        //fixDrawerLayout();
        prepareOtherViews();
        //getStatusBarHeight();
        //settingsDrawer.setShown(isContextDrawerOpen());
    }
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        Log.i("PagedActivity", "OnWindowFocusChanged " + this);
        super.onWindowFocusChanged(hasFocus);
        setActionBarHidden(true);
        SettingsKeeper.reloadCurrentSystemUiState();
        //setNavBarHidden(true);
        //setStatusBarHidden(true);
    }

    // TODO: add joystick controls
//    @Override
//    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
//        // source: https://stackoverflow.com/questions/34536195/how-to-differentiate-a-d-pad-movement-from-a-joystick-movement/58510631#58510631
//        Log.d("PagedActivity", "x: " + ev.getAxisValue(MotionEvent.AXIS_X) + " y: " + ev.getAxisValue(MotionEvent.AXIS_Y) + " z: " + ev.getAxisValue(MotionEvent.AXIS_Z) + " rz: " + ev.getAxisValue(MotionEvent.AXIS_RZ));
//        return super.dispatchGenericMotionEvent(ev);
//    }

//    @Override
//    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
//        Log.d("PagedActivity", ev.toString());
//        return super.dispatchGenericMotionEvent(ev);
//    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        //Log.d("PagedActivity", ev.getPointerCount() + ", " + ev.getActionMasked() + " ?= " + MotionEvent.ACTION_UP + " || " + MotionEvent.ACTION_POINTER_UP + ", " + ev);
        if (ev.getActionMasked() == MotionEvent.ACTION_POINTER_UP && ev.getPointerCount() == 5)
            getDebugView().setShown(!getDebugView().isDebugShown());
        return super.dispatchTouchEvent(ev);
    }
    @Override
    public boolean dispatchKeyEvent(KeyEvent key_event) {
        //Log.d("PagedActivity", key_event.toString());
        String activeTag = OxShellApp.getInputHandler().getActiveTag();
        DebugView.print("INPUT_DEBUG", "InputTag: " + (activeTag != null ? activeTag : "null"));
        boolean isDpadInput = key_event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP || key_event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN || key_event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT || key_event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT;
        if (isKeyboardShown()) {// && (key_event.getKeyCode() == KeyEvent.KEYCODE_BACK || key_event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_B))
            if (!isDpadInput) {
                Log.d("PagedActivity", "Passing to keyboard: " + key_event);
                return super.dispatchKeyEvent(key_event);
            } else
                return true;
        }

        if (OxShellApp.getInputHandler().onInputEvent(key_event))
            return true;

        if (!isNonPermissable(key_event)) {
            Log.d("PagedActivity", "Passing to system: " + key_event);
            return super.dispatchKeyEvent(key_event);
        }
        return true;
    }
    private boolean isNonPermissable(KeyEvent key_event) {
        return key_event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_A ||
                key_event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_B ||
                key_event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_C ||
                key_event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_X ||
                key_event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_Y ||
                key_event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_Z ||
                key_event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_L1 ||
                key_event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_L2 ||
                key_event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_R1 ||
                key_event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_R2 ||
                key_event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_THUMBL ||
                key_event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_THUMBR ||
                key_event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_SELECT ||
                key_event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_START ||
                key_event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP ||
                key_event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN ||
                key_event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT ||
                key_event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT ||
                key_event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN_LEFT ||
                key_event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN_RIGHT ||
                key_event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP_LEFT ||
                key_event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP_RIGHT ||
                key_event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER ||
                key_event.getKeyCode() == KeyEvent.KEYCODE_BACK;
    }
    private static void sendKeyEvent(View targetView, KeyEvent keyEvent) {
        //Reference: https://developer.android.com/reference/android/view/inputmethod/InputConnection#sendKeyEvent(android.view.KeyEvent)
        //& https://stackoverflow.com/questions/13026505/how-can-i-send-key-events-in-android
        //long eventTime = SystemClock.uptimeMillis();
        BaseInputConnection mInputConnection = new BaseInputConnection(targetView, true);
        mInputConnection.sendKeyEvent(keyEvent);
        //dispatchKeyEvent(new KeyEvent(eventTime, eventTime, action, keyeventcode, 0));
    }

//    public void showDynamicInput(boolean onOff) {
//        inputViewOpen = onOff;
//        dynamicInputView.setVisibility(inputViewOpen ? View.VISIBLE : View.GONE);
//    }
//    public boolean isDynamicInputShown() {
//        return inputViewOpen;
//    }
    public boolean isInAContextMenu() {
        return prompt.isPromptShown() || settingsDrawer.isDrawerOpen() || dynamicInput.isOverlayShown();
    }
    private void prepareOtherViews() {
        parentView = findViewById(R.id.parent_layout);
        //refreshShaderViewBg();
        initSettingsDrawer();
        settingsDrawer.setShown(settingsDrawer.isDrawerOpen());
        initDynamicInputView();
        dynamicInput.setShown(dynamicInput.isOverlayShown());
        initPromptView();
        prompt.setShown(prompt.isPromptShown());
        initDebugView();
        debugView.setShown(debugView.isDebugShown());
    }
    private void initDynamicInputView() {
        dynamicInput = parentView.findViewById(DYNAMIC_INPUT_ID);
        if (dynamicInput == null) {
            Log.d("PagedActivity", "Dynamic input not found, creating...");
            dynamicInput = new DynamicInputView(this);
            dynamicInput.setId(DYNAMIC_INPUT_ID);
            parentView.addView(dynamicInput);
        }
    }
    private void initSettingsDrawer() {
        settingsDrawer = parentView.findViewById(SETTINGS_DRAWER_ID);
        if (settingsDrawer == null) {
            Log.d("PagedActivity", "Settings drawer not found, creating...");
            settingsDrawer = new SettingsDrawer(this);
            settingsDrawer.setId(SETTINGS_DRAWER_ID);
            parentView.addView(settingsDrawer);
        }
    }
    private void initPromptView() {
        prompt = parentView.findViewById(PROMPT_ID);
        if (prompt == null) {
            Log.d("PagedActivity", "Prompt not found, creating...");
            prompt = new PromptView(this);
            prompt.setId(PROMPT_ID);
            parentView.addView(prompt);
        }
    }
    private void initDebugView() {
        debugView = parentView.findViewById(DEBUG_VIEW_ID);
        if (debugView == null) {
            Log.d("PagedActivity", "Debug view not found, creating...");
            debugView = new DebugView(this);
            debugView.setId(DEBUG_VIEW_ID);
            parentView.addView(debugView);
        }
    }
    public DynamicInputView getDynamicInput() {
        return dynamicInput;
    }
    public SettingsDrawer getSettingsDrawer() {
        return settingsDrawer;
    }
    public PromptView getPrompt() {
        return prompt;
    }
    public DebugView getDebugView() {
        return debugView;
    }

    private void trySetStartTime() {
        if (!startTimeSet) {
            startTimeSet = true;
            startTime = SystemClock.uptimeMillis();
            //prevFrameTime = startTime;
        }
    }

    public void applyTvBg() {
        int bgType = SettingsKeeper.getTvBgType();
        if (bgType == SettingsKeeper.BG_TYPE_SHADER) {
            destroyImageBg();
            resetShaderViewBg();
        } else if (bgType == SettingsKeeper.BG_TYPE_IMAGE) {
            destroyShaderBg();
            initImageBg();
            refreshImageBg();
        }
    }
    private void initImageBg() {
        if (AndroidHelpers.isRunningOnTV() && tvImageBg == null) {
            tvImageBg = new FrameLayout(this);
            tvImageBg.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            tvImageBg.setZ(-1000);
            parentView.addView(tvImageBg);
        }
    }
    private void destroyImageBg() {
        if (tvImageBg != null) {
            parentView.removeView(tvImageBg);
            tvImageBg = null;
        }
    }
    private void refreshImageBg() {
        if (tvImageBg != null) {
            String bgPath = AndroidHelpers.combinePaths(Paths.SHADER_ITEMS_DIR_INTERNAL, "bg.png");
            tvImageBg.setBackground(AndroidHelpers.bitmapToDrawable(this, AndroidHelpers.bitmapFromFile(bgPath)));
        }
    }
    private void initShaderBg() {
        if (AndroidHelpers.isRunningOnTV() && tvShaderBg == null) {
            tvShaderBg = new ShaderView(this);
            tvShaderBg.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            tvShaderBg.setZ(-1000);
            parentView.addView(tvShaderBg);
            startTimeSet = false;
        }
    }
    private void destroyShaderBg() {
        if (tvShaderBg != null) {
            tvShaderBg.setUpdateContinuously(false);
            parentView.removeView(tvShaderBg);
            tvShaderBg = null;
        }
    }
    private void pauseBackground() {
        if (tvShaderBg != null)
            tvShaderBg.setUpdateContinuously(false);
    }
    private void resumeBackground() {
        //Later will have more logic based on options set by user
        if (tvShaderBg != null) {
            tvShaderBg.setUpdateContinuously(true);
            tvShaderBg.setFramerate(60);
        }
    }
    private void resetShaderViewBg() {
        boolean updateContinuously = false;
        int framerate = 0;
        if (tvShaderBg != null) {
            updateContinuously = tvShaderBg.getUpdateContinuously();
            framerate = tvShaderBg.getFramerate();
        }

        destroyShaderBg();
        initShaderBg();
        trySetStartTime();
        refreshShaderViewBg(updateContinuously, framerate);
    }
    private void refreshShaderViewBg(boolean updateContinuously, int framerate) {
        Log.d("Paged Activity", "Shader view null: " + (tvShaderBg == null));
        if (tvShaderBg != null) {
            String fragPath = AndroidHelpers.combinePaths(Paths.SHADER_ITEMS_DIR_INTERNAL, "frag.fsh");
            String vertPath = AndroidHelpers.combinePaths(Paths.SHADER_ITEMS_DIR_INTERNAL, "vert.vsh");
            String channel0Path = AndroidHelpers.combinePaths(Paths.SHADER_ITEMS_DIR_INTERNAL, "channel0.png");
            String channel1Path = AndroidHelpers.combinePaths(Paths.SHADER_ITEMS_DIR_INTERNAL, "channel1.png");
            String channel2Path = AndroidHelpers.combinePaths(Paths.SHADER_ITEMS_DIR_INTERNAL, "channel2.png");
            String channel3Path = AndroidHelpers.combinePaths(Paths.SHADER_ITEMS_DIR_INTERNAL, "channel3.png");

            String fragShader;
            String vertShader;
            if (AndroidHelpers.fileExists(fragPath))
                fragShader = AndroidHelpers.readFile(fragPath);
            else
                fragShader = AndroidHelpers.readAssetAsString(this, "Shaders/blue_dune.fsh");
            if (AndroidHelpers.fileExists(vertPath))
                vertShader = AndroidHelpers.readFile(vertPath);
            else
                vertShader = AndroidHelpers.readAssetAsString(this, "Shaders/vert.vsh");

            tvShaderBg.setFragmentShader(fragShader);
            tvShaderBg.setVertexShader(vertShader);
            tvShaderBg.setUpdateContinuously(updateContinuously);
            tvShaderBg.setFramerate(framerate);
            ShaderParamsBuilder paramsBuilder = new ShaderParamsBuilder();
            paramsBuilder.addFloat("iTime", 0f);
            paramsBuilder.addVec4f("iMouse", new float[] { 0, 0, 0, 0 });
            //DisplayMetrics displayMetrics = OxShellApp.getCurrentActivity().getDisplayMetrics();
            paramsBuilder.addVec3f("iResolution", new float[] { OxShellApp.getDisplayWidth(), OxShellApp.getDisplayHeight(), 1f });
            if (AndroidHelpers.fileExists(channel0Path))
                paramsBuilder.addTexture2D("iChannel0", AndroidHelpers.bitmapFromFile(channel0Path), GLES32.GL_TEXTURE0);
            if (AndroidHelpers.fileExists(channel1Path))
                paramsBuilder.addTexture2D("iChannel1", AndroidHelpers.bitmapFromFile(channel1Path), GLES32.GL_TEXTURE1);
            if (AndroidHelpers.fileExists(channel2Path))
                paramsBuilder.addTexture2D("iChannel2", AndroidHelpers.bitmapFromFile(channel2Path), GLES32.GL_TEXTURE2);
            if (AndroidHelpers.fileExists(channel3Path))
                paramsBuilder.addTexture2D("iChannel3", AndroidHelpers.bitmapFromFile(channel3Path), GLES32.GL_TEXTURE3);
            tvShaderBg.setShaderParams(paramsBuilder.build());
            tvShaderBg.setOnDrawFrameListener(shaderParams -> {
                //float deltaTime = (System.currentTimeMillis() - prevFrameTime) / 1000f;
                //float fps = 1f / deltaTime;
                //Log.d("FPS", String.valueOf(fps));
                //prevFrameTime = System.currentTimeMillis();
                float secondsElapsed = (SystemClock.uptimeMillis() - startTime) / 1000f;
                if (secondsElapsed > 60 * 60 * 24) {
                    // if more than 24 hours have passed, then reset the timer
                    secondsElapsed = 0;
                    startTime = SystemClock.uptimeMillis();
                }
                shaderParams.updateValue("iTime", secondsElapsed);
                shaderParams.updateValue("iMouse", new float[] { 0, 0, 0, 0 });
                return null;
            });
        }
    }

//    public void addPermissionListener(PermissionsListener listener) {
//        permissionListeners.add(listener);
//    }

//    public static PagedActivity GetInstance() {
//        return instance;
//    }

    public void setActionBarHidden(boolean onOff) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            if (onOff)
                actionBar.hide();
            else
                actionBar.show();
        }
    }
}
