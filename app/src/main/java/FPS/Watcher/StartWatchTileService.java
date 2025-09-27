package FPS.Watcher;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

import java.util.List;

public class StartWatchTileService extends TileService {

    @Override
    public void onTileAdded() {
        Tile tile = getQsTile();
        if (tile == null) return;
        tile.setState(isFPSWatchServiceRunning(this) ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
//        if (isActivated()) {
//            tile.setState(isFPSWatchServiceRunning(this) ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
//            tile.setLabel(getString(R.string.app_label));
//        } else {
//            tile.setState(Tile.STATE_UNAVAILABLE);
//            tile.setLabel(getString(R.string.longpress_to_activate));
//        }
        tile.updateTile();
        super.onTileAdded();
    }

    @Override
    public void onStartListening() {
        Tile tile = getQsTile();
        if (tile == null) return;
        tile.setState(isFPSWatchServiceRunning(this) ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
//        if (isActivated()) {
//            tile.setState(isFPSWatchServiceRunning(this) ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
//            tile.setLabel(getString(R.string.app_label));
//        } else {
//            tile.setState(Tile.STATE_UNAVAILABLE);
//            tile.setLabel(getString(R.string.longpress_to_activate));
//        }
        tile.updateTile();
        super.onStartListening();
    }

    @Override
    public void onClick() {
        Tile tile = getQsTile();
        if (tile == null) return;
        boolean isFPSWatchServiceRunning = isFPSWatchServiceRunning(this);
        tile.setState(isFPSWatchServiceRunning ? Tile.STATE_INACTIVE : Tile.STATE_ACTIVE);
        tile.updateTile();
        if (isFPSWatchServiceRunning) {
            sendBroadcast(new Intent(FPSWatchService.EXIT_ACTION));
            tile.setState(Tile.STATE_INACTIVE);
        } else if (isActivated()) {
            SharedPreferences sp = getSharedPreferences("s", 0);
            if (sp.getBoolean("enable_notification", true)) {
                if (!((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).areNotificationsEnabled()) {
                    Toast.makeText(this, R.string.request_perm_notification, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                } else {
                    startForegroundService(new Intent(this, FPSWatchService.class));
                }
            } else {
                if (!Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, R.string.request_perm_overlay, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                } else {
                    startService(new Intent(this, FPSWatchService.class));
                }
            }
            tile.setState(Tile.STATE_ACTIVE);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                PendingIntent intent = PendingIntent.getActivity(this, 0, new Intent(this, StartWatchActivity.class), PendingIntent.FLAG_IMMUTABLE);
                startActivityAndCollapse(intent);
            } else {
                Intent intent = new Intent(this, StartWatchActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivityAndCollapse(intent);
            }
            tile.setState(Tile.STATE_ACTIVE);
        }

        super.onClick();
    }

    public static boolean isFPSWatchServiceRunning(Context context) {

        ActivityManager activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);

        List<ActivityManager.RunningServiceInfo> runningServices = activityManager.getRunningServices(Integer.MAX_VALUE);
        if (runningServices.isEmpty()) {
            return false;
        }

        for (ActivityManager.RunningServiceInfo serviceInfo : runningServices) {
            if (FPSWatchService.class.getName().equals(serviceInfo.service.getClassName())) {
                return true;
            }
        }
        return false;

    }

    public boolean isActivated() {
        return MyProvider.binder != null && MyProvider.binder.pingBinder();
    }
}
