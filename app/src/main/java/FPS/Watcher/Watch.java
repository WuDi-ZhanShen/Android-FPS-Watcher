package FPS.Watcher;

import android.app.ActivityManager;
import android.app.TaskStackListener;
import android.content.AttributionSource;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;
import android.window.TaskFpsCallback;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;


public class Watch {

    public static final String SEND_BINDER_ACTION = "intent.FPSWatch.SendBinder";
    public static int currentWatchingTaskId = 0;
    public static boolean isFpsCallBackRegistered = false;
    public static String currentWatchingPackageName = "";
    public static String currentWatchingClassName = "";
    public static boolean isKeepWatching = false;
    public static IFpsCallback iFpsCallback = null;
    static public Intent intent = null;
    static public Object iActivityManager = null;
    static public Context context = null;
    public static WindowManager windowManager = null;
    public static Object iActivityTaskManager = null;
    public static Method getFocusedRootTaskInfoMethod = null;
    public static Method registerFpsCallbackMethod = null;
    public static Method unregisterFpsCallbackMethod = null;
    public static Method registerTaskStackListenerMethod = null;
    public static Method unregisterTaskStackListenerMethod = null;
    public static Method startServiceMethod = null;

    public static Handler handler = null;
    public static Executor executor = Runnable::run;
    public static TaskFpsCallback taskFpsCallback = new TaskFpsCallback() {
        public void onFpsReported(float f) {
            if (iFpsCallback != null) {
                try {
                    iFpsCallback.onFpsReported(f);
                } catch (RemoteException e) {
                }
            }
        }
    };

    public static TaskStackListener taskStackListener = new TaskStackListener() {
        @Override
        public void onTaskRemovalStarted(ActivityManager.RunningTaskInfo taskInfo) {
            // 正常APP内退出会调用，划卡退出不会调用
            if (taskInfo.taskId == currentWatchingTaskId) {
                try {
                    unregisterFpsCallbackMethod.invoke(windowManager, taskFpsCallback);
                    if (!isKeepWatching) iFpsCallback.onTargetTaskRemoved();
                } catch (Exception ignored) {
                }
                currentWatchingTaskId = 0;
            }
        }

        @Override
        public void onTaskRemoved(int taskId) {
            // 正常APP内退出会调用，划卡退出也会调用

            if (taskId == currentWatchingTaskId) {
                try {
                    unregisterFpsCallbackMethod.invoke(windowManager, taskFpsCallback);
                    if (!isKeepWatching) iFpsCallback.onTargetTaskRemoved();
                } catch (Exception ignored) {
                }
                currentWatchingTaskId = 0;
            }
        }

        @Override
        public void onTaskMovedToFront(ActivityManager.RunningTaskInfo taskInfo) {
            // 用于恢复FPS监测

            ComponentName componentName = taskInfo.baseActivity; // 或 taskInfo.topActivity
            if (componentName == null) return;
            String packageName = componentName.getPackageName();

            boolean isServiceRunning = iFpsCallback != null && iFpsCallback.asBinder().isBinderAlive();

            if (isServiceRunning) {
                if (isKeepWatching && taskInfo.taskId != currentWatchingTaskId) {
                    unregisterFpsCallback();
                    registerFpsCallback(taskInfo.taskId);
                    try {
                        iFpsCallback.onTargetTaskChanged(packageName);
                    } catch (Exception ignored) {
                        ignored.printStackTrace();
                    }
                    return;
                }
                if (isFpsCallBackRegistered) return;

                if (packageName.equals(currentWatchingPackageName)) {
                    registerFpsCallback(taskInfo.taskId);
                }
            }

        }

    };


