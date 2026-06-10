package net.mcreator.simpleenhancement;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.mcreator.simpleenhancement.item.UpgradeOrbItem;
import net.mcreator.simpleenhancement.init.SimpleEnhancementModItems;

@EventBusSubscriber(modid = "simple_enhancement")
public class UpgradeHandler {

	@SubscribeEvent
	public static void onCrafting(PlayerEvent.ItemCraftedEvent event) {
		Player player = event.getEntity();
		ItemStack result = event.getCrafting();
		var inv = event.getInventory();
		Level level = player.level();
		if (level.isClientSide) return;

		// 基础宝珠合成：中心钻石，周围8金锭
		if (result.getItem() == SimpleEnhancementModItems.UPGRADE_ORB.get()) {
			boolean diamond = false;
			int gold = 0;
			for (int i = 0; i < inv.getContainerSize(); i++) {
				ItemStack stack = inv.getItem(i);
				if (i == 4) {
					if (stack.getItem() == Items.DIAMOND) diamond = true;
					else break;
				} else {
					if (stack.getItem() == Items.GOLD_INGOT) gold++;
					else if (!stack.isEmpty()) break;
				}
			}
			if (diamond && gold == 8) {
				UpgradeOrbItem.setOrbLevel(result, 1);
			}
		}

		// 宝珠升级已达上限提示（升级配方已由 OrbUpgradeRecipe 处理）
		if (result.getItem() == SimpleEnhancementModItems.UPGRADE_ORB.get()) {
			int resultLevel = UpgradeOrbItem.getOrbLevel(result);
			if (resultLevel >= 6) {
				boolean hasDiamondBlock = false;
				for (int i = 0; i < inv.getContainerSize(); i++) {
					if (inv.getItem(i).getItem() == Items.DIAMOND_BLOCK) {
						hasDiamondBlock = true;
						break;
					}
				}
				if (hasDiamondBlock) {
					player.sendSystemMessage(
						Component.translatable("message.simple_enhancement.orb_max_level"));
				}
			}
		}
	}
}
