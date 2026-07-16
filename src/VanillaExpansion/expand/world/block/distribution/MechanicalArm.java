package VanillaExpansion.expand.world.block.distribution;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import static arc.math.Angles.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.scene.event.*;
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

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import static mindustry.Vars.*;

/**
 * 机械臂方块
 * 继承RoboticArmBase，(其实可以不要)
 * 只需要电力即可工作
 * 多人游戏有机械臂数据被删的问题
 * UI需要汉化
 */
public class MechanicalArm extends RoboticArmBase {
    public static final float range = 12f * tilesize;
    public static final float warmupSpeed = 0.05f;
    
    public TextureRegion baseRegion;
    public TextureRegion lowerArmRegion;
    public TextureRegion upperArmRegion;
    public TextureRegion clawLeftRegion;
    public TextureRegion clawRightRegion;
    
    public MechanicalArm(String name){
        super(name);
        size = 2;
        update = true;
        solid = true;
        hasItems = true;
        configurable = true;
        saveConfig = true;
        itemCapacity = 10;
        noUpdateDisabled = true;
        consumePower(1f);
        
        requirements(Category.logic, ItemStack.with(
            Items.copper, 100,
            Items.lead, 75,
            Items.titanium, 50,
            Items.silicon, 25
        ));
        
        config(byte[].class, (MechanicalArmBuild build, byte[] data) -> {
            build.readConfig(data);
        });
        
        config(Integer.class, (MechanicalArmBuild build, Integer pos) -> {
            build.setOneLink(pos);
        });
    }
    
