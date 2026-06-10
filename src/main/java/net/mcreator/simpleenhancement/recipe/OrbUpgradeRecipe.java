package net.mcreator.simpleenhancement.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mcreator.simpleenhancement.SimpleEnhancementMod;
import net.mcreator.simpleenhancement.init.SimpleEnhancementModItems;
import net.mcreator.simpleenhancement.item.UpgradeOrbItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import java.util.ArrayList;

/**
 * 升级宝珠升级配方 —— 在 assemble() 时直接生成正确等级的宝珠。
 *
 * 解决了 PlayerEvent.ItemCraftedEvent 在 shift+点击 时无法正确设置
 * CustomData 的问题，同时让结果预览也显示正确的名称。
 */
public class OrbUpgradeRecipe implements CraftingRecipe {
    private final String group;
    private final CraftingBookCategory category;
    private final ItemStack templateResult;
    private final NonNullList<Ingredient> ingredients;
    private final boolean isSimple;

    public OrbUpgradeRecipe(String group, CraftingBookCategory category,
                            ItemStack templateResult, NonNullList<Ingredient> ingredients) {
        this.group = group;
        this.category = category;
        this.templateResult = templateResult;
        this.ingredients = ingredients;
        this.isSimple = ingredients.stream().allMatch(Ingredient::isSimple);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.ingredientCount() != this.ingredients.size()) {
            return false;
        }
        if (!isSimple) {
            var nonEmpty = new ArrayList<ItemStack>(input.ingredientCount());
            for (var item : input.items()) {
                if (!item.isEmpty()) nonEmpty.add(item);
            }
            return net.neoforged.neoforge.common.util.RecipeMatcher.findMatches(
                nonEmpty, this.ingredients) != null;
        } else {
            return input.size() == 1 && this.ingredients.size() == 1
                ? this.ingredients.getFirst().test(input.getItem(0))
                : input.stackedContents().canCraft(this, null);
        }
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        // 从输入中找到宝珠，读取等级并生成升级后的结果
        for (ItemStack stack : input.items()) {
            if (stack.getItem() == SimpleEnhancementModItems.UPGRADE_ORB.get()) {
                int oldLevel = UpgradeOrbItem.getOrbLevel(stack);
                if (oldLevel > 0 && oldLevel < 6) {
                    // 正常升级：等级 +1
                    ItemStack result = new ItemStack(SimpleEnhancementModItems.UPGRADE_ORB.get());
                    UpgradeOrbItem.setOrbLevel(result, oldLevel + 1);
                    return result;
                } else {
                    // 已达上限：返回与输入等级相同的宝珠，保留 6 级
                    ItemStack result = new ItemStack(SimpleEnhancementModItems.UPGRADE_ORB.get());
                    UpgradeOrbItem.setOrbLevel(result, oldLevel);
                    return result;
                }
            }
        }
        return this.templateResult.copy();
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        // 检查宝珠是否已达等级上限
        for (ItemStack stack : input.items()) {
            if (stack.getItem() == SimpleEnhancementModItems.UPGRADE_ORB.get()) {
                int level = UpgradeOrbItem.getOrbLevel(stack);
                if (level >= 6) {
                    // 已达上限：只返还1个钻石块（原版 remainder 会 grow 叠加，copy 整个堆叠会导致复制）
                    NonNullList<ItemStack> remainders = NonNullList.withSize(input.size(), ItemStack.EMPTY);
                    for (int i = 0; i < input.size(); i++) {
                        ItemStack item = input.getItem(i);
                        if (item.getItem() == Items.DIAMOND_BLOCK) {
                            remainders.set(i, item.copyWithCount(1));
                        }
                    }
                    return remainders;
                }
                break;
            }
        }
        // 正常升级：使用默认剩余物品逻辑
        NonNullList<ItemStack> remainders = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.getItem().hasCraftingRemainingItem()) {
                remainders.set(i, new ItemStack(stack.getItem().getCraftingRemainingItem()));
            }
        }
        return remainders;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.templateResult;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return this.ingredients;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public CraftingBookCategory category() {
        return this.category;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= this.ingredients.size();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }

    /**
     * 自定义配方序列化器 —— 兼容原版 shapeless 配方 JSON 格式
     */
    public static final class Serializer implements RecipeSerializer<OrbUpgradeRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        private static final String NAME = "orb_upgrade";

        public static void register(net.neoforged.bus.api.IEventBus modEventBus) {
            var deferred = net.neoforged.neoforge.registries.DeferredRegister.create(
                net.minecraft.core.registries.Registries.RECIPE_SERIALIZER,
                SimpleEnhancementMod.MODID);
            deferred.register(NAME, () -> INSTANCE);
            deferred.register(modEventBus);
        }

        private static final MapCodec<OrbUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(OrbUpgradeRecipe::getGroup),
                CraftingBookCategory.CODEC.fieldOf("category")
                    .orElse(CraftingBookCategory.MISC)
                    .forGetter(OrbUpgradeRecipe::category),
                ItemStack.STRICT_CODEC.fieldOf("result")
                    .forGetter(r -> r.templateResult),
                Ingredient.CODEC_NONEMPTY.listOf()
                    .fieldOf("ingredients")
                    .flatXmap(
                        list -> {
                            Ingredient[] arr = list.toArray(Ingredient[]::new);
                            if (arr.length == 0) {
                                return DataResult.error(() ->
                                    "No ingredients for orb upgrade recipe");
                            }
                            return DataResult.success(
                                NonNullList.of(Ingredient.EMPTY, arr));
                        },
                        DataResult::success
                    )
                    .forGetter(OrbUpgradeRecipe::getIngredients)
            ).apply(instance, OrbUpgradeRecipe::new)
        );

        private static final StreamCodec<RegistryFriendlyByteBuf, OrbUpgradeRecipe> STREAM_CODEC =
            StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);

        @Override
        public MapCodec<OrbUpgradeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, OrbUpgradeRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static OrbUpgradeRecipe fromNetwork(RegistryFriendlyByteBuf buf) {
            String group = buf.readUtf();
            CraftingBookCategory cat = buf.readEnum(CraftingBookCategory.class);
            int count = buf.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(count, Ingredient.EMPTY);
            ingredients.replaceAll(i -> Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
            ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
            return new OrbUpgradeRecipe(group, cat, result, ingredients);
        }

        private static void toNetwork(RegistryFriendlyByteBuf buf, OrbUpgradeRecipe recipe) {
            buf.writeUtf(recipe.group);
            buf.writeEnum(recipe.category);
            buf.writeVarInt(recipe.ingredients.size());
            for (Ingredient ingredient : recipe.ingredients) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
            }
            ItemStack.STREAM_CODEC.encode(buf, recipe.templateResult);
        }
    }
}
