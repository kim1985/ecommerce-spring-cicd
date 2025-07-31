package com.myecom.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Servizio per gestire i token JWT nell'applicazione e-commerce.
 *
 * JWT (JSON Web Token) è uno standard per l'autenticazione stateless:
 * - Il server genera un token firmato quando l'utente fa login
 * - Il client invia questo token in ogni richiesta successiva
 * - Il server valida il token senza dover consultare il database
 *
 * Vantaggi: scalabile, stateless, sicuro se implementato correttamente
 */
@Service
public class JwtService {

    // Chiave segreta per firmare i token JWT
    // In produzione questa deve essere una variabile d'ambiente sicura
    @Value("${jwt.secret:mySecretKey123456789012345678901234567890}")
    private String secretKey;

    // Durata del token in millisecondi (default: 24 ore)
    // Dopo questo tempo il token scade e l'utente deve rifare login
    @Value("${jwt.expiration:86400000}")
    private Long jwtExpiration;

    /**
     * Metodo principale: genera un token JWT per l'utente che ha fatto login.
     *
     * Il token contiene l'email dell'utente e la data di scadenza.
     * Viene firmato con la nostra chiave segreta per garantire che non sia stato modificato.
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Versione avanzata che permette di aggiungere informazioni extra nel token.
     *
     * Ad esempio potresti aggiungere il ruolo utente o altre informazioni
     * che vuoi avere disponibili senza interrogare il database.
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    /**
     * Costruisce fisicamente il token JWT usando la libreria JJWT.
     *
     * Un token JWT è formato da tre parti separate da punti:
     * - Header: tipo di token e algoritmo di firma
     * - Payload: i dati (claims) che vogliamo memorizzare
     * - Signature: firma per verificare che non sia stato modificato
     */
    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        return Jwts.builder()
                .claims(extraClaims)                                       // Dati personalizzati
                .subject(userDetails.getUsername())                        // Email dell'utente
                .issuedAt(new Date(System.currentTimeMillis()))           // Quando è stato creato
                .expiration(new Date(System.currentTimeMillis() + expiration)) // Quando scade
                .signWith(getSignInKey(), Jwts.SIG.HS256)                 // Firma con algoritmo HS256
                .compact();                                                // Crea la stringa finale
    }

    /**
     * Estrae l'email dell'utente dal token JWT.
     *
     * Questo è utile quando ricevi una richiesta con token e vuoi sapere
     * chi è l'utente senza dover decodificare manualmente il token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Estrae la data di scadenza dal token per verificare se è ancora valido.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Metodo generico per estrarre qualsiasi informazione dal token.
     *
     * Usiamo una Function per dire quale campo vogliamo estrarre.
     * È un pattern comune in Java per rendere il codice più flessibile.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Decodifica il token JWT e estrae tutti i dati contenuti.
     *
     * Se il token è stato modificato o la firma non corrisponde,
     * questo metodo lancerà un'eccezione per sicurezza.
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSignInKey())                     // Usa la nostra chiave per verificare
                    .build()
                    .parseSignedClaims(token)                       // Decodifica e verifica
                    .getPayload();                                  // Restituisce i dati
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Token JWT non valido", e);
        }
    }

    /**
     * Verifica che il token sia valido per l'utente specificato.
     *
     * Controlla due cose:
     * 1. Il token contiene l'email corretta dell'utente
     * 2. Il token non è scaduto
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;                                           // Se c'è qualsiasi errore, il token non è valido
        }
    }

    /**
     * Verifica se il token è scaduto confrontando con l'ora attuale.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Crea la chiave crittografica per firmare e verificare i token.
     *
     * Usiamo HMAC-SHA256 che è sicuro e veloce per la maggior parte delle applicazioni.
     * La chiave deve essere lunga almeno 256 bit per essere sicura.
     */
    private SecretKey getSignInKey() {
        byte[] keyBytes = secretKey.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Metodi di compatibilità con il codice esistente

    /**
     * Valida solo il formato del token senza controllare l'utente.
     * Utile per controlli rapidi prima di estrarre i dati.
     */
    public boolean isValidToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Estrae l'email dal token (alias per extractUsername).
     * Mantiene compatibilità con il codice che usava il JwtService precedente.
     */
    public String getEmailFromToken(String token) {
        try {
            return extractUsername(token);
        } catch (Exception e) {
            return null;
        }
    }
}