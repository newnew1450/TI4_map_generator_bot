package ti4.commands.player;

import java.nio.file.Path;

import ti4.generator.Mapper;
import ti4.helpers.Constants;
import ti4.map.Map;
import ti4.map.Player;
import ti4.map.UnitHolder;

public class PlanetRemove extends PlanetAddRemove {
    public PlanetRemove() {
        super(Constants.PLANET_REMOVE, "Remove Planet");
    }

    @Override
    public void doAction(Player player, String planet, Map map) {
        player.removePlanet(planet);
        UnitHolder unitHolder = map.getPlanetsInfo().get(planet);
        String color = player.getColor();
        if (unitHolder != null && color != null && !"white".equals(color)) {
            String ccID = Mapper.getControlID(color);
            Path ccPath = Mapper.getCCPath(ccID);
            if (ccPath != null) {
                unitHolder.removeControl(ccID);
            }
        }
    }
}
