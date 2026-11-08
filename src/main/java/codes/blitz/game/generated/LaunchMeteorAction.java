package codes.blitz.game.generated;


/**
 * Launch a meteor at the target world position. The meteor hits after meteorDelay turns. At most
 * maxMeteors meteors can be in flight at once.
 *
 * @param target
 */
public record LaunchMeteorAction(ActionType type, WorldPosition target) implements Action {
  public LaunchMeteorAction(WorldPosition target) {
    this(ActionType.LAUNCH_METEOR, target);
  }
}
