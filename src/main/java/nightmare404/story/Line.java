package nightmare404.story;

// One box of text in the dialogue box (see nightmare404.ui.DialogueBox), and how it's shown:
//   SPEECH     someone talking: their name over the box and their face beside the text
//   THOUGHT    Flor thinking: in italics, no face
//   PAPER      something written down (a logbook page, a memo): on lined paper, with its title
//   NARRATION  what happens: plain text, no name
//   TIP        how to play: in gold, so it's clear it isn't the story talking
// portrait is a sprite name for BattleScreen.Sprites ("principal", or a full path like
// "/characters/player/down_idle"), or null for none.
public class Line {
    public enum Style { SPEECH, THOUGHT, PAPER, NARRATION, TIP }

    private final Style style;
    private final String speaker;
    private final String portrait;
    private final String text;

    public Line(Style style, String speaker, String portrait, String text) {
        this.style = style;
        this.speaker = speaker;
        this.portrait = portrait;
        this.text = text;
    }

    public Style style() {
        return style;
    }

    public String speaker() {
        return speaker;
    }

    public String portrait() {
        return portrait;
    }

    public String text() {
        return text;
    }

    public static final String FLOR = "Flor";
    public static final String FLOR_FACE = "/characters/player/down_idle";

    public static Line flor(String text) {
        return new Line(Style.SPEECH, FLOR, FLOR_FACE, text);
    }

    public static Line say(String speaker, String portrait, String text) {
        return new Line(Style.SPEECH, speaker, portrait, text);
    }

    public static Line think(String text) {
        return new Line(Style.THOUGHT, FLOR, null, text);
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
