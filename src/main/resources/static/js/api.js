// Appels à l'API : ajoute le jeton CSRF et renvoie vers la connexion si la session a expiré.

function jetonCsrf() {
    const m = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]+)/);
    return m ? decodeURIComponent(m[1]) : '';
}

async function requete(methode, url, corps) {
    const options = { method: methode, headers: { 'X-XSRF-TOKEN': jetonCsrf() }, credentials: 'same-origin' };
    if (corps !== undefined) {
        options.headers['Content-Type'] = 'application/json';
        options.body = JSON.stringify(corps);
    }
    const reponse = await fetch(url, options);
    if (reponse.status === 401) {
        location.href = '/login.html';
        throw new Error('Session expirée, reconnectez-vous.');
    }
    if (reponse.status === 403) {
        throw new Error('Accès refusé.');
    }
    if (!reponse.ok) {
        let message = 'Erreur ' + reponse.status;
        try {
            const json = await reponse.json();
            message = json.erreur || message;
        } catch (e) { /* pas de JSON */ }
        throw new Error(message);
    }
    if (reponse.status === 204) return null;
    const type = reponse.headers.get('Content-Type') || '';
    return type.includes('json') ? reponse.json() : reponse.text();
}

const api = {
    get: url => requete('GET', url),
    post: (url, corps) => requete('POST', url, corps ?? {}),
    put: (url, corps) => requete('PUT', url, corps),
    patch: url => requete('PATCH', url),
    del: url => requete('DELETE', url),
};

function deconnexion() {
    const form = document.createElement('form');
    form.method = 'POST';
    form.action = '/logout';
    const jeton = document.createElement('input');
    jeton.type = 'hidden';
    jeton.name = '_csrf';
    jeton.value = jetonCsrf();
    form.appendChild(jeton);
    document.body.appendChild(form);
    form.submit();
}

// ── Petits utilitaires d'affichage ─────────────────────────────────────────

function esc(valeur) {
    if (valeur === null || valeur === undefined) return '';
    return String(valeur)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

function heure(h) {
    return h ? h.substring(0, 5) : '—';
}

function dateFr(d) {
    if (!d) return '';
    const [a, m, j] = d.split('-');
    return `${j}/${m}/${a}`;
}

function aujourdhui() {
    const d = new Date();
    return dateIso(d);
}

function dateIso(d) {
    const p = n => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
}

function heuresTexte(h) {
    if (h === null || h === undefined) return '—';
    const total = Math.round(h * 60);
    return `${Math.floor(total / 60)}h${String(total % 60).padStart(2, '0')}`;
}

function argent(v) {
    if (v === null || v === undefined) return '—';
    return v.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' €';
}

let minuteurMessage;
function afficherMessage(texte, erreur = false) {
    let zone = document.getElementById('message');
    if (!zone) {
        zone = document.createElement('div');
        zone.id = 'message';
        document.body.appendChild(zone);
    }
    zone.className = 'message' + (erreur ? ' erreur' : '');
    zone.textContent = texte;
    zone.hidden = false;
    clearTimeout(minuteurMessage);
    minuteurMessage = setTimeout(() => { zone.hidden = true; }, erreur ? 6000 : 3000);
}
