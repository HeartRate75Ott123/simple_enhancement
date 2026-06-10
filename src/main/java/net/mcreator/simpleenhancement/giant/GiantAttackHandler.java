package net.mcreator.simpleenhancement.giant;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;

/**
 * 巨人攻击处理器
 * 左键空挥触发范围伤害，包含方向检测、墙体检测、伤害衰减
 * 使用客户端按键监听 + 网络包，无冷却
 */
public class GiantAttackHandler {

    private static final double ATTACK_RANGE = 15.0;
    private static final double DAMAGE_FALLOFF = 0.1;
    private static final double DIRECTION_THRESHOLD = 0.2;
    private static final double WALL_CHECK_TOLERANCE = 0.8;

    public record AttackPacket() implements CustomPacketPayload {
        public static final ResourceLocation ID = ResourceLocation.parse("simpleenhancement:giant_attack");
        public static final Type<AttackPacket> TYPE = new Type<>(ID);
        public static final StreamCodec<FriendlyByteBuf, AttackPacket> STREAM_CODEC = StreamCodec.unit(new AttackPacket());
        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void registerPackets(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(AttackPacket.TYPE, AttackPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    Player player = context.player();
                    if (player == null) return;
                    if (!isGiant(player)) return;
                    triggerAreaDamage(player);
                }));
    }

    @EventBusSubscriber(modid = "simpleenhancement", value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onLeftClick(InputEvent.InteractionKeyMappingTriggered event) {
            if (!event.isAttack()) return;
            Player player = Minecraft.getInstance().player;
            if (player == null) return;

            Minecraft mc = Minecraft.getInstance();
            // 挖掘时不应触发巨人攻击
            if (mc.gameMode != null && mc.gameMode.isDestroying()) return;
            if (player.isUsingItem()) return;
            if (!isGiant(player)) return;

            PacketDistributor.sendToServer(new AttackPacket());
        }
    }

    private static boolean isGiant(Player player) {
        AttributeInstance scaleAttr = player.getAttribute(Attributes.SCALE);
        if (scaleAttr != null && scaleAttr.getBaseValue() > 1.0) return true;
        AttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null && healthAttr.getBaseValue() > 20.0) return true;
        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        return speedAttr != null && speedAttr.getBaseValue() > 0.15;
    }

    private static void triggerAreaDamage(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        // 爆炸粒子
        Vec3 look = player.getLookAngle();
        Vec3 origin = player.getEyePosition().add(look.scale(2.0));
        serverLevel.sendParticles(ParticleTypes.EXPLOSION, origin.x, origin.y, origin.z, 1, 0, 0, 0, 0);

        // 范围内实体
        AABB aabb = player.getBoundingBox().inflate(ATTACK_RANGE);
        List<LivingEntity> entities = serverLevel.getEntitiesOfClass(LivingEntity.class, aabb,
                e -> e != player && e.isAlive() && e.distanceToSqr(player) <= ATTACK_RANGE * ATTACK_RANGE);

        double baseDamage = player.getAttribute(Attributes.ATTACK_DAMAGE).getValue();
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookDir = player.getLookAngle();

        for (LivingEntity target : entities) {
            // 方向检查
            Vec3 toTarget = target.getEyePosition().subtract(eyePos).normalize();
            if (lookDir.dot(toTarget) < DIRECTION_THRESHOLD) continue;

            // 墙体检查
            Vec3 targetCenter = target.getBoundingBox().getCenter();
            HitResult hit = serverLevel.clip(new ClipContext(eyePos, targetCenter,
                    Block.COLLIDER, Fluid.NONE, player));
            if (hit.getType() != HitResult.Type.MISS) {
                double distToHit = hit.getLocation().distanceTo(targetCenter);
                if (distToHit > WALL_CHECK_TOLERANCE) continue;
            }

            double distance = Math.sqrt(target.distanceToSqr(player));
            double damage = baseDamage * (1 - DAMAGE_FALLOFF * (distance / ATTACK_RANGE));
            if (damage > 0.1) {
                target.hurt(player.damageSources().playerAttack(player), (float) damage);
            }
        }
    }
}