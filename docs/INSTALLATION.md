# Installation sur le PC du bureau

Ce guide met en place l'application sur le PC du bureau du patron : base de données,
démarrage automatique, borne de pointage, accès à distance et sauvegardes.

```
 Plantation (Wi-Fi du bureau)                     N'importe où
 ┌──────────────────┐   ┌─────────────────────┐        ┌───────────────────┐
 │ Tablette borne   │──▶│ PC du bureau         │◀══════│ Téléphone / PC     │
 │ /borne.html      │   │ Plantation Manager   │Tailscale│ du patron         │
 └──────────────────┘   │ + PostgreSQL         │ (VPN)  └───────────────────┘
                        └─────────────────────┘
```

## 1. Ce qu'il faut installer

- **Java 21** (par exemple [Eclipse Temurin 21](https://adoptium.net/)).
- **PostgreSQL** (version 15 ou plus récente) : pendant l'installation, notez le mot de passe de l'utilisateur `postgres`.

## 2. Créer la base

Ouvrez **pgAdmin** (installé avec PostgreSQL) ou SQL Shell, puis :

```sql
CREATE DATABASE "Plantation";
```

Les tables sont créées automatiquement au premier démarrage de l'application.

## 3. Définir les mots de passe

Dans PowerShell, une seule fois (remplacez par vos propres mots de passe) :

```powershell
setx DB_PASSWORD "mot-de-passe-postgres"
setx ADMIN_PASSWORD "mot-de-passe-du-patron"
setx BORNE_PASSWORD "mot-de-passe-de-la-borne"
```

Fermez puis rouvrez PowerShell pour que les variables soient prises en compte.

- Compte **patron** : identifiant `patron`, mot de passe `ADMIN_PASSWORD`. Accès à tout.
- Compte **borne** : identifiant `borne`, mot de passe `BORNE_PASSWORD`. Accès uniquement à l'écran de pointage.

Pour changer les identifiants : `setx ADMIN_USERNAME "..."` et `setx BORNE_USERNAME "..."`.

Choisissez des mots de passe longs, surtout pour le patron : c'est lui qui donne accès aux salaires.

## 4. Compiler et lancer

Dans le dossier du projet :

```powershell
.\mvnw.cmd package -DskipTests
.\scripts\demarrer.ps1
```

Ouvrez <http://localhost:8080> et connectez-vous avec le compte patron.
Les messages de l'application sont écrits dans `plantation.log`.

## 5. Démarrage automatique

Pour que l'application démarre toute seule quand le PC s'allume :

1. Ouvrez le **Planificateur de tâches** Windows → *Créer une tâche…*
2. Onglet **Général** : nom `Plantation Manager`, cochez *Exécuter même si l'utilisateur n'est pas connecté*.
3. Onglet **Déclencheurs** : *Nouveau…* → *Au démarrage*.
4. Onglet **Actions** : *Nouveau…* →
   - Programme : `powershell.exe`
   - Arguments : `-ExecutionPolicy Bypass -File "C:\chemin\vers\Projet-Plantation\scripts\demarrer.ps1"`
5. Onglet **Conditions** : décochez *Ne démarrer que si l'ordinateur est sur secteur*.
6. Onglet **Paramètres** : décochez *Arrêter la tâche si elle s'exécute plus de…*.

Réglez aussi Windows pour que **le PC ne se mette jamais en veille** (Paramètres → Système →
Alimentation → Écran et veille → *Jamais*), sinon la borne et l'accès à distance s'arrêtent.

## 6. La borne de pointage

La borne est une simple page web : une tablette (ou un vieux PC/téléphone) sur le même Wi-Fi que le PC du bureau.

1. Trouvez l'adresse du PC du bureau : dans PowerShell, `ipconfig` → *Adresse IPv4* (ex. `192.168.1.20`).
   Donnez-lui une adresse fixe dans votre box pour qu'elle ne change pas.
2. Autorisez le port 8080 dans le pare-feu Windows du PC du bureau (PowerShell **en administrateur**) :

   ```powershell
   New-NetFirewallRule -DisplayName "Plantation Manager" -Direction Inbound -Protocol TCP -LocalPort 8080 -Profile Private -Action Allow
   ```

   Le réseau Wi-Fi doit être réglé en *Réseau privé* dans Windows.
3. Sur la tablette, ouvrez `http://192.168.1.20:8080` et connectez-vous avec le compte **borne**.
4. Ajoutez la page à l'écran d'accueil et mettez la tablette en plein écran / mode kiosque.

Avant le premier pointage, le patron crée dans le tableau de bord :
les **employés** (avec un **code PIN** de 4 à 6 chiffres et leur **taux horaire**), les **parcelles** et les **tâches**.

Chaque employé touche son nom sur la borne, tape son code, puis :
**J'arrive** → **Commencer une tâche** (tâche + parcelle) → … → **Je pars**.
Après 5 codes faux, l'employé est bloqué 5 minutes.

## 7. Accès à distance (Tailscale)

[Tailscale](https://tailscale.com/) crée un réseau privé chiffré entre le PC du bureau et les appareils du patron.
C'est gratuit pour un usage personnel, il n'y a rien à ouvrir sur la box, et l'application reste invisible depuis Internet.

1. Créez un compte Tailscale (avec le compte Google/Microsoft du patron).
2. Installez Tailscale sur le **PC du bureau** et connectez-vous.
3. Installez l'application Tailscale sur le **téléphone** (et/ou le PC portable) du patron, même compte.
4. Dans la console Tailscale (<https://login.tailscale.com/admin/machines>), notez le nom du PC du bureau
   (ex. `bureau`) et désactivez *Key expiry* pour cette machine, pour qu'elle ne se déconnecte pas.
5. Autorisez Tailscale dans le pare-feu du PC du bureau (PowerShell **en administrateur**) :

   ```powershell
   New-NetFirewallRule -DisplayName "Plantation Manager (Tailscale)" -Direction Inbound -Protocol TCP -LocalPort 8080 -RemoteAddress 100.64.0.0/10 -Action Allow
   ```

6. Depuis le téléphone, Tailscale activé, ouvrez `http://bureau:8080` (ou l'adresse `100.x.y.z` du PC).

**Ne redirigez jamais le port 8080 directement sur la box Internet.** Passez toujours par Tailscale.

Pour prendre la main sur **tout le PC du bureau** (pas seulement l'application), installez en plus
[RustDesk](https://rustdesk.com/) ou utilisez le Bureau à distance Windows (édition Pro) à travers Tailscale.

Conseil : installez aussi Tailscale sur la tablette borne. Le trafic de la borne (codes PIN) est alors chiffré,
même si quelqu'un d'autre utilise le Wi-Fi.

## 8. Sauvegardes

Toutes les données sont sur le PC du bureau : s'il tombe en panne sans sauvegarde, tout est perdu.

```powershell
.\scripts\sauvegarde.ps1
```

crée un fichier dans `Documents\Sauvegardes-Plantation` et garde les 30 derniers.
Planifiez-le chaque soir avec le Planificateur de tâches (comme à l'étape 5, déclencheur *Quotidien*),
et copiez régulièrement ce dossier ailleurs (clé USB, disque externe, OneDrive…).

Pour restaurer une sauvegarde (adaptez le chemin de `pg_restore.exe` : il est dans le dossier `bin`
de votre installation PostgreSQL, par exemple `C:\Program Files\bin` ou `C:\Program Files\PostgreSQL\18\bin`) :

```powershell
& "C:\Program Files\PostgreSQL\18\bin\pg_restore.exe" -U postgres -h localhost -d Plantation --clean "chemin\vers\plantation_AAAA-MM-JJ_HHMM.backup"
```

## 9. Régler les règles de la plantation

Dans `src/main/resources/application.properties` (puis recompilez) :

| Réglage | Par défaut | Rôle |
|---|---|---|
| `plantation.heure-debut-journee` | `07:00` | Heure normale d'arrivée |
| `plantation.tolerance-retard-minutes` | `10` | Minutes de tolérance avant de marquer un retard |
| `plantation.heures-normales-par-semaine` | `35` | Au-delà, les heures sont des heures sup |
| `plantation.majoration-heures-sup` | `0.25` | Majoration des heures sup (0.25 = +25 %) |

Vérifiez ces valeurs avec les règles qui s'appliquent à vos employés (convention collective, contrat).
