/*
 * 滞斩 (Delay Slash) 附魔核心实现
 * 攻击命中后，延迟 3 秒对目标造成 20 点真实伤害。
 * 多次攻击会重置延迟，只触发最后一次攻击后的 3 秒伤害。
 */
package net.mcreator.simpleenhancement;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@EventBusSubscriber(modid = "simple_enhancement")
public class DelaySlashCore {

    private static final ResourceLocation ENCHANT_ID = ResourceLocation.fromNamespaceAndPath("simple_enhancement", "delay_slash");
    private static final ResourceKey<net.minecraft.world.item.enchantment.Enchantment> ENCHANT_KEY = ResourceKey.create(Registries.ENCHANTMENT, ENCHANT_ID);

    // 存储待触发延迟伤害的实体及其预计触发时间（游戏刻）
    private static final Map<LivingEntity, Long> DELAY_DAMAGE_MAP = new ConcurrentHashMap<>();

    private static int getEnchantLevel(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof SwordItem)) return 0;
        Registry<net.minecraft.world.item.enchantment.Enchantment> registry = player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        Holder<net.minecraft.world.item.enchantment.Enchantment> enchant = registry.getHolderOrThrow(ENCHANT_KEY);
        return stack.getEnchantmentLevel(enchant);
    }

    @SubscribeEvent
    public static void onPlayerAttack(LivingIncomingDamageEvent event) {
        // 只处理玩家造成的伤害
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) return;

        int level = getEnchantLevel(player);
        if (level <= 0) return;

        // 延迟 3 秒 = 60 刻 (20 刻/秒)
        long delayTicks = 60;
        long triggerTime = target.level().getGameTime() + delayTicks;

        // 更新或添加延迟伤害任务（重置计时）
        DELAY_DAMAGE_MAP.put(target, triggerTime);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (DELAY_DAMAGE_MAP.isEmpty()) return;

        long currentTime = event.getServer().overworld().getGameTime();
        // 遍历并移除过期任务
        DELAY_DAMAGE_MAP.entrySet().removeIf(entry -> {
            LivingEntity entity = entry.getKey();
            long triggerTime = entry.getValue();
            if (currentTime >= triggerTime) {
                // 确保实体仍存活且未被移除
                if (entity != null && entity.isAlive() && !entity.level().isClientSide()) {
                    // 造成 20 点固定伤害（魔法伤害，无视护甲）
                    DamageSource source = entity.level().damageSources().source(DamageTypes.MAGIC);
                    entity.hurt(source, 20.0F);
                }
                return true; // 移除该条目
            }
            return false;
        });
    }
}