package codes.blitz.game.generated;


/**
 * Trigger a volcanic eruption at the world position of a mountain tile. At most maxVolcanoes
 * volcanoes can be active at once.
 *
 * @param target
 */
public record TriggerVolcanoAction(ActionType type, WorldPosition target) implements Action {
  public TriggerVolcanoAction(WorldPosition target) {
    this(ActionType.TRIGGER_VOLCANO, target);
  }
}
