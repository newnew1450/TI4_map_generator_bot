package ti4.commands.leaders;

import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import ti4.commands.cardsac.ACInfo;
import ti4.commands.explore.DrawRelic;
import ti4.commands.planet.PlanetRefresh;
import ti4.commands.special.KeleresHeroMentak;
import ti4.commands.tokens.AddCC;
import ti4.commands.tokens.AddFrontierTokens;
import ti4.commands.tokens.RemoveCC;
import ti4.generator.Mapper;
import ti4.helpers.AliasHandler;
import ti4.helpers.ButtonHelper;
import ti4.helpers.ButtonHelperHeroes;
import ti4.helpers.CombatModHelper;
import ti4.helpers.Constants;
import ti4.helpers.Emojis;
import ti4.helpers.Helper;
import ti4.map.Game;
import ti4.map.Leader;
import ti4.map.Player;
import ti4.map.Tile;
import ti4.message.BotLogger;
import ti4.message.MessageHelper;
import ti4.model.LeaderModel;

public class HeroPlay extends LeaderAction {
    public HeroPlay() {
        super(Constants.ACTIVE_LEADER, "Play Hero");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Game activeGame = getActiveGame();
        Player player = activeGame.getPlayer(getUser().getId());
        player = Helper.getGamePlayer(activeGame, player, event, null);

        if (player == null) {
            sendMessage("Player could not be found");
            return;
        }
        action(event, "hero", activeGame, player);
    }

    @Override
    protected void options() {
        addOptions(new OptionData(OptionType.STRING, Constants.LEADER, "Leader for which to do action").setAutoComplete(true));
        addOptions(new OptionData(OptionType.STRING, Constants.FACTION_COLOR, "Faction or Color for which you set stats").setAutoComplete(true));
    }

    @Override
    void action(SlashCommandInteractionEvent event, String leaderID, Game activeGame, Player player) {
        Leader playerLeader = player.unsafeGetLeader(leaderID);
        
        if (playerLeader == null) {
            sendMessage("Leader '" + leaderID + "'' could not be found. The leader might have been purged earlier.");
            return;
        }
        
        if (playerLeader.isLocked()) {
            sendMessage("Leader is locked, use command to unlock `/leaders unlock leader:" + leaderID + "`");
            sendMessage(Helper.getLeaderLockedRepresentation(playerLeader));
            return;
        }

        if (!playerLeader.getType().equals(Constants.HERO)) {
            sendMessage("Leader is not a hero");
            return;
        }

        playHero(event, activeGame, player, playerLeader);
    }

