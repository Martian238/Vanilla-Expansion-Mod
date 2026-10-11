package VanillaExpansion.ui.bossrush;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.scene.Group;
import arc.scene.event.ElementGestureListener;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Log;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import VanillaExpansion.content.VEPlanets;
import VanillaExpansion.ui.bossrush.card.*;

public class BossRushView extends Table{
    public final BossRushData data = new BossRushData();
    public final BossRushLayout layout = new BossRushLayout();

    public View view;

    public String hoverTitle;
    public String hoverDesc;
    public Table infoTable = new Table();

    public static final float INFO_SCALE = 0.7f;

    public BossRushView(){
        super();

        data.load();
        data.loadProgress();
        data.relocalize();
        data.bindMaps();
        if(data.root != null){
            layout.layout(data.root);
            for(BossRushNode n : data.nodes){
                //Log.info("BossRushView: node @ pos=@,@", n.id, n.x, n.y);
            }
        }
        BossRushRegistry.data = data;

        setBackground(null);
        setFillParent(true);
        touchable = Touchable.enabled;
        addListener(new InputListener(){
            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY){
                view.setScale(Mathf.clamp(view.scaleX - amountY / 10f * view.scaleX, 0.25f, 2f));
                view.setOrigin(Align.center);
                view.setTransform(true);
                return true;
            }

            @Override
            public boolean mouseMoved(InputEvent event, float x, float y){
                view.requestScroll();
                return super.mouseMoved(event, x, y);
            }
        });
        addCaptureListener(new ElementGestureListener(){
            @Override
            public void zoom(InputEvent event, float initialDistance, float distance){
                if(view.lastZoom < 0){
                    view.lastZoom = view.scaleX;
                }
                view.setScale(Mathf.clamp(distance / initialDistance * view.lastZoom, 0.25f, 2f));
                view.setOrigin(Align.center);
                view.setTransform(true);
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                view.lastZoom = view.scaleX;
            }

            @Override
            public void pan(InputEvent event, float x, float y, float deltaX, float deltaY){
                view.panX += deltaX / view.scaleX;
                view.panY += deltaY / view.scaleY;
                view.moved = true;
                view.clamp();
            }
        });

        Table inner = new Table();
        inner.setBackground(Styles.black6);
        inner.touchable = Touchable.enabled;
        inner.stack(view = new View()).grow();
        Table innerContainer = new Table();
        innerContainer.touchable = Touchable.childrenOnly;
        innerContainer.right().bottom();
        innerContainer.add(inner).grow()
                .maxWidth(Core.graphics.getWidth() * 0.7f)
                .maxHeight(Core.graphics.getHeight() * 0.7f)
                .padLeft(Core.graphics.getWidth() * 0.3f)
                .padTop(Core.graphics.getHeight() * 0.1f)
                .padBottom(60f);
        Table backBar = new Table();
        backBar.bottom().left();
        backBar.touchable = Touchable.childrenOnly;
        backBar.button("@back", Icon.left, () -> BossRushRegistry.returnToMenu())
                .size(200f, 54f).pad(2).bottom();
        Table clearBar = new Table();
        clearBar.bottom().right();
        clearBar.touchable = Touchable.childrenOnly;
        clearBar.button(Icon.trash, Styles.clearNonei, () -> {
            Vars.ui.showConfirm("@confirm", "@save.delete.confirm", () -> {
                data.clearProgress();
                data.bindMaps();
                rebuild();
                Vars.ui.showInfoFade("@done");
            });
        }).size(50f).pad(8f).tooltip("@save.delete");
        Table stackLayer = new Table();
        stackLayer.setFillParent(true);
        stackLayer.touchable = Touchable.childrenOnly;
        stackLayer.stack(innerContainer, backBar, clearBar).grow();

        add(stackLayer).grow();
        infoTable.touchable = Touchable.disabled;
        infoTable.visible(() -> hoverTitle != null);
        infoTable.update(() -> {
            attachInfoTable();
            if(hoverTitle != null){
                float scale = infoTable.scaleX;
                float w = infoTable.getWidth() * scale;
                float h = infoTable.getHeight() * scale;

                float mx = Core.input.mouseX() + 12f;
                float my = Core.input.mouseY() + 12f;
                float maxX = Core.graphics.getWidth() - w - 4f;
                float maxY = Core.graphics.getHeight() - h - 4f;

                infoTable.setPosition(
                        Math.min(mx, Math.max(4f, maxX)),
                        Math.min(my, Math.max(4f, maxY)),
                        Align.bottomLeft
                );
                infoTable.toFront();
            }
        });

