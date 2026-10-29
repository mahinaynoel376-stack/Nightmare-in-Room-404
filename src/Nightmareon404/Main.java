package Nightmareon404;
import javax.swing.JFrame;

public class Main {
	public static void main(String[] args) {
        JFrame window = new JFrame("Nightmare in Room 404");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);

        GamePanel gamePanel = new GamePanel();
        window.add(gamePanel);
        
        // Pack causes the window to be sized to fit the preferred size of GamePanel
        window.pack(); 
        
        window.setLocationRelativeTo(null); // Center the window
        window.setVisible(true);

        // Start the game loop thread
        gamePanel.startGameThread();
    }
}
