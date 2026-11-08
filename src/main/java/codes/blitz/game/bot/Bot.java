package codes.blitz.game.bot;

import codes.blitz.game.generated.Action;
import codes.blitz.game.generated.Dinosaur;
import codes.blitz.game.generated.LaunchMeteorAction;
import codes.blitz.game.generated.TeamGameState;
import java.util.ArrayList;
import java.util.List;

public class Bot {
  public Bot() {
    System.out.println("Initializing your super mega duper bot");
  }

  /*
   * Here is where the magic happens, for now the moves are not very good. I bet you can do better ;)
   */
  public List<Action> getActions(TeamGameState gameMessage) {
    List<Action> actions = new ArrayList<>();

    // The engine applies only ONE action per turn (actions[0]); extra actions are ignored and
    // reported in lastTickErrors.
    // Baseline strategy: grab the first dinosaur we can see and drop a meteor right on top of it.
    // This is intentionally simple and far from optimal: it just shows the shape of a working bot.
    if (!gameMessage.dinosaurs().isEmpty()) {
      Dinosaur firstDinosaur = gameMessage.dinosaurs().get(0);
      actions.add(new LaunchMeteorAction(firstDinosaur.position()));
    }

    // You can clearly do better than this. Have fun!!
    return actions;
  }
}
