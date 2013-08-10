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
import android.widget.TextView;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

/**
 * イメージの拡大縮小、移動ができるImageView拡張サンプル。
 *
 * 初期表示時に画像をスクリーン中央に描画。
 * ドラッグで画像を移動。
 * ピンチイン/ピンチアウトで拡大縮小。
 *
 * TODO:
 *   拡大縮小する際の基点を決める。
 *     →画像中心部
 *
 * 以下のサイトを参考にしました。
 * @see http://tsukaayapontan.web.fc2.com/doc/customview/customview.html
 */
public class ResizableImageView extends ImageView implements OnTouchListener {

    private static final String TAG = "ResizableImageView";

    /** 動作モード */
    private enum ACTION_MODE {
        NONE,DRAG,ZOOM;
    }

    /** リソース種類 */
    private enum LOAD_TYPE {
        RESOURCE, URI, FILE, NONE;
    }

    /** Matrix要素index数 */
    private static final int MATRIX_VALUES_NUM = 9;
    /** デフォルト最小倍率 */
    private static final float DEFAULT_MIN_SCALE = 1.0f;
    /** デフォルト最大倍率 */
    private static final float DEFAULT_MAX_SCALE = 5.0f;
    /** ピンチ操作最小距離 */
    private static final float MIN_POINTER_DISTANCE = 10f;

    /** 表示中の画像Matrix */
    private Matrix imageMatrix = new Matrix();
    /** 直前の操作したMatrix */
    private Matrix savedImageMatrix = new Matrix();

    /** ドラッグ開始点 */
    private PointF dragStartPoint = new PointF();
    /** ピンチ中心点 */
    private PointF pinchPoint = new PointF();
    /** ピンチ移動距離 */
    private float pinchDistance = 0.0f;
    /** 最小倍率 */
    private float minScale = DEFAULT_MIN_SCALE;
    /** 最大倍率 */
    private float maxScale = DEFAULT_MAX_SCALE;
    /** モード */
    private ACTION_MODE actionMode = ACTION_MODE.NONE;
    /** リソース */
    private LOAD_TYPE loadType = LOAD_TYPE.NONE;

    private Bitmap bitmap = null;
    private String filename;
    private int imageResourceId;
    private Uri imageUri;

    /**
     * コンストラクタ.
     * @param context Context
     */
    public ResizableImageView(Context context) {
        super(context);
        init(context);
    }

    /**
     * コンストラクタ.
     * @param context Context
     * @param attrs AttributeSet
     */
    public ResizableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * コンストラクタ.
     * @param context Context
     * @param attrs AttributeSet
     * @param defStyle int
     */
    public ResizableImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    /**
     * 初期処理
     * @param context Context
     */
    private void init(Context context) {
        super.setOnTouchListener(this);
    }

    /*
     * ビューの準備が終わったらコールされるイベント.
     * @see android.view.View#onLayout(boolean, int, int, int, int)
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (super.getWidth() == 0) {
            return;
        }

        if (this.bitmap != null) {
            this.bitmap.recycle();
        }

        initialImageDraw();
    }

    /*
     * 画像描画イベント.
     * @see android.widget.ImageView#onDraw(android.graphics.Canvas)
     */
    @Override
    protected void onDraw(Canvas canvas) {
        Matrix matrix = super.getImageMatrix();
        float[] values = new float[MATRIX_VALUES_NUM];

        if (actionMode == ACTION_MODE.ZOOM) {
            super.onDraw(canvas);
            matrix = super.getImageMatrix();
        }
        matrix.getValues(values);

        // TODO 位置調整
        showDebug();

        super.setImageMatrix(matrix);
        super.onDraw(canvas);
    }

    /*
     * タッチイベント.
     * @see android.view.View.OnTouchListener#onTouch(android.view.View, android.view.MotionEvent)
     */
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

    /**
     * ドラッグ操作イベント.
     * @param event MotionEvent
     * @param actionCode ポインター動作コード
     * @return
     */
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
                this.dragStartPoint = new PointF(event.getX(), event.getY());
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

    /**
     * ピンチ操作イベント.
     * @param event MotionEvent
     * @param actionCode ポインター動作コード
     * @return
     */
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

    /**
     * ドラッグ開始.
     * @param event　MotionEvent
     * @return
     */
    private boolean actionDown(MotionEvent event) {
        // 開始点を保持
        this.dragStartPoint.set(event.getX(), event.getY());

        this.imageMatrix.set(this.savedImageMatrix);

        return true;
    }

    /**
     * ドラッグ中.
     * @param event MotionEvent
     * @return
     */
    private boolean actionMove(MotionEvent event) {
        PointF currentPoint = new PointF(event.getX(), event.getY());

        // 移動距離を取得
        float distX = currentPoint.x - this.dragStartPoint.x;
        float distY = currentPoint.y - this.dragStartPoint.y;

        // 移動
        this.imageMatrix.postTranslate(distX, distY);
        super.setImageMatrix(this.imageMatrix);

        return true;
    }

