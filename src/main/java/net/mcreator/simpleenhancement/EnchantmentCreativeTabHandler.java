package net.mcreator.simpleenhancement;

import net.mcreator.simpleenhancement.init.SimpleEnhancementModItems;
import net.mcreator.simpleenhancement.init.SimpleEnhancementModTabs;
import net.mcreator.simpleenhancement.item.UpgradeOrbItem;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 创造模式选项卡处理器
 * - 将所有13个模组附魔的附魔书添加到 simple_enhancement 创造模式选项卡
 * - 将升级宝珠 LV2-LV6 添加到模组选项卡（LV1 已由 MCreator 生成代码添加）
 * - 从原版选项卡（如材料选项卡）中移除本模组的附魔书
 */
@EventBusSubscriber(modid = SimpleEnhancementMod.MODID)
public class EnchantmentCreativeTabHandler {

    private static final List<EnchantmentInfo> ENCHANTMENTS = List.of(
        new EnchantmentInfo("reach_strike", 3),
        new EnchantmentInfo("swift_strike", 3),
        new EnchantmentInfo("heavy_strike", 3),
        new EnchantmentInfo("guard_strike", 3),
        new EnchantmentInfo("weaken_strike", 3),
        new EnchantmentInfo("all_in_strike", 1),
        new EnchantmentInfo("leap_teleport", 3),
        new EnchantmentInfo("delay_slash", 1),
        new EnchantmentInfo("sacrificial_slash", 3),
        new EnchantmentInfo("hunger_slash", 3),
        new EnchantmentInfo("wave_swift", 1),
        new EnchantmentInfo("life_absorb", 3),
        new EnchantmentInfo("mob_friend", 1)
    );

    @SubscribeEvent
    public static void onBuildCreativeModeTabContents(BuildCreativeModeTabContentsEvent event) {
        ResourceKey<?> tabKey = event.getTabKey();
        String namespace = tabKey.location().getNamespace();

        if (tabKey.equals(SimpleEnhancementModTabs.SIMPLE_ENHANCEMENT.getKey())) {
            // 在模组选项卡中：添加所有附魔书
            addEnchantmentBooks(event);
            // 在模组选项卡中：添加升级宝珠 LV2-LV6（LV1 已由显示物品生成器添加）
            addUpgradeOrbs(event);
        } else if (namespace.equals("minecraft")) {
            // 在原版选项卡中（如材料选项卡）：移除本模组的附魔书
            removeModEnchantmentBooks(event);
        }
    }

    /**
     * 将升级宝珠 LV2-LV6 逐个插入到 LV1 后面，保持连续排列
     */
    private static void addUpgradeOrbs(BuildCreativeModeTabContentsEvent event) {
        // 找到 MCreator 已添加的默认 LV1 宝珠
        ItemStack level1Template = new ItemStack(SimpleEnhancementModItems.UPGRADE_ORB.get());
        ItemStack previousOrb = null;
        for (ItemStack stack : event.getParentEntries()) {
            if (ItemStack.isSameItemSameComponents(stack, level1Template)) {
                previousOrb = stack;
                break;
            }
        }

        if (previousOrb == null) {
            SimpleEnhancementMod.LOGGER.warn("LV1 升级宝珠未在选项卡中找到，无法插入后续等级");
            return;
        }

        // 将 LV2-LV6 依次插入到前一级后面
        for (int level = 2; level <= 6; level++) {
            ItemStack orb = new ItemStack(SimpleEnhancementModItems.UPGRADE_ORB.get());
            UpgradeOrbItem.setOrbLevel(orb, level);
            event.insertAfter(previousOrb, orb, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
            previousOrb = orb;
        }
    }

    /**
     * 将 13 个模组附魔的附魔书添加到选项卡中
     */
    private static void addEnchantmentBooks(BuildCreativeModeTabContentsEvent event) {
        var enchantmentLookup = event.getParameters().holders()
            .lookup(Registries.ENCHANTMENT)
            .orElse(null);
        if (enchantmentLookup == null) return;

        for (EnchantmentInfo info : ENCHANTMENTS) {
            ResourceLocation id = ResourceLocation.parse(
                SimpleEnhancementMod.MODID + ":" + info.id());
            ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, id);
            Holder<Enchantment> holder = enchantmentLookup.get(key).orElse(null);
            if (holder != null) {
                for (int level = 1; level <= info.maxLevel(); level++) {
                    event.accept(
                        EnchantedBookItem.createForEnchantment(
                            new EnchantmentInstance(holder, level)));
                }
            } else {
                SimpleEnhancementMod.LOGGER.warn(
                    "附魔 {} 未在注册表中找到，无法添加到创造模式选项卡", id);
            }
        }
    }

    /**
     * 从原版选项卡的父选项卡条目中移除所有本模组的附魔书
     * （搜索选项卡不受影响，因为其使用 SEARCH_TAB_ONLY 可见性）
     */
    private static void removeModEnchantmentBooks(BuildCreativeModeTabContentsEvent event) {
        List<ItemStack> toRemove = new ArrayList<>();
        for (ItemStack stack : event.getParentEntries()) {
            if (isModEnchantmentBook(stack)) {
                toRemove.add(stack);
            }
        }
        for (ItemStack stack : toRemove) {
            event.remove(stack, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
        }
    }

    /**
     * 检查物品栈是否为本模组的附魔书
     */
    private static boolean isModEnchantmentBook(ItemStack stack) {
        if (stack.getItem() != Items.ENCHANTED_BOOK) return false;

        ItemEnchantments enchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (enchantments == null || enchantments.isEmpty()) return false;

        for (var entry : enchantments.entrySet()) {
            Holder<Enchantment> holder = entry.getKey();
            if (holder.unwrapKey()
                    .map(key -> key.location().getNamespace().equals(SimpleEnhancementMod.MODID))
                    .orElse(false)) {
                return true;
            }
        }
        return false;
    }

    private record EnchantmentInfo(String id, int maxLevel) {}
}
