package VanillaExpansion.expand.graphics;
import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.gl.FrameBuffer;
import arc.graphics.gl.Shader;
import arc.math.Interp;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.struct.FloatSeq;
import arc.struct.Seq;
import arc.util.Time;
import arc.util.pooling.Pool.Poolable;
import VanillaExpansion.Utils.*;
import mindustry.game.EventType.Trigger;
import mindustry.graphics.Layer;

import static mindustry.Vars.headless;
public class LensShockwaveFX{
    public static LensShockwaveFX inst;
    public static final int maxShocks = 12;
    private final Seq<Shock> shocks = new Seq<>();
    private final BasicPool<Shock> pool = new BasicPool<>(Shock::new);
    private FrameBuffer buffer;
    private LensShockShader shader;
    private boolean loaded = false;
    protected LensShockwaveFX(){
        Events.run(Trigger.update, this::update);
        Events.run(Trigger.draw, this::draw);
        inst = this;
    }
    public static void init(){
        if(inst == null && !headless) inst = new LensShockwaveFX();
    }
    public void spawnShock(float x, float y, float maxRadius, float lifetime, float strength){
        Shock s = pool.obtain();
        s.x = x;
        s.y = y;
        s.maxRadius = Math.max(maxRadius, 4f);
        s.lifetime = Math.max(lifetime, 1f);
        s.strength = Math.max(strength, 0f);
        s.time = 0f;
        shocks.add(s);
    }
    void update(){
        if(shocks.isEmpty()) return;
        shocks.removeAll(s -> {
            s.time += Time.delta;
            if(s.time >= s.lifetime){
                pool.free(s);
                return true;
            }
            return false;
        });
    }
    void draw(){
        if(shocks.isEmpty()) return;
        ensure();
        buffer.resize(Core.graphics.getWidth(), Core.graphics.getHeight());
        Draw.draw(Layer.floor - 1f, () -> buffer.begin(Color.clear));
        Draw.draw(Layer.blockOver + 0.1f, () -> {
            buffer.end();
            shader.setShocks(shocks);
            buffer.blit(shader);
        });
    }
    private void ensure(){
        if(loaded) return;
        loaded = true;
        buffer = new FrameBuffer(2, 2);
        shader = new LensShockShader();
    }
    static class Shock implements Poolable{
        float x, y, maxRadius, lifetime, strength, time;

        @Override
        public void reset(){
            x = y = maxRadius = lifetime = strength = time = 0f;
        }
    }
    public static class LensShockShader extends Shader{
        private final FloatSeq uniforms = new FloatSeq(maxShocks * 8);
        private int count = 0;
        LensShockShader(){
            super(Core.files.internal("shaders/screenspace.vert").readString(), frag);
        }
        void setShocks(Seq<Shock> shocks){
            uniforms.clear();
            int n = Math.min(shocks.size, maxShocks);
            Vec2 center = Core.camera.project(shocks.get(0).x, shocks.get(0).y);
            Vec2 edge = Core.camera.project(shocks.get(0).x + 1f, shocks.get(0).y);
            float zoom = Math.max(edge.x - center.x, 0.0001f);
            for(int i = 0; i < n; i++){
                Shock s = shocks.get(i);
                float fin = Mathf.clamp(s.time / s.lifetime);
                float radius = Mathf.lerp(1f, s.maxRadius, Interp.pow2Out.apply(fin)) * zoom;
                float thickness = Math.max(6f, radius * 0.12f);
                float fade = Mathf.clamp(1f - fin * 1.1f);
                Vec2 v = Core.camera.project(s.x, s.y);
                uniforms.add(v.x, v.y, radius, thickness);
                uniforms.add(s.strength * fade, fade, 0f, 0f);
            }
            for(int i = n; i < maxShocks; i++){
                uniforms.add(0f, 0f, 0f, 0f);
                uniforms.add(0f, 0f, 0f, 0f);
            }
            count = n;
        }
        @Override
        public void apply(){
            setUniformf("u_texsize", inst.buffer.getWidth(), inst.buffer.getHeight());
            setUniformi("u_shockCount", count);
            setUniform4fv("u_shocks", uniforms.items, 0, maxShocks * 4);
            setUniform4fv("u_params", uniforms.items, maxShocks * 4, maxShocks * 4);
        }
        private static final String frag = """
            uniform sampler2D u_texture;
            uniform vec2 u_texsize;
            uniform int u_shockCount;
            uniform vec4 u_shocks[12];
            uniform vec4 u_params[12];

            void main(){
                vec2 uv = gl_FragCoord.xy / u_texsize;
                vec3 col = texture2D(u_texture, uv).rgb;
                vec2 total = vec2(0.0);
                float highlight = 0.0;

                for(int i = 0; i < 12; i++){
                    if(i >= u_shockCount) break;

                    vec2 center = u_shocks[i].xy;
                    float radius = u_shocks[i].z;
                    float thickness = max(u_shocks[i].w, 0.001);
                    float strength = u_params[i].x;
                    float fade = u_params[i].y;

                    vec2 delta = gl_FragCoord.xy - center;
                    float dist = length(delta);
                    float band = clamp(1.0 - abs(dist - radius) / thickness, 0.0, 1.0);
                    float wave = sin(band * 3.14159265);
                    vec2 dir = delta / max(dist, 0.001);

                    total += dir * wave * strength * fade;
                    highlight += band * band * fade;
                }

                vec2 suv = uv + total / u_texsize;
                col = texture2D(u_texture, clamp(suv, 0.0, 1.0)).rgb;
                col += vec3(0.55, 0.6, 0.7) * highlight * 0.4;

                gl_FragColor = vec4(col, 1.0);
            }
            """;
    }
}
