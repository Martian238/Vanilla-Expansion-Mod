package VanillaExpansion.expand.parts;

import arc.Core;
import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.Tmp;
import mindustry.entities.part.DrawPart;
import mindustry.entities.part.RegionPart;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;

public class AutoShadingPart extends DrawPart {
    public AutoShadingPart(){}


    public @Nullable String name;
    public String suffix2 = "-2";
    public String suffix3 = "-3";
    public String suffix4 = "-4";
    public boolean invisible = false;

    public Seq<DrawPart> children = new Seq<>();


    protected PartParams childParam = new PartParams();

    /** Appended to unit/weapon/block name and drawn. */
    public String suffix = "";
    /** Overrides suffix if set. */

    public TextureRegion heat, light;
    public TextureRegion[] regions = {};
    public TextureRegion[] outlines = {};

    public boolean shading = true;

    /** If true, parts are mirrored across the turret. Requires -1 and -2 regions. */
    public boolean mirror = false;
    /** If true, an outline is drawn under the part. */
    public boolean outline = false;
    /** If true, this part has an outline created 'in-place'. Currently vanilla only, do not use this! */
    public boolean replaceOutline = false;
    /** If true, the base + outline regions are drawn. Set to false for heat-only regions. */
    public boolean drawRegion = true;
    /** If true, the heat region produces light. */
    //public boolean heatLight = false;
    /** Whether to clamp progress to (0-1). If false, allows usage of interps that go past the range, but may have unwanted visual bugs depending on values. */
    public boolean clampProgress = true;
    /** Progress function for determining position/rotation. */
    public PartProgress progress = PartProgress.warmup;
    /** Progress function for scaling. */
    public PartProgress growProgress = PartProgress.warmup;
    /** Progress function for heat alpha. */
    public PartProgress heatProgress = PartProgress.heat;
    public Blending blending = Blending.normal;
    public float layer = -1, layerOffset = 0f, heatLayerOffset = 1f, turretHeatLayer = Layer.turretHeat;
    public float outlineLayerOffset = -0.001f;
    //note that origin DOES NOT AFFECT child parts
    public float x, y, xScl = 1f, yScl = 1f, rotation, originX, originY;
    public float moveX, moveY, growX, growY, moveRot;
    //public float heatLightOpacity = 0.3f;
    public @Nullable Color color, colorTo, mixColor, mixColorTo;
    //public Color heatColor = Pal.turretHeat.cpy();

    public Seq<PartMove> moves = new Seq<>();

    @Override
    public void draw(PartParams params){
        float z = Draw.z();
        if(layer > 0) Draw.z(layer);
        //TODO 'under' should not be special cased like this...
        if(under && turretShading) Draw.z(z - 0.0001f);
        Draw.z(Draw.z() + layerOffset);

        float prevZ = Draw.z();
        float prog = progress.getClamp(params, clampProgress), sclProg = growProgress.getClamp(params, clampProgress);
        float mx = moveX * prog, my = moveY * prog, mr = moveRot * prog + rotation,
                gx = growX * sclProg, gy = growY * sclProg;

        if(moves.size > 0){
            for(int i = 0; i < moves.size; i++){
                var move = moves.get(i);
                float p = move.progress.getClamp(params, clampProgress);
                mx += move.x * p;
                my += move.y * p;
                mr += move.rot * p;
                gx += move.gx * p;
                gy += move.gy * p;
            }
        }

        int len = mirror && params.sideOverride == -1 ? 2 : 1;
        float preXscl = Draw.xscl, preYscl = Draw.yscl;
        Draw.xscl *= xScl + gx;
        Draw.yscl *= yScl + gy;
        float prevMixCol = Draw.getMixColorPacked(), prevCol = Draw.getColorPacked();

        for(int s = 0; s < len; s++){
            //use specific side if necessary
            int i = params.sideOverride == -1 ? s : params.sideOverride;

            //can be null
            var region = drawRegion && regions.length > 0 ? regions[Math.min(i, regions.length - 1)] : null;

                var region2 = drawRegion && region2s.length > 0 ? region2s[Math.min(i, region2s.length - 1)] : null;
                var region3 = drawRegion && region3s.length > 0 ? region3s[Math.min(i, region3s.length - 1)] : null;
                var region4 = drawRegion && region4s.length > 0 ? region4s[Math.min(i, region4s.length - 1)] : null;
            float sign = (i == 0 ? 1 : -1) * params.sideMultiplier;
            Tmp.v1.set((x + mx) * sign, y + my).rotateRadExact((params.rotation - 90) * Mathf.degRad);

            Draw.xscl *= sign;

            if(originX != 0f || originY != 0f){
                //correct for offset caused by origin shift
                Tmp.v1.sub(Tmp.v2.set(-originX * Draw.xscl, -originY * Draw.yscl).rotate(params.rotation - 90f).add(originX * Draw.xscl, originY * Draw.yscl));
            }

            float
                    rx = params.x + Tmp.v1.x,
                    ry = params.y + Tmp.v1.y,
                    rot = mr * sign + params.rotation - 90;

            if(outline && drawRegion && !invisible){
                Draw.z(prevZ + outlineLayerOffset);
                rect(outlines[Math.min(i, regions.length - 1)], rx, ry, rot);
                Draw.z(prevZ);
            }

            if (region != null && drawRegion && region.found() && !invisible) {
                if (color != null && colorTo != null) {
                    Draw.color(color, colorTo, prog);
                } else if (color != null) {
                    Draw.color(color);
                }

                if (mixColor != null && mixColorTo != null) {
                    Draw.mixcol(mixColor, mixColorTo, prog);
                } else if (mixColor != null) {
                    Draw.mixcol(mixColor, mixColor.a);
                }

                Draw.blend(blending);
                if(shading) {
                    rect2(region, region2, region3, region4, rx, ry, rot);
                }else{
                    rect(region, rx, ry, rot);
                }
                Draw.blend();
                if (color != null) Draw.color();
            }

            /*
            if(heat.found()){
                float hprog = heatProgress.getClamp(params, clampProgress);
                heatColor.write(Tmp.c1).a(hprog * heatColor.a);
                Drawf.additive(heat, Tmp.c1, 1f, rx, ry, rot, turretShading ? turretHeatLayer : Draw.z() + heatLayerOffset, originX, originY);
                if(heatLight) Drawf.light(rx, ry, light.found() ? light : heat, rot, Tmp.c1, heatLightOpacity * hprog);
            }
            */

            Draw.xscl *= sign;
        }

        Draw.color(prevCol);
        Draw.mixcol(prevMixCol);

        Draw.z(z);

        //draw child, if applicable - only at the end
        //TODO lots of copy-paste here
        if(children.size > 0){
            for(int s = 0; s < len; s++){
                int i = (params.sideOverride == -1 ? s : params.sideOverride);
                float sign = (i == 1 ? -1 : 1) * params.sideMultiplier;
                Tmp.v1.set((x + mx) * sign, y + my).rotateRadExact((params.rotation - 90) * Mathf.degRad);

                childParam.set(params.warmup, params.reload, params.smoothReload, params.heat, params.recoil, params.charge, params.x + Tmp.v1.x, params.y + Tmp.v1.y, mr * sign + params.rotation);
                childParam.sideMultiplier = params.sideMultiplier;
                childParam.life = params.life;
                childParam.sideOverride = i;
                for(var child : children){
                    child.draw(childParam);
                }
            }
        }

        Draw.scl(preXscl, preYscl);
    }

