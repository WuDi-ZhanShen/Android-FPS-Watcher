package FPS.Watcher;

import static FPS.Watcher.StartWatchTileService.isFPSWatchServiceRunning;

import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {
    private boolean isPermissionResultListenerRegistered = false;
    public IMyAidlInterface myAidlInterface = null;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BinderContainer binderContainer = intent.getParcelableExtra("binder");
            IBinder binder = binderContainer.getBinder();
            //如果binder已经失去活性了，则不再继续解析
            if (!binder.isBinderAlive()) return;
            myAidlInterface = IMyAidlInterface.Stub.asInterface(binder);
            Handler handler = new Handler(Looper.getMainLooper());
            // 主线程更新 UI
            handler.post(MainActivity.this::enableScreenOffFunctions);


        }
    };

    private final Shizuku.OnRequestPermissionResultListener RL = (requestCode, grantResult) -> check();


    //检查Shizuku权限，申请Shizuku权限的函数
    private void check() {

        if (!isPermissionResultListenerRegistered) {
            Shizuku.addRequestPermissionResultListener(RL);
            isPermissionResultListenerRegistered = true;
        }
        boolean b = true, c = false;
        try {
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED)
                Shizuku.requestPermission(0);
            else c = true;
        } catch (Exception e) {
            if (checkSelfPermission("moe.shizuku.manager.permission.API_V23") == PackageManager.PERMISSION_GRANTED)
                c = true;
            if (e.getClass() == IllegalStateException.class) {
                b = false;
                Toast.makeText(this, R.string.shizuku_notrun, Toast.LENGTH_SHORT).show();
            }
        }
        if (b && c) {
            try {
                Process p = Shizuku.newProcess(new String[]{"sh"}, null, null);
                OutputStream out = p.getOutputStream();
                String cmd = "cmd appops set " + getPackageName() +" RUN_IN_BACKGROUND allow;cmd appops set "+getPackageName() +" RUN_IN_BACKGROUND allow;export CLASSPATH=" + getExternalFilesDir(null).getAbsolutePath() + "/classes.dex;nohup app_process / " + Watch.class.getName() + " > /dev/null 2>&1 &";
                out.write(cmd.getBytes());
                out.flush();
                out.close();
            } catch (IOException ioException) {
                Toast.makeText(this, R.string.active_failed, Toast.LENGTH_SHORT).show();
            }
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.getAttributes().dimAmount = 0.5f;
        setContentView(R.layout.main);

        window.setNavigationBarContrastEnforced(false);
        boolean isNight = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_YES) == Configuration.UI_MODE_NIGHT_YES;
        window.setNavigationBarColor(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? Color.TRANSPARENT : getColor(isNight ? R.color.bgBlack : R.color.bgWhite));
        window.setStatusBarColor(Color.TRANSPARENT);

        SharedPreferences sp = getSharedPreferences("s", 0);
        if (sp.getBoolean("first", true)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.privacy)
                    .setMessage(R.string.privacypolicy)
                    .setNegativeButton(R.string.agree, (dialogInterface, i) -> {
                        help();
                        sp.edit().putBoolean("first", false).apply();
                    })
                    .setCancelable(false)
                    .setPositiveButton(R.string.disagree, (dialogInterface, i) -> finish())
                    .show();


        }

        setButtonsOnclick(isNight, sp);
        registerReceiver(mBroadcastReceiver, new IntentFilter(Watch.SEND_BINDER_ACTION), Context.RECEIVER_EXPORTED);

        super.onCreate(savedInstanceState);

        if (!Settings.canDrawOverlays(this)) {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
            Toast.makeText(this, R.string.float_window_permission, Toast.LENGTH_SHORT).show();
        }
    }

    private void unzipFiles() {
        try {
            // 获取当前 APK 的路径
            String apkPath = getPackageResourcePath();

            // 构建输出文件路径
            File outFile = new File(getExternalFilesDir(null), "classes.dex");


            // 打开 APK 作为 zip 文件
            ZipFile zipFile = new ZipFile(apkPath);
            ZipEntry dexEntry = zipFile.getEntry("classes.dex");

            if (dexEntry == null) {
                return;
            }

            // 创建输出流
            InputStream inputStream = zipFile.getInputStream(dexEntry);
            FileOutputStream outputStream = new FileOutputStream(outFile);

            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            inputStream.close();
            outputStream.close();
            zipFile.close();

        } catch (Exception e) {
            Toast.makeText(this, R.string.unzip_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void setButtonsOnclick(boolean isNight, SharedPreferences sp) {

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            findViewById(R.id.left).setVisibility(View.VISIBLE);
            findViewById(R.id.right).setVisibility(View.VISIBLE);
        }
        LinearLayout linearLayout = findViewById(R.id.ll);


        SeekBar sb = findViewById(R.id.sb);
        sb.setProgress(sp.getInt("size", 40));
        EditText eb = findViewById(R.id.eb);
        eb.setText(String.valueOf(sp.getInt("size", 40)));
        SeekBar sc = findViewById(R.id.sc);
        sc.setProgress(sp.getInt("tran", 90));
        EditText ec = findViewById(R.id.ec);
        ec.setText(String.valueOf(sp.getInt("tran", 90)));
        SeekBar sd = findViewById(R.id.sd);
        sd.setProgress(sp.getInt("corner", 5));
        EditText ed = findViewById(R.id.ed);
        ed.setText(String.valueOf(sp.getInt("corner", 5)));
        SeekBar se = findViewById(R.id.se);
        se.setProgress(sp.getInt("text_size", 16));
        EditText ee = findViewById(R.id.ee);
        ee.setText(String.valueOf(sp.getInt("text_size", 16)));

        Switch switch1 = findViewById(R.id.switch1);
        switch1.setChecked(!sp.getBoolean("is_keep", true));
        switch1.setOnCheckedChangeListener((buttonView, isChecked) -> sp.edit().putBoolean("is_keep", !isChecked).apply());
        TextView textView1 = findViewById(R.id.text1);
        textView1.setOnClickListener(v -> showIsKeepSwitchHelp());


        Switch switch2 = findViewById(R.id.switch2);
        switch2.setChecked(sp.getBoolean("aspect_change", false));
        switch2.setOnCheckedChangeListener((buttonView, isChecked) -> sp.edit().putBoolean("aspect_change", isChecked).apply());

        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sp.edit().putInt("size", i).apply();
                eb.setText(String.valueOf(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        eb.setOnKeyListener((view, i, keyEvent) -> {
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_DOWN && eb.getText().length() > 0) {
                int value = Integer.parseInt(eb.getText().toString());
                if (value >= 0 && value <= 100) {
                    sp.edit().putInt("size", value).apply();
                    sb.setProgress(value);
                }
            }
            return false;
        });

        sc.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sp.edit().putInt("tran", i).apply();
                ec.setText(String.valueOf(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        ec.setOnKeyListener((view, i, keyEvent) -> {
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_DOWN && ec.getText().length() > 0) {
                int value = Integer.parseInt(ec.getText().toString());
                if (value >= 0 && value <= 100) {
                    sp.edit().putInt("tran", value).apply();
                    sc.setProgress(value);
                }
            }
            return false;
        });

        sd.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sp.edit().putInt("corner", i).apply();
                ed.setText(String.valueOf(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (seekBar.getProgress() < 1) {
                    seekBar.setProgress(1);
                    Toast.makeText(MainActivity.this, R.string.toosmall, Toast.LENGTH_SHORT).show();
                }

            }
        });
        ed.setOnKeyListener((view, i, keyEvent) -> {
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_DOWN && ed.getText().length() > 0) {
                int value = Integer.parseInt(ed.getText().toString());
                if (value >= 0 && value <= 20) {
                    sp.edit().putInt("corner", value).apply();
                    sd.setProgress(value);
                }
            }
            return false;
        });

        se.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sp.edit().putInt("text_size", i).apply();
                ee.setText(String.valueOf(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (seekBar.getProgress() < 1) {
                    seekBar.setProgress(1);
                    Toast.makeText(MainActivity.this, R.string.toosmall, Toast.LENGTH_SHORT).show();
                }

            }
        });
        ee.setOnKeyListener((view, i, keyEvent) -> {
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_DOWN && ee.getText().length() > 0) {
                int value = Integer.parseInt(ee.getText().toString());
                if (value >= 0 && value <= 20) {
                    sp.edit().putInt("text_size", value).apply();
                    se.setProgress(value);
                }
            }
            return false;
        });

        findViewById(R.id.title_text).setOnClickListener(view -> help());
        float density = getResources().getDisplayMetrics().density;
        findViewById(R.id.activate_button).setOnClickListener(view -> showActivate());
        ShapeDrawable oval = new ShapeDrawable(new RoundRectShape(new float[]{30 * density, 30 * density, 30 * density, 30 * density, 0, 0, 0, 0}, null, null));
        oval.getPaint().setColor(getColor(isNight ? R.color.bgBlack : R.color.bgWhite));
        linearLayout.setBackground(oval);
        LayoutTransition transition = new LayoutTransition();
        transition.setDuration(400L);
        ObjectAnimator animator = ObjectAnimator.ofFloat(null, "scaleX", 0.0f, 1.0f);
        transition.setAnimator(2, animator);
        linearLayout.setLayoutTransition(transition);


    }

    private void showIsKeepSwitchHelp() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.single_task)
                .setMessage(R.string.keep_help_content)
                .setNegativeButton(R.string.understand, null)
                .show();
    }


    public void enableScreenOffFunctions() {
        findViewById(R.id.title_text).setOnLongClickListener(view -> {
            if (!isFPSWatchServiceRunning(this)) {
                startService(new Intent(this, MyService.class));
            } else {
                stopService(new Intent(this, MyService.class));
            }
            return true;
        });
        Button button = findViewById(R.id.activate_button);
        button.setText(getString(R.string.all_ok));
        button.setTextColor(getColor(R.color.right));
        button.setOnClickListener(v -> help());
        button.setOnLongClickListener(view -> {
            if (isFPSWatchServiceRunning(this)) {
                sendBroadcast(new Intent(MyService.EXIT_ACTION));
            }
            Toast.makeText(this, R.string.service_closed, Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(() -> {
                try {
                    myAidlInterface.exit();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                finish();
            }, 1000);

            return true;
        });
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onDestroy() {
        if (isPermissionResultListenerRegistered) Shizuku.removeRequestPermissionResultListener(RL);
        unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            findViewById(R.id.left).setVisibility(View.VISIBLE);
            findViewById(R.id.right).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.left).setVisibility(View.GONE);
            findViewById(R.id.right).setVisibility(View.GONE);
        }
        super.onConfigurationChanged(newConfig);
    }


    public void finish(View view) {
        finish();
    }

    public void help() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.help_title)
                .setMessage(R.string.help_conntent)
                .setNegativeButton(R.string.understand, null)
                .show();
    }

    public void showActivate() {
        unzipFiles();
        String cmd = "cmd appops set " + getPackageName() +" RUN_IN_BACKGROUND allow;cmd appops set "+getPackageName() +" RUN_IN_BACKGROUND allow;export CLASSPATH=" + getExternalFilesDir(null).getAbsolutePath() + "/classes.dex;nohup app_process / " + Watch.class.getName() + " > /dev/null 2>&1 &";

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                .setMessage(String.format(getString(R.string.active_steps), cmd))
                .setTitle(R.string.need_active)
                .setNeutralButton(R.string.copy_cmd, (dialogInterface, i) -> {
                    ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("c", "adb shell " + cmd));
                    Toast.makeText(MainActivity.this, String.format(getString(R.string.cmd_copy_finish), cmd), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.by_root, (dialoginterface, i) -> {
                    Process p;
                    try {
                        p = Runtime.getRuntime().exec("su");
                        DataOutputStream o = new DataOutputStream(p.getOutputStream());
                        o.writeBytes(cmd);
                        o.flush();
                        o.close();
                    } catch (IOException ignored) {
                        Toast.makeText(MainActivity.this, R.string.active_failed, Toast.LENGTH_SHORT).show();
                    }
                });
        builder.setPositiveButton(R.string.by_shizuku, (dialogInterface, i) -> check());
        builder.show();
    }

}
