package me.modmuss50.dg.neoforge.registry;

import me.modmuss50.dg.neoforge.GlobeDimensionMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.BlockItem;
import me.modmuss50.dg.neoforge.recipe.GlobeRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GlobeDimensionMod.MOD_ID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GLOBE = TABS.register("globe", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.globedimension.globes"))
            .icon(() -> ModItems.GLOBE.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                BuiltInRegistries.BLOCK.getTag(GlobeRecipe.BASE_BLOCKS).ifPresent(tag -> tag.forEach(holder -> {
                    if (holder.value().asItem() instanceof BlockItem) {
                        output.accept(ModItems.GLOBE.get().withBase(holder.value(), -1));
                    }
                }));
            })
            .build());

    private ModCreativeTabs() {}
}
