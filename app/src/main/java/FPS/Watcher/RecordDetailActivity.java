package FPS.Watcher;

import static FPS.Watcher.ManageRecordActivity.getAppLabelByName;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

//import com.github.mikephil.charting.charts.LineChart;
//import com.github.mikephil.charting.components.AxisBase;
//import com.github.mikephil.charting.components.LimitLine;
//import com.github.mikephil.charting.components.MarkerView;
//import com.github.mikephil.charting.components.XAxis;
//import com.github.mikephil.charting.components.YAxis;
//import com.github.mikephil.charting.data.Entry;
//import com.github.mikephil.charting.data.LineData;
//import com.github.mikephil.charting.data.LineDataSet;
//import com.github.mikephil.charting.formatter.ValueFormatter;
//import com.github.mikephil.charting.highlight.Highlight;
//import com.github.mikephil.charting.utils.MPPointF;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class RecordDetailActivity extends Activity {
//    LineChart lineChart;
//    LineDataSet lineDataSet;
//    LineData lineData;
    LineChartView lineChart;
    File file;
    String date, appName, appLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getIntent().getExtras();
        if (bundle == null) {
            Toast.makeText(this, R.string.cant_resolve_csv, Toast.LENGTH_SHORT).show();
            return;
        }

        file = bundle.getSerializable("csvFile", File.class);

        if (file == null) {
            Toast.makeText(this, R.string.cant_resolve_csv, Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("m:ss.SSS", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // 👈 避免时区偏移

        lineChart = new LineChartView(this,true);
//        lineChart = new LineChart(this);
        SharedPreferences sp = getSharedPreferences("s", 0);
        float density = getResources().getDisplayMetrics().density;
        int ROUND_CORNER = sp.getInt("corner", 5);
        ShapeDrawable oval3 = new ShapeDrawable(new RoundRectShape(new float[]{ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density, ROUND_CORNER * density}, null, null));
        oval3.getPaint().setColor(getColor(R.color.colorAccent));
        lineChart.setBackground(oval3);
//
//        lineChart.setDoubleTapToZoomEnabled(false);
//        lineChart.getDescription().setEnabled(false);
//        lineChart.setTouchEnabled(true);
//        lineChart.setScaleEnabled(true);
//        lineChart.setPinchZoom(false);
//        lineChart.setDrawGridBackground(false);
//        lineChart.setHighlightPerTapEnabled(true);       // 禁止点击高亮
//        lineChart.setHighlightPerDragEnabled(false);      // 禁止拖拽高亮
//        MarkerView markerView = new FpsMarkerView(this);
//        markerView.setChartView(lineChart); // 必须设置
//        lineChart.setMarker(markerView);
////        lineChart.setVisibleXRangeMaximum(600000f);
//
//        lineChart.getLegend().setEnabled(false);
//        lineChart.getAxisRight().setEnabled(false);
//        lineChart.getAxisLeft().setAxisMinimum(0f);
//        lineChart.getAxisLeft().setAxisMaximum(60f);
//        boolean isYAxisMaxSet = true;
//
//        lineChart.getAxisLeft().setTextColor(getColor(R.color.right));
//



//        float sum = 0f;
//        float count = 0;

        List<LineChartView.Point> entries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            reader.readLine(); // 读首行header
            String line;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    String timeStr = parts[0].trim();
                    float fps = Float.parseFloat(parts[1].trim());
//                    count++;
//                    sum += fps;
//                    if (fps > 60f && isYAxisMaxSet) {
//                        lineChart.getAxisLeft().resetAxisMaximum();
//                        isYAxisMaxSet = false;
//                    }
                    Date date = sdf.parse(timeStr);
                    long mills = date.getTime();
                    entries.add(new LineChartView.Point(mills, fps));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        lineChart.setPointsData(entries);

//        lineDataSet = new LineDataSet(entries, "FPS");
//
//        lineDataSet.setColor(getColor(R.color.right));
//        lineDataSet.setDrawValues(false);
//        lineDataSet.setDrawCircles(false);
//        lineDataSet.setLineWidth(2f);
//        lineDataSet.setDrawFilled(true);//填充底部颜色
//        lineDataSet.setFillColor(getColor(R.color.bg));
//
//
//
//        lineData = new LineData(lineDataSet);
//        lineChart.setData(lineData);



//        XAxis xAxis = lineChart.getXAxis();
//        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
//        xAxis.setGranularity(1000f);
//
//        xAxis.setValueFormatter(new ValueFormatter() {
//          private final SimpleDateFormat sdf = new SimpleDateFormat("m:ss", Locale.getDefault());
//            @Override
//            public String getAxisLabel(float value, AxisBase axis) {
//                return sdf.format(new Date((long) value));
//            }
//        });
//
//        xAxis.setTextColor(getColor(R.color.right));
//
//        int average = (int) (sum / count);
//        if (count > 10) {
//            YAxis leftAxis = lineChart.getAxisLeft();
//            LimitLine avgLine = new LimitLine(average, getString(R.string.average) + average);
//            int color = getColor(R.color.right);
//            int red = Color.red(color);
//            int green = Color.green(color);
//            int blue = Color.blue(color);
//
//            int colorWithAlpha = Color.argb(128, red, green, blue); // 128 = 半透明
//
//            avgLine.setLineColor(colorWithAlpha);
//            avgLine.setLineWidth(2f);
//            avgLine.setTextColor(colorWithAlpha);
//            avgLine.setTextSize(12f);
//            avgLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
//
//            leftAxis.removeAllLimitLines(); // 如果之前有其他 LimitLine
//            leftAxis.addLimitLine(avgLine);
//        }
//        lineChart.moveViewToX(entries.get(entries.size() - 1).getX()); // 移动到最后



        FrameLayout lineChartContainer = new MyFrameLayout(this);

        lineChartContainer.addView(lineChart, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));



        FrameLayout container = new FrameLayout(this);

        container.addView(lineChartContainer);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
        String fileName = file.getName();
        // 提取日期和APP名称
        String[] parts = fileName.split("_");
        if (parts.length == 2) {
             date = parts[0];  // 记录日期
             appName = parts[1].replace(".csv", "");  // APP名称
             appLabel = getAppLabelByName(getPackageManager(),appName);
            setTitle(date + " " + appLabel);
        }
        container.setFitsSystemWindows(true); // 避免安卓15上进入edgeToEdge模式
        setContentView(container);
    }

    public static class MyFrameLayout extends FrameLayout {

        public MyFrameLayout(Context context) {
            super(context);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = MeasureSpec.getSize(heightMeasureSpec);
            float mAspectRatio = 16f / 9f;
            float scale = (float) width / height;
            if (scale > mAspectRatio) {
                width = (int) (height * mAspectRatio);
            } else {
                height = (int) (width / mAspectRatio);
            }

            int newWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                    width, MeasureSpec.EXACTLY);
            int newHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                    height, MeasureSpec.EXACTLY);

            super.onMeasure(newWidthMeasureSpec, newHeightMeasureSpec);

        }

    }

