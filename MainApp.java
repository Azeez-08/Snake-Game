package Project;

import javax.swing.*;
import java.awt.*;

public class MainApp extends JFrame {
    private static final long serialVersionUID = 1L;

    private CardLayout cardLayout;
    private JPanel mainPanel;
    private HomePanel homePanel;
    private GamePanel gamePanel;

    // We store a static reference so that GamePanel (or Snake) can call showHome()
    public static MainApp instance;

    // Store the selected map (default is groundTexture)
    private String selectedMap;

    // Store the selected difficulty (default is Easy)
    private String selectedDifficulty;

    public MainApp() {
        instance = this;
        setTitle("Java3D Snake Game");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // center on screen

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        getContentPane().add(mainPanel, BorderLayout.CENTER);

        homePanel = new HomePanel(this);
        gamePanel = new GamePanel(this);

        mainPanel.add(homePanel, "home");
        mainPanel.add(gamePanel, "game");

        // Show home screen on startup.
        cardLayout.show(mainPanel, "home");
    }
    
    public void showGameOverScreen() {
        // Create a new instance of GameOverPanel and add it to the card layout
        GameOverPanel gameOverPanel = new GameOverPanel(this);
        mainPanel.add(gameOverPanel, "gameover");
        cardLayout.show(mainPanel, "gameover");
    }

    // Called by the HomePanel Start button.
    // Modified to accept both the selected map and difficulty as arguments.
    public void showGame(String selectedMap, String selectedDifficulty) {
        this.selectedMap = selectedMap; // Store the selected map
        this.selectedDifficulty = selectedDifficulty; // Store the selected difficulty

        // Pass the selected map and difficulty to the GamePanel
        gamePanel.setSelectedMap(selectedMap);
        gamePanel.setSelectedDifficulty(selectedDifficulty);

        // Switch to the game screen and start 3D rendering
        cardLayout.show(mainPanel, "game");
        gamePanel.start3D();
    }

    // Called by GamePanel when the snake hits the border.
    public void showHome() {
        gamePanel.stop3D(); // optional cleanup
        cardLayout.show(mainPanel, "home");
    }

    // Getter methods to access the selected map and difficulty in GamePanel or other components
    public String getSelectedMap() {
        return selectedMap;
    }

    public String getSelectedDifficulty() {
        return selectedDifficulty;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainApp().setVisible(true);
        });
    }
}
