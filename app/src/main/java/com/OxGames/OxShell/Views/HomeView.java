package com.OxGames.OxShell.Views;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import androidx.activity.result.ActivityResult;

import com.OxGames.OxShell.Adapters.XMBAdapter;
import com.OxGames.OxShell.Data.DataLocation;
import com.OxGames.OxShell.Data.DynamicInputRow;
import com.OxGames.OxShell.Data.IntentPutExtra;
import com.OxGames.OxShell.Data.PackagesCache;
import com.OxGames.OxShell.Data.Paths;
import com.OxGames.OxShell.Data.ShortcutsCache;
import com.OxGames.OxShell.Data.XMBItem;
import com.OxGames.OxShell.Helpers.ActivityManager;
import com.OxGames.OxShell.Data.HomeItem;
import com.OxGames.OxShell.Data.IntentLaunchData;
import com.OxGames.OxShell.Helpers.AndroidHelpers;
import com.OxGames.OxShell.Helpers.ExplorerBehaviour;
import com.OxGames.OxShell.Helpers.MathHelpers;
import com.OxGames.OxShell.Helpers.Serialaver;
import com.OxGames.OxShell.Interfaces.DynamicInputListener;
import com.OxGames.OxShell.Interfaces.Refreshable;
import com.OxGames.OxShell.OxShellApp;
import com.OxGames.OxShell.PagedActivity;
import com.OxGames.OxShell.R;
import com.OxGames.OxShell.Wallpaper.GLWallpaperService;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HomeView extends XMBView implements Refreshable {
    public HomeView(Context context) {
        super(context);
        refresh();
    }
    public HomeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        refresh();
    }
    public HomeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
