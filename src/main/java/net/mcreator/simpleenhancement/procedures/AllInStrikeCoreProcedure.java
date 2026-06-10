package net.mcreator.simpleenhancement.procedures;

import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;

@EventBusSubscriber
public class AllInStrikeCoreProcedure {
    private static final ResourceKey<net.minecraft.world.item.enchantment.Enchantment> ENCHANT_KEY = ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("simple_enhancement", "all_in_strike"));

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getHand() != event.getEntity().getUsedItemHand()) return;

        Player player = event.getEntity();
        ItemStack mainhand = player.getMainHandItem();
        if (mainhand.isEmpty()) return;

        int enchantLevel = mainhand.getEnchantmentLevel(event.getLevel().registryAccess().holderOrThrow(ENCHANT_KEY));
        if (enchantLevel <= 0) return;

        if (event.getLevel() instanceof ServerLevel serverLevel) {
            mainhand.hurtAndBreak((int) (mainhand.getMaxDamage() * 0.2), serverLevel, player, _stk -> {});
        }

        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 200, 1));
    }
}