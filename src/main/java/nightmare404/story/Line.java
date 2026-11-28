package nightmare404.story;

// One box of text in the dialogue box (see nightmare404.ui.DialogueBox), and how it's shown:
//   SPEECH     someone talking: their name over the box and their face beside the text
//   THOUGHT    Kai thinking: in italics, no face
//   PAPER      something written down (a logbook page, a memo): on lined paper, with its title
//   NARRATION  what happens: plain text, no name
//   TIP        how to play: in gold, so it's clear it isn't the story talking
// portrait is a sprite name for BattleScreen.Sprites ("principal", or a full path like
// "/characters/player/down_idle"), or null for none.
public record Line(Style style, String speaker, String portrait, String text) {
    public enum Style { SPEECH, THOUGHT, PAPER, NARRATION, TIP }

    public static final String KAI = "Kai";
    public static final String KAI_FACE = "/characters/player/down_idle";

    public static Line kai(String text) {
        return new Line(Style.SPEECH, KAI, KAI_FACE, text);
    }

    public static Line say(String speaker, String portrait, String text) {
        return new Line(Style.SPEECH, speaker, portrait, text);
    }

    public static Line think(String text) {
        return new Line(Style.THOUGHT, KAI, null, text);
    }

    public static Line paper(String title, String text) {
        return new Line(Style.PAPER, title, null, text);
    }

    public static Line narrate(String text) {
        return new Line(Style.NARRATION, null, null, text);
    }

    public static Line tip(String text) {
        return new Line(Style.TIP, "How to play", null, text);
    }
}
