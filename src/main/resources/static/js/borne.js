// Borne de pointage : l'employé touche son nom, tape son code, puis pointe.

const DELAI_INACTIVITE = 30000;

let employeChoisi = null;
let pin = '';
let tacheChoisie = null;
let parcelleChoisie = null;
let minuteurInactivite;
let minuteurListe;

// ── Horloge ────────────────────────────────────────────────────────────────

function majHorloge() {
    document.getElementById('horloge').textContent =
        new Date().toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
}
majHorloge();
setInterval(majHorloge, 10000);

api.get('/api/moi').then(moi => {
    if (moi.patron) document.getElementById('lien-patron').hidden = false;
}).catch(() => {});

// ── Écrans ─────────────────────────────────────────────────────────────────

function afficherEcran(nom) {
    document.querySelectorAll('.ecran').forEach(e => e.classList.toggle('visible', e.id === 'ecran-' + nom));
    clearTimeout(minuteurInactivite);
    if (nom !== 'accueil') minuteurInactivite = setTimeout(accueil, DELAI_INACTIVITE);
}

function accueil() {
    employeChoisi = null;
    pin = '';
    afficherEcran('accueil');
    chargerEmployes();
}

function initiales(e) {
    return ((e.prenom?.[0] ?? '') + (e.nom?.[0] ?? '')).toUpperCase();
}

async function chargerEmployes() {
    clearTimeout(minuteurListe);
    try {
        const liste = await api.get('/api/borne/employes');
        document.getElementById('liste-employes').innerHTML = liste.length ? liste.map(e => `
            <button class="tuile" data-id="${e.id}">
                <span class="point ${e.present ? 'present' : ''}"></span>
                ${e.photo ? `<img class="avatar" src="${esc(e.photo)}" alt="">` : `<span class="avatar">${esc(initiales(e))}</span>`}
                <span>${esc(e.prenom)} ${esc(e.nom)}</span>
            </button>`).join('') : '<p class="consigne">Aucun employé enregistré. Le patron doit les ajouter.</p>';
        document.querySelectorAll('#liste-employes .tuile').forEach(t => {
            t.addEventListener('click', () => choisirEmploye(liste.find(e => e.id === Number(t.dataset.id))));
        });
    } catch (e) {
        document.getElementById('liste-employes').innerHTML = `<p class="consigne">${esc(e.message)}</p>`;
    }
    // Rafraîchit les points verts régulièrement (et garde la session active)
    minuteurListe = setTimeout(() => {
        if (document.getElementById('ecran-accueil').classList.contains('visible')) chargerEmployes();
    }, 30000);
}

// ── Code PIN ───────────────────────────────────────────────────────────────

function choisirEmploye(e) {
    employeChoisi = e;
    pin = '';
    document.getElementById('pin-nom').textContent = `${e.prenom} ${e.nom}`;
    document.getElementById('pin-erreur').textContent = e.pinDefini ? '' : "Vous n'avez pas encore de code. Voyez le patron.";
    majPin();
    afficherEcran('pin');
}

function majPin() {
    document.getElementById('pin-affichage').textContent = '•'.repeat(pin.length) || ' ';
}

document.querySelectorAll('[data-chiffre]').forEach(b => b.addEventListener('click', () => {
    if (pin.length < 6) pin += b.dataset.chiffre;
    document.getElementById('pin-erreur').textContent = '';
    majPin();
    afficherEcran('pin');
}));

document.getElementById('pin-effacer').addEventListener('click', () => {
    pin = pin.slice(0, -1);
    majPin();
});

document.getElementById('pin-valider').addEventListener('click', async () => {
    if (pin.length < 4) {
        document.getElementById('pin-erreur').textContent = 'Le code fait au moins 4 chiffres.';
        return;
    }
    try {
        const statut = await api.post('/api/borne/identifier', { employeId: employeChoisi.id, pin });
        afficherActions(statut);
    } catch (e) {
        pin = '';
        majPin();
        document.getElementById('pin-erreur').textContent = e.message;
    }
});

// ── Actions ────────────────────────────────────────────────────────────────

