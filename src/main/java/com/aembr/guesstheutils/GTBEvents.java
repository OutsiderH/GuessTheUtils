package com.aembr.guesstheutils;

import com.aembr.guesstheutils.utils.Message;
import com.aembr.guesstheutils.utils.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

public class GTBEvents {
    public interface BaseEvent {}
    /// Emitted as soon as the setup phase is complete and all player info has been collected.
    public record GameStartEvent(Set<InitialPlayerData> players) implements BaseEvent {}
    /// Emitted when the current builder changes. Can be null if the builder has left.
    public record BuilderChangeEvent(String previous, String current) implements BaseEvent {}
    /// Emitted when the user becomes builder.
    public record UserBuilderEvent() implements BaseEvent {}
    /// Emitted once the builder has picked a theme.
    public record RoundStartEvent(int currentRound, int totalRounds) implements BaseEvent {}
    /// Emitted when one or more players guess correctly. Contains all players that guessed during the same tick.
    public record CorrectGuessEvent(List<FormattedName> players) implements BaseEvent {}
    /// Emitted when the building and guessing part of the round has concluded.
    public record RoundEndEvent(boolean skipped) implements BaseEvent {}
    /// Emitted when the builder leaves before picking a theme. Always followed by `BuilderChangeEvent`.
    public record RoundSkipEvent() implements BaseEvent {}
    /// Emitted when the theme hint updates or the theme is revealed.
    public record ThemeUpdateEvent(String theme) implements BaseEvent {}
    /// Emitted when the game is over and all scores are displayed.
    public record GameEndEvent(Map<String, Integer> scores) implements BaseEvent {}
    /// Emitted when the game leaverState changes. See `GameState` for all possible states.
    public record StateChangeEvent(GameState previous, GameState current) implements BaseEvent {}
    /// Emitted when the true scores, as seen on the vanilla scoreboard, have updated.
    /// The first 3 elements are reserved for the top 3 players, and the 4th for the user (if not in top 3).
    public record TrueScoresUpdateEvent(List<TrueScore> scores) implements BaseEvent {}
    ///  Emitted when the user leaves a game that's in progress.
    public record UserLeaveEvent() implements BaseEvent {}
    /// Emitted when the user rejoins a game that's in progress.
    public record UserRejoinEvent() implements BaseEvent {}
    /// Emitted when a new tick update arrives.
    public record TickUpdateEvent() implements BaseEvent {}
    /// Emitted when a player sends any chat message.
    public record PlayerChatEvent(String player, String message) implements BaseEvent {}
    //public record PlayerChatEvent(PlayerChatMessage message) implements BaseEvent {}
    /// Emitted when the 1-second remaining in round alert is received.
    public record OneSecondAlertEvent() implements BaseEvent {}
    /// Emitted when the timer updates.
    public record TimerUpdateEvent(String timer) implements BaseEvent {}
    /// Emitted when the user guesses correctly.
    public record UserCorrectGuessEvent() implements BaseEvent {}

    private final Map<Consumer<?>, Module> modules = new HashMap<>();
    private final Map<Class<? extends BaseEvent>, List<Consumer<?>>> subscribers = new HashMap<>();

    public enum GameState { NONE, LOBBY, SETUP, ROUND_PRE, ROUND_BUILD, ROUND_END, POST_GAME }

    private final Utils.FixedSizeBuffer<List<Component>> scoreboardLineHistory = new Utils.FixedSizeBuffer<>(3);
    private final Utils.FixedSizeBuffer<List<Component>> playerListEntryHistory = new Utils.FixedSizeBuffer<>(3);

    private final String[] validEmblems = new String[]{"≈", "α", "Ω", "$", "π", "ƒ"};
    private final String[] validTitles = new String[]{"Rookie", "Untrained", "Amateur", "Prospect", "Apprentice",
            "Experienced", "Seasoned", "Trained", "Skilled", "Talented", "Professional", "Artisan", "Expert",
            "Master", "Legend", "Grandmaster", "Celestial", "Divine", "Ascended"};
    private final String[] validLeaderboardTitles = new String[]{"#10", "#9", "#8", "#7", "#6", "#5", "#4", "#3", "#2",
            "#1"};
    private final String[] validRanks = new String[]{"[VIP]", "[VIP+]", "[MVP]", "[MVP+]", "[MVP++]", "[YOUTUBE]"};
    private final String[] roundSkipMessages = new String[]{"The plot owner has left the game! Skipping...",
            "The plot owner is AFK! Skipping...", "The plot owner hasn't placed any blocks! Skipping..."};

