package com.OxGames.OxShell;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

public class HomeContextMenu extends Dialog {
    public HomeView currentHomeView; //Should change to an interface and implement the interfaces wherever context menus are needed (HomeView, ExplorerView, PackagesView...)
    private SlideTouchListView contextListView;
    private DetailAdapter listAdapter;

    public HomeContextMenu(@NonNull Context context) {
        super(context, android.R.style.ThemeOverlay);
        setContentView(R.layout.home_context_menu);
        hideSystemUI();
        refresh();

        setBackgroundColor(Color.parseColor("#00000000"));
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent ev) {
        return contextListView.onTouchEvent(ev);
    }
    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent key_event) {
        //Log.d("DialogDispatchKey", key_event.toString());

        if (key_event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_B) {
            dismiss();
            return true;
        }

        return contextListView.receiveKeyEvent(key_event);
    }

    public void setBackgroundColor(int bgColor) {
        final Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setBackgroundDrawable(new ColorDrawable(bgColor));
    }

    public void hideSystemUI() {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    public void addButton(String label, Callable btnEvent) {
        int matchingIndex = listAdapter.getCount();
        listAdapter.add(new DetailItem(null, label, null, null));
        contextListView.addListener(index -> {
            if (index == matchingIndex) {
                //Do item action here
                try {
                    btnEvent.call();
                } catch (Exception ex) {

                }
            }
        });
    }
    public void refresh() {
        contextListView = findViewById(R.id.context_btns_list);
        listAdapter = new DetailAdapter(getContext());
        contextListView.setAdapter(listAdapter);
        //Button creation will be moved to where the context menus are being created
        addButton("Move", () -> {
            Log.d("DialogItemSelection", "Move");
            return null;
        });
        addButton("Remove", () -> {
            currentHomeView.deleteSelection();
            dismiss();
            return null;
        });
        addButton("Uninstall", () -> {
            currentHomeView.uninstallSelection();
            currentHomeView.deleteSelection(); //only if uninstall was successful
            dismiss();
            return null;
        });
    }
}
