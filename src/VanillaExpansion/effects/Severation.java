package VanillaExpansion.effects;
import arc.*;
import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.math.geom.QuadTree.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.Pool.*;
import VanillaExpansion.entities.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;

import static arc.math.geom.Geometry.*;
import static mindustry.Vars.*;
public class Severation extends DrawEntity implements QuadTreeObject{
    static float[] tmpVerts = new float[24];
    static float minArea = 4f * 4f;
    static int slashIDs = 0;
    static QuadTree<Severation> cutTree;
    static Seq<Severation> cutsSeq = new Seq<>();
    static Seq<Slash> slashes = new Seq<>();
    Seq<CutTri> tris = new Seq<>();
    float bounds = 0f, area = 0f;
    float centerX, centerY;
    float rotation;
    float width, height;
    TextureRegion region = new TextureRegion();
    public float color = Color.whiteFloatBits;
    public float z = Layer.flyingUnit, shadowZ, zTime;
    public Effect explosionEffect = VEFX.fragmentExplosion;
    public Sound explosionSound = Sounds.none;
    float time = 0f, lifetime = 3f * 60f;
    public float vx, vy, vr;
    public float drag = 0.05f;
    public static void init(){
        Events.on(ResetEvent.class, e -> {
            slashes.clear();
            cutsSeq.clear();
        });
        Events.on(EventType.WorldLoadEvent.class, e -> cutTree = new QuadTree<>(new Rect(-finalWorldBounds, -finalWorldBounds, world.width() * tilesize + finalWorldBounds * 2, world.height() * tilesize + finalWorldBounds * 2)));
    }
    public static Severation generate(TextureRegion region, float x, float y, float width, float height, float rotation){
        Severation c = new Severation();
        c.region.set(region);
        c.width = width;
        c.height = height;
        c.rotation = rotation;
        c.x = x;
        c.y = y;
        for(int i = 0; i < 2; i++){
            CutTri tr = new CutTri();
            float[] p = tr.pos;
            p[0] = i == 0 ? 0f : 1f;
            p[1] = i == 0 ? 0f : 1f;
            p[2] = 1f;
            p[3] = 0f;
            p[4] = 0f;
            p[5] = 1f;
            c.tris.add(tr);
        }
        c.updateBounds();
        c.add();
        return c;
    }
    @Override
    public void update(){
        x += vx * Time.delta;
        y += vy * Time.delta;
        rotation += vr * Time.delta;
        zTime = Mathf.clamp(zTime + Time.delta / 40f);
        float drg = zTime < 1 ? drag : 0.2f;
        vx *= 1f - drg * Time.delta;
        vy *= 1f - drg * Time.delta;
        vr *= 1f - drg * Time.delta;
        if(time >= lifetime){
            float b = Mathf.sqrt(area / 4f);
            explosionEffect.at(x, y, b);
            float shake = b / 3f;
            Effect.shake(shake, shake, x, y);
            //if(explosionSound != Sounds.none) explosionSound.at(x, y, 1f, Mathf.clamp(b / 1.1f));
            if(explosionSound != Sounds.none) explosionSound.at(x, y, Mathf.random(0.9f, 1.1f) * Math.max(1f / (1f + (b - 8f) / 70f), 0.5f), Mathf.clamp(b / 1.1f));
            remove();
        }
        float speed = area < minArea ? 2f : 1f;
        time += Time.delta * speed;
    }
    @Override
    public void hitbox(Rect out){
        out.setCentered(x, y, bounds);
    }
    @Override
    public void draw(){
        float sin = Mathf.sinDeg(rotation);
        float cos = Mathf.cosDeg(rotation);
        float col = color, mcol = Color.clearFloatBits;
        float oz = Draw.z();
        float sdz = shadowZ * (1 - zTime);
        TextureRegion r = region;
        Tmp.c1.set(Pal.shadow).a(Pal.shadow.a * Mathf.curve(zTime, 0f, 0.3f));
        float scol = Tmp.c1.toFloatBits();
        float trueZ = zTime < 1f ? z : (Layer.debris + 0.1f);
        for(CutTri t : tris){
            float[] pos = t.pos, verts = tmpVerts;
            int vertI = 0;
            for(int i = 0; i < 8; i += 2){
                int mi = Math.min(i, 4);
                float vx = (pos[mi] - centerX) * width;
                float vy = (pos[mi + 1] - centerY) * height;
                float tx = (vx * cos - vy * sin) + x;
                float ty = (vx * sin + vy * cos) + y;
                verts[vertI] = tx;
                verts[vertI + 1] = ty;
                verts[vertI + 2] = col;
                verts[vertI + 3] = Mathf.lerp(r.u, r.u2, pos[mi]);
                verts[vertI + 4] = Mathf.lerp(r.v2, r.v, pos[mi + 1]);
                verts[vertI + 5] = mcol;
                vertI += 6;
            }
            Draw.z(trueZ);
            Draw.vert(region.texture, verts, 0, 24);
            if(sdz > 0.001f){
                Draw.z(Math.min(trueZ - 1f, Layer.darkness));
                //float scol = Pal.shadow.toFloatBits();
                //Tmp.c1
                for(int i = 0; i < 4; i++){
                    int j = i * 6;
                    verts[j] += UnitType.shadowTX * sdz;
                    verts[j + 1] += UnitType.shadowTY * sdz;
                    verts[j + 2] = scol;
                }
                Draw.vert(region.texture, verts, 0, 24);
            }
        }
        /*
        Draw.color(Color.green);
        for(CutTri t : tris){
            float[] pos = t.pos;
            Lines.beginLine();
            for(int i = 0; i < 6; i += 2){
                //float x = pos[i], y = pos[i + 1];
                
                float vx = (pos[i] - centerX) * width;
                float vy = (pos[i + 1] - centerY) * height;
                float tx = (vx * cos - vy * sin) + x;
                float ty = (vx * sin + vy * cos) + y;
                
                Lines.linePoint(tx, ty);
            }
            Lines.endLine(true);
        }
        */
        Draw.color();

        Draw.z(oz);
    }
    @Override
    public float clipSize(){
        return bounds * 2f;
    }
    void updateBounds(){
        float maxW = 0, minW = 1;
        float maxH = 0, minH = 1;
        float cx = 0, cy = 0;
        float tw = width, th = height;
        int cc = 0;
        area = 0f;
        for(CutTri t : tris){
            for(int i = 0; i < t.pos.length; i += 2){
                float x = t.pos[i], y = t.pos[i + 1];
                maxW = Math.max(x, maxW);
                minW = Math.min(x, minW);
                maxH = Math.max(y, maxH);
                minH = Math.min(y, minH);

                cx += x;
                cy += y;
                cc++;
            }
            float[] p = t.pos;
            area += triangleArea(p[0] * tw, p[1] * th, p[2] * tw, p[3] * th, p[4] * tw, p[5] * th);
        }
        float w = maxW - minW, h = maxH - minH;

        centerX = cx / cc;
        centerY = cy / cc;
        //minBounds = Math.min(w * width, h * height);
        bounds = Math.max(w * Math.abs(width), h * Math.abs(height));
    }
    @Override
    protected void addGroup(){
        super.addGroup();
        cutsSeq.add(this);
    }
    @Override
    protected void removeGroup(){
        super.removeGroup();
        cutsSeq.remove(this);
    }
    static class CutTri{
        //0-1
        float[] pos = new float[3 * 2];
    }
    static class Slash implements Poolable{
        float x1, y1, x2, y2;
        float time;
        int id = slashIDs++;
        @Override
        public void reset(){
            x1 = y1 = x2 = y2 = 0f;
            time = 0f;
            id = slashIDs++;
        }
    }
}
