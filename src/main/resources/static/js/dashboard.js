// Tableau de bord du patron.

let employes = [];
let parcelles = [];
let taches = [];
let minuteurJour;

// ── Navigation entre onglets ───────────────────────────────────────────────

const chargeurs = {
    jour: chargerJour,
    employes: chargerEmployes,
    pointages: () => { chargerIncomplets(); chargerPointages(); },
    salaires: chargerSalaires,
    plantation: chargerPlantation,
};

document.querySelectorAll('nav.onglets button').forEach(bouton => {
    bouton.addEventListener('click', () => afficherVue(bouton.dataset.vue));
});

function afficherVue(nom) {
    document.querySelectorAll('nav.onglets button').forEach(b => b.classList.toggle('actif', b.dataset.vue === nom));
    document.querySelectorAll('section.vue').forEach(s => s.classList.toggle('visible', s.id === 'vue-' + nom));
    chargeurs[nom]().catch(e => afficherMessage(e.message, true));
}

async function chargerReferentiels() {
    [employes, parcelles, taches] = await Promise.all([
        api.get('/api/employes'), api.get('/api/parcelles'), api.get('/api/taches'),
    ]);
    const options = employes
        .slice().sort((a, b) => a.nom.localeCompare(b.nom))
        .map(e => `<option value="${e.id}">${esc(e.nom)} ${esc(e.prenom)}${e.actif ? '' : ' (inactif)'}</option>`)
        .join('');
    document.getElementById('pointages-employe').innerHTML = '<option value="">Tous</option>' + options;
    document.querySelector('#form-pointage [name=employeId]').innerHTML = options;
}

function nomEmploye(e) {
    return e ? `${esc(e.prenom)} ${esc(e.nom)}` : '';
}

// ── Aujourd'hui ────────────────────────────────────────────────────────────

document.getElementById('jour-date').value = aujourdhui();
document.getElementById('jour-date').addEventListener('change', () => chargerJour().catch(e => afficherMessage(e.message, true)));

const libellesStatut = { PRESENT: 'Présent', PARTI: 'Parti', ABSENT: 'Absent', INCOMPLET: 'Départ non pointé' };

async function chargerJour() {
    const date = document.getElementById('jour-date').value || aujourdhui();
    const [presence, travaux, incomplets] = await Promise.all([
        api.get('/api/presence?date=' + date),
        api.get('/api/travaux/date?date=' + date),
        api.get('/api/pointages/incomplets'),
    ]);

    const compte = statut => presence.filter(p => p.statut === statut).length;
    const retards = presence.filter(p => p.enRetard).length;
    document.getElementById('jour-compteurs').innerHTML = `
        <div class="compteur vert"><div class="valeur">${compte('PRESENT')}</div><div class="libelle">Sur place</div></div>
        <div class="compteur"><div class="valeur">${compte('PARTI')}</div><div class="libelle">Partis</div></div>
        <div class="compteur rouge"><div class="valeur">${compte('ABSENT')}</div><div class="libelle">Absents</div></div>
        <div class="compteur orange"><div class="valeur">${retards}</div><div class="libelle">En retard</div></div>`;

    document.getElementById('jour-alerte').innerHTML = incomplets.length
        ? `<div class="alerte">⚠ ${incomplets.length} pointage(s) d'un jour passé sans départ : les heures ne comptent pas pour la paie.
             <button class="petit" onclick="afficherVue('pointages')">Corriger</button></div>`
        : '';

    document.getElementById('jour-presence').innerHTML = presence.length ? presence.map(p => `
        <tr>
            <td>${esc(p.prenom)} ${esc(p.nom)}</td>
            <td><span class="pastille ${p.statut}">${libellesStatut[p.statut]}</span>
                ${p.enRetard ? '<span class="pastille retard">Retard</span>' : ''}</td>
            <td>${heure(p.arrivee)}</td>
            <td>${heure(p.depart)}</td>
            <td class="nombre">${p.statut === 'ABSENT' ? '—' : heuresTexte(p.heuresPresence)}</td>
            <td>${p.tacheEnCours ? esc(p.tacheEnCours) + ' · ' + esc(p.parcelleEnCours) : ''}</td>
        </tr>`).join('') : '<tr><td colspan="6" class="vide">Aucun employé actif. Ajoutez-en dans l\'onglet Employés.</td></tr>';

    document.getElementById('jour-travaux').innerHTML = travaux.length ? travaux
        .sort((a, b) => a.heureDebut.localeCompare(b.heureDebut))
        .map(t => `
        <tr>
            <td>${nomEmploye(t.employe)}</td>
            <td>${esc(t.tache.nom)}</td>
            <td>${esc(t.parcelle.nom)}</td>
            <td>${heure(t.heureDebut)}</td>
            <td>${t.heureFin ? heure(t.heureFin) : '<span class="pastille PRESENT">En cours</span>'}</td>
            <td class="nombre">${t.heureFin ? heuresTexte(duree(t.heureDebut, t.heureFin)) : ''}</td>
        </tr>`).join('') : '<tr><td colspan="6" class="vide">Aucune tâche pointée ce jour-là.</td></tr>';

    // Rafraîchissement automatique quand on regarde aujourd'hui
    clearTimeout(minuteurJour);
    if (date === aujourdhui()) {
        minuteurJour = setTimeout(() => {
            if (document.getElementById('vue-jour').classList.contains('visible')) chargerJour().catch(() => {});
        }, 60000);
    }
}

