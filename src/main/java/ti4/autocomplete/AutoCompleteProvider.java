package ti4.autocomplete;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import ti4.AsyncTI4DiscordBot;
import ti4.MessageListener;
import ti4.generator.Mapper;
import ti4.generator.TileHelper;
import ti4.helpers.Constants;
import ti4.helpers.Storage;
import ti4.helpers.FoWHelper;
import ti4.helpers.GlobalSettings;
import ti4.helpers.Helper;
import ti4.map.Game;
import ti4.map.GameManager;
import ti4.map.Player;
import ti4.message.BotLogger;
import ti4.model.*;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

public class AutoCompleteProvider {

    public static void autoCompleteListener(CommandAutoCompleteInteractionEvent event) {
        // List<OptionMapping> allOptions = event.getOptions();
        //String fullCommandName = event.getFullCommandName();
        String commandName = event.getName();
        //String subCommandGroupName = event.getSubcommandGroup();
        String subCommandName = event.getSubcommandName();
        String optionName = event.getFocusedOption().getName();

        //boolean showAllChoicesInGame = false;
        //OptionMapping factionOrColourOption = event.getOption(Constants.FACTION_COLOR);
        //if (factionOrColourOption != null) showAllChoicesInGame = true;

        String userID = event.getUser().getId();
        MessageListener.setActiveGame(event.getMessageChannel(), userID, event.getName(), event.getSubcommandName());
        Game activeGame = GameManager.getInstance().getUserActiveGame(userID);
        Player player = null;
        if (activeGame != null) {
            player = activeGame.getPlayer(userID);
            player = Helper.getGamePlayer(activeGame, player, event, null);
        }

        // VERY SPECIFIC HANDLING OF OPTIONS
        switch (commandName) {
            case Constants.DEVELOPER -> resolveDeveloperCommandAutoComplete(event, subCommandName, optionName);
            case Constants.DS_COMMAND -> resolveDiscordantStarsCommandAutoComplete(event, subCommandName, optionName);
            case Constants.SEARCH -> resolveSearchCommandAutoComplete(event, subCommandName, optionName);
            case Constants.CARDS_AC -> resolveActionCardAutoComplete(event, subCommandName, optionName, activeGame);
            case Constants.FRANKEN -> resolveFrankenAutoComplete(event, subCommandName, optionName, activeGame);
            case Constants.MAP -> resolveMapAutoComplete(event, subCommandName, optionName, activeGame);
        }

        // DON'T APPLY GENERIC HANDLING IF SPECIFIC HANDLING WAS APPLIED
        if (event.isAcknowledged()) return;

        // GENERIC HANDLING OF OPTIONS
        switch (optionName) {
            case Constants.COLOR -> {
                String enteredValue = event.getFocusedOption().getValue();
                List<Command.Choice> options = Mapper.getColors().stream()
                    .filter(color -> color.startsWith(enteredValue))
                    .limit(25)
                    .map(color -> new Command.Choice(color, color))
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();
            }
            case Constants.FACTION -> {
                String enteredValue = event.getFocusedOption().getValue();
                Map<String, String> factions = Mapper.getFactionRepresentations();
                List<Command.Choice> options;
                if (activeGame != null && activeGame.isDiscordantStarsMode()) {
                    options = factions.entrySet().stream()
                        .filter(token -> token.getValue().toLowerCase().contains(enteredValue))
                        .limit(25)
                        .map(token -> new Command.Choice(token.getValue(), token.getKey()))
                        .collect(Collectors.toList());
                } else {
                    options = factions.entrySet().stream()
                        .filter(Predicate.not(token -> token.getValue().toUpperCase().endsWith("(DS)")))
                        .filter(token -> token.getValue().toLowerCase().contains(enteredValue))
                        .limit(25)
                        .map(token -> new Command.Choice(token.getValue(), token.getKey()))
                        .collect(Collectors.toList());
                }
                event.replyChoices(options).queue();
            }
            case Constants.FACTION_COLOR, Constants.FACTION_COLOR_1, Constants.FACTION_COLOR_2 -> {
                if (activeGame == null) {
                    event.replyChoiceStrings("No game found in this channel").queue();
                    break;
                }
                String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                if (activeGame.isFoWMode()) {
                    List<String> factionColors = new ArrayList<>(Mapper.getFactionIDs());
                    factionColors.addAll(Mapper.getColors());

                    List<String> factionColorsRetain = new ArrayList<>();
                    boolean privateGame = FoWHelper.isPrivateGame(activeGame, null, event.getChannel());
                    for (Player player_ : activeGame.getPlayers().values()) {
                        if (!privateGame) {
                            factionColorsRetain.add(player_.getFaction());
                        }
                        factionColorsRetain.add(player_.getColor());
                    }
                    factionColors.retainAll(factionColorsRetain);
                    List<Command.Choice> options = factionColors.stream()
                        .filter(token -> token.contains(enteredValue))
                        .limit(25)
                        .map(token -> new Command.Choice(token, token))
                        .collect(Collectors.toList());
                    event.replyChoices(options).queue();
                } else {
                    List<Command.Choice> options = activeGame.getPlayers().values().stream()
                        .filter(p -> p.getAutoCompleteRepresentation().toLowerCase().contains(enteredValue))
                        .limit(25)
                        .map(p -> new Command.Choice(p.getAutoCompleteRepresentation(), p.getColor()))
                        .toList();
                    event.replyChoices(options).queue();
                }
            }
            case Constants.CC_USE -> {
                String enteredValue = event.getFocusedOption().getValue();
                List<String> values = Arrays.asList("t/tactics", "r/retreat/reinforcements", "no");
                List<Command.Choice> options = values.stream()
                    .filter(token -> token.contains(enteredValue))
                    .limit(25)
                    .map(token -> new Command.Choice(token, token))
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();
            }
            case Constants.TOKEN -> {
                String enteredValue = event.getFocusedOption().getValue();
                List<Command.Choice> options = Mapper.getTokens().stream()
                    .filter(token -> token.contains(enteredValue))
                    .limit(25)
                    .map(token -> new Command.Choice(token, token))
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();
            }
            case Constants.DISPLAY_TYPE -> {
                String enteredValue = event.getFocusedOption().getValue();
                List<Command.Choice> options = Stream.of("all", "map", "stats", "split", "none")
                    .filter(value -> value.contains(enteredValue))
                    .limit(25)
                    .map(value -> new Command.Choice(value, value))
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();
            }

            case Constants.RELIC -> {
                String enteredValue = event.getFocusedOption().getValue().toLowerCase();

                List<String> tableRelics = new ArrayList<>();
                if (activeGame != null) {
                    for (Player player_ : activeGame.getPlayers().values()) {
                        List<String> playerRelics = player_.getRelics();
                        tableRelics.addAll(playerRelics);
                    }
                    List<String> relicDeck = activeGame.getAllRelics();
                    tableRelics.addAll(relicDeck);
                    Collections.shuffle(tableRelics);
                }

                List<Command.Choice> options = tableRelics.stream()
                    .filter(value -> value.toLowerCase().contains(enteredValue))
                    .limit(25)
                    .map(value -> new Command.Choice(value, value))
                    .collect(Collectors.toList());

                event.replyChoices(options).queue();
            }
            case Constants.RELIC_ALL -> {
                String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                Map<String, RelicModel> relics = Mapper.getRelics();

                List<Command.Choice> options = relics.entrySet().stream()
                    .filter(value -> value.getValue().getName().toLowerCase().contains(enteredValue))
                    .limit(25)
                    .map(value -> new Command.Choice(value.getValue().getName(), value.getKey()))
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();
            }
            case Constants.PO_ID -> {
                String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                Map<String, PublicObjectiveModel> publicObjectives = Mapper.getPublicObjectives();
                List<Command.Choice> options = publicObjectives.entrySet().stream()
                    .filter(value -> value.getValue().getName().toLowerCase().contains(enteredValue))
                    .limit(25)
                    .map(value -> new Command.Choice(value.getValue().getName(), value.getKey()))
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();
            }
            case Constants.SO_ID -> {
                String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                HashMap<String, String> actionCards = Mapper.getSecretObjectivesJustNamesAndSource();
                List<Command.Choice> options = actionCards.entrySet().stream()
                    .filter(value -> value.getValue().toLowerCase().contains(enteredValue))
                    .limit(25)
                    .map(value -> new Command.Choice(value.getValue(), value.getKey()))
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();
            }
            case Constants.AGENDA_ID -> {
                String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                switch (subCommandName) {
                    case Constants.REVEAL_SPECIFIC -> {
                        List<Command.Choice> options = Mapper.getAgendas().entrySet().stream()
                            .filter(value -> value.getValue().getName().toLowerCase().contains(enteredValue) || value.getValue().getAlias().toLowerCase().contains(enteredValue))
                            .limit(25)
                            .map(value -> new Command.Choice(value.getValue().getName() + " (" + value.getValue().getSource() + ")", value.getKey()))
                            .collect(Collectors.toList());
                        event.replyChoices(options).queue();
                    }
                    default -> {
                        HashMap<String, String> agendas = Mapper.getAgendaJustNames(activeGame);
                        List<Command.Choice> options = agendas.entrySet().stream()
                            .filter(value -> value.getValue().toLowerCase().contains(enteredValue))
                            .limit(25)
                            .map(value -> new Command.Choice(value.getValue(), value.getKey()))
                            .collect(Collectors.toList());
                        event.replyChoices(options).queue();
                    }
                }
            }
            case Constants.AC_ID -> {
                String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                Map<String, String> actionCards = Mapper.getACJustNames();
                List<Command.Choice> options = actionCards.entrySet().stream()
                    .filter(value -> value.getValue().toLowerCase().contains(enteredValue))
                    .limit(25)
                    .map(value -> new Command.Choice(value.getValue(), value.getKey()))
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();
            }
            case Constants.PROMISSORY_NOTE_ID -> {
                String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                Map<String, PromissoryNoteModel> PNs = Mapper.getPromissoryNotes();
                List<Command.Choice> options = PNs.values().stream()
                    .filter(pn -> (pn.getAlias() + " " + pn.getName() + " " + pn.getOwner()).toLowerCase().contains(enteredValue))
                    .limit(25)
                    .map(pn -> new Command.Choice(pn.getAlias() + " " + pn.getName() + " " + pn.getOwner(), pn.getAlias()))
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();
            }
            case Constants.UNIT_ID, Constants.UNIT_ID_1, Constants.UNIT_ID_2, Constants.UNIT_ID_3, Constants.UNIT_ID_4, Constants.UNIT_ID_5 -> {
                String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                Map<String, UnitModel> units = Mapper.getUnits();
                List<Command.Choice> options = units.values().stream()
                    .filter(unit -> (unit.getId() + " " + unit.getName()).toLowerCase().contains(enteredValue))
                    .limit(25)
                    .map(unit -> new Command.Choice(unit.getId() + " (" + unit.getName() + ")", unit.getId()))
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();
            }
            case Constants.LEADER, Constants.LEADER_1, Constants.LEADER_2, Constants.LEADER_3, Constants.LEADER_4 -> {
                List<String> leaderIDs = new ArrayList<>();
                if (activeGame == null || activeGame.isFoWMode() || Constants.LEADER_ADD.equals(event.getSubcommandName())) {
                    leaderIDs.addAll(Mapper.getLeaders().keySet());
                } else {
                    leaderIDs.addAll(List.of("agent", "commander", "hero"));
                    for (Player player_ : activeGame.getPlayers().values()) {
                        leaderIDs.addAll(player_.getLeaderIDs());
                    }
                }

                String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                List<Command.Choice> options = leaderIDs.stream()
                    .filter(value -> value.toLowerCase().contains(enteredValue))
                    .limit(25)
                    .map(value -> new Command.Choice(value, value))
                    .collect(Collectors.toList());
                try {
                    event.replyChoices(options).queue();
                } catch (Exception e) {
                    BotLogger.log(event, "Could not suggest leaders", e);
                }
            }
            case Constants.TECH, Constants.TECH2, Constants.TECH3, Constants.TECH4 -> {
                String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                Map<String, TechnologyModel> techs = Mapper.getTechs().entrySet().stream()
                    .filter(entry -> activeGame == null ? true : activeGame.getTechnologyDeck().contains(entry.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                List<Command.Choice> options = techs.entrySet().stream()
                    .filter(value -> value.getValue().getName().toLowerCase().contains(enteredValue))
                    .limit(25)
                    .map(value -> new Command.Choice(value.getValue().getName() + " (" + value.getValue().getSource() + ")", value.getKey()))
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();
            }
            case Constants.PLANET, Constants.PLANET2, Constants.PLANET3, Constants.PLANET4, Constants.PLANET5, Constants.PLANET6 -> {
                MessageListener.setActiveGame(event.getMessageChannel(), event.getUser().getId(), event.getName(), event.getSubcommandName());
                String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                Set<String> planetIDs;
                Map<String, String> planets = Mapper.getPlanetRepresentations();
                if (activeGame != null && !activeGame.isFoWMode()) {
                    planetIDs = activeGame.getPlanets();
                    List<Command.Choice> options = planets.entrySet().stream()
                        .filter(value -> value.getValue().toLowerCase().contains(enteredValue))
                        .filter(value -> planetIDs.isEmpty() || planetIDs.contains(value.getKey()))
                        .limit(25)
                        .map(value -> new Command.Choice(
                            value.getValue() + " (" + Helper.getPlanetResources(value.getKey(), activeGame) + "/" + Helper.getPlanetInfluence(value.getKey(), activeGame) + ")", value.getKey()))
                        .collect(Collectors.toList());
                    event.replyChoices(options).queue();
                } else if (activeGame != null && activeGame.isFoWMode()) {
                    List<Command.Choice> options = planets.entrySet().stream()
                        .filter(value -> value.getValue().toLowerCase().contains(enteredValue))
                        .limit(25)
                        .map(value -> new Command.Choice(value.getValue(), value.getKey()))
                        .collect(Collectors.toList());
                    event.replyChoices(options).queue();
                }
            }
            case Constants.TRAIT -> {
                String enteredValue = event.getFocusedOption().getValue();
                List<Command.Choice> options = Stream.of(Constants.CULTURAL, Constants.INDUSTRIAL, Constants.HAZARDOUS, Constants.FRONTIER)
                    .filter(value -> value.contains(enteredValue))
                    .limit(25)
                    .map(value -> new Command.Choice(value, value))
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();
            }
            case Constants.DECAL_SET -> {
                String enteredValue = event.getFocusedOption().getValue();
                List<Command.Choice> options = Mapper.getDecals().stream()
                    .filter(value -> value.contains(enteredValue) || Mapper.getDecalName(value).toLowerCase().contains(enteredValue))
                    .limit(25)
                    .map(value -> new Command.Choice(Mapper.getDecalName(value), value))
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();
            }
            case Constants.TTPG_FILE_NAME -> {
                String enteredValue = event.getFocusedOption().getValue();
                File exportDirectory = Storage.getTTPGExportDirectory();
                String dir = exportDirectory == null ? "/" : exportDirectory.getPath();

                Set<String> fileSet = Stream.of(new File(dir).listFiles())
                    .filter(file -> !file.isDirectory())
                    .map(File::getName)
                    .collect(Collectors.toSet());

                List<Command.Choice> options = fileSet.stream()
                    .filter(value -> value.contains(enteredValue))
                    .limit(25)
                    .map(value -> new Command.Choice(value, value))
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();
            }
            case Constants.FOG_FILTER -> {
                String enteredValue = event.getFocusedOption().getValue();
                List<Command.Choice> options = Stream.of("Dark Grey (default)", "Sepia", "White", "Pink", "Purple")
                    .filter(value -> value.toLowerCase().contains(enteredValue))
                    .limit(25)
                    .map(value -> new Command.Choice(value, value))
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();
            }
            case Constants.PRIMARY_TILE_DIRECTION -> {
                String enteredValue = event.getFocusedOption().getValue();
                List<Command.Choice> options = Stream.of("North", "Northeast", "Southeast", "South", "Southwest", "Northwest")
                    .filter(value -> value.toLowerCase().contains(enteredValue))
                    .limit(25)
                    .map(value -> new Command.Choice(value, value))
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();
            }
            case Constants.CATEGORY -> {
                String enteredValue = event.getFocusedOption().getValue();
                List<Category> categories = new ArrayList<>();
                for (Guild guild : AsyncTI4DiscordBot.jda.getGuilds()) {
                    categories.addAll(guild.getCategories());
                }
                List<Command.Choice> options = categories.stream()
                    .filter(c -> c.getName().toLowerCase().contains(enteredValue))
                    .limit(25)
                    .map(c -> new Command.Choice(c.getGuild().getName() + ": #" + c.getName(), c.getName()))
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();
            }
            case Constants.ANON -> {
                String enteredValue = event.getFocusedOption().getValue();
                List<Command.Choice> options = Stream.of("y", "n")
                    .filter(value -> value.contains(enteredValue))
                    .limit(25)
                    .map(value -> new Command.Choice(value, value))
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();
            }
            case Constants.TEXT_SIZE -> {
                String enteredValue = event.getFocusedOption().getValue();
                List<Command.Choice> options = Stream.of("tiny", "small", "medium", "large")
                    .filter(value -> value.contains(enteredValue))
                    .limit(25)
                    .map(value -> new Command.Choice(value, value))
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();
            }
            case Constants.SPECIFIC_PHASE -> {
                String enteredValue = event.getFocusedOption().getValue();
                List<Command.Choice> options = Stream.of("strategy", "voting", "statusScoring", "statusHomework", "action", "agendaResolve", "playerSetup")
                    .filter(value -> value.contains(enteredValue))
                    .limit(25)
                    .map(value -> new Command.Choice(value, value))
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();
            }
            case Constants.CREUSS_TOKEN_NAME -> {
                String enteredValue = event.getFocusedOption().getValue();
                List<Command.Choice> options = Stream.of("alpha", "beta", "gamma")
                    .filter(value -> value.contains(enteredValue))
                    .limit(25)
                    .map(value -> new Command.Choice(value, "creuss" + value))
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();
            }
            case Constants.ABILITY, Constants.ABILITY_1, Constants.ABILITY_2, Constants.ABILITY_3, Constants.ABILITY_4, Constants.ABILITY_5 -> {
                String enteredValue = event.getFocusedOption().getValue().toLowerCase();

                HashMap<String, AbilityModel> abilities = new HashMap<>();
                try {
                    if (player != null && event.getSubcommandName().equals(Constants.ABILITY_REMOVE)) {
                        for (String abilityID : player.getAbilities()) {
                            abilities.put(abilityID, Mapper.getAbilities().get(abilityID));
                        }
                    } else if (player != null && event.getSubcommandName().equals(Constants.ABILITY_ADD)) {
                        abilities = Mapper.getAbilities();
                        for (String abilityID : player.getAbilities()) {
                            abilities.remove(abilityID);
                        }
                    } else {
                        abilities = Mapper.getAbilities();
                    }
                } catch (Exception e) {
                    BotLogger.log(event, "Ability Autocomplete Setup Error", e);
                    abilities = Mapper.getAbilities();
                }

                List<Command.Choice> options = abilities.entrySet().stream()
                    .filter(value -> value.getValue().search(enteredValue))
                    .limit(25)
                    .map(value -> new Command.Choice(value.getValue().getAutoCompleteName(), value.getKey()))
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();

            }
            case Constants.LATEST_COMMAND -> {
                if (activeGame == null) {
                    event.replyChoiceStrings("No Active Map for this Channel").queue();
                    return;
                }
                String latestCommand;
                if (activeGame.isFoWMode()) { //!event.getUser().getID().equals(activeMap.getGMID()); //TODO: Validate that the user running the command is the FoW GM, if so, display command.
                    latestCommand = "Game is Fog of War mode - last command is hidden.";
                } else {
                    latestCommand = StringUtils.left(activeGame.getLatestCommand(), 100);
                }
                event.replyChoice(latestCommand, Constants.LATEST_COMMAND).queue();
            }
            case Constants.TILE_NAME, Constants.TILE_NAME_FROM, Constants.TILE_NAME_TO, Constants.HS_TILE_POSITION -> {
                String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                if (activeGame == null) {
                    event.replyChoiceStrings("No Active Map for this Channel").queue();
                    return;
                }
                if (activeGame.isFoWMode()) {
                    List<String> positions = new ArrayList<>(activeGame.getTileMap().keySet());
                    List<Command.Choice> options = positions.stream()
                        .filter(value -> value.toLowerCase().contains(enteredValue))
                        .limit(25)
                        .map(value -> new Command.Choice(value, value))
                        .collect(Collectors.toList());
                    event.replyChoices(options).queue();
                } else {
                    List<Command.Choice> options = activeGame.getTileNameAutocompleteOptionsCache().stream()
                        .filter(value -> value.getKey().toLowerCase().contains(enteredValue))
                        .limit(25)
                        .map(value -> new Command.Choice(value.getKey(), value.getValue()))
                        .collect(Collectors.toList());
                    event.replyChoices(options).queue();
                }
            }
            case Constants.DECK_NAME -> {
                String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                HashMap<String, DeckModel> decks = Mapper.getDecks();
                List<Command.Choice> options = decks.values().stream()
                    .filter(value -> value.getAlias().contains(enteredValue))
                    .map((deck) -> new Command.Choice(deck.getName(), deck.getAlias()))
                    .limit(25)
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();
            }
            case Constants.AC_DECK -> {
                String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                HashMap<String, DeckModel> decks = Mapper.getDecks();
                List<Command.Choice> options = decks.values().stream()
                    .filter(deckModel -> deckModel.getType().equals(Constants.ACTION_CARD))
                    .filter(value -> value.getAlias().contains(enteredValue))
                    .map((deck) -> new Command.Choice(deck.getName(), deck.getAlias()))
                    .limit(25)
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();
            }
            case Constants.SO_DECK -> {
                String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                HashMap<String, DeckModel> decks = Mapper.getDecks();
                List<Command.Choice> options = decks.values().stream()
                    .filter(deckModel -> deckModel.getType().equals(Constants.SECRET_OBJECTIVE))
                    .filter(value -> value.getAlias().contains(enteredValue))
                    .map((deck) -> new Command.Choice(deck.getName(), deck.getAlias()))
                    .limit(25)
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();
            }
            case Constants.STAGE_1_PUBLIC_DECK -> {
                String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                HashMap<String, DeckModel> decks = Mapper.getDecks();
                List<Command.Choice> options = decks.values().stream()
                    .filter(deckModel -> deckModel.getType().equals(Constants.STAGE_1_PUBLIC))
                    .filter(value -> value.getAlias().contains(enteredValue))
                    .map((deck) -> new Command.Choice(deck.getName(), deck.getAlias()))
                    .limit(25)
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();
            }
            case Constants.STAGE_2_PUBLIC_DECK -> {
                String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                HashMap<String, DeckModel> decks = Mapper.getDecks();
                List<Command.Choice> options = decks.values().stream()
                    .filter(deckModel -> deckModel.getType().equals(Constants.STAGE_2_PUBLIC))
                    .filter(value -> value.getAlias().contains(enteredValue))
                    .map((deck) -> new Command.Choice(deck.getName(), deck.getAlias()))
                    .limit(25)
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();
            }
            case Constants.RELIC_DECK -> {
                String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                HashMap<String, DeckModel> decks = Mapper.getDecks();
                List<Command.Choice> options = decks.values().stream()
                    .filter(deckModel -> deckModel.getType().equals(Constants.RELIC))
                    .filter(value -> value.getAlias().contains(enteredValue))
                    .map((deck) -> new Command.Choice(deck.getName(), deck.getAlias()))
                    .limit(25)
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();
            }
            case Constants.AGENDA_DECK -> {
                String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                HashMap<String, DeckModel> decks = Mapper.getDecks();
                List<Command.Choice> options = decks.values().stream()
                    .filter(deckModel -> deckModel.getType().equals(Constants.AGENDA))
                    .filter(value -> value.getAlias().contains(enteredValue))
                    .map((deck) -> new Command.Choice(deck.getName(), deck.getAlias()))
                    .limit(25)
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();
            }
            case Constants.EVENT_DECK -> {
                String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                HashMap<String, DeckModel> decks = Mapper.getDecks();
                List<Command.Choice> options = decks.values().stream()
                    .filter(deckModel -> deckModel.getType().equals(Constants.EVENT))
                    .filter(value -> value.getAlias().contains(enteredValue))
                    .map((deck) -> new Command.Choice(deck.getName(), deck.getAlias()))
                    .limit(25)
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();
            }
            case Constants.EXPLORATION_DECKS -> {
                String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                HashMap<String, DeckModel> decks = Mapper.getDecks();
                List<Command.Choice> options = decks.values().stream()
                    .filter(deckModel -> deckModel.getType().equals(Constants.EXPLORE))
                    .filter(value -> value.getAlias().contains(enteredValue))
                    .map((deck) -> new Command.Choice(deck.getName(), deck.getAlias()))
                    .limit(25)
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();
            }
            case Constants.TECHNOLOGY_DECK -> {
                String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                Map<String, DeckModel> decks = Mapper.getDecks();
                List<Command.Choice> options = decks.values().stream()
                    .filter(deckModel -> deckModel.getType().equals(Constants.TECHNOLOGY))
                    .filter(value -> value.getAlias().contains(enteredValue))
                    .map((deck) -> new Command.Choice(deck.getName(), deck.getAlias()))
                    .limit(25)
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();
            }
            case Constants.STRATEGY_CARD_SET -> {
                String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                Map<String, StrategyCardModel> decks = Mapper.getStrategyCardSets();
                List<Command.Choice> options = decks.values().stream()
                    .filter(scSet -> !"template".equals(scSet.getAlias()))
                    .filter(value -> value.getAlias().contains(enteredValue))
                    .map((scSet) -> new Command.Choice(scSet.getName(), scSet.getAlias()))
                    .limit(25)
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();
            }
            case Constants.BORDER_TYPE -> {
                String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                Map<String, String> anomalies = Arrays.stream(BorderAnomalyModel.BorderAnomalyType.values()) //Search string:name
                    .filter(anomalyType -> anomalyType != BorderAnomalyModel.BorderAnomalyType.ARROW)
                    .collect(Collectors.toMap(BorderAnomalyModel.BorderAnomalyType::toSearchString,
                        BorderAnomalyModel.BorderAnomalyType::getName));
                List<Command.Choice> options = anomalies.entrySet().stream()
                    .filter(anomaly -> anomaly.getValue().contains(enteredValue))
                    .map(anomaly -> new Command.Choice(anomaly.getValue(), anomaly.getKey()))
                    .limit(25)
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();
            }
            case Constants.VERBOSITY -> event.replyChoiceStrings(Constants.VERBOSITY_OPTIONS).queue();
            case Constants.AUTO_ARCHIVE_DURATION -> event.replyChoiceStrings("1_HOUR", "24_HOURS", "3_DAYS", "1_WEEK").queue();
            case Constants.PLANET_TYPE -> {
                List<String> allPlanetTypes = Arrays.stream(PlanetTypeModel.PlanetType.values())
                    .map(PlanetTypeModel.PlanetType::toString)
                    .toList();
                event.replyChoiceStrings(allPlanetTypes).queue();
            }
            case Constants.PLANET_TECH_SKIPS -> {
                List<String> allTechSkips = Arrays.stream(TechSpecialtyModel.TechSpecialty.values())
                    .map(TechSpecialtyModel.TechSpecialty::toString)
                    .toList();
                event.replyChoiceStrings(allTechSkips).queue();
            }
            case Constants.TILE_TYPE -> {
                List<String> allTileTypes = Arrays.stream(ShipPositionModel.ShipPosition.values())
                    .map(ShipPositionModel.ShipPosition::getTypeString)
                    .toList();
                event.replyChoiceStrings(allTileTypes).queue();
            }
            case Constants.TILE_WORMHOLES -> {
                List<String> allWormholeTypes = Arrays.stream(WormholeModel.Wormhole.values())
                    .limit(25)
                    .map(WormholeModel.Wormhole::toString)
                    .toList();
                event.replyChoiceStrings(allWormholeTypes).queue();
            }
            case Constants.ADD_REMOVE -> {
                String enteredValue = event.getFocusedOption().getValue();
                List<String> values = Arrays.asList("add", "remove");
                List<Command.Choice> options = values.stream()
                    .filter(token -> token.contains(enteredValue))
                    .limit(25)
                    .map(token -> new Command.Choice(token, token))
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();
            }
            case Constants.UNIT_SOURCE -> {
                String enteredValue = event.getFocusedOption().getValue();
                List<Command.Choice> options = Mapper.getUnitSources().stream()
                    .filter(token -> token.contains(enteredValue))
                    .limit(25)
                    .map(token -> new Command.Choice(token, token))
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();
            }
            case Constants.GAME_NAME -> {
                String enteredValue = event.getFocusedOption().getValue();
                List<Command.Choice> options = GameManager.getInstance().getGameNames().stream()
                    .filter(token -> token.contains(enteredValue))
                    .limit(25)
                    .map(token -> new Command.Choice(token, token))
                    .collect(Collectors.toList());
                event.replyChoices(options).queue();
            }
        }
    }

    private static void resolveActionCardAutoComplete(CommandAutoCompleteInteractionEvent event, String subCommandName, String optionName, Game activeGame) {
        switch (subCommandName) {
            case Constants.PICK_AC_FROM_DISCARD, Constants.SHUFFLE_AC_BACK_INTO_DECK -> {
                switch (optionName) {
                    case Constants.ACTION_CARD_ID -> {
                        String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                        Map<String, Integer> discardActionCardIDs = activeGame.getDiscardActionCards();
                        List<Command.Choice> options = discardActionCardIDs.entrySet().stream()
                            .map(entry -> Map.entry(Mapper.getActionCard(entry.getKey()), entry.getValue()))
                            .filter(entry -> entry.getKey().getName().toLowerCase().contains(enteredValue))
                            .limit(25)
                            .map(entry -> new Command.Choice(
                                entry.getKey().getName() + " (" + entry.getValue() + ")",
                                entry.getValue()))
                            .collect(Collectors.toList());
                        event.replyChoices(options).queue();
                    }
                }
            }
        }
    }

    private static void resolveDeveloperCommandAutoComplete(CommandAutoCompleteInteractionEvent event, String subCommandName, String optionName) {
        switch (subCommandName) {
            case Constants.SET_SETTING -> {
                switch (optionName) {
                    case Constants.SETTING_TYPE -> event.replyChoiceStrings("string", "number", "bool").queue();
                    case Constants.SETTING_NAME -> {
                        String enteredValue = event.getFocusedOption().getValue();
                        List<GlobalSettings.ImplementedSettings> settings = new ArrayList<>(
                            List.of(GlobalSettings.ImplementedSettings.values()));
                        List<Command.Choice> options = settings.stream()
                            .map(setting -> setting.toString())
                            .filter(setting -> setting.contains(enteredValue))
                            .limit(25)
                            .map(setting -> new Command.Choice(setting, setting))
                            .collect(Collectors.toList());
                        event.replyChoices(options).queue();
                    }
                }
            }
        }
    }

    private static void resolveDiscordantStarsCommandAutoComplete(CommandAutoCompleteInteractionEvent event, String subCommandName, String optionName) {
        switch (subCommandName) {
            case Constants.SET_POLICY -> {
                switch (optionName) {
                    case Constants.SET_PEOPLE -> {
                        String enteredValue = event.getFocusedOption().getValue();
                        List<Command.Choice> options = Stream.of("Connect", "Control", "+", "-")
                            .filter(value -> value.contains(enteredValue))
                            .limit(25)
                            .map(value -> new Command.Choice(value, value))
                            .collect(Collectors.toList());
                        event.replyChoices(options).queue();
                    }
                    case Constants.SET_ENVIRONMENT -> {
                        String enteredValue = event.getFocusedOption().getValue();
                        List<Command.Choice> options = Stream.of("Preserve", "Plunder", "+", "-")
                            .filter(value -> value.contains(enteredValue))
                            .limit(25)
                            .map(value -> new Command.Choice(value, value))
                            .collect(Collectors.toList());
                        event.replyChoices(options).queue();
                    }
                    case Constants.SET_ECONOMY -> {
                        String enteredValue = event.getFocusedOption().getValue();
                        List<Command.Choice> options = Stream.of("Empower", "Exploit", "+", "-")
                            .filter(value -> value.contains(enteredValue))
                            .limit(25)
                            .map(value -> new Command.Choice(value, value))
                            .collect(Collectors.toList());
                        event.replyChoices(options).queue();
                    }
                }
            }
        }
    }

    private static void resolveSearchCommandAutoComplete(CommandAutoCompleteInteractionEvent event, String subCommandName, String optionName) {
        switch (subCommandName) {
            case Constants.SEARCH_PLANETS -> {
                switch (optionName) {
                    case Constants.SEARCH -> {
                        String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                        List<Command.Choice> options = TileHelper.getAllPlanets().entrySet().stream()
                            .filter(entry -> entry.getValue().search(enteredValue))
                            .limit(25)
                            .map(entry -> new Command.Choice(entry.getValue().getAutoCompleteName(), entry.getKey()))
                            .collect(Collectors.toList());
                        event.replyChoices(options).queue();
                    }
                }
            }
            case Constants.SEARCH_TILES -> {
                switch (optionName) {
                    case Constants.SEARCH -> {
                        String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                        List<Command.Choice> options = TileHelper.getAllTiles().values().stream()
                            .filter(value -> value.getName() != null && value.getName().toLowerCase().contains(enteredValue))
                            .limit(25)
                            .map(value -> new Command.Choice("(" + value.getId() + ") " + value.getName(), value.getId()))
                            .collect(Collectors.toList());
                        event.replyChoices(options).queue();
                    }
                }
            }
            case Constants.SEARCH_LEADERS -> {
                switch (optionName) {
                    case Constants.SEARCH -> {
                        String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                        List<Command.Choice> options =  Mapper.getLeaders().entrySet().stream()
                            .filter(entry -> entry.getValue().search(enteredValue))
                            .limit(25)
                            .map(entry -> new Command.Choice(entry.getValue().getAutoCompleteName(), entry.getKey()))
                            .collect(Collectors.toList());
                        event.replyChoices(options).queue();
                    }
                }
            }
            case Constants.SEARCH_UNITS -> {
                switch (optionName) {
                    case Constants.SEARCH -> {
                        String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                        List<Command.Choice> options = Mapper.getUnits().entrySet().stream()
                            .filter(entry -> entry.getValue().search(enteredValue))
                            .limit(25)
                            .map(entry -> new Command.Choice(entry.getValue().getAutoCompleteName(), entry.getKey()))
                            .collect(Collectors.toList());
                        event.replyChoices(options).queue();
                    }
                }
            }
            case Constants.SEARCH_TECHS -> {
                switch (optionName) {
                    case Constants.SEARCH -> {
                        String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                        List<Command.Choice> options = Mapper.getTechs().entrySet().stream()
                            .filter(entry -> entry.getValue().search(enteredValue))
                            .limit(25)
                            .map(entry -> new Command.Choice(entry.getValue().getAutoCompleteName(), entry.getKey()))
                            .collect(Collectors.toList());
                        event.replyChoices(options).queue();
                    }
                }
            }
            case Constants.SEARCH_ABILITIES -> {
                switch (optionName) {
                    case Constants.SEARCH -> {
                        String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                        List<Command.Choice> options = Mapper.getAbilities().entrySet().stream()
                            .filter(entry -> entry.getValue().search(enteredValue))
                            .limit(25)
                            .map(entry -> new Command.Choice(entry.getValue().getAutoCompleteName(), entry.getKey()))
                            .collect(Collectors.toList());
                        event.replyChoices(options).queue();
                    }
                }
            }
            case Constants.SEARCH_EXPLORES -> {
                switch (optionName) {
                    case Constants.SEARCH -> {
                        String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                        List<Command.Choice> options = Mapper.getExplores().entrySet().stream()
                            .filter(entry -> entry.getValue().search(enteredValue))
                            .limit(25)
                            .map(entry -> new Command.Choice(entry.getValue().getAutoCompleteName(), entry.getKey()))
                            .collect(Collectors.toList());
                        event.replyChoices(options).queue();
                    }
                }
            }
            case Constants.SEARCH_RELICS -> {
                switch (optionName) {
                    case Constants.SEARCH -> {
                        String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                        List<Command.Choice> options = Mapper.getRelics().entrySet().stream()
                            .filter(entry -> entry.getValue().search(enteredValue))
                            .limit(25)
                            .map(entry -> new Command.Choice(entry.getValue().getAutoCompleteName(), entry.getKey()))
                            .collect(Collectors.toList());
                        event.replyChoices(options).queue();
                    }
                }
            }
            case Constants.SEARCH_AGENDAS -> {
                switch (optionName) {
                    case Constants.SEARCH -> {
                        String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                        List<Command.Choice> options = Mapper.getAgendas().entrySet().stream()
                            .filter(entry -> entry.getValue().search(enteredValue))
                            .limit(25)
                            .map(entry -> new Command.Choice(entry.getValue().getAutoCompleteName(), entry.getKey()))
                            .collect(Collectors.toList());
                        event.replyChoices(options).queue();
                    }
                }
            }
            case Constants.SEARCH_ACTION_CARDS -> {
                switch (optionName) {
                    case Constants.SEARCH -> {
                        String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                        List<Command.Choice> options = Mapper.getActionCards().entrySet().stream()
                            .filter(entry -> entry.getValue().search(enteredValue))
                            .limit(25)
                            .map(entry -> new Command.Choice(entry.getValue().getAutoCompleteName(), entry.getKey()))
                            .collect(Collectors.toList());
                        event.replyChoices(options).queue();
                    }
                }
            }
            case Constants.SEARCH_SECRET_OBJECTIVES -> {
                switch (optionName) {
                    case Constants.SEARCH -> {
                        String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                        List<Command.Choice> options = Mapper.getSecretObjectives().entrySet().stream()
                            .filter(entry -> entry.getValue().search(enteredValue))
                            .limit(25)
                            .map(entry -> new Command.Choice(entry.getValue().getAutoCompleteName(), entry.getKey()))
                            .collect(Collectors.toList());
                        event.replyChoices(options).queue();
                    }
                }
            }
            case Constants.SEARCH_PUBLIC_OBJECTIVES -> {
                switch (optionName) {
                    case Constants.SEARCH -> {
                        String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                        List<Command.Choice> options = Mapper.getPublicObjectives().entrySet().stream()
                            .filter(entry -> entry.getValue().search(enteredValue))
                            .limit(25)
                            .map(entry -> new Command.Choice(entry.getValue().getAutoCompleteName(), entry.getKey()))
                            .collect(Collectors.toList());
                        event.replyChoices(options).queue();
                    }
                }
            }
            case Constants.SEARCH_PROMISSORY_NOTES -> {
                switch (optionName) {
                    case Constants.SEARCH -> {
                        String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                        List<Command.Choice> options = Mapper.getPromissoryNotes().entrySet().stream()
                            .filter(entry -> entry.getValue().search(enteredValue))
                            .limit(25)
                            .map(entry -> new Command.Choice(entry.getValue().getAutoCompleteName(), entry.getKey()))
                            .collect(Collectors.toList());
                        event.replyChoices(options).queue();
                    }
                }
            }
            case Constants.SEARCH_DECKS -> {
                switch (optionName) {
                    case Constants.SEARCH -> {
                        String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                        List<Command.Choice> options = Mapper.getDecks().entrySet().stream()
                            .filter(entry -> entry.getValue().search(enteredValue))
                            .limit(25)
                            .map(entry -> new Command.Choice(entry.getValue().getAutoCompleteName(), entry.getKey()))
                            .collect(Collectors.toList());
                        event.replyChoices(options).queue();
                    }
                }
            }
        }
    }

    private static void resolveFrankenAutoComplete(CommandAutoCompleteInteractionEvent event, String subCommandName, String optionName, Game activeGame) {
        switch (subCommandName) {
            case Constants.FACTION_TECH_ADD, Constants.FACTION_TECH_REMOVE -> {
                switch (optionName) {
                    case Constants.TECH, Constants.TECH2, Constants.TECH3, Constants.TECH4 -> {
                        String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                        Map<String, TechnologyModel> techs = new HashMap<>(Mapper.getTechs());
                        List<Command.Choice> options = techs.entrySet().stream()
                            .filter(entry -> entry.getValue().getFaction() != null)
                            .filter(entry -> entry.getValue().search(enteredValue))
                            .limit(25)
                            .map(entry -> new Command.Choice(entry.getValue().getAutoCompleteName(), entry.getKey()))
                            .collect(Collectors.toList());
                        event.replyChoices(options).queue();
                    }
                }
            }
        }
    }

    private static void resolveMapAutoComplete(CommandAutoCompleteInteractionEvent event, String subCommandName, String optionName, Game activeGame) {
        if (activeGame == null) {
            event.replyChoiceStrings("No Active Map for this Channel").queue();
            return;
        }
        switch (subCommandName) {
            case Constants.ADD_TILE -> {
                switch (optionName) {
                    case Constants.TILE_NAME -> {
                        String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                        List<Command.Choice> options = TileHelper.getAllTiles().entrySet().stream()
                            .filter(entry -> entry.getValue().search(enteredValue))
                            .limit(25)
                            .map(entry -> new Command.Choice(entry.getValue().getAutoCompleteName(), entry.getKey()))
                            .collect(Collectors.toList());
                        event.replyChoices(options).queue();
                    }
                    // case Constants.POSITION -> {
                    //     String enteredValue = event.getFocusedOption().getValue().toLowerCase();
                    //     List<Command.Choice> options = activeGame.getTileMap().entrySet().stream()
                    //         .filter(entry -> entry.getValue().search(enteredValue))
                    //         .limit(25)
                    //         .map(entry -> new Command.Choice(entry.getValue().getAutoCompleteName(), entry.getKey()))
                    //         .collect(Collectors.toList());
                    //     event.replyChoices(options).queue();
                    // }
                }
            }
        }
    }
}