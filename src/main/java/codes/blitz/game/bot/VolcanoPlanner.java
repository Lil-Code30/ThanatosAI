package codes.blitz.game.bot;

import codes.blitz.game.generated.Dinosaur;
import codes.blitz.game.generated.GameMap;
import codes.blitz.game.generated.Tile;
import codes.blitz.game.generated.WorldPosition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class VolcanoPlanner {

    // Nombre de tours de coulée simulés pour comparer les montagnes.
    private static final int SIM_TURNS = 16;

    /**
     * Choisit la montagne où déclencher un volcan.
     *
     * <p>L'ancienne règle prenait la montagne la plus proche d'un dinosaure. Le problème est
     * qu'un volcan dure jusqu'à ce que sa montagne sorte du cadre, et que la lave coule vers
     * le bas : une montagne en haut du cadre disparaît en quelques tours, tandis qu'une
     * montagne basse alimente une coulée pendant des dizaines de tours. On simule donc la
     * coulée et on privilégie les montagnes qui inondent une grande zone près des
     * dinosaures et qui restent longtemps dans le cadre.
     *
     * @param view la vue du tour courant
     * @return la position de la montagne choisie, ou {@link Optional#empty()} si aucune action n'est utile
     */
    public Optional<WorldPosition> bestMountain(TurnView view){
        if(view.volcanoCount() >= view.constants().maxVolcanoes()){
            return Optional.empty();
        }
        if(view.dinosaurs().isEmpty()){
            return Optional.empty();
        }

        WorldPosition best = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for(WorldPosition mountain : view.state().mountains()){
            if (!view.inFrame(mountain) || view.hasVolcanoAt(mountain)) {
                continue;
            }
            double score = scoreMountain(view, mountain);
            if (score > bestScore) {
                bestScore = score;
                best = mountain;
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * Note une montagne : coulée simulée + durée de vie + proximité des dinosaures.
     */
    private double scoreMountain(TurnView view, WorldPosition mountain){
        GameMap map = view.state().map();
        Map<WorldPosition, Tile> tiles = new HashMap<>();
        for (Tile[] row : map.tiles()) {
            for (Tile tile : row) {
                tiles.put(tile.position(), tile);
            }
        }

        int originY = map.origin().y();
        int lifetime = Math.max(0, (mountain.y() - originY) * view.constants().mapShiftInterval());
        if (lifetime <= 0) {
            return -1;
        }

        Set<WorldPosition> lava = new HashSet<>();
        Set<WorldPosition> frontier = new HashSet<>();
        frontier.add(mountain);

        for (int t = 0; t < SIM_TURNS; t++) {
            Set<WorldPosition> added = new HashSet<>();
            for (WorldPosition source : frontier) {
                Tile sourceTile = tiles.get(source);
                if (sourceTile == null) {
                    continue;
                }
                int lowest = Integer.MAX_VALUE;
                for (WorldPosition n : neighbours(source, map)) {
                    Tile tile = tiles.get(n);
                    if (tile != null && !tile.isImpassable() && !tile.hasLava() && !lava.contains(n)) {
                        lowest = Math.min(lowest, tile.elevation());
                    }
                }
                if (lowest == Integer.MAX_VALUE || lowest > sourceTile.elevation()) {
                    continue;
                }
                for (WorldPosition n : neighbours(source, map)) {
                    Tile tile = tiles.get(n);
                    if (tile != null && !tile.isImpassable() && !tile.hasLava()
                            && !lava.contains(n) && tile.elevation() == lowest) {
                        added.add(n);
                    }
                }
            }
            if (added.isEmpty()) {
                break;
            }
            lava.addAll(added);
            frontier = added;
        }

        double proximity = 0;
        for (Dinosaur dino : view.dinosaurs()) {
            int nearest = Integer.MAX_VALUE;
            for (WorldPosition l : lava) {
                nearest = Math.min(nearest, manhattan(l, dino.position()));
            }
            if (nearest <= 4) {
                proximity += Math.max(1.0, TurnView.ageValue(dino.age())) / (nearest + 1.0);
            }
        }

        return lava.size() * 0.6 + proximity * 5.0 + lifetime * 0.25;
    }

    private static java.util.List<WorldPosition> neighbours(WorldPosition p, GameMap map){
        java.util.List<WorldPosition> result = new java.util.ArrayList<>(4);
        WorldPosition[] candidates = {
                new WorldPosition(p.x() + 1, p.y()),
                new WorldPosition(p.x() - 1, p.y()),
                new WorldPosition(p.x(), p.y() + 1),
                new WorldPosition(p.x(), p.y() - 1)
        };
        for (WorldPosition c : candidates) {
            if (c.x() >= map.origin().x() && c.x() < map.origin().x() + map.width()
                    && c.y() >= map.origin().y() && c.y() < map.origin().y() + map.height()) {
                result.add(c);
            }
        }
        return result;
    }

    private static int manhattan(WorldPosition a, WorldPosition b){
        return Math.abs(a.x() - b.x()) + Math.abs(a.y() - b.y());
    }
}
