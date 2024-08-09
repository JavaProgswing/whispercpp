package com.jprcoder;

import io.github.ggerganov.whispercpp.WhisperCpp;
import io.github.ggerganov.whispercpp.params.CBool;
import io.github.ggerganov.whispercpp.params.WhisperFullParams;
import io.github.ggerganov.whispercpp.params.WhisperSamplingStrategy;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Whisper {
    private static final WhisperCpp whisper = new WhisperCpp();

    static {
        try {
            whisper.initContext("ggml-base.en.bin");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    static float[] getAudioAsFloat(byte[] b) {
        float[] floats = new float[b.length / 2];

        for (int i = 0, j = 0; i < b.length; i += 2, j++) {
            int intSample = (int) (b[i + 1]) << 8 | (int) (b[i]) & 0xFF;
            floats[j] = intSample / 32767.0f;
        }
        return floats;
    }

    static float[] getAudioAsFloat(File file) {
        try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file)) {
            byte[] b = new byte[audioInputStream.available()];
            float[] floats = new float[b.length / 2];
            audioInputStream.read(b);

            for (int i = 0, j = 0; i < b.length; i += 2, j++) {
                int intSample = (int) (b[i + 1]) << 8 | (int) (b[i]) & 0xFF;
                floats[j] = intSample / 32767.0f;
            }
            return floats;
        } catch (UnsupportedAudioFileException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    static String transcribeFile(final File file) throws IOException {
        WhisperFullParams params = whisper.getFullDefaultParams(WhisperSamplingStrategy.WHISPER_SAMPLING_BEAM_SEARCH);
        // params.setProgressCallback((ctx, state, progress, user_data) -> System.out.println("progress: " + progress));
        params.print_progress = CBool.FALSE;
        return whisper.fullTranscribe(params, getAudioAsFloat(file));
    }

    static String transcribeAudioAsFloats(final float[] floats) throws IOException {
        WhisperFullParams params = whisper.getFullDefaultParams(WhisperSamplingStrategy.WHISPER_SAMPLING_BEAM_SEARCH);
        // params.setProgressCallback((ctx, state, progress, user_data) -> System.out.println("progress: " + progress));
        params.print_progress = CBool.FALSE;
        return whisper.fullTranscribe(params, floats);
    }
}