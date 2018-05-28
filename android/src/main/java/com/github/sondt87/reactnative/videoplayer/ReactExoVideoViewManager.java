package com.github.sondt87.reactnative.videoplayer;

import com.facebook.react.uimanager.ThemedReactContext;
import com.github.sondt87.AbsReactVideoViewManager;
import com.github.sondt87.AbsVideoView;

/**
 * Created by dengchu on 2017/10/26.
 */

public class ReactExoVideoViewManager extends AbsReactVideoViewManager {

    public static final String REACT_CLASS = "RCTEXOVideo";


    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    protected AbsVideoView createViewInstance(final ThemedReactContext reactContext) {
        final AbsVideoView mVideoView = new ReactExoPlayerView(reactContext);
        return mVideoView;
    }

}