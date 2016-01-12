package com.sohu.tv.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.LinearLayout;

import com.google.gson.Gson;
import com.sohu.lib.common.util.LogUtils;
import com.sohu.lib.common.util.StringUtils;
import com.sohu.lib.net.parse.CommonDataParser;
import com.sohu.lib.net.request.DataRequest;
import com.sohu.lib.net.request.RequestManager;
import com.sohu.lib.net.request.listener.DataResponseListener;
import com.sohu.lib.net.request.listener.DefaultDataResponse;
import com.sohu.lib.net.util.ErrorType;
import com.sohu.tv.R;
import com.sohu.tv.activity.AccountActivity;
import com.sohu.tv.activity.BuySohuFilmActivity;
import com.sohu.tv.activity.IndividualCenterActivity;
import com.sohu.tv.activity.SohuMoviePayManager;
import com.sohu.tv.activity.SohufilmCommodityListActivity;
import com.sohu.tv.control.constants.ChannelIdConstants;
import com.sohu.tv.control.constants.UIConstants;
import com.sohu.tv.control.http.DefaultResultParser;
import com.sohu.tv.control.http.ResponseDataWrapperSet;
import com.sohu.tv.control.http.request.DataRequestFactory;
import com.sohu.tv.control.log.LoggerUtil;
import com.sohu.tv.control.sharepreferences.ConfigurationSharedPreferences;
import com.sohu.tv.control.util.LogManager;
import com.sohu.tv.control.util.StringConstants;
import com.sohu.tv.control.util.UserActionStatistUtil;
import com.sohu.tv.model.Channel;
import com.sohu.tv.model.ChannelListData;
import com.sohu.tv.model.Column;
import com.sohu.tv.model.ColumnDataModel;
import com.sohu.tv.model.ColumnListModel;
import com.sohu.tv.model.IndividualData;
import com.sohu.tv.model.ListItemModel;
import com.sohu.tv.model.TemplateChannel;
import com.sohu.tv.model.VideoInfo;
import com.sohu.tv.model.VideoInfoListData;
import com.sohu.tv.ui.adapter.HomePageAdapter;
import com.sohu.tv.ui.listener.DismissAllChannelListener;
import com.sohu.tv.ui.listener.LoadingDialogListener;
import com.sohu.tv.ui.listener.OnVideoItemClickListener;
import com.sohu.tv.ui.listener.ScrollingNotLoadingImageScrollListener;
import com.sohu.tv.ui.manager.ChannelVipFocusItemHolder;
import com.sohu.tv.ui.manager.ListViewItemManager;
import com.sohu.tv.ui.manager.NormalVideoListItemHolder;
import com.sohu.tv.ui.view.GifDecoderView;
import com.sohu.tv.ui.view.HeaderPullListView;
import com.sohu.tv.ui.view.HeaderPullListView.OnRefreshListener;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class HomePageFragment extends Fragment implements View.OnClickListener,OnVideoItemClickListener {
    private static final String TAG = "HomePageFragment";
    private LoadingDialogListener loadingDialogListener;
    private OnVideoItemClickListener mCallback;
    private DismissAllChannelListener mDismissAllChannelListener;
    private HeaderPullListView homeListView;
    private LinearLayout networkErrorLayout;
    private Button mBtnVideoDownloaded;
    private Button mBtnTryAgain;
    private HomePageAdapter mHomePageAdapter;
    private View contentView;
    private View mFooterView;
    private RequestManager mRequestManager = new RequestManager();
    private com.sohu.tv.model.Channel mChannel;
    private long mChannelId;
    private String mChanneled;
    private boolean canLoadData = false;
    private boolean needLoadData = true;
    private int mCurrentOffset;
    private int mPageSize;
    private ListItemModel mLastNeedLoadMoreItemMode;
    private View mLoadingView;
    private View mEndView;
    private boolean isLoadingMore; //是否正在请求更多数据，如果是，则放弃本次请求
    private boolean hasMoreData = true; //加载更多是否有更多数据。
    private Gson mGson;
    private GifDecoderView mSmallGif;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogManager.d(TAG, " onCreate()");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LogManager.d(TAG, "onCreateView");
        contentView = inflater.inflate(R.layout.fragment_homepage, container,
                false);
        mFooterView = inflater.inflate(R.layout.listview_footer_end, null);

        findViews(contentView);
        LogManager.d(TAG, "onCreateView()");
        mGson=new Gson();
        return contentView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        canLoadData = true;
        if (getUserVisibleHint() && needLoadData && canLoadData) {
            startLoadData(mChannel, mChannel.getChannel_id());
        }
        LogManager.d(TAG, "onViewCreated");

    }

    public void startLoadData(com.sohu.tv.model.Channel channel, long loadColumnId) {
        LogManager.d(TAG, "startLoadData:" + channel.getName());
        homeListView.setVisibility(View.GONE);
        needLoadData = false;
        mChannel = channel;
        mChannelId = loadColumnId;
        if (mChannel != null && StringUtils.isNotBlank(mChannel.getChanneled())) {
        	mChanneled = mChannel.getChanneled();
        } else {
        	mChanneled = String.valueOf(mChannelId);
        }
        showLoadingDialog();
        requestColumnAndVideoList(channel, false, true);
    }

    //频道打包数据
    private void requestColumnAndVideoList(final Channel channel, final boolean isReflash, final boolean iscache) {
        DataRequest request = DataRequestFactory.createChannelPageDataListRequest(channel.getChannel_id(), UIConstants.DEFAULT_CURSOR, UIConstants.DEFUALT_PAGE_SIZE_FOR_CHANNEL_PAGE_DATA);
        DefaultResultParser parser = new DefaultResultParser(ColumnDataModel.class);
        if (mRequestManager != null) {
            mRequestManager.startDataRequestAsync(request, new DefaultDataResponse() {
                @Override
                public void onSuccess(Object notNullData, boolean isCache) {
                    final ColumnDataModel data = (ColumnDataModel) notNullData;
                    if (data.getData() != null && data.getData().getColumns() != null) {
                        final ColumnListModel columnList = data.getData();
                        if (null == contentView)
                            return;
                        contentView.post(new Runnable() {
                            @Override
                            public void run() {
                                if (columnList != null && columnList.getColumns() != null
                                        && columnList.getColumns().size() > 0) {
                                    if (columnList.getColumns() != null && !columnList.getColumns().isEmpty()) {
                                        Iterator<Column> iter = columnList.getColumns().iterator();
                                        while (iter.hasNext()) {
                                            Column c = iter.next();
                                            if (c.getColumnType() == Column.COLUMN_TYPE_HOT_STREAM) {
                                                iter.remove();
                                            }
                                        }
                                    }
                                    List<ListItemModel> homeListItemModelList = ListViewItemManager
                                            .createHomeListItemModleList(columnList.getColumns(), true, true);
                                    ListViewItemManager.handleItemModleList(homeListItemModelList,mChannelId);
                                    mHomePageAdapter.updateListAndHotPointStream(homeListItemModelList, mChannelId,mChanneled);
                                    mHomePageAdapter.setButtonClickListener(mButtonClickListener);
                                    homeListView.setVisibility(View.VISIBLE);
                                    if (homeListItemModelList != null && homeListItemModelList.size() > 0){
                                        mLastNeedLoadMoreItemMode = homeListItemModelList.get(homeListItemModelList.size() - 1);
                                        mCurrentOffset = mLastNeedLoadMoreItemMode.getColumn().getOffset();
//                                        setCurrentOffset();
                                    }
                                    LogManager.w(TAG, "requestColumnAndVideoList  channel id==" + channel.getChannel_id());
                                    if (ChannelIdConstants.CHANNEL_ID_PGC == channel.getChannel_id())
                                        requestPgcHotLabelList();
                                } else {
                                    showNetworkError();
                                }
                                dismissLoadingDialog();
                                if (isReflash) {
                                    homeListView.onRefreshComplete();
                                }
                            }

                        });
                    } else {
                        showNetworkError();
                        dismissLoadingDialog();

                    }
                    LogUtils.d(TAG, "sendRequestGetList onSuccess");
                }

                @Override
                public void onFailure(ErrorType errorCode) {
                    showNetworkError();
                    dismissLoadingDialog();
                    homeListView.onRefreshComplete();
                }
            }, parser, iscache);
        }

    }
    
    private void setLoadingView(){
    	mLoadingView = mFooterView.findViewById(R.id.more_loading);
    	mEndView = mFooterView.findViewById(R.id.more_end);
    	if(ChannelIdConstants.CHANNEL_ID_HOME_PAGE == mChannelId){
    		mLoadingView.setVisibility(View.GONE);
    		mEndView.setVisibility(View.VISIBLE);
    	} else{
    		mLoadingView.setVisibility(View.VISIBLE);
    		mEndView.setVisibility(View.GONE);
    	}
    }
    
    private void setNoMoreLoadingDataView(){
    	mLoadingView = mFooterView.findViewById(R.id.more_loading);
    	mEndView = mFooterView.findViewById(R.id.more_end);
    	mLoadingView.setVisibility(View.GONE);
    	mEndView.setVisibility(View.VISIBLE);
    }
    

    //个别频道加载更多数据
    private void loadMoreData() {
    	isLoadingMore = true;
    	LogManager.d(TAG,"loadMoreData");
    	LogManager.d(NormalVideoListItemHolder.TAG, "loadMoreData");
        String baseUrl ="";
        int templateId=0;
        if (mLastNeedLoadMoreItemMode != null && mLastNeedLoadMoreItemMode.getColumn() != null) {
            baseUrl = mLastNeedLoadMoreItemMode.getColumn().getMore_list();
            //判断横竖图显示的个数
            TemplateChannel templateChannel = mLastNeedLoadMoreItemMode.getColumn().getTemplate();
            if (templateChannel != null)
                templateId = templateChannel.getTemplate_id();
        }
        LogManager.w(TAG, "loadMoreData baseurl=" + baseUrl);
        if (TextUtils.isEmpty(baseUrl)){
        	setNoMoreLoadingDataView();
        	isLoadingMore = false;
        	return;
        }
        setLoadingView();
        if (mLastNeedLoadMoreItemMode.getItemType() == ListItemModel.ITEM_TYPE_NORMAL_VIDEO_LIST) {
        	mPageSize = 7*ListViewItemManager.NUM_PER_ROW_HOR_PIC;
		}else if(mLastNeedLoadMoreItemMode.getItemType() == ListItemModel.ITEM_TYPE_NORMAL_VERTICAL_VIDEO_LIST){
			mPageSize = 7*ListViewItemManager.NUM_PER_ROW_VER_PIC;
		}else{
			mPageSize = 7*ListViewItemManager.NUM_PER_ROW_VER_PIC;
		}
        LogManager.d(TAG,"TextLoader.requestData mCurrentOffset ? " + mCurrentOffset);
        final int templateID = templateId;
        DataRequest request = DataRequestFactory.createColumnMoreListRequest(baseUrl, mCurrentOffset, mPageSize);
        if (mRequestManager != null) {
        	mRequestManager.startDataRequestAsyncWithDefaultCache(request, new DataResponseListener() {
				
				@Override
				public void onSuccess(Object notNullData, boolean isCache) {
					LogManager.d(NormalVideoListItemHolder.TAG,"onDataReady begin ? " + mCurrentOffset);
		            if (getActivity() == null || isDetached()) {
		            	isLoadingMore = false;
		                return;
		            }
		            VideoInfoListData videoInfoListData = ((ResponseDataWrapperSet.VideoInfoListDataWrapper) notNullData).getData();
		            if (videoInfoListData == null) {
		            	onFailure(ErrorType.ERROR_CACHE_ONLY_NO_DATA);
		                return;
		            }
		            if (videoInfoListData.getVideos() != null && videoInfoListData.getVideos().size() > 0
		                    && !isDetached()) {
		                List<ListItemModel> list = convertVideo2LitemModel(videoInfoListData, templateID);
		                mHomePageAdapter.addMoreListData(list, mChannelId,mChanneled);
		                LogManager.d(NormalVideoListItemHolder.TAG,"after addMoreListData");
		                mCurrentOffset += mPageSize;
		            	isLoadingMore = false;
		            } else{
		            	onFailure(ErrorType.ERROR_CACHE_ONLY_NO_DATA);
		            }
		            LogManager.d(NormalVideoListItemHolder.TAG,"onDataReady end ? " + mCurrentOffset);
					
				}
				
				@Override
				public void onFailure(ErrorType errorCode) {
					LogManager.d(NormalVideoListItemHolder.TAG,"onFailure ");
		        	setNoMoreLoadingDataView();
		        	isLoadingMore = false;
		        	hasMoreData = false;
				}
				
				@Override
				public void onCancelled() {
					LogManager.d(NormalVideoListItemHolder.TAG,"onCancelled ");
		        	setNoMoreLoadingDataView();
		        	isLoadingMore = false;
				}
			},new CommonDataParser(ResponseDataWrapperSet.VideoInfoListDataWrapper.class));
        }
    }

    private class VideoListScroll extends
            ScrollingNotLoadingImageScrollListener {

        public VideoListScroll(AbsListView listView) {
            super(listView);
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            mDismissAllChannelListener.dismissAllChannel();
            super.onScrollStateChanged(view, scrollState);
            if (view == null)
                return;
            if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
            	LogManager.d(TAG,"onScrollStateChanged SCROLL_STATE_IDLE");
            	LogManager.d(NormalVideoListItemHolder.TAG, "onScrollStateChanged SCROLL_STATE_IDLE");
//                if (isFastDoubleScroll()) {
//                	LogManager.d(TAG,"isFastDoubleScroll");
//                    return;
//                }

                if (view.getLastVisiblePosition() + 1 == view.getCount()) {
                	LogManager.d(NormalVideoListItemHolder.TAG, "onScrollStateChanged view.getLastVisiblePosition() + 1 == view.getCount()");
                	LogManager.d(NormalVideoListItemHolder.TAG, "onScrollStateChanged isLoadingMore ? " + isLoadingMore);
                	if (isLoadingMore) {
                		return;
                	}
                    mFooterView.setVisibility(View.VISIBLE);
                    if (mChannel != null && mChannel.getChannel_id() == ChannelIdConstants.CHANNEL_ID_HOME_PAGE)
                        UserActionStatistUtil.sendChangeViewLog(LoggerUtil.ActionId.HOME_PAGE_SCROLL_TO_BOTTOM);
                    if (hasMoreData) {
                        loadMoreData();
                    }
                }
            }
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                             int visibleItemCount, int totalItemCount) {
            if (homeListView != null) {
                homeListView.setFirstItemIndex(firstVisibleItem);
            }
            if (firstVisibleItem > 0) {
                if (mFooterView != null) {
                    mFooterView.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    //加载更多数据时，转换数据格式，适配mHomePageAdapter
    private List<ListItemModel> convertVideo2LitemModel(VideoInfoListData videoInfoListData, int templateId) {
        List<ListItemModel> list = null;
        List<VideoInfo> videoInfos = videoInfoListData.getVideos();
        ArrayList<Column> columns = new ArrayList<Column>();
        int count = 0, everyLineNumber = 0;
        if (templateId == Column.COLUMN_TYPE_HORIZON_PIC) {
            everyLineNumber = 4;
            count = videoInfos.size() / 4;
        } else if (templateId == Column.COLUMN_TYPE_VERTICAL_PIC || templateId == Column.COLUMN_TYPE_VERTICAL_ONE_PLUS_N) {
            everyLineNumber = 5;
            count = videoInfos.size() / 5;
        }

        for (int i = 0; i < count; i++) {
            List<VideoInfo> subVideoInfos = videoInfos.subList(i * everyLineNumber, (i + 1) * everyLineNumber);
            Column column = new Column();
            TemplateChannel templateChannel = mLastNeedLoadMoreItemMode.getVideoList().get(0).getTemplateChannel();
            if(templateChannel == null){
            	templateChannel = new TemplateChannel();
            }
            if (templateId == Column.COLUMN_TYPE_HORIZON_PIC) {
                templateChannel.setTemplate_id(Column.COLUMN_TYPE_HORIZON_PIC);
            } else if (templateId == Column.COLUMN_TYPE_VERTICAL_PIC || templateId == Column.COLUMN_TYPE_VERTICAL_ONE_PLUS_N) {
                templateChannel.setTemplate_id(Column.COLUMN_TYPE_VERTICAL_PIC);
            }
            column.setTemplate(templateChannel);
            column.setData_list(subVideoInfos);
            columns.add(column);
        }

        list = ListViewItemManager.createHomeListItemModleList(columns, false, false);

        return list;
    }

    //get PGC hotlable inteface
    private void requestPgcHotLabelList() {
        DataRequest request = DataRequestFactory.createChannelListRequest(UIConstants.CHANNEL_LIST_TYPE_PGC_RANK);
        if (mRequestManager != null) {
        	mRequestManager.startDataRequestAsyncWithDefaultCache(request, new DataResponseListener() {
				
				@Override
				public void onSuccess(Object notNullData, boolean isCache) {
					 LogManager.d(TAG,
                             "pgc request hotlabel onDataReady data=" + notNullData);
                     ResponseDataWrapperSet.ChannelListDataWrapper wrapper = (ResponseDataWrapperSet.ChannelListDataWrapper) notNullData;
                     ChannelListData channelListData = wrapper.getData();
                     if (channelListData != null) {

                         if (getActivity() != null && !isDetached()) {

                             List<ListItemModel> pgcHotLabelItemModelList = new ArrayList<ListItemModel>();

                             ListItemModel mPgcHotLabelMode = null;
                             if (mPgcHotLabelMode == null)
                                 mPgcHotLabelMode = new ListItemModel();
                             mPgcHotLabelMode.setItemType(ListItemModel.ITEM_TYPE_FOCUS);

                             //保存热门标签到SP,PgcViewPagerActivity中直接读取SP,避免二次请求
                             // ConfigurationSharedPreferences.setPgcHotLabel(SohuVideoPadApplication.mContext, mGson.toJson(channelListData.getCateCodes()));

                             mPgcHotLabelMode.setHotLabelList(convertColumn2VideoInfo(channelListData.getCateCodes()));
                             pgcHotLabelItemModelList.add(mPgcHotLabelMode);
                             mHomePageAdapter.addListAndHotPointStream(pgcHotLabelItemModelList, mChannelId,mChanneled);
                         }
                     }
				}
				
				@Override
				public void onFailure(ErrorType errorCode) {
					LogManager.w(TAG, "requestPgcHotLabelList onFailure");
					
				}
				
				@Override
				public void onCancelled() {
					LogManager.w(TAG, "requestPgcHotLabelList onCancelled");
					
				}
			}, new CommonDataParser(ResponseDataWrapperSet.ChannelListDataWrapper.class));
        }
    }

    //PGC 将单独接口标签转换为和其他频道一样的热门标签
    private List<VideoInfo> convertColumn2VideoInfo(List<Channel> channels) {
        if (channels == null || channels.size() <= 0)
            return null;
        List<VideoInfo> videoInfos = new ArrayList<VideoInfo>();
        for (Channel channel : channels) {
            VideoInfo info = new VideoInfo();
            info.setTag_name(channel.getName());
            info.setAction_url(generatePgcHotLabelActionUrl(info, channel));
            videoInfos.add(info);
        }
        return videoInfos;
    }

    //构建PGC热门标签action_url
    private String generatePgcHotLabelActionUrl(VideoInfo info, Channel channel) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("sva://action.cmd?action=2.4&");
        buffer.append("cateCode=" + channel.getCate_code() + "&");
        buffer.append("ex3=" + channel.getChannel_id() + "&");
        buffer.append("channel_list_type=" + 2);

        //为每个标签设置cateCode,channelid
        info.setCate_code(String.valueOf(channel.getCate_code()));
        info.setChannel_id(String.valueOf(channel.getChannel_id()));
        return buffer.toString();
    }

    @Override
    public void onResume() {
        LogManager.d(TAG, "onResume()");
        super.onResume();
        startGalleyAutoSlide();
        getPlayHistory();
        getUserdata();
    }

    private void getPlayHistory() {
        if (ChannelIdConstants.CHANNEL_ID_HOME_PAGE == mChannelId) {
            if (isLogin()) {
                mHomePageAdapter.getUserPlayHistory();
            } else {
                mHomePageAdapter.getLocalPlayHistory();
            }
        }
    }
    private void getUserdata() {
        if (ChannelIdConstants.CHANNEL_ID_CINEMA == mChannelId) {
          if (isLogin()) {
	      mHomePageAdapter.showLoginView();
 	      mHomePageAdapter.showUserData();
            } else {
                mHomePageAdapter.showNoLoginView();
            }
        }
    }
    public void startGalleyAutoSlide() {
        if (mHomePageAdapter != null) {
            mHomePageAdapter.startAutoSlide();
        }
    }

    public void cancelGalleryAutoSlide() {
        if (mHomePageAdapter != null) {
            mHomePageAdapter.cancelAutoSlide();
        }
    }

    //用户是否登陆
    private boolean isLogin() {
        boolean isAutoLogin = ConfigurationSharedPreferences
                .getIsAutoLogin(getActivity());
        boolean isLogin = ConfigurationSharedPreferences
                .getIsLogin(getActivity());
        return isAutoLogin || isLogin;
    }

    @Override
    public void onAttach(Activity activity) {
        LogManager.d(TAG, "onAttach()");
        super.onAttach(activity);
        try {
            loadingDialogListener = (LoadingDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + "must implement LoadingDialogListener");
        }
        try {
            mCallback = (OnVideoItemClickListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnVideoItemClickListener");
        }

        try {
            mDismissAllChannelListener = (DismissAllChannelListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement DismissAllChannelListener");
        }
    }

    @Override
    public void onPause() {
        LogManager.d(TAG, "onPause()");
        super.onPause();
        cancelGalleryAutoSlide();
    }

    public void onHide() {
        if (mHomePageAdapter != null) {
            mHomePageAdapter.cancelAutoSlide();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogManager.d(TAG, "onDestroy()");
        if (homeListView != null) {
            homeListView.setAdapter(null);
            homeListView.removeAllViewsInLayout();
        }
        if (mHomePageAdapter != null) {
            mHomePageAdapter.onDestroy();
        }
        if (mRequestManager != null) {
            mRequestManager.cancelAllRequest();
            mRequestManager = null;
        }
    }

    private void findViews(View view) {

        initSmallGif(view);
        homeListView = (HeaderPullListView) view.findViewById(R.id.home_listview);
        mHomePageAdapter = new HomePageAdapter(getActivity(), this, mRequestManager);
        homeListView.addFooterView(mFooterView);
        mFooterView.setVisibility(View.GONE);

        homeListView.setAdapter(null);
        mHomePageAdapter.clear();
        mHomePageAdapter.notifyDataSetChanged();

        homeListView.setAdapter(mHomePageAdapter);

        networkErrorLayout = (LinearLayout) view
                .findViewById(R.id.network_error_linearlayout);
        //homeListView.setOnScrollListener(mHomePageListViewScrollListener);
        mBtnVideoDownloaded = (Button) view
                .findViewById(R.id.btn_video_downloaded);
        mBtnVideoDownloaded.setOnClickListener(this);
        mBtnTryAgain = (Button) view.findViewById(R.id.btn_try_again);
        mBtnTryAgain.setOnClickListener(this);
        homeListView.setOnScrollListener(new VideoListScroll(homeListView));

        homeListView.setonRefreshListener(new OnRefreshListener() {

            @Override
            public void onRefresh() {
                if (mChannel != null && mChannel.getChannel_id() == ChannelIdConstants.CHANNEL_ID_HOME_PAGE)
                    UserActionStatistUtil.sendChangeViewLog(LoggerUtil.ActionId.HOME_PAGE_PULL_REFRESH);
                requestColumnAndVideoList(mChannel, true, true);
            }
        });
    }

    private void initSmallGif(View view) {
        mSmallGif = (GifDecoderView) view.findViewById(R.id.xiaomaomi_gif);
        mSmallGif.playGif();
    }

    private void stopSmallGifView() {
        if (mSmallGif != null) {
            mSmallGif.stopRendering();
        }
    }

    @Override
    public void onItemClick(VideoInfo videoInfo, long columnId) {
    	if (mCallback != null) {
    		mCallback.onItemClick(videoInfo, columnId);
    	}
    }
    
    @Override
    public void onColumnMoreButtonClick(Column column, boolean isFromHotlabelClick) {
    	if (mCallback != null) {
            // MainActivity--> onColumnMoreButtonClick()
    		mCallback.onColumnMoreButtonClick(column, isFromHotlabelClick);
    	}
    	
    }
    
    @Override
    public Channel getCurrentChannel() {
    	if (mCallback != null) {
    		return mCallback.getCurrentChannel();
    	}
    	return null;
    }

    private void showNetworkError() {
        networkErrorLayout.setVisibility(View.VISIBLE);
        if (homeListView != null) {
            homeListView.setVisibility(View.GONE);
        }
    }

    private void dismissNetworkError() {
        networkErrorLayout.setVisibility(View.GONE);

        homeListView.setVisibility(View.VISIBLE);

    }

    private void showLoadingDialog() {
        if (loadingDialogListener != null) {
            loadingDialogListener.showLoadingDialog();
        }
    }

    private void dismissLoadingDialog() {
        if (loadingDialogListener != null) {
            loadingDialogListener.dismissLoadingDialog();
        }
    }

    private void reloadHomePage() {
        showLoadingDialog();
        dismissNetworkError();
        requestColumnAndVideoList(mChannel, false, true);
    }

    @Override
    public void onStop() {
        LogManager.d(TAG, " onStop()");
        super.onStop();
    }

    @Override
    public void onDetach() {
        LogManager.d(TAG, " onDetach()");
        {
            super.onDetach();
            try {
                Field childFragmentManager = Fragment.class
                        .getDeclaredField("mChildFragmentManager");
                childFragmentManager.setAccessible(true);
                childFragmentManager.set(this, null);

            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        super.onDetach();
    }

    @Override
    public void onDestroyView() {
        LogManager.d(TAG, " onDestroyView()");
        needLoadData = true;
        super.onDestroyView();
        stopSmallGifView();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_try_again:
                reloadHomePage();
                break;
            case R.id.btn_video_downloaded:

                Intent intent = new Intent(getActivity(),
                        IndividualCenterActivity.class);
                intent.putExtra("ID_PRELOAD", IndividualData.ID_PRELOAD);
                startActivity(intent);

                break;
            default:
                break;
        }
    }

    public static int getIndex(int desPosition, List<Integer> list) {
        if (list != null) {
            if (!list.contains(desPosition)) {
                list.add(desPosition);
            }
            Collections.sort(list);
            return list.indexOf(desPosition);
        }
        return 0;
    }

    public void setChannel(Channel channel) {
        mChannel = channel;

    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && needLoadData && canLoadData) {
            startLoadData(mChannel, mChannel.getChannel_id());
        }
    }
    
    private final ChannelVipFocusItemHolder.ButtonClickListener mButtonClickListener = new ChannelVipFocusItemHolder.ButtonClickListener() {

        @Override
        public void onSohufilmPersonHeadPicClick() {
            if (isLogin()) {
            	Intent intent = new Intent(getActivity(), IndividualCenterActivity.class);
                intent.putExtra(IndividualData.ID_PRELOAD_KEY, "-1");
                startActivity(intent);
            } else {
                Intent intent = new Intent(getActivity(), AccountActivity.class);
                intent.putExtra(AccountActivity.LOGIN_ENTRANCE, "4");
                intent.putExtra(AccountActivity.LOGIN_FROM, "activity");  
                startActivityForResult(intent, StringConstants.REQUEST_CODE_SOHUFILM_LOGIN);
            }
        }

        @Override
        public void onSohufilmOpenVipBtnClick() {
        	UserActionStatistUtil.sendstoreprivilegelog(LoggerUtil.ActionId.STOPRE_BUY_MONTH, null,
                    null, String.valueOf(LoggerUtil.FROMMOVIES_PAGE), null);
            Intent openIntent = new Intent(getActivity(), SohufilmCommodityListActivity.class);
            startActivityForResult(openIntent, StringConstants.REQUEST_CODE_SOHUFILM_PAY);

        }

        @Override
        public void onSohufilmBuyVideoTipClick() {
            Intent buyIntent = new Intent(getActivity(), BuySohuFilmActivity.class);
            startActivity(buyIntent);

        }
    };
public void onActivityResult(int requestCode, int resultCode, Intent data) {
	 switch (requestCode) {
     case StringConstants.REQUEST_CODE_SOHUFILM_LOGIN:
         if (resultCode == Activity.RESULT_OK) {
             if (mHomePageAdapter != null) {
            	 mHomePageAdapter.showLoginView();
            	 mHomePageAdapter.showUserData();
             }
             break;

         }
     case StringConstants.REQUEST_CODE_SOHUFILM_PAY:
         if (resultCode == SohuMoviePayManager.PAYSUCCESS) {
             if (mHomePageAdapter != null) {
            	 mHomePageAdapter.showLoginView();
            	 mHomePageAdapter.showUserData();
             }
             break;
         } else if (resultCode == Activity.RESULT_OK) {
             if (mHomePageAdapter != null) {
            	 mHomePageAdapter.showLoginView();
            	 mHomePageAdapter.showUserData();
             }
             break;
         }
 }
}
}