    private boolean roundSkipped = false;

    /// We only consider a true score entry valid if it remains the same for at least 2 ticks.
    /// Random corruption that only lasts a tick is surprisingly common, as is slight de-sync between server and tracker.
    private final Utils.FixedSizeBuffer<List<TrueScore>> trueScoreHistory = new Utils.FixedSizeBuffer<>(2);
    private List<TrueScore> trueScores = null;

    private Tick currentTick;
    public GameState gameState = GameState.NONE;

    private List<Component> lobbyPlayerList = new ArrayList<>();
    private List<Component> setupPlayerList = new ArrayList<>();
    private String currentBuilder = "";
    private String currentTheme = "";
    private String currentTimer = "";

    /// Sometimes the "Builder: name" message happens before we emit GameStartEvent, so we store it to emit after instead
    private String prematureBuilder;

    public void processTickUpdate(Tick tick) {
        currentTick = tick;
        emit(new TickUpdateEvent());

        if (tick.scoreboardLines != null) scoreboardLineHistory.add(tick.scoreboardLines);
        if (tick.playerListEntries != null) playerListEntryHistory.add(tick.playerListEntries);

        if (tick.scoreboardLines != null) onScoreboardUpdate(tick.scoreboardLines);
        if (tick.playerListEntries != null) onPlayerListUpdate(tick.playerListEntries);
        if (tick.chatMessages != null) onChatMessages(tick.chatMessages);
        if (tick.actionBarMessage != null) onActionBarMessage(tick.actionBarMessage);
        if (tick.title != null) onTitleSet(tick.title);
        if (tick.subtitle != null) onSubtitleSet(tick.subtitle);
        if (tick.screenTitle != null) onScreenTitle(tick.screenTitle);
    }

    private void onPlayerListUpdate(List<Component> playerListEntries) {
        if (gameState.equals(GameState.LOBBY)) lobbyPlayerList = playerListEntries;
        if (gameState.equals(GameState.SETUP) && setupPlayerList.isEmpty()) {
            //if (playerListEntries.stream().noneMatch(entry -> entry.getSiblings().isEmpty())) return;
            if (playerListEntries.stream().filter(entry -> !entry.getSiblings().isEmpty()).count() != 1) return;
            setupPlayerList = playerListEntries;
        }
    }

    private void onScoreboardUpdate(List<Component> scoreboardLines) {
        List<String> stringLines = scoreboardLines.stream().map(line -> ChatFormatting.stripFormatting(line.getString())).toList();
        GameState state = getStateFromScoreboard(stringLines);

        if (gameState.equals(GameState.LOBBY) && state == null) {
            changeState(GameState.SETUP);
            return;
        }

        if (gameState.equals(GameState.SETUP) && (state == null || state.equals(GameState.NONE))) {
            return;
        }

        if (state != null) {
            changeState(state);
        }

        // Extract timer
        Optional<String> timerLine = stringLines.stream()
                .filter(line -> (line.startsWith("Starts In: ")
                        || line.startsWith("Time: ")
                        || line.startsWith("Next Round: ")))
                .reduce((first, second) -> second);
        if (timerLine.isPresent()) {
            String[] timerLineParts = timerLine.get().split(": ", 2);
            if (timerLineParts.length == 2
                    && timerLineParts[1].matches("\\d{2}:\\d{2}")
                    && !timerLineParts[1].equals(currentTimer)) {
                currentTimer = timerLineParts[1];
                emit(new TimerUpdateEvent(currentTimer));
            }
        }

        // Extract theme
        if (gameState.equals(GameState.ROUND_BUILD)) {
            String theme = getThemeFromScoreboard(stringLines);
            if (!theme.isEmpty() && !currentTheme.equals(theme)) {
                emit(new ThemeUpdateEvent(theme));
                currentTheme = theme;
            }
        }

        // Extract true scores
        if (gameState.equals(GameState.ROUND_BUILD) || gameState.equals(GameState.ROUND_PRE) || gameState.equals(GameState.ROUND_END)) {
            List<TrueScore> trueScoreEntries = getTrueScoresFromScoreboard(scoreboardLines);
            //System.out.println("Got true scores from scoreboard: " + trueScoreEntries);
            //System.out.println("True score history is " + trueScoreHistory.size());
            if (trueScoreHistory.size() == 2) {
                if (trueScoreEntries.equals(trueScoreHistory.get(0))
                        && trueScoreEntries.equals(trueScoreHistory.get(1))
                        && !Objects.equals(trueScoreEntries, trueScores)) {
                    trueScores = trueScoreEntries;

                    emit(new TrueScoresUpdateEvent(trueScores));
                }
            }
            trueScoreHistory.add(trueScoreEntries);
        }
    }

