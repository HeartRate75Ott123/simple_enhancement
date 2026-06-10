/*
 * 自定义盔甲套装效果处理器
 * 穿戴全套 netherite_diamond 装备时：
 * - 最大生命值 +20（属性修饰器）
 * - 获得抗性提升 II（无粒子）
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
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = "simple_enhancement")
public class CustomArmorSetBonusHandler {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private static final int CHECK_INTERVAL = 20;
    private static final Map<Player, Long> lastCheckTime = new HashMap<>();

    // 使用 ResourceLocation 作为修饰器标识
    private static final ResourceLocation HEALTH_BOOST_ID = ResourceLocation.fromNamespaceAndPath("simple_enhancement", "custom_armor_health_boost");

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

        boolean hasFullSet = isWearingFullSet(player);
        AttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);

        // 处理生命值属性修饰器
        if (healthAttr != null) {
            AttributeModifier oldMod = healthAttr.getModifier(HEALTH_BOOST_ID);
            if (hasFullSet && oldMod == null) {
                // 添加 +20 生命值（新构造函数：ResourceLocation, double, Operation）
                AttributeModifier modifier = new AttributeModifier(HEALTH_BOOST_ID, 20.0, AttributeModifier.Operation.ADD_VALUE);
                healthAttr.addPermanentModifier(modifier);
                // 如果当前生命值超过新的最大值，自动调整（原版自动处理）
                if (player.getHealth() > player.getMaxHealth()) {
                    player.setHealth(player.getMaxHealth());
                }
            } else if (!hasFullSet && oldMod != null) {
                healthAttr.removeModifier(HEALTH_BOOST_ID);
                // 移除后若当前生命值超过新的最大值，自动降低
                if (player.getHealth() > player.getMaxHealth()) {
                    player.setHealth(player.getMaxHealth());
                }
            }
        }

        // 处理抗性提升效果
        if (hasFullSet) {
            MobEffectInstance currentResist = player.getEffect(MobEffects.DAMAGE_RESISTANCE);
            if (currentResist == null || currentResist.getDuration() <= 20) {
                // 抗性提升 II（amplifier = 1），持续 40 tick，无粒子
                MobEffectInstance effect = new MobEffectInstance(
                    MobEffects.DAMAGE_RESISTANCE,
                    60, 1, false, false, true
                );
                player.addEffect(effect);
            }
        }
    }

    /**
     * 检测玩家是否穿戴全套 netherite_diamond 盔甲
     */
    private static boolean isWearingFullSet(Player player) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) return false;
            if (!(stack.getItem() instanceof ArmorItem)) return false;
            if (!isNetheriteDiamondArmor(stack)) return false;
        }
        return true;
    }

    /**
     * 判断物品是否为 netherite_diamond 盔甲（通过注册名包含 "netherite_diamond"）
     */
    private static boolean isNetheriteDiamondArmor(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) return false;
        String path = id.getPath();
        return path.contains("netherite_diamond");
    }
}