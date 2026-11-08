package codes.blitz.game.generated;


/**
 * A dinosaur on the map.
 *
 * @param id Unique identifier for this dinosaur. This identifier remains unchanged across turns.
 * @param position Current world position of the dinosaur.
 * @param age Number of turns since this dinosaur spawned. Use this value with the decay curve to
 *     calculate the points awarded for eliminating it.
 * @param name Name of this dinosaur. Dinosaurs sharing a name always behave the same way. See the
 *     documentation for details.
 */
public record Dinosaur(int id, WorldPosition position, int age, String name) {}
