package net.mcreator.simpleenhancement.procedures;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import net.mcreator.simpleenhancement.init.SimpleEnhancementModEntities;
import net.mcreator.simpleenhancement.entity.EnhancedConfusionChargeEntity;

public class EnhancedConfusionChargeShootProcedure {
    public static void execute(Entity entity, ItemStack itemstack) {
        if (entity == null) return;
        Level level = entity.level();
        if (level.isClientSide()) return;

        itemstack.shrink(1);

        float volume = 0.5F + level.random.nextFloat() * 0.5F;
        float pitch = 0.8F + level.random.nextFloat() * 0.4F;
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, volume, pitch);

        Entity _shootFrom = entity;
        Level projectileLevel = _shootFrom.level();
        if (!projectileLevel.isClientSide()) {
            Projectile _entityToSpawn = initArrowProjectile(
                    new EnhancedConfusionChargeEntity(SimpleEnhancementModEntities.ENHANCED_CONFUSION_CHARGE.get(),
                            0, 0, 0, projectileLevel, createArrowWeaponItemStack(projectileLevel, 0, (byte) 0)),  // knockback 改为 0
                    entity, 2, true, false, false, AbstractArrow.Pickup.DISALLOWED);
            _entityToSpawn.setPos(_shootFrom.getX(), _shootFrom.getEyeY(), _shootFrom.getZ());
            _entityToSpawn.shoot(_shootFrom.getLookAngle().x, _shootFrom.getLookAngle().y, _shootFrom.getLookAngle().z, 1, 0);
            projectileLevel.addFreshEntity(_entityToSpawn);
        }
    }

    private static AbstractArrow initArrowProjectile(AbstractArrow entityToSpawn, Entity shooter, float damage, boolean silent, boolean fire, boolean particles, AbstractArrow.Pickup pickup) {
        entityToSpawn.setOwner(shooter);
        entityToSpawn.setBaseDamage(damage);
        if (silent) entityToSpawn.setSilent(true);
        if (fire) entityToSpawn.igniteForSeconds(100);
        if (particles) entityToSpawn.setCritArrow(true);
        entityToSpawn.pickup = pickup;
        return entityToSpawn;
    }

    private static ItemStack createArrowWeaponItemStack(Level level, int knockback, byte piercing) {
        ItemStack weapon = new ItemStack(Items.ARROW);
        if (knockback > 0)
            weapon.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.KNOCKBACK), knockback);
        if (piercing > 0)
            weapon.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PIERCING), piercing);
        return weapon;
    }
}