package com.example.pluginarchitect.plugincore;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.pluginarchitect.RegisteredActivity;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author AlexisYin
 */
public class AMSInvocationHandler implements InvocationHandler {
    public static final String NEW_INTENT_EXTRA_NAME = "actionIntent";
    private Context context;
    private Object subject;

    public AMSInvocationHandler(Context context, Object subject) {
        this.context = context;
        this.subject = subject;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("startActivity".equals(method.getName())) {
            Log.d("TAG", "AMSInvocationHandler startActivity invoke");
            //PluginActivity替換成RegisteredActivity
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (arg instanceof Intent) {
                    Intent newIntent = new Intent();
                    newIntent.setClass(context, RegisteredActivity.class);
                    newIntent.putExtra(NEW_INTENT_EXTRA_NAME, (Intent)arg);
                    args[i] = newIntent;
                    Log.d("TAG", "AMSInvocationHandler newIntent: " + newIntent);
                    break;
                }
            }
        }
        return method.invoke(subject, args);
    }
}
