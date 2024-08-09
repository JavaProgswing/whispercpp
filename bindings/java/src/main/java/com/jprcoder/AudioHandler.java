package com.jprcoder;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AudioHandler {
    private static final String tempPath = String.valueOf(Paths.get(System.getenv("Temp"), "WhisperSpeechToText"));
    private static final int step_ms = 3000, keep_ms = 200, WHISPER_SAMPLE_RATE = 16000;

    static {
        try {
            Files.createDirectories(Path.of(tempPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final AudioFormat audioFormat;
    private File file;
    private TargetDataLine targetDataLine;
    private boolean isRecording;

    public AudioHandler(final File file) {
        this.file = file;
        audioFormat = getAudioFormat();
    }

    public AudioHandler() {
        generateNewFile();
        audioFormat = getAudioFormat();
    }

    public static AudioFormat getAudioFormat() {
        return new AudioFormat(WHISPER_SAMPLE_RATE, 16, 1, true, false);
    }

    public static int getBytesReadLength() {
        return (step_ms / 1000) * WHISPER_SAMPLE_RATE;
    }

    public static int getKeepSamplesLength() {
        return (keep_ms / 1000) * WHISPER_SAMPLE_RATE;
    }

    public void generateNewFile() {
        file = new File(Paths.get(tempPath, String.format("%d.wav", System.currentTimeMillis())).toString());
    }

    public File getFile() {
        return file;
    }

    public void start(Mixer.Info selectedMixerInfo) {
        if (isRecording) return;
        isRecording = true;

        Mixer mixer = AudioSystem.getMixer(selectedMixerInfo);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);

        try {
            targetDataLine = (TargetDataLine) mixer.getLine(info);
            targetDataLine.open(audioFormat);

            startParallelRecording();
        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
        }
    }

    public File stopAndGet() {
        if (!isRecording) return null;
        isRecording = false;
        targetDataLine.stop();
        targetDataLine.close();
        File copyFile = file;
        generateNewFile();
        return copyFile;
    }

    byte[] readBytes(int byteLength) throws IOException {
        targetDataLine.start();
        AudioInputStream audioStream = new AudioInputStream(targetDataLine);
        byte[] result = new byte[byteLength];
        audioStream.read(result, 0, byteLength);
        return result;
    }

    void startParallelRecording() {
        Thread recordingThread = new Thread(() -> {
            targetDataLine.start();
            AudioInputStream audioStream = new AudioInputStream(targetDataLine);
            try {
                AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, file);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        recordingThread.start();
    }
}
