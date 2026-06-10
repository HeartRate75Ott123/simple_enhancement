package net.mcreator.simpleenhancement;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

@EventBusSubscriber(modid = "simple_enhancement")
public class SwordTooltipHandler {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof SwordItem) {
            int level = getSwordLevel(stack);
            if (level > 0) {
                event.getToolTip().add(Component.translatable("tooltip.simple_enhancement.sword_level", level));
            }
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