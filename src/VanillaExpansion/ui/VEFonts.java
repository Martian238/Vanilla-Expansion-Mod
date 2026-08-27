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

public class VEFonts {
    public static Font novo;
    public static void loadFonts(){
        Fi root = Vars.mods.getMod(VanillaExpansionMod.class).root;
        Log.info("mod root: " + root.path() + " exists=" + root.exists());
        Fi fi = findFont(root);
        if(fi == null){
            fi = Core.files.internal("fonts/novo.ttf");
            Log.info("fallback internal font path: " + fi.path() + " exists=" + fi.exists());
        }
        if(fi != null && fi.exists()) {
            gen(fi);
            Log.info("Font loaded from " + fi.path());
        }else{
            Log.info("Font not found");
        }
    }

    private static Fi findFont(Fi dir){
        if(dir == null || !dir.exists() || !dir.isDirectory()) return null;
        for(Fi f : dir.list()){
            if(f.isDirectory()){
                Fi found = findFont(f);
                if(found != null) return found;
            }else if(f.name().equals("novo.ttf")){
                Log.info("found font: " + f.path());
                return f;
            }
        }
        return null;
    }

    public static void gen(Fi fi){
        FreeTypeFontGenerator.FreeTypeFontParameter param = fontParameter();
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(fi);
        //FontLoader.FontParameter params = new FontLoader.FontParameter();
        novo = generator.generateFont(param);
    }

    static FreeTypeFontGenerator.FreeTypeFontParameter fontParameter(){
        return new FreeTypeFontGenerator.FreeTypeFontParameter(){{
            size = 32;
            shadowColor = Color.white;
            shadowOffsetY = 0;
            incremental = true;
        }};
    }
}
