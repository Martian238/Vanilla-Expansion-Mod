package VanillaExpansion.expand.exp;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.graphics.*;
import VanillaExpansion.VEPal;

import static arc.Core.atlas;
import static mindustry.Vars.*;

/** Exp 墙：受击获得经验并升级。Ported from Project Unity's LevelLimitWall (simplified onto {@link ExpBase}). */
public class ExpWall extends ExpBase {
    public TextureRegion[] levelRegions; //level top regions
    public TextureRegion edgeRegion, edgeMaxRegion;
    public float damageExp = 1 / 20f;
    public float damageReduction = 0f;

    public Effect updateEffect = Fx.none;
    public float updateChance = 0.01f;

    public ExpWall(String name){
        super(name);
        maxLevel = 6;
        solid = destructible = true;
        update = true;
        upgradeEffect = Fx.none;
    }

    @Override
    public void load(){
        super.load();
        edgeRegion = atlas.find(name + "-under");
        edgeMaxRegion = atlas.find(name + "-under-max", name + "-under");
        int n = 1;
        while(n <= 100){ //worst-case scenario
            TextureRegion t = atlas.find(name + n);
            if(!t.found()) break;
            n++;
        }
        if(n > 1){
            //name+n-1 was the last sprite that was found
            levelRegions = new TextureRegion[n];
            levelRegions[0] = region;
            for(int i = 1; i < n; i++){
                levelRegions[i] = atlas.find(name + i);
            }
        }
    }

    public class ExpWallBuild extends ExpBaseBuild {
        public TextureRegion levelRegion(){
            if(levelRegions == null) return region;
            return levelRegions[Math.min((int)(levelf() * levelRegions.length), levelRegions.length - 1)];
        }

        @Override
        public void draw(){
            TextureRegion top = levelRegion();
            Draw.z(Layer.block);
            Draw.rect(top, x, y);
            if(top != region){
                Draw.z(Layer.blockUnder - 0.01f);
                if(edgeRegion.found()) Draw.rect(top == levelRegions[levelRegions.length - 1] ? edgeMaxRegion : edgeRegion, x, y);
                if(!state.isPaused() && updateEffect != Fx.none && top == levelRegions[levelRegions.length - 1] && Mathf.chanceDelta(updateChance)) updateEffect.at(x + Mathf.range(size * 4f), y + Mathf.range(size * 4f), VEPal.exp);
            }
        }

        @Override
        public float handleDamage(float amount){
            float a = amount * damageExp;

            if(a >= 1f) handleExp((int)a);
            else if(a > 0f && Mathf.chance(a)) handleExp(1);
            setEFields(level());
            return super.handleDamage(amount) * Mathf.clamp(1f - damageReduction);
        }
    }
}