    /**
     * ピンチ開始.
     * @param event MotionEvent
     * @return
     */
    private boolean actionPointerDown(MotionEvent event) {
        float distance = getPointerSqrt(event);
        if (distance < MIN_POINTER_DISTANCE) {
            return false;
        }
        this.pinchDistance = distance;

        float[] values = new float[MATRIX_VALUES_NUM];
        this.imageMatrix.getValues(values);

        float iw = this.bitmap.getWidth() * values[Matrix.MSCALE_X] / 2;
        float ih = this.bitmap.getHeight() * values[Matrix.MSCALE_Y] / 2;

        this.pinchPoint.set(iw+values[Matrix.MTRANS_X], ih+values[Matrix.MTRANS_Y]);
        this.savedImageMatrix.set(super.getImageMatrix());

        return true;
    }

    /**
     * ピンチ中.
     * @param event MotionEvent
     * @return
     */
    private boolean actionPointerMove(MotionEvent event) {
        this.imageMatrix.set(this.savedImageMatrix);

        float currentScale = getMatrixScale(this.imageMatrix);
        float scale = calcPinchDistance(event);

        float tmpScale = scale * currentScale;
        if (tmpScale < this.minScale) {
            return false;
        }
        if (tmpScale > this.maxScale) {
            return false;
        }

        this.imageMatrix.postScale(scale, scale, this.pinchPoint.x, this.pinchPoint.y);
        super.setImageMatrix(this.imageMatrix);
        return true;
    }

    /**
     * 最小倍率を設定.
     * @param scale 倍率
     */
    public void setMinScale(float scale) {
        this.minScale = scale;
    }

    /**
     * 最大倍率を設定.
     * @param scale 倍率
     */
    public void setMaxScale(float scale) {
        this.maxScale = scale;
    }

    /**
     * 画像ファイルを設定.
     * @param filename ファイルパス
     */
    public void setImageSource(String filename) {
        this.loadType = LOAD_TYPE.FILE;
        this.filename = filename;

        initialImageDraw();
    }

    /**
     * リソースから画像ファイルを設定.
     * @param resId リソースID
     */
    public void setImageSource(int resId) {
        this.loadType = LOAD_TYPE.RESOURCE;
        this.imageResourceId = resId;

        initialImageDraw();
    }

    /**
     * URIから画像ファイルを設定.
     * @param uri URI
     */
    public void setImageSource(Uri uri) {
        this.loadType = LOAD_TYPE.URI;
        this.imageUri = uri;

        initialImageDraw();
    }

    /**
     * 初期描画処理.
     * スクリーンサイズに合わせて画像を描画.
     */
    private void initialImageDraw() {
        if (super.getWidth() == 0) {
            return;
        }

        if (this.bitmap != null) {
            this.bitmap.recycle();
        }

        this.bitmap = loadBitmap();
        super.setImageBitmap(this.bitmap);

        float[] values = new float[MATRIX_VALUES_NUM];
        this.imageMatrix.getValues(values);

        float scale = getInitialScale(this.bitmap);
        if (scale > DEFAULT_MIN_SCALE) {
            this.minScale = DEFAULT_MIN_SCALE;
        } else {
            this.minScale = scale;
        }

        if (scale > DEFAULT_MAX_SCALE) {
            this.maxScale = scale;
        }

        setCenter(this.bitmap, scale, this.imageMatrix);

        if (values[Matrix.MSCALE_X] == DEFAULT_MIN_SCALE) {
            this.imageMatrix.postScale(scale, scale);
            this.savedImageMatrix.set(this.imageMatrix);
        }

        super.setImageMatrix(this.imageMatrix);
    }

    /**
     * スクリーンの中心位置をMatrixへ設定する.
     * @param bitmap Bitmap
     * @param scale 倍率
     * @param matrix　Matrix
     */
    private void setCenter(Bitmap bitmap, float scale, Matrix matrix) {
        if (bitmap == null) {
            return;
        }

        float viewWidth = super.getWidth();
        float viewHeight = super.getHeight();
        float imgWidth = bitmap.getWidth() * scale;
        float imgHeight = bitmap.getHeight() * scale;

        float[] values = new float[MATRIX_VALUES_NUM];
        matrix.getValues(values);

        float distX = viewWidth - imgWidth;
        if (distX > 0) {
            distX = distX / 2;
        }

        float distY = viewHeight - imgHeight;
        if (distY > 0) {
            distY = distY / 2;
        }

        values[Matrix.MTRANS_X] = distX;
        values[Matrix.MTRANS_Y] = distY;
        matrix.setValues(values);
    }

