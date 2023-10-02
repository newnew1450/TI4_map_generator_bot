package ti4.commands.game;

import java.util.ArrayList;

import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ti4.generator.Mapper;
import ti4.helpers.Constants;
import ti4.map.Game;
import ti4.map.Player;
import ti4.message.MessageHelper;

public class Setup extends GameSubcommandData {
    public Setup() {
        super(Constants.SETUP, "Game Setup");
        addOptions(new OptionData(OptionType.INTEGER, Constants.PLAYER_COUNT_FOR_MAP, "Specify player map size between 2 or 30. Default 6").setRequired(false));
        addOptions(new OptionData(OptionType.INTEGER, Constants.VP_COUNT, "Specify game VP count. Default is 10").setRequired(false));
        addOptions(new OptionData(OptionType.STRING, Constants.GAME_CUSTOM_NAME, "Add Custom description to game").setRequired(false));
        addOptions(new OptionData(OptionType.BOOLEAN, Constants.TIGL_GAME, "True to mark the game as TIGL"));
        addOptions(new OptionData(OptionType.BOOLEAN, Constants.COMMUNITY_MODE, "True if want Community Mode for map, False to disable it").setRequired(false));
        addOptions(new OptionData(OptionType.BOOLEAN, Constants.FOW_MODE, "True if want FoW Mode for map, False to disable it").setRequired(false));
        addOptions(new OptionData(OptionType.BOOLEAN, Constants.BASE_GAME_MODE, "True to "));
        addOptions(new OptionData(OptionType.BOOLEAN, Constants.ABSOL_MODE, "True to switch out the PoK Agendas & Relics for Absol's "));
        addOptions(new OptionData(OptionType.BOOLEAN, Constants.DISCORDANT_STARS_MODE, "True to add the Discordant Stars factions to the pool."));
        addOptions(new OptionData(OptionType.INTEGER, Constants.AUTO_PING, "Hours between auto pings. Min 1. Enter 0 to turn off."));
        addOptions(new OptionData(OptionType.BOOLEAN, Constants.BETA_TEST_MODE, "True to test new features that may not be released to all games yet."));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Game activeGame = getActiveGame();

        OptionMapping playerCount = event.getOption(Constants.PLAYER_COUNT_FOR_MAP);
        if (playerCount != null) {
            int count = playerCount.getAsInt();
            if (count < 2 || count > 30) {
                MessageHelper.sendMessageToChannel(event.getChannel(), "Must specify between 2 or 30 players.");
            } else {
                activeGame.setPlayerCountForMap(count);
            }
        }

        OptionMapping vpOption = event.getOption(Constants.VP_COUNT);
        if (vpOption != null) {
            int count = vpOption.getAsInt();
            if (count < 1) {
                count = 1;
            } else if (count > 20) {
                count = 20;
            }
            activeGame.setVp(count);
        }

        Boolean communityMode = event.getOption(Constants.COMMUNITY_MODE, null, OptionMapping::getAsBoolean);
        if (communityMode != null) activeGame.setCommunityMode(communityMode);

        Boolean fowMode = event.getOption(Constants.FOW_MODE, null, OptionMapping::getAsBoolean);
        if (fowMode != null) activeGame.setFoWMode(fowMode);

        Integer pingHours = event.getOption(Constants.AUTO_PING, null, OptionMapping::getAsInt);
        if (pingHours != null) {
            if (pingHours == 0) {
                activeGame.setAutoPing(false);
                activeGame.setAutoPingSpacer(pingHours);
            } else {
                activeGame.setAutoPing(true);
                if (pingHours < 1){
                    pingHours = 1;
                }
                activeGame.setAutoPingSpacer(pingHours);
            }
        }

        String customGameName = event.getOption(Constants.GAME_CUSTOM_NAME, null, OptionMapping::getAsString);
        if (customGameName != null) {
            activeGame.setCustomName(customGameName);
        }

        if (!setGameMode(event, activeGame)) {
            MessageHelper.sendMessageToChannel(event.getChannel(), "Something went wrong and the game modes could not be set, please see error above.");
        }

        Boolean betaTestMode = event.getOption(Constants.BETA_TEST_MODE, null, OptionMapping::getAsBoolean);
        if (betaTestMode != null) activeGame.setTestBetaFeaturesMode(betaTestMode);
    }

    public static boolean setGameMode(SlashCommandInteractionEvent event, Game activeGame) {
        if (event.getOption(Constants.TIGL_GAME) == null && event.getOption(Constants.ABSOL_MODE) == null && event.getOption(Constants.DISCORDANT_STARS_MODE) == null && event.getOption(Constants.BASE_GAME_MODE) == null) {
            return true; //no changes were made
        }
        boolean isTIGLGame = event.getOption(Constants.TIGL_GAME, activeGame.isCompetitiveTIGLGame(), OptionMapping::getAsBoolean);
        boolean absolMode = event.getOption(Constants.ABSOL_MODE, activeGame.isAbsolMode(), OptionMapping::getAsBoolean);
        boolean discordantStarsMode = event.getOption(Constants.DISCORDANT_STARS_MODE, activeGame.isDiscordantStarsMode(), OptionMapping::getAsBoolean);
        boolean baseGameMode = event.getOption(Constants.BASE_GAME_MODE, activeGame.isBaseGameMode(), OptionMapping::getAsBoolean);
        return setGameMode(event, activeGame, baseGameMode, absolMode, discordantStarsMode, isTIGLGame);
    }

