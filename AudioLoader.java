package Project;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class AudioLoader {

    public void playAudio(String name) {
        String filename = "resources/apple_eaten.wav";  // Construct the file path to the .wav file

        try {
            File audioFile = new File(filename);  // Create a File object for the audio file
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);  // Load the audio file

            Clip clip = AudioSystem.getClip();  // Get a Clip object to play the audio
            clip.open(audioStream);  // Open the audio stream for playback
            clip.start();  // Start playing the audio

        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();  // Handle exceptions if the audio file cannot be loaded or played
            System.out.println("Cannot load or play audio file: " + filename);
        }
    }
}
