# 插件化
宿主与插件（免安装）

## 组件化和插件化的区别
- 组件化：将一个APP分成多个模块，每个模块都是一个组件，开发过程中可以让这些组件相互依赖或单独调试部分组件，但最终发布时这些组件合并成一个**统一的APK**。组件化强调功能拆分，单独编译，单独开发，根据需求动态配置组件。
- 插件化：将整个APP拆分成很多模块，每个模块都是一个APK(组件化的每个模块是一个lib)，最终打包时将宿主APk和插件APK分开打包，插件APK通过动态下发到宿主APK。插件化更关注动态加载、热更新。
- 热修复：热修复强调的是在不需要二次安装应用的前提下修复已知的bug。
  ![组件化和插件化](https://img-blog.csdnimg.cn/45e778296ca54c4f89f192770dd647ac.png)
  ![热修复基本原理](https://img-blog.csdnimg.cn/fe2fec2d14c442a790815311ff7895b4.png)
  ![对比](https://img-blog.csdnimg.cn/0c5f4bb2f14f4b5090c3356912258b9a.png)
## 插件化的优点
- 减少安装apk的体积，按需下载模块
- 动态更新插件
- 宿主和插件分开编译，提升团队开发效率
- 解决方法数超过65535问题
- 插件无需安装即可运行

## 插件化发展历程
![发展历程](https://img-blog.csdnimg.cn/f2e7c5ec01e146539c5ab4fa1915e406.png)
1. 静态代理
   dynamic-load-apk最早使用ProxyActivity这种静态代理技术，由ProxyActivity去控制插件中PluginActivity的生命周期
2. 动态替换（HOOK）
   在实现原理上都是趋近于选择尽量少的hook，并通过在manifest中预埋一些组件实现对四大组件的动态插件化。像Replugin。
3. 容器化框架
   VirtualApp能够完全模拟app的运行环境，能够实现app的免安装运行和双开技术。Atlas是阿里的结合组件化和热修复技术的一个app基础框架，号称是一个容器化框架。

## 插件化框架对比
![插件化框架对比](https://img-blog.csdnimg.cn/131e7f5b3d2d4195b8f815aff4e12615.png)
## 插件化技术原理
实现插件化需要解决的问题
1. 插件类的加载，解决宿主加载插件以及插件加载宿主的问题
2. 资源文件的加载，解决宿主和插件的资源文件的加载问题，以及资源合并和资源冲突的问题
3. 四大组件的支撑，支撑包括Activity，BroadReceiver. ContentProvider，Service四大组件在插件中的正常使用

# 插件加载
## Java类的加载过程
详细分析：[Java类的加载过程](https://editor.csdn.net/md/?articleId=124854988)
加载——>连接（验证——>准备——>解析）——>初始化

在加载阶段，虚拟机主要完成三件事：
- 通过一个类的全限定名来获取定义此类的二进制字节流。
- 将这个字节流所代表的静态存储结构转化为方法区域的运行时数据结构。
- 在Java堆中生成一个代表这个类的Class对象，作为方法区数据的访问入口。

## Android 类加载机制
### ClassLoader类结构
![Android中类加载器继承结构](https://img-blog.csdnimg.cn/26f5c56c6048426b82fbed757a712290.png)
其中：
1. BootClassLoader
   和java虚拟机中不同的是，BootClassLoader是ClassLoader内部类，由java代码实现而不是c++实现，是Android平台上所有ClassLoader的最终parent，这个内部类是包内可见。
2. BaseDexClassLoader
   负责从指定的路径中加载类，加载类里面的各种校验、检查和初始化工作都由它来完成
3. PathClassLoader
   继承自BaseDexClassLoader，只能加载已经安装到Android系统的APK里的类，主要逻辑由BaseDexClassLoader实现
4. DexClassLoader
   继承自BaseDexClassLoader，可以加载用户自定义的其他路径里的类，主要逻辑都由BaseDexClassLoader实现。
   加载dex文件及包含dex文件的apk或jar。也支持从SD卡进行加载，这就意味着DexClassLoader可以在应用未安装的情况下加载dex相关文件。因此，它是**热修复和插件化技术的基础**。

### 双亲委派机制
一个类加载器在加载类时，首先它会把这个类请求委派给父类加载器去完成，每一层都是如此。一直递归到顶层，当父加载器无法完成这个请求时，子类才会尝试去加载。
![双亲委派机制](https://img-blog.csdnimg.cn/ce96cdcb593b4da3b0eacf6d8c59a262.jpeg#pic_center)
双亲委派机制的优点
- 避免重复加载，若已加载直接从缓存中读取。
- 安全性考虑，防止核心API被随意篡改。

### 源码
详细分析：[Android类加载流程](https://www.cnblogs.com/luoyesiqiu/p/classload.html)
![ClassLoader加载过程](https://img-blog.csdnimg.cn/b5c39e0f3001461e867400b965eda399.png)在线源码阅读：[http://androidxref.com/](http://androidxref.com/)
[PathClassLoader.java](http://androidxref.com/9.0.0_r3/xref/libcore/dalvik/src/main/java/dalvik/system/PathClassLoader.java)

```java
public class PathClassLoader extends BaseDexClassLoader {
    public PathClassLoader(String dexPath, ClassLoader parent) {
        super(dexPath, null, null, parent);
    }
    public PathClassLoader(String dexPath, String librarySearchPath, ClassLoader parent) {
        super(dexPath, null, librarySearchPath, parent);
    }
}
```

[BaseDexClassLoader.java](http://androidxref.com/9.0.0_r3/xref/libcore/dalvik/src/main/java/dalvik/system/BaseDexClassLoader.java)
每个BaseDexClassLoader都持有一个DexPathList，BaseDexClassLoader的findClass类调用了DexPathList的findClass。
```java
public class BaseDexClassLoader extends ClassLoader {
    private final DexPathList pathList;
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        List<Throwable> suppressedExceptions = new ArrayList<Throwable>();
        Class c = pathList.findClass(name, suppressedExceptions);
        if (c == null) {
            ClassNotFoundException cnfe = new ClassNotFoundException(
                    "Didn't find class \"" + name + "\" on path: " + pathList);
            for (Throwable t : suppressedExceptions) {
                cnfe.addSuppressed(t);
            }
            throw cnfe;
        }
        return c;
    }
```
[DexPathList.java](http://androidxref.com/9.0.0_r3/xref/libcore/dalvik/src/main/java/dalvik/system/DexPathList.java)
```java
/*package*/ final class DexPathList {
    /**
     * List of dex/resource (class path) elements.
     * 数组，用于存储已加载的dex或者jar的信息。
     */
    private Element[] dexElements;
    
    //遍历所有dexElements，并调用Element类的findClass。
    public Class<?> findClass(String name, List<Throwable> suppressed) {
        for (Element element : dexElements) {
            Class<?> clazz = element.findClass(name, definingContext, suppressed);
            if (clazz != null) {
                return clazz;
            }
        }

        if (dexElementsSuppressedExceptions != null) {
            suppressed.addAll(Arrays.asList(dexElementsSuppressedExceptions));
        }
        return null;
    }
```

[ClassLoader.java](http://androidxref.com/9.0.0_r3/xref/libcore/ojluni/src/main/java/java/lang/ClassLoader.java)

```java
public abstract class ClassLoader {
    // The parent class loader for delegation(委托)
    private final ClassLoader parent;
    
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, false);
    }
    protected Class<?> loadClass(String name, boolean resolve)
        throws ClassNotFoundException
    {
            // 首先判断该类型是否已经被加载
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                //如果没有被加载，就委托给父类加载或者委派给启动类加载器加载
                try {
                    if (parent != null) {
                        //如果存在父类加载器，就委派给父类加载器加载
                        c = parent.loadClass(name, false);
                    } else {
                        //如果不存在父类加载器，就检查是否是由启动类加载器加载的类，通过调用本地方法native Class findBootstrapClass(String name)
                        c = findBootstrapClassOrNull(name);
                    }
                } catch (ClassNotFoundException e) {
                    // ClassNotFoundException thrown if class not found
                    // from the non-null parent class loader
                }

                if (c == null) {
                    // 如果父类加载器和启动类加载器都不能完成加载任务，才调用自身的加载功能
                    c = findClass(name);
                }
            }
            return c;
    }
```
## 插件Dex的处理
![插件加载](https://img-blog.csdnimg.cn/1f620df2b3164ca289fe4057bccd0034.jpeg#pic_center)
```java
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
```
# 插件化中的组件支持
## Hook
Hook，钩子，勾住系统的程序逻辑，在某段SDK源码执行的过程中，通过代码拦截执行该逻辑，加入自己的代码逻辑。

Hook技巧：
- 掌握反射和代理模式
- 尽量Hook静态变量或单例对象
- 尽量Hook public的对象和方法

![Activity启动中的跨进程访问](https://img-blog.csdnimg.cn/077f857134e74137b10385d512843beb.jpeg#pic_center)Hook在插件化中的运用
![Hook在插件化中的运用](https://img-blog.csdnimg.cn/80781e1455ac46ef801b8fe2a46bdf23.jpeg#pic_center)
### Hook AMS
![Hook AMS](https://img-blog.csdnimg.cn/eb281e6c314e4dbca82b045754c81e66.jpeg#pic_center)![AMS的代理对象](https://img-blog.csdnimg.cn/9b185ae6f6444050a29b2a6470e00eb6.jpeg#pic_center)
### Hook Handler
![Hook Handler](https://img-blog.csdnimg.cn/386d32ec86de454d81fc1e93b39dc22b.jpeg#pic_center)
## 实现
```java
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
```

```java
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
```

```java
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
```

# 插件化中的资源加载
![Resources资源加载过程](https://img-blog.csdnimg.cn/c434f6d3877d4f1cb59e106327ab79cb.jpeg#pic_center)
![插件资源加载](https://img-blog.csdnimg.cn/c44743629708407489a1afb1555ef9f4.jpeg#pic_center)
```java
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
```
```java
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
```

