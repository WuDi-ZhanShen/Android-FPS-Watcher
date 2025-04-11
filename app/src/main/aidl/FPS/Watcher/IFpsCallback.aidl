// IFpsCallback.aidl
package FPS.Watcher;

// Declare any non-default types here with import statements

interface IFpsCallback {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void onFpsReported(float fps);

    void onTargetTaskRemoved();

    void onTargetTaskChanged(String packageName);
}