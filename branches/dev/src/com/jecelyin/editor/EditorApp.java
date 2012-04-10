package com.jecelyin.editor;

import android.app.Application;
import android.widget.Toast;

public class EditorApp extends Application
{
    public void onCreate() {
        super.onCreate();
    }
    
    public void onLowMemory() {
        Toast.makeText(getApplicationContext(), R.string.out_of_memory, Toast.LENGTH_LONG).show();
    }
}