    @SuppressWarnings("SequencedCollectionMethodCanBeUsed")
    private void onGameStart(List<Component> lobbyList, List<Component> setupList, List<Component> finalList) {
//        System.out.println("lobby list: " + lobbyList);
//        System.out.println("setup list: " + setupList);
//        System.out.println("final list: " + finalList);

        List<InitialPlayerData> players = new ArrayList<InitialPlayerData>();
        for (Component playerEntry : finalList) {
            String name = playerEntry.getSiblings().isEmpty() ?
                    playerEntry.getString() : playerEntry.getSiblings().get(1).tryCollapseToString();

            Component title = null;
            Component emblem = null;
            if (!playerEntry.getSiblings().isEmpty()) {
                if (playerEntry.getSiblings().get(0).getSiblings().isEmpty()) {
                    // new
                    title = playerEntry.getSiblings().get(0);
                } else {
                    // old
                    title = playerEntry.getSiblings().get(0).getSiblings().get(0);
                }

                if (playerEntry.getSiblings().get(2).getSiblings().isEmpty()) {
                    // new
                    emblem = playerEntry.getSiblings().get(2);
                } else {
                    // old
                    emblem = playerEntry.getSiblings().get(2).getSiblings()
                        .get(playerEntry.getSiblings().get(2).getSiblings().size() - 1);
                }

                // trim spaces
                title = Component.literal(title.getString().trim()).setStyle(title.getStyle());
                emblem = Component.literal(emblem.getString().trim()).setStyle(emblem.getStyle());

                if (emblem.getString().equals("")) {
                    // empty emblem
                    emblem = Component.empty();
                }
            }

            players.add(new InitialPlayerData(name, title, emblem, null, false));
        }

        for (Component playerEntry : lobbyList) {
            String name = ChatFormatting.stripFormatting(playerEntry.getString());
            TextColor styleColor = playerEntry.getStyle().getColor();
            if (styleColor == null) {
                continue;
            }
            ChatFormatting rankColor = ChatFormatting.getByName(styleColor.serialize());


            InitialPlayerData player = players.stream().filter(p -> p.name.equals(name)).findAny().orElse(null);
            if (player == null) {
                players.add(new InitialPlayerData(name, null, null, rankColor, false));
            } else {
                player.rankColor = rankColor;
            }
        }

        Component setupEntry = setupList.stream().filter(
                entry -> !entry.getStyle().isEmpty()).findFirst().orElse(null);

        if (setupEntry != null) {
            String name = ChatFormatting.stripFormatting(setupEntry.getString());
            InitialPlayerData player = players.stream().filter(p -> p.name.equals(name)).findAny().orElse(null);
            if (player == null) {
                players.add(new InitialPlayerData(name, null, null, null, true));
            } else {
                player.isUser = true;
            }
        }

        currentBuilder = null;
        emit(new GameStartEvent(new HashSet<>(players)));

        lobbyPlayerList = new ArrayList<>();
        setupPlayerList = new ArrayList<>();

        if (prematureBuilder != null) {
            emit(new BuilderChangeEvent(null, prematureBuilder));
            currentBuilder = prematureBuilder;
            prematureBuilder = null;
        }
    }


    private void onGameEnd(List<Component> scoreboardLines) {
        Map<String, Integer> actualScores = new HashMap<>();
        scoreboardLines.stream().map(line -> ChatFormatting.stripFormatting(line.getString()))
                .filter(line -> line != null && !line.isBlank())
                .map(line -> line.split("\\. ", 2))
                .filter(parts -> parts.length > 1)
                .map(parts -> parts[1].split(": ", 2))
                .filter(parts -> parts.length > 1 && parts[0].length() < 16)
                .forEach(parts -> {
                    try { actualScores.put(parts[0], Integer.valueOf(parts[1])); }
                    catch (NumberFormatException ignored) {} //yep, score can be malformed and cause a crash
                });
        emit(new GameEndEvent(actualScores));
    }

