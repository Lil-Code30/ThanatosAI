package codes.blitz.game.generated;

import java.util.List;

/**
 * A meteor in flight that has not yet hit its target.
 *
 * @param target World position where the meteor will land.
 * @param turnsUntilImpact Number of turns remaining before impact.
 * @param impactedTiles World positions of all tiles in the meteor's impact area. Dinosaurs
 *     occupying these tiles when the meteor hits are killed.
 */
public record Meteor(
    WorldPosition target, int turnsUntilImpact, List<WorldPosition> impactedTiles) {}
