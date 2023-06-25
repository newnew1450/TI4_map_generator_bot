package ti4.commands.installation;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.apache.commons.lang3.StringUtils;
import ti4.commands.units.AddRemoveUnits;
import ti4.generator.InstallationHelper;
import ti4.helpers.AliasHandler;
import ti4.helpers.Constants;
import ti4.map.Map;
import ti4.map.Tile;
import ti4.model.Installation;

public class AddInstallation extends InstallationSubcommandData {
    public AddInstallation() {
        super(Constants.ADD_INSTALLATION, "Add an installation to the map");
        addOptions(new OptionData(OptionType.STRING, Constants.TILE_NAME, "Tile to add the installation on").setRequired(true).setAutoComplete(true));
        addOptions(new OptionData(OptionType.STRING, Constants.INSTALLATION_ID, "Installation you want to add").setRequired(true).setAutoComplete(true));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Map activeMap = getActiveMap();

        OptionMapping option = event.getOption(Constants.TILE_NAME);
        String tileOption = option != null ? StringUtils.substringBefore(event.getOption(Constants.TILE_NAME, null, OptionMapping::getAsString).toLowerCase(), " ") : "nombox";
        String tileID = AliasHandler.resolveTile(tileOption);
        Tile tile = AddRemoveUnits.getTile(event, tileID, activeMap);
        if (tile == null)
            return;

        tile.addToken(Constants.INSTALLATION_TOKEN, Constants.SPACE);
        activeMap.addInstallation(tile.getPosition(),
                new Installation(tile.getPosition(),
                        false,
                        InstallationHelper.getAllInstallations().get(event.getOption(Constants.INSTALLATION_ID).getAsString())));
    }
}
