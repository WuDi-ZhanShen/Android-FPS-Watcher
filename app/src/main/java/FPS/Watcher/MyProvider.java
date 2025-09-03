package FPS.Watcher;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class MyProvider extends ContentProvider {

    private static final String TAG = MyProvider.class.getSimpleName();
    public static final String EXTRA_BINDER = "FPS.Watcher.intent.extra.BINDER";
    public static final String ACTION_BINDER_RECEIVED = "FPS.Watcher.action.BINDER_RECEIVED";
    public static IBinder binder = null;
    public static IMyAidlInterface myAidlInterface = null;
    public static final String METHOD_SEND_BINDER = "sendBinder";
    public static final String METHOD_GET_BINDER = "getBinder";

    @Override
    public Bundle call(String method, String arg, Bundle extras) {

        if (extras == null) {
            return null;
        }

        extras.setClassLoader(BinderContainer.class.getClassLoader());

        Bundle reply = new Bundle();
        switch (method) {
            case METHOD_SEND_BINDER: {
                sendBinder(extras);
                break;
            }
            case METHOD_GET_BINDER: {
                if (!getBinder(reply)) {
                    return null;
                }
                break;
            }
        }
        return reply;
    }

    private boolean getBinder(Bundle reply) {
        if (binder == null || !binder.pingBinder())
            return false;

        reply.putParcelable(EXTRA_BINDER, new BinderContainer(binder));
        return true;
    }

    private void sendBinder(Bundle extras) {
        if (binder != null && binder.pingBinder()) {
            Log.d(TAG, "sendBinder is called when already a living binder");
            return;
        }

        BinderContainer container = extras.getParcelable(EXTRA_BINDER, BinderContainer.class);
        if (container == null || container.binder == null) {
            return;
        }

        Log.d(TAG, "binder received");

        binder = container.binder;
        myAidlInterface = IMyAidlInterface.Stub.asInterface(binder);

        getContext().sendBroadcast(new Intent(ACTION_BINDER_RECEIVED).setPackage(getContext().getPackageName()));
    }






    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }


}
