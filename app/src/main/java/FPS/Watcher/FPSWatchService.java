package FPS.Watcher;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

//import com.github.mikephil.charting.charts.LineChart;
//import com.github.mikephil.charting.components.AxisBase;
//import com.github.mikephil.charting.components.LimitLine;
//import com.github.mikephil.charting.components.XAxis;
//import com.github.mikephil.charting.components.YAxis;
//import com.github.mikephil.charting.data.Entry;
//import com.github.mikephil.charting.data.LineData;
//import com.github.mikephil.charting.data.LineDataSet;
//import com.github.mikephil.charting.formatter.ValueFormatter;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FPSWatchService extends Service {
    public static final String EXIT_ACTION = "intent.FPSWatch.Exit";
    public static final String START_RECORD_ACTION = "intent.FPSWatch.StartRecord";
    private LruCache<String, Drawable> appIconCache;
    private long mRegisterTime;
    private boolean isRecording = false;
    private boolean isKeepWatching = true, notifyForeAppChange = true;
    private String mWatchingPackageName = null;

    private final BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case MyProvider.ACTION_BINDER_RECEIVED:
                    startFPSWatch(context);
                    break;
                case EXIT_ACTION:
                    saveRecordAndStopSelf();
                    break;

                case START_RECORD_ACTION:
                    if (!isRecording) {
                        isRecording = true;
                        mRegisterTime = System.currentTimeMillis();
//                        if (lineData != null) lineData.notifyDataChanged();
//                        lineDataSet.notifyDataSetChanged();
                        try {
                            PackageManager pm = context.getPackageManager();
                            ApplicationInfo appInfo = pm.getApplicationInfo(mWatchingPackageName, 0);
                            String label = pm.getApplicationLabel(appInfo).toString();
                            mRegisterTime = System.currentTimeMillis();
                            notification.setContentTitle(getString(R.string.record_target) + label);
                            notificationManager.notify(1, notification.build());
                        } catch (Throwable ignored) {
                        }
                        Toast.makeText(context, R.string.toast_start_record, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, R.string.toast_recording, Toast.LENGTH_SHORT).show();
                    }
                    try {
                        MyProvider.myAidlInterface.collapseNotificationPanels();
                    } catch (Throwable ignored) {
                    }

                    break;
            }
        }
    };

    private void startFPSWatch(Context context) {
        try {
            mWatchingPackageName = MyProvider.myAidlInterface.registerFpsWatch(iFpsCallback, isKeepWatching);
            PackageManager pm = context.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(mWatchingPackageName, 0);
            String label = pm.getApplicationLabel(appInfo).toString();

            if (notifyForeAppChange) {
                if (appIconView != null) {
                    appIconView.setImageDrawable(getAppIcon(mWatchingPackageName));
                    animateTargetChange();
                }
                Toast.makeText(FPSWatchService.this, getString(R.string.start_watch_toast) + label, Toast.LENGTH_SHORT).show();
            }

            mRegisterTime = System.currentTimeMillis();
            if (notification != null) {
                notification.setContentTitle(getString(isRecording ? R.string.record_target : R.string.watch_target) + label);
                notificationManager.notify(1, notification.build());
            }
            MyProvider.myAidlInterface.collapseNotificationPanels();
        } catch (Throwable ignored) {
        }
    }

    private SharedPreferences sp;
    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (textView == null) return;
            int size, height;
            switch (key) {
                case "tran":
                    params.alpha = sharedPreferences.getInt(key, 90) * 0.01f;
                    windowManager.updateViewLayout(frameLayout, params);
                    break;
                case "size":
                    size = (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sharedPreferences.getInt(key, 40), getResources().getDisplayMetrics()));
                    height = sharedPreferences.getBoolean("aspect_change", true) ? (size * 2 / 3) : size;
                    params.width = size;
                    params.height = height;
                    windowManager.updateViewLayout(frameLayout, params);
                    break;
                case "corner":
                    float density = getResources().getDisplayMetrics().density;
                    int ROUND_CORNER = sharedPreferences.getInt(key, 5);
                    ShapeDrawable oval = new ShapeDrawable(new RoundRectShape(new float[]{ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density}, null, null));
                    oval.getPaint().setColor(getColor(R.color.colorAccent));
                    textView.setBackground(oval);
                    windowManager.updateViewLayout(frameLayout, params);
                    break;
                case "text_size":
                    textView.setTextSize(sharedPreferences.getInt(key, 16));
                    windowManager.updateViewLayout(frameLayout, params);
                    break;
                case "aspect_change":
                    size = (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sharedPreferences.getInt("size", 40), getResources().getDisplayMetrics()));
                    height = sharedPreferences.getBoolean(key, true) ? (size * 2 / 3) : size;
                    params.width = size;
                    params.height = height;
                    windowManager.updateViewLayout(frameLayout, params);
                    break;
            }
        }
    };
    private WindowManager windowManager;
    private WindowManager.LayoutParams params;
    private TextView textView;
    private ImageView appIconView;
    private LinearLayout toolsContainer;
    private ImageView tool1, tool2, tool3, appIcon;
    private FrameLayout frameLayout;
    private LineChartView lineChart;
    //    private LineDataSet lineDataSet;
