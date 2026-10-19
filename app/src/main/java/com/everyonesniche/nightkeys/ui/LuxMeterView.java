package com.everyonesniche.nightkeys.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import com.everyonesniche.nightkeys.R;

/** Non-interactive logarithmic lux scale: sparse, no continuous animation or battery work. */
public final class LuxMeterView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float lux = Float.NaN, threshold = 10;
    public LuxMeterView(Context c, AttributeSet a) { super(c,a); }
    public void readings(float lux, float threshold) { this.lux = lux; this.threshold = threshold; invalidate(); }
    private float dp(float value) { return value*getResources().getDisplayMetrics().density; }
    private float position(float value) { return (float)(Math.log10(1+Math.max(0,Math.min(value,1000)))/Math.log10(1001)); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float left=dp(4), right=getWidth()-dp(4), y=dp(25);
        paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeWidth(dp(6)); paint.setColor(getContext().getColor(R.color.stroke));
        canvas.drawLine(left,y,right,y,paint);
        paint.setColor(getContext().getColor(R.color.accent));
        canvas.drawLine(left,y,left+(right-left)*position(threshold),y,paint);
        if (Float.isFinite(lux)) {
            float x=left+(right-left)*position(lux);
            paint.setColor(getContext().getColor(R.color.background)); canvas.drawCircle(x,y,dp(7),paint);
            paint.setColor(getContext().getColor(R.color.text)); canvas.drawCircle(x,y,dp(4),paint);
        }
        paint.setColor(getContext().getColor(R.color.muted));
        paint.setTextSize(11*getResources().getDisplayMetrics().scaledDensity);
        paint.setTextAlign(Paint.Align.LEFT); canvas.drawText("Dark",left,dp(55),paint);
        paint.setTextAlign(Paint.Align.RIGHT); canvas.drawText("Bright",right,dp(55),paint);
    }
}
