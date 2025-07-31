package com.myecom.config;

import com.myecom.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro di sicurezza che intercetta ogni richiesta HTTP per verificare il token JWT.
 *
 * Come funziona:
 * 1. Ogni richiesta passa attraverso questo filtro PRIMA di arrivare ai controller
 * 2. Se trova un token JWT valido nell'header Authorization, autentica l'utente
 * 3. Se non trova token o è invalido, la richiesta continua senza autenticazione
 * 4. Gli endpoint protetti poi rifiuteranno richieste non autenticate
 *
 * Questo è il pattern standard per autenticazione JWT in Spring Boot.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Cerca l'header "Authorization" nella richiesta HTTP
        final String authHeader = request.getHeader("Authorization");

        // Se non c'è header Authorization o non inizia con "Bearer ",
        // significa che non c'è token JWT, quindi passa al prossimo filtro
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Estrae il token JWT rimuovendo il prefisso "Bearer "
        final String jwt = authHeader.substring(7);

        try {
            // Estrae l'email dell'utente dal token JWT
            final String userEmail = jwtService.extractUsername(jwt);

            // Se c'è un'email nel token e l'utente non è già autenticato in questa richiesta
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Carica i dettagli completi dell'utente dal database
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                // Verifica che il token sia valido per questo utente specifico
                if (jwtService.isTokenValid(jwt, userDetails)) {

                    // Crea un oggetto di autenticazione di Spring Security
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,                    // L'utente autenticato
                                    null,                          // Non serve la password qui
                                    userDetails.getAuthorities()   // I ruoli/permessi dell'utente
                            );

                    // Aggiunge informazioni sulla richiesta HTTP (IP, user agent, etc.)
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Dice a Spring Security che questo utente è autenticato per questa richiesta
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Se il token non è valido, non autentica l'utente ma non blocca la richiesta
            // Gli endpoint protetti poi rifiuteranno richieste non autenticate
            logger.debug("Token JWT non valido: " + e.getMessage());
        }

        // Continua con il prossimo filtro nella catena di sicurezza
        filterChain.doFilter(request, response);
    }
}