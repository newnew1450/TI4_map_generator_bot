package ti4.map;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import ti4.ResourceHelper;
import ti4.generator.Mapper;
import ti4.generator.PositionMapper;
import ti4.generator.TileHelper;
import ti4.helpers.Constants;
import ti4.helpers.FoWHelper;
import ti4.helpers.Units.UnitKey;
import ti4.helpers.Units.UnitType;
import ti4.message.BotLogger;
import ti4.model.TileModel;
import ti4.model.UnitModel;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;
import java.util.concurrent.ThreadLocalRandom;

public class Tile {
    private final String tileID;
    private String position;
    private final HashMap<String, UnitHolder> unitHolders = new HashMap<>();

    private final HashMap<Player, Boolean> fog = new HashMap<>();
    private final HashMap<Player, String> fogLabel = new HashMap<>();

    public Tile(@JsonProperty("tileID") String tileID, @JsonProperty("position") String position) {
        this.tileID = tileID;
        this.position = position != null ? position.toLowerCase() : null;
        initPlanetsAndSpace(tileID);
    }

    public Tile(String tileID, String position, Player player, Boolean fog_, String fogLabel_) {
        this.tileID = tileID;
        this.position = position != null ? position.toLowerCase() : null;
        if (player != null) {
            fog.put(player, fog_);
            fogLabel.put(player, fogLabel_);
        }
        initPlanetsAndSpace(tileID);
    }

    public Tile(String tileID, String position, UnitHolder spaceHolder) {
        this.tileID = tileID;
        this.position = position != null ? position.toLowerCase() : null;
        initPlanetsAndSpace(tileID);
        unitHolders.replace(Constants.SPACE, spaceHolder);
    }

    private void initPlanetsAndSpace(String tileID) {
        Space space = new Space(Constants.SPACE, Constants.SPACE_CENTER_POSITION);
        unitHolders.put(Constants.SPACE, space);
        Map<String, Point> tilePlanetPositions = PositionMapper.getTilePlanetPositions(tileID);

        if (Optional.ofNullable(tilePlanetPositions).isPresent())
            tilePlanetPositions.forEach((planetName, position) -> unitHolders.put(planetName, new Planet(planetName, position)));
    }

    @Nullable
    public static String getUnitPath(UnitKey unitID) {
        if (unitID == null) return null;
        String unitPath = ResourceHelper.getInstance().getUnitFile(unitID);
        if (unitPath == null) {
            BotLogger.log("Could not find unit: " + unitID.toString());
            return null;
        }
        return unitPath;
    }

    @Nullable
    public String getCCPath(String ccID) {
        return Mapper.getCCPath(ccID);
    }

    @Nullable
    public String getAttachmentPath(String tokenID) {
        //            LoggerHandler.log("Could not find attachment token: " + tokenID);
        return ResourceHelper.getInstance().getAttachmentFile(tokenID);
    }

    @Nullable
    public String getTokenPath(String tokenID) {
        return Mapper.getTokenPath(tokenID);
    }

    public boolean isSpaceHolderValid(String spaceHolder) {
        return unitHolders.get(spaceHolder) != null;
    }

    public void addUnit(String spaceHolder, UnitKey unitID, Integer count) {
        UnitHolder unitHolder = unitHolders.get(spaceHolder);
        if (unitHolder != null) {
            unitHolder.addUnit(unitID, count);
        }
    }

    public void addUnitDamage(String spaceHolder, UnitKey unitID, @Nullable Integer count) {
        UnitHolder unitHolder = unitHolders.get(spaceHolder);
        if (unitHolder != null && count != null) {
            HashMap<UnitKey, Integer> units = unitHolder.getUnits();
            Integer unitCount = units.get(unitID);
            if (unitCount != null) {
                if (unitCount < count) {
                    count = unitCount;
                }
                unitHolder.addUnitDamage(unitID, count);
            }
        }
    }

    public void addCC(String ccID) {
        UnitHolder unitHolder = unitHolders.get(Constants.SPACE);
        if (unitHolder != null) {
            unitHolder.addCC(ccID);
        }
    }

    public boolean hasCC(String ccID) {
        UnitHolder unitHolder = unitHolders.get(Constants.SPACE);
        if (unitHolder != null) {
            return unitHolder.hasCC(ccID);
        }
        return false;
    }

    public void addControl(String ccID, String spaceHolder) {
        UnitHolder unitHolder = unitHolders.get(spaceHolder);
        if (unitHolder != null) {
            unitHolder.addControl(ccID);
        }
    }

    public void removeControl(String tokenID, String spaceHolder) {
        UnitHolder unitHolder = unitHolders.get(spaceHolder);
        if (unitHolder != null) {
            unitHolder.removeControl(tokenID);
        }
    }

