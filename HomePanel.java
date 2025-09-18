package Project;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class HomePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private MainApp parent;

    public HomePanel(MainApp parent) {
        this.parent = parent;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.DARK_GRAY);

        add(Box.createVerticalStrut(80));

        // Title Label
        JLabel title = new JLabel("Java3D Snake Game");
        title.setFont(new Font("Arial", Font.BOLD, 32));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(title);

        add(Box.createVerticalStrut(40));

        // Start Button
        JButton startButton = new JButton("Start");
        startButton.setFont(new Font("Arial", Font.BOLD, 24));
        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showSelectMapDialog(); // Show map selection dialog
            }
        });
        add(startButton);

        add(Box.createVerticalStrut(20));

        // How to Play Button
        JButton howToPlayButton = new JButton("How to Play");
        howToPlayButton.setFont(new Font("Arial", Font.BOLD, 24));
        howToPlayButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        howToPlayButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showHowToPlay(); // Show the "How to Play" instructions
            }
        });
        add(howToPlayButton);

        add(Box.createVerticalStrut(20));

        // Exit Button
        JButton exitButton = new JButton("Exit");
        exitButton.setFont(new Font("Arial", Font.BOLD, 24));
        exitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        exitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0); // Exit the game
            }
        });
        add(exitButton);
    }

    // Show the map selection dialog
    private void showSelectMapDialog() {
        // Create the map selection dialog with options
        String[] options = {"Ground Map", "Ocean Map"};
        int selection = JOptionPane.showOptionDialog(
            this,
            "Select a map for the game:",
            "Select Map",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            options,
            options[0] // Default selection
        );

        // If the user selected a map, proceed
        if (selection != -1) {
            String selectedMap = "";
            switch (selection) {
                case 0: selectedMap = "groundTexture"; break;
                case 1: selectedMap = "oceanTexture"; break;
            }

            // Show the difficulty selection dialog
            showSelectDifficultyDialog(selectedMap);
        }
    }

    // Show the difficulty selection dialog
    private void showSelectDifficultyDialog(String selectedMap) {
        // Create the difficulty selection dialog with options
        String[] difficultyOptions = {"Easy", "Hard"};
        int difficultySelection = JOptionPane.showOptionDialog(
            this,
            "Select difficulty level:",
            "Select Difficulty",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            difficultyOptions,
            difficultyOptions[0] // Default selection
        );

        // If the user selected a difficulty, proceed
        if (difficultySelection != -1) {
            String selectedDifficulty = difficultySelection == 0 ? "Easy" : "Hard";
            
            // If "Hard" is selected, show the alert message
            if (selectedDifficulty.equals("Hard")) {
                JOptionPane.showMessageDialog(
                    this,
                    "Be Alert! The user cannot zoom in or out and can only see their immediate surroundings!\nBest of Luck!",
                    "Hard Difficulty Alert",
                    JOptionPane.WARNING_MESSAGE
                );
            }   
            
            // Pass the selected map and difficulty to MainApp and show the game
            parent.showGame(selectedMap, selectedDifficulty);
        }
    }

    // Show instructions on how to play the game
    private void showHowToPlay() {
        String instructions = "Movement Controls:\n" +
                "Use the left and right arrow keys to move the snake.\n\n" +
                "Camera Controls:\n" +
                "Use the WASD keys to adjust the camera view.\n" +
                "Use the Z key to zoom in and the key X to zoom out.\n\n" +
                "Objective:\n" +
                "Eat apples to grow the snake and avoid running into walls or yourself!";
        
        // Display the instructions in a message box
        JOptionPane.showMessageDialog(this, instructions, "How to Play", JOptionPane.INFORMATION_MESSAGE);
    }
}
