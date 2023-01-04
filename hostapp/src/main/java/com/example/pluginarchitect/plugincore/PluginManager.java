package com.example.pluginarchitect.plugincore;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Log;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

/**
 * @author AlexisYin
 */
public class PluginManager {
    private static PluginManager sInstance;
    private Context context;

    private PluginManager(Context context) {
        this.context = context;
    }

    public static PluginManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new PluginManager(context);
        }
        return sInstance;
    }

    public void init() {
        try {
            //插件加载
            loadApk();
            //组件支持
            HookUtils.hookAMS(context);
            HookUtils.hookHandler();
        } catch (Exception e) {
            Log.d("TAG", "init: " + e);
        }
    }

    /*
    * https://www.androidos.net.cn/android/9.0.0_r8/raw/libcore/dalvik/src/main/java/dalvik/system/BaseDexClassLoader.java
    * BaseDexClassLoader.java: private final DexPathList pathList;
    * DexPathList.java: private Element[] dexElements;
    * */
    //加载插件的apk文件，且合并dexElements
    private void loadApk() throws Exception {
        //加载插件的apk
        //将pluginapp模块生成的pluginapp-debug.apk，上传到sdcard/Android/data/packagename/file/目录下
        String pluginApkPath = context.getExternalFilesDir(null).getAbsolutePath() + "/pluginapp-debug.apk";
        String cachePath = context.getDir("cache_plugin", Context.MODE_PRIVATE).getAbsolutePath();
        DexClassLoader dexClassLoader = new DexClassLoader(pluginApkPath, cachePath, null, context.getClassLoader());

        Class<?> baseDexClassLoader = dexClassLoader.getClass().getSuperclass();
        Field pathListField = baseDexClassLoader.getDeclaredField("pathList");
        pathListField.setAccessible(true);

        //1. 获取plugin的dexElements
        Object pluginPathListObject = pathListField.get(dexClassLoader);
        Class<?> pathListClass = pluginPathListObject.getClass();
        Field dexElementsField = pathListClass.getDeclaredField("dexElements");
        dexElementsField.setAccessible(true);
        Object pluginDexElements = dexElementsField.get(pluginPathListObject);
        Log.d("TAG", "pluginDexElements: " + pluginDexElements);

        //2. 获取host的dexElements
        ClassLoader pathClassLoader = context.getClassLoader();
        Object hostPathListObject = pathListField.get(pathClassLoader);
        Object hostDexElements = dexElementsField.get(hostPathListObject);
        Log.d("TAG", "hostDexElements: " + hostDexElements);

        //3. 合并
        int pluginDexElementsLength = Array.getLength(pluginDexElements);
        int hostDexElementsLength = Array.getLength(hostDexElements);
        int newDexElementsLength = pluginDexElementsLength + hostDexElementsLength;

        Object newDexElements = Array.newInstance(hostDexElements.getClass().getComponentType(), newDexElementsLength);
        for (int i = 0; i < newDexElementsLength; i++) {
            //plugin
            if (i < pluginDexElementsLength) {
                Array.set(newDexElements, i, Array.get(pluginDexElements, i));
            }
            //host
            else {
                Array.set(newDexElements, i, Array.get(hostDexElements, i-pluginDexElementsLength));
            }
        }
        dexElementsField.set(hostPathListObject, newDexElements);
        Log.d("TAG", "newDexElements: " + newDexElements);
    }


    /**
     * 获取插件的Resources对象
     * @return
     * @throws Exception
     */
    public Resources loadResources() throws Exception {
        String pluginApkPath = context.getExternalFilesDir(null).getAbsolutePath() + "/pluginapp-debug.apk";
        AssetManager assetManager = AssetManager.class.newInstance();

        Method addAssetPathMethod = AssetManager.class.getMethod("addAssetPath", String.class);
        addAssetPathMethod.setAccessible(true);
        addAssetPathMethod.invoke(assetManager, pluginApkPath);

        return new Resources(assetManager,
                context.getResources().getDisplayMetrics(),
                context.getResources().getConfiguration());
    }
}
