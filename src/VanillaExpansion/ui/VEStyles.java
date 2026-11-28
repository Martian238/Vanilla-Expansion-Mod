package VanillaExpansion.ui;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.scene.style.BaseDrawable;
import arc.scene.style.Drawable;
import arc.scene.ui.ImageButton;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.Slider;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Scl;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;

public class VEStyles {
    public static final Color DEFAULT_COLOR = Color.white;
    public static final Color ADD_COLOR = Color.valueOf("6ee787");
    public static final Color REMOVE_COLOR = Color.valueOf("ff6b6b");
    public static final Color TOGGLE_COLOR = Color.valueOf("59d6ff");
    public static final Color DIALOG_FILL = Color.valueOf("121212");
    public static final Color DIALOG_BORDER = Color.valueOf("4a4a4a");
    public static final Color PANEL_BORDER = Color.valueOf("3a3a3a");
    public static final Color CARD_BORDER = Color.valueOf("363636");
    public static final Color BUTTON_FILL = Color.valueOf("242424");
    public static final Color BUTTON_HOVER = Color.valueOf("343434");
    public static final Color BUTTON_DOWN = Color.valueOf("454545");
    public static final Color BUTTON_CHECKED = Color.valueOf("2c2c2c");
    public static final Color BUTTON_BORDER = Color.valueOf("505050");
    public static final Color BUTTON_HOVER_BORDER = Color.valueOf("6a6a6a");
    public static final Color SELECTED_BORDER = Color.valueOf("ffd37f");
    public static final Color SCROLL_TRACK = Color.valueOf("202020");
    public static final Color SCROLL_KNOB = Color.valueOf("555555");
    public static final Color SLIDER_TRACK = Color.valueOf("2a2a2a");
    public static final Color SLIDER_BORDER = Color.valueOf("505050");
    public static final Color SLIDER_FILL = Color.valueOf("ffd37f");
    public static final Color SLIDER_KNOB = Color.valueOf("ffd37f");
    public static final float DIALOG_BORDER_WIDTH = 3f;
    public static final float PANEL_BORDER_WIDTH = 2f;
    public static final float BUTTON_BORDER_WIDTH = 2f;
    public static final float FRAME_WIDTH = 2f;
    public static final float FRAME_PAD = 2f;

    public static ImageButton.ImageButtonStyle square;
    public static ImageButton.ImageButtonStyle squareAdd;
    public static ImageButton.ImageButtonStyle squareRemove;
    public static ImageButton.ImageButtonStyle squareToggle;
    public static TextButton.TextButtonStyle category;
    public static TextButton.TextButtonStyle back;
    public static Drawable dialog;
    public static Drawable panel;
    public static Drawable card;
    public static Drawable buttonUp;
    public static Drawable buttonOver;
    public static Drawable buttonDown;
    public static Drawable buttonChecked;
    public static Drawable selection;
    public static ScrollPane.ScrollPaneStyle scroll;
    public static Slider.SliderStyle slider;

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

        dialog = new RectDrawable(DIALOG_FILL, DIALOG_BORDER, DIALOG_BORDER_WIDTH);
        panel = new RectDrawable(Color.clear, PANEL_BORDER, PANEL_BORDER_WIDTH);
        card = new RectDrawable(Color.clear, CARD_BORDER, BUTTON_BORDER_WIDTH);

        buttonUp = new RectDrawable(BUTTON_FILL, BUTTON_BORDER, BUTTON_BORDER_WIDTH);
        buttonOver = new RectDrawable(BUTTON_HOVER, BUTTON_HOVER_BORDER, BUTTON_BORDER_WIDTH);
        buttonDown = new RectDrawable(BUTTON_DOWN, BUTTON_HOVER_BORDER, BUTTON_BORDER_WIDTH);
        buttonChecked = new RectDrawable(BUTTON_CHECKED, BUTTON_BORDER, BUTTON_BORDER_WIDTH);

        selection = new RectDrawable(Color.clear, SELECTED_BORDER, FRAME_WIDTH);
        selection.setMinWidth(1f);
        selection.setMinHeight(1f);

        category = new TextButton.TextButtonStyle();
        category.up = new RectDrawable(Color.clear, Color.clear, 0f);
        category.over = new RectDrawable(Color.clear, Color.clear, 0f);
        category.down = new RectDrawable(Color.clear, Color.clear, 0f);
        category.checked = new RectDrawable(Color.clear, Color.clear, 0f);
        category.checkedOver = new RectDrawable(Color.clear, Color.clear, 0f);
        category.font = Fonts.def;
        category.fontColor = Color.white;
        category.disabledFontColor = Color.gray;

        back = new TextButton.TextButtonStyle(category);
        back.up = buttonUp;
        back.over = buttonOver;
        back.down = buttonDown;
        back.checked = buttonUp;
        back.checkedOver = buttonOver;

        scroll = new ScrollPane.ScrollPaneStyle();
        RectDrawable scrollTrack = new RectDrawable(SCROLL_TRACK, Color.clear, 0f);
        scrollTrack.setMinWidth(10f);
        RectDrawable scrollKnob = new RectDrawable(SCROLL_KNOB, Color.clear, 0f);
        scrollKnob.setMinWidth(10f);
        scrollKnob.setMinHeight(24f);
        scroll.vScroll = scrollTrack;
        scroll.vScrollKnob = scrollKnob;

        slider = new Slider.SliderStyle();
        RectDrawable sliderTrack = new RectDrawable(SLIDER_TRACK, SLIDER_BORDER, 1f);
        sliderTrack.setMinHeight(8f);
        RectDrawable sliderFill = new RectDrawable(SLIDER_FILL, Color.clear, 0f);
        sliderFill.setMinHeight(8f);
        RectDrawable sliderKnob = new RectDrawable(SLIDER_KNOB, Color.clear, 0f);
        sliderKnob.setMinWidth(14f);
        sliderKnob.setMinHeight(22f);
        slider.background = sliderTrack;
        slider.knobBefore = sliderFill;
        slider.knob = sliderKnob;
        slider.knobOver = sliderKnob;
        slider.knobDown = sliderKnob;
    }

    private static void apply(ImageButton.ImageButtonStyle style, Color color) {
        style.imageUpColor = color;
        style.imageOverColor = color;
        style.imageDownColor = color;
        style.imageCheckedColor = color;
    }

    public static class RectDrawable extends BaseDrawable {
        public Color fill;
        public Color border;
        public float borderWidth;

        public RectDrawable(Color fill, Color border, float borderWidth) {
            this.fill = fill;
            this.border = border;
            this.borderWidth = borderWidth;
            setMinWidth(1f);
            setMinHeight(1f);
        }

        @Override
        public void draw(float x, float y, float width, float height) {
            float alpha = Draw.getColorAlpha();
            if (fill != null && fill.a > 0f) {
                Draw.color(fill.r, fill.g, fill.b, fill.a * alpha);
                Fill.rect(x, y, width, height);
            }
            if (border != null && border.a > 0f && borderWidth > 0f) {
                float thick = Scl.scl(borderWidth);
                Draw.color(border.r, border.g, border.b, border.a * alpha);
                Lines.stroke(thick);
                Lines.rect(x + thick / 2f, y + thick / 2f, width - thick, height - thick);
            }
            Draw.reset();
        }

        @Override
        public void draw(float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation) {
            draw(x, y, width * scaleX, height * scaleY);
        }
    }
}