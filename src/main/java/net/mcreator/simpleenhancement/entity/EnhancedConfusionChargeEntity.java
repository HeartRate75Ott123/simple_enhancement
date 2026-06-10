package net.mcreator.simpleenhancement.entity;

import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.BlockHitResult;

import net.mcreator.simpleenhancement.procedures.EnhancedConfusionChargeHitProcedure;
import net.mcreator.simpleenhancement.init.SimpleEnhancementModItems;

@OnlyIn(value = Dist.CLIENT, _interface = ItemSupplier.class)
public class EnhancedConfusionChargeEntity extends AbstractArrow implements ItemSupplier {

    private static final double GRAVITY = 0.03; // 雪球相似的重力

    public EnhancedConfusionChargeEntity(EntityType<? extends EnhancedConfusionChargeEntity> type, Level world) {
        super(type, world);
        this.setNoGravity(true);
    }

    public EnhancedConfusionChargeEntity(EntityType<? extends EnhancedConfusionChargeEntity> type, double x, double y, double z, Level world, ItemStack firedFromWeapon) {
        super(type, x, y, z, world, new ItemStack(SimpleEnhancementModItems.ENHANCED_CONFUSION_CHARGE_ITEM.get()), firedFromWeapon);
        this.setNoGravity(true);
    }

    public EnhancedConfusionChargeEntity(EntityType<? extends EnhancedConfusionChargeEntity> type, LivingEntity owner, Level world, ItemStack firedFromWeapon) {
        super(type, owner, world, new ItemStack(SimpleEnhancementModItems.ENHANCED_CONFUSION_CHARGE_ITEM.get()), firedFromWeapon);
        this.setNoGravity(true);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (result.getEntity() instanceof LivingEntity target) {
            EnhancedConfusionChargeHitProcedure.execute(target);
        }
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        // 碰到方块立即消失，不插地
        this.discard();
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(this.getDeltaMovement().add(0, -GRAVITY, 0));
        // 超时消失（避免永远不消失）
        if (this.tickCount > 100) {
            this.discard();
        }
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(SimpleEnhancementModItems.ENHANCED_CONFUSION_CHARGE_ITEM.get());
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public ItemStack getItem() {
        return new ItemStack(SimpleEnhancementModItems.ENHANCED_CONFUSION_CHARGE_ITEM.get());
    }
}