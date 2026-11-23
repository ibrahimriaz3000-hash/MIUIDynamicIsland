package com.dynamicpill.module;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

public class PillView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    public PillView(Context context) {
        super(context);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        rect.set(0, 0, getWidth(), getHeight());
        float r = getHeight() / 2f;
        canvas.drawRoundRect(rect, r, r, paint);
    }
}
