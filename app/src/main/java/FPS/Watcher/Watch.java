package FPS.Watcher;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.ActivityThread;
import android.app.IActivityManager;
import android.app.IActivityTaskManager;
import android.app.ITaskStackListener;
import android.app.TaskStackListener;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.os.Looper;
import android.os.ServiceManager;
import android.view.IWindowManager;
import android.window.ITaskFpsCallback;

import com.android.internal.statusbar.IStatusBarService;

import java.lang.reflect.Field;


public class Watch {
    public static final String SEND_BINDER_ACTION = "intent.FPSWatch.SendBinder";
    public static int currentWatchingTaskId = 0;
    public static boolean isFpsCallBackRegistered = false;
    public static String currentWatchingPackageName = "";
    public static String currentWatchingClassName = "";
    public static boolean isKeepWatching = false;
    public static IFpsCallback iFpsCallback = null;
    static public Intent intent = null;
    public static IActivityTaskManager iActivityTaskManager = null;
    static public IActivityManager iActivityManager = null;
    static public IWindowManager iWindowManager = null;
    static public IStatusBarService iStatusBarManager = null;
    public static IBinder iMyAidlInterface = new IMyAidlInterface.Stub() {

        @Override
        public String registerFpsWatch(IFpsCallback taskFpsCallback, boolean keep) {
            iFpsCallback = taskFpsCallback;
            isKeepWatching = keep;

            if (isFpsCallBackRegistered) {
                unregisterFpsCallback();
            }

            try {
                iStatusBarManager.collapsePanels();
                ActivityTaskManager.RootTaskInfo rootTaskInfo = iActivityTaskManager.getFocusedRootTaskInfo();
                if (rootTaskInfo == null) return null;

                ComponentName topActivity = rootTaskInfo.topActivity;

                if (topActivity == null) return null;

                currentWatchingPackageName = topActivity.getPackageName();
                currentWatchingClassName = topActivity.getClassName();
                currentWatchingTaskId = rootTaskInfo.taskId;
                registerFpsCallback(currentWatchingTaskId);
                return currentWatchingPackageName;
            } catch (Throwable e) {
                e.printStackTrace();
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
    public static ITaskFpsCallback taskFpsCallback = new ITaskFpsCallback.Stub() {
        @Override
        public void onFpsReported(float fps) {
            if (iFpsCallback != null) {
                try {
                    iFpsCallback.onFpsReported(fps);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    };
    public static ITaskStackListener taskStackListener = new TaskStackListener() {
        @Override
        public void onTaskRemovalStarted(ActivityManager.RunningTaskInfo taskInfo) {
            // 正常APP内退出会调用，划卡退出不会调用
            if (taskInfo.taskId == currentWatchingTaskId) {

                try {
                    iWindowManager.unregisterTaskFpsCallback(taskFpsCallback);
                    if (!isKeepWatching) iFpsCallback.onTargetTaskRemoved();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                currentWatchingTaskId = 0;
            }
        }

        @Override
        public void onTaskRemoved(int taskId) {
            // 正常APP内退出会调用，划卡退出也会调用

            if (taskId == currentWatchingTaskId) {
                try {
                    iWindowManager.unregisterTaskFpsCallback(taskFpsCallback);
                    if (!isKeepWatching) iFpsCallback.onTargetTaskRemoved();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                currentWatchingTaskId = 0;
            }
        }

        @Override
        public void onTaskMovedToFront(ActivityManager.RunningTaskInfo taskInfo) {
            // 用于恢复FPS监测

            ComponentName componentName = taskInfo.topActivity; // 或 taskInfo.baseActivity
            if (componentName == null) return;
            String packageName = componentName.getPackageName();

            boolean isServiceRunning = iFpsCallback != null && iFpsCallback.asBinder().isBinderAlive();

            if (isServiceRunning) {
                if (isKeepWatching && taskInfo.taskId != currentWatchingTaskId) {
                    unregisterFpsCallback();
                    registerFpsCallback(taskInfo.taskId);
                    try {
                        iFpsCallback.onTargetTaskChanged(packageName);
                    } catch (Throwable e) {
                        iFpsCallback = null;
                        e.printStackTrace();
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

    public static void main(String[] arguments) {

        if (Looper.getMainLooper() == null) {
            Looper.prepareMainLooper();
        }

        int uid = android.os.Process.myUid();
        if (uid != 0 && uid != 2000) {
            System.err.printf("Insufficient permission! Need to be launched by adb (uid 2000) or root (uid 0), but your uid is %d \n", uid);
            System.exit(-1);
            return;
        }

        iActivityTaskManager = IActivityTaskManager.Stub.asInterface(ServiceManager.getService("activity_task"));
        iActivityManager = IActivityManager.Stub.asInterface(ServiceManager.getService("activity"));
        iWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        iStatusBarManager = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));

        sendBinderToAppByStickyBroadcast();
        try {
            iActivityTaskManager.registerTaskStackListener(taskStackListener);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        Runtime.getRuntime().addShutdownHook(new Thread(Watch::exitFPSWatch));
        Looper.loop();
        exitFPSWatch();
    }

    private static void exitFPSWatch() {
        unregisterFpsCallback();
        try {
            iActivityTaskManager.unregisterTaskStackListener(taskStackListener);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        removeStickyBroadcast();
    }
    private static void sendBinderToAppByStickyBroadcast() {

        try {

            //把binder填到一个可以用Intent来传递的容器中
            BinderContainer binderContainer = new BinderContainer(iMyAidlInterface);
            // 创建 Intent 对象，并将binder作为附加参数
            intent = new Intent(SEND_BINDER_ACTION);
            intent.putExtra("binder", binderContainer);

            // 调用 broadcastIntent 方法，发送粘性广播
            iActivityManager.broadcastIntent(null, intent, null, null, -1, null, null, null, 0, null, false, true, -1);


            // 安卓15无法立即收到粘性广播，因而补发一条普通广播
            ActivityThread activityThread = new ActivityThread();

            // ActivityThread.sCurrentActivityThread = activityThread;
            Field sCurrentActivityThreadField = ActivityThread.class.getDeclaredField("sCurrentActivityThread");
            sCurrentActivityThreadField.setAccessible(true);
            sCurrentActivityThreadField.set(null, activityThread);

            // activityThread.mSystemThread = true;
            Field mSystemThreadField = ActivityThread.class.getDeclaredField("mSystemThread");
            mSystemThreadField.setAccessible(true);
            mSystemThreadField.setBoolean(activityThread, true);

            activityThread.getSystemContext().sendBroadcast(intent);

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    private static void removeStickyBroadcast() {
        if (intent == null || iActivityManager == null) return;

        try {
            iActivityManager.unbroadcastIntent(null, intent, -1);
            intent = null;
            iActivityManager = null;
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    public static void registerFpsCallback(int taskId) {
        if (isFpsCallBackRegistered) return;
        try {
            iWindowManager.registerTaskFpsCallback(taskId, taskFpsCallback);
            currentWatchingTaskId = taskId;
            isFpsCallBackRegistered = true;
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    public static void unregisterFpsCallback() {
        if (!isFpsCallBackRegistered) return;
        isFpsCallBackRegistered = false;
        currentWatchingTaskId = 0;
        try {
            iWindowManager.unregisterTaskFpsCallback(taskFpsCallback);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
