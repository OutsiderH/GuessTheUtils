package com.aembr.guesstheutils;

import com.aembr.guesstheutils.utils.Message;
import com.aembr.guesstheutils.utils.Scheduler;
import com.aembr.guesstheutils.utils.TranslationData;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Util;

import java.util.Map;

public class Commands {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommands.literal("guesstheutils")
                .then(ClientCommands.literal("replay")
                        .then(ClientCommands.literal("save")
                            .executes((command) -> {
                                GuessTheUtils.replay.save();
                                return Command.SINGLE_SUCCESS;
                            }))

                        .then(ClientCommands.literal("open")
                            .executes((command) -> {
                                Util.getPlatform().openPath(Replay.replayDir);
                                return Command.SINGLE_SUCCESS;
                            })))

                .then(ClientCommands.literal("config")
                        .executes((command) -> {
                            GuessTheUtils.openConfig = true;
                            return Command.SINGLE_SUCCESS;
                        }))
        );

        dispatcher.register(ClientCommands.literal("gettranslation")
                .then(ClientCommands.argument("theme", StringArgumentType.string())
                        .suggests((ctx, builder) -> {
                            TranslationData.entries.stream()
                                    .map(TranslationData.TranslationDataEntry::theme)
                                    .map(theme -> theme.replace(" ", "_"))
                                    .filter(theme -> theme.toLowerCase().contains(builder.getRemainingLowerCase()))
                                    .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .then(ClientCommands.argument("language", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    String themeWithUnderscores = StringArgumentType.getString(ctx, "theme");
                                    String theme = themeWithUnderscores.replace("_", " ");

                                    TranslationData.entries.stream()
                                            .filter(entry -> entry.theme().equals(theme))
                                            .findFirst()
                                            .ifPresent(entry -> {entry.translations().entrySet().stream()
                                                        .filter(langEntry -> langEntry.getValue().isApproved())
                                                        .map(Map.Entry::getKey)
                                                        .filter(langCode -> langCode.toLowerCase().contains(builder.getRemainingLowerCase()))
                                                        .forEach(builder::suggest);
                                            });
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    String themeWithUnderscores = StringArgumentType.getString(ctx, "theme");
                                    String theme = themeWithUnderscores.replace("_", " ");
                                    String languageCode = StringArgumentType.getString(ctx, "language");
                                    printTranslation(languageCode, theme);
                                    return Command.SINGLE_SUCCESS;
                                }))));

        dispatcher.register(ClientCommands.literal("qgtb")
                .executes(command -> {
                    Message.sendMessage("/queue build_battle_guess_the_build");
                    return Command.SINGLE_SUCCESS;
                }));

        dispatcher.register(ClientCommands.literal("lrj")
                .executes(command -> {
                    Message.sendMessage("/hub");
                    Scheduler.schedule(30, () -> Message.sendMessage("/back"));
                    return Command.SINGLE_SUCCESS;
                }));
    }

    private static void printTranslation(String lang, String theme) {
        TranslationData.entries.stream()
                .filter(entry -> entry.theme().equals(theme))
                .findFirst()
                .ifPresent(entry -> {
                    TranslationData.Translation translation = entry.translations().get(lang);

                    if (translation != null && translation.isApproved()) {
                        //? if >=1.21.5 {
                        ClickEvent clickEvent = new ClickEvent.SuggestCommand("➤ " + translation.translation());
                        //?} else {
                        /*ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "➤ " + translation.translation());
                         *///?}

                        //? if >=1.21.5 {
                        HoverEvent hoverEvent = new HoverEvent.ShowText(Component.literal("Click to draft this translation"));
                        //?} else {
                        /*HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to draft this translation"));
                         *///?}

                        Component draftButtonText = Component.literal(" [Draft]").setStyle(Style.EMPTY
                                .withClickEvent(clickEvent)
                                .withHoverEvent(hoverEvent)
                                .withColor(ChatFormatting.YELLOW));

                        Message.displayMessage(Component.empty()
                                .append(Component.literal(theme).withStyle(ChatFormatting.GREEN))
                                .append(Component.literal(" in ").withStyle(ChatFormatting.GRAY))
                                .append(Component.literal(lang).withStyle(ChatFormatting.AQUA))
                                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                                .append(Component.literal(translation.translation()).withStyle(ChatFormatting.GOLD).withStyle(ChatFormatting.BOLD))
                                .append(draftButtonText));
                    } else {
                        Message.displayMessage(Component.empty()
                                .append(Component.literal("No approved translation found for ").withStyle(ChatFormatting.RED))
                                .append(Component.literal(theme).withStyle(ChatFormatting.GREEN))
                                .append(Component.literal(" in ").withStyle(ChatFormatting.RED))
                                .append(Component.literal(lang).withStyle(ChatFormatting.AQUA))
                                .append(Component.literal(".").withStyle(ChatFormatting.RED)));
                    }
                });
    }
}
