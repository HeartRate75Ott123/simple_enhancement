/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.simpleenhancement.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;

import net.mcreator.simpleenhancement.SimpleEnhancementMod;

public class SimpleEnhancementModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SimpleEnhancementMod.MODID);
	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SIMPLE_ENHANCEMENT = REGISTRY.register("simple_enhancement",
			() -> CreativeModeTab.builder().title(Component.translatable("item_group.simple_enhancement.simple_enhancement")).icon(() -> new ItemStack(Blocks.COPPER_BLOCK)).displayItems((parameters, tabData) -> {
				tabData.accept(SimpleEnhancementModItems.NETHERITE_DIAMOND_ALLOY.get());
				tabData.accept(SimpleEnhancementModItems.NETHERITE_DIAMOND_HELMET.get());
				tabData.accept(SimpleEnhancementModItems.NETHERITE_DIAMOND_CHESTPLATE.get());
				tabData.accept(SimpleEnhancementModItems.NETHERITE_DIAMOND_LEGGINGS.get());
				tabData.accept(SimpleEnhancementModItems.NETHERITE_DIAMOND_BOOTS.get());
				tabData.accept(SimpleEnhancementModItems.NETHERITE_DIAMOND_SWORD.get());
				tabData.accept(SimpleEnhancementModItems.NETHERITE_DIAMOND_PICKAXE.get());
				tabData.accept(SimpleEnhancementModItems.NETHERITE_DIAMOND_AXE.get());
				tabData.accept(SimpleEnhancementModItems.NETHERITE_DIAMOND_SHOVEL.get());
				tabData.accept(SimpleEnhancementModItems.NETHERITE_DIAMOND_HOE.get());
				tabData.accept(SimpleEnhancementModItems.UPGRADE_ORB.get());
				tabData.accept(SimpleEnhancementModItems.BASIC_SMITHING_TEMPLATE.get());
				tabData.accept(SimpleEnhancementModItems.ADVANCED_SMITHING_TEMPLATE.get());
				tabData.accept(SimpleEnhancementModItems.CONFUSION_CHARGE_ITEM.get());
				tabData.accept(SimpleEnhancementModItems.ENHANCED_CONFUSION_CHARGE_ITEM.get());
				tabData.accept(SimpleEnhancementModItems.REFLECTIVE_SHIELD.get());
				tabData.accept(SimpleEnhancementModItems.MUTAGEN.get());
			}).build());
}