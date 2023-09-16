package ti4.map;

import static java.util.stream.Collectors.toSet;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import ti4.helpers.Constants;
import ti4.helpers.Helper;

@Getter
@Setter
public class GameStats {

  private long timestamp;
  private Map<String, List<String>> objectives;
  private boolean isPoK;
  private int scoreboard;
  private List<PlayerStats> player;
  private String mapString;
  private long setupTimestamp;
  private String speaker;
  private Set<String> laws;
  private boolean isFranken;
  private int round;
  private List<GameStats> history;

  public GameStats(Game game) {
    timestamp = Instant.now().toEpochMilli();
    setupObjectives(game);
    isPoK = !game.isBaseGameMode();
    scoreboard = game.getVp();
    player = new ArrayList<>();
    game.getPlayers().values().forEach(p -> player.add(new PlayerStats(game, p)));
    mapString = Helper.getMapString(game);
    try {
      Date date =  new SimpleDateFormat("yyyy.MM.dd").parse(game.getCreationDate());
      setupTimestamp = date.getTime();
    } catch (ParseException ignored) {}
    speaker = game.getSpeaker();
    //laws = game.getLaws()...//TODO
    isFranken = game.getPlayers().values().stream().findFirst().map(p -> p.getFaction().startsWith("franken")).orElse(false);
    round = game.getRound();
    history = new ArrayList<>();//TODO
  }

  private void setupObjectives(Game game) {//TODO
    objectives = new HashMap<>();
    objectives.put("Secret Objectives", new ArrayList<>(game.getSecretObjectives()));
    objectives.put("Public Objectives I", new ArrayList<>(game.getPublicObjectives1()));
    objectives.put("Public Objectives II", new ArrayList<>(game.getPublicObjectives2()));
    objectives.put("Agenda", new ArrayList<>(game.getAgendas()));//TODO
    objectives.put("Relics", new ArrayList<>(game.getAllRelics()));//TODO
    objectives.put("OTHER", new ArrayList<>());//TODO
  }

  private static class PlayerStats {

    private final Map<String, Integer> commandTokens;
    private final Map<String, String>  leaders;
    private final Set<String> strategyCards;
    private String color;
    private String factionName;
    private List<String> handSummary;
    private int commodities;
    private Map<String, Map<String, Integer>> planetTotals;
    private int custodianPoints;
    private boolean active;
    private int score;
    private Set<String> technologies;
    private Set<String> alliances;
    private Set<String> objectives;
    private int tradeGoods;
    private Set<String> laws;

    public PlayerStats(Game game, Player player) {
      commandTokens = new HashMap<>();
      commandTokens.put("strategy", player.getStrategicCC());
      commandTokens.put("fleet", player.getFleetCC());
      commandTokens.put("tactics", player.getTacticalCC());
      leaders = new HashMap<>();
      leaders.put("hero", player.getLeaders().stream()
          .filter(l -> "hero".equals(l.getType()))
          .map(l -> l.isLocked() ? "locked" : "unlocked")
          .findFirst().orElse(null));
      leaders.put("commander", player.getLeaders().stream()
          .filter(l -> "commander".equals(l.getType()))
          .map(l -> l.isLocked() ? "locked" : "unlocked")
          .findFirst().orElse(null));
      strategyCards = player.getSCs().stream().map(s -> Helper.getSCName(s, game)).collect(toSet());
      color = player.getColor();
      factionName = player.getFaction();
      //handSummary TODO
      commodities = player.getCommodities();
      //planetTotals TODO
      custodianPoints = (int) game.getScoredPublicObjectives().get("0").stream().filter(s -> s.equals(player.getUserID())).count();
      active = !player.isPassed();
      //score = game.get TODO
      //technologies =TODO
      //alliances =TODO
      //objectives =TODO
      tradeGoods = player.getTg();
      //laws = TODO
    }
  }
}