    public static void playHero(GenericInteractionCreateEvent event, Game activeGame, Player player, Leader playerLeader) {
        LeaderModel leaderModel = playerLeader.getLeaderModel().orElse(null);
        boolean showFlavourText = Constants.VERBOSITY_VERBOSE.equals(activeGame.getOutputVerbosity());
        StringBuilder sb = new StringBuilder();
        if (leaderModel != null) {
            MessageHelper.sendMessageToChannel(ButtonHelper.getCorrectChannel(player, activeGame), player.getRepresentation() + " played:");
            ButtonHelper.getCorrectChannel(player, activeGame).sendMessageEmbeds(leaderModel.getRepresentationEmbed(false, true, false, showFlavourText)).queue();
        } else {
            MessageHelper.sendMessageToChannel(ButtonHelper.getCorrectChannel(player, activeGame), Emojis.getFactionLeaderEmoji(playerLeader));
            sb.append(player.getRepresentation()).append(" played ").append(Helper.getLeaderFullRepresentation(playerLeader));
            BotLogger.log(event, "Missing LeaderModel: " + playerLeader.getId());
        }

        if ("letnevhero".equals(playerLeader.getId()) || "nomadhero".equals(playerLeader.getId())) {
            playerLeader.setLocked(false);
            playerLeader.setActive(true);
            sb.append("\nLeader will be PURGED after status cleanup");
            MessageHelper.sendMessageToChannel(ButtonHelper.getCorrectChannel(player, activeGame), sb.toString());
        } else {
            boolean purged = player.removeLeader(playerLeader);


            if (purged) {
                MessageHelper.sendMessageToChannel(ButtonHelper.getCorrectChannel(player, activeGame), "Leader " + playerLeader.getId() + " has been purged");
                ButtonHelperHeroes.checkForMykoHero(activeGame, playerLeader.getId());
            } else {
                MessageHelper.sendMessageToChannel(event.getMessageChannel(), "Leader was not purged - something went wrong");
                return;
            }
        }

        switch (playerLeader.getId()) {
            case "titanshero" -> {
                String titanshero = Mapper.getTokenID("titanshero");
                System.out.println(titanshero);
                Tile t = activeGame.getTile(AliasHandler.resolveTile(player.getFaction()));
                if (activeGame.getTileFromPlanet("elysium") != null && activeGame.getTileFromPlanet("elysium").getPosition().equalsIgnoreCase(t.getPosition())) {
                    t.addToken("attachment_titanshero.png", "elysium");
                    MessageHelper.sendMessageToChannel(event.getMessageChannel(), "Attachment added to Elysium and it has been readied");
                    new PlanetRefresh().doAction(player, "elysium", activeGame);
                } else {
                    MessageHelper.sendMessageToChannel(event.getMessageChannel(), "`Use the following command to add the attachment: /add_token token:titanshero`");
                }
            }
            case "solhero" -> {
                MessageHelper.sendMessageToChannel(event.getMessageChannel(), player.getRepresentation(true, true) + " removed all of your ccs from the board");
                for (Tile t : activeGame.getTileMap().values()) {
                    if (AddCC.hasCC(event, player.getColor(), t)) {
                        RemoveCC.removeCC(event, player.getColor(), t, activeGame);
                    }
                }
            }
            case "l1z1xhero"->{
                String message = player.getRepresentation()+" Resolving L1 Hero. L1 Hero is at the moment implemented as a sort of tactical action, relying on the player to follow the rules. The game will know not to take a tactical cc from you, and will allow you to move out of locked systems. Reminder that you can carry infantry/ff with your dreads/flagship, and that they cant move into supernovas(or asteroid fields if you dont have antimass.)";
                List<Button> ringButtons = ButtonHelper.getPossibleRings(player, activeGame);
                activeGame.setL1Hero(true);
                activeGame.resetCurrentMovedUnitsFrom1TacticalAction();
                MessageHelper.sendMessageToChannelWithButtons(event.getMessageChannel(), message, ringButtons);
            }
            case "winnuhero" -> {
                List<Button> buttons = ButtonHelperHeroes.getWinnuHeroSCButtons(activeGame, player);
                MessageHelper.sendMessageToChannelWithButtons(event.getMessageChannel(), player.getRepresentation(true, showFlavourText)
                    + " use the button to pick which SC you'd like to do the primary of. Reminder you can allow others to do the secondary, but they should still pay a cc for resolving it.",
                    buttons);
            }
            case "arborechero" -> {
                List<Button> buttons = ButtonHelperHeroes.getArboHeroButtons(activeGame, player, event);
                MessageHelper.sendMessageToChannelWithButtons(event.getMessageChannel(), player.getRepresentation(true, showFlavourText)
                    + " use the buttons to build in a system", buttons);
            }
            case "saarhero" -> {
                List<Button> buttons = ButtonHelperHeroes.getSaarHeroButtons(activeGame, player, event);
                MessageHelper.sendMessageToChannelWithButtons(event.getMessageChannel(), player.getRepresentation(true, showFlavourText)
                    + " use the buttons to select the system to remove all opposing ff and inf from",
                    buttons);
            }
            case "nekrohero" -> {
                List<Button> buttons = ButtonHelperHeroes.getNekroHeroButtons(player, activeGame);
                MessageHelper.sendMessageToChannelWithButtons(event.getMessageChannel(), player.getRepresentation(true, showFlavourText)
                    + " use the button to pick which planet youd like to get a tech and tgs from (and kill any opponent units)",
                    buttons);
            }
            case "nivynhero" -> {
                ButtonHelperHeroes.resolveNivynHeroSustainEverything(activeGame, player);
                MessageHelper.sendMessageToChannel(event.getMessageChannel(), ButtonHelper.getIdent(player)+ " sustained all units except their mechs");
            }
            case "jolnarhero" -> {
                List<Button> buttons = ButtonHelperHeroes.getJolNarHeroSwapOutOptions(player, activeGame);
                MessageHelper.sendMessageToChannelWithButtons(event.getMessageChannel(), player.getRepresentation(true, showFlavourText)
                    + " use the buttons to pick what tech you would like to swap out. Reminder that since all swap are simultenous, you cannot swap out a tech and then swap it back in.",
                    buttons);
            }
            case "yinhero" -> {
                List<Button> buttons = new ArrayList<>();
                buttons.add(Button.primary(player.getFinButtonChecker() + "yinHeroStart", "Invade a planet with Yin Hero"));
                buttons.add(Button.danger("deleteButtons", "Delete Buttons"));
                MessageHelper.sendMessageToChannelWithButtons(event.getMessageChannel(), player.getRepresentation(true, showFlavourText)
                    + " use the button to do individual invasions, then delete the buttons when you have placed 3 total infantry.", buttons);
            }
            case "naazhero" -> {
                DrawRelic.drawRelicAndNotify(player, event, activeGame);
                List<Button> buttons = ButtonHelperHeroes.getNRAHeroButtons(activeGame, player);
                MessageHelper.sendMessageToChannelWithButtons(event.getMessageChannel(), player.getRepresentation(true, showFlavourText)
                    + " use the button to do TWO of the avaiable secondaries. (note, all are presented for conveinence, but two is the limit)", buttons);
            }
            case "mahacthero" -> {
                List<Button> buttons = ButtonHelperHeroes.getBenediction1stTileOptions(player, activeGame);
                MessageHelper.sendMessageToChannelWithButtons(event.getMessageChannel(), player.getRepresentation(true, showFlavourText)
                    + " use the button to decide which tile you wish to force ships to move from.", buttons);
            }
            case "ghosthero" -> {
                List<Button> buttons = ButtonHelperHeroes.getGhostHeroTilesStep1(activeGame, player);
                MessageHelper.sendMessageToChannelWithButtons(event.getMessageChannel(), player.getRepresentation(true, showFlavourText)
                    + " use the button to select the first tile you would like to swap with your hero.", buttons);
            }
            case "augershero" -> {
                List<Button> buttons = new ArrayList<>();
                buttons.add(Button.primary(player.getFinButtonChecker() + "augersHeroStart_" + 1, "Resolve Augers Hero on Stage 1 Deck"));
                buttons.add(Button.primary(player.getFinButtonChecker() + "augersHeroStart_" + 2, "Resolve Augers Hero on Stage 2 Deck"));
                buttons.add(Button.danger("deleteButtons", "Delete Buttons"));
                MessageHelper.sendMessageToChannelWithButtons(event.getMessageChannel(),
                    player.getRepresentation(true, showFlavourText) + " use the button to choose which objective type you wanna hero on", buttons);
            }
            case "empyreanhero" -> {
                new AddFrontierTokens().parsingForTile(event, activeGame);
                MessageHelper.sendMessageToChannel(event.getMessageChannel(), "Added frontier tokens");
                MessageHelper.sendMessageToChannelWithButtons(event.getMessageChannel(), "Use Buttons to explore empties", ButtonHelperHeroes.getEmpyHeroButtons(player, activeGame));
            }
            case "cabalhero" -> {
                List<Button> buttons = ButtonHelperHeroes.getCabalHeroButtons(player, activeGame);
                MessageHelper.sendMessageToChannelWithButtons(event.getMessageChannel(), "Use Buttons to capture people", buttons);
            }
            case "yssarilhero" -> {
                for (Player p2 : activeGame.getRealPlayers()) {
                    if (p2 == player || p2.getAc() == 0) {
                        continue;
                    }
                    List<Button> buttons = new ArrayList<>(ACInfo.getYssarilHeroActionCardButtons(activeGame, player, p2));
                    MessageHelper.sendMessageToChannelWithButtons(p2.getCardsInfoThread(),
                        Helper.getPlayerRepresentation(p2, activeGame, activeGame.getGuild(), true) + " Yssaril hero played.  Use buttons to select which AC you will offer to them.",
                        buttons);
                }
                MessageHelper.sendMessageToChannel(event.getMessageChannel(),
                    player.getRepresentation(true, showFlavourText) + " sent everyone a ping in their private threads with buttons to send you an AC");
            }
            case "keleresheroharka" -> {
                KeleresHeroMentak.resolveKeleresHeroMentak(activeGame, player, event);
            }
        }
        var posssibleCombatMod = CombatModHelper.GetPossibleTempModifier(Constants.LEADER, leaderModel.getID(), player.getNumberTurns());
        if (posssibleCombatMod != null) {
            player.addNewTempCombatMod(posssibleCombatMod);
            MessageHelper.sendMessageToChannel(event.getMessageChannel(), "Combat modifier will be applied next time you push the combat roll button.");
        }
    }
}
