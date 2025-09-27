package FPS.Watcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LineChartView extends View {
    private final Paint axisPaint = new Paint();   // 坐标轴画笔
    private final Paint linePaint = new Paint();   // 折线画笔
    //    private Paint pointPaint;  // 数据点画笔
    private final Paint textPaint = new Paint();   // 文本画笔

    private final Paint fillPaint = new Paint();
    private final Paint avgPaint = new Paint();
    private final Paint textAvgPaint = new Paint();
    private final Paint valuePaint = new Paint();
    private final Paint crossPaint = new Paint();

    private final Path linePath = new Path();
    private final Path fillPath = new Path();

    public final List<Long> dataX = new ArrayList<>();
    public final List<Float> dataY = new ArrayList<>();

    private float maxX = 10;  // X 轴最大值
    private float maxY = 60;  // Y 轴最大值

    public LineChartView(Context context) {
        this(context, true);
    }

    public LineChartView(Context context, boolean allowTouch) {
        super(context);
        isAllowTouch = allowTouch;

        axisPaint.setColor(getContext().getColor(R.color.right));
        axisPaint.setStrokeWidth(3);

        linePaint.setColor(getContext().getColor(R.color.right));
        linePaint.setStrokeWidth(4);
        linePaint.setStyle(Paint.Style.STROKE);

//        pointPaint = new Paint();
//        pointPaint.setColor(Color.RED);
//        pointPaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(getContext().getColor(R.color.right));
        textPaint.setTextSize(35);

        fillPaint.setColor(getContext().getColor(R.color.bg));
        fillPaint.setStyle(Paint.Style.FILL);


        int color = getContext().getColor(R.color.right);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        int colorWithAlpha = Color.argb(128, red, green, blue); // 128 = 半透明

        avgPaint.setColor(colorWithAlpha); // 平均线颜色
        avgPaint.setStrokeWidth(5);
        avgPaint.setStyle(Paint.Style.STROKE);


        textAvgPaint.setColor(colorWithAlpha);
        textAvgPaint.setTextSize(35);

        valuePaint.setColor(getContext().getColor(R.color.right));
        valuePaint.setTextSize(35);
        valuePaint.setFakeBoldText(true);
        crossPaint.setColor(getContext().getColor(R.color.right));
        crossPaint.setStrokeWidth(3);
    }

    // 添加新点
    private float sumY = 0;      // 所有点 Y 值累加和
    private float avgValue = 0;  // 当前平均值

    public static class Point {
        public long x; // 可以存毫秒或秒
        public float y;

        public Point(long x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    public void setPointsData(List<Point> newPoints) {
        dataX.clear();
        dataY.clear();
        for (Point point : newPoints) {
            dataX.add(point.x);
            dataY.add(point.y);
            sumY += point.y;
            // Y 轴直接用本次添加的点
            maxY = Math.max(maxY, point.y);
        }
        maxX = newPoints.get(newPoints.size() - 1).x;
        avgValue = sumY / dataY.size();
        invalidate(); // 请求重绘
    }

    public void addPoint(long x, float y) {
        dataX.add(x);
        dataY.add(y);

        // X 轴仍用所有数据计算最大值
        maxX = x;

        // Y 轴直接用本次添加的点
        maxY = Math.max(maxY, y);

        // 更新累加和和平均值
        sumY += y;
        avgValue = sumY / dataY.size();

        invalidate(); // 请求重绘
    }


    float marginLeft = 90;
    float marginBottom = 80;


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();


        // 1. 画坐标轴
        canvas.drawLine(marginLeft, height - marginBottom, width - 50, height - marginBottom, axisPaint); // X 轴
        canvas.drawLine(marginLeft, height - marginBottom, marginLeft, 50, axisPaint);                  // Y 轴

// 画 X 轴刻度
        int xSteps = 5;
        float displayMaxX = maxX / 1000; // 使用秒为单位计算刻度

        for (int i = 0; i <= xSteps; i++) {
            float value = displayMaxX / xSteps * i; // 秒
            int minutes = (int) (value / 60);
            int seconds = (int) (value % 60);

            String label = String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);

            float px = marginLeft + (i / (float) xSteps) * (width - marginLeft - 50);
            float py = height - marginBottom;

            canvas.drawLine(px, py, px, py + 10, axisPaint);
            canvas.drawText(label, px - 20, py + 40, textPaint);
        }


        // 3. 画 Y 轴刻度
        int ySteps = 5;
        for (int i = 0; i <= ySteps; i++) {
            float value = maxY / ySteps * i;
            float px = marginLeft + 10;
            float py = (height - marginBottom) - (value / maxY) * (height - marginBottom - 50);
            canvas.drawLine(px - 10, py, px - 30, py, axisPaint);
            canvas.drawText(String.format(Locale.getDefault(), "%.0f", value), px - 80, py + 10, textPaint);
        }

        // 4. 画折线及下方填充
        if (!dataX.isEmpty()) {

            linePath.reset();
            fillPath.reset();

            for (int i = 0; i < dataX.size(); i++) {
                float px = marginLeft + (dataX.get(i) / maxX) * (width - marginLeft - 50);
                float py = (height - marginBottom) - (dataY.get(i) / maxY) * (height - marginBottom - 50);

                if (i == 0) {
                    linePath.moveTo(px, py);
                    fillPath.moveTo(px, height - marginBottom); // 从 X 轴起点开始
                    fillPath.lineTo(px, py);
                } else {
                    linePath.lineTo(px, py);
                    fillPath.lineTo(px, py);
                }

//                canvas.drawCircle(px, py, 5, pointPaint); // 数据点
            }

            // 闭合填充 Path
            float lastX = marginLeft + (dataX.get(dataX.size() - 1) / maxX) * (width - marginLeft - 50);
            fillPath.lineTo(lastX, height - marginBottom); // 回到底部
            fillPath.close();

            // 先填充灰色

            canvas.drawPath(fillPath, fillPaint);

            // 再画折线
            canvas.drawPath(linePath, linePaint);
        }


        // 6. 绘制平均值线
        if (!dataY.isEmpty()) {

            // 横线对应屏幕坐标
            float lineY = (height - marginBottom) - (avgValue / maxY) * (height - marginBottom - 50);

            // 画横线
            canvas.drawLine(marginLeft, lineY, width - 50, lineY, avgPaint);

            // 在右上角显示 avgValue

            canvas.drawText(String.format(Locale.getDefault(), getContext().getString(R.string.avg_fps), avgValue), width - 150, lineY - 10, textAvgPaint);
        }

// 显示选中点 Y 值和标记
        if (selectedIndex != -1) {
            float px = marginLeft + (dataX.get(selectedIndex) / maxX) * (width - marginLeft - 50);
            float py = (height - marginBottom) - (dataY.get(selectedIndex) / maxY) * (height - marginBottom - 50);

            // 显示 Y 值文字

            canvas.drawText(String.format(Locale.getDefault(), "FPS:%.2f", dataY.get(selectedIndex)), px + 10, py - 10, valuePaint);

            // 画十字标记

            float crossSize = 10; // 十字半边长度
            canvas.drawLine(px - crossSize, py, px + crossSize, py, crossPaint); // 横
            canvas.drawLine(px, py - crossSize, px, py + crossSize, crossPaint); // 竖


        }

    }

    private final boolean isAllowTouch;
    private int selectedIndex = -1; // 当前被选中的点索引

    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        if (!isAllowTouch) return true;
        if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {

            float touchX = event.getX();

            float width = getWidth();

            float plotWidth = width - marginLeft - 50;

            float minDistance = Float.MAX_VALUE;
            int nearestIndex = -1;

            for (int i = 0; i < dataX.size(); i++) {
                float px = marginLeft + (dataX.get(i) / maxX) * plotWidth;
                float distance = Math.abs(px - touchX);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestIndex = i;
                }
            }

            // 点击同一个点时取消选择
            if (nearestIndex == selectedIndex) {
                selectedIndex = -1;
            } else {
                selectedIndex = nearestIndex;
            }

            invalidate(); // 触发重绘显示或隐藏 Y 值
        }
        return true;
    }


}
