package net.mcreator.simpleenhancement;

import net.mcreator.simpleenhancement.init.SimpleEnhancementModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.*;
import java.util.function.Supplier;

/**
 * 战利品配置中心
 * 按档位1-4分组，严格梯度，附魔等级不共存，盔甲/工具使用标签随机。
 */
public final class LootConfig {

    // ---------- 条目定义 ----------
    public record ItemEntry(Supplier<? extends Item> itemSupplier, int weight) {}
    public record EnchantEntry(String enchantPath, int level, int weight) {}
    public record OrbEntry(int level, int weight) {}
    public record TagEntry(TagKey<Item> tag, int weight) {}

    /**
     * 一个物品槽，包含普通物品、附魔、宝珠以及标签条目。
     */
    public static class Slot {
        public final List<ItemEntry> items = new ArrayList<>();
        public final List<EnchantEntry> enchants = new ArrayList<>();
        public final List<OrbEntry> orbs = new ArrayList<>();
        public final List<TagEntry> tags = new ArrayList<>();

        public void addItem(Supplier<? extends Item> item, int weight) {
            items.add(new ItemEntry(item, weight));
        }

        // 从等级1开始添加附魔（默认行为）
        public void addEnchant(String path, int... weights) {
            addEnchant(1, path, weights);
        }

        // 从指定起始等级开始添加附魔
        public void addEnchant(int startLevel, String path, int... weights) {
            for (int i = 0; i < weights.length; i++)
                enchants.add(new EnchantEntry(path, startLevel + i, weights[i]));
        }

        // 统一使用带起始等级的宝珠添加方法
        public void addOrb(int startLevel, int... weights) {
            for (int i = 0; i < weights.length; i++)
                orbs.add(new OrbEntry(startLevel + i, weights[i]));
        }

        public void addTag(TagKey<Item> tag, int weight) {
            tags.add(new TagEntry(tag, weight));
        }
    }

    // 物品标签
    public static final TagKey<Item> ARMOR_TAG = TagKey.create(Registries.ITEM,
            ResourceLocation.parse(SimpleEnhancementMod.MODID + ":armors"));
    public static final TagKey<Item> TOOL_TAG = TagKey.create(Registries.ITEM,
            ResourceLocation.parse(SimpleEnhancementMod.MODID + ":tools"));

    // 四个结构组各自的低档槽和高档槽
    public static final Slot LOW_TIER_LOW = new Slot();
    public static final Slot LOW_TIER_HIGH = new Slot();

    public static final Slot MID_TIER_LOW = new Slot();
    public static final Slot MID_TIER_HIGH = new Slot();

    public static final Slot MID_HIGH_TIER_LOW = new Slot();
    public static final Slot MID_HIGH_TIER_HIGH = new Slot();

    public static final Slot TOP_TIER_LOW = new Slot();
    public static final Slot TOP_TIER_HIGH = new Slot();

    // 特殊上浮附魔池（沙漠神殿、海底神殿）
    public static final List<EnchantEntry> DESERT_BONUS_ENCHANTS = new ArrayList<>();
    public static final List<EnchantEntry> OCEAN_BONUS_ENCHANTS = new ArrayList<>();

    private LootConfig() {}

