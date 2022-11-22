package com.example.pluginarchitect.plugincore;


import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.util.List;

/**
 * @author AlexisYin
 */
public class MyCallback implements Handler.Callback {
    private static final int LAUNCH_ACTIVITY = 100;
    private static final  int EXECUTE_TRANSACTION = 159;


    @Override
    public boolean handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            //API 26
            case LAUNCH_ACTIVITY:
                try {
                    //msg.obj：ActivityClientRecord
                    Field intentField = msg.obj.getClass().getDeclaredField("intent");
                    replaceIntent(msg.obj, intentField);
                } catch (Exception e) {
                    Log.e("TAG", "MyCallback handleMessage: " + e);
                }
                break;
            //API 28
            case EXECUTE_TRANSACTION:
                try {
                    /*
                     * final ClientTransaction transaction = (ClientTransaction) msg.obj;
                     * ClientTransaction.java：private List<ClientTransactionItem> mActivityCallbacks;
                     * LaunchActivityItem.java：private Intent mIntent;
                     * */
                    //1. 获取mActivityCallbacks
                    Object clientTransactionObject = msg.obj;
                    Class<?> clientTransactionClazz = clientTransactionObject.getClass();
                    Field mActivityCallbacksField = clientTransactionClazz.getDeclaredField("mActivityCallbacks");
                    mActivityCallbacksField.setAccessible(true);
                    List mActivityCallbacks = (List)mActivityCallbacksField.get(clientTransactionObject);

                    //2. 遍历mActivityCallbacks，得到LaunchActivityItem
                    for (Object item: mActivityCallbacks) {
                        if ("android.app.servertransaction.LaunchActivityItem".equals(item.getClass().getName())) {
                            Field mIntentField = item.getClass().getDeclaredField("mIntent");
                            replaceIntent(item, mIntentField);
                        }
                    }
                    //3. 替换LaunchActivityItem的Intent
                } catch (Exception e) {
                    Log.e("TAG", "MyCallback handleMessage: " + e);
                }

                break;
        }
        return false;
    }

    private void replaceIntent(Object obj, Field intentField) throws IllegalAccessException {
        intentField.setAccessible(true);
        Intent intent = (Intent) intentField.get(obj);

        Parcelable actionIntent = intent.getParcelableExtra(AMSInvocationHandler.NEW_INTENT_EXTRA_NAME);
        if(actionIntent != null) {
            intentField.set(obj, actionIntent);
            Log.d("TAG", "MyCallback intent replaced: " + actionIntent);
        }
    }
}
