package VanillaExpansion.ui;

import VanillaExpansion.VanillaExpansionMod;
import arc.Core;
import arc.assets.loaders.FontLoader;
import arc.files.Fi;
import arc.freetype.FreeTypeFontGenerator;
import arc.freetype.FreetypeFontLoader;
import arc.graphics.Color;
import arc.graphics.g2d.Font;
import arc.util.Log;
import mindustry.Vars;
import mindustry.ui.Fonts;

import java.util.Arrays;

import static mindustry.Vars.tree;

public class VEFonts {
    public static Font novo;
    public static void loadFonts(){

        Vars.tree.get("fonts/" + "novo.ttf");
        Fi fi = Core.files.internal("fonts/" + "novo.ttf");
        Log.info("font file " + fi.path() + " " + fi.exists());
        if(fi.exists()) {
            gen(fi);
            Log.info("Font loaded");
        }else{
            Log.info("Font not found");
        }
    }

    public static void gen(Fi fi){
        FreeTypeFontGenerator.FreeTypeFontParameter param = fontParameter();
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(fi);
        //FontLoader.FontParameter params = new FontLoader.FontParameter();
        novo = generator.generateFont(param);
    }

    static FreeTypeFontGenerator.FreeTypeFontParameter fontParameter(){
        return new FreeTypeFontGenerator.FreeTypeFontParameter(){{
            size = 128;
            shadowColor = Color.white;
            shadowOffsetY = 0;
            incremental = true;
        }};
    }
}
