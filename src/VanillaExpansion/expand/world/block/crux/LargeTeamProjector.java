package VanillaExpansion.expand.world.block.crux;

import arc.Core;
import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.scene.ui.Slider;
import arc.scene.ui.layout.Table;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Sounds;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.world.blocks.production.GenericCrafter;

public class LargeTeamProjector extends GenericCrafter {
    public LargeTeamProjector(String name){
        super(name);
        clipSize = 8000f;
        ambientSound = Sounds.none;

        configurable = true;
        consumesTap = true;
        clearOnDoubleTap = true;

        config(Object.class, (LargeTeamProjectorBuild tile, Object f) -> {
            if(tile.configuring == 1) {
                tile.currentHeightFraction = (float) f;
            }else{
                tile.currentRadiusConfig = (int) f;
            }
        });
        configClear((LargeTeamProjectorBuild tile) -> {
            tile.currentHeightFraction = defaultHeightFraction;
            tile.currentRadiusConfig = (int) defaultRadiusFraction * 10;
        });
    }

    public float cameraScale = 1f;
    public float projectionHeight = 200f;
    public float minDisplayHeight = 200f;
    public float fullDisplayHeight = 225f;

    public TextureRegion projectionRegion = new TextureRegion();
    public TextureRegion shadowRegion = new TextureRegion();
    public String projectionSuffix = "-projection";
    public String shadowSprite = "ve-circle-soft";
    public Color projectionColor = Color.valueOf("f25555");
    public float projectionSize = 128f;
    public float projectionRotation = 0f;

    public float ringRadius = 80f;
    public float ringStroke = 16f;
    public float ringAlpha = 0.6f;
    public float ringAlphaMag = 0.1f;
    public float ringAlphaScl = 10f;
    public float ringOuterRadius = 96f;
    public float ringOuterStroke = 6f;
    public float ringOuterRotateSpeed = 1f;
    public float ringOuterFraction = 0.25f;

    public float ringEdgeRadius = 107f;
    public float ringEdgeStroke = 6f;
    public float ringEdgeMag = 0.5f;
    public float ringEdgeScl = 30f;
    public float ringEdgeRange = 800f;

    public float waveTime = 60f;
    public int waveGroups = 3;

    public float defaultHeightFraction = 1f;
    public float defaultRadiusFraction = 1f;

    @Override
    public void load(){
        super.load();
        projectionRegion = Core.atlas.find(name + projectionSuffix);
        shadowRegion = Core.atlas.find(shadowSprite);
    }

    public class LargeTeamProjectorBuild extends GenericCrafterBuild{
        private float rot;
        private float timer;
        private float currentHeightFraction = defaultHeightFraction;
        private int currentRadiusConfig = (int) (10 * defaultRadiusFraction);
        private Slider slider = new Slider(0, 2, 0.1f, false);
        private Slider slider2 = new Slider(1, 20, 1, false);
        private int configuring = 1;

        @Override
        public void update(){
            super.update();
            if(warmup() > 0) rot += ringOuterRotateSpeed;
            if(rot >= 360f) rot -= 360f;
            if(rot < 0f) rot += 360f;
            timer++;
            if(timer >= waveTime) timer = 0;
        }

