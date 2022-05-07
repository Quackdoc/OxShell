package com.OxGames.OxShell;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;

import com.google.gson.Gson;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class IntentLaunchData implements Serializable {
    //Might be best to implement a guid that other data structures can identify by so they don't store copies
    private UUID id;

    public enum DataType { None, AbsolutePath, FileNameWithExt, FileNameWithoutExt }
    private String displayName;
    private ArrayList<String> associatedExtensions;
    private String action;
    private String packageName;
    private String className;
    private ArrayList<IntentPutExtra> extras;
    private DataType dataType = DataType.None;

    public IntentLaunchData() {

    }
    public IntentLaunchData(String _packageName) {
        id = UUID.randomUUID();
        packageName = _packageName;
    }
    public IntentLaunchData(String _displayName, String _action, String _packageName, String _className, String[] extensions) {
        id = UUID.randomUUID();
        displayName = _displayName;
        action = _action;
        packageName = _packageName;
        className = _className;
        extras = new ArrayList<>();
        associatedExtensions = new ArrayList<>();
        if (extensions != null && extensions.length > 0)
            for (int i = 0; i < extensions.length; i++)
                if (extensions[i] != null && !extensions[i].isEmpty())
                    associatedExtensions.add(extensions[i].toLowerCase());
//            Collections.addAll(associatedExtensions, extensions);
    }

    public void setAction(String value) {
        action = value;
    }
    public String getAction() {
        return action;
    }
    public void setPackageName(String value) {
        packageName = value;
    }
    public String getPackageName() {
        return packageName;
    }
    public void setClassName(String value) {
        className = value;
    }
    public String getClassName() {
        return className;
    }
    public void setDisplayName(String value) {
        displayName = value;
    }
    public String getDisplayName() {
        return displayName;
    }
    public void setExtensions(String[] values) {
        associatedExtensions = new ArrayList<>(Arrays.asList(values));
    }
    public String[] getExtensions() {
        String[] extensions = new String[associatedExtensions.size()];
        return associatedExtensions.toArray(extensions);
    }

    public Intent buildIntent(String[] extrasValues) {
        return buildIntent(null, extrasValues);
    }
    public Intent buildIntent(String data) {
        return buildIntent(data, null);
    }
    public Intent buildIntent() {
        return buildIntent(null, null);
    }
    public Intent buildIntent(String data, String[] extrasValues) {
        if (action != null && !action.isEmpty()) {
            Intent intent = new Intent();
            if (action != null && !action.isEmpty())
                intent.setAction(action);
            if (className != null && !className.isEmpty())
                intent.setComponent(new ComponentName(packageName, className));
            else if (packageName != null && !packageName.isEmpty())
                intent.setPackage(packageName);
            for (int i = 0; i < extras.size(); i++)
                intent.putExtra(extras.get(i).getName(), extrasValues[i]);
            if (data != null && !data.isEmpty())
                intent.setData(Uri.parse(data));
            return intent;
        } else
            return ActivityManager.getCurrentActivity().getPackageManager().getLaunchIntentForPackage(packageName);
    }

    public void addExtra(IntentPutExtra extra) {
        extras.add(extra);
    }
    public IntentPutExtra[] getExtras() {
        IntentPutExtra[] extrasArray = new IntentPutExtra[extras.size()];
        extrasArray = extras.toArray(extrasArray);
        return extrasArray;
    }
//    public void SetData(String _data) {
//        data = _data;
//    }
    public void setDataType(DataType _dataType) {
        dataType = _dataType;
    }
    public DataType getDataType() {
        return dataType;
    }

    public String toJSON() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
    public static IntentLaunchData fromJSON(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, IntentLaunchData.class);
    }
    public boolean containsExtension(String extension) {
        return associatedExtensions.contains(extension.toLowerCase());
    }

    public void launch(String data, String[] extrasValues) {
        //IntentLaunchData launchData = new IntentLaunchData(Intent.ACTION_VIEW, "com.dsemu.drastic", "com.dsemu.drastic.DraSticActivity");
        //launchData.AddExtra(new IntentPutExtra("GAMEPATH", clickedItem.absolutePath));
        startActivity(ActivityManager.getCurrentActivity(), buildIntent(data, extrasValues), null);
    }
    public void launch(String data) {
        String dataEntry = null;
        if (dataType != DataType.None)
            dataEntry = data;

        String[] extrasValues = null;
        if (extras != null && extras.size() > 0) {
            extrasValues = new String[extras.size()];
            for (int i = 0; i < extras.size(); i++)
                extrasValues[i] = data;
        }
        launch(dataEntry, extrasValues);
    }
    public void launch() {
        launch(null, null);
    }
}
