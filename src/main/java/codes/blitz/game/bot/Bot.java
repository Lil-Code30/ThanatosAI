package codes.blitz.game.bot;

import codes.blitz.game.generated.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Bot {

  private final MeteorPlanner meteorPlanner = new MeteorPlanner();
  private final VolcanoPlanner volcanoPlanner = new VolcanoPlanner();


  /**
   * Crée le bot et affiche un message de démarrage.
   * Appelé une seule fois par {@code WebsocketClient}, donc les planificateurs
   * sont conservés d'un tour à l'autre.
   */
  public Bot() {
    System.out.println("Initializing your super mega duper bot");
  }


  /**
   * Décide de l'action à jouer pour le tour courant.
   *
   * <p>Un volcan doit être lancé tôt : il coule pendant des dizaines de tours et sa lave
   * reste sur la carte. On le déclenche donc dès qu'il n'y en a plus, sauf quand il ne reste
   * que quelques dinosaures : là, une météorite peut vider la carte et déclencher la vague
   * suivante immédiatement.
   *
   * @param state l'état du jeu reçu du serveur pour ce tour
   * @return la liste des actions à envoyer (vide si rien n'est utile ce tour)
   */
  public List<Action> getActions(TeamGameState state) {
    List<Action> actions = new ArrayList<>();

    TurnView view = new TurnView(state);

    boolean volcanoActive = view.volcanoCount() >= view.constants().maxVolcanoes();
    Optional<WorldPosition> volcanoTarget = volcanoActive
            ? Optional.empty()
            : volcanoPlanner.bestMountain(view);

    // Priorité au volcan tant que la carte n'est pas presque vide. Il vaut mieux perdre
    // un tour de météorite que de laisser mourir la coulée : la lave reste et bloque les
    // dinosaures pendant toute la partie.
    boolean nearlyClear = view.dinosaurs().size() <= 3;
    if (!nearlyClear && !volcanoActive) {
      if (volcanoTarget.isPresent()) {
        actions.add(new TriggerVolcanoAction(volcanoTarget.get()));
        return actions;
      }
    }

    Optional<WorldPosition> meteorTarget = meteorPlanner.bestTarget(view);
    if (meteorTarget.isPresent()) {
      actions.add(new LaunchMeteorAction(meteorTarget.get()));
      return actions;
    }

    // Sinon, on tente le volcan (y compris quand la carte est presque vide et qu'aucune
    // cible de météorite n'a été trouvée).
    if (volcanoTarget.isPresent()) {
      actions.add(new TriggerVolcanoAction(volcanoTarget.get()));
    }

    return actions;
  }
}
