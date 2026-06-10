package net.mcreator.simpleenhancement;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.mcreator.simpleenhancement.init.SimpleEnhancementModItems;
import net.mcreator.simpleenhancement.item.UpgradeOrbItem;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Supplier;

/**
 * 全局战利品修改器 - 平行槽位（无空槽）
 * - 每个箱子进行多次独立随机抽取，每次抽取必定从低档槽或高档槽中选择（各50%概率）。
 * - 普通模式抽取1~2次，替换模式（5%概率）抽取3~4次并清空原版物品。
 * - 特殊上浮（沙漠/海底）单独增加一次抽奖。
 * - 支持从物品标签中随机选取单件（盔甲/工具）。
 */
public class SimpleEnhancementModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> GLM =
            DeferredRegister.create(NeoForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS, SimpleEnhancementMod.MODID);

    @SuppressWarnings("unused")
    public static final Supplier<MapCodec<SimpleChestModifier>> SIMPLE_CHEST =
            GLM.register("simple_chest", () -> SimpleChestModifier.CODEC);

    public static void register(IEventBus modEventBus) {
        GLM.register(modEventBus);
    }

    public static class SimpleChestModifier extends LootModifier {
        public static final MapCodec<SimpleChestModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
                codecStart(instance).apply(instance, SimpleChestModifier::new)
        );

        protected SimpleChestModifier(LootItemCondition[] conditionsIn) {
            super(conditionsIn);
        }

        @Nonnull
        @Override
        protected ObjectArrayList<ItemStack> doApply(@Nonnull ObjectArrayList<ItemStack> generatedLoot, @Nonnull LootContext context) {
            boolean replaceMode = context.getRandom().nextFloat() < 0.05f;
            if (replaceMode) {
                generatedLoot.clear();
            }

            ResourceLocation lootTableId = context.getQueriedLootTableId();
            String path = lootTableId.getPath();

            LootConfig.Slot lowSlot;
            LootConfig.Slot highSlot;
            boolean applyDesertBonus = false;
            boolean applyOceanBonus = false;

            if (path.startsWith("chests/simple_dungeon") ||
                    path.startsWith("chests/shipwreck") ||
                    path.startsWith("chests/underwater_ruin_small")) {
                lowSlot = LootConfig.LOW_TIER_LOW;
                highSlot = LootConfig.LOW_TIER_HIGH;
            } else if (path.startsWith("chests/desert_pyramid") ||
                    path.startsWith("chests/jungle_temple") ||
                    path.startsWith("chests/pillager_outpost") ||
                    path.startsWith("chests/village")) {
                lowSlot = LootConfig.MID_TIER_LOW;
                highSlot = LootConfig.MID_TIER_HIGH;
                if (path.startsWith("chests/desert_pyramid")) applyDesertBonus = true;
            } else if (path.startsWith("chests/trial_chambers") ||
                    path.startsWith("chests/underwater_ruin_big")) {
                lowSlot = LootConfig.MID_HIGH_TIER_LOW;
                highSlot = LootConfig.MID_HIGH_TIER_HIGH;
                if (path.startsWith("chests/underwater_ruin_big")) applyOceanBonus = true;
            } else if (path.startsWith("chests/nether_bridge") ||
                    path.startsWith("chests/bastion_bridge") ||
                    path.startsWith("chests/bastion_hoglin_stable") ||
                    path.startsWith("chests/bastion_other") ||
                    path.startsWith("chests/bastion_treasure") ||
                    path.startsWith("chests/woodland_mansion") ||
                    path.startsWith("chests/end_city")) {
                lowSlot = LootConfig.TOP_TIER_LOW;
                highSlot = LootConfig.TOP_TIER_HIGH;
            } else {
                return generatedLoot;
            }

            int rolls;
            if (replaceMode) {
                rolls = 3 + context.getRandom().nextInt(2);
            } else {
                rolls = 1 + context.getRandom().nextInt(2);
            }

            for (int i = 0; i < rolls; i++) {
                boolean chooseHigh = context.getRandom().nextBoolean();
                ItemStack result = chooseHigh ? selectFromSlot(context, highSlot) : selectFromSlot(context, lowSlot);
                if (!result.isEmpty()) {
                    generatedLoot.add(result);
                }
            }

            if (applyDesertBonus && !LootConfig.DESERT_BONUS_ENCHANTS.isEmpty()) {
                if (context.getRandom().nextBoolean()) {
                    ItemStack bonus = selectEnchantFromList(context, LootConfig.DESERT_BONUS_ENCHANTS);
                    if (!bonus.isEmpty()) generatedLoot.add(bonus);
                }
            }
            if (applyOceanBonus && !LootConfig.OCEAN_BONUS_ENCHANTS.isEmpty()) {
                if (context.getRandom().nextBoolean()) {
                    ItemStack bonus = selectEnchantFromList(context, LootConfig.OCEAN_BONUS_ENCHANTS);
                    if (!bonus.isEmpty()) generatedLoot.add(bonus);
                }
            }

            return generatedLoot;
        }

        private ItemStack selectFromSlot(LootContext context, LootConfig.Slot slot) {
            List<WeightedEntry> entries = new ArrayList<>();
            for (LootConfig.ItemEntry e : slot.items) {
                entries.add(new WeightedEntry(() -> new ItemStack(e.itemSupplier().get()), e.weight()));
            }
            for (LootConfig.EnchantEntry e : slot.enchants) {
                entries.add(new WeightedEntry(() -> createEnchantBook(context, e.enchantPath(), e.level()), e.weight()));
            }
            for (LootConfig.OrbEntry e : slot.orbs) {
                entries.add(new WeightedEntry(() -> createOrb(e.level()), e.weight()));
            }
            for (LootConfig.TagEntry e : slot.tags) {
                entries.add(new WeightedEntry(() -> randomItemFromTag(context, e.tag()), e.weight()));
            }
            if (entries.isEmpty()) return ItemStack.EMPTY;

            int total = entries.stream().mapToInt(WeightedEntry::weight).sum();
            int roll = context.getRandom().nextInt(total);
            int cur = 0;
            for (WeightedEntry entry : entries) {
                cur += entry.weight();
                if (roll < cur) {
                    return entry.supplier().get();
                }
            }
            return ItemStack.EMPTY;
        }

        private ItemStack randomItemFromTag(LootContext context, TagKey<Item> tag) {
            var registry = BuiltInRegistries.ITEM;
            var items = registry.getTag(tag).orElse(null);
            if (items == null || items.size() == 0) return ItemStack.EMPTY;
            var list = items.stream().toList();
            var random = context.getRandom();
            var holder = list.get(random.nextInt(list.size()));
            return new ItemStack(holder.value());
        }

        private ItemStack selectEnchantFromList(LootContext context, List<LootConfig.EnchantEntry> list) {
            if (list.isEmpty()) return ItemStack.EMPTY;
            int total = list.stream().mapToInt(LootConfig.EnchantEntry::weight).sum();
            int roll = context.getRandom().nextInt(total);
            int cur = 0;
            for (LootConfig.EnchantEntry e : list) {
                cur += e.weight();
                if (roll < cur) {
                    return createEnchantBook(context, e.enchantPath(), e.level());
                }
            }
            return ItemStack.EMPTY;
        }

        private ItemStack createEnchantBook(LootContext context, String enchantPath, int level) {
            ResourceLocation id = ResourceLocation.parse(SimpleEnhancementMod.MODID + ":" + enchantPath);
            ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, id);
            var reg = context.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
            Holder<Enchantment> holder = reg.getHolder(key).orElse(null);
            if (holder == null) {
                SimpleEnhancementMod.LOGGER.warn("战利品附魔未找到: {}", id);
                return ItemStack.EMPTY;
            }
            return EnchantedBookItem.createForEnchantment(new EnchantmentInstance(holder, level));
        }

        private ItemStack createOrb(int level) {
            ItemStack orb = new ItemStack(SimpleEnhancementModItems.UPGRADE_ORB.get());
            UpgradeOrbItem.setOrbLevel(orb, level);
            return orb;
        }

        private record WeightedEntry(Supplier<ItemStack> supplier, int weight) {}

        @Nonnull
        @Override
        public MapCodec<? extends IGlobalLootModifier> codec() {
            return CODEC;
        }
    }
}