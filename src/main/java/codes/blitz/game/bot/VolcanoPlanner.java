package codes.blitz.game.bot;

import codes.blitz.game.generated.Dinosaur;
import codes.blitz.game.generated.WorldPosition;

import java.util.Optional;

public final class VolcanoPlanner {

    /**
     * Choisit la montagne où déclencher un volcan : celle la plus proche d'un dinosaure.
     * Ne renvoie rien si la limite de volcans actifs est atteinte, s'il n'y a aucun dinosaure,
     * ou si aucune montagne valable n'est dans le cadre.
     *
     * @param view la vue du tour courant
     * @return la position de la montagne choisie, ou {@link Optional#empty()} si aucune action n'est utile
     */
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

    /**
     * Calcule la distance entre une position et le dinosaure le plus proche de celle-ci.
     *
     * @param view la vue du tour courant (contient la liste des dinosaures)
     * @param from la position de départ (par exemple une montagne)
     * @return la plus petite distance de Manhattan entre {@code from} et un dinosaure,
     *         ou {@link Integer#MAX_VALUE} s'il n'y a aucun dinosaure
     */
    // Distance entre une position et le dinosaure le plus proche.
    private int distanceToNearestDinosaur(TurnView view, WorldPosition from){
        int nearest = Integer.MAX_VALUE;

        for(Dinosaur dino : view.dinosaurs()){
            nearest = Math.min(nearest, MeteorPlanner.manhattan(from, dino.position()));
        }

        return nearest;
    }
}
