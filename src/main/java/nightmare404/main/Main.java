package nightmare404.main;

import javax.swing.JFrame;

public class Main {
    // Developing? true = no enemies: every monster starts beaten and every floor's key is in your
    // pocket, so you can walk the whole school freely. Set back to false for the real game.
    // (Or leave it false and run with the VM argument -Dnightmare.noEnemies=true.)
    public static final boolean NO_ENEMIES = false;

	public static void main(String[] args) {
        JFrame window = new JFrame("Nightmare in Room 404");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);

        GamePanel gamePanel = new GamePanel();
        if (NO_ENEMIES || Boolean.getBoolean("nightmare.noEnemies")) gamePanel.peaceful();
        window.add(gamePanel);
        
        // Pack causes the window to be sized to fit the preferred size of GamePanel
        window.pack(); 
        
        window.setLocationRelativeTo(null); // Center the window
        window.setVisible(true);

        // Start the game loop thread
        gamePanel.startGameThread();
    }
}
 