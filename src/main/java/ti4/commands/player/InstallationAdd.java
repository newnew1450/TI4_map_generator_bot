package ti4.commands.player;

import org.jetbrains.annotations.NotNull;
import ti4.helpers.Constants;
import ti4.map.Player;
import ti4.model.Installation;

public class InstallationAdd extends InstallationAddRemove{
    public InstallationAdd() {
        super(Constants.ADD_INSTALLATION, "Take control of an Installation");
    }

    @Override
    public void doAction(Player player, String tile, Installation installation) {
        player.addInstallation(tile, installation);
    }
}
