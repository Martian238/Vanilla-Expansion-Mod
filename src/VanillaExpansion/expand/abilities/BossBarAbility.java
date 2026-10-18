package VanillaExpansion.expand.abilities;

import VanillaExpansion.content.VEJSStatuses;
import arc.Core;
import arc.graphics.Color;
import arc.graphics.Texture;
import arc.graphics.g2d.*;
import arc.math.Mathf;
import arc.scene.ui.layout.Scl;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Time;
import arc.util.pooling.Pools;
import mindustry.Vars;
import mindustry.content.StatusEffects;
import mindustry.content.UnitTypes;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.type.StatusEffect;
import mindustry.type.UnitType;
import mindustry.ui.Fonts;

import java.util.Objects;

import static mindustry.Vars.*;

public class BossBarAbility extends Ability {

    public String nameBundle = "";
    public boolean autoSetName = false;
    public boolean subBar = false;
    public float subFull = 100;
    public boolean subColorChange = false;
    public boolean drawStatuses = true;
    public boolean smoothAppear = true;
    public String iconSprite = "";
    public TextureRegion iconRegion = new TextureRegion();
    public boolean regionLoaded = false;

    public float currentFullness = 0;
    public float currentFullnessLast = 0;
    public float currentSubFullness = 0;
    public float currentSubFullnessLast = 0;

    public Color white = Color.white;
    public Color black = Color.black;
    public Color mainColor = Color.valueOf("f25555");
    public Color mainDecreaseColor = mainColor.cpy().mul(1.25f);
    public Color mainDecreaseColorTo = mainColor.cpy().a(0f);
    public Color subColor = mainColor.cpy().mul(1.25f);
    public Color subColorTo = Color.white;
    public Color subIncreaseColor = subColor.cpy().mul(1f);
    public Color subIncreaseColorTo = subColor.cpy().a(0f);
    public Color subChangeColor = Color.valueOf("ffd37f");

    public static class DecreaseRec {
        public float fullness;
        public float start;
        public int order;
        public float timer;

        public DecreaseRec(float fullness, int order, float start){
            this.fullness = fullness;
            this.order = order;
            this.timer = 0;
            this.start = start;
        }
    }
    public Seq<DecreaseRec> mainDecreases = new Seq<>();
    public int mainDecreaseOrder = 0;
    public Seq<DecreaseRec> subIncreases = new Seq<>();
    public int subIncreaseOrder = 0;

    public float shortenStartTime = 24f;
    public float shortenSpeed = 1f / 120f;

    public boolean subShine = false;
    public boolean subFullAutoShine = true;
    public float subShineTimer = 0f;
    public float lengthMultiplier = 0f;
    public float scale = 1778f / 3072f;
    public String text = "";
    public boolean shake = true;
    public float shakeTimer = 0;
    public float shakeMag = 0;

    /** Can be edited in settings */
    public static float barScale = 1;

    public BossBarAbility(){
        display = false;
    }

    @Override
    public void draw(Unit unit){
        if(!Vars.ui.hudfrag.shown) return;
        drawBar(unit);
    }

    @Override
    public void update(Unit unit){
        if(lengthMultiplier < 1 && Vars.ui.hudfrag.shown){
            lengthMultiplier = Mathf.lerpDelta(lengthMultiplier, 1f, 0.1f);
            if(lengthMultiplier >= 0.999f) lengthMultiplier = 1f;
        }
        float current = Mathf.clamp(unit.health / unit.maxHealth);
        currentFullness = current;
        barScale = Core.settings.getFloat("ve-endfield-health-bar-scale", 1);
        if(currentFullness < currentFullnessLast){
            mainDecreases.add(new DecreaseRec(currentFullnessLast - currentFullness,
                    mainDecreaseOrder, current));
            mainDecreaseOrder++;
            if(shake) {
                float totalDecrease = 0;
                for (DecreaseRec d : mainDecreases) {
                    totalDecrease += d.fullness;
                }
                if (totalDecrease > 0.05f) {
                    shakeTimer = 20f;
                    shakeMag = Mathf.clamp(totalDecrease - 0.05f);
                }
            }
        }
        if(shakeTimer > 0f){
            shakeTimer -= Time.delta * (1f + 1.5f * (shakeTimer / 20f));
        }
        currentFullnessLast = currentFullness;
        for(DecreaseRec d : mainDecreases){
            d.timer += Time.delta;
            if(d.timer >= shortenStartTime &&
                    !mainDecreases.contains(d2 -> d2.order < d.order && d2.fullness > 0)){
                d.fullness -= shortenSpeed * Time.delta;
            }
            if(d.fullness <= 0){
                mainDecreases.remove(d);
            }
        }
        if(!subBar) return;
        float currentSub = Mathf.clamp(unit.shield / subFull);
        currentSubFullness = currentSub;
        if(!subColorChange) {
            if (currentSubFullness > currentSubFullnessLast) {
                subIncreases.add(new DecreaseRec(currentSubFullness - currentSubFullnessLast,
                        subIncreaseOrder, currentSub));
                subIncreaseOrder++;
            }
            for (DecreaseRec d : subIncreases) {
                d.timer += Time.delta;
                if (d.timer >= shortenStartTime &&
                        !subIncreases.contains(d2 -> d2.order < d.order && d2.fullness > 0)) {
                    d.fullness -= shortenSpeed * Time.delta;
                }
                if (d.fullness <= 0) {
                    subIncreases.remove(d);
                }
            }
        }else{
            subIncreases.clear();
            subIncreaseOrder = 0;
        }
        currentSubFullnessLast = currentSubFullness;
        if(!subShine && subColorChange) {
            subShineTimer = 40f;
        }
        if(subShineTimer > 0){
            subShineTimer -= Time.delta;
        }
        subShine = subColorChange;
        if(subFullAutoShine){
            if(currentSubFullness >= 1){
                subColorChange = true;
            }
            if(subColorChange && currentSubFullness <= 0){
                subColorChange = false;
            }
        }
    }

