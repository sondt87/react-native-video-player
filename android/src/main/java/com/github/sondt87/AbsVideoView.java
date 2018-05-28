package com.github.sondt87;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.facebook.react.bridge.ReadableMap;


public abstract class AbsVideoView extends FrameLayout {

    public AbsVideoView(Context context) {
        super(context);
    }

    public AbsVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AbsVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

//    public IVideoView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
//        super(context, attrs, defStyleAttr, defStyleRes);
//    }

    public abstract void setDataSource(String source);

    public abstract void setResizeModeModifier(int resizeModeModifier);

    public abstract void setPausedModifier(boolean paused);

    public abstract void setMirror(boolean mirror);

    public abstract void setRotateDegree(int degree);

    public abstract void setMutedModifier(boolean muted) ;

    public abstract void setVolumeModifier(float volume);

    public abstract void setRepeatModifier(boolean repeat) ;

    public abstract void setProgressUpdateInterval(float progressUpdateInterval);

    public abstract void seekToModifier(long seek);

    public abstract void setPlayInBackground(boolean playInBackground);

    public abstract void cleanupMediaPlayerResources();

    public abstract void Release();

    public abstract void setControls(final boolean controls) ;

    public abstract void setTimeout(int prepareTimeout, int readTimeout);

    public abstract void setBufferSize(int bufferSize);

    public abstract void setBufferTime(int bufferTime);

    public abstract void saveBitmap();
}
