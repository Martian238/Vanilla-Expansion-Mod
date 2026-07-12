package VanillaExpansion;

import arc.Core;
import arc.Events;
import arc.audio.Sound;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.ui.ButtonGroup;
import arc.scene.ui.ImageButton;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.struct.*;
import arc.util.*;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.ai.UnitCommand;
import mindustry.content.Fx;
import mindustry.content.TechTree;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.entities.Effect;
import mindustry.entities.Units;
import mindustry.game.EventType.UnitCreateEvent;
import mindustry.game.Objectives;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.io.TypeIO;
import mindustry.logic.LAccess;
import mindustry.logic.Senseable;
import mindustry.type.*;
import mindustry.ui.Bar;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.heat.HeatBlock;
import mindustry.world.blocks.heat.HeatConductor;
import mindustry.world.blocks.liquid.Conduit;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.production.HeatCrafter;
import mindustry.world.consumers.ConsumePower;
import mindustry.world.draw.DrawBlock;
import mindustry.world.meta.*;
import mindustry.mod.Mods.*;

import static MultiCraft.MultiCrafter.MultiCrafterBuild.allPayloadTypes;
import static mindustry.Vars.*;


import static mindustry.Vars.content;
import static mindustry.graphics.Pal.command;

public class MultiCrafter extends HeatCrafter {
    public Seq<Recipe> recipes = new Seq<>();
    public float updateEffectChance = 0.05f;
    public Effect updateEffect = Fx.none;
    public boolean showWarmup = false;

    public boolean hasPayloads = false;
    public int payloadCapacity = 1;
    public float payloadSpeed = 0.7f;
    public float payloadRotateSpeed = 5f;
    public Recipe[] recipe;
    protected boolean hasItemOutput = false;
    protected boolean hasLiquidOutput = false;

    public MultiCrafter(String name) {
        super(name);
        heatRequirement = 0f;
        configurable = true;
        hasItems = true;
        hasLiquids = true;
        saveConfig = true;
        update = true;
        solid = true;
        group = BlockGroup.none;
        buildType = MultiCrafterBuild::new;
        maxEfficiency = 1f;
        acceptsPayload = true;
        outputsPayload = true;
        rotate = false;
        drawArrow = false;

        config(Integer.class, (MultiCrafterBuild build, Integer value) -> {
            int newVal = Mathf.clamp(value, 0, recipes.size);
            if (build.selectRecipe != newVal) {
                build.selectRecipe = newVal;
                build.progress = 0f;
            }
        });


        config(UnitCommand.class, (MultiCrafterBuild build, UnitCommand cmd) -> {
            build.command = cmd;
        });
    }


    @Nullable
    public static Effect findEffectByPath(String path) {
        if (path == null || path.isEmpty()) return Fx.none;
        try {
            int lastDot = path.lastIndexOf('.');
            if (lastDot < 0) return Fx.none;
            String className = path.substring(0, lastDot);
            String fieldName = path.substring(lastDot + 1);
            Class<?> clazz = Class.forName(className);
            return (Effect) clazz.getField(fieldName).get(null);
        } catch (Exception e) {
            return Fx.none;
        }
    }

    @Nullable
    public static Sound findSoundByPath(String path) {
        if (path == null || path.isEmpty()) return Sounds.none;
        try {
            int lastDot = path.lastIndexOf('.');
            if (lastDot < 0) return Sounds.none;
            String className = path.substring(0, lastDot);
            String fieldName = path.substring(lastDot + 1);
            Class<?> clazz = Class.forName(className);
            return (Sound) clazz.getField(fieldName).get(null);
        } catch (Exception e) {
            return Sounds.none;
        }
    }

    // ---------- 配方添加 ----------
    public void addRecipe(ItemStack[] inputItems, LiquidStack[] inputLiquids, float inputPower, float inputHeat,
                          ItemStack[] outputItems, LiquidStack[] outputLiquids, float outputPower, float outputHeat,
                          float craftTime) {
        recipes.add(new Recipe(inputItems, inputLiquids, inputPower, inputHeat,
                outputItems, outputLiquids, outputPower, outputHeat, craftTime));
    }

    public void addRecipe(ItemStack[] inputItems, LiquidStack[] inputLiquids, float inputPower,
                          ItemStack[] outputItems, LiquidStack[] outputLiquids, float outputPower,
                          float craftTime) {
        addRecipe(inputItems, inputLiquids, inputPower, 0f,
                outputItems, outputLiquids, outputPower, 0f, craftTime);
    }

    public void addRecipe(ItemStack[] inputItems, LiquidStack[] inputLiquids, float inputPower, float inputHeat,
                          ItemStack[] outputItems, LiquidStack[] outputLiquids, float outputPower, float outputHeat,
                          float craftTime, PayloadStack[] inputPayloads, PayloadStack[] outputPayloads, @Nullable DrawBlock visual) {
        recipes.add(new Recipe(inputItems, inputLiquids, inputPower, inputHeat,
                outputItems, outputLiquids, outputPower, outputHeat, craftTime,
                inputPayloads, outputPayloads, visual));
    }

    public void addRecipe(ItemStack[] inputItems, LiquidStack[] inputLiquids, float inputPower,
                          ItemStack[] outputItems, LiquidStack[] outputLiquids, float outputPower,
                          float craftTime, PayloadStack[] inputPayloads, PayloadStack[] outputPayloads) {
        addRecipe(inputItems, inputLiquids, inputPower, 0f,
                outputItems, outputLiquids, outputPower, 0f, craftTime,
                inputPayloads, outputPayloads);
    }

    public void addRecipe(ItemStack[] inputItems, LiquidStack[] inputLiquids, float inputPower, float inputHeat,
                          ItemStack[] outputItems, LiquidStack[] outputLiquids, float outputPower, float outputHeat,
                          float craftTime, PayloadStack[] inputPayloads, PayloadStack[] outputPayloads) {
        recipes.add(new Recipe(inputItems, inputLiquids, inputPower, inputHeat,
                outputItems, outputLiquids, outputPower, outputHeat, craftTime,
                inputPayloads, outputPayloads, null));
    }

    public void addRecipe(ItemStack[] inputItems, LiquidStack[] inputLiquids, float inputPower, float inputHeat,
                          ItemStack[] outputItems, LiquidStack[] outputLiquids, float outputPower, float outputHeat,
                          float craftTime, @Nullable DrawBlock visual) {
        recipes.add(new Recipe(inputItems, inputLiquids, inputPower, inputHeat,
                outputItems, outputLiquids, outputPower, outputHeat, craftTime, visual));
    }

    public void addRecipe(ItemStack[] inputItems, LiquidStack[] inputLiquids, float inputPower,
                          ItemStack[] outputItems, LiquidStack[] outputLiquids, float outputPower,
                          float craftTime, @Nullable DrawBlock visual) {
        addRecipe(inputItems, inputLiquids, inputPower, 0f,
                outputItems, outputLiquids, outputPower, 0f, craftTime, visual);
    }

    public void addRecipe(
            ItemStack[] inputItems, LiquidStack[] inputLiquids, float inputPower, float inputHeat,
            PayloadStack[] inputPayloads,
            ItemStack[] outputItems, LiquidStack[] outputLiquids, float outputPower, float outputHeat,
            PayloadStack[] outputPayloads,
            float craftTime, float maxEfficiency, DrawBlock visual,
            String craftEffectPath, String updateEffectPath, String switchEffectPath, float updateEffectChance,
            String craftSoundPath, String updateSoundPath
    ) {
        recipes.add(new Recipe(
                inputItems, inputLiquids, inputPower, inputHeat, inputPayloads,
                outputItems, outputLiquids, outputPower, outputHeat, outputPayloads,
                craftTime, maxEfficiency, visual,
                craftEffectPath, updateEffectPath, switchEffectPath, updateEffectChance,
                craftSoundPath, updateSoundPath
        ));
    }


