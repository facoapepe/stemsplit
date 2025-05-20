package com.example.castapp;

import android.app.Service;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.WindowManager;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ScreenCaptureService extends Service {
    private MediaProjection mediaProjection;
    private MediaCodec mediaCodec;
    private Surface surface;
    private VirtualDisplay virtualDisplay;
    private boolean isRunning;
    private static int videoBitrate = 5000000; // 5 Mbps default
    private Thread encoderThread;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.hasExtra("data")) {
            Intent data = intent.getParcelableExtra("data");
            startScreenCapture(data);
        }
        return START_NOT_STICKY;
    }

    private void startScreenCapture(Intent data) {
        try {
            MediaProjectionManager projectionManager = 
                    (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            mediaProjection = projectionManager.getMediaProjection(RESULT_OK, data);
            
            setupMediaCodec();
            setupVirtualDisplay();
            startEncoder();
            
            isRunning = true;
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }
    }

    private void setupMediaCodec() throws IOException {
        mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_VP8);
        MediaFormat format = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_VP8,
                getScreenWidth(),
                getScreenHeight()
        );
        format.setInteger(MediaFormat.KEY_BIT_RATE, videoBitrate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        surface = mediaCodec.createInputSurface();
        mediaCodec.start();
    }

    private void setupVirtualDisplay() {
        virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenCapture",
                getScreenWidth(),
                getScreenHeight(),
                getResources().getDisplayMetrics().densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface,
                null,
                null
        );
    }

    private void startEncoder() {
        encoderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                
                while (isRunning) {
                    int outputBufferId = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
                    if (outputBufferId >= 0) {
                        ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferId);
                        // Here you would process the encoded video data
                        // For example, send it to a Cast device or save to file
                        
                        mediaCodec.releaseOutputBuffer(outputBufferId, false);
                    }
                }
            }
        });
        encoderThread.start();
    }

    public static void setVideoBitrate(int bitrate) {
        videoBitrate = bitrate;
    }

    private int getScreenWidth() {
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        return metrics.widthPixels;
    }

    private int getScreenHeight() {
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        return metrics.heightPixels;
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        if (encoderThread != null) {
            try {
                encoderThread.join(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (virtualDisplay != null) {
            virtualDisplay.release();
        }
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}