    public void addToken(String tokenID, String spaceHolder) {
        UnitHolder unitHolder = unitHolders.get(spaceHolder);
        if (unitHolder != null) {
            unitHolder.addToken(tokenID);
        }
    }

    public boolean removeToken(String tokenID, String spaceHolder) {
        UnitHolder unitHolder = unitHolders.get(spaceHolder);
        if (unitHolder != null) {
            return unitHolder.removeToken(tokenID);
        }
        return false;
    }

    public void removeCC(String ccID) {
        UnitHolder unitHolder = unitHolders.get(Constants.SPACE);
        if (unitHolder != null) {
            unitHolder.removeCC(ccID);
        }

    }

    public void removeAllCC() {
        UnitHolder unitHolder = unitHolders.get(Constants.SPACE);
        if (unitHolder != null) {
            unitHolder.removeAllCC();
        }
    }

    public void removeUnit(String spaceHolder, UnitKey unitID, Integer count) {
        UnitHolder unitHolder = unitHolders.get(spaceHolder);
        if (unitHolder != null) {
            unitHolder.removeUnit(unitID, count);
        }
    }

    public void removeUnitDamage(String spaceHolder, UnitKey unitID, @Nullable Integer count) {
        UnitHolder unitHolder = unitHolders.get(spaceHolder);
        if (unitHolder != null && count != null) {
            unitHolder.removeUnitDamage(unitID, count);
        }
    }

    public void removeAllUnits(String color) {
        for (UnitHolder unitHolder : unitHolders.values()) {
            unitHolder.removeAllUnits(color);
            unitHolder.removeAllUnitDamage(color);
        }
    }

    public void removeAllUnitDamage(String color) {
        for (UnitHolder unitHolder : unitHolders.values()) {
            unitHolder.removeAllUnitDamage(color);
        }
    }

    public void addUnit(String spaceHolder, UnitKey unitID, String count) {
        try {
            int unitCount = Integer.parseInt(count);
            addUnit(spaceHolder, unitID, unitCount);
        } catch (Exception e) {
            BotLogger.log("Could not parse unit count", e);
        }
    }

    public void addUnitDamage(String spaceHolder, UnitKey unitID, String count) {
        try {
            int unitCount = Integer.parseInt(count);
            addUnitDamage(spaceHolder, unitID, unitCount);
        } catch (Exception e) {
            BotLogger.log("Could not parse unit count", e);
        }
    }

    @JsonIgnore
    public List<Boolean> getHyperlaneData(Integer sourceDirection) {
        List<List<Boolean>> fullHyperlaneData = Mapper.getHyperlaneData(tileID);
        if (fullHyperlaneData.size() == 0) {
            return null;
        } else if (sourceDirection < 0 || sourceDirection > 5) {
            return Collections.emptyList();
        }
        return fullHyperlaneData.get(sourceDirection);
    }

    public String getTileID() {
        return tileID;
    }

