package net.mcreator.simpleenhancement;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

@EventBusSubscriber(modid = "simple_enhancement")
public class UpgradeAttackHandler {

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        
        ItemStack weapon = player.getMainHandItem();
        if (!(weapon.getItem() instanceof SwordItem)) return;
        
        int level = getSwordLevel(weapon);
        if (level > 0) {
            // 每级增加 1.5 伤害
            float bonus = level * 1.5f;
            float originalDamage = event.getAmount();
            event.setAmount(originalDamage + bonus);
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