    private void onScreenTitle(Component screenTitle) {
        if (!(gameState.equals(GameState.ROUND_PRE) || gameState.equals(GameState.ROUND_END))) return;
        if (screenTitle.getString().equals("Select a theme to build!")) {
            if (scoreboardLineHistory.size() == 0) return;
            String builderName = getBuilderNameFromScoreboard(scoreboardLineHistory.get(0));
            if (Objects.equals(currentBuilder, builderName)) return;
            emit(new BuilderChangeEvent(currentBuilder, builderName));
            emit(new UserBuilderEvent());
            currentBuilder = builderName;
        }
    }

    public String getPlayerNameFromMessage(String message) {
        if (!message.contains(": ")) return null;
        String[] nameParts = message.split(": ")[0].split(" ");
        if (message.startsWith("[GUESSER CHAT]")) {
            return nameParts[nameParts.length - 1];
        } else {
            // check if first part is a valid emblem
            if (Arrays.stream(validEmblems).anyMatch(e -> e.equals(nameParts[0]))) {
                return nameParts[nameParts.length - 1];
            }
            // if not, let's check if it's a valid title
            if (Arrays.stream(validTitles).anyMatch(e -> e.equals(nameParts[0]))) {
                return nameParts[nameParts.length - 1];
            }
            // if not, maybe there's a multi-word title (leaderboard)
            if (Arrays.stream(validLeaderboardTitles)
                    .anyMatch(e -> e.equals(nameParts[0]) && nameParts[1].equals("Builder"))) {
                return nameParts[nameParts.length - 1];
            }
            // if not, let's check if it's a valid rank (for when and titles don't appear at end of round)
            if (Arrays.stream(validRanks).anyMatch(e -> e.equals(nameParts[0]))) {
                return nameParts[nameParts.length - 1];
            }
        }
        return null;
    }

    private void onSubtitleSet(Component subtitle) {
        if (gameState.equals(GameState.ROUND_BUILD)
                && Objects.equals(ChatFormatting.stripFormatting(subtitle.getString()), "1 second remaining!")) {
            emit(new OneSecondAlertEvent());
        }
    }

    private void onTitleSet(Component title) {
    }

    private void onActionBarMessage(Component actionBarMessage) {
        String strMessage = ChatFormatting.stripFormatting(actionBarMessage.getString());
        if (strMessage == null || strMessage.isEmpty()) return;

        if (strMessage.startsWith("The theme is ") && gameState.equals(GameState.ROUND_BUILD)) {
            String theme = strMessage.replace("The theme is ", "");
            if (!currentTheme.equals(theme)) {
                emit(new ThemeUpdateEvent(theme));
                currentTheme = theme;
            }
        }
    }

