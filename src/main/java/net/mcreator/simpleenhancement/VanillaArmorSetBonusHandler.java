/*
 * 原版盔甲套装效果处理器
 * 穿齐全套（皮革/金/锁链/铁/钻石/下界合金）获得抗性提升 I，无粒子效果
 * 优化：每 20 tick 检测一次，避免每 tick 开销
 */
package net.mcreator.simpleenhancement;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = "simple_enhancement")
public class VanillaArmorSetBonusHandler {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private static final int CHECK_INTERVAL = 20;
    private static final Map<Player, Long> lastCheckTime = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        long currentTime = player.level().getGameTime();
        Long last = lastCheckTime.get(player);
        if (last != null && currentTime - last < CHECK_INTERVAL) {
            return;
        }
        lastCheckTime.put(player, currentTime);

        String material = getFullSetMaterial(player);
        if (material != null) {
            MobEffectInstance current = player.getEffect(MobEffects.DAMAGE_RESISTANCE);
            if (current == null || current.getDuration() <= 20) {
                MobEffectInstance effect = new MobEffectInstance(
                    MobEffects.DAMAGE_RESISTANCE,
                    60, 0, false, false, true
                );
                player.addEffect(effect);
            }
        }
    }

    private static String getFullSetMaterial(Player player) {
        String material = null;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) return null;
            if (!(stack.getItem() instanceof ArmorItem armor)) return null;

            String mat = getArmorMaterialString(armor);
            if (mat == null) return null;

            if (material == null) {
                material = mat;
            } else if (!material.equals(mat)) {
                return null;
            }
        }
        return material;
    }

    private static String getArmorMaterialString(ArmorItem armor) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(armor);
        if (id == null) return null;
        String path = id.getPath();
        if (path.contains("leather")) return "leather";
        if (path.contains("chainmail")) return "chainmail";
        if (path.contains("iron")) return "iron";
        if (path.contains("gold")) return "gold";
        if (path.contains("diamond")) return "diamond";
        if (path.contains("netherite")) return "netherite";
        return null;
    }
}