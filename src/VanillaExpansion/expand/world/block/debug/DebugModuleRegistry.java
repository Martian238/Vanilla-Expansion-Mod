package VanillaExpansion.expand.world.block.debug;
import arc.func.Prov;
import arc.struct.ObjectMap;
import arc.struct.Seq;
public final class DebugModuleRegistry {
    private static final ObjectMap<String, Prov<DebugModule>> factories = new ObjectMap<>();
    private DebugModuleRegistry() {}
    public static boolean register(String id, Prov<DebugModule> factory) {
        if (id == null || id.isEmpty() || factory == null || factories.containsKey(id)) return false;
        factories.put(id, factory);
        return true;
    }
    public static boolean unregister(String id) {
        if (id == null || !factories.containsKey(id)) return false;
        factories.remove(id);
        return true;
    }
    public static boolean contains(String id) {
        return id != null && factories.containsKey(id);
    }
    public static DebugModule create(String id) {
        Prov<DebugModule> factory = id == null ? null : factories.get(id);
        return factory == null ? null : factory.get();
    }
    public static Seq<String> ids() {
        Seq<String> result = new Seq<>();
        for (String id : factories.keys()) result.add(id);
        return result;
    }
    public static void clear() {
        factories.clear();
    }
}
