package ti4.commands.player;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import ti4.helpers.Constants;
import ti4.helpers.Helper;
import ti4.map.Map;
import ti4.map.Player;
import ti4.model.Installation;

public abstract class InstallationAddRemove extends PlayerSubcommandData {
    public InstallationAddRemove(@NotNull String name, @NotNull String description) {
        super(name, description);
        addOptions(new OptionData(OptionType.STRING, Constants.TILE_NAME, "Tile where the installation is located").setRequired(true).setAutoComplete(true));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Map activeMap = getActiveMap();
        Player player = activeMap.getPlayer(getUser().getId());
        player = Helper.getGamePlayer(activeMap, player, event, null);
        player = Helper.getPlayer(activeMap, player, event);
        if (player == null) {
            sendMessage("Player could not be found");
            return;
        }

        String tile = event.getOption(Constants.TILE_NAME).getAsString();
        Installation installation = activeMap.getInstallations().get(tile);
    }

    public abstract void doAction(Player player, String tile, Installation installation);
}
