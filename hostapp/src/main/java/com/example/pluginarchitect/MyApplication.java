package com.example.pluginarchitect;

import android.app.Application;
import android.content.res.Resources;
import android.util.Log;

import com.example.pluginarchitect.plugincore.PluginManager;

/**
 * @author AlexisYin
 */
public class MyApplication extends Application {

    private Resources resources;

    @Override
    public void onCreate() {
        super.onCreate();
        PluginManager pluginManager = PluginManager.getInstance(this);
        pluginManager.init();
        try {
            //将插件的Resources对象绑定到Application
            resources = pluginManager.loadResources();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d("TAG", "MyApplication onCreate resources: " + resources);
    }

    @Override
    public Resources getResources() {
        return resources == null ? super.getResources() : resources;
    }
}
