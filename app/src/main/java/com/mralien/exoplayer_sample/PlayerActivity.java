package com.mralien.exoplayer_sample;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashChunkSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsDataSourceFactory;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

public class PlayerActivity extends AppCompatActivity {
    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
    private static final String TAG = PlayerActivity.class.getSimpleName();

    private PlayerView playerView;
    private SimpleExoPlayer player;
    private boolean playWhenReady = true;
    private int currentWindow = 0;
    private long playbackPosition = 0;
    private String url;
    private int mediaType;
    private ComponentListener componentListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        componentListener = new ComponentListener();
        playerView = findViewById(R.id.player_view);
        Bundle bundle = getIntent().getExtras();
        url = bundle.getString(Constants.BUNDLE_KEY_URL);
        mediaType = bundle.getInt(Constants.BUNDLE_KEY_TYPE);

    }

    /* Starting with API level 24 Android supports multiple windows.
     * As our app can be visible but not active in split window mode, we need to initialize
     * the player in onStart. Before API level 24 we wait as long as possible until
     * we grab resources, so we wait until onResume before initializing the player.
     */
    @Override
    protected void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            initPlayer();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        if ((Util.SDK_INT <= 23 || player == null)) {
            initPlayer();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Util.SDK_INT <= 23) {
            releasePlayer();
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }

    /* Implementation detail to have a pure full screen experience */
    @SuppressLint("InlineAPI")
    private void hideSystemUI() {
        playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    private void initPlayer() {
        /* Create player */
        switch (mediaType) {
            case Constants.TYPE_MP3:
            case Constants.TYPE_MP4:
            case Constants.TYPE_PLAYLIST:
                player = ExoPlayerFactory.newSimpleInstance(new DefaultRenderersFactory(this),
                        new DefaultTrackSelector(), new DefaultLoadControl());
                break;
            case Constants.TYPE_DASH:
            case Constants.TYPE_HLS:
                TrackSelection.Factory adaptiveTrackSelectionFactory =
                        new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);
                player = ExoPlayerFactory.newSimpleInstance(
                        new DefaultRenderersFactory(this),
                        new DefaultTrackSelector(adaptiveTrackSelectionFactory),
                        new DefaultLoadControl());
                break;
            default:
                break;
        }


        playerView.setPlayer(player);
        player.addListener(componentListener);
        player.addVideoDebugListener(componentListener);
        player.addAudioDebugListener(componentListener);

        player.setPlayWhenReady(playWhenReady);
        player.seekTo(currentWindow, playbackPosition);

        /* Create media source */
        Uri uri = Uri.parse(url);
//        Uri uri = Uri.parse(getString(R.string.media_url_mp3));
        MediaSource mediaSource = buildMediaSource(mediaType, uri);
        player.prepare(mediaSource, true, false);
    }

    private MediaSource buildMediaSource(int mediaType, Uri uri) {
        MediaSource mediaSource = null;
        String userAgent = Util.getUserAgent(this, getString(R.string.app_name));
        DefaultHttpDataSourceFactory httpDataSourceFactory = new DefaultHttpDataSourceFactory(userAgent,
                null, DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
                true);
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(this,
                null, httpDataSourceFactory);

        switch (mediaType) {
            case Constants.TYPE_MP3:
            case Constants.TYPE_MP4:
                mediaSource = new ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
                break;
            case Constants.TYPE_PLAYLIST:
                Uri audioUri = Uri.parse(getString(R.string.media_url_mp3));
                MediaSource audioSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(audioUri);
                Uri videoUri = Uri.parse(getString(R.string.media_url_mp4));
                MediaSource videoSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(videoUri);
                mediaSource = new ConcatenatingMediaSource(audioSource, videoSource);
                break;
            case Constants.TYPE_DASH:
                DataSource.Factory manifestDataSourceFactory = new DefaultHttpDataSourceFactory(userAgent);
                DashChunkSource.Factory dashChunkSourceFactory = new DefaultDashChunkSource.Factory(
                        new DefaultHttpDataSourceFactory(userAgent, BANDWIDTH_METER));
                mediaSource = new DashMediaSource.Factory(dashChunkSourceFactory, manifestDataSourceFactory)
                        .createMediaSource(uri);
                break;
            case Constants.TYPE_HLS:
                mediaSource = new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
                break;
            default:
                break;
        }
        return mediaSource;
    }

    private void releasePlayer() {
        if (player != null) {
            playbackPosition = player.getCurrentPosition();
            currentWindow = player.getCurrentWindowIndex();
            playWhenReady = player.getPlayWhenReady();
            player.removeListener(componentListener);
            player.removeVideoDebugListener(componentListener);
            player.removeAudioDebugListener(componentListener);
            player.release();
            player = null;
        }
    }

    private class ComponentListener extends Player.DefaultEventListener
            implements VideoRendererEventListener, AudioRendererEventListener {
        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            super.onPlayerStateChanged(playWhenReady, playbackState);
            String stateString;
            switch (playbackState) {
                case Player.STATE_IDLE:
                    stateString = "Player.STATE_IDLE        - ";
                    break;
                case Player.STATE_BUFFERING:
                    stateString = "Player.STATE_BUFFERING   - ";
                    break;
                case Player.STATE_READY:
                    stateString = "Player.STATE_READY       - ";
                    break;
                case Player.STATE_ENDED:
                    stateString = "Player.STATE_ENDED       - ";
                    break;
                default:
                    stateString = "UNKNOWN_STATE            - ";
                    break;
            }
            Log.d(TAG, "changed state to " + stateString
                + "playWhenReady: " + playWhenReady);
        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            super.onTracksChanged(trackGroups, trackSelections);
            /* ExoPlayer.EventListener.onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray
             * trackSelections) informs about track changes. At least when track selection
             * is downgraded to a lower quality we want to know about it.
             */
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            super.onPlayerError(error);
        }

        @Override
        public void onAudioEnabled(DecoderCounters counters) {

        }

        @Override
        public void onAudioSessionId(int audioSessionId) {

        }

        @Override
        public void onAudioDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {

        }

        @Override
        public void onAudioInputFormatChanged(Format format) {

        }

        @Override
        public void onAudioSinkUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {

        }

        @Override
        public void onAudioDisabled(DecoderCounters counters) {

        }

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

        }

        @Override
        public void onRenderedFirstFrame(Surface surface) {

        }

        @Override
        public void onVideoDisabled(DecoderCounters counters) {

        }
    }
}