//    private LineData lineData;
//    private float sum = 0f;
//    private float count = 0;
//    private boolean isYAxisMaxSet;
    private int SCREEN_WIDTH, SCREEN_HEIGHT;
    private Notification.Builder notification = null;
    private NotificationManager notificationManager = null;
    private Bitmap bitmap;
    private Canvas canvas;
    private TextPaint textPaint1, textPaint2, textPaint3;
    private final IFpsCallback iFpsCallback = new IFpsCallback.Stub() {
        @Override
        public void onFpsReported(float fps) {
            int roundFps = Math.round(fps);
            if (textView != null) {
                textView.post(() -> textView.setText(String.format(Locale.getDefault(), "%d", roundFps)));
            }
            if (notification != null) {
                notification.setContentText(getString(R.string.current_fps) + roundFps);
                drawFpsTextOToIcon(roundFps);
                notification.setSmallIcon(Icon.createWithBitmap(bitmap));
                notificationManager.notify(1, notification.build());
            }
            if (isRecording) lineChart.addPoint(System.currentTimeMillis() - mRegisterTime, fps);
        }

        @Override
        public void onTargetTaskRemoved() {
            sendBroadcast(new Intent(EXIT_ACTION));
        }

        @Override
        public void onTargetTaskChanged(String packageName) {
            mWatchingPackageName = packageName;
            try {
                PackageManager pm = getPackageManager();
                ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                String label = pm.getApplicationLabel(appInfo).toString();
                if (notification != null) {
                    notification.setContentTitle(getString(isRecording ? R.string.record_target : R.string.watch_target) + label);
                    notificationManager.notify(1, notification.build());
                }
                if (notifyForeAppChange) {
                    new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(FPSWatchService.this, getString(R.string.switch_watch_toast) + label, Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                if (notifyForeAppChange) {
                    new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(FPSWatchService.this, getString(R.string.switch_watch_toast) + packageName, Toast.LENGTH_SHORT).show());
                }
            }
            if (notifyForeAppChange) {
                if (textView != null) {
                    textView.post(() -> {
                        appIconView.setImageDrawable(getAppIcon(packageName));
                        animateTargetChange();
                    });
                }
            }

        }
    };


    public void initAppIconCache() {
        int cacheMaxSize = (int) (Runtime.getRuntime().maxMemory() / 16);
        appIconCache = new LruCache<>(cacheMaxSize) {
            @Override
            protected int sizeOf(String key, Drawable value) {
                // 根据Drawable的大小计算缓存占用
                return value.getIntrinsicWidth() * value.getIntrinsicHeight();
            }
        };
    }

    public Drawable getAppIcon(String packageName) {
        Drawable drawable = appIconCache.get(packageName);
        if (drawable == null) {
            PackageManager packageManager = getPackageManager();
            try {
                ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
                drawable = applicationInfo.loadIcon(packageManager);
                appIconCache.put(packageName, drawable);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        return drawable;
    }

    public void animateTargetChange() {
        if (textView.getVisibility() == View.GONE) return;
        appIconView.setVisibility(View.VISIBLE);
        Animation a = appIconView.getAnimation();
        if (a != null) {
            a.cancel();
        }
        textView.setVisibility(View.INVISIBLE);
        // 创建水平移动动画
//        ObjectAnimator translationXAnimator = ObjectAnimator.ofFloat(
//                animatedView, "translationX", 0f, 0f
//        );

        // 创建缩放动画（大小变大）
        ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(
                appIconView, "scaleX", 0.8f, 1f
        );
        ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(
                appIconView, "scaleY", 0.8f, 1f
        );

        // 创建透明度提升动画
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(
                appIconView, "alpha", 0.6f, 1f
        );

        // 将所有动画组合在一起
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(
//                translationXAnimator,
                scaleXAnimator,
                scaleYAnimator,
                alphaAnimator
        );

        // 设置动画时长和插值器
        animatorSet.setDuration(600);
        animatorSet.setInterpolator(new DecelerateInterpolator());

        // 创建透明度提升动画
        ObjectAnimator alphaAnimator2 = ObjectAnimator.ofFloat(
                textView, "alpha", 0.8f, 1f
        );
        alphaAnimator2.setDuration(300);

        // 开始动画
        animatorSet.start();
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                appIconView.setVisibility(View.GONE);
                textView.setVisibility(View.VISIBLE);
                alphaAnimator2.start();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                appIconView.setVisibility(View.GONE);
                textView.setVisibility(View.VISIBLE);
                alphaAnimator2.start();
            }
        });
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void initNotificationIconDraw() {
        bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);

        textPaint1 = new TextPaint();
        textPaint1.setAntiAlias(true);
        textPaint1.setTextSize(40);
        textPaint1.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint1.setTextAlign(Paint.Align.CENTER);
        textPaint1.setStyle(Paint.Style.FILL);
        textPaint1.setFakeBoldText(true);
        textPaint1.setSubpixelText(true);
        textPaint1.setLetterSpacing(0);

        textPaint2 = new TextPaint();
        textPaint2.setAntiAlias(true);
        textPaint2.setTextSize(27.0f);
        textPaint2.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint2.setTextAlign(Paint.Align.CENTER);
        textPaint2.setStyle(Paint.Style.FILL);
        textPaint2.setFakeBoldText(true);
        textPaint2.setSubpixelText(true);

        textPaint3 = new TextPaint();
        textPaint3.setAntiAlias(true);
        textPaint3.setTextSize(50);
        textPaint3.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint3.setTextAlign(Paint.Align.CENTER);
        textPaint3.setStyle(Paint.Style.FILL);
        textPaint3.setFakeBoldText(true);
        textPaint3.setSubpixelText(true);
        textPaint3.setLetterSpacing(0);

    }

    public void drawFpsTextOToIcon(int fps) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        String fo1 = String.valueOf(fps);

        //大于999则使用K为单位，使用大号字体显示
        if (fps > 999) {
            String fo2 = String.format(Locale.getDefault(), "%.1f", fps / 1000f);
            canvas.drawText(fo2, 31f, 40f, textPaint3);
            canvas.drawText("K", 31f, 64f, textPaint2);
            //大于99则说明是三位数字
        } else if (fps > 99) {
            canvas.drawText(fo1, 31f, 48f, textPaint1);
            //小于等于99则是两位数字，需要使用大号字体显示
        } else {
            canvas.drawText(fo1, 31f, 50f, textPaint3);
        }
    }

    void GetWidthHeight() {
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        SCREEN_WIDTH = metrics.widthPixels;
        SCREEN_HEIGHT = metrics.heightPixels;
    }


    @Override
    public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager) getSystemService(Service.WINDOW_SERVICE);

        mLastConfig = new Configuration(getResources().getConfiguration());
        sp = getSharedPreferences("s", MODE_PRIVATE);
        sp.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

        isKeepWatching = sp.getBoolean("is_keep", true);
        notifyForeAppChange = sp.getBoolean("notify_fore_change", false);


        GetWidthHeight();

