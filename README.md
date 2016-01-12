# Gif-picture-play
play gif picture
declare in xml file

<!-- GIF 图片-->
    <com.sohu.tv.ui.view.GifDecoderView
        android:id="@+id/xiaomaomi_gif"
        android:layout_width="@dimen/iv_head_photo_height"
        android:layout_height="@dimen/adver_pic_160_80"
        android:layout_alignParentBottom="true"
        android:layout_alignParentRight="true"
        android:layout_marginBottom="@dimen/button_download_width"
        android:layout_marginRight="@dimen/button_download_width"
        android:scaleType="fitXY"/>
  
  Activity or Fragment  use
  
    private void initSmallGif(View view) {
        mSmallGif = (GifDecoderView) view.findViewById(R.id.xiaomaomi_gif);
        mSmallGif.playGif();
    }

    private void stopSmallGifView() {
        if (mSmallGif != null) {
            mSmallGif.stopRendering();
        }
    }
