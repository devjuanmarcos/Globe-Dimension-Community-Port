package me.modmuss50.dg.neoforge.registry;

import me.modmuss50.dg.neoforge.GlobeDimensionMod;
import me.modmuss50.dg.neoforge.recipe.GlobeRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, GlobeDimensionMod.MOD_ID);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<GlobeRecipe>> GLOBE =
            SERIALIZERS.register("globe", () -> new SimpleCraftingRecipeSerializer<>(GlobeRecipe::new));

    private ModRecipes() {}
}