//        lineDataSet = new LineDataSet(null, "FPS");
//        lineDataSet.setColor(getColor(R.color.right));
//        lineDataSet.setDrawValues(false);
//        lineDataSet.setDrawCircles(false);
//        lineDataSet.setLineWidth(2f);
//        lineDataSet.setDrawFilled(true);//填充底部颜色
//        lineDataSet.setFillColor(getColor(R.color.bg));

        if (Settings.canDrawOverlays(this) && sp.getBoolean("enable_float_window", true)) {
            initAppIconCache();
            initFloatWindow();
        }


        if (((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).areNotificationsEnabled() && sp.getBoolean("enable_notification", true)) {
            initNotification();
            initNotificationIconDraw();
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(MyProvider.ACTION_BINDER_RECEIVED);
        filter.addAction(EXIT_ACTION);
        filter.addAction(START_RECORD_ACTION);

        registerReceiver(myReceiver, filter, Context.RECEIVER_EXPORTED);

        if (MyProvider.binder != null && MyProvider.binder.pingBinder()) {
            startFPSWatch(this);
        } else {
            Toast.makeText(this, R.string.activate_first, Toast.LENGTH_SHORT).show();
        }
    }

    private void initNotification() {
        PendingIntent exitIntent = PendingIntent.getBroadcast(
                this,
                0,
                new Intent(EXIT_ACTION),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        PendingIntent startRecordIntent = PendingIntent.getBroadcast(
                this,
                0,
                new Intent(START_RECORD_ACTION),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );


        notification = new Notification.Builder(this)
                .setContentTitle(getString(R.string.notification_title))
                .setColor(getColor(R.color.colorAccent))
                .setSmallIcon(Icon.createWithResource(this, R.drawable.ic_tile))
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE))
                .setChannelId("watch")
                .addAction(R.drawable.exit, getString(R.string.notification_exit), exitIntent)
                .addAction(R.drawable.exit, getString(R.string.notification_record), startRecordIntent)
                .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);


        NotificationChannel notificationChannel = new NotificationChannel("watch", getString(R.string.noti_channel_name), NotificationManager.IMPORTANCE_DEFAULT);
        notificationChannel.enableLights(false);
        notificationChannel.setShowBadge(false);
        notificationChannel.setImportance(NotificationManager.IMPORTANCE_MIN);
        notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(notificationChannel);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(1, notification.build());
        }
    }

    private void initFloatWindow() {
        int size = (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sp.getInt("size", 40), getResources().getDisplayMetrics()));

        int height = sp.getBoolean("aspect_change", true) ? (size * 2 / 3) : size;
        params = new WindowManager.LayoutParams(size, height, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, PixelFormat.RGBA_8888);
        params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;


        params.alpha = sp.getInt("tran", 90) * 0.01f;
        params.x = 100;
        params.y = 100;
        params.gravity = Gravity.START | Gravity.TOP;
        textView = new TextView(this);
        textView.setTextColor(getColor(R.color.right));
        textView.setTypeface(null, Typeface.BOLD);
        textView.setTextSize(sp.getInt("text_size", 16));
        textView.setText(R.string.fps);
        textView.setGravity(Gravity.CENTER);
        float density = getResources().getDisplayMetrics().density;
        int ROUND_CORNER = sp.getInt("corner", 5);
        ShapeDrawable oval = new ShapeDrawable(new RoundRectShape(new float[]{ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density}, null, null));
        oval.getPaint().setColor(getColor(R.color.colorAccent));
        textView.setBackground(oval);


        textView.setOnTouchListener(new View.OnTouchListener() {
            private float initialTouchX, initialTouchY;
            private int initialX, initialY;
            private boolean isMoved;

            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = motionEvent.getRawX();
                        initialTouchY = motionEvent.getRawY();
                        isMoved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int diffX = ((int) (motionEvent.getRawX() - initialTouchX));
                        int diffY = ((int) (motionEvent.getRawY() - initialTouchY));
                        params.x = initialX + diffX;
                        params.y = initialY + diffY;
                        windowManager.updateViewLayout(frameLayout, params);
                        if (Math.abs(diffY) > 10 || Math.abs(diffX) > 10) {
                            isMoved = true;
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        params.x = Math.min(Math.max(params.x, 0), SCREEN_WIDTH);
                        params.y = Math.min(Math.max(params.y, 0), SCREEN_HEIGHT);
//                        sharedPreferences.edit().putInt("menuX", params.x).putInt("menuY", params.y).apply();
                        windowManager.updateViewLayout(frameLayout, params);

                        if (!isMoved) {
                            params.width = WindowManager.LayoutParams.WRAP_CONTENT;
                            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                            toolsContainer.setVisibility(View.VISIBLE);
                            appIconView.setVisibility(View.GONE);
                            lineChart.setVisibility(View.GONE);
                            textView.setVisibility(View.GONE);
                            windowManager.updateViewLayout(frameLayout, params);

                        }
                        return true;
                    default:
                        return false;
                }
            }
        });

        appIconView = new ImageView(this);
        appIconView.setVisibility(View.GONE);

        toolsContainer = new LinearLayout(this);
        toolsContainer.setGravity(LinearLayout.HORIZONTAL);
        ShapeDrawable oval2 = new ShapeDrawable(new RoundRectShape(new float[]{ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density}, null, null));
        oval2.getPaint().setColor(getColor(R.color.colorAccent));
        toolsContainer.setBackground(oval2);
        toolsContainer.setVisibility(View.GONE);

        ShapeDrawable shapeDrawable = new ShapeDrawable(new OvalShape());
        shapeDrawable.getPaint().setColor(getColor(R.color.colorAccent));
        tool1 = new ImageView(this);
        tool1.setBackground(shapeDrawable);
        tool1.setImageDrawable(getDrawable(R.drawable.arrow_left));
        tool1.setOnClickListener(v -> {
            toolsContainer.setVisibility(View.GONE);
            appIconView.setVisibility(View.GONE);
            lineChart.setVisibility(View.GONE);
            textView.setVisibility(View.VISIBLE);

            int origSize = (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sp.getInt("size", 40), getResources().getDisplayMetrics()));
            int origHeight = sp.getBoolean("aspect_change", true) ? (origSize * 2 / 3) : origSize;
            params.width = origSize;
            params.height = origHeight;
            windowManager.updateViewLayout(frameLayout, params);
        });
        tool2 = new ImageView(this);
        tool2.setBackground(shapeDrawable);
        tool2.setImageDrawable(getDrawable(R.drawable.chart_line));
        tool2.setOnClickListener(v -> {
            if (!isRecording) {
                isRecording = true;
                mRegisterTime = System.currentTimeMillis();
//                lineData.notifyDataChanged();
//                lineDataSet.notifyDataSetChanged();
                Toast.makeText(this, R.string.toast_start_record, Toast.LENGTH_SHORT).show();
            }

            if (lineChart.getVisibility() == View.VISIBLE) {
                toolsContainer.setVisibility(View.VISIBLE);
                appIconView.setVisibility(View.GONE);
                lineChart.setVisibility(View.GONE);
                textView.setVisibility(View.GONE);
            } else {
                toolsContainer.setVisibility(View.VISIBLE);
                appIconView.setVisibility(View.GONE);
                lineChart.setVisibility(View.VISIBLE);
                textView.setVisibility(View.GONE);
            }
        });
        tool2.setOnLongClickListener(v -> {
            if (isRecording) {
                saveFpsDataToFile();
            }
            return true;
        });
        tool3 = new ImageView(this);
        tool3.setBackground(shapeDrawable);
        tool3.setImageDrawable(getDrawable(R.drawable.exit));
        tool3.setOnClickListener(v -> saveRecordAndStopSelf());


        lineChart = new LineChartView(this, false);
        int lineChartWidth = Math.min(SCREEN_WIDTH, SCREEN_HEIGHT) * 4 / 5;
        int lineChartHeight = lineChartWidth * 2 / 3;
        LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(lineChartWidth, lineChartHeight);
        params1.topMargin = getDrawable(R.drawable.arrow_left).getIntrinsicHeight() + 2;
        lineChart.setLayoutParams(params1);
        ShapeDrawable oval3 = new ShapeDrawable(new RoundRectShape(new float[]{ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density}, null, null));
        oval3.getPaint().setColor(getColor(R.color.colorAccent));
        lineChart.setBackground(oval3);
        lineChart.setVisibility(View.GONE);
