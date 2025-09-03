package android.app;

// Declare any non-default types here with import statements
import android.app.IApplicationThread;
import android.content.IIntentReceiver;
import android.app.ContentProviderHolder;

interface IActivityManager {

   int broadcastIntent(in android.app.IApplicationThread caller, in Intent intent,
            in String resolvedType, in android.content.IIntentReceiver resultTo, int resultCode,
            in String resultData, in Bundle map, in String[] requiredPermissions,
            int appOp, in Bundle options, boolean serialized, boolean sticky, int userId);

   void unbroadcastIntent(in IApplicationThread caller, in Intent intent, int userId);
   ContentProviderHolder getContentProviderExternal(in String name, int userId, in IBinder token, String tag);
   void removeContentProviderExternalAsUser(in String name, in IBinder token, int userId);
}