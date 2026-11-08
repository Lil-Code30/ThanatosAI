package codes.blitz.game.generated;


/**
 * Game constants for this match. These values remain fixed throughout a match but may change
 * between matches as the game is balanced.
 *
 * @param maxTicks Maximum number of turns in the match.
 * @param maxMeteors Maximum number of meteors in flight simultaneously.
 * @param maxVolcanoes Maximum number of active volcanoes simultaneously.
 * @param meteorDelay Number of turns between meteor launch and impact.
 * @param meteorRadius Radius of the meteor's impact area.
 * @param mapShiftInterval Number of turns between successive shifts of the visible map frame. The
 *     frame shifts on every turn whose number is a positive multiple of this value.
 * @param waveCadence Number of turns between successive waves of dinosaurs.
 * @param dinosPerWave Number of dinosaurs in each wave before applying the limit on living
 *     dinosaurs.
 * @param activeDinoCap Maximum number of dinosaurs alive at any time. Waves only spawn enough to
 *     fill remaining slots.
 */
public record Constants(
    int maxTicks,
    int maxMeteors,
    int maxVolcanoes,
    int meteorDelay,
    int meteorRadius,
    int mapShiftInterval,
    int waveCadence,
    int dinosPerWave,
    int activeDinoCap) {}
