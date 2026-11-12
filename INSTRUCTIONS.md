# INSTRUCTIONS — Blitz 2027, défi d'inscription (bot Java « ThanatosAI »)

Ce document regroupe les règles du défi, la structure du projet, la façon
d'implémenter la stratégie, les tests locaux, l'envoi sur les serveurs et le
dépannage. C'est la version publique de la documentation : le dossier `docs/`
reste local et est ignoré par Git.

## Sommaire

1. Règles du jeu
2. Architecture du projet
3. Guide d'implémentation de la stratégie
4. Tests locaux
5. Serveurs et inscription
6. Dépannage

---

## 1. Règles du jeu

Ce document résume les règles telles qu'elles sont décrites dans la page
Documentation du défi, et les relie aux types Java du projet. La source de
vérité reste la page Documentation du dashboard : en cas de doute, c'est elle
qui fait foi.

### 1.1 Objectif

- Partie **solo** : un seul bot par équipe, contre le jeu lui-même.
- Durée fixe : **1000 tours**, sans fin anticipée.
- Le score ne fait qu'augmenter, rien ne le retire.
- Le score d'équipe pour l'inscription est le meilleur score d'une partie jouée
  jusqu'au bout sur les serveurs (`timedOut` à `false`).

### 1.2 Boucle de jeu

À chaque tour :

1. Le serveur envoie au bot l'état du jeu (`TeamGameState`).
2. Le bot répond avec une liste d'actions.
3. **Seule la première action est appliquée.** Les suivantes sont ignorées et
   signalées dans `lastTickErrors` au tour suivant.

Une seule action par tour : une météorite **ou** un volcan, jamais les deux.

### 1.3 Actions

| Action | Classe Java | Cible | Effet |
| --- | --- | --- | --- |
| `LAUNCH_METEOR` | `LaunchMeteorAction(WorldPosition target)` | Une position dans le cadre visible | Frappe après `meteorDelay` tours et tue les dinosaures de la zone d'impact |
| `TRIGGER_VOLCANO` | `TriggerVolcanoAction(WorldPosition target)` | Une case de montagne sans volcan | Démarre une coulée de lave immédiate |

Limites et rejets :

- Au plus `maxMeteors` météorites en vol (3 selon la doc).
- Au plus `maxVolcanoes` volcans actifs (1 selon la doc).
- Une action hors du cadre visible est rejetée.
- Un volcan sur une case qui n'est pas une montagne, ou sur une montagne déjà en
  éruption, est rejeté.
- Une action rejetée ou ignorée n'a aucun effet : lisez `lastTickErrors`.

### 1.4 Météorites

- La météorite **prévient les dinosaures dès son lancement** : ils peuvent fuir.
- `meteor.impactedTiles` donne la liste exacte des cases touchées.
- À l'impact : les dinosaures présents dans la zone meurent, et l'élévation de
  chaque case de la zone baisse de 1 (plancher 0).
- Le type de terrain ne change pas. La lave existante n'est pas touchée au tour
  de l'impact.
- Le relief abaissé change le trajet de la lave aux tours suivants.

### 1.5 Volcans et lave

- Un volcan produit de la lave tant que sa montagne reste dans le cadre.
- La lave se propage d'une case par tour vers l'élévation la plus basse.
  Si plusieurs voisines ont la même élévation minimale, elle se divise.
- Le terrain infranchissable (montagnes, crevasses) bloque la lave.
- La lave reste en place jusqu'à sortir du cadre.
- Tout dinosaure touché par la lave meurt.

### 1.6 Dinosaures

Ce sont des unités neutres : vous ne les dirigez pas. Chaque espèce a un
comportement propre, identifié par le champ `Dinosaur.name()`.

| Espèce | Comportement (résumé de la doc) |
| --- | --- |
| `Stegosaurus` | Marcheur lent et prévisible. Avance vers une cible en avant du cadre. Fuit les zones d'explosion. |
| `Velociraptor` | Charognard. Immobile en sécurité, esquive avec un mouvement minimal, va vers le cadavre le plus proche. |
| `Triceratops` | Se déplace en meute vers les hauteurs favorables. |
| `Tyrannosaurus` | Prédateur. Évite les autres, mange les dinosaures au contact (la proie ne rapporte rien). |

Les espèces sont listées du plus facile au plus difficile. Un Pterodactyl
apparaît aussi d'après le texte de la page (voir 1.9).

Règles de déplacement :

- Deux dinosaures ne peuvent pas occuper la même case. En cas de conflit, l'un
  reste sur place. Les échanges de position sont permis.
- Le terrain infranchissable bloque le mouvement.

### 1.7 Cadre mobile

- Le cadre visible (`map`) se déplace vers les `y` croissants.
- Il se décale tous les `mapShiftInterval` tours (5 selon la doc).
- La rangée du haut disparaît, une nouvelle apparaît en bas.
- Un dinosaure qui sort du cadre meurt immédiatement et rapporte des points
  selon son âge à ce moment-là.
- Les cadavres et la lave de la rangée qui disparaît sont supprimés.

### 1.8 Vagues

- Une vague apparaît toutes les `waveCadence` tours (100 selon la doc), ou quand
  la carte est vidée de ses dinosaures (la vague suivante arrive un tour plus tard).
- Chaque vague compte `dinosPerWave` dinosaures, sans dépasser `activeDinoCap`
  vivants (20 selon la doc).

### 1.9 Score

- Un dinosaure rapporte d'autant plus qu'il meurt jeune (`Dinosaur.age()`).
  La valeur chute vite au début, puis plus lentement.
- **Multi-éliminations** : si `k` dinosaures meurent au même tour, le score de ce
  tour est multiplié par `1 + 0.5 × (k − 1)`. Pas de bonus pour `k < 2`.