    public static boolean setGameMode(GenericInteractionCreateEvent event, Game activeGame, boolean baseGameMode, boolean absolMode, boolean discordantStarsMode, boolean isTIGLGame) {

        if (isTIGLGame && (baseGameMode || absolMode || discordantStarsMode || activeGame.isHomeBrewSCMode() || activeGame.isFoWMode() || activeGame.isAllianceMode() || activeGame.isCommunityMode())) {
            MessageHelper.sendMessageToChannel(event.getMessageChannel(), "TIGL Games can not be mixed with other game modes.");
            return false;
        } else if (isTIGLGame) {
            activeGame.setCompetitiveTIGLGame(isTIGLGame);
            return true;
        }

        if (baseGameMode && (absolMode || discordantStarsMode)) {
            MessageHelper.sendMessageToChannel(event.getMessageChannel(), "Base Game Mode is not supported with Discordant Stars or Absol Mode");
            return false;
        } else if (baseGameMode) {
            if (!activeGame.validateAndSetAgendaDeck(event, Mapper.getDeck("agendas_base_game"))) return false;
            if (!activeGame.validateAndSetPublicObjectivesStage1Deck(event, Mapper.getDeck("public_stage_1_objectives_base"))) return false;
            if (!activeGame.validateAndSetPublicObjectivesStage2Deck(event, Mapper.getDeck("public_stage_2_objectives_base"))) return false;
            if (!activeGame.validateAndSetSecretObjectiveDeck(event, Mapper.getDeck("secret_objectives_base"))) return false;
            if (!activeGame.validateAndSetActionCardDeck(event, Mapper.getDeck("action_cards_basegame_and_codex1"))) return false;
            if (!activeGame.validateAndSetRelicDeck(event, Mapper.getDeck("relics_base"))) return false;
            if (!activeGame.validateAndSetExploreDeck(event, Mapper.getDeck("explores_base"))) return false;

            for (Player player : activeGame.getPlayers().values()) {
                player.setLeaders(new ArrayList<>());
            }

            activeGame.setScSetID("base_game");

            activeGame.setTechnologyDeckID("techs_base");
            activeGame.setBaseGameMode(baseGameMode);
            activeGame.setAbsolMode(false);
            activeGame.setDiscordantStarsMode(false);
            return true;
        }
        
        // BOTH ABSOL & DS, and/or if either was set before the other
        if (absolMode && discordantStarsMode) {
            if (!activeGame.validateAndSetAgendaDeck(event, Mapper.getDeck("agendas_absol"))) return false;
            if (!activeGame.validateAndSetActionCardDeck(event, Mapper.getDeck("action_cards_ds"))) return false;
            if (!activeGame.validateAndSetRelicDeck(event, Mapper.getDeck("relics_absol_ds"))) return false;
            activeGame.setTechnologyDeckID("techs_ds_absol");
            // SOMEHOW HANDLE MECHS AND STARTING/FACTION TECHS
            activeGame.setAbsolMode(absolMode);
            activeGame.setDiscordantStarsMode(discordantStarsMode);
            activeGame.setBaseGameMode(false);
            return true;
        }
    
        // JUST DS
        if (discordantStarsMode && !absolMode) {
            if (!activeGame.validateAndSetAgendaDeck(event, Mapper.getDeck("agendas_pok"))) return false;
            if (!activeGame.validateAndSetActionCardDeck(event, Mapper.getDeck("action_cards_ds"))) return false;
            if (!activeGame.validateAndSetRelicDeck(event, Mapper.getDeck("relics_ds"))) return false;
            activeGame.setTechnologyDeckID("techs_ds");
            activeGame.setAbsolMode(false);
        }
        activeGame.setDiscordantStarsMode(discordantStarsMode);

        // JUST ABSOL
        if (absolMode && !discordantStarsMode) {
            if (!activeGame.validateAndSetAgendaDeck(event, Mapper.getDeck("agendas_absol"))) return false;
            if (!activeGame.validateAndSetActionCardDeck(event, Mapper.getDeck("action_cards_pok"))) return false;
            if (!activeGame.validateAndSetRelicDeck(event, Mapper.getDeck("relics_absol"))) return false;
            activeGame.setTechnologyDeckID("techs_absol");
            activeGame.setDiscordantStarsMode(false);
            // SOMEHOW HANDLE MECHS AND STARTING/FACTION TECHS
        }
        activeGame.setAbsolMode(absolMode);

        // JUST PoK
        if (!absolMode && !discordantStarsMode) {
            if (!activeGame.validateAndSetAgendaDeck(event, Mapper.getDeck("agendas_pok"))) return false;
            if (!activeGame.validateAndSetActionCardDeck(event, Mapper.getDeck("action_cards_pok"))) return false;
            if (!activeGame.validateAndSetRelicDeck(event, Mapper.getDeck("relics_pok"))) return false;
            activeGame.setTechnologyDeckID("techs_pok");
            activeGame.setBaseGameMode(false);
            activeGame.setAbsolMode(false);
            activeGame.setDiscordantStarsMode(false);
        }

        return true;
    }
}

