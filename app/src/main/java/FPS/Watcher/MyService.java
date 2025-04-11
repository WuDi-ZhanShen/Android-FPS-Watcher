package FPS.Watcher;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
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

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MyService extends Service {
    public static final String EXIT_ACTION = "intent.FPSWatch.Exit";

    IMyAidlInterface myAidlInterface = null;
    private LruCache<String, Drawable> appIconCache;

    public void initCache() {
        int cacheSize = (int) (Runtime.getRuntime().maxMemory() / 16);
        appIconCache = new LruCache<>(cacheSize) {
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

    private long mRegisterTime = System.currentTimeMillis();
    private boolean isRecording = false;

    final IFpsCallback iFpsCallback = new IFpsCallback.Stub() {
        @Override
        public void onFpsReported(float fps) {
            textView.post(() -> textView.setText(String.format(Locale.getDefault(),"%d",Math.round(fps))));
           if (isRecording) addEntry(System.currentTimeMillis() - mRegisterTime, fps);
        }

        @Override
        public void onTargetTaskRemoved() throws RemoteException {
            myAidlInterface.unregisterFpsWatch(iFpsCallback);
            stopSelf();
        }

        @Override
        public void onTargetTaskChanged(String packageName) {
            mWatchingPackageName = packageName;
            textView.post(() -> {
                try {
                    PackageManager pm = getPackageManager();
                    ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                    String label = pm.getApplicationLabel(appInfo).toString();
                    Toast.makeText(textView.getContext(), getString(R.string.switch_watch_toast) + label, Toast.LENGTH_SHORT).show();
                }  catch (PackageManager.NameNotFoundException e) {
                    Toast.makeText(MyService.this, getString(R.string.switch_watch_toast) + packageName, Toast.LENGTH_SHORT).show();
                }
                imageView.setImageDrawable(getAppIcon(packageName));
                animateTargetChange();
            });
        }

    };

    void animateTargetChange() {
        if (textView.getVisibility() == View.GONE) return;
        imageView.setVisibility(View.VISIBLE);
        Animation a = imageView.getAnimation();
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
                imageView, "scaleX", 0.8f, 1f
        );
        ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(
                imageView, "scaleY", 0.8f, 1f
        );

        // 创建透明度提升动画
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(
                imageView, "alpha", 0.6f, 1f
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
                imageView.setVisibility(View.GONE);
                textView.setVisibility(View.VISIBLE);
                alphaAnimator2.start();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                imageView.setVisibility(View.GONE);
                textView.setVisibility(View.VISIBLE);
                alphaAnimator2.start();
            }
        });
    }

    boolean isKeepWatching = true;

    String mWatchingPackageName = null;
    final BroadcastReceiver myReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Watch.SEND_BINDER_ACTION:
                    BinderContainer binderContainer = intent.getParcelableExtra("binder");
                    IBinder binder = binderContainer.getBinder();
                    //如果binder已经失去活性了，则不再继续解析
                    if (!binder.pingBinder()) break;

                    myAidlInterface = IMyAidlInterface.Stub.asInterface(binder);
                    try {
                        mWatchingPackageName = myAidlInterface.registerFpsWatch(iFpsCallback, isKeepWatching);
                        PackageManager pm = context.getPackageManager();
                        ApplicationInfo appInfo = pm.getApplicationInfo(mWatchingPackageName, 0);
                        String label = pm.getApplicationLabel(appInfo).toString();
                        imageView.setImageDrawable(getAppIcon(mWatchingPackageName));
                        animateTargetChange();
                        Toast.makeText(MyService.this, getString(R.string.start_watch_toast) + label, Toast.LENGTH_SHORT).show();
                        mRegisterTime = System.currentTimeMillis();
                    } catch (RemoteException ignored) {

                    } catch (PackageManager.NameNotFoundException e) {
                        Toast.makeText(MyService.this, R.string.start_watch_toast, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case EXIT_ACTION:
                    try {
                        myAidlInterface.unregisterFpsWatch(iFpsCallback);
                        Toast.makeText(MyService.this, R.string.stop_watch_toast, Toast.LENGTH_SHORT).show();
                        stopSelf();
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                    break;
            }
        }
    };

    final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            int size, height;
            switch (key) {
                case "tran":
                    params.alpha = sharedPreferences.getInt(key,90) * 0.01f;
                    windowManager.updateViewLayout(frameLayout,params);
                    break;
                case "size":
                    size = (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sharedPreferences.getInt(key,40), getResources().getDisplayMetrics()));
                    height = sharedPreferences.getBoolean("aspect_change", false) ? (size * 2 / 3) : size;
                    params.width = size;
                    params.height = height;
                    windowManager.updateViewLayout(frameLayout,params);
                    break;
                case "corner":
                    float density = getResources().getDisplayMetrics().density;
                    int ROUND_CORNER = sharedPreferences.getInt(key,5);
                    ShapeDrawable oval = new ShapeDrawable(new RoundRectShape(new float[]{ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density}, null, null));
                    oval.getPaint().setColor(getColor(R.color.colorAccent));
                    textView.setBackground(oval);
                    windowManager.updateViewLayout(frameLayout,params);
                    break;
                case "text_size":
                    textView.setTextSize(sharedPreferences.getInt(key,16));
                    windowManager.updateViewLayout(frameLayout,params);
                    break;
                case "aspect_change":
                    size = (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sharedPreferences.getInt("size", 40), getResources().getDisplayMetrics()));
                    height = sharedPreferences.getBoolean(key, false) ? (size * 2 / 3) : size;
                    params.width = size;
                    params.height = height;
                    windowManager.updateViewLayout(frameLayout,params);
                    windowManager.updateViewLayout(frameLayout,params);
                    break;
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private WindowManager windowManager;
    private WindowManager.LayoutParams params;
    private TextView textView;
    private ImageView imageView;
    private LinearLayout toolsContainer;
    private ImageView tool1, tool2, tool3;
    private FrameLayout frameLayout;

    int SCREEN_WIDTH, SCREEN_HEIGHT;

    void GetWidthHeight() {
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        SCREEN_WIDTH = metrics.widthPixels;
        SCREEN_HEIGHT = metrics.heightPixels;
    }

    SharedPreferences sp;

    @Override
    public void onCreate() {
        super.onCreate();
        initCache();
        mLastConfig = new Configuration(getResources().getConfiguration());
        sp = getSharedPreferences("s", 0);
        sp.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

        isKeepWatching = sp.getBoolean("is_keep", true);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Watch.SEND_BINDER_ACTION);
        filter.addAction(EXIT_ACTION);
        registerReceiver(myReceiver, filter, Context.RECEIVER_EXPORTED);
        windowManager = (WindowManager) getSystemService(Service.WINDOW_SERVICE);
        int size = (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sp.getInt("size",40), getResources().getDisplayMetrics()));

        int height = sp.getBoolean("aspect_change", false) ? (size * 2 / 3) : size;
        params = new WindowManager.LayoutParams(size, height, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.RGBA_8888);
        params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;


        params.alpha = sp.getInt("tran",90) * 0.01f;
        params.x = 100;
        params.y = 100;
        params.gravity = Gravity.START | Gravity.TOP;
        textView = new TextView(this);
        textView.setTextColor(getColor(R.color.right));
        textView.setTypeface(null, Typeface.BOLD);
        textView.setTextSize(sp.getInt("text_size",16));
        textView.setText("FPS");
        textView.setGravity(Gravity.CENTER);
        float density = getResources().getDisplayMetrics().density;
        int ROUND_CORNER = sp.getInt("corner",5);
        ShapeDrawable oval = new ShapeDrawable(new RoundRectShape(new float[]{ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density}, null, null));
        oval.getPaint().setColor(getColor(R.color.colorAccent));
        textView.setBackground(oval);

        GetWidthHeight();
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
                            imageView.setVisibility(View.GONE);
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

        imageView = new ImageView(this);
        imageView.setVisibility(View.GONE);

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
            imageView.setVisibility(View.GONE);
            lineChart.setVisibility(View.GONE);
            textView.setVisibility(View.VISIBLE);

            int origSize = (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sp.getInt("size", 40), getResources().getDisplayMetrics()));
            int origHeight = sp.getBoolean("aspect_change", false) ? (origSize * 2 / 3) : origSize;
            params.width = origSize;
            params.height = origHeight;
            windowManager.updateViewLayout(frameLayout, params);
        });
        tool2 = new ImageView(this);
        tool2.setBackground(shapeDrawable);
        tool2.setImageDrawable(getDrawable(R.drawable.chart_line));
        tool2.setOnClickListener(v -> {
            isRecording = true;
            if (lineChart.getVisibility() == View.VISIBLE) {
                toolsContainer.setVisibility(View.VISIBLE);
                imageView.setVisibility(View.GONE);
                lineChart.setVisibility(View.GONE);
                textView.setVisibility(View.GONE);
            } else {
                toolsContainer.setVisibility(View.VISIBLE);
                imageView.setVisibility(View.GONE);
                lineChart.setVisibility(View.VISIBLE);
                textView.setVisibility(View.GONE);
            }
        });
        tool2.setOnLongClickListener(v -> {
            if(isRecording) {
                saveFpsDataToFile();


            }
            return true;
        });
        tool3 = new ImageView(this);
        tool3.setBackground(shapeDrawable);
        tool3.setImageDrawable(getDrawable(R.drawable.exit));
        tool3.setOnClickListener(v -> {
            try {
                myAidlInterface.unregisterFpsWatch(iFpsCallback);
                Toast.makeText(MyService.this, R.string.stop_watch, Toast.LENGTH_SHORT).show();
                stopSelf();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        });
        lineChart = new LineChart(this);
        int lineChartWidth = Math.min(SCREEN_WIDTH, SCREEN_HEIGHT) * 4 / 5;
        int lineChartHeight = lineChartWidth * 2 / 3;
        LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(lineChartWidth, lineChartHeight);
        params1.topMargin = getDrawable(R.drawable.arrow_left).getIntrinsicHeight() + 2;
        lineChart.setLayoutParams(params1);
        ShapeDrawable oval3 = new ShapeDrawable(new RoundRectShape(new float[]{ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density}, null, null));
        oval3.getPaint().setColor(getColor(R.color.colorAccent));
        lineChart.setBackground(oval3);
        lineChart.setVisibility(View.GONE);
        setupChart();


        toolsContainer.addView(tool1);
        toolsContainer.addView(tool2);
        toolsContainer.addView(tool3);

        frameLayout = new FrameLayout(this);
        frameLayout.addView(textView);
        frameLayout.addView(imageView);
        frameLayout.addView(lineChart);
        frameLayout.addView(toolsContainer);


        LayoutTransition transition = new LayoutTransition();
        transition.setDuration(200L);
        ObjectAnimator animator = ObjectAnimator.ofFloat(null, "scaleX", 0.0f, 1.0f);
        transition.setAnimator(2, animator);
        frameLayout.setLayoutTransition(transition);
        toolsContainer.setLayoutTransition(transition);

        windowManager.addView(frameLayout, params);
    }

    private LineChart lineChart;
    private LineDataSet lineDataSet;
    private LineData lineData;

    private boolean isYAxisMaxSet;

    private void setupChart() {
        lineDataSet = new LineDataSet(null, "实时帧率");
        lineDataSet.setColor(getColor(R.color.right));
        lineDataSet.setDrawValues(false);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setLineWidth(2f);
        lineDataSet.setDrawFilled(true);//填充底部颜色
        lineDataSet.setFillColor(getColor(R.color.bg));

        lineData = new LineData(lineDataSet);
        lineChart.setData(lineData);

        lineChart.setDoubleTapToZoomEnabled(false);
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setScaleEnabled(false);
        lineChart.setPinchZoom(false);
        lineChart.setDrawGridBackground(false);
        lineChart.setHighlightPerTapEnabled(false);       // 禁止点击高亮
        lineChart.setHighlightPerDragEnabled(false);      // 禁止拖拽高亮


        lineChart.getLegend().setEnabled(false);
        lineChart.getAxisLeft().setAxisMinimum(0f);
        lineChart.getAxisLeft().setAxisMaximum(60f);
        isYAxisMaxSet = true;

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1000f);
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat sdf = new SimpleDateFormat("m:ss", Locale.getDefault());

            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                return sdf.format(new Date((long) value));
            }
        });

        lineChart.getXAxis().setTextColor(getColor(R.color.right));
        lineChart.getAxisLeft().setTextColor(getColor(R.color.right));
        lineChart.getAxisRight().setEnabled(false);
    }

    private float sum = 0f;
    private float count = 0;
    private int average = 0;

    private void addEntry(float timestamp, float value) {
        lineDataSet.addEntry(new Entry(timestamp, value));
        count++;
        sum += value;
        average = (int) (sum / count);

        if (lineChart.getVisibility() == View.VISIBLE) {
            if (value > 60f && isYAxisMaxSet) {
                lineChart.getAxisLeft().resetAxisMaximum();
                isYAxisMaxSet = false;
            }
            lineData.notifyDataChanged();
            lineChart.notifyDataSetChanged();

            // 设置最大可见区域为60秒
            lineChart.setVisibleXRangeMaximum(60000f);
            lineChart.moveViewToX(timestamp);

            if (count > 10) {
                YAxis leftAxis = lineChart.getAxisLeft();
                LimitLine avgLine = new LimitLine(average, getString(R.string.average) + average);
                int color = getColor(R.color.right);
                int red = Color.red(color);
                int green = Color.green(color);
                int blue = Color.blue(color);

                int colorWithAlpha = Color.argb(128, red, green, blue); // 128 = 半透明

                avgLine.setLineColor(colorWithAlpha);
                avgLine.setLineWidth(2f);
                avgLine.setTextColor(colorWithAlpha);
                avgLine.setTextSize(12f);
                avgLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);

                leftAxis.removeAllLimitLines(); // 如果之前有其他 LimitLine
                leftAxis.addLimitLine(avgLine);
            }


        }

    }

    private void saveFpsDataToFile() {
        final SimpleDateFormat sdf2 = new SimpleDateFormat("MM月dd日hh:mm", Locale.getDefault());

        StringBuilder sb = new StringBuilder();
        sb.append("时间, FPS\n"); // CSV header
        final SimpleDateFormat sdf = new SimpleDateFormat("m:ss", Locale.getDefault());

        for (Entry entry : lineDataSet.getValues()) {
            long timestamp = (long) entry.getX();
            float value = entry.getY();
            sb.append(sdf.format(timestamp)).append(", ").append(value).append("\n");
        }

        try {
            // 文件名带时间戳
            String filename = sdf2.format(System.currentTimeMillis()) + "_" + mWatchingPackageName + ".txt";
            File file = new File(getExternalFilesDir(null), filename);

            FileOutputStream fos = new FileOutputStream(file);
            fos.write(sb.toString().getBytes());
            fos.close();
            Toast.makeText(this, getString(R.string.saved_to) + filename, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.save_failed) + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        windowManager.removeViewImmediate(frameLayout);
        unregisterReceiver(myReceiver);
        sp.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }


    private Configuration mLastConfig;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if ((mLastConfig.diff(newConfig) & ActivityInfo.CONFIG_ORIENTATION) > 0) {
            GetWidthHeight();
            params.x = Math.min(Math.max(params.x, 0), SCREEN_WIDTH);
            params.y = Math.min(Math.max(params.y, 0), SCREEN_HEIGHT);
            windowManager.updateViewLayout(frameLayout, params);
        }else if ((mLastConfig.diff(newConfig) & ActivityInfo.CONFIG_UI_MODE) > 0) {
            textView.setTextColor(getColor(R.color.right));
            float density = getResources().getDisplayMetrics().density;
            int ROUND_CORNER = sp.getInt("corner",5);
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

            lineChart.getXAxis().setTextColor(getColor(R.color.right));
            lineChart.getAxisLeft().setTextColor(getColor(R.color.right));

            lineDataSet.setColor(getColor(R.color.right));
            lineDataSet.setFillColor(getColor(R.color.bg));
        }
        mLastConfig = new Configuration(newConfig);
    }
}