- Un dinosaure dévoré par un Tyrannosaurus ne rapporte rien et ne compte pas
  dans le bonus.

#### Points à vérifier avant de compter dessus

Le texte de la page contient aussi des affirmations qui **ne figurent pas dans
les types générés** et qui ne sont pas confirmées par la documentation officielle
structurée. Vérifiez-les dans le launcher ou sur une partie rejouée avant de
construire une stratégie dessus :

- **Cascade sismique** : une météorite tombant sur une case déjà abaissée dans
  les 5 derniers tours élargit son rayon de 1,7 case. Non exposé par l'API.
- **Pterodactyl** : aucune valeur `ANSWER_PTERODACTYL` n'existe dans
  `ActionType` du starter kit. Ne rien coder pour cette espèce tant que le
  dashboard ne la documente pas et que le type n'apparaît pas dans `generated/`.
- **Coordonnées hexagonales** : le texte décrit une grille hexagonale axiale.
  Le code généré, lui, convertit les positions avec `i = y - origin.y` et
  `j = x - origin.x`. Suivez le code généré et la doc officielle pour les
  conversions.
- Les consignes de stratégie et les chiffres (83 %, 37°, 6,2 cases, etc.)
  n'ont pas de source vérifiée : les traiter comme des hypothèses à tester.

### 1.10 Constantes

Les valeurs sont lues à chaque partie dans `TeamGameState.constants()`. Ne pas
les coder en dur ; elles peuvent être rééquilibrées.

| Champ Java | Signification |
| --- | --- |
| `maxTicks` | Nombre de tours de la partie |
| `maxMeteors` | Météorites en vol simultanément |
| `maxVolcanoes` | Volcans actifs simultanément |
| `meteorDelay` | Tours entre le lancement et l'impact |
| `meteorRadius` | Rayon de la zone d'impact |
| `mapShiftInterval` | Tours entre deux décalages du cadre |
| `waveCadence` | Tours entre deux vagues |
| `dinosPerWave` | Dinosaures par vague, avant plafond |
| `activeDinoCap` | Dinosaures vivants maximum |
## 2. Architecture du projet

### 2.1 Arborescence

```
ThanatosAI/
├── pom.xml
├── AGENTS.md                                   # consignes pour les agents IA
├── src/main/java/codes/blitz/game/
│   ├── Application.java                        # ❌ point d'entrée, connexion ws://127.0.0.1:8765
│   ├── bot/
│   │   └── Bot.java                            # ✅ stratégie (à modifier)
│   ├── generated/                              # ❌ types générés par le starter kit
│   │   ├── TeamGameState.java                  # état reçu à chaque tour
│   │   ├── Constants.java                      # constantes de la partie
│   │   ├── GameMap.java / Tile.java            # carte visible
│   │   ├── Dinosaur.java / Meteor.java / Volcano.java
│   │   ├── WorldPosition.java                  # coordonnées (x, y)
│   │   ├── Action.java / ActionType.java       # actions possibles
│   │   ├── LaunchMeteorAction.java
│   │   └── TriggerVolcanoAction.java
│   ├── message/                                # ❌ sérialisation JSON du protocole
│   │   ├── MessageType.java
│   │   ├── bot/BotCommandMessage.java          # message envoyé au serveur
│   │   ├── bot/BotRegistrationWith*.java       # enregistrement (token ou non)
│   │   └── serialization/                      # encodeurs/décodeurs Jackson
│   └── websocket/
│       └── WebsocketClient.java                # ❌ boucle de réception/envoi
```

Le dossier `target/` est créé par `mvn package` ; il ne doit jamais être
envoyé sur la plateforme.

### 2.2 Flux d'exécution

```
Application.main
  └─ ClientManager.connectToServer(ws://127.0.0.1:8765)
       └─ WebsocketClient.onOpen
            └─ envoie BotRegistrationWithToken (si variable TOKEN)
               ou BotRegistrationWithoutToken("myJavaBot")
       └─ WebsocketClient.processMessageFromServer (à chaque tour)
            ├─ affiche les erreurs lastTickErrors
            ├─ bot.getActions(TeamGameState)     ← VOTRE CODE
            └─ envoie BotCommandMessage(tick, actions)
       └─ WebsocketClient.onClose → latch.countDown() → fin du programme
```

Points importants :

- `WebsocketClient` crée **une seule instance** de `Bot` (dans son
  constructeur). L'état que vous gardez dans les champs de `Bot` survit donc
  d'un tour à l'autre : c'est là que vit l'historique
  des impacts.
- `getActions` est appelé dans un `try/catch` : une exception n'arrête pas le
  bot, mais le tour part **sans action**. Lisez la sortie du terminal pour
  repérer les exceptions (`Exception while getting next moves:`).
- Le message envoyé contient `tick`, la valeur reçue de `TeamGameState.tick()`.
  Ne modifiez pas cette correspondance.

### 2.3 Ce que vous modifiez

| Fichier | Rôle | Modifier ? |
| --- | --- | --- |
| `bot/Bot.java` | Stratégie | **Oui** |
| Nouveaux fichiers dans `bot/` | Découpage de la stratégie (ex. `Planner.java`) | **Oui** |
| `pom.xml` | Dépendances | Seulement si nécessaire (voir 2.4) |
| `generated/*` | Types | Non |
| `message/*`, `websocket/*`, `Application.java` | Connexion | Non |

### 2.4 Dépendances

Définies dans `pom.xml` :

| Dépendance | Rôle |
| --- | --- |
| `tyrus-client`, `tyrus-container-grizzly-client` | Client WebSocket (Jakarta) |
| `jackson-databind` 2.20.0 | Sérialisation JSON |

Le plugin `maven-assembly-plugin` produit `target/bot.jar`, un jar autonome
(`jar-with-dependencies`) dont la classe principale est
`codes.blitz.game.Application`.

