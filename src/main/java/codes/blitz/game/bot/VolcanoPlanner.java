package codes.blitz.game.bot;

import codes.blitz.game.generated.Dinosaur;
import codes.blitz.game.generated.WorldPosition;

import java.util.Optional;

public final class VolcanoPlanner {

    // Renvoie la montagne la plus proche d'un dinosaure, ou rien.
    public Optional<WorldPosition> bestMountain(TurnView view){

        if(view.volcanoCount() >= view.constants().maxVolcanoes()){
            return Optional.empty();
        }

        // Pas de dinosaure : inutile de faire couler de la lave.
        if(view.dinosaurs().isEmpty()){
            return Optional.empty();
        }

        WorldPosition best = null;
        int bestDistance = Integer.MAX_VALUE;

        for(WorldPosition mountain : view.state().mountains()){
            // On ignore les montagnes hors cadre et celles qui ont déjà un volcan.
            if (!view.inFrame(mountain) || view.hasVolcanoAt(mountain)) {
                continue;
            }

            int distance = distanceToNearestDinosaur(view, mountain);

            // On garde la montagne la plus proche des dinosaures.
            if(distance < bestDistance){
                bestDistance = distance;
                best = mountain;
            }
        }

        return Optional.ofNullable(best);
    }

    // Distance entre une position et le dinosaure le plus proche.
    private int distanceToNearestDinosaur(TurnView view, WorldPosition from){
        int nearest = Integer.MAX_VALUE;

        for(Dinosaur dino : view.dinosaurs()){
            nearest = Math.min(nearest, MeteorPlanner.manhattan(from, dino.position()));
        }

        return nearest;
    }
}
