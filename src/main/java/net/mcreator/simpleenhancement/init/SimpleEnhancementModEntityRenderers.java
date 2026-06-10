/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.simpleenhancement.init;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;

@EventBusSubscriber(Dist.CLIENT)
public class SimpleEnhancementModEntityRenderers {
	@SubscribeEvent
	public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(SimpleEnhancementModEntities.CONFUSION_CHARGE.get(), ThrownItemRenderer::new);
		event.registerEntityRenderer(SimpleEnhancementModEntities.ENHANCED_CONFUSION_CHARGE.get(), ThrownItemRenderer::new);
	}
}