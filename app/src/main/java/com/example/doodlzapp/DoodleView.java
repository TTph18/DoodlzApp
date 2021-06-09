package com.example.doodlzapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.MaskFilter;
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
    public enum Tool {
        DEFAULT_BRUSH,
        BLUR_BRUSH,
        RECTANGLE,
        CIRCLE,
        PAINT_BUCKET,
        ERASER;
    }
    //public vars
    public Bitmap canvasBitmap;

    private Tool tool = Tool.DEFAULT_BRUSH;
    //private vars
    private Path drawPath;
    private Paint drawPaint, canvasPaint;
    private int paintColor = Color.BLACK;
    private Canvas drawCanvas;

    private int drawingBackgroundColor;

    private Integer currentBrushSize = 15;
    private ArrayList<Path> mPaths = new ArrayList<Path>();;
    private ArrayList<Path> undonePaths = new ArrayList<>();
    private ArrayList<Paint> undonePaints = new ArrayList<>();
    private ArrayList<Paint> mPaints = new ArrayList<Paint>();;

    // for Eraser
    private int baseColor = Color.WHITE;

    public static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123;

    private static float MIN_ZOOM = 1f;
    private static float MAX_ZOOM = 5f;

    private float scaleFactor = 1.f;
    private ScaleGestureDetector detector;

    private MaskFilter mBlur;
    private MaskFilter mDefault;

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
        this.addPath(false);

        drawPaint = new Paint();
        drawPath = new Path();

        drawPaint.setColor(paintColor);
        drawPaint.setAntiAlias(true);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);

        drawPaint.setPathEffect(new CornerPathEffect(10) );
        canvasPaint = new Paint(Paint.DITHER_FLAG);
        drawingBackgroundColor = 0;

        mBlur = new BlurMaskFilter(30, BlurMaskFilter.Blur.NORMAL);
        mDefault = new BlurMaskFilter(1, BlurMaskFilter.Blur.SOLID);

    }

    // clear the painting
    public void eraseAll() {
        mPaths.clear(); // remove all paths
        mPaints.clear();
        drawPath.reset();
        undonePaths.clear();
        undonePaints.clear();
        canvasBitmap.eraseColor(drawingBackgroundColor); // clear the bitmap
        invalidate(); // refresh the screen
    }

    @Override
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

        switch (this.tool)
        {
            case ERASER:
                drawPaint.setColor(baseColor);
                break;
            case BLUR_BRUSH:
                drawPaint.setMaskFilter(mBlur);
                break;
            case DEFAULT_BRUSH:
                drawPaint.setMaskFilter(mDefault);
                break;
            default:
                break;
        }
        invalidate();
    }

    public boolean enableUndo() {
        return mPaths.size() > 0;
    }

    public boolean enableRedo() {
        return undonePaths.size() > 0;
    }

    public void onClickUndo() {
        if (enableUndo()) {
            undonePaths.add(mPaths.remove(mPaths.size() - 1));
            undonePaints.add(mPaints.remove(mPaints.size() - 1));
            invalidate();
            if (enableUndo()) {
                undonePaths.add(mPaths.remove(mPaths.size() - 1));
                undonePaints.add(mPaints.remove(mPaints.size() - 1));
                invalidate();
            }
        }

    }

    public void onClickRedo() {
        if (enableRedo()) {
            mPaths.add(undonePaths.remove(undonePaths.size() - 1));
            mPaints.add(undonePaints.remove(undonePaints.size() - 1));
            invalidate();
            if (undonePaths.size() > 0) {
                mPaths.add(undonePaths.remove(undonePaths.size() - 1));
                mPaints.add(undonePaints.remove(undonePaints.size() - 1));
                invalidate();
            }
        }
    }

    //Touch event
    private float mX, mY;
    private static final float TOUCH_TOLERANCE = 4;

    private void touch_start(float x, float y) {

        switch (this.tool)
        {
            case PAINT_BUCKET:
                for (int i = 0; i < mPaths.size(); ++i) {
                    drawCanvas.drawPath(mPaths.get(i), mPaints.get(i));
                }
                break;
            case CIRCLE:
            case RECTANGLE:
                this.addPath(true);
                break;
            default:
                this.addPath(true);
                drawPath.moveTo(x, y);
                break;
        }
        mX = x;
        mY = y;
    }

    private void touch_move(float x, float y) {
        switch (this.tool)
        {
            case PAINT_BUCKET:
                for (int i = 0; i < mPaths.size(); ++i) {
                    drawCanvas.drawPath(mPaths.get(i), mPaints.get(i));
                }
                break;
            case CIRCLE:
                double distanceX = (double)(this.mX - x);
                double distanceY = (double)(this.mY - y);
                double radius    = Math.sqrt(Math.pow(Math.abs(distanceX/2), 2.0) + Math.pow(Math.abs(distanceY/2), 2.0));
                drawPath.reset();
                drawPath.addCircle((float)(this.mX - distanceX/2), (float)(this.mY - distanceY/2), (float)radius, Path.Direction.CCW);
                break;
            case RECTANGLE:
                drawPath.reset();
                drawPath.addRect(mX, mY, x, y, Path.Direction.CW);
                break;
            default:
                float dx = Math.abs(x - mX);
                float dy = Math.abs(y - mY);
                if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                    drawPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
                    mX = x;
                    mY = y;
                }
                break;
        }
    }

    private void touch_up() {
        switch (this.tool)
        {
            case PAINT_BUCKET:
                Point _point = new  Point((int)mX, (int)mY);
                FloodFill(canvasBitmap, _point, canvasBitmap.getPixel((int)mX, (int)mY), paintColor);
                break;
            case CIRCLE:
            case RECTANGLE:
                this.addPath(true);
                break;
            default:
                drawPath.lineTo(mX, mY);
                this.addPath(true);
                break;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        detector.onTouchEvent(event);

        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                touch_start(touchX, touchY);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touch_move(touchX, touchY);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touch_up();
                drawPath.reset();
                undonePaths.clear();
                undonePaints.clear();
                invalidate();
                break;
            default:
                return false;
        }
        invalidate();
        return true;
    }

    private void FloodFill(Bitmap bmp, Point pt, int targetColor, int replacementColor){
        Queue<Point> q = new LinkedList<Point>();
        q.add(pt);
        while (q.size() > 0) {
            Point n = q.poll();
            if (bmp.getPixel(n.x, n.y) != targetColor)
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

        setDrawingCacheEnabled(true);
        setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        Bitmap bitmap = getDrawingCache();
        // insert the image on the device
        String location = MediaStore.Images.Media.insertImage(
                getContext().getContentResolver(), bitmap, name,
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

    // return the painted line's width
    public int getBrushWidth() {
        return (int)drawPaint.getStrokeWidth();
    }

    public void setBrushWidth(int size) {
        currentBrushSize = size;
        drawPaint.setStrokeWidth(currentBrushSize);
        invalidate();
    }

    public Tool getTool() {
        return this.tool;
    }

    public void setTool(Tool _tool) {
        this.tool = _tool;
    }
}
