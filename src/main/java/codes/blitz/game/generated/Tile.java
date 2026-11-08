package codes.blitz.game.generated;


/**
 * A single tile on the visible map.
 *
 * @param position World position of this tile.
 * @param elevation Elevation of this tile.
 * @param isImpassable Whether this tile blocks movement and lava flow (mountains, crevasses).
 * @param isMountain Whether this tile is a mountain and therefore a valid volcano target.
 * @param hasLava Whether this tile is covered in lava.
 */
public record Tile(
    WorldPosition position,
    int elevation,
    boolean isImpassable,
    boolean isMountain,
    boolean hasLava) {}
