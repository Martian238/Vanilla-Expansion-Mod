package VanillaExpansion.expand.abilities;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.struct.Queue;
import arc.struct.Seq;
import arc.util.Interval;
import mindustry.Vars;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.graphics.Trail;
import mindustry.input.Binding;
import mindustry.type.UnitType;
import VanillaExpansion.expand.units.AncientEngine;

public class BoostAbility extends Ability {
    public static final int maxSize = 8;

    public boolean drawAirFlow = true;

    public float angleMaxDst = 90f;
    public float velocityMultiple;
    public float warmupTime = 120f;
    public int trailLength = 8;

    public float angleCone = 5f;
    protected Seq<Trail> trails = new Seq<>();  // 初始化为空序列
    protected Seq<AncientEngine> ancientEngines = new Seq<>();  // 初始化为空序列
    protected Queue<Float> seq = new Queue<>(maxSize + 1);
    protected Interval timer = new Interval();

    public BoostAbility() {}

    public BoostAbility(boolean drawAirFlow, float velocityMultiple, float angleCone) {
        this.drawAirFlow = drawAirFlow;
        this.velocityMultiple = velocityMultiple;
        this.angleCone = angleCone;
    }

    public BoostAbility(float velocityMultiple, float angleCone) {
        this.velocityMultiple = velocityMultiple;
        this.angleCone = angleCone;
    }

    public BoostAbility(float velocityMultiple) {
        this.velocityMultiple = velocityMultiple;
    }

    @Override
    public void init(UnitType type) {
        // 清空并重新初始化
        trails.clear();
        ancientEngines.clear();

        float size = type.engineSize;
        if (size <= 0) size = 1f;  // 防止除以0

        for (UnitType.UnitEngine e : type.engines) {
            if (e instanceof AncientEngine ae) {
                ancientEngines.add(ae);
            } else {
                int length = Math.max(1, (int)(Mathf.clamp(e.radius / size) * trailLength));
                trails.add(new Trail(length));
            }
        }

        // 如果没有任何引擎，添加默认轨迹
        if (trails.isEmpty() && ancientEngines.isEmpty()) {
            trails.add(new Trail(trailLength));
        }
    }

    @Override
    public BoostAbility copy() {
        BoostAbility out = (BoostAbility)super.copy();

        // 安全复制 trails - 检查是否为 null
        out.trails = new Seq<>();
        if (trails != null) {
            for (Trail trail : trails) {
                if (trail != null) {
                    out.trails.add(new Trail(trail.length));
                } else {
                    out.trails.add(new Trail(trailLength));
                }
            }
        } else {
            // 如果 trails 为 null，创建默认轨迹
            out.trails.add(new Trail(trailLength));
        }

        // 安全复制 ancientEngines
        out.ancientEngines = new Seq<>();
        if (ancientEngines != null) {
            out.ancientEngines.addAll(ancientEngines);
        }

        out.seq = new Queue<>(maxSize + 1);
        out.timer = new Interval();
        return out;
    }

    public void resetTrails() {
        if (trails != null) {
            for (Trail trail : trails) {
                if (trail != null) {
                    trail.clear();
                }
            }
        }
        seq.clear();
        timer = new Interval();
    }

    public float warmup(float angle) {
        if (seq == null || seq.size == 0) return 0f;
        float f = 0;
        for (float i : seq) {
            if (Angles.within(angle, i, angleCone)) f++;
        }
        return f / seq.size;
    }

    public boolean allSame(float angle, float lookAng) {
        if (seq == null || seq.size < maxSize - 1 || !Angles.within(angle, lookAng, angleMaxDst)) return false;
        for (float f : seq) {
            if (!Angles.within(angle, f, angleCone)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void update(Unit unit) {
        // 确保 trails 不为 null
        if (trails == null) {
            trails = new Seq<>();
            trails.add(new Trail(trailLength));
        }

        float angle = unit.vel.angle();
        float speed = unit.vel.len();
        boolean same = allSame(angle, unit.rotation());
        boolean shiftBoost = Core.input.keyDown(Binding.boost);
        boolean boosting = same || shiftBoost;

        if (speed < 0.01f && !seq.isEmpty()) {
            seq.removeFirst();
        }

        if (boosting) {
            unit.speedMultiplier(unit.speedMultiplier() * velocityMultiple);
        }

        if (seq.size > maxSize) seq.removeFirst();
        if (timer.get(12f) && speed > 0.1f) seq.add(angle);

        if (ancientEngines != null) {
            for (AncientEngine ae : ancientEngines) {
                ae.setBoost(boosting ? velocityMultiple : 1f);
            }
        }

        if (Vars.headless) return;

        if (trails != null && unit.type != null && unit.type.engines != null) {
            for (int i = 0; i < trails.size && i < unit.type.engines.size; i++) {
                Trail trail = trails.get(i);
                if (trail == null) continue;

                UnitType.UnitEngine engine = unit.type.engines.get(i);
                if (engine instanceof AncientEngine) continue;

                Vec2 vec2 = unitEngineOffset(unit, engine);
                trail.update(unit.x + vec2.x, unit.y + vec2.y, boosting ? 1 : 0);
            }
        }
    }

    @Override
    public void draw(Unit unit) {
        // 确保 trails 不为 null
        if (trails == null || trails.isEmpty()) return;
        if (unit.type == null || unit.type.engines == null) return;

        float z = Draw.z();
        Draw.z(unit.type.engineLayer > 0 ? unit.type.engineLayer : unit.type.lowAltitude ? Layer.flyingUnitLow - 0.001f : Layer.flyingUnit - 0.001f);
        Color color = unit.type.engineColor == null ? unit.team.color : unit.type.engineColor;

        for (int i = 0; i < trails.size && i < unit.type.engines.size; i++) {
            UnitType.UnitEngine engine = unit.type.engines.get(i);
            if (engine instanceof AncientEngine) continue;

            Trail trail = trails.get(i);
            if (trail != null) {
                trail.draw(color, engine.radius / 1.25f);
            }
        }
        Draw.z(z);
    }

    private static final Vec2 tmp = new Vec2();

    private Vec2 unitEngineOffset(Unit unit, UnitType.UnitEngine engine) {
        tmp.trns(unit.rotation, engine.y, -engine.x);
        return tmp;
    }
}