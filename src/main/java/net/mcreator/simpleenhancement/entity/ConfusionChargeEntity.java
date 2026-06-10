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

import net.mcreator.simpleenhancement.procedures.ConfusionChargeHitProcedure;
import net.mcreator.simpleenhancement.init.SimpleEnhancementModItems;

@OnlyIn(value = Dist.CLIENT, _interface = ItemSupplier.class)
public class ConfusionChargeEntity extends AbstractArrow implements ItemSupplier {

    private static final double GRAVITY = 0.03; // 雪球相似重力

    public ConfusionChargeEntity(EntityType<? extends ConfusionChargeEntity> type, Level world) {
        super(type, world);
        this.setNoGravity(true);
    }

    public ConfusionChargeEntity(EntityType<? extends ConfusionChargeEntity> type, double x, double y, double z, Level world, ItemStack firedFromWeapon) {
        super(type, x, y, z, world, new ItemStack(SimpleEnhancementModItems.CONFUSION_CHARGE_ITEM.get()), firedFromWeapon);
        this.setNoGravity(true);
    }

    public ConfusionChargeEntity(EntityType<? extends ConfusionChargeEntity> type, LivingEntity owner, Level world, ItemStack firedFromWeapon) {
        super(type, owner, world, new ItemStack(SimpleEnhancementModItems.CONFUSION_CHARGE_ITEM.get()), firedFromWeapon);
        this.setNoGravity(true);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (result.getEntity() instanceof LivingEntity target) {
            ConfusionChargeHitProcedure.execute(target);
        }
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        this.discard(); // 碰到方块立即消失，不插地
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(this.getDeltaMovement().add(0, -GRAVITY, 0));
        if (this.tickCount > 100) {
            this.discard(); // 超时消失
        }
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(SimpleEnhancementModItems.CONFUSION_CHARGE_ITEM.get());
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public ItemStack getItem() {
        return new ItemStack(SimpleEnhancementModItems.CONFUSION_CHARGE_ITEM.get());
    }
}