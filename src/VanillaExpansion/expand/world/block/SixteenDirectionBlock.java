package VanillaExpansion.expand.world.block;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.input.Binding;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;

import static mindustry.Vars.*;
import static arc.Core.*;

/**
 * 支持16个放置方向的方块基类。
 * 只有4个垂直方向（上、下、左、右）可以与原版Mindustry方块传递数据。
 * 其他12个斜方向不与原版方块传递数据。
 */
public class SixteenDirectionBlock extends Block implements Autotiler {

    /** 方向数量 */
    public static final int DIRECTIONS = 16;
    
    /** 每个方向的角度（度数） */
    public static final float DEG_PER_DIRECTION = 360f / DIRECTIONS;
    
    /** 静态变量：当前16方向旋转值（用于在drawPlace中跟踪） */
    private static int sixteenRotation = 0;

    public static int getSixteenRotation() {
        return sixteenRotation;
    }

    public static void setSixteenRotation(int rotation) {
        sixteenRotation = Mathf.mod(rotation, DIRECTIONS);
    }

    public static void addSixteenRotation(int delta) {
        sixteenRotation = Mathf.mod(sixteenRotation + delta, DIRECTIONS);
    }

    /**
     * 判断方向是否为垂直方向（可与原版方块交互）
     * 垂直方向: 0(上), 4(右), 8(下), 12(左)
     */
    public static boolean isCardinalDirection(int direction) {
        return direction % 4 == 0;
    }

    /**
     * 将16方向转换为4方向（只保留垂直方向）
     * 返回 -1 表示非垂直方向
     */
    public static int toCardinalDirection(int direction) {
        if (!isCardinalDirection(direction)) {
            return -1;
        }
        return direction / 4;
    }

    /**
     * 获取方向对应的角度（弧度）
     */
    public static float directionToRad(int direction) {
        return (direction * DEG_PER_DIRECTION) * Mathf.degRad;
    }

    /**
     * 获取方向对应的角度（度数）
     */
    public static float directionToDeg(int direction) {
        return direction * DEG_PER_DIRECTION;
    }

    /**
     * 获取方向对应的单位向量
     */
    public static Point2 directionToPoint(int direction) {
        float angle = directionToRad(direction);
        int x = Math.round(Mathf.cos(angle));
        int y = Math.round(Mathf.sin(angle));
        return new Point2(x, y);
    }

    /** 视觉旋转是否使用16方向 */
    public boolean visual16Direction = true;
    
    /**
     * 16方向旋转角度映射表
     * 每个方向值(0-15)对应22.5°的增量
     */
    public static final float[] DIRECTION_ROTATION_MAP = {
        0f,      // 方向0: 0°
        22.5f,   // 方向1: 22.5°
        45f,     // 方向2: 45°
        67.5f,   // 方向3: 67.5°
        90f,     // 方向4: 90°
        112.5f,  // 方向5: 112.5°
        135f,    // 方向6: 135°
        157.5f,  // 方向7: 157.5°
        180f,    // 方向8: 180°
        202.5f,  // 方向9: 202.5°
        225f,    // 方向10: 225°
        247.5f,  // 方向11: 247.5°
        270f,    // 方向12: 270°
        292.5f,  // 方向13: 292.5°
        315f,    // 方向14: 315°
        337.5f   // 方向15: 337.5°
    };

    public SixteenDirectionBlock(String name) {
        super(name);
        rotate = true;
        saveConfig = true;
        config(Integer.class, (SixteenDirectionBuild build, Integer rotation) -> {
            build.directionData.setRotation(rotation);
        });
    }

    @Override
    public Object nextConfig() {
        // 返回当前16方向值，确保在创建BuildPlan时能正确传递
        return getSixteenRotation();
    }

    @Override
    public int planRotation(int rot) {
        // 支持16方向旋转，将输入的旋转值映射到16个方向
        // 原版输入是0-3的循环，我们需要将其扩展到0-15
        return Mathf.mod(rot, DIRECTIONS);
    }