//        setupChart();


        toolsContainer.addView(tool1);
        toolsContainer.addView(tool2);
        toolsContainer.addView(tool3);

        frameLayout = new FrameLayout(this);
        frameLayout.addView(textView);
        frameLayout.addView(appIconView);
        frameLayout.addView(lineChart);
        frameLayout.addView(toolsContainer);

//        frameLayout.setOnTouchListener((view, motionEvent) -> {
//            switch(motionEvent.getAction()){
//                case MotionEvent.ACTION_OUTSIDE:
//                    toolsContainer.setVisibility(View.GONE);
//                    appIconView.setVisibility(View.GONE);
//                    lineChart.setVisibility(View.GONE);
//                    textView.setVisibility(View.VISIBLE);
//                    windowManager.updateViewLayout(frameLayout, params);
//                    return false;
//                default:
//                    return false;
//            }
//        });

        LayoutTransition transition = new LayoutTransition();
        transition.setDuration(200L);
        ObjectAnimator animator = ObjectAnimator.ofFloat(null, "scaleX", 0.0f, 1.0f);
        transition.setAnimator(2, animator);
        frameLayout.setLayoutTransition(transition);
        toolsContainer.setLayoutTransition(transition);

        windowManager.addView(frameLayout, params);
    }


//    private void setupChart() {


