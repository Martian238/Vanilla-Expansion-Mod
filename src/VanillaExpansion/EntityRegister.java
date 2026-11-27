package VanillaExpansion;

import VanillaExpansion.expand.type.unit.*;
import VanillaExpansion.expand.type.unit.annihilate.AnnihilateMainUnit;
import VanillaExpansion.expand.type.unit.annihilate.AnnihilatePartUnit;
import VanillaExpansion.expand.type.unit.internal.ChiniunChargeUnit;
import VanillaExpansion.expand.type.unit.internal.HyperSpawnUnit;
import VanillaExpansion.expand.type.unit.internal.OrbitStrikeBeaconUnit;
import arc.func.Prov;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.gen.EntityMapping;
import mindustry.gen.Entityc;

public class EntityRegister {


    //代码来源：新视界 New Horizon Mod (https://github.com/Yuria-Shikibe/NewHorizonMod)

    private static final ObjectMap<Class<?>, ProvSet> needIdClasses = new ObjectMap<>();
    private static final ObjectMap<Class<?>, Integer> classIdMap = new ObjectMap<>();

    static {
        registerEntities();
    }
    private static void registerEntities() {
        put(HyperUnit.class, HyperUnit::new);
        put(SentryUnit.class, SentryUnit::new);
        put(IronGolemUnit.class, IronGolemUnit::new);
        put(DayunTankUnit.class, DayunTankUnit::new);
        put(AnnihilatePartUnit.class, AnnihilatePartUnit::new);
        put(AnnihilateMainUnit.class, AnnihilateMainUnit::new);
        put(ChiniunChargeUnit.class, ChiniunChargeUnit::new);
        put(OmicronUnit.class, OmicronUnit::new);
        put(HyperSpawnUnit.class, HyperSpawnUnit::new);
        put(OrbitStrikeBeaconUnit.class, OrbitStrikeBeaconUnit::new);
    }
    public static <T extends Entityc> void put(Class<T> c, Prov<T> prov) {
        needIdClasses.put(c, new ProvSet(prov));
    }
    public static <T extends Entityc> int getID(Class<T> c) {
        return classIdMap.get(c, -1);
    }
    public static void load() {
        Seq<Class<?>> sortedKeys = needIdClasses.keys().toSeq().sortComparing(Class::getName);

        for (Class<?> c : sortedKeys) {
            int id = EntityMapping.register(c.getName(), needIdClasses.get(c).prov);
            classIdMap.put(c, id);
        }
    }
    public static class ProvSet {
        public final String name;
        public final Prov<?> prov;

        public ProvSet(Prov<?> prov) {
            this.name = prov.get().getClass().toString();
            this.prov = prov;
        }
    }
}
