package VanillaExpansion.expand.world.block.sandbox;

import VanillaExpansion.VESettings;
import VanillaExpansion.content.VEJSStatuses;
import VanillaExpansion.expand.abilities.BossBarAbility;
import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Log;
import arc.util.Time;
import arc.util.pooling.Pools;
import mindustry.Vars;
import mindustry.content.StatusEffects;
import mindustry.content.UnitTypes;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.type.Category;
import mindustry.type.StatusEffect;
import mindustry.type.UnitType;
import mindustry.ui.Fonts;
import mindustry.world.Block;
import mindustry.world.meta.BuildVisibility;

import java.util.Objects;

import static mindustry.Vars.content;

public class AutoBossBarBlock extends Block {
    public AutoBossBarBlock(String name) {
        super(name);
        clipSize = 80000f;
        forceDark = true;
        placeableLiquid = true;
        buildVisibility = BuildVisibility.sandboxOnly;
        privileged = true;
        category = Category.logic;
        update = true;
        solid = true;
        targetable = false;
        canOverdrive = false;
        size = 1;
    }

    //Bar configs
    public boolean drawStatuses = true;
    public boolean smoothAppear = true;

    public Color white = Color.white;
    public Color black = Color.black;
    public Color mainColor = Color.valueOf("f25555");
    public Color mainDecreaseColor = mainColor.cpy().mul(1.25f);
    public Color mainDecreaseColorTo = mainColor.cpy().a(0f);

    public float shortenStartTime = 24f;
    public float shortenSpeed = 1f / 120f;

    public float scale = 1778f / 3072f;
    public boolean shake = true;



    /** Use the maximum maxHealth sum value as the constant bar max value during one multi-bosses battle */
    public boolean useMaxMax = true;

    public class AutoBossBarBuild extends Building {

        //Bar configs
        public float currentFullness = 0;
        public float currentFullnessLast = 0;
        public float maxLast = 0;
        public float maxMax = 0;
        public float currentMaxResult = 0;



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

        public float lengthMultiplier = 0f;
        public String text = "";
        public float shakeTimer = 0;
        public float shakeMag = 0;

        /** Can be edited in settings */
        public static float barScale = 1;

        //Build
        public static class BarUnit {
            public Unit unit = null;
            public boolean multi = false;
            public Seq<Unit> units = new Seq<>();
            public int order = 0;
            public BarUnit(Unit unit, int order) {
                this.unit = unit;
                this.order = order;
                multi = false;
            }
            public BarUnit(Seq<Unit> units, int order) {
                this.units = units;
                this.order = order;
                multi = true;
            }
        }
        public Seq<BarUnit> bars = new Seq<>();

        public boolean drawBar = false;
        public boolean hasBoss = false;

        public Seq<Unit> resultBossUnits = new Seq<>();

        public boolean isBoss(Unit u) {
            return u.type.isEnemy && (u.hasEffect(StatusEffects.boss) || u.hasEffect(VEJSStatuses.fakeBoss)) && u.team != Vars.player.team();
        }

        public void initBar(){
            lengthMultiplier = 0;
            maxMax = 0;
        }

        @Override
        public void update() {

            drawBar = false;
            if(!VESettings.getBool(VESettings.endfieldHealthBar, true) || Vars.state.enemies <= 0 || !enabled || Vars.state.isEditor()) {
                initBar();
                return;
            }
            hasBoss = Groups.unit.contains(
                    u -> isBoss(u));
            if(!hasBoss) {
                initBar();
                return;
            }
            Seq<Unit> bossUnits = new Seq<>();
            bossUnits.clear();
            for(Unit u : Groups.unit) {
                if(isBoss(u)) {
                    bossUnits.add(u);
                }
            }

            for(Unit u : bossUnits) {
                if(u.hasEffect(StatusEffects.boss)) {
                    u.unapply(StatusEffects.boss);
                    u.apply(VEJSStatuses.fakeBoss);
                }
            }

            bars.clear();
            int order = 0;
            Seq<Unit> scannedUnits = new Seq<>();
            scannedUnits.clear();
            for(UnitType t : Vars.content.units()){
                for(Unit u : bossUnits){
                    if(u.type == t && !scannedUnits.contains(u)) {
                        if (!bars.contains(b -> b.unit.type == t)) {
                            bars.add(new BarUnit(u, order));
                            order++;
                        } else {
                            BarUnit repeatBar = bars.find(b -> b.unit.type == t);
                            if(!repeatBar.multi) {
                                repeatBar.multi = true;
                                repeatBar.units.add(repeatBar.unit);
                            }
                            repeatBar.units.add(u);
                        }
                        scannedUnits.add(u);
                    }
                }
            }
            resultBossUnits = bossUnits;
            drawBar = true;
            barUpdate();

            if(Core.input.keyDown(KeyCode.z)){
                StringBuilder log = new StringBuilder();
                log.append("bossUnits: [");
                for(Unit u : bossUnits){
                    log.append(" ").append(u.type.name).append(" ");
                }
                log.append("], scannedUnits: ");
                for(Unit u : scannedUnits){
                    log.append(" ").append(u.type.name).append(" ");
                }
                log.append("], bars count: ").append(bars.count(b -> true));
                Log.info(log.toString());
            }
        }