    @Override
    public void flipRotation(BuildPlan req, boolean x) {
        // 16方向的翻转逻辑 - 翻转180度（8个方向）
        if ((x == (req.rotation % 2 == 0)) != invertFlip) {
            req.rotation = Mathf.mod(req.rotation + DIRECTIONS / 2, DIRECTIONS);
        }
    }

    @Override
    public void placeEnded(Tile tile, Unit builder, int rotation, Object config) {
        // 如果config为null但tile存在building，则使用静态变量sixteenRotation来设置16方向值
        // 这发生在玩家替换方块时，因为原版只能传递4方向旋转值
        if (config == null && tile != null && tile.build instanceof SixteenDirectionBuild build) {
            build.directionData.setRotation(getSixteenRotation());
        }
    }

    @Override
    public void onNewPlan(BuildPlan plan) {
        // 当创建新的放置计划时，将16方向值存储在config中
        // 无论plan.config是否为null，都需要确保它是正确的16方向值
        plan.config = getSixteenRotation();
    }

    /**
     * 增加旋转（支持16方向）
     */
    public int rotateBy(int currentRotation, int amount) {
        return Mathf.mod(currentRotation + amount, DIRECTIONS);
    }

    /**
     * 获取面向的方块（使用16方向）
     */
    public Building front(Building build, int direction) {
        Point2 dir = directionToPoint(direction);
        return world.build(build.tile.x + dir.x, build.tile.y + dir.y);
    }

    /**
     * 获取面向的方块（使用当前旋转）
     */
    public Building front(Building build) {
        return front(build, build.rotation);
    }

    /**
     * 判断是否可以与指定方向的方块进行数据传递
     * 只有垂直方向才能与原版方块传递数据
     */
    public boolean canTransferData(Building build, int direction) {
        // 如果是垂直方向，检查是否可以与相邻方块交互
        if (isCardinalDirection(direction)) {
            Building other = front(build, direction);
            if (other == null) return false;
            
            // 可以与原版Mindustry方块传递数据
            return true;
        }
        
        // 斜方向不与原版方块传递数据
        return false;
    }

    /**
     * 判断是否可以与指定方向的方块进行数据传递（使用当前旋转）
     */
    public boolean canTransferData(Building build) {
        return canTransferData(build, build.rotation);
    }

    /**
     * 获取垂直方向的相邻方块
     */
    public Building getCardinalNeighbor(Building build, int cardinalDir) {
        // cardinalDir: 0=上, 1=右, 2=下, 3=左
        int sixteenDir = cardinalDir * 4;
        return front(build, sixteenDir);
    }

