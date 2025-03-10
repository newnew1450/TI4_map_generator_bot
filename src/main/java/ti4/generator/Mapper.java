package ti4.generator;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import ti4.ResourceHelper;
import ti4.helpers.AliasHandler;
import ti4.helpers.Units;
import ti4.helpers.Units.UnitKey;
import ti4.map.Game;
import ti4.message.BotLogger;
import ti4.model.*;
import ti4.model.TechnologyModel.TechnologyType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Mapper {
    private static final Properties colors = new Properties();
    private static final Properties decals = new Properties();
    private static final Properties attachment_tokens = new Properties();
    private static final Properties tokens = new Properties();
    private static final Properties special_case = new Properties();
    private static final Properties general = new Properties();
    private static final Properties faction_representation = new Properties();
    private static final Properties miltyDraft = new Properties();
    private static final Properties hyperlaneAdjacencies = new Properties();
    private static final Properties ds_handcards = new Properties();
    
    //TODO: Finish moving all files over from properties to json
    private static final Map<String, DeckModel> decks = new HashMap<>();
    private static final Map<String, ExploreModel> explore = new HashMap<>();
    private static final Map<String, AbilityModel>  abilities = new HashMap<>();
    private static final Map<String, ActionCardModel> actionCards = new HashMap<>();
    private static final Map<String, AgendaModel> agendas = new HashMap<>();
    private static final Map<String, EventModel> events = new HashMap<>();
    private static final Map<String, FactionModel> factions = new HashMap<>();
    private static final Map<String, PublicObjectiveModel> publicObjectives = new HashMap<>();
    private static final Map<String, SecretObjectiveModel> secretObjectives = new HashMap<>();
    private static final Map<String, PromissoryNoteModel> promissoryNotes = new HashMap<>();
    private static final Map<String, RelicModel> relics = new HashMap<>();
    private static final Map<String, TechnologyModel> technologies = new HashMap<>();
    private static final Map<String, UnitModel> units = new HashMap<>();
    private static final Map<String, AttachmentModel> attachments = new HashMap<>();
    private static final Map<String, LeaderModel> leaders = new HashMap<>();

    @Getter
    private static final Map<String, StrategyCardModel> strategyCardSets = new HashMap<>();
    private static final Map<String, CombatModifierModel> combatModifiers = new HashMap<>();
    private static final Map<String, DraftErrataModel> frankenErrata = new HashMap<>();

    public static void init() {
        importJsonObjectsFromFolder("factions", factions, FactionModel.class, "Could not read faction setup file");
        readData("color.properties", colors, "Could not read color name file");
        readData("decals.properties", decals, "Could not read decals name file");
        readData("attachments.properties", attachment_tokens, "Could not read attachment token name file");
        readData("tokens.properties", tokens, "Could not read token name file");
        readData("special_case.properties", special_case, "Could not read token name file");
        readData("general.properties", general, "Could not read general token name file");
        readData("faction_representation.properties", faction_representation, "Could not read faction representation file");
        readData("milty_draft.properties", miltyDraft, "Could not read milty draft file");
        readData("hyperlanes.properties", hyperlaneAdjacencies, "Could not read hyperlanes file");
        readData("DS_handcards.properties", ds_handcards, "Could not read ds_handcards file");
        importJsonObjectsFromFolder("explores", explore, ExploreModel.class, "Could not read explore file");
        importJsonObjectsFromFolder("secret_objectives", secretObjectives, SecretObjectiveModel.class, "Could not read secret objectives file");
        importJsonObjectsFromFolder("abilities", abilities, AbilityModel.class, "Could not read faction abilities file");
        importJsonObjectsFromFolder("action_cards", actionCards, ActionCardModel.class, "Could not read action cards file");
        importJsonObjectsFromFolder("agendas", agendas, AgendaModel.class, "Could not read agendas file");
        importJsonObjectsFromFolder("events", events, EventModel.class, "Could not read events file");
        importJsonObjectsFromFolder("public_objectives", publicObjectives, PublicObjectiveModel.class, "Could not read public objective file");
        importJsonObjectsFromFolder("promissory_notes", promissoryNotes, PromissoryNoteModel.class, "Could not read promissory notes file");
        importJsonObjectsFromFolder("relics", relics, RelicModel.class, "Could not read relic file");
        importJsonObjectsFromFolder("technologies", technologies, TechnologyModel.class, "Could not read technology file");
        importJsonObjectsFromFolder("leaders", leaders, LeaderModel.class, "Could not read leader file");
        importJsonObjectsFromFolder("decks", decks, DeckModel.class, "could not read decks file");
        importJsonObjectsFromFolder("units", units, UnitModel.class, "could not read units file");
        importJsonObjectsFromFolder("attachments", attachments, AttachmentModel.class, "Could not read attachments file");
        importJsonObjectsFromFolder("strategy_card_sets", strategyCardSets, StrategyCardModel.class, "could not read strat cards file");
        importJsonObjectsFromFolder("combat_modifiers", combatModifiers, CombatModifierModel.class, "could not read combat modifiers file");
        importJsonObjectsFromFolder("franken_errata", frankenErrata, DraftErrataModel.class, "Could not read faction setup file");
    }

    private static void readData(String propertyFileName, Properties properties, String s) {
        String propFile = ResourceHelper.getInstance().getDataFile(propertyFileName);
        if (propFile != null) {
            try (InputStream input = new FileInputStream(propFile)) {
                properties.load(input);
            } catch (IOException e) {
                BotLogger.log(s);
            }
        }
    }

    private static <T extends ModelInterface> void importJsonObjectsFromFolder(String jsonFolderName, Map<String, T> objectMap, Class<T> target, String error) {
        String folderPath = ResourceHelper.getInstance().getDataFolder(jsonFolderName);
        objectMap.clear(); // Added to prevent duplicates when running Mapper.init() over and over with *ModelTest classes

        try {
            File folder = new File(folderPath);
            File[] listOfFiles = folder.listFiles();
            for (File file : listOfFiles) {
                if (file.isFile() && file.getName().endsWith(".json")) {
                    importJsonObjects(jsonFolderName + File.separator + file.getName(), objectMap, target, error);
                }
            }
        } catch (Exception e) {
            BotLogger.log(error, e);
        }
    }

    private static <T extends ModelInterface> void importJsonObjects(String jsonFileName, Map<String, T> objectMap, Class<T> target, String error) {
        ObjectMapper objectMapper = new ObjectMapper();
        List<T> allObjects = new ArrayList<>();
        String filePath = ResourceHelper.getInstance().getDataFile(jsonFileName);
        JavaType type = objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, target);

        if (filePath != null) {
            try {
                InputStream input = new FileInputStream(filePath);
                allObjects = objectMapper.readValue(input, type);
            } catch (Exception e) {
                BotLogger.log(error);
                BotLogger.log(e.getMessage());
            }
        }

        List<String> badObjects = new ArrayList<>();
        for (T obj : allObjects) {
            if (objectMap.containsKey(obj.getAlias())) { //duplicate found
                BotLogger.log("Duplicate **" + target.getSimpleName() + "** found: " + obj.getAlias());
            }
            objectMap.put(obj.getAlias(), obj);
            if (!obj.isValid()) {
                badObjects.add(obj.getAlias());
            }
        }
        if (!badObjects.isEmpty())
            BotLogger.log("The following **" + target.getSimpleName() + "** are improperly formatted:\n> "
                + String.join("\n> ", badObjects));
    }

    public static List<String> getColourFactionPromissoryNoteIDs(Game activeGame, String color, String faction) {
        List<String> pnList = new ArrayList<>();
        color = AliasHandler.resolveColor(color);
        if (isColorValid(color) && isFaction(faction)) {
            for (PromissoryNoteModel pn : promissoryNotes.values()) {
                if (pn.getColour().orElse("").equals(color) || pn.getFaction().orElse("").equalsIgnoreCase(faction)) {
                    if (activeGame.isAbsolMode() && pn.getAlias().endsWith("_ps")
                        && !"Absol".equalsIgnoreCase(pn.getSource())) {
                        continue;
                    }
                    if (!activeGame.isAbsolMode() && pn.getAlias().endsWith("_ps")
                        && "Absol".equalsIgnoreCase(pn.getSource())) {
                        continue;
                    }
                    pnList.add(pn.getAlias());
                }
            }
        }
        return pnList;
    }

    public static Map<String, PromissoryNoteModel> getPromissoryNotes() {
        return promissoryNotes;
    }

    public static PromissoryNoteModel getPromissoryNoteByID(String id) {
        return promissoryNotes.get(id);
    }

    public static boolean isValidPromissoryNote(String id) {
        return promissoryNotes.containsKey(id);
    }

    public static List<String> getAllPromissoryNoteIDs() {
        return new ArrayList<>(promissoryNotes.keySet());
    }

    public static Set<String> getDecals() {
        return decals.keySet().stream()
            .filter(decal -> decal instanceof String)
            .map(decal -> (String) decal)
            .sorted()
            .collect(Collectors.toSet());
    }

    public static String getDecalName(String decalID) {
        if (decalID == null || "null".equals(decalID)) return null;
        return decals.getProperty(decalID);
    }

    public static boolean isValidDecalSet(String decalID) {
        if (decalID == null || "null".equals(decalID)) return false;
        return decals.containsKey(decalID);
    }

    public static boolean isColorValid(String color) {
        String property = colors.getProperty(color);
        return property != null && !"null".equals(property);
    }

    public static boolean isFaction(String faction) {
        return factions.containsKey(faction);
    }

    public static String getColorID(String color) {
        return color != null ? colors.getProperty(color) : null;
    }

    public static String getSpecialCaseValues(String id) {
        String property = special_case.getProperty(id);
        return property != null ? property : "";
    }

    public static List<String> getFrontierTileIds() {
        List<String> exclusionList = List.of("Hyperlane", "", "Mallice (Locked)");
        return TileHelper.getAllTiles().values().stream()
            .filter(tileModel -> !exclusionList.contains(tileModel.getNameNullSafe()))
            .filter(tileModel -> tileModel.getPlanetIds().size() == 0)
            .map(TileModel::getId)
            .toList();
    }

    public static String getTileID(String tileID) {
        if(TileHelper.getAllTiles().get(tileID) == null){
            return null;
        }
        return TileHelper.getAllTiles().get(tileID).getImagePath();
    }

    public static List<List<Boolean>> getHyperlaneData(String tileID) {
        String property = hyperlaneAdjacencies.getProperty(tileID);
        if (property == null)
            return Collections.emptyList();

        List<String> directions = Arrays.stream(property.split(";")).toList();
        List<List<Boolean>> data = new ArrayList<>();
        for (String dir : directions) {
            List<String> info = Arrays.stream(dir.split(",")).toList();
            List<Boolean> connections = new ArrayList<>();
            for (String value : info)
                connections.add("1".equals(value));
            data.add(connections);
        }
        return data;
    }

    public static Set<String> getWormholes(String tileID) {
        if (TileHelper.getAllTiles().get(tileID).getWormholes() == null) {
            return null;
        }
        return TileHelper.getAllTiles().get(tileID).getWormholes().stream()
            .filter(Objects::nonNull)
            .map(WormholeModel.Wormhole::toString)
            .collect(Collectors.toSet());
    }

    public static Set<String> getWormholesTiles(String wormholeID) {
        WormholeModel wormholeModel = new WormholeModel();
        WormholeModel.Wormhole wormhole = wormholeModel.getWormholeFromString(wormholeID);
        if (wormhole == null) {
            return new HashSet<>();
        }

        return TileHelper.getAllTiles().values().stream()
            .filter(tileModel -> tileModel.getWormholes() != null && tileModel.getWormholes().contains(wormhole))
            .map(TileModel::getId)
            .collect(Collectors.toSet());
    }

    public static String getFactionFileName(String factionID) {
        return factionID + ".png";
    }

    public static String getGeneralFileName(String id) {
        return general.getProperty(id);
    }

    public static Map<String, UnitModel> getUnits() {
        return units;
    }

    public static List<String> getUnitSources() {
        return units.values().stream().map(unit -> unit.getSource()).distinct().sorted().toList();
    }

    public static UnitModel getUnit(String unitID) {
        return units.get(unitID);
    }

    public static boolean isValidUnit(String unitID) {
        return units.containsKey(unitID);
    }

    public static UnitModel getUnitModelByTechUpgrade(String techID) {
        return units.values().stream()
            .filter(unitModel -> techID.equals(unitModel.getRequiredTechId().orElse("")))
            .findFirst()
            .orElse(null);
    }

    public static Map<String, String> getColorToId() {
        Map<String, String> unitMap = new HashMap<>();
        for (Map.Entry<Object, Object> entry : colors.entrySet()) {
            unitMap.put((String) entry.getValue(), (String) entry.getKey());
        }
        return unitMap;
    }

    public static Map<String, String> getDSHandcards() {
        Map<String, String> cards = new HashMap<>();
        for (Map.Entry<Object, Object> entry : ds_handcards.entrySet()) {
            cards.put((String) entry.getKey(), (String) entry.getValue());
        }
        return cards;
    }

    // public static String getUnitID(String unitID, String color) {
    //     return colors.getProperty(color) + "_" + unitID + ".png";
    // }

    public static UnitKey getUnitKey(String unitID, String colorID) {
        if (!isValidAsyncUnitID(unitID)) return null;
        String actuallyColorID = getColorID(colorID) == null ? colorID : getColorID(colorID);
        return Units.getUnitKey(unitID, actuallyColorID);
    }

    public static boolean isValidAsyncUnitID(String asyncUnitID) {
        return getUnitIDList().contains(asyncUnitID);
    }

    public static Set<String> getUnitIDList() {
        return getUnits().values().stream()
            .map(UnitModel::getAsyncId)
            .sorted()
            .collect(Collectors.toSet());
    }

    public static String getCCID(String color) {
        String property = colors.getProperty(color);
        return "command_" + property + ".png";
    }

    public static String getFleetCCID(String color) {
        String property = colors.getProperty(color);
        return "fleet_" + property + ".png";
    }

    public static String getAttachmentID(String tokenID) {
        return attachment_tokens.getProperty(tokenID);
    }

    public static String getTokenID(String tokenID) {
        return tokens.getProperty(tokenID);
    }

    public static FactionModel getFaction(String factionID) {
        return factions.get(factionID);
    }

    public static String getControlID(String color) {
        String property = colors.getProperty(color);
        return "control_" + property + ".png";
    }

    public static String getSweepID(String color) {
        String property = colors.getProperty(color);
        return "sweep_" + property + ".png";
    }

    public static List<String> getColors() {
        return colors.keySet().stream().filter(color -> color instanceof String)
            .map(color -> (String) color)
            .sorted()
            .collect(Collectors.toList());
    }

    public static List<String> getTokens() {
        return Stream.of(attachment_tokens.keySet(), tokens.keySet()).flatMap(Collection::stream)
            .filter(token -> token instanceof String)
            .map(token -> (String) token)
            .sorted()
            .collect(Collectors.toList());
    }

    public static Map<String, String> getTokensToName() {
        Map<String, String> tokensToName = new HashMap<>();
        for (Map.Entry<Object, Object> attachment : attachment_tokens.entrySet()) {
            String key = (String) attachment.getKey();
            String value = (String) attachment.getValue();
            tokensToName.put(value, key);
        }

        for (Map.Entry<Object, Object> tokens : tokens.entrySet()) {
            String key = (String) tokens.getKey();
            String value = (String) tokens.getValue();
            tokensToName.put(value, key);
        }
        return tokensToName;
    }

    public static SecretObjectiveModel getSecretObjective(String id) {
        id = id.replace("extra1", "");
        id = id.replace("extra2", "");
        return secretObjectives.get(id);
    }

    public static boolean isValidSecretObjective(String id) {
        return secretObjectives.containsKey(id);
    }

    public static ActionCardModel getActionCard(String id) {
        id = id.replace("extra1", "");
        id = id.replace("extra2", "");
        return actionCards.get(id);
    }

    public static boolean isValidActionCard(String id) {
        return actionCards.containsKey(id);
    }

    @Nullable
    public static String getActionCardName(String id) {
        ActionCardModel info = getActionCard(id);
        // if we would break trying to split the note, just return whatever is there
        if (info == null) {
            return "unknown action card, contact developer";
        }
        return info.getName();
    }

    public static String getPromissoryNote(String id, boolean longDisplay) {
        if (longDisplay) {
            return getPromissoryNote(id);
        } else {
            return getShortPromissoryNote(id);
        }
    }

    public static String getPromissoryNote(String id) {
        return promissoryNotes.get(id).getText();
    }

    public static String getShortPromissoryNote(String id) {
        String promStr = promissoryNotes.get(id).getText();
        // if we would break trying to split the note, just return whatever is there
        if ((promStr == null) || !promStr.contains(";")) {
            return promStr;
        }
        return promissoryNotes.get(id).getName() + ";" + promissoryNotes.get(id).getFaction()
            + promissoryNotes.get(id).getColour();
    }

    public static String getPromissoryNoteOwner(String id) {
        if (promissoryNotes.get(id) == null) {
            return "finNullDodger";
        }
        return promissoryNotes.get(id).getOwner();
    }

    public static PublicObjectiveModel getPublicObjective(String id) {
        return publicObjectives.get(id);
    }

    public static boolean isValidPublicObjective(String id) {
        return publicObjectives.containsKey(id);
    }

    public static AgendaModel getAgenda(String id) {
        return agendas.get(id);
    }

    public static EventModel getEvent(String id) {
        return events.get(id);
    }

    public static String getExploreRepresentation(String id) {
        id = id.replace("extra1", "");
        id = id.replace("extra2", "");
        if (explore.get(id) != null) {
            return (String) explore.get(id).getRepresentation();
        }
        id = id.replace("_", "");

        if (explore.get(id) != null) {
            return (String) explore.get(id).getRepresentation();
        } else {
            BotLogger.log("Cannot find explore with ID: " + id);
            return null;
        }
    }

    public static ExploreModel getExplore(String exploreId) {
        exploreId = exploreId.replace("extra1", "");
        exploreId = exploreId.replace("extra2", "");
        return explore.get(exploreId);
    }

    public static RelicModel getRelic(String id) {
        id = id.replace("extra1", "");
        id = id.replace("extra2", "");
        return relics.get(id);
    }

    public static PlanetModel getPlanet(String id) {
        return TileHelper.getAllPlanets().get(id);
    }

    public static AttachmentModel getAttachmentInfo(String id) {
        return attachments.get(id);
    }

    public static List<AttachmentModel> getAttachmentInfoAll() {
        return new ArrayList<>(attachments.values());
    }

    public static String getAgendaForOnly(String id) {
        AgendaModel agenda = agendas.get(id);
        StringBuilder sb = new StringBuilder();
        sb.append(agenda.getName()).append(";");
        sb.append(agenda.getType()).append(";");
        if (agenda.getTarget().contains("For/Against")) {
            sb.append(agenda.getText1());
        } else {
            sb.append(agenda.getTarget()).append(";");
            sb.append(agenda.getText1());
            if (agenda.getText2().length() > 0) {
                sb.append(";").append(agenda.getText2());
            }
        }
        return sb.toString();
    }

    @Nullable
    public static String getAgendaTitle(String id) {
        AgendaModel agendaModel = agendas.get(id);
        if (agendaModel == null) {
            return null;
        }
        return agendaModel.getName().toUpperCase();
    }

    public static String getAgendaTitleNoCap(String id) {
        AgendaModel agendaModel = agendas.get(id);
        if (agendaModel == null) {
            return null;
        }
        return agendaModel.getName();
    }

    public static String getAgendaType(String id) {
        AgendaModel agendaModel = agendas.get(id);
        if (agendaModel == null) {
            return "1";
        }
        return agendaModel.displayElectedFaction() ? "0" : "1";
    }

    @Nullable
    public static String getAgendaText(String id) {
        AgendaModel agendaModel = agendas.get(id);
        if (agendaModel == null) {
            return null;
        }
        return agendaModel.getMapText();
    }

    public static Map<String, SecretObjectiveModel> getSecretObjectives() {
        return new HashMap<>(secretObjectives);
    }

    public static Map<String, String> getPlanetRepresentations() {
        return TileHelper.getAllPlanets().values().stream()
            .collect(Collectors.toMap(PlanetModel::getId, PlanetModel::getNameNullSafe));
    }

    public static Map<String, String> getFactionRepresentations() {
        Map<String, String> factions = new HashMap<>();
        for (Map.Entry<Object, Object> entry : faction_representation.entrySet()) {
            factions.put((String) entry.getKey(), (String) entry.getValue());
        }
        return factions;
    }

    public static HashMap<String, LeaderModel> getLeaders() {
        return new HashMap<>(leaders);
    }

    public static LeaderModel getLeader(String leaderID) {
        return leaders.get(leaderID);
    }

    public static boolean isValidLeader(String leaderID) {
        return leaders.containsKey(leaderID);
    }

    public static Map<String, String> getTileRepresentations() {
        return TileHelper.getAllTiles().values().stream()
            .collect(Collectors.toMap(TileModel::getId, TileModel::getNameNullSafe));
    }

    public static HashMap<String, String> getSecretObjectivesJustNames() {
        HashMap<String, String> soList = new HashMap<>();
        for (Map.Entry<String, SecretObjectiveModel> entry : secretObjectives.entrySet()) {
            soList.put(entry.getKey(), entry.getValue().getName());
        }
        return soList;
    }

    public static HashMap<String, String> getSecretObjectivesJustNamesAndSource() {
        HashMap<String, String> soList = new HashMap<>();
        for (Map.Entry<String, SecretObjectiveModel> entry : secretObjectives.entrySet()) {
            soList.put(entry.getKey(), entry.getValue().getName() + " (" + entry.getValue().getSource() + ")");
        }
        return soList;
    }

    public static HashMap<String, String> getAgendaJustNames() {
        HashMap<String, String> agendaList = new HashMap<>();
        for (AgendaModel agenda : agendas.values()) {
            agendaList.put(agenda.getAlias(), agenda.getName());
        }
        return agendaList;
    }

    public static HashMap<String, String> getAgendaJustNames(Game activeGame) {
        HashMap<String, String> agendaList = new HashMap<>();
        for (AgendaModel agenda : agendas.values()) {
            if (activeGame.isAbsolMode() && agenda.getAlias().contains("absol_")) {
                agendaList.put(agenda.getAlias(), agenda.getName());
            }
            if (!activeGame.isAbsolMode() && !agenda.getAlias().contains("absol_")) {
                agendaList.put(agenda.getAlias(), agenda.getName());
            }

        }
        return agendaList;
    }

    @Nullable
    public static String getCCPath(String ccID) {
        return ResourceHelper.getInstance().getCCFile(ccID);
    }

    @Nullable
    public static String getTokenPath(String tokenID) {
        String tokenPath = ResourceHelper.getInstance().getAttachmentFile(tokenID);
        if (tokenPath == null || !(new File(tokenPath).exists())) {
            tokenPath = ResourceHelper.getInstance().getTokenFile(tokenID);
            if (tokenPath == null) {
                BotLogger.log("Could not find token path: " + tokenID);
                return null;
            }
        }
        return tokenPath;
    }

    public static HashMap<String, ActionCardModel> getActionCards() {
        return new HashMap<>(actionCards);
    }

    public static HashMap<String, ActionCardModel> getActionCards(String extra) {
        HashMap<String, ActionCardModel> acList = new HashMap<>();
        for (Map.Entry<String, ActionCardModel> entry : actionCards.entrySet()) {
            acList.put(entry.getKey() + extra, entry.getValue());
        }
        return acList;
    }

    public static HashMap<String, String> getACJustNames() {
        HashMap<String, String> acNameList = new HashMap<>();
        for (Map.Entry<String, ActionCardModel> entry : actionCards.entrySet()) {
            acNameList.put(entry.getKey(), entry.getValue().getName());
        }
        return acNameList;
    }

    public static TechnologyType getTechType(String id) {
        return technologies.get(id).getType();
    }

    public static Map<String, TechnologyModel> getTechs() {
        return technologies;
    }

    public static TechnologyModel getTech(String id) {
        return technologies.get(id);
    }

    public static boolean isValidTech(String id) {
        return technologies.get(id) != null;
    }

    public static boolean isValidPlanet(String id) {
        return AliasHandler.getPlanetKeyList().contains(id);
    }

    public static HashMap<String, PublicObjectiveModel> getPublicObjectives() {
        return new HashMap<>(publicObjectives);
    }

    public static HashMap<String, String> getPublicObjectivesStage1() {
        return getPublicObjectives(1);
    }

    public static HashMap<String, String> getPublicObjectivesStage2() {
        return getPublicObjectives(2);
    }

    @NotNull
    private static HashMap<String, String> getPublicObjectives(int requiredStage) {
        HashMap<String, String> poList = new HashMap<>();
        for (Map.Entry<String, PublicObjectiveModel> entry : publicObjectives.entrySet()) {
            PublicObjectiveModel po = entry.getValue();
            if (requiredStage == po.getPoints()) {
                poList.put(entry.getKey(), po.getName());
            }
        }
        return poList;
    }

    public static HashMap<String, ExploreModel> getExplores() {
        return new HashMap<>(explore);
    }

    public static boolean isValidExplore(String exploreID) {
        return explore.containsKey(exploreID);
    }

    public static Map<String, RelicModel> getRelics() {
        return new HashMap<>(relics);
    }

    public static boolean isValidRelic(String relicID) {
        return relics.containsKey(relicID);
    }

    public static HashMap<String, AgendaModel> getAgendas() {
        return new HashMap<>(agendas);
    }

    public static boolean isValidAgenda(String agendaID) {
        return getAgendas().containsKey(agendaID);
    }

    public static HashMap<String, EventModel> getEvents() {
        return new HashMap<>(events);
    }

    public static boolean isValidEvent(String eventID) {
        return getEvents().containsKey(eventID);
    }

    public static HashMap<String, DeckModel> getDecks() {
        return new HashMap<>(decks);
    }

    public static DeckModel getDeck(String deckID) {
        return getDecks().get(deckID);
    }

    public static boolean isValidDeck(String deckID) {
        return getDecks().containsKey(deckID);
    }

    public static HashMap<String, CombatModifierModel> getCombatModifiers() {
        return new HashMap<>(combatModifiers);
    }

    public static HashMap<String, AbilityModel> getAbilities() {
        return new HashMap<>(abilities);
    }

    public static boolean isValidAbility(String abilityID) {
        return abilities.containsKey(abilityID);
    }

    public static AbilityModel getAbility(String abilityID) {
        return abilities.get(abilityID);
    }

    public static List<String> getFactionIDs() {
        return factions.keySet().stream()
            .filter(token -> token instanceof String)
            .map(token -> (String) token)
            .sorted()
            .collect(Collectors.toList());
    }

    public static List<FactionModel> getFactions() {
        return factions.values().stream()
            .sorted(Comparator.comparing(FactionModel::getFactionName))
            .collect(Collectors.toList());
    }

    public static Map<String, DraftErrataModel> getFrankenErrata() {
        return frankenErrata;
    }

    public static String getUnitBaseTypeFromAsyncID(String asyncID) {
        return getUnits().values().stream()
            .filter(unitModel -> asyncID.equals(unitModel.getAsyncId()))
            .map(UnitModel::getBaseType)
            .findFirst()
            .orElse(null);
    }
}
