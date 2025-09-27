package FPS.Watcher;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;

import android.os.Bundle;

public class StartWatchActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        setTheme(android.R.style.Theme_NoDisplay);

        // 如果已经激活，Watch会在此Activity启动时发送Binder
        super.onCreate(savedInstanceState);
        if (getSharedPreferences("s", 0).getBoolean("enable_notification", true)) {
            if (!((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).areNotificationsEnabled())
                return;
            startForegroundService(new Intent(this, FPSWatchService.class));
        } else
            startService(new Intent(this, FPSWatchService.class));
    }


    @Override
    protected void onResume() {
        finish();
        super.onResume();
    }
}
