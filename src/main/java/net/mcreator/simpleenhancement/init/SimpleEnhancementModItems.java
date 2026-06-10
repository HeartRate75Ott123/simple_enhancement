/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.simpleenhancement.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;

import net.minecraft.world.item.Item;

import net.mcreator.simpleenhancement.item.*;
import net.mcreator.simpleenhancement.SimpleEnhancementMod;

public class SimpleEnhancementModItems {
	public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(SimpleEnhancementMod.MODID);
	public static final DeferredItem<Item> NETHERITE_DIAMOND_ALLOY;
	public static final DeferredItem<Item> NETHERITE_DIAMOND_HELMET;
	public static final DeferredItem<Item> NETHERITE_DIAMOND_CHESTPLATE;
	public static final DeferredItem<Item> NETHERITE_DIAMOND_LEGGINGS;
	public static final DeferredItem<Item> NETHERITE_DIAMOND_BOOTS;
	public static final DeferredItem<Item> NETHERITE_DIAMOND_SWORD;
	public static final DeferredItem<Item> NETHERITE_DIAMOND_PICKAXE;
	public static final DeferredItem<Item> NETHERITE_DIAMOND_AXE;
	public static final DeferredItem<Item> NETHERITE_DIAMOND_SHOVEL;
	public static final DeferredItem<Item> NETHERITE_DIAMOND_HOE;
	public static final DeferredItem<Item> UPGRADE_ORB;
	public static final DeferredItem<Item> BASIC_SMITHING_TEMPLATE;
	public static final DeferredItem<Item> ADVANCED_SMITHING_TEMPLATE;
	public static final DeferredItem<Item> CONFUSION_CHARGE_ITEM;
	public static final DeferredItem<Item> ENHANCED_CONFUSION_CHARGE_ITEM;
	public static final DeferredItem<Item> REFLECTIVE_SHIELD;
	public static final DeferredItem<Item> MUTAGEN;
	static {
		NETHERITE_DIAMOND_ALLOY = REGISTRY.register("netherite_diamond_alloy", NetheriteDiamondAlloyItem::new);
		NETHERITE_DIAMOND_HELMET = REGISTRY.register("netherite_diamond_helmet", NetheriteDiamondItem.Helmet::new);
		NETHERITE_DIAMOND_CHESTPLATE = REGISTRY.register("netherite_diamond_chestplate", NetheriteDiamondItem.Chestplate::new);
		NETHERITE_DIAMOND_LEGGINGS = REGISTRY.register("netherite_diamond_leggings", NetheriteDiamondItem.Leggings::new);
		NETHERITE_DIAMOND_BOOTS = REGISTRY.register("netherite_diamond_boots", NetheriteDiamondItem.Boots::new);
		NETHERITE_DIAMOND_SWORD = REGISTRY.register("netherite_diamond_sword", NetheriteDiamondSwordItem::new);
		NETHERITE_DIAMOND_PICKAXE = REGISTRY.register("netherite_diamond_pickaxe", NetheriteDiamondPickaxeItem::new);
		NETHERITE_DIAMOND_AXE = REGISTRY.register("netherite_diamond_axe", NetheriteDiamondAxeItem::new);
		NETHERITE_DIAMOND_SHOVEL = REGISTRY.register("netherite_diamond_shovel", NetheriteDiamondShovelItem::new);
		NETHERITE_DIAMOND_HOE = REGISTRY.register("netherite_diamond_hoe", NetheriteDiamondHoeItem::new);
		UPGRADE_ORB = REGISTRY.register("upgrade_orb", UpgradeOrbItem::new);
		BASIC_SMITHING_TEMPLATE = REGISTRY.register("basic_smithing_template", BasicSmithingTemplateItem::new);
		ADVANCED_SMITHING_TEMPLATE = REGISTRY.register("advanced_smithing_template", AdvancedSmithingTemplateItem::new);
		CONFUSION_CHARGE_ITEM = REGISTRY.register("confusion_charge_item", ConfusionChargeItemItem::new);
		ENHANCED_CONFUSION_CHARGE_ITEM = REGISTRY.register("enhanced_confusion_charge_item", EnhancedConfusionChargeItemItem::new);
		REFLECTIVE_SHIELD = REGISTRY.register("reflective_shield", ReflectiveShieldItem::new);
		MUTAGEN = REGISTRY.register("mutagen", MutagenItem::new);
	}
	// Start of user code block custom items
	// End of user code block custom items
}