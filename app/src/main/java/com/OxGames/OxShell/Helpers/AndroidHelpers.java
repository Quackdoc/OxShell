package com.OxGames.OxShell.Helpers;

import android.Manifest;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import androidx.activity.result.ActivityResult;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.OxGames.OxShell.BuildConfig;
import com.OxGames.OxShell.OxShellApp;
import com.OxGames.OxShell.PagedActivity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

public class AndroidHelpers {
    public static final char[] ILLEGAL_FAT_CHARS = new char[] { '"', '*', '/', ':', '<', '>', '?', '\\', '|', 0x7F };
    public static final char[] ILLEGAL_EXT_CHARS = new char[] { '\0', '/' };
    public static final int UNKNOWN_FORMAT = 0;
    public static final int FAT_FORMAT = 1;
    public static final int EXT_FORMAT = 2;

    public static final int READ_EXTERNAL_STORAGE = 100;
    public static final int WRITE_EXTERNAL_STORAGE = 101;
    public static final int MANAGE_EXTERNAL_STORAGE = 102;
    private static WallpaperManager wallpaperManager;

    public static final String RECENT_ACTIVITY;
    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            RECENT_ACTIVITY = "com.android.launcher3.RecentsActivity";
        }
        else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            RECENT_ACTIVITY = "com.android.systemui.recents.RecentsActivity";
        } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) {
            RECENT_ACTIVITY = "com.android.systemui.recent.RecentsActivity";
        } else {
            RECENT_ACTIVITY = "com.android.internal.policy.impl.RecentApplicationDialog";
        }
    }

    public static StateListDrawable createStateListDrawable(Drawable drawable) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] { android.R.attr.state_focused }, drawable);
        states.addState(new int[] { android.R.attr.state_active }, drawable);
        states.addState(new int[] { android.R.attr.state_pressed }, drawable);
        states.addState(new int[] { android.R.attr.state_enabled }, drawable);
        states.addState(new int[] { android.R.attr.state_empty }, drawable);
        states.addState(new int[] { android.R.attr.state_checked }, drawable);
        states.addState(new int[] { android.R.attr.state_drag_can_accept }, drawable);
        states.addState(new int[] { android.R.attr.state_window_focused }, drawable);
        states.addState(new int[] { android.R.attr.state_checkable }, drawable);
        states.addState(new int[] { android.R.attr.state_above_anchor }, drawable);
        states.addState(new int[] { android.R.attr.state_selected }, drawable);
        states.addState(new int[] { android.R.attr.state_activated }, drawable);
        states.addState(new int[] { android.R.attr.state_accelerated }, drawable);
        states.addState(new int[] { android.R.attr.state_drag_hovered }, drawable);
        states.addState(new int[] { android.R.attr.state_expanded }, drawable);
        states.addState(new int[] { android.R.attr.state_first }, drawable);
        states.addState(new int[] { android.R.attr.state_last }, drawable);
        states.addState(new int[] { android.R.attr.state_middle }, drawable);
        states.addState(new int[] { android.R.attr.state_multiline }, drawable);
        states.addState(new int[] { android.R.attr.state_single }, drawable);
        states.addState(new int[] { android.R.attr.state_long_pressable }, drawable);
        states.addState(new int[] { android.R.attr.state_hovered }, drawable);
        return states;
    }
    // source: https://stackoverflow.com/a/45661088/5430992
    public static void uninstallApp(PagedActivity launchingActivity, String pkgName, Consumer<ActivityResult> onResult) {
        Uri uri = Uri.fromParts("package", pkgName, null);
        Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, uri);
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
        launchingActivity.requestResult(intent, onResult);
    }
    public static void setWallpaper(PagedActivity launchingActivity, String pkgName, String wallpaperService, Consumer<ActivityResult> onResult) {
        // make sure to include the dot in the wallpaperService
        ComponentName component = new ComponentName(pkgName, pkgName + wallpaperService);
        Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
        intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component);
        launchingActivity.requestResult(intent, onResult);
        //launchingActivity.startActivityForResult(intent, requestCode);
    }
    public static void setWallpaper(Context context, int resId) {
        if (wallpaperManager == null)
            wallpaperManager = WallpaperManager.getInstance(context);
        try { wallpaperManager.setResource(resId); } catch (IOException e) { e.printStackTrace(); }
    }
    public static void setWallpaper(Context context, Bitmap bitmap) {
        if (wallpaperManager == null)
            wallpaperManager = WallpaperManager.getInstance(context);
        try { wallpaperManager.setBitmap(bitmap); } catch (IOException e) { e.printStackTrace(); }
    }
    public static void setWallpaper(Context context, Bitmap bitmap, Rect visibleCropHint, boolean allowBackup) {
        if (wallpaperManager == null)
            wallpaperManager = WallpaperManager.getInstance(context);
        try { wallpaperManager.setBitmap(bitmap, visibleCropHint, allowBackup); } catch (IOException e) { e.printStackTrace(); }
    }
    public static void setWallpaper(Context context, Bitmap bitmap, Rect visibleCropHint, boolean allowBackup, int which) {
        // source: https://stackoverflow.com/questions/53466302/is-there-any-way-of-changing-lockscreen-wallpaper-photo-in-android-programmatica
        // the 'which' parameter can be set to WallpaperManager.FLAG_LOCK to set the lockscreen wallpaper, the rect can be set to null and allowBackup to true
        if (wallpaperManager == null)
            wallpaperManager = WallpaperManager.getInstance(context);
        try { wallpaperManager.setBitmap(bitmap, visibleCropHint, allowBackup, which); } catch (IOException e) { e.printStackTrace(); }
    }
    public static Bitmap bitmapFromResource(Resources res, int resId) {
        return BitmapFactory.decodeResource(res, resId);
    }
    public static Bitmap bitmapFromFile(String path) {
        return BitmapFactory.decodeFile(path);
    }
    public static Drawable bitmapToDrawable(Context context, Bitmap bitmap) {
        return new BitmapDrawable(context.getResources(), bitmap);
    }

    public static void saveBitmapToFile(Bitmap bm, String path) {
        try {
            FileOutputStream out = new FileOutputStream(path);
            bm.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch(Exception e) { Log.e("AndroidHelpers", e.toString()); }
    }
    public static String readAssetAsString(Context context, String asset) {
        String assetData = null;
        try {
            InputStream inputStream = context.getAssets().open(asset);
            BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder total = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                total.append(line).append('\n');
            }
            assetData = total.toString();
        } catch (IOException ex) {
            Log.e("Reading_Asset_Error", ex.toString());
        }
        //Log.d("Asset", assetData);
        return assetData;
    }
    // source: https://stackoverflow.com/a/8501428/5430992
    public static Bitmap readAssetAsBitmap(Context context, String filePath) {
        AssetManager assetManager = context.getAssets();

        InputStream istr;
        Bitmap bitmap = null;
        try {
            istr = assetManager.open(filePath);
            bitmap = BitmapFactory.decodeStream(istr);
        } catch (IOException e) {
            Log.e("AndroidHelpers", "Failed to load bitmap from assets: " + e);
        }

        return bitmap;
    }

    public static int getRelativeLeft(View view) {
        if (view.getParent() == view.getRootView())
            return view.getLeft();
        else
            return view.getLeft() + getRelativeLeft((View) view.getParent());
    }
    public static int getRelativeTop(View view) {
        if (view.getParent() == view.getRootView())
            return view.getTop();
        else
            return view.getTop() + getRelativeTop((View) view.getParent());
    }

    public static String combinePaths(String... subPaths) {
        StringBuilder combined = new StringBuilder();
        for (String subPath : subPaths) {
            boolean firstEndsWithSeparator = combined.toString().lastIndexOf('/') == combined.length() - 1;
            boolean secondStartsWithSeparator = subPath.indexOf('/') == 0;
            if (firstEndsWithSeparator && secondStartsWithSeparator)
                combined.append(subPath.substring(1));
            else if (firstEndsWithSeparator || secondStartsWithSeparator)
                combined.append(subPath);
            else
                combined.append("/").append(subPath);
        }
//        boolean firstEndsWithSeparator = first.lastIndexOf('/') == first.length() - 1;
//        boolean secondStartsWithSeparator = second.indexOf('/') == 0;
//        if (firstEndsWithSeparator && secondStartsWithSeparator)
//            combined = first + second.substring(1);
//        else if (firstEndsWithSeparator || secondStartsWithSeparator)
//            combined = first + second;
//        else
//            combined = first + "/" + second;
        return combined.toString();
    }
    public static boolean hasPermission(String permType) {
        return ContextCompat.checkSelfPermission(OxShellApp.getContext(), permType) == PackageManager.PERMISSION_GRANTED;
    }
    public static boolean hasReadStoragePermission() {
        //Log.d("FileHelpers", "Checking has read permission");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            return hasPermission(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
        else
            return hasPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE);
    }
    public static boolean hasWriteStoragePermission() {
        //Log.d("FileHelpers", "Checking has write permission");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            return hasPermission(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
        else
            return hasPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }
    public static void requestReadStoragePermission() {
        Log.d("FileHelpers", "Requesting read permission");
        //From here https://stackoverflow.com/questions/47292505/exception-writing-exception-to-parcel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            ActivityCompat.requestPermissions(ActivityManager.getCurrentActivity(), new String[] { android.Manifest.permission.MANAGE_EXTERNAL_STORAGE }, MANAGE_EXTERNAL_STORAGE);
        else
            ActivityCompat.requestPermissions(ActivityManager.getCurrentActivity(), new String[] { android.Manifest.permission.READ_EXTERNAL_STORAGE }, READ_EXTERNAL_STORAGE);
        //ActivityCompat.shouldShowRequestPermissionRationale(ActivityManager.GetCurrentActivity(), Manifest.permission.READ_EXTERNAL_STORAGE);
    }
    public static void requestWriteStoragePermission() {
        Log.d("FileHelpers", "Requesting write permission");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            ActivityCompat.requestPermissions(ActivityManager.getCurrentActivity(), new String[] { android.Manifest.permission.MANAGE_EXTERNAL_STORAGE }, MANAGE_EXTERNAL_STORAGE);
        else
            ActivityCompat.requestPermissions(ActivityManager.getCurrentActivity(), new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, WRITE_EXTERNAL_STORAGE);
    }

    public static Uri uriFromFile(File file) {
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { //Unnecessary since version code is always over 24
            return Uri.parse(FileProvider.getUriForFile(OxShellApp.getContext(), BuildConfig.DOCUMENTS_AUTHORITY, file).toString().replace("%2F", "/").replace("%20", " "));
        //} else {
        //    return Uri.fromFile(file);
        //}
    }
    public static Uri uriFromPath(String path) {
        return uriFromFile(new File(path));
    }

    public static void install(String path) {
        Context context = OxShellApp.getContext();
        final Uri contentUri = uriFromPath(path);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(intent);
    }

    public static File[] listContents(String dirName) {
        return (new File(dirName)).listFiles();
    }
    public static void makeDir(String dirName) {
        File homeItemsDir = new File(dirName);
        homeItemsDir.mkdirs();
    }
    public static void makeFile(String fileName) {
        try {
            File file = new File(fileName);
            if (!file.exists())
                file.createNewFile();
        } catch (IOException ex) {
            Log.e("AndroidHelpers", "Failed to create file (" + fileName + "): " + ex);
        }
    }
    public static boolean isNameFSLegal(String fileName, int filesystem) {
        switch (filesystem) {
            case FAT_FORMAT:
                for (char illegalChar : ILLEGAL_FAT_CHARS)
                    if (fileName.indexOf(illegalChar) >= 0)
                        return false;
                break;
            case EXT_FORMAT:
                for (char illegalChar : ILLEGAL_EXT_CHARS)
                    if (fileName.indexOf(illegalChar) >= 0)
                        return false;
                break;
            default:
                for (char illegalChar : ILLEGAL_FAT_CHARS)
                    if (fileName.indexOf(illegalChar) >= 0)
                        return false;
                for (char illegalChar : ILLEGAL_EXT_CHARS)
                    if (fileName.indexOf(illegalChar) >= 0)
                        return false;
                break;
        }
        return true;
    }
    public static boolean dirExists(String dirName) {
        return (new File(dirName)).isDirectory();
    }
    public static boolean fileExists(String dirName) {
        return (new File(dirName)).isFile();
    }
    public static void writeToFile(String fileName, String text) {
        String parentDir = new File(fileName).getParent();
        if (!dirExists(parentDir))
            makeDir(parentDir);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
            writer.write(text);
            writer.close();
        } catch (IOException ex) {
            Log.e("AndroidHelpers", "Failed to write to file (" + fileName + "): " + ex);
        }
    }
    public static String readFile(String fileName) {
        String fileData = null;
        try {
            BufferedReader r = new BufferedReader(new FileReader(fileName));
            StringBuilder total = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                total.append(line).append('\n');
            }
            fileData = total.toString();
        } catch (IOException ex) {
            Log.e("ExplorerBehaviour", ex.getMessage());
        }
        //Log.d("Asset", assetData);
        return fileData;
    }

    public static String getExtension(String path) {
        String extension = null;
        if (hasExtension(path))
            extension = path.substring(path.lastIndexOf(".") + 1);
        return extension;
    }
    public static boolean hasExtension(String path) {
        int dotIndex = path.lastIndexOf(".");
        int spaceIndex = path.lastIndexOf(" ");
        return dotIndex > 0 && spaceIndex < dotIndex;
    }
    public static boolean isDirectory(String path) {
        return new File(path).isDirectory();
    }
    public static String removeExtension(String path) {
        String fileName = (new File(path)).getName();
        if (!isDirectory(path))
            while (hasExtension(fileName))
                fileName = fileName.substring(0, fileName.lastIndexOf("."));
        return fileName;
    }
    public static ArrayList<File> getItemsInDirWithExt(String path, String[] extensions) {
        ArrayList<File> matching = new ArrayList<>();
        File[] files = listContents(path);
        boolean isEmpty = files == null || files.length <= 0;
        if (!isEmpty) {
            for (int i = 0; i < files.length; i++) {
                String ext = getExtension(files[i].getAbsolutePath());
                if (Arrays.stream(extensions).anyMatch(otherExt -> otherExt.equalsIgnoreCase(ext)))
                    matching.add(files[i]);
            }
        }
        return matching;
    }

    // source: https://stackoverflow.com/questions/8399184/convert-dip-to-px-in-android
    public static float dpToPixels(Context context, float dipValue) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, context.getResources().getDisplayMetrics());
    }
    public static float dpToInches(Context context, float dipValue) {
        return pxToInches(context, dpToPixels(context, dipValue));
    }
    public static float pxToInches(Context context, float pxValue) {
        return pxValue / context.getResources().getDisplayMetrics().xdpi;
    }
    // source: https://stackoverflow.com/a/51833445/5430992
    public static float spToPixels(Context context, float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics());
    }
    public static float getScaledSpToPixels(Context context, float sp) {
        return spToPixels(context, sp) * (float)Math.pow(AndroidHelpers.getUiScale(), 1.8);
    }
    public static float getScaledDpToPixels(Context context, float dp) {
        return AndroidHelpers.dpToPixels(context, dp) * getUiScale();
    }
    public static float getUiScale() {
        // return 2.952551f / AndroidHelpers.dpToInches(context, OxShellApp.getSmallestScreenWidthDp()); // the smallest width when converted to inches was almost always the same size
        float percent = OxShellApp.getSmallestScreenWidthDp() / 462f;
        return (float)Math.pow(percent, 1.2f); // fine tuned to my liking, not scientific
    }
}
