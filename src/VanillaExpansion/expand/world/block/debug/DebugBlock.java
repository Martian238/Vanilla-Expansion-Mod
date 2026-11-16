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

    // 构造调试方块
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

    // 添加默认模块
    public DebugBlock addDefaultModule(String id) {
        if (id != null && !id.isEmpty() && !defaultModuleIds.contains(id)) defaultModuleIds.add(id);
        return this;
    }

    // 移除默认模块
    public DebugBlock removeDefaultModule(String id) {
        defaultModuleIds.remove(id);
        return this;
    }

    public class DebugBuild extends Building {
        public final Seq<ModuleSlot> moduleSlots = new Seq<>();
        protected boolean modulePlaced = false;
        // 模块贡献的每 tick 电力生产量
        public float modulePowerProduction = 0f;

        // 返回模块贡献的电力生产量供电网汇总
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

        // 创建建筑时安装默认模块
        @Override
        public void created() {
            super.created();
            installDefaultModules();
        }

        // 更新建筑与已启用模块
        @Override
        public void updateTile() {
            super.updateTile();
            if (!enabled) return;
            for (int i = 0; i < moduleSlots.size; i++) {
                ModuleSlot slot = moduleSlots.get(i);
                if (slot.enabled) slot.module.update(this);
            }
        }

        // 绘制建筑与已启用模块
        @Override
        public void draw() {
            super.draw();
            for (int i = 0; i < moduleSlots.size; i++) {
                ModuleSlot slot = moduleSlots.get(i);
                if (slot.enabled) slot.module.draw(this);
            }
        }

        // 显示各模块的状态栏
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

        // 构建单个模块的状态栏区域
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

        // 鼠标悬停时绘制各模块的范围指示
        @Override
        public void drawSelect() {
            super.drawSelect();
            if (Vars.headless) return;
            for (int i = 0; i < moduleSlots.size; i++) {
                ModuleSlot slot = moduleSlots.get(i);
                if (slot.enabled) slot.module.drawSelect(this);
            }
        }

        // 建筑放置后通知已启用模块
        @Override
        public void placed() {
            super.placed();
            modulePlaced = true;
            for (int i = 0; i < moduleSlots.size; i++) {
                ModuleSlot slot = moduleSlots.get(i);
                if (slot.enabled) slot.module.placed(this);
            }
        }

        // 打开模块管理界面
        @Override
        public void buildConfiguration(Table table) {
            if (Vars.headless || !isValid()) return;
            deselect();
            new DebugModuleDialog(this).show();
        }

        // 返回可复制的模块配置
        @Override
        public Object config() {
            return encodeConfig();
        }

        // 建筑移除时通知并卸载全部模块
        @Override
        public void onRemoved() {
            modulePlaced = false;
            clearModules();
            super.onRemoved();
        }

        // 建筑摧毁时通知已启用模块
        @Override
        public void onDestroyed() {
            super.onDestroyed();
            for (int i = 0; i < moduleSlots.size; i++) {
                ModuleSlot slot = moduleSlots.get(i);
                if (slot.enabled) slot.module.destroyed(this);
            }
        }

        // 邻接关系更新时通知已启用模块
        @Override
        public void onProximityUpdate() {
            super.onProximityUpdate();
            for (int i = 0; i < moduleSlots.size; i++) {
                ModuleSlot slot = moduleSlots.get(i);
                if (slot.enabled) slot.module.proximityUpdated(this);
            }
        }

        // 返回存档版本
        @Override
        public byte version() {
            return SAVE_VERSION;
        }

        // 写入模块列表与模块数据
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

        // 读取模块列表与模块数据
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

        // 安装方块声明的默认模块
        protected void installDefaultModules() {
            for (int i = 0; i < defaultModuleIds.size; i++) {
                String id = defaultModuleIds.get(i);
                if (getSlot(id) == null) addModule(id);
            }
        }

        // 处理模块管理指令
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

        // 将配置值分发给已启用模块
        protected boolean configureModules(Object value) {
            for (int i = 0; i < moduleSlots.size; i++) {
                ModuleSlot slot = moduleSlots.get(i);
                if (slot.enabled && slot.module.configure(this, value)) return true;
            }
            return false;
        }

        // 应用编码后的模块配置
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

        // 按标识添加模块
        public DebugModule addModule(String id) {
            DebugModule module = DebugModuleRegistry.create(id);
            if (module == null || !addModule(module)) return null;
            return module;
        }

        // 添加模块实例
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

        // 按标识移除模块
        public boolean removeModule(String id) {
            ModuleSlot slot = getSlot(id);
            if (slot == null) return false;
            moduleSlots.remove(slot);
            if (slot.enabled) slot.module.removed(this);
            slot.module.detach(this);
            return true;
        }

        // 清空全部模块
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

        // 启用或禁用指定模块
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

        // 获取指定模块槽位
        public ModuleSlot getSlot(String id) {
            if (id == null) return null;
            for (int i = 0; i < moduleSlots.size; i++) {
                ModuleSlot slot = moduleSlots.get(i);
                if (slot.id.equals(id)) return slot;
            }
            return null;
        }

        // 获取指定模块实例
        public DebugModule getModule(String id) {
            ModuleSlot slot = getSlot(id);
            return slot == null ? null : slot.module;
        }

        // 判断是否已添加指定模块
        public boolean hasModule(String id) {
            return getSlot(id) != null;
        }

        // 序列化单个模块数据
        protected byte[] writeModuleData(DebugModule module) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Writes writes = new Writes(new DataOutputStream(output));
            module.write(this, writes);
            writes.close();
            return output.toByteArray();
        }

        // 编码全部模块配置用于复制
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

        // 反序列化单个模块数据
        protected void readModuleData(DebugModule module, byte[] data, byte revision) {
            if (data.length == 0) return;
            module.read(this, new Reads(new DataInputStream(new ByteArrayInputStream(data))), revision);
        }
    }

    public static class ModuleSlot {
        public final String id;
        public final DebugModule module;
        public boolean enabled;

        // 构造模块槽位
        public ModuleSlot(String id, DebugModule module, boolean enabled) {
            this.id = id;
            this.module = module;
            this.enabled = enabled;
        }
    }
}
