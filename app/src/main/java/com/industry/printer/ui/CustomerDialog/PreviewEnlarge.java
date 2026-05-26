package com.industry.printer.ui.CustomerDialog;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;

import com.industry.printer.R;
import com.industry.printer.Utils.ConfigPath;
import com.industry.printer.Utils.Debug;

import java.io.File;

// H.M.Wang 2026-5-15 可以放大显示预览图的类
public class PreviewEnlarge {
    public static final String TAG = PreviewEnlarge.class.getSimpleName();

    private Context mContext;
    private String mObjPath;

    private PopupWindow mPopupWindow;
    private ImageView mPreviewView;
    private Bitmap mBitmap;

    public PreviewEnlarge(Context ctx, String path) {
        mContext = ctx;
        mObjPath = path;
    }

    public void show(final View v) {
        View popupView = LayoutInflater.from(mContext).inflate(R.layout.preview_enlarge, null);

        mPopupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true);
        mPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#CC000000")));
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setTouchable(true);
        mPopupWindow.update();

        ImageView cancel = popupView.findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPopupWindow.dismiss();
            }
        });

        mPreviewView = popupView.findViewById(R.id.preview_img);
        try {
            String prevFile = ConfigPath.getTlkDir(mObjPath) + "/1A.bmp";
            if(!new File(prevFile).exists()) {
                prevFile = ConfigPath.getTlkDir(mObjPath) + "/1.bmp";
            }
Debug.d(TAG, "prevFile: " + prevFile);
            mBitmap = BitmapFactory.decodeFile(prevFile);
Debug.d(TAG, "mBitmap: " + mBitmap.getWidth() + ", " + mBitmap.getHeight());
            mPreviewView.setImageBitmap(mBitmap);
            mPreviewView.setOnTouchListener(new HTFTouchListener());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mPopupWindow.showAtLocation(v, Gravity.NO_GRAVITY, 0, 0);
    }

        private class HTFTouchListener implements View.OnTouchListener {
            private final int MODE_UNKNOWN = 0;
            private final int MODE_TRANSLATE = 1;
            private final int MODE_SCALE = 2;

            private double mLenStart = 0;
            private int mMode = MODE_UNKNOWN;
            private float mStartPosX = 0;
            private float mStartPosY = 0;
            private GestureDetector mGestureDetector;

            public HTFTouchListener() {
                super();
                mGestureDetector = new GestureDetector(mContext, new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true;
                    }
                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        mPreviewView.setScaleType(ImageView.ScaleType.FIT_CENTER);
//                        mPreviewView.invalidate();
                        return true; // 返回true表示事件已被处理
                    }
                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent e) {
                        return super.onSingleTapUp(e);
                    }
                });
            }

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ImageView view = (ImageView)v;

                Matrix matrix = view.getImageMatrix();
                float[] values = new float[9];
                matrix.getValues(values);

                if(view.getScaleType() != ImageView.ScaleType.MATRIX) {
                    view.setScaleType(ImageView.ScaleType.MATRIX);
                }

                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_POINTER_DOWN:
                        if(event.getPointerCount() == 2) {
                            int xlen = Math.abs((int)event.getX(0) - (int)event.getX(1));
                            int ylen = Math.abs((int)event.getY(0) - (int)event.getY(1));
                            mLenStart = Math.sqrt((double)xlen*xlen + (double)ylen*ylen);
                            mStartPosX = (int)((event.getX(0) + event.getX(1))/2);
                            mStartPosY = (int)((event.getY(0) + event.getY(1))/2);
                            mMode = MODE_SCALE;
                        }
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                        if(event.getPointerCount() == 2 && mMode == MODE_SCALE) {
                            float dx = 1.0f;
                            float minScale = Math.min(1.0f * mPreviewView.getWidth() / mBitmap.getWidth(), 1.0f * mPreviewView.getHeight() / mBitmap.getHeight());
                            float maxScale = Math.max(minScale, 4);
                            if(values[Matrix.MSCALE_X] < minScale) dx = minScale / values[Matrix.MSCALE_X];
                            if(values[Matrix.MSCALE_X] > maxScale) dx = maxScale / values[Matrix.MSCALE_X];
                            matrix.postScale(dx, dx, mStartPosX, mStartPosY);
                            view.setImageMatrix(matrix);
                            view.invalidate();
                        }
                        mMode = MODE_UNKNOWN;
                        break;
                    case MotionEvent.ACTION_DOWN:
                        mMode = MODE_TRANSLATE;
                        mStartPosX = event.getX();
                        mStartPosY = event.getY();
                        break;
                    case MotionEvent.ACTION_UP:
                        float cx = 0, cy = 0;
                        if(values[Matrix.MTRANS_X] > 0 && values[Matrix.MTRANS_X] + mBitmap.getWidth() * values[Matrix.MSCALE_X] > mPreviewView.getWidth()) {     // 左面有空，右面还出去了，拉回来
                            cx = -Math.min(values[Matrix.MTRANS_X], values[Matrix.MTRANS_X] + mBitmap.getWidth() * values[Matrix.MSCALE_X] - mPreviewView.getWidth());
                        }
                        if(values[Matrix.MTRANS_X] < 0 && values[Matrix.MTRANS_X] + mBitmap.getWidth() * values[Matrix.MSCALE_X] < mPreviewView.getWidth()) {     // 右边有空，左面还出去了，拉回来
                            cx = -Math.max(values[Matrix.MTRANS_X], values[Matrix.MTRANS_X] + mBitmap.getWidth() * values[Matrix.MSCALE_X] - mPreviewView.getWidth());
                        }
                        if(values[Matrix.MTRANS_Y] > 0 && values[Matrix.MTRANS_Y] + mBitmap.getHeight() * values[Matrix.MSCALE_Y] > mPreviewView.getHeight()) {
                            cy = -Math.min(values[Matrix.MTRANS_Y], values[Matrix.MTRANS_Y] + mBitmap.getHeight() * values[Matrix.MSCALE_Y] - mPreviewView.getHeight());
                        }
                        if(values[Matrix.MTRANS_Y] < 0 && values[Matrix.MTRANS_Y] + mBitmap.getHeight() * values[Matrix.MSCALE_Y] < mPreviewView.getHeight()) {
                            cy = -Math.max(values[Matrix.MTRANS_Y], values[Matrix.MTRANS_Y] + mBitmap.getHeight() * values[Matrix.MSCALE_Y] - mPreviewView.getHeight());
                        }
                        matrix.postTranslate(cx, cy);
                        view.setImageMatrix(matrix);
                        view.invalidate();
                        mMode = MODE_UNKNOWN;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if(mMode == MODE_TRANSLATE) {
                            cx = event.getX() - mStartPosX;
                            cy = event.getY() - mStartPosY;
                            if(Math.abs(cx) > 5.0f || Math.abs(cy) > 5.0f) {
                                matrix.postTranslate(cx, cy);
                                view.setImageMatrix(matrix);
                                view.invalidate();
                                mStartPosX = event.getX();
                                mStartPosY = event.getY();
                            }
                        } else if(mMode == MODE_SCALE && event.getPointerCount() == 2) {
                            int xlen = Math.abs((int)event.getX(0) - (int)event.getX(1));
                            int ylen = Math.abs((int)event.getY(0) - (int)event.getY(1));
                            double nLenEnd = Math.sqrt((double)xlen*xlen + (double)ylen * ylen);
                            float dx = (float)(nLenEnd/mLenStart);
                            matrix.postScale(dx, dx,  mStartPosX, mStartPosY);
                            view.setImageMatrix(matrix);
                            view.invalidate();
                            mLenStart = nLenEnd;
                        }
                        break;
                }

                return mGestureDetector.onTouchEvent(event);
            }
        }
    }