    @Override
    public void init() {

        if (recipe != null && recipe.length > 0) {
            for (Recipe r : recipe) {
                r.ensureArrays();
                if (r.inputPayloads == null) r.inputPayloads = new String[0];
                if (r.outputPayloads == null) r.outputPayloads = new String[0];
                r.cachedInputPayloads = parsePayloadStacks(r.inputPayloads);
                r.cachedOutputPayloads = parsePayloadStacks(r.outputPayloads);
                recipes.add(r);
            }
            recipe = null;
        }


        for (Recipe rec : recipes) {
            rec.ensureArrays();
            if (rec.cachedInputPayloads == null && rec.inputPayloads != null)
                rec.cachedInputPayloads = parsePayloadStacks(rec.inputPayloads);
            if (rec.cachedOutputPayloads == null && rec.outputPayloads != null)
                rec.cachedOutputPayloads = parsePayloadStacks(rec.outputPayloads);
            if (rec.cachedInputPayloads == null) rec.cachedInputPayloads = new PayloadStack[0];
            if (rec.cachedOutputPayloads == null) rec.cachedOutputPayloads = new PayloadStack[0];

            if (rec.cachedInputPayloads.length > 0 || rec.cachedOutputPayloads.length > 0) hasPayloads = true;

            rec.cachedCraftEffect = findEffectByPath(rec.craftEffect);
            rec.cachedUpdateEffect = findEffectByPath(rec.updateEffect);
            rec.cachedSwitchEffect = findEffectByPath(rec.switchEffect);
            rec.cachedCraftSound = findSoundByPath(rec.craftSound);
            rec.cachedUpdateSound = findSoundByPath(rec.updateSound);
        }


        ObjectSet<Item> allOutputItems = new ObjectSet<>();
        ObjectSet<Liquid> allOutputLiquids = new ObjectSet<>();
        for (Recipe rec : recipes) {
            for (ItemStack s : rec.outputItems) if (s != null && s.item != null) allOutputItems.add(s.item);
            for (LiquidStack s : rec.outputLiquids) if (s != null && s.liquid != null) allOutputLiquids.add(s.liquid);
        }

        if (allOutputItems.size > 0) {
            outputItems = new ItemStack[allOutputItems.size];
            int i = 0;
            for (Item item : allOutputItems) outputItems[i++] = new ItemStack(item, 1);
        }
        if (allOutputLiquids.size > 0) {
            outputLiquids = new LiquidStack[allOutputLiquids.size];
            int i = 0;
            for (Liquid liq : allOutputLiquids) outputLiquids[i++] = new LiquidStack(liq, 0.1f);
        }

        hasItemOutput = outputItems != null;
        hasLiquidOutput = outputLiquids != null;


        boolean hasPowerInput = recipes.contains(r -> r.inputPower > 0);
        if (hasPowerInput) {
            consume(new ConsumePower(0f, 0f, false) {
                @Override
                public float requestedPower(Building entity) {
                    if (entity instanceof MultiCrafterBuild build) {
                        Recipe rec = build.getCurrentRecipe();
                        if (rec != null && rec.inputPower > 0) return rec.inputPower;
                    }
                    return 0;
                }

                @Override
                public float efficiency(Building build) {
                    if (build instanceof MultiCrafterBuild multi) {
                        Recipe rec = multi.getCurrentRecipe();
                        if (rec != null && rec.inputPower > 0)
                            return build.power != null ? build.power.status : 0f;
                        return 1f;
                    }
                    return super.efficiency(build);
                }
            });

        }


        for (Recipe r : recipes) if (r.visual != null) r.visual.load(this);

        boolean hasUnitOutput = recipes.contains(r -> {
            for (PayloadStack stack : r.cachedOutputPayloads)
                if (stack.item instanceof UnitType) return true;
            return false;
        });
        if (hasUnitOutput) {
            rotate = true;
            drawArrow = true;
            commandable = true;
        }
        for (Recipe rec : recipes) {
            for (PayloadStack stack : rec.cachedInputPayloads) {
                if (!allPayloadTypes.contains(stack.item)) {
                    allPayloadTypes.add(stack.item);
                }
            }
            for (PayloadStack stack : rec.cachedOutputPayloads) {
                if (!allPayloadTypes.contains(stack.item)) {
                    allPayloadTypes.add(stack.item);
                }
            }
        }

        boolean hasPowerOutput = recipes.contains(r -> r.outputPower > 0);
        if (hasPowerOutput) {
            outputsPower = true;
        }

        boolean hasAnyPower = recipes.contains(r -> r.inputPower > 0 || r.outputPower > 0);
        if (hasAnyPower) {
            hasPower = true;
        }


        for (int i = 0; i < recipes.size; i++) {
            Recipe rec = recipes.get(i);

            // 判断是否需要解锁节点
            boolean forced = rec.alwaysUnlocked != null;
            boolean hasUnlockConditions;
            if (forced) {
                hasUnlockConditions = !rec.alwaysUnlocked; // false 表示需要解锁
            } else {
                hasUnlockConditions = rec.parent != null || rec.objectives.any();
            }

            String blockName = name + "-recipe-" + i;

            // 防止重复注册
            Block existing = Vars.content.getByName(ContentType.block, blockName);
            if (existing != null) {
                rec.unlockBlock = existing;
                continue;
            }

            Block block = new Block(blockName) {
                {
                    placeablePlayer = false;
                    buildVisibility = BuildVisibility.sandboxOnly;
                    requirements = ItemStack.empty;
                    alwaysUnlocked = !hasUnlockConditions;
                    databaseCategory = rec.databaseCategory != null ? rec.databaseCategory : "recipe";
                    databaseTag = rec.databaseTag != null ? rec.databaseTag : "default";
                }

                @Override
                public boolean isHidden() {
                    return false; // 所有配方在数据库中可见
                }
            };
            block.minfo.mod = minfo.mod;
            block.localizedName = localizedName + " Recipe " + (i + 1);

            rec.unlockBlock = block;

        }

        super.init();
        outputsLiquid = hasLiquidOutput;
        if (outputsLiquid && (liquidOutputDirections == null || liquidOutputDirections.length == 0))
            liquidOutputDirections = new int[]{-1};


        consumesPower = consPower != null;


    }

    @Override
    public void load() {
        super.load();

        for (Recipe rec : recipes) {
            if (rec.unlockBlock != null) {
                rec.unlockBlock.fullIcon = fullIcon;
                rec.unlockBlock.uiIcon = fullIcon;
            }
        }

        TechTree.TechNode factoryNode = techNode;

        for (int i = 0; i < recipes.size; i++) {
            Recipe rec = recipes.get(i);

            // 判断是否需要解锁节点（与 init 中保持一致）
            boolean forced = rec.alwaysUnlocked != null;
            boolean hasUnlockConditions;
            if (forced) {
                hasUnlockConditions = !rec.alwaysUnlocked;
            } else {
                hasUnlockConditions = rec.parent != null || rec.objectives.any();
            }

            if (rec.unlockBlock == null || !hasUnlockConditions) continue;
            // 防止重复节点
            if (rec.unlockBlock.techNode != null) continue;

            TechTree.TechNode node = new TechTree.TechNode(null, rec.unlockBlock, ItemStack.empty);
            node.objectives.addAll(rec.objectives);

            // 如果没有自定义 objective 且没有 parent，但用户显式设置了 alwaysUnlocked = false，
            // 则自动添加所有输入输出物品/液体/载荷为 Research 目标
            if (rec.objectives.isEmpty() && rec.parent == null && forced && !rec.alwaysUnlocked) {
                ObjectSet<UnlockableContent> allContent = new ObjectSet<>();
                // 输入物品
                for (ItemStack stack : rec.inputItems) allContent.add(stack.item);
                // 输出物品
                for (ItemStack stack : rec.outputItems) allContent.add(stack.item);
                // 输入液体
                for (LiquidStack stack : rec.inputLiquids) allContent.add(stack.liquid);
                // 输出液体
                for (LiquidStack stack : rec.outputLiquids) allContent.add(stack.liquid);
                // 载荷（输入输出）
                if (rec.cachedInputPayloads != null)
                    for (PayloadStack stack : rec.cachedInputPayloads)
                        if (stack.item != null) allContent.add(stack.item);
                if (rec.cachedOutputPayloads != null)
                    for (PayloadStack stack : rec.cachedOutputPayloads)
                        if (stack.item != null) allContent.add(stack.item);

                for (UnlockableContent c : allContent) {
                    node.objectives.add(new Objectives.Research(c));
                }
            }

            // 挂载父节点
            if (rec.parent != null) {
                TechTree.TechNode parentNode = TechTree.all.find(t ->
                        t.content.name.equals(rec.parent) ||
                                t.content.name.equals((minfo.mod != null ? minfo.mod.name + "-" : "") + rec.parent)
                );
                if (parentNode != null) {
                    parentNode.children.add(node);
                    node.parent = parentNode;
                    node.depth = parentNode.depth + 1;
                    node.planet = parentNode.planet;
                } else if (factoryNode != null) {
                    factoryNode.children.add(node);
                    node.parent = factoryNode;
                    node.depth = factoryNode.depth + 1;
                    node.planet = factoryNode.planet;
                }
            } else {
                if (factoryNode != null) {
                    factoryNode.children.add(node);
                    node.parent = factoryNode;
                    node.depth = factoryNode.depth + 1;
                    node.planet = factoryNode.planet;
                }
            }

            TechTree.all.add(node);
        }
    }


    private PayloadStack[] parsePayloadStacks(String[] strs) {
        if (strs == null || strs.length == 0) return new PayloadStack[0];
        Seq<PayloadStack> list = new Seq<>();
        for (String s : strs) {
            if (s == null || s.isEmpty()) continue;
            String[] parts = s.split("/");
            if (parts.length != 2) continue;
            UnlockableContent content = Vars.content.getByName(ContentType.block, parts[0]);
            if (content == null) content = Vars.content.getByName(ContentType.unit, parts[0]);
            if (content != null) {
                try {
                    int amount = Integer.parseInt(parts[1]);
                    if (amount > 0) list.add(new PayloadStack(content, amount));
                } catch (NumberFormatException ignored) {}
            }
        }
        return list.toArray(PayloadStack.class);
    }

    @Override
    public boolean outputsItems() { return hasItemOutput; }


