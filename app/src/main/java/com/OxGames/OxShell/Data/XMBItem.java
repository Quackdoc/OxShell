package com.OxGames.OxShell.Data;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import com.OxGames.OxShell.Helpers.ActivityManager;
import com.OxGames.OxShell.Helpers.AndroidHelpers;
import com.OxGames.OxShell.OxShellApp;
import com.OxGames.OxShell.R;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class XMBItem<T> implements Serializable {
    public T obj;
    public String title;
    // meant for when reloading items from file to keep their correct positions in the menu (do not set this manually)
    public int colIndex;
    public int localIndex;
    protected Object iconLoc;
    protected boolean iconIsResource;
    private List<XMBItem> innerItems;

    //protected transient Drawable icon;
    protected transient Drawable icon;
    private transient float currentX;
    private transient float currentY;
    private transient float prevX;
    private transient float prevY;

    public XMBItem(T _obj, String _title, Object _iconLoc, int _colIndex, int _localIndex, XMBItem... innerItems) {
        obj = _obj;
        title = _title;
        iconLoc = _iconLoc;
        iconIsResource = _iconLoc instanceof Integer;
        //icon = _icon;
        colIndex = _colIndex;
        localIndex = _localIndex;
        this.innerItems = new ArrayList<>();
        if (innerItems != null)
            Collections.addAll(this.innerItems, innerItems);
    }
    public XMBItem(T _obj, String _title, Object _iconLoc) {
        this(_obj, _title, _iconLoc, -1, -1, null);
//        obj = _obj;
//        title = _title;
//        iconLoc = _iconLoc;
//        iconIsResource = _iconLoc instanceof Integer;
//        //icon = _icon;
//        colIndex = _colIndex;
//        localIndex = _localIndex;
//        this.innerItems = new ArrayList<>();
//        Collections.addAll(this.innerItems, innerItems);
    }
    public XMBItem(T _obj, String _title, int _colIndex, int _localIndex, XMBItem... innerItems) {
        this(_obj, _title, null, _colIndex, _localIndex, innerItems);
    }
    public XMBItem(T _obj, String _title, XMBItem... innerItems) {
        this(_obj, _title, null, -1, -1, innerItems);
    }

    public Drawable getIcon() {
        if (icon == null && iconLoc != null) {
            if (iconIsResource) {
                icon = ContextCompat.getDrawable(OxShellApp.getContext(), (Integer)iconLoc);
            } else if (iconLoc instanceof Drawable) {
                icon = (Drawable)iconLoc;
            } else if (iconLoc instanceof Bitmap) {
                icon = AndroidHelpers.bitmapToDrawable(OxShellApp.getContext(), AndroidHelpers.bitmapFromFile((String)iconLoc));
            }
        }
        return icon;
    }

    public boolean hasInnerItems() {
        return innerItems != null && innerItems.size() > 0;
    }
    public int getInnerItemCount() {
        return innerItems != null ? innerItems.size() : 0;
//        int size = 0;
//        if (innerItems != null) {
//            size = innerItems.size();
//            for (XMBItem innerItem : innerItems)
//                if (innerItem != null)
//                    size += innerItem.getInnerItemCount();
//        }
//        return size;
    }
    public XMBItem getInnerItem(int index) {
        return innerItems.get(index);
    }

    public float getX() {
        return currentX;
    }
    public float getY() {
        return currentY;
    }
    public float getPrevX() {
        return prevX;
    }
    public float getPrevY() {
        return prevY;
    }
    public void setX(float x) {
        prevX = currentX;
        currentX = x;
    }
    public void setY(float y) {
        prevY = currentY;
        currentY = y;
    }
}
