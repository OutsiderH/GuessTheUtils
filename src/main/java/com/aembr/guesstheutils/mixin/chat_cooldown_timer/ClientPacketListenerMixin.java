package com.aembr.guesstheutils.mixin.chat_cooldown_timer;

import com.aembr.guesstheutils.GuessTheUtils;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(method = "sendChat", at = @At("HEAD"))
    private void onSendMessage(String content, CallbackInfo ci) {
        GuessTheUtils.chatCooldown.onMessageSent();
    }
}
