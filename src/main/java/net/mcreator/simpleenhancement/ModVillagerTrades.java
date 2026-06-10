package net.mcreator.simpleenhancement;

import net.mcreator.simpleenhancement.init.SimpleEnhancementModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 图书管理员交易 - 平行奖池模式（支持自定义第二价格物品）
 * 每个等级只添加一个交易条目，内部根据权重随机选择附魔书。
 * 专家级和大师级交易以「下界合金钻石合金锭」替代书本作为第二价格。
 */
public class ModVillagerTrades {

    // 带权重的附魔交易，支持第二物品（数量可随机范围）
    private static class WeightedEnchantTrade implements VillagerTrades.ItemListing {
        private final List<Entry> entries = new ArrayList<>();
        private int totalWeight = 0;

        private record Entry(
                String enchantPath, int level, int emeraldCost, int weight,
                Item secondItem, int secondCountMin, int secondCountMax
        ) {}

        /**
         * 添加交易选项（默认第二物品为书本，数量固定为1）
         */
        public void addEnchant(String enchantPath, int level, int emeraldCost, int weight) {
            addEnchant(enchantPath, level, emeraldCost, weight, Items.BOOK, 1, 1);
        }

        /**
         * 添加交易选项（自定义第二物品及其数量范围）
         */
        public void addEnchant(String enchantPath, int level, int emeraldCost, int weight,
                               Item secondItem, int secondCountMin, int secondCountMax) {
            entries.add(new Entry(enchantPath, level, emeraldCost, weight, secondItem, secondCountMin, secondCountMax));
            totalWeight += weight;
        }

        @Override
        public MerchantOffer getOffer(Entity trader, RandomSource random) {
            if (entries.isEmpty() || totalWeight <= 0) return null;
            int roll = random.nextInt(totalWeight);
            int current = 0;
            Entry selected = null;
            for (Entry e : entries) {
                current += e.weight();
                if (roll < current) {
                    selected = e;
                    break;
                }
            }
            if (selected == null) return null;

            // 获取附魔
            var registryAccess = trader.level().registryAccess();
            var enchantRegistry = registryAccess.registryOrThrow(Registries.ENCHANTMENT);
            ResourceLocation enchantId = ResourceLocation.parse(SimpleEnhancementMod.MODID + ":" + selected.enchantPath());
            ResourceKey<Enchantment> enchantKey = ResourceKey.create(Registries.ENCHANTMENT, enchantId);
            Holder<Enchantment> holder = enchantRegistry.getHolder(enchantKey).orElse(null);
            if (holder == null) {
                SimpleEnhancementMod.LOGGER.warn("平行奖池交易：附魔未找到 " + enchantId);
                return null;
            }
            ItemStack result = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(holder, selected.level()));

            // 第一价格：绿宝石
            ItemCost costEmerald = new ItemCost(Items.EMERALD, selected.emeraldCost());

            // 第二价格：自定义物品（随机数量）
            int secondCount = selected.secondCountMin();
            if (selected.secondCountMax() > selected.secondCountMin()) {
                secondCount = random.nextInt(selected.secondCountMax() - selected.secondCountMin() + 1) + selected.secondCountMin();
            }
            ItemCost costSecond = new ItemCost(selected.secondItem(), secondCount);

            return new MerchantOffer(costEmerald, Optional.of(costSecond), result, 12, 2, 0.05f);
        }
    }

    @SubscribeEvent
    public void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() != VillagerProfession.LIBRARIAN) return;

        // ==================== 学徒级（等级2）====================
        WeightedEnchantTrade trade2 = new WeightedEnchantTrade();
        trade2.addEnchant("reach_strike", 1, 8, 5);
        trade2.addEnchant("swift_strike", 1, 10, 4);
        trade2.addEnchant("heavy_strike", 1, 14, 3);
        trade2.addEnchant("guard_strike", 1, 16, 3);
        trade2.addEnchant("weaken_strike", 1, 18, 2);
        trade2.addEnchant("leap_teleport", 1, 20, 2);
        event.getTrades().get(2).add(trade2);

        // ==================== 老手级（等级3）====================
        WeightedEnchantTrade trade3 = new WeightedEnchantTrade();
        trade3.addEnchant("all_in_strike", 1, 28, 2);
        trade3.addEnchant("hunger_slash", 1, 22, 3);
        event.getTrades().get(3).add(trade3);

        // ==================== 专家级（等级4）====================
        WeightedEnchantTrade trade4 = new WeightedEnchantTrade();
        // 生命窃取 I（权重2）普通，只消耗绿宝石+书
        trade4.addEnchant("life_absorb", 1, 38, 2);
        // 生命窃取 II（权重1）罕见，消耗绿宝石 + 1~2 合金锭
        trade4.addEnchant("life_absorb", 2, 48, 1,
                SimpleEnhancementModItems.NETHERITE_DIAMOND_ALLOY.get(), 1, 2);
        event.getTrades().get(4).add(trade4);

        // ==================== 大师级（等级5）====================
        WeightedEnchantTrade trade5 = new WeightedEnchantTrade();
        // 滞斩（权重1）罕见，消耗绿宝石 + 3~4 合金锭
        trade5.addEnchant("delay_slash", 1, 32, 1,
                SimpleEnhancementModItems.NETHERITE_DIAMOND_ALLOY.get(), 3, 4);
        // 生命窃取 III（权重2）较常见，消耗绿宝石 + 3~4 合金锭
        trade5.addEnchant("life_absorb", 3, 64, 2,
                SimpleEnhancementModItems.NETHERITE_DIAMOND_ALLOY.get(), 3, 4);
        event.getTrades().get(5).add(trade5);
    }

    public static void register() {
        NeoForge.EVENT_BUS.register(new ModVillagerTrades());
    }
}