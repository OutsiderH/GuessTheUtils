package com.aembr.guesstheutils.mixin.item_hider;

import com.aembr.guesstheutils.GTBEvents;
import com.aembr.guesstheutils.GuessTheUtils;
import com.aembr.guesstheutils.config.GuessTheUtilsConfig;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.flag.FeatureFlagSet;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreativeModeTabs.class)
public abstract class CreativeModeTabsMixin {

    @Shadow
    @Nullable
    private static CreativeModeTab.@Nullable ItemDisplayParameters CACHED_PARAMETERS;

    @Shadow
    private static void buildAllTabContents(CreativeModeTab.ItemDisplayParameters displayContext) {
    }

    @Unique
    private static GTBEvents.GameState state;

    @Unique
    private static boolean moduleEnabled;

    @Inject(method = "tryRebuildTabContents", at = @At("HEAD"), cancellable = true)
    private static void trackLastVersion(FeatureFlagSet enabledFeatures, boolean operatorEnabled, HolderLookup.Provider lookup, CallbackInfoReturnable<Boolean> cir) {
        if ((GuessTheUtils.events != null && GuessTheUtils.events.gameState != state)
                || moduleEnabled != GuessTheUtilsConfig.CONFIG.instance().enableDisallowedItemHiderModule) {
            if (GuessTheUtils.events != null) {
                state = GuessTheUtils.events.gameState;
            }
            moduleEnabled = GuessTheUtilsConfig.CONFIG.instance().enableDisallowedItemHiderModule;
            CACHED_PARAMETERS = new CreativeModeTab.ItemDisplayParameters(enabledFeatures, operatorEnabled, lookup);
            buildAllTabContents(CACHED_PARAMETERS);
            cir.setReturnValue(true);
        }
    }
}