//        setLayoutParams(new GridView.LayoutParams(256, 256));
        refresh();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        PagedActivity currentActivity = ActivityManager.getCurrentActivity();
        if (!currentActivity.isInAContextMenu())
            return super.onInterceptTouchEvent(ev);
        else
            return false;
    }
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        PagedActivity currentActivity = ActivityManager.getCurrentActivity();
        if (!currentActivity.isInAContextMenu())
            return super.onTouchEvent(ev);
        else
            return false;
    }

    @Override
    public boolean affirmativeAction() {
        // this is so that we don't both go into the inner items of an item and try to execute it at the same time
        //if (super.affirmativeAction())
        //    return true;

        if (!isInMoveMode()) {
            if (getSelectedItem() instanceof HomeItem) {
                HomeItem selectedItem = (HomeItem) getSelectedItem();
                //Log.d("HomeView", currentIndex + " selected " + selectedItem.title + " @(" + selectedItem.colIndex + ", " + selectedItem.localIndex + ")");
                if (selectedItem.type == HomeItem.Type.explorer) {
                    ActivityManager.goTo(ActivityManager.Page.explorer);
                    return true;
//            HomeActivity.GetInstance().GoTo(HomeActivity.Page.explorer);
                } else if (selectedItem.type == HomeItem.Type.app) {
                    (IntentLaunchData.createFromPackage((String)selectedItem.obj, Intent.FLAG_ACTIVITY_NEW_TASK)).launch();
                    return true;
                } else if (selectedItem.type == HomeItem.Type.settings) {
                    //ActivityManager.goTo(ActivityManager.Page.settings);
                    //return true;
//            HomeActivity.GetInstance().GoTo(HomeActivity.Page.addToHome);
                } else if (selectedItem.type == HomeItem.Type.addAppOuter) {
                    List<ResolveInfo> apps = PackagesCache.getLaunchableInstalledPackages();
                    XMBItem[] sortedApps = apps.stream().map(currentPkg -> new HomeItem(currentPkg.activityInfo.packageName, HomeItem.Type.addApp, PackagesCache.getAppLabel(currentPkg))).collect(Collectors.toList()).toArray(new XMBItem[0]);
                    Arrays.sort(sortedApps, Comparator.comparing(o -> o.getTitle().toLowerCase()));
                    selectedItem.setInnerItems(sortedApps);
                } else if (selectedItem.type == HomeItem.Type.addApp) {
                    Adapter adapter = getAdapter();
                    adapter.createColumnAt(adapter.getColumnCount() - 1, new HomeItem(selectedItem.obj, HomeItem.Type.app, selectedItem.getTitle()));
                    save(getItems());
                    return true;
                } else if (selectedItem.type == HomeItem.Type.addExplorer) {
                    Adapter adapter = getAdapter();
                    adapter.createColumnAt(adapter.getColumnCount() - 1, new HomeItem(HomeItem.Type.explorer, "Explorer"));
                    save(getItems());
                    return true;
                } else if (selectedItem.type == HomeItem.Type.addAssocOuter) {
                    IntentLaunchData[] intents = ShortcutsCache.getStoredIntents();
                    XMBItem[] intentItems = new XMBItem[intents.length];
                    for (int i = 0; i < intents.length; i++)
                        intentItems[i] = new HomeItem(intents[i].getId(), HomeItem.Type.addAssoc);
                    selectedItem.setInnerItems(intentItems);
                } else if (selectedItem.type == HomeItem.Type.addAssoc) {
                    PagedActivity currentActivity = ActivityManager.getCurrentActivity();
                    DynamicInputView dynamicInput = currentActivity.getDynamicInput();
                    dynamicInput.setTitle("Add " + ShortcutsCache.getIntent(((UUID)selectedItem.obj)).getDisplayName() + " Association to Home");
                    DynamicInputRow.TextInput titleInput = new DynamicInputRow.TextInput("Path");
                    DynamicInputRow.ButtonInput selectDirBtn = new DynamicInputRow.ButtonInput("Choose", v -> {
                        // TODO: add non-scoped storage alternative for when storage access is granted
                        //if (!AndroidHelpers.hasReadStoragePermission()) {
                            currentActivity.requestDirectoryAccess(null, uri -> {
                                if (uri != null) {
                                    currentActivity.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    Uri docUri = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri));
                                    //String path = getPath(this, docUri);
                                    titleInput.setText(getPath(context, docUri));
                                }
                            });
                        //} else {

                        //}
                    });
                    DynamicInputRow.ButtonInput okBtn = new DynamicInputRow.ButtonInput("Done", v -> {
                        // TODO: show some kind of error when input is invalid
                        Adapter adapter = getAdapter();
                        HomeItem assocItem = new HomeItem(selectedItem.obj, HomeItem.Type.assoc);
                        assocItem.addToDirsList(titleInput.getText());
                        adapter.createColumnAt(adapter.getColumnCount() - 1, assocItem);
                        save(getItems());
                        dynamicInput.setShown(false);
                    }, KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_ENTER);
                    DynamicInputRow.ButtonInput cancelBtn = new DynamicInputRow.ButtonInput("Cancel", v -> {
                        dynamicInput.setShown(false);
                    }, KeyEvent.KEYCODE_ESCAPE);
                    dynamicInput.setItems(new DynamicInputRow(titleInput, selectDirBtn), new DynamicInputRow(okBtn, cancelBtn));

                    dynamicInput.setShown(true);
                    return true;
                } else if (selectedItem.type == HomeItem.Type.assocExe) {
                    String path = (String)selectedItem.obj;
                    IntentLaunchData launcher = ShortcutsCache.getIntent((UUID)((HomeItem)getAdapter().getItem(getEntryPosition())).obj);
                    if (PackagesCache.isPackageInstalled(launcher.getPackageName()))
                        launcher.launch(path);
                    else
                        Log.e("IntentShortcutsView", "Failed to launch, " + launcher.getPackageName() + " is not installed on the device");
                    return true;
                } else if (selectedItem.type == HomeItem.Type.createAssoc) {
                    PagedActivity currentActivity = ActivityManager.getCurrentActivity();
                    DynamicInputView dynamicInput = currentActivity.getDynamicInput();
                    dynamicInput.setTitle("Create Association");
                    DynamicInputRow.TextInput displayNameInput = new DynamicInputRow.TextInput("Display Name");
                    List<ResolveInfo> pkgs = PackagesCache.getLaunchableInstalledPackages();
                    pkgs.sort(Comparator.comparing(PackagesCache::getAppLabel));
                    List<String> pkgNames = pkgs.stream().map(PackagesCache::getAppLabel).collect(Collectors.toList());
                    pkgNames.add(0, "Unlisted");
                    DynamicInputRow.TextInput pkgNameInput = new DynamicInputRow.TextInput("Package Name");
                    DynamicInputRow.Dropdown pkgsDropdown = new DynamicInputRow.Dropdown(index -> {
                        //Log.d("HomeView", "Dropdown index changed to " + index);
                        if (index >= 1) {
                            String pkgName = pkgs.get(index - 1).activityInfo.packageName;
                            pkgNameInput.setText(pkgName);
                        }
                    });
                    final String[][] classNames = new String[1][];
                    DynamicInputRow.TextInput classNameInput = new DynamicInputRow.TextInput("Class Name");
                    DynamicInputRow.Dropdown classesDropdown = new DynamicInputRow.Dropdown(index -> {
                        if (index >= 1) {
                            String className = classNames[0][index];
                            classNameInput.setText(className);
                        }
                    });
                    classesDropdown.setVisibility(GONE);
                    pkgNameInput.addListener(new DynamicInputListener() {
                        @Override
                        public void onFocusChanged(View view, boolean hasFocus) {

                        }

                        @Override
                        public void onValuesChanged() {
                            //Log.d("HomeView", "Looking for package in list");
                            // populate next list with class names
                            int index = 0;
                            for (int i = 0; i < pkgs.size(); i++)
                                if (pkgs.get(i).activityInfo.packageName.equals(pkgNameInput.getText())) {
                                    index = i + 1;
                                    break;
                                }
                            //Log.d("HomeView", "Index of " + pkgNameInput.getText() + " is " + index);
                            // if the user is typing and they type a valid package name, then select it in the drop down
                            String[] classes = new String[0];
                            pkgsDropdown.setIndex(index);
                            if (index >= 1) { // 0 is unlisted, so skip
                                //Log.d("HomeView", "Package exists");

                                String[] tmp = PackagesCache.getClassesOfPkg(pkgNameInput.getText());
                                if (tmp.length > 0) {
                                    classes = new String[tmp.length + 1];
                                    System.arraycopy(tmp, 0, classes, 1, tmp.length);
                                    classes[0] = "Unlisted";
                                }
                            }
                            classNames[0] = classes;
                            classesDropdown.setOptions(classes);
                            classesDropdown.setVisibility(classes.length > 0 ? VISIBLE : GONE);
                            //Log.d("HomeView", "pkgNameInput value changed to " + pkgNameInput.getText());
                        }
                    });
                    pkgsDropdown.setOptions(pkgNames.toArray(new String[0]));
                    String[] intentActions = PackagesCache.getAllIntentActions();
                    String[] actionsTmp = PackagesCache.getAllIntentActionNames();
                    String[] intentActionNames = new String[actionsTmp.length + 1];
                    System.arraycopy(actionsTmp, 0, intentActionNames, 1, actionsTmp.length);
                    intentActionNames[0] = "Unlisted";
                    DynamicInputRow.TextInput actionInput = new DynamicInputRow.TextInput("Intent Action");
                    DynamicInputRow.Dropdown actionsDropdown = new DynamicInputRow.Dropdown(index -> {
                        if (index > 0) {
                            String actionName = intentActions[index - 1];
                            actionInput.setText(actionName);
                        }
                    }, intentActionNames);
                    actionInput.addListener(new DynamicInputListener() {
                        @Override
                        public void onFocusChanged(View view, boolean hasFocus) {

                        }

                        @Override
                        public void onValuesChanged() {
                            int index = 0;
                            for (int i = 0; i < intentActions.length; i++)
                                if (actionInput.getText().equals(intentActions[i])) {
                                    index = i + 1;
                                    break;
                                }
                            actionsDropdown.setIndex(index);
                        }
                    });
                    DynamicInputRow.TextInput extensionsInput = new DynamicInputRow.TextInput("Associated Extensions (comma separated)");
                    String[] dataTypes = { "None", "AbsPathAsProvider", "AbsolutePath", "FileNameWithExt", "FileNameWithoutExt" };
                    DynamicInputRow.Label dataLabel = new DynamicInputRow.Label("Data");
                    DynamicInputRow.Dropdown dataDropdown = new DynamicInputRow.Dropdown(null, dataTypes);
                    DynamicInputRow.TextInput extrasInput = new DynamicInputRow.TextInput("Extras (comma separated pairs, second values can be same as data type [case sensitive])");

                    DynamicInputRow.ButtonInput okBtn = new DynamicInputRow.ButtonInput("Create", v -> {
                        String displayName = displayNameInput.getText();
                        String pkgName = pkgNameInput.getText();
                        String actionName = actionInput.getText();
                        String className = classNameInput.getText();
                        String extensionsRaw = extensionsInput.getText();
                        String[] extras = !extrasInput.getText().isEmpty() ? Arrays.stream(extrasInput.getText().split(",")).map(String::trim).toArray(String[]::new) : new String[0];
                        if (!displayName.isEmpty() && !pkgName.isEmpty() && !actionName.isEmpty() && !className.isEmpty() && !extensionsRaw.isEmpty() && (extras.length % 2 == 0)) {
                            // TODO: show some kind of error when input is invalid
                            // TODO: add ability to choose flags
                            // TODO: check if name exists
                            // TODO: work purely with files
                            String[] extensions = Stream.of(extensionsRaw.split(",")).map(ext -> { String result = ext.trim(); if(result.charAt(0) == '.') result = result.substring(1, result.length() - 1); return result; }).toArray(String[]::new);
                            IntentLaunchData newAssoc = new IntentLaunchData(displayName, actionName, pkgName, className, extensions, Intent.FLAG_ACTIVITY_NEW_TASK);
                            newAssoc.setDataType(IntentLaunchData.DataType.valueOf(dataDropdown.getOption(dataDropdown.getIndex())));
                            for (int i = 0; i < extras.length; i += 2)
                                newAssoc.addExtra(IntentPutExtra.parseFrom(extras[i], extras[i + 1]));
                            ShortcutsCache.addIntent(newAssoc);
                            dynamicInput.setShown(false);
                        }
//                        String path = displayNameInput.getText();
//                        if (path != null && AndroidHelpers.fileExists(path)) {
//                            AndroidHelpers.setWallpaper(context, AndroidHelpers.bitmapFromFile(path));
//                            dynamicInput.setShown(false);
//                        }
                    }, KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_ENTER);
                    DynamicInputRow.ButtonInput cancelBtn = new DynamicInputRow.ButtonInput("Cancel", v -> {
                        dynamicInput.setShown(false);
                    }, KeyEvent.KEYCODE_ESCAPE);
                    dynamicInput.setItems(new DynamicInputRow(displayNameInput), new DynamicInputRow(pkgNameInput, pkgsDropdown), new DynamicInputRow(classNameInput, classesDropdown), new DynamicInputRow(actionInput, actionsDropdown), new DynamicInputRow(extensionsInput), new DynamicInputRow(dataLabel, dataDropdown), new DynamicInputRow(extrasInput), new DynamicInputRow(okBtn, cancelBtn));

                    dynamicInput.setShown(true);
                    return true;
                } else if (selectedItem.type == HomeItem.Type.setImageBg) {
                    PagedActivity currentActivity = ActivityManager.getCurrentActivity();
                    DynamicInputView dynamicInput = currentActivity.getDynamicInput();
                    dynamicInput.setTitle("Set Image as Background");
                    DynamicInputRow.TextInput titleInput = new DynamicInputRow.TextInput("Image File Path");

                    DynamicInputRow.ButtonInput selectFileBtn = new DynamicInputRow.ButtonInput("Choose", v -> {
                        currentActivity.requestContent("file/*", uri -> {
                            if (uri != null)
                                titleInput.setText(uri.getPath());
                        });
                    });
                    DynamicInputRow.ButtonInput okBtn = new DynamicInputRow.ButtonInput("Apply", v -> {
                        // TODO: show some kind of error when image/path invalid
                        String path = titleInput.getText();
                        if (path != null && AndroidHelpers.fileExists(path)) {
                            AndroidHelpers.setWallpaper(context, AndroidHelpers.bitmapFromFile(path));
                            dynamicInput.setShown(false);
                        }
                    }, KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_ENTER);
                    DynamicInputRow.ButtonInput cancelBtn = new DynamicInputRow.ButtonInput("Cancel", v -> {
                        dynamicInput.setShown(false);
                    }, KeyEvent.KEYCODE_ESCAPE);
                    dynamicInput.setItems(new DynamicInputRow(titleInput, selectFileBtn), new DynamicInputRow(okBtn, cancelBtn));

                    dynamicInput.setShown(true);
                    return true;
                } else if (selectedItem.type == HomeItem.Type.setShaderBg) {
                    PagedActivity currentActivity = ActivityManager.getCurrentActivity();
                    DynamicInputView dynamicInput = currentActivity.getDynamicInput();
                    dynamicInput.setTitle("Set Shader as Background");
                    String[] options = { "Blue Dune", "Planet", "Custom" };
                    DynamicInputRow.TextInput titleInput = new DynamicInputRow.TextInput("Fragment Shader Path");
                    String fragDest = AndroidHelpers.combinePaths(Paths.SHADER_ITEMS_DIR_INTERNAL, "frag.fsh");
                    String fragTemp = AndroidHelpers.combinePaths(Paths.SHADER_ITEMS_DIR_INTERNAL, "frag.tmp");
                    String vertDest = AndroidHelpers.combinePaths(Paths.SHADER_ITEMS_DIR_INTERNAL, "vert.vsh");
                    String channel0Dest = AndroidHelpers.combinePaths(Paths.SHADER_ITEMS_DIR_INTERNAL, "channel0.png");
                    String channel1Dest = AndroidHelpers.combinePaths(Paths.SHADER_ITEMS_DIR_INTERNAL, "channel1.png");
                    String channel2Dest = AndroidHelpers.combinePaths(Paths.SHADER_ITEMS_DIR_INTERNAL, "channel2.png");
                    String channel3Dest = AndroidHelpers.combinePaths(Paths.SHADER_ITEMS_DIR_INTERNAL, "channel3.png");
                    final boolean[] alreadyBackedUp = { false };
                    Runnable backupExistingShader = () -> {
                        // if a background shader file already exists
                        if (AndroidHelpers.fileExists(fragDest)) {
                            // move background shader to a temporary file if we haven't already or else delete since its the previews the user has been trying out
                            if (!alreadyBackedUp[0]) {
                                alreadyBackedUp[0] = true;
                                ExplorerBehaviour.moveFiles(fragTemp, fragDest);
                            } else
                                ExplorerBehaviour.delete(fragDest);
                        }
                    };
                    if (AndroidHelpers.fileExists(fragTemp))
                        ExplorerBehaviour.delete(fragTemp);

                    DynamicInputRow.ButtonInput selectFileBtn = new DynamicInputRow.ButtonInput("Choose", v -> {
                        // TODO: add way to choose certain values within chosen shader
                        currentActivity.requestContent("file/*", uri -> {
                            if (uri != null)
                                titleInput.setText(uri.getPath());
                        });
                    });
                    DynamicInputRow.Dropdown dropdown = new DynamicInputRow.Dropdown(index -> {
                        titleInput.setVisibility(index == options.length - 1 ? View.VISIBLE : View.GONE);
                        selectFileBtn.setVisibility(index == options.length - 1 ? View.VISIBLE : View.GONE);
                    }, options);
                    DynamicInputRow.ButtonInput okBtn = new DynamicInputRow.ButtonInput("Preview", v -> {
                        // TODO: show some kind of error when input is invalid
                        // TODO: add scoped storage alternative for when no storage access is granted
                        AndroidHelpers.writeToFile(vertDest, AndroidHelpers.readAssetAsString(context, "Shaders/vert.vsh"));
                        boolean readyForPreview = false;
                        if (dropdown.getIndex() == 0) {
                            backupExistingShader.run();
                            AndroidHelpers.writeToFile(fragDest, AndroidHelpers.readAssetAsString(context, "Shaders/xmb.fsh"));
                            readyForPreview = true;
                        }
                        if (dropdown.getIndex() == 1) {
                            backupExistingShader.run();
                            AndroidHelpers.writeToFile(fragDest, AndroidHelpers.readAssetAsString(context, "Shaders/planet.fsh"));
                            AndroidHelpers.saveBitmapToFile(AndroidHelpers.readAssetAsBitmap(context, "Shaders/channel0.png"), channel0Dest);
                            AndroidHelpers.saveBitmapToFile(AndroidHelpers.readAssetAsBitmap(context, "Shaders/channel1.png"), channel1Dest);
                            AndroidHelpers.saveBitmapToFile(AndroidHelpers.readAssetAsBitmap(context, "Shaders/channel2.png"), channel2Dest);
                            AndroidHelpers.saveBitmapToFile(AndroidHelpers.readAssetAsBitmap(context, "Shaders/channel3.png"), channel3Dest);
                            readyForPreview = true;
                        }
                        if (dropdown.getIndex() == options.length - 1) {
                            //dropdown.getIndex();
                            String path = titleInput.getText();
                            if (AndroidHelpers.fileExists(path)) {
                                // if the chosen file is not the destination we want to copy to
                                if (!new File(path).getAbsolutePath().equalsIgnoreCase(new File(fragDest).getAbsolutePath())) {
                                    //Log.d("HomeView", path + " != " + fragDest);
                                    backupExistingShader.run();
                                    // copy the chosen file to the destination
                                    ExplorerBehaviour.copyFiles(fragDest, path);
                                    readyForPreview = true;
                                    //Log.d("HomeView", "Copied new shader to destination");
                                }
                            }
                        }
                        if (readyForPreview)
                            AndroidHelpers.setWallpaper(currentActivity, currentActivity.getPackageName(), ".Wallpaper.GLWallpaperService", result -> {
                                if (result.getResultCode() == Activity.RESULT_OK) {
                                    // delete the old background shader
                                    if (AndroidHelpers.fileExists(fragTemp))
                                        ExplorerBehaviour.delete(fragTemp);
                                    // TODO: restart live background to reflect any changes
                                    GLWallpaperService.requestReload();
                                    dynamicInput.setShown(false);
                                }
                            });
                    }, KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_ENTER);
                    DynamicInputRow.ButtonInput cancelBtn = new DynamicInputRow.ButtonInput("Cancel", v -> {
                        if (AndroidHelpers.fileExists(fragTemp)) {
                            // delete what was being previewed if anything
                            if (AndroidHelpers.fileExists(fragDest))
                                ExplorerBehaviour.delete(fragDest);
                            // return the old background shader
                            ExplorerBehaviour.moveFiles(fragDest, fragTemp);
                            // TODO: restart live background to reflect any changes (might not need this one)
                            GLWallpaperService.requestReload();
                        }
                        dynamicInput.setShown(false);
                    }, KeyEvent.KEYCODE_ESCAPE);
                    // so that they will only show up when the custom option is selected in the dropdown
                    titleInput.setVisibility(View.GONE);
                    selectFileBtn.setVisibility(View.GONE);
                    dynamicInput.setItems(new DynamicInputRow(dropdown), new DynamicInputRow(titleInput, selectFileBtn), new DynamicInputRow(okBtn, cancelBtn));

                    dynamicInput.setShown(true);
                    return true;
                }
            }
        }// else
        //    applyMove();

        return super.affirmativeAction();
    }
    @Override
    public boolean secondaryAction() {
        if (super.secondaryAction())
            return true;

        if (!isInMoveMode()) {
            PagedActivity currentActivity = ActivityManager.getCurrentActivity();
            if (!currentActivity.getSettingsDrawer().isDrawerOpen()) {
                Integer[] position = getPosition();
                boolean isNotSettings = position[0] < (getAdapter().getColumnCount() - 1);
                boolean hasColumnHead = getAdapter().isColumnHead(position[0], 0);
                boolean isColumnHead = getAdapter().isColumnHead(position);
                boolean hasInnerItems = getAdapter().hasInnerItems(position);
                boolean isInnerItem = position.length > 2;
                XMBItem selectedItem = (XMBItem)getSelectedItem();
                HomeItem homeItem = null;
                if (selectedItem instanceof HomeItem)
                    homeItem = (HomeItem)selectedItem;

                ArrayList<SettingsDrawer.ContextBtn> btns = new ArrayList<>();
                if (isNotSettings && !isColumnHead && !isInnerItem)
                    btns.add(moveItemBtn);
                if (isNotSettings && !isColumnHead && !isInnerItem)
                    btns.add(deleteBtn);
                if (isNotSettings && !isColumnHead && !isInnerItem && !hasInnerItems && homeItem.type != HomeItem.Type.explorer)
                    btns.add(uninstallBtn);
                btns.add(createColumnBtn);
                if (isNotSettings && hasColumnHead && !isInnerItem)
                    btns.add(moveColumnBtn);
                if (isNotSettings && hasColumnHead && !isInnerItem)
                    btns.add(deleteColumnBtn);
                btns.add(cancelBtn);

                currentActivity.getSettingsDrawer().setButtons(btns.toArray(new SettingsDrawer.ContextBtn[0]));
                currentActivity.getSettingsDrawer().setShown(true);
                return true;
            }
        }
        return false;
    }
    @Override
    public boolean cancelAction() {
        if (super.cancelAction())
            return true;

        if (!isInMoveMode()) {
            PagedActivity currentActivity = ActivityManager.getCurrentActivity();
            if (!currentActivity.getSettingsDrawer().isDrawerOpen()) {
                currentActivity.getSettingsDrawer().setShown(false);
                return true;
            }
        }
        return false;
    }

    public void deleteSelection() {
        Integer[] position = getPosition();
        getAdapter().removeSubItem(position[0], position[1]);
        save(getItems());
    }
    public void uninstallSelection(Consumer<ActivityResult> onResult) {
        HomeItem selectedItem = (HomeItem)getSelectedItem();
        String packageName = selectedItem.type == HomeItem.Type.app ? (String)selectedItem.obj : selectedItem.type == HomeItem.Type.assoc ? ShortcutsCache.getIntent((UUID)selectedItem.obj).getPackageName() : null;
        if (packageName != null)
            AndroidHelpers.uninstallApp(ActivityManager.getCurrentActivity(), packageName, onResult);
    }
    @Override
    public void refresh() {
        //Log.d("HomeView", "Refreshing home view");
//        Consumer<ArrayList<ArrayList<XMBItem>>> prosumer = items -> {
//
////            createSettingsColumn(settings -> {
////            });
//        };
        long loadHomeStart = SystemClock.uptimeMillis();

        ArrayList<ArrayList<XMBItem>> items;
        if (!cachedItemsExists()) {
            // if no file exists then add apps to the home
            // TODO: make optional?
            Log.d("HomeView", "Home items does not exist in data folder, creating...");
            items = createDefaultItems();
        }
        else {
            // if the file exists in the data folder then read it, if the read fails then create defaults
            Log.d("HomeView", "Home items exists in data folder, reading...");
            items = load();
            if (items == null)
                items = createDefaultItems();
        }
        save(items);
        items.add(createSettingsColumn());
        int cachedColIndex = colIndex;
        int cachedRowIndex = rowIndex;
        setAdapter(new XMBAdapter(getContext(), items));
        setIndex(cachedColIndex, cachedRowIndex, true);
        Log.i("HomeView", "Time to load home items: " + ((SystemClock.uptimeMillis() - loadHomeStart) / 1000f) + "s");
    }
    private static ArrayList<XMBItem> createSettingsColumn() {
        ArrayList<XMBItem> settingsColumn = new ArrayList<>();
        XMBItem[] innerSettings;

        XMBItem settingsItem = new XMBItem(null, "Settings", R.drawable.ic_baseline_settings_24);//, colIndex, localIndex++);
        settingsColumn.add(settingsItem);

        // TODO: add option to change icon alpha
        // TODO: add option to reset home items to default
        // TODO: add option to change home/explorer scale
        // TODO: move add association to home settings?
        // TODO: add edit association option (and/or add it in the context drawer)
//        innerSettings = new XMBItem[2];
//        innerSettings[0] = new HomeItem(HomeItem.Type.settings, "Set font size");
//        innerSettings[1] = new HomeItem(HomeItem.Type.settings, "Set typeface");
//        settingsItem = new XMBItem(null, "General", R.drawable.ic_baseline_view_list_24, innerSettings);
//        settingsColumn.add(settingsItem);

        innerSettings = new XMBItem[2];
        innerSettings[0] = new HomeItem(HomeItem.Type.addExplorer, "Add explorer item to home");
//        List<ResolveInfo> apps = PackagesCache.getLaunchableInstalledPackages();
//        List<XMBItem> sortedApps = apps.stream().map(currentPkg -> new HomeItem(currentPkg.activityInfo.packageName, HomeItem.Type.addApp, PackagesCache.getAppLabel(currentPkg))).collect(Collectors.toList());
//        sortedApps.sort(Comparator.comparing(o -> o.getTitle().toLowerCase()));
        innerSettings[1] = new HomeItem(HomeItem.Type.addAppOuter, "Add application to home");
        //innerSettings[2] = new HomeItem(HomeItem.Type.settings, "Add new column to home");
        settingsItem = new XMBItem(null, "Home", R.drawable.ic_baseline_home_24, innerSettings);
        settingsColumn.add(settingsItem);

        innerSettings = new XMBItem[2];
        innerSettings[0] = new HomeItem(HomeItem.Type.setImageBg, "Set picture as background");
        innerSettings[1] = new HomeItem(HomeItem.Type.setShaderBg, "Set shader as background");
        settingsItem = new XMBItem(null, "Background", R.drawable.ic_baseline_image_24, innerSettings);
        settingsColumn.add(settingsItem);

        //innerSettings = new XMBItem[0];
        //settingsItem = new XMBItem(null, "Explorer", R.drawable.ic_baseline_source_24, colIndex, localIndex++, innerSettings);
        //settingsColumn.add(settingsItem);

        innerSettings = new XMBItem[2];
//        IntentLaunchData[] intents = ShortcutsCache.getStoredIntents();
//        XMBItem[] intentItems = new XMBItem[intents.length];
//        for (int i = 0; i < intents.length; i++)
//            intentItems[i] = new HomeItem(intents[i].getId(), HomeItem.Type.addAssoc);
        innerSettings[0] = new HomeItem(HomeItem.Type.addAssocOuter, "Add association to home");
        innerSettings[1] = new HomeItem(HomeItem.Type.createAssoc, "Create new association");
        settingsItem = new XMBItem(null, "Associations", R.drawable.ic_baseline_send_time_extension_24, innerSettings);
        settingsColumn.add(settingsItem);

        return settingsColumn;
    }

    @Override
    protected void onAppliedMove(int fromColIndex, int fromLocalIndex, int toColIndex, int toLocalIndex) {
        save(getItems());
    }

    // TODO: remove assoc inner items
    public ArrayList<ArrayList<XMBItem>> getItems() {
        ArrayList<ArrayList<Object>> items = getAdapter().getItems();
        items.remove(items.size() - 1); // remove the settings
        for (int i = 0; i < items.size(); i++) {
            ArrayList<Object> column = items.get(i);
            for (int j = 0; j < column.size(); j++) {
                Object item = column.get(j);
                if (item instanceof HomeItem && ((HomeItem)item).type == HomeItem.Type.assoc)
                    ((HomeItem)item).clearInnerItems();
            }
        }
        return cast(items);
    }
    private static ArrayList<ArrayList<XMBItem>> cast(ArrayList<ArrayList<Object>> items) {
        ArrayList<ArrayList<XMBItem>> casted = new ArrayList<>();
        for (ArrayList<Object> column : items) {
            ArrayList<XMBItem> innerCasted = new ArrayList<>();
            for (Object item : column)
                innerCasted.add((XMBItem)item);
            casted.add(innerCasted);
        }
        return casted;
    }

    private static boolean cachedItemsExists() {
        return AndroidHelpers.fileExists(AndroidHelpers.combinePaths(Paths.HOME_ITEMS_DIR_INTERNAL, Paths.HOME_ITEMS_FILE_NAME));
    }
    private static void save(ArrayList<ArrayList<XMBItem>> items) {
        saveHomeItemsToFile(items, Paths.HOME_ITEMS_DIR_INTERNAL, Paths.HOME_ITEMS_FILE_NAME);
    }
    private static ArrayList<ArrayList<XMBItem>> load() {
        return loadHomeItemsFromFile(Paths.HOME_ITEMS_DIR_INTERNAL, Paths.HOME_ITEMS_FILE_NAME);
    }
    private static void saveHomeItemsToFile(ArrayList<ArrayList<XMBItem>> items, String parentDir, String fileName) {
        AndroidHelpers.makeDir(parentDir);
        String fullPath = AndroidHelpers.combinePaths(parentDir, fileName);
        //Serialaver.saveFile(items, fullPath);
        Serialaver.saveAsFSTJSON(items, fullPath);
    }
    private static ArrayList<ArrayList<XMBItem>> loadHomeItemsFromFile(String parentDir, String fileName) {
        ArrayList<ArrayList<XMBItem>> items = null;
        String path = AndroidHelpers.combinePaths(parentDir, fileName);
        if (AndroidHelpers.fileExists(path))
            //items = (ArrayList<ArrayList<XMBItem>>)Serialaver.loadFile(path);
            items = (ArrayList<ArrayList<XMBItem>>)Serialaver.loadFromFSTJSON(path);
        else
            Log.e("HomeView", "Attempted to read non-existant home items file @ " + path);
        return items;
    }
    private static ArrayList<ArrayList<XMBItem>> createDefaultItems() {
        Log.d("HomeView", "Retrieving default apps");
        long createDefaultStart = SystemClock.uptimeMillis();

        String[] categories = new String[] { "Games", "Audio", "Video", "Image", "Social", "News", "Maps", "Productivity", "Accessibility", "Other" };
        HashMap<Integer, ArrayList<XMBItem>> sortedApps = new HashMap<>();
        List<ResolveInfo> apps = PackagesCache.getLaunchableInstalledPackages();
        Log.d("HomeView", "Time to get installed packages: " + ((SystemClock.uptimeMillis() - createDefaultStart) / 1000f) + "s");
        createDefaultStart = SystemClock.uptimeMillis();
        ArrayList<ArrayList<XMBItem>> defaultItems = new ArrayList<>();
        // go through all apps creating HomeItems for them and sorting them into their categories
        int otherIndex = getOtherCategoryIndex();
        for (int i = 0; i < apps.size(); i++) {
            ResolveInfo currentPkg = apps.get(i);
            if (currentPkg.activityInfo.packageName.equals(OxShellApp.getContext().getPackageName()))
                continue;
            int category = -1;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                category = currentPkg.activityInfo.applicationInfo.category;
            if (category < 0)
                category = otherIndex;
            if (!sortedApps.containsKey(category))
                sortedApps.put(category, new ArrayList<>());
            ArrayList<XMBItem> currentList = sortedApps.get(category);
            currentList.add(new HomeItem(currentPkg.activityInfo.packageName, HomeItem.Type.app, PackagesCache.getAppLabel(currentPkg)));
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
            ArrayList<XMBItem> column = new ArrayList<>();
            // add the category item at the top
            column.add(new XMBItem(null, categories[catIndex], getDefaultIconForCategory(catIndex)));
            column.addAll(sortedApps.get(existingCategories.get(index)));
            defaultItems.add(column);
        }
        ArrayList<XMBItem> explorerColumn = new ArrayList<>();
        explorerColumn.add(new HomeItem(HomeItem.Type.explorer, "Explorer"));
        defaultItems.add(0, explorerColumn);
        Log.d("HomeView", "Time to sort packages: " + ((SystemClock.uptimeMillis() - createDefaultStart) / 1000f) + "s");
        return defaultItems;
    }
    public static int getDefaultIconForCategory(int category) {
        if (category == ApplicationInfo.CATEGORY_GAME)
            return R.drawable.ic_baseline_games_24;
        else if (category == ApplicationInfo.CATEGORY_AUDIO)
            return R.drawable.ic_baseline_headphones_24;
        else if (category == ApplicationInfo.CATEGORY_VIDEO)
            return R.drawable.ic_baseline_movie_24;
        else if (category == ApplicationInfo.CATEGORY_IMAGE)
            return R.drawable.ic_baseline_photo_camera_24;
        else if (category == ApplicationInfo.CATEGORY_SOCIAL)
            return R.drawable.ic_baseline_forum_24;
        else if (category == ApplicationInfo.CATEGORY_NEWS)
            return R.drawable.ic_baseline_newspaper_24;
        else if (category == ApplicationInfo.CATEGORY_MAPS)
            return R.drawable.ic_baseline_map_24;
        else if (category == ApplicationInfo.CATEGORY_PRODUCTIVITY)
            return R.drawable.ic_baseline_work_24;
        else if (category == ApplicationInfo.CATEGORY_ACCESSIBILITY)
            return R.drawable.ic_baseline_accessibility_24;
        else if (category == getOtherCategoryIndex())
            return R.drawable.ic_baseline_auto_awesome_24;
        else
            return R.drawable.ic_baseline_view_list_24;
    }
    private static int getOtherCategoryIndex() {
        return MathHelpers.max(ApplicationInfo.CATEGORY_GAME, ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_IMAGE, ApplicationInfo.CATEGORY_SOCIAL, ApplicationInfo.CATEGORY_NEWS, ApplicationInfo.CATEGORY_MAPS, ApplicationInfo.CATEGORY_PRODUCTIVITY, ApplicationInfo.CATEGORY_ACCESSIBILITY) + 1;
    }

    SettingsDrawer.ContextBtn moveColumnBtn = new SettingsDrawer.ContextBtn("Move Column", () ->
    {
        toggleMoveMode(true, true);
        ActivityManager.getCurrentActivity().getSettingsDrawer().setShown(false);
        return null;
    });
    SettingsDrawer.ContextBtn moveItemBtn = new SettingsDrawer.ContextBtn("Move Item", () ->
    {
        toggleMoveMode(true, false);
        ActivityManager.getCurrentActivity().getSettingsDrawer().setShown(false);
        return null;
    });
    SettingsDrawer.ContextBtn deleteBtn = new SettingsDrawer.ContextBtn("Remove Item", () ->
    {
        deleteSelection();
        ActivityManager.getCurrentActivity().getSettingsDrawer().setShown(false);
        return null;
    });
    SettingsDrawer.ContextBtn deleteColumnBtn = new SettingsDrawer.ContextBtn("Remove Column", () ->
    {
        getAdapter().removeColumnAt(getPosition()[0]);
        save(getItems());
        ActivityManager.getCurrentActivity().getSettingsDrawer().setShown(false);
        return null;
    });
    SettingsDrawer.ContextBtn createColumnBtn = new SettingsDrawer.ContextBtn("Create Column", () ->
    {
        // TODO: add way to pick icon
        PagedActivity currentActivity = ActivityManager.getCurrentActivity();
        DynamicInputView dynamicInput = currentActivity.getDynamicInput();
        dynamicInput.setTitle("Create Column");
        DynamicInputRow.TextInput titleInput = new DynamicInputRow.TextInput("Title");
        //DynamicInputRow.ImageDisplay imageDisplay = new DynamicInputRow.ImageDisplay(null, DataLocation.none);
        DynamicInputRow.ButtonInput okBtn = new DynamicInputRow.ButtonInput("Create", v -> {
            String title = titleInput.getText();
            getAdapter().createColumnAt(getPosition()[0], new XMBItem(null, title.length() > 0 ? title : "Unnamed"));
            save(getItems());
            dynamicInput.setShown(false);
        }, KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_ENTER);
        DynamicInputRow.ButtonInput cancelBtn = new DynamicInputRow.ButtonInput("Cancel", v -> {
            dynamicInput.setShown(false);
        }, KeyEvent.KEYCODE_ESCAPE);
        dynamicInput.setItems(new DynamicInputRow(titleInput), new DynamicInputRow(okBtn, cancelBtn));

        currentActivity.getSettingsDrawer().setShown(false);
        dynamicInput.setShown(true);
        return null;
    });
    SettingsDrawer.ContextBtn cancelBtn = new SettingsDrawer.ContextBtn("Cancel", () ->
    {
        ActivityManager.getCurrentActivity().getSettingsDrawer().setShown(false);
        return null;
    });
    SettingsDrawer.ContextBtn uninstallBtn = new SettingsDrawer.ContextBtn("Uninstall App", () ->
    {
        uninstallSelection((result) -> {
            if (result.getResultCode() == Activity.RESULT_OK)
                deleteSelection();
        });
        ActivityManager.getCurrentActivity().getSettingsDrawer().setShown(false);
        return null;
    });

    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }
}