    private void onChatMessages(List<Component> chatMessages) {
        List<FormattedName> correctGuessers = new ArrayList<>();
        for (Component message : chatMessages) {
            String strMessage = ChatFormatting.stripFormatting(message.getString());
            if (strMessage == null || strMessage.isEmpty()) continue;

            if (gameState.equals(GameState.ROUND_PRE) && strMessage.startsWith("Round: ") && strMessage.contains("/")) {
                String[] currentOverTotal = strMessage.replace("Round: ", "").split("/");
                if (!currentOverTotal[0].matches("\\d{1,2}") && !currentOverTotal[1].matches("\\d{1,2}")) {
                    return;
                }

                int current = Integer.parseInt(currentOverTotal[0]);
                int total = Integer.parseInt(currentOverTotal[1]);
                emit(new RoundStartEvent(current, total));
                changeState(GameState.ROUND_BUILD);
            }

            if (strMessage.startsWith("Builder: ")) {
                String builderName = strMessage.replace("Builder: ", "");
                if (Objects.equals(currentBuilder, builderName)) return;

                if (gameState.equals(GameState.ROUND_PRE) || gameState.equals(GameState.ROUND_END)) {
                    emit(new BuilderChangeEvent(currentBuilder, builderName));
                    currentBuilder = builderName;
                } else if (gameState.equals(GameState.SETUP)) {
                    prematureBuilder = builderName;
                }
            }

            if (gameState.equals(GameState.ROUND_BUILD) && !strMessage.contains(":")
                    && strMessage.endsWith(" correctly guessed the theme!")) {
                String name = strMessage.replace(" correctly guessed the theme!", "");
                ChatFormatting rank = ChatFormatting.getByName(Objects.requireNonNull(message.getSiblings().get(0).getStyle().getColor()).serialize());
                correctGuessers.add(new FormattedName(name, rank));
            }

            if (gameState.equals(GameState.ROUND_BUILD) && strMessage.startsWith("+") && strMessage.endsWith("(Correct Guess)")) {
                emit(new UserCorrectGuessEvent());
            }

            if (Arrays.asList(roundSkipMessages).contains(strMessage)) {
                roundSkipped = true;
            }

            if (gameState.equals(GameState.ROUND_BUILD) && strMessage.startsWith("The theme was: ")) {
                String theme = strMessage.replace("The theme was: ", "").replace("!", "");
                if (!currentTheme.equals(theme)) {
                    emit(new ThemeUpdateEvent(theme));
                    currentTheme = theme;
                }
            }

            if (strMessage.startsWith("Welcome back! You are building something relevant to the theme ")) {
                emit(new UserRejoinEvent());
                emit(new RoundEndEvent(false));
            }

            if (strMessage.startsWith("Welcome back! It is ") && strMessage.endsWith("'s turn to build. You have to guess the build!")) {
                String builder = strMessage.replace("Welcome back! It is ", "")
                        .replace("'s turn to build. You have to guess the build!", "");

                if (!currentBuilder.equals(builder)) {
                    emit(new BuilderChangeEvent(currentBuilder, builder));
                    currentBuilder = builder;
                }
                emit(new UserRejoinEvent());
            }

            if (gameState.equals(GameState.ROUND_PRE) && strMessage.equals("The owner left! Skipping...")) {
                emit(new RoundSkipEvent());
                emit(new BuilderChangeEvent(currentBuilder, null));
            }

            if (gameState.equals(GameState.ROUND_PRE) || gameState.equals(GameState.ROUND_BUILD)
                    || gameState.equals(GameState.ROUND_END)) {
                String playerName = getPlayerNameFromMessage(strMessage);
                if (playerName != null) {
                    String playerMessage = strMessage.split(": ", 2)[1];
                    emit(new PlayerChatEvent(playerName, playerMessage));
                }
            }
        }
        if (!correctGuessers.isEmpty()) emit(new CorrectGuessEvent(correctGuessers));
    }

    private String getBuilderNameFromScoreboard(List<Component> scoreboardLines) {
        List<String> strLines = scoreboardLines.stream().map(line -> ChatFormatting.stripFormatting(line.getString())).toList();
        int builderLine = strLines.indexOf("Builder:");
        if (builderLine == -1 || strLines.size() <= builderLine + 1) return "";

        String builderLineString = strLines.get(builderLine + 1);
        if (!builderLineString.startsWith(" ")) return "";

        return builderLineString.substring(1);
    }

    private String getThemeFromScoreboard(List<String> scoreboardLines) {
        int themeLine = scoreboardLines.indexOf("Theme:");
        if (themeLine == -1 || scoreboardLines.size() <= themeLine + 1) return "";

        String themeLineString = scoreboardLines.get(themeLine + 1);
        if (!themeLineString.startsWith(" ")) return "";
        if (themeLineString.isBlank()) return "";
        if (themeLineString.equals(" ???")) return "";
        return themeLineString.substring(1);
    }

    @SuppressWarnings("SequencedCollectionMethodCanBeUsed")
    public GameState getStateFromScoreboard(List<String> scoreboardLines) {
        if (scoreboardLines.isEmpty()) return null;
        if (!scoreboardLines.get(0).equals("GUESS THE BUILD")) return GameState.NONE;
        if (scoreboardLines.size() == 1) return GameState.NONE;
        if (scoreboardLines.contains("Mode: Guess The Build")) return GameState.LOBBY;
        if (scoreboardLines.stream().anyMatch(line -> line.startsWith("1. "))) return GameState.POST_GAME;
        if (scoreboardLines.stream().anyMatch(line -> line.startsWith("Starts In: 00:"))) return GameState.ROUND_PRE;
        if (scoreboardLines.stream().anyMatch(line -> line.startsWith("Next Round: 00:0"))) return GameState.ROUND_END;
        if (scoreboardLines.stream()
                // technically, it's possible for a player named "Time" to be on the scoreboard with 0 points
                .anyMatch(line -> line.startsWith("Time: 0") && line.length() > 7)) return GameState.ROUND_BUILD;
        return null;
    }

