package net.mcreator.simpleenhancement;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.resources.ResourceLocation;

@EventBusSubscriber(modid = "simple_enhancement")
public class SwordAttackSpeedHandler {

    private static final ResourceLocation SPEED_MOD_ID = ResourceLocation.parse("simple_enhancement:sword_upgrade_speed");

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        ItemStack weapon = player.getMainHandItem();
        int level = getSwordLevel(weapon);

        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed == null) return;

        // 移除旧的修饰器（通过 ResourceLocation 识别）
        attackSpeed.removeModifier(SPEED_MOD_ID);

        if (level > 0 && weapon.getItem() instanceof SwordItem) {
            // 每级增加 20% 攻击速度（乘法）
            double speedBonus = 0.2 * level;
            AttributeModifier modifier = new AttributeModifier(
                    SPEED_MOD_ID,
                    speedBonus,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            );
            attackSpeed.addTransientModifier(modifier);
        }
    }

    private static int getSwordLevel(ItemStack sword) {
        CustomData data = sword.get(DataComponents.CUSTOM_DATA);
        if (data != null && data.contains("SwordLevel")) {
            return data.copyTag().getInt("SwordLevel");
        }
        return 0;
    }
}