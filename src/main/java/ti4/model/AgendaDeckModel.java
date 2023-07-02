package ti4.model;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class AgendaDeckModel {
    public enum AgendaDeckOptions {

        POK("agendas_pok"),

        BASEGAME("agendas_basegame"),

        ABSOL("agendas_absol");

        @Getter
        private String deckName;

        AgendaDeckOptions(String deckName) {
            this.deckName = deckName;
        }

        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }
    }
    public static AgendaDeckOptions getAgendaDeckFromString(String deck) {
        Map<String, AgendaDeckOptions> allAgendaDecks = Arrays.stream(AgendaDeckOptions.values())
                .collect(
                        Collectors.toMap(
                                AgendaDeckOptions::toString,
                                (agendaDeck -> agendaDeck)
                        )
                );
        if (allAgendaDecks.containsKey(deck.toLowerCase()))
            return allAgendaDecks.get(deck.toLowerCase());
        return null;
    }
}