    public String getPosition() {
        return position != null ? position.toLowerCase() : null;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    @JsonIgnore
    public String getTilePath() {
        String tileName = Mapper.getTileID(tileID);
        if ((tileID.equals("44") || (tileID.equals("45")))
                && (ThreadLocalRandom.current().nextInt(Constants.EYE_CHANCE) == 0)) {
            tileName = "S15_Cucumber.png";
        }
        String tilePath = ResourceHelper.getInstance().getTileFile(tileName);
        if (tilePath == null) {
            BotLogger.log("Could not find tile: " + tileID);
        }
        return tilePath;
    }

    public boolean hasFog(Player player) {
        Boolean hasFog = fog.get(player);
        //default all tiles to being foggy to prevent unintended info leaks
        return hasFog == null || hasFog;
    }

    public void setTileFog(@NotNull Player player, Boolean fog_) {
        fog.put(player, fog_);
    }

    public String getFogLabel(Player player) {
        return fogLabel.get(player);
    }

    public void setFogLabel(@NotNull Player player, String fogLabel_) {
        fogLabel.put(player, fogLabel_);
    }

    @JsonIgnore
    public String getFowTilePath(Player player) {
        String fogTileColor = player == null ? "default" : player.getFogFilter();
        String fogTileColorSuffix = "_" + fogTileColor;
        String fowTileID = "fow" + fogTileColorSuffix;

        if ("82b".equals(tileID) || "51".equals(tileID)) { //mallice || creuss
            fowTileID = "fowb" + fogTileColorSuffix;
        }
        if ("82a".equals(tileID)) { //mallicelocked
            fowTileID = "fowc" + fogTileColorSuffix;
        }

        String tileName = Mapper.getTileID(fowTileID);
        String tilePath = ResourceHelper.getInstance().getTileFile(tileName);
        if (tilePath == null) {
            BotLogger.log("Could not find tile: " + fowTileID);
        }
        return tilePath;
    }

    public HashMap<String, UnitHolder> getUnitHolders() {
        return unitHolders;
    }

    @JsonIgnore
    public String getRepresentation() {
        try {
            if (Mapper.getTileRepresentations().get(getTileID()) == null){
                return getTileID() + "(" + getPosition()+ ")";
            }
            return Mapper.getTileRepresentations().get(getTileID());
        } catch (Exception e) {
            // DO NOTHING
        }
        return null;
    }

    @JsonIgnore
    public String getRepresentationForButtons(Game activeGame, Player player) {
        try {
            if (activeGame.isFoWMode()) {
                if (player == null) return getPosition();

                Set<String> tilesToShow = FoWHelper.getTilePositionsToShow(activeGame, player);
                if (tilesToShow.contains(getPosition())) {
                    return getPosition() + " (" + getRepresentation() + ")";
                } else {
                    return getPosition();
                }
            } else {
                return getPosition() + " (" + getRepresentation() + ")";
            }

        } catch (Exception e) {
            return getTileID();
        }
    }

    @JsonIgnore
    public String getAutoCompleteName() {
        try {
            return getPosition() + " (" + getRepresentation() + ")";
        } catch (Exception e) {
            return getTileID();
        }
    }

    @JsonIgnore
    public List<String> getPlanetsWithSleeperTokens() {
        List<String> planetsWithSleepers = new ArrayList<>();
        for (UnitHolder unitHolder : getUnitHolders().values()) {
            if (unitHolder instanceof Planet planet) {
                if (planet.getTokenList().contains(Constants.TOKEN_SLEEPER_PNG)) {
                    planetsWithSleepers.add(planet.getName());
                }
            }
        }
        return planetsWithSleepers;
    }

    @JsonIgnore
    public TileModel getTileModel() {
        return TileHelper.getTile(getTileID());
    }

    @JsonIgnore
    public boolean isAsteroidField() {
        return getTileModel().isAsteroidField();
    }

    @JsonIgnore
    public boolean isSupernova() {
        return getTileModel().isSupernova();
    }

    @JsonIgnore
    public boolean isNebula() {
        return getTileModel().isNebula();
    }

    @JsonIgnore
    public boolean isGravityRift() {
        return getTileModel().isGravityRift() || hasCabalSpaceDockOrGravRiftToken();
    }

    @JsonIgnore
    public boolean hasCabalSpaceDockOrGravRiftToken() {
        for (UnitHolder unitHolder : getUnitHolders().values()) {
            Set<String> tokenList = unitHolder.getTokenList();
            if (CollectionUtils.containsAny(tokenList, "token_gravityrift.png")) {
                return true;
            }
            for (UnitKey unit : unitHolder.getUnits().keySet()) {
                if (unit.getUnitType().equals(UnitType.CabalSpacedock)) {
                    return true;
                }
            }
        }
        return false;
    }

    @JsonIgnore
    public boolean isAnomaly() {
        if (isAsteroidField() || isSupernova() || isNebula() || isGravityRift()) {
            return true;
        }
        for (UnitHolder unitHolder : getUnitHolders().values()) {
            if (CollectionUtils.containsAny(unitHolder.getTokenList(), "token_ds_wound.png", "token_ds_sigil.png", "token_anomalydummy.png")) {
                return true;
            }
        }
        return false;
    }

    @JsonIgnore
    public boolean containsPlayersUnits(Player p) {
        return getUnitHolders().values().stream()
            .flatMap(uh -> uh.getUnits().entrySet().stream())
            .anyMatch(e -> e.getValue() > 0 && p.unitBelongsToPlayer(e.getKey()));
    }

    @JsonIgnore
    public boolean containsPlayersUnitsWithModelCondition(Player p, Predicate<? super UnitModel> condition) {
        return getUnitHolders().values().stream()
            .flatMap(uh -> uh.getUnits().entrySet().stream())
            .filter(e -> e.getValue() > 0 && p.unitBelongsToPlayer(e.getKey()))
            .map(Map.Entry::getKey)
            .map(key -> p.getUnitFromUnitKey(key))
            .filter(unit -> unit != null)
            .anyMatch(condition);
    }

    @JsonIgnore
    public boolean containsPlayersUnitsWithKeyCondition(Player p, Predicate<? super UnitKey> condition) {
        return getUnitHolders().values().stream()
            .flatMap(uh -> uh.getUnits().entrySet().stream())
            .filter(e -> e.getValue() > 0 && p.unitBelongsToPlayer(e.getKey()))
            .map(Map.Entry::getKey)
            .filter(x -> x != null)
            .anyMatch(condition);
    }

    @JsonIgnore
    public boolean search(String searchString) {
        return getTileID().contains(searchString) ||
            getPosition().contains(searchString)  ||
            getTileModel().search(searchString);
    }
}
