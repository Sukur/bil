package com.bil.rates.domain;

/** Container equipment types (canonical, ISO-aligned codes). */
public enum Equipment {
    DC20,    // 20' Dry / General Purpose (20'DC, 20'DV, 20'GP)
    DC40,    // 40' Dry (40'DC, 40'DV)
    HC40,    // 40' High Cube (40'HQ, 40'HC)
    RF20,    // 20' Reefer
    RF40,    // 40' Reefer
    HR40,    // 40' High Cube Reefer
    OOG_FR,  // Flat Rack
    OOG_OT,  // Open Top
    OOG_PL,  // Platform
    FR20, FR40, OT20, OT40,  // HMM specific OOG aliases
    OTHER;

    /** Lenient label resolver: handles common ratesheet header variants AND the enum name itself. */
    public static Equipment fromLabel(String raw) {
        if (raw == null) return OTHER;
        String s = raw.trim().toUpperCase().replace("'", "").replace("'", "").replace(" ", "");
        // First try exact enum name match (e.g. "HC40", "DC20")
        try { return Equipment.valueOf(s); } catch (IllegalArgumentException ignored) {}
        return switch (s) {
            case "20DC", "20DV", "20GP", "20ST" -> DC20;
            case "40DC", "40DV", "40GP", "40ST" -> DC40;
            case "40HQ", "40HC", "40HCDV", "40HQDV" -> HC40;
            case "20RF", "20REEFER", "20RE" -> RF20;
            case "40RF", "40REEFER", "40RE" -> RF40;
            case "40HR", "40HRF", "40HCRF", "40HCREEFER" -> HR40;
            case "FR", "FLATRACK", "20FR", "40FR" -> OOG_FR;
            case "OT", "OPENTOP", "20OT", "40OT" -> OOG_OT;
            case "PL", "PLATFORM" -> OOG_PL;
            default -> OTHER;
        };
    }
}

