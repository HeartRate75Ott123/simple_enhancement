/*
 * The code of this mod element is always locked.
 */
package net.mcreator.simpleenhancement;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
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
public class HeavyStrikeCore {

    private static final ResourceLocation ENCHANT_ID = ResourceLocation.fromNamespaceAndPath("simple_enhancement", "heavy_strike");
    private static final ResourceKey<net.minecraft.world.item.enchantment.Enchantment> ENCHANT_KEY = ResourceKey.create(Registries.ENCHANTMENT, ENCHANT_ID);
    private static final ResourceLocation ATTACK_SPEED_MOD = ResourceLocation.fromNamespaceAndPath("simple_enhancement", "heavy_strike_attack_speed");

    /**
     * 获取玩家手持剑上的重击附魔等级
     */
    private static int getHeavyStrikeLevel(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof SwordItem)) return 0;
        Registry<net.minecraft.world.item.enchantment.Enchantment> registry = player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        Holder<net.minecraft.world.item.enchantment.Enchantment> enchant = registry.getHolderOrThrow(ENCHANT_KEY);
        return stack.getEnchantmentLevel(enchant);
    }

    /**
     * 每 tick 更新攻击速度属性（固定降低 30%）
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        int level = getHeavyStrikeLevel(player);
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);

        if (attackSpeed != null) {
            attackSpeed.removeModifier(ATTACK_SPEED_MOD);
            if (level > 0) {
                // 固定降低 30% 攻击速度
                attackSpeed.addTransientModifier(new AttributeModifier(ATTACK_SPEED_MOD, -0.3, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
    }

    /**
     * 增加伤害（每级 +2，即1级+2，2级+4，3级+6）并增加饱食度消耗（固定 +0.4）
     */
    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        
        int level = getHeavyStrikeLevel(player);
        if (level <= 0) return;

        // 1. 增加伤害：每级 +2 点
        float originalDamage = event.getContainer().getNewDamage();
        float extraDamage = level * 2.0F;   // 2,4,6
        float newDamage = originalDamage + extraDamage;
        event.getContainer().setNewDamage(newDamage);

        // 2. 额外饱食度消耗：固定 0.4 饥饿度
        FoodData foodData = player.getFoodData();
        foodData.addExhaustion(0.4F);
    }
}