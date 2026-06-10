package net.mcreator.simpleenhancement;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.mcreator.simpleenhancement.item.UpgradeOrbItem;
import net.mcreator.simpleenhancement.init.SimpleEnhancementModItems;

@EventBusSubscriber(modid = "simple_enhancement")
public class AnvilUpgradeHandler {

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        String itemName = event.getName();

        if (left.isEmpty() || right.isEmpty()) return;
        if (!(left.getItem() instanceof SwordItem)) return;
        if (right.getItem() != SimpleEnhancementModItems.UPGRADE_ORB.get()) return;

        int swordLevel = getSwordLevel(left);
        int orbLevel = UpgradeOrbItem.getOrbLevel(right);

        if (orbLevel != swordLevel + 1 || swordLevel >= 6) return;

        ItemStack upgraded = left.copy();
        setSwordLevel(upgraded, swordLevel + 1);

        if (itemName != null && !itemName.isEmpty()) {
            upgraded.set(DataComponents.CUSTOM_NAME, Component.literal(itemName));
        }

        event.setOutput(upgraded);
        event.setMaterialCost(1);
        event.setCost(5 + swordLevel);
    }

    private static int getSwordLevel(ItemStack sword) {
        CustomData data = sword.get(DataComponents.CUSTOM_DATA);
        if (data != null && data.contains("SwordLevel")) {
            return data.copyTag().getInt("SwordLevel");
        }
        return 0;
    }

    private static void setSwordLevel(ItemStack sword, int level) {
        CustomData oldData = sword.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        var tag = oldData.copyTag();
        tag.putInt("SwordLevel", level);
        sword.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}