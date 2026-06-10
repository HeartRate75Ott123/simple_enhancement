/*
 * The code of this mod element is always locked.
 */
package net.mcreator.simpleenhancement;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = "simple_enhancement")
public class ReachStrikeCore {

    private static final ResourceLocation ENCHANT_ID = ResourceLocation.fromNamespaceAndPath("simple_enhancement", "reach_strike");
    private static final ResourceKey<net.minecraft.world.item.enchantment.Enchantment> ENCHANT_KEY = ResourceKey.create(Registries.ENCHANTMENT, ENCHANT_ID);
    private static final ResourceLocation ATTACK_SPEED_MOD = ResourceLocation.fromNamespaceAndPath("simple_enhancement", "attack_speed_penalty");
    private static final ResourceLocation MOVE_SPEED_MOD = ResourceLocation.fromNamespaceAndPath("simple_enhancement", "move_speed_penalty");
    private static final ResourceLocation ATTACK_RANGE_MOD = ResourceLocation.fromNamespaceAndPath("simple_enhancement", "attack_range_boost");

    private static final Set<LivingEntity> sweepingEntities = new HashSet<>();

    // ==========================
    // 1. 动态属性更新：攻击速度、移动速度、攻击距离
    // ==========================
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        ItemStack stack = player.getMainHandItem();
        int level = getEnchantLevel(player, stack);
        boolean valid = level > 0 && stack.getItem() instanceof SwordItem;

        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        AttributeInstance moveSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        // [关键修改] 使用 1.21 的实体交互范围属性
        AttributeInstance attackRange = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);

        if (attackSpeed != null) attackSpeed.removeModifier(ATTACK_SPEED_MOD);
        if (moveSpeed != null) moveSpeed.removeModifier(MOVE_SPEED_MOD);
        if (attackRange != null) attackRange.removeModifier(ATTACK_RANGE_MOD);

        if (valid) {
            double speedPenalty = 0.1 * level;       // -10%, -20%, -30%
            // 攻击距离加成：等级1 +2格 -> 属性增加2；等级2 +3；等级3 +4
            double rangeBonus = level + 1;           // 2,3,4

            if (attackSpeed != null) {
                attackSpeed.addTransientModifier(new AttributeModifier(ATTACK_SPEED_MOD, -speedPenalty, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
            if (moveSpeed != null) {
                moveSpeed.addTransientModifier(new AttributeModifier(MOVE_SPEED_MOD, -speedPenalty, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
            if (attackRange != null) {
                // [关键修改] 直接增加绝对值，基础值 3.0
                attackRange.addTransientModifier(new AttributeModifier(ATTACK_RANGE_MOD, rangeBonus, AttributeModifier.Operation.ADD_VALUE));
            }
        }
    }

    // ==========================
    // 2. 横扫范围扩大：每级 +50%（半径乘数 1.5/2.0/2.5）
    // ==========================
    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        LivingEntity target = event.getEntity();
        if (sweepingEntities.contains(target)) return;

        ItemStack stack = player.getMainHandItem();
        int level = getEnchantLevel(player, stack);
        if (level <= 0 || !(stack.getItem() instanceof SwordItem)) return;

        double expand = 1.0 + (0.5 * level);   // 1.5, 2.0, 2.5
        AABB area = target.getBoundingBox().inflate(expand, 0.5, expand);
        List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e != target);

        float damage = event.getContainer().getNewDamage() * 0.5F;

        sweepingEntities.add(target);
        for (LivingEntity entity : entities) {
            if (sweepingEntities.contains(entity)) continue;
            sweepingEntities.add(entity);
            entity.hurt(player.damageSources().playerAttack(player), damage);
            sweepingEntities.remove(entity);
        }
        sweepingEntities.remove(target);
    }

    // ==========================
    // 辅助方法：获取附魔等级
    // ==========================
    private static int getEnchantLevel(Player player, ItemStack stack) {
        if (stack.isEmpty()) return 0;
        Registry<net.minecraft.world.item.enchantment.Enchantment> registry = player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        Holder<net.minecraft.world.item.enchantment.Enchantment> enchant = registry.getHolderOrThrow(ENCHANT_KEY);
        return stack.getEnchantmentLevel(enchant);
    }
}