    public void drawBar(Unit unit){
        float camW = Core.camera.width * barScale;
        float camH = Core.camera.height;
        float camX = Core.camera.position.x;
        float camY = Core.camera.position.y;
        float x = camX;
        float w = camW * 0.5f * (smoothAppear ? lengthMultiplier : 1f);
        float y = camY + camH * (0.425f - (Vars.state.isPaused() ? 0.02f : 0f) - Mathf.clamp(barScale - 1f) * 0.025f);
        float h = camW * scale * 0.0246f; //camH * 0.0246f;
        float thick = camW * 0.0025f;
        if(shake && !Vars.state.isPaused()){
            float shakeMagBase = h * 4f;
            if(shakeTimer > 0f){
                x += shakeMagBase * (shakeTimer / 20f) * shakeMag * Mathf.range(1f);
                y += shakeMagBase * (shakeTimer / 20f) * shakeMag * Mathf.range(1f);
            }
        }

        //shadow
        Draw.z(Layer.end - 10f);
        Draw.color(black);
        Draw.alpha(0.25f);
        Draw.rect(unit.type.softShadowRegion, x, y, w * 2.5f, h * 6f);

        //main
        Draw.color(black);
        Draw.alpha(0.33f);
        Fill.rect(x, y, w, h);
        Draw.alpha(1f);
        for(DecreaseRec d : mainDecreases){
            if(d.fullness <= 0) return;
            float dx = x + w * (d.start - 0.5f);
            Draw.color(white);
            Fill.rect(dx + 0.5f * w * d.fullness, y, w * d.fullness, h);
            Fill.quad(dx, y + 0.5f * h, mainDecreaseColor.toFloatBits(),
                    dx, y + 2.5f * h, mainDecreaseColorTo.toFloatBits(),
                    dx + w * d.fullness, y + 2.5f * h, mainDecreaseColorTo.toFloatBits(),
                    dx + w * d.fullness, y + 0.5f * h, mainDecreaseColor.toFloatBits());
        }
        Draw.color(mainColor);
        Fill.rect(x - 0.5f * w * (1 - currentFullness), y, w * currentFullness, h);

        //frame
        Draw.color(white);
        Draw.alpha(1f);
        Lines.stroke(thick * 0.5f);
        float x1 = x - w * 0.5f - thick * 2f;
        float x2 = x + w * 0.5f + thick * 2f;
        float y1 = y - h * 0.5f - thick;
        float y2 = y + h * 0.5f + thick;
        Lines.line(x1, y1, x1, y2);
        Lines.line(x1, y1, x1 + thick, y1);
        Lines.line(x1, y2, x1 + thick, y2);
        Lines.line(x2, y1, x2, y2);
        Lines.line(x2, y1, x2 - thick, y1);
        Lines.line(x2, y2, x2 - thick, y2);
        if(subBar){
            Lines.line(x1, y - h, x1 + thick, y - h);
            Lines.line(x2, y - h, x2 - thick, y - h);
        }

        //sub
        if(subBar){
            Draw.color(black);
            Draw.alpha(0.5f);
            Fill.rect(x, y - h, w, h * 0.35f);
            Draw.alpha(1f);
            if(subColorChange) {
                Draw.color(subChangeColor);
                Fill.rect(x - 0.5f * w * (1 - currentSubFullness), y - h, w * currentSubFullness, h * 0.25f);
            }else{
                Draw.color(white);
                float endColorBits = subColor.cpy().lerp(subColorTo, 1 - currentSubFullness).toFloatBits();
                Fill.quad(x - 0.5f * w, y - 1.175f * h, endColorBits,
                        x - 0.5f * w, y - 0.825f * h, endColorBits,
                        x + w * (currentSubFullness - 0.5f), y - 0.825f * h, subColorTo.toFloatBits(),
                        x + w * (currentSubFullness - 0.5f), y - 1.175f * h, subColorTo.toFloatBits());
            }
            if(!subColorChange){
                for(DecreaseRec d : subIncreases){
                    if(d.fullness <= 0) return;
                    float dx2 = x + w * (d.start - 0.5f);
                    Fill.quad(dx2, y - 2.175f * h, subIncreaseColorTo.toFloatBits(),
                            dx2, y - 1.175f * h, subIncreaseColor.toFloatBits(),
                            dx2 - w * d.fullness, y - 1.175f * h, subIncreaseColor.toFloatBits(),
                            dx2 - w * d.fullness, y - 2.175f * h, subIncreaseColorTo.toFloatBits());
                }
            }
            if(subShineTimer > 0){
                Draw.color(white);
                Draw.alpha(subShineTimer / 40f);
                Fill.rect(x, y - h, w, h * 0.35f);
                Draw.alpha(1f);
                float colorBits1 = white.cpy().a(subShineTimer / 40f).toFloatBits();
                float colorBits2 = white.cpy().a(0f).toFloatBits();
                Fill.quad(x - w * 0.7f, y - 1.175f * h, colorBits2,
                        x - w * 0.7f, y - 0.825f * h, colorBits2,
                        x - w * 0.5f, y - 0.825f * h, colorBits1,
                        x - w * 0.5f, y - 1.175f * h, colorBits1);
                Fill.quad(x + w * 0.5f, y - 1.175f * h, colorBits1,
                        x + w * 0.5f, y - 0.825f * h, colorBits1,
                        x + w * 0.7f, y - 0.825f * h, colorBits2,
                        x + w * 0.7f, y - 1.175f * h, colorBits2);
            }
        }

        //name
        float nameLen = 0;
        if((!nameBundle.isEmpty()) || autoSetName) {
            Font font = Fonts.def;
            GlyphLayout l = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
            boolean ints = font.usesIntegerPositions();
            font.getData().setScale(camW * 0.02f / font.getData().lineHeight);
            font.setUseIntegerPositions(false);

            if(Objects.equals(text, "") || text.isEmpty()) {
                text = Core.bundle.get(nameBundle);
                if (autoSetName) {
                    text = autoSetName();
                }
            }

            l.setText(font, text, Color.white, 1000000f, Align.left, false);
            float offset = h * 2f;
            Draw.color();
            Draw.alpha(1f);
            font.setColor(white);
            font.draw(text, x - l.width / 2f, y + offset, 1000000f, Align.left, false);
            font.setUseIntegerPositions(ints);
            font.getData().setScale(1f);
            nameLen = l.width;

            Pools.free(l);
            Draw.color();
            Draw.alpha(1f);
        }

        //icon
        if(!regionLoaded) loadRegion();
        if(iconRegion != null) {
            Draw.color(black);
            Draw.alpha(0.25f);
            float iconSize = h * 1.75f;
            float pix = iconSize / 64f;
            float iconX = x - (nameLen > 0 ? nameLen * 0.5f + 0.6f * iconSize : 0f);
            Draw.rect(iconRegion, iconX,
                    y + h * 2f - pix * 6f - iconSize * 0.3f, iconSize, iconSize);
            Draw.rect(iconRegion, iconX,
                    y + h * 2f - pix * 4f - iconSize * 0.3f, iconSize, iconSize);
            Draw.color();
            Draw.alpha(1f);
            Draw.rect(iconRegion, iconX,
                    y + h * 2f - iconSize * 0.3f, iconSize, iconSize);
        }

        //statuses
        if(drawStatuses){
            float size = h * 1.2f;
            float sx = x - w * 0.5f + h * 0.5f;
            float sy = y - h * (subBar ? 1.2f : 0.6f) - size * 0.5f;
            int num = 0;
            for(StatusEffect s : content.statusEffects()){
                if(s != StatusEffects.boss && s != VEJSStatuses.fakeBoss && s.show && !s.reactive && unit.hasEffect(s)){
                    Draw.rect(s.uiIcon, sx + num * size * 1.1f, sy, size, size);
                    num ++;
                }
            }
        }

        Draw.color();
        Draw.alpha(1f);
    }

