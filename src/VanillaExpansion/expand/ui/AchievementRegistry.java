package VanillaExpansion.expand.ui;
import arc.Core;
import arc.scene.style.Drawable;
import java.util.ArrayList;
import java.util.List;
public class AchievementRegistry {
    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final String SAVE_PREFIX = "ve-achievement-";
    public static void register(String id, String nameKey, String descKey, Drawable icon, int rarity, int maxLevel, String levelKey) {
        if (find(id) != null) return;
        Entry e = new Entry(id, nameKey, descKey, icon, rarity, maxLevel, levelKey);
        ENTRIES.add(e);
        AchievementArchive.register(id, nameKey, descKey, icon, rarity, maxLevel, levelKey);
    }
    public static void register(String id, Drawable icon, int rarity, int maxLevel) {
        register(id, "achievement." + id + ".name", "achievement." + id + ".desc", icon, rarity, maxLevel, "achievement." + id + ".level");
    }
    //启动即得成就
    /*
    public static void registerAndUnlock(String id, String nameKey, String descKey, Drawable icon, int rarity) {
        register(id, nameKey, descKey, icon, rarity);
        unlock(id, 1);
    }
     */
    public static void unlock(String id, int level) {
        Entry e = find(id);
        if (e == null) return;
        if (isUnlocked(id)) return;
        Core.settings.put(SAVE_PREFIX + id, level);
        AchievementUI.add(
                id,
                Core.bundle.get(e.nameKey, e.nameKey),
                e.icon,
                e.rarity,
                e.maxLevel,
                getLevel(id)
        );
    }
    public static boolean isUnlocked(String id) {
        return getLevel(id) > 0;
    }
    public static int getLevel(String id) {
        return Core.settings.getInt(SAVE_PREFIX + id, 0);
    }
    public static Entry find(String id) {
        for (Entry e : ENTRIES) if (e.id.equals(id)) return e;
        return null;
    }
    public static void clearAll() {
        for (Entry e : ENTRIES) {
            Core.settings.remove(SAVE_PREFIX + e.id);
        }
    }
    public static List<Entry> all() {
        return ENTRIES;
    }
    public static class Entry {
        public final String id;
        public final String nameKey;
        public final String descKey;
        public final Drawable icon;
        public final int rarity;
        public final int maxLevel;
        public final String levelKey;
        public Entry(String id, String nameKey, String descKey, Drawable icon, int rarity, int maxLevel, String levelKey) {
            this.id = id;
            this.nameKey = nameKey;
            this.descKey = descKey;
            this.icon = icon;
            this.rarity = rarity;
            this.maxLevel = maxLevel;
            this.levelKey = levelKey;
        }
    }
}