# Project Sturcture
Module app: Contains main logic. MainAcitvity unzips classes.dex and runs Watch.class using adb, Shizuku or root. Watch.class monitors both the foreground task and it's fps, and send them to FPSWatchService by aidl.


Module hiddenapi: Provide direct call to hidden api.


# Core API
IWindowManager.registerTaskFpsCallback(int taskId, ITaskFpsCallback callback); (Since Android 13+) -- Monitor a task's fps.


IActivityTaskManager.registerITaskStackListener(ITaskStackListener listener); -- Monitor forground task.


# Other API
IActivityManager.broadcastIntent(); -- Send IBinder from Watch.class to FPSWatchService by Broadcast.


IActivityTaskManager.getFocusedRootTaskInfo(); -- Get forground task immediately.


IStatusBarService.collapsePanels(); -- Collapse Notification Panel.