    public String autoSetName(){
        if(Groups.unit.contains(u -> u.type.name.startsWith("ve-fungitron"))){
            return Core.bundle.get("unit.ve-fungitron-phase1.name");
        }
        if(Groups.unit.contains(u -> u.type.name.startsWith("ve-zentack"))){
            return Core.bundle.get("unit.ve-zentack-body.name");
        }
        if(Groups.unit.contains(u -> u.type.name.startsWith("ve-chiniun"))){
            return Core.bundle.get("unit.ve-chiniun.name");
        }
        return "";
    }

    public void loadRegion(){
        if(autoSetName){
            iconRegion = autoLoadRegion();
        }else if(!iconSprite.isEmpty()){
            iconRegion = Core.atlas.find(iconSprite);
        }
        regionLoaded = true;
    }
    public TextureRegion autoLoadRegion(){
        if(Groups.unit.contains(u -> u.type.name.startsWith("ve-fungitron"))){
            return Core.atlas.find("ve-bar-fungitron");
        }
        if(Groups.unit.contains(u -> u.type.name.startsWith("ve-zentack"))){
            return Core.atlas.find("ve-bar-zentack");
        }
        if(Groups.unit.contains(u -> u.type.name.startsWith("ve-chiniun"))){
            return Core.atlas.find("ve-bar-chiniun");
        }
        return new TextureRegion();
    }
}
