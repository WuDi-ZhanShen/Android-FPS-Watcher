package FPS.Watcher;

import android.os.Build;

public class Watch {


    public static void main(String[] arguments) {


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            System.err.println("Only Support Android 12+!");
            System.exit(-1);
            return;
        }

        int uid = android.os.Process.myUid();
        if (uid != 0 && uid != 2000) {
            System.err.printf("Insufficient permission! Need to be launched by adb (uid 2000) or root (uid 0), but your uid is %d \n", uid);
            System.exit(-1);
            return;
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            Watch_A12.main(arguments);
        } else {
            Watch_A13.main(arguments);
        }

    }

}
