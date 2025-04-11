package FPS.Watcher;

import android.app.Activity;
import android.os.Bundle;

public class EmptyActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        finish();
    }
}
