package com.example.pluginapp;

import android.os.Bundle;
import android.util.Log;

public class PluginActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("TAG", "PluginActivity onCreate resources: " + getResources());
    }

    public static void doSomething() {
        Log.d("TAG", "doSomething");
    }

}