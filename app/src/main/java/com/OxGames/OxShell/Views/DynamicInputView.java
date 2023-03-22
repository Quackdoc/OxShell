package com.OxGames.OxShell.Views;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.OxGames.OxShell.Adapters.DynamicInputAdapter;
import com.OxGames.OxShell.Data.DynamicInputRow;
import com.OxGames.OxShell.Data.FontRef;
import com.OxGames.OxShell.Data.SettingsKeeper;
import com.OxGames.OxShell.Helpers.ActivityManager;
import com.OxGames.OxShell.Helpers.AndroidHelpers;
import com.OxGames.OxShell.Interfaces.DynamicInputListener;
import com.OxGames.OxShell.Interfaces.InputReceiver;
import com.OxGames.OxShell.PagedActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DynamicInputView extends FrameLayout implements InputReceiver {
    private boolean isShown = false;
    private final Context context;
    private BetterTextView title;
    private RecyclerView mainList;
    private int prevUIState;

    private List<DynamicInputRow.ButtonInput> gamepadable;
    private DynamicInputRow[] rows;

    //private boolean firstRun;
    private int directionKeyCode = -1;

    private List<Consumer<Boolean>> onShownListeners = new ArrayList<>();

    public void addShownListener(Consumer<Boolean> onShownListener) {
        onShownListeners.add(onShownListener);
    }
    public void removeShownListener(Consumer<Boolean> onShownListener) {
        onShownListeners.remove(onShownListener);
    }
    public void clearShownListeners() {
        onShownListeners.clear();
    }

    public DynamicInputView(@NonNull Context context) {
        super(context);
        this.context = context;
        init();
    }
    public DynamicInputView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }
    public DynamicInputView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        init();
    }
    public DynamicInputView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.context = context;
        init();
    }

    private boolean canFocusOn(DynamicInputRow.DynamicInput item) {
        return item != null && item.getVisibility() == VISIBLE && item.isEnabled() && item.inputType != DynamicInputRow.DynamicInput.InputType.label;
    }
    private void init() {
        setShown(false);
        setFocusable(false);
        //setClickable(true); // block out touch input to views behind
        //setBackgroundColor(Color.parseColor("#80323232"));

        LayoutParams layoutParams;

        layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        setLayoutParams(layoutParams);
        setFocusable(false);
        getViewTreeObserver().addOnGlobalFocusChangeListener(onFocusChange);

        FrameLayout header = new FrameLayout(context);
        layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.round(AndroidHelpers.getScaledDpToPixels(context, 40)));
        layoutParams.gravity = Gravity.TOP;
        header.setLayoutParams(layoutParams);
        header.setBackgroundColor(Color.parseColor("#66646464"));
        header.setFocusable(false);
        addView(header);

        title = new BetterTextView(context);
        layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        title.setLayoutParams(layoutParams);
        title.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        //title.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        int dip = Math.round(AndroidHelpers.getScaledDpToPixels(context, 8));
        title.setPadding(dip, 0, dip, 0);
        title.setTextAlignment(TEXT_ALIGNMENT_GRAVITY);
        title.setTextColor(Color.WHITE);
        title.setTypeface(SettingsKeeper.getFont());
        title.setOutlineColor(Color.BLACK);
        title.setOutlineSize(Math.round(AndroidHelpers.getScaledDpToPixels(context, 3)));
        title.setFocusable(false);
        header.addView(title);

        mainList = new RecyclerView(context);
        RecyclerView.LayoutParams recyclerParams = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dip = Math.round(AndroidHelpers.getScaledDpToPixels(context, 40));
        recyclerParams.topMargin = dip;
        recyclerParams.bottomMargin = dip;
        mainList.setLayoutParams(recyclerParams);
        dip = Math.round(AndroidHelpers.getScaledDpToPixels(context, 20));
        mainList.setPadding(dip, dip, dip, dip);
        mainList.setBackgroundColor(Color.parseColor("#66323232"));
        mainList.setLayoutManager(new LinearLayoutManager(context));
        mainList.setVisibility(VISIBLE);
        mainList.setFocusable(false);
        addView(mainList);

        FrameLayout footer = new FrameLayout(context);
        layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.round(AndroidHelpers.getScaledDpToPixels(context, 40)));
        layoutParams.gravity = Gravity.BOTTOM;
        footer.setLayoutParams(layoutParams);
        footer.setBackgroundColor(Color.parseColor("#66646464"));
        footer.setFocusable(false);
        addView(footer);
    }
    ViewTreeObserver.OnGlobalFocusChangeListener onFocusChange = (oldFocus, newFocus) -> {
        //Log.d("DynamicInputView", "onGlobalFocusChange");
        boolean isDirectional = directionKeyCode == KeyEvent.KEYCODE_DPAD_UP || directionKeyCode == KeyEvent.KEYCODE_DPAD_DOWN || directionKeyCode == KeyEvent.KEYCODE_DPAD_LEFT || directionKeyCode == KeyEvent.KEYCODE_DPAD_RIGHT;
        if (isDirectional) {
            View oldDynamicItem = oldFocus;
            int timeout = 0;
            while (oldDynamicItem != null && !(oldDynamicItem instanceof DynamicInputItemView) && timeout++ < 100)
                oldDynamicItem = oldDynamicItem.getParent() instanceof View ? (View) oldDynamicItem.getParent() : null;
            if (oldDynamicItem instanceof DynamicInputItemView) {
                DynamicInputRow.DynamicInput oldItem = ((DynamicInputItemView) oldDynamicItem).getInputItem();
                int nextRow = oldItem.row;
                int nextCol = oldItem.col;
                DynamicInputRow.DynamicInput nextItem = null;
                boolean withinBounds;
                if (directionKeyCode == KeyEvent.KEYCODE_DPAD_UP)
                    nextRow -= 1;
                if (directionKeyCode == KeyEvent.KEYCODE_DPAD_DOWN)
                    nextRow += 1;
                //nextRow = Math.min(Math.max(nextRow, 0), rows.length - 1);
                if (directionKeyCode == KeyEvent.KEYCODE_DPAD_LEFT)
                    nextCol -= 1;
                if (directionKeyCode == KeyEvent.KEYCODE_DPAD_RIGHT)
                    nextCol += 1;
                //nextCol = Math.min(Math.max(nextCol, 0), rows[nextRow].getCount() - 1);
                do {
                    withinBounds = (nextRow >= 0 && nextRow < rows.length) && (nextCol >= 0 && nextCol < rows[nextRow].getCount());
                    if (nextRow >= 0 && nextRow < rows.length) {
                        if (withinBounds)
                            nextItem = rows[nextRow].get(nextCol);
                        boolean foundFocusable = canFocusOn(nextItem);
                        if (!foundFocusable) {
                            if (directionKeyCode == KeyEvent.KEYCODE_DPAD_RIGHT || directionKeyCode == KeyEvent.KEYCODE_DPAD_UP || directionKeyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                                // if next item to go to has not been found and we were originally going up, down, or right, then search the right of the row for items
                                int startCol = nextCol;
                                while (!foundFocusable && ++nextCol < rows[nextRow].getCount()) {
                                    if (nextCol >= 0)
                                        nextItem = rows[nextRow].get(nextCol);
                                    foundFocusable = canFocusOn(nextItem);
                                }
                                if (!foundFocusable) {
                                    if (directionKeyCode == KeyEvent.KEYCODE_DPAD_DOWN || directionKeyCode == KeyEvent.KEYCODE_DPAD_UP) {
                                        // if next item still hasn't been found and we were originally going up or down, then search the left side
                                        nextCol = startCol;
                                        while (!foundFocusable && --nextCol >= 0) {
                                            if (nextCol < rows[nextRow].getCount())
                                                nextItem = rows[nextRow].get(nextCol);
                                            foundFocusable = canFocusOn(nextItem);
                                        }
                                        if (!foundFocusable) {
                                            // if still no focusable has been found then go to the next row based on if we were going up or down
                                            if (directionKeyCode == KeyEvent.KEYCODE_DPAD_UP) {
                                                nextRow -= 1;
                                                if (nextRow >= 0)
                                                    nextCol = Math.min(Math.max(oldItem.col, 0), rows[nextRow].getCount() - 1);
                                            }
                                            if (directionKeyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                                                nextRow += 1;
                                                if (nextRow < rows.length)
                                                    nextCol = Math.min(Math.max(oldItem.col, 0), rows[nextRow].getCount() - 1);
                                            }
                                        }
                                    }
                                }
                            }
                            if (directionKeyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                                // if no focusable was found and we were originally going left then keep searching left
                                while (!foundFocusable && --nextCol >= 0) {
                                    if (nextCol < rows[nextRow].getCount())
                                        nextItem = rows[nextRow].get(nextCol);
                                    foundFocusable = canFocusOn(nextItem);
                                }
                            }
                            //}
                        }
                        // should go right then left then continue down/up if going down/up
                        // should go up/down after exhausting left/right if going left/right
                    }
                } while (withinBounds && !canFocusOn(nextItem));
                //Log.d("DynamicInputView", "[" + oldItem.row + ", " + oldItem.col + "] => [" + nextRow + ", " + nextCol + "]");
                if (!canFocusOn(nextItem)) {
                    nextItem = oldItem;
                    nextRow = oldItem.row;
                    nextCol = oldItem.col;
                }

                DynamicInputItemView nextItemView = nextItem.view;
                View newDynamicItem = newFocus;
                while (newDynamicItem != null && !(newDynamicItem instanceof DynamicInputItemView))
                    newDynamicItem = newDynamicItem.getParent() instanceof View ? (View) newDynamicItem.getParent() : null;
                if (newFocus != null && newDynamicItem != nextItemView)
                    newFocus.clearFocus();
                nextItemView.requestFocus();
                for (int row = 0; row < rows.length; row++)
                    for (int col = 0; col < rows[row].getCount(); col++)
                        rows[row].get(col).setSelected(row == nextRow && col == nextCol);
                //Log.d("DynamicInputView", "[" + oldItem.row + ", " + oldItem.col + "] => [" + nextItem.getInputItem().row + ", " + nextItem.getInputItem().col + "]");
            }
            directionKeyCode = -1;
        } else {
            // when an item receives focus through touch, then make sure to unhighlight any other views
            View newDynamicItem = newFocus;
            int timeout = 0;
            while (newDynamicItem != null && !(newDynamicItem instanceof DynamicInputItemView) && timeout++ < 100)
                newDynamicItem = newDynamicItem.getParent() instanceof View ? (View) newDynamicItem.getParent() : null;
            if (newDynamicItem instanceof DynamicInputItemView) {
                DynamicInputRow.DynamicInput newItem = ((DynamicInputItemView)newDynamicItem).getInputItem();
                for (int row = 0; row < rows.length; row++)
                    for (int col = 0; col < rows[row].getCount(); col++)
                        rows[row].get(col).setSelected(row == newItem.row && col == newItem.col);
            }
        }
    };

    public void setTitle(String value) {
        title.setText(value);
    }
    public void setItems(DynamicInputRow... items) {
        if (mainList.getAdapter() != null)
            ((DynamicInputAdapter)mainList.getAdapter()).clearListeners();
        DynamicInputAdapter adapter = new DynamicInputAdapter(context, items);
        mainList.setAdapter(adapter);
        rows = items;

        gamepadable = new ArrayList<>();
        for (int i = 0; i < rows.length; i++) {
            DynamicInputRow.DynamicInput[] inputItems = rows[i].getAll();
            for (int j = 0; j < inputItems.length; j++) {
                DynamicInputRow.DynamicInput item = inputItems[j];
                item.row = i;
                item.col = j;
            }
        }

        // TODO: figure out how to request focus here on the first item
    }

    public boolean isOverlayShown() {
        return isShown;
    }
    public void setShown(boolean onOff) {
        for (Consumer<Boolean> onShownListener : onShownListeners)
            if (onShownListener != null)
                onShownListener.accept(onOff);

        isShown = onOff;
        setVisibility(onOff ? VISIBLE : GONE);
        PagedActivity current = ActivityManager.getCurrentActivity();
        if (onOff) {
            prevUIState = current.getSystemUIState();
            current.setNavBarHidden(true);
            current.setStatusBarHidden(true);
            //rowIndex = 0;
            //colIndex = 0;
        } else {
            current.setSystemUIState(prevUIState);
            if (mainList != null) {
                RecyclerView.Adapter adapter = mainList.getAdapter();
                if (adapter != null)
                    ((DynamicInputAdapter)adapter).clear();
            }
        }
    }

    @Override
    public boolean receiveKeyEvent(KeyEvent key_event) {
        //Log.d("DynamicInputView", key_event.toString());
        if (key_event.getAction() == KeyEvent.ACTION_DOWN)
            if (key_event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT || key_event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT || key_event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP || key_event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN)
                directionKeyCode = key_event.getKeyCode();

        if (key_event.getAction() == KeyEvent.ACTION_UP) {
            for (DynamicInputRow.ButtonInput button : gamepadable) {
                if (button.hasKeycode(key_event.getKeyCode())) {
                    button.executeAction();
                    return true;
                }
            }
            if (key_event.getKeyCode() == KeyEvent.KEYCODE_BACK)
                return true; // in case its not mapped to anything, then don't quit OxShell
        }
        return false;
    }
}
