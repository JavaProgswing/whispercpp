package com.jprcoder;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.jprcoder.AudioHandler.getAudioFormat;

public class SpeechToText extends JFrame {
    private JComboBox<Mixer.Info> deviceComboBox;
    private List<Mixer.Info> availableMixers;
    private JTextArea transcriptLabel;
    private AudioHandler recorder;
    private JButton recordButton, stopButton, playButton, liveButton;
    private LiveSpeechToTextHandler liveHandler;

    public SpeechToText() {
        setTitle("Speech To Text");
        setSize(1920, 1080);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create buttons
        recordButton = new JButton("Record");
        recordButton.setFont(new Font("Arial", Font.PLAIN, 14));
        stopButton = new JButton("Stop");
        stopButton.setFont(new Font("Arial", Font.PLAIN, 14));
        stopButton.setEnabled(false);
        playButton = new JButton("Play");
        playButton.setFont(new Font("Arial", Font.PLAIN, 14));
        playButton.setEnabled(false);
        liveButton = new JButton("Start LIVE");
        liveButton.setFont(new Font("Arial", Font.PLAIN, 14));

        // Button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(recordButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(playButton);
        buttonPanel.add(liveButton);
        add(buttonPanel, BorderLayout.CENTER);

        // Device selection panel
        JPanel devicePanel = new JPanel();
        availableMixers = getAvailableMixers();
        deviceComboBox = new JComboBox<>(availableMixers.toArray(new Mixer.Info[0]));
        recorder = new AudioHandler();
        JLabel selectDevicePrompt = new JLabel("Select Audio Device:");
        selectDevicePrompt.setFont(new Font("Arial", Font.PLAIN, 14));
        devicePanel.add(selectDevicePrompt);
        deviceComboBox.setFont(new Font("Arial", Font.PLAIN, 14));
        devicePanel.add(deviceComboBox);
        transcriptLabel = new JTextArea("Transcript: ...");
        // transcriptLabel.setEditable(false);
        //transcriptLabel.setFont( new Font("Arial", Font.PLAIN, 14));
        add(devicePanel, BorderLayout.NORTH);
        add(transcriptLabel, BorderLayout.WEST);
        liveButton.addActionListener(e -> {
            transcriptLabel.setText("Transcript: ...");
            if (liveButton.getText().equals("Start LIVE")) {
                recordButton.setEnabled(false);
                stopButton.setEnabled(false);
                playButton.setEnabled(false);
                liveButton.setText("Stop LIVE");
                SpeechCallback callback = text -> {
                    updateTranscript(text);
                };
                liveHandler = new LiveSpeechToTextHandler(callback);
                liveHandler.start((Mixer.Info) deviceComboBox.getSelectedItem());
            } else {
                if (liveHandler != null)
                    liveHandler.stop();
                liveButton.setText("Start LIVE");
                stopButton.setEnabled(false);
                playButton.setEnabled(false);
                recordButton.setEnabled(true);
            }
        });

        recordButton.addActionListener(e -> startRecording());

        stopButton.addActionListener(e -> {
            try {
                stopRecording();
            } catch (UnsupportedAudioFileException | IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        playButton.addActionListener(e -> playAudio());

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SpeechToText::new);
    }

    private synchronized void updateTranscript(String text) {
        String tr = String.format("Transcript: %s", text);
        System.out.println(tr);
        transcriptLabel.setText(tr);
    }

    private List<Mixer.Info> getAvailableMixers() {
        List<Mixer.Info> mixerInfoList = new ArrayList<>();
        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            if (mixer.isLineSupported(new DataLine.Info(TargetDataLine.class, getAudioFormat()))) {
                mixerInfoList.add(mixerInfo);
            }
        }
        return mixerInfoList;
    }

    private void startRecording() {
        Mixer.Info selectedMixerInfo = (Mixer.Info) deviceComboBox.getSelectedItem();
        recorder.start(selectedMixerInfo);
        recordButton.setEnabled(false);
        stopButton.setEnabled(true);
        playButton.setEnabled(false);
    }

    private void stopRecording() throws UnsupportedAudioFileException, IOException {
        String transcription = Whisper.transcribeFile(recorder.stopAndGet());
        // JOptionPane.showMessageDialog(this, transcription, "Transcription", JOptionPane.INFORMATION_MESSAGE);
        recordButton.setEnabled(true);
        playButton.setEnabled(true);
        stopButton.setEnabled(false);
        updateTranscript(transcription);
    }

    private void playAudio() {
        if (!recorder.getFile().exists()) {
            JOptionPane.showMessageDialog(this, "No audio file available to play.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Clip clip = AudioSystem.getClip();
            clip.open(AudioSystem.getAudioInputStream(recorder.getFile()));
            clip.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
