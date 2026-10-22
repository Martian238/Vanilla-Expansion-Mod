package VanillaExpansion.expand.units;

import arc.Core;
import arc.audio.Sound;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.entities.units.WeaponMount;
import mindustry.gen.Groups;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.type.UnitType;
import mindustry.type.Weapon;

import static mindustry.Vars.tilesize;

public class ChainWeapon extends Weapon {
    public ChainWeapon(){
        useAttackRange = false;
        mirror = false;
        mountType = ChainWeaponMount::new;
    }

    /** 发射时的锁链最大长度 */
    public float maxLength = 52f * tilesize;
    /** 抓取后的锁链最大长度 */
    public float catchLength = 28f * tilesize;
    /** 存在时间，每条锁链独立计算，为负时不消失 */
    public float lifetime = -1f;
    /** 发射速度 */
    public float speed = 8f;
    /** 牵引力度 */
    public float force = 0.5f;
    /** 额外抓取半径 */
    public float baseCatchRadius = 8f;
    /** 抓取单位后锁链改变长度时的速度倍率 */
    public float backSpeedMultiplier = 0.2f;

    /** 启用后，电脑端按右键可切断所有锁链 */
    public boolean rightClickCancel = true;

    /** 锁链发射点(Json使用) */
    public static class ChainPoint{
        public float x = 0, y = 0;
        public float rot = 0;
        public int order = 0;
        protected float realX, realY;
        public ChainPoint(){}
        public ChainPoint(float x, float y, float rot, int order) {
            this.x = x;
            this.y = y;
            this.rot = rot;
            this.order = order;
        }
    }
    /** 一个能力项的所有锁链发射点(Json使用) */
    public Seq<ChainPoint> chainPointsAdd = new Seq<>();

    /** 是否能抓取己方单位 */
    public boolean catchSameTeam = true;
    /** 启用后只在玩家操控时可以抓取己方单位 */
    public boolean catchSameTeamPlayerOnly = true;

    /** 特效颜色，为空时自动使用队伍颜色 */
    public @Nullable Color effectColor;
    /** 锁链释放时的特效，生成于头部 */
    public Effect releaseEffect = Fx.shockwave;
    public Sound releaseSound = Sounds.shootSalvo;
    public float releaseVolume = 1f;
    public float releaseShake = 2f;
    /** 锁链延长时的特效，生成于头部 */
    public Effect extendEffect = Fx.none;
    public float extendEffectInterval = 5f;
    public Sound extendSound = Sounds.loopSpray;
    public float extendVolume = 0.5f;
    /** 启用后，根部也会播放延长音效 */
    public boolean extendSoundRoot = true;
    /** 锁链抓取到一个单位时的特效，生成于头部 */
    public Effect catchEffect = Fx.hitBulletBig;
    public Sound catchSound = Sounds.payloadPickup;
    public float catchVolume = 1f;
    public float catchShake = 2f;
    /** 锁链消失时的特效，生成于头部 */
    public Effect breakEffect = Fx.blastExplosion;
    public Sound breakSound = Sounds.unitExplode1;
    public float breakVolume = 1f;
    public float breakShake = 2f;

    /** 贴图所在图层 */
    public float layer = Layer.flyingUnit;
    public String headSprite = "phase-wall";
    public String rootSprite = "titanium-wall-large";
    public String chainSprite = "router";
    /** 锁链贴图宽度 */
    public float chainSpriteWidth = 4f;
    /** 锁链单张贴图原长度 */
    public float chainSpriteLength = 12f;
    /** 启用后，锁链贴图为拉伸的单张贴图，而不是逐个拼接 */
    public boolean chainShrink = false;
    /** chainShrink为false时，启用后，最靠近根部的一张锁链贴图会被压缩以防反方向超出根部贴图 */
    public boolean chainRootShrink = false;
    /** 启用后，锁链抓住单位时头部切换为另一贴图 */
    public boolean headChange = true;
    public String headCaughtSprite = "surge-wall";
    /** 启用后，锁链发射出去时根部切换为另一贴图 */
    public boolean rootChange = true;
    public String rootReleasedSprite = "plastanium-wall-large";

    @Override
    public void addStats(UnitType u, Table t){
        t.row();
        t.add("test chain weapon");
    }

    public TextureRegion headRegion = new TextureRegion();
    public TextureRegion rootRegion = new TextureRegion();
    public TextureRegion chainRegion = new TextureRegion();
    public TextureRegion headCaughtRegion = new TextureRegion();
    public TextureRegion rootReleasedRegion = new TextureRegion();

    private final boolean debug = false;

    protected class OutChain {
        public Unit unit;
        public int number;
        public float timer;
        public float rot;
        public float x, y;
        public float catchD, catchRot, catchVisualRot;
        public float length = maxLength;
        public boolean caught = false;
        public float pointX, pointY, pointRot, pointD;
        public OutChain(int number, float rot, float x, float y){
            this.number = number;
            this.rot = rot;
            this.x = x;
            this.y = y;
        }
    }

