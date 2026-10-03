<p align="center">
  <img src="docs/icon.svg" width="128" alt="Spotlight">
</p>

<h1 align="center">Spotlight</h1>

<p align="center">
  Une recherche d'apps façon Spotlight (macOS) / menu Démarrer (Windows) pour tablette Android,<br>
  pilotable entièrement au clavier physique.
</p>

---

## Fonctionnalités

- **Ouverture au clavier** : touche Meta (⊞, la touche « 4 carrés ») seule, Meta + Espace, Alt + Espace, Ctrl + Espace
  ou **n'importe quel raccourci personnalisé** enregistré depuis l'app. Appuyer à nouveau sur le raccourci referme la fenêtre.
- **Recherche instantanée** des apps installées : préfixe, début de mot, initiales (`ym` → YouTube Music),
  correspondance approximative (`ytmsc`), insensible aux accents (`parametres` → Paramètres).
- **100 % clavier** : `↑` `↓` / `Tab` pour naviguer, `Entrée` pour ouvrir, `Échap` pour effacer puis fermer.
- **Suggestions** : les apps les plus utilisées s'affichent quand le champ est vide, et remontent dans les résultats.
- **Recherche Play Store** quand aucune app ne correspond.
- Interface inspirée de macOS Sonoma / OneUI / HyperOS : panneau translucide, flou d'arrière-plan (Android 12+),
  coins arrondis, thème clair/sombre automatique.

## Installation

1. Téléchargez le dernier `Spotlight-vX.Y.Z-arm64-v8a.apk` depuis les [Releases](../../releases/latest) et installez-le.
2. Ouvrez **Spotlight**, puis touchez **Activer** : dans *Accessibilité*, activez **Spotlight – raccourci clavier**.
3. Si l'option est grisée (« paramètre restreint », Android 13+ / HyperOS) :
   *Paramètres → Applications → Spotlight → ⋮ (en haut à droite) → Autoriser les paramètres restreints*, puis recommencez l'étape 2.
4. **Xiaomi / HyperOS (indispensable)** : la section *Fonctionnement en arrière-plan* de l'app liste les autorisations
   à accorder, chacune avec un bouton qui ouvre directement le bon écran :
   - **Afficher des fenêtres pop-up en arrière-plan** : sans elle, le raccourci est détecté mais la fenêtre ne s'ouvre pas ;
   - **Démarrage automatique** : sans elle, HyperOS tue le service dès qu'on quitte l'app et ne le relance jamais ;
   - **Batterie sans restriction**.

   Si l'app indique « Service arrêté par le système », désactivez puis réactivez le service dans l'accessibilité
   après avoir accordé ces autorisations.

Le service tourne au premier plan pour résister au nettoyage mémoire de HyperOS. Sa notification (priorité minimale)
n'apparaît que si vous autorisez les notifications de Spotlight.

### Mises à jour

Spotlight se met à jour tout seul depuis les releases de ce repo (section *Mises à jour* de l'app) : vérification
toutes les 6 heures, téléchargement puis installation en arrière-plan. Il faut autoriser une fois Spotlight à
installer des apps. La première mise à jour depuis l'app demande une confirmation ; Android 12+ autorise ensuite les
suivantes sans intervention, car Spotlight devient l'installateur de sa propre version.

> Les mises à jour s'installent par-dessus la version précédente : toutes les releases sont signées avec la même clé
> (`app/spotlight.keystore`, volontairement publique, l'app n'étant pas destinée au Play Store).

## Pourquoi un service d'accessibilité ?

C'est le seul moyen, sans root, pour une app Android de voir les touches d'un clavier physique **avant** le système et
donc d'intercepter la touche Meta. Le service ne lit aucun contenu de l'écran (`canRetrieveWindowContent=false`) et ne
consomme que le raccourci configuré : toutes les autres touches passent normalement.

Le mode **Bloquer l'action système** (raccourcis à touche seule) consomme aussi l'appui de la touche, utile si HyperOS
lui associe déjà une action. Si votre touche « 4 carrés » n'est pas reconnue comme Meta, utilisez **Personnalisé** :
l'app affiche le nom de la touche capturée.

## Développement

```bash
./gradlew testReleaseUnitTest   # tests unitaires (détection du raccourci, moteur de recherche)
./gradlew assembleRelease       # APK dans app/build/outputs/apk/release/
```

- Kotlin + Jetpack Compose, `minSdk` 29, `targetSdk` 35, aucune dépendance hors AndroidX.
- Chaque push sur `main` lance [le workflow](.github/workflows/release.yml) qui exécute les tests, compile l'APK arm64
  et publie une release `v1.0.<numéro de run>`.

```
app/src/main/java/fr/nico7an/spotlight/
├── core/      Raccourcis, détection des touches, enregistrement d'un raccourci
├── data/      Liste des apps + icônes, moteur de recherche, réglages, statistiques d'usage
├── service/   Service d'accessibilité (filtre clavier)
└── ui/        Fenêtre de recherche et écran de réglages (Compose)
```

## Licence

MIT
