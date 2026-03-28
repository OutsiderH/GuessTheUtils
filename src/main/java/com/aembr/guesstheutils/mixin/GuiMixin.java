package com.aembr.guesstheutils.mixin;

import com.aembr.guesstheutils.GuessTheUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.scores.Objective;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {
	@Inject(at = @At("HEAD"), method = "setTitle")
	private void onSetTitle(Component title, CallbackInfo ci) {
		GuessTheUtils.onTitleSet(title);
	}

	@Inject(at = @At("HEAD"), method = "setSubtitle")
	private void onSetSubtitle(Component subtitle, CallbackInfo ci) {
		GuessTheUtils.onSubtitleSet(subtitle);
	}
}
