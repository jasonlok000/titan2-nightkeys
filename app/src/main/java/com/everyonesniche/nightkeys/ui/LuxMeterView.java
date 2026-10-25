package com.everyonesniche.nightkeys.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import com.everyonesniche.nightkeys.R;

/** Non-interactive logarithmic lux scale: the lit "keys on" zone, a threshold tick and the live reading. */
public final class LuxMeterView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float lux = Float.NaN, threshold = 10;
    public LuxMeterView(Context c, AttributeSet a) { super(c,a); }
    public void readings(float lux, float threshold) { this.lux = lux; this.threshold = threshold; invalidate(); }
    private float dp(float value) { return value*getResources().getDisplayMetrics().density; }
    private float position(float value) { return (float)(Math.log10(1+Math.max(0,Math.min(value,1000)))/Math.log10(1001)); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float left=dp(8), right=getWidth()-dp(8), y=dp(22);
        float edge=left+(right-left)*position(threshold);
        paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeWidth(dp(8)); paint.setColor(getContext().getColor(R.color.stroke));
        canvas.drawLine(left,y,right,y,paint);
        paint.setColor(getContext().getColor(R.color.accent));
        canvas.drawLine(left,y,edge,y,paint);
        paint.setStrokeWidth(dp(2)); paint.setColor(getContext().getColor(R.color.text));
        canvas.drawLine(edge,y-dp(9),edge,y+dp(9),paint);
        if (Float.isFinite(lux)) {
            float x=left+(right-left)*position(lux);
            paint.setColor(getContext().getColor(R.color.background)); canvas.drawCircle(x,y,dp(9),paint);
            paint.setColor(getContext().getColor(R.color.text)); canvas.drawCircle(x,y,dp(6),paint);
        }
        paint.setTextSize(12*getResources().getDisplayMetrics().scaledDensity);
        paint.setColor(getContext().getColor(R.color.accent));
        paint.setTextAlign(Paint.Align.LEFT); canvas.drawText(getContext().getString(R.string.meter_on),left,dp(50),paint);
        paint.setColor(getContext().getColor(R.color.muted));
        paint.setTextAlign(Paint.Align.RIGHT); canvas.drawText(getContext().getString(R.string.meter_off),right,dp(50),paint);
    }
}
