package com.aembr.guesstheutils.mixin.name_autocomplete;

import com.aembr.guesstheutils.GuessTheUtils;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collection;

@Mixin(ClientSuggestionProvider.class)
public class ClientSuggestionProviderMixin {

    @ModifyReturnValue(method = "getCustomTabSugggestions", at = @At("RETURN"))
    private Collection<String> addNamesToSuggestions(Collection<String> original) {
        original.addAll(GuessTheUtils.nameAutocomplete.getNames());
        return original;
    }
}