    protected Color effectColor(Unit u){
        return effectColor == null ? u.team.color : effectColor;
    }
    protected float posD(float x, float y, float rot, boolean sin){
        if(!sin) return x * Mathf.cosDeg(rot - 90f) + y * Mathf.cosDeg(rot);
        return x * Mathf.sinDeg(rot - 90f) + y * Mathf.sinDeg(rot);
    }

    public class ChainWeaponMount extends WeaponMount{
        public ChainWeaponMount(Weapon w) {
            super(w);
        }
        protected Seq<ChainPoint> chainPoints = new Seq<>();
        protected Seq<OutChain> chains = new Seq<>();
        protected boolean shootingLast = false;
        protected int currentChainOrder = -1;
        protected boolean init = false;

        protected float pointCount(){
            return chainPoints.size;
        }
        protected boolean pointReleased(ChainPoint p){
            if(p == null) return false;
            return chains.contains(c -> c.number == p.order);
        }
        protected float outCount(){
            return chains.size;
        }
        protected ChainPoint getChainPoint(OutChain c){
            return chainPoints.find(p -> p.order == c.number);
        }

        protected void chainBreak(OutChain c, Unit u){
            breakSound.at(c.x, c.y, 1f, breakVolume);
            breakEffect.at(c.x, c.y, c.rot, effectColor(u));
            Effect.shake(breakShake, breakShake * 2f, c.x, c.y);
            chains.remove(c);
        }

        protected void catchUnit(OutChain c, Unit u){
            for(Unit u2 : Groups.unit){
                if(!((catchSameTeam && (!catchSameTeamPlayerOnly || u.isPlayer())) || u2.team != u.team) || u2 == u) continue;
                float r = baseCatchRadius + u2.hitSize() * 0.5f;
                if(Mathf.dst2(c.x, c.y, u2.x, u2.y) <= r * r){
                    c.caught = true;
                    c.unit = u2;
                    c.catchRot = Mathf.atan2(c.x - u2.x, c.y - u2.y) * Mathf.radiansToDegrees - u2.rotation();
                    c.catchD = Mathf.dst(c.x, c.y, u2.x, u2.y);
                    c.length = Math.max(catchLength, Mathf.dst(getChainPoint(c).realX, getChainPoint(c).realY, c.x, c.y));
                    c.catchVisualRot = c.rot - u2.rotation();
                    catchEffect.at(c.x, c.y, c.rot, effectColor(u));
                    catchSound.at(c.x, c.y, 1f, catchVolume);
                    Effect.shake(catchShake, catchShake * 2, c.x, c.y);
                    if(debug) Log.info("unit caught: " + u2.type.name + " by order: " + c.number);
                    break;
                }
            }
        }
    }




    @Override
    public void update(Unit u, WeaponMount wm){
        if(!(wm instanceof ChainWeaponMount m)) return;
        if(!m.init){
            headRegion = Core.atlas.find(headSprite);
            rootRegion = Core.atlas.find(rootSprite);
            chainRegion = Core.atlas.find(chainSprite);
            headCaughtRegion = Core.atlas.find(headCaughtSprite);
            rootReleasedRegion = Core.atlas.find(rootReleasedSprite);
            for(ChainPoint p : chainPointsAdd){
                m.chainPoints.add(new ChainPoint(p.x, p.y, p.rot, p.order));
            }
            u.type.clipSize = Math.max(u.type.clipSize, maxLength * 1.2f);
            m.init = true;
            if(debug) Log.info("chain init complete");
        }
        chainUpdate(u, m);
        if(!m.shootingLast && u.isShooting()){
            for(int i = 0; i < m.chainPoints.size; i++){
                int finalI = i;
                ChainPoint p = m.chainPoints.find(pf -> pf.order == finalI);
                if(m.pointReleased(p) && m.pointCount() > m.outCount()){
                    continue;
                }
                if(m.pointCount() <= m.outCount()){
                    m.currentChainOrder ++;
                    if(m.currentChainOrder >= m.pointCount()){
                        m.currentChainOrder = 0;
                    }
                    m.chainBreak(m.chains.find(c -> c.number == m.currentChainOrder), u);
                    p = m.chainPoints.find(pf -> pf.order == m.currentChainOrder);
                }else{
                    m.currentChainOrder = p == null ? 0 : p.order;
                }
                float px = p.realX;
                float py = p.realY;
                float releaseRot = Mathf.atan2(u.aimX() - px, u.aimY() - py) * Mathf.radiansToDegrees;
                m.chains.add(new OutChain(p.order, releaseRot, px, py));
                releaseEffect.at(px, py, releaseRot, effectColor(u));
                releaseSound.at(px, py, 1f, releaseVolume);
                Effect.shake(releaseShake, releaseShake * 2f, px, py);
                if(debug) Log.info("chain released, order: " + p.order);
                break;
            }
        }
        m.shootingLast = u.isShooting();
    }

    private float extendEffectTimer = 0;