//        lineData = new LineData(lineDataSet);
//        lineChart.setData(lineData);
//
//        lineChart.setDoubleTapToZoomEnabled(false);
//        lineChart.getDescription().setEnabled(false);
//        lineChart.setTouchEnabled(true);
//        lineChart.setScaleEnabled(false);
//        lineChart.setPinchZoom(false);
//        lineChart.setDrawGridBackground(false);
//        lineChart.setHighlightPerTapEnabled(false);       // 禁止点击高亮
//        lineChart.setHighlightPerDragEnabled(false);      // 禁止拖拽高亮
//
//
//        lineChart.getLegend().setEnabled(false);
//        lineChart.getAxisLeft().setAxisMinimum(0f);
//        lineChart.getAxisLeft().setAxisMaximum(60f);
//        isYAxisMaxSet = true;
//
//        XAxis xAxis = lineChart.getXAxis();
//        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
//        xAxis.setGranularity(1000f);
//        xAxis.setValueFormatter(new ValueFormatter() {
//            private final SimpleDateFormat sdf = new SimpleDateFormat("m:ss", Locale.getDefault());
//
//            @Override
//            public String getAxisLabel(float value, AxisBase axis) {
//                return sdf.format(new Date((long) value));
//            }
//        });
//
//        lineChart.getXAxis().setTextColor(getColor(R.color.right));
//        lineChart.getAxisLeft().setTextColor(getColor(R.color.right));
//        lineChart.getAxisRight().setEnabled(false);
//    }


