package VanillaExpansion;

import VanillaExpansion.expand.type.unit.HyperUnit;
import VanillaExpansion.expand.type.unit.IronGolemUnit;
import VanillaExpansion.expand.type.unit.SentryUnit;
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

    /**
     * Registers all entities with their corresponding provider.
     */
    private static void registerEntities() {
        put(HyperUnit.class, HyperUnit::new);
        put(SentryUnit.class, SentryUnit::new);
        put(IronGolemUnit.class, IronGolemUnit::new);
    }

    /**
     * Adds a class and its corresponding provider to the registration map.
     */
    public static <T extends Entityc> void put(Class<T> c, Prov<T> prov) {
        needIdClasses.put(c, new ProvSet(prov));
    }

    /**
     * Retrieves the ID of a registered class.
     * Returns -1 if the class is not registered.
     */
    public static <T extends Entityc> int getID(Class<T> c) {
        return classIdMap.get(c, -1);
    }

    /**
     * Loads all registered entities and assigns them unique IDs.
     */
    public static void load() {
        Seq<Class<?>> sortedKeys = needIdClasses.keys().toSeq().sortComparing(Class::getName);

        for (Class<?> c : sortedKeys) {
            int id = EntityMapping.register(c.getName(), needIdClasses.get(c).prov);
            classIdMap.put(c, id);
        }
    }

    /**
     * Logs debug information about the registered classes and their IDs.
     */


    /**
     * Wrapper class for entity providers with their corresponding name.
     */
    public static class ProvSet {
        public final String name;
        public final Prov<?> prov;

        public ProvSet(Prov<?> prov) {
            this.name = prov.get().getClass().toString();
            this.prov = prov;
        }
    }
}
