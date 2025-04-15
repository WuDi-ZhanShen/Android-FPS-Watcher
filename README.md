# Project Sturcture
Module app: Contains main logic.
Module hiddenapi: Provide direct call to hidden api.


# Core API
IWindowManager.registerTaskFpsCallback(int taskId, ITaskFpsCallback callback); (Since Android 13+)
IActivityTaskManager.registerITaskStackListener(ITaskStackListener listener);


# Other API
IActivityManager.broadcastIntent();
IActivityTaskManager.getFocusedRootTaskInfo();
IStatusBarService.collapsePanels();
