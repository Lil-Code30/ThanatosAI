package codes.blitz.game.bot;

import codes.blitz.game.generated.Dinosaur;
import codes.blitz.game.generated.WorldPosition;
import java.util.Optional;

public final class MeteorPlanner {

  // Une météorite n'est lancée que si elle touche au moins ce nombre de dinosaures.
  private static final int MIN_DINOS_PER_METEOR = 1;

  /**
   * Choisit la meilleure cible de météorite pour ce tour.
   * Chaque dinosaure vivant dans le cadre sert de cible candidate ; on garde celle qui
   * toucherait le plus de points. Ne renvoie rien si la limite de météorites en vol est atteinte
   * ou si aucune cible valable n'existe.
   *
   * @param view la vue du tour courant
   * @return la position à viser, ou {@link Optional#empty()} si on ne doit pas lancer de météorite
   */
  public Optional<WorldPosition> bestTarget(TurnView view) {
    if (view.meteorsInFlight() >= view.constants().maxMeteors()) {
      return Optional.empty();
    }

    int radius = view.constants().meteorRadius();
    WorldPosition best = null;
    int bestScore = 0;

    for (Dinosaur dino : view.dinosaurs()) {
      WorldPosition target = dino.position();

      if (!view.inFrame(target)) {
        continue;
      }
      if (view.isInDanger(target)) {
        continue;
      }

      int score = scoreTarget(view, target, radius);
      if (score > bestScore) {
        bestScore = score;
        best = target;
      }
    }
    return Optional.ofNullable(best);
  }

  /**
   * Calcule la valeur d'une cible : somme des poids de tous les dinosaures situés
   * dans le rayon d'impact approximé. Un dinosaure jeune pèse plus lourd.
   *
   * @param view la vue du tour courant
   * @param target la position candidate de l'impact
   * @param radius le rayon d'explosion (constante {@code meteorRadius})
   * @return le score de la cible, ou 0 si elle touche moins de {@code MIN_DINOS_PER_METEOR} dinosaures
   */
  private int scoreTarget(TurnView view, WorldPosition target, int radius) {
    int score = 0;
    int covered = 0;
    for (Dinosaur dino : view.dinosaurs()) {
      if (manhattan(dino.position(), target) <= radius) {
        score += Math.max(1, 20 - dino.age());
        covered++;
      }
    }
    if (covered < MIN_DINOS_PER_METEOR) {
      return 0;
    }
    return score;
  }

  /**
   * Calcule la distance de Manhattan entre deux positions : |dx| + |dy|.
   *
   * @param a la première position
   * @param b la seconde position
   * @return la distance entre {@code a} et {@code b}, toujours positive ou nulle
   */
  static int manhattan(WorldPosition a, WorldPosition b) {
    return Math.abs(a.x() - b.x()) + Math.abs(a.y() - b.y());
  }
}
