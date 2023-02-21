package com.OxGames.OxShell.Data;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.OxGames.OxShell.Helpers.ActivityManager;
import com.OxGames.OxShell.Helpers.AndroidHelpers;
import com.OxGames.OxShell.Helpers.Serialaver;
import com.OxGames.OxShell.HomeActivity;
import com.OxGames.OxShell.OxShellApp;
import com.OxGames.OxShell.PagedActivity;
import com.OxGames.OxShell.R;
import com.OxGames.OxShell.Views.HomeView;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;

public class HomeManager {
    //private static boolean initialized = false;
//    private static ArrayList<XMBItem> homeItems;
    private static ArrayList<ArrayList<XMBItem>> allHomeItems;
    private static final int GAMES = 0, AUDIO = 1, VIDEO = 2, IMAGE = 3, SOCIAL = 4, NEWS = 5, MAPS = 6, PRODUCTIVITY = 7, ACCESSIBILITY = 8, OTHER = 9;

    public static boolean isInitialized() {
        return allHomeItems != null;
    }
    public static void init() {
        if (!isInitialized()) {
            allHomeItems = new ArrayList<>();
//            //initialized = true;
//            boolean requestingAccess = false;
//            String home_items_dir = AndroidHelpers.combinePaths(Paths.HOME_ITEMS_DIR_EXTERNAL, Paths.HOME_ITEMS_FILE_NAME);
//            boolean home_items_exist =
//                AndroidHelpers.fileExists(home_items_dir + Paths.HOME_ITEMS_CATS)
//                || AndroidHelpers.fileExists(home_items_dir + Paths.HOME_ITEMS_DEFAULTS)
//                || AndroidHelpers.fileExists(home_items_dir + Paths.HOME_ITEMS_APPS)
//                || AndroidHelpers.fileExists(home_items_dir + Paths.HOME_ITEMS_DEFAULTS);
//            if (home_items_exist) {
//                // the home items file exists in the phone storage so attempt to read
//                if (!AndroidHelpers.hasReadStoragePermission()) {
//                    // we do not have permission to read the file so request it
//                    Log.d("HomeManager", "HomeItems exists in phone storage but we do not have permission to read, requesting permission");
//                    //addExplorer(); //Temp explorer in case no permissions granted
//
//                    requestingAccess = true;
//                    //TODO: show pop up message explaining why storage access is being requested
//                    AndroidHelpers.requestReadStoragePermission();
//                    ActivityManager.getCurrentActivity().addPermissionListener((requestCode, permissions, grantResults) -> {
//                        if (requestCode == AndroidHelpers.READ_EXTERNAL_STORAGE) {
//                            // listen for when the permission is granted/denied
//                            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                                // if storage permission is granted then read the file from the phone storage and save it to the data folder
//                                Log.d("HomeManager", "Read permission granted, loading HomeItems from disk");
//                                loadHomeItemsFromFile(Paths.HOME_ITEMS_DIR_EXTERNAL, Paths.HOME_ITEMS_FILE_NAME);
//                                saveHomeItemsToFile(Paths.HOME_ITEMS_DIR_INTERNAL, Paths.HOME_ITEMS_FILE_NAME);
//                            } else {
//                                // if storage permission is denied then just use the internal data folder
//                                Log.e("HomeManager", "Read storage permission denied");
//                                initInternal();
//                            }
//                        }
//                    });
//                } else {
//                    // since we have storage permission then read the file from the phone storage and save it to the data folder
//                    Log.d("HomeManager", "HomeItems exists in phone storage and we have permission to read, loading HomeItems from disk");
//                    loadHomeItemsFromFile(Paths.HOME_ITEMS_DIR_EXTERNAL, Paths.HOME_ITEMS_FILE_NAME);
//                    saveHomeItemsToFile(Paths.HOME_ITEMS_DIR_INTERNAL, Paths.HOME_ITEMS_FILE_NAME);
//                }
//            } else {
//                Log.d("HomeManager", "HomeItems did not exist in phone storage");
//                //addExplorer(); //Since nothing is saved anyway no need to save this because it is the default (this way the user isn't always bombarded with permission request on start)
//            }
//            if (!requestingAccess)
                initInternal();
        } else
            Log.d("HomeManager", "homeItems not null, so not reading from disk");
    }
    private static void initInternal() {
        String home_items_path = AndroidHelpers.combinePaths(Paths.HOME_ITEMS_DIR_INTERNAL, Paths.HOME_ITEMS_FILE_NAME);
        if (!AndroidHelpers.fileExists(home_items_path)) {
            // if no file exists then we can ask the user if they want to add their apps to the home then save
            Log.d("HomeManager", "Home items does not exist in data folder, creating...");
            addExplorer();

            String[] categories = new String[] { "Games", "Audio", "Video", "Image", "Social", "News", "Maps", "Productivity", "Accessibility", "Other" };
            HashMap<Integer, ArrayList<XMBItem>> sortedApps = new HashMap<>();
            PagedActivity currentActivity = ActivityManager.getCurrentActivity();
            PackagesCache.requestInstalledPackages(Intent.ACTION_MAIN, new String[] { Intent.CATEGORY_LAUNCHER }, apps -> currentActivity.runOnUiThread(() -> {
                // go through all apps creating HomeItems for them and sorting them into their categories
//                for (ResolveInfo app : apps)
//                    addItem(new HomeItem(HomeItem.Type.app, PackagesCache.getPackageIcon(app), PackagesCache.getAppLabel(app), app.activityInfo.packageName), false);
                for (int i = 0; i < apps.size(); i++) {
                    ResolveInfo currentPkg = apps.get(i);
                    int category = -1;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                        category = currentPkg.activityInfo.applicationInfo.category;
                    if (!sortedApps.containsKey(category))
                        sortedApps.put(category, new ArrayList<>());
                    ArrayList<XMBItem> currentList = sortedApps.get(category);
                    currentList.add(new HomeItem(HomeItem.Type.app, PackagesCache.getAppLabel(currentPkg), currentPkg.activityInfo.packageName));
                }
                // separate the categories to avoid empty ones and order them into an arraylist so no game in indices occurs
                ArrayList<Integer> existingCategories = new ArrayList<>();
                for (Integer key : sortedApps.keySet())
                    existingCategories.add(key);
                // add the categories and apps
                for (int index = 0; index < existingCategories.size(); index++) {
                    int catIndex = existingCategories.get(index);
                    if (catIndex == -1)
                        catIndex = categories.length - 1;
                    // add the category item at the top
                    int colIndex = createCategory(getDefaultIconForCategory(catIndex), categories[catIndex], false);
                    //addItem(new XMBItem(null, categories[catIndex], index + 2, 0), false);
                    // add the apps into the category as items
                    ArrayList<XMBItem> column = sortedApps.get(existingCategories.get(index));
                    for (XMBItem app : column) {
                        //XMBItem app = column.get(i);
                        //app.colIndex = index + 2;
                        //app.localIndex = i + 1;
                        addItemTo(app, colIndex, false);
                    }
                }
                refreshHomeItems();
                saveHomeItemsToFile(Paths.HOME_ITEMS_DIR_INTERNAL, Paths.HOME_ITEMS_FILE_NAME);
            }));
        }
        else {
            // if the file exists in the data folder then read it
            Log.d("HomeManager", "Home items exists in data folder, reading...");
            loadHomeItemsFromFile(Paths.HOME_ITEMS_DIR_INTERNAL, Paths.HOME_ITEMS_FILE_NAME);
        }
    }
    public static int getDefaultIconForCategory(int category) {
        switch (category) {
            case(GAMES):
                return R.drawable.ic_baseline_games_24;
            case(AUDIO):
                return R.drawable.ic_baseline_headphones_24;
            case(VIDEO):
                return R.drawable.ic_baseline_movie_24;
            case(IMAGE):
                return R.drawable.ic_baseline_photo_camera_24;
            case(SOCIAL):
                return R.drawable.ic_baseline_forum_24;
            case(NEWS):
                return R.drawable.ic_baseline_newspaper_24;
            case(MAPS):
                return R.drawable.ic_baseline_map_24;
            case(PRODUCTIVITY):
                return R.drawable.ic_baseline_work_24;
            case(ACCESSIBILITY):
                return R.drawable.ic_baseline_accessibility_24;
            case(OTHER):
                return R.drawable.ic_baseline_auto_awesome_24;
            default:
                return R.drawable.ic_baseline_view_list_24;
        }
    }
    public static ArrayList<ArrayList<XMBItem>> getItems() {
        //Might be called before initialization
        //return homeItems != null ? new ArrayList<>(homeItems) : null;
        ArrayList<ArrayList<XMBItem>> cloned = null;
        if (allHomeItems != null) {
            cloned = new ArrayList<>();
            for (ArrayList<XMBItem> column : allHomeItems)
                cloned.add(new ArrayList<>(column));
        }
        return cloned;
    }
    public static void addExplorerAndSave() {
        addItemAndSave(new HomeItem(HomeItem.Type.explorer, "Explorer"));
    }
    private static void addExplorer() {
        addItem(new HomeItem(HomeItem.Type.explorer, "Explorer"));
    }

