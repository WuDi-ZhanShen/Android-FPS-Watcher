package android.window;

public abstract class TaskFpsCallback {
    // 定义抽象方法，子类需实现以接收FPS报告
    public abstract void onFpsReported(float fps);
}
