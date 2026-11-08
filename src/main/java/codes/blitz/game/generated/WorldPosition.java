package codes.blitz.game.generated;


/**
 * A world position on the game map. Use x and y for world coordinates. To convert to tile array
 * indices, use i = y - map.origin.y and j = x - map.origin.x.
 *
 * @param x
 * @param y
 */
public record WorldPosition(int x, int y) {}
