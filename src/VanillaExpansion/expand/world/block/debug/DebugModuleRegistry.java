package VanillaExpansion.expand.world.block.debug;

import arc.func.Prov;
import arc.struct.ObjectMap;
import arc.struct.Seq;

public final class DebugModuleRegistry {
    private static final ObjectMap<String, Prov<DebugModule>> factories = new ObjectMap<>();

    // 禁止实例化
    private DebugModuleRegistry() {}

    // 注册模块工厂
    public static boolean register(String id, Prov<DebugModule> factory) {
        if (id == null || id.isEmpty() || factory == null || factories.containsKey(id)) return false;
        factories.put(id, factory);
        return true;
    }

    // 注销模块工厂
    public static boolean unregister(String id) {
        if (id == null || !factories.containsKey(id)) return false;
        factories.remove(id);
        return true;
    }

    // 判断模块是否已注册
    public static boolean contains(String id) {
        return id != null && factories.containsKey(id);
    }

    // 创建模块实例
    public static DebugModule create(String id) {
        Prov<DebugModule> factory = id == null ? null : factories.get(id);
        return factory == null ? null : factory.get();
    }

    // 获取全部已注册模块标识
    public static Seq<String> ids() {
        Seq<String> result = new Seq<>();
        for (String id : factories.keys()) result.add(id);
        return result;
    }

    // 清空全部模块工厂
    public static void clear() {
        factories.clear();
    }
}
