// IMyAidlInterface.aidl
package FPS.Watcher;

// Declare any non-default types here with import statements

interface IMyAidlInterface {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
//    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
//            double aDouble, String aString);

    String registerFpsWatch(in FPS.Watcher.IFpsCallback taskFpsCallback, boolean keep) = 0;

    void unregisterFpsWatch(in FPS.Watcher.IFpsCallback taskFpsCallback) = 1;

    void collapseNotificationPanels() = 2;

    void exit() = 255;

}