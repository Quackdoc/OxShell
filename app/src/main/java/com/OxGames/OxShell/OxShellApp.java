package com.OxGames.OxShell;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.annotation.NonNull;

import com.OxGames.OxShell.Data.DataLocation;
import com.OxGames.OxShell.Data.DataRef;
import com.OxGames.OxShell.Data.SettingsKeeper;
import com.OxGames.OxShell.Data.ShortcutsCache;
import com.OxGames.OxShell.Helpers.InputHandler;
import com.OxGames.OxShell.Helpers.MusicPlayer;
import com.appspell.shaderview.log.LibLog;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// source: https://stackoverflow.com/questions/9445661/how-to-get-the-context-from-anywhere
public class OxShellApp extends Application {
    private BroadcastReceiver pkgInstallationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctxt, Intent intent) {
        String pkgName = null;
        if (intent != null && intent.getData() != null)
            pkgName = intent.getData().getEncodedSchemeSpecificPart();
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(pkgName, 0);
            if (packageInfo.firstInstallTime == packageInfo.lastUpdateTime) {
                // This is the first installation of the package
                for (Consumer<String> listener : pkgInstalledListeners)
                    if (listener != null)
                        listener.accept(pkgName);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("OxShellApp", "Could not find package: " + e);
        }

        //Log.d("OxShellApp", "Broadcast receiver: " + intent + ", " + (intent != null ? (intent.getExtras() + ", " + pkgName) : "no extras"));
        }
    };
    private BroadcastReceiver musicActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctxt, Intent intent) {
            Log.d("OxShellApp", "Music action receiver: " + intent + ", " + (intent != null ? intent.getExtras() : "no extras"));
            if (intent.getAction().equals(MusicPlayer.PREV_INTENT)) {
                MusicPlayer.seekToPrev();
            } else if (intent.getAction().equals(MusicPlayer.NEXT_INTENT)) {
                MusicPlayer.seekToNext();
            } else if (intent.getAction().equals(MusicPlayer.PLAY_INTENT)) {
                MusicPlayer.play();
            } else if (intent.getAction().equals(MusicPlayer.PAUSE_INTENT)) {
                MusicPlayer.pause();
            } else if (intent.getAction().equals(MusicPlayer.STOP_INTENT)) {
                MusicPlayer.stop();
            }
        }
    };

    private static OxShellApp instance;
    private static InputHandler inputHandler;
    private static List<Consumer<String>> pkgInstalledListeners;

//    public static OxShellApp getInstance() {
//        return instance;
//    }
    private static PagedActivity currentActivity;
    private static AudioManager manager;

    public static Context getContext() {
        return instance;
        // or return instance.getApplicationContext();
    }
    public static AudioManager getAudioManager() {
        if (manager == null)
            manager = (AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE);
        return manager;
    }

    @Override
    public void onCreate() {
        instance = this;
        LibLog.INSTANCE.setEnabled(true);
        Log.i("OxShellApp", "onCreate");

        inputHandler = new InputHandler();
        pkgInstalledListeners = new ArrayList<>();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addDataScheme("package");
        registerReceiver(pkgInstallationReceiver, intentFilter);

        intentFilter = new IntentFilter();
        intentFilter.addAction(MusicPlayer.PREV_INTENT);
        intentFilter.addAction(MusicPlayer.NEXT_INTENT);
        intentFilter.addAction(MusicPlayer.PLAY_INTENT);
        intentFilter.addAction(MusicPlayer.PAUSE_INTENT);
        intentFilter.addAction(MusicPlayer.STOP_INTENT);
        registerReceiver(musicActionReceiver, intentFilter);

        //prepareSession();

        super.onCreate();

        SettingsKeeper.loadOrCreateSettings();
        // this is important since it helps us shape how certain changes need to be made
        SettingsKeeper.updateVersion();
        Log.i("PagedActivity", "Time(s) loaded: " + SettingsKeeper.getTimesLoaded() + "\nVersion: " + SettingsKeeper.getPrevVersionCode() + " (" + SettingsKeeper.getPrevVersionName() + ") => " + BuildConfig.VERSION_CODE + " (" + BuildConfig.VERSION_NAME + ")");
        if (SettingsKeeper.getTimesLoaded() < 1) {
            ShortcutsCache.createAndStoreDefaults();
            SettingsKeeper.setValueAndSave(SettingsKeeper.FONT_REF, DataRef.from("Fonts/exo.regular.otf", DataLocation.asset));
            Log.i("PagedActivity", "First time launch");
        } else {
            ShortcutsCache.readIntentsFromDisk();
            Log.i("PagedActivity", "Not first time launch");
        }
        SettingsKeeper.incrementTimesLoaded();

        getDisplayInfo();
    }
    @Override
    public void onTerminate() {
        Log.i("OxShellApp", "onTerminate");
        unregisterReceiver(pkgInstallationReceiver);
        unregisterReceiver(musicActionReceiver);
        currentActivity = null;
        //releaseSession();
        super.onTerminate();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        // it seems when the app launches from an app that is forced landscape into portrait, the first time onConfigurationChanged should happen does not fire
        super.onConfigurationChanged(newConfig);
        getDisplayInfo();
    }

    @Override
    public void onLowMemory() {
        Log.e("OxShellApp", "Low memory");
        super.onLowMemory();
    }

    public static void addPkgInstalledListener(Consumer<String> listener) {
        pkgInstalledListeners.add(listener);
    }
    public static void removePkgInstalledListener(Consumer<String> listener) {
        pkgInstalledListeners.remove(listener);
    }
    public static void clearPkgInstalledListeners() {
        pkgInstalledListeners.clear();
    }

    public static InputHandler getInputHandler() {
        return inputHandler;
    }

    protected static void setCurrentActivity(PagedActivity activity) {
        currentActivity = activity;
    }
    public static PagedActivity getCurrentActivity() {
        return currentActivity;
    }

    public static int getNavBarHeight() {
        int result = 0;
        int resourceId = instance.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0)
            result = instance.getResources().getDimensionPixelSize(resourceId);
        else
            Log.e("OxShellApp", "Failed to retrieve resource id for android.dimen.navigation_bar_height");
        //Log.i("OxShellApp", "Navbar Height: " + result);
        return result;
    }
    public static int getStatusBarHeight() {
        int result = 0;
        int resourceId = instance.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0)
            result = instance.getResources().getDimensionPixelSize(resourceId);
        else
            Log.e("OxShellApp", "Failed to retrieve resource id for android.dimen.status_bar_height");
        //Log.i("OxShellApp", "StatusBar Height: " + result);
        return result;
    }
    private void getDisplayInfo() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int displayWidth = dm.widthPixels;
        int displayHeight = dm.heightPixels;
        Configuration cfg = getResources().getConfiguration();
        int smallestScreenWidthDp = cfg.smallestScreenWidthDp;
        int densityDpi = cfg.densityDpi;
        Log.i("OxShellApp", "Display width: " + displayWidth + "\nDisplay height: " + displayHeight + "\nSmallest screen width: " + smallestScreenWidthDp + "\nDensity DPI: " + densityDpi);
    }
    public static int getDisplayWidth() {
        DisplayMetrics dm = instance.getResources().getDisplayMetrics();
        return dm.widthPixels;
    }
    public static int getDisplayHeight() {
        DisplayMetrics dm = instance.getResources().getDisplayMetrics();
        return dm.heightPixels;
    }
    public static int getSmallestScreenWidthDp() {
        Configuration cfg = instance.getResources().getConfiguration();
        return cfg.smallestScreenWidthDp;
    }
    public static int getDensityDpi() {
        Configuration cfg = instance.getResources().getConfiguration();
        return cfg.densityDpi;
    }

