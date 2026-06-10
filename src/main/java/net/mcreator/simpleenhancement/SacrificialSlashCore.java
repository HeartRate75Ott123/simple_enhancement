/*
 * 殉斩 - 右键释放剑气，消耗生命，范围穿透伤害（衰减），粒子特效
 * 剑气会被固体方块阻挡（草、花、藤蔓等非固体不阻挡）
 */
package net.mcreator.simpleenhancement;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@EventBusSubscriber(modid = "simple_enhancement")
public class SacrificialSlashCore {

    private static final ResourceLocation ENCHANT_ID = ResourceLocation.fromNamespaceAndPath("simple_enhancement", "sacrificial_slash");
    private static final ResourceKey<net.minecraft.world.item.enchantment.Enchantment> ENCHANT_KEY = ResourceKey.create(Registries.ENCHANTMENT, ENCHANT_ID);

    private static int getEnchantLevel(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof SwordItem)) return 0;
        Registry<net.minecraft.world.item.enchantment.Enchantment> registry = player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        Holder<net.minecraft.world.item.enchantment.Enchantment> enchant = registry.getHolderOrThrow(ENCHANT_KEY);
        return stack.getEnchantmentLevel(enchant);
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        int level = getEnchantLevel(player);
        if (level <= 0) return;

        // 客户端：生成粒子特效（需检查生命值，并考虑方块阻挡）
        if (player.level().isClientSide()) {
            if (player.getHealth() > 4.0F) {
                Vec3 eyePos = player.getEyePosition();
                Vec3 look = player.getLookAngle();
                double maxRange = getMaxRangeBeforeBlock(player, eyePos, look, 8.0);
                spawnSlashParticles(player, eyePos, look, maxRange);
            }
            return;
        }

        // 服务端：执行逻辑
        if (player.getHealth() <= 4.0F) return;
        player.setHealth(player.getHealth() - 4.0F);
        player.playSound(SoundEvents.PLAYER_HURT, 1.0F, 1.0F);

        // 添加短冷却（5 tick = 0.25秒），防止超高频点击
        player.getCooldowns().addCooldown(player.getMainHandItem().getItem(), 5);

        double range = 8.0;
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle();

        // 射线步进检测实体（步长0.5格），遇到固体方块则停止
        double step = 0.5;
        List<LivingEntity> hitEntities = new ArrayList<>();
        for (double d = step; d <= range; d += step) {
            Vec3 point = start.add(direction.scale(d));
            BlockPos pos = BlockPos.containing(point);
            BlockState state = player.level().getBlockState(pos);
            if (state.isSolid() && !state.isAir()) {
                break;
            }
            AABB box = new AABB(point.x - 0.5, point.y - 0.5, point.z - 0.5,
                                 point.x + 0.5, point.y + 0.5, point.z + 0.5);
            List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class, box,
                    e -> e != player && e.isAlive() && !hitEntities.contains(e));
            if (!entities.isEmpty()) {
                entities.sort(Comparator.comparingDouble(e -> e.distanceToSqr(point)));
                hitEntities.add(entities.get(0));
            }
        }

        if (hitEntities.isEmpty()) return;

        // 基础伤害计算
        float baseDamage = 2 + 2 * level;
        float multiplier = 1 + (player.experienceLevel / 50.0f);
        float fullDamage = baseDamage * multiplier;

        // 伤害衰减序列（第一个100%，后续依次70%）
        float[] multipliers = {1.0f, 0.7f, 0.49f, 0.343f, 0.2401f, 0.168f};
        int idx = 0;
        for (LivingEntity target : hitEntities) {
            float damage = fullDamage * (idx < multipliers.length ? multipliers[idx] : 0.12f);
            if (damage < 0.5f) break;
            target.hurt(player.damageSources().playerAttack(player), damage);
            target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 1.2f);
            idx++;
        }
    }

    /**
     * 获取从起点沿方向到第一个固体方块的距离（最大不超过 maxRange）
     */
    private static double getMaxRangeBeforeBlock(Player player, Vec3 start, Vec3 direction, double maxRange) {
        double step = 0.2;
        for (double d = step; d <= maxRange; d += step) {
            Vec3 point = start.add(direction.scale(d));
            BlockPos pos = BlockPos.containing(point);
            BlockState state = player.level().getBlockState(pos);
            if (state.isSolid() && !state.isAir()) {
                return d - step;
            }
        }
        return maxRange;
    }

    // 生成剑气粒子轨迹（客户端，只生成到被阻挡位置）
    private static void spawnSlashParticles(Player player, Vec3 start, Vec3 direction, double length) {
        int steps = Math.max(10, (int) (length * 2.5));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            Vec3 point = start.add(direction.scale(length * t));
            player.level().addParticle(ParticleTypes.GUST, point.x, point.y, point.z, 0, 0, 0);
            if (t > 0 && t < 1) {
                player.level().addParticle(ParticleTypes.FIREWORK, point.x, point.y, point.z, 0, 0, 0);
            }
        }
    }
}