function duree(debut, fin) {
    const minutes = t => { const [h, m] = t.split(':').map(Number); return h * 60 + m; };
    return Math.max(minutes(fin) - minutes(debut), 0) / 60;
}

// ── Employés ───────────────────────────────────────────────────────────────

document.getElementById('employes-recherche').addEventListener('input', afficherEmployes);
document.getElementById('employes-inactifs').addEventListener('change', afficherEmployes);

async function chargerEmployes() {
    await chargerReferentiels();
    afficherEmployes();
}

function afficherEmployes() {
    const terme = document.getElementById('employes-recherche').value.trim().toLowerCase();
    const inactifs = document.getElementById('employes-inactifs').checked;
    const liste = employes
        .filter(e => inactifs || e.actif)
        .filter(e => !terme || (e.nom + ' ' + e.prenom).toLowerCase().includes(terme))
        .sort((a, b) => a.nom.localeCompare(b.nom));
    document.getElementById('employes-liste').innerHTML = liste.length ? liste.map(e => `
        <tr class="${e.actif ? '' : 'inactif'}">
            <td>${esc(e.nom)}</td>
            <td>${esc(e.prenom)}</td>
            <td>${dateFr(e.dateNaissance)}</td>
            <td class="nombre">${e.tauxHoraire != null ? argent(e.tauxHoraire) : '<span class="pastille INCOMPLET">À définir</span>'}</td>
            <td>${e.pinDefini ? '✔ Défini' : '<span class="pastille INCOMPLET">Aucun</span>'}</td>
            <td>${e.actif ? 'Actif' : 'Inactif'}</td>
            <td class="actions">
                <button class="petit" onclick="ouvrirEmploye(${e.id})">Modifier</button>
                ${e.actif ? `<button class="petit danger" onclick="desactiverEmploye(${e.id})">Désactiver</button>` : ''}
            </td>
        </tr>`).join('') : '<tr><td colspan="7" class="vide">Aucun employé.</td></tr>';
}

function ouvrirEmploye(id) {
    const e = employes.find(x => x.id === id);
    const form = document.getElementById('form-employe');
    form.reset();
    document.getElementById('dlg-employe-titre').textContent = e ? 'Modifier ' + e.prenom + ' ' + e.nom : 'Nouvel employé';
    form.cle.value = e ? e.id : '';
    form.nom.value = e?.nom ?? '';
    form.prenom.value = e?.prenom ?? '';
    form.dateNaissance.value = e?.dateNaissance ?? '';
    form.tauxHoraire.value = e?.tauxHoraire ?? '';
    form.photo.value = e?.photo ?? '';
    form.actif.checked = e ? e.actif : true;
    form.codePin.placeholder = e?.pinDefini ? 'Laisser vide pour garder le code actuel' : '';
    document.getElementById('dlg-employe').showModal();
}