function afficherActions(statut) {
    document.getElementById('actions-nom').textContent = `Bonjour ${statut.prenom} !`;
    document.getElementById('actions-erreur').textContent = '';

    let situation = '';
    if (!statut.present) {
        situation = '<p class="situation">Vous n\'êtes pas encore pointé aujourd\'hui.</p>';
    } else {
        situation = `<p class="situation">Arrivé à ${heure(statut.arrivee)}</p>`;
        situation += statut.tacheEnCours
            ? `<p class="situation">En cours : <strong>${esc(statut.tacheEnCours)}</strong> · ${esc(statut.parcelleEnCours)} depuis ${heure(statut.debutTache)}</p>`
            : '<p class="situation">Aucune tâche en cours.</p>';
    }
    document.getElementById('actions-situation').innerHTML = situation;

    const boutons = [];
    if (!statut.present) {
        boutons.push('<button class="arrivee" data-action="arrivee">J\'arrive</button>');
    } else {
        boutons.push(`<button class="tache" data-action="choisir-tache">${statut.tacheEnCours ? 'Changer de tâche' : 'Commencer une tâche'}</button>`);
        if (statut.tacheEnCours) boutons.push('<button data-action="terminer">Terminer la tâche</button>');
        boutons.push('<button class="depart" data-action="depart">Je pars</button>');
    }
    const zone = document.getElementById('actions-boutons');
    zone.innerHTML = boutons.join('');
    zone.querySelectorAll('button').forEach(b => b.addEventListener('click', () => action(b.dataset.action)));
    afficherEcran('actions');
}

async function action(nom) {
    const corps = { employeId: employeChoisi.id, pin };
    try {
        if (nom === 'arrivee') {
            const s = await api.post('/api/borne/arrivee', corps);
            confirmer(`Bonjour ${s.prenom}, arrivée pointée à ${heure(s.arrivee)}.`);
        } else if (nom === 'depart') {
            const s = await api.post('/api/borne/depart', corps);
            confirmer(`Au revoir ${s.prenom}, départ pointé. Bonne soirée !`);
        } else if (nom === 'terminer') {
            const s = await api.post('/api/borne/travail/terminer', corps);
            afficherActions(s);
        } else if (nom === 'choisir-tache') {
            await choisirTache();
        }
    } catch (e) {
        document.getElementById('actions-erreur').textContent = e.message;
    }
}

// ── Choix de tâche ─────────────────────────────────────────────────────────

async function choisirTache() {
    tacheChoisie = null;
    parcelleChoisie = null;
    document.getElementById('tache-erreur').textContent = '';
    const [taches, parcelles] = await Promise.all([api.get('/api/borne/taches'), api.get('/api/borne/parcelles')]);
    remplirChoix('liste-taches', taches, id => { tacheChoisie = id; });
    remplirChoix('liste-parcelles', parcelles, id => { parcelleChoisie = id; });
    if (parcelles.length === 1) document.querySelector('#liste-parcelles .tuile').click();
    afficherEcran('tache');
}

function remplirChoix(zoneId, elements, surChoix) {
    const zone = document.getElementById(zoneId);
    zone.innerHTML = elements.length
        ? elements.map(x => `<button class="tuile" data-id="${x.id}">${esc(x.nom)}</button>`).join('')
        : '<p>Aucun choix disponible. Le patron doit en ajouter.</p>';
    zone.querySelectorAll('.tuile').forEach(t => t.addEventListener('click', () => {
        zone.querySelectorAll('.tuile').forEach(x => x.classList.remove('choisie'));
        t.classList.add('choisie');
        surChoix(Number(t.dataset.id));
        afficherEcran('tache');
    }));
}

document.getElementById('tache-valider').addEventListener('click', async () => {
    if (!tacheChoisie || !parcelleChoisie) {
        document.getElementById('tache-erreur').textContent = 'Choisissez une tâche et une parcelle.';
        return;
    }
    try {
        const s = await api.post('/api/borne/travail/demarrer',
            { employeId: employeChoisi.id, pin, tacheId: tacheChoisie, parcelleId: parcelleChoisie });
        confirmer(`${s.prenom} : ${s.tacheEnCours} sur ${s.parcelleEnCours}, c'est parti !`);
    } catch (e) {
        document.getElementById('tache-erreur').textContent = e.message;
    }
});

// ── Confirmation ───────────────────────────────────────────────────────────

function confirmer(texte) {
    pin = '';
    document.getElementById('confirmation-texte').textContent = texte;
    afficherEcran('confirmation');
    clearTimeout(minuteurInactivite);
    minuteurInactivite = setTimeout(accueil, 3500);
}

accueil();
