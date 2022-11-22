package com.example.pluginarchitect;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //测试plugin的加载
        try {
            Class<?> clazz = Class.forName("com.example.pluginapp.PluginActivity");
            Log.d("TAG", "onCreate: " + clazz);
            clazz.getMethod("doSomething").invoke(null, null);

        } catch (Exception e) {
            Log.e("TAG", "onCreate: " + e);
        }

    }

    public void jump(View view) {
        //测试plugin的组件支持
        /*
        * android.content.ActivityNotFoundException: Unable to find explicit activity class {com.example.pluginapp/com.example.pluginapp.PluginActivity};
        * have you declared this activity in your AndroidManifest.xml?
        * AMS会检查Activity是否在清单文件中注册
        * -->Hook AMS
        * */
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.example.pluginapp", "com.example.pluginapp.PluginActivity"));
        startActivity(intent);
    }
}