package codes.blitz.game.generated;

import java.util.List;

/**
 * The full game state sent to the player each turn.
 *
 * @param tick Current turn number (starts at 1).
 * @param currentTick Same value as tick.
 * @param score Current score.
 * @param lastTickErrors Errors from the previous turn, such as invalid actions.
 * @param constants Game constants for this match.
 * @param map The visible map frame. Only tiles inside the rolling window are exposed.
 * @param dinosaurs All dinosaurs currently alive within the visible frame.
 * @param meteors All meteors currently in flight with targets within the visible frame.
 * @param volcanoes All active volcanoes within the visible frame.
 * @param mountains World positions of all mountain tiles within the visible frame.
 * @param corpses World positions of dinosaur corpses within the visible frame. Each position
 *     identifies a tile where a dinosaur died.
 */
public record TeamGameState(
    int tick,
    int currentTick,
    int score,
    List<String> lastTickErrors,
    Constants constants,
    GameMap map,
    List<Dinosaur> dinosaurs,
    List<Meteor> meteors,
    List<Volcano> volcanoes,
    List<WorldPosition> mountains,
    List<WorldPosition> corpses) {}
