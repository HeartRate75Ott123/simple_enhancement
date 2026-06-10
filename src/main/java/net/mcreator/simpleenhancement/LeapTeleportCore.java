/*
 * The code of this mod element is always locked.
 */
package net.mcreator.simpleenhancement;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.food.FoodData;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

@EventBusSubscriber(modid = "simple_enhancement")
public class LeapTeleportCore {

    private static final ResourceLocation ENCHANT_ID = ResourceLocation.fromNamespaceAndPath("simple_enhancement", "leap_teleport");
    private static final ResourceKey<net.minecraft.world.item.enchantment.Enchantment> ENCHANT_KEY = ResourceKey.create(Registries.ENCHANTMENT, ENCHANT_ID);

    private static int getLeapTeleportLevel(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof SwordItem)) return 0;
        Registry<net.minecraft.world.item.enchantment.Enchantment> registry = player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        Holder<net.minecraft.world.item.enchantment.Enchantment> enchant = registry.getHolderOrThrow(ENCHANT_KEY);
        return stack.getEnchantmentLevel(enchant);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide()) return;

        int levelEnchant = getLeapTeleportLevel(player);
        if (levelEnchant <= 0) return;

        FoodData foodData = player.getFoodData();
        // 检查饥饿度是否足够 4 点
        if (foodData.getFoodLevel() < 4) {
            return; // 饥饿不足，无法瞬移
        }

        // 扣除 4 点饥饿度（2个鸡腿）
        foodData.setFoodLevel(foodData.getFoodLevel() - 4);

        // 瞬移距离：基础3格，每级+2格 => 3,5,7
        int distance = 3 + (levelEnchant - 1) * 2;

        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(distance));

        HitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 targetPos = hit.getLocation();
        if (hit.getType() != HitResult.Type.MISS) {
            Vec3 direction = look.normalize();
            targetPos = targetPos.subtract(direction.scale(0.5));
        }

        // 简单安全检测：目标位置不能是固体方块
        if (!level.getBlockState(new net.minecraft.core.BlockPos((int)targetPos.x, (int)targetPos.y, (int)targetPos.z)).isSolid()) {
            player.teleportTo(targetPos.x, targetPos.y, targetPos.z);
            player.playSound(net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
        }
    }
}