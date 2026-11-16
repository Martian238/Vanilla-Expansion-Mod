package VanillaExpansion.expand.world.block.debug.Module;

import VanillaExpansion.expand.world.block.debug.DebugModuleRegistry;

public final class DebugModules {
    // 禁止实例化
    private DebugModules() {}

    // 注册模块
    public static void register() {
        DebugModuleRegistry.register(ShieldProjectionModule.ID, ShieldProjectionModule::new);
        DebugModuleRegistry.register(MendProjectorModule.ID, MendProjectorModule::new);
        DebugModuleRegistry.register(OverdriveProjectorModule.ID, OverdriveProjectorModule::new);
        DebugModuleRegistry.register(SourceModule.ID, SourceModule::new);
    }
}
