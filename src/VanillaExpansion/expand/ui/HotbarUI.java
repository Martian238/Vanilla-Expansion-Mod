package VanillaExpansion.expand.ui;
import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.ui.layout.Table;
public class HotbarUI {
    private mindustry.gen.Unit cachedUnit = null;
    private VanillaExpansion.expand.abilities.HotbarAbility cachedAbility = null;
    public static HotbarUI instance;
    public static final float SLOT_SIZE = 48f;
    public static final float SLOT_GAP = 4f;
    public static final float BORDER = 3f;
    public static final int SLOT_COUNT = 7;
    public static final Color SELECT_COLOR = Color.valueOf("ffff55");
    public static final Color SLOT_BG = Color.valueOf("00000088");
    public static final Color SLOT_BORDER = Color.valueOf("8b8b8b");
    private final mindustry.type.Item[] items = new mindustry.type.Item[SLOT_COUNT];
    private int selected = 0;
    //右偏移
    private final float shiftright = 28;
    private Table root;
    private boolean visible = true;
    private final arc.math.geom.Vec2 tmpVec = new arc.math.geom.Vec2();
    private boolean initialized = false;
    private boolean dragging = false;
    private float dragOffsetX, dragOffsetY;
    private arc.scene.event.InputListener globalListener;
    private final Vec2 tmp = new Vec2();
    public void init() {
        instance = this;
        if (initialized) return;
        initialized = true;
        root = new Table();
        root.setFillParent(false);
        placeAtLeftCenter();
        for (int i = 0; i < SLOT_COUNT; i++) {
            final int index = i;
            Slot slot = new Slot(index);
            root.add(slot).size(SLOT_SIZE).pad(SLOT_GAP / 2f);
            root.row();
        }
        installDrag(root);
        installGlobalInput();
        Core.scene.add(root);
        root.visible = visible;
        updateVisibility();
        int slotIndex = 0;
        for (mindustry.type.Item item : mindustry.Vars.content.items()) {
            if (slotIndex >= SLOT_COUNT) break;
            if (item instanceof VanillaExpansion.content.CustomItem) {
                items[slotIndex] = item;
                slotIndex++;
            }
        }
    }
    public void dispose() {
        if (root != null) {
            root.remove();
            root = null;
        }
        if (globalListener != null) {
            Core.scene.removeListener(globalListener);
            globalListener = null;
        }
        initialized = false;
    }
    // 显示时机
    protected boolean shouldShow() {
        // 游戏内、非菜单、有被控单位
        if (mindustry.Vars.state == null
                || !mindustry.Vars.state.isGame()
                || mindustry.Vars.state.isMenu()
                || mindustry.Vars.player == null
                || mindustry.Vars.player.unit() == null) {
            return false;
        }
        // 有 HotbarAbility
        return findHotbarAbility() != null;
    }
    public void updateVisibility() {
        if (root == null) return;
        boolean show = visible && shouldShow();
        if (root.visible != show) root.visible = show;
    }
    public void setVisible(boolean v) {
        visible = v;
        if (root != null) root.visible = v && shouldShow();
    }
    public boolean isVisible() {
        return root != null && root.visible;
    }
    // 位置
    public void placeAtLeftCenter() {
        if (root == null) return;
        root.pack();
        float w = root.getWidth();
        float h = root.getHeight();
        float screenW = Core.graphics.getWidth();
        float screenH = Core.graphics.getHeight();
        float targetX = shiftright - w / 2f;
        float targetY = (screenH - h) / 2f;
        root.setPosition(targetX, targetY);
    }
    public float getX() { return root == null ? 0 : root.x; }
    public float getY() { return root == null ? 0 : root.y; }
    //设置某格的物品，弃用方法
    public void setItem(int slot, mindustry.type.Item item) {
        if (slot < 0 || slot >= SLOT_COUNT) return;
        items[slot] = item;
    }
    //获取某格的物品
    public mindustry.type.Item getItem(int slot) {
        return (slot < 0 || slot >= SLOT_COUNT) ? null : items[slot];
    }
    public int getSelected() { return selected; }
    public void setSelected(int index) {
        selected = Mathf.clamp(index, 0, SLOT_COUNT - 1);
    }
    private void installGlobalInput() {
        globalListener = new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, KeyCode keycode) {
                if (!isVisible()) return false;
                int idx = mapNumberKey(keycode);
                if (idx >= 0) {
                    setSelected(idx);
                    return true;
                }
                return false;
            }
            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
                if (!isVisible() || root == null) return false;
                float mouseX = Core.input.mouseX();
                float mouseY = Core.input.mouseY();
                tmpVec.set(mouseX, mouseY);
                root.stageToLocalCoordinates(tmpVec);
                if (tmpVec.x >= 0 && tmpVec.x <= root.getWidth() &&
                        tmpVec.y >= 0 && tmpVec.y <= root.getHeight()) {
                    if (amountY != 0) {
                        int dir = amountY > 0 ? -1 : 1;
                        setSelected((selected + dir + SLOT_COUNT) % SLOT_COUNT);
                        event.stop();
                        return true;
                    }
                }
                return false;
            }
        };

        Core.scene.addListener(globalListener);
    }
    private int mapNumberKey(KeyCode k) {
        switch (k) {
            case num1: case numpad1: return 0;
            case num2: case numpad2: return 1;
            case num3: case numpad3: return 2;
            case num4: case numpad4: return 3;
            case num5: case numpad5: return 4;
            case num6: case numpad6: return 5;
            case num7: case numpad7: return 6;
            default: return -1;
        }
    }
    private void installDrag(Element target) {
        target.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                if (button != KeyCode.mouseLeft) return false;
                dragging = true;
                dragOffsetX = x;
                dragOffsetY = y;
                event.stop();
                return true;
            }
            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                if (!dragging) return;
                Element e = event.listenerActor;
                if (e == null) return;
                e.localToStageCoordinates(tmp.set(x, y));
                root.setPosition(tmp.x - dragOffsetX, tmp.y - dragOffsetY);
                clampToScreen();
                event.stop();
            }
            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
                dragging = false;
            }
        });
    }
    private void clampToScreen() {
        if (root == null) return;
        float w = root.getWidth(), h = root.getHeight();
        float x = Mathf.clamp(root.x, 0, Math.max(0, Core.graphics.getWidth() - w));
        float y = Mathf.clamp(root.y, 0, Math.max(0, Core.graphics.getHeight() - h));
        root.setPosition(x, y);
    }
    private class Slot extends Element {
        final int index;
        Slot(int index) {
            this.index = index;
            addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                    if (button == KeyCode.mouseLeft) {
                        setSelected(index);
                    }
                    return false;
                }
            });
        }

        @Override
        public void draw() {
            float cx = x + width / 2f;
            float cy = y + height / 2f;
            float s = SLOT_SIZE;
            float half = s / 2f;
            TextureRegion white = Core.atlas.white();
            Draw.color(SLOT_BG);
            Draw.rect(white, cx, cy, s, s);
            Draw.color(SLOT_BORDER);
            float t = 1f;
            Draw.rect(white, cx, cy + half - t / 2f, s, t); // 上
            Draw.rect(white, cx, cy - half + t / 2f, s, t); // 下
            Draw.rect(white, cx - half + t / 2f, cy, t, s); // 左
            Draw.rect(white, cx + half - t / 2f, cy, t, s); // 右
            mindustry.type.Item item = items[index];
            if (item != null) {
                Draw.color(Color.white);
                float pad = 6f;
                float iconSize = s - pad * 2;
                Draw.rect(item.uiIcon, cx, cy, iconSize, iconSize);
            }
            if (index == selected) {
                Draw.color(SELECT_COLOR);
                float bt = BORDER;
                Draw.rect(white, cx, cy + half - bt / 2f, s, bt); // 上
                Draw.rect(white, cx, cy - half + bt / 2f, s, bt); // 下
                Draw.rect(white, cx - half + bt / 2f, cy, bt, s); // 左
                Draw.rect(white, cx + half - bt / 2f, cy, bt, s); // 右
            }
            Draw.color();
        }
    }
    //找HotbarAbility
    private VanillaExpansion.expand.abilities.HotbarAbility findHotbarAbility() {
        mindustry.gen.Unit unit = mindustry.Vars.player != null
                ? mindustry.Vars.player.unit() : null;
        if (unit != cachedUnit) {
            cachedUnit = unit;
            cachedAbility = null;
            if (unit != null) {
                for (int i = 0; i < unit.abilities.length; i++) {
                    var ab = unit.abilities[i];
                    if (ab instanceof VanillaExpansion.expand.abilities.HotbarAbility h) {
                        cachedAbility = h;
                        break;
                    }
                }
            }
        }
        return cachedAbility;
    }
}