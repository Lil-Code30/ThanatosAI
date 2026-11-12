package codes.blitz.game.bot;

import codes.blitz.game.generated.*;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TurnView {

    private final TeamGameState state;
    private final List<Dinosaur> dinosaurs; // sorted from young to old
    private final Set<WorldPosition> dangerTiles = new HashSet<>(); // position of the tiles that will be touch by a
    private final Set<WorldPosition> volcanoTiles = new HashSet<>(); // position of active volcano

    /**
     * Prépare la vue du tour à partir de l'état reçu du serveur.
     * Calcule une fois : la liste des dinosaures triés par âge, les cases touchées
     * par les météorites en vol, et les positions des volcans actifs.
     *
     * @param state l'état du jeu envoyé par le serveur pour ce tour
     */
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

    /**
     * Renvoie l'état brut du jeu pour ce tour, tel que reçu du serveur.
     *
     * @return l'état complet du tour (score, carte, dinosaures, météorites, volcans...)
     */
    public TeamGameState state(){
        return state;
    }

    /**
     * Renvoie les constantes de la partie (limites, délais, rayons...).
     *
     * @return les constantes fixes de la partie en cours
     */
    public  Constants constants() {
        return state.constants();
    }

    /**
     * Renvoie les dinosaures vivants, triés du plus jeune au plus vieux.
     *
     * @return la liste triée des dinosaures visibles dans le cadre
     */
    public List<Dinosaur> dinosaurs() {
        return dinosaurs;
    }

    /**
     * Compte les météorites actuellement en vol.
     *
     * @return le nombre de météorites qui n'ont pas encore frappé
     */
    // count volcano in air
    public int meteorsInFlight(){
        return state.meteors().size();
    }

    /**
     * Compte les volcans actuellement actifs.
     *
     * @return le nombre de volcans en éruption
     */
    // count active volcano
    public int volcanoCount(){
        return state.volcanoes().size();
    }

    /**
     * Indique si une météorite en vol va frapper cette position.
     *
     * @param position la position monde à tester
     * @return {@code true} si la position fait partie de la zone d'impact d'une météorite en vol
     */
    // determine if a meteor in air will hit this position
    public boolean isInDanger(WorldPosition position){
        return dangerTiles.contains(position);
    }

    /**
     * Indique si un volcan est déjà actif sur cette position.
     *
     * @param position la position monde à tester
     * @return {@code true} si un volcan est déjà présent sur cette case
     */
    // determine if the volcano is already at this position
    public boolean hasVolcanoAt(WorldPosition position){
        return volcanoTiles.contains(position);
    }

    /**
     * Indique si une position se trouve dans le cadre visible. Une action hors du cadre est rejetée par le serveur.
     *
     * @param position la position monde à tester
     * @return {@code true} si x et y sont dans les bornes du cadre (origine comprise, fin exclue)
     */
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

    /**
     * Renvoie la case située à une position monde.
     * Convertit la position en indices de tableau (i = y - origin.y, j = x - origin.x).
     *
     * @param position la position monde de la case recherchée
     * @return la case à cette position, ou {@code null} si la position est hors du cadre
     */
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