    @Override
    public void load() {
        super.load();
        baseRegion = Core.atlas.find(name + "-base", Core.atlas.find("block"));
        lowerArmRegion = Core.atlas.find(name + "-lower-arm", Core.atlas.find("block"));
        upperArmRegion = Core.atlas.find(name + "-upper-arm", Core.atlas.find("block"));
        clawLeftRegion = Core.atlas.find(name + "-claw-left", Core.atlas.find("block"));
        clawRightRegion = Core.atlas.find(name + "-claw-right", Core.atlas.find("block"));
    }
    
    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid) {
        super.drawPlace(x, y, rotation, valid);
        Drawf.dashSquare(Pal.accent, x * tilesize + offset, y * tilesize + offset, range * 2);
    }
    
    @Override
    public void setBars() {
        super.setBars();
        addBar("power", (MechanicalArmBuild entity) -> new Bar(
            () -> Core.bundle.get("bar.power") + ": " + (int)(entity.power.status * 100) + "%",
            () -> Pal.power,
            () -> entity.power.status
        ));
    }
    
    public class MechanicalArmBuild extends RoboticArmBuild {
        public Seq<Integer> inputs = new Seq<>();
        public Seq<Integer> outputs = new Seq<>();
        public ObjectMap<Integer, Boolean> linkModes = new ObjectMap<>(); // true为提取模式，false为推送模式
        public ObjectMap<Integer, Integer> clickCounts = new ObjectMap<>();
        public ObjectMap<Integer, Seq<Item>> linkItemFilters = new ObjectMap<>();
        public float warmup = 0f;
        public float baseAngle = 0f;
        public float lowerArmAngle = 0f;
        public float upperArmAngle = 0f;
        public float clawAngle = 0f;
        public float armExtend = 0.3f; // 手臂伸长值，平滑插值
        public float rotateSpeed = 0f;
        public boolean consValid = false;
        public boolean itemSent = false;
        public Interval timer = new Interval(6);
        public static final int MAX_LOOP = 50;
        public static final int FRAME_DELAY = 5;
        
        public int loopIndex = 0;
        // Phase: 0=(空闲), 1=(移动到输入), 2=(提取), 3=(移动到输出), 4=(放置)
        public int phase = 0;
        public int currentInputIndex = -1;
        public int currentOutputIndex = -1;
        public float progress = 0f; // 0到1的进度
        public float phaseTime = 0f; // 每个阶段的时间
        public int itemTransferAmount = 1; // 单次抓取的物品量，默认为1
        public ItemStack heldItem = new ItemStack();
        public Building currentTarget = null; // 当前目标方块
        
        public Seq<Integer> getInputs() {
            return inputs;
        }
        
        public void setInputs(Seq<Integer> v) {
            inputs = v;
        }
        
        public Seq<Integer> getOutputs() {
            return outputs;
        }
        
        public void setOutputs(Seq<Integer> v) {
            outputs = v;
        }
        
        public void setOneLink(int v) {
            Building linkTarget = world.build(v);
            if (linkTarget == null) return;
            
            boolean isInput = linkModes.get(v, false);
            int inputIndex = inputs.indexOf(v);
            int outputIndex = outputs.indexOf(v);
            
            if (inputIndex >= 0) {
                // 在输入列表中，第二次点击：从输入列表移除，添加到输出列表
                inputs.remove(inputIndex);
                outputs.add(v);
                linkModes.put(v, false);
                clickCounts.put(v, 0);
            } else if (outputIndex >= 0) {
                // 在输出列表中，第三次点击：删除链接
                outputs.remove(outputIndex);
                linkModes.remove(v);
                clickCounts.remove(v);
                linkItemFilters.remove(v);
            } else {
                // 不在任何列表中，第一次点击：添加到输入列表
                inputs.add(v);
                linkModes.put(v, true);
                clickCounts.put(v, 1);
                linkItemFilters.put(v, new Seq<>());
            }
        }
        
        public boolean linkValidTarget(Building the, Building target) {
            return target != null && target.team == the.team && 
                   Math.abs(target.x - the.x) <= range && Math.abs(target.y - the.y) <= range;
        }
        
        public boolean linkValid(Building the, int pos) {
            if (pos == -1) return false;
            Building linkTarget = world.build(pos);
            return linkValidTarget(the, linkTarget);
        }
        
        public boolean extractItem(Building target) {
            if (heldItem.item != null && heldItem.amount > 0) return false;
            if (target.items == null) return false;
            
            // 保证currentInputIndex在有效范围内
            if (currentInputIndex < 0 || currentInputIndex >= inputs.size) {
                return false;
            }
            
            int pos = inputs.get(currentInputIndex);
            Seq<Item> filter = linkItemFilters.get(pos, new Seq<>());
            
            if (filter.isEmpty()) {
                // 没有过滤器，检查所有物品
                for (Item item : content.items()) {
                    int count = target.items.get(item);
                    if (count > 0) {
                        int accept = Math.min(count, itemTransferAmount);
                        if (accept > 0) {
                            heldItem = new ItemStack(item, accept);
                            target.removeStack(item, accept);
                            return true;
                        }
                    }
                }
            } else {
                // 有过滤器，只检查过滤的物品
                for (Item item : filter) {
                    int count = target.items.get(item);
                    if (count > 0) {
                        int accept = Math.min(count, itemTransferAmount);
                        if (accept > 0) {
                            heldItem = new ItemStack(item, accept);
                            target.removeStack(item, accept);
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        
        public boolean depositItem(Building target) {
            if (heldItem.item == null || heldItem.amount <= 0) return false;
            if (target.items == null) return false;
            
            int accept = target.acceptStack(heldItem.item, heldItem.amount, this);
            if (accept > 0) {
                int depositAmount = Math.min(accept, heldItem.amount);
                target.handleStack(heldItem.item, depositAmount, this);
                heldItem = new ItemStack();
                return true;
            }
            return false;
        }
        
        @Override
        public void updateTile() {
            super.updateTile();
            
            consValid = power.status > 0.1f;
            if (consValid) {
                consume();
            }
            
            if (consValid) {
                warmup = Mathf.lerpDelta(warmup, (inputs.size > 0 || outputs.size > 0) ? 1 : 0, warmupSpeed);
            } else {
                warmup = Mathf.lerpDelta(warmup, 0, warmupSpeed);
            }
            
            if (!consValid) return;
            
            if (timer.get(1, FRAME_DELAY)) {
                itemSent = false;
                
                // 更新阶段时间
                phaseTime += Time.delta / 60f; // 转换为秒
                
                // 检查阶段超时（每个阶段最多执行10秒）
                if (phaseTime > 10f) {
                    // 阶段超时，重置到IDLE状态
                    phase = 0;
                    currentTarget = null;
                    currentInputIndex = -1;
                    currentOutputIndex = -1;
                    progress = 0f;
                    phaseTime = 0f;
                }
                
                switch (phase) {
                    case 0: // IDLE - 搜索输入
                        if (searchInput()) {
                            phase = 1; // 找到输入，开始移动
                            phaseTime = 0f; // 重置阶段时间
                        }
                        break;
                    case 1: // MOVE_TO_INPUT - 移动到输入
                        if (moveToTarget()) {
                            phase = 2; // 到达目标，开始提取
                            phaseTime = 0f; // 重置阶段时间
                        }
                        break;
                    case 2: // EXTRACT - 提取物品
                        if (extract()) {
                            phase = 3; // 提取成功，搜索输出
                            phaseTime = 0f; // 重置阶段时间
                        }
                        break;
                    case 3: // 搜索输出
                        if (searchOutput()) {
                            phase = 4; // 找到输出，开始移动
                            phaseTime = 0f; // 重置阶段时间
                        }
                        break;
                    case 4: // MOVE_TO_OUTPUT - 移动到输出
                        if (moveToTarget()) {
                            phase = 5; // 到达目标，开始放置
                            phaseTime = 0f; // 重置阶段时间
                        }
                        break;
                    case 5: // DEPOSIT - 放置物品
                        if (deposit()) {
                            phase = 0; // 放置成功，回到空闲
                            itemSent = true;
                            phaseTime = 0f; // 重置阶段时间
                        }
                        break;
                }
            }
            
            if (consValid) {
                rotateSpeed = Mathf.lerpDelta(rotateSpeed, itemSent ? 1 : 0, warmupSpeed);
            } else {
                rotateSpeed = Mathf.lerpDelta(rotateSpeed, 0, warmupSpeed);
            }
        }
        
        public boolean searchInput() {
            if (inputs.isEmpty()) {
                return false;
            }
            
            // 限制搜索次数，避免无限循环
            int searchCount = 0;
            int maxSearches = inputs.size * 2; // 最多搜索两次所有输入方块
            
            while (searchCount < maxSearches) {
                // 确保loopIndex在有效范围内
                if (inputs.isEmpty()) {
                    return false;
                }
                
                loopIndex--;
                if (loopIndex < 0) {
                    loopIndex = inputs.size - 1;
                } else if (loopIndex >= inputs.size) {
                    loopIndex = inputs.size - 1;
                }
                
                int index = loopIndex;
                if (index >= inputs.size) break;
                
                int pos = inputs.get(index);
                Building linkTarget = world.build(pos);
                if (!linkValidTarget(this, linkTarget)) {
                    searchCount++;
                    continue;
                }
                
                if (linkTarget.items != null) {
                    Seq<Item> filter = linkItemFilters.get(pos, new Seq<>());
                    
                    if (filter.isEmpty()) {
                        for (Item item : content.items()) {
                            if (linkTarget.items.get(item) > 0) { // 检查是否有物品
                                currentInputIndex = index;
                                currentTarget = linkTarget;
                                progress = 0f;
                                return true;
                            }
                        }
                    } else {
                        for (Item item : filter) {
                            if (linkTarget.items.get(item) > 0) { // 检查是否有物品
                                currentInputIndex = index;
                                currentTarget = linkTarget;
                                progress = 0f;
                                return true;
                            }
                        }
                    }
                }
                searchCount++;
            }
            return false;
        }
        
        public boolean searchOutput() {
            if (outputs.isEmpty() || heldItem.item == null || heldItem.amount <= 0) {
                return false;
            }
            
            // 限制搜索次数，避免无限循环
            int searchCount = 0;
            int maxSearches = outputs.size * 2; // 最多搜索两次所有输出方块
            
            while (searchCount < maxSearches) {
                // 确保loopIndex在有效范围内
                if (outputs.isEmpty()) {
                    return false;
                }
                
                loopIndex--;
                if (loopIndex < 0) {
                    loopIndex = outputs.size - 1;
                } else if (loopIndex >= outputs.size) {
                    loopIndex = outputs.size - 1;
                }
                
                int index = loopIndex;
                if (index >= outputs.size) break;
                
                int pos = outputs.get(index);
                Building linkTarget = world.build(pos);
                if (!linkValidTarget(this, linkTarget)) {
                    searchCount++;
                    continue;
                }
                
                if (linkTarget.items != null && linkTarget.acceptStack(heldItem.item, heldItem.amount, this) > 0) {
                    Seq<Item> filter = linkItemFilters.get(pos, new Seq<>());
                    
                    if (!filter.isEmpty() && !filter.contains(heldItem.item)) {
                        searchCount++;
                        continue;
                    }
                    
                    currentOutputIndex = index;
                    currentTarget = linkTarget;
                    progress = 0f;
                    return true;
                }
                searchCount++;
            }
            return false;
        }
        
        public boolean moveToTarget() {
            if (currentTarget == null) return false;

            // 检测目标方块是否存在
            Building target = world.build(currentTarget.pos());
            if (target == null) {
                // 目标方块不存在，停止并返回IDLE
                currentTarget = null;
                phase = 0;
                progress = 0f;
                phaseTime = 0f; // 重置阶段时间
                return false;
            }

            progress += 0.2f;
            if (progress >= 1f) {
                progress = 1f;
                return true;
            }
            return false;
        }
        
        public boolean extract() {
            if (currentTarget == null || progress < 1f) return false;
            
            // 检测目标方块是否存在
            Building target = world.build(currentTarget.pos());
            if (target == null) {
                // 目标方块不存在，停止并返回IDLE
                currentTarget = null;
                currentInputIndex = -1;
                phase = 0;
                progress = 0f;
                phaseTime = 0f; // 重置阶段时间
                return false;
            }
            
            // 确保currentInputIndex在有效范围内
            if (currentInputIndex < 0 || currentInputIndex >= inputs.size) {
                currentTarget = null;
                currentInputIndex = -1;
                phase = 0;
                progress = 0f;
                phaseTime = 0f; // 重置阶段时间
                return false;
            }
            
            if (currentTarget != null && extractItem(currentTarget)) {
                currentTarget = null;
                currentInputIndex = -1;
                progress = 0f;
                phaseTime = 0f;
                return true;
            }
            return false;
        }
        
        public boolean deposit() {
            if (currentTarget == null || progress < 1f) return false;
            
            // 检测目标方块是否存在
            Building target = world.build(currentTarget.pos());
            if (target == null) {
                // 目标方块不存在，停止并返回IDLE
                currentTarget = null;
                currentOutputIndex = -1;
                phase = 0;
                progress = 0f;
                phaseTime = 0f; // 重置阶段时间
                return false;
            }
            
            // 检测目标方块是否还能接受物品
            int canAccept = target.acceptStack(heldItem.item, heldItem.amount, this);
            if (canAccept <= 0) {
                // 目标方块已满或不能接受物品，停止并返回IDLE
                currentTarget = null;
                currentOutputIndex = -1;
                phase = 0;
                progress = 0f;
                phaseTime = 0f; // 重置阶段时间
                return false;
            }
            
            if (currentTarget != null && depositItem(currentTarget)) {
                currentTarget = null;
                currentOutputIndex = -1;
                progress = 0f;
                phaseTime = 0f;
                return true;
            }
            return false;
        }
        
        @Override
        public void drawConfigure() {
            super.drawConfigure();
            
            float sin = Mathf.absin(Time.time, 6, 1);
            
            Draw.color(Pal.accent);
            Lines.stroke(1);
            Drawf.circles(x, y, (size / 2 + 1) * tilesize + sin - 2, Pal.accent);
            
            for (int pos : inputs) {
                if (linkValid(this, pos)) {
                    Building linkTarget = world.build(pos);
                    Lines.stroke(2);
                    Drawf.square(linkTarget.x, linkTarget.y, linkTarget.block.size * tilesize / 2 + 1, Color.purple);
                    Lines.stroke(1);
                }
            }
            
            for (int pos : outputs) {
                if (linkValid(this, pos)) {
                    Building linkTarget = world.build(pos);
                    Lines.stroke(2);
                    Drawf.square(linkTarget.x, linkTarget.y, linkTarget.block.size * tilesize / 2 + 1, Pal.place);
                    Lines.stroke(1);
                }
            }
            
            Drawf.dashSquare(Pal.accent, x, y, range * 2);
        }
        
        @Override
        public void draw() {
            super.draw();

            // Draw.alpha(warmup);
            
            // 绘制底座 - 保持在建筑层
            Draw.rect(baseRegion, x, y);
            
            // 将绘制图层提升到建筑覆盖层（高于建筑，低于单位）
            Draw.z(Layer.blockOver);
            
            // 计算机械臂各部分的位置和角度
            float baseX = x;
            float baseY = y;
            
            // 目标角度
            float targetBaseAngle = getTargetAngle();
            
            // 底座旋转
            float rotateSpeed = 6f; // 旋转速度
            // 最短路线旋转
            float currentDegrees = baseAngle * Mathf.radiansToDegrees;
            float targetDegrees = targetBaseAngle * Mathf.radiansToDegrees;
            float newDegrees = Angles.moveToward(currentDegrees, targetDegrees, rotateSpeed);
            baseAngle = newDegrees * Mathf.degreesToRadians;
            
            // 计算到目标方块的距离
            float targetArmExtend = 0.3f; // 目标伸长值
            if (currentTarget != null && (phase == 1 || phase == 2 || phase == 4 || phase == 5)) {
                // 计算机械臂的中点坐标
                float armCenterX = x;
                float armCenterY = y;
                
                // 计算目标方块的中点坐标
                float targetCenterX = currentTarget.x;
                float targetCenterY = currentTarget.y;
                
                // 绘制一条隐形的线（透明度为0）（调试，可以删掉）
                Draw.alpha(0);
                Lines.stroke(1);
                Lines.line(armCenterX, armCenterY, targetCenterX, targetCenterY);
                Draw.alpha(1);
                
                // 直接采集方块到机械臂的直线距离
                float distance = Mathf.dst(armCenterX, armCenterY, targetCenterX, targetCenterY);
                
                // 根据实际距离计算合适的伸长值
                float minExtend = 0.3f; // 最小收拢状态
                float maxExtend = 12f / 3.8f; // 最大伸长状态，对应12格范围
                
                // 直接使用距离计算目标伸长值，确保手臂能正好伸长到目标方块
                float rawExtend = distance / (3.8f * tilesize);
                // 限制在有效范围内
                targetArmExtend = Mathf.clamp(rawExtend, minExtend, maxExtend);
            } else if (phase == 0 || phase == 3) {
                // 空闲或搜索时保持收拢状态
                targetArmExtend = 0.3f;
            } else {
                // 其他状态，保持当前长度
                targetArmExtend = armExtend;
            }
            // 平滑插值手臂伸长值
            armExtend = Mathf.lerpDelta(armExtend, targetArmExtend, 0.16f);
            
            // 下臂角度 - 根据phase和移动进度调整
            float targetLowerArmAngle;
            if (phase == 1 || phase == 4) {
                // 移动过程中，下臂抬起使爪子抬高
                targetLowerArmAngle = 20f * Mathf.degreesToRadians;
            } else if (phase == 2 || phase == 5) {
                // 提取/放置时，下臂降低到接触位置
                targetLowerArmAngle = 70f * Mathf.degreesToRadians;
            } else {
                // 空闲状态，下臂保持中等角度
                targetLowerArmAngle = 45f * Mathf.degreesToRadians;
            }
            // 将弧度转换为度数进行插值
            float lowerArmAngleDeg = lowerArmAngle * Mathf.radiansToDegrees;
            float targetLowerArmAngleDeg = targetLowerArmAngle * Mathf.radiansToDegrees;
            float newLowerArmAngleDeg = Angles.moveToward(lowerArmAngleDeg, targetLowerArmAngleDeg, 10f);
            lowerArmAngle = newLowerArmAngleDeg * Mathf.degreesToRadians;
            
            // 下臂长度 - 根据伸长值变化
            float lowerArmLength = tilesize * 2.0f * armExtend;
            // 下臂端点位置 - 使用baseAngle + lowerArmAngle使下臂随关节旋转
            float lowerArmEndX = baseX + Mathf.cos(baseAngle + lowerArmAngle) * lowerArmLength;
            float lowerArmEndY = baseY + Mathf.sin(baseAngle + lowerArmAngle) * lowerArmLength;
            // 下臂中点位置
            float lowerArmMidX = baseX + Mathf.cos(baseAngle + lowerArmAngle) * lowerArmLength / 2;
            float lowerArmMidY = baseY + Mathf.sin(baseAngle + lowerArmAngle) * lowerArmLength / 2;
            // 绘制下臂 - 宽度为臂长，高度为厚度（保持恒定），旋转角度转换为度数
            float lowerArmThickness = lowerArmRegion.height * 0.12f;
            Draw.rect(lowerArmRegion, lowerArmMidX, lowerArmMidY, lowerArmLength, lowerArmThickness, (baseAngle + lowerArmAngle) * Mathf.radiansToDegrees);

            // 上臂角度 - 根据phase和移动进度调整
            float targetUpperArmAngle;
            if (phase == 1 || phase == 4) {
                // 移动过程中，上臂稍微伸展
                targetUpperArmAngle = -10f * Mathf.degreesToRadians;
            } else if (phase == 2 || phase == 5) {
                // 提取/放置时，上臂降低到接触位置
                targetUpperArmAngle = -50f * Mathf.degreesToRadians;
            } else {
                // 空闲状态，上臂保持中等角度
                targetUpperArmAngle = -30f * Mathf.degreesToRadians;
            }
            // 将弧度转换为度数进行插值
            float upperArmAngleDeg = upperArmAngle * Mathf.radiansToDegrees;
            float targetUpperArmAngleDeg = targetUpperArmAngle * Mathf.radiansToDegrees;
            float newUpperArmAngleDeg = Angles.moveToward(upperArmAngleDeg, targetUpperArmAngleDeg, 10f);
            upperArmAngle = newUpperArmAngleDeg * Mathf.degreesToRadians;

            // 上臂长度 - 根据伸长值变化
            float upperArmLength = tilesize * 1.8f * armExtend;
            // 上臂总角度 = 底座角度 + 下臂关节角度 + 上臂关节角度
            float upperArmTotalAngle = baseAngle + lowerArmAngle + upperArmAngle;
            // 上臂端点位置 - 从下臂末端沿上臂方向延伸
            float upperArmEndX = lowerArmEndX + Mathf.cos(upperArmTotalAngle) * upperArmLength;
            float upperArmEndY = lowerArmEndY + Mathf.sin(upperArmTotalAngle) * upperArmLength;
            // 上臂中点位置 - 在上臂的中间
            float upperArmMidX = lowerArmEndX + Mathf.cos(upperArmTotalAngle) * upperArmLength / 2;
            float upperArmMidY = lowerArmEndY + Mathf.sin(upperArmTotalAngle) * upperArmLength / 2;
            // 绘制上臂 - 宽度为臂长，高度为厚度（保持恒定），旋转角度转换为度数
            float upperArmThickness = upperArmRegion.height * 0.12f;
            Draw.rect(upperArmRegion, upperArmMidX, upperArmMidY, upperArmLength, upperArmThickness, upperArmTotalAngle * Mathf.radiansToDegrees);
            
            // 爪子角度 - 根据 phase 和 progress 决定
            // phase 0: 空闲 - 根据是否持有物品决定
            // phase 1: 移动到输入 - 保持张开状态
            // phase 2: 提取物品 - 到达后闭合抓取（在方块上完成）
            // phase 3: 搜索输出 - 闭合（持有物品）
            // phase 4: 移动到输出 - 闭合（持有物品）
            // phase 5: 放置物品 - 到达后张开释放（在方块上完成）
            float targetClawAngle;
            if (phase == 0) {
                // 空闲状态，根据是否持有物品决定
                targetClawAngle = (heldItem.item != null && heldItem.amount > 0) ? 0f : 60f * Mathf.degreesToRadians;
            } else if (phase == 1) {
                // 移动到输入 - 保持张开状态
                targetClawAngle = 60f * Mathf.degreesToRadians;
            } else if (phase == 2) {
                // 提取物品 - 到达后闭合抓取（在方块上完成闭合动画）
                // 使用进度的后半部分来触发闭合动画
                float grabProgress = Mathf.clamp((progress - 0.5f) * 2f); // 只在 progress > 0.5 时开始
                float easedProgress = Mathf.pow(grabProgress, 2f); // 使用幂函数实现缓入效果
                targetClawAngle = Mathf.lerp(60f, 0f, easedProgress) * Mathf.degreesToRadians;
            } else if (phase == 3 || phase == 4) {
                // 搜索输出/移动到输出 - 闭合（持有物品）
                targetClawAngle = 0f;
            } else if (phase == 5) {
                // 放置物品 - 到达后张开释放（在方块上完成张开动画）（张开貌似没有实现）
                // 使用进度的后半部分来触发张开动画
                float releaseProgress = Mathf.clamp((progress - 0.5f) * 2f); // 只在 progress > 0.5 时开始
                float easedProgress = 1f - Mathf.pow(1f - releaseProgress, 2f); // 使用幂函数实现缓出效果
                targetClawAngle = Mathf.lerp(0f, 60f, easedProgress) * Mathf.degreesToRadians;
            } else {
                targetClawAngle = 60f * Mathf.degreesToRadians;
            }
            clawAngle = Angles.moveToward(clawAngle, targetClawAngle, 0.3f);

            // 爪子位置 - 在上臂末端前方4像素
            float clawX = upperArmEndX + Mathf.cos(upperArmTotalAngle) * 4f;
            float clawY = upperArmEndY + Mathf.sin(upperArmTotalAngle) * 4f;
            // 爪子基础旋转 = 上臂总角度，转换为度数
            float clawRotation = (upperArmTotalAngle) * Mathf.radiansToDegrees;
            
            // 爪子张合角度（归一化到 0-1）
            float clawSpread = clawAngle / (60f * Mathf.degreesToRadians);
            
            // 左爪子：围绕旋转轴逆时针旋转（张开时向上）
            float leftClawRotation = clawRotation + clawSpread * 30f; // 左爪最大旋转 30 度
            
            // 右爪子：围绕旋转轴顺时针旋转（张开时向下）
            float rightClawRotation = clawRotation - clawSpread * 30f; // 右爪最大旋转 -30 度
            
            // 绘制左右爪子 - 指定旋转中心
            float scale = 1f / 4f;
            Draw.rect(clawLeftRegion, clawX, clawY, clawLeftRegion.width * scale, clawLeftRegion.height * scale, leftClawRotation);
            Draw.rect(clawRightRegion, clawX, clawY, clawRightRegion.width * scale, clawRightRegion.height * scale, rightClawRotation);
            
            // 绘制持有物品 - 只有在爪子闭合时才显示
            if (heldItem.item != null && heldItem.amount > 0 && clawSpread < 0.3f) {
                Draw.z(Layer.blockOver + 1);
                Draw.rect(heldItem.item.uiIcon, clawX, clawY, 8, 8);
            }
            
            // 恢复图层到建筑层
            Draw.z(Layer.block);
            Draw.alpha(1);
        }
        
        public float getTargetAngle() {
            // 在所有移动和操作阶段都指向currentTarget
            if (currentTarget != null && (phase == 1 || phase == 2 || phase == 4 || phase == 5)) {
                // 反转旋转方向：交换x和y的差值顺序
                float angle = Mathf.atan2(currentTarget.x - x, currentTarget.y - y);
                // 提取阶段(1,2)减少15度，推送阶段(4,5)减少15度
                if (phase == 1 || phase == 2) {
                    angle -= 15f * Mathf.degreesToRadians;
                } else if (phase == 4 || phase == 5) {
                    angle -= 15f * Mathf.degreesToRadians;
                }
                return angle;
            }
            return baseAngle;
        }
        
        public Runnable rebuildUI;
        
        @Override
        public void buildConfiguration(Table table) {
            super.buildConfiguration(table);
            
            // 添加配置按钮
            Table buttonTable = new Table();
            buttonTable.center();
            buttonTable.button(Icon.settings, 60f, () -> {
                // 点击按钮显示配置界面
                showConfigurationDialog();
            });
            
            table.add(buttonTable).grow();
        }
        
        public void showConfigurationDialog() {
            BaseDialog dialog = new BaseDialog("Mechanical Arm Configuration");
            dialog.cont.pane(pane -> {
                Table cont = new Table().top();
                cont.left().defaults().left().growX();
                
                rebuildUI = () -> {
                    cont.clearChildren();
                    
                    // 单次抓取物品量控制
                    cont.table(Styles.grayPanel, amountControl -> {
                        amountControl.left().defaults().left();
                        amountControl.add("[accent]Item Transfer Amount:[]").row();
                        Slider slider = new Slider(1, 10, 1, false);
                        // 先添加到场景中
                        amountControl.add(slider).growX().row();
                        // 然后设置值和监听器
                        slider.setValue(itemTransferAmount);
                        // 创建一个标签来显示当前值
                        Label currentValueLabel = new Label("Current: " + itemTransferAmount);
                        currentValueLabel.setColor(Color.lightGray);
                        amountControl.add(currentValueLabel).row();
                        // 设置监听器，直接更新标签而不是重建整个UI
                        slider.changed(() -> {
                            itemTransferAmount = (int)slider.getValue();
                            currentValueLabel.setText("Current: " + itemTransferAmount);
                        });
                        
                        // 添加点击事件，在用户释放滑块时重置状态
                        slider.addListener(new ClickListener() {
                            @Override
                            public void clicked(InputEvent event, float x, float y) {
                                // 重置机械臂状态，避免因itemTransferAmount变化导致卡死
                                if (phase != 0) {
                                    phase = 0;
                                    currentTarget = null;
                                    currentInputIndex = -1;
                                    currentOutputIndex = -1;
                                    progress = 0f;
                                    phaseTime = 0f;
                                    
                                }
                            }
                        });
                    }).growX().left().pad(10);
                    cont.row();
                    
                    // 连接的方块预览
                    if (!inputs.isEmpty() || !outputs.isEmpty()) {
                        cont.table(Styles.grayPanel, linksTable -> {
                            linksTable.left().defaults().left();
                            linksTable.add("[accent]Connected Blocks:[]").row();
                            
                            // 分类显示：输入模式和输出模式
                            if (!inputs.isEmpty()) {
                                linksTable.add("[purple]Input Mode:[] (" + inputs.size + ")").row();
                                for (int pos : inputs) {
                                    Building linkTarget = world.build(pos);
                                    if (linkTarget != null) {
                                        linksTable.image(linkTarget.block.uiIcon).size(16).padRight(4);
                                        linksTable.label(() -> linkTarget.block.localizedName + " (Input)").color(Color.lightGray).padRight(4);
                                        linksTable.button("Settings", Styles.defaultt, () -> {
                                            showLinkSettings(pos, true);
                                        }).size(80, 30);
                                        linksTable.button("Remove", Styles.defaultt, () -> {
                                            setOneLink(pos);
                                            rebuildUI.run();
                                        }).size(80, 30);
                                        linksTable.row();
                                    }
                                }
                            }
                            
                            if (!outputs.isEmpty()) {
                                linksTable.add("[green]Output Mode:[] (" + outputs.size + ")").row();
                                for (int pos : outputs) {
                                    Building linkTarget = world.build(pos);
                                    if (linkTarget != null) {
                                        linksTable.image(linkTarget.block.uiIcon).size(16).padRight(4);
                                        linksTable.label(() -> linkTarget.block.localizedName + " (Output)").color(Color.lightGray).padRight(4);
                                        linksTable.button("Settings", Styles.defaultt, () -> {
                                            showLinkSettings(pos, false);
                                        }).size(80, 30);
                                        linksTable.button("Remove", Styles.defaultt, () -> {
                                            setOneLink(pos);
                                            rebuildUI.run();
                                        }).size(80, 30);
                                        linksTable.row();
                                    }
                                }
                            }
                        }).growX().left().pad(10);
                        cont.row();
                    }
                    
                    // 显示当前持有物品
                    cont.table(Styles.grayPanel, heldItemInfo -> {
                        heldItemInfo.left().defaults().left();
                        heldItemInfo.add("[accent]Held Item:[]").row();
                        
                        if (heldItem.item != null && heldItem.amount > 0) {
                            heldItemInfo.image(heldItem.item.uiIcon).size(16).padRight(4);
                            heldItemInfo.label(() -> heldItem.item.localizedName + " x" + heldItem.amount).color(Color.lightGray);
                        } else {
                            heldItemInfo.add("[gray]None[]").padTop(4);
                        }
                    }).growX().left().pad(10);
                };
                
                rebuildUI.run();
                
                ScrollPane scrollPane = new ScrollPane(cont, Styles.smallPane);
                scrollPane.setScrollingDisabled(false, false);
                scrollPane.setOverscroll(false, false);
                pane.add(scrollPane).maxWidth(600).maxHeight(400);
            });
            
            dialog.cont.button("Close", dialog::hide).size(100, 40);
            dialog.show();
        }
        
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
                
                Seq<Item> itemFilter = linkItemFilters.get(pos, new Seq<>());
                
                Table itemTable = new Table();
                itemTable.left().defaults().left();
                
                int itemCount = 0;
                for (Item item : content.items()) {
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
                
                pane.add("[accent]Item Filter:[]").row();
                pane.add(itemPane).maxHeight(120).maxWidth(400).row();
                
                Slider itemSlider = new Slider(0, 100, 1, false);
                itemSlider.changed(() -> {
                    float scrollPos = itemSlider.getValue() / 100f;
                    itemPane.setScrollX(scrollPos * (itemPane.getMaxWidth() - itemPane.getWidth()));
                });
                pane.add(itemSlider).width(400).height(20).row();
            }).maxHeight(400);
            
            dialog.cont.button("OK", dialog::hide).size(100, 40);
            dialog.show();
        }
        
        @Override
        public boolean onConfigureBuildTapped(Building other) {
            if (this == other) {
                return false;
            }

            if (Math.abs(other.x - x) <= range && Math.abs(other.y - y) <= range && other.team == team) {
                int pos = other.pos();
                setOneLink(pos);
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
            return compressConfig();
        }
        
        public byte[] configBytes() {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(new DeflaterOutputStream(baos));
                
                dos.write(1); // version
                
                dos.writeShort(inputs.size);
                for (int pos : inputs) {
                    Point2 point2 = Point2.unpack(pos).sub(tile.x, tile.y);
                    dos.writeShort(point2.x);
                    dos.writeShort(point2.y);
                    dos.writeByte(linkModes.get(pos, true) ? 1 : 0);
                    dos.writeInt(clickCounts.get(pos, 1));
                    Seq<Item> itemFilter = linkItemFilters.get(pos, new Seq<>());
                    dos.writeShort(itemFilter.size);
                    for (Item item : itemFilter) {
                        dos.writeShort(item.id);
                    }
                }
                dos.writeShort(outputs.size);
                for (int pos : outputs) {
                    Point2 point2 = Point2.unpack(pos).sub(tile.x, tile.y);
                    dos.writeShort(point2.x);
                    dos.writeShort(point2.y);
                    dos.writeByte(linkModes.get(pos, false) ? 1 : 0);
                    dos.writeInt(clickCounts.get(pos, 0));
                    Seq<Item> itemFilter = linkItemFilters.get(pos, new Seq<>());
                    dos.writeShort(itemFilter.size);
                    for (Item item : itemFilter) {
                        dos.writeShort(item.id);
                    }
                }
                dos.close();
                return baos.toByteArray();
            } catch (Exception e) {
                Log.err("Error serializing config", e);
                return new byte[0];
            }
        }
        
        public void readConfig(byte[] data) {
            if (data == null || data.length == 0) {
                return;
            }
            
            try (DataInputStream dis = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(data)))) {
                int version = dis.read();
                
                // 版本检查
                if (version != 1) {
                    Log.warn("Unknown config version in readConfig: " + version);
                    return;
                }
                
                Seq<Integer> newInputs = new Seq<>();
                Seq<Integer> newOutputs = new Seq<>();
                ObjectMap<Integer, Boolean> newLinkModes = new ObjectMap<>();
                ObjectMap<Integer, Integer> newClickCounts = new ObjectMap<>();
                ObjectMap<Integer, Seq<Item>> newLinkItemFilters = new ObjectMap<>();
                
                int inputSize = dis.readShort();
                // 数据验证
                if (inputSize < 0 || inputSize > 1000) {
                    Log.warn("Invalid input size in readConfig: " + inputSize);
                    return;
                }
                
                for (int i = 0; i < inputSize; i++) {
                    int linkX = dis.readShort();
                    int linkY = dis.readShort();
                    int pos = Point2.pack(linkX + tile.x, linkY + tile.y);
                    newInputs.add(pos);
                    newLinkModes.put(pos, dis.readByte() == 1);
                    newClickCounts.put(pos, dis.readInt());
                    int itemFilterSize = dis.readShort();
                    if (itemFilterSize < 0 || itemFilterSize > 100) {
                        Log.warn("Invalid item filter size in readConfig: " + itemFilterSize);
                        return;
                    }
                    Seq<Item> itemFilter = new Seq<>();
                    for (int j = 0; j < itemFilterSize; j++) {
                        Item item = content.item(dis.readShort());
                        if (item != null) {
                            itemFilter.add(item);
                        }
                    }
                    newLinkItemFilters.put(pos, itemFilter);
                }
                
                int outputSize = dis.readShort();
                if (outputSize < 0 || outputSize > 1000) {
                    Log.warn("Invalid output size in readConfig: " + outputSize);
                    return;
                }
                
                for (int i = 0; i < outputSize; i++) {
                    int linkX = dis.readShort();
                    int linkY = dis.readShort();
                    int pos = Point2.pack(linkX + tile.x, linkY + tile.y);
                    newOutputs.add(pos);
                    newLinkModes.put(pos, dis.readByte() == 1);
                    newClickCounts.put(pos, dis.readInt());
                    int itemFilterSize = dis.readShort();
                    if (itemFilterSize < 0 || itemFilterSize > 100) {
                        Log.warn("Invalid item filter size in readConfig: " + itemFilterSize);
                        return;
                    }
                    Seq<Item> itemFilter = new Seq<>();
                    for (int j = 0; j < itemFilterSize; j++) {
                        Item item = content.item(dis.readShort());
                        if (item != null) {
                            itemFilter.add(item);
                        }
                    }
                    newLinkItemFilters.put(pos, itemFilter);
                }
                
                // 只有在新配置包含实际链接时才更新，防止空配置覆盖现有数据
                if (!newInputs.isEmpty() || !newOutputs.isEmpty()) {
                    inputs = newInputs;
                    outputs = newOutputs;
                    linkModes = newLinkModes;
                    clickCounts = newClickCounts;
                    linkItemFilters = newLinkItemFilters;
                }
            } catch (Exception e) {
                Log.err("Error deserializing config", e);
            }
        }
        
        @Override
        public void configure(Object value) {
            if (value instanceof byte[]) {
                readConfig((byte[])value);
            } else if (value instanceof Integer) {
                setOneLink((Integer)value);
            }
        }
        
        public byte[] compressConfig() {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(new DeflaterOutputStream(baos));
                
                dos.write(1); // version
                
                // 添加防御性检查，确保集合不为null
                Seq<Integer> safeInputs = inputs != null ? inputs : new Seq<>();
                Seq<Integer> safeOutputs = outputs != null ? outputs : new Seq<>();
                
                dos.writeShort(safeInputs.size);
                for (int pos : safeInputs) {
                    Point2 point2 = Point2.unpack(pos);
                    dos.writeShort(point2.x - tile.x);
                    dos.writeShort(point2.y - tile.y);
                    dos.writeByte(linkModes != null && linkModes.get(pos, true) ? 1 : 0);
                    dos.writeInt(clickCounts != null ? clickCounts.get(pos, 1) : 1);
                    Seq<Item> itemFilter = linkItemFilters != null ? linkItemFilters.get(pos, new Seq<>()) : new Seq<>();
                    dos.writeShort(itemFilter.size);
                    for (Item item : itemFilter) {
                        dos.writeShort(item.id);
                    }
                }
                
                dos.writeShort(safeOutputs.size);
                for (int pos : safeOutputs) {
                    Point2 point2 = Point2.unpack(pos);
                    dos.writeShort(point2.x - tile.x);
                    dos.writeShort(point2.y - tile.y);
                    dos.writeByte(linkModes != null && linkModes.get(pos, false) ? 1 : 0);
                    dos.writeInt(clickCounts != null ? clickCounts.get(pos, 0) : 0);
                    Seq<Item> itemFilter = linkItemFilters != null ? linkItemFilters.get(pos, new Seq<>()) : new Seq<>();
                    dos.writeShort(itemFilter.size);
                    for (Item item : itemFilter) {
                        dos.writeShort(item.id);
                    }
                }
                
                dos.close();
                return baos.toByteArray();
            } catch (Exception e) {
                Log.err("Error compressing config", e);
                return new byte[0];
            }
        }
        
        public void decompressConfig(byte[] data) {
            if (data == null || data.length == 0) {
                return;
            }
            
            try (DataInputStream dis = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(data)))) {
                int version = dis.read();
                
                if (version != 1) {
                    Log.warn("Unknown config version: " + version);
                    return;
                }
                
                Seq<Integer> newInputs = new Seq<>();
                Seq<Integer> newOutputs = new Seq<>();
                ObjectMap<Integer, Boolean> newLinkModes = new ObjectMap<>();
                ObjectMap<Integer, Integer> newClickCounts = new ObjectMap<>();
                ObjectMap<Integer, Seq<Item>> newLinkItemFilters = new ObjectMap<>();
                
                int inputSize = dis.readShort();
                if (inputSize < 0 || inputSize > 1000) {
                    Log.warn("Invalid input size: " + inputSize);
                    return;
                }
                
                for (int i = 0; i < inputSize; i++) {
                    int linkX = dis.readShort() + tile.x;
                    int linkY = dis.readShort() + tile.y;
                    int pos = Point2.pack(linkX, linkY);
                    newInputs.add(pos);
                    newLinkModes.put(pos, dis.readByte() == 1);
                    newClickCounts.put(pos, dis.readInt());
                    int itemFilterSize = dis.readShort();
                    if (itemFilterSize < 0 || itemFilterSize > 100) {
                        Log.warn("Invalid item filter size: " + itemFilterSize);
                        return;
                    }
                    Seq<Item> itemFilter = new Seq<>();
                    for (int j = 0; j < itemFilterSize; j++) {
                        short itemId = dis.readShort();
                        Item item = content.item(itemId);
                        if (item != null) {
                            itemFilter.add(item);
                        }
                    }
                    newLinkItemFilters.put(pos, itemFilter);
                }
                
                int outputSize = dis.readShort();
                if (outputSize < 0 || outputSize > 1000) {
                    Log.warn("Invalid output size: " + outputSize);
                    return;
                }
                
                for (int i = 0; i < outputSize; i++) {
                    int linkX = dis.readShort() + tile.x;
                    int linkY = dis.readShort() + tile.y;
                    int pos = Point2.pack(linkX, linkY);
                    newOutputs.add(pos);
                    newLinkModes.put(pos, dis.readByte() == 1);
                    newClickCounts.put(pos, dis.readInt());
                    int itemFilterSize = dis.readShort();
                    if (itemFilterSize < 0 || itemFilterSize > 100) {
                        Log.warn("Invalid item filter size: " + itemFilterSize);
                        return;
                    }
                    Seq<Item> itemFilter = new Seq<>();
                    for (int j = 0; j < itemFilterSize; j++) {
                        short itemId = dis.readShort();
                        Item item = content.item(itemId);
                        if (item != null) {
                            itemFilter.add(item);
                        }
                    }
                    newLinkItemFilters.put(pos, itemFilter);
                }
                
                inputs = newInputs;
                outputs = newOutputs;
                linkModes = newLinkModes;
                clickCounts = newClickCounts;
                linkItemFilters = newLinkItemFilters;
            } catch (Exception e) {
                Log.err("Error decompressing config", e);
            }
        }
        
        @Override
        public void write(Writes write) {
            super.write(write);
            
            byte[] compressed = compressConfig();
            write.i(compressed.length);
            write.b(compressed);
            
            write.i(phase);
            write.i(currentInputIndex);
            write.i(currentOutputIndex);
            write.f(progress);
            write.s(heldItem.item != null ? heldItem.item.id : -1);
            write.i(heldItem.amount);
            write.i(itemTransferAmount);
        }
        
        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            
            if (revision >= 5) {
                int compl = read.i();
                if (compl > 0) {
                    byte[] bytes = new byte[compl];
                    read.b(bytes);
                    decompressConfig(bytes);
                } else {
                    // 如果没有压缩数据，初始化空集合（第一次加载或空配置）
                    inputs = new Seq<>();
                    outputs = new Seq<>();
                    linkModes = new ObjectMap<>();
                    clickCounts = new ObjectMap<>();
                    linkItemFilters = new ObjectMap<>();
                }
            } else {
                inputs = new Seq<>();
                outputs = new Seq<>();
                linkModes = new ObjectMap<>();
                clickCounts = new ObjectMap<>();
                linkItemFilters = new ObjectMap<>();
                
                int inputSize = read.s();
                for (int i = 0; i < inputSize; i++) {
                    int pos = read.i();
                    inputs.add(pos);
                    linkModes.put(pos, read.b() == 1);
                    clickCounts.put(pos, read.i());
                    if (revision >= 3) {
                        int itemFilterSize = read.s();
                        Seq<Item> itemFilter = new Seq<>();
                        for (int j = 0; j < itemFilterSize; j++) {
                            Item item = content.item(read.s());
                            if (item != null) {
                                itemFilter.add(item);
                            }
                        }
                        linkItemFilters.put(pos, itemFilter);
                    } else {
                        linkItemFilters.put(pos, new Seq<>());
                    }
                }
                int outputSize = read.s();
                for (int i = 0; i < outputSize; i++) {
                    int pos = read.i();
                    outputs.add(pos);
                    linkModes.put(pos, read.b() == 1);
                    clickCounts.put(pos, read.i());
                    if (revision >= 3) {
                        int itemFilterSize = read.s();
                        Seq<Item> itemFilter = new Seq<>();
                        for (int j = 0; j < itemFilterSize; j++) {
                            Item item = content.item(read.s());
                            if (item != null) {
                                itemFilter.add(item);
                            }
                        }
                        linkItemFilters.put(pos, itemFilter);
                    } else {
                        linkItemFilters.put(pos, new Seq<>());
                    }
                }
            }
            
            phase = read.i();
            currentInputIndex = read.i();
            currentOutputIndex = read.i();
            progress = read.f();
            int itemId = read.s();
            int amount = read.i();
            heldItem = new ItemStack(itemId >= 0 ? content.item(itemId) : null, amount);
            if (revision >= 4) {
                itemTransferAmount = read.i();
            } else {
                itemTransferAmount = 1;
            }
            currentTarget = null;
        }
        
        @Override
        public byte version() {
            return 5;
        }
    }
}