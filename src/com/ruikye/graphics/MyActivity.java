package com.ruikye.graphics;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.ruikye.graphics.view.GLWaveformView;
import com.ruikye.graphics.view.WaveformView;

public class MyActivity extends Activity {
    private static final int MSG_GET_VOLUME = 0x1001;
    private WaveformView mWaveformView;
    //private GLWaveformView mGLWaveformView;
    private Handler mHandler;
    private RecordThread mRecordThread;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mWaveformView = (WaveformView) findViewById(R.id.waveform_view);
        //mGLWaveformView = (GLWaveformView) findViewById(R.id.gl_waveform_view);
        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (msg.what == MSG_GET_VOLUME) {
                    update((Float) msg.obj);
                }
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRecordThread = new RecordThread(mHandler);
        mRecordThread.start();
        //mGLWaveformView.onResume();
    }

    @Override
    protected void onPause() {
        //mGLWaveformView.onPause();
        if (mRecordThread != null) {
            mRecordThread.pause();
            mRecordThread = null;
        }
        super.onPause();
    }

    private void update(final float volume) {
        mWaveformView.post(new Runnable() {
            @Override
            public void run() {
                mWaveformView.updateAmplitude(volume * 0.1f / 4000);
            }
        });
    }

    static class RecordThread extends Thread {
        private AudioRecord ar;
        private int bs;
        private final int SAMPLE_RATE_IN_HZ = 8000;
        private boolean isRun = false;
        private Handler mHandler;

        public RecordThread(Handler handler) {
            super();
            bs = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ,
                    AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            ar = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE_IN_HZ,
                    AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, bs);
            mHandler = handler;
        }

        public void run() {
            super.run();
            ar.startRecording();
            byte[] buffer = new byte[bs];
            isRun = true;
            while (running()) {
                int r = ar.read(buffer, 0, bs);
                int v = 0;
                for (byte aBuffer : buffer) {
                    v += aBuffer * aBuffer;
                }

                Message msg = mHandler.obtainMessage(MSG_GET_VOLUME, v * 1f / r);
                mHandler.sendMessage(msg);
            }
            ar.stop();
        }

        public synchronized void pause() {
            isRun = false;
        }

        private synchronized boolean running() {
            return isRun;
        }

        public void start() {
            if (!running()) {
                super.start();
            }
        }
    }
}