    @Override
    public boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock) {
        // 只有垂直方向才考虑与其他方块的混合
        if (!isCardinalDirection(rotation)) {
            return false;
        }
        
        // 转换为4方向进行混合判断
        int cardinalRot = toCardinalDirection(rotation);
        int otherCardinalRot = isCardinalDirection(otherrot) ? toCardinalDirection(otherrot) : -1;
        
        // 使用4方向逻辑判断混合
        return (otherblock.outputsItems() || 
                (lookingAt(tile, cardinalRot, otherx, othery, otherblock) && otherblock.hasItems))
            && lookingAtEither(tile, cardinalRot, otherx, othery, otherCardinalRot >= 0 ? otherCardinalRot : otherrot, otherblock);
    }

    /**
     * 简化的lookingAt方法，使用4方向
     */
    public boolean lookingAt(Tile tile, int cardinalRotation, int otherx, int othery, Block otherblock) {
        return Point2.equals(
            tile.x + Geometry.d4x(cardinalRotation),
            tile.y + Geometry.d4y(cardinalRotation),
            otherx, othery
        );
    }

    /**
     * 简化的lookingAtEither方法，使用4方向
     */
    public boolean lookingAtEither(Tile tile, int cardinalRotation, int otherx, int othery, int otherrot, Block otherblock) {
        return Point2.equals(tile.x + Geometry.d4x(cardinalRotation), tile.y + Geometry.d4y(cardinalRotation), otherx, othery)
            || !otherblock.rotatedOutput(otherx, othery, tile)
            || Point2.equals(otherx + Geometry.d4x(otherrot), othery + Geometry.d4y(otherrot), tile.x, tile.y);
    }

    @Override
    public float drawPlaceText(String text, int x, int y, boolean valid) {
        // 使用16方向的角度绘制放置文本
        return super.drawPlaceText(text, x, y, valid);
    }

    /**
     * 获取16方向的旋转角度（用于绘制）
     * 使用方向映射表确保每个方向值对应正确的旋转角度
     */
    public float getDrawRotation(int rotation) {
        // 使用映射表获取旋转角度，确保每个方向值对应正确的22.5°增量
        int normalizedRotation = Mathf.mod(rotation, DIRECTIONS);
        return DIRECTION_ROTATION_MAP[normalizedRotation];
    }

    /**
     * 处理玩家旋转输入
     * 覆盖此方法以支持16方向旋转
     */
    public int handleRotationInput(int currentRotation, int mouseDelta) {
        // 将鼠标滚轮增量转换为16方向旋转
        return Mathf.mod(currentRotation + mouseDelta, DIRECTIONS);
    }

    /**
     * 根据鼠标位置计算16方向旋转
     * 这是关键方法，用于预建造时支持16方向
     */
    public int calculate16Direction(int planX, int planY) {
        // 获取鼠标在世界坐标中的位置
        float mouseWorldX = Core.input.mouseWorldX();
        float mouseWorldY = Core.input.mouseWorldY();
        
        // 计算预建造位置的中心坐标
        float planCenterX = planX * tilesize + tilesize / 2f;
        float planCenterY = planY * tilesize + tilesize / 2f;
        
        // 计算鼠标相对于预建造位置的角度
        float angle = Angles.angle(planCenterX, planCenterY, mouseWorldX, mouseWorldY);
        
        // 将角度转换为16方向（每个方向22.5度）
        // 添加11.25度偏移使方向对齐到中心
        int direction = (int)((angle + DEG_PER_DIRECTION / 2f) / DEG_PER_DIRECTION) % DIRECTIONS;
        
        return direction;
    }

    /**
     * 处理放置时的旋转
     * 结合鼠标滚轮的粗调和鼠标位置的微调
     */
    public int handlePlacementRotation(int planX, int planY, int baseRotation) {
        // 如果按住Shift键，使用鼠标位置计算精确的16方向
        if (Core.input.shift()) {
            return calculate16Direction(planX, planY);
        }
        
        // 否则使用基础旋转（鼠标滚轮控制的4方向）
        // 将4方向转换为16方向
        return baseRotation * 4;
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid) {
        // 使用16方向旋转绘制
        int drawRotation = getSixteenRotation();
    }

    @Override
    public void drawPlan(BuildPlan plan, Eachable<BuildPlan> list, boolean valid) {
        // 获取16方向旋转值
        int drawRotation = getSixteenRotation();
        float rotationDeg = getDrawRotation(drawRotation);
        float x = plan.drawx();
        float y = plan.drawy();

        // 绘制方块贴图（应用16方向旋转）
        Draw.reset();
        Draw.mixcol(!valid ? Pal.breakInvalid : Color.white, (!valid ? 0.4f : 0.24f) + Mathf.absin(Time.globalTime, 6f, 0.28f));
        Draw.alpha(1f);

        TextureRegion reg = getPlanRegion(plan, list);
        Draw.rect(reg, x, y, rotationDeg);

        Draw.reset();

    }

    /**
     * 16方向方块的Building内部类
     */
    public class SixteenDirectionBuild extends Building implements mindustry.logic.LReadable, mindustry.logic.LWritable {
        // 使用专门的16方向数据类存储旋转值
        protected SixteenDirectionData directionData = new SixteenDirectionData();

        @Override
        public void created() {
            super.created();
            // 初始使用父类的4方向rotation乘以4得到16方向值
            // 这个值后续会被configured()方法中的config值覆盖（如果config存在）
            // 方向值0→0°, 1→22.5°, 2→45°, ..., 15→337.5°
            directionData.setRotation(rotation * 4);
        }

        /**
         * 获取16方向数据对象
         */
        public SixteenDirectionData getDirectionData() {
            return directionData;
        }

        /**
         * 获取16方向旋转值
         */
        public int sixteenRotation() {
            return directionData.getRotation();
        }

        /**
         * 设置16方向旋转值
         */
        public void sixteenRotation(int rotation) {
            directionData.setRotation(rotation);
        }

        /**
         * 覆盖父类的rotation方法，返回16方向旋转值
         */
        public int rotation() {
            return directionData.getRotation();
        }

        /**
         * 设置旋转值（同时更新16方向旋转值）
         */
        public void rotation(int rot) {
            directionData.setRotation(rot);
        }

        @Override
        public void placed() {
            super.placed();
            // 如果config不为null，说明已经有正确的16方向值，不需要覆盖
            // 只有在没有config（替换方块）时才使用当前选择的16方向
        }

        @Override
        public void configured(@Nullable Unit builder, Object config) {
            super.configured(builder, config);
            // 如果config是Integer类型，设置16方向旋转值
            if (config instanceof Integer rotation) {
                directionData.setRotation(rotation);
            }
        }

        @Override
        public void configureAny(Object config) {
            // 同步设置 directionData，因为 configured() 是异步调用的
            // 当 config 为 null 时（替换方块），使用当前选择的16方向
            // 当 config 不为 null 时（蓝图放置），使用蓝图中的16方向值
            if (config == null) {
                directionData.setRotation(getSixteenRotation());
            } else if (config instanceof Integer) {
                directionData.setRotation((Integer) config);
            }
            // 调用父类的 configureAny，它会通过 Call.tileConfig() 异步调用 configured()
            super.configureAny(config);
        }

        /**
         * 获取方块配置（用于蓝图、复制等操作）
         * 返回16方向旋转值
         */
        @Override
        public Object config() {
            return directionData.getRotation();
        }

        @Override
        public void draw() {
            // 使用储存的16方向旋转值绘制贴图
            Draw.rect(block.region, x, y, getDrawRotation(directionData.getRotation()));
        }

        @Override
        public float drawrot() {
            // 返回16方向的旋转角度（度数）
            return getDrawRotation(directionData.getRotation());
        }

        @Override
        public void write(arc.util.io.Writes write) {
            super.write(write);
            // 将16方向数据写入存档
            directionData.write(write);
        }

        @Override
        public void read(arc.util.io.Reads read, byte revision) {
            super.read(read, revision);
            // 从存档读取16方向数据
            directionData.read(read);
        }

        // === LReadable 接口实现 ===
        @Override
        public boolean readable(mindustry.logic.LExecutor exec) {
            return isValid() && (exec.privileged || (this.team == exec.team && !this.block.privileged));
        }

        @Override
        public void read(mindustry.logic.LVar position, mindustry.logic.LVar output) {
            int address = position.numi();
            // 地址0: 读取16方向旋转值
            if (address == 0) {
                output.setnum(directionData.getRotation());
            }
            // 地址1: 读取旋转角度（度数）
            else if (address == 1) {
                output.setnum(directionData.getRotationDeg());
            }
            // 地址2: 读取是否为垂直方向
            else if (address == 2) {
                output.setnum(directionData.isCardinalDirection() ? 1 : 0);
            }
            // 地址3: 读取4方向值（如果是垂直方向）
            else if (address == 3) {
                output.setnum(directionData.toCardinalDirection());
            }
            // 无效地址返回NaN
            else {
                output.setnum(Double.NaN);
            }
        }

        // === LWritable 接口实现 ===
        @Override
        public boolean writable(mindustry.logic.LExecutor exec) {
            return readable(exec);
        }

        @Override
        public void write(mindustry.logic.LVar position, mindustry.logic.LVar value) {
            int address = position.numi();
            double val = value.num();
            
            // 地址0: 设置16方向旋转值
            if (address == 0) {
                directionData.setRotation((int) val);
            }
            // 地址1: 通过角度设置旋转值
            else if (address == 1) {
                int rotation = (int) Math.round(val / SixteenDirectionData.DEG_PER_DIRECTION);
                directionData.setRotation(rotation);
            }
            // 地址2: 通过4方向值设置旋转值
            else if (address == 2) {
                directionData.fromCardinalDirection((int) val);
            }
        }
    }
}
