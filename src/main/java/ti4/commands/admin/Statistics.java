package ti4.commands.admin;

import java.util.Map;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import ti4.helpers.Constants;
import ti4.helpers.Helper;
import ti4.map.Game;
import ti4.map.GameManager;
import ti4.map.Player;
import ti4.message.MessageHelper;

import java.util.Date;
import java.util.HashMap;

public class Statistics extends AdminSubcommandData {

    public Statistics() {
        super(Constants.STATISTICS, "Statistics");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Map<String, Integer> factionCount = new HashMap<>();
        Map<String, Integer> winnerFactionCount = new HashMap<>();
        Map<String, Integer> colorCount = new HashMap<>();
        Map<String, Integer> winnerColorCount = new HashMap<>();

        Date currentDate = new Date();
        Map<String, Game> mapList = GameManager.getInstance().getGameNameToGame();
        for (Game activeGame : mapList.values()) {
            if (activeGame.getName().startsWith("pbd")) {
                Date date = new Date(activeGame.getLastModifiedDate());
                long time_difference = currentDate.getTime() - date.getTime();
                long days_difference = (time_difference / (1000 * 60 * 60 * 24)) % 365;

                for (Player player : activeGame.getPlayers().values()) {
                    String color = player.getColor();
                    String faction = player.getFaction();
                    if (faction != null && color != null && !faction.isEmpty() && !"null".equals(faction)) {
                        factionCount.putIfAbsent(faction, 1);
                        factionCount.computeIfPresent(faction, (key, integer) -> integer + 1);

                        colorCount.putIfAbsent(color, 1);
                        colorCount.computeIfPresent(color, (key, integer) -> integer + 1);

                        if (days_difference > 30 && player.getTotalVictoryPoints(activeGame) >= activeGame.getVp()) {
                            winnerFactionCount.putIfAbsent(faction, 1);
                            winnerFactionCount.computeIfPresent(faction, (key, integer) -> integer + 1);

                            winnerColorCount.putIfAbsent(color, 1);
                            winnerColorCount.computeIfPresent(color, (key, integer) -> integer + 1);
                        }
                    }
                }
            }
        }

        sendStatistics(event, factionCount, "Faction played:");
        sendStatisticsColor(event, colorCount, "Color played:");
        sendStatistics(event, winnerFactionCount, "Winning Faction:");
        sendStatisticsColor(event, winnerColorCount, "Winning Color:");
    }

    private static void sendStatistics(SlashCommandInteractionEvent event, Map<String, Integer> factionCount, String text) {
        StringBuilder sb = new StringBuilder();
        sb.append(text).append("\n");
        factionCount.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(entry -> sb.append(Helper.getFactionIconFromDiscord(entry.getKey())).append(" - ").append(entry.getValue()).append("\n"));
        MessageHelper.sendMessageToChannel(event.getMessageChannel(), sb.toString());
    }

    private static void sendStatisticsColor(SlashCommandInteractionEvent event, Map<String, Integer> factionCount, String text) {
        StringBuilder sb = new StringBuilder();
        sb.append(text).append("\n");
        factionCount.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(entry -> sb.append(entry.getKey()).append(" - ").append(entry.getValue()).append("\n"));
        MessageHelper.sendMessageToChannel(event.getMessageChannel(), sb.toString());
    }
}
