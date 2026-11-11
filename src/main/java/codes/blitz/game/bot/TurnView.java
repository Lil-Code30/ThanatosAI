package codes.blitz.game.bot;

import codes.blitz.game.generated.*;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// we calculate one time per turn : sorted dinosaurs, danger tiles, volcano
public final class TurnView {

    private final TeamGameState state;
    private final List<Dinosaur> dinosaurs; // sorted from young to old
    private final Set<WorldPosition> dangerTiles = new HashSet<>(); // position of the tiles that will be touch by a
    private final Set<WorldPosition> volcanoTiles = new HashSet<>(); // position of active volcano

    public TurnView(TeamGameState state){

        this.state = state;
        this.dinosaurs = state.dinosaurs().stream()
                .sorted(Comparator.comparing(Dinosaur::age)).toList();

        for(Meteor meteor : state.meteors()){
            dangerTiles.addAll(meteor.impactedTiles());
        }

        // note the positions of the already erupted volcanoes
        for(Volcano volcano : state.volcanoes()){
            volcanoTiles.add(volcano.position());
        }
    }

    public TeamGameState state(){
        return state;
    }

    public  Constants constants() {
        return state.constants();
    }

    public List<Dinosaur> dinosaurs() {
        return dinosaurs;
    }

    // count volcano in air
    public int meteorsInFlight(){
        return state.meteors().size();
    }

    // count active volcano
    public int volcanoCount(){
        return state.volcanoes().size();
    }

    // determine if a meteor in air will hit this position
    public boolean isInDanger(WorldPosition position){
        return dangerTiles.contains(position);
    }

    // determine if the volcano is already at this position
    public boolean hasVolcanoAt(WorldPosition position){
        return volcanoTiles.contains(position);
    }

    // True if the position is in the visible frame (since action outside the frame are rejected)
    public boolean inFrame(WorldPosition position){
        GameMap map = state.map();

        int minX = map.origin().x();
        int minY = map.origin().y();


        return position.x() >= minX
                && position.x() < minX + map.width()
                && position.y() >= minY
                && position.y() < minY + map.height();
    }

    public Tile titleAt(WorldPosition position){
        if(!inFrame(position)){
            return null;
        }

        GameMap map = state.map();

        // world -> array
        // i = y - origin.y (row) and j = x - origin.x (column).
        int i = position.y() - map.origin().y();
        int j = position.x() - map.origin().x();

        // [row][column]
        return map.tiles()[i][j];
    }

}
