package VanillaExpansion.expand.graphics;
import arc.graphics.*;
import arc.math.*;
import arc.util.*;
import mindustry.type.Planet;
public class NeutronJetParticleMesh extends ParticleMesh{
    public float length = 27f;
    public float jetRadius = 0.25f;
    public float speed = 54f;
    public float twistSpeed = 360f;
    public float rotateSpeed = 210f;
    public float particleSize = 0.5f;
    public Color color1 = Color.valueOf("ddeeff");
    public Color color2 = Color.valueOf("4488ff");
    public NeutronJetParticleMesh(Planet planet, int maxParticles){
        super(planet, maxParticles);
        tiltDeg = 15f;
        initParticles();
    }
    private void initParticles(){
        for(int i = 0; i < particleCount; i++){
            respawn(particles[i], i * 7);
        }
    }
    private void respawn(Particle p, int seed){
        boolean up = Mathf.randomSeed(seed + 1, 0f, 1f) > 0.5f;
        float sign = up ? 1f : -1f;
        p.x = 0;
        p.y = sign * planet.radius;
        p.z = 0;
        p.size = particleSize * (0.5f + Mathf.randomSeed(seed + 2, 0f, 0.5f));
        p.life = length / speed + Mathf.randomSeed(seed + 3, -0.3f, 0.3f);
        if(p.life < 0.1f) p.life = 0.1f;
        p.maxLife = p.life;
        p.color.set(color1).lerp(color2, Mathf.randomSeed(seed + 4, 0f, 0.6f));
        p.color.a = 0.9f;
    }
    @Override
    public void updateParticles(float delta){
        time += delta;
        for(int i = 0; i < particleCount; i++){
            Particle p = particles[i];
            p.life -= delta;
            if(p.life <= 0){
                respawn(p, i * 13 + 7);
                continue;
            }
            float t = 1f - p.life / p.maxLife;
            float sign = p.y >= 0 ? 1f : -1f;
            float angle = t * twistSpeed + time * rotateSpeed + i * 17.3f;
            float radiusFactor = t < 0.3f ? t / 0.3f : 1f - (t - 0.3f) / 0.7f * 0.5f;
            float r = jetRadius * radiusFactor;
            p.x = Angles.trnsx(angle, r);
            p.z = Angles.trnsy(angle, r);
            p.y = sign * (planet.radius + t * length);
            p.color.set(color1).lerp(color2, t * 0.7f);
            p.color.r *= 0.5f;
            p.color.g *= 0.5f;
            p.color.b *= 0.5f;
            p.color.a = (1f - t * t) * 0.9f;
        }
    }
}