Ajouter une dépendance (par exemple une librairie de collections) est possible,
mais elle doit figurer dans `pom.xml`, sinon le serveur ne pourra pas construire
le bot. Préférez la bibliothèque standard (`java.util.*`) quand elle suffit.

### 2.5 Types de données importants

Tous sont des `record` immuables.

| Type | Champs utiles |
| --- | --- |
| `TeamGameState` | `tick()`, `score()`, `lastTickErrors()`, `constants()`, `map()`, `dinosaurs()`, `meteors()`, `volcanoes()`, `mountains()`, `corpses()` |
| `GameMap` | `width()`, `height()`, `origin()`, `tiles()` (`Tile[][]`, indexé `[i][j]`) |
| `Tile` | `position()`, `elevation()`, `isImpassable()`, `isMountain()`, `hasLava()` |
| `Dinosaur` | `id()`, `position()`, `age()`, `name()` |
| `Meteor` | `target()`, `turnsUntilImpact()`, `impactedTiles()` |
| `Volcano` | `position()` |
| `WorldPosition` | `x()`, `y()` |
| `Constants` | voir 01-regles-du-jeu.md |

Conversion monde → tableau, selon le code généré :

```java
int i = pos.y() - map.origin().y();   // ligne
int j = pos.x() - map.origin().x();   // colonne
Tile tile = map.tiles()[i][j];
```

Vérifiez toujours que `i` est dans `[0, height)` et `j` dans `[0, width)` avant
d'accéder à la case.

### 2.6 Remarque sur `Action`

`Action` est une interface avec `type()`. Le bot renvoie une `List<Action>`,
mais une seule action est appliquée par tour (la première). Vous pouvez donc
renvoyer une liste vide pour « ne rien faire » ce tour-ci.
## 3. Guide d'implémentation de la stratégie

Ce guide montre, étape par étape, comment passer du bot de départ à une
stratégie complète. Chaque étape contient le code à écrire, avec des
commentaires qui expliquent ce qu'il fait et pourquoi.

Le code de cette page a été compilé avec `javac --release 25` contre les types
du dossier `generated/`, sans erreur. Il compile donc tel quel.

Tous les fichiers vont dans `src/main/java/codes/blitz/game/bot/`, le même
dossier que `Bot.java`. Vous n'avez jamais besoin de toucher aux fichiers du
dossier `generated/`.

### 3.1 Le bot de départ

Le `Bot.java` d'origine envoie une météorite sur le premier dinosaure visible.
C'est un point de départ pour vérifier que la connexion fonctionne, pas une
bonne stratégie.

```java
public List<Action> getActions(TeamGameState gameMessage) {
  // Liste des actions à renvoyer au serveur pour ce tour.
  List<Action> actions = new ArrayList<>();

  // S'il y a au moins un dinosaure visible...
  if (!gameMessage.dinosaurs().isEmpty()) {
    // ...on prend le premier de la liste (pas forcément le meilleur choix).
    Dinosaur firstDinosaur = gameMessage.dinosaurs().get(0);
    // On lance une météorite sur sa position actuelle.
    actions.add(new LaunchMeteorAction(firstDinosaur.position()));
  }

  // Une liste vide signifie « rien à faire ce tour ».
  return actions;
}
```

Problèmes de ce code :

- La météorite vise la position **actuelle** du dinosaure. Il peut bouger avant
  l'impact, et il est prévenu dès le lancement.
- Aucun test de `maxMeteors` : si la limite est atteinte, l'action est rejetée.
- Aucun volcan n'est utilisé.

### 3.2 Étape 1 : garder un état entre les tours

Le serveur envoie un état à chaque tour, mais l'objet `Bot` n'est créé
**qu'une seule fois**. Tout ce qui doit survivre d'un tour à l'autre doit donc
être stocké dans un champ de `Bot`.

```java
// Planificateurs : chacun choisit une cible pour une action (étapes 3 et 4).
private final MeteorPlanner meteorPlanner = new MeteorPlanner();
private final VolcanoPlanner volcanoPlanner = new VolcanoPlanner();
// Historique des lancements récents (étape 6, facultative).
private final List<Launch> recentLaunches = new ArrayList<>();
```

Ces champs sont `final` : on ne remplace pas l'objet, on met seulement son
contenu à jour. `Launch` est défini à la fin de la classe (étape 6).

### 3.3 Étape 2 : construire une vue du tour

Au lieu de refaire les mêmes calculs dans chaque règle, on calcule une fois par
tour : la liste des dinosaures triée, les cases dangereuses, les volcans, et la
conversion monde → tableau. Cette classe s'appelle `TurnView`.

Fichier `bot/TurnView.java` :

