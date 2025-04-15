# Project Sturcture
Module app: Contains main logic. MainAcitvity unzips classes.dex and runs Watch.class using adb, Shizuku or root. Watch.class monitors both the foreground task and it's fps, and send them to FPSWatchService by aidl.


Module hiddenapi: Provide direct call to hidden api.


# Core API
IWindowManager.registerTaskFpsCallback(int taskId, ITaskFpsCallback callback); (Since Android 13+)


IActivityTaskManager.registerITaskStackListener(ITaskStackListener listener);


# Other API
IActivityManager.broadcastIntent();


IActivityTaskManager.getFocusedRootTaskInfo();


IStatusBarService.collapsePanels();
