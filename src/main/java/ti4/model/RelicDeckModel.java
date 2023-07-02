package ti4.model;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class RelicDeckModel {
    public enum RelicDeckOptions {

        POK("relics_pok"),

        BASEGAME("relics_basegame"),

        ABSOL("relics_absol");

        @Getter
        private String deckName;

        RelicDeckOptions(String deckName) {
            this.deckName = deckName;
        }

        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }
    }
    public static RelicDeckOptions getRelicDeckFromString(String deck) {
        Map<String, RelicDeckOptions> allRelicDecks = Arrays.stream(RelicDeckOptions.values())
                .collect(
                        Collectors.toMap(
                                RelicDeckOptions::toString,
                                (relicDeck -> relicDeck)
                        )
                );
        if (allRelicDecks.containsKey(deck.toLowerCase()))
            return allRelicDecks.get(deck.toLowerCase());
        return null;
    }
}