//    public class FpsMarkerView extends MarkerView {
//        private final TextView tvContent;
//
//
//        public FpsMarkerView(Context context) {
//            super(context, R.layout.marker); // 你可以自己定义 layout，也可以下面我提供一个简单示例
//            tvContent = findViewById(R.id.tvContent);
//        }
//
//        @Override
//        public void refreshContent(Entry e, Highlight highlight) {
//            float fps = e.getY();
//            String label = "FPS: " + fps;
//            tvContent.setText(label);
//            tvContent.setTextColor(getColor(R.color.right));
//            super.refreshContent(e, highlight);
//        }
//
//        @Override
//        public MPPointF getOffset() {
//            // 居中浮窗
//            return new MPPointF(-(getWidth() / 2f), -getHeight());
//        }
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 创建菜单项
        MenuItem savePng = menu.add(Menu.NONE, 1, Menu.NONE, R.string.save_to_gallery);
        savePng.setIcon(android.R.drawable.ic_menu_sort_by_size);
        savePng.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        MenuItem saveCsv = menu.add(Menu.NONE, 2, Menu.NONE, R.string.export_csv_file);
        saveCsv.setIcon(android.R.drawable.ic_menu_sort_by_size);
        saveCsv.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            if (lineChart != null) {
                int width = lineChart.getWidth();
                int height = lineChart.getHeight();

                if (width == 0 || height == 0) {
                    // 如果没有尺寸，先测量一次（可选）
                    lineChart.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                    width =  lineChart.getMeasuredWidth();
                    height = lineChart.getMeasuredHeight();

                    lineChart.layout(0, 0, width, height);  // 设置布局
                }


                Drawable originalBackground = lineChart.getBackground();
                lineChart.setBackgroundColor(getColor(R.color.colorAccent));
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                lineChart.draw(canvas);  // 把 View 绘制到 Bitmap 上
                lineChart.setBackground(originalBackground);

                try {
                    Drawable drawable = getPackageManager().getApplicationIcon(appName);
                    int drawLeft = width / 8;
                    int drawTop = height / 32;
                    int drawWidth = width / 8;
                    drawable.setBounds(drawLeft, drawTop, drawLeft + drawWidth, drawTop + drawWidth);

                    // 在 Canvas 上绘制
                    drawable.draw(canvas);
                    drawable.draw(canvas);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }


                insertImageToGallery(bitmap,this);


            }
            return true;
        } else if (item.getItemId() == 2) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            // 文件类型
            intent.setType("text/csv");  // 设置为 CSV 类型
            // 文件名称
            intent.putExtra(Intent.EXTRA_TITLE, file.getName());
            startActivityForResult(intent, 66);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public void insertImageToGallery(Bitmap bitmap, Context context) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "Record_" + date + "_" + appLabel + ".png");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FPSRecord"); // 相册子目录
        values.put(MediaStore.Images.Media.IS_PENDING, 1);

        ContentResolver resolver = context.getContentResolver();
        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (uri != null) {
            try (OutputStream out = resolver.openOutputStream(uri)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                Toast.makeText(context, R.string.save_success, Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // 更新 IS_PENDING = 0，让图片变为可见
            values.clear();
            values.put(MediaStore.Images.Media.IS_PENDING, 0);
            resolver.update(uri, values, null, null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 66 && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            if (uri != null) {
                try (InputStream in = new FileInputStream(file);
                     OutputStream out = getContentResolver().openOutputStream(uri)) {

                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }

                    Toast.makeText(this, R.string.export_toast1, Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Toast.makeText(this, R.string.export_toast2, Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }

            }
        }
    }
}
