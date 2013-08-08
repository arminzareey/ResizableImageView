package com.example.resizable;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.net.Uri;
import android.widget.ImageView;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

/**
 * イメージの拡大縮小、移動ができるImageView拡張サンプル。
 *
 * 以下のサイトを参考にしました。
 * @see http://tsukaayapontan.web.fc2.com/doc/customview/customview.html
 */
public class ResizableImageView extends ImageView implements OnTouchListener {

    private static final String TAG = "ResizableImageView";

    private enum ACTION_MODE {
        NONE,DRAG,ZOOM;
    }

    private enum LOAD_TYPE {
        RESOURCE, URI, FILE, NONE;
    }

    private static final int MATRIX_VALUES_NUM = 9;
    private static final float DEFAULT_MAX_SCALE = 5.0f;
    private static final float DEFAULT_SCALE = 1.0f;
    private static final float MIN_POINTER_DISTANCE = 10f;

    private Matrix imageMatrix = new Matrix();
    private Matrix savedImageMatrix = new Matrix();

    private PointF movePoint = new PointF();
    private PointF midPoint = new PointF();

    private float pointerDistance = 0.0f;
    private float initialScale = DEFAULT_SCALE;
    private float maxScale = DEFAULT_MAX_SCALE;

    private ACTION_MODE actionMode = ACTION_MODE.NONE;
    private LOAD_TYPE loadType = LOAD_TYPE.NONE;

    private Bitmap bitmap = null;
    private String filename;
    private int imageResourceId;
    private Uri imageUri;

    public ResizableImageView(Context context) {
        super(context);
        init(context);
    }

    public ResizableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ResizableImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        super.setOnTouchListener(this);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (super.getWidth() == 0) {
            return;
        }

        if (this.bitmap != null) {
            this.bitmap.recycle();
        }

        initialDraw();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Matrix matrix = super.getImageMatrix();
        float[] values = new float[MATRIX_VALUES_NUM];

        if (actionMode == ACTION_MODE.ZOOM) {
            super.onDraw(canvas);
            matrix = super.getImageMatrix();
        }
        matrix.getValues(values);

//        setCenteringY(this.bitmap, values[Matrix.MSCALE_X], matrix);
//        chkPositionX(this.bitmap, values[Matrix.MSCALE_X], matrix);
//        chkPositionY(this.bitmap, values[Matrix.MSCALE_Y], matrix);

