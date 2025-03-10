package ti4.commands.leaders;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import ti4.helpers.ButtonHelper;
import ti4.helpers.ButtonHelperAbilities;
import ti4.helpers.Constants;
import ti4.helpers.Emojis;
import ti4.helpers.Helper;
import ti4.map.Game;
import ti4.map.Leader;
import ti4.map.Player;
import ti4.message.MessageHelper;

public class RefreshLeader extends LeaderAction {
    public RefreshLeader() {
        super(Constants.REFRESH_LEADER, "Ready leader");
    }

    @Override
    void action(SlashCommandInteractionEvent event, String leaderID, Game activeGame, Player player) {
        Leader playerLeader = player.getLeader(leaderID).orElse(null);
        if (playerLeader != null) {
            if (playerLeader.isLocked()) {
                sendMessage("Leader is locked");
                return;
            }
            int tgCount = playerLeader.getTgCount();
            refreshLeader(player, playerLeader, activeGame);
            sendMessage(Emojis.getFactionLeaderEmoji(playerLeader));
            StringBuilder message = new StringBuilder(player.getRepresentation())
                .append(" readied ")
                .append(Helper.getLeaderShortRepresentation(playerLeader));
            if (tgCount > 0) {
                message.append(" - ").append(tgCount).append(Emojis.getTGorNomadCoinEmoji(activeGame)).append(" transferred from leader to player");
                
            }
            String msg = message.toString();
            sendMessage(msg);
        } else {
            sendMessage("Leader not found");
        }
    }

    public static void refreshLeader(Player player, Leader playerLeader, Game activeGame) {
        int tgCount = playerLeader.getTgCount();
        playerLeader.setExhausted(false);
        if (tgCount > 0) {
            int tg = player.getTg();
            tg += tgCount;
            player.setTg(tg);
            MessageHelper.sendMessageToChannel(ButtonHelper.getCorrectChannel(player, activeGame),
                ButtonHelper.getTrueIdentity(player, activeGame) + " you gained " + tgCount + " tgs (" + (tg - tgCount) + "->" + tg + ") from " + playerLeader.getId() + " being readied");
            ButtonHelperAbilities.pillageCheck(player, activeGame);
            playerLeader.setTgCount(0);
        }
    }
}
