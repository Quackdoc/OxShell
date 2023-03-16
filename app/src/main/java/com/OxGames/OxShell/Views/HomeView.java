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
import android.provider.OpenableColumns;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;

import com.OxGames.OxShell.Adapters.XMBAdapter;
import com.OxGames.OxShell.BuildConfig;
import com.OxGames.OxShell.Data.DataLocation;
import com.OxGames.OxShell.Data.DynamicInputRow;
import com.OxGames.OxShell.Data.ImageRef;
import com.OxGames.OxShell.Data.IntentPutExtra;
import com.OxGames.OxShell.Data.PackagesCache;
import com.OxGames.OxShell.Data.Paths;
import com.OxGames.OxShell.Data.ResImage;
import com.OxGames.OxShell.Data.ShortcutsCache;
import com.OxGames.OxShell.Data.XMBItem;
import com.OxGames.OxShell.FileChooserActivity;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HomeView extends XMBView implements Refreshable {
    public final static ResImage[] resourceImages = {
        new ResImage(R.drawable.ic_baseline_accessibility_24, "Accessibility"),
        new ResImage(R.drawable.ic_baseline_add_circle_outline_24, "Plus Circle"),
        new ResImage(R.drawable.ic_baseline_cancel_24, "Cross Circle"),
        new ResImage(R.drawable.ic_baseline_auto_awesome_24, "Stars"),
        new ResImage(R.drawable.ic_baseline_block_24, "Block"),
        new ResImage(R.drawable.ic_baseline_check_24, "Checkmark"),
        new ResImage(R.drawable.ic_baseline_construction_24, "Construction"),
        new ResImage(R.drawable.ic_baseline_folder_24, "Folder"),
        new ResImage(R.drawable.ic_baseline_forum_24, "Message Bubbles"),
        new ResImage(R.drawable.ic_baseline_games_24, "Directional Pad"),
        new ResImage(R.drawable.ic_baseline_headphones_24, "Headphones"),
        new ResImage(R.drawable.ic_baseline_hide_image_24, "Crossed Image"),
        new ResImage(R.drawable.ic_baseline_home_24, "Home"),
        new ResImage(R.drawable.ic_baseline_image_24, "Image"),
        new ResImage(R.drawable.ic_baseline_map_24, "Map"),
        new ResImage(R.drawable.ic_baseline_movie_24, "Film"),
        new ResImage(R.drawable.ic_baseline_newspaper_24, "Newspaper"),
        new ResImage(R.drawable.ic_baseline_photo_camera_24, "Camera"),
        new ResImage(R.drawable.ic_baseline_question_mark_24, "Question Mark"),
        new ResImage(R.drawable.ic_baseline_send_time_extension_24, "Send Puzzle Piece"),
        new ResImage(R.drawable.ic_baseline_settings_24, "Cog"),
        new ResImage(R.drawable.ic_baseline_source_24, "Source Folder"),
        new ResImage(R.drawable.ic_baseline_audio_file_24, "Audio File"),
        new ResImage(R.drawable.ic_baseline_video_file_24, "Video File"),
        new ResImage(R.drawable.ic_baseline_view_list_24, "List"),
        new ResImage(R.drawable.ic_baseline_work_24, "Suitcase"),
        new ResImage(R.drawable.baseline_info_24, "Info")
    };

    public HomeView(Context context) {
        super(context);
        init();
    }
    public HomeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    public HomeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
//        setLayoutParams(new GridView.LayoutParams(256, 256));
        init();
    }

    private void init() {
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
                    // TODO: show pop up explaining permissions?
                    ActivityManager.goTo(ActivityManager.Page.explorer);
                    return true;
//            HomeActivity.GetInstance().GoTo(HomeActivity.Page.explorer);
                } else if (selectedItem.type == HomeItem.Type.app) {
                    (IntentLaunchData.createFromPackage((String)selectedItem.obj, Intent.FLAG_ACTIVITY_NEW_TASK)).launch();
                    return true;
                } else if (selectedItem.type == HomeItem.Type.addAppOuter) {
                    List<ResolveInfo> apps = PackagesCache.getLaunchableInstalledPackages();
                    XMBItem[] sortedApps = apps.stream().map(currentPkg -> new HomeItem(currentPkg.activityInfo.packageName, HomeItem.Type.addApp, PackagesCache.getAppLabel(currentPkg))).collect(Collectors.toList()).toArray(new XMBItem[0]);
                    Arrays.sort(sortedApps, Comparator.comparing(o -> o.getTitle().toLowerCase()));
                    selectedItem.setInnerItems(sortedApps);
                } else if (selectedItem.type == HomeItem.Type.addApp) {
                    Adapter adapter = getAdapter();
                    adapter.createColumnAt(adapter.getColumnCount() - 1, new HomeItem(selectedItem.obj, HomeItem.Type.app, selectedItem.getTitle()));
                    Toast.makeText(ActivityManager.getCurrentActivity(), selectedItem.getTitle() + " added to home", Toast.LENGTH_SHORT).show();
                    save(getItems());
                    return true;
                } else if (selectedItem.type == HomeItem.Type.addExplorer) {
                    Adapter adapter = getAdapter();
                    adapter.createColumnAt(adapter.getColumnCount() - 1, new HomeItem(HomeItem.Type.explorer, "Explorer"));
                    Toast.makeText(ActivityManager.getCurrentActivity(), "Explorer added to home", Toast.LENGTH_SHORT).show();
                    save(getItems());
                    return true;
                } else if (selectedItem.type == HomeItem.Type.addAssocOuter) {
                    XMBItem[] intentItems;
                    IntentLaunchData[] intents = ShortcutsCache.getStoredIntents();
                    if (intents.length > 0) {
                        intentItems = new XMBItem[intents.length];
                        for (int i = 0; i < intents.length; i++)
                            intentItems[i] = new HomeItem(intents[i].getId(), HomeItem.Type.addAssoc);
                    } else {
                        intentItems = new XMBItem[1];
                        intentItems[0] = new XMBItem(null, "None created", ImageRef.from(R.drawable.ic_baseline_block_24, DataLocation.resource));
                    }
                    selectedItem.setInnerItems(intentItems);
                } else if (selectedItem.type == HomeItem.Type.appInfo) {
                    DynamicInputView dynamicInput = ActivityManager.getCurrentActivity().getDynamicInput();
                    dynamicInput.setTitle("Ox Shell Info");
                    DynamicInputRow.Label versionLabel = new DynamicInputRow.Label("Version: " + BuildConfig.VERSION_NAME);
                    versionLabel.setGravity(Gravity.CENTER);
                    DynamicInputRow.Label goldLabel = new DynamicInputRow.Label("Running in gold");
                    goldLabel.setGravity(Gravity.CENTER);
                    DynamicInputRow.Label byLabel = new DynamicInputRow.Label("Created by: Oxters Wyzgowski");
                    byLabel.setGravity(Gravity.CENTER);
                    DynamicInputRow.ButtonInput okBtn = new DynamicInputRow.ButtonInput("Ok", v -> {
                        dynamicInput.setShown(false);
                    }, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_START);

                    List<DynamicInputRow> rows = new ArrayList<>();
                    rows.add(new DynamicInputRow(versionLabel));
                    if (BuildConfig.GOLD)
                        rows.add(new DynamicInputRow(goldLabel));
                    rows.add(new DynamicInputRow(byLabel));
                    rows.add(new DynamicInputRow(okBtn));
                    dynamicInput.setItems(rows.toArray(new DynamicInputRow[0]));
                    dynamicInput.setShown(true);
                } else if (selectedItem.type == HomeItem.Type.saveLogs) {
                    PagedActivity currentActivity = ActivityManager.getCurrentActivity();
                    String[] logs = Arrays.stream(AndroidHelpers.listContents(Paths.LOGCAT_DIR_INTERNAL)).map(file -> file.getName().endsWith(".log") ? file.getAbsolutePath() : null).toArray(String[]::new);
                    if (logs.length > 0) {
                        currentActivity.requestCreateZipFile(uri -> {
                            AndroidHelpers.writeToUriAsZip(uri, logs);
                            Toast.makeText(currentActivity, "Saved " + logs.length + " log(s)", Toast.LENGTH_LONG).show();
                        }, "logs.zip");
                    } else
                        Toast.makeText(currentActivity, "No logs to save", Toast.LENGTH_LONG).show();
                } else if (selectedItem.type == HomeItem.Type.addAssoc) {
                    PagedActivity currentActivity = ActivityManager.getCurrentActivity();
                    DynamicInputView dynamicInput = currentActivity.getDynamicInput();
                    dynamicInput.setTitle("Add " + ShortcutsCache.getIntent(((UUID)selectedItem.obj)).getDisplayName() + " Association to Home");
                    DynamicInputRow.TextInput titleInput = new DynamicInputRow.TextInput("Path");
                    DynamicInputRow.ButtonInput selectDirBtn = new DynamicInputRow.ButtonInput("Choose", v -> {
                        Intent intent = new Intent();
                        //intent.setPackage(context.getPackageName());
                        intent.setClass(context, FileChooserActivity.class);
                        currentActivity.requestResult(intent, result -> {
                            Log.d("HomeView", result.toString() + ", " + (result.getData() != null ? result.getData().toString() : null) + ", " + (result.getData() != null && result.getData().getExtras() != null ? result.getData().getExtras().toString() : null));
                            if (result.getResultCode() == Activity.RESULT_OK) {
                                titleInput.setText(Uri.decode(result.getData().getData().toString()));
                            }
                        });
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
                    showAssocEditor("Create Association", null);
                    return true;
                } else if (selectedItem.type == HomeItem.Type.setImageBg) {
                    PagedActivity currentActivity = ActivityManager.getCurrentActivity();
                    DynamicInputView dynamicInput = currentActivity.getDynamicInput();
                    dynamicInput.setTitle("Set Image as Background");
                    //DynamicInputRow.TextInput titleInput = new DynamicInputRow.TextInput("Image File Path");
                    AtomicReference<Uri> permittedUri = new AtomicReference<>();

                    DynamicInputRow.ButtonInput selectFileBtn = new DynamicInputRow.ButtonInput("Choose", v -> {
                        currentActivity.requestContent(permittedUri::set, "image/*");
                    });
                    DynamicInputRow.ButtonInput okBtn = new DynamicInputRow.ButtonInput("Apply", v -> {
                        // TODO: show some kind of error when image/path invalid
                        if (permittedUri.get() != null) {
                            AndroidHelpers.setWallpaper(context, AndroidHelpers.readResolverUriAsBitmap(context, permittedUri.get()));
                            dynamicInput.setShown(false);
                        }
//                        String path = titleInput.getText();
//                        if (path != null && AndroidHelpers.uriExists(Uri.parse(path))) {
//                            AndroidHelpers.setWallpaper(context, AndroidHelpers.readResolverUriAsBitmap(context, Uri.parse(path)));
//                            dynamicInput.setShown(false);
//                        }
                    }, KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_ENTER);
                    DynamicInputRow.ButtonInput cancelBtn = new DynamicInputRow.ButtonInput("Cancel", v -> {
                        dynamicInput.setShown(false);
                    }, KeyEvent.KEYCODE_ESCAPE);
                    dynamicInput.setItems(new DynamicInputRow(selectFileBtn), new DynamicInputRow(okBtn, cancelBtn));

                    dynamicInput.setShown(true);
                    return true;
                } else if (selectedItem.type == HomeItem.Type.setShaderBg) {
                    PagedActivity currentActivity = ActivityManager.getCurrentActivity();
                    DynamicInputView dynamicInput = currentActivity.getDynamicInput();
                    dynamicInput.setTitle("Set Shader as Background");
                    String[] options = { "Blue Dune", "The Other Dune", "Planet", "Custom" };
                    //DynamicInputRow.TextInput titleInput = new DynamicInputRow.TextInput("Fragment Shader Path");
                    AtomicReference<Uri> permittedUri = new AtomicReference<>();
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
                        currentActivity.requestContent(uri -> {
                            String fileName = AndroidHelpers.queryUriDisplayName(uri);
                            if (fileName != null && fileName.endsWith(".fsh"))
                                permittedUri.set(uri);
                            else
                                Toast.makeText(currentActivity, "File must end with .fsh", Toast.LENGTH_LONG).show();
                        }, "*/*");
                    });
                    DynamicInputRow.Dropdown dropdown = new DynamicInputRow.Dropdown(index -> {
                        //titleInput.setVisibility(index == options.length - 1 ? View.VISIBLE : View.GONE);
                        selectFileBtn.setVisibility(index == options.length - 1 ? View.VISIBLE : View.GONE);
                    }, options);
                    DynamicInputRow.ButtonInput okBtn = new DynamicInputRow.ButtonInput("Preview", v -> {
                        // TODO: show some kind of error when input is invalid
                        // TODO: add scoped storage alternative for when no storage access is granted
                        AndroidHelpers.writeToFile(vertDest, AndroidHelpers.readAssetAsString(context, "Shaders/vert.vsh"));
                        boolean readyForPreview = false;
                        if (dropdown.getIndex() == 0) {
                            backupExistingShader.run();
                            AndroidHelpers.writeToFile(fragDest, AndroidHelpers.readAssetAsString(context, "Shaders/blue_dune.fsh"));
                            readyForPreview = true;
                        }
                        if (dropdown.getIndex() == 1) {
                            backupExistingShader.run();
                            AndroidHelpers.writeToFile(fragDest, AndroidHelpers.readAssetAsString(context, "Shaders/other_dune.fsh"));
                            readyForPreview = true;
                        }
                        if (dropdown.getIndex() == 2) {
                            backupExistingShader.run();
                            AndroidHelpers.writeToFile(fragDest, AndroidHelpers.readAssetAsString(context, "Shaders/planet.fsh"));
                            //Log.d("HomeView", "Saving channel0 to " + channel0Dest);
                            AndroidHelpers.saveBitmapToFile(AndroidHelpers.readAssetAsBitmap(context, "Shaders/channel0.png"), channel0Dest);
                            AndroidHelpers.saveBitmapToFile(AndroidHelpers.readAssetAsBitmap(context, "Shaders/channel1.png"), channel1Dest);
                            AndroidHelpers.saveBitmapToFile(AndroidHelpers.readAssetAsBitmap(context, "Shaders/channel2.png"), channel2Dest);
                            AndroidHelpers.saveBitmapToFile(AndroidHelpers.readAssetAsBitmap(context, "Shaders/channel3.png"), channel3Dest);
                            readyForPreview = true;
                        }
                        if (dropdown.getIndex() == options.length - 1) {
                            //dropdown.getIndex();
                            if (permittedUri.get() != null) {
                                backupExistingShader.run();
                                AndroidHelpers.saveStringToFile(fragDest, AndroidHelpers.readResolverUriAsString(context, permittedUri.get()));
                                readyForPreview = true;
                            }
//                            String path = titleInput.getText();
//                            if (AndroidHelpers.uriExists(Uri.parse(path))) {
//                                // if the chosen file is not the destination we want to copy to
//                                //if (!new File(path).getAbsolutePath().equalsIgnoreCase(new File(fragDest).getAbsolutePath())) {
//                                    //Log.d("HomeView", path + " != " + fragDest);
//                                    backupExistingShader.run();
//                                    // copy the chosen file to the destination
//                                    //ExplorerBehaviour.copyFiles(fragDest, path);
//                                    AndroidHelpers.saveStringToFile(fragDest, AndroidHelpers.readResolverUriAsString(context, Uri.parse(path)));
//                                    readyForPreview = true;
//                                    //Log.d("HomeView", "Copied new shader to destination");
//                                //}
//                            }
                        }
                        if (readyForPreview)
                            AndroidHelpers.setWallpaper(currentActivity, currentActivity.getPackageName(), ".Wallpaper.GLWallpaperService", result -> {
                                if (result.getResultCode() == Activity.RESULT_OK) {
                                    // delete the old background shader
                                    if (AndroidHelpers.fileExists(fragTemp))
                                        ExplorerBehaviour.delete(fragTemp);
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
                            GLWallpaperService.requestReload();
                        }
                        dynamicInput.setShown(false);
                    }, KeyEvent.KEYCODE_ESCAPE);
                    // so that they will only show up when the custom option is selected in the dropdown
                    //titleInput.setVisibility(View.GONE);
                    selectFileBtn.setVisibility(View.GONE);
                    dynamicInput.setItems(new DynamicInputRow(dropdown), new DynamicInputRow(selectFileBtn), new DynamicInputRow(okBtn, cancelBtn));

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
                if (homeItem != null && (homeItem.type == HomeItem.Type.addAssoc || homeItem.type == HomeItem.Type.assoc))
                    btns.add(editAssocBtn);
                if (homeItem != null && homeItem.type == HomeItem.Type.addAssoc)
                    btns.add(deleteAssocBtn);
                if (!isInnerItem)
                    btns.add(createColumnBtn);
                if (isNotSettings && hasColumnHead && !isInnerItem)
                    btns.add(moveColumnBtn);
                if (isNotSettings && hasColumnHead && !isInnerItem)
                    btns.add(editColumnBtn);
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

        XMBItem settingsItem = new XMBItem(null, "Settings", ImageRef.from(R.drawable.ic_baseline_settings_24, DataLocation.resource));//, colIndex, localIndex++);
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
        settingsItem = new XMBItem(null, "Home", ImageRef.from(R.drawable.ic_baseline_home_24, DataLocation.resource), innerSettings);
        settingsColumn.add(settingsItem);

        innerSettings = new XMBItem[2];
        innerSettings[0] = new HomeItem(HomeItem.Type.setImageBg, "Set picture as background");
        innerSettings[1] = new HomeItem(HomeItem.Type.setShaderBg, "Set shader as background");
        settingsItem = new XMBItem(null, "Background", ImageRef.from(R.drawable.ic_baseline_image_24, DataLocation.resource), innerSettings);
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
        settingsItem = new XMBItem(null, "Associations", ImageRef.from(R.drawable.ic_baseline_send_time_extension_24, DataLocation.resource), innerSettings);
        settingsColumn.add(settingsItem);

        innerSettings = new XMBItem[2];
        innerSettings[0] = new HomeItem(HomeItem.Type.appInfo, "App info");
        innerSettings[1] = new HomeItem(HomeItem.Type.saveLogs, "Save logs to file");
        settingsItem = new XMBItem(null, "About", ImageRef.from(R.drawable.baseline_info_24, DataLocation.resource), innerSettings);
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
        if (AndroidHelpers.fileExists(path)) {
            try {
                items = (ArrayList<ArrayList<XMBItem>>) Serialaver.loadFromFSTJSON(path);
            } catch (Exception e) { Log.e("HomeView", "Failed to load home items: " + e); }
        } else
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
            column.add(new XMBItem(null, categories[catIndex], ImageRef.from(getDefaultIconForCategory(catIndex), DataLocation.resource)));
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

//    private void editAssocBtn(IntentLaunchData toBeEdited) {
//        showAssocEditor("Edit Association", toBeEdited);
//    }
//    private void createAssocBtn() {
//        showAssocEditor("Create Association", null);
//    }
    private void showAssocEditor(String title, IntentLaunchData toBeEdited) {
        PagedActivity currentActivity = ActivityManager.getCurrentActivity();
        DynamicInputView dynamicInput = currentActivity.getDynamicInput();
        dynamicInput.setTitle(title);
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
        classNameInput.addListener(new DynamicInputListener() {
            @Override
            public void onFocusChanged(View view, boolean hasFocus) {

            }

            @Override
            public void onValuesChanged() {
                //Log.d("HomeView", "Looking for package in list");
                // populate next list with class names
                String[] currentClassNames = classesDropdown.getOptions();
                if (currentClassNames != null) {
                    int index = 0;
                    for (int i = 0; i < currentClassNames.length; i++)
                        if (currentClassNames[i].equals(classNameInput.getText())) {
                            index = i;
                            break;
                        }
                    // if the user is typing and they type a valid class name, then select it in the drop down
                    classesDropdown.setIndex(index);
                }
            }
        });
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
        String[] dataTypes = { IntentLaunchData.DataType.None.toString(), IntentLaunchData.DataType.AbsolutePath.toString(), IntentLaunchData.DataType.FileNameWithExt.toString(), IntentLaunchData.DataType.FileNameWithoutExt.toString() };
        DynamicInputRow.Label dataLabel = new DynamicInputRow.Label("Data");
        dataLabel.setGravity(Gravity.LEFT | Gravity.BOTTOM);
        DynamicInputRow.Dropdown dataDropdown = new DynamicInputRow.Dropdown(null, dataTypes);
        DynamicInputRow.TextInput extrasInput = new DynamicInputRow.TextInput("Extras (comma separated pairs, second values can be same as data type [case sensitive])");
        DynamicInputRow.Label errorLabel = new DynamicInputRow.Label("");
        //errorLabel.setVisibility(GONE);

        DynamicInputRow.ButtonInput okBtn = new DynamicInputRow.ButtonInput(toBeEdited != null ? "Apply" : "Create", v -> {
            String displayName = displayNameInput.getText();
            displayName = displayName.isEmpty() ? "Unnamed" : displayName;
            String pkgName = pkgNameInput.getText();
            String actionName = actionInput.getText();
            String className = classNameInput.getText();
            String extensionsRaw = extensionsInput.getText();
            String[] extras = !extrasInput.getText().isEmpty() ? Arrays.stream(extrasInput.getText().split(",")).map(String::trim).toArray(String[]::new) : new String[0];
            if (pkgName.isEmpty())
                errorLabel.setLabel("Must provide package name");
            else if (className.isEmpty())
                errorLabel.setLabel("Must provide class name");
            else if (actionName.isEmpty())
                errorLabel.setLabel("Must provide action");
            else if (extensionsRaw.isEmpty())
                errorLabel.setLabel("Must provide associated extensions");
            else if (extras.length % 2 != 0)
                errorLabel.setLabel("Extras must be a multiple of two");
            else {
                // TODO: add ability to choose flags
                String[] extensions = Stream.of(extensionsRaw.split(",")).map(ext -> { String result = ext.trim(); if(result.charAt(0) == '.') result = result.substring(1, result.length() - 1); return result; }).toArray(String[]::new);
                IntentLaunchData newAssoc = toBeEdited;
                if (newAssoc != null) {
                    newAssoc.setDisplayName(displayName);
                    newAssoc.setAction(actionName);
                    newAssoc.setPackageName(pkgName);
                    newAssoc.setClassName(className);
                    newAssoc.setExtensions(extensions);
                    newAssoc.clearExtras();
                } else
                    newAssoc = new IntentLaunchData(displayName, actionName, pkgName, className, extensions, Intent.FLAG_ACTIVITY_NEW_TASK);

                newAssoc.setDataType(IntentLaunchData.DataType.valueOf(dataDropdown.getOption(dataDropdown.getIndex())));
                for (int i = 0; i < extras.length; i += 2)
                    newAssoc.addExtra(IntentPutExtra.parseFrom(extras[i], extras[i + 1]));
                ShortcutsCache.saveIntentAndReload(newAssoc);
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

        if (toBeEdited != null) {
            // set values to assoc being edited
            displayNameInput.setText(toBeEdited.getDisplayName());
            pkgNameInput.setText(toBeEdited.getPackageName());
            classNameInput.setText(toBeEdited.getClassName());
            actionInput.setText(toBeEdited.getAction());
            extensionsInput.setText(Arrays.toString(toBeEdited.getExtensions()).replace("[", "").replace("]", ""));
            for (int i = 0; i < dataTypes.length; i++) {
                if (dataTypes[i].equals(toBeEdited.getDataType().toString())) {
                    dataDropdown.setIndex(i);
                    break;
                }
            }
            extrasInput.setText(Arrays.stream(toBeEdited.getExtras()).map(extra -> extra.getName() + ", " + (extra.getValue() != null ? extra.getValue() : extra.getExtraType())).collect(Collectors.joining(", ")));
        }
        dynamicInput.setItems(new DynamicInputRow(displayNameInput), new DynamicInputRow(pkgNameInput, pkgsDropdown), new DynamicInputRow(classNameInput, classesDropdown), new DynamicInputRow(actionInput, actionsDropdown), new DynamicInputRow(extensionsInput), new DynamicInputRow(dataLabel), new DynamicInputRow(dataDropdown), new DynamicInputRow(extrasInput), new DynamicInputRow(errorLabel), new DynamicInputRow(okBtn, cancelBtn));
        dynamicInput.setShown(true);
    }

    SettingsDrawer.ContextBtn moveColumnBtn = new SettingsDrawer.ContextBtn("Move Column", () ->
    {
        toggleMoveMode(true, true);
        ActivityManager.getCurrentActivity().getSettingsDrawer().setShown(false);
    });
    SettingsDrawer.ContextBtn moveItemBtn = new SettingsDrawer.ContextBtn("Move Item", () ->
    {
        toggleMoveMode(true, false);
        ActivityManager.getCurrentActivity().getSettingsDrawer().setShown(false);
    });
    SettingsDrawer.ContextBtn deleteBtn = new SettingsDrawer.ContextBtn("Remove Item", () ->
    {
        deleteSelection();
        ActivityManager.getCurrentActivity().getSettingsDrawer().setShown(false);
    });
    SettingsDrawer.ContextBtn deleteColumnBtn = new SettingsDrawer.ContextBtn("Remove Column", () ->
    {
        ImageRef colImgRef = ((XMBItem)getAdapter().getItem(getPosition()[0], 0)).getImgRef();
        if (colImgRef.getRefType() == DataLocation.file)
            ExplorerBehaviour.delete((String)colImgRef.getImageObj());
        getAdapter().removeColumnAt(getPosition()[0]);
        save(getItems());
        ActivityManager.getCurrentActivity().getSettingsDrawer().setShown(false);
    });
    SettingsDrawer.ContextBtn deleteAssocBtn = new SettingsDrawer.ContextBtn("Delete Association", () ->
    {
        HomeItem assocItem = (HomeItem)getSelectedItem();
        ShortcutsCache.deleteIntent((UUID)assocItem.obj);
        refresh();
        ActivityManager.getCurrentActivity().getSettingsDrawer().setShown(false);
    });
    SettingsDrawer.ContextBtn editAssocBtn = new SettingsDrawer.ContextBtn("Edit Association", () ->
    {
        HomeItem assocItem = (HomeItem)getSelectedItem();
        showAssocEditor("Edit Association", ShortcutsCache.getIntent((UUID)assocItem.obj));
        //refresh();
        ActivityManager.getCurrentActivity().getSettingsDrawer().setShown(false);
    });
    SettingsDrawer.ContextBtn editColumnBtn = new SettingsDrawer.ContextBtn("Edit Column", () -> {
        PagedActivity currentActivity = ActivityManager.getCurrentActivity();
        DynamicInputView dynamicInput = currentActivity.getDynamicInput();
        dynamicInput.setTitle("Edit Column");
        List<String> dropdownItems = Arrays.stream(resourceImages).map(ResImage::getName).collect(Collectors.toList());
        dropdownItems.add("Custom");
        XMBItem colItem = (XMBItem)getAdapter().getItem(getPosition()[0], 0);
        ImageRef origColIcon = colItem.getImgRef();
        int origDropdownIndex = resourceImages.length;
        if (origColIcon.getRefType() == DataLocation.resource)
            for (int i = 0; i < resourceImages.length; i++) {
                if (resourceImages[i].getId() == (int)origColIcon.getImageObj()) {
                    origDropdownIndex = i;
                    break;
                }
            }
        DynamicInputRow.ImageDisplay imageDisplay = new DynamicInputRow.ImageDisplay(ImageRef.from(null, DataLocation.none));
        //DynamicInputRow.TextInput filePathInput = new DynamicInputRow.TextInput("File Path");
        AtomicReference<Uri> permittedUri = new AtomicReference<>();
        Runnable updateImg = () -> {
            if (permittedUri.get() != null)
                imageDisplay.setImage(ImageRef.from(permittedUri.get(), DataLocation.resolverUri));
            else
                imageDisplay.setImage(ImageRef.from(R.drawable.ic_baseline_question_mark_24, DataLocation.resource));
        };
//        filePathInput.addListener(new DynamicInputListener() {
//            @Override
//            public void onFocusChanged(View view, boolean hasFocus) {
//
//            }
//
//            @Override
//            public void onValuesChanged() {
//                if (AndroidHelpers.uriExists(Uri.parse(filePathInput.getText())))
//                    imageDisplay.setImage(ImageRef.from(filePathInput.getText(), DataLocation.resolverUri));
//                else
//                    imageDisplay.setImage(ImageRef.from(R.drawable.ic_baseline_question_mark_24, DataLocation.resource));
//            }
//        });
        DynamicInputRow.ButtonInput chooseFileBtn = new DynamicInputRow.ButtonInput("Choose", v -> {
            currentActivity.requestContent(uri -> {
                permittedUri.set(uri);
                updateImg.run();
            }, "image/*");
            updateImg.run();
        });
        DynamicInputRow.Dropdown resourcesDropdown = new DynamicInputRow.Dropdown(index -> {
            Log.d("HomeView", "Resources dropdown index set to " + index);
            boolean isResource = index < resourceImages.length;
            if (!isResource)
                updateImg.run();
            //filePathInput.setVisibility(isResource ? GONE : VISIBLE);
            chooseFileBtn.setVisibility(isResource ? GONE : VISIBLE);
            if (index >= 0 && isResource)
                imageDisplay.setImage(ImageRef.from(resourceImages[index].getId(), DataLocation.resource));
        }, dropdownItems.toArray(new String[0]));
        DynamicInputRow.TextInput titleInput = new DynamicInputRow.TextInput("Title");
        DynamicInputRow.ButtonInput okBtn = new DynamicInputRow.ButtonInput("Done", v -> {
            String title = titleInput.getText();
            ImageRef imgRef = imageDisplay.getImageRef();
            // delete old icon
            if (imgRef != colItem.getImgRef())
                ExplorerBehaviour.delete((String)colItem.getImgRef().getImageObj());
            if (imgRef.getRefType() == DataLocation.resolverUri) {
                String iconPath = AndroidHelpers.combinePaths(Paths.ICONS_DIR_INTERNAL, UUID.randomUUID().toString());
                AndroidHelpers.saveBitmapToFile(AndroidHelpers.readResolverUriAsBitmap(context, permittedUri.get()), iconPath);
                imgRef = ImageRef.from(iconPath, DataLocation.file);
            }
            colItem.setTitle(title.length() > 0 ? title : "Unnamed");
            colItem.setImgRef(imgRef);
            save(getItems());
            refresh();
            dynamicInput.setShown(false);
        }, KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_ENTER);
        DynamicInputRow.ButtonInput cancelBtn = new DynamicInputRow.ButtonInput("Cancel", v -> {
            dynamicInput.setShown(false);
        }, KeyEvent.KEYCODE_ESCAPE);

        titleInput.setText(colItem.getTitle());
        resourcesDropdown.setIndex(origDropdownIndex);
        imageDisplay.setImage(colItem.getImgRef());
        dynamicInput.setItems(new DynamicInputRow(imageDisplay, resourcesDropdown), new DynamicInputRow(chooseFileBtn), new DynamicInputRow(titleInput), new DynamicInputRow(okBtn, cancelBtn));

        currentActivity.getSettingsDrawer().setShown(false);
        dynamicInput.setShown(true);
    });
    SettingsDrawer.ContextBtn createColumnBtn = new SettingsDrawer.ContextBtn("Create Column", () -> {
        PagedActivity currentActivity = ActivityManager.getCurrentActivity();
        DynamicInputView dynamicInput = currentActivity.getDynamicInput();
        dynamicInput.setTitle("Create Column");
        List<String> dropdownItems = Arrays.stream(resourceImages).map(ResImage::getName).collect(Collectors.toList());
        dropdownItems.add("Custom");
        DynamicInputRow.ImageDisplay imageDisplay = new DynamicInputRow.ImageDisplay(ImageRef.from(null, DataLocation.none));
        //DynamicInputRow.TextInput filePathInput = new DynamicInputRow.TextInput("File Path");
        AtomicReference<Uri> permittedUri = new AtomicReference<>();
        Runnable updateImg = () -> {
            if (permittedUri.get() != null)
                imageDisplay.setImage(ImageRef.from(permittedUri.get(), DataLocation.resolverUri));
            else
                imageDisplay.setImage(ImageRef.from(R.drawable.ic_baseline_question_mark_24, DataLocation.resource));
        };
//        filePathInput.addListener(new DynamicInputListener() {
//            @Override
//            public void onFocusChanged(View view, boolean hasFocus) {
//
//            }
//
//            @Override
//            public void onValuesChanged() {
//                if (AndroidHelpers.uriExists(Uri.parse(filePathInput.getText())))
//                    imageDisplay.setImage(ImageRef.from(filePathInput.getText(), DataLocation.resolverUri));
//                else
//                    imageDisplay.setImage(ImageRef.from(R.drawable.ic_baseline_question_mark_24, DataLocation.resource));
//            }
//        });
        DynamicInputRow.ButtonInput chooseFileBtn = new DynamicInputRow.ButtonInput("Choose", v -> {
            currentActivity.requestContent(uri -> {
                permittedUri.set(uri);
                updateImg.run();
            }, "image/*");
        });
        DynamicInputRow.Dropdown resourcesDropdown = new DynamicInputRow.Dropdown(index -> {
            Log.d("HomeView", "Resources dropdown index set to " + index);
            boolean isResource = index < resourceImages.length;
            if (!isResource)
                updateImg.run();
            //filePathInput.setVisibility(isResource ? GONE : VISIBLE);
            chooseFileBtn.setVisibility(isResource ? GONE : VISIBLE);
            if (index >= 0 && isResource)
                imageDisplay.setImage(ImageRef.from(resourceImages[index].getId(), DataLocation.resource));
        }, dropdownItems.toArray(new String[0]));
        DynamicInputRow.TextInput titleInput = new DynamicInputRow.TextInput("Title");
        DynamicInputRow.ButtonInput okBtn = new DynamicInputRow.ButtonInput("Create", v -> {
            String title = titleInput.getText();
            ImageRef imgRef = imageDisplay.getImageRef();
            if (imgRef.getRefType() == DataLocation.resolverUri) {
                String iconPath = AndroidHelpers.combinePaths(Paths.ICONS_DIR_INTERNAL, UUID.randomUUID().toString());
                AndroidHelpers.saveBitmapToFile(AndroidHelpers.readResolverUriAsBitmap(context, permittedUri.get()), iconPath);
                imgRef = ImageRef.from(iconPath, DataLocation.file);
            }
            getAdapter().createColumnAt(getPosition()[0], new XMBItem(null, title.length() > 0 ? title : "Unnamed", imgRef));
            save(getItems());
            dynamicInput.setShown(false);
        }, KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_ENTER);
        DynamicInputRow.ButtonInput cancelBtn = new DynamicInputRow.ButtonInput("Cancel", v -> {
            dynamicInput.setShown(false);
        }, KeyEvent.KEYCODE_ESCAPE);

        dynamicInput.setItems(new DynamicInputRow(imageDisplay, resourcesDropdown), new DynamicInputRow(chooseFileBtn), new DynamicInputRow(titleInput), new DynamicInputRow(okBtn, cancelBtn));

        currentActivity.getSettingsDrawer().setShown(false);
        dynamicInput.setShown(true);
    });
    SettingsDrawer.ContextBtn cancelBtn = new SettingsDrawer.ContextBtn("Cancel", () ->
    {
        ActivityManager.getCurrentActivity().getSettingsDrawer().setShown(false);
    });
    SettingsDrawer.ContextBtn uninstallBtn = new SettingsDrawer.ContextBtn("Uninstall App", () ->
    {
        uninstallSelection((result) -> {
            if (result.getResultCode() == Activity.RESULT_OK)
                deleteSelection();
        });
        ActivityManager.getCurrentActivity().getSettingsDrawer().setShown(false);
    });

    public static String getRealPathFromURI(Uri uri, Context context) {
        Cursor returnCursor = context.getContentResolver().query(uri, null, null, null, null);
        int nameIndex =  returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
//        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();
        String name = returnCursor.getString(nameIndex);
//        String size = Long.toString(returnCursor.getLong(sizeIndex));
        File file = new File(context.getFilesDir(), name);
//        try {
//            InputStream inputStream = context.getContentResolver().openInputStream(uri);
//            FileOutputStream outputStream = new FileOutputStream(file);
//            int read = 0;
//            int maxBufferSize = 1 * 1024 * 1024;
//            int bytesAvailable = inputStream.available();
//            //int bufferSize = 1024;
//            int bufferSize = Math.min(bytesAvailable, maxBufferSize);
//            byte[] buffers = new byte[bufferSize];
//            while (inputStream.read(buffers).also {
//                if (it != null) {
//                    read = it
//                }
//            } != -1) {
//                outputStream.write(buffers, 0, read)
//            }
//            Log.e("File Size", "Size " + file.length())
//            inputStream?.close()
//            outputStream.close()
//            Log.e("File Path", "Path " + file.path)
//
//        } catch (Exception e) {
//            Log.e("Exception", e.getMessage());
//        }
        return file.getPath();
    }
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