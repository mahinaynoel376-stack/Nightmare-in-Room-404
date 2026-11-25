package nightmare404.main;

// The choices on the main menu's Options list (see MainMenu). They last until the game is closed.
public final class Settings {
    // The word battle's turn timer: a clock on each of the player's turns (see BattleScreen)
    public static boolean turnTimer = true;

    // Flickering lights: the hallways' ceiling lights (see Lighting), the word battle's tube light,
    // and the main menu's. Off for anyone who finds flashing uncomfortable.
    public static boolean flicker = true;

    private Settings() { }
}
