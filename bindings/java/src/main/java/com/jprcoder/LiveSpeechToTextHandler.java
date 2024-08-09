package com.jprcoder;

import javax.sound.sampled.Mixer;
import java.io.IOException;

import static com.jprcoder.AudioHandler.getBytesReadLength;
import static com.jprcoder.AudioHandler.getKeepSamplesLength;
import static com.jprcoder.Whisper.getAudioAsFloat;

public class LiveSpeechToTextHandler {
    private final int samplesKeepLength;
    private final AudioHandler handler;
    private final int bytesReadLength;
    private boolean status = false;
    private float[] oldAudioTake;
    private SpeechCallback callback;

    public LiveSpeechToTextHandler(SpeechCallback callback) {
        this.handler = new AudioHandler();
        samplesKeepLength = getKeepSamplesLength();
        oldAudioTake = new float[samplesKeepLength];
        this.callback = callback;
        bytesReadLength = getBytesReadLength();
    }

    public void start(Mixer.Info selectedMixerInfo) {
        if (status)
            return;
        status = true;

        new Thread(() -> {
            handler.start(selectedMixerInfo);
            while (status) {
                byte[] result;
                try {
                    result = handler.readBytes(bytesReadLength);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                float[] newAudio = getAudioAsFloat(result);

                float[] audio = new float[newAudio.length + oldAudioTake.length];
                System.arraycopy(oldAudioTake, 0, audio, 0, samplesKeepLength);
                System.arraycopy(newAudio, 0, audio, samplesKeepLength, newAudio.length);
                try {
                    callback.callback(Whisper.transcribeAudioAsFloats(audio));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                final int srcPos = Math.max(newAudio.length - samplesKeepLength, 0);
                final int length = Math.min(newAudio.length, samplesKeepLength);
                System.arraycopy(newAudio, srcPos, oldAudioTake, 0, length);
            }
        }).start();
    }

    public void stop() {
        status = false;
    }
}
