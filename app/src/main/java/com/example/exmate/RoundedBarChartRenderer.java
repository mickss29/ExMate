package com.example.exmate;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.buffer.BarBuffer;
import com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.renderer.BarChartRenderer;
import com.github.mikephil.charting.utils.ViewPortHandler;

public class RoundedBarChartRenderer extends BarChartRenderer {

    private final float radius;

    public RoundedBarChartRenderer(
            BarDataProvider chart,
            ChartAnimator animator,
            ViewPortHandler viewPortHandler,
            float radiusDp,
            Context context                  // ← Context passed separately
    ) {
        super(chart, animator, viewPortHandler);
        // Convert dp → px using Context
        this.radius = radiusDp * context.getResources()
                .getDisplayMetrics().density;
    }

    @Override
    protected void drawDataSet(Canvas c, IBarDataSet dataSet, int index) {

        BarBuffer buffer = mBarBuffers[index];
        buffer.setPhases(mAnimator.getPhaseX(), mAnimator.getPhaseY());
        buffer.setDataSet(index);
        buffer.setBarWidth(mChart.getBarData().getBarWidth());
        buffer.feed(dataSet);

        Paint renderPaint = mRenderPaint;

        for (int j = 0; j < buffer.size(); j += 4) {

            if (!mViewPortHandler.isInBoundsLeft(buffer.buffer[j + 2])) continue;
            if (!mViewPortHandler.isInBoundsRight(buffer.buffer[j])) break;

            renderPaint.setColor(dataSet.getColor(j / 4));

            float left   = buffer.buffer[j];
            float top    = buffer.buffer[j + 1];
            float right  = buffer.buffer[j + 2];
            float bottom = buffer.buffer[j + 3];

            // Draw rounded top rectangle
            Path path = roundedTop(left, top, right, bottom, radius, radius);
            c.drawPath(path, renderPaint);
        }
    }

    private Path roundedTop(
            float left, float top, float right, float bottom,
            float rx, float ry
    ) {
        Path path = new Path();
        if (rx < 0) rx = 0;
        if (ry < 0) ry = 0;

        float width  = right - left;
        float height = bottom - top;

        if (rx > width / 2)  rx = width  / 2;
        if (ry > height / 2) ry = height / 2;

        path.moveTo(right, bottom);
        path.lineTo(right, top + ry);
        path.quadTo(right, top, right - rx, top);
        path.lineTo(left + rx, top);
        path.quadTo(left, top, left, top + ry);
        path.lineTo(left, bottom);
        path.close();

        return path;
    }
}