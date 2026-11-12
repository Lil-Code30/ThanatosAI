package codes.blitz.game.bot;

import codes.blitz.game.generated.Dinosaur;
import codes.blitz.game.generated.WorldPosition;
import java.util.Optional;

public final class MeteorPlanner {

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

  private int scoreTarget(TurnView view, WorldPosition target, int radius) {
    int score = 0;
    for (Dinosaur dino : view.dinosaurs()) {
      if (manhattan(dino.position(), target) <= radius) {
        score += Math.max(1, 20 - dino.age());
      }
    }
    return score;
  }

  static int manhattan(WorldPosition a, WorldPosition b) {
    return Math.abs(a.x() - b.x()) + Math.abs(a.y() - b.y());
  }
}