        public void barUpdate(){
            if(lengthMultiplier < 1 && Vars.ui.hudfrag.shown){
                lengthMultiplier = Mathf.lerpDelta(lengthMultiplier, 1f, 0.1f);
                if(lengthMultiplier >= 0.999f) lengthMultiplier = 1f;
            }
            float current = 0, currentMax = 0;
            for(BarUnit b : bars){
                if(b.multi) {
                    for(Unit u : b.units) {
                        currentMax += u.maxHealth;
                        current += u.health;
                    }
                }else{
                    currentMax += b.unit.maxHealth;
                    current += b.unit.health;
                }
            }
            currentMaxResult = currentMax;
            if(useMaxMax && maxMax < currentMax){
                maxMax = currentMax;
            }
            if(useMaxMax && currentMax != maxMax){
                currentMax = maxMax;
            }
            currentFullness = Mathf.clamp(current / currentMax);
            if(currentFullness < currentFullnessLast){
                mainDecreases.add(new DecreaseRec(currentFullnessLast - current / maxLast,
                        mainDecreaseOrder, current / maxLast));
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
            maxLast = currentMax;
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
        }

        @Override
        public void draw(){
            Draw.color();
            Draw.z(Layer.block);
            Draw.rect(region, x, y);
            if(!drawBar || !Vars.ui.hudfrag.shown) return;

            drawBar();
        }

        public void drawBar(){
            float camW = Core.camera.width * barScale;
            float camH = Core.camera.height;
            float camX = Core.camera.position.x;
            float camY = Core.camera.position.y;
            float x = camX;
            float w = camW * 0.5f * (smoothAppear ? lengthMultiplier : 1f);
            float y = camY + camH * (0.425f - (Vars.state.isPaused() ? 0.02f : 0f));
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
            Draw.rect(UnitTypes.dagger.softShadowRegion, x, y, w * 2.5f, h * 6f);

            //main
            Draw.color(black);
            Draw.alpha(0.33f);
            if(useMaxMax){
                Fill.rect(x - 0.5f * w * (1 - currentMaxResult / maxMax), y, w * currentMaxResult / maxMax, h);
                Draw.alpha(0.1f);
                Fill.rect(x + 0.5f * w * (currentMaxResult / maxMax), y, w * (1 - currentMaxResult / maxMax), h);
                if(maxMax > currentMaxResult){
                    Draw.alpha(1f);
                    Draw.color(white);
                    Lines.stroke(thick * 0.5f);
                    Lines.line(x + w * (currentMaxResult / maxMax - 0.5f), y - h * 0.5f,
                            x + w * (currentMaxResult / maxMax - 0.5f), y + h * 0.5f);
                }
            }else {
                Fill.rect(x, y, w, h);
            }
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

            //name
                Font font = Fonts.def;
                GlyphLayout l = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
                boolean ints = font.usesIntegerPositions();
                font.getData().setScale(camW * 0.02f / font.getData().lineHeight);
                font.setUseIntegerPositions(false);

                text = getBarText();

                l.setText(font, text, Color.white, 1000000f, Align.left, false);
                float offset = h * 2f;
                Draw.color();
                Draw.alpha(1f);
                font.setColor(white);
                font.draw(text, x - l.width / 2f, y + offset, 1000000f, Align.left, false);
                font.setUseIntegerPositions(ints);
                font.getData().setScale(1f);

                Pools.free(l);
                Draw.color();
                Draw.alpha(1f);

            //statuses
            if(drawStatuses){
                float size = h * 1.2f;
                float sx = x - w * 0.5f + h * 0.5f;
                float sy = y - h * (0.6f) - size * 0.5f;
                int num = 0;
                for(StatusEffect s : content.statusEffects()){
                    if(s != StatusEffects.boss && s != VEJSStatuses.fakeBoss && s.show && !s.reactive){
                        int contain = resultBossUnits.count(u -> u.hasEffect(s));
                        if(contain > 0){
                            if(contain < resultBossUnits.count(u -> true)){
                                Draw.alpha(0.5f);
                            }else{
                                Draw.alpha(1f);
                            }
                            Draw.rect(s.uiIcon, sx + num * size * 1.1f, sy, size, size);
                            num ++;
                        }
                    }
                }
            }

            Draw.color();
            Draw.alpha(1f);
        }

        public String getBarText(){
            StringBuilder result = new StringBuilder();
            boolean more = false;
            for(int i = 0; i < bars.count(b -> true) ; i++){
                if(more){
                    result.append(" [gray]&[] ");
                }
                int num = i;
                BarUnit b = bars.find(bu -> bu.order == num);
                result.append(Core.bundle.get("unit." + b.unit.type.name + ".name"));
                if(b.multi){
                    result.append("×").append(b.units.count(u -> true));
                }
                more = true;
            }
            return result.toString();
        }
    }
}
