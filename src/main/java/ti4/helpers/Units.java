package ti4.helpers;

import java.util.concurrent.ThreadLocalRandom;

import lombok.Data;
import lombok.Getter;

public class Units {

    private static String emdash = "—";

    @Data
    public static class UnitKey {
        private UnitType unitType;
        private String colorID;

        public String getColor() {
            return AliasHandler.resolveColor(colorID);
        }

        public String asyncID() {
            return unitType.toString();
        }

        public String unitName() {
            return unitType.plainName();
        }

        public String unitEmoji() {
            return unitType.getUnitTypeEmoji();
        }

        public String getFileName() {
            if (unitType.equals(UnitType.Destroyer) && ThreadLocalRandom.current().nextInt(Constants.EYE_CHANCE) == 0) {
                return String.format("%s_dd_eyes.png", colorID);
            }
            return String.format("%s_%s.png", colorID, asyncID());
        }

        public String getOldUnitID() {
            return String.format("%s_%s.png", colorID, asyncID());
        }

        public String toString() {
            return String.format("%s—%s", colorID, unitType.humanReadableName());
        }

        public String outputForSave() {
            return String.format("%s%s%s", colorID, emdash, asyncID());
        }

        UnitKey(UnitType unitType, String colorID) {
            this.unitType = unitType;
            this.colorID = colorID;
        }
    }

    public enum UnitType {
        Infantry("gf"), Mech("mf"), Pds("pd"), Spacedock("sd"), CabalSpacedock("csd"), // ground based
        Fighter("ff"), Destroyer("dd"), Cruiser("ca"), Carrier("cv"), Dreadnought("dn"), Flagship("fs"), Warsun("ws"), //ships
        PlenaryOrbital("plenaryorbital"), TyrantsLament("tyrantslament"); //relics

        @Getter
        public final String value;

        UnitType(String value) {
            this.value = value;
        }

        public String humanReadableName() {
            return switch (value) {
                case "gf" -> "Infantry";
                case "mf" -> "Mech";
                case "pd" -> "PDS";
                case "sd" -> "Space Dock";
                case "csd" -> "Dimensional Tear";
                case "ff" -> "Fighter";
                case "dd" -> "Destroyer";
                case "ca" -> "Cruiser";
                case "cv" -> "Carrier";
                case "dn" -> "Dreadnought";
                case "fs" -> "Flagship";
                case "ws" -> "War Sun";
                case "plenaryorbital" -> "Plenary Orbital";
                case "tyrantslament" -> "Tyrant's Lament";
                default -> null;
            };
        }

        public String plainName() {
            return switch (value) {
                case "gf" -> "infantry";
                case "mf" -> "mech";
                case "pd" -> "pds";
                case "sd" -> "spacedock";
                case "csd" -> "cabalspacedock";
                case "ff" -> "fighter";
                case "dd" -> "destroyer";
                case "ca" -> "cruiser";
                case "cv" -> "carrier";
                case "dn" -> "dreadnought";
                case "fs" -> "flagship";
                case "ws" -> "warsun";
                case "plenaryorbital" -> null;
                case "tyrantslament" -> null;
                default -> null;
            };
        }

        public String getUnitTypeEmoji() {
            return switch (value) {
                case "gf" -> Emojis.infantry;
                case "mf" -> Emojis.mech;
                case "pd" -> Emojis.pds;
                case "sd", "csd", "plenaryorbital" -> Emojis.spacedock;
                case "ff" -> Emojis.fighter;
                case "dd" -> Emojis.destroyer;
                case "ca" -> Emojis.cruiser;
                case "cv" -> Emojis.carrier;
                case "dn" -> Emojis.dreadnought;
                case "fs", "tyrantslament" -> Emojis.flagship;
                case "ws" -> Emojis.warsun;
                default -> null;
            };
        }

        @Override
        public String toString() {
            return value;
        }
    }

    private static UnitType findUnitType(String unitType) {
        for (UnitType t : UnitType.values()) {
            if (t.value.equalsIgnoreCase(unitType)) return t;
        }
        return null;
    }

    public static UnitKey getUnitKey(String unitType, String colorID) {
        UnitType u = findUnitType(unitType);
        if (colorID == null || u == null) return null;
        return new UnitKey(u, colorID);
    }

    public static UnitKey parseID(String id) {
        if (id.contains(".png")) {
            id = id.replace(".png", "").replace("_", emdash);
        }
        String[] parts = id.split(emdash);
        String colorID = parts[0];
        String unitType = parts[1];
        return getUnitKey(unitType, colorID);
    }
}
