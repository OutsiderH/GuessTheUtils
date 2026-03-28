package com.aembr.guesstheutils.utils;

import com.aembr.guesstheutils.GuessTheUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public class Message {
    public static void sendMessage(String message) {
        if (GuessTheUtils.CLIENT.player != null) {
            if (message.startsWith("/")) {
                GuessTheUtils.CLIENT.player.connection.sendCommand(message.substring(1));
            } else {
                GuessTheUtils.CLIENT.gui.getChat().addRecentChat(message);
                GuessTheUtils.CLIENT.player.connection.sendChat(message);
            }
        }
    }

    public static void displayMessage(String message) {
        if (GuessTheUtils.CLIENT == null || GuessTheUtils.CLIENT.player == null) return;
        GuessTheUtils.CLIENT.player.sendSystemMessage(GuessTheUtils.prefix.copy()
                .append(Component.literal(message).withStyle(ChatFormatting.GRAY)));
    }

    public static void displayMessage(Component message) {
        if (GuessTheUtils.CLIENT == null || GuessTheUtils.CLIENT.player == null) return;
        GuessTheUtils.CLIENT.player.sendSystemMessage(GuessTheUtils.prefix.copy().append(message));
    }
}