    public List<TrueScore> getTrueScoresFromScoreboard(List<Component> scoreboardLines) {
        List<TrueScore> trueScores = new ArrayList<>();
        List<String> stringLines = scoreboardLines.stream().map(line -> ChatFormatting.stripFormatting(line.getString())).toList();

        for (int i = 0; i < stringLines.size(); i++) {
            String[] parts = stringLines.get(i).split(": ");
            if (parts.length != 2) continue;
            if (!parts[0].contains(" ") && parts[1].matches("\\d{1,2}")) {
                Component line = scoreboardLines.get(i);
                TextColor color = null;
                ChatFormatting rank = null;
                if (line.getSiblings().get(0).getSiblings().isEmpty()) {
                    if (line.getSiblings().get(0).getStyle().getColor() != null) {
                        color = line.getSiblings().get(0).getStyle().getColor();
                        rank = ChatFormatting.getByName(color.serialize());
                    } else {
                        String content = line.getSiblings().get(0).getContents().toString();
                        if (content.length() >= 2 && content.charAt(0) == '§') {
                            char colorChar = content.charAt(1);
                            rank = ChatFormatting.getByCode(colorChar);
                        }
                    }
                } else {
                    color = line.getSiblings().get(0).getSiblings().get(0).getStyle().getColor();
                    rank = ChatFormatting.getByName(color.serialize());
                }
                trueScores.add(new TrueScore(new FormattedName(parts[0], rank), Integer.parseInt(parts[1])));
            }
        }

        return trueScores;
    }

    public void changeState(GameState newState) {
        if (gameState == newState) return;

        if (newState.equals(GameState.LOBBY)) {
            if (playerListEntryHistory.size() > 0) {
                lobbyPlayerList = playerListEntryHistory.get(0);
            }
        }


        if (newState.equals(GameState.ROUND_PRE)) {
            if (gameState.equals(GameState.SETUP) && !setupPlayerList.isEmpty()) {
                if (playerListEntryHistory.get(0).size() < 3) {
                    onGameStart(lobbyPlayerList, setupPlayerList, playerListEntryHistory.get(1));
                } else {
                    onGameStart(lobbyPlayerList, setupPlayerList, playerListEntryHistory.get(0));
                }
            }
            currentTheme = "";
        }

        if (newState.equals(GameState.POST_GAME)) {
            onGameEnd(scoreboardLineHistory.get(0));
        }

        if (gameState.equals(GameState.ROUND_BUILD) && newState.equals(GameState.ROUND_END)) {
            emit(new RoundEndEvent(roundSkipped));
            roundSkipped = false;
        }

        if (newState.equals(GameState.NONE)
                && (gameState.equals(GameState.ROUND_PRE)
                || gameState.equals(GameState.ROUND_BUILD)
                || gameState.equals(GameState.ROUND_END))) {
            emit(new UserLeaveEvent());
        }

        emit(new StateChangeEvent(gameState, newState));
        gameState = newState;
    }

    public boolean isInGtb() {
        return gameState.equals(GameState.ROUND_BUILD)
                || gameState.equals(GameState.ROUND_PRE)
                || gameState.equals(GameState.ROUND_END);
    }

    public <T extends BaseEvent> void subscribe(Class<T> eventClass, Consumer<T> consumer, Module module) {
        subscribers.computeIfAbsent(eventClass, k -> new ArrayList<>()).add(consumer);
        modules.put(consumer, module);
    }

    @SuppressWarnings("unchecked")
    public <T extends BaseEvent> void emit(T event) {
        List<Consumer<?>> eventSubscribers = subscribers.get(event.getClass());
        if (eventSubscribers != null) {
            for (Consumer<?> consumer : eventSubscribers) {
                try {
                    ((Consumer<T>) consumer).accept(event);
                } catch (Exception e) {
                    handleException(e, consumer);
                }
            }
        }
    }

