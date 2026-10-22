# Plantation Manager

Application de gestion des employés d'une plantation de bananes.

## Objectif

Faciliter le travail du patron de la plantation :

- **Tout est centralisé sur le PC du bureau** : les informations des employés, les parcelles, les tâches et les pointages sont enregistrés dans une base de données locale.
- **Savoir qui est là ou non** : les employés pointent leur arrivée, leurs tâches et leur départ sur une borne. Le patron voit en temps réel qui est présent, absent ou en retard, et qui travaille sur quelle parcelle.
- **Simplifier les salaires** : les heures et les montants à payer sont calculés automatiquement pour une période, avec un export pour Excel.
- **Accès à distance** : le patron consulte l'application depuis son téléphone, sans être au bureau, via un réseau privé sécurisé (Tailscale).
- **Évolutif** : d'autres fonctions pourront s'ajouter au fil du temps.

## Fonctionnalités

### Tableau de bord du patron (`/`)

- **Aujourd'hui** : nombre de personnes sur place, parties, absentes et en retard. Liste de présence avec heure d'arrivée, de départ, heures faites et tâche en cours. Liste des tâches de la journée. Se met à jour toutes les minutes.
- **Employés** : ajout, modification et désactivation. Pour chaque employé : taux horaire et code PIN de la borne.
- **Pointages** : historique par période et par employé, ajout et correction à la main. Les **départs oubliés** sont signalés en haut pour être corrigés.
- **Salaires** : pour une période, les jours travaillés, les heures normales, les heures sup majorées et le total à payer par employé. Export CSV qui s'ouvre directement dans Excel.
- **Parcelles & tâches** : la liste de ce qui est proposé sur la borne.

Le tableau de bord s'utilise aussi sur téléphone.

### Borne de pointage (`/borne.html`)

Écran tactile sur une tablette au bureau ou à l'entrée de la plantation :

1. l'employé touche son nom (un point vert indique qui est déjà sur place) ;
2. il tape son **code PIN** ;
3. il choisit **J'arrive**, **Commencer une tâche** (tâche + parcelle), **Changer de tâche**, **Terminer la tâche** ou **Je pars**.

Le départ termine automatiquement la tâche en cours. Après 5 codes faux, l'employé est bloqué 5 minutes.

### Sécurité

- Deux comptes : **patron** (accès à tout) et **borne** (accès uniquement au pointage).
- Les mots de passe ne sont pas dans le code : ils viennent des variables d'environnement.
- Les codes PIN sont enregistrés chiffrés (BCrypt).
- Protection contre les requêtes falsifiées (CSRF).

### Calcul des salaires

- Les heures viennent des pointages d'**arrivée et de départ**. Plusieurs pointages le même jour sont additionnés (pause du midi, par exemple).
- Au-delà de **35 h par semaine** (lundi au dimanche), les heures sont des heures sup, majorées de **25 %**. Ces deux valeurs sont réglables.
- Un pointage sans départ ne compte pas tant que le patron ne l'a pas corrigé. Il est signalé dans le tableau de bord.
- Le montant utilise le taux horaire actuel de l'employé : si le taux change, recalculez les périodes passées avant le changement.

## Installation

Le guide complet est dans **[docs/INSTALLATION.md](docs/INSTALLATION.md)** : base de données, mots de passe, démarrage automatique, borne, accès à distance avec Tailscale et sauvegardes.

En résumé :

```powershell
setx DB_PASSWORD "..."
setx ADMIN_PASSWORD "..."
setx BORNE_PASSWORD "..."
.\mvnw.cmd package -DskipTests
.\scripts\demarrer.ps1
```

Puis ouvrir <http://localhost:8080>.

## Essayer sans rien installer (mode démo)

Le mode démo utilise une base en mémoire (effacée à l'arrêt) avec 4 employés, 3 parcelles et 5 tâches d'exemple :

```bash
./mvnw spring-boot:test-run
```

- Patron : `patron` / `patron-demo`
- Borne : `borne` / `borne-demo`
- Code PIN des employés : `1234`

## Tests

```bash
./mvnw test
```

Ils couvrent le calcul des salaires (heures sup, pauses, oublis), le parcours complet sur la borne (PIN, blocage, arrivée, changement de tâche, départ) et les droits d'accès (patron, borne, non connecté, CSRF).

## API REST

Toutes les routes demandent d'être connecté. Les routes `/api/borne/**` sont ouvertes au compte borne, les autres au patron uniquement.

| Domaine | Routes principales |
|---|---|
| Employés | `GET/POST /api/employes`, `GET/PUT /api/employes/{id}`, `GET /api/employes/actifs`, `GET /api/employes/search?terme=`, `PATCH /api/employes/{id}/desactiver` |
| Parcelles | `GET/POST /api/parcelles`, `PUT /api/parcelles/{id}`, `GET /api/parcelles/actives`, `PATCH /api/parcelles/{id}/desactiver` |
| Tâches | `GET/POST /api/taches`, `PUT /api/taches/{id}`, `GET /api/taches/actives`, `PATCH /api/taches/{id}/desactiver` |
| Présence | `GET /api/presence?date=` |
| Pointages | `GET /api/pointages?date=` ou `?debut=&fin=&employeId=`, `GET /api/pointages/incomplets`, `POST /api/pointages`, `PUT/DELETE /api/pointages/{id}` |
| Travaux | `GET /api/travaux`, `/en-cours`, `/date?date=`, `/employe/{id}`, `/employe/{id}/aujourd-hui`, `/employe/{id}/periode?debut=&fin=`, `/heures?debut=&fin=` |
| Salaires | `GET /api/salaires?debut=&fin=`, `GET /api/salaires/export?debut=&fin=` (CSV) |
| Borne | `GET /api/borne/employes`, `/taches`, `/parcelles` ; `POST /api/borne/identifier`, `/arrivee`, `/depart`, `/travail/demarrer`, `/travail/terminer` (corps : `{ "employeId", "pin", "tacheId", "parcelleId" }`) |

En cas d'erreur métier, l'API répond `400` avec `{ "erreur": "message" }`.

## Technologies

- Java 21, Spring Boot 4 (Web, Data JPA, Security)
- PostgreSQL (H2 en mémoire pour les tests et la démo)
- Pages HTML/CSS/JavaScript sans framework, servies par Spring Boot
- Maven

## Pistes d'évolution

- Photos des employés envoyées depuis le tableau de bord (aujourd'hui : une adresse web)
- Historique des taux horaires, primes et avances sur salaire
- Fiches de paie en PDF
- Congés et absences justifiées
- Rendement par parcelle (récolte, nombre de régimes)
- Pointage par badge ou QR code en plus du code PIN
