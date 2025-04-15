package FPS.Watcher;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import java.util.List;

public class StartWatchTileService extends TileService {

    @Override
    public void onTileAdded() {
        Tile tile = getQsTile();
        if (tile == null) return;
        tile.setState(isFPSWatchServiceRunning(this) ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
        super.onTileAdded();
    }

    @Override
    public void onStartListening() {
        Tile tile = getQsTile();
        if (tile == null) return;
        tile.setState(isFPSWatchServiceRunning(this) ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
        super.onStartListening();
    }

    @Override
    public void onClick() {
        Tile tile = getQsTile();
        if (tile == null) return;
        if (tile.getState() == Tile.STATE_ACTIVE) {
            stopService(new Intent(this, FPSWatchService.class));
            tile.setState(Tile.STATE_INACTIVE);
        } else {
            startForegroundService(new Intent(this, FPSWatchService.class));
            tile.setState(Tile.STATE_ACTIVE);
        }
        tile.updateTile();
//        startActivityAndCollapse(new Intent(this,EmptyActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));


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
}