document.getElementById('form-employe').addEventListener('submit', async ev => {
    ev.preventDefault();
    const form = ev.target;
    const corps = {
        nom: form.nom.value.trim(),
        prenom: form.prenom.value.trim(),
        dateNaissance: form.dateNaissance.value || null,
        tauxHoraire: form.tauxHoraire.value ? Number(form.tauxHoraire.value) : null,
        photo: form.photo.value.trim() || null,
        actif: form.actif.checked,
        codePin: form.codePin.value.trim() || null,
    };
    try {
        if (form.cle.value) await api.put('/api/employes/' + form.cle.value, corps);
        else await api.post('/api/employes', corps);
        document.getElementById('dlg-employe').close();
        afficherMessage('Employé enregistré.');
        await chargerEmployes();
    } catch (e) {
        afficherMessage(e.message, true);
    }
});

async function desactiverEmploye(id) {
    const e = employes.find(x => x.id === id);
    if (!confirm(`Désactiver ${e.prenom} ${e.nom} ? Il ne pourra plus pointer, mais son historique est conservé.`)) return;
    try {
        await api.patch(`/api/employes/${id}/desactiver`);
        afficherMessage('Employé désactivé.');
        await chargerEmployes();
    } catch (err) {
        afficherMessage(err.message, true);
    }
}

// ── Pointages ──────────────────────────────────────────────────────────────

let pointages = [];

(function initPeriodePointages() {
    const fin = new Date();
    const debut = new Date();
    debut.setDate(debut.getDate() - 6);
    document.getElementById('pointages-debut').value = dateIso(debut);
    document.getElementById('pointages-fin').value = dateIso(fin);
})();

async function chargerIncomplets() {
    const incomplets = await api.get('/api/pointages/incomplets');
    document.getElementById('pointages-incomplets').innerHTML = incomplets.length ? `
        <div class="alerte"><strong>Départs oubliés à corriger :</strong>
            ${incomplets.map(p => `<div>${dateFr(p.date)} — ${nomEmploye(p.employe)}, arrivé à ${heure(p.heureArrivee)}
                <button class="petit" onclick="ouvrirPointage(${p.id}, true)">Corriger</button></div>`).join('')}
        </div>` : '';
    incomplets.forEach(p => { if (!pointages.some(x => x.id === p.id)) pointages.push(p); });
}

async function chargerPointages() {
    if (!employes.length) await chargerReferentiels();
    const debut = document.getElementById('pointages-debut').value;
    const fin = document.getElementById('pointages-fin').value;
    const employeId = document.getElementById('pointages-employe').value;
    let url = `/api/pointages?debut=${debut}&fin=${fin}`;
    if (employeId) url += '&employeId=' + employeId;
    const liste = await api.get(url);
    pointages = pointages.filter(p => !p.heureDepart).concat(liste);

    document.getElementById('pointages-liste').innerHTML = liste.length ? liste.slice().reverse().map(p => `
        <tr>
            <td>${dateFr(p.date)}</td>
            <td>${nomEmploye(p.employe)}</td>
            <td>${heure(p.heureArrivee)}</td>
            <td>${p.heureDepart ? heure(p.heureDepart) : '<span class="pastille INCOMPLET">Non pointé</span>'}</td>
            <td class="nombre">${p.heureDepart ? heuresTexte(duree(p.heureArrivee, p.heureDepart)) : ''}</td>
            <td class="actions">
                <button class="petit" onclick="ouvrirPointage(${p.id})">Modifier</button>
                <button class="petit danger" onclick="supprimerPointage(${p.id})">Supprimer</button>
            </td>
        </tr>`).join('') : '<tr><td colspan="6" class="vide">Aucun pointage sur cette période.</td></tr>';
}

async function ouvrirPointage(id) {
    if (!employes.length) await chargerReferentiels();
    const p = pointages.find(x => x.id === id);
    const form = document.getElementById('form-pointage');
    form.reset();
    document.getElementById('dlg-pointage-titre').textContent = p ? 'Corriger le pointage' : 'Nouveau pointage';
    form.cle.value = p ? p.id : '';
    form.employeId.value = p ? p.employe.id : (employes.find(e => e.actif)?.id ?? '');
    form.date.value = p ? p.date : aujourdhui();
    form.heureArrivee.value = p ? heure(p.heureArrivee) : '';
    form.heureDepart.value = p?.heureDepart ? heure(p.heureDepart) : '';
    document.getElementById('dlg-pointage').showModal();
}