```java
package codes.blitz.game.bot;

import codes.blitz.game.generated.Constants;
import codes.blitz.game.generated.Dinosaur;
import codes.blitz.game.generated.GameMap;
import codes.blitz.game.generated.Meteor;
import codes.blitz.game.generated.TeamGameState;
import codes.blitz.game.generated.Tile;
import codes.blitz.game.generated.Volcano;
import codes.blitz.game.generated.WorldPosition;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TurnView {
  // L'état reçu du serveur pour ce tour, gardé tel quel.
  private final TeamGameState state;
  // Dinosaures triés du plus jeune au plus vieux (les jeunes valent plus de points).
  private final List<Dinosaur> dinosaurs;
  // Toutes les cases qui seront touchées par une météorite déjà en vol.
  private final Set<WorldPosition> dangerTiles = new HashSet<>();
  // Positions des volcans déjà actifs.
  private final Set<WorldPosition> volcanoTiles = new HashSet<>();

  public TurnView(TeamGameState state) {
    this.state = state;
    // stream().sorted(...).toList() crée une nouvelle liste triée par âge croissant.
    this.dinosaurs =
        state.dinosaurs().stream().sorted(Comparator.comparingInt(Dinosaur::age)).toList();
    // Pour chaque météorite en vol, on ajoute toutes les cases de sa zone d'impact.
    for (Meteor meteor : state.meteors()) {
      dangerTiles.addAll(meteor.impactedTiles());
    }
    // On note les positions des volcans déjà en éruption.
    for (Volcano volcano : state.volcanoes()) {
      volcanoTiles.add(volcano.position());
    }
  }

  public TeamGameState state() {
    return state;
  }

  public Constants constants() {
    return state.constants();
  }

  public List<Dinosaur> dinosaurs() {
    return dinosaurs;
  }

  // Nombre de météorites en vol (comparé à maxMeteors plus tard).
  public int meteorsInFlight() {
    return state.meteors().size();
  }

  // Nombre de volcans actifs (comparé à maxVolcanoes plus tard).
  public int volcanoCount() {
    return state.volcanoes().size();
  }

  // Vrai si une météorite en vol va déjà frapper cette case.
  public boolean isInDanger(WorldPosition position) {
    return dangerTiles.contains(position);
  }

  // Vrai si un volcan est déjà sur cette case.
  public boolean hasVolcanoAt(WorldPosition position) {
    return volcanoTiles.contains(position);
  }

  // Vrai si la position est dans le cadre visible. Une action hors cadre est rejetée.
  public boolean inFrame(WorldPosition position) {
    GameMap map = state.map();
    int minX = map.origin().x();
    int minY = map.origin().y();
    // Le cadre va de origin à origin + width - 1 (en x) et origin + height - 1 (en y).
    return position.x() >= minX
        && position.x() < minX + map.width()
        && position.y() >= minY
        && position.y() < minY + map.height();
  }

  // Renvoie la case à une position monde, ou null si elle est hors cadre.
  public Tile tileAt(WorldPosition position) {
    if (!inFrame(position)) {
      return null;
    }
    GameMap map = state.map();
    // Conversion monde -> tableau, selon la règle du starter kit :
    // i = y - origin.y (ligne) et j = x - origin.x (colonne).
    int i = position.y() - map.origin().y();
    int j = position.x() - map.origin().x();
    return map.tiles()[i][j];
  }
}
```

Points à retenir :

- `inFrame` doit toujours être vérifié avant d'envoyer une action, sinon elle
  est rejetée.
- Le tableau `tiles` est indexé `[ligne][colonne]`, donc `[y][x]` après
  conversion, pas `[x][y]`. C'est l'erreur la plus fréquente.

### 3.4 Étape 3 : choisir une cible de météorite

Principe : la météorite touche `impactedTiles` au moment de l'impact. Un
dinosaure meurt s'il occupe une de ces cases **à ce moment-là**.

Heuristique utilisée ici :

1. Pour chaque dinosaure, on prend sa position actuelle comme cible candidate.
2. On compte les dinosaures situés dans un rayon `meteorRadius` autour de cette
   cible. Ce rayon est une **approximation** (distance de Manhattan, en forme de
   losange). La vraie zone est dans `Meteor.impactedTiles` après le lancement :
   vérifiez-la dans `game.json` avant de vous y fier.
3. Chaque dinosaure compte selon son âge : plus il est jeune, plus il vaut.
4. On garde la cible au meilleur score.

Fichier `bot/MeteorPlanner.java` :

```java
package codes.blitz.game.bot;

import codes.blitz.game.generated.Dinosaur;
import codes.blitz.game.generated.WorldPosition;
import java.util.Optional;

public final class MeteorPlanner {

  // Renvoie la meilleure cible de météorite, ou rien si on ne doit pas lancer.
  public Optional<WorldPosition> bestTarget(TurnView view) {
    // Règle du jeu : au maximum maxMeteors météorites en vol. Au-delà, l'action est rejetée.
    if (view.meteorsInFlight() >= view.constants().maxMeteors()) {
      return Optional.empty();
    }

    int radius = view.constants().meteorRadius();
    WorldPosition best = null;
    int bestScore = 0;

    for (Dinosaur dino : view.dinosaurs()) {
      // Cible candidate : la position actuelle du dinosaure.
      WorldPosition target = dino.position();

      // Une cible hors cadre serait rejetée : on passe au suivant.
      if (!view.inFrame(target)) {
        continue;
      }
      // Inutile de viser une case déjà couverte par une météorite en vol.
      if (view.isInDanger(target)) {
        continue;
      }

      int score = scoreTarget(view, target, radius);
      // On garde la cible avec le meilleur score.
      if (score > bestScore) {
        bestScore = score;
        best = target;
      }
    }
    // Optional.ofNullable : vide si best est resté null (aucune cible trouvée).
    return Optional.ofNullable(best);
  }

  // Score d'une cible : somme du poids de chaque dinosaure qui serait touché.
  private int scoreTarget(TurnView view, WorldPosition target, int radius) {
    int score = 0;
    for (Dinosaur dino : view.dinosaurs()) {
      // Dinosaure dans la zone d'impact approximée ?
      if (manhattan(dino.position(), target) <= radius) {
        // Poids décroissant avec l'âge : un dinosaure jeune vaut plus.
        // max(1, ...) évite un poids nul ou négatif pour les vieux dinosaures.
        score += Math.max(1, 20 - dino.age());
      }
    }
    return score;
  }

  // Distance de Manhattan : |dx| + |dy|. Simple et rapide à calculer.
  static int manhattan(WorldPosition a, WorldPosition b) {
    return Math.abs(a.x() - b.x()) + Math.abs(a.y() - b.y());
  }
}
```

### 3.5 Étape 4 : utiliser les volcans