    static {
        // ========== 低阶结构（地牢、沉船、小型水下遗迹）==========
        // 低档槽：档位1物品，附魔仅I级，宝珠1-2级
        LOW_TIER_LOW.addItem(SimpleEnhancementModItems.BASIC_SMITHING_TEMPLATE, 350);
        LOW_TIER_LOW.addEnchant("swift_strike", 80);
        LOW_TIER_LOW.addEnchant("guard_strike", 70);
        LOW_TIER_LOW.addEnchant("weaken_strike", 50);
        LOW_TIER_LOW.addOrb(1, 100, 60);

        // 高档槽：档位2物品，附魔仅I级，宝珠2-3级
        LOW_TIER_HIGH.addEnchant("all_in_strike", 30);
        LOW_TIER_HIGH.addEnchant("leap_teleport", 25);
        LOW_TIER_HIGH.addEnchant("reach_strike", 80);
        LOW_TIER_HIGH.addEnchant("heavy_strike", 70);
        LOW_TIER_HIGH.addItem(SimpleEnhancementModItems.CONFUSION_CHARGE_ITEM, 40);
        LOW_TIER_HIGH.addOrb(2, 45, 20);

        // ========== 中阶结构（沙漠神殿、丛林神庙、掠夺者前哨）==========
        // 低档槽：档位1-2物品，附魔仅I级，宝珠2-3级
        copySlot(LOW_TIER_LOW, MID_TIER_LOW);
        MID_TIER_LOW.addEnchant("all_in_strike", 25);
        MID_TIER_LOW.addEnchant("leap_teleport", 20);
        MID_TIER_LOW.addEnchant("reach_strike", 70);
        MID_TIER_LOW.addEnchant("heavy_strike", 60);
        MID_TIER_LOW.addOrb(2, 40, 15);

        // 高档槽：档位3物品，附魔I/II级，宝珠3级
        MID_TIER_HIGH.addItem(SimpleEnhancementModItems.CONFUSION_CHARGE_ITEM, 40);
        MID_TIER_HIGH.addItem(SimpleEnhancementModItems.ENHANCED_CONFUSION_CHARGE_ITEM, 25);
        MID_TIER_HIGH.addItem(SimpleEnhancementModItems.REFLECTIVE_SHIELD, 50);
        MID_TIER_HIGH.addOrb(3, 30);
        MID_TIER_HIGH.addEnchant("hunger_slash", 60, 20);
        MID_TIER_HIGH.addEnchant("wave_swift", 35);

        // ========== 中高阶结构（试炼密室、大型水下遗迹、海底神殿）==========
        // 低档槽：档位3物品为主，附魔II级，宝珠3-4级
        MID_HIGH_TIER_LOW.addItem(SimpleEnhancementModItems.CONFUSION_CHARGE_ITEM, 60);
        MID_HIGH_TIER_LOW.addItem(SimpleEnhancementModItems.ENHANCED_CONFUSION_CHARGE_ITEM, 70);
        MID_HIGH_TIER_LOW.addEnchant(2, "hunger_slash", 25, 15);
        MID_HIGH_TIER_LOW.addEnchant(2, "sacrificial_slash", 30, 20);
        MID_HIGH_TIER_LOW.addEnchant("wave_swift", 35);
        MID_HIGH_TIER_LOW.addOrb(3, 35, 20);

        // 高档槽：档位4物品，附魔II/III级，宝珠4-5级
        MID_HIGH_TIER_HIGH.addEnchant(2, "life_absorb", 30, 60);
        MID_HIGH_TIER_HIGH.addEnchant("delay_slash", 50);
        MID_HIGH_TIER_HIGH.addEnchant(3, "sacrificial_slash", 45);
        MID_HIGH_TIER_HIGH.addItem(SimpleEnhancementModItems.MUTAGEN, 50);
        MID_HIGH_TIER_HIGH.addItem(SimpleEnhancementModItems.ADVANCED_SMITHING_TEMPLATE, 60);
        MID_HIGH_TIER_HIGH.addItem(SimpleEnhancementModItems.NETHERITE_DIAMOND_ALLOY, 45);
        MID_HIGH_TIER_HIGH.addTag(ARMOR_TAG, 50);
        MID_HIGH_TIER_HIGH.addTag(TOOL_TAG, 40);
        MID_HIGH_TIER_HIGH.addEnchant("mob_friend", 40);
        MID_HIGH_TIER_HIGH.addOrb(4, 20, 12);

        // ========== 顶级结构（下界要塞、堡垒遗迹、林地府邸、末地城）==========
        // 低档槽：极少量过渡物品，不包含任何宝珠和低级附魔
        TOP_TIER_LOW.addItem(SimpleEnhancementModItems.ENHANCED_CONFUSION_CHARGE_ITEM, 10);

        // 高档槽：仅档位4顶级物品，附魔III级，宝珠5-6级
        TOP_TIER_HIGH.addEnchant(3, "life_absorb", 120);
        TOP_TIER_HIGH.addEnchant("delay_slash", 90);
        TOP_TIER_HIGH.addEnchant(3, "sacrificial_slash", 85);
        TOP_TIER_HIGH.addItem(SimpleEnhancementModItems.MUTAGEN, 110);
        TOP_TIER_HIGH.addItem(SimpleEnhancementModItems.ADVANCED_SMITHING_TEMPLATE, 100);
        TOP_TIER_HIGH.addItem(SimpleEnhancementModItems.NETHERITE_DIAMOND_ALLOY, 90);
        TOP_TIER_HIGH.addTag(ARMOR_TAG, 70);
        TOP_TIER_HIGH.addTag(TOOL_TAG, 60);
        TOP_TIER_HIGH.addEnchant("mob_friend", 75);
        TOP_TIER_HIGH.addOrb(5, 35, 20);

        // ========== 特殊上浮附魔池（沙漠神殿、海底神殿）==========
        DESERT_BONUS_ENCHANTS.add(new EnchantEntry("hunger_slash", 1, 240));
        DESERT_BONUS_ENCHANTS.add(new EnchantEntry("hunger_slash", 2, 80));
        OCEAN_BONUS_ENCHANTS.add(new EnchantEntry("hunger_slash", 1, 240));
        OCEAN_BONUS_ENCHANTS.add(new EnchantEntry("hunger_slash", 2, 80));
        OCEAN_BONUS_ENCHANTS.add(new EnchantEntry("hunger_slash", 3, 40));
        OCEAN_BONUS_ENCHANTS.add(new EnchantEntry("wave_swift", 1, 200));
    }

    private static void copySlot(Slot src, Slot dst) {
        dst.items.addAll(src.items);
        dst.enchants.addAll(src.enchants);
        dst.orbs.addAll(src.orbs);
        dst.tags.addAll(src.tags);
    }
}