        super.setImageMatrix(matrix);
        super.onDraw(canvas);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        boolean ret = false;
        int actionCode = event.getAction() & MotionEvent.ACTION_MASK;
        ret = onTouchDragEvent(event, actionCode);
        if (!ret) {
            ret = onTouchPointerEvent(event, actionCode);
        }
        return ret;
    }

    private boolean onTouchDragEvent(MotionEvent event, int actionCode) {
        boolean ret = false;
        switch (actionCode) {
        case MotionEvent.ACTION_DOWN:
            actionDown(event);
            this.actionMode = ACTION_MODE.DRAG;
            ret = true;
            break;
        case MotionEvent.ACTION_MOVE:
            if (this.actionMode == ACTION_MODE.DRAG) {
                actionMove(event);
                this.movePoint = new PointF(event.getX(), event.getY());
                ret = true;
            }
            break;
        case MotionEvent.ACTION_UP:
            this.actionMode = ACTION_MODE.NONE;
            this.savedImageMatrix.set(super.getImageMatrix());
            ret = true;
            break;
        }
        return ret;
    }

    private boolean onTouchPointerEvent(MotionEvent event, int actionCode) {
        boolean ret = false;
        switch (actionCode) {
        case MotionEvent.ACTION_POINTER_DOWN:
            actionPointerDown(event);
            this.actionMode = ACTION_MODE.ZOOM;
            ret = true;
            break;
        case MotionEvent.ACTION_MOVE:
            if (this.actionMode == ACTION_MODE.ZOOM) {
                ret = actionPointerMove(event);
            }
            break;
        case MotionEvent.ACTION_POINTER_UP:
            this.savedImageMatrix.set(super.getImageMatrix());
            this.actionMode = ACTION_MODE.NONE;
            ret = true;
            break;
        }
        return ret;
    }

    private boolean actionDown(MotionEvent event) {
        this.movePoint.set(event.getX(), event.getY());
        this.imageMatrix.set(this.savedImageMatrix);
        return true;
    }

    private boolean actionMove(MotionEvent event) {
        PointF currentPoint = new PointF(event.getX(), event.getY());

        float distX = currentPoint.x - this.movePoint.x;
        float distY = currentPoint.y - this.movePoint.y;

        this.imageMatrix.postTranslate(distX, distY);
        super.setImageMatrix(this.imageMatrix);
        return true;
    }

    private boolean actionPointerDown(MotionEvent event) {
        float distance = getPointerDistance(event);
        if (distance < MIN_POINTER_DISTANCE) {
            return false;
        }
        this.pointerDistance = distance;

        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);

        this.midPoint.set(x, y);
        this.savedImageMatrix.set(super.getImageMatrix());

        return true;
    }

    private boolean actionPointerMove(MotionEvent event) {
        this.imageMatrix.set(this.savedImageMatrix);

        float currentScale = getMatrixScale(this.imageMatrix);
        float scale = getScale(event);

        float tmpScale = scale * currentScale;
        if (tmpScale < this.initialScale) {
            return false;
        }
        if (tmpScale > this.maxScale) {
            return false;
        }

//        this.imageMatrix.postScale(scale, scale);// スクリーンの左上(0, 0)が基点
        this.imageMatrix.postScale(scale, scale, this.midPoint.x, this.midPoint.y); // 第３，４引数が基点
        super.setImageMatrix(this.imageMatrix);
        return true;
    }

    public void setInitialScale(float scale) {
        this.initialScale = scale;
    }

    public void setMaxScale(float scale) {
        this.maxScale = scale;
    }

    public void setImage(String filename) {
        this.loadType = LOAD_TYPE.FILE;
        this.filename = filename;

        if (super.getWidth() == 0) {
            return;
        }

        initialDraw();
    }

    public void setImage(int resId) {
        this.loadType = LOAD_TYPE.RESOURCE;
        this.imageResourceId = resId;

        if (super.getWidth() == 0) {
            return;
        }

        initialDraw();
    }

    public void setImage(Uri uri) {
        this.loadType = LOAD_TYPE.URI;
        this.imageUri = uri;

        if (super.getWidth() == 0) {
            return;
        }

        initialDraw();
    }

    protected Bitmap createBitmapFrimResource(int resId) {
        int width = super.getWidth();
        int height = super.getHeight();

        Options options = new BitmapFactory.Options();
        decodeResource(resId, options, true);

        int imgWidth = options.outWidth;
        int imgHeight = options.outHeight;

        setSampleSize(imgWidth, imgHeight, width, height, options);

        return decodeResource(resId, options, false);
    }

    protected Bitmap createBitmapFromUri(Uri uri) {
        int width = super.getWidth();
        int height = super.getHeight();

        Options options = new BitmapFactory.Options();
        decodeFile(uri.getPath(), options, true);

        int imgWidth = options.outWidth;
        int imgHeight = options.outHeight;

        setSampleSize(imgWidth, imgHeight, width, height, options);

        return decodeFile(uri.getPath(), options, false);
    }

    protected Bitmap createBitmap(String filename) {
        int width = super.getWidth();
        int height = super.getHeight();

        BitmapFactory.Options options = new BitmapFactory.Options();
        decodeFile(filename, options, true);

        int imgWidth = options.outWidth;
        int imgHeight = options.outHeight;

        setSampleSize(imgWidth, imgHeight, width, height, options);

        return decodeFile(filename, options, false);
    }

    protected void setSampleSize(int width, int height, int maxWidth, int maxHeight, Options opts) {
        if (chkSize(width, height, maxWidth, maxHeight)) {
            int scale = getBmpImageScale(width, height, maxWidth, maxHeight);
            opts.inSampleSize = scale;
        }
    }

    protected Bitmap decodeResource(int resId, Options opts, boolean decodeBounds) {
        opts.inJustDecodeBounds = decodeBounds;
        return BitmapFactory.decodeResource(getResources(), resId, opts);
    }

    protected Bitmap decodeFile(String file, Options opts, boolean decodeBounds) {
        opts.inJustDecodeBounds = decodeBounds;
        return BitmapFactory.decodeFile(file, opts);
    }

    protected Bitmap loadBitmap() {
        Bitmap bmp = null;
        switch (loadType) {
        case FILE:
            bmp = createBitmap(this.filename);
            break;
        case RESOURCE:
            bmp = createBitmapFrimResource(this.imageResourceId);
            break;
        case URI:
            bmp = createBitmapFromUri(this.imageUri);
            break;
        case NONE:
            bmp = null;
            break;
        }
        return bmp;
    }

    private void initialDraw() {
        if (this.bitmap != null) {
            this.bitmap.recycle();
        }

        this.bitmap = loadBitmap();
        super.setImageBitmap(this.bitmap);

        float[] values = new float[MATRIX_VALUES_NUM];
        this.imageMatrix.getValues(values);

        this.initialScale = getInitialScale(this.bitmap);

        setCenteringY(this.bitmap, this.initialScale, this.imageMatrix);
//        setValueToImageMatrix(Matrix.MTRANS_X, 0f, this.imageMatrix);

        if (values[Matrix.MSCALE_X] == DEFAULT_SCALE) {
            this.imageMatrix.postScale(this.initialScale, this.initialScale);
            this.savedImageMatrix.set(this.imageMatrix);
        }

        super.setImageMatrix(this.imageMatrix);
    }

    private boolean chkSize(int imageWidth, int imageHeight, int maxWidth, int maxHeight) {
        boolean isResize = true;

        if(imageWidth <= maxWidth && imageHeight <= maxHeight) {
            isResize = false;
        }
        return isResize;
    }

    private int getBmpImageScale(int imageWidth, int imageHeight, int maxWidth, int maxHeight) {
        int retScale = 1;

        float scaleX = (imageWidth / maxWidth) + 1.0f;
        float scaleY = (imageHeight / maxHeight) + 1.0f;

        retScale = Math.max((int)scaleX, (int)scaleY);

        return retScale;
    }

    private void setCenteringY(Bitmap bitmap, float scale, Matrix matrix) {
        if (bitmap == null) {
            return;
        }

        float viewHeight = (float) super.getHeight();
        float imageHeight = (float) bitmap.getHeight();
        imageHeight *= scale;

        float[] values = new float[MATRIX_VALUES_NUM];
        matrix.getValues(values);

        float cal = viewHeight - imageHeight;
        if (cal > 0) {
            cal /= 2.0f;
            setValueToImageMatrix(Matrix.MTRANS_Y, cal, matrix);
        }
    }

    private void chkPositionX(Bitmap bitmap, float scale, Matrix matrix) {
        if (bitmap == null) {
            return;
        }

        float viewWidth = (float) super.getWidth();
        float imageWidth = (float) bitmap.getWidth();
        imageWidth *= scale;

        float[] values = new float[MATRIX_VALUES_NUM];
        matrix.getValues(values);

        float currentX = values[Matrix.MTRANS_X];

        if (currentX > 0) {
            setValueToImageMatrix(Matrix.MTRANS_X, 0f, matrix);
        } else if ((imageWidth + currentX) < viewWidth) {
            float cal = values[Matrix.MTRANS_X] + (viewWidth - (imageWidth + currentX));
            setValueToImageMatrix(Matrix.MTRANS_X, cal, matrix);
        }
    }

    private void chkPositionY(Bitmap bitmap, float scale, Matrix matrix) {
        if (bitmap == null) {
            return;
        }

        float viewHeight = (float) super.getHeight();
        float imageHeight = (float) bitmap.getHeight();
        imageHeight *= scale;

        float[] values = new float[MATRIX_VALUES_NUM];
        matrix.getValues(values);

        float currentY = values[Matrix.MTRANS_Y];

        if (viewHeight > imageHeight) {
            return;
        }

        if (currentY > 0) {
            // 画面左に余白あり
            setValueToImageMatrix(Matrix.MTRANS_Y, 0f, matrix);
        } else if ((imageHeight + currentY) < viewHeight) {
            // 画面右に余白あり
            float cal = values[Matrix.MTRANS_Y] + (viewHeight - (imageHeight + currentY));
            setValueToImageMatrix(Matrix.MTRANS_Y, cal, matrix);
        }
    }

    private void setValueToImageMatrix(int index, float value, Matrix matrix) {
        float[] values = new float[MATRIX_VALUES_NUM];
        matrix.getValues(values);

        values[index] = value;
        matrix.setValues(values);
    }

    private float getInitialScale(Bitmap bitmap) {
        if (bitmap == null) {
            return DEFAULT_SCALE;
        }

        float viewWidth = super.getWidth();
        float viewHeight = super.getHeight();

        float imgWidth = bitmap.getWidth();
        float imgHeight = bitmap.getHeight();

        float scaleX = viewWidth / imgWidth;
        float scaleY = viewHeight / imgHeight;

        if (scaleX > 1 || scaleY > 1) {
            return DEFAULT_SCALE;
        }
        return Math.min(scaleX, scaleY);
    }

    private float getMatrixScale(Matrix matrix) {
        float[] values = new float[MATRIX_VALUES_NUM];
        matrix.getValues(values);

        float currentScale = values[Matrix.MSCALE_X];
        if(currentScale == 0f) {
            return 1f;
        }
        return currentScale;
    }

    private float getScale(MotionEvent event) {
        float distance = getPointerDistance(event);
        return distance / this.pointerDistance;
    }

    @SuppressLint("FloatMath")
    private float getPointerDistance(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return FloatMath.sqrt(x*x+y*y);
    }


}
