package codes.blitz.game.bot;

import codes.blitz.game.generated.Dinosaur;
import codes.blitz.game.generated.WorldPosition;

import codes.blitz.game.generated.Meteor;
import codes.blitz.game.generated.Tile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class MeteorPlanner {

  // Une météorite n'est lancée que si elle touche au moins ce nombre de dinosaures.
  private static final int MIN_DINOS_PER_METEOR = 1;

  // Bonus accordé quand la carte est presque vide : vider la carte déclenche une nouvelle
  // vague au tour suivant, ce qui vaut bien plus que les points du dernier dinosaure.
  private static final double CLEAR_BONUS = 350.0;

  /**
   * Choisit la meilleure cible de météorite pour ce tour.
   *
   * <p>La différence avec l'ancienne version : on ne vise plus seulement la case d'un
   * dinosaure. On évalue aussi les cases autour de chaque dinosaure, et on donne un poids
   * important aux dinosaures qui n'ont pas de sortie. C'est ce qui compte, car le dinosaure
   * est prévenu du lancement et sort de la zone d'impact en un ou deux tours ; seuls ceux
   * qui sont coincés contre un bord, une montagne ou de la lave restent dedans.
   *
   * @param view la vue du tour courant
   * @return la position à viser, ou {@link Optional#empty()} si on ne doit pas lancer de météorite
   */
  public Optional<WorldPosition> bestTarget(TurnView view) {
    if (view.meteorsInFlight() >= view.constants().maxMeteors()) {
      return Optional.empty();
    }

    int radius = view.constants().meteorRadius();
    boolean nearlyClear = view.dinosaurs().size() <= 3;

    WorldPosition best = null;
    double bestScore = 0;
    WorldPosition pressure = null;
    double pressureScore = -1;
    Set<WorldPosition> tried = new HashSet<>();

    // On essaie la case de chaque dinosaure ET les cases autour : une cible décalée peut
    // couvrir un groupe ou bloquer la sortie d'un dinosaure coincé.
    for (Dinosaur dino : view.dinosaurs()) {
      for (int dx = -radius; dx <= radius; dx++) {
        for (int dy = -radius; dy <= radius; dy++) {
          if (Math.abs(dx) + Math.abs(dy) > radius) {
            continue;
          }
          WorldPosition target = new WorldPosition(dino.position().x() + dx, dino.position().y() + dy);
          if (!view.inFrame(target) || view.isInDanger(target) || !tried.add(target)) {
            continue;
          }

          double score = scoreTarget(view, target, radius);
          // Même si la case ne devrait pas tuer, on garde une cible de pression : une
          // météorite force les dinosaures à bouger et les pousse vers la lave/les bords.
          double pressureHere = pressureScore(view, target, radius);
          if (pressureHere > pressureScore) {
            pressureScore = pressureHere;
            pressure = target;
          }
          if (score <= 0) {
            continue;
          }

          // Quand il ne reste que quelques dinosaures, tuer le dernier vaut une vague
          // entière : on met un gros bonus pour finir la carte.
          if (nearlyClear) {
            score += CLEAR_BONUS;
          }

          if (score > bestScore) {
            bestScore = score;
            best = target;
          }
        }
      }
    }
    if (best == null) {
      // Aucune élimination n'est prévue par la simulation, mais une météorite reste
      // utile : elle déplace les dinosaures et prépare la prochaine cible.
      return Optional.ofNullable(pressure);
    }
    return Optional.of(best);
  }

  /**
   * Valeur d'une cible sans tenir compte de la fuite : sert de cible de pression quand
   * aucune élimination n'est simulée.
   */
  private double pressureScore(TurnView view, WorldPosition target, int radius){
    double score = 0;
    for (Dinosaur dino : view.dinosaurs()) {
      if (manhattan(dino.position(), target) <= radius) {
        score += Math.max(1.0, TurnView.ageValue(dino.age()));
      }
    }
    if (view.dinosaurs().size() <= 3) {
      score += CLEAR_BONUS;
    }
    return score;
  }

  /**
   * Calcule la valeur d'une cible.
   *
   * <p>Pour chaque dinosaure dans la zone d'impact :
   * <ul>
   *   <li>on utilise le vrai barème du jeu (il chute avec l'âge), pas un poids linéaire ;</li>
   *   <li>on multiplie par un facteur de "coincé" : un dinosaure qui a beaucoup de cases de
   *       sortie va fuir, un dinosaure collé à un bord, une montagne ou de la lave beaucoup
   *       moins ;</li>
   *   <li>on applique le bonus de multi-élimination du jeu (1 + 0.5 × (k - 1)).</li>
   * </ul>
   *
   * @param view la vue du tour courant
   * @param target la position candidate de l'impact
   * @param radius le rayon d'explosion (constante {@code meteorRadius})
   * @return le score de la cible, ou 0 si elle touche moins de {@code MIN_DINOS_PER_METEOR} dinosaures
   */
  private double scoreTarget(TurnView view, WorldPosition target, int radius) {
    Set<WorldPosition> blast = blastTiles(target, radius);
    Map<WorldPosition, Integer> safety = safetyField(view, blast);
    List<WorldPosition> sources = dangerSources(view, target);
    int delay = view.constants().meteorDelay();

    double score = 0;
    int covered = 0;
    for (Dinosaur dino : view.dinosaurs()) {
      if (!blast.contains(dino.position())) {
        continue;
      }
      // Le dinosaure est prévenu et va fuir. S'il peut sortir de la zone en quatre
      // déplacements, la météorite ne le touchera pas : inutile de la lancer pour lui.
      if (canEscape(view, dino.position(), blast, delay, safety, sources)) {
        continue;
      }
      double value = Math.max(1.0, TurnView.ageValue(dino.age()));
      if (view.touchesEdge(dino.position())) {
        value *= 1.4;
      }
      score += value;
      covered++;
    }

    if (covered < MIN_DINOS_PER_METEOR) {
      return 0;
    }
    if (covered >= 2) {
      score *= 1.0 + 0.5 * (covered - 1);
    }
    return score;
  }

  /**
   * Cases de la zone d'impact. Le jeu utilise un cercle (dx² + dy² <= r²) ; pour r = 2
   * cela coïncide avec le losange de Manhattan.
   */
  private Set<WorldPosition> blastTiles(WorldPosition target, int radius){
    Set<WorldPosition> blast = new HashSet<>();
    for (int dx = -radius; dx <= radius; dx++) {
      for (int dy = -radius; dy <= radius; dy++) {
        if (dx * dx + dy * dy <= radius * radius) {
          blast.add(new WorldPosition(target.x() + dx, target.y() + dy));
        }
      }
    }
    return blast;
  }

  /**
   * Simule la fuite d'un dinosaure pendant les tours qui précèdent l'impact.
   *
   * <p>On reproduit la priorité du jeu : d'abord une case hors de la zone d'impact, puis
   * la case dont l'impact est le plus lointain, puis celle qui est le plus près d'une case
   * sûre, et enfin celle qui est le plus loin des météorites et de la lave. Un dinosaure
   * coincé entre un bord, une montagne et de la lave ne peut pas sortir en quatre tours :
   * la météorite le tue.
   *
   * @return true si le dinosaure arrive à sortir de la zone avant l'impact
   */
  private boolean canEscape(TurnView view, WorldPosition start, Set<WorldPosition> blast,
                            int steps, Map<WorldPosition, Integer> safety, List<WorldPosition> sources){
    WorldPosition pos = start;
    for (int step = 0; step < steps; step++) {
      List<WorldPosition> adjacent = new ArrayList<>(4);
      for (WorldPosition n : view.neighbours(pos)) {
        Tile tile = view.titleAt(n);
        if (tile != null && !tile.isImpassable() && !tile.hasLava()) {
          adjacent.add(n);
        }
      }
      if (adjacent.isEmpty()) {
        break;
      }
      List<WorldPosition> safe = new ArrayList<>(4);
      for (WorldPosition n : adjacent) {
        if (!blast.contains(n) && !view.isDangerous(n)) {
          safe.add(n);
        }
      }
      List<WorldPosition> candidates = safe.isEmpty() ? adjacent : safe;

      // 1. La case dont l'impact est le plus lointain.
      int maxTime = Integer.MIN_VALUE;
      for (WorldPosition n : candidates) {
        maxTime = Math.max(maxTime, timeToImpact(view, n, blast, step));
      }
      List<WorldPosition> filtered = new ArrayList<>(candidates.size());
      for (WorldPosition n : candidates) {
        if (timeToImpact(view, n, blast, step) == maxTime) {
          filtered.add(n);
        }
      }
      // 2. La plus proche d'une case sûre.
      if (maxTime < Integer.MAX_VALUE) {
        int minSafety = Integer.MAX_VALUE;
        for (WorldPosition n : filtered) {
          minSafety = Math.min(minSafety, safety.getOrDefault(n, Integer.MAX_VALUE));
        }
        List<WorldPosition> next = new ArrayList<>(filtered.size());
        for (WorldPosition n : filtered) {
          if (safety.getOrDefault(n, Integer.MAX_VALUE) == minSafety) {
            next.add(n);
          }
        }
        filtered = next;
      }
      // 3. La plus loin des météorites et de la lave.
      int bestDistance = -1;
      for (WorldPosition n : filtered) {
        bestDistance = Math.max(bestDistance, minDistance2(n, sources));
      }
      WorldPosition chosen = null;
      for (WorldPosition n : filtered) {
        if (minDistance2(n, sources) == bestDistance) {
          if (chosen == null || manhattan(n, start) > manhattan(chosen, start)) {
            chosen = n;
          }
        }
      }
      if (chosen == null) {
        break;
      }
      pos = chosen;
      if (!blast.contains(pos) && !view.isDangerous(pos)) {
        return true;
      }
    }
    return !blast.contains(pos);
  }

  /**
   * Distance à la première case sûre, en se déplaçant sur les cases praticables : c'est
   * le champ de sécurité utilisé par le jeu pour choisir la direction de fuite.
   */
  private Map<WorldPosition, Integer> safetyField(TurnView view, Set<WorldPosition> blast){
    Map<WorldPosition, Integer> distance = new HashMap<>();
    List<WorldPosition> queue = new ArrayList<>();
    for (Tile[] row : view.state().map().tiles()) {
      for (Tile tile : row) {
        WorldPosition p = tile.position();
        if (!tile.isImpassable() && !tile.hasLava() && !view.isDangerous(p) && !blast.contains(p)) {
          distance.put(p, 0);
          queue.add(p);
        }
      }
    }
    for (int i = 0; i < queue.size(); i++) {
      WorldPosition p = queue.get(i);
      int next = distance.get(p) + 1;
      for (WorldPosition n : view.neighbours(p)) {
        Tile tile = view.titleAt(n);
        if (tile == null || tile.isImpassable() || distance.containsKey(n)) {
          continue;
        }
        distance.put(n, next);
        queue.add(n);
      }
    }
    return distance;
  }

  /** Nombre de tours avant que cette case soit touchée (météorite ou lave). */
  private int timeToImpact(TurnView view, WorldPosition p, Set<WorldPosition> blast, int step){
    int best = Integer.MAX_VALUE;
    if (view.hasLava(p)) {
      return 0;
    }
    if (blast.contains(p)) {
      best = Math.max(0, view.constants().meteorDelay() - step);
    }
    for (Meteor meteor : view.state().meteors()) {
      int turns = meteor.turnsUntilImpact() - 1 - step;
      if (turns > 0 && meteor.impactedTiles().contains(p)) {
        best = Math.min(best, turns);
      }
    }
    return best;
  }

  /** Cibles et lave qui repoussent les dinosaures. */
  private List<WorldPosition> dangerSources(TurnView view, WorldPosition target){
    List<WorldPosition> sources = new ArrayList<>();
    sources.add(target);
    for (Meteor meteor : view.state().meteors()) {
      sources.add(meteor.target());
    }
    for (Tile[] row : view.state().map().tiles()) {
      for (Tile tile : row) {
        if (tile.hasLava()) {
          sources.add(tile.position());
        }
      }
    }
    return sources;
  }

  private int minDistance2(WorldPosition p, List<WorldPosition> sources){
    int best = Integer.MAX_VALUE;
    for (WorldPosition s : sources) {
      int dx = p.x() - s.x();
      int dy = p.y() - s.y();
      best = Math.min(best, dx * dx + dy * dy);
    }
    return best;
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
