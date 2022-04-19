package ti4.map;

import ti4.helpers.Constants;
import ti4.helpers.Helper;
import ti4.helpers.LoggerHandler;
import ti4.helpers.Storage;

import javax.annotation.CheckForNull;
import java.io.*;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class MapSaveLoadManager {

    public static final String TXT = ".txt";
    public static final String TILE = "-tile-";
    public static final String UNITS = "-units-";
    public static final String UNITHOLDER = "-unitholder-";
    public static final String ENDUNITHOLDER = "-endunitholder-";
    public static final String ENDUNITS = "-endunits-";
    public static final String ENDUNITDAMAGE = "-endunitdamage-";
    public static final String UNITDAMAGE = "-unitdamage-";
    public static final String ENDTILE = "-endtile-";
    public static final String TOKENS = "-tokens-";
    public static final String ENDTOKENS = "-endtokens-";
    public static final String PLANET_TOKENS = "-planettokens-";
    public static final String PLANET_ENDTOKENS = "-planetendtokens-";

    public static final String MAPINFO = "-mapinfo-";
    public static final String ENDMAPINFO = "-endmapinfo-";
    public static final String GAMEINFO = "-gameinfo-";
    public static final String ENDGAMEINFO = "-endgameinfo-";
    public static final String PLAYERINFO = "-playerinfo-";
    public static final String ENDPLAYERINFO = "-endplayerinfo-";
    public static final String PLAYER = "-player-";
    public static final String ENDPLAYER = "-endplayer-";

    public static void saveMaps() {
        HashMap<String, Map> mapList = MapManager.getInstance().getMapList();
        for (java.util.Map.Entry<String, Map> mapEntry : mapList.entrySet()) {
            saveMap(mapEntry.getValue());
        }
    }

    public static void saveMap(Map map) {

        File mapFile = Storage.getMapImageStorage(map.getName() + ".txt");
        if (mapFile != null) {
            saveUndo(map, mapFile);
            try (FileWriter writer = new FileWriter(mapFile.getAbsoluteFile())) {
                HashMap<String, Tile> tileMap = map.getTileMap();
                writer.write(map.getOwnerID());
                writer.write(System.lineSeparator());
                writer.write(map.getOwnerName());
                writer.write(System.lineSeparator());
                writer.write(map.getName());
                writer.write(System.lineSeparator());
                saveMapInfo(writer, map);

                for (java.util.Map.Entry<String, Tile> tileEntry : tileMap.entrySet()) {
                    Tile tile = tileEntry.getValue();
                    saveTile(writer, tile);
                }
            } catch (IOException e) {
                LoggerHandler.log("Could not save map: " + map.getName(), e);
            }
        } else {
            LoggerHandler.log("Could not save map, error creating save file");
        }
    }

    public static void undo(Map map) {
        File originalMapFile = Storage.getMapImageStorage(map.getName() + Constants.TXT);
        if (originalMapFile != null) {
            File mapUndoDirectory = Storage.getMapUndoDirectory();
            if (mapUndoDirectory == null) {
                return;
            }
            if (!mapUndoDirectory.exists()) {
                return;
            }

            String mapName = map.getName();
            String mapNameForUndoStart = mapName + "_";
            String[] mapUndoFiles = mapUndoDirectory.list((dir, name) -> name.startsWith(mapNameForUndoStart));
            if (mapUndoFiles != null && mapUndoFiles.length > 0) {
                try {
                    List<Integer> numbers = Arrays.stream(mapUndoFiles)
                            .map(fileName -> fileName.replace(mapNameForUndoStart, ""))
                            .map(fileName -> fileName.replace(Constants.TXT, ""))
                            .map(Integer::parseInt).toList();
                    Integer maxNumber = numbers.isEmpty() ? 0 : numbers.stream().mapToInt(value -> value)
                            .max().orElseThrow(NoSuchElementException::new);
                    File mapUndoStorage = Storage.getMapUndoStorage(mapName + "_" + maxNumber + Constants.TXT);
                    CopyOption[] options = {StandardCopyOption.REPLACE_EXISTING};
                    Files.copy(mapUndoStorage.toPath(), originalMapFile.toPath(), options);
                    mapUndoStorage.delete();
                    Map loadedMap = loadMap(originalMapFile);
                    MapManager.getInstance().deleteMap(map.getName());
                    MapManager.getInstance().addMap(loadedMap);
                } catch (Exception e) {
                    LoggerHandler.log("Error trying to make undo copy for map: " + mapName, e);
                }
            }
        }
    }

    public static void reload(Map map) {
        File originalMapFile = Storage.getMapImageStorage(map.getName() + Constants.TXT);
        if (originalMapFile != null) {
            Map loadedMap = loadMap(originalMapFile);
            MapManager.getInstance().deleteMap(map.getName());
            MapManager.getInstance().addMap(loadedMap);
        }
    }

    private static void saveUndo(Map map, File originalMapFile) {
        File mapUndoDirectory = Storage.getMapUndoDirectory();
        if (mapUndoDirectory == null) {
            return;
        }
        if (!mapUndoDirectory.exists()) {
            mapUndoDirectory.mkdir();
        }

        String mapName = map.getName();
        String mapNameForUndoStart = mapName + "_";
        String[] mapUndoFiles = mapUndoDirectory.list((dir, name) -> name.startsWith(mapNameForUndoStart));
        if (mapUndoFiles != null) {
            try {
                List<Integer> numbers = Arrays.stream(mapUndoFiles)
                        .map(fileName -> fileName.replace(mapNameForUndoStart, ""))
                        .map(fileName -> fileName.replace(Constants.TXT, ""))
                        .map(Integer::parseInt).toList();
                if (numbers.size() == 10) {
                    int minNumber = numbers.stream().mapToInt(value -> value)
                            .min().orElseThrow(NoSuchElementException::new);
                    File mapToDelete = Storage.getMapUndoStorage(mapName + "_" + minNumber + Constants.TXT);
                    mapToDelete.delete();
                }
                Integer maxNumber = numbers.isEmpty() ? 0 : numbers.stream().mapToInt(value -> value)
                        .max().orElseThrow(NoSuchElementException::new);
                maxNumber++;
                File mapUndoStorage = Storage.getMapUndoStorage(mapName + "_" + maxNumber + Constants.TXT);
                CopyOption[] options = {StandardCopyOption.REPLACE_EXISTING};
                Files.copy(originalMapFile.toPath(), mapUndoStorage.toPath(), options);
            } catch (Exception e) {
                LoggerHandler.log("Error trying to make undo copy for map: " + mapName, e);
            }
        }
    }

    private static void saveMapInfo(FileWriter writer, Map map) throws IOException {
        writer.write(MAPINFO);
        writer.write(System.lineSeparator());

        writer.write(map.getMapStatus());
        writer.write(System.lineSeparator());

        writer.write(GAMEINFO);
        writer.write(System.lineSeparator());
        //game information
        writer.write(Constants.SO + " " + String.join(",", map.getSecretObjectives()));
        writer.write(System.lineSeparator());

        writer.write(Constants.AC + " " + String.join(",", map.getActionCards()));
        writer.write(System.lineSeparator());

        writeCards(map.getDiscardActionCards(), writer, Constants.AC_DISCARDED);
        writer.write(Constants.SPEAKER + " " + map.getSpeaker());
        writer.write(System.lineSeparator());

        HashMap<Integer, Boolean> scPlayed = map.getScPlayed();
        StringBuilder sb = new StringBuilder();
        for (java.util.Map.Entry<Integer, Boolean> entry : scPlayed.entrySet()) {
            sb.append(entry.getKey()).append(",").append(entry.getValue()).append(";");
        }
        writer.write(Constants.SC_PLAYED + " " + sb);
        writer.write(System.lineSeparator());

        writer.write(Constants.AGENDAS + " " + String.join(",", map.getAgendas()));
        writer.write(System.lineSeparator());

        writeCards(map.getDiscardAgendas(), writer, Constants.DISCARDED_AGENDAS);
        writeCards(map.getSentAgendas(), writer, Constants.SENT_AGENDAS);
        writeCards(map.getLaws(), writer, Constants.LAW);

        sb = new StringBuilder();
        for (java.util.Map.Entry<String, String> entry : map.getLawsInfo().entrySet()) {
            sb.append(entry.getKey()).append(",").append(entry.getValue()).append(";");
        }
        writer.write(Constants.LAW_INFO + " " + sb);
        writer.write(System.lineSeparator());

        sb = new StringBuilder();
        for (java.util.Map.Entry<Integer, Integer> entry : map.getScTradeGoods().entrySet()) {
            sb.append(entry.getKey()).append(",").append(entry.getValue()).append(";");
        }
        writer.write(Constants.SC_TRADE_GOODS + " " + sb);
        writer.write(System.lineSeparator());

        writeCards(map.getRevealedPublicObjectives(), writer, Constants.REVEALED_PO);
        writeCards(map.getCustomPublicVP(), writer, Constants.CUSTOM_PO_VP);
        writer.write(Constants.PO1 + " " + String.join(",", map.getPublicObjectives1()));
        writer.write(System.lineSeparator());
        writer.write(Constants.PO2 + " " + String.join(",", map.getPublicObjectives2()));
        writer.write(System.lineSeparator());


        StringBuilder sb1 = new StringBuilder();
        for (java.util.Map.Entry<String, List<String>> entry : map.getScoredPublicObjectives().entrySet()) {
            String userIds = String.join("-", entry.getValue());
            sb1.append(entry.getKey()).append(",").append(userIds).append(";");
        }
        writer.write(Constants.SCORED_PO + " " + sb1);
        writer.write(System.lineSeparator());

        writer.write(ENDGAMEINFO);
        writer.write(System.lineSeparator());

        //Player information
        writer.write(PLAYERINFO);
        writer.write(System.lineSeparator());
        HashMap<String, Player> players = map.getPlayers();
        for (java.util.Map.Entry<String, Player> playerEntry : players.entrySet()) {
            writer.write(PLAYER);
            writer.write(System.lineSeparator());

            writer.write(playerEntry.getKey());
            writer.write(System.lineSeparator());
            Player player = playerEntry.getValue();
            writer.write(player.getUserName());
            writer.write(System.lineSeparator());

            writer.write(Constants.FACTION + " " + player.getFaction());
            writer.write(System.lineSeparator());
            writer.write(Constants.COLOR + " " + player.getColor());
            writer.write(System.lineSeparator());

            writer.write(Constants.PASSED + " " + player.isPassed());
            writer.write(System.lineSeparator());

            writeCards(player.getActionCards(), writer, Constants.AC);

            writer.write(Constants.TACTICAL + " " + player.getTacticalCC());
            writer.write(System.lineSeparator());
            writer.write(Constants.FLEET + " " + player.getFleetCC());
            writer.write(System.lineSeparator());
            writer.write(Constants.STRATEGY + " " + player.getStrategicCC());
            writer.write(System.lineSeparator());

            writer.write(Constants.TG + " " + player.getTg());
            writer.write(System.lineSeparator());
            writer.write(Constants.COMMODITIES + " " + player.getCommodities());
            writer.write(System.lineSeparator());
            writer.write(Constants.COMMODITIES_TOTAL + " " + player.getCommoditiesTotal());
            writer.write(System.lineSeparator());
            writer.write(Constants.PN + " " + player.getPn());
            writer.write(System.lineSeparator());


            writer.write(Constants.SO + " " + getSecretList(player.getSecrets()));
            writer.write(System.lineSeparator());
            writer.write(Constants.SO_SCORED + " " + getSecretList(player.getSecretsScored()));
            writer.write(System.lineSeparator());
            writer.write(Constants.CRF + " " + player.getCrf());
            writer.write(System.lineSeparator());
            writer.write(Constants.HRF + " " + player.getHrf());
            writer.write(System.lineSeparator());
            writer.write(Constants.IRF + " " + player.getIrf());
            writer.write(System.lineSeparator());
            writer.write(Constants.VRF + " " + player.getVrf());
            writer.write(System.lineSeparator());
            writer.write(Constants.SC + " " + player.getSC());
            writer.write(System.lineSeparator());

            writer.write(ENDPLAYER);
            writer.write(System.lineSeparator());
        }

        writer.write(ENDPLAYERINFO);
        writer.write(System.lineSeparator());


        writer.write(ENDMAPINFO);
        writer.write(System.lineSeparator());
    }

    private static void writeCards(LinkedHashMap<String, Integer> cardList, FileWriter writer, String saveID) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (java.util.Map.Entry<String, Integer> entry : cardList.entrySet()) {
            sb.append(entry.getKey()).append(",").append(entry.getValue()).append(";");
        }
        writer.write(saveID + " " + sb);
        writer.write(System.lineSeparator());
    }

    private static String getSecretList(LinkedHashMap<String, Integer> secrets) {
        StringBuilder sb = new StringBuilder();
        for (java.util.Map.Entry<String, Integer> so : secrets.entrySet()) {
            sb.append(so.getKey()).append(",").append(so.getValue()).append(";");
        }
        return sb.toString();
    }

    private static void saveTile(Writer writer, Tile tile) throws IOException {
        writer.write(TILE);
        writer.write(System.lineSeparator());
        writer.write(tile.getTileID() + " " + tile.getPosition());
        writer.write(System.lineSeparator());
        HashMap<String, UnitHolder> unitHolders = tile.getUnitHolders();
        writer.write(UNITHOLDER);
        writer.write(System.lineSeparator());
        for (UnitHolder unitHolder : unitHolders.values()) {
            writer.write(UNITS);
            writer.write(System.lineSeparator());
            writer.write(unitHolder.getName());
            writer.write(System.lineSeparator());
            HashMap<String, Integer> units = unitHolder.getUnits();
            for (java.util.Map.Entry<String, Integer> entry : units.entrySet()) {
                writer.write(entry.getKey() + " " + entry.getValue());
                writer.write(System.lineSeparator());
            }
            writer.write(ENDUNITS);
            writer.write(System.lineSeparator());

            writer.write(UNITDAMAGE);
            writer.write(System.lineSeparator());
            HashMap<String, Integer> unitDamage = unitHolder.getUnitDamage();
            for (java.util.Map.Entry<String, Integer> entry : unitDamage.entrySet()) {
                writer.write(entry.getKey() + " " + entry.getValue());
                writer.write(System.lineSeparator());
            }
            writer.write(ENDUNITDAMAGE);
            writer.write(System.lineSeparator());

            writer.write(PLANET_TOKENS);
            writer.write(System.lineSeparator());
            for (String ccID : unitHolder.getCCList()) {
                writer.write(ccID);
                writer.write(System.lineSeparator());
            }

            for (String controlID : unitHolder.getControlList()) {
                writer.write(controlID);
                writer.write(System.lineSeparator());
            }

            for (String tokenID : unitHolder.getTokenList()) {
                writer.write(tokenID);
                writer.write(System.lineSeparator());
            }
            writer.write(PLANET_ENDTOKENS);
            writer.write(System.lineSeparator());
        }
        writer.write(ENDUNITHOLDER);
        writer.write(System.lineSeparator());

        writer.write(TOKENS);
        writer.write(System.lineSeparator());

        writer.write(ENDTOKENS);
        writer.write(System.lineSeparator());
        writer.write(ENDTILE);
        writer.write(System.lineSeparator());
    }

    private static File[] readAllMapFiles() {
        File folder = Storage.getMapImageDirectory();
        if (folder == null) {
            try {
                //noinspection ConstantConditions
                if (folder.createNewFile()) {
                    folder = Storage.getMapImageDirectory();
                }
            } catch (IOException e) {
                LoggerHandler.log("Could not create folder for maps");
            }

        }
        return folder.listFiles();
    }

    private static boolean isTxtExtention(File file) {
        return file.getAbsolutePath().endsWith(TXT);

    }

    public static boolean deleteMap(String mapName) {
        File mapStorage = Storage.getMapStorage(mapName + TXT);
        if (mapStorage == null) {
            return false;
        }
        File deletedMapStorage = Storage.getDeletedMapStorage(mapName + "_" + System.currentTimeMillis() + TXT);
        return mapStorage.renameTo(deletedMapStorage);
    }

    public static void loadMaps() {
        HashMap<String, Map> mapList = new HashMap<>();
        File[] files = readAllMapFiles();
        if (files != null) {
            for (File file : files) {
                if (isTxtExtention(file)) {
                    Map map = loadMap(file);
                    if (map != null) {
                        mapList.put(map.getName(), map);
                    }
                }
            }
        }
        MapManager.getInstance().setMapList(mapList);
    }

    @CheckForNull
    private static Map loadMap(File mapFile) {
        if (mapFile != null) {
            Map map = new Map();
            try (Scanner myReader = new Scanner(mapFile)) {
                map.setOwnerID(myReader.nextLine());
                map.setOwnerName(myReader.nextLine());
                map.setName(myReader.nextLine());
                while (myReader.hasNextLine()) {
                    String data = myReader.nextLine();
                    if (MAPINFO.equals(data)) {
                        continue;
                    }
                    if (ENDMAPINFO.equals(data)) {
                        break;
                    }
                    map.setMapStatus(MapStatus.valueOf(data));

                    while (myReader.hasNextLine()) {
                        data = myReader.nextLine();
                        if (GAMEINFO.equals(data)) {
                            continue;
                        }
                        if (ENDGAMEINFO.equals(data)) {
                            break;
                        }
                        readGameInfo(map, data);
                    }

                    while (myReader.hasNextLine()) {
                        String tmpData = myReader.nextLine();
                        if (PLAYERINFO.equals(tmpData)) {
                            continue;
                        }
                        if (ENDPLAYERINFO.equals(tmpData)) {
                            break;
                        }
                        Player player = null;
                        while (myReader.hasNextLine()) {
                            data = tmpData != null ? tmpData : myReader.nextLine();
                            tmpData = null;
                            if (PLAYER.equals(data)) {

                                player = map.addPlayerLoad(myReader.nextLine(), myReader.nextLine());
                                continue;
                            }
                            if (ENDPLAYER.equals(data)) {
                                break;
                            }
                            readPlayerInfo(player, data);
                        }
                    }
                }
                HashMap<String, Tile> tileMap = new HashMap<>();
                while (myReader.hasNextLine()) {
                    String tileData = myReader.nextLine();
                    if (TILE.equals(tileData)) {
                        continue;
                    }
                    if (ENDTILE.equals(tileData)) {
                        continue;
                    }
                    Tile tile = readTile(tileData);
                    tileMap.put(tile.getPosition(), tile);

                    while (myReader.hasNextLine()) {
                        String tmpData = myReader.nextLine();
                        if (UNITHOLDER.equals(tmpData)) {
                            continue;
                        }
                        if (ENDUNITHOLDER.equals(tmpData)) {
                            break;
                        }
                        String spaceHolder = null;
                        while (myReader.hasNextLine()) {
                            String data = tmpData != null ? tmpData : myReader.nextLine();
                            tmpData = null;
                            if (UNITS.equals(data)) {
                                spaceHolder = myReader.nextLine();
                                if (Constants.MIRAGE.equals(spaceHolder)) {
                                    Helper.addMirageToTile(tile);
                                } else if (!tile.isSpaceHolderValid(spaceHolder)) {
                                    LoggerHandler.log("Not valid space holder detected: " + spaceHolder);
                                }
                                continue;
                            }
                            if (ENDUNITS.equals(data)) {
                                break;
                            }
                            readUnit(tile, data, spaceHolder);
                        }

                        while (myReader.hasNextLine()) {
                            String data = myReader.nextLine();
                            if (UNITDAMAGE.equals(data)) {
                                continue;
                            }
                            if (ENDUNITDAMAGE.equals(data)) {
                                break;
                            }
                            readUnitDamage(tile, data, spaceHolder);
                        }

                        while (myReader.hasNextLine()) {
                            String data = myReader.nextLine();
                            if (PLANET_TOKENS.equals(data)) {
                                continue;
                            }
                            if (PLANET_ENDTOKENS.equals(data)) {
                                break;
                            }
                            readPlanetTokens(tile, data, spaceHolder);
                        }
                    }

                    while (myReader.hasNextLine()) {
                        String data = myReader.nextLine();
                        if (TOKENS.equals(data)) {
                            continue;
                        }
                        if (ENDTOKENS.equals(data)) {
                            break;
                        }
                        readTokens(tile, data);
                    }
                }
                map.setTileMap(tileMap);
            } catch (FileNotFoundException e) {
                LoggerHandler.log("File not found to read map data: " + mapFile.getName(), e);
            }
            return map;
        } else {
            LoggerHandler.log("Could not save map, error creating save file");
        }
        return null;
    }

    private static void readGameInfo(Map map, String data) {
        String[] tokenizer = data.split(" ", 2);
        if (tokenizer.length == 2) {
            String identification = tokenizer[0];
            if (Constants.SO.equals(identification)) {
                map.setSecretObjectives(getCardList(tokenizer[1]));
            } else if (Constants.AC.equals(identification)) {
                map.setActionCards(getCardList(tokenizer[1]));
            } else if (Constants.PO1.equals(identification)) {
                map.setPublicObjectives1(getCardList(tokenizer[1]));
            } else if (Constants.PO2.equals(identification)) {
                map.setPublicObjectives2(getCardList(tokenizer[1]));
            } else if (Constants.REVEALED_PO.equals(identification)) {
                map.setRevealedPublicObjectives(getParsedCards(tokenizer[1]));
            } else if (Constants.CUSTOM_PO_VP.equals(identification)) {
                map.setCustomPublicVP(getParsedCards(tokenizer[1]));
            } else if (Constants.SCORED_PO.equals(identification)) {
                map.setScoredPublicObjectives(getParsedCardsForScoredPO(tokenizer[1]));
            } else if (Constants.AGENDAS.equals(identification)) {
                map.setAgendas(getCardList(tokenizer[1]));
            } else if (Constants.AC_DISCARDED.equals(identification)) {
                map.setDiscardActionCards(getParsedCards(tokenizer[1]));
            } else if (Constants.DISCARDED_AGENDAS.equals(identification)) {
                map.setDiscardAgendas(getParsedCards(tokenizer[1]));
            } else if (Constants.SENT_AGENDAS.equals(identification)) {
                map.setSentAgendas(getParsedCards(tokenizer[1]));
            } else if (Constants.LAW.equals(identification)) {
                map.setLaws(getParsedCards(tokenizer[1]));
            } else if (Constants.LAW_INFO.equals(identification)) {
                StringTokenizer actionCardToken = new StringTokenizer(tokenizer[1], ";");
                LinkedHashMap<String, String> cards = new LinkedHashMap<>();
                while (actionCardToken.hasMoreTokens()) {
                    StringTokenizer cardInfo = new StringTokenizer(actionCardToken.nextToken(), ",");
                    String id = cardInfo.nextToken();
                    String value = cardInfo.nextToken();
                    cards.put(id, value);
                }
                map.setLawsInfo(cards);
            } else if (Constants.SC_TRADE_GOODS.equals(identification)) {
                StringTokenizer scTokenizer = new StringTokenizer(tokenizer[1], ";");
                while (scTokenizer.hasMoreTokens()) {
                    StringTokenizer cardInfo = new StringTokenizer(scTokenizer.nextToken(), ",");
                    Integer id = Integer.parseInt(cardInfo.nextToken());
                    Integer value = Integer.parseInt(cardInfo.nextToken());
                    map.setScTradeGood(id, value);
                }
            } else if (Constants.SPEAKER.equals(identification)) {
                map.setSpeaker(tokenizer[1]);
            } else if (Constants.SC_PLAYED.equals(identification)) {
                StringTokenizer scPlayed = new StringTokenizer(tokenizer[1], ";");
                while (scPlayed.hasMoreTokens()) {
                    StringTokenizer dataInfo = new StringTokenizer(scPlayed.nextToken(), ",");
                    Integer scID = Integer.parseInt(dataInfo.nextToken());
                    Boolean status = Boolean.parseBoolean(dataInfo.nextToken());
                    map.setSCPlayed(scID, status);
                }
            }
        }
    }

    private static ArrayList<String> getCardList(String tokenizer) {
        StringTokenizer cards = new StringTokenizer(tokenizer, ",");
        ArrayList<String> cardList = new ArrayList<>();
        while (cards.hasMoreTokens()) {
            cardList.add(cards.nextToken());
        }
        return cardList;
    }

    private static LinkedHashMap<String, Integer> getParsedCards(String tokenizer) {
        StringTokenizer actionCardToken = new StringTokenizer(tokenizer, ";");
        LinkedHashMap<String, Integer> cards = new LinkedHashMap<>();
        while (actionCardToken.hasMoreTokens()) {
            StringTokenizer cardInfo = new StringTokenizer(actionCardToken.nextToken(), ",");
            String id = cardInfo.nextToken();
            Integer index = Integer.parseInt(cardInfo.nextToken());
            cards.put(id, index);
        }
        return cards;
    }

    private static LinkedHashMap<String, List<String>> getParsedCardsForScoredPO(String tokenizer) {
        StringTokenizer po = new StringTokenizer(tokenizer, ";");
        LinkedHashMap<String, List<String>> scoredPOs = new LinkedHashMap<>();
        while (po.hasMoreTokens()) {
            StringTokenizer poInfo = new StringTokenizer(po.nextToken(), ",");
            String id = poInfo.nextToken();

            StringTokenizer userIDs = new StringTokenizer(poInfo.nextToken(), "-");
            List<String> userIDList = new ArrayList<>();
            while (userIDs.hasMoreTokens()) {
                userIDList.add(userIDs.nextToken());
            }
            scoredPOs.put(id, userIDList);
        }
        return scoredPOs;
    }

    private static void readPlayerInfo(Player player, String data) {
        StringTokenizer tokenizer = new StringTokenizer(data, " ");
        if (tokenizer.countTokens() == 2) {
            data = tokenizer.nextToken();
            switch (data) {
                case Constants.FACTION -> player.setFaction(tokenizer.nextToken());
                case Constants.COLOR -> player.setColor(tokenizer.nextToken());
                case Constants.TACTICAL -> player.setTacticalCC(Integer.parseInt(tokenizer.nextToken()));
                case Constants.FLEET -> player.setFleetCC(Integer.parseInt(tokenizer.nextToken()));
                case Constants.STRATEGY -> player.setStrategicCC(Integer.parseInt(tokenizer.nextToken()));
                case Constants.TG -> player.setTg(Integer.parseInt(tokenizer.nextToken()));
                case Constants.COMMODITIES_TOTAL -> player.setCommoditiesTotal(Integer.parseInt(tokenizer.nextToken()));
                case Constants.COMMODITIES -> player.setCommodities(Integer.parseInt(tokenizer.nextToken()));
                case Constants.AC -> {
                    StringTokenizer actionCardToken = new StringTokenizer(tokenizer.nextToken(), ";");
                    while (actionCardToken.hasMoreTokens()) {
                        StringTokenizer actionCardInfo = new StringTokenizer(actionCardToken.nextToken(), ",");
                        String id = actionCardInfo.nextToken();
                        Integer index = Integer.parseInt(actionCardInfo.nextToken());
                        player.setActionCard(id, index);
                    }
                }
                case Constants.PN -> player.setPn(Integer.parseInt(tokenizer.nextToken()));
                case Constants.SO_SCORED -> {
                    StringTokenizer secrets = new StringTokenizer(tokenizer.nextToken(), ";");
                    while (secrets.hasMoreTokens()) {
                        StringTokenizer secretInfo = new StringTokenizer(secrets.nextToken(), ",");
                        String id = secretInfo.nextToken();
                        Integer index = Integer.parseInt(secretInfo.nextToken());
                        player.setSecretScored(id, index);
                    }
                }
                case Constants.SO -> {
                    StringTokenizer secrets = new StringTokenizer(tokenizer.nextToken(), ";");
                    while (secrets.hasMoreTokens()) {
                        StringTokenizer secretInfo = new StringTokenizer(secrets.nextToken(), ",");
                        String id = secretInfo.nextToken();
                        Integer index = Integer.parseInt(secretInfo.nextToken());
                        player.setSecret(id, index);
                    }
                }
                case Constants.CRF -> player.setCrf(Integer.parseInt(tokenizer.nextToken()));
                case Constants.HRF -> player.setHrf(Integer.parseInt(tokenizer.nextToken()));
                case Constants.IRF -> player.setIrf(Integer.parseInt(tokenizer.nextToken()));
                case Constants.VRF -> player.setVrf(Integer.parseInt(tokenizer.nextToken()));
                case Constants.SC -> player.setSC(Integer.parseInt(tokenizer.nextToken()));
                case Constants.PASSED -> player.setPassed(Boolean.parseBoolean(tokenizer.nextToken()));
            }
        }
    }

    private static Tile readTile(String tileData) {
        StringTokenizer tokenizer = new StringTokenizer(tileData, " ");
        return new Tile(tokenizer.nextToken(), tokenizer.nextToken());
    }

    private static void readUnit(Tile tile, String data, String spaceHolder) {
        StringTokenizer tokenizer = new StringTokenizer(data, " ");
        tile.addUnit(spaceHolder, tokenizer.nextToken(), tokenizer.nextToken());
    }

    private static void readUnitDamage(Tile tile, String data, String spaceHolder) {
        StringTokenizer tokenizer = new StringTokenizer(data, " ");
        tile.addUnitDamage(spaceHolder, tokenizer.nextToken(), tokenizer.nextToken());
    }

    private static void readPlanetTokens(Tile tile, String data, String unitHolderName) {
        StringTokenizer tokenizer = new StringTokenizer(data, " ");
        if (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token.startsWith(Constants.COMMAND)) {
                tile.addCC(token);
            } else if (token.startsWith(Constants.CONTROL)) {
                tile.addControl(token, unitHolderName);
            } else {
                tile.addToken(token, unitHolderName);
            }
        }
    }

    private static void readTokens(Tile tile, String data) {
        StringTokenizer tokenizer = new StringTokenizer(data, " ");
//        tile.setUnit(tokenizer.nextToken(), tokenizer.nextToken());
        //todo implement token read
    }

}
