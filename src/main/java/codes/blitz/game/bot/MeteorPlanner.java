package codes.blitz.game.bot;

import codes.blitz.game.generated.Dinosaur;
import codes.blitz.game.generated.WorldPosition;

import java.util.Optional;

public final class MeteorPlanner {

    public Optional<WorldPosition> bestTarget(TurnView view, DinoTracker tracker){
        //
        if(view.meteorsInFlight() >= view.constants().maxMeteors()){
            return Optional.empty();
        }

        int radius = view.constants().meteorRadius();;
        WorldPosition best = null;
        double bestScore = 0;

        for(Dinosaur dino : view.dinosaurs()){

            // cible candidate
            WorldPosition target = dino.position();

            // une cible hors cadre serait rejetée
            if (!view.inFrame(target)){
                continue;
            }

            //
            if (view.isInDanger(target)) {
                continue;
            }

            double score = scoreTarget(view, tracker, target, radius){
                if(score > bestScore){
                    bestScore = score;
                    best = target;
                }
            }
        }

        return Optional.ofNullable(best);
    }


    // helpers
    private double scoreTarget(TurnView view, DinoTracker tracker, WorldPosition target, int radius){
        double score = 0;

        for(Dinosaur dino : view.dinosaurs()){
            // Dinosaure dans la zone d'impact approximée ?
            if(manhattan(dino.position(), target) <= radius){
                double weight = Math.max(1, 20 - dino.age());
                if(tracker.isDormant(dino.id())){
                    weight *= 1.25;
                }

                score += weight;
            }
        }

        return score;
    }

    // Distance de Manhattan : |dx| + |dy|
    static int manhattan(WorldPosition a, WorldPosition b){
        return Math.abs(a.x() - b.x()) + Math.abs(a.y() - b.y());
    }
}
