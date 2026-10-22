package me.modmuss50.dg.neoforge.client.mixin;

import me.modmuss50.dg.neoforge.item.GlobeBlockItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep Hold My Items' item path for the globe, whose static block model is empty. */
@Pseudo
@Mixin(targets = "de.bene2212.holdmyitemsnf.util.RenderHelper", remap = false)
public abstract class HoldMyItemsRenderHelperMixin {
    @Inject(method = "shouldRenderCustom(Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void globedimension$useGlobeItemRenderer(ItemStack stack,
                                                           CallbackInfoReturnable<Boolean> callback) {
        if (stack.getItem() instanceof GlobeBlockItem) {
            callback.setReturnValue(false);
        }
    }
}
