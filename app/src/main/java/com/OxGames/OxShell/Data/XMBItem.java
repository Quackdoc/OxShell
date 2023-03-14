package com.OxGames.OxShell.Data;

import android.graphics.drawable.Drawable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class XMBItem<T> implements Serializable {
    public T obj;
    protected String title;
    //protected Object iconLoc;
    protected ImageRef iconLoc;
    protected List<XMBItem> innerItems;

    protected transient Drawable icon;

    public XMBItem(T _obj, String _title, ImageRef _iconLoc, XMBItem... innerItems) {
        obj = _obj;
        title = _title;
        iconLoc = _iconLoc;
        this.innerItems = new ArrayList<>();
        if (innerItems != null)
            Collections.addAll(this.innerItems, innerItems);
    }
    public XMBItem(T _obj, String _title, ImageRef _iconLoc) {
        this(_obj, _title, _iconLoc, null);
    }
    public XMBItem(T _obj, String _title, XMBItem... innerItems) {
        this(_obj, _title, null, innerItems);
    }

    public void getIcon(Consumer<Drawable> onIconLoaded) {
        if (icon == null && iconLoc != null) {
            onIconLoaded.accept(icon = iconLoc.getImage());
//            if (iconLoc instanceof Integer) {
//                onIconLoaded.accept(icon = ContextCompat.getDrawable(OxShellApp.getContext(), (Integer)iconLoc));
//            } else if (iconLoc instanceof Drawable) {
//                onIconLoaded.accept(icon = (Drawable)iconLoc);
//            } else if (iconLoc instanceof String) {
//                onIconLoaded.accept(icon = AndroidHelpers.bitmapToDrawable(OxShellApp.getContext(), AndroidHelpers.bitmapFromFile((String)iconLoc)));
//            }
        }
        onIconLoaded.accept(icon);
        //return icon;
    }

    public void setInnerItems(XMBItem... innerItems) {
        this.innerItems = new ArrayList<>(Arrays.asList(innerItems));
    }
    public boolean hasInnerItems() {
        return innerItems != null && innerItems.size() > 0;
    }
    public int getInnerItemCount() {
        return innerItems != null ? innerItems.size() : 0;
    }
    public XMBItem getInnerItem(int index) {
        return innerItems.get(index);
    }
    public void clearInnerItems() {
        if (innerItems != null)
            innerItems.clear();
    }
    public String getTitle() {
        return title;
    }
}
