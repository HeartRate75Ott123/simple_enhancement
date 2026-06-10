package net.mcreator.simpleenhancement.procedures;

import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;

@EventBusSubscriber
public class GuardStrikeCoreProcedure {
    private static final ResourceLocation ENCHANT_ID = ResourceLocation.fromNamespaceAndPath("simple_enhancement", "guard_strike");
    private static final ResourceKey<net.minecraft.world.item.enchantment.Enchantment> ENCHANT_KEY = ResourceKey.create(Registries.ENCHANTMENT, ENCHANT_ID);

    @SubscribeEvent
    public static void onEntityAttacked(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getContainer().getSource().getEntity() instanceof Player attacker)) return;
        if (attacker.getMainHandItem().isEmpty()) return;

        int enchantLevel = attacker.getMainHandItem().getEnchantmentLevel(attacker.level().registryAccess().holderOrThrow(ENCHANT_KEY));
        if (enchantLevel <= 0) return;

        attacker.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 40, enchantLevel - 1));
    }
}