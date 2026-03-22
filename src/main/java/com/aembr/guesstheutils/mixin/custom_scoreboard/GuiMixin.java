package com.aembr.guesstheutils.mixin.custom_scoreboard;

import com.aembr.guesstheutils.modules.CustomScoreboard;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {
    @Inject(at = @At("HEAD"), method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V", cancellable = true)
    private void onRenderScoreboardSidebar(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (CustomScoreboard.isRendering()) ci.cancel();
    }
}
