package com.example.castapp;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import java.io.IOException;
import java.nio.ByteBuffer;

public class AudioCapture {
    private AudioRecord audioRecord;
    private MediaCodec audioCodec;
    private boolean isRunning;
    private Thread recordingThread;
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

    public void startAudioCapture() {
        try {
            setupAudioRecord();
            setupAudioCodec();
            startRecording();
        } catch (IOException e) {
            e.printStackTrace();
            cleanup();
        }
    }

    private void setupAudioRecord() {
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                BUFFER_SIZE
        );
    }

    private void setupAudioCodec() throws IOException {
        audioCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_OPUS);
        MediaFormat format = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_OPUS,
                SAMPLE_RATE,
                2 // Stereo
        );
        format.setInteger(MediaFormat.KEY_BIT_RATE, 128000); // 128 kbps
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, BUFFER_SIZE);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2);
        format.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_STEREO);

        audioCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        audioCodec.start();
    }

    private void startRecording() {
        isRunning = true;
        audioRecord.startRecording();

        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[BUFFER_SIZE];
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

                while (isRunning) {
                    int bytesRead = audioRecord.read(buffer, 0, BUFFER_SIZE);
                    if (bytesRead > 0) {
                        int inputBufferId = audioCodec.dequeueInputBuffer(-1);
                        if (inputBufferId >= 0) {
                            ByteBuffer inputBuffer = audioCodec.getInputBuffer(inputBufferId);
                            inputBuffer.clear();
                            inputBuffer.put(buffer, 0, bytesRead);
                            audioCodec.queueInputBuffer(inputBufferId, 0, bytesRead, 
                                    System.nanoTime() / 1000, 0);
                        }

                        int outputBufferId = audioCodec.dequeueOutputBuffer(bufferInfo, 0);
                        while (outputBufferId >= 0) {
                            ByteBuffer outputBuffer = audioCodec.getOutputBuffer(outputBufferId);
                            // Here you would process the encoded audio data
                            // For example, send it to a Cast device or save to file
                            
                            audioCodec.releaseOutputBuffer(outputBufferId, false);
                            outputBufferId = audioCodec.dequeueOutputBuffer(bufferInfo, 0);
                        }
                    }
                }
            }
        });
        recordingThread.start();
    }

    public void stopAudioCapture() {
        isRunning = false;
        cleanup();
    }

    private void cleanup() {
        if (recordingThread != null) {
            try {
                recordingThread.join(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        if (audioRecord != null) {
            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                audioRecord.stop();
            }
            audioRecord.release();
            audioRecord = null;
        }
        
        if (audioCodec != null) {
            audioCodec.stop();
            audioCodec.release();
            audioCodec = null;
        }
    }
}