    public static void main(String[] arguments) throws Throwable {

        if (Looper.getMainLooper() == null) {
            Looper.prepareMainLooper();
        }

        int uid = android.os.Process.myUid();
        if (uid != 0 && uid != 2000) {
            System.err.printf("Insufficient permission! Need to be launched by adb (uid 2000) or root (uid 0), but your uid is %d \n", uid);
            System.exit(-1);
            return;
        }

        // ActivityThread activityThread = new ActivityThread();
        Class<?> ACTIVITY_THREAD_CLASS = Class.forName("android.app.ActivityThread");
        Constructor<?> activityThreadConstructor = ACTIVITY_THREAD_CLASS.getDeclaredConstructor();
        activityThreadConstructor.setAccessible(true);
        Object ACTIVITY_THREAD = activityThreadConstructor.newInstance();
        // ActivityThread.sCurrentActivityThread = activityThread;
        Field sCurrentActivityThreadField = ACTIVITY_THREAD_CLASS.getDeclaredField("sCurrentActivityThread");
        sCurrentActivityThreadField.setAccessible(true);
        sCurrentActivityThreadField.set(null, ACTIVITY_THREAD);
        // activityThread.mSystemThread = true;
        Field mSystemThreadField = ACTIVITY_THREAD_CLASS.getDeclaredField("mSystemThread");
        mSystemThreadField.setAccessible(true);
        mSystemThreadField.setBoolean(ACTIVITY_THREAD, true);


        // 三星设备的CompactSandbox会用到mConfigurationController.getConfiguration()，报错NullPointer。也许这样手动可以解决
        {        // 构造 ConfigurationController
            Class<?> configControllerClass = Class.forName("android.app.ConfigurationController");
            Constructor<?> configControllerConstructor = configControllerClass.getDeclaredConstructor(Class.forName("android.app.ActivityThreadInternal"));
            configControllerConstructor.setAccessible(true);
            Object configurationController = configControllerConstructor.newInstance(ACTIVITY_THREAD);

            // 注入 mConfigurationController 字段
            Field mConfigField = ACTIVITY_THREAD_CLASS.getDeclaredField("mConfigurationController");
            mConfigField.setAccessible(true);
            mConfigField.set(ACTIVITY_THREAD, configurationController);
        }


        Method getSystemContextMethod = ACTIVITY_THREAD_CLASS.getDeclaredMethod("getSystemContext");
//        Method getSystemUiContextMethod = ACTIVITY_THREAD_CLASS.getDeclaredMethod("getSystemUiContext");
        context = (Context) getSystemContextMethod.invoke(ACTIVITY_THREAD);

        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
        Method getServiceMethod = serviceManagerClass.getMethod("getService", String.class);
        IBinder activityTaskBinder = (IBinder) getServiceMethod.invoke(null, "activity_task");
        Class<?> stubClass = Class.forName("android.app.IActivityTaskManager$Stub");
        iActivityTaskManager = stubClass.getMethod("asInterface", IBinder.class).invoke(null, activityTaskBinder);
        IBinder activityService = (IBinder) getServiceMethod.invoke(null, "activity");
        Class<?> iActivityManagerStubClass = Class.forName("android.app.IActivityManager$Stub");
        iActivityManager = iActivityManagerStubClass.getMethod("asInterface", IBinder.class).invoke(null, activityService);

        getFocusedRootTaskInfoMethod = iActivityTaskManager.getClass().getMethod("getFocusedRootTaskInfo");
        registerFpsCallbackMethod = WindowManager.class.getMethod("registerTaskFpsCallback", int.class,Executor.class, TaskFpsCallback.class);
        unregisterFpsCallbackMethod = WindowManager.class.getMethod("unregisterTaskFpsCallback", TaskFpsCallback.class);
        registerTaskStackListenerMethod = iActivityTaskManager.getClass().getMethod("registerTaskStackListener", Class.forName("android.app.ITaskStackListener"));
        unregisterTaskStackListenerMethod = iActivityTaskManager.getClass().getMethod("unregisterTaskStackListener", Class.forName("android.app.ITaskStackListener"));

        startServiceMethod = Class.forName("android.app.IActivityManager").getDeclaredMethod(
                "startService",
                Class.forName("android.app.IApplicationThread"), // caller
                Intent.class,                                    // service intent
                String.class,                                    // resolvedType
                boolean.class,                                   // requireForeground
                String.class,                                    // callingPackage
                String.class,                                    // callingFeatureId
                int.class                                         // userId
        );

        handler = new Handler(Looper.getMainLooper());
        registerTaskStackListenerMethod.invoke(iActivityTaskManager, taskStackListener);
        sendBinderToAppByStickyBroadcast(iActivityManager);
        Runtime.getRuntime().addShutdownHook(new Thread(Watch::exitFPSWatch));
        Looper.loop();
        exitFPSWatch();
    }

