package VanillaExpansion.expand.world.block.crux;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.GlyphLayout;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.ui.layout.Scl;
import arc.util.Align;
import arc.util.Log;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import arc.util.pooling.Pools;
import mindustry.gen.Building;
import mindustry.gen.Bullet;
import mindustry.graphics.Layer;
import mindustry.type.Category;
import mindustry.ui.Fonts;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.logic.MemoryBlock;
import mindustry.world.blocks.logic.SwitchBlock;
import mindustry.world.meta.BuildVisibility;
import mindustry.world.meta.Env;

import java.text.DecimalFormat;

import static mindustry.Vars.*;
import static mindustry.Vars.tilesize;

public class SentryAlertStorer extends SwitchBlock {
    public SentryAlertStorer(String name) {
        super(name);
        category = Category.logic;
        buildVisibility = BuildVisibility.worldProcessorOnly;
        envEnabled = Env.any;
        targetable = false;
        health = 99999;
        clipSize = 80000f;
        update = true;
    }

    @Override
    public boolean checkForceDark(Tile tile){
        return true;
    }


    public float maxTime = 99.99f * 60f; //Max time of alert and caution phase
    public float suspectTime = 5.00f * 60f; //Time of suspect phase
    public float cooldownTime = 120f;
    public float speed = 1f;

    public class SentryAlertStorerBuild extends SwitchBuild {

        @Override
        public boolean collision(Bullet b){
            return false;
        }

        public int status = 0; //Global status for all sentries. 0:safe, 1:suspect, 2:caution, 3:alert

        private float alertTimer = 0;
        private double cautionTimer = 0;
        private float suspectTimer = 0;
        private DecimalFormat alertTimerDf = new DecimalFormat("00.00");
        private DecimalFormat suspectTimerDf = new DecimalFormat("00.00");
        private DecimalFormat cautionTimerDf = new DecimalFormat("00.00");
        private String alertTimerDisplayed;
        private String suspectTimerDisplayed;
        private String cautionTimerDisplayed;
        private boolean displaying;
        private String text;
        public Vec2 lastPos = new Vec2(0, 0);

        private float cooldownTimer = 0; //Timer before a timer starts to decrease

        public void alert(){
            alertTimer = maxTime;
            cautionTimer = maxTime;
            cooldownTimer = cooldownTime;
            status = 3;
        }
        public void caution(){
            if(status == 3){
                alertTimer = maxTime;
                cautionTimer = maxTime;
            }else if(status == 2){
                cautionTimer = maxTime;
            }else{
                suspectTimer = suspectTime;
                status = 1;
            }
            cooldownTimer = cooldownTime;
        }

        @Override
        public void update(){
            if(cooldownTimer > 0) cooldownTimer--; //Log.info(cooldownTimer);
            if(cooldownTimer < 0) cooldownTimer = 0;
            if(cooldownTimer <= 0){
                if(suspectTimer > 0) suspectTimer -= speed;
                if(cautionTimer > 0 && alertTimer <= 0) cautionTimer -= speed;
                if(alertTimer > 0) alertTimer -= speed;
            }
            displaying = false;
            if(suspectTimer > 0 || cautionTimer > 0 || alertTimer > 0){
                displaying = true;
                alertTimerDisplayed = alertTimerDf.format(alertTimer / 60);
                suspectTimerDisplayed = suspectTimerDf.format(suspectTimer / 60);
                cautionTimerDisplayed = cautionTimerDf.format(cautionTimer / 60);
                if(alertTimer > 0){
                    text = Core.bundle.format("stealth.alert") + "\n" + alertTimerDisplayed;
                    status = 3;
                }else if(cautionTimer > 0){
                    text = Core.bundle.format("stealth.caution") + "\n" + cautionTimerDisplayed;
                    status = 2;
                }else if(suspectTimer > 0){
                    text = Core.bundle.format("stealth.suspect") + "\n" + suspectTimerDisplayed;
                    status = 1;
                }
            }else{
                status = 0;
            }
        }

        private Color c1 = Color.valueOf("ffd37f");
        private Color c2 = Color.valueOf("ffa665");
        private Color c3 = Color.valueOf("f25555");
        @Override
        public void draw(){
            super.draw();
            if(renderer.pixelate || !displaying) return;
            float drawX = Core.camera.position.x;
            float drawY = Core.camera.position.y + 0.35f * Core.camera.height;
            float drawSize = 300f / Core.camera.width;
            float drawScale = 300f / Core.camera.width;
            Font font = Fonts.outline;
            GlyphLayout l = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
            boolean ints = font.usesIntegerPositions();
            font.getData().setScale(1 / 4f / Scl.scl(drawScale));
            font.setUseIntegerPositions(false);



            Draw.z(Layer.max - 0.1f);
            l.setText(font, text, Color.white, drawSize, Align.center, true);

            Draw.color(0f, 0f, 0f, 0.2f);
            Fill.rect(drawX, drawY - l.height * 0.5f, l.width, l.height);
            Draw.color();
            if(alertTimer > 0) {
                Draw.color(c1, c2, c3,
                        0.75f + 0.25f * Mathf.sin(Time.globalTime / 5f));
                font.setColor(Draw.getColor());
                Draw.color();
            }else if(cautionTimer > 0) {
                Draw.color(c1, c1, c2,
                        0.75f + 0.25f * Mathf.sin(Time.globalTime / 15f));
                font.setColor(Draw.getColor());
                Draw.color();
            }else{
                font.setColor(c1);
            }
            font.draw(text, drawX, drawY, drawSize, Align.center, true);
            font.setUseIntegerPositions(ints);

            font.getData().setScale(drawScale);

            Pools.free(l);
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.i(status);
            write.f(alertTimer);
            write.d(cautionTimer);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 1){
                status = read.i();
                alertTimer = read.f();
                cautionTimer = read.d();
            }else{
                status = 0;
            }
        }
    }
}
