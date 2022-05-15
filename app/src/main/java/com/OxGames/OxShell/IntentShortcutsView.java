package com.OxGames.OxShell;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class IntentShortcutsView extends SlideTouchListView {
    private static IntentShortcutsView instance;
    private ExplorerBehaviour explorerBehaviour;
    private HomeItem currentIntent;

    public IntentShortcutsView(Context context) {
        super(context);
        instance = this;
        explorerBehaviour = new ExplorerBehaviour();
        refresh();
    }
    public IntentShortcutsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        instance = this;
        explorerBehaviour = new ExplorerBehaviour();
        refresh();
    }
    public IntentShortcutsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        instance = this;
        explorerBehaviour = new ExplorerBehaviour();
        refresh();
    }

//    @Override
//    public boolean onKeyDown(int key_code, KeyEvent key_event) {
////        Log.d("ExplorerView", key_code + " " + key_event);
//        if (key_code == KeyEvent.KEYCODE_BUTTON_B || key_code == KeyEvent.KEYCODE_BACK) {
//            ActivityManager.GoTo(ActivityManager.Page.home);
//            return false;
//        }
//
//        return super.onKeyDown(key_code, key_event);
//    }
    @Override
    public boolean receiveKeyEvent(KeyEvent key_event) {
    //        Log.d("ExplorerView", key_code + " " + key_event);
        if (key_event.getAction() == KeyEvent.ACTION_DOWN) {
            if (key_event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_B || key_event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                ActivityManager.goTo(ActivityManager.Page.home);
                return true;
            }
        }

        return super.receiveKeyEvent(key_event);
    }

    @Override
    public void makeSelection() {
        File selectedItem = (File)((DetailItem)getItemAtPosition(properPosition)).obj;
        ((IntentLaunchData)currentIntent.obj).launch(selectedItem.getAbsolutePath());
    }

    public static void setLaunchItem(HomeItem intent) {
        instance.currentIntent = intent;
        instance.refresh();
    }

    @Override
    public void refresh() {
        PackagesCache.loadIntents();

        if (currentIntent != null) {
            String[] dirs = currentIntent.getDirsList();
            if (dirs != null && dirs.length > 0) {
                ArrayList<DetailItem> intentItems = new ArrayList<>();

                IntentLaunchData launchData = (IntentLaunchData) currentIntent.obj;
                String[] extensions = launchData.getExtensions();
                for (int i = 0; i < dirs.length; i++) {
                    ArrayList<File> executables = getItemsInDirWithExt(dirs[i], extensions);
                    for (int j = 0; j < executables.size(); j++) {
                        intentItems.add(new DetailItem(null, FileHelpers.removeExtension(executables.get(j).getName()), null, executables.get(j)));
                    }
                }

                DetailAdapter executablesAdapter = new DetailAdapter(getContext(), intentItems);
                setAdapter(executablesAdapter);
            }
        }
        super.refresh();
    }

    private ArrayList<File> getItemsInDirWithExt(String path, String[] extensions) {
        ArrayList<File> matching = new ArrayList<>();
        explorerBehaviour.setDirectory(path);
        File[] files = explorerBehaviour.listContents();
        boolean isEmpty = files == null || files.length <= 0;
        if (!isEmpty) {
            for (int i = 0; i < files.length; i++) {
                String ext = FileHelpers.getExtension(files[i].getAbsolutePath());
                if (Arrays.stream(extensions).anyMatch(otherExt -> otherExt.equalsIgnoreCase(ext)))
                    matching.add(files[i]);
            }
        }
        return matching;
    }
}
