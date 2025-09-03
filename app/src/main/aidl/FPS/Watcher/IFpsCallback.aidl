package FPS.Watcher;


interface IFpsCallback {

    void onFpsReported(float fps);

    void onTargetTaskRemoved();

    void onTargetTaskChanged(String packageName);
}