    private void handleException(Exception e, Consumer<?> consumer) {
        System.out.println("Exception occurred in subscriber: " + consumer);
        GuessTheUtils.LOGGER.error("Exception occurred in subscriber: {}", consumer);

        Module module = modules.get(consumer);

        if (module != null) {
            String moduleName = module.getClass().getSimpleName();
            Module.ErrorAction action = module.getErrorAction();
            String stackTrace = Utils.getStackTraceAsString(e);

            if (GuessTheUtils.CLIENT != null && GuessTheUtils.CLIENT.player != null) {
                Message.displayMessage("Exception in module " + moduleName + ": " + e.getMessage() + ". Saving details to replay file...");

                Tick error = new Tick();
                error.error = stackTrace;
                GuessTheUtils.replay.addTick(error);
                GuessTheUtils.replay.save();
            } else {
                throw new RuntimeException();
            }

            switch (action) {
                case STOP:
                    GuessTheUtils.LOGGER.error("Stopping module: {}", moduleName);
                    unsubscribeModule(module);
                    Message.displayMessage("Stopped " + moduleName + ".");
                    break;
                case RESTART:
                    GuessTheUtils.LOGGER.error("Restarting module: {}", moduleName);
                    unsubscribeModule(module);
                    createNewModuleInstance(module.getClass());
                    Message.displayMessage("Restarted " + moduleName + ".");
                    break;
                case LOG_AND_CONTINUE:
                    GuessTheUtils.LOGGER.error("Logging error and continuing for module: {}", moduleName);
                    break;
            }
        }
    }

    private void unsubscribeModule(Module module) {
        List<Consumer<?>> toRemove = new ArrayList<>();
        for (Map.Entry<Consumer<?>, Module> entry : modules.entrySet()) {
            if (entry.getValue() == module) {
                toRemove.add(entry.getKey());
            }
        }
        for (Consumer<?> consumer : toRemove) {
            modules.remove(consumer);
            subscribers.values().forEach(list -> list.remove(consumer));
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    private Module createNewModuleInstance(Class<? extends Module> moduleClass) {
        try {
            return moduleClass.getDeclaredConstructor(GTBEvents.class).newInstance(this);
        } catch (Exception e) {
            e.printStackTrace();
            return null; // Handle this case appropriately
        }
    }

    public static class InitialPlayerData {
        public final String name;
        public final Component title;
        public final Component emblem;
        public ChatFormatting rankColor;
        public boolean isUser;

        public InitialPlayerData(String name, Component title, Component emblem, ChatFormatting rankColor, boolean isUser) {
            this.name = name;
            this.title = title;
            this.emblem = emblem;
            this.rankColor = rankColor;
            this.isUser = isUser;
        }

        @Override
        public String toString() {
            return "InitialPlayerData{" +
                    "name='" + name + '\'' +
                    ", title=" + title +
                    ", emblem=" + emblem +
                    ", rankColor=" + rankColor +
                    ", isUser=" + isUser +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof InitialPlayerData that)) return false;
            return isUser == that.isUser && Objects.equals(name, that.name) && Objects.equals(title, that.title) && Objects.equals(emblem, that.emblem) && rankColor == that.rankColor;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, title, emblem, rankColor, isUser);
        }
    }

    public record PlayerChatMessage(String name, Component title, Component emblem, Component rank, String message) {
        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            PlayerChatMessage that = (PlayerChatMessage) o;
            return Objects.equals(rank, that.rank) && Objects.equals(title, that.title) && Objects.equals(name, that.name) && Objects.equals(emblem, that.emblem) && Objects.equals(message, that.message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, title, emblem, rank, message);
        }

        @Override
        public @NotNull String toString() {
            return "PlayerChatMessage{" +
                    "name='" + name + '\'' +
                    ", title=" + title +
                    ", emblem=" + emblem +
                    ", rank=" + rank +
                    ", message='" + message + '\'' +
                    '}';
        }
    }

    public static class Module {
        public enum ErrorAction {
            STOP,
            RESTART,
            LOG_AND_CONTINUE
        }

        protected final GTBEvents events;

        public Module(GTBEvents events) {
            this.events = events;
        }

        // Default error action is DISABLE
        public ErrorAction getErrorAction() {
            return ErrorAction.STOP;
        }
    }

    public record TrueScore(FormattedName fName, int points) {
        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            TrueScore trueScore = (TrueScore) o;
            return points == trueScore.points && Objects.equals(fName, trueScore.fName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(fName, points);
        }

        @Override
        public @NotNull String toString() {
            return "TrueScore{" +
                    "fName=" + fName +
                    ", points=" + points +
                    '}';
        }
    }

    public record FormattedName(String name, ChatFormatting rankColor) {
        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            FormattedName that = (FormattedName) o;
            return Objects.equals(name, that.name) && rankColor == that.rankColor;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, rankColor);
        }

        @Override
        public @NotNull String toString() {
            return "FormattedName{" +
                    "name='" + name + '\'' +
                    ", rankColor=" + rankColor +
                    '}';
        }
    }
}
