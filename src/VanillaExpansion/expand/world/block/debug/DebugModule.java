package VanillaExpansion.expand.world.block.debug;
import arc.scene.ui.layout.Table;
import arc.util.io.Reads;
import arc.util.io.Writes;
public interface DebugModule {
    String id();
    default String displayName() { return id(); }
    default void buildConfig(DebugBlock.DebugBuild build, Table table, Runnable rebuild) {}
    default boolean buildBars(DebugBlock.DebugBuild build, Table table) { return false; }
    default void attach(DebugBlock.DebugBuild build) {}
    default void detach(DebugBlock.DebugBuild build) {}
    default void update(DebugBlock.DebugBuild build) {}
    default void draw(DebugBlock.DebugBuild build) {}
    default void drawSelect(DebugBlock.DebugBuild build) {}
    default void placed(DebugBlock.DebugBuild build) {}
    default void removed(DebugBlock.DebugBuild build) {}
    default void destroyed(DebugBlock.DebugBuild build) {}
    default void proximityUpdated(DebugBlock.DebugBuild build) {}
    default boolean configure(DebugBlock.DebugBuild build, Object value) { return false; }
    default void read(DebugBlock.DebugBuild build, Reads read, byte revision) {}
    default void write(DebugBlock.DebugBuild build, Writes write) {}
}