        @Override
        public void draw(){
            super.draw();

            float f = currentHeightFraction;
            float rf = currentRadiusConfig * 0.1f;
            float cs = Core.camera.height;
            float cx = Core.camera.position.x;
            float cy = Core.camera.position.y;
            float h = Math.max(projectionHeight * f, 0.01f);
            float r = ringRadius * rf;
            float cz = cs * 0.5f * cameraScale;
            if(cz > minDisplayHeight * f) {
                float alpha = warmup() * Mathf.clamp((cz - minDisplayHeight * f) / (Math.max(fullDisplayHeight * f - ((fullDisplayHeight - minDisplayHeight) * (1 - f)), minDisplayHeight + 0.01f) - minDisplayHeight * f), 0, 1);
                float ar = cz * (r / (cz - h));
                float scale = ar / r;
                float px = (cx / cz - x / h) / (1 / cz - 1 / h);
                float py = (cy / cz - y / h) / (1 / cz - 1 / h);
                Draw.z(Layer.weather - 1f);
                Draw.color(Color.black);
                Draw.alpha(alpha * 0.4f);
                Draw.rect(shadowRegion, px, py, 3f * ar, 3f * ar);
                Draw.color(projectionColor);
                Draw.alpha(alpha);
                Draw.blend(Blending.additive);
                Draw.rect(projectionRegion, px, py, projectionSize * scale * rf, projectionSize * scale * rf, projectionRotation);
                    Draw.alpha((float) (alpha * (ringAlpha + ringAlphaMag * Mathf.sin(Time.time / ringAlphaScl))));
                    Lines.stroke(ringStroke * scale * rf);
                    Lines.circle(px, py, ar);
                    Lines.stroke(ringOuterStroke * scale * rf);
                    Lines.arc(px, py, ringOuterRadius * scale * rf, ringOuterFraction, rot);
                    Lines.arc(px, py, ringOuterRadius * scale * rf, ringOuterFraction, rot - 180f);
                    float edgeSin = Mathf.sin(Time.time / ringEdgeScl);
                    edgeSin = Mathf.sinDeg(90f * edgeSin);
                    edgeSin = Mathf.sinDeg(90f * edgeSin);
                    float edgeSin2 = 0.5f * (Mathf.sinDeg(90f * edgeSin) + 1f);
                    Draw.alpha(alpha * ringEdgeMag * edgeSin2);
                    Lines.stroke(ringEdgeStroke * scale * rf);
                    Lines.circle(px, py, ringEdgeRadius * scale * rf);
                    float wavep = Mathf.clamp((float) ((Time.time / ringEdgeScl) % (Math.PI * 2f) / Math.PI));
                    wavep = (float) Math.sqrt(wavep) * 0.5f + wavep * 0.5f;
                    Draw.alpha(alpha * ringEdgeMag * Mathf.clamp(edgeSin - wavep * 0.5f));
                    Lines.stroke(ringEdgeStroke * scale * rf * (1 + wavep * 4f));
                    Lines.circle(px, py, Mathf.lerp(ringEdgeRadius * scale * rf, ringEdgeRange * scale * rf, wavep));
                for(int i = 0; i < waveGroups; i++){
                    float t = timer + ((float) i / waveGroups) * waveTime;
                    if(t >= waveTime) t -= waveTime;
                    float wh1 = Mathf.lerp(0, h * 0.5f, t / waveTime);
                    float wh2 = Mathf.lerp(h * 0.5f, h, t / waveTime);
                    float wx1 = (cx / cz - x / wh1) / (1 / cz - 1 / wh1);
                    float wy1 = (cy / cz - y / wh1) / (1 / cz - 1 / wh1);
                    float wx2 = (cx / cz - x / wh2) / (1 / cz - 1 / wh2);
                    float wy2 = (cy / cz - y / wh2) / (1 / cz - 1 / wh2);
                    float wr1 = Mathf.lerp(0, r * 0.5f, t / waveTime) * (cz / (cz - wh1));
                    float wr2 = Mathf.lerp(r * 0.5f, r, t / waveTime) * (cz / (cz - wh2));
                    float ws1 = Mathf.lerp(0, ringStroke * (cz / (cz - wh1)) * 0.375f, t / waveTime);
                    float ws2 = Mathf.lerp(ringStroke * (cz / (cz - wh2)) * 0.375f, ringStroke * (cz / (cz - wh2)) * 0.75f, t / waveTime);
                    Draw.alpha(Mathf.lerp(0, alpha * ringAlpha * 0.25f, t / waveTime));
                    Lines.stroke(ws1 * rf);
                    Lines.circle(wx1, wy1, wr1);
                    Draw.alpha(Mathf.lerp(0, alpha * ringAlpha * 0.25f, 1f - t / waveTime));
                    Lines.stroke(ws2 * rf);
                    Lines.circle(wx2, wy2, wr2);
                }
                Draw.blend();
                Draw.color();
            }
        }

        @Override
        public void buildConfiguration(Table table){
            slider.changed(() -> {
                currentHeightFraction = slider.getValue();
                configuring = 1;
                configure(currentHeightFraction);
            });
            slider2.changed(() -> {
                currentRadiusConfig = (int) slider2.getValue();
                configuring = 2;
                configure(currentRadiusConfig);
            });
            table.image().color(projectionColor).height(40f).width(8f);
            table.add(slider).height(40f).width(160f);
            table.image().color(projectionColor).height(40f).width(8f);
            table.row();
            table.image().color(projectionColor).height(40f).width(8f);
            table.add(slider2).height(40f).width(160f);
            table.image().color(projectionColor).height(40f).width(8f);
        }

        @Override
        public Object config(){
            return currentHeightFraction;
        }

        @Override
        public byte version(){
            return 3;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(currentHeightFraction);
            write.i(currentRadiusConfig);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 3) {
                currentHeightFraction = read.f();
                currentRadiusConfig = read.i();
            }else if(revision >= 2) {
                currentHeightFraction = read.f();
                currentRadiusConfig = (int) defaultRadiusFraction * 10;
            }else{
                currentHeightFraction = defaultHeightFraction;
                currentRadiusConfig = (int) defaultRadiusFraction * 10;
            }
        }
    }
}
