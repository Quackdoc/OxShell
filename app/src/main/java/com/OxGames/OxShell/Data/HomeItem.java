package com.OxGames.OxShell.Data;

import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import com.OxGames.OxShell.Helpers.ActivityManager;
import com.OxGames.OxShell.Interfaces.DirsCarrier;
import com.OxGames.OxShell.OxShellApp;
import com.OxGames.OxShell.R;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

public class HomeItem<T> extends XMBItem<T> implements DirsCarrier {
    public enum Type { explorer, app, assoc, settings, addApp, addExplorer, }
    public Type type;
    public ArrayList<String> extraData;

    public HomeItem(Type _type) {
        this(_type, null);
    }
    public HomeItem(Type _type, String _title) {
        this(_type, _title, null);
    }
    public HomeItem(T _obj, Type _type, String _title, XMBItem... innerItems) {
        super(_obj, _title, innerItems);
        type = _type;
        extraData = new ArrayList<>();
    }
    public HomeItem(Type _type, String _title, XMBItem... innerItems) {
        this(null, _type, _title, innerItems);
    }
    public HomeItem(T _obj, Type _type, XMBItem... innerItems) {
        this(_obj, _type, null, innerItems);
    }
    @Override
    public Drawable getIcon() {
        //Drawable icon = null;
        icon = super.getIcon();
        if (icon == null) {
            if (type == Type.explorer)
                icon = ContextCompat.getDrawable(OxShellApp.getContext(), R.drawable.ic_baseline_source_24);
            else if (type == Type.app || type == Type.addApp)
                icon = PackagesCache.getPackageIcon((String)obj);
            else if (type == Type.settings || type == Type.addExplorer)
                icon = ContextCompat.getDrawable(OxShellApp.getContext(), R.drawable.ic_baseline_construction_24);
            else if (type == Type.assoc)
                icon = PackagesCache.getPackageIcon((ShortcutsCache.getIntent((UUID)obj)).getPackageName());
        }
        return icon;
    }

    @Override
    public String getTitle() {
        if (type == Type.assoc) {
            IntentLaunchData intent = ShortcutsCache.getIntent((UUID)obj);
            ResolveInfo rsv = PackagesCache.getResolveInfo(intent.getPackageName());
            String pkgLabel;
            if (rsv != null)
                pkgLabel = PackagesCache.getAppLabel(rsv);
            else
                pkgLabel = "not_installed";
            return intent.getDisplayName() + " (" + pkgLabel + ")";
        }
        return super.getTitle();
    }

    @Override
    public String toString() {
        return "title: " + title + " item type: " + type.toString() + " type of obj: " + (obj != null ? obj.getClass() : "null");
    }
//    @Override
//    public Drawable getSuperIcon() {
//        Drawable icon = null;
//        if (type == Type.assoc)
//            icon = ContextCompat.getDrawable(ActivityManager.getCurrentActivity(), R.drawable.ic_baseline_view_list_24);
//        return icon;
//    }

    public void clearDirsList() {
        extraData = new ArrayList<>();
    }
    public void addToDirsList(String dir) {
        extraData.add(dir);
    }
    public void removeFromDirsList(String dir) {
        extraData.remove(dir);
    }
    public String[] getDirsList() {
        String[] arrayed = new String[extraData.size()];
        return extraData.toArray(arrayed);
    }
}
