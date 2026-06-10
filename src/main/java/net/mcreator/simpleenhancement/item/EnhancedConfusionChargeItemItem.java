package net.mcreator.simpleenhancement.item;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;

import net.mcreator.simpleenhancement.procedures.EnhancedConfusionChargeShootProcedure;

public class EnhancedConfusionChargeItemItem extends Item {
    public EnhancedConfusionChargeItemItem() {
        super(new Item.Properties().stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (!world.isClientSide) {
            player.swing(hand); // 挥手动画
            EnhancedConfusionChargeShootProcedure.execute(player, itemstack);
        }
        return InteractionResultHolder.success(itemstack);
    }
}