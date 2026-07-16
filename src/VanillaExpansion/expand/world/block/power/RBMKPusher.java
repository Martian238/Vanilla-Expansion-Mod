package VanillaExpansion.expand.world.block.power;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import arc.math.geom.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.meta.*;
import mindustry.graphics.g3d.*;
import mindustry.graphics.*;

import static mindustry.Vars.*;

/**
 * RBMK推送器
 * 2x2方块，用于在RBMK反应堆系统中推送物品和流体
 */
public class RBMKPusher extends RBMKBase {
    public static final float range = 400f;
    public static final float warmupSpeed = 0.05f;
    
    public TextureRegion topRegion;
    public TextureRegion bottomRegion;
    public TextureRegion rotatorRegion;
    
    public RBMKPusher(String name){
        super(name);
        size = 4;
        update = true;
        solid = true;
        hasItems = true;
        hasLiquids = true;
        configurable = true;
        saveConfig = false;
        itemCapacity = 500;
        liquidCapacity = 1000f;
        noUpdateDisabled = true;
        
        requirements(Category.power, ItemStack.with(
            Items.copper, 200,
            Items.lead, 150,
            Items.titanium, 100,
            Items.silicon, 50
        ));
    }
    
    @Override
    public void load() {
        super.load();
        topRegion = Core.atlas.find(name + "-top");
        bottomRegion = Core.atlas.find(name + "-bottom");
        rotatorRegion = Core.atlas.find(name + "-rotator");
    }
    
    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid) {
        super.drawPlace(x, y, rotation, valid);
        Drawf.dashCircle(x * tilesize, y * tilesize, range, Pal.accent);
    }
    
    @Override
    public void setBars() {
        super.setBars();
        addBar("capacity", (RBMKPusherBuild entity) -> new Bar(
            () -> Core.bundle.get("bar.capacity") + ": " + entity.items.total() + "/" + itemCapacity,
            () -> Pal.items,
            () -> (float)entity.items.total() / itemCapacity
        ));
        addBar("liquid", (RBMKPusherBuild entity) -> new Bar(
            () -> Core.bundle.get("bar.liquid") + ": " + entity.liquids.currentAmount() + "/" + liquidCapacity,
            () -> entity.liquids.current() == null ? Pal.gray : entity.liquids.current().color,
            () -> entity.liquids.currentAmount() / liquidCapacity
        ));
    }
    
    @Override
    public boolean outputsItems() {
        return false;
    }
    
    public class RBMKPusherBuild extends RBMKBaseBuild {
        public Seq<Integer> links = new Seq<>();
        public Seq<Integer> deadLinks = new Seq<>();
        public ObjectMap<Integer, Boolean> linkModes = new ObjectMap<>(); // 存储每个连接的模式，true为提取模式，false为推送模式
        public ObjectMap<Integer, Integer> clickCounts = new ObjectMap<>(); // 存储每个连接的点击次数
        public float warmup = 0f;
        public float rotateDeg = 0f;
        public float rotateSpeed = 0f;
        public boolean consValid = false;
        public boolean itemSent = false;
        public boolean liquidSent = false;
        public Interval timer = new Interval(6);
        public static final int MAX_LOOP = 50;
        public static final int FRAME_DELAY = 5;
        
        public int loopIndex = 0;
        
        public Seq<Integer> getLink() {
            return links;
        }
        
        public void setLink(Seq<Integer> v) {
            links = v;
            for (int i = links.size - 1; i >= 0; i--) {
                int link = links.get(i);
                Building linkTarget = world.build(link);
                if (!linkValidTarget(this, linkTarget)) {
                    links.remove(i);
                } else {
                    links.set(i, linkTarget.pos());
                }
            }
        }
        
        public void setOneLink(int v) {
            Seq<Integer> newLinks = new Seq<>();
            for (int i = 0; i < links.size; i++) {
                int link = links.get(i);
                if (link != v) {
                    newLinks.add(link);
                }
            }
            if (newLinks.size == links.size) {
                links.add(v);
                linkModes.put(v, false); // 新连接默认设置为推送模式
                clickCounts.put(v, 0); // 初始化点击计数为0
                linkItemFilters.put(v, new Seq<>()); // 初始化物品过滤器
                linkLiquidFilters.put(v, new Seq<>()); // 初始化流体过滤器
            } else {
                links.clear();
                links.addAll(newLinks);
                // 移除被删除连接的设置
                linkModes.remove(v);
                clickCounts.remove(v);
                linkItemFilters.remove(v); // 移除物品过滤器
                linkLiquidFilters.remove(v); // 移除流体过滤器
            }
        }
        
        public void deadLink(int v) {
            if (net.client()) return;
            if (links.contains(v)) {
                configure(v);
            }
            deadLinks.add(v);
            // 移除失效连接的设置
            linkModes.remove(v);
            clickCounts.remove(v);
            linkItemFilters.remove(v); // 移除物品过滤器
            linkLiquidFilters.remove(v); // 移除流体过滤器
            if (deadLinks.size >= 50) {
                deadLinks.removeRange(0, 25);
            }
        }
        
        public void tryResumeDeadLink(int v) {
            if (net.client()) return;
            if (!deadLinks.contains(v)) return;
            deadLinks.remove(v);
            Building linkTarget = world.build(v);
            if (linkValid(this, v)) {
                configure(linkTarget.pos());
            }
        }
        
        public boolean linkValidTarget(Building the, Building target) {
            return target != null && target.team == the.team && the.within(target, range);
        }
        
        public boolean linkValid(Building the, int pos) {
            if (pos == -1) return false;
            Building linkTarget = world.build(pos);
            return linkValidTarget(the, linkTarget);
        }
        
        public boolean sendItems(Building target) {
            boolean sent = false;
            int pos = target.pos();
            Seq<Item> filter = linkItemFilters.get(pos, new Seq<>());
            
            if (filter.isEmpty()) {
                // 如果没有设置过滤器，处理所有物品
                for (Item item : content.items()) {
                    int count = items.get(item);
                    if (count > 0) {
                        int accept = Math.min(count, target.acceptStack(item, Math.min(count, FRAME_DELAY * 32), this));
                        if (accept > 0) {
                            sent = true;
                            target.handleStack(item, accept, this);
                            items.remove(item, accept);
                        }
                    }
                }
            } else {
                // 只处理过滤器中的物品
                for (Item item : filter) {
                    int count = items.get(item);
                    if (count > 0) {
                        int accept = Math.min(count, target.acceptStack(item, Math.min(count, FRAME_DELAY * 32), this));
                        if (accept > 0) {
                            sent = true;
                            target.handleStack(item, accept, this);
                            items.remove(item, accept);
                        }
                    }
                }
            }
            return sent;
        }
        
        public boolean sendLiquids(Building target) {
            boolean sent = false;
            // 检查流体容量，如果等于0则停止推送
            if (liquids != null && liquids.currentAmount() <= 0) {
                return sent;
            }
            
            // 检查目标方块的流体容量，如果等于容量则停止推送
            if (target.liquids != null && target.liquids.currentAmount() >= target.block.liquidCapacity) {
                return sent;
            }
            
            int pos = target.pos();
            Seq<Liquid> filter = linkLiquidFilters.get(pos, new Seq<>());
            
            if (filter.isEmpty()) {
                // 如果没有设置过滤器，处理当前液体
                Liquid liquid = liquids.current();
                if (liquid != null) {
                    float amount = liquids.get(liquid);
                    if (amount > 0) {
                        float accept = Math.min(amount, 6f * 32);
                        if (accept > 0 && target.acceptLiquid(this, liquid)) {
                            sent = true;
                            target.liquids.add(liquid, accept);
                            liquids.remove(liquid, accept);
                        }
                    }
                }
            } else {
                // 只处理过滤器中的液体
                for (Liquid liquid : filter) {
                    float amount = liquids.get(liquid);
                    if (amount > 0) {
                        float accept = Math.min(amount, 6f * 32);
                        if (accept > 0 && target.acceptLiquid(this, liquid)) {
                            sent = true;
                            target.liquids.add(liquid, accept);
                            liquids.remove(liquid, accept);
                        }
                    }
                }
            }
            return sent;
        }
        
        public boolean extractItems(Building target) {
            boolean extracted = false;
            if (target.items != null) {
                int pos = target.pos();
                Seq<Item> filter = linkItemFilters.get(pos, new Seq<>());
                
                if (filter.isEmpty()) {
                    // 如果没有设置过滤器，提取所有物品
                    for (Item item : content.items()) {
                        int count = target.items.get(item);
                        if (count > 0) {
                            int accept = Math.min(count, acceptStack(item, Math.min(count, FRAME_DELAY * 32), target));
                            if (accept > 0) {
                                extracted = true;
                                handleStack(item, accept, target);
                                target.removeStack(item, accept);
                            }
                        }
                    }
                } else {
                    // 只提取过滤器中的物品
                    for (Item item : filter) {
                        int count = target.items.get(item);
                        if (count > 0) {
                            int accept = Math.min(count, acceptStack(item, Math.min(count, FRAME_DELAY * 32), target));
                            if (accept > 0) {
                                extracted = true;
                                handleStack(item, accept, target);
                                target.removeStack(item, accept);
                            }
                        }
                    }
                }
            }
            return extracted;
        }
        
        public boolean extractLiquids(Building target) {
            boolean extracted = false;
            // 检查流体容量，如果等于容量则停止抽取
            if (liquids != null && liquids.currentAmount() >= liquidCapacity) {
                return extracted;
            }
            
            if (target.liquids != null) {
                int pos = target.pos();
                Seq<Liquid> filter = linkLiquidFilters.get(pos, new Seq<>());
                
                if (filter.isEmpty()) {
                    // 如果没有设置过滤器，提取当前液体
                    Liquid liquid = target.liquids.current();
                    if (liquid != null) {
                        float amount = target.liquids.get(liquid);
                        if (amount > 0) {
                            float accept = Math.min(amount, 6f * 32);
                            if (accept > 0 && acceptLiquid(this, liquid)) {
                                extracted = true;
                                liquids.add(liquid, accept);
                                target.liquids.remove(liquid, accept);
                            }
                        }
                    }
                } else {
                    // 只提取过滤器中的液体
                    for (Liquid liquid : filter) {
                        float amount = target.liquids.get(liquid);
                        if (amount > 0) {
                            float accept = Math.min(amount, 6f * 32);
                            if (accept > 0 && acceptLiquid(this, liquid)) {
                                extracted = true;
                                liquids.add(liquid, accept);
                                target.liquids.remove(liquid, accept);
                            }
                        }
                    }
                }
            }
            return extracted;
        }
        
        @Override
        public void updateTile() {
            super.updateTile();
            
            if (timer.get(1, FRAME_DELAY)) {
                itemSent = false;
                liquidSent = false;
                consValid = efficiency > 0;
                if (consValid) {
                    consume();
                    
                    int max = links.size;
                    for (int i = 0; i < Math.min(MAX_LOOP, max); i++) {
                        loopIndex--;
                        if (loopIndex < 0) {
                            loopIndex = max - 1;
                        }
                        int index = loopIndex;
                        if (index >= links.size) continue;
                        
                        int pos = links.get(index);
                        if (pos == -1) {
                            configure(pos);
                            continue;
                        }
                        
                        Building linkTarget = world.build(pos);
                        if (!linkValidTarget(this, linkTarget)) {
                            deadLink(pos);
                            max--;
                            if (max <= 0) {
                                break;
                            }
                            continue;
                        }
                        
                        // 根据每个连接的模式来决定是推送还是提取
                        boolean isExtractMode = linkModes.get(pos, false);
                        if (isExtractMode) {
                            if (extractItems(linkTarget)) {
                                itemSent = true;
                            }
                            
                            if (extractLiquids(linkTarget)) {
                                liquidSent = true;
                            }
                        } else {
                            if (sendItems(linkTarget)) {
                                itemSent = true;
                            }
                            
                            if (sendLiquids(linkTarget)) {
                                liquidSent = true;
                            }
                        }
                    }
                }
            }
            
            if (consValid) {
                warmup = Mathf.lerpDelta(warmup, links.isEmpty() ? 0 : 1, warmupSpeed);
                rotateSpeed = Mathf.lerpDelta(rotateSpeed, (itemSent || liquidSent) ? 1 : 0, warmupSpeed);
            } else {
                warmup = Mathf.lerpDelta(warmup, 0, warmupSpeed);
                rotateSpeed = Mathf.lerpDelta(rotateSpeed, 0, warmupSpeed);
            }
            
            if (warmup > 0) {
                rotateDeg += rotateSpeed;
            }
        }
        
        @Override
        public void drawConfigure() {
            super.drawConfigure();
            
            float sin = Mathf.absin(Time.time, 6, 1);
            
            Draw.color(Pal.accent);
            Lines.stroke(1);
            Drawf.circles(x, y, (size / 2 + 1) * tilesize + sin - 2, Pal.accent);
            
            for (int i = 0; i < links.size; i++) {
                int pos = links.get(i);
                if (linkValid(this, pos)) {
                    Building linkTarget = world.build(pos);
                    // 增加边框宽度以提高对比度
                    Lines.stroke(2);
                    // 根据每个连接的模式显示不同的边框颜色
                    boolean isExtractMode = linkModes.get(pos, false);
                    if (isExtractMode) {
                        Drawf.square(linkTarget.x, linkTarget.y, linkTarget.block.size * tilesize / 2 + 1, Color.purple);
                    } else {
                        Drawf.square(linkTarget.x, linkTarget.y, linkTarget.block.size * tilesize / 2 + 1, Pal.place);
                    }
                    // 恢复默认线条宽度
                    Lines.stroke(1);
                }
            }
            
            Drawf.dashCircle(x, y, range, Pal.accent);
        }
        
        @Override
        public void draw() {
            super.draw();
            
            Draw.alpha(warmup);
            Draw.rect(bottomRegion, x, y);
            Draw.color();
            
            Draw.alpha(warmup);
            Draw.rect(rotatorRegion, x, y, -rotateDeg);
            
            Draw.alpha(1);
            Draw.rect(topRegion, x, y);
        }
        
        public Runnable rebuildUI;
        
        @Override
        public void buildConfiguration(Table table) {
            super.buildConfiguration(table);
            
            // 添加动画覆盖层
            Table animOverlay = new Table();
            animOverlay.background(Styles.black6);
            animOverlay.center();
            
            // 添加动画标签
            animOverlay.add("@").color(Color.yellow).pad(40).row();
            animOverlay.label(() -> "RBMKLogisticsOS").color(Color.green).pad(40).row();
            
            table.add(animOverlay).grow().minHeight(400);
            
            // 0.75秒后移除动画效果并显示正常UI
            Time.run(45f, () -> {
                animOverlay.remove();
                showNormalUI(table);
            });
        }
        
        public void showNormalUI(Table table) {
            Table cont = new Table().top();
            cont.left().defaults().left().growX();
            
            rebuildUI = () -> {
                cont.clearChildren();
                
                cont.table(Styles.grayPanel, info -> {
                    info.left().defaults().left();
                    info.add("[accent]Linked Blocks:[] " + links.size).row();
                    info.add("Range: " + range / tilesize + " tiles").row();
                }).growX().left().pad(10);
                cont.row();
                
                // 连接的方块预览
                if (!links.isEmpty()) {
                    cont.table(Styles.grayPanel, linksTable -> {
                        linksTable.left().defaults().left();
                        linksTable.add("[accent]Connected Blocks:[]").row();
                        
                        // 分类显示：推送模式和提取模式
                        int pushCount = 0;
                        int extractCount = 0;
                        
                        // 计算推送和提取模式的数量
                        for (int pos : links) {
                            if (linkModes.get(pos, false)) {
                                extractCount++;
                            } else {
                                pushCount++;
                            }
                        }
                        
                        // 显示推送模式的连接
                        if (pushCount > 0) {
                            linksTable.add("[green]Push Mode:[] (" + pushCount + ")").row();
                            for (int pos : links) {
                                if (!linkModes.get(pos, false)) {
                                    Building linkTarget = world.build(pos);
                                    if (linkTarget != null) {
                                        linksTable.image(linkTarget.block.uiIcon).size(16).padRight(4);
                                        linksTable.label(() -> linkTarget.block.localizedName + " (Push)").color(Color.lightGray).padRight(4);
                                        linksTable.button("Settings", Styles.defaultt, () -> {
                                            showLinkSettings(pos, false);
                                        }).size(80, 30);
                                        linksTable.row();
                                    }
                                }
                            }
                        }
                        
                        // 显示提取模式的连接
                        if (extractCount > 0) {
                            linksTable.add("[purple]Extract Mode:[] (" + extractCount + ")").row();
                            for (int pos : links) {
                                if (linkModes.get(pos, false)) {
                                    Building linkTarget = world.build(pos);
                                    if (linkTarget != null) {
                                        linksTable.image(linkTarget.block.uiIcon).size(16).padRight(4);
                                        linksTable.label(() -> linkTarget.block.localizedName + " (Extract)").color(Color.lightGray).padRight(4);
                                        linksTable.button("Settings", Styles.defaultt, () -> {
                                            showLinkSettings(pos, true);
                                        }).size(80, 30);
                                        linksTable.row();
                                    }
                                }
                            }
                        }
                        
                        // 添加分类按钮
                        linksTable.row();
                        linksTable.button("+ Add Category", Styles.defaultt, () -> {
                            Core.app.post(() -> {
                                BaseDialog dialog = new BaseDialog("Add Category");
                                dialog.cont.add("Category functionality coming soon!").row();
                                dialog.cont.button("OK", dialog::hide).size(100, 40);
                                dialog.show();
                            });
                        }).size(150, 40);
                    }).growX().left().pad(10);
                    cont.row();
                }
                
                if (items != null) {
                    cont.table(Styles.grayPanel, inventory -> {
                        inventory.left().defaults().left();
                        inventory.add("[accent]Inventory:[]").row();
                        
                        boolean hasItems = false;
                        for (Item item : content.items()) {
                            int count = items.get(item);
                            if (count > 0) {
                                hasItems = true;
                                inventory.image(item.uiIcon).size(16).padRight(4);
                                inventory.label(() -> count + "").color(Color.lightGray);
                                inventory.row();
                            }
                        }
                        
                        if (!hasItems) {
                            inventory.add("[gray]Empty[]").padTop(4);
                        }
                    }).growX().left().pad(10);
                }
                
                if (liquids != null) {
                    cont.table(Styles.grayPanel, liquidInfo -> {
                        liquidInfo.left().defaults().left();
                        liquidInfo.add("[accent]Liquid:[]").row();
                        
                        Liquid liquid = liquids.current();
                        if (liquid != null) {
                            liquidInfo.image(liquid.uiIcon).size(16).padRight(4);
                            liquidInfo.label(() -> liquid.localizedName + " " + (int)liquids.get(liquid) + " / " + (int)liquidCapacity).color(Color.lightGray);
                        } else {
                            liquidInfo.add("[gray]Empty[]").padTop(4);
                        }
                    }).growX().left().pad(10);
                }
            };
            
            rebuildUI.run();
            
            Table main = new Table().background(Styles.black6);
            ScrollPane pane = new ScrollPane(cont, Styles.smallPane);
            pane.setScrollingDisabled(false, false);
            pane.setOverscroll(false, false);
            
            Table scrollTable = new Table();
            scrollTable.add(pane).maxWidth(600).maxHeight(350);
            scrollTable.row();
            
            Slider horizontalSlider = new Slider(0, 100, 1, false);
            horizontalSlider.changed(() -> {
                float scrollPos = horizontalSlider.getValue() / 100f;
                pane.setScrollX(scrollPos * (pane.getMaxWidth() - pane.getWidth()));
            });
            scrollTable.add(horizontalSlider).width(600).height(20);
            
            main.add(scrollTable);
            table.top().add(main);
        }
        
        // 存储每个连接的物品过滤器
        public ObjectMap<Integer, Seq<Item>> linkItemFilters = new ObjectMap<>();
        // 存储每个连接的流体过滤器
        public ObjectMap<Integer, Seq<Liquid>> linkLiquidFilters = new ObjectMap<>();
        
        public void showLinkSettings(int pos, boolean isExtractMode) {
            Building linkTarget = world.build(pos);
            if (linkTarget == null) return;
            
            Core.app.post(() -> {
                showSettingsDialog(pos, isExtractMode);
            });
        }
        
        public void showSettingsDialog(int pos, boolean isExtractMode) {
            BaseDialog dialog = new BaseDialog(isExtractMode ? "Extract Settings" : "Push Settings");
            dialog.cont.pane(pane -> {
                pane.left().defaults().left();
                
                // 物品过滤器设置
                pane.add("[accent]Item Filter:[]").row();
                Seq<Item> itemFilter = linkItemFilters.get(pos, new Seq<>());
                
                // 使用ScrollPane包装物品选择器，支持所有物品显示
                Table itemTable = new Table();
                itemTable.left().defaults().left();
                
                int itemCount = 0;
                for (Item item : content.items()) {
                    // 跳过隐藏物品
                    if (item.isHidden()) continue;
                    
                    boolean checked = itemFilter.contains(item);
                    itemTable.image(item.uiIcon).size(32).padRight(4);
                    itemTable.check("", checked, b -> {
                        if (b) {
                            if (!itemFilter.contains(item)) {
                                itemFilter.add(item);
                            }
                        } else {
                            itemFilter.remove(item);
                        }
                        linkItemFilters.put(pos, itemFilter);
                    }).padRight(4);
                    
                    itemCount++;
                    if (itemCount % 4 == 0) {
                        itemTable.row();
                    }
                }
                
                ScrollPane itemPane = new ScrollPane(itemTable, Styles.smallPane);
                itemPane.setScrollingDisabled(false, false);
                itemPane.setOverscroll(false, false);
                
                pane.add(itemPane).maxHeight(120).maxWidth(400).row();
                
                // 添加物品横向滑轨
                Slider itemSlider = new Slider(0, 100, 1, false);
                itemSlider.changed(() -> {
                    float scrollPos = itemSlider.getValue() / 100f;
                    itemPane.setScrollX(scrollPos * (itemPane.getMaxWidth() - itemPane.getWidth()));
                });
                pane.add(itemSlider).width(400).height(20).row();
                
                // 流体过滤器设置
                pane.add("[accent]Liquid Filter:[]").row();
                Seq<Liquid> liquidFilter = linkLiquidFilters.get(pos, new Seq<>());
                
                Table liquidTable = new Table();
                liquidTable.left().defaults().left();
                
                int liquidCount = 0;
                for (Liquid liquid : content.liquids()) {
                    if (liquid == null || liquid.isHidden()) continue;
                    
                    boolean checked = liquidFilter.contains(liquid);
                    liquidTable.image(liquid.uiIcon).size(32).padRight(4);
                    liquidTable.check("", checked, b -> {
                        if (b) {
                            if (!liquidFilter.contains(liquid)) {
                                liquidFilter.add(liquid);
                            }
                        } else {
                            liquidFilter.remove(liquid);
                        }
                        linkLiquidFilters.put(pos, liquidFilter);
                    }).padRight(4);
                    
                    liquidCount++;
                    if (liquidCount % 4 == 0) {
                        liquidTable.row();
                    }
                }
                
                ScrollPane liquidPane = new ScrollPane(liquidTable, Styles.smallPane);
                liquidPane.setScrollingDisabled(false, false);
                liquidPane.setOverscroll(false, false);
                
                pane.add(liquidPane).maxHeight(120).maxWidth(400).row();
                
                // 添加流体横向滑轨
                Slider liquidSlider = new Slider(0, 100, 1, false);
                liquidSlider.changed(() -> {
                    float scrollPos = liquidSlider.getValue() / 100f;
                    liquidPane.setScrollX(scrollPos * (liquidPane.getMaxWidth() - liquidPane.getWidth()));
                });
                pane.add(liquidSlider).width(400).height(20).row();
            }).maxHeight(400);
            
            dialog.cont.button("OK", dialog::hide).size(100, 40);
            dialog.show();
        }
        
        @Override
        public boolean onConfigureBuildTapped(Building other) {
            if (this == other) {
                return false;
            }
            
            if (within(other, range) && other.team == team) {
                int pos = other.pos();
                // 检查是否已经连接到这个方块
                if (links.contains(pos)) {
                    // 已连接，增加点击计数
                    int count = clickCounts.get(pos, 0) + 1;
                    clickCounts.put(pos, count);
                    
                    if (count == 1) {
                        // 第一次点击，保持推送模式
                        linkModes.put(pos, false);
                    } else if (count == 2) {
                        // 第二次点击，切换到提取模式
                        linkModes.put(pos, true);
                    } else {
                        // 第三次点击，断开连接
                        configure(pos);
                    }
                } else {
                    // 未连接，添加连接
                    configure(pos);
                }
                // 更新UI显示
                if (rebuildUI != null) {
                    rebuildUI.run();
                }
                return false;
            }
            
            return true;
        }
        
        @Override
        public Object config() {
            IntSeq output = new IntSeq(links.size * 2);
            for (int i = 0; i < links.size; i++) {
                int pos = links.get(i);
                Point2 point2 = Point2.unpack(pos).sub(tile.x, tile.y);
                output.add(point2.x, point2.y);
            }
            return output;
        }
        
        @Override
        public void configure(Object value) {
            if (value instanceof IntSeq) {
                IntSeq sq = (IntSeq)value;
                Seq<Integer> newLinks = new Seq<>();
                for (int i = 0; i < sq.size; i += 2) {
                    int linkX = sq.get(i);
                    int linkY = sq.get(i + 1);
                    int pos = Point2.pack(linkX + tile.x, linkY + tile.y);
                    newLinks.add(pos);
                }
                setLink(newLinks);
            } else if (value instanceof Integer) {
                setOneLink((Integer)value);
            }
        }
        
        @Override
        public void write(Writes write) {
            super.write(write);
            write.s(links.size);
            for (int pos : links) {
                write.i(pos);
                // 写入每个连接的模式
                write.b((byte)(linkModes.get(pos, false) ? 1 : 0));
                // 写入每个连接的点击计数
                write.i(clickCounts.get(pos, 0));
                // 写入物品过滤器
                Seq<Item> itemFilter = linkItemFilters.get(pos, new Seq<>());
                write.s(itemFilter.size);
                for (Item item : itemFilter) {
                    write.s(item.id);
                }
                // 写入流体过滤器
                Seq<Liquid> liquidFilter = linkLiquidFilters.get(pos, new Seq<>());
                write.s(liquidFilter.size);
                for (Liquid liquid : liquidFilter) {
                    write.s(liquid.id);
                }
            }
        }
        
        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            links = new Seq<>();
            linkModes = new ObjectMap<>();
            clickCounts = new ObjectMap<>();
            linkItemFilters = new ObjectMap<>();
            linkLiquidFilters = new ObjectMap<>();
            int linkSize = read.s();
            for (int i = 0; i < linkSize; i++) {
                int pos = read.i();
                links.add(pos);
                // 读取每个连接的模式
                if (revision >= 3) {
                    linkModes.put(pos, read.b() == 1);
                    // 读取每个连接的点击计数
                    if (revision >= 4) {
                        clickCounts.put(pos, read.i());
                        // 读取物品过滤器
                        if (revision >= 5) {
                            int itemFilterSize = read.s();
                            Seq<Item> itemFilter = new Seq<>();
                            for (int j = 0; j < itemFilterSize; j++) {
                                int itemId = read.s();
                                Item item = content.item(itemId);
                                if (item != null) {
                                    itemFilter.add(item);
                                }
                            }
                            linkItemFilters.put(pos, itemFilter);
                            // 读取流体过滤器
                            int liquidFilterSize = read.s();
                            Seq<Liquid> liquidFilter = new Seq<>();
                            for (int j = 0; j < liquidFilterSize; j++) {
                                int liquidId = read.s();
                                Liquid liquid = content.liquid(liquidId);
                                if (liquid != null) {
                                    liquidFilter.add(liquid);
                                }
                            }
                            linkLiquidFilters.put(pos, liquidFilter);
                        } else {
                            linkItemFilters.put(pos, new Seq<>());
                            linkLiquidFilters.put(pos, new Seq<>());
                        }
                    } else {
                        clickCounts.put(pos, 0); // 默认点击计数为0
                        linkItemFilters.put(pos, new Seq<>());
                        linkLiquidFilters.put(pos, new Seq<>());
                    }
                } else {
                    linkModes.put(pos, false); // 默认推送模式
                    clickCounts.put(pos, 0); // 默认点击计数为0
                    linkItemFilters.put(pos, new Seq<>());
                    linkLiquidFilters.put(pos, new Seq<>());
                }
            }
        }
        
        @Override
        public byte version() {
            return 5;
        }
    }
}