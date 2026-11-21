// AchievementRegistry.java
package VanillaExpansion.expand.ui;

import arc.Core;
import arc.scene.style.Drawable;

import java.util.ArrayList;
import java.util.List;

public class AchievementRegistry {
    // 所有已注册成就
    private static final List<Entry> ENTRIES = new ArrayList<>();
    // 存档前缀
    private static final String SAVE_PREFIX = "ve-achievement-";

    // 注册成就（key 用于本地化，id 用于存档）
    public static void register(String id, String nameKey, String descKey, Drawable icon, int rarity) {
        if (find(id) != null) return;
        Entry e = new Entry(id, nameKey, descKey, icon, rarity);
        ENTRIES.add(e);
        AchievementArchive.register(id, nameKey, descKey, icon, rarity);
    }

    // 注册成就（使用 id 推导本地化 key）
    public static void register(String id, Drawable icon, int rarity) {
        register(id, "achievement." + id + ".name", "achievement." + id + ".desc", icon, rarity);
    }

    // 注册后立即尝试解锁（用于启动即得成就）
    public static void registerAndUnlock(String id, String nameKey, String descKey, Drawable icon, int rarity) {
        register(id, nameKey, descKey, icon, rarity);
        unlock(id);
    }

    // 解锁成就
    public static void unlock(String id) {
        Entry e = find(id);
        if (e == null) return;
        if (isUnlocked(id)) return;
        Core.settings.put(SAVE_PREFIX + id, true);
        AchievementUI.add(
                Core.bundle.get(e.nameKey, e.nameKey),
                Core.bundle.get(e.descKey, e.descKey),
                e.icon,
                e.rarity
        );
    }

    // 是否已解锁
    public static boolean isUnlocked(String id) {
        return Core.settings.getBool(SAVE_PREFIX + id, false);
    }

    // 查找成就
    public static Entry find(String id) {
        for (Entry e : ENTRIES) if (e.id.equals(id)) return e;
        return null;
    }

    public static void clearAll() {
        for (Entry e : ENTRIES) {
            Core.settings.remove(SAVE_PREFIX + e.id);
        }
    }

    // 全部成就
    public static List<Entry> all() {
        return ENTRIES;
    }

    public static class Entry {
        // 唯一标识
        public final String id;
        // 名称语言键
        public final String nameKey;
        // 描述语言键
        public final String descKey;
        // 图标
        public final Drawable icon;
        // 稀有度
        public final int rarity;

        // 构造
        public Entry(String id, String nameKey, String descKey, Drawable icon, int rarity) {
            this.id = id;
            this.nameKey = nameKey;
            this.descKey = descKey;
            this.icon = icon;
            this.rarity = rarity;
        }
    }
}