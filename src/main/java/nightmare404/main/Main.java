package nightmare404.main;

import java.awt.Dimension;
import javax.swing.JComponent;
import javax.swing.JFrame;

import nightmare404.battle.BattleScreen;
import nightmare404.ui.MainMenu;

// Opens the window on the main menu, and swaps between the menu and the game in it. The menu's
// lines call these methods (see MainMenu):
//   Start Game  startGame: a new game (dropping any game in progress)
//   Continue    continueGame: back to the game in progress, just as it was left
//   Journal     openJournal: the game in progress with its journal open; closing it comes back here
//   Options     handled by the menu itself (see Settings)
//   Quit        quit: closes the game
// Esc in the game, and Enter after the ending, call showMenu.
public class Main {
    // Developing? true = no enemies: every monster starts beaten and every floor's key is in your
    // pocket, so you can walk the whole school freely. Set back to false for the real game.
    // (Or leave it false and run with the VM argument -Dnightmare.noEnemies=true.)
    public static final boolean NO_ENEMIES = false;

    private static JFrame window;
    private static MainMenu menu;
    private static GamePanel game;          // The game in progress (null before Start Game, and after the ending)

	public static void main(String[] args) {
        // Read the word battle's 170,000-word dictionary now (it takes a moment), so battles open at once
        BattleScreen.dictionary();

        window = new JFrame("Nightmare in Room 404");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);

        menu = new MainMenu();
        menu.setPreferredSize(new Dimension(32 * 25, 32 * 19));   // The game's screen: 25 x 19 tiles of 32 px (see GamePanel)
        menu.setGameInProgress(false);
        window.setContentPane(menu);

        // Pack causes the window to be sized to fit the preferred size of the menu (the same as the game's)
        window.pack();
        window.setLocationRelativeTo(null); // Center the window
        window.setVisible(true);
        menu.requestFocusInWindow();
    }

    public static void startGame() {
        if (game != null) game.stop();
        game = new GamePanel();
        if (NO_ENEMIES || Boolean.getBoolean("nightmare.noEnemies")) game.peaceful();
        show(game);
        game.start();                       // Start the game loop
    }

    public static void continueGame() {
        if (game == null) return;
        show(game);
        game.start();
    }

    public static void openJournal() {
        if (game == null) return;
        game.openJournalFromMenu();
        continueGame();
    }

    // Esc in the game, or Enter after the ending (when the game is over and is dropped)
    public static void showMenu() {
        if (game != null && game.ended) {
            game.stop();
            game = null;
        }
        menu.setGameInProgress(game != null);
        show(menu);
    }

    // Puts the menu or the game in the window
    private static void show(JComponent screen) {
        window.setContentPane(screen);
        window.revalidate();
        window.repaint();
        screen.requestFocusInWindow();
    }

    public static void quit() {
        window.dispose();
        System.exit(0);
    }
}