document.getElementById('form-pointage').addEventListener('submit', async ev => {
    ev.preventDefault();
    const form = ev.target;
    const corps = {
        employeId: Number(form.employeId.value),
        date: form.date.value,
        heureArrivee: form.heureArrivee.value,
        heureDepart: form.heureDepart.value || null,
    };
    try {
        if (form.cle.value) await api.put('/api/pointages/' + form.cle.value, corps);
        else await api.post('/api/pointages', corps);
        document.getElementById('dlg-pointage').close();
        afficherMessage('Pointage enregistré.');
        await Promise.all([chargerIncomplets(), chargerPointages()]);
    } catch (e) {
        afficherMessage(e.message, true);
    }
});

async function supprimerPointage(id) {
    if (!confirm('Supprimer ce pointage ?')) return;
    try {
        await api.del('/api/pointages/' + id);
        afficherMessage('Pointage supprimé.');
        await Promise.all([chargerIncomplets(), chargerPointages()]);
    } catch (e) {
        afficherMessage(e.message, true);
    }
}

// ── Salaires ───────────────────────────────────────────────────────────────

(function initPeriodeSalaires() {
    const d = new Date();
    document.getElementById('salaires-debut').value = dateIso(new Date(d.getFullYear(), d.getMonth(), 1));
    document.getElementById('salaires-fin').value = dateIso(new Date(d.getFullYear(), d.getMonth() + 1, 0));
})();

function periodeSalaires() {
    return `debut=${document.getElementById('salaires-debut').value}&fin=${document.getElementById('salaires-fin').value}`;
}

async function chargerSalaires() {
    const fiches = await api.get('/api/salaires?' + periodeSalaires());
    const sansTaux = fiches.filter(f => f.tauxHoraire == null && f.heuresTotal > 0);
    const incomplets = fiches.reduce((n, f) => n + f.pointagesIncomplets, 0);
    let alertes = '';
    if (sansTaux.length) alertes += `<div class="alerte">⚠ Taux horaire manquant pour : ${sansTaux.map(f => esc(f.prenom) + ' ' + esc(f.nom)).join(', ')}.</div>`;
    if (incomplets) alertes += `<div class="alerte">⚠ ${incomplets} pointage(s) sans départ sur la période ne sont pas comptés.
        <button class="petit" onclick="afficherVue('pointages')">Corriger</button></div>`;
    document.getElementById('salaires-alerte').innerHTML = alertes;

    const somme = cle => fiches.reduce((s, f) => s + f[cle], 0);
    document.getElementById('salaires-liste').innerHTML = fiches.length ? fiches.map(f => `
        <tr>
            <td>${esc(f.prenom)} ${esc(f.nom)}</td>
            <td class="nombre">${f.tauxHoraire != null ? argent(f.tauxHoraire) : '—'}</td>
            <td class="nombre">${f.joursTravailles}</td>
            <td class="nombre">${heuresTexte(f.heuresTotal)}</td>
            <td class="nombre">${f.heuresSup ? heuresTexte(f.heuresSup) : '—'}</td>
            <td class="nombre">${argent(f.montantNormal)}</td>
            <td class="nombre">${argent(f.montantSup)}</td>
            <td class="nombre"><strong>${argent(f.montantTotal)}</strong></td>
        </tr>`).join('') + `
        <tr class="total">
            <td>Total</td><td></td><td></td>
            <td class="nombre">${heuresTexte(somme('heuresTotal'))}</td>
            <td class="nombre">${heuresTexte(somme('heuresSup'))}</td>
            <td class="nombre">${argent(somme('montantNormal'))}</td>
            <td class="nombre">${argent(somme('montantSup'))}</td>
            <td class="nombre">${argent(somme('montantTotal'))}</td>
        </tr>` : '<tr><td colspan="8" class="vide">Aucun employé.</td></tr>';
}

