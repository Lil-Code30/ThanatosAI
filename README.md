# ThanatosAI — Blitz 2027 registration challenge bot (Java)

Bot Java pour le défi d'inscription Blitz 2027 « Le défi de l'extinction ».
Le bot joue seul contre le jeu : chaque tour il reçoit l'état du jeu et envoie
**une seule action** (lancer une météorite ou déclencher un volcan). Le but est
d'obtenir le meilleur score possible sur une partie de 1000 tours.

> ‼️ I am just responsible for the code in `src/bot/Bot.java` And the reste are [Coveo's](https://github.com/coveo) properties

## Prérequis

- **JDK 25** (le `pom.xml` compile en `release 25`; un JDK plus récent fonctionne aussi).
- **Maven 3.9+**.
- Le **launcher** du défi (`blitz-challenge-…`), téléchargé depuis le dashboard.
  Il sert de serveur de jeu local et de client pour les serveurs officiels.

Vérification rapide :

```bash
java -version
mvn -v
```

## Structure rapide

```
ThanatosAI/
├── pom.xml                                   # dépendances et build (jar exécutable bot.jar)
├── README.md                                 # ce fichier
├── INSTRUCTIONS.md                           # règles et guide complet
├── launcher/                                 # launcher du défi (serveur de jeu local), ignoré par Git
│   ├── blitz-challenge-macos-arm64           # binaire du launcher
│   └── ui/                                   # interface web locale du launcher
└── src/main/java/codes/blitz/game/
    ├── bot/Bot.java                          # ✅ VOTRE STRATÉGIE (seul fichier à modifier)
    ├── generated/                            # ❌ types générés, ne pas modifier
    ├── message/                              # ❌ protocole de connexion, ne pas modifier
    ├── websocket/WebsocketClient.java        # ❌ connexion, ne pas modifier
    └── Application.java                      # ❌ point d'entrée, ne pas modifier
```

## Démarrage rapide

1. Lancer le serveur de jeu local, depuis le dossier `launcher/`, en arrière-plan :

   ```bash
   cd launcher
   ./blitz-challenge-macos-arm64 --keepAlive false --delayBetweenTicksMs 0 --recordPath game.json > launcher.log 2>&1 &
   cd ..
   ```

   `game.json` et `launcher.log` sont écrits dans `launcher/`.

2. Compiler et lancer le bot depuis la racine de ce projet :

   ```bash
   mvn package
   java -jar target/bot.jar
   ```

Le bot se connecte à `ws://127.0.0.1:8765`, joue la partie et s'arrête quand le
launcher ferme la connexion. Le détail de chaque étape est dans
[INSTRUCTIONS.md](INSTRUCTIONS.md) (section 4).

## Commandes utiles

| Action | Commande |
| --- | --- |
| Compiler le jar exécutable | `mvn package` |
| Nettoyer les sorties de build | `mvn clean` |
| Lancer le bot | `java -jar target/bot.jar` |
| Se connecter aux serveurs | `./launcher/blitz-challenge-macos-arm64 login` |
| Envoyer le bot | `./launcher/blitz-challenge-macos-arm64 upload ../bot.zip` |
| Partie d'entraînement sur serveur | `./launcher/blitz-challenge-macos-arm64 practice` |

Les commandes qui parlent aux serveurs (`login`, `upload`, `practice`,
`download`, `games`, `builds`) se lancent depuis la racine de ce projet. Le zip
est créé hors du projet (`../bot.zip`), voir [INSTRUCTIONS.md](INSTRUCTIONS.md),
section 5. Sur Windows ou Linux, remplacez `blitz-challenge-macos-arm64` par le
binaire de votre système.

## Règles à respecter

- Ne modifier **que** `src/main/java/codes/blitz/game/bot/Bot.java` et les nouveaux
  fichiers ajoutés à côté (même package `codes.blitz.game.bot`).
- Ne jamais modifier les types générés (`generated/`) : ils sont remplacés à chaque
  mise à jour du starter kit.
- Ne jamais modifier les fichiers de connexion (`Application.java`,
  `websocket/`, `message/`).
- Garder le calcul par tour court : sur les serveurs, une réponse en retard compte
  comme « aucune action », et 50 retards entraînent une exclusion.
- Ne pas envoyer `target/`, `.idea/` ni de fichiers `.env` dans le zip.

## Liens utiles

- Documentation du défi : page **Documentation** du dashboard (les règles complètes
  ne sont pas dans ce dépôt).
- Discord et https://www.twitch.tv/coveolife : liens sur le dashboard.
