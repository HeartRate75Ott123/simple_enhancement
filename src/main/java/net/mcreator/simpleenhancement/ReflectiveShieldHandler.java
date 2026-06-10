/*
 * 弹反盾 - 右键获得抗性提升V，受击获得力量II，耐久恢复与消耗机制
 * 格挡成功时播放盾牌格挡音效
 */
package net.mcreator.simpleenhancement;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;

import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = "simple_enhancement")
public class ReflectiveShieldHandler {

    // 玩家状态存储（WeakHashMap 自动回收离线玩家）
    private static final Map<Player, Integer> counterMarkTicks = new WeakHashMap<>();   // 弹反预备剩余时间
    private static final Map<Player, Integer> cooldownTicks = new WeakHashMap<>();      // 右键冷却剩余时间
    private static final Map<Player, Long> lastRepairTime = new WeakHashMap<>();        // 上次自动修复时间
    private static final Map<Player, Boolean> hasCounteredThisBlock = new WeakHashMap<>(); // 本次格挡是否已触发过弹反

    private static final int COOLDOWN_MAX = 10;
    private static final int MARK_DURATION = 20;
    private static final int REPAIR_INTERVAL = 20;
    private static final int REPAIR_AMOUNT = 2;
    private static final int USAGE_COST = 4;
    private static final int MIN_DURABILITY_TO_USE = 11;

    private static final net.neoforged.neoforge.registries.DeferredItem<?> REFLECTIVE_SHIELD_DELEGATE =
            net.mcreator.simpleenhancement.init.SimpleEnhancementModItems.REFLECTIVE_SHIELD;

    private static Item getShieldItem() {
        return REFLECTIVE_SHIELD_DELEGATE.get();
    }

    private static boolean isHoldingShield(Player player) {
        Item shieldItem = getShieldItem();
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return main.getItem() == shieldItem || off.getItem() == shieldItem;
    }

    private static ItemStack getShieldStack(Player player) {
        Item shieldItem = getShieldItem();
        ItemStack main = player.getMainHandItem();
        if (main.getItem() == shieldItem) return main;
        ItemStack off = player.getOffhandItem();
        if (off.getItem() == shieldItem) return off;
        return ItemStack.EMPTY;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        // 冷却递减
        Integer cd = cooldownTicks.get(player);
        if (cd != null && cd > 0) {
            int newCd = cd - 1;
            if (newCd <= 0) cooldownTicks.remove(player);
            else cooldownTicks.put(player, newCd);
        }

        // 弹反预备标记递减，同时清理对应的已触发标志
        Integer mark = counterMarkTicks.get(player);
        if (mark != null && mark > 0) {
            int newMark = mark - 1;
            if (newMark <= 0) {
                counterMarkTicks.remove(player);
                hasCounteredThisBlock.remove(player);
            } else {
                counterMarkTicks.put(player, newMark);
            }
        } else if (mark == null) {
            hasCounteredThisBlock.remove(player);
        }

        // 耐久自动恢复
        if (isHoldingShield(player)) {
            long currentTime = player.level().getGameTime();
            Long last = lastRepairTime.get(player);
            if (last == null || currentTime - last >= REPAIR_INTERVAL) {
                ItemStack shield = getShieldStack(player);
                if (!shield.isEmpty()) {
                    int damage = shield.getDamageValue();
                    if (damage > 0) {
                        shield.setDamageValue(Math.max(0, damage - REPAIR_AMOUNT));
                    }
                    lastRepairTime.put(player, currentTime);
                }
            }
        } else {
            lastRepairTime.remove(player);
        }
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        ItemStack stack = event.getItemStack();
        if (stack.getItem() != getShieldItem()) return;

        // 冷却中则无法右键
        Integer cd = cooldownTicks.get(player);
        if (cd != null && cd > 0) return;

        int maxDamage = stack.getMaxDamage();
        int currentDamage = stack.getDamageValue();
        int durability = maxDamage - currentDamage;
        if (durability < MIN_DURABILITY_TO_USE || durability < USAGE_COST) return;

        // 消耗耐久，进入冷却，获得抗性，标记弹反预备
        stack.setDamageValue(currentDamage + USAGE_COST);
        cooldownTicks.put(player, COOLDOWN_MAX);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20, 4, false, false, true));
        counterMarkTicks.put(player, MARK_DURATION);
        hasCounteredThisBlock.remove(player); // 新的格挡周期重置触发记录
    }

    @SubscribeEvent
    public static void onPlayerHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        // 只有处于弹反预备状态才可能触发
        Integer mark = counterMarkTicks.get(player);
        if (mark == null || mark <= 0) return;

        // 同一格挡周期内只触发一次
        if (Boolean.TRUE.equals(hasCounteredThisBlock.get(player))) return;
        hasCounteredThisBlock.put(player, true);

        // 弹反成功：给予力量并播放音效
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 1, false, false, true));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}