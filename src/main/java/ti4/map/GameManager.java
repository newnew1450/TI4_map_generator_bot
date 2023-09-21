package ti4.map;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GameManager {

    private static final Map<String, Game> gameNameToGame = new ConcurrentHashMap<>();

    public static Game getGame(String gameName) {
        return gameNameToGame.computeIfAbsent(gameName, k -> GameSaveLoadManager.loadGame(gameName));
    }

    public static Collection<Game> getGames() {
        return gameNameToGame.values();
    }

    public void addGame(Game game) {
        gameNameToGame.put(game.getName(), game);
    }

    public Game deleteGame(String gameName) {
        return gameNameToGame.remove(gameName);
    }
}
