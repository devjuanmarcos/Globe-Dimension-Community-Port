package me.modmuss50.dg.neoforge.recipe;

import me.modmuss50.dg.neoforge.GlobeDimensionMod;
import me.modmuss50.dg.neoforge.item.GlobeBlockItem;
import me.modmuss50.dg.neoforge.registry.ModItems;
import me.modmuss50.dg.neoforge.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public final class GlobeRecipe extends CustomRecipe {
    public static final TagKey<Block> BASE_BLOCKS = TagKey.create(Registries.BLOCK,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(GlobeDimensionMod.MOD_ID, "base_blocks"));

    public GlobeRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return input.width() == 3 && input.height() == 3 && !assemble(input, level.registryAccess()).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        if (input.width() != 3 || input.height() != 3) return ItemStack.EMPTY;
        int[] glassSlots = {0, 1, 2, 3, 5};
        for (int slot : glassSlots) {
            if (!input.getItem(slot).is(Items.GLASS)) return ItemStack.EMPTY;
        }
        if (!input.getItem(4).isEmpty()) return ItemStack.EMPTY;

        Block base = null;
        for (int slot : new int[]{6, 7, 8}) {
            ItemStack ingredient = input.getItem(slot);
            if (!(ingredient.getItem() instanceof BlockItem blockItem)) return ItemStack.EMPTY;
            Block candidate = blockItem.getBlock();
            if (!candidate.defaultBlockState().is(BASE_BLOCKS)) return ItemStack.EMPTY;
            if (base != null && candidate.asItem() != ingredient.getItem()) return ItemStack.EMPTY;
            if (base != null && base != candidate) return ItemStack.EMPTY;
            base = candidate;
        }
        return base == null ? ItemStack.EMPTY : ModItems.GLOBE.get().withBase(base);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(ModItems.GLOBE.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.GLOBE.get();
    }
}