    private static int createCategory(String name, boolean refresh) {
        return createCategory(null, name, refresh);
    }
    private static int createCategory(Object iconLoc, String name, boolean refresh) {
        ArrayList<XMBItem> newColumn = new ArrayList<>();
        XMBItem colItem = new XMBItem(null, name, iconLoc, allHomeItems.size(), 0);
        newColumn.add(colItem);
        int colIndex = allHomeItems.size();
        allHomeItems.add(newColumn);
        if (refresh)
            refreshHomeItems();
        return colIndex;
    }
    public static void createCategory(String name) {
        createCategory(name, true);
    }
    private static int addItem(XMBItem homeItem, boolean refresh) {
        ArrayList<XMBItem> newColumn = new ArrayList<>();
        homeItem.colIndex = allHomeItems.size();
        newColumn.add(homeItem);
        int colIndex = allHomeItems.size();
        allHomeItems.add(newColumn);
        //homeItems.add(homeItem);
        if (refresh)
            refreshHomeItems();
        return colIndex;
    }
    private static void addItemTo(XMBItem homeItem, int colIndex, boolean refresh) {
        ArrayList<XMBItem> column = allHomeItems.get(colIndex);
        homeItem.colIndex = colIndex;
        homeItem.localIndex = column.size();
        column.add(homeItem);
        if (refresh)
            refreshHomeItems();
    }
    public static void addItem(XMBItem homeItem) {
        addItem(homeItem, true);
    }
    public static void addItemAndSave(XMBItem homeItem) {
        Log.d("HomeManager", "Adding item and saving to disk");
        addItem(homeItem);
        saveHomeItemsToFile(Paths.HOME_ITEMS_DIR_INTERNAL, Paths.HOME_ITEMS_FILE_NAME);
        //TODO: add settings toggle for storing data externally and use that as a condition as well
        if (AndroidHelpers.hasWriteStoragePermission())
            saveHomeItemsToFile(Paths.HOME_ITEMS_DIR_EXTERNAL, Paths.HOME_ITEMS_FILE_NAME);
    }
    public static void removeItem(XMBItem homeItem) {
        for (ArrayList<XMBItem> column : allHomeItems) {
            int index = column.indexOf(homeItem);
            if (index >= 0) {
                column.remove(index);
                // fix the later items within the column to have the proper local index
                for (int i = index; i < column.size(); i++)
                    column.get(i).localIndex = i;
                break;
            }
        }
        //homeItems.remove(homeItem);
        refreshHomeItems();
        saveHomeItemsToFile(Paths.HOME_ITEMS_DIR_INTERNAL, Paths.HOME_ITEMS_FILE_NAME);
        if (AndroidHelpers.hasWriteStoragePermission())
            saveHomeItemsToFile(Paths.HOME_ITEMS_DIR_EXTERNAL, Paths.HOME_ITEMS_FILE_NAME);
    }

