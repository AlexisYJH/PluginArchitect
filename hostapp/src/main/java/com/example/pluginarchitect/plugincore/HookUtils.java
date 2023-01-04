package com.example.pluginarchitect.plugincore;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

/**
 * @author AlexisYin
 */
public class HookUtils {
    /**
     * hook AMS对象
     * 对AMS的startActivity方法进行拦截
     * @param context
     */
    public static void hookAMS(Context context) throws Exception {
        //1. 获取AMS对象
        //1.1 获取静态属性ActivityManager.IActivityManagerSingleton的值
        Field iActivityManagerSingletonField = null;
        //API29 ActivityTaskManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Class<?> activityTaskManagerClazz = Class.forName("android.app.ActivityTaskManager");
            iActivityManagerSingletonField = activityTaskManagerClazz.getDeclaredField("IActivityTaskManagerSingleton");
        }
        //API26-28 ActivityManager
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            iActivityManagerSingletonField = ActivityManager.class.getDeclaredField("IActivityManagerSingleton");
        }
        //API26以下 ActivityManagerNative
        else {
            Class<?> activityManagerNativeClazz = Class.forName("android.app.ActivityManagerNative");
            iActivityManagerSingletonField = activityManagerNativeClazz.getDeclaredField("gDefault");
            
        }
        iActivityManagerSingletonField.setAccessible(true);
        Object iActivityManagerSingletonObject = iActivityManagerSingletonField.get(null);
        //Log.d("TAG", "kookAMS iActivityManagerSingletonObject: " + iActivityManagerSingletonObject);

        //1.2 获取Singleton的mInstance的属性值
        Class<?> singletonClazz = Class.forName("android.util.Singleton");
        Field mInstanceField = singletonClazz.getDeclaredField("mInstance");
        mInstanceField.setAccessible(true);
        //Android 29得到的mInstance对象为null, 先调用get方法完成mInstance的初始化
        singletonClazz.getMethod("get").invoke(iActivityManagerSingletonObject, null);
        Object AMSSubject = mInstanceField.get(iActivityManagerSingletonObject);

        //2. 对AMS对象进行代理
        Class<?> iActivityManagerInterface = null;
        //API29 IActivityTaskManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            iActivityManagerInterface = Class.forName("android.app.IActivityTaskManager");
        }
        //API29以下 IActivityManager
        else {
            iActivityManagerInterface = Class.forName("android.app.IActivityManager");
        }
        //Log.d("TAG", "kookAMS iActivityManagerInterface: " + iActivityManagerInterface);

        AMSInvocationHandler handler = new AMSInvocationHandler(context, AMSSubject);
        Object AMSProxy = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class[]{iActivityManagerInterface}, handler);
        mInstanceField.set(iActivityManagerSingletonObject, AMSProxy);

        //3. Invocation对AMS对象的方法进行拦截
    }

    /**
     * 获取到handler特定消息中的Intent进行处理
     * 将Intent对象中的RegisteredActivity替换为PluginActivity
     */
    public static void hookHandler() throws Exception {
        //1. 获取handle对象（mH属性值）
        //1.1 获取ActivityThread对象
        Class<?> activityThreadClazz = Class.forName("android.app.ActivityThread");
        Field sCurrentActivityThreadField = activityThreadClazz.getDeclaredField("sCurrentActivityThread");
        sCurrentActivityThreadField.setAccessible(true);
        Object activityThreadObject = sCurrentActivityThreadField.get(null);


        //1.2 获取ActivityThread对象的mH属性值
        Field mHField = activityThreadClazz.getDeclaredField("mH");
        mHField.setAccessible(true);
        Object handler = mHField.get(activityThreadObject);

        //2. 给handler的mCallback属性进行赋值
        Field mCallbackField = Handler.class.getDeclaredField("mCallback");
        mCallbackField.setAccessible(true);
        mCallbackField.set(handler, new MyCallback());

        //3. 在callback中将Intent对象中的RegisteredActivity替换为PluginActivity
    }
}
