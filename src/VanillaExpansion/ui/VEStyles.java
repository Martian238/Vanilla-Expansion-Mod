package VanillaExpansion.ui;

import arc.graphics.Color;
import arc.scene.ui.ImageButton;
import mindustry.ui.Styles;

public class VEStyles {
    public static final Color DEFAULT_COLOR = Color.white;
    public static final Color ADD_COLOR = Color.valueOf("6ee787");
    public static final Color REMOVE_COLOR = Color.valueOf("ff6b6b");
    public static final Color TOGGLE_COLOR = Color.valueOf("59d6ff");

    public static ImageButton.ImageButtonStyle square;
    public static ImageButton.ImageButtonStyle squareAdd;
    public static ImageButton.ImageButtonStyle squareRemove;
    public static ImageButton.ImageButtonStyle squareToggle;

    public static void load() {
        if (square != null) return;
        ImageButton.ImageButtonStyle base = Styles.squarei != null ? Styles.squarei : Styles.defaulti;
        if (base == null) base = new ImageButton.ImageButtonStyle();
        square = new ImageButton.ImageButtonStyle(base);
        squareAdd = new ImageButton.ImageButtonStyle(base);
        squareRemove = new ImageButton.ImageButtonStyle(base);
        squareToggle = new ImageButton.ImageButtonStyle(base);
        apply(square, DEFAULT_COLOR);
        apply(squareAdd, ADD_COLOR);
        apply(squareRemove, REMOVE_COLOR);
        apply(squareToggle, TOGGLE_COLOR);
    }

    private static void apply(ImageButton.ImageButtonStyle style, Color color) {
        style.imageUpColor = color;
        style.imageOverColor = color;
        style.imageDownColor = color;
        style.imageCheckedColor = color;
    }
}