//    private void addEntry(float timestamp, float value) {
//        lineDataSet.addEntry(new Entry(timestamp, value));
//        lineData.notifyDataChanged();
//        lineChart.notifyDataSetChanged();
//        count++;
//        sum += value;
//        int average = (int) (sum / count);
//
//        if (lineChart.getVisibility() == View.VISIBLE) {
//            if (value > 60f && isYAxisMaxSet) {
//                lineChart.getAxisLeft().resetAxisMaximum();
//                isYAxisMaxSet = false;
//            }
//
//
//            // 设置最大可见区域为60秒
//            lineChart.setVisibleXRangeMaximum(60000f);
//            lineChart.moveViewToX(timestamp);
//
//            if (count > 10) {
//                YAxis leftAxis = lineChart.getAxisLeft();
//                LimitLine avgLine = new LimitLine(average, getString(R.string.average) + average);
//                int color = getColor(R.color.right);
//                int red = Color.red(color);
//                int green = Color.green(color);
//                int blue = Color.blue(color);
//
//                int colorWithAlpha = Color.argb(128, red, green, blue); // 128 = 半透明
//
//                avgLine.setLineColor(colorWithAlpha);
//                avgLine.setLineWidth(2f);
//                avgLine.setTextColor(colorWithAlpha);
//                avgLine.setTextSize(12f);
//                avgLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
//
//                leftAxis.removeAllLimitLines(); // 如果之前有其他 LimitLine
//                leftAxis.addLimitLine(avgLine);
//            }
//        }
//    }

    public static File getFPSRecordFolder(Context context) {
        return new File(context.getExternalFilesDir(null), "FPSRecord");
    }

    public static final String csvHeader1 = "time";
    public static final String csvHeader2 = "FPS";

    private void saveFpsDataToFile() {
        final SimpleDateFormat sdf2 = new SimpleDateFormat("MM月dd日HH时mm分", Locale.getDefault());

        StringBuilder sb = new StringBuilder();
        sb.append(csvHeader1).append(",").append(csvHeader2).append("\n"); // CSV header
        final SimpleDateFormat sdf = new SimpleDateFormat("m:ss.SSS", Locale.getDefault());
        int size = lineChart.dataX.size();
        for (int i = 0; i < size; i++) {
            long timestamp = lineChart.dataX.get(i);
            float value = lineChart.dataY.get(i);
            sb.append(sdf.format(timestamp)).append(",").append(value).append("\n");
        }

        try {
//            PackageManager pm = getPackageManager();
//            ApplicationInfo appInfo = pm.getApplicationInfo(mWatchingPackageName, 0);
//            String label = pm.getApplicationLabel(appInfo).toString();
            String filename = sdf2.format(System.currentTimeMillis()) + "_" + mWatchingPackageName + ".csv";
            File recordFolder = getFPSRecordFolder(this);
            if (!recordFolder.exists()) {
                recordFolder.mkdirs();
            } else if (recordFolder.isFile()) {
                recordFolder.delete();
                recordFolder.mkdirs();
            }
            File file = new File(recordFolder, filename);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(sb.toString().getBytes());
            fos.close();
            Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
        } catch (Throwable e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.save_failed) + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    void saveRecordAndStopSelf() {
        try {
            MyProvider.myAidlInterface.unregisterFpsWatch(iFpsCallback);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        if (isRecording) {
            saveFpsDataToFile();
        } else {
            Toast.makeText(FPSWatchService.this, R.string.stop_watch_toast, Toast.LENGTH_SHORT).show();
        }
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(myReceiver);
        sp.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        if (frameLayout == null) return;
        windowManager.removeViewImmediate(frameLayout);
    }


    private Configuration mLastConfig;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (frameLayout == null) return;

        if ((mLastConfig.diff(newConfig) & ActivityInfo.CONFIG_ORIENTATION) > 0) {
            GetWidthHeight();
            params.x = Math.min(Math.max(params.x, 0), SCREEN_WIDTH);
            params.y = Math.min(Math.max(params.y, 0), SCREEN_HEIGHT);
            windowManager.updateViewLayout(frameLayout, params);
        } else if ((mLastConfig.diff(newConfig) & ActivityInfo.CONFIG_UI_MODE) > 0) {
            textView.setTextColor(getColor(R.color.right));
            float density = getResources().getDisplayMetrics().density;
            int ROUND_CORNER = sp.getInt("corner", 5);
            ShapeDrawable oval = new ShapeDrawable(new RoundRectShape(new float[]{ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density}, null, null));
            oval.getPaint().setColor(getColor(R.color.colorAccent));
            textView.setBackground(oval);

            ShapeDrawable shapeDrawable = new ShapeDrawable(new OvalShape());
            shapeDrawable.getPaint().setColor(getColor(R.color.colorAccent));
            tool1.setBackground(shapeDrawable);
            tool1.setImageDrawable(getDrawable(R.drawable.arrow_left));
            tool2.setBackground(shapeDrawable);
            tool2.setImageDrawable(getDrawable(R.drawable.chart_line));
            tool3.setBackground(shapeDrawable);
            tool3.setImageDrawable(getDrawable(R.drawable.exit));
            ShapeDrawable oval2 = new ShapeDrawable(new RoundRectShape(new float[]{ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density}, null, null));
            oval2.getPaint().setColor(getColor(R.color.colorAccent));
            toolsContainer.setBackground(oval2);

            ShapeDrawable oval3 = new ShapeDrawable(new RoundRectShape(new float[]{ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density}, null, null));
            oval3.getPaint().setColor(getColor(R.color.colorAccent));
            lineChart.setBackground(oval3);

//            lineChart.getXAxis().setTextColor(getColor(R.color.right));
//            lineChart.getAxisLeft().setTextColor(getColor(R.color.right));
//
//            lineDataSet.setColor(getColor(R.color.right));
//            lineDataSet.setFillColor(getColor(R.color.bg));
        }
        mLastConfig = new Configuration(newConfig);
    }
}