function exporterSalaires() {
    location.href = '/api/salaires/export?' + periodeSalaires();
}

// ── Parcelles & tâches ─────────────────────────────────────────────────────

async function chargerPlantation() {
    await chargerReferentiels();
    const ligneActions = (type, x) => `
        <td class="actions">
            <button class="petit" onclick="ouvrir${type}(${x.id})">Modifier</button>
            ${x.actif ? `<button class="petit danger" onclick="desactiver${type}(${x.id})">Désactiver</button>` : ''}
        </td>`;
    document.getElementById('parcelles-liste').innerHTML = parcelles.length ? parcelles.map(p => `
        <tr class="${p.actif ? '' : 'inactif'}">
            <td>${esc(p.nom)}${p.actif ? '' : ' (inactive)'}</td>
            <td class="nombre">${p.surface != null ? p.surface.toLocaleString('fr-FR') : ''}</td>
            ${ligneActions('Parcelle', p)}
        </tr>`).join('') : '<tr><td colspan="3" class="vide">Aucune parcelle.</td></tr>';
    document.getElementById('taches-liste').innerHTML = taches.length ? taches.map(t => `
        <tr class="${t.actif ? '' : 'inactif'}">
            <td>${esc(t.nom)}${t.actif ? '' : ' (inactive)'}</td>
            <td>${esc(t.description)}</td>
            ${ligneActions('Tache', t)}
        </tr>`).join('') : '<tr><td colspan="3" class="vide">Aucune tâche.</td></tr>';
}

function ouvrirParcelle(id) {
    const p = parcelles.find(x => x.id === id);
    const form = document.getElementById('form-parcelle');
    form.reset();
    form.cle.value = p ? p.id : '';
    form.nom.value = p?.nom ?? '';
    form.surface.value = p?.surface ?? '';
    form.actif.checked = p ? p.actif : true;
    document.getElementById('dlg-parcelle').showModal();
}

function ouvrirTache(id) {
    const t = taches.find(x => x.id === id);
    const form = document.getElementById('form-tache');
    form.reset();
    form.cle.value = t ? t.id : '';
    form.nom.value = t?.nom ?? '';
    form.description.value = t?.description ?? '';
    form.actif.checked = t ? t.actif : true;
    document.getElementById('dlg-tache').showModal();
}

async function enregistrer(url, id, corps, dialogue) {
    try {
        if (id) await api.put(url + '/' + id, corps);
        else await api.post(url, corps);
        document.getElementById(dialogue).close();
        afficherMessage('Enregistré.');
        await chargerPlantation();
    } catch (e) {
        afficherMessage(e.message, true);
    }
}

document.getElementById('form-parcelle').addEventListener('submit', ev => {
    ev.preventDefault();
    const f = ev.target;
    enregistrer('/api/parcelles', f.cle.value, {
        nom: f.nom.value.trim(),
        surface: f.surface.value ? Number(f.surface.value) : null,
        actif: f.actif.checked,
    }, 'dlg-parcelle');
});

document.getElementById('form-tache').addEventListener('submit', ev => {
    ev.preventDefault();
    const f = ev.target;
    enregistrer('/api/taches', f.cle.value, {
        nom: f.nom.value.trim(),
        description: f.description.value.trim() || null,
        actif: f.actif.checked,
    }, 'dlg-tache');
});

async function desactiver(url, message) {
    if (!confirm(message)) return;
    try {
        await api.patch(url);
        await chargerPlantation();
    } catch (e) {
        afficherMessage(e.message, true);
    }
}

function desactiverParcelle(id) {
    desactiver(`/api/parcelles/${id}/desactiver`, 'Désactiver cette parcelle ? Elle ne sera plus proposée sur la borne.');
}

function desactiverTache(id) {
    desactiver(`/api/taches/${id}/desactiver`, 'Désactiver cette tâche ? Elle ne sera plus proposée sur la borne.');
}

// ── Démarrage ──────────────────────────────────────────────────────────────

chargerReferentiels()
    .then(chargerJour)
    .catch(e => afficherMessage(e.message, true));
