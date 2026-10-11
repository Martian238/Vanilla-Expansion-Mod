package VanillaExpansion.ui.bossrush.card;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Color;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import arc.scene.event.Touchable;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.util.Align;
import arc.util.Log;
import arc.util.Scaling;
import mindustry.Vars;
import mindustry.gen.Tex;
import VanillaExpansion.VanillaExpansionMod;
import VanillaExpansion.ui.bossrush.BossRushNode;
import VanillaExpansion.ui.bossrush.BossRushView;

public abstract class BossRushCardWidget extends Table{
    protected static final float BASE_WIDTH = 160f;
    protected static final float BASE_HEIGHT = 220f;
    protected static final float SCALE = 0.6f;
    protected static final float WIDTH_SCALE = 1f;

    public static final float CARD_WIDTH = BASE_WIDTH * SCALE * WIDTH_SCALE;
    public static final float CARD_HEIGHT = BASE_HEIGHT * SCALE;

    protected static final float CONTENT_WIDTH = CARD_WIDTH - 12f;
    protected static final float FONT_SCALE = 0.5f;

    private static final ObjectMap<String, TextureRegion> textureCache = new ObjectMap<>();

    public BossRushNode node;
    protected BossRushView parentView;
    protected Image image;
    protected Label title;
    protected Table inner;

    public BossRushCardWidget(BossRushNode node, BossRushView parentView){
        this.node = node;
        this.parentView = parentView;

        background(Tex.pane);
        margin(2f);
        touchable = Touchable.enabled;
        setSize(CARD_WIDTH, CARD_HEIGHT);

        inner = new Table();
        inner.margin(4f);
        inner.background(statusBackground());
        add(inner).width(CARD_WIDTH - 4f).height(CARD_HEIGHT - 4f);

        image = new Image(new TextureRegionDrawable(findRegion(node.imagePath)));
        image.setScaling(Scaling.fit);
        inner.add(image).width(CONTENT_WIDTH).height(CARD_HEIGHT * 0.33f).pad(1f).row();

        title = new Label(node.title);
        title.setAlignment(Align.center);
        title.setFontScale(FONT_SCALE);
        title.setEllipsis(true);
        inner.add(title).width(CONTENT_WIDTH).height(CARD_HEIGHT * 0.15f).pad(1f).row();

        Table buttons = new Table();
        inner.add(buttons).width(CONTENT_WIDTH).height(CARD_HEIGHT * 0.20f).pad(1f).row();

        buildExtra(buttons);
    }

    protected void onButtonHover(String title, String desc){
        if(parentView != null){
            parentView.setHover(title, desc);
        }
    }

    protected void onButtonExit(){
        if(parentView != null){
            parentView.clearHover();
        }
    }

    protected Drawable statusBackground(){
        Color color;
        if(!node.unlocked){
            color = Color.valueOf("1a1a1a");
        }else if(node.completed){
            color = Color.valueOf("6b5a1e");
        }else{
            color = Color.valueOf("2a2f3a");
        }
        return new TextureRegionDrawable(Core.atlas.find("white")).tint(color);
    }
    protected static TextureRegion findRegion(String name){
        if(name == null || name.isEmpty()){
            //Log.warn("BossRushCardWidget: empty name, using error region");
            return Core.atlas.find("error");
        }

        if(textureCache.containsKey(name)){
            return textureCache.get(name);
        }

        TextureRegion region = Core.atlas.find(name);
        if(region != null && region.found()){
            textureCache.put(name, region);
            return region;
        }

        for(String prefix : new String[]{"sprites/", ""}){
            Fi file = Core.files.internal(prefix + name + ".png");
            //Log.info("BossRushCardWidget: internal @ exists=@", prefix + name + ".png", file.exists());
            if(file.exists()){
                try{
                    Texture tex = new Texture(file);
                    TextureRegion r = new TextureRegion(tex);
                    textureCache.put(name, r);
                    //Log.info("BossRushCardWidget: loaded from internal @", file.path());
                    return r;
                }catch(Exception e){
                    //Log.err("BossRushCardWidget: failed to load @", file.path(), e);
                }
            }
        }

        Fi modRoot = Vars.mods.getMod(VanillaExpansionMod.class).root;
        if(modRoot != null){
            //Log.info("BossRushCardWidget: modRoot = @", modRoot.absolutePath());
            for(String prefix : new String[]{"assets/sprites/", "assets/", "sprites/"}){
                Fi file = modRoot;
                for(String part : (prefix + name + ".png").split("/")){
                    if(part.isEmpty()) continue;
                    file = file.child(part);
                }
                //Log.info("BossRushCardWidget: modRoot check @ exists=@", file.absolutePath(), file.exists());
                if(file.exists()){
                    try{
                        Texture tex = new Texture(file);
                        TextureRegion r = new TextureRegion(tex);
                        textureCache.put(name, r);
                        //Log.info("BossRushCardWidget: loaded from modRoot @", file.absolutePath());
                        return r;
                    }catch(Exception e){
                        //Log.err("BossRushCardWidget: failed to load @", file.path(), e);
                    }
                }
            }
        }

        //Log.warn("BossRushCardWidget: texture not found: @", name);
        TextureRegion err = Core.atlas.find("error");
        textureCache.put(name, err);
        return err;
    }

    protected abstract void buildExtra(Table buttons);
}