    /**
     * 画像ファイルを読み込む.
     * @return Bitmap
     */
    private Bitmap loadBitmap() {
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

    private Bitmap createBitmapFrimResource(int resId) {
        Options options = new BitmapFactory.Options();
        decodeResource(resId, options, true);

//        setSampleSize(options);

        return decodeResource(resId, options, false);
    }

    private Bitmap createBitmapFromUri(Uri uri) {
        Options options = new BitmapFactory.Options();
        decodeFile(uri.getPath(), options, true);

//        setSampleSize(options);

        return decodeFile(uri.getPath(), options, false);
    }

    private Bitmap createBitmap(String filename) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        decodeFile(filename, options, true);

//        setSampleSize(options);

        return decodeFile(filename, options, false);
    }

    private Bitmap decodeResource(int resId, Options opts, boolean decodeBounds) {
        opts.inJustDecodeBounds = decodeBounds;
        return BitmapFactory.decodeResource(getResources(), resId, opts);
    }

    private Bitmap decodeFile(String file, Options opts, boolean decodeBounds) {
        opts.inJustDecodeBounds = decodeBounds;
        return BitmapFactory.decodeFile(file, opts);
    }

    private void setSampleSize(Options opts) {
        int width = opts.outWidth;
        int height = opts.outHeight;
        int maxWidth = super.getWidth();
        int maxHeight = super.getHeight();

        if (chkSize(width, height, maxWidth, maxHeight)) {
            int sample = getBmpImageSample(width, height, maxWidth, maxHeight);
            opts.inSampleSize = sample;
        }
    }

    private boolean chkSize(int imageWidth, int imageHeight, int maxWidth, int maxHeight) {
        boolean isResize = true;

        if(imageWidth <= maxWidth && imageHeight <= maxHeight) {
            isResize = false;
        }
        return isResize;
    }

    private int getBmpImageSample(int imageWidth, int imageHeight, int maxWidth, int maxHeight) {
        int retSample = 1;

        // 画面比を算出
        float scaleX = (imageWidth / maxWidth) + 1.0f;
        float scaleY = (imageHeight / maxHeight) + 1.0f;
        retSample = Math.max((int)scaleX, (int)scaleY);
        return retSample;
    }

    /**
     * 初期表示時の表示倍率を取得.
     * @param bitmap Bitmap
     * @return 初期表示倍率
     */
    private float getInitialScale(Bitmap bitmap) {
        if (bitmap == null) {
            return DEFAULT_MIN_SCALE;
        }

        float viewWidth = super.getWidth();
        float viewHeight = super.getHeight();

        float imgWidth = bitmap.getWidth();
        float imgHeight = bitmap.getHeight();

        float ratioX = viewWidth / imgWidth;
        float ratioY = viewHeight / imgHeight;

        // スクリーンに収まる最小値を求める
        return Math.min(ratioX, ratioY);
    }

    /**
     * Matrixに設定されている倍率(Matrix.MSCALE_X)を取得.
     * @param matrix Matrix
     * @return
     */
    private float getMatrixScale(Matrix matrix) {
        float[] values = new float[MATRIX_VALUES_NUM];
        matrix.getValues(values);

        float currentScale = values[Matrix.MSCALE_X];
        if(currentScale == 0f) {
            return this.minScale;
        }
        return currentScale;
    }

    /**
     * ピンチ移動距離を計算.
     * @param event MotionEvent
     * @return
     */
    private float calcPinchDistance(MotionEvent event) {
        float distance = getPointerSqrt(event);
        return distance / this.pinchDistance;
    }

    /**
     * ピンチ時の２点間を計算
     * @param event
     * @return
     */
    @SuppressLint("FloatMath")
    private float getPointerSqrt(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return FloatMath.sqrt(x*x+y*y);
    }

    //
    // for debug
    /////////////////////////////////////////////
    private TextView textview;

    public void setTextView(TextView textView) {
        this.textview = textView;
    }

    private void showDebug() {
        if (this.textview == null) {
            return;
        }

        float[] values = new float[MATRIX_VALUES_NUM];
        super.getImageMatrix().getValues(values);

        float w = this.bitmap.getWidth()*values[Matrix.MSCALE_X];
        float h = this.bitmap.getHeight()*values[Matrix.MSCALE_Y];

        StringBuilder sb = new StringBuilder();

        append(sb, "X0="+values[Matrix.MTRANS_X]);
        append(sb, "Y0="+values[Matrix.MTRANS_Y]);
        append(sb, "X1="+(w+values[Matrix.MTRANS_X]));
        append(sb, "Y1="+(h+values[Matrix.MTRANS_Y]));
        append(sb, "W="+w);
        append(sb, "H="+h);
        append(sb, "Scale="+values[Matrix.MSCALE_X]);

        this.textview.setText(sb);
    }

    private void append(StringBuilder sb, String str) {
        if (sb.length() > 0) {
            sb.append("\n");
        }
        sb.append(str);
    }
}
