// package com.ksyun.media.reactnative;

// import com.facebook.react.uimanager.ThemedReactContext;
// import com.github.sondt87.AbsReactVideoViewManager;
// import com.github.sondt87.AbsVideoView;

// /**
//  * Created by dengchu on 2017/10/26.
//  */

// public class ReactKSYVideoViewManager extends AbsReactVideoViewManager {

//     public static final String REACT_CLASS = "RCTKSYVideo";


//     @Override
//     public String getName() {
//         return REACT_CLASS;
//     }

//     @Override
//     protected AbsVideoView createViewInstance(final ThemedReactContext reactContext) {
//         final AbsVideoView mVideoView = new ReactKSYVideoView(reactContext);
//         return mVideoView;
//     }
// }