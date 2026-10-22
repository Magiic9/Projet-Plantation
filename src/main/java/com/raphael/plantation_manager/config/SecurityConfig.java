package com.raphael.plantation_manager.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/**
 * Deux comptes :
 *  - le patron (ROLE_ADMIN) : accès à tout ;
 *  - la borne (ROLE_BORNE) : uniquement la page de pointage et /api/borne.
 * Les mots de passe viennent des variables d'environnement ADMIN_PASSWORD et BORNE_PASSWORD.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService utilisateurs(PasswordEncoder encoder,
                                           @Value("${plantation.admin.username}") String patron,
                                           @Value("${plantation.admin.password}") String motDePassePatron,
                                           @Value("${plantation.borne.username}") String borne,
                                           @Value("${plantation.borne.password}") String motDePasseBorne) {
        if (motDePassePatron.isBlank() || motDePasseBorne.isBlank()) {
            throw new IllegalStateException("Définissez ADMIN_PASSWORD et BORNE_PASSWORD avant de lancer l'application.");
        }
        return new InMemoryUserDetailsManager(
                User.withUsername(patron).password(encoder.encode(motDePassePatron)).roles("ADMIN").build(),
                User.withUsername(borne).password(encoder.encode(motDePasseBorne)).roles("BORNE").build());
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Jeton CSRF dans un cookie XSRF-TOKEN, renvoyé par le JavaScript dans l'en-tête X-XSRF-TOKEN
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login.html", "/css/**", "/js/**", "/favicon.ico", "/error").permitAll()
                .requestMatchers("/borne.html", "/api/borne/**", "/api/moi").hasAnyRole("ADMIN", "BORNE")
                .anyRequest().hasRole("ADMIN"))
            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/login")
                .successHandler((request, response, auth) ->
                        response.sendRedirect(estBorne(auth) ? "/borne.html" : "/"))
                .failureUrl("/login.html?erreur")
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login.html?deconnexion"))
            // L'API répond 401 au lieu de rediriger vers la page de connexion
            .exceptionHandling(e -> e.defaultAuthenticationEntryPointFor(
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                PathPatternRequestMatcher.withDefaults().matcher("/api/**")));
        return http.build();
    }

    private static boolean estBorne(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_BORNE"));
    }

    /** Force la création du cookie CSRF pour que les pages puissent le lire dès le premier chargement. */
    private static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (token != null) {
                token.getToken();
            }
            chain.doFilter(request, response);
        }
    }
}