        Core.app.post(() -> {
            view.rebuildAll();
            attachInfoTable();
        });
    }

    private void attachInfoTable(){
        if(infoTable.parent == null && Core.scene != null && Core.scene.root != null){
            Core.scene.root.addChild(infoTable);
        }
    }

    public void setHover(String title, String desc){
        this.hoverTitle = title;
        this.hoverDesc = desc;
        rebuildInfo();
    }

    public void clearHover(){
        this.hoverTitle = null;
        this.hoverDesc = null;
        rebuildInfo();
    }

    public void rebuildInfo(){
        infoTable.clear();
        if(hoverTitle == null) return;

        infoTable.background(Styles.black6);
        infoTable.margin(4f);

        Label title = new Label(hoverTitle);
        title.setColor(Color.white);
        infoTable.add(title).left().row();

        if(hoverDesc != null && !hoverDesc.isEmpty()){
            Label desc = new Label(hoverDesc);
            desc.setWrap(true);
            desc.setColor(Color.lightGray);
            infoTable.add(desc).maxWidth(180f).left().padTop(2f).row();
        }

        infoTable.pack();
        infoTable.setTransform(true);
        infoTable.setOrigin(Align.bottomLeft);
        infoTable.setScale(INFO_SCALE);
    }

    public void rebuild(){
        if(view != null) view.rebuildAll();
    }
    public class View extends Group{
        public float panX = 0, panY = 0, lastZoom = -1;
        public boolean moved = false;

        {
            setOrigin(Align.center);
            setTransform(true);
            released(() -> moved = false);
            touchable = Touchable.childrenOnly;
        }

        public void rebuildAll(){
            clearChildren();
            if(data.root == null) return;

            for(BossRushNode node : data.nodes){
                BossRushCardWidget card = node.type == BossRushCardType.LEVEL
                        ? new LevelCardWidget(node, BossRushView.this)
                        : new GalleryCardWidget(node, BossRushView.this);

                card.setSize(BossRushCardWidget.CARD_WIDTH, BossRushCardWidget.CARD_HEIGHT);
                card.update(() -> {
                    card.setPosition(
                            node.x + panX + width / 2f,
                            node.y + panY + height / 2f,
                            Align.center
                    );
                    node.pos.set(
                            node.x + panX + width / 2f,
                            node.y + panY + height / 2f,
                            0f
                    );
                });

                addChild(card);
            }
        }

        void clamp(){
            float pad = 100f;
            float ox = width / 2f, oy = height / 2f;
            float rx = layout.bounds.x + panX + ox;
            float ry = panY + oy + layout.bounds.y;
            float rw = layout.bounds.width, rh = layout.bounds.height;
            rx = Mathf.clamp(rx, -rw + pad, Core.graphics.getWidth() - pad);
            ry = Mathf.clamp(ry, -rh + pad, Core.graphics.getHeight() - pad);
            panX = rx - layout.bounds.x - ox;
            panY = ry - layout.bounds.y - oy;
        }

        @Override
        public void drawChildren(){
            clamp();

            float offsetX = panX + width / 2f;
            float offsetY = panY + height / 2f;

            Draw.sort(true);

            for(BossRushNode node : data.nodes){
                for(BossRushNode child : node.children){
                    Color from = node.completed ? Pal.accent : Pal.gray;
                    Color to = child.completed ? Pal.accent : Pal.gray;

                    float x1 = node.x + offsetX;
                    float y1 = node.y + offsetY;
                    float x2 = child.x + offsetX;
                    float y2 = child.y + offsetY;

                    float midY = (y1 + y2) / 2f;

                    Lines.stroke(8f);

                    Draw.color(from);
                    Lines.line(x1, y1, x1, midY);
                    Lines.line(x1, midY, x2, midY);

                    Draw.color(to);
                    Lines.line(x2, midY, x2, y2);
                }
            }

            Lines.stroke(1f);
            Draw.reset();
            Draw.sort(false);

            super.drawChildren();
        }
    }
}