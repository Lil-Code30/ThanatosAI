package codes.blitz.game.bot;

import codes.blitz.game.generated.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Bot {

  private final MeteorPlanner meteorPlanner = new MeteorPlanner();
  private final VolcanoPlanner volcanoPlanner = new VolcanoPlanner();


  public Bot() {
    System.out.println("Initializing your super mega duper bot");
  }


  /*
   * Here is where the magic happens, for now the moves are not very good. I bet you can do better ;)
   */
  public List<Action> getActions(TeamGameState state) {
    List<Action> actions = new ArrayList<>();

    TurnView view = new TurnView(state);

    Optional<WorldPosition> meteorTarget = meteorPlanner.bestTarget(view);
    if(meteorTarget.isPresent()) {
      // Une météorite a une bonne cible, on la lance
      actions.add(new LaunchMeteorAction(meteorTarget.get()));
    }else{
      // Sinon, on regarde si un volcan peut être utile.
      Optional<WorldPosition> volcanoTarget  = volcanoPlanner.bestMountain(view);
      volcanoTarget.ifPresent(target -> actions.add(new TriggerVolcanoAction(target)));
    }

    // The engine applies only ONE action per turn (actions[0]); extra actions are ignored and
    // reported in lastTickErrors.
    return actions;
  }
}
