/*
 * 怪物之友 - 手持时怪物不攻击你，你也无法伤害怪物
 */
package net.mcreator.simpleenhancement;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

@EventBusSubscriber(modid = "simple_enhancement")
public class MobFriendCore {

    private static final ResourceLocation ENCHANT_ID = ResourceLocation.fromNamespaceAndPath("simple_enhancement", "mob_friend");
    private static final ResourceKey<net.minecraft.world.item.enchantment.Enchantment> ENCHANT_KEY = ResourceKey.create(Registries.ENCHANTMENT, ENCHANT_ID);

    private static int getEnchantLevel(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof SwordItem)) return 0;
        Registry<net.minecraft.world.item.enchantment.Enchantment> registry = player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        Holder<net.minecraft.world.item.enchantment.Enchantment> enchant = registry.getHolderOrThrow(ENCHANT_KEY);
        return stack.getEnchantmentLevel(enchant);
    }

    // 阻止玩家对怪物造成伤害
    @SubscribeEvent
    public static void onPlayerDamageMonster(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (!(event.getEntity() instanceof Mob)) return;
        if (getEnchantLevel(player) > 0) {
            event.setCanceled(true);
        }
    }

    // 阻止怪物对玩家造成伤害
    @SubscribeEvent
    public static void onMonsterDamagePlayer(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Mob)) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (getEnchantLevel(player) > 0) {
            event.setCanceled(true);
        }
    }
}