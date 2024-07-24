package de.tobi1craft.repairswapper;

import eu.midnightdust.lib.config.MidnightConfig;

public class RepairSwapperConfig extends MidnightConfig {

    @Entry()
    public static boolean auto = true;

    @Entry()
    public static Hand hand = Hand.OFFHAND;

    @Entry(min = 0, max = 1200)
    public static int delayToReset = 60;

    public enum Hand {
        MAINHAND, OFFHAND
    }
}
