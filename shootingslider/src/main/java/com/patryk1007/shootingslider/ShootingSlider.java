package com.patryk1007.shootingslider;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ShootingSlider extends View {

    private int lineColor;
    private int pointColor;
    private int soundIconColor;
    private int pointerSize;
    private int lineSize;
    private boolean isSoundIconVisible;
    private boolean isNightmareMode;


    private Bitmap bitmapOrg;
    private Paint pointerPaint = new Paint();
    private Paint linePaint = new Paint();

    private long startPointerAnimationTime;
    private long startLoadingTime;
    private boolean isSoundIconTouched;
    private int defaultLeftMarginSoundIcon = 80;
    private int soundIconMaxTimeTouchMillis = 2000;

    private float deceleration = 1.01f;
    private float progressPercent;
    private float shotPower;
    private int leftMargin = 30;
    private int rightMargin = 50;

    private int viewWidth;
    private int viewHeight;


    public ShootingSlider(Context context) {
        super(context);
        startAnimation();
        setSoundsIcon(1);
    }


    public ShootingSlider(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        getAttr(attrs);
        startAnimation();
        setSoundsIcon(1);
    }

    private void getAttr(AttributeSet attrs) {
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.ShootingSlider);
        lineColor = typedArray.getInt(R.styleable.ShootingSlider_ssLineColor, Color.parseColor("#000000"));
        pointColor = typedArray.getInt(R.styleable.ShootingSlider_ssPointColor, Color.parseColor("#92B558"));
        soundIconColor = typedArray.getInt(R.styleable.ShootingSlider_ssSoundIconColor, Color.parseColor("#92B558"));
        pointerSize = (int) typedArray.getDimension(R.styleable.ShootingSlider_ssPointSize, 30);
        lineSize = (int) typedArray.getDimension(R.styleable.ShootingSlider_ssLineSize, 7);
        isSoundIconVisible = typedArray.getBoolean(R.styleable.ShootingSlider_ssSoundIconVisible, true);
        isNightmareMode = typedArray.getBoolean(R.styleable.ShootingSlider_ssNightmareMode, false);
        if (isNightmareMode) {
            isSoundIconVisible = true;
        }
        if (leftMargin < pointerSize ) {
            leftMargin = pointerSize ;
        }
        typedArray.recycle();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (viewWidth > 0) {
            if (event.getX() > getLeftSpace() && event.getX() < getRealProgressWidth() + getLeftSpace()) {
                if (event.getAction() == MotionEvent.ACTION_DOWN && !isNightmareMode) {
                    progressPercent = calculateProgressPercent(event.getX());
                    startAnimation();
                }
            } else if (isSoundIconVisible && event.getX() < getLeftSpace()) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    isSoundIconTouched = true;
                    loadingAnimation();
                }
            }
        }
        if (isSoundIconTouched && event.getAction() == MotionEvent.ACTION_UP) {
            isSoundIconTouched = false;
            progressPercent = shotPower;
            startAnimation();
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (viewWidth == 0 || viewHeight == 0) {
            viewWidth = getWidth();
            viewHeight = getHeight();
        }
        float elapsedTime = System.currentTimeMillis() - startPointerAnimationTime;
        float distance = calculateDistance();
        float xPosition = elapsedTime / deceleration;
        float yPosition = calculateYPosition(xPosition, distance);

        drawLine(canvas);
        drawProgressPoint(canvas, xPosition, yPosition, distance);
        updateSoundIcon();
        drawSoundIcon(canvas, yPosition < viewHeight / 2);
    }

    private float calculateDistance() {
        return getRealProgressWidth() * progressPercent + (isSoundIconVisible ? getLeftSpace() - defaultLeftMarginSoundIcon : 0);
    }

    private float calculateYPosition(float x, float distance) {
        double startSpeed = Math.sqrt(9.81 * distance / 2);
        return ((float) (x - (9.81 / (2 * startSpeed * startSpeed)) * x * x) * -1) + viewHeight / 2;
    }

    private void drawLine(Canvas canvas) {
        linePaint.setColor(lineColor);
        linePaint.setStrokeWidth(lineSize);
        canvas.drawLine(getLeftSpace(), viewHeight / 2, getRealProgressWidth() + getLeftSpace(), viewHeight / 2, linePaint);
    }

    private void drawProgressPoint(Canvas canvas, float x, float y, float distance) {
        pointerPaint.setColor(pointColor);
        if (y < viewHeight / 2) {
            canvas.drawCircle(x + (isSoundIconVisible ? defaultLeftMarginSoundIcon : getLeftSpace()), y, pointerSize, pointerPaint);
            this.postInvalidate();
        } else {
            canvas.drawCircle(distance + (isSoundIconVisible ? defaultLeftMarginSoundIcon : getLeftSpace()), viewHeight / 2, pointerSize, pointerPaint);
        }
    }

    private void updateSoundIcon() {
        if (isSoundIconTouched) {
            shotPower = calculateShootPercent();
            setSoundsIcon(shotPower);
            this.postInvalidate();
        }
    }

    private void drawSoundIcon(Canvas canvas, boolean isPointShot) {
        if (isSoundIconVisible) {
            Matrix matrix = new Matrix();
            matrix.postRotate((isSoundIconTouched || isPointShot) ? -45 : 0, bitmapOrg.getWidth() / 2, bitmapOrg.getHeight() / 2);
            matrix.postTranslate(10, viewHeight / 2 - bitmapOrg.getHeight() / 2);
            canvas.drawBitmap(bitmapOrg, matrix, null);
        }
    }

    private void setSoundsIcon(float power) {
        int iconId;
        if (power < 0.3f) {
            iconId = R.drawable.ic_volume_up_white_48dp_0;
        } else if (power < 0.6f) {
            iconId = R.drawable.ic_volume_up_white_48dp_1;
        } else {
            iconId = R.drawable.ic_volume_up_white_48dp;
        }
        bitmapOrg = BitmapFactory.decodeResource(getResources(),
                iconId).copy(Bitmap.Config.ARGB_8888, true);
        bitmapOrg = changeBitmapColor(bitmapOrg, soundIconColor);
    }

    private static Bitmap changeBitmapColor(Bitmap sourceBitmap, int color) {
        Bitmap resultBitmap = sourceBitmap.copy(sourceBitmap.getConfig(), true);
        Paint paint = new Paint();
        ColorFilter filter = new LightingColorFilter(color, 1);
        paint.setColorFilter(filter);
        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawBitmap(resultBitmap, 0, 0, paint);
        return resultBitmap;
    }

    private float calculateProgressPercent(float clickPositionX) {
        float percent = (clickPositionX - getLeftSpace()) / getRealProgressWidth();
        setSoundsIcon(percent);
        return percent;
    }

    private float calculateShootPercent() {
        float elapsedTime = System.currentTimeMillis() - startLoadingTime;
        float percent = elapsedTime / soundIconMaxTimeTouchMillis;
        if (percent > 1f) {
            percent = 1f;
        } else if (percent < 0) {
            percent = 0;
        }
        return percent;
    }

    private int getRealProgressWidth() {
        return viewWidth - getLeftSpace() - rightMargin;
    }

    private void startAnimation() {
        this.startPointerAnimationTime = System.currentTimeMillis();
        this.postInvalidate();
    }

    private void loadingAnimation() {
        this.startLoadingTime = System.currentTimeMillis();
        this.postInvalidate();
    }


    private int getLeftSpace() {
        return isSoundIconVisible ? leftMargin + 150 : leftMargin;
    }
}