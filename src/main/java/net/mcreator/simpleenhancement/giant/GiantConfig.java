package net.mcreator.simpleenhancement.giant;

public class GiantConfig {
    // 视觉、碰撞箱、相机高度统一缩放倍数（SCALE 属性控制）
    public static final float MODEL_SCALE = 5.0f;

    // 属性倍数
    public static final float SPEED_MULTIPLIER = 2.0f;
    public static final float JUMP_MULTIPLIER = 3.0f;
    public static final float REACH_MULTIPLIER = 3.0f;

    // 直接增加值
    public static final int HEALTH_BOOST = 40;
    public static final int DAMAGE_BOOST = 2;

    // 持续时间（刻）
    public static final int DURATION_TICKS = 20 * 60;   // 60秒

    // 范围破坏半径
    public static final int BREAK_RADIUS = 1;

    // ========== 自动跨坑参数 ==========
    public static final double FORWARD_DISTANCE = 6.0;       // 向前检测距离（格）
    public static final double SIDE_HALF = 3.0;              // 左右检测宽度的一半（格）
    public static final double HOLE_RATIO_THRESHOLD = 0.6;    // 空洞比例阈值（超过则触发跳跃）
    public static final int AUTO_JUMP_COOLDOWN = 30;          // 自动跳跃冷却（刻）
    public static final double DYNAMIC_BOOST_MULTIPLIER = 1.0; // 推力倍率系数（最终推力 = 1 + holeRatio * 该系数）
    public static final double AUTO_JUMP_VERTICAL_BOOST = 1.5;  // 自动跨坑垂直倍数

    public static final float STEP_HEIGHT_GIANT = 5.0f;

    // ========== 摔落伤害减免参数 ==========
    public static final float FALL_SAFE_OFFSET = 6.0F;          // 安全高度额外偏移（巨人比普通玩家多6格安全高度）
    public static final float FALL_MAX_REDUCTION_DIST = 20.0F;  // 最大减免参考距离（超过此距离，减免比例不再增加）
    public static final float FALL_REDUCTION_FACTOR = 0.5F;     // 最高减免比例（50%）
}