    @Override
    public void setStats() {
        stats.timePeriod = craftTime;
        stats.add(Stat.size, "@x@", size, size);
        if (synthetic()) {
            stats.add(Stat.health, health, StatUnit.none);
            if (armor > 0) stats.add(Stat.armor, armor, StatUnit.none);
        }
        if (canBeBuilt() && requirements.length > 0) {
            stats.add(Stat.buildTime, buildTime / 60, StatUnit.seconds);
            stats.add(Stat.buildCost, StatValues.items(false, requirements));
        }
        for (var c : consumers) c.display(stats);
        if (hasLiquids) stats.add(Stat.liquidCapacity, liquidCapacity, StatUnit.liquidUnits);
        if (hasItems && itemCapacity > 0) stats.add(Stat.itemCapacity, itemCapacity, StatUnit.items);

        stats.add(Stat.output, table -> {
            table.clearChildren();
            table.left();
            for (int i = 0; i < recipes.size; i++) {
                Recipe rec = recipes.get(i);
                rec.ensureArrays();
                int idx = i;
                table.table(Styles.grayPanel, t -> {
                    t.left().defaults().left().padLeft(4);

                    Block unlock = rec.unlockBlock;
                    boolean banned = unlock != null && unlock.isBanned();
                    boolean locked = unlock != null && !unlock.unlockedNow();

                    if (banned) {
                        t.align(Align.center);
                        t.image(Icon.cancel).color(Pal.remove).size(40).pad(8f).center();
                        return;  // 只显示叉号
                    }

                    if (locked) {
                        t.align(Align.center);
                        t.image(Icon.lock).color(Pal.darkerGray).size(40).pad(8f).center();
                        return;  // 只显示锁
                    }

                    String title = "[accent]Recipe " + (idx + 1) + "[]";
                    t.add(title).padTop(4).padBottom(4);
                    t.row();
                    boolean hasInput = rec.inputItems.length > 0 || rec.inputLiquids.length > 0
                            || rec.inputPower > 0 || rec.inputHeat > 0 || rec.cachedInputPayloads.length > 0;
                    if (hasInput) {
                        t.add("[lightgray]" + Core.bundle.get("stat.input") + ":[]").padRight(8);
                        for (ItemStack s : rec.inputItems)
                            t.add(StatValues.displayItem(s.item, s.amount, rec.craftTime, true)).padRight(8);
                        for (LiquidStack s : rec.inputLiquids)
                            t.add(StatValues.displayLiquid(s.liquid, s.amount * 60f, true)).padRight(8);
                        for (PayloadStack s : rec.cachedInputPayloads)
                            t.table(pl -> {
                                pl.image(s.item.uiIcon).size(32).padRight(2);
                                pl.add(s.item.localizedName).color(Color.lightGray).padRight(4);
                                pl.add(Strings.autoFixed(s.amount / (rec.craftTime / 60f), 1) + StatUnit.perSecond.localized()).color(Color.lightGray);
                            }).padRight(8);
                        if (rec.inputPower > 0)
                            t.table(p -> StatValues.number(rec.inputPower * 60f, StatUnit.powerSecond).display(p)).padRight(8);
                        if (rec.inputHeat > 0)
                            t.table(h -> StatValues.number(rec.inputHeat, StatUnit.heatUnits).display(h)).padRight(8);
                        t.row();
                    }
                    boolean hasOutput = rec.outputItems.length > 0 || rec.outputLiquids.length > 0
                            || rec.outputHeat > 0 || rec.outputPower > 0 || rec.cachedOutputPayloads.length > 0
                            || rec.randomResults != null;
                    if (hasOutput) {
                        t.add("[lightgray]" + Core.bundle.get("stat.output") + ":[]").padRight(8);
                        for (ItemStack s : rec.outputItems)
                            t.add(StatValues.displayItem(s.item, s.amount, rec.craftTime, true)).padRight(8);
                        for (LiquidStack s : rec.outputLiquids)
                            t.add(StatValues.displayLiquid(s.liquid, s.amount * 60f, true)).padRight(8);
                        for (PayloadStack s : rec.cachedOutputPayloads)
                            t.table(pl -> {
                                pl.image(s.item.uiIcon).size(32).padRight(2);
                                pl.add(s.item.localizedName).color(Color.lightGray).padRight(4);
                                pl.add(Strings.autoFixed(s.amount / (rec.craftTime / 60f), 1) + StatUnit.perSecond.localized()).color(Color.lightGray);
                            }).padRight(8);
                        if (rec.outputPower > 0)
                            t.table(p -> StatValues.number(rec.outputPower * 60f, StatUnit.powerSecond).display(p)).padRight(8);
                        if (rec.outputHeat > 0)
                            t.table(h -> StatValues.number(rec.outputHeat, StatUnit.heatUnits).display(h)).padRight(8);

                        if (rec.randomResults != null) {
                            t.row();
                            float sum = 0f;
                            for (ItemStack s : rec.randomResults) sum += s.amount;
                            for (ItemStack stack : rec.randomResults) {
                                int percent = (int)(stack.amount / sum * 100);
                                t.add(StatValues.displayItemPercent(stack.item, percent, true)).padRight(5);
                            }
                        }
                    }
                    if (rec.attribute != null) {
                        t.row();
                        Table affTable = new Table();
                        StatValues.blocks(rec.attribute, rec.floating, rec.boostScale * size * size, !rec.displayEfficiency).display(affTable);
                        t.add(affTable).padLeft(4);
                    }
                    t.add("[lightgray]" + Core.bundle.get("stat.productiontime") + ":[] " + Strings.autoFixed(rec.craftTime / 60f, 3) + " " + Core.bundle.get("unit.seconds")).padTop(4);
                    if (rec.inputHeat > 0)
                        t.add("  [lightgray]最大效率:[] " + Strings.autoFixed(rec.maxEfficiency * 100f, 0) + "%");
                }).growX().pad(5).row();
            }
        });
    }

    @Override
    public void setBars() {
        super.setBars();
        removeBar("items");
        removeBar("heat");
        removeBar("liquid");

        if (showWarmup)
            addBar("warmup", (MultiCrafterBuild entity) ->
                    new Bar(() -> Core.bundle.get("bar.warmup"), () -> Pal.accent, () -> entity.warmup));

        ObjectSet<Liquid> allLiquids = new ObjectSet<>();
        for (Recipe rec : recipes) {
            for (LiquidStack stack : rec.inputLiquids) allLiquids.add(stack.liquid);
            for (LiquidStack stack : rec.outputLiquids) allLiquids.add(stack.liquid);
        }
        if (allLiquids.size > 0)
            for (Liquid liquid : allLiquids)
                addBar("liquid-" + liquid.name, (MultiCrafterBuild entity) ->
                        new Bar(() -> liquid.localizedName, liquid::barColor,
                                () -> entity.liquids.get(liquid) / liquidCapacity));

        if (recipes.contains(r -> r.outputPower > 0))
            addBar("power-output", (MultiCrafterBuild entity) ->
                    new Bar(() -> Core.bundle.format("bar.poweroutput", entity.getPowerProduction() * 60 * entity.timeScale()),
                            () -> Pal.powerBar, () -> entity.getPowerStat()));

        if (recipes.contains(r -> r.inputHeat > 0))
            addBar("heat-input", (MultiCrafterBuild entity) -> {
                Recipe rec = entity.getCurrentRecipe();
                float need = rec != null && rec.inputHeat > 0 ? rec.inputHeat :
                        recipes.find(r2 -> r2.inputHeat > 0) != null ? recipes.find(r2 -> r2.inputHeat > 0).inputHeat : 1f;
                return new Bar(
                        () -> "热量输入：" + (int)(entity.heatInput + 0.01f) + " (" + (int)(entity.heatEfficiencyScale() * 100 + 0.01f) + "%)",
                        () -> Pal.lightOrange,
                        () -> need > 0 ? Mathf.clamp(entity.heatInput / need) : 0f
                );
            });

        if (recipes.contains(r -> r.outputHeat > 0))
            addBar("heat-output", (MultiCrafterBuild entity) -> {
                Recipe rec = entity.getCurrentRecipe();
                float max = rec != null && rec.outputHeat > 0 ? rec.outputHeat :
                        recipes.find(r2 -> r2.outputHeat > 0) != null ? recipes.find(r2 -> r2.outputHeat > 0).outputHeat : 1f;
                return new Bar(
                        () -> "热量输出：" + (int)(entity.heatOutput + 0.01f) + " (" + (int)(entity.heatOutput / max * 100 + 0.01f) + "%)",
                        () -> Pal.lightOrange,
                        () -> max > 0 ? Mathf.clamp(entity.heatOutput / max) : 0f
                );
            });

        addBar("efficiency", (MultiCrafterBuild entity) -> {
            Recipe rec = entity.getCurrentRecipe();
            if (rec == null || rec.attribute == null || !rec.displayEfficiency) return null;
            return new Bar(
                    () -> Core.bundle.format("bar.efficiency", (int)(entity.efficiencyMultiplier() * 100)),
                    () -> Pal.lightOrange,
                    entity::efficiencyMultiplier
            );
        });

        // 单位上限条
        ObjectSet<UnitType> allUnitOutputs = new ObjectSet<>();
        for (Recipe rec : recipes)
            for (PayloadStack stack : rec.cachedOutputPayloads)
                if (stack.item instanceof UnitType) allUnitOutputs.add((UnitType) stack.item);
        if (!allUnitOutputs.isEmpty())
            addBar("units", (MultiCrafterBuild e) -> {
                UnitType type = e.getDisplayedUnitType();
                return new Bar(
                        () -> type == null ? "[lightgray]" + Iconc.cancel :
                                Core.bundle.format("bar.unitcap",
                                        Fonts.getUnicodeStr(type.name),
                                        e.team.data().countType(type),
                                        type.useUnitCap ? Units.getStringCap(e.team) : "∞"),
                        () -> Pal.power,
                        () -> type == null ? 0f : (type.useUnitCap ? (float) e.team.data().countType(type) / Units.getCap(e.team) : 1f)
                );
            });

        // 单位生产进度条
        boolean hasUnitOutput = recipes.contains(r -> {
            for (PayloadStack stack : r.cachedOutputPayloads)
                if (stack.item instanceof UnitType) return true;
            return false;
        });
        if (hasUnitOutput)
            addBar("progress", (MultiCrafterBuild e) -> new Bar(
                    "bar.progress", Pal.ammo,
                    () -> {
                        Recipe rec = e.getCurrentRecipe();
                        return (rec != null && rec.cachedOutputPayloads != null &&
                                Structs.contains(rec.cachedOutputPayloads, s -> s.item instanceof UnitType)) ?
                                e.progress  : 0f;
                    }
            ));
    }

