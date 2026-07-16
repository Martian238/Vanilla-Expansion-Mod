package VanillaExpansion.expand.world.block;

import arc.util.io.Reads;
import arc.util.io.Writes;

/**
 * 16方向旋转值存储类
 * 用于存储和管理方块的16方向旋转数据
 */
public class SixteenDirectionData {
    
    /** 方向总数 */
    public static final int DIRECTIONS = 16;
    
    /** 每个方向的角度（22.5度） */
    public static final float DEG_PER_DIRECTION = 360f / DIRECTIONS;
    
    /** 存储的16方向旋转值（0-15） */
    protected int rotation = 0;
    
    /**
     * 默认构造函数
     */
    public SixteenDirectionData() {}
    
    /**
     * 带初始旋转值的构造函数
     * @param rotation 初始旋转值（0-15）
     */
    public SixteenDirectionData(int rotation) {
        this.rotation = normalize(rotation);
    }
    
    /**
     * 获取16方向旋转值
     * @return 旋转值（0-15）
     */
    public int getRotation() {
        return rotation;
    }
    
    /**
     * 设置16方向旋转值
     * @param rotation 旋转值（会被规范化到0-15）
     */
    public void setRotation(int rotation) {
        this.rotation = normalize(rotation);
    }
    
    /**
     * 获取旋转角度（度数）
     * @return 角度值（0-360度）
     */
    public float getRotationDeg() {
        return rotation * DEG_PER_DIRECTION;
    }
    
    /**
     * 获取旋转角度（弧度）
     * @return 弧度值
     */
    public float getRotationRad() {
        return (float) Math.toRadians(getRotationDeg());
    }
    
    /**
     * 判断是否为垂直方向（上、右、下、左）
     * @return true如果是垂直方向（0, 4, 8, 12）
     */
    public boolean isCardinalDirection() {
        return rotation % 4 == 0;
    }
    
    /**
     * 转换为4方向值
     * @return 4方向值（0-3），如果不是垂直方向返回-1
     */
    public int toCardinalDirection() {
        if (isCardinalDirection()) {
            return rotation / 4;
        }
        return -1;
    }
    
    /**
     * 从4方向值设置旋转
     * @param cardinalRotation 4方向值（0-3）
     */
    public void fromCardinalDirection(int cardinalRotation) {
        this.rotation = normalize(cardinalRotation * 4);
    }
    
    /**
     * 旋转指定数量的方向
     * @param steps 旋转步数（可正可负）
     */
    public void rotate(int steps) {
        rotation = normalize(rotation + steps);
    }
    
    /**
     * 翻转180度（8个方向）
     */
    public void flip() {
        rotate(DIRECTIONS / 2);
    }
    
    /**
     * 规范化旋转值到0-15范围
     * @param rotation 输入的旋转值
     * @return 规范化后的旋转值（0-15）
     */
    private int normalize(int rotation) {
        rotation = rotation % DIRECTIONS;
        if (rotation < 0) {
            rotation += DIRECTIONS;
        }
        return rotation;
    }
    
    /**
     * 序列化写入
     * @param write 写入器
     */
    public void write(Writes write) {
        write.s(rotation);
    }
    
    /**
     * 反序列化读取
     * @param read 读取器
     */
    public void read(Reads read) {
        rotation = read.s();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SixteenDirectionData other = (SixteenDirectionData) obj;
        return rotation == other.rotation;
    }
    
    @Override
    public int hashCode() {
        return rotation;
    }
    
    @Override
    public String toString() {
        return "SixteenDirectionData{" +
                "rotation=" + rotation +
                ", deg=" + getRotationDeg() +
                '}';
    }
}