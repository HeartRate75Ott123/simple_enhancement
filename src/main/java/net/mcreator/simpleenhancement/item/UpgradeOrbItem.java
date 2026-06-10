package net.mcreator.simpleenhancement.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;

public class UpgradeOrbItem extends Item {
    public UpgradeOrbItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .component(DataComponents.CUSTOM_DATA, CustomData.of(createDefaultTag()))
        );
    }

    private static CompoundTag createDefaultTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("UpgradeLevel", 1);
        return tag;
    }

    @Override
    public Component getName(ItemStack stack) {
        int level = getOrbLevel(stack);
        return super.getName(stack).copy().append(Component.literal(" Lv." + level));
    }

    public static int getOrbLevel(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null && data.contains("UpgradeLevel")) {
            return data.copyTag().getInt("UpgradeLevel");
        }
        return 1;
    }

    public static void setOrbLevel(ItemStack stack, int level) {
        CustomData oldData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        var tag = oldData.copyTag();
        tag.putInt("UpgradeLevel", level);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}