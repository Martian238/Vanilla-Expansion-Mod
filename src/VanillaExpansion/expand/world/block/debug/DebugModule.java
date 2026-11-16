package VanillaExpansion.expand.world.block.debug;

import arc.scene.ui.layout.Table;
import arc.util.io.Reads;
import arc.util.io.Writes;

public interface DebugModule {
    // 返回模块唯一标识
    String id();

    // 返回模块显示名称
    default String displayName() { return id(); }

    // 在卡片内构建模块配置界面
    default void buildConfig(DebugBlock.DebugBuild build, Table table, Runnable rebuild) {}

    // 构建模块状态栏，返回是否添加了状态栏
    default boolean buildBars(DebugBlock.DebugBuild build, Table table) { return false; }

    // 模块挂载到建筑时调用
    default void attach(DebugBlock.DebugBuild build) {}

    // 模块从建筑卸载时调用
    default void detach(DebugBlock.DebugBuild build) {}

    // 模块随建筑更新时调用
    default void update(DebugBlock.DebugBuild build) {}

    // 模块随建筑绘制时调用
    default void draw(DebugBlock.DebugBuild build) {}

    // 鼠标悬停建筑时绘制范围指示
    default void drawSelect(DebugBlock.DebugBuild build) {}

    // 建筑放置到世界时调用
    default void placed(DebugBlock.DebugBuild build) {}

    // 建筑从世界移除时调用
    default void removed(DebugBlock.DebugBuild build) {}

    // 建筑被摧毁时调用
    default void destroyed(DebugBlock.DebugBuild build) {}

    // 建筑邻接关系更新时调用
    default void proximityUpdated(DebugBlock.DebugBuild build) {}

    // 接收配置值并返回是否消费
    default boolean configure(DebugBlock.DebugBuild build, Object value) { return false; }

    // 读取模块持久化数据
    default void read(DebugBlock.DebugBuild build, Reads read, byte revision) {}

    // 写入模块持久化数据
    default void write(DebugBlock.DebugBuild build, Writes write) {}
}
