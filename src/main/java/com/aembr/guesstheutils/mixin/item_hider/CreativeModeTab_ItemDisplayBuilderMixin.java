package com.aembr.guesstheutils.mixin.item_hider;

import com.aembr.guesstheutils.modules.DisallowedItemHider;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.item.Item;
import net.minecraft.world.flag.FeatureFlagSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.item.CreativeModeTab$ItemDisplayBuilder")
public abstract class CreativeModeTab_ItemDisplayBuilderMixin {

    @WrapOperation(method = "accept", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;isEnabled(Lnet/minecraft/world/flag/FeatureFlagSet;)Z"))
    private boolean removeDisallowedItems(Item instance, FeatureFlagSet featureSet, Operation<Boolean> original) {
        boolean originalVal = original.call(instance, featureSet);
        return DisallowedItemHider.isAllowed(instance) && originalVal;
    }
}
