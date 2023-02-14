package com.OxGames.OxShell;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import com.OxGames.OxShell.Helpers.ActivityManager;
import com.OxGames.OxShell.Helpers.AndroidHelpers;

import java.io.File;
import java.util.Hashtable;

public class FileChooserActivity extends PagedActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentPage = ActivityManager.Page.chooser;
        ActivityManager.setCurrent(currentPage);
        setContentView(R.layout.activity_chooser);
        initViewsTable();
    }
//    @Override
//    public void startActivityForResult(Intent intent, int requestCode) {
//        super.startActivityForResult(intent, requestCode);
//        Log.d("StartForResult", requestCode + " "  + intent.toString());
//    }


    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setMarginsFor(R.id.parent_layout);
    }

    @Override
    protected void initViewsTable() {
        allPages = new Hashtable<>();
        allPages.put(ActivityManager.Page.chooser, findViewById(R.id.explorer_list));
    }

    public void sendResult(String path) {
        final Uri data = AndroidHelpers.uriFromPath(path);
        Intent returnIntent = new Intent();
        returnIntent.setData(data);
        returnIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        returnIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        setResult(Activity.RESULT_OK, returnIntent);
        Log.i("FileChooser", "Called from " + getCallingActivity() + " giving result " + data);
        finish();
    }
}