//    private MediaSessionCompat session;
//    public static void setSessionActive(boolean onOff) {
//        instance.session.setActive(onOff);
//    }
//    public static MediaSessionCompat.Token getSessionToken() {
//        return instance.session.getSessionToken();
//    }
//    private void releaseSession() {
//        if (session != null) {
//            session.release();
//            session = null;
//        }
//    }
//    public static void setSessionState(boolean isPlaying) {
//        PlaybackStateCompat.Builder playbackState = new PlaybackStateCompat.Builder();
//        playbackState.setActions(PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
//        playbackState.setState(isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f);
//        instance.session.setPlaybackState(playbackState.build());
//    }
//    private void prepareSession() {
//        session = new MediaSessionCompat(OxShellApp.getContext(), BuildConfig.APP_LABEL);
//        session.setCallback(new MediaSessionCompat.Callback() {
//            @Override
//            public boolean onMediaButtonEvent(@NonNull Intent mediaButtonIntent) {
//                Log.d("MusicPlayer", mediaButtonIntent + ", " + (mediaButtonIntent.getExtras() != null ? mediaButtonIntent.getExtras().toString() : "null"));
//                return super.onMediaButtonEvent(mediaButtonIntent);
//            }
//
//            @Override
//            public void onPlay() {
//                super.onPlay();
//                Log.d("MusicPlayer", "onPlay");
//            }
//
//            @Override
//            public void onSkipToQueueItem(long id) {
//                super.onSkipToQueueItem(id);
//                Log.d("MusicPlayer", "onSkipToQueueItem " + id);
//            }
//
//            @Override
//            public void onPause() {
//                super.onPause();
//                Log.d("MusicPlayer", "onPause");
//            }
//
//            @Override
//            public void onSkipToNext() {
//                super.onSkipToNext();
//                Log.d("MusicPlayer", "onSkipToNext");
//            }
//
//            @Override
//            public void onSkipToPrevious() {
//                super.onSkipToPrevious();
//                Log.d("MusicPlayer", "onSkipToPrevious");
//            }
//
//            @Override
//            public void onFastForward() {
//                super.onFastForward();
//                Log.d("MusicPlayer", "onFastForward");
//            }
//
//            @Override
//            public void onRewind() {
//                super.onRewind();
//                Log.d("MusicPlayer", "onRewind");
//            }
//
//            @Override
//            public void onStop() {
//                super.onStop();
//                Log.d("MusicPlayer", "onStop");
//            }
//
//            @Override
//            public void onSeekTo(long pos) {
//                super.onSeekTo(pos);
//                Log.d("MusicPlayer", "onSeekTo " + pos);
//            }
//        });
//    }
}
