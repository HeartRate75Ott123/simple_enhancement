package net.mcreator.simpleenhancement.giant;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 巨人形态核心事件处理器。
 * 负责变身/重置、属性修改、自动跨坑、范围破坏以及对抗拔刀剑模组的步高重置。
 */
@EventBusSubscriber(modid = "simpleenhancement")
@SuppressWarnings({"resource", "SpellCheckingInspection", "ExtractVariable"})
public class GiantEventHandler {

    static {
        NeoForge.EVENT_BUS.register(GiantEventHandler.class);
        System.out.println("[Giant] Server event handler registered");
    }

    // 巨人模型缩放时，调整玩家垂直位置的系数
    private static final float POSITION_ADJUST_FACTOR = 0.9f;

    // ========================== 变身核心 ==========================

    /**
     * 使玩家进入巨人形态。
     * @param player    目标玩家
     * @param itemstack 触发变身的物品（消耗品）
     */
    public static void becomeGiant(Player player, ItemStack itemstack) {
        if (player.level().isClientSide) return;
        CompoundTag data = player.getPersistentData();

        long endTime = player.level().getGameTime() + GiantConfig.DURATION_TICKS;

        // 已处于巨人状态 → 刷新持续时间
        if (data.getBoolean("isGiant")) {
            data.putLong("giantEndTime", endTime);
            player.removeEffect(MobEffects.DAMAGE_BOOST);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, GiantConfig.DURATION_TICKS, 1, false, false, true));
            System.out.println("[Giant] " + player.getName().getString() + " refreshed giant duration");
            if (!player.isCreative() && !player.isSpectator()) {
                itemstack.shrink(1);
            }
            return;
        }

        // 首次变身
        data.putBoolean("isGiant", true);
        data.putLong("giantEndTime", endTime);
        applyAttributes(player, true);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, GiantConfig.DURATION_TICKS, 1, false, false, true));
        adjustPosition(player, true);
        player.refreshDimensions();

        if (!player.isCreative() && !player.isSpectator()) {
            itemstack.shrink(1);
        }
        System.out.println("[Giant] " + player.getName().getString() + " became giant");
    }

    /**
     * 强制退出巨人形态。
     */
    public static void resetGiant(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.getBoolean("isGiant")) return;

        data.putBoolean("isGiant", false);
        data.remove("giantEndTime");
        applyAttributes(player, false);
        adjustPosition(player, false);
        player.refreshDimensions();
        player.removeEffect(MobEffects.DAMAGE_BOOST);
        player.displayClientMessage(Component.translatable("message.simpleenhancement.mutation_ended"), true);
        System.out.println("[Giant] " + player.getName().getString() + " reset");
    }

    // ========================== 属性与模型 ==========================

    /**
     * 应用/移除巨人相关属性（速度、跳跃、范围、缩放、生命、攻击等）。
     */
    private static void applyAttributes(Player player, boolean giant) {
        double speed = giant ? 0.1 * GiantConfig.SPEED_MULTIPLIER : 0.1;
        double jump = giant ? 0.42 * GiantConfig.JUMP_MULTIPLIER : 0.42;
        double reach = giant ? 3.0 * GiantConfig.REACH_MULTIPLIER : 3.0;
        double blockReach = giant ? 4.5 * GiantConfig.REACH_MULTIPLIER : 4.5;

        setAttr(player, Attributes.MOVEMENT_SPEED, speed);
        setAttr(player, Attributes.JUMP_STRENGTH, jump);
        setAttr(player, Attributes.ENTITY_INTERACTION_RANGE, reach);
        setAttr(player, Attributes.BLOCK_INTERACTION_RANGE, blockReach);

        AttributeInstance scaleAttr = player.getAttribute(Attributes.SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(giant ? GiantConfig.MODEL_SCALE : 1.0);
        }

        ResourceLocation healthId = ResourceLocation.parse("simpleenhancement:giant_health");
        ResourceLocation damageId = ResourceLocation.parse("simpleenhancement:giant_damage");

        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (giant) {
            if (health != null && health.getModifier(healthId) == null) {
                health.addPermanentModifier(new AttributeModifier(healthId, GiantConfig.HEALTH_BOOST, AttributeModifier.Operation.ADD_VALUE));
                player.setHealth(player.getHealth() + GiantConfig.HEALTH_BOOST);
            }
            AttributeInstance damage = player.getAttribute(Attributes.ATTACK_DAMAGE);
            if (damage != null && damage.getModifier(damageId) == null) {
                damage.addPermanentModifier(new AttributeModifier(damageId, GiantConfig.DAMAGE_BOOST, AttributeModifier.Operation.ADD_VALUE));
            }
        } else {
            if (health != null) health.removeModifier(healthId);
            AttributeInstance damage = player.getAttribute(Attributes.ATTACK_DAMAGE);
            if (damage != null) damage.removeModifier(damageId);
            if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
        }
    }

    private static void setAttr(Player player, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, double value) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst != null) inst.setBaseValue(value);
    }

    /**
     * 根据巨人缩放修正玩家位置，避免卡地板或浮空。
     */
    private static void adjustPosition(Player player, boolean giant) {
        float deltaY = (GiantConfig.MODEL_SCALE - 1f) * POSITION_ADJUST_FACTOR;
        if (giant) player.setPos(player.getX(), player.getY() + deltaY, player.getZ());
        else player.setPos(player.getX(), player.getY() - deltaY, player.getZ());
    }

    // ========================== 登录/重生恢复 ==========================

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        restoreGiantState(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        restoreGiantState(event.getEntity());
    }

    /**
     * 玩家重新进入世界时恢复巨人状态（通过 NBT 或碰撞箱/缩放检测）。
     */
    private static void restoreGiantState(Player player) {
        if (player.level().isClientSide) return;
        CompoundTag data = player.getPersistentData();
        boolean isGiantFlag = data.getBoolean("isGiant");
        long endTime = data.getLong("giantEndTime");
        long currentTime = player.level().getGameTime();

        boolean shouldBeGiant = isGiantFlag;
        if (!shouldBeGiant) {
            AttributeInstance scaleAttr = player.getAttribute(Attributes.SCALE);
            if (scaleAttr != null && scaleAttr.getBaseValue() > 1.0) {
                shouldBeGiant = true;
                data.putBoolean("isGiant", true);
                System.out.println("[Giant] Detected giant via SCALE for " + player.getName().getString());
            } else if (player.getBoundingBox().getYsize() > 2.0) {
                shouldBeGiant = true;
                data.putBoolean("isGiant", true);
                System.out.println("[Giant] Detected giant via bounding box for " + player.getName().getString());
            }
        }

        if (shouldBeGiant) {
            if (endTime > currentTime) {
                applyAttributes(player, true);
                int remainingTicks = (int)(endTime - currentTime);
                if (remainingTicks > 0 && player.getEffect(MobEffects.DAMAGE_BOOST) == null) {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, remainingTicks, 1, false, false, true));
                }
                adjustPosition(player, true);
                player.refreshDimensions();
                System.out.println("[Giant] Restored giant state for " + player.getName().getString() + ", remaining ticks: " + remainingTicks);
            } else {
                // 时间已过期，清除巨人状态
                data.putBoolean("isGiant", false);
                data.remove("giantEndTime");
                applyAttributes(player, false);
                adjustPosition(player, false);
                player.refreshDimensions();
                System.out.println("[Giant] Invalid or expired endTime, cleared giant state for " + player.getName().getString());
            }
        } else {
            // 清理残留的巨人属性
            AttributeInstance scaleAttr = player.getAttribute(Attributes.SCALE);
            if (scaleAttr != null && scaleAttr.getBaseValue() != 1.0) {
                applyAttributes(player, false);
                adjustPosition(player, false);
                player.refreshDimensions();
                System.out.println("[Giant] Cleaned up orphaned giant attributes for " + player.getName().getString());
            }
        }
    }

    // ========================== 每 tick 处理 ==========================

    // 自动跨坑冷却记录
    private static final Map<UUID, Integer> lastAutoJump = new HashMap<>();

    /**
     * 【关键】在玩家 tick 结束后，以最低优先级强制设置巨人步高属性。
     * 此方法专门用于对抗拔刀剑模组（SlashBladeResharped）在 handleTickEnd 中强制将步高重置为 0.6 的问题。
     * 原理：拔刀剑的步高重置发生在 PlayerTickEvent 的某个阶段（可能是 Pre 或 Post），
     * 通过使用 {@link EventPriority#LOWEST} 确保本监听器在所有其他监听器之后执行，
     * 从而将步高重新锁定为巨人步高（默认 5.0）。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (!player.getPersistentData().getBoolean("isGiant")) return;

        AttributeInstance step = player.getAttribute(Attributes.STEP_HEIGHT);
        if (step != null && Math.abs(step.getBaseValue() - GiantConfig.STEP_HEIGHT_GIANT) > 0.001) {
            step.setBaseValue(GiantConfig.STEP_HEIGHT_GIANT);
        }
    }

    /**
     * 在主 tick 中处理巨人到期检查、自动跨坑（防坠落）。
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        CompoundTag data = player.getPersistentData();

        // 到期检测
        if (data.getBoolean("isGiant")) {
            long endTime = data.getLong("giantEndTime");
            if (player.level().getGameTime() >= endTime) {
                resetGiant(player);
                return;
            }
        } else {
            return;
        }

        // ===== 自动跨坑（防坠落） =====
        if (!player.onGround()) return;

        int forwardSteps = (int) Math.ceil(GiantConfig.FORWARD_DISTANCE);
        int sideSteps = (int) Math.ceil(GiantConfig.SIDE_HALF);
        int totalHoleBlocks = 0;
        int scannedBlocks = 0;
        float yaw = player.getYRot();

        for (int dx = 1; dx <= forwardSteps; dx++) {
            for (int dz = -sideSteps; dz <= sideSteps; dz++) {
                double angleRad = Math.toRadians(yaw);
                double forwardX = Math.sin(angleRad) * dx;
                double forwardZ = Math.cos(angleRad) * dx;
                double rightX = Math.sin(angleRad + Math.PI / 2) * dz;
                double rightZ = Math.cos(angleRad + Math.PI / 2) * dz;
                BlockPos checkPos = new BlockPos(
                        (int) Math.floor(player.getX() + forwardX + rightX),
                        (int) Math.floor(player.getY()),
                        (int) Math.floor(player.getZ() + forwardZ + rightZ)
                );
                BlockPos belowPos = checkPos.below();
                Level level = player.level();
                if (level.isEmptyBlock(checkPos) && level.isEmptyBlock(belowPos)) {
                    totalHoleBlocks++;
                }
                scannedBlocks++;
            }
        }

        double holeRatio = (double) totalHoleBlocks / scannedBlocks;
        if (holeRatio > GiantConfig.HOLE_RATIO_THRESHOLD) {
            UUID uuid = player.getUUID();
            int currentTick = player.tickCount;
            int lastTick = lastAutoJump.getOrDefault(uuid, 0);
            if (currentTick - lastTick > GiantConfig.AUTO_JUMP_COOLDOWN) {
                double dynamicBoost = 1.0 + holeRatio * GiantConfig.DYNAMIC_BOOST_MULTIPLIER;
                player.jumpFromGround();
                Vec3 motion = player.getDeltaMovement();
                player.setDeltaMovement(
                        motion.x * dynamicBoost,
                        motion.y * GiantConfig.AUTO_JUMP_VERTICAL_BOOST,
                        motion.z * dynamicBoost
                );
                lastAutoJump.put(uuid, currentTick);
                System.out.println("[Giant] Auto-jump, hole ratio=" + holeRatio + ", boost=" + dynamicBoost);
            }
        }
    }

    // ========================== 范围破坏 ==========================

    private static final ThreadLocal<Boolean> isProcessingBreak = ThreadLocal.withInitial(() -> false);

    /**
     * 巨人破坏方块时，同时破坏周围一定范围内的方块。
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (isProcessingBreak.get()) return;
        Player player = event.getPlayer();
        if (!player.getPersistentData().getBoolean("isGiant")) return;

        Level level = (Level) event.getLevel();
        BlockPos center = event.getPos();
        int r = GiantConfig.BREAK_RADIUS;
        isProcessingBreak.set(true);
        try {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos pos = center.offset(dx, dy, dz);
                        BlockState state = level.getBlockState(pos);
                        if (state.getDestroySpeed(level, pos) >= 0) {
                            level.destroyBlock(pos, true, player);
                        }
                    }
                }
            }
        } finally {
            isProcessingBreak.set(false);
        }
    }

    /**
     * 为巨人形态提供基于身高的摔落伤害减免。
     * 原理：将安全下落高度从原版3格提升至约9格，并根据下落距离按比例减免伤害。
     * 此监听器使用 HIGH 优先级，确保在其他摔落伤害修改之前生效。
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.getPersistentData().getBoolean("isGiant")) return;

        float original = event.getDistance();
        float adjusted = Math.max(0, original - GiantConfig.FALL_SAFE_OFFSET);
        float reduction = Math.min(1.0F, original / GiantConfig.FALL_MAX_REDUCTION_DIST);
        float finalDist = (float) (adjusted * (1.0 - reduction * GiantConfig.FALL_REDUCTION_FACTOR));

        if (finalDist <= 0) {
            event.setCanceled(true);
        } else {
            event.setDistance(finalDist);
        }
    }
}