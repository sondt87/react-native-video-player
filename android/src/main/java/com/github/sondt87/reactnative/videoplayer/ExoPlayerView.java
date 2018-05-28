package com.github.sondt87.reactnative.videoplayer;

import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.Surface;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.MediaController;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.github.sondt87.AbsVideoView;
import com.github.sondt87.Events;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.ext.rtmp.RtmpDataSourceFactory;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.util.EventLogger;
import com.google.android.exoplayer2.video.VideoRendererEventListener;
import com.ksyun.media.reactnative.R;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;

public class ExoPlayerView extends AbsVideoView implements ExoPlayer.EventListener, LifecycleEventListener, MediaController.MediaPlayerControl, VideoRendererEventListener {
    private static final String EVENT_PROP_DURATION = "duration";

    private static final String EVENT_PROP_CURRENT_TIME = "currentTime";

    private static final String EVENT_PROP_WARNING_MESSAGE = "warningMessage";

    private static final String EVENT_PROP_ERROR = "error";

    private static final String EVENT_PROP_SEEK_TIME = "seekTime";

    private ThemedReactContext mContext;

    private RCTEventEmitter mEventEmitter;

    private PlayerView mSimpleExoPlayerView;

    private SimpleExoPlayer mPlayer;

    private final Handler mHandler = new Handler();

    private Runnable mProgressUpdateRunnable = null;

    private Handler mProgressUpdateHandler = new Handler();

    private EventLogger mEventLogger;

    private MappingTrackSelector mTrackSelector;

    private DefaultBandwidthMeter mBandwidthMeter = new DefaultBandwidthMeter();

    private DataSource.Factory mMediaDataSourceFactory;

    private String mUserAgent;

    private Uri mUri;

    private long mPlayerPosition;

    private float mSpeed = 1.0f;

    private float mVolume = 1.0f;

    private boolean mIsMuted = false;

    private boolean mIsPlaying = true;

    private boolean mIsDetached = false;

    private boolean mIsSeeked = false;

    public ExoPlayerView(final ThemedReactContext context) {
        super(context.getCurrentActivity());
        mContext = context;
        context.addLifecycleEventListener(this);
        mEventEmitter = context.getJSModule(RCTEventEmitter.class);
        mSimpleExoPlayerView = new PlayerView(context);
        mSimpleExoPlayerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH);
        mSimpleExoPlayerView.setUseController(false);
        addView(mSimpleExoPlayerView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));
        mUserAgent = Util.getUserAgent(mContext, mContext.getPackageName());
        mMediaDataSourceFactory = buildDataSourceFactory();
        mProgressUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (mPlayer != null && mPlayer.getPlaybackState() == ExoPlayer.STATE_READY &&
                        mPlayer.getPlayWhenReady()) {
                    sendProgressEvent(Math.max(0, (int) mPlayer.getCurrentPosition()),
                            Math.max(0, (int) mPlayer.getDuration()));
                }
                mProgressUpdateHandler.postDelayed(mProgressUpdateRunnable, 250);
            }
        };
    }


    @Override
    public void onRepeatModeChanged(int repeatMode) {

    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }

    @Override
    public void onSeekProcessed() {

    }

    public void setSpeed( float speed) {
        mSpeed = speed;
        changeSpeed();
    }

    public void setVolume( float volume) {
        mVolume = volume;
        changeVolume();
    }

    public void setMuted( boolean isMuted) {
        mIsMuted = isMuted;
        changeVolume();
    }

    public void setPaused(boolean isPaused) {
        mIsPlaying = !isPaused;
        if (mPlayer != null) {
            mPlayer.setPlayWhenReady(mIsPlaying);
        }
    }

    public void seekTo( int position) {
        mPlayerPosition = position;
        if (mPlayer != null) {
            mPlayer.seekTo(mPlayerPosition);
            mIsSeeked = true;
        }
    }

    public void setControls(final boolean isControlVisible) {
        if (mSimpleExoPlayerView != null) {
            mSimpleExoPlayerView.setUseController(isControlVisible);
        }
    }

    private void sendProgressEvent(final int currentTime, final int duration) {
        final WritableMap event = Arguments.createMap();
        event.putInt(EVENT_PROP_CURRENT_TIME, currentTime);
        event.putInt(EVENT_PROP_DURATION, duration);
        mEventEmitter.receiveEvent(getId(), Events.EVENT_PROGRESS.toString(), event);
    }

    private void sendSeekEvent() {
        final WritableMap event = Arguments.createMap();
        event.putInt(EVENT_PROP_SEEK_TIME, (int) mPlayer.getCurrentPosition());
        mEventEmitter.receiveEvent(getId(), Events.EVENT_SEEK.toString(), event);
    }

    private void sendEndEvent() {
        final WritableMap event = Arguments.createMap();
        mEventEmitter.receiveEvent(getId(), Events.EVENT_END.toString(), event);
    }

    private void sendErrorEvent(final String errorMessage) {
        final WritableMap event = Arguments.createMap();
        event.putString(EVENT_PROP_ERROR, errorMessage);
        mEventEmitter.receiveEvent(getId(), Events.EVENT_ERROR.toString(), event);
    }

    private void releasePlayer() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer.removeListener(this);
            mPlayer = null;
        }
        mTrackSelector = null;
        mEventLogger = null;
        mProgressUpdateHandler.removeCallbacks(mProgressUpdateRunnable);
    }

    private void initializePlayer() {
        if (mPlayer != null) {
            return;
        }

        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(bandwidthMeter);
        mTrackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        mPlayer = ExoPlayerFactory.newSimpleInstance(mContext, mTrackSelector);
        mEventLogger = new EventLogger(mTrackSelector);
        mPlayer.addListener(this);
        mPlayer.addListener(mEventLogger);
        mSimpleExoPlayerView.setPlayer(mPlayer);
        mPlayer.addVideoDebugListener(this);
    }

    private void preparePlayer() {
        if (mUri == null) {
            return;
        }
        changeSpeed();
        changeVolume();
        mPlayer.seekTo(mPlayerPosition);
        mPlayer.setPlayWhenReady(mIsPlaying);
        mPlayer.prepare(buildMediaSource(mUri));
    }

    private void changeVolume() {
        if (mPlayer == null) {
            return;
        }
        if (mIsMuted) {
            mPlayer.setVolume(0.0f);
        } else {
            mPlayer.setVolume(mVolume);
        }
    }

    private void changeSpeed() {
        try {
            if (mPlayer == null || mSpeed == 1.0f) {
                return;
            }
            if (RNExoPlayerModule.isRateSupported) {
                final PlaybackParams playbackParams = new PlaybackParams();
                playbackParams.setSpeed(mSpeed);
                mPlayer.setPlaybackParams(playbackParams);
            } else {
                throw new UnsupportedRateException(
                        "Change of speed is supported " + "starting from API level 23.");
            }
        } catch (UnsupportedRateException e) {
            e.printStackTrace();
            mSpeed = 1.0f;
            final WritableMap event = Arguments.createMap();
            String warningMessage = e.getMessage() + '\n';
            for (final StackTraceElement element : e.getStackTrace()) {
                if (element != null) {
                    warningMessage += '\n' + element.toString();
                }
            }
            event.putString(EVENT_PROP_WARNING_MESSAGE, warningMessage);
            mEventEmitter.receiveEvent(getId(), Events.EVENT_WARNING.toString(), event);
        }
    }

    private MediaSource buildMediaSource(final Uri uri) {
        int type = Util.inferContentType(uri.getLastPathSegment());
        switch (type) {
            case C.TYPE_SS:
                return new SsMediaSource(uri,
                        buildDataSourceFactory(),
                        new DefaultSsChunkSource.Factory(mMediaDataSourceFactory),
                        mHandler,
                        mEventLogger);
            case C.TYPE_DASH:
            return new DashMediaSource(uri,
                    buildDataSourceFactory(),
                    new DefaultDashChunkSource.Factory(
                            mMediaDataSourceFactory),
                    mHandler,
                    mEventLogger);
            case C.TYPE_HLS:
                return new HlsMediaSource(uri, mMediaDataSourceFactory, mHandler, mEventLogger);
            case C.TYPE_OTHER:
                return buildOtherTypeDataSource(uri);

//                return new ExtractorMediaSource(uri,mMediaDataSourceFactory,new DefaultExtractorsFactory(),mHandler,
//                        mEventLogger);
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }

    private MediaSource buildOtherTypeDataSource(Uri uri) {
        String scheme = uri.getScheme();
        String fileName = Util.toLowerInvariant(uri.getLastPathSegment());

        if(scheme == null)
            throw new IllegalStateException("Unsupported relative URI: " + uri.toString());

        if(scheme.startsWith("rtmp")) return buildRtmpDataSource(uri);
        else
           return buildOtherDataSource(uri);
    }

    @NonNull
    private MediaSource buildOtherDataSource(Uri uri) {
        DataSource.Factory factory = new DefaultDataSourceFactory(mContext,
                    Util.getUserAgent(mContext, mContext.getPackageName()));
        MediaSource mediaSource = new ExtractorMediaSource.Factory(factory).createMediaSource(uri);
        return mediaSource;
    }

    private MediaSource buildRtmpDataSource(Uri uri) {
        RtmpDataSourceFactory rtmpDataSourceFactory = new RtmpDataSourceFactory();

        MediaSource videoSource = new ExtractorMediaSource.Factory(rtmpDataSourceFactory)
                .createMediaSource(uri);

        return videoSource;
    }

    private DataSource.Factory buildDataSourceFactory() {
        return new DefaultDataSourceFactory(mContext,
                mBandwidthMeter,
                buildHttpDataSourceFactory());
    }

    private HttpDataSource.Factory buildHttpDataSourceFactory() {
        return new DefaultHttpDataSourceFactory(mUserAgent, mBandwidthMeter);
    }

    @Override
    public void onLoadingChanged(final boolean isLoading) {

    }

    @Override
    public void onPlayerStateChanged(final boolean playWhenReady, final int playbackState) {
        switch (playbackState) {
            case Player.STATE_ENDED:
                sendProgressEvent((int) mPlayer.getDuration(), (int) mPlayer.getDuration());
                sendEndEvent();
                break;
            case Player.STATE_READY:
                if (mIsSeeked) {
                    mIsSeeked = false;
                    sendSeekEvent();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onTimelineChanged(final Timeline timeline, final Object manifest, int reason) {

    }

    @Override
    public void onTracksChanged(
            final TrackGroupArray trackGroups, final TrackSelectionArray trackSelections) {

    }

    @Override
    public void onPlayerError(final ExoPlaybackException e) {
        String errorString = e.getMessage();
        if (errorString == null) {
            if (e.type == ExoPlaybackException.TYPE_RENDERER) {
                Exception cause = e.getRendererException();
                if (cause instanceof MediaCodecRenderer.DecoderInitializationException) {
                    MediaCodecRenderer.DecoderInitializationException
                            decoderInitializationException =
                            (MediaCodecRenderer.DecoderInitializationException) cause;
                    if (decoderInitializationException.decoderName == null) {
                        if (decoderInitializationException.getCause() instanceof MediaCodecUtil
                                .DecoderQueryException) {
                            errorString = mContext.getString(R.string.error_querying_decoders);
                        } else if (decoderInitializationException.secureDecoderRequired) {
                            errorString = mContext.getString(R.string.error_no_secure_decoder,decoderInitializationException.mimeType);
                        } else {
                            errorString = mContext.getString(R.string.error_no_decoder,
                                    decoderInitializationException
                                            .mimeType);
                        }
                    } else {
                        errorString = mContext.getString(R.string.error_instantiating_decoder,
                                decoderInitializationException
                                        .decoderName);
                    }
                }
            } else if (e.type == ExoPlaybackException.TYPE_SOURCE) {
                errorString = mContext.getString(R.string.error_unable_to_connect, mUri);
            }
        }
        sendErrorEvent(errorString);
    }

    @Override
    public void onPositionDiscontinuity(int reason) {

    }

    @Override
    public void onHostPause() {
        if (mPlayer != null) {
            mPlayerPosition = mPlayer.getCurrentPosition();
            mIsPlaying = mPlayer.getPlayWhenReady();
        } else {
            mPlayerPosition = 0;
        }
        releasePlayer();
    }

    @Override
    public void onHostResume() {
        if (!mIsDetached) {
            initializePlayer();
            preparePlayer();
            mProgressUpdateHandler.post(mProgressUpdateRunnable);
        }
    }

    @Override
    public void onHostDestroy() {
        releasePlayer();
    }

    @Override
    protected void onDetachedFromWindow() {
        releasePlayer();
        mIsDetached = true;
        super.onDetachedFromWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mIsDetached = false;
    }

    @Override
    public void start() {

    }

    @Override
    public void pause() {
//        setPaused(true);
    }

    @Override
    public int getDuration() {
        return 0;
    }

    @Override
    public int getCurrentPosition() {
        return 0;
    }


    @Override
    public boolean isPlaying() {
        return mIsPlaying;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return false;
    }

    @Override
    public boolean canSeekBackward() {
        return false;
    }

    @Override
    public boolean canSeekForward() {
        return false;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }


    public void setUri(final Uri uri) {
        mUri = uri;
        initializePlayer();
        preparePlayer();
        mProgressUpdateHandler.removeCallbacks(mProgressUpdateRunnable);
        mProgressUpdateHandler.post(mProgressUpdateRunnable);
    }
    @Override
    public void setDataSource(String source) {
        setUri(Uri.parse(source));
    }

    @Override
    public void setResizeModeModifier(int resizeModeModifier) {

    }

    @Override
    public void setPausedModifier(boolean paused) {
        setPaused(paused);
    }

    @Override
    public void setMirror(boolean mirror) {

    }

    @Override
    public void setRotateDegree(int degree) {

    }

    @Override
    public void setMutedModifier(boolean muted) {
        setMuted(muted);
    }

    @Override
    public void setVolumeModifier(float volume) {
        setVolume(volume);
    }

    @Override
    public void setRepeatModifier(boolean repeat) {

    }

    @Override
    public void setProgressUpdateInterval(float progressUpdateInterval) {

    }

    @Override
    public void seekToModifier(long seek) {
        seekTo((int) seek);
    }

    @Override
    public void setPlayInBackground(boolean playInBackground) {

    }

    @Override
    public void cleanupMediaPlayerResources() {

    }

    @Override
    public void Release() {
        releasePlayer();
    }

    @Override
    public void setTimeout(int prepareTimeout, int readTimeout) {

    }

    @Override
    public void setBufferSize(int bufferSize) {

    }

    @Override
    public void setBufferTime(int bufferTime) {

    }

    @Override
    public void saveBitmap() {

    }

    @Override
    public void requestLayout() {
        super.requestLayout();

        // The spinner relies on a measure + layout pass happening after it calls requestLayout().
        // Without this, the widget never actually changes the selection and doesn't call the
        // appropriate listeners. Since we override onLayout in our ViewGroups, a layout pass never
        // happens after a call to requestLayout, so we simulate one here.
        post(measureAndLayout);
    }

    private final Runnable measureAndLayout = new Runnable() {
        @Override
        public void run() {
            measure(
                    MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.EXACTLY));
            layout(getLeft(), getTop(), getRight(), getBottom());
        }
    };

    //START VideoRendererEventListener
    @Override
    public void onVideoEnabled(DecoderCounters counters) {

    }

    @Override
    public void onVideoDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {

    }

    @Override
    public void onVideoInputFormatChanged(Format format) {

    }

    @Override
    public void onDroppedFrames(int count, long elapsedMs) {

    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        System.out.println("--------> onVideoSizeChanged: " + width +"/" + height +"/" + unappliedRotationDegrees +"/" + pixelWidthHeightRatio);
    }

    @Override
    public void onRenderedFirstFrame(Surface surface) {

    }

    @Override
    public void onVideoDisabled(DecoderCounters counters) {

    }

    //END VideoRendererEventListener
}
