package com.raphael.plantation_manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/** Vérifie qui a accès à quoi, avec de vraies requêtes HTTP. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecuriteTest {

    @Autowired
    private Environment environment;

    private String base;
    private CookieManager cookies;
    private HttpClient client;

    @BeforeEach
    void preparer() {
        base = "http://localhost:" + environment.getProperty("local.server.port");
        cookies = new CookieManager();
        client = HttpClient.newBuilder().cookieHandler(cookies).followRedirects(HttpClient.Redirect.NEVER).build();
    }

    private HttpResponse<String> get(String chemin) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + chemin)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private String csrf() {
        return cookies.getCookieStore().getCookies().stream()
                .filter(c -> c.getName().equals("XSRF-TOKEN")).map(HttpCookie::getValue).findFirst().orElse("");
    }

    private HttpResponse<String> connexion(String utilisateur, String motDePasse) throws Exception {
        get("/login.html"); // récupère le cookie CSRF
        String formulaire = "username=" + URLEncoder.encode(utilisateur, StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode(motDePasse, StandardCharsets.UTF_8)
                + "&_csrf=" + URLEncoder.encode(csrf(), StandardCharsets.UTF_8);
        return client.send(HttpRequest.newBuilder(URI.create(base + "/login"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(formulaire)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void apiFermeeSansConnexion() throws Exception {
        assertEquals(401, get("/api/employes").statusCode());
        assertEquals(401, get("/api/borne/employes").statusCode());
    }

    @Test
    void pageDeConnexionAccessible() throws Exception {
        assertEquals(200, get("/login.html").statusCode());
    }

    @Test
    void mauvaisMotDePasse() throws Exception {
        HttpResponse<String> r = connexion("patron", "faux");
        assertTrue(r.headers().firstValue("Location").orElse("").contains("/login.html?erreur"));
    }

    @Test
    void lePatronAccedeATout() throws Exception {
        HttpResponse<String> r = connexion("patron", "patron-demo");
        assertEquals(302, r.statusCode());
        assertTrue(r.headers().firstValue("Location").orElse("").endsWith("/"));
        assertEquals(200, get("/api/employes").statusCode());
        assertEquals(200, get("/api/borne/employes").statusCode());
        assertEquals(200, get("/api/salaires?debut=2026-10-01&fin=2026-10-31").statusCode());
    }

    @Test
    void laBorneNAccedeQuALaBorne() throws Exception {
        HttpResponse<String> r = connexion("borne", "borne-demo");
        assertTrue(r.headers().firstValue("Location").orElse("").endsWith("/borne.html"));
        assertEquals(200, get("/api/borne/employes").statusCode());
        assertEquals(200, get("/borne.html").statusCode());
        assertEquals(403, get("/api/employes").statusCode());
        assertEquals(403, get("/api/salaires?debut=2026-10-01&fin=2026-10-31").statusCode());
    }

    @Test
    void postSansJetonCsrfRefuse() throws Exception {
        connexion("patron", "patron-demo");
        HttpResponse<String> r = client.send(HttpRequest.newBuilder(URI.create(base + "/api/parcelles"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"nom\":\"Test\"}")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(403, r.statusCode());

        HttpResponse<String> ok = client.send(HttpRequest.newBuilder(URI.create(base + "/api/parcelles"))
                        .header("Content-Type", "application/json")
                        .header("X-XSRF-TOKEN", csrf())
                        .POST(HttpRequest.BodyPublishers.ofString("{\"nom\":\"Test\"}")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, ok.statusCode());
    }
}
