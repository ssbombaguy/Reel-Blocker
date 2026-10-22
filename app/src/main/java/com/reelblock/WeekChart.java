package com.reelblock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

/** Seven rounded bars, oldest day on the left, today on the right. */
public class WeekChart extends View {
    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint empty = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint value = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float density;

    private int[] counts = new int[7];
    private String[] labels = new String[7];

    public WeekChart(Context c, AttributeSet attrs) {
        super(c, attrs);
        density = getResources().getDisplayMetrics().density;
        empty.setColor(c.getColor(R.color.off));
        label.setColor(c.getColor(R.color.text_dim));
        label.setTextSize(12 * density);
        label.setTextAlign(Paint.Align.CENTER);
        value.setColor(c.getColor(R.color.text));
        value.setTextSize(12 * density);
        value.setTextAlign(Paint.Align.CENTER);
        value.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    }

    void setData(int[] counts, String[] labels) {
        this.counts = counts;
        this.labels = labels;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        bar.setShader(new LinearGradient(0, h, 0, 0,
                new int[] {
                    getContext().getColor(R.color.orange),
                    getContext().getColor(R.color.pink),
                    getContext().getColor(R.color.purple)
                },
                null, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int max = 1;
        for (int n : counts) max = Math.max(max, n);

        float w = getWidth();
        float labelArea = 22 * density;
        float valueArea = 18 * density;
        float chartTop = valueArea;
        float chartBottom = getHeight() - labelArea;
        float slot = w / counts.length;
        float barW = Math.min(slot * 0.5f, 28 * density);
        float radius = barW / 2;

        for (int i = 0; i < counts.length; i++) {
            float cx = slot * i + slot / 2;
            float left = cx - barW / 2;
            if (counts[i] == 0) {
                rect.set(left, chartBottom - 6 * density, left + barW, chartBottom);
                canvas.drawRoundRect(rect, radius, radius, empty);
            } else {
                float h = Math.max(barW, (chartBottom - chartTop) * counts[i] / max);
                rect.set(left, chartBottom - h, left + barW, chartBottom);
                canvas.drawRoundRect(rect, radius, radius, bar);
                canvas.drawText(String.valueOf(counts[i]), cx, chartBottom - h - 6 * density, value);
            }
            boolean today = i == counts.length - 1;
            label.setColor(getContext().getColor(today ? R.color.text : R.color.text_dim));
            label.setTypeface(today ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            canvas.drawText(labels[i] == null ? "" : labels[i], cx, getHeight() - 4 * density, label);
        }
    }
}
