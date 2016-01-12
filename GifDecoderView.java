package com.sohu.tv.ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.sohu.tv.control.util.LogManager;
import com.sohu.tv.ui.util.GifHelper;

public class GifDecoderView extends ImageView {

    private static final String TAG = GifDecoderView.class.getSimpleName();
    private boolean mIsPlayingGif = false;
    private GifDecoder mGifDecoder;
    private Bitmap mTmpBitmap;
    final Handler mHandler = new Handler();
    private Context context;

    final Runnable mUpdateResults = new Runnable() {
        public void run() {
            if (mTmpBitmap != null && !mTmpBitmap.isRecycled()) {
                GifDecoderView.this.setImageBitmap(mTmpBitmap);
            }
        }
    };

    public GifDecoderView(Context context) {
        super(context);
        this.context = context;
    }

    public GifDecoderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public GifDecoderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
    }

    public void playGif() {
        LogManager.d(TAG, "GifDecoderView playGif ---- 1");
        mGifDecoder = GifHelper.getGifDecoder(context);

        mIsPlayingGif = true;
        LogManager.d(TAG, "GifDecoderView playGif ---- 2");

        new Thread(new Runnable() {
            public void run() {
                try {
                    LogManager.d(TAG, "GifDecoderView playGif ---- 3");
                    final int n = mGifDecoder.getFrameCount();
                    final int ntimes = mGifDecoder.getLoopCount();
                    int repetitionCounter = 0;
                    do {
                        LogManager.d(TAG, "GifDecoderView playGif will show img");
                        for (int i = 0; i < n; i++) {
                            LogManager.d(TAG, "GifDecoderView playGif show img : " + i);
                            mTmpBitmap = mGifDecoder.getFrame(i);
                            int t = mGifDecoder.getDelay(i);
                            mHandler.post(mUpdateResults);
                            try {
                                Thread.sleep(t);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        if (ntimes != 0) {
                            repetitionCounter++;
                        }
                        LogManager.d(TAG, "GifDecoderView playGif end show img");
                    } while (mIsPlayingGif && (repetitionCounter <= ntimes));
                } catch (Error e) {
                    LogManager.e(TAG, e.getMessage());
                } catch (Exception e) {
                    LogManager.e(TAG, e.getMessage());
                }
            }
        }).start();
    }

    public void stopRendering() {
        mIsPlayingGif = false;
    }
}

