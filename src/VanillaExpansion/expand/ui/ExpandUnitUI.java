package VanillaExpansion.expand.ui;

import arc.scene.ui.layout.Table;

import static mindustry.Vars.*;

/**
 * 可扩展单位物品栏UI - 简化版
 * 现在主要通过设置菜单和对话框管理
 */
public class ExpandUnitUI {
    private static boolean visible = true;

    public static void build() {
        // 不再需要构建独立的UI
        // 所有交互通过设置菜单和对话框完成
    }

    public static void update() {
        // 不再需要每帧更新UI
        // 物品栏状态由 ExpandUnitCore 管理
    }

    public static void show() {
        visible = true;
    }

    public static void hide() {
        visible = false;
    }

    public static boolean isVisible() {
        return visible;
    }

    public static boolean isBuilt() {
        return true;
    }
}