package com.OxGames.OxShell.Helpers;

import android.util.Log;

import com.google.gson.Gson;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Type;

public class Serialaver {
    public static void saveFile(Serializable obj, String path) {
        try {
            FileOutputStream file = new FileOutputStream(path);
            ObjectOutputStream out = new ObjectOutputStream(file);
            out.writeObject(obj);
            out.close();
            file.close();
        } catch(Exception e) {
            Log.e("Serialaver", e.getMessage());
        }
    }
    public static Object loadFile(String path) {
        Object obj = null;

        try {
            FileInputStream file = new FileInputStream(path);
            ObjectInputStream in = new ObjectInputStream(file);
            obj = in.readObject();
            in.close();
            file.close();
        } catch (Exception e) {
            Log.e("Serialaver", e.getMessage());
        }
        return obj;
    }
    public static void saveAsJSON(Object data, String absPath) {
        Gson gson = new Gson();
        if (!AndroidHelpers.fileExists(absPath))
            AndroidHelpers.makeFile(absPath);
        String json = gson.toJson(data);
        //Log.d("Serialaver", "Saving json to " + absPath + ":\n" + json);
        AndroidHelpers.writeToFile(absPath, json);
    }
    public static <T> T loadFromJSON(String absPath, Class<T> tClass) {
        Gson gson = new Gson();
        String json = AndroidHelpers.readFile(absPath);
        //Log.d("Serialaver", "Read json from " + absPath + ":\n" + json);
        return gson.fromJson(json, tClass);
    }
    public static <T> T loadFromJSON(String absPath, Type tType) {
        Gson gson = new Gson();
        String json = AndroidHelpers.readFile(absPath);
        //Log.d("Serialaver", "Read json from " + absPath + ":\n" + json);
        return gson.fromJson(json, tType);
    }
}
