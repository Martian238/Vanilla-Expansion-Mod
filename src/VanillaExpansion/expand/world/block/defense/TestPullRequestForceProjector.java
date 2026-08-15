package VanillaExpansion.expand.world.block.defense;

import VanillaExpansion.content.CustomFx;
import mindustry.world.blocks.defense.ForceProjector;
import arc.*;
import arc.audio.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import VanillaExpansion.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class TestPullRequestForceProjector extends ForceProjector {

    public int sides = 6;
    public float shieldRotation = 0f;
    public float cooldownNormal = 1.75f;
    public float cooldownLiquid = 1.5f;
    public float cooldownBrokenBase = 1.0f;
    public Effect shieldBreakEffect = CustomFx.shieldBreakProjector;

    public TestPullRequestForceProjector(String name){
        super(name);
    }

    protected static TestPullRequestForceProjector paramBlock;
    protected static TestPullRequestForceBuild paramEntity;
    protected static final Cons<Bullet> shieldConsumer = bullet -> {
        if(bullet.team != paramEntity.team && bullet.type.absorbable && !bullet.absorbed &&
                Intersector.isInRegularPolygon(paramBlock.sides, paramEntity.x, paramEntity.y, paramEntity.realRadius(), paramBlock.shieldRotation, bullet.x, bullet.y)){

            bullet.absorb();
            paramBlock.hitSound.at(bullet.x, bullet.y, 1f + Mathf.range(0.1f), paramBlock.hitSoundVolume);
            paramBlock.absorbEffect.at(bullet);
            paramEntity.hit = 1f;
            paramEntity.buildup += bullet.type.shieldDamage(bullet);
        }
    };

    @Override
    public void setBars(){
        super.setBars();
        removeBar("shield");
        addBar("shield", (TestPullRequestForceBuild entity) -> new Bar("stat.shieldhealth", Pal.accent, () -> entity.broken ? 0f : 1f - entity.buildup / (shieldHealth + phaseShieldBoost * entity.phaseHeat)).blink(Color.white));
    }

    public class TestPullRequestForceBuild extends Building implements Ranged, ExplosionShield{
        public boolean broken = true;
        public float buildup, radscl, hit, warmup, phaseHeat;

        @Override
        public void setProp(LAccess prop, double value){
            if(prop == LAccess.shield){
                buildup = Math.max(shieldHealth + phaseShieldBoost * phaseHeat - (float)value, 0f);
            }else{
                super.setProp(prop, value);
            }
        }

        @Override
        public float range(){
            return realRadius();
        }

        @Override
        public boolean shouldAmbientSound(){
            return !broken && realRadius() > 1f;
        }

        @Override
        public void onRemoved(){
            float radius = realRadius();
            if(!broken && radius > 1f) Fx.forceShrink.at(x, y, radius, team.color);
            super.onRemoved();
        }

        @Override
        public void pickedUp(){
            super.pickedUp();
            radscl = warmup = 0f;
        }

        @Override
        public boolean inFogTo(Team viewer){
            return false;
        }

        @Override
        public void updateTile(){
            boolean phaseValid = itemConsumer != null && itemConsumer.efficiency(this) > 0;

            phaseHeat = Mathf.lerpDelta(phaseHeat, Mathf.num(phaseValid), 0.1f);

            if(phaseValid && !broken && timer(timerUse, phaseUseTime / timeScale) && efficiency > 0){
                consume();
            }

            radscl = Mathf.lerpDelta(radscl, broken ? 0f : warmup, 0.05f);

            if(Mathf.chanceDelta(buildup / shieldHealth * 0.1f)){
                Fx.reactorsmoke.at(x + Mathf.range(tilesize / 2f), y + Mathf.range(tilesize / 2f));
            }

            warmup = Mathf.lerpDelta(warmup, efficiency, 0.1f);

            if(buildup > 0){
                float scale = !broken ? cooldownNormal : cooldownBrokenBase;

                //TODO I hate this system
                if(coolantConsumer != null){
                    if(coolantConsumer.efficiency(this) > 0){
                        coolantConsumer.update(this);
                        scale *= (cooldownLiquid * (1f + (liquids.current().heatCapacity - 0.4f) * 0.9f));
                    }
                }

                buildup -= delta() * scale;
            }

            if(broken && buildup <= 0){
                broken = false;
            }

            if(buildup >= shieldHealth + phaseShieldBoost * phaseHeat && !broken){
                broken = true;
                buildup = shieldHealth;
                shieldBreakEffect.at(x, y, realRadius(), team.color, this.block);
                breakSound.at(x, y);
                if(team != state.rules.defaultTeam){
                    Events.fire(Trigger.forceProjectorBreak);
                }
            }

            if(hit > 0f){
                hit -= 1f / 5f * Time.delta;
            }

            deflectBullets();
        }

        public void deflectBullets(){
            float realRadius = realRadius();

            if(realRadius > 0 && !broken){
                paramBlock = TestPullRequestForceProjector.this;
                paramEntity = this;
                Groups.bullet.intersect(x - realRadius, y - realRadius, realRadius * 2f, realRadius * 2f, shieldConsumer);
            }
        }

        @Override
        public boolean absorbExplosion(float ex, float ey, float damage){
            boolean absorb = !broken && Intersector.isInRegularPolygon(sides, x, y, realRadius(), shieldRotation, ex, ey);
            if(absorb){
                absorbEffect.at(ex, ey);
                hit = 1f;
                buildup += damage * crashDamageMultiplier;
            }
            return absorb;
        }

        public float realRadius(){
            return (radius + phaseHeat * phaseRadiusBoost) * radscl;
        }

        @Override
        public double sense(LAccess sensor){
            if(sensor == LAccess.heat) return buildup;
            if(sensor == LAccess.shield) return broken ? 0f : Math.max(shieldHealth + phaseShieldBoost * phaseHeat - buildup, 0);
            return super.sense(sensor);
        }

        @Override
        public void draw(){
            super.draw();

            if(buildup > 0f){
                Draw.alpha(buildup / shieldHealth * 0.75f);
                Draw.z(Layer.blockAdditive);
                Draw.blend(Blending.additive);
                Draw.rect(topRegion, x, y);
                Draw.blend();
                Draw.z(Layer.block);
                Draw.reset();
            }

            drawShield();
        }

        public void drawShield(){
            if(!broken){
                float radius = realRadius();

                if(radius > 0.001f){
                    Draw.color(team.color, Color.white, Mathf.clamp(hit));

                    if(renderer.animateShields){
                        Draw.z(Layer.shields + 0.001f * hit);
                        Fill.poly(x, y, sides, radius, shieldRotation);
                    }else{
                        Draw.z(Layer.shields);
                        Lines.stroke(1.5f);
                        Draw.alpha(0.09f + Mathf.clamp(0.08f * hit));
                        Fill.poly(x, y, sides, radius, shieldRotation);
                        Draw.alpha(1f);
                        Lines.poly(x, y, sides, radius, shieldRotation);
                        Draw.reset();
                    }
                }
            }

            Draw.reset();
        }

        @Override
        public void overwrote(Seq<Building> previous){
            if(previous.size > 0 && previous.first().block == block && previous.first() instanceof TestPullRequestForceBuild b){
                broken = b.broken;
                buildup = b.buildup;
            }
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.bool(broken);
            write.f(buildup);
            write.f(radscl);
            write.f(warmup);
            write.f(phaseHeat);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            broken = read.bool();
            buildup = read.f();
            radscl = read.f();
            warmup = read.f();
            phaseHeat = read.f();
        }
    }
}
