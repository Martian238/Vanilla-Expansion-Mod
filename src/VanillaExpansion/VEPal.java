package VanillaExpansion;

import arc.graphics.*;
import mindustry.content.Items;
import mindustry.graphics.Pal;

public class VEPal {
    public static Color primary = new Color(0x9a75ffff);
    public static Color blood = new Color(0.5f, 0.1f, 0.1f);
    public static Color paleYellow = new Color(1f, 1f, 0.5f);

    public static Color empathy = new Color(0xffcae9ff);
    public static Color empathyAdd = new Color(0xff7dbcff);
    public static Color empathyDark = new Color(0xff2e93ff);

    public static Color red = new Color(0xf53036ff);
    public static Color redLight = red.cpy().mul(2f);
    public static Color darkRed = new Color(0.5f, 0f, 0f);

    public static Color melt = new Color(0xffa20aff);

    public static Color chordon = new Color(0xffcae9ff);
    public static Color chordonAdd = new Color(0xff7dbcff);
    public static Color chordonDark = new Color(0xff2e93ff);

    public static Color ancient = Items.surgeAlloy.color.cpy().lerp(Pal.accent, 0.115f);
    public static Color ancientLight = ancient.cpy().lerp(Color.white, 0.7f);
    public static Color ancientLightMid = ancient.cpy().lerp(Color.white, 0.4f);
    public static Color ancientDark = ancient.cpy().lerp(Color.black, 0.995f);

    public static Color lightSkyMiddle = new Color(0x78a7ff);
}
