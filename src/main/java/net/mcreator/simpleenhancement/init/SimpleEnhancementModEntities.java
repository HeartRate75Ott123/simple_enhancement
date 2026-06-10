/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.simpleenhancement.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.registries.Registries;

import net.mcreator.simpleenhancement.entity.EnhancedConfusionChargeEntity;
import net.mcreator.simpleenhancement.entity.ConfusionChargeEntity;
import net.mcreator.simpleenhancement.SimpleEnhancementMod;

public class SimpleEnhancementModEntities {
	public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(Registries.ENTITY_TYPE, SimpleEnhancementMod.MODID);
	public static final DeferredHolder<EntityType<?>, EntityType<ConfusionChargeEntity>> CONFUSION_CHARGE = register("confusion_charge",
			EntityType.Builder.<ConfusionChargeEntity>of(ConfusionChargeEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.6f, 0.6f));
	public static final DeferredHolder<EntityType<?>, EntityType<EnhancedConfusionChargeEntity>> ENHANCED_CONFUSION_CHARGE = register("enhanced_confusion_charge",
			EntityType.Builder.<EnhancedConfusionChargeEntity>of(EnhancedConfusionChargeEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.6f, 0.6f));

	// Start of user code block custom entities
	// End of user code block custom entities
	private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> register(String registryname, EntityType.Builder<T> entityTypeBuilder) {
		return REGISTRY.register(registryname, () -> (EntityType<T>) entityTypeBuilder.build(registryname));
	}
}