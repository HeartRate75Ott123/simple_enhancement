/*
 * The code of this mod element is always locked.
 */
package net.mcreator.simpleenhancement;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodData;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

@EventBusSubscriber(modid = "simple_enhancement")
public class SwiftStrikeCore {

    private static final ResourceLocation ENCHANT_ID = ResourceLocation.fromNamespaceAndPath("simple_enhancement", "swift_strike");
    private static final ResourceKey<net.minecraft.world.item.enchantment.Enchantment> ENCHANT_KEY = ResourceKey.create(Registries.ENCHANTMENT, ENCHANT_ID);
    private static final ResourceLocation ATTACK_SPEED_MOD = ResourceLocation.fromNamespaceAndPath("simple_enhancement", "swift_strike_attack_speed");

    /**
     * 获取玩家手持剑上的迅击附魔等级
     */
    private static int getSwiftStrikeLevel(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof SwordItem)) return 0;
        Registry<net.minecraft.world.item.enchantment.Enchantment> registry = player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        Holder<net.minecraft.world.item.enchantment.Enchantment> enchant = registry.getHolderOrThrow(ENCHANT_KEY);
        return stack.getEnchantmentLevel(enchant);
    }

    /**
     * 每 tick 更新攻击速度属性（随等级提升）
     * 每级增加 40% 攻击速度（乘法），三级共 +120%
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        int level = getSwiftStrikeLevel(player);
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);

        if (attackSpeed != null) {
            attackSpeed.removeModifier(ATTACK_SPEED_MOD);
            if (level > 0) {
                // 每级 +40% 攻击速度（例如 1级 +40%，2级 +80%，3级 +120%）
                double speedBonus = 0.4 * level;
                attackSpeed.addTransientModifier(new AttributeModifier(ATTACK_SPEED_MOD, speedBonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
    }

    /**
     * 降低伤害（固定减2，不低于1）并增加饱食度消耗
     */
    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        LivingEntity target = event.getEntity();
        
        int level = getSwiftStrikeLevel(player);
        if (level <= 0) return;

        // 1. 降低伤害：减2点，最低1点
        float originalDamage = event.getContainer().getNewDamage();
        float newDamage = Math.max(1.0F, originalDamage - 2.0F);
        event.getContainer().setNewDamage(newDamage);

        // 2. 额外饱食度消耗：每级增加 0.2 exhaustion
        // 注意：原版攻击本身就消耗 0.1 exhaustion，这里额外增加
        FoodData foodData = player.getFoodData();
        float extraExhaustion = level * 0.2F;
        foodData.addExhaustion(extraExhaustion);
    }
}