Un volcan produit de la lave qui tue les dinosaures au contact. Il sert à
**canaliser** les dinosaures. Ici, on choisit la montagne la plus proche d'un
dinosaure.

Règles à respecter (indiquées par le jeu) :

- la cible doit être une montagne (`state.mountains()`) ;
- pas de volcan déjà présent sur cette case ;
- respecter `maxVolcanoes` ;
- la lave descend vers l'élévation la plus basse et s'arrête sur le terrain
  infranchissable. Le code ci-dessous ne prévoit pas ce trajet : c'est une
  heuristique simple.

Fichier `bot/VolcanoPlanner.java` :

```java
package codes.blitz.game.bot;

import codes.blitz.game.generated.Dinosaur;
import codes.blitz.game.generated.WorldPosition;
import java.util.Optional;

public final class VolcanoPlanner {

  // Renvoie la montagne la plus proche d'un dinosaure, ou rien.
  public Optional<WorldPosition> bestMountain(TurnView view) {
    // Limite de volcans actifs atteinte : on ne peut rien lancer.
    if (view.volcanoCount() >= view.constants().maxVolcanoes()) {
      return Optional.empty();
    }
    // Pas de dinosaure : inutile de faire couler de la lave.
    if (view.dinosaurs().isEmpty()) {
      return Optional.empty();
    }

    WorldPosition best = null;
    int bestDistance = Integer.MAX_VALUE;

    for (WorldPosition mountain : view.state().mountains()) {
      // On ignore les montagnes hors cadre et celles qui ont déjà un volcan.
      if (!view.inFrame(mountain) || view.hasVolcanoAt(mountain)) {
        continue;
      }
      int distance = distanceToNearestDinosaur(view, mountain);
      // On garde la montagne la plus proche des dinosaures.
      if (distance < bestDistance) {
        bestDistance = distance;
        best = mountain;
      }
    }
    return Optional.ofNullable(best);
  }

  // Distance entre une position et le dinosaure le plus proche.
  private int distanceToNearestDinosaur(TurnView view, WorldPosition from) {
    int nearest = Integer.MAX_VALUE;
    for (Dinosaur dino : view.dinosaurs()) {
      // Math.min garde la plus petite valeur trouvée jusqu'ici.
      nearest = Math.min(nearest, MeteorPlanner.manhattan(from, dino.position()));
    }
    return nearest;
  }
}
```

### 3.6 Étape 5 : arbitrer une action par tour

Le serveur n'applique qu'une seule action par tour. Il faut donc choisir.
Règle simple dans ce code : **la météorite est prioritaire** si une cible
existe ; sinon on tente un volcan. Une seule action est renvoyée.