    void rect(TextureRegion region, float x, float y, float rotation){
        float w = region.width * region.scl() * Draw.xscl, h = region.height * region.scl() * Draw.yscl;
        Draw.rect(region, x, y, w, h, w / 2f + originX * Draw.xscl, h / 2f + originY * Draw.yscl, rotation);
    }

    void rect2(TextureRegion region, TextureRegion region2, TextureRegion region3, TextureRegion region4, float x, float y, float rotation){
        float w = region.width * region.scl() * Draw.xscl, h = region.height * region.scl() * Draw.yscl;
        float angle = angleStandardize(rotation);
        Draw.alpha(Mathf.clamp((angle <= 90 || angle >= 270) ? 1 : 0));
        Draw.rect(region, x, y, w, h, w / 2f + originX * Draw.xscl, h / 2f + originY * Draw.yscl, rotation);
        Draw.alpha(Mathf.clamp(angle < 90 ? angle / 90 : (angle <= 180 ? 1 : 0)));
        Draw.rect(region2, x, y, w, h, w / 2f + originX * Draw.xscl, h / 2f + originY * Draw.yscl, rotation);
        Draw.alpha(Mathf.clamp((angle >= 90 && angle < 180) ? (angle - 90) / 90 : (angle >= 180 && angle <= 270 ? 1 : 0)));
        Draw.rect(region3, x, y, w, h, w / 2f + originX * Draw.xscl, h / 2f + originY * Draw.yscl, rotation);
        Draw.alpha(Mathf.clamp(angle < 0? (angle + 360 < 270? (angle + 180) / 90 : - angle / 90) : (angle < 270? (angle - 180) / 90 : (360 - angle) / 90)));
        Draw.rect(region4, x, y, w, h, w / 2f + originX * Draw.xscl, h / 2f + originY * Draw.yscl, rotation);
        Draw.alpha(1);
    }

    public float angleStandardize(Float a){
        if(a >= 0 && a < 360) return a;
        for(int i = 0; a >= 360; i++){
            a -= 360;
        }
        for(int i = 0; a < 0; i++){
            a += 360;
        }
        return a;
    }

    public TextureRegion[] region2s = {};
    public TextureRegion[] region3s = {};
    public TextureRegion[] region4s = {};

    @Override
    public void load(String name){
        String realName = this.name == null ? name + suffix : this.name;

        if(drawRegion){
            if(mirror && turretShading){
                regions = new TextureRegion[]{
                        Core.atlas.find(realName + "-r"),
                        Core.atlas.find(realName + "-l"),

                };

                outlines = new TextureRegion[]{
                        Core.atlas.find(realName + "-r-outline"),
                        Core.atlas.find(realName + "-l-outline")
                };
            }else{
                regions = new TextureRegion[]{Core.atlas.find(realName)};
                region2s = new TextureRegion[]{Core.atlas.find(realName + suffix2)};
                region3s = new TextureRegion[]{Core.atlas.find(realName + suffix3)};
                region4s = new TextureRegion[]{Core.atlas.find(realName + suffix4)};
                outlines = new TextureRegion[]{Core.atlas.find(realName + "-outline")};

            }
        }

        heat = Core.atlas.find(realName + "-heat");
        light = Core.atlas.find(realName + "-light");
        for(var child : children){
            child.turretShading = turretShading;
            child.load(name);
        }
    }

    @Override
    public void getOutlines(Seq<TextureRegion> out){
        if(outline && drawRegion){
            out.addAll(regions);
        }
        for(var child : children){
            child.getOutlines(out);
        }
    }
}
