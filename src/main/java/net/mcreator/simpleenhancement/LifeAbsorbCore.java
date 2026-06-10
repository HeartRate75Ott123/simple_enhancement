/*
 * 生命窃取 - 按实际伤害比例回血（1级20%，2级35%，3级50%）
 */
package net.mcreator.simpleenhancement;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

@EventBusSubscriber(modid = "simple_enhancement")
public class LifeAbsorbCore {

    private static final ResourceLocation ENCHANT_ID = ResourceLocation.fromNamespaceAndPath("simple_enhancement", "life_absorb");
    private static final ResourceKey<net.minecraft.world.item.enchantment.Enchantment> ENCHANT_KEY = ResourceKey.create(Registries.ENCHANTMENT, ENCHANT_ID);

    private static int getEnchantLevel(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof SwordItem)) return 0;
        Registry<net.minecraft.world.item.enchantment.Enchantment> registry = player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        Holder<net.minecraft.world.item.enchantment.Enchantment> enchant = registry.getHolderOrThrow(ENCHANT_KEY);
        return stack.getEnchantmentLevel(enchant);
    }

    @SubscribeEvent
    public static void onPlayerDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) return;

        int level = getEnchantLevel(player);
        if (level <= 0) return;

        // 获取实际造成的伤害（经过护甲等减伤后的数值）
        float damage = event.getContainer().getNewDamage();
        if (damage <= 0) return;

        // 根据等级计算吸血比例：1级20%，2级35%，3级50%
        float ratio;
        switch (level) {
            case 1: ratio = 0.20f; break;
            case 2: ratio = 0.35f; break;
            case 3: ratio = 0.50f; break;
            default: return;
        }

        float healAmount = damage * ratio;
        if (healAmount > 0) {
            float newHealth = player.getHealth() + healAmount;
            float maxHealth = player.getMaxHealth();
            player.setHealth(Math.min(newHealth, maxHealth));
        }
    }
}