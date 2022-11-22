package com.example.pluginapp;

import android.content.res.Resources;

import androidx.appcompat.app.AppCompatActivity;

/**
 * @author AlexisYin
 */
public class BaseActivity extends AppCompatActivity {
    @Override
    public Resources getResources() {
        //宿主和插件共用一个Application对象
        if (getApplication() != null && getApplication().getResources() != null) {
            return getApplication().getResources();
        }
        return super.getResources();
    }
}