    private static void loadHomeItemsFromFile(String parentDir, String fileName) {
        //homeItems = new ArrayList<>();
        allHomeItems = new ArrayList<>();
        // Object[] savedItems = (Object[])Serialaver.loadFile(AndroidHelpers.combinePaths(parentDir, fileName));
        String path = AndroidHelpers.combinePaths(parentDir, fileName);
        Log.d("HomeManager", "Looking for " + path);
        if (AndroidHelpers.fileExists(path)) {
//            ArrayList<XMBItem> savedItems = new ArrayList<>();
//            savedItems.addAll(Serialaver.loadFromJSON(path + Paths.HOME_ITEMS_CATS, new TypeToken<ArrayList<XMBItem>>(){}.getType()));
//            savedItems.addAll(Serialaver.loadFromJSON(path + Paths.HOME_ITEMS_DEFAULTS, new TypeToken<ArrayList<HomeItem>>(){}.getType()));
//            savedItems.addAll(Serialaver.loadFromJSON(path + Paths.HOME_ITEMS_APPS, new TypeToken<ArrayList<HomeItem<String>>>(){}.getType()));
//            savedItems.addAll(Serialaver.loadFromJSON(path + Paths.HOME_ITEMS_ASSOCS, new TypeToken<ArrayList<HomeItem<IntentLaunchData>>>(){}.getType()));
            //HomeItem[] savedItems = Serialaver.loadFromJSON(path, HomeItem[].class);
            ArrayList<ArrayList<XMBItem>> savedItems = (ArrayList<ArrayList<XMBItem>>)Serialaver.loadFile(path);
            //ArrayList<ArrayList<XMBItem>> savedItems = Serialaver.loadFromJSON(path, new TypeToken<ArrayList<ArrayList<XMBItem>>>(){}.getType());
            if (savedItems.size() > 0) {
                for (ArrayList<XMBItem> column : savedItems) {
                    if (column.size() > 0) {
                        int colIndex = addItem(column.get(0), false);
                        for (int i = 1; i < column.size(); i++) {
                            //Log.d("HomeManager", savedItem.toString());
                            addItemTo(column.get(i), colIndex, false);
                        }
                    }
                }
            } else
                Log.e("HomeManager", "Could not load home items, format unknown");
        } else
            Log.e("HomeManager", "Attempted to read missing home items @ " + path);
        refreshHomeItems();
    }
    private static void saveHomeItemsToFile(String parentDir, String fileName) {
        AndroidHelpers.makeDir(parentDir);
        String fullPath = AndroidHelpers.combinePaths(parentDir, fileName);
        //AndroidHelpers.makeFile(fullPath);
        // Serialaver.saveFile(homeItems.toArray(), fullPath);
//        ArrayList<XMBItem> cats = new ArrayList<>();
//        ArrayList<XMBItem> defaults = new ArrayList<>();
//        ArrayList<XMBItem<IntentLaunchData>> assocs = new ArrayList<>();
//        ArrayList<XMBItem<String>> apps = new ArrayList<>();
//        for (XMBItem homeItem : homeItems) {
//            if (homeItem instanceof HomeItem) {
//                if (((HomeItem)homeItem).type == HomeItem.Type.app)
//                    apps.add(homeItem);
//                else if (((HomeItem)homeItem).type == HomeItem.Type.assoc)
//                    assocs.add(homeItem);
//                else
//                    defaults.add(homeItem);
//            } else
//                cats.add(homeItem);
//        }
//        Serialaver.saveAsJSON(cats, fullPath + Paths.HOME_ITEMS_CATS);
//        Serialaver.saveAsJSON(defaults, fullPath + Paths.HOME_ITEMS_DEFAULTS);
//        Serialaver.saveAsJSON(apps, fullPath + Paths.HOME_ITEMS_APPS);
//        Serialaver.saveAsJSON(assocs, fullPath + Paths.HOME_ITEMS_ASSOCS);
        //Serialaver.saveAsJSON(allHomeItems, fullPath);
        Serialaver.saveFile(allHomeItems, fullPath);
    }
    private static void refreshHomeItems() {
        ((HomeView)ActivityManager.getInstance(HomeActivity.class).getView(ActivityManager.Page.home)).refresh();
    }
}
