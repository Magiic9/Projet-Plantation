# Plantation Manager

Application de gestion des employés d'une plantation de bananes.

## Objectif

Faciliter le travail du patron de la plantation :

- **Tout est centralisé sur le PC du bureau** : informations des employés, parcelles, tâches et pointages sont enregistrés dans une base de données locale.
- **Savoir qui est là ou non** : les employés pointent le début et la fin de leurs tâches sur une borne, le patron voit en temps réel qui travaille, sur quelle parcelle et à quoi.
- **Simplifier les salaires** : les heures travaillées sont calculées automatiquement sur une période pour préparer les fiches de paie.
- **Accès à distance** : le patron doit pouvoir consulter l'application depuis l'extérieur, sans être au bureau.
- **Évolutif** : d'autres fonctions pourront s'ajouter au fil du temps.

## Fonctionnalités actuelles

Le projet est pour l'instant un **backend** (API REST). Il n'y a pas encore d'interface graphique.

### Employés — `/api/employes`

| Méthode | Route | Rôle |
|---|---|---|
| GET | `/api/employes` | Liste de tous les employés |
| GET | `/api/employes/actifs` | Employés actifs uniquement |
| GET | `/api/employes/{id}` | Détail d'un employé |
| GET | `/api/employes/search?terme=...` | Recherche d'un employé |
| POST | `/api/employes` | Ajouter un employé |
| PUT | `/api/employes/{id}` | Modifier un employé |
| PATCH | `/api/employes/{id}/desactiver` | Désactiver un employé (sans le supprimer) |
| DELETE | `/api/employes/{id}` | Supprimer un employé |

Un employé a : nom, prénom, date de naissance, photo, statut actif.

### Parcelles — `/api/parcelles` et Tâches — `/api/taches`

Mêmes opérations pour les deux : lister tout (`GET`), lister les actives (`GET /actives`), détail (`GET /{id}`), ajouter (`POST`), modifier (`PUT /{id}`), désactiver (`PATCH /{id}/desactiver`), supprimer (`DELETE /{id}`).

- Une **parcelle** a : nom, surface, statut actif.
- Une **tâche** a : nom, description, statut actif (ex. : récolte, désherbage, traitement…).

### Travaux et pointage — `/api/travaux`

Un **travail** relie un employé, une tâche et une parcelle, avec une date, une heure de début et une heure de fin.

**Borne de pointage**

| Méthode | Route | Rôle |
|---|---|---|
| POST | `/api/travaux/demarrer` | L'employé démarre une tâche (corps : `{ "employeId", "tacheId", "parcelleId" }`) — l'heure de début est l'heure actuelle |
| PATCH | `/api/travaux/{id}/terminer` | L'employé termine la tâche — l'heure de fin est l'heure actuelle |

**Suivi par le patron**

| Méthode | Route | Rôle |
|---|---|---|
| GET | `/api/travaux` | Tous les travaux |
| GET | `/api/travaux/en-cours` | Qui travaille en ce moment |
| GET | `/api/travaux/date?date=AAAA-MM-JJ` | Travaux d'une journée |
| GET | `/api/travaux/employe/{id}` | Historique d'un employé |
| GET | `/api/travaux/employe/{id}/aujourd-hui` | Travaux du jour d'un employé |
| GET | `/api/travaux/employe/{id}/periode?debut=...&fin=...` | Travaux d'un employé sur une période |
| DELETE | `/api/travaux/{id}` | Supprimer un travail |

**Salaires**

| Méthode | Route | Rôle |
|---|---|---|
| GET | `/api/travaux/heures?debut=AAAA-MM-JJ&fin=AAAA-MM-JJ` | Total d'heures par employé sur la période (travaux terminés) |

## Technologies

- Java 21
- Spring Boot 4 (Web, Data JPA)
- PostgreSQL
- Maven

## Lancer le projet

1. Installer PostgreSQL et créer une base nommée `Plantation` (utilisateur `postgres`).
2. Définir le mot de passe de la base dans la variable d'environnement `DB_PASSWORD`.
3. Lancer l'application :

   ```bash
   ./mvnw spring-boot:run
   ```

   Sous Windows : `mvnw.cmd spring-boot:run`

4. L'API est disponible sur `http://localhost:8080`. Les tables sont créées automatiquement au premier démarrage.

## Pistes d'évolution

- Pointage d'arrivée et de départ de la journée (présence / absence / retard)
- Taux horaire par employé et calcul du salaire à payer (heures sup, primes, avances)
- Connexion sécurisée (login) avant tout accès à distance
- Interface pour le patron (tableau de bord) et écran simple pour la borne (badge, QR code ou code PIN)
- Accès distant sécurisé au PC du bureau (par exemple via un VPN comme Tailscale), sans exposer l'application directement sur Internet
