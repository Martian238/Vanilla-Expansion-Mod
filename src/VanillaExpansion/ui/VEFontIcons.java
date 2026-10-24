package VanillaExpansion.ui;

import arc.Core;
import arc.graphics.g2d.TextureAtlas;
import arc.graphics.g2d.TextureRegion;
import mindustry.Vars;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Unit;
import mindustry.type.Item;
import mindustry.type.UnitType;
import mindustry.world.Block;

import static mindustry.ui.Fonts.registerIcon;

public class VEFontIcons {
    public static void load(){

            ContentType[] types = {ContentType.liquid, ContentType.item, ContentType.block, ContentType.status, ContentType.unit};
            int startChar = 0xE000 + 1;

            for(var type : types){
                for(var cont : Vars.content.getBy(type)){
                    if(cont instanceof UnlockableContent u && u.name.startsWith("ve-")){
                        int id = startChar;
                        u.loadIcon();
                        TextureRegion icon = u.uiIcon != null ? u.uiIcon : u.fullIcon;
                        if(icon == null) continue;
                        registerIcon(u.name, u.name, id, icon);

                        startChar ++;
                    }
                }
            }

    }
}