    private static void exitFPSWatch() {
        unregisterFpsCallback();
        try {
            unregisterTaskStackListenerMethod.invoke(iActivityTaskManager, taskStackListener);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        removeStickyBroadcast();
    }


    public static void registerFpsCallback(int taskId) {
        if (isFpsCallBackRegistered) return;
        try {
            registerFpsCallbackMethod.invoke(windowManager, taskId, executor, taskFpsCallback);
            currentWatchingTaskId = taskId;
            isFpsCallBackRegistered = true;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static void unregisterFpsCallback() {
        if (!isFpsCallBackRegistered) return;
        isFpsCallBackRegistered = false;
        currentWatchingTaskId = 0;
        try {
            unregisterFpsCallbackMethod.invoke(windowManager, taskFpsCallback);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static void sendBinderToAppByStickyBroadcast(Object activityManager) {

        try {
            //生成binder
            IBinder binder = new IMyAidlInterface.Stub() {

                @Override
                public String registerFpsWatch(IFpsCallback taskFpsCallback, boolean keep) {
                    iFpsCallback = taskFpsCallback;
                    isKeepWatching = keep;

                    if (isFpsCallBackRegistered) {
                        unregisterFpsCallback();
                    }

                    try {
                        Object rootTaskInfo = getFocusedRootTaskInfoMethod.invoke(iActivityTaskManager);
                        if (rootTaskInfo == null) return null;

                        Field taskIdField = rootTaskInfo.getClass().getField("taskId");
                        taskIdField.setAccessible(true);
                        Field topActivityField = rootTaskInfo.getClass().getField("topActivity");
                        ComponentName topActivity = (ComponentName) topActivityField.get(rootTaskInfo);

                        if (topActivity == null) return null;

                        currentWatchingPackageName = topActivity.getPackageName();
                        currentWatchingClassName = topActivity.getClassName();
                        currentWatchingTaskId = taskIdField.getInt(rootTaskInfo);
                        registerFpsCallback(currentWatchingTaskId);
                        return currentWatchingPackageName;
                    } catch (IllegalAccessException | InvocationTargetException | NoSuchFieldException e) {
                        return null;
                    }
                }

                @Override
                public void unregisterFpsWatch(IFpsCallback taskFpsCallback) {
                    iFpsCallback = null;
                    unregisterFpsCallback();
                }

                @Override
                public void exit() {
                    exitFPSWatch();
                }
            };
            //把binder填到一个可以用Intent来传递的容器中
            BinderContainer binderContainer = new BinderContainer(binder);
            // 创建 Intent 对象，并将binder作为附加参数
            intent = new Intent(SEND_BINDER_ACTION);
            intent.putExtra("binder", binderContainer);

            // 获取 broadcastIntent 方法
            Method broadcastIntentMethod = Class.forName("android.app.IActivityManager").getDeclaredMethod(
                    "broadcastIntent",
                    Class.forName("android.app.IApplicationThread"),
                    Intent.class,
                    String.class,
                    Class.forName("android.content.IIntentReceiver"),
                    int.class,
                    String.class,
                    Bundle.class,
                    String[].class,
                    int.class,
                    Bundle.class,
                    boolean.class,
                    boolean.class,
                    int.class
            );

            // 调用 broadcastIntent 方法，发送粘性广播
            broadcastIntentMethod.invoke(activityManager, null, intent, null, null, -1, null, null, null, 0, null, false, true, -1);

            // 安卓15无法立即收到粘性广播，因而补发一条普通广播

            context.sendBroadcast(intent);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void removeStickyBroadcast() {
        if (intent == null || iActivityManager == null) return;

        try {
            // 获取 unbroadcastIntent 方法
            Method unbroadcastIntentMethod = Class.forName("android.app.IActivityManager").getDeclaredMethod("unbroadcastIntent", Class.forName("android.app.IApplicationThread"), Intent.class, int.class);
            unbroadcastIntentMethod.invoke(iActivityManager, null, intent, -1);
            intent = null;
            iActivityManager = null;
        } catch (Exception e) {
        }
    }

}
