package android.app;

public class ActivityThread {
    private static volatile ActivityThread sCurrentActivityThread;
        boolean mSystemThread = false;

    public ActivityThread() {

    }

    public ContextImpl getSystemContext() {
        return null;
    }

    public ContextImpl getSystemUiContext() {
        return null;
    }

}
