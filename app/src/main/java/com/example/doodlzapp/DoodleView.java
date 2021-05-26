package com.example.doodlzapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.ArrayList;

public class DoodleView extends View
{
    //public vars
    public Bitmap canvasBitmap;

    //private vars
    private Path drawPath;
    private Paint drawPaint, canvasPaint;
    private int paintColor = 0xFF660000;
    private Canvas drawCanvas;
    private float brushSize, lastBrushSize;
    private boolean erase = false;

    private Integer currentBrushSize = 5;
    private ArrayList<Path> mPaths;
    private ArrayList<Path> undonePaths = new ArrayList<>();
    private ArrayList<Paint> undonePaints = new ArrayList<>();
    private ArrayList<Paint> mPaints;

    public boolean flagline = false;
    public static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123;

    private static float MIN_ZOOM = 1f;
    private static float MAX_ZOOM = 5f;

    private float scaleFactor = 1.f;
    private ScaleGestureDetector detector;


    public DoodleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupDraw();
        detector = new ScaleGestureDetector(getContext(), new ScaleListener());
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(MIN_ZOOM, Math.min(scaleFactor, MAX_ZOOM));
            invalidate();
            return true;
        }
    }

    public void startNew() {
        drawCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        setupDraw();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(canvasBitmap);
    }

    private void setupDraw()
    {
        this.mPaths = new ArrayList<Path>();
        this.mPaints = new ArrayList<Paint>();

        this.addPath(false);

        drawPaint = new Paint();
        drawPath = new Path();

        drawPaint.setColor(paintColor);
        drawPaint.setStrokeWidth(currentBrushSize);
        drawPaint.setAntiAlias(true);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);

        drawPaint.setPathEffect(new CornerPathEffect(10) );
        canvasPaint = new Paint(Paint.DITHER_FLAG);
//        lastBrushSize = currentBrushSize;
    }

    // return the painted line's width
    public int getBrushWidth() {
        return (int)drawPaint.getStrokeWidth();
    }

    public void setBrushWidth(int size) {
        currentBrushSize = size;
        drawPaint.setStrokeWidth(currentBrushSize);
        invalidate();
    }

    public void setDefaultBrush(String brushType) {
        drawPaint.setMaskFilter(new BlurMaskFilter(1, BlurMaskFilter.Blur.SOLID) );
    }

    public void setBlurBrush(String brushType){
        drawPaint.setMaskFilter(new BlurMaskFilter(8, BlurMaskFilter.Blur.NORMAL));
    }
    
    public void setLastBrushSize(float lastSize)
    {
        lastBrushSize = lastSize;
    }

    public float getLastBrushSize()
    {
        return lastBrushSize;
    }

    public void setErase(boolean isErase) {
        this.setColor("#FFFFFF");
        erase = isErase;

        if (erase)
        {
            drawPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        }
        else
        {
            drawPaint.setXfermode(null);
        }
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        canvas.scale(scaleFactor, scaleFactor);

        canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);
//        canvas.drawPath(drawPath, drawPaint);

        for (int i = 0; i < mPaths.size(); ++i) {
            canvas.drawPath(mPaths.get(i), mPaints.get(i));
            invalidate();
        }

        canvas.restore();
    }

    private void addPath(boolean fill)
    {
        drawPath = new Path();
        mPaths.add(drawPath);

        drawPaint = new Paint();
        mPaints.add(drawPaint);
        System.out.print(paintColor);
        drawPaint.setColor(paintColor);
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(currentBrushSize);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
        invalidate();

    }

    public void onClickUndo() {
        if (mPaths.size() > 0) {
            undonePaths.add(mPaths.remove(mPaths.size() - 1));
            undonePaints.add(mPaints.remove(mPaints.size() - 1));
            invalidate();
        } else {}
        if (mPaths.size() > 0) {
            undonePaths.add(mPaths.remove(mPaths.size() - 1));
            undonePaints.add(mPaints.remove(mPaints.size() - 1));
            invalidate();
        }
        else{}
    }

    public void onClickRedo() {
        if (undonePaths.size() > 0) {
            mPaths.add(undonePaths.remove(undonePaths.size() - 1));
            mPaints.add(undonePaints.remove(undonePaints.size() - 1));
            invalidate();
        }
        else{}
        if (undonePaths.size() > 0) {
            mPaths.add(undonePaths.remove(undonePaths.size() - 1));
            mPaints.add(undonePaints.remove(undonePaints.size() - 1));
            invalidate();
        }
        else{}
    }

    public void drawLine(boolean flag) {
        flagline = flag;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        detector.onTouchEvent(event);

        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                this.addPath(true);
                drawPath.moveTo(touchX, touchY);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                drawPath.lineTo(touchX, touchY);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:

                drawPath.lineTo(touchX, touchY);

                this.addPath(true);
                drawCanvas.drawPath(drawPath, drawPaint);
                drawPath.reset();
                undonePaths.clear();
                undonePaints.clear();
                //MainActivity.redoBtn.setEnabled(false);
                invalidate();
                break;
            default:
                return false;
        }
        invalidate();
        return true;
    }

    public int getPaintColor() {
        return paintColor;
    }

    public void setColor(String newColor) {
        paintColor = Color.parseColor(newColor);
        drawPaint.setColor(paintColor);
        invalidate();
    }
}