    @Override
    public boolean rotatedOutput(int fromX, int fromY, Tile destination) {
        if (!(destination.build instanceof Conduit.ConduitBuild)) return false;
        Building crafter = world.build(fromX, fromY);
        if (crafter instanceof MultiCrafterBuild build) {
            Recipe rec = build.getCurrentRecipe();
            if (rec == null || rec.outputLiquids == null || rec.outputLiquids.length == 0) return false;
            int relative = Mathf.mod(crafter.relativeTo(destination) - crafter.rotation, 4);
            for (int dir : rec.liquidOutputDirections)
                if (dir == -1 || dir == relative) return false;
        }
        return true;
    }

    @Override
    public boolean configSenseable() {
        return configurations.containsKey(Integer.class) || super.configSenseable();
    }

    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation) {
        for (Recipe rec : recipes) {
            if (rec.attribute != null && rec.minEfficiency >= 0) {
                float sum = sumAttribute(rec.attribute, tile.x, tile.y);
                float eff = rec.baseEfficiency + Math.min(rec.maxBoost, rec.boostScale * sum) + rec.attribute.env();
                if (eff < rec.minEfficiency) return false;
            }
        }
        return true;

    }




    public class MultiCrafterBuild extends HeatCrafterBuild implements HeatBlock {
        public ObjectSet<Item> outputItemsSet = new ObjectSet<>();
        public ObjectSet<Liquid> outputLiquidsSet = new ObjectSet<>();
        public int selectRecipe = 0; //选择的配方
        public float heatInput, heatOutput; //热量
        public float warmupRate = 0.15f; //热量变化速度

        public @Nullable Payload payload;
        public Vec2 payVector = new Vec2();
        public float payRotation;
        public boolean carried; //载荷输出方向
        public IntMap<Integer> payloadCounts = new IntMap<>(); // 输入 + 方块输出

        public @Nullable Recipe currentVisualRecipe; //视觉上运行的配方
        public ObjectSet<UnlockableContent> outputPayloadsSet = new ObjectSet<>(); // 方块产物类型

        public @Nullable Payload outputPayload;
        public Vec2 outPayVector = new Vec2();
        public float outPayRotation;

        public float time, speedScl;
        public @Nullable Vec2 commandPos;
        public @Nullable UnitCommand command;  //指令（指挥模式用）
        public static Seq<UnlockableContent> allPayloadTypes = new Seq<>();  //可接受载荷列表（用于显示工厂中的载荷）
        public float attrsum; //环境加成
        public int seed;  //分离器模式种子








        private void updateAttrsum() {
            Recipe rec = getCurrentRecipe();
            if (rec != null && rec.attribute != null) {
                attrsum = sumAttribute(rec.attribute, tile.x, tile.y);
            } else {
                attrsum = 0f;
            }
        }


        public float efficiencyMultiplier() {
            Recipe rec = getCurrentRecipe();
            if (rec == null || rec.attribute == null) return 1f;
            return rec.baseEfficiency + Math.min(rec.maxBoost, rec.boostScale * attrsum) + rec.attribute.env();
        }


        @Override
        public void created() {
            seed = Mathf.randomSeed(tile.pos(), 0, Integer.MAX_VALUE - 1);
        }

        @Override
        public void placed() {
            super.placed();
            syncLiquidOutputs();
            updateAttrsum();
        }

        private void syncLiquidOutputs() {
            Recipe rec = getCurrentRecipe();
            if (rec != null && rec.outputLiquids.length > 0) {

                outputLiquids = rec.outputLiquids;

                liquidOutputDirections = rec.liquidOutputDirections;
            } else {

                outputLiquids = null;
                liquidOutputDirections = new int[]{-1};
            }
        }

        public float heatEfficiencyScale() {
            Recipe rec = getCurrentRecipe();
            if (rec == null && selectRecipe > 0) {
                int idx = selectRecipe - 1;
                if (idx < recipes.size) rec = recipes.get(idx);
            }
            if (rec == null || rec.inputHeat <= 0) return heatInput > 0.001f ? 1f : 0f;
            float req = rec.inputHeat;
            float over = Math.max(heatInput - req, 0f);
            return Math.min(Mathf.clamp(heatInput / req) + over / req * overheatScale, rec.maxEfficiency);
        }

        @Override @Nullable
        public PayloadSeq getPayloads() {
            boolean has = payload != null || !payloadCounts.isEmpty() || outputPayload != null;
            if (!has) return null;
            PayloadSeq seq = new PayloadSeq();
            if (payload != null) seq.add(payload.content(), 1);
            for (IntMap.Entry<Integer> e : payloadCounts.entries()) {
                UnlockableContent c = content.block(e.key);
                if (c == null) c = content.unit(e.key);
                if (c != null) seq.add(c, e.value);
            }
            if (outputPayload != null) seq.add(outputPayload.content(), 1);
            return seq;
        }

        @Override
        public void buildConfiguration(Table table) {
            table.clearChildren();
            TextButton autoBtn = new TextButton("自动", Styles.flatTogglet);
            autoBtn.setChecked(selectRecipe == 0);
            autoBtn.update(() -> autoBtn.setChecked(selectRecipe == 0));
            autoBtn.changed(() -> { if (selectRecipe != 0) configure(0); });
            table.add(autoBtn).height(40f).growX().pad(4f).row();
            table.image().color(Pal.gray).height(2f).growX().padBottom(4f).row();

            Table recipeList = new Table().left();
            for (int i = 0; i < recipes.size; i++) {
                final int recipeNum = i + 1;
                Recipe rec = recipes.get(i);

                rec.ensureArrays();


                Block unlock = rec.unlockBlock;
                if (unlock != null && unlock.isBanned()) {
                    continue;
                }
                boolean locked = unlock != null && !unlock.unlockedNow();

                TextButton recipeBtn = new TextButton("", Styles.flatTogglet);
                recipeBtn.setChecked(selectRecipe == recipeNum);
                if (locked) {
                    recipeBtn.setDisabled(true);
                    recipeBtn.add("[gray]" + Iconc.lock + " " + recipeNum).growX().pad(2f);
                }else {
                    Table content = new Table().left();
                    content.add("[accent]" + recipeNum + "[]").padLeft(6f).padRight(8f).width(24f);

                    Table inputTable = new Table().left();
                    if (rec.inputItems.length > 0 || rec.inputLiquids.length > 0
                            || rec.cachedInputPayloads.length > 0 || rec.inputPower > 0 || rec.inputHeat > 0) {
                        for (ItemStack s : rec.inputItems) inputTable.image(s.item.uiIcon).size(24f).padRight(2f);
                        for (LiquidStack s : rec.inputLiquids) inputTable.image(s.liquid.uiIcon).size(24f).padRight(2f);
                        for (PayloadStack s : rec.cachedInputPayloads) inputTable.image(s.item.uiIcon).size(24f).padRight(2f);
                        if (rec.inputPower > 0) inputTable.add("[accent]" + Iconc.power + "[]").padRight(2f);
                        if (rec.inputHeat > 0) inputTable.add("[red]" + Iconc.waves + "[]").padRight(2f);
                    } else inputTable.add("[darkGray]-[]");
                    content.add(inputTable).padRight(8f);
                    content.add("[accent]" + Iconc.right + "[]").padRight(8f).padLeft(4f);

                    Table outputTable = new Table().left();
                    if (rec.outputItems.length > 0 || rec.outputLiquids.length > 0
                            || rec.cachedOutputPayloads.length > 0 || rec.outputPower > 0 || rec.outputHeat > 0
                            || rec.randomResults != null) {
                        for (ItemStack s : rec.outputItems) outputTable.image(s.item.uiIcon).size(24f).padRight(2f);
                        if (rec.randomResults != null) {
                            for (ItemStack stack : rec.randomResults) {
                                outputTable.image(stack.item.uiIcon).size(24f).padRight(2f);
                            }
                        }
                        for (LiquidStack s : rec.outputLiquids) outputTable.image(s.liquid.uiIcon).size(24f).padRight(2f);
                        for (PayloadStack s : rec.cachedOutputPayloads) outputTable.image(s.item.uiIcon).size(24f).padRight(2f);
                        if (rec.outputPower > 0) outputTable.add("[accent]" + Iconc.power + "[]").padRight(2f);
                        if (rec.outputHeat > 0) outputTable.add("[red]" + Iconc.waves + "[]").padRight(2f);
                    }
                    content.add(outputTable);
                    recipeBtn.add(content).growX().pad(2f);
                }

                recipeBtn.changed(() -> configure(selectRecipe == recipeNum ? 0 : recipeNum));
                recipeBtn.update(() -> recipeBtn.setChecked(selectRecipe == recipeNum));
                recipeList.add(recipeBtn).growX().height(44f).padBottom(3f).row();
            }


            ScrollPane pane = new ScrollPane(recipeList, Styles.smallPane);
            pane.setScrollingDisabled(true, false);
            pane.setOverscroll(false, false);
            pane.setFadeScrollBars(false);
            pane.setClamp(true);
            pane.setCancelTouchFocus(true);
            pane.setFlickScroll(true);
            table.add(pane).growX().maxHeight(Math.min(recipes.size, 3) * 48f).pad(4f);
            buildCommandUI(table);

        }

        private void buildCommandUI(Table parent) {
            // 检查当前配方是否产出可指挥单位
            if (!isCommandable()) return;

            parent.row();
            Table commands = new Table();
            commands.top().left();

            Runnable rebuildCommands = () -> {
                commands.clear();
                UnitType unitType = getCommandableUnitType();
                if (unitType != null) {
                    commands.background(Styles.black6);
                    var group = new ButtonGroup<ImageButton>();
                    group.setMinCheckCount(0);
                    int cols = 3;
                    var list = unitType.commands;
                    commands.image(Tex.whiteui, Pal.gray).height(4f).growX().colspan(cols).row();

                    for (int i = 0; i < list.size; i++) {
                        UnitCommand cmd = list.get(i);
                        ImageButton button = commands.button(cmd.getIcon(), Styles.clearNoneTogglei, 40f, () -> {
                            configure(cmd);
                        }).tooltip(cmd.localized()).group(group).get();

                        button.update(() -> button.setChecked(command == cmd || (command == null && unitType.defaultCommand == cmd)));
                        if (i % cols == cols - 1) {
                            commands.row();
                        }
                    }
                }
            };
            rebuildCommands.run();
            parent.add(commands).fillX().left().padTop(4f);
        }

        public boolean isCommandable() {
            Recipe rec = currentVisualRecipe;
            if (rec != null && rec.cachedOutputPayloads != null) {
                for (PayloadStack stack : rec.cachedOutputPayloads) {
                    if (stack.item instanceof UnitType ut && ut.commands.size > 0) {
                        return true;
                    }
                }
            }
            return false;
        }

        public Vec2 getCommandPosition() {
            return commandPos;
        }

        public void onCommand(Vec2 target) {
            commandPos = target;
        }

        @Nullable
        private UnitType getCommandableUnitType() {
            Recipe rec = getCurrentRecipe();
            if (rec != null && rec.cachedOutputPayloads != null) {
                for (PayloadStack stack : rec.cachedOutputPayloads) {
                    if (stack.item instanceof UnitType ut && ut.commands.size > 0) {
                        return ut;
                    }
                }
            }
            return null;
        }




        @Override public Object config() { return selectRecipe; }


        @Override
        public void configured(Unit builder, Object value) {
            if (value instanceof Integer val && val >= 0 && val <= recipes.size) {
                if (selectRecipe != val) {
                    selectRecipe = val;
                    progress = 0f;
                    outputLiquidsSet.clear();
                    currentVisualRecipe = (selectRecipe != 0) ? recipes.get(selectRecipe - 1) : null;
                    Recipe newRec = selectRecipe > 0 && selectRecipe - 1 < recipes.size ? recipes.get(selectRecipe - 1) : null;
                    Effect switchEff = (newRec != null && newRec.cachedSwitchEffect != null) ? newRec.cachedSwitchEffect : Fx.rotateBlock;
                    switchEff.at(x, y, block.size);


                    outputPayloadsSet.clear();
                    if (outputPayload != null) {
                        if (!outputPayload.dump()) {

                            outputPayload = null;
                        }
                        outPayVector.setZero();
                    }


                    if (newRec != null) {
                        ObjectSet<UnlockableContent> validInputs = new ObjectSet<>();
                        for (PayloadStack s : newRec.cachedInputPayloads) {
                            validInputs.add(s.item);
                        }
                        Seq<UnlockableContent> toRemove = new Seq<>();
                        for (IntMap.Entry<Integer> e : payloadCounts.entries()) {
                            UnlockableContent c = content.block(e.key);
                            if (c == null) c = content.unit(e.key);
                            if (c != null && !validInputs.contains(c)) {
                                toRemove.add(c);
                            }
                        }
                        for (UnlockableContent c : toRemove) {
                            payloadCounts.remove(c.id);
                        }
                    } else {

                        payloadCounts.clear();
                    }
                    syncLiquidOutputs();
                    updateAttrsum();
                }
            }
            else if (value instanceof UnitCommand cmd) {

                this.command = cmd;
            }
        }

        @Override public double sense(LAccess sensor) {
            return sensor == LAccess.config ? selectRecipe : super.sense(sensor);
        }
        @Override public Object senseObject(LAccess sensor) {
            return sensor == LAccess.config ? Senseable.noSensed : super.senseObject(sensor);
        }
        @Override public void control(LAccess type, double p1, double p2, double p3, double p4) {
            if (type == LAccess.config) {
                int val = (int) Math.round(p1);
                if (val >= 0 && val <= recipes.size) {
                    selectRecipe = val;
                    progress = 0f;
                    outputLiquidsSet.clear();
                    syncLiquidOutputs();
                    updateAttrsum();
                }
                return;
            }
            super.control(type, p1, p2, p3, p4);
        }

        @Nullable public Recipe getCurrentRecipe() {
            if (selectRecipe == 0) {
                for (Recipe rec : recipes) {
                    // 过滤被禁用的配方
                    if (rec.unlockBlock != null && rec.unlockBlock.isBanned()) continue;
                    // 过滤未解锁的配方
                    if (rec.unlockBlock != null && !rec.unlockBlock.unlockedNow()) continue;
                    // 原有的热量/电力/输入输出检查
                    if (rec.inputHeat > 0 && heatInput <= 0f) continue;
                    if (rec.inputPower > 0 && (power == null || power.status <= 0f)) continue;
                    if (checkInput(rec) && checkOutput(rec)) return rec;
                }
                return null;
            } else {
                int idx = selectRecipe - 1;
                if (idx >= 0 && idx < recipes.size) {
                    Recipe rec = recipes.get(idx);
                    // 如果当前选中的配方被禁用，返回 null
                    if (rec.unlockBlock != null && rec.unlockBlock.isBanned()) return null;
                    // 如果未解锁，也返回 null
                    if (rec.unlockBlock != null && !rec.unlockBlock.unlockedNow()) return null;
                    if (checkInput(rec) && checkOutput(rec)) return rec;
                }
                return null;
            }
        }

        private boolean checkInput(Recipe rec) {
            if (rec.inputPower > 0 && (power == null || power.status <= 0f)) return false;
            if (rec.inputHeat > 0 && heatInput <= 0f) return false;
            for (ItemStack stack : rec.inputItems) if (items.get(stack.item) < stack.amount) return false;
            for (PayloadStack stack : rec.cachedInputPayloads) if (getPayloadCount(stack.item) < stack.amount) return false;
            for (LiquidStack stack : rec.inputLiquids) if (liquids.get(stack.liquid) <= 0.001f) return false;
            return true;
        }

        private boolean checkOutput(Recipe rec) {

            for (ItemStack stack : rec.outputItems) {
                if (items.get(stack.item) + stack.amount > itemCapacity) {
                    return false;
                }
            }


            if (rec.outputLiquids.length > 0) {
                boolean dumpExtra = rec.dumpExtraLiquid != null ? rec.dumpExtraLiquid : dumpExtraLiquid;
                boolean ignoreFull = rec.ignoreLiquidFullness != null ? rec.ignoreLiquidFullness : ignoreLiquidFullness;

                if (!ignoreFull) {
                    boolean allFull = true;
                    for (LiquidStack output : rec.outputLiquids) {
                        float inputAmount = 0f;
                        for (LiquidStack input : rec.inputLiquids) {
                            if (input.liquid == output.liquid) {
                                inputAmount = input.amount;
                                break;
                            }
                        }
                        float netOutput = output.amount - inputAmount;
                        if (netOutput > 0) {
                            if (liquids.get(output.liquid) < liquidCapacity - 0.001f) {
                                allFull = false;
                                break;
                            } else if (!dumpExtra) {
                                // 不允许排空多余液体，且液体已满
                                return false;
                            }
                        } else {
                            allFull = false;
                        }
                    }
                    if (allFull) {
                        return false;
                    }
                }
            }

            // 3. 载荷输出检查
            if (rec.cachedOutputPayloads.length > 0) {
                boolean hasUnitOutput = false;
                int willProduceBlocks = 0;

                for (PayloadStack stack : rec.cachedOutputPayloads) {
                    if (stack.item instanceof UnitType ut) {

                        hasUnitOutput = true;
                    } else {
                        // 方块载荷
                        willProduceBlocks += stack.amount;
                    }
                }

                // 如果配方输出单位，且 outputPayload 已存在，则不能继续生产
                if (hasUnitOutput && outputPayload != null) {
                    return false;
                }

                // 方块载荷容量检查（使用全局 payloadCapacity）
                int currentBlocks = 0;
                for (IntMap.Entry<Integer> e : payloadCounts.entries()) {
                    UnlockableContent c = content.block(e.key);
                    if (c == null) c = content.unit(e.key);
                    if (c != null && outputPayloadsSet.contains(c)) {
                        currentBlocks += e.value;
                    }
                }
                if (currentBlocks + willProduceBlocks > payloadCapacity) {
                    return false;
                }
            }

            return true;
        }

        public int getPayloadCount(UnlockableContent content) {
            return payloadCounts.get(content.id, 0);
        }

        public void addPayload(UnlockableContent content, int amount) {
            payloadCounts.put(content.id, getPayloadCount(content) + amount);
        }

        public void removePayload(UnlockableContent content, int amount) {
            int cur = getPayloadCount(content);
            if (cur <= amount) payloadCounts.remove(content.id); else payloadCounts.put(content.id, cur - amount);
        }

        // 热量计算
        @Override public float calculateHeat(float[] sideHeat) {
            return calculateHeat(sideHeat, null);
        }


        @Override public float calculateHeat(float[] sideHeat, IntSet cameFrom) {
            java.util.Arrays.fill(sideHeat, 0f);
            if (cameFrom != null) cameFrom.clear();
            float heat = 0f; IntSet visited = new IntSet(); visited.add(id());
            for (Building b : proximity) {
                if (b == null || b.team != team || !(b instanceof HeatBlock heater)) continue;
                if (b == this || visited.contains(b.id)) continue;
                visited.add(b.id);
                boolean split = b.block instanceof HeatConductor c && c.splitHeat;
                if (!b.block.rotate || !split && (relativeTo(b) + 2) % 4 == b.rotation || split && relativeTo(b) != b.rotation) {
                    float diff = Math.min(Math.abs(b.x - x), Math.abs(b.y - y)) / 8f;
                    int pts = Math.min((int)(block.size/2f + b.block.size/2f - diff), Math.min(b.block.size, block.size));
                    float add = heater.heat() / b.block.size * pts;
                    if (split) add /= 3f;
                    int dir = Mathf.mod(relativeTo(b), 4);
                    if (dir >= 0) { sideHeat[dir] += add; heat += add; }
                    if (heater instanceof HeatConductor.HeatConductorBuild hc) hc.updateHeat();
                }
            }
            return heat;
        }

        // 载荷输入
        @Override public boolean acceptPayload(Building source, Payload p) {
            if (!hasPayloads || this.payload != null) return false;
            for (Recipe rec : recipes)
                for (PayloadStack stack : rec.cachedInputPayloads)
                    if (stack.item == p.content()) return getPayloadCount(stack.item) < payloadCapacity;
            return false;
        }

        @Override public void handlePayload(Building source, Payload p) {
            this.payload = p;
            payVector.set(source).sub(this).clamp(-size*tilesize/2f, -size*tilesize/2f, size*tilesize/2f, size*tilesize/2f);
            payRotation = p.rotation();
            updatePayload();
        }

        @Override public Payload getPayload() {
            return payload;
        }

        @Override public Payload takePayload() {
            Payload t = payload;
            payload = null;
            return t;
        }

        public boolean moveInPayload() {
            return moveInPayload(true);
        }

        public boolean moveInPayload(boolean rotate) {
            if (payload == null) return false;
            updatePayload();
            if (rotate) payRotation = Mathf.approachDelta(payRotation, block.rotate ? rotdeg() : 90f, payloadRotateSpeed * delta());
            payVector.approach(Vec2.ZERO, payloadSpeed * delta());
            if (payVector.isZero(0.01f)) {
                addPayload(payload.content(), 1);
                payload = null;
                return true;
            }
            return false;
        }

        public void updatePayload() {
            if (payload != null) payload.set(x + payVector.x, y + payVector.y, payRotation);
        }

        public void moveOutPayload() {
            if (outputPayload == null) return;

            Payload p = outputPayload;
            p.update(null, this);

            int outDir = rotation;
            float targetAngle = outDir * 90f;
            Vec2 dest = Tmp.v1.trns(targetAngle, size * tilesize / 2f);

            // 使用独立的输出移动状态
            outPayRotation = Angles.moveToward(outPayRotation, targetAngle, payloadRotateSpeed * delta());
            outPayVector.approach(dest, payloadSpeed * delta());
            p.set(x + outPayVector.x, y + outPayVector.y, outPayRotation);

            float checkOffset = size * tilesize / 2f + 0.1f;
            float wx = x + Angles.trnsx(targetAngle, checkOffset);
            float wy = y + Angles.trnsy(targetAngle, checkOffset);
            Tile exitTile = world.tileWorld(wx, wy);

            boolean canMove = false, canDump = false;
            Building front = null;

            if (exitTile != null) {
                front = exitTile.build;
                canMove = front != null && front != this && (front.block.outputsPayload || front.block.acceptsPayload);
                canDump = front == null && !exitTile.solid();
            }

            if (canDump && !canMove) {
                PayloadBlock.pushOutput(p, 1f - (outPayVector.dst(dest) / (size * tilesize / 2f)));
            }

            if (outPayVector.within(dest, 0.001f)) {
                outPayVector.clamp(-size * tilesize / 2f, -size * tilesize / 2f,
                        size * tilesize / 2f, size * tilesize / 2f);

                if (canMove) {
                    p.set(wx, wy, targetAngle);
                    if (movePayload(p)) {
                        outputPayload = null;
                        outPayVector.setZero();
                    }
                } else if (canDump) {
                    p.set(wx, wy, targetAngle);
                    if (p.dump()) {
                        outputPayload = null;
                        outPayVector.setZero();
                    }
                }
            }
        }


        @Override
        public void pickedUp() {
            carried = true;
            attrsum = 0f;
            warmup = 0f;
        }

        @Override public void drawTeamTop() {
            carried = false;
        }

        @Override public void onRemoved() {
            super.onRemoved();
            if (payload != null && !carried) payload.dump();
        }

        @Override public void onDestroyed() {
            if (payload != null) payload.destroyed();
            super.onDestroyed();
        }

        @Override public void onProximityAdded() {
            super.onProximityAdded();
            for (Building o : proximity)
                if (o instanceof Conduit.ConduitBuild) o.onProximityUpdate();
        }

        @Override
        public void onProximityUpdate() {
            super.onProximityUpdate();
            Recipe rec = getCurrentRecipe();
            if (rec != null && rec.attribute != null) {
                attrsum = sumAttribute(rec.attribute, tile.x, tile.y);
            }
        }

        @Override
        public void updateTile() {
            if (payload != null) { payload.update(null, this); moveInPayload(); }

            heat = calculateHeat(sideHeat);
            heatInput = heat;
            Recipe active = getCurrentRecipe();
            if (active != null && active.heatOutputDirections != null) {
                boolean allDirections = false;
                for (int d : active.heatOutputDirections) {
                    if (d == -1) { allDirections = true; break; }
                }
                if (!allDirections) {
                    for (int i = 0; i < 4; i++) {
                        boolean allowed = false;
                        for (int d : active.heatOutputDirections) {

                            if (Mathf.mod(d + rotation, 4) == i) { allowed = true; break; }
                        }
                        if (!allowed) sideHeat[i] = 0f;
                    }

                    float total = 0f;
                    for (float s : sideHeat) total += s;
                    this.heat = total;
                    this.heatInput = total;
                }
            }


            if (selectRecipe == 0) {
                if (active != null && active != currentVisualRecipe) {
                    currentVisualRecipe = active;
                    outputLiquidsSet.clear();
                    syncLiquidOutputs();
                    updateAttrsum();
                }
                else if (active == null && currentVisualRecipe == null)
                    for (Recipe r : recipes) if (r.visual != null) { currentVisualRecipe = r; break; }
            }

            float totalRatio = active != null ? efficiencyScale() : 0f;
            boolean canProduce = totalRatio > 0.001f;
            float targetHeat = canProduce && active.outputHeat > 0 ? active.outputHeat * efficiencyScale() : 0f;
            heatOutput = Mathf.approachDelta(heatOutput, targetHeat, warmupRate * delta());

            if (canProduce) {
                for (LiquidStack stack : active.inputLiquids) {
                    float consume = stack.amount * delta() * totalRatio;
                    if (active != null && active.scaleLiquidConsumption) consume *= efficiencyMultiplier();
                    if (consume > 0) liquids.remove(stack.liquid, consume);
                }
                progress += delta() / active.craftTime * totalRatio;
                warmup = Mathf.lerpDelta(warmup, warmupTarget(), warmupRate);
                if (active.outputLiquids.length > 0) {
                    float inc = delta() * totalRatio;
                    for (LiquidStack out : active.outputLiquids) {
                        float amt = out.amount * inc;
                        if (amt > 0) {
                            float canAdd = Math.min(amt, liquidCapacity - liquids.get(out.liquid));
                            if (canAdd > 0) { handleLiquid(this, out.liquid, canAdd); outputLiquidsSet.add(out.liquid); }
                        }
                    }
                }
                if (active.cachedUpdateSound != null && active.cachedUpdateSound != Sounds.none && wasVisible
                        && Mathf.chanceDelta(active.updateEffectChance > 0 ? active.updateEffectChance : updateEffectChance))
                    active.cachedUpdateSound.at(x, y);
                Effect eff = active.cachedUpdateEffect != null && active.cachedUpdateEffect != Fx.none ? active.cachedUpdateEffect : updateEffect;
                if (wasVisible && eff != Fx.none && Mathf.chanceDelta(updateEffectChance))
                    eff.at(x + Mathf.range(size * 4), y + Mathf.range(size * 4), 0f, Color.white);
                if (progress >= 1f) craft(active);
            } else warmup = Mathf.lerpDelta(warmup, 0f, warmupRate);

            boolean unitRecipe = false;
            if (active != null)
                for (PayloadStack s : active.cachedOutputPayloads) if (s.item instanceof UnitType) { unitRecipe = true; break; }
            if (canProduce && unitRecipe) {
                time += edelta() * speedScl * Vars.state.rules.unitBuildSpeed(team);
                speedScl = Mathf.lerpDelta(speedScl, 1f, 0.05f);
            } else speedScl = Mathf.lerpDelta(speedScl, 0f, 0.05f);

            moveOutPayload();
            dumpOutputs();
            totalProgress += warmup * Time.delta;
        }

        public void craft(Recipe rec) {
            for (ItemStack stack : rec.inputItems) items.remove(stack.item, stack.amount);
            for (PayloadStack stack : rec.cachedInputPayloads) removePayload(stack.item, stack.amount);
            if (rec.randomResults != null) {
                int sum = 0;
                for (ItemStack stack : rec.randomResults) sum += stack.amount;
                if (sum > 0) {
                    int i = Mathf.randomSeed(seed++, 0, sum - 1);
                    int count = 0;
                    Item item = null;
                    for (ItemStack stack : rec.randomResults) {
                        if (i >= count && i < count + stack.amount) {
                            item = stack.item;
                            break;
                        }
                        count += stack.amount;
                    }
                    if (item != null && items.get(item) < itemCapacity) {
                        offload(item);
                        outputItemsSet.add(item);
                    }
                }
            } else {
                for (ItemStack stack : rec.outputItems) {
                    for (int i = 0; i < stack.amount; i++) {
                        offload(stack.item);
                    }
                    outputItemsSet.add(stack.item);
                }
            }
            for (PayloadStack stack : rec.cachedOutputPayloads) {
                for (int i = 0; i < stack.amount; i++) {
                    Payload p = createPayload(stack.item);
                    if (p instanceof UnitPayload up) {

                        Unit unit = up.unit;
                        if (unit.isCommandable()) {
                            if (commandPos != null) {
                                unit.command().commandPosition(commandPos);
                            }
                            unit.command().command(command == null && unit.type.defaultCommand != null ? unit.type.defaultCommand : command);
                        }

                        if (outputPayload != null) {

                            outputPayload = null;
                        }
                        outputPayload = p;
                        outPayVector.setZero();
                        outPayRotation = rotdeg();
                        Events.fire(new UnitCreateEvent(((UnitPayload)p).unit, this));

                    } else {
                        // 方块载荷：放入库存并标记
                        addPayload(stack.item, 1);
                        outputPayloadsSet.add(stack.item);
                    }
                }
            }

            progress = 0f;
            if (wasVisible) {
                Effect effect = rec.cachedCraftEffect != null && rec.cachedCraftEffect != Fx.none ? rec.cachedCraftEffect : craftEffect;
                effect.at(x, y);
                if (rec.cachedCraftSound != null && rec.cachedCraftSound != Sounds.none) rec.cachedCraftSound.at(x, y);
            }
        }

        public void dumpOutputs() {
            for (Item item : outputItemsSet) {
                while (items.has(item) && dump(item)) {}
                if (!items.has(item)) outputItemsSet.remove(item);
            }

            Seq<Liquid> liquidsToDump = new Seq<>();
            if (selectRecipe == 0) liquidsToDump.addAll(outputLiquidsSet);
            else {
                int idx = selectRecipe - 1;
                if (idx >= 0 && idx < recipes.size) {
                    Recipe cur = recipes.get(idx);
                    for (LiquidStack s : cur.outputLiquids) liquidsToDump.add(s.liquid);
                }
            }
            Recipe activeRecipe = getCurrentRecipe();
            for (Liquid liquid : liquidsToDump) {
                if (liquids.get(liquid) > 0.001f) {
                    int dir = -1;
                    if (activeRecipe != null && activeRecipe.outputLiquids.length > 0)
                        for (int i = 0; i < activeRecipe.outputLiquids.length; i++)
                            if (activeRecipe.outputLiquids[i].liquid == liquid && i < activeRecipe.liquidOutputDirections.length)
                            { dir = activeRecipe.liquidOutputDirections[i]; break; }
                    dumpLiquid(liquid, 2f, dir);
                }
            }
            if (selectRecipe == 0) {
                Seq<Liquid> toRemove = new Seq<>();
                for (Liquid l : outputLiquidsSet) if (liquids.get(l) <= 0.001f) toRemove.add(l);
                for (Liquid l : toRemove) outputLiquidsSet.remove(l);
            }

            // 方块载荷放置
            Seq<UnlockableContent> toRemove = new Seq<>();
            for (UnlockableContent c : outputPayloadsSet) {
                while (getPayloadCount(c) > 0) {
                    Payload p = createPayload(c);
                    if (p != null && dumpPayload(p)) {
                        removePayload(c, 1);
                    } else {
                        break;
                    }
                }
                if (getPayloadCount(c) == 0) toRemove.add(c);
            }
            for (UnlockableContent c : toRemove) {
                outputPayloadsSet.remove(c);
            }
        }

        private Payload createPayload(UnlockableContent c) {
            if (c instanceof Block b) return new BuildPayload(b, team);
            else if (c instanceof UnitType u) return new UnitPayload(u.create(team));
            return null;
        }

        @Override public boolean shouldConsume() {
            if (selectRecipe == 0) { for (Recipe r : recipes) if (checkOutput(r)) return enabled; return false; }
            else {
                int idx = selectRecipe - 1;
                return idx >= 0 && idx < recipes.size && checkOutput(recipes.get(idx)) && enabled;
            }
        }

        @Override public float warmupTarget() {
            Recipe rec = getCurrentRecipe();
            if (rec != null && rec.inputHeat > 0) { if (heatRequirement() <= 0) return 1f; return Mathf.clamp(heatInput / heatRequirement()); }
            return 1f;
        }

        @Override public float efficiencyScale() {
            if (!enabled) return 0f;
            Recipe rec = getCurrentRecipe();
            if (rec == null) return 0f;
            float scale = 1f;
            if (rec.inputHeat > 0) {
                float req = rec.inputHeat, over = Math.max(heatInput - req, 0);
                scale = Math.min(Mathf.clamp(heatInput/req) + over/req*overheatScale, rec.maxEfficiency);
            }
            if (rec.inputPower > 0) scale *= power == null ? 0f : power.status;
            for (LiquidStack stack : rec.inputLiquids) {
                float have = liquids.get(stack.liquid), need = stack.amount * delta() * scale;
                if (need > 0) scale *= Math.min(have/need, 1f);
            }
            for (ItemStack stack : rec.inputItems) if (items.get(stack.item) < stack.amount) return 0f;
            for (PayloadStack stack : rec.cachedInputPayloads) if (getPayloadCount(stack.item) < stack.amount) return 0f;
            scale *= efficiencyMultiplier();
            return scale;
        }

        @Override
        public float getProgressIncrease(float base) {
            return super.getProgressIncrease(base) * efficiencyMultiplier();
        }

        @Override
        public float heatRequirement() {
            Recipe rec = getCurrentRecipe();
            return rec == null ? 0f : rec.inputHeat;
        }

        @Override
        public float heat() {
            Recipe rec = getCurrentRecipe();
            return (rec != null && rec.outputHeat > 0 && heatOutput > 0.001f) ? heatOutput : 0f;
        }

        @Override
        public float heatFrac() {
            Recipe rec = getCurrentRecipe();
            return (rec != null && rec.outputHeat > 0) ? heatOutput/rec.outputHeat : 0f;
        }

        @Override
        public float getPowerProduction() {
            Recipe rec = getCurrentRecipe();
            return (rec != null && rec.outputPower > 0) ? rec.outputPower * efficiencyScale() : 0f;
        }
        public float getPowerStat() {
            Recipe rec = getCurrentRecipe();
            return (rec != null && rec.outputPower > 0) ? efficiencyScale() : 0f;
        }

        @Nullable public UnitType getDisplayedUnitType() {
            if (outputPayload instanceof UnitPayload up) return up.unit.type;
            Recipe rec = getCurrentRecipe();
            if (rec != null) for (PayloadStack s : rec.cachedOutputPayloads) if (s.item instanceof UnitType) return (UnitType) s.item;
            return null;
        }

        @Override public boolean acceptItem(Building source, Item item) {
            if (items.get(item) >= itemCapacity) return false;
            return recipes.contains(r -> { for (ItemStack s : r.inputItems) if (s.item == item) return true; return false; });
        }
        @Override public boolean acceptLiquid(Building source, Liquid liquid) {
            return recipes.contains(r -> { for (LiquidStack s : r.inputLiquids) if (s.liquid == liquid) return true; return false; });
        }
        @Override public void handleItem(Building source, Item item) { items.add(item, 1); }
        @Override public int acceptStack(Item item, int amount, Teamc source) { return Math.min(amount, itemCapacity - items.get(item)); }
        @Override public void handleStack(Item item, int amount, Teamc source) { items.add(item, amount); }

        @Override
        public void draw() {
            drawer.draw(this);
            if (currentVisualRecipe != null && currentVisualRecipe.visual != null) currentVisualRecipe.visual.draw(this);
            drawPayload();
            Recipe active = getCurrentRecipe();
            if (active != null && progress > 0) {
                for (PayloadStack s : active.cachedOutputPayloads) {
                    if (s.item instanceof UnitType unit) {
                        Draw.draw(Layer.blockOver, () -> Drawf.construct(this, unit, rotdeg() - 90f, progress , speedScl, time));
                        break;
                    }
                }
            }
            if (outputPayload != null) {
                Draw.z(Layer.blockOver);
                outputPayload.draw();
            }

            drawStatus();
        }

        public void drawPayload() { if (payload != null) { updatePayload(); Draw.z(Layer.blockOver); payload.draw(); } }

        @Override public void drawStatus() {
            if (block.enableDrawStatus) {
                float m = block.size > 1 ? 1f : 0.64f;
                float bx = x + block.size*8f/2f - 8f*m/2f, by = y - block.size*8f/2f + 8f*m/2f;
                Draw.z(71f); Draw.color(Pal.gray); Fill.square(bx, by, 2.5f*m, 45f);
                Draw.color(status().color); Fill.square(bx, by, 1.5f*m, 45f); Draw.color();
            }
        }
        @Override public BlockStatus status() {
            if (!enabled) return BlockStatus.logicDisable;
            if (!shouldConsume()) return BlockStatus.noOutput;
            Recipe rec = getCurrentRecipe();
            if (rec == null) return BlockStatus.noInput;
            if (efficiencyScale() > 0 && productionValid())
                return Vars.state.tick / 30.0 % 1.0 < efficiencyScale() ? BlockStatus.active : BlockStatus.noInput;
            return BlockStatus.noInput;
        }

        @Override
        public byte version() {
            return 2;
        }

        @Override
        public void write(Writes w) {
            super.write(w);
            w.i(selectRecipe);
            w.f(heatInput);
            w.f(heatOutput);
            w.s(outputItemsSet.size);
            for (Item item : outputItemsSet) w.s(item.id);
            w.s(outputLiquidsSet.size);
            for (Liquid l : outputLiquidsSet) w.s(l.id);
            w.s(outputPayloadsSet.size);
            for (UnlockableContent c : outputPayloadsSet) w.s(c.id);
            w.f(payVector.x);
            w.f(payVector.y);
            w.f(payRotation);
            Payload.write(payload, w);
            w.s(payloadCounts.size);
            for (IntMap.Entry<Integer> e : payloadCounts.entries()) {
                w.s(e.key);
                w.i(e.value);
            }

            w.f(time);
            w.f(speedScl);
            Payload.write(outputPayload, w);
            w.i(currentVisualRecipe == null ? -1 : recipes.indexOf(currentVisualRecipe));
            w.f(outPayVector.x);
            w.f(outPayVector.y);
            w.f(outPayRotation);
            TypeIO.writeVecNullable(w, commandPos);
            TypeIO.writeCommand(w, command);
            w.f(attrsum);
            w.i(seed);
        }

        @Override
        public void read(Reads r, byte rev) {
            super.read(r, rev);
            selectRecipe = r.i();
            selectRecipe = Mathf.clamp(selectRecipe, 0, recipes.size);
            heatInput = r.f();
            heatOutput = r.f();
            outputItemsSet.clear();
            int is = r.s();
            for (int i = 0; i < is; i++) outputItemsSet.add(content.item(r.s()));
            outputLiquidsSet.clear();
            int ls = r.s();
            for (int i = 0; i < ls; i++) outputLiquidsSet.add(content.liquid(r.s()));
            outputPayloadsSet.clear();
            int ps = r.s();
            for (int i = 0; i < ps; i++) {
                int id = r.s();
                UnlockableContent c = content.block(id);
                if (c == null) c = content.unit(id);
                if (c != null) outputPayloadsSet.add(c);
            }
            payVector.set(r.f(), r.f());
            payRotation = r.f();
            payload = Payload.read(r);
            payloadCounts.clear();
            int pcs = r.s();
            for (int i = 0; i < pcs; i++) {
                int id = r.s();
                int count = r.i();
                payloadCounts.put(id, count);
            }

            if (rev >= 1) {
                time = r.f();
                speedScl = r.f();
                outputPayload = Payload.read(r);
                int idx = r.i();
                currentVisualRecipe = (idx >= 0 && idx < recipes.size) ? recipes.get(idx) : null;
                outPayVector.set(r.f(), r.f());
                outPayRotation = r.f();
                commandPos = TypeIO.readVecNullable(r);
                command = TypeIO.readCommand(r);
                if (rev >= 2) {
                    attrsum = r.f();
                    seed = r.i();
                } else {
                    attrsum = 0f;
                    seed = 0;
                }
            } else {
                int idx = r.i();
                currentVisualRecipe = (idx >= 0 && idx < recipes.size) ? recipes.get(idx) : null;
                time = 0f;
                speedScl = 0f;
                outputPayload = null;
                outPayVector.setZero();
                outPayRotation = 0f;
                commandPos = null;
                command = null;
                attrsum = 0f;
                seed = 0;
            }

            syncLiquidOutputs();
        }
    }




    public static class Recipe {
        public ItemStack[] inputItems = {}, outputItems = {};
        public LiquidStack[] inputLiquids = {}, outputLiquids = {};
        public float inputPower, inputHeat, outputPower, outputHeat;
        public float craftTime = 60f;
        public int payloadOutputDirection = -1;
        public String[] inputPayloads = {}, outputPayloads = {};
        public @Nullable DrawBlock visual;
        public float maxEfficiency = 4f, updateEffectChance = 0.04f;
        public String craftEffect = "", updateEffect = "", switchEffect = "", craftSound = "", updateSound = "";
        public int[] liquidOutputDirections = {-1};
        public transient @Nullable Effect cachedCraftEffect, cachedUpdateEffect, cachedSwitchEffect;  //为了json使用原版字段而设置的内部反射查找参数
        public transient @Nullable Sound cachedCraftSound, cachedUpdateSound;
        public transient PayloadStack[] cachedInputPayloads = {}, cachedOutputPayloads = {};
        public Boolean dumpExtraLiquid = true;
        public Boolean ignoreLiquidFullness = false;
        public int[] heatOutputDirections = {-1};
        public @Nullable Attribute attribute;
        public float baseEfficiency = 1f;
        public float boostScale = 1f;
        public float maxBoost = 1f;
        public float minEfficiency = -1f;
        public boolean scaleLiquidConsumption = false;
        public boolean displayEfficiency = true;
        public boolean floating = false;

        public @Nullable String parent;
        public Seq<Objectives.Objective> objectives = new Seq<>();
        public @Nullable String databaseCategory;
        public @Nullable String databaseTag;


        public transient @Nullable Block unlockBlock;
        public @Nullable Boolean alwaysUnlocked;





        public @Nullable ItemStack[] randomResults;

        public Recipe() { ensureArrays(); }
        public void ensureArrays() {
            if (inputItems == null) inputItems = new ItemStack[0]; if (inputLiquids == null) inputLiquids = new LiquidStack[0];
            if (outputItems == null) outputItems = new ItemStack[0]; if (outputLiquids == null) outputLiquids = new LiquidStack[0];
            if (inputPayloads == null) inputPayloads = new String[0]; if (outputPayloads == null) outputPayloads = new String[0];
            if (cachedInputPayloads == null) cachedInputPayloads = new PayloadStack[0]; if (cachedOutputPayloads == null) cachedOutputPayloads = new PayloadStack[0];
            if (liquidOutputDirections == null) liquidOutputDirections = new int[]{-1};
            if (payloadOutputDirection < -1 || payloadOutputDirection > 3) payloadOutputDirection = -1;
            if (heatOutputDirections == null) heatOutputDirections = new int[]{-1};
        }
        public Recipe(ItemStack[] i, LiquidStack[] il, float ip, float ih, ItemStack[] o, LiquidStack[] ol, float op, float oh, float ct, PayloadStack[] ipay, PayloadStack[] opay, @Nullable DrawBlock v) {
            inputItems=i; inputLiquids=il; inputPower=ip; inputHeat=ih; outputItems=o; outputLiquids=ol; outputPower=op; outputHeat=oh;
            craftTime=ct; cachedInputPayloads=ipay; cachedOutputPayloads=opay; visual=v; inputPayloads=new String[0]; outputPayloads=new String[0]; ensureArrays();
        }
        public Recipe(ItemStack[] i, LiquidStack[] il, float ip, float ih, ItemStack[] o, LiquidStack[] ol, float op, float oh, float ct) { this(i,il,ip,ih,o,ol,op,oh,ct,new PayloadStack[0],new PayloadStack[0],null); }
        public Recipe(ItemStack[] i, LiquidStack[] il, float ip, float ih, ItemStack[] o, LiquidStack[] ol, float op, float oh, float ct, @Nullable DrawBlock v) { this(i,il,ip,ih,o,ol,op,oh,ct,new PayloadStack[0],new PayloadStack[0],v); }
        public Recipe(ItemStack[] i, LiquidStack[] il, float ip, float ih, ItemStack[] o, LiquidStack[] ol, float op, float oh, float ct, PayloadStack[] ipay, PayloadStack[] opay) { this(i,il,ip,ih,o,ol,op,oh,ct,ipay,opay,null); }
        public Recipe(ItemStack[] i, LiquidStack[] il, float ip, float ih, PayloadStack[] ipay, ItemStack[] o, LiquidStack[] ol, float op, float oh, PayloadStack[] opay, float ct, float maxE, DrawBlock v, String ce, String ue, String se, float uec, String cs, String us) {
            inputItems=i; inputLiquids=il; inputPower=ip; inputHeat=ih; cachedInputPayloads=ipay;
            outputItems=o; outputLiquids=ol; outputPower=op; outputHeat=oh; cachedOutputPayloads=opay;
            craftTime=ct; maxEfficiency=maxE; visual=v;
            craftEffect=ce; updateEffect=ue; switchEffect=se; updateEffectChance=uec; craftSound=cs; updateSound=us;
            inputPayloads=new String[0]; outputPayloads=new String[0]; ensureArrays();
        }
    }
}