package com.example.doodlzapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class DoodleView extends View
{
    //public vars
    public Bitmap canvasBitmap;

    //private vars
    private Path drawPath;
    private Paint drawPaint, canvasPaint;
    private int paintColor = Color.BLACK;
    private Canvas drawCanvas;

    private int drawingBackgroundColor;

    private Integer currentBrushSize = 15;
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

    private boolean erase = false;
    private boolean isBlurBrush = false;
    private boolean isPaintBucket = false;

    public DoodleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupDraw();
        setLayerType(View.LAYER_TYPE_SOFTWARE, drawPaint);

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
        drawPaint.setAntiAlias(true);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);

        drawPaint.setPathEffect(new CornerPathEffect(10) );
        canvasPaint = new Paint(Paint.DITHER_FLAG);
        drawingBackgroundColor = canvasPaint.getColor();
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
        isBlurBrush = false;
        drawPaint.setMaskFilter(new BlurMaskFilter(1, BlurMaskFilter.Blur.SOLID) );
    }

    public void setBlurBrush(){
        isBlurBrush = true;
    }

    public void setErase(boolean isErase) {
        this.setColor(Color.WHITE);
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

    // clear the painting
    public void eraseAll() {
        mPaths.clear(); // remove all paths
        canvasBitmap.eraseColor(drawingBackgroundColor); // clear the bitmap
        invalidate(); // refresh the screen
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        canvas.scale(scaleFactor, scaleFactor);

        canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);

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
        if (isBlurBrush) drawPaint.setMaskFilter(new BlurMaskFilter(8, BlurMaskFilter.Blur.NORMAL) );
        else drawPaint.setMaskFilter(new BlurMaskFilter(1, BlurMaskFilter.Blur.SOLID) );
        invalidate();

    }

    //Get brush color
    public int getPaintColor() {
        return paintColor;
    }

    //Set brush color
    public void setColor(int newColor) {
        paintColor = newColor;
        drawPaint.setColor(paintColor);
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
                if (isPaintBucket)
                {
                    Point _point = new  Point((int)touchX, (int)touchY);
                    FloodFill(canvasBitmap, _point, drawingBackgroundColor, paintColor);
                }
                else {
                    this.addPath(true);
                    drawPath.moveTo(touchX, touchY);
                }
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                if (isPaintBucket)
                {
                    Point _point = new  Point((int)touchX, (int)touchY);
                    FloodFill(canvasBitmap, _point, drawingBackgroundColor, Color.RED);
                }
                else {
                    drawPath.lineTo(touchX, touchY);
                }

                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                if (isPaintBucket)
                {
                    Point _point = new  Point((int)touchX, (int)touchY);
                    FloodFill(canvasBitmap, _point, drawingBackgroundColor, paintColor);
                }
                else {
                    drawPath.lineTo(touchX, touchY);
                    this.addPath(true);
                    drawCanvas.drawPath(drawPath, drawPaint);
                    drawPath.reset();
                    undonePaths.clear();
                    undonePaints.clear();
                }
                invalidate();
                break;
            default:
                return false;
        }
        invalidate();
        return true;
    }

    public void setPaintBucket() {
        if (isPaintBucket) isPaintBucket = false; else isPaintBucket = true;
    }

    public boolean getPaintBucket() {
        return isPaintBucket;
    }

    private void FloodFill(Bitmap bmp, Point pt, int targetColor, int replacementColor){
        Queue<Point> q = new LinkedList<Point>();
        q.add(pt);
        while (q.size() > 0) {
            Point n = q.poll();
            int pixel_color = bmp.getPixel(n.x, n.y);
            if (pixel_color != targetColor)
                continue;

            Point w = n, e = new Point(n.x + 1, n.y);
            while ((w.x > 0) && (bmp.getPixel(w.x, w.y) == targetColor)) {
                bmp.setPixel(w.x, w.y, replacementColor);
                if ((w.y > 0) && (bmp.getPixel(w.x, w.y - 1) == targetColor))
                    q.add(new Point(w.x, w.y - 1));
                if ((w.y < bmp.getHeight() - 1)
                        && (bmp.getPixel(w.x, w.y + 1) == targetColor))
                    q.add(new Point(w.x, w.y + 1));
                w.x--;
            }
            while ((e.x < bmp.getWidth() - 1)
                    && (bmp.getPixel(e.x, e.y) == targetColor)) {
                bmp.setPixel(e.x, e.y, replacementColor);

                if ((e.y > 0) && (bmp.getPixel(e.x, e.y - 1) == targetColor))
                    q.add(new Point(e.x, e.y - 1));
                if ((e.y < bmp.getHeight() - 1)
                        && (bmp.getPixel(e.x, e.y + 1) == targetColor))
                    q.add(new Point(e.x, e.y + 1));
                e.x++;
            }
        }
    }

    // save the current image to the Gallery
    public void saveImage() {
        // use "Doodlz" followed by current time as the image name
        final String name = "Doodlz" + System.currentTimeMillis() + ".jpg";

        // insert the image on the device
        String location = MediaStore.Images.Media.insertImage(
                getContext().getContentResolver(), canvasBitmap, name,
                "Doodlz Drawing");

        Toast message;
        if (location != null) {
            // display a message indicating that the image was saved
            message = Toast.makeText(getContext(),
                    R.string.message_saved,
                    Toast.LENGTH_SHORT);
        }
        else {
            // display a message indicating that there was an error saving
            message = Toast.makeText(getContext(),
                    R.string.message_error_saving, Toast.LENGTH_SHORT);
        }
        message.setGravity(Gravity.CENTER, message.getXOffset() / 2,
                message.getYOffset() / 2);
        message.show();
    }

}
