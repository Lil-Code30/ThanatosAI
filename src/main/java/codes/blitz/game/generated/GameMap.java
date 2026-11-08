package codes.blitz.game.generated;


/**
 * The visible game map. Only tiles within the rolling window are exposed. Access tiles using array
 * indices [i][j]. To convert world coordinates to array indices, use i = y - origin.y and j = x -
 * origin.x.
 *
 * @param width Width of the visible frame in tiles. The column index j ranges from 0 to width - 1.
 * @param height Height of the visible frame in tiles. The row index i ranges from 0 to height - 1.
 * @param origin World position of the top-left corner of the visible frame. Use this position to
 *     convert between world coordinates (x, y) and array indices (i, j).
 * @param tiles Grid of tiles in the visible frame. Access tiles using tiles[i][j], where i is the
 *     row index (y-axis) and j is the column index (x-axis).
 */
public record GameMap(int width, int height, WorldPosition origin, Tile[][] tiles) {}