Ce code se trouve dans `Bot.java` (voir la classe complète à l'étape 7). Le
passage central est :

```java
Optional<WorldPosition> meteorTarget = meteorPlanner.bestTarget(view);
if (meteorTarget.isPresent()) {
  // Une météorite a une bonne cible : on la lance, et on s'arrête là.
  actions.add(new LaunchMeteorAction(meteorTarget.get()));
} else {
  // Sinon, on regarde si un volcan peut être utile.
  Optional<WorldPosition> volcanoTarget = volcanoPlanner.bestMountain(view);
  // ifPresent exécute la lambda seulement si une montagne a été trouvée.
  volcanoTarget.ifPresent(target -> actions.add(new TriggerVolcanoAction(target)));
}
```

Un tour sans action est possible : `actions` reste vide si aucune cible ne
vaut le coup.

### 3.7 Étape 6 : suivre les impacts récents (facultatif)

La page mentionne une « cascade sismique » : une météorite qui tombe sur une
case déjà abaissée dans les 5 derniers tours a une zone plus grande. Cet effet
n'est pas exposé par l'API. Le bot peut seulement garder la trace de ses propres
lancements, pour pouvoir le vérifier ensuite dans `game.json`.

Ajouts dans `Bot.java` :

```java
// Un lancement mémorisé : la cible et le tour où il a été fait.
private record Launch(WorldPosition target, int tick) {}

// Champ de la classe :
private final List<Launch> recentLaunches = new ArrayList<>();

// Dans getActions, juste après avoir lancé une météorite :
recentLaunches.add(new Launch(meteorTarget.get(), state.tick()));
// Ne garder que les lancements des 5 derniers tours.
recentLaunches.removeIf(launch -> state.tick() - launch.tick() > 5);
```

`recentLaunches` n'est lu par aucune décision pour l'instant. Il sert à
préparer une analyse, pas à choisir une action.

### 3.8 Étape 7 : assembler `Bot.java`

Cette classe fait le lien entre les étapes précédentes. Elle reste courte parce
que la logique est dans les autres fichiers (séparation des rôles).

Fichier `bot/Bot.java` :

```java
package codes.blitz.game.bot;

import codes.blitz.game.generated.Action;
import codes.blitz.game.generated.LaunchMeteorAction;
import codes.blitz.game.generated.TeamGameState;
import codes.blitz.game.generated.TriggerVolcanoAction;
import codes.blitz.game.generated.WorldPosition;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Bot {
  // Planificateurs et mémoire qui survivent d'un tour à l'autre (étapes 1 et 6).
  private final MeteorPlanner meteorPlanner = new MeteorPlanner();
  private final VolcanoPlanner volcanoPlanner = new VolcanoPlanner();
  private final List<Launch> recentLaunches = new ArrayList<>();

  public Bot() {
    System.out.println("Initializing your super mega duper bot");
  }

  // Appelée à chaque tour par WebsocketClient. Renvoie la liste d'actions du tour.
  public List<Action> getActions(TeamGameState state) {
    // Mesure du temps de calcul (étape 8).
    long start = System.nanoTime();
    List<Action> actions = new ArrayList<>();

    // 1. Préparer la vue du tour.
    TurnView view = new TurnView(state);

    // 2. Choisir l'action : météorite en priorité, sinon volcan.
    Optional<WorldPosition> meteorTarget = meteorPlanner.bestTarget(view);
    if (meteorTarget.isPresent()) {
      actions.add(new LaunchMeteorAction(meteorTarget.get()));
      recentLaunches.add(new Launch(meteorTarget.get(), state.tick()));
    } else {
      Optional<WorldPosition> volcanoTarget = volcanoPlanner.bestMountain(view);
      volcanoTarget.ifPresent(target -> actions.add(new TriggerVolcanoAction(target)));
    }

    // 3. Nettoyer l'historique des lancements (étape 6).
    recentLaunches.removeIf(launch -> state.tick() - launch.tick() > 5);

    // 4. Afficher un avertissement si le tour a été trop lent (étape 8).
    long elapsedMs = (System.nanoTime() - start) / 1_000_000;
    if (elapsedMs > 100) {
      System.out.println("Tour " + state.tick() + " lent : " + elapsedMs + " ms");
    }
    return actions;
  }

  // Un lancement mémorisé : la cible et le tour où il a été fait.
  private record Launch(WorldPosition target, int tick) {}
}
```

Pour vérifier que tout compile, lancez :

```bash
mvn package
```

### 3.9 Étape 8 : mesurer le temps de calcul

Ce qui a été fait ci-dessus : une classe par rôle (`TurnView`, `MeteorPlanner`,
`VolcanoPlanner`) et `Bot` qui les appelle. Chaque classe ne dépend que des
types de `generated/` : vous pouvez la tester sans lancer le serveur.

Pour le temps de calcul, la limite est courte sur les serveurs (150 ms en local
par défaut). Le code mesure chaque tour :

```java
// Au début de getActions :
long start = System.nanoTime();   // heure en nanosecondes

// À la fin de getActions :
long elapsedMs = (System.nanoTime() - start) / 1_000_000;  // conversion en millisecondes
if (elapsedMs > 100) {
  System.out.println("Tour " + state.tick() + " lent : " + elapsedMs + " ms");
}
```

Si un tour est lent, cherchez les boucles imbriquées (par exemple, une boucle
sur les dinosaures à l'intérieur d'une boucle sur les dinosaures). Une carte de
quelques milliers de cases se parcourt en quelques millisecondes ; une recherche
sans limite peut dépasser le délai.

### 3.10 Étape 9 : ordre de travail recommandé

1. Faire tourner le bot de départ en local (vérifier la connexion).
2. Ajouter `TurnView` et `MeteorPlanner` (étapes 2 et 3), avec une seule météorite par tour.
3. Ajouter `VolcanoPlanner` et l'arbitrage (étapes 4 et 5).
4. Tester plusieurs cartes et comparer les versions.
5. Envoyer sur les serveurs et analyser les parties (voir [05-serveurs-et-inscription.md](05-serveurs-et-inscription.md)).

Commandes de test locales (depuis la racine du projet, le launcher tournant dans
`launcher/`) :

```bash
# Compiler le projet
mvn package

# Lancer le bot (le launcher doit déjà attendre une connexion)
java -jar target/bot.jar
```

### 3.11 Pterodactyl

Le texte de la page décrit une espèce Pterodactyl avec une action
`ANSWER_PTERODACTYL`. Cette action n'existe ni dans `ActionType` ni dans les
autres types générés. **Il n'y a donc pas de code à écrire pour cette espèce**
tant que la documentation officielle et le starter kit ne la mentionnent pas.
## 4. Tests locaux

Le launcher `blitz-challenge-…` est le serveur de jeu local. Il attend le bot sur
`ws://127.0.0.1:8765`. Lancez-le d'abord, puis le bot.

### 4.1 Télécharger le launcher

Téléchargez le launcher depuis le dashboard. Dans ce projet, il est rangé dans
le dossier `launcher/` (ignoré par Git) :

```
ThanatosAI/
└── launcher/
    ├── blitz-challenge-macos-arm64     # binaire (sur Windows : blitz-challenge-win.exe)
    └── ui/                             # interface web locale
```

| Système | Exécutable |
| --- | --- |
| Windows | `blitz-challenge-win.exe` |
| macOS (Apple Silicon) | `blitz-challenge-macos-arm64` |
| Linux | `blitz-challenge-linux` |

Sur macOS et Linux, rendez-le exécutable si besoin :

```bash
chmod +x launcher/blitz-challenge-macos-arm64
```

Sur macOS, si le système bloque le binaire, autorisez-le dans
Réglages Système → Confidentialité et sécurité.

### 4.2 Lancer une partie locale

Depuis le dossier `launcher/` (`game.json` et `launcher.log` y sont écrits) :

```bash
cd launcher
./blitz-challenge-macos-arm64 --keepAlive false --delayBetweenTicksMs 0 --recordPath game.json > launcher.log 2>&1 &
cd ..
```

Puis, depuis la racine du projet, lancez le bot (`mvn package` puis `java -jar target/bot.jar`).

| Option | Effet |
| --- | --- |
| `--keepAlive false` | Joue une partie puis s'arrête |
| `--delayBetweenTicksMs 0` | Pas de pause entre les tours (plus rapide) |
| `--recordPath game.json` | Enregistre l'état de chaque tour dans `game.json` |
| `--timePerTickMs 150` | Limite de temps par tour, comme sur les serveurs |
| `--maxTimeoutsBeforeKick 50` | Nombre de retards avant exclusion, comme sur les serveurs |
| `--biome …`, `--randomSeed …` | Rejouer la même carte |
| `--help` | Toutes les options |

Pour tester avec les mêmes conditions que les serveurs, ajoutez
`--timePerTickMs 150 --maxTimeoutsBeforeKick 50`.

Pour déboguer pas à pas, `--timePerTickMs 0` met le jeu en pause entre les tours
le temps que vous posiez un point d'arrêt dans votre code.

### 4.3 Lancer le bot

Dans un second terminal, depuis le dossier du projet :

```bash
mvn package
java -jar target/bot.jar
```

Le bot affiche `Initializing your super mega duper bot`, puis une ligne
`Playing tick N` par tour. Les erreurs de tour s'affichent sur la sortie
d'erreur (`Errors during last tick: …`).

Si le bot démarre avant le launcher, la connexion échoue : lancez le launcher
d'abord.

#### Enregistrement avec un token

Par défaut le bot s'enregistre sous le nom `myJavaBot`. Si la variable
d'environnement `TOKEN` est définie, le bot s'enregistre avec ce token :

```bash
TOKEN=votre_token java -jar target/bot.jar
```

Ne mettez jamais ce token dans le code ni dans le zip.

### 4.4 Interface de jeu locale

Le launcher sert une interface dans le navigateur (l'adresse est affichée dans
`launcher.log`). Raccourcis utiles :

- `d` : superposition de débogage ;
- `h` : aide des raccourcis.

### 4.5 Lire `launcher.log`

```bash
grep -i "replay" launcher.log
```

La ligne `Replay:` donne `--biome` et `--randomSeed`. Relancez avec ces valeurs
pour rejouer exactement la même carte, par exemple pour comparer deux versions
du bot.

### 4.6 Analyser `game.json`

`game.json` peut être très gros : ne l'affichez jamais en entier. Utilisez `jq`
(installation : `brew install jq`).

Lister les clés d'un tour :

```bash
jq -c '.commandsAndTicks[0] | keys' game.json
```

Voir les scores et les erreurs sur les derniers tours :

```bash
jq -c '.gameResults' game.json
```

Suivre un champ dans le temps (ici le score du tour, adaptez le chemin selon
les clés affichées ci-dessus) :

```bash
jq -c '.commandsAndTicks[] | {tick: .tick, score: .score}' game.json | tail -20
```

Les chemins exacts dépendent du format du fichier : commencez par les clés, puis
affinez.

### 4.7 Comparer deux versions

1. Lancez le launcher une fois, notez `--biome` et `--randomSeed`.
2. Lancez la version A du bot, notez le score final.
3. Relancez le launcher avec les mêmes options, lancez la version B, comparez.

Une seule partie ne suffit pas à conclure : répétez sur plusieurs graines.

### 4.8 Boucle de développement conseillée

1. Modifier la stratégie dans `Bot.java` (ou un fichier voisin).
2. `mvn package` : le build doit réussir sans erreur.
3. Relancer le launcher, puis `java -jar target/bot.jar`.
4. Vérifier `launcher.log` et la sortie du bot pour les exceptions
   (`Exception while getting next moves:`).
5. Analyser `game.json` avec `jq` si le score est inattendu.

Les dossiers générés (`game.json`, `launcher.log`, `blitz-games/`) sont déjà
listés dans `.gitignore` : ne les versionnez pas.
## 5. Serveurs et inscription

Les parties qui comptent pour l'inscription se jouent sur les serveurs officiels.
Le même launcher sert à s'y connecter. Dans ce projet, les commandes se lancent
depuis la racine du projet, avec le binaire de `launcher/` :

```bash
./launcher/blitz-challenge-macos-arm64 login
```

Dans les exemples ci-dessous, `<launcher>` désigne cette commande.

### 5.1 Connexion

Depuis le dossier du launcher (ou avec son chemin complet) :

```bash
<launcher> login
```

- Une page s'ouvre dans le navigateur : cliquez sur **Authorize**.
- La commande attend l'autorisation jusqu'à 5 minutes.
- La connexion dure 24 heures.

Si le navigateur est sur une autre machine (SSH, IDE cloud) : autorisez là-bas,
puis, dans la minute, exécutez sur cette machine la commande `curl` indiquée
(l'adresse `127.0.0.1` que le navigateur n'a pas pu ouvrir).

### 5.2 Créer le zip du bot (Java)

Le zip doit contenir uniquement ce dont le serveur a besoin pour construire le
bot, **à la racine du zip** (sous-dossiers conservés) :

| Dans le zip | Remarque |
| --- | --- |
| `pom.xml` | Build Maven |
| `src/` | Code source (y compris `generated/` et les fichiers de connexion, nécessaires au build) |

À exclure : `target/`, `.idea/`, `.git/`, `.DS_Store`, `AGENTS.md`, `README.md`,
`docs/`, les logs, les parties téléchargées (`blitz-games/`, `game.json`,
`launcher.log`), et tout fichier `.env`.

Depuis la racine du projet, pour créer `../bot.zip` (en dehors du projet) :

```bash
cd /chemin/vers/ThanatosAI
zip -r ../bot.zip pom.xml src -x '*.DS_Store'
```

Contrôlez le contenu avant d'envoyer :

```bash
unzip -l ../bot.zip
```

Vous devez voir `pom.xml` et les fichiers `src/main/java/...` uniquement. Pas de
`target/`, pas de `.DS_Store`. La taille doit rester sous 10 Mo.

### 5.3 Envoyer le bot

```bash
<launcher> upload ../bot.zip
```

- Le serveur compile le bot, puis répond.
- Code de sortie `0` : le build a réussi, le bot est actif.
- Code de sortie `1` : le build a échoué. Le log de build et la raison
  s'affichent ; l'ancien bot reste actif.
- Code de sortie `2` : la commande a cessé d'attendre avant la fin du build.

Pour ne pas attendre sur place :

```bash
<launcher> upload ../bot.zip --no-wait
<launcher> builds <buildId>      # code 2 tant que le build tourne
```

### 5.4 Lancer une partie d'entraînement

```bash
<launcher> practice
```

Affiche une ligne : le premier champ est l'identifiant de la partie, `score` est
le score du bot (`rank` vaut toujours 1 : le bot joue seul). Un score vide signifie
que le bot ne s'est jamais connecté : regardez `bot.log` dans le dossier
téléchargé.

Pour ne pas attendre :

```bash
<launcher> practice --no-wait
<launcher> games                 # l'état (state) de chaque partie
```

### 5.5 Télécharger une partie

```bash
<launcher> download <gameId>
```

Crée `blitz-games/<gameId>/` contenant :

- `replay.jsonl` : un état de jeu par ligne, tel que le voit un spectateur.
  Une ligne fait plusieurs dizaines de Ko : ne jamais l'afficher en entier.

  ```bash
  tail -1 replay.jsonl | jq -c 'keys'
  jq -c '{currentTick, score}' replay.jsonl | tail -5
  ```

  Les numéros de ligne ne sont pas les numéros de tour : cherchez le tour par son
  champ `currentTick`.

- `bot.log` : la sortie de votre bot (prints, erreurs, crashs).
- `game.log` : le log du serveur. Sa ligne `Replay:` permet de rejouer la carte
  en local avec `--biome` et `--randomSeed`.

### 5.6 Suivre ses parties et ses builds

```bash
<launcher> games      # parties de l'équipe, les plus récentes d'abord (100 max)
<launcher> builds     # builds de l'équipe
<launcher> builds <buildId>   # log d'un build précis
```

Téléchargez les parties au score le plus bas pour comprendre ce qui a échoué.

### 5.7 Score d'inscription

- Le score d'équipe est le **meilleur** score parmi les parties jouées jusqu'au
  bout sur les serveurs (`timedOut` à `false` dans `games`).
- Une exclusion (kick) ou un crash pendant une partie invalide cette partie.
- Le tableau de bord indique ce qui manque encore pour finaliser l'inscription.

### 5.8 Codes de sortie et limites

| Code | Signification |
| --- | --- |
| `0` | Succès |
| `1` | Échec (build ou partie) : le message explique la raison |
| `2` | Arrêt de l'attente avant la fin : relancer plus tard |
| `3` | Limite de débit atteinte : attendre une minute puis relancer |

Le launcher ne voit que le défi d'inscription : `games` et `builds` concernent
uniquement ce défi.
## 6. Dépannage

### Le build Maven échoue

| Symptôme | Cause probable | Solution |
| --- | --- | --- |
| `invalid target release: 25` | JDK trop ancien | Installer JDK 25 ou plus récent, puis `java -version` |
| `mvn: command not found` | Maven absent | Installer Maven 3.9+ (`brew install maven`) |
| Erreur de dépendance / téléchargement | Pas d'accès Internet ou cache corrompu | Relancer, ou `mvn -U package` |
| Erreur sur un fichier `generated/` | Fichier modifié | Restaurer le fichier d'origine (`git checkout -- <fichier>`) |

### Le bot ne se connecte pas

- Vérifiez que le launcher tourne **avant** le bot.
- Le bot se connecte à `ws://127.0.0.1:8765`. Un autre programme sur ce port
  empêche la connexion.
- Consultez `launcher.log`.

### Le bot joue mais ne fait rien

- Regardez la sortie du bot : `Exception while getting next moves:` signale une
  exception dans `getActions`. Le tour part alors sans action.
- Lisez `lastTickErrors` affiché dans le terminal (`Errors during last tick: …`).
  Causes fréquentes :
  - cible hors du cadre (vérifier `origin`, `width`, `height`) ;
  - météorites en vol au maximum (`maxMeteors`) ;
  - volcan sur une case qui n'est pas une montagne, ou déjà en éruption ;
  - plusieurs actions envoyées : seule la première est appliquée.

### Une exception `ArrayIndexOutOfBoundsException`

Conversion monde → tableau incorrecte. Utilisez :

```java
int i = pos.y() - map.origin().y();
int j = pos.x() - map.origin().x();
```

et vérifiez `0 <= i < height` et `0 <= j < width` avant d'accéder à
`map.tiles()[i][j]`.

### Le bot se fait exclure sur les serveurs

- Un tour qui dépasse la limite compte comme « aucune action ». Au-delà de 50
  retards, le bot est exclu, et la partie est invalidée.
- Testez avec `--timePerTickMs 150 --maxTimeoutsBeforeKick 50` en local.
- Retirez les calculs lourds (simulation illimitée, copies de grandes listes à
  chaque tour, recherches récursives sans borne).

### `upload` échoue

- Code `1` : lire le log de build affiché. C'est souvent une erreur de
  compilation qui n'apparaissait pas en local (fichier manquant dans le zip,
  dépendance absente du `pom.xml`).
- Vérifiez le zip avec `unzip -l ../bot.zip` : `pom.xml` et `src/` doivent être
  à la racine du zip.
- Zip trop gros (plus de 10 Mo) : vérifier qu'aucun `target/` n'est inclus.
- Code `3` : limite de débit, attendre une minute.

### `login` expire

Le login dure 24 heures et l'autorisation doit être donnée dans les 5 minutes.
Relancez `<launcher> login`. Si le navigateur est sur une autre machine, voir
05-serveurs-et-inscription.md.

### Une partie serveur a un score vide

Le bot ne s'est jamais connecté. Ouvrez `bot.log` dans `blitz-games/<gameId>/`
pour voir le crash au démarrage (souvent : jar absent du zip, ou exception dans
le constructeur de `Bot`).

### Le launcher est bloqué par macOS

Réglages Système → Confidentialité et sécurité → autoriser le binaire, puis
relancer. Vous pouvez aussi retirer l'attribut de quarantaine :

```bash
xattr -d com.apple.quarantine launcher/blitz-challenge-macos-arm64
```
