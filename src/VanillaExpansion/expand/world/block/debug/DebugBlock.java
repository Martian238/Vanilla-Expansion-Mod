package VanillaExpansion.expand.world.block.debug;
import VanillaExpansion.ui.debug.DebugModuleDialog;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.io.Reads;
import arc.util.io.Writes;
import arc.util.serialization.Base64Coder;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.type.Category;
import mindustry.world.Block;
import mindustry.world.meta.Env;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
public class DebugBlock extends Block {
    public static final int DEFAULT_SIZE = 1;
    public static final int DEFAULT_HEALTH = 200;
    public static final int DEFAULT_MAX_MODULES = 24;
    public static final byte SAVE_VERSION = 2;
    public static final float MODULE_BAR_HEIGHT = 42f;
    public static final String CONFIG_ADD_PREFIX = "+";
    public static final String CONFIG_REMOVE_PREFIX = "-";
    public static final String CONFIG_ENABLE_PREFIX = "on:";
    public static final String CONFIG_DISABLE_PREFIX = "off:";
    public static final String CONFIG_CLEAR = "clear";
    public static final String CONFIG_DATA_PREFIX = "data:";

    public final Seq<String> defaultModuleIds = new Seq<>();
    public int maxModules = DEFAULT_MAX_MODULES;
    public DebugBlock(String name) {
        super(name);
        size = DEFAULT_SIZE;
        health = DEFAULT_HEALTH;
        update = true;
        solid = true;
        destructible = true;
        configurable = true;
        logicConfigurable = true;
        saveConfig = false;
        copyConfig = true;
        clearOnDoubleTap = false;
        category = Category.logic;
        envEnabled = Env.any;
        hasPower = true;
        outputsPower = true;
        consumePower(0f);
        config(String.class, (DebugBuild build, String command) -> build.applyModuleCommand(command));
    }
    public class DebugBuild extends Building {
        public final Seq<ModuleSlot> moduleSlots = new Seq<>();
        protected boolean modulePlaced = false;
        public float modulePowerProduction = 0f;
        @Override
        public float getPowerProduction() {
            return super.getPowerProduction() + modulePowerProduction;
        }
        // 滚木伤害
        @Override
        public void damage(float amount) {
            //super.damage(amount);
        }
        // 滚木kill
        @Override
        public void kill() {
            //super.kill();
        }
        @Override
        public void created() {
            super.created();
            installDefaultModules();
        }
        @Override
        public void updateTile() {
            super.updateTile();
            if (!enabled) return;
            for (int i = 0; i < moduleSlots.size; i++) {
                ModuleSlot slot = moduleSlots.get(i);
                if (slot.enabled) slot.module.update(this);
            }
        }
        @Override
        public void draw() {
            super.draw();
            for (int i = 0; i < moduleSlots.size; i++) {
                ModuleSlot slot = moduleSlots.get(i);
                if (slot.enabled) slot.module.draw(this);
            }
        }
        @Override
        public void displayBars(Table table) {
            super.displayBars(table);
            if (Vars.headless) return;
            for (int i = 0; i < moduleSlots.size; i++) {
                ModuleSlot slot = moduleSlots.get(i);
                if (!slot.enabled) continue;
                Table bars = moduleBars(slot);
                if (bars != null) table.add(bars).growX().height(MODULE_BAR_HEIGHT).padTop(2f).row();
            }
        }
        private Table moduleBars(ModuleSlot slot) {
            Table bars = new Table();
            if (!slot.module.buildBars(this, bars)) return null;
            Table content = new Table();
            content.top().left();
            Table head = new Table();
            head.left();
            head.add().growX();
            head.add("[lightgray]" + slot.module.displayName() + "[]").right();
            content.add(head).growX().padBottom(2f).row();
            content.add(bars).growX();
            return content;
        }
        @Override
        public void drawSelect() {
            super.drawSelect();
            if (Vars.headless) return;
            for (int i = 0; i < moduleSlots.size; i++) {
                ModuleSlot slot = moduleSlots.get(i);
                if (slot.enabled) slot.module.drawSelect(this);
            }
        }
        @Override
        public void placed() {
            super.placed();
            modulePlaced = true;
            for (int i = 0; i < moduleSlots.size; i++) {
                ModuleSlot slot = moduleSlots.get(i);
                if (slot.enabled) slot.module.placed(this);
            }
        }
        @Override
        public void buildConfiguration(Table table) {
            if (Vars.headless || !isValid()) return;
            deselect();
            new DebugModuleDialog(this).show();
        }
        @Override
        public Object config() {
            return encodeConfig();
        }
        @Override
        public void onRemoved() {
            modulePlaced = false;
            clearModules();
            super.onRemoved();
        }
        @Override
        public void onDestroyed() {
            super.onDestroyed();
            for (int i = 0; i < moduleSlots.size; i++) {
                ModuleSlot slot = moduleSlots.get(i);
                if (slot.enabled) slot.module.destroyed(this);
            }
        }
        @Override
        public void onProximityUpdate() {
            super.onProximityUpdate();
            for (int i = 0; i < moduleSlots.size; i++) {
                ModuleSlot slot = moduleSlots.get(i);
                if (slot.enabled) slot.module.proximityUpdated(this);
            }
        }
        @Override
        public byte version() {
            return SAVE_VERSION;
        }
        @Override
        public void write(Writes write) {
            super.write(write);
            write.i(moduleSlots.size);
            for (int i = 0; i < moduleSlots.size; i++) {
                ModuleSlot slot = moduleSlots.get(i);
                write.str(slot.id);
                write.bool(slot.enabled);
                byte[] data = writeModuleData(slot.module);
                write.i(data.length);
                write.b(data);
            }
        }
        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            if (revision < 1) {
                installDefaultModules();
                return;
            }
            clearModules();
            int count = read.i();
            for (int i = 0; i < count; i++) {
                String id = read.str();
                boolean enabled = read.bool();
                int length = read.i();
                byte[] data = read.b(length);
                if (i >= maxModules) continue;
                DebugModule module = DebugModuleRegistry.create(id);
                if (module == null) continue;
                ModuleSlot slot = new ModuleSlot(id, module, enabled);
                moduleSlots.add(slot);
                module.attach(this);
                readModuleData(module, data, revision);
                if (modulePlaced && enabled) module.placed(this);
            }
        }
        protected void installDefaultModules() {
            for (int i = 0; i < defaultModuleIds.size; i++) {
                String id = defaultModuleIds.get(i);
                if (getSlot(id) == null) addModule(id);
            }
        }
        public boolean applyModuleCommand(String command) {
            if (command == null) return false;
            String value = command.trim();
            if (value.isEmpty()) return false;
            if (value.equals(CONFIG_CLEAR)) return clearModules();
            if (value.startsWith(CONFIG_DATA_PREFIX)) return applyEncodedConfig(value.substring(CONFIG_DATA_PREFIX.length()));
            if (value.startsWith(CONFIG_ADD_PREFIX)) return addModule(value.substring(CONFIG_ADD_PREFIX.length()).trim()) != null;
            if (value.startsWith(CONFIG_REMOVE_PREFIX)) return removeModule(value.substring(CONFIG_REMOVE_PREFIX.length()).trim());
            if (value.startsWith(CONFIG_ENABLE_PREFIX)) return setModuleEnabled(value.substring(CONFIG_ENABLE_PREFIX.length()).trim(), true);
            if (value.startsWith(CONFIG_DISABLE_PREFIX)) return setModuleEnabled(value.substring(CONFIG_DISABLE_PREFIX.length()).trim(), false);
            return configureModules(value);
        }
        protected boolean configureModules(Object value) {
            for (int i = 0; i < moduleSlots.size; i++) {
                ModuleSlot slot = moduleSlots.get(i);
                if (slot.enabled && slot.module.configure(this, value)) return true;
            }
            return false;
        }
        protected boolean applyEncodedConfig(String data) {
            try {
                byte[] bytes = Base64Coder.decode(data);
                Reads read = new Reads(new DataInputStream(new ByteArrayInputStream(bytes)));
                clearModules();
                int count = read.i();
                for (int i = 0; i < count; i++) {
                    String id = read.str();
                    boolean enabled = read.bool();
                    int length = read.i();
                    byte[] moduleData = read.b(length);
                    if (i >= maxModules) continue;
                    DebugModule module = DebugModuleRegistry.create(id);
                    if (module == null) continue;
                    ModuleSlot slot = new ModuleSlot(id, module, enabled);
                    moduleSlots.add(slot);
                    module.attach(this);
                    readModuleData(module, moduleData, SAVE_VERSION);
                    if (modulePlaced && enabled) module.placed(this);
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        public DebugModule addModule(String id) {
            DebugModule module = DebugModuleRegistry.create(id);
            if (module == null || !addModule(module)) return null;
            return module;
        }
        public boolean addModule(DebugModule module) {
            if (module == null) return false;
            String id = module.id();
            if (id == null || id.isEmpty() || moduleSlots.size >= maxModules || getSlot(id) != null) return false;
            ModuleSlot slot = new ModuleSlot(id, module, true);
            moduleSlots.add(slot);
            module.attach(this);
            if (modulePlaced) module.placed(this);
            return true;
        }
        public boolean removeModule(String id) {
            ModuleSlot slot = getSlot(id);
            if (slot == null) return false;
            moduleSlots.remove(slot);
            if (slot.enabled) slot.module.removed(this);
            slot.module.detach(this);
            return true;
        }
        public boolean clearModules() {
            if (moduleSlots.isEmpty()) return false;
            for (int i = 0; i < moduleSlots.size; i++) {
                ModuleSlot slot = moduleSlots.get(i);
                if (slot.enabled) slot.module.removed(this);
                slot.module.detach(this);
            }
            moduleSlots.clear();
            return true;
        }
        public boolean setModuleEnabled(String id, boolean value) {
            ModuleSlot slot = getSlot(id);
            if (slot == null || slot.enabled == value) return false;
            slot.enabled = value;
            if (value) {
                if (modulePlaced) slot.module.placed(this);
            } else {
                slot.module.removed(this);
            }
            return true;
        }
        public ModuleSlot getSlot(String id) {
            if (id == null) return null;
            for (int i = 0; i < moduleSlots.size; i++) {
                ModuleSlot slot = moduleSlots.get(i);
                if (slot.id.equals(id)) return slot;
            }
            return null;
        }
        protected byte[] writeModuleData(DebugModule module) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Writes writes = new Writes(new DataOutputStream(output));
            module.write(this, writes);
            writes.close();
            return output.toByteArray();
        }
        protected String encodeConfig() {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Writes writes = new Writes(new DataOutputStream(output));
            writes.i(moduleSlots.size);
            for (int i = 0; i < moduleSlots.size; i++) {
                ModuleSlot slot = moduleSlots.get(i);
                writes.str(slot.id);
                writes.bool(slot.enabled);
                byte[] data = writeModuleData(slot.module);
                writes.i(data.length);
                writes.b(data);
            }
            writes.close();
            return CONFIG_DATA_PREFIX + new String(Base64Coder.encode(output.toByteArray()));
        }
        protected void readModuleData(DebugModule module, byte[] data, byte revision) {
            if (data.length == 0) return;
            module.read(this, new Reads(new DataInputStream(new ByteArrayInputStream(data))), revision);
        }
    }
    public static class ModuleSlot {
        public final String id;
        public final DebugModule module;
        public boolean enabled;
        public ModuleSlot(String id, DebugModule module, boolean enabled) {
            this.id = id;
            this.module = module;
            this.enabled = enabled;
        }
    }
}
