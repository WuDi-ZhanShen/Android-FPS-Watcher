package android.app;

import android.content.ComponentName;
import android.os.RemoteException;

public abstract class TaskStackListener extends ITaskStackListener.Stub {
    private boolean mIsRemote = true;

    /** Indicates that this listener lives in system server. */
    public void setIsLocal() {
        mIsRemote = false;
    }

    //    @Override
    public void onTaskStackChanged() throws RemoteException {
    }

    //    @Override
    public void onActivityPinned(String packageName, int userId, int taskId, int rootTaskId)
            throws RemoteException {
    }

    //    @Override
    public void onActivityUnpinned() throws RemoteException {
    }

    //    @Override
    public void onActivityRestartAttempt(ActivityManager.RunningTaskInfo task, boolean homeTaskVisible,
                                         boolean clearedTask, boolean wasVisible) throws RemoteException {
    }

    //    @Override
    public void onActivityForcedResizable(String packageName, int taskId, int reason)
            throws RemoteException {
    }

    //    @Override
    public void onActivityDismissingDockedTask() throws RemoteException {
    }

    //    @Override
    public void onActivityLaunchOnSecondaryDisplayFailed(ActivityManager.RunningTaskInfo taskInfo,
                                                         int requestedDisplayId) throws RemoteException {
        onActivityLaunchOnSecondaryDisplayFailed();
    }


    @Deprecated
    public void onActivityLaunchOnSecondaryDisplayFailed() throws RemoteException {
    }

    //    @Override
    public void onActivityLaunchOnSecondaryDisplayRerouted(ActivityManager.RunningTaskInfo taskInfo,
                                                           int requestedDisplayId) throws RemoteException {
    }

    //    @Override
    public void onTaskCreated(int taskId, ComponentName componentName) throws RemoteException {
    }

    //    @Override
    public void onTaskRemoved(int taskId) throws RemoteException {
    }

    //    @Override
    public void onTaskMovedToFront(ActivityManager.RunningTaskInfo taskInfo)
            throws RemoteException {
        onTaskMovedToFront(taskInfo.taskId);
    }


    @Deprecated
    public void onTaskMovedToFront(int taskId) throws RemoteException {
    }

    //    @Override
    public void onTaskRemovalStarted(ActivityManager.RunningTaskInfo taskInfo)
            throws RemoteException {
        onTaskRemovalStarted(taskInfo.taskId);
    }


    @Deprecated
    public void onTaskRemovalStarted(int taskId) throws RemoteException {
    }

    //    @Override
    public void onTaskDescriptionChanged(ActivityManager.RunningTaskInfo taskInfo)
            throws RemoteException {
        onTaskDescriptionChanged(taskInfo.taskId, taskInfo.taskDescription);
    }


    @Deprecated
    public void onTaskDescriptionChanged(int taskId, ActivityManager.TaskDescription td)
            throws RemoteException {
    }

    //    @Override
    public void onActivityRequestedOrientationChanged(int taskId, int requestedOrientation)
            throws RemoteException {
    }

    //    @Override
    public void onTaskProfileLocked(ActivityManager.RunningTaskInfo taskInfo) throws RemoteException {
    }

////    @Override
//    public void onTaskSnapshotChanged(int taskId, TaskSnapshot snapshot) throws RemoteException {
//        if (mIsRemote && snapshot != null && snapshot.getHardwareBuffer() != null) {
//            // Preemptively clear any reference to the buffer
//            snapshot.getHardwareBuffer().close();
//        }
//    }

    //    @Override
    public void onBackPressedOnTaskRoot(ActivityManager.RunningTaskInfo taskInfo)
            throws RemoteException {
    }

    //    @Override
    public void onTaskDisplayChanged(int taskId, int newDisplayId) throws RemoteException {
    }

    //    @Override
    public void onRecentTaskListUpdated() throws RemoteException {
    }

    //    @Override
    public void onRecentTaskListFrozenChanged(boolean frozen) {
    }

    //    @Override
    public void onTaskFocusChanged(int taskId, boolean focused) {
    }

    //    @Override
    public void onTaskRequestedOrientationChanged(int taskId, int requestedOrientation) {
    }

    //    @Override
    public void onActivityRotation(int displayId) {
    }

    //    @Override
    public void onTaskMovedToBack(ActivityManager.RunningTaskInfo taskInfo) {
    }

    //    @Override
    public void onLockTaskModeChanged(int mode) {
    }

}
