package com.sohu.tv.ui.util;

import android.content.Context;

import com.sohu.tv.ui.view.GifDecoder;

import java.io.IOException;
import java.io.InputStream;

public class GifHelper {

    private static GifDecoder mGifDecoder;

    public static GifDecoder getGifDecoder(Context context) {
        synchronized (GifHelper.class) {
            if (context == null) {
                mGifDecoder = null;
                return mGifDecoder;
            }
            if (mGifDecoder != null) {
                return mGifDecoder;
            }
            InputStream stream = null;
            try {
                stream = context.getAssets().open("xiaomaomi.gif");
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (stream == null) {
                mGifDecoder = null;
                return mGifDecoder;
            }
            mGifDecoder = new GifDecoder();
            mGifDecoder.read(stream);
            return mGifDecoder;
        }
    }
}
