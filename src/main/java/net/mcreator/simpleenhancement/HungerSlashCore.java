/*
 * 充饥 (Hunger Slash) 附魔核心实现
 * 攻击到敌人时，回复玩家的饥饿值（食物水平）
 * 回复量 = 2 + (附魔等级 - 1)  → 1级+2，2级+3，3级+4
 */
package net.mcreator.simpleenhancement;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.food.FoodData;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

@EventBusSubscriber(modid = "simple_enhancement")
public class HungerSlashCore {

    private static final ResourceLocation ENCHANT_ID = ResourceLocation.fromNamespaceAndPath("simple_enhancement", "hunger_slash");
    private static final ResourceKey<net.minecraft.world.item.enchantment.Enchantment> ENCHANT_KEY = ResourceKey.create(Registries.ENCHANTMENT, ENCHANT_ID);

    private static int getEnchantLevel(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof SwordItem)) return 0;
        Registry<net.minecraft.world.item.enchantment.Enchantment> registry = player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        Holder<net.minecraft.world.item.enchantment.Enchantment> enchant = registry.getHolderOrThrow(ENCHANT_KEY);
        return stack.getEnchantmentLevel(enchant);
    }

    @SubscribeEvent
    public static void onPlayerAttack(LivingIncomingDamageEvent event) {
        // 只有玩家造成的伤害才处理
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        // 被攻击的目标
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) return;

        int level = getEnchantLevel(player);
        if (level <= 0) return;

        // 计算回复量：基础 2 点，每级 +1（1级2，2级3，3级4）
        int restoreAmount = 1 + level;   // level=1→2, level=2→3, level=3→4

        FoodData foodData = player.getFoodData();
        int currentFood = foodData.getFoodLevel();
        int newFood = Math.min(20, currentFood + restoreAmount); // 上限20
        foodData.setFoodLevel(newFood);

        // 可选：播放进食音效或粒子，增强反馈
        // player.playSound(net.minecraft.sounds.SoundEvents.PLAYER_BURP, 0.5F, 1.0F);
    }
}