    public void chainUpdate(Unit u, ChainWeaponMount m){
        for(ChainPoint p : m.chainPoints){
            p.realX = u.x + posD(p.x, p.y, u.rotation(), false);
            p.realY = u.y + posD(p.x, p.y, u.rotation(), true);
        }
        for(OutChain c : m.chains){
            float px = m.getChainPoint(c).realX;
            float py = m.getChainPoint(c).realY;
            c.pointX = px;
            c.pointY = py;
            c.pointRot = Mathf.atan2(c.x - px, c.y - py) * Mathf.radiansToDegrees;
            c.pointD = Mathf.dst(c.x, c.y, px, py);
            if(c.unit == null){
                if(c.caught || c.pointD > maxLength || (rightClickCancel && Core.input.keyDown(KeyCode.mouseRight) && u.isPlayer() && !Vars.control.input.commandMode)){
                    m.chainBreak(c, u);
                    continue;
                }
                c.x += speed * Mathf.cosDeg(c.rot);
                c.y += speed * Mathf.sinDeg(c.rot);
                Vars.control.sound.loop(extendSound, new Vec2(c.x, c.y), extendVolume);
                if(extendSoundRoot){
                    Vars.control.sound.loop(extendSound, new Vec2(px, py), extendVolume);
                }
                if(extendEffect != Fx.none){
                    if(extendEffectInterval > 0){
                        extendEffectTimer += Time.delta;
                        if(extendEffectTimer >= extendEffectInterval){
                            extendEffectTimer = 0;
                            extendEffect.at(c.x, c.y, c.rot, effectColor(u));
                        }
                    }else{
                        extendEffect.at(c.x, c.y, c.rot, effectColor(u));
                    }
                }
                m.catchUnit(c, u);
            }else{
                if(c.caught && (c.unit.dead() || (rightClickCancel && Core.input.keyDown(KeyCode.mouseRight) && u.isPlayer() && !Vars.control.input.commandMode))){
                    m.chainBreak(c, u);
                }
                if(lifetime > 0){
                    c.timer += Time.delta;
                    if(c.timer >= lifetime){
                        m.chainBreak(c, u);
                        continue;
                    }
                }
                c.x = c.unit.x() + c.catchD * Mathf.cosDeg(c.catchRot + c.unit.rotation());
                c.y = c.unit.y() + c.catchD * Mathf.sinDeg(c.catchRot + c.unit.rotation());
                c.rot = c.catchVisualRot + c.unit.rotation();
                float d = c.pointD;
                if(d > catchLength && d < c.length){
                    c.length = d;
                }
                c.length = Mathf.approachDelta(c.length, catchLength, speed * backSpeedMultiplier);
                if(d > c.length){
                    float extra = d - c.length;
                    float rot = Mathf.atan2(px - c.x, py - c.y) * Mathf.radiansToDegrees;
                    Tmp.v3.set(c.unit).sub(new Vec2(c.x, c.y)).nor().scl(force * 10f * extra);
                    Tmp.v3.setAngle(rot);
                    c.unit.impulse(Tmp.v3);
                    Tmp.v3.setAngle(rot + 180f);
                    u.impulse(Tmp.v3);
                }
            }
        }
    }



    @Override
    public void draw(Unit u, WeaponMount wm){
        if(!(wm instanceof ChainWeaponMount m) || !m.init) return;
        Draw.z(layer);
        Draw.color();
        for(OutChain c : m.chains){
            if(chainShrink){
                Draw.rect(chainRegion, (c.pointX + c.x) * 0.5f, (c.pointY + c.y) * 0.5f,
                        chainSpriteWidth, c.pointD, c.pointRot - 90f);
            }else{
                float dd = 0;
                while(dd < c.pointD - chainSpriteLength){
                    Draw.rect(chainRegion, c.x - (dd + 0.5f * chainSpriteLength) * Mathf.cosDeg(c.pointRot),
                            c.y - (dd + 0.5f * chainSpriteLength) * Mathf.sinDeg(c.pointRot),
                            chainSpriteWidth, chainSpriteLength, c.pointRot - 90f);
                    dd += chainSpriteLength;
                }
                if(!chainRootShrink){
                    Draw.rect(chainRegion, c.x - (dd + 0.5f * chainSpriteLength) * Mathf.cosDeg(c.pointRot),
                            c.y - (dd + 0.5f * chainSpriteLength) * Mathf.sinDeg(c.pointRot),
                            chainSpriteWidth, chainSpriteLength, c.pointRot - 90f);
                }else{
                    float restLen = dd - c.pointD;
                    Draw.rect(chainRegion, c.x - (dd + 0.5f * restLen) * Mathf.cosDeg(c.pointRot),
                            c.y - (dd + 0.5f * restLen) * Mathf.sinDeg(c.pointRot),
                            chainSpriteWidth, restLen, c.pointRot - 90f);
                }
            }
            Draw.rect(headChange && c.caught ? headCaughtRegion : headRegion, c.x, c.y, c.rot - 90f);
            Draw.rect(rootChange ? rootReleasedRegion : rootRegion, c.pointX, c.pointY, c.pointRot - 90f);
        }
        for(ChainPoint p : m.chainPoints){
            if(m.pointReleased(p)) continue;
            Draw.rect(headRegion, p.realX, p.realY, p.rot + u.rotation() - 90f);
            Draw.rect(rootRegion, p.realX, p.realY, p.rot + u.rotation() - 90f);
        }
    }
}
