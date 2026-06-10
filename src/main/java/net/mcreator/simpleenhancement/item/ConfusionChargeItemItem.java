package net.mcreator.simpleenhancement.item;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;

import net.mcreator.simpleenhancement.procedures.ConfusionChargeShootProcedure;

public class ConfusionChargeItemItem extends Item {
    public ConfusionChargeItemItem() {
        super(new Item.Properties().stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (!world.isClientSide) {
            // 播放挥手动画（客户端自动同步）
            player.swing(hand);
            // 执行发射逻辑
            ConfusionChargeShootProcedure.execute(player, itemstack);
        }
        return InteractionResultHolder.success(itemstack);
    }
}