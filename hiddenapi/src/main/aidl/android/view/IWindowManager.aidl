package android.view;

import android.window.ITaskFpsCallback;

interface IWindowManager {

   void registerTaskFpsCallback(in int taskId, in ITaskFpsCallback callback);
    void unregisterTaskFpsCallback(in ITaskFpsCallback listener);
}