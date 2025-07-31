package com.myecom.service;

import com.myecom.dto.auth.AuthResponse;
import com.myecom.dto.auth.LoginRequest;
import com.myecom.dto.auth.RegisterRequest;
import com.myecom.dto.auth.UserResponse;
import com.myecom.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService; // Servizio per generare e validare token JWT

    /**
     * Registra un nuovo utente nel sistema e gli fornisce subito un token JWT.
     *
     * Flusso:
     * 1. UserService crea l'utente nel database
     * 2. Recuperiamo l'utente appena creato
     * 3. Generiamo un token JWT per questo utente
     * 4. Restituiamo token + dati utente
     */
    public AuthResponse register(RegisterRequest request) {
        UserResponse userResponse = userService.registerUser(request);

        // Recupera l'utente dal database per generare il token
        // Serve l'oggetto User completo (con password hash) per Spring Security
        User user = userService.findByEmail(userResponse.getEmail())
                .orElseThrow(() -> new IllegalStateException("Errore nel recupero utente registrato"));

        // Genera il token JWT reale (non più dummy)
        String jwtToken = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(jwtToken)
                .type("Bearer")
                .user(userResponse)
                .build();
    }

    /**
     * Autentica un utente esistente e genera un nuovo token JWT.
     *
     * Flusso di sicurezza:
     * 1. Verifica che l'email esista nel database
     * 2. Controlla che la password sia corretta (usando BCrypt)
     * 3. Verifica che l'account sia attivo
     * 4. Se tutto ok, genera token JWT per la sessione
     */
    public AuthResponse login(LoginRequest request) {
        // Trova utente nel database
        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Email o password non corretti"));

        // Verifica password usando BCrypt (confronta hash)
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Email o password non corretti");
        }

        // Controlla che l'account non sia stato disabilitato
        if (!user.isEnabled()) {
            throw new IllegalArgumentException("Account disabilitato");
        }

        // Genera nuovo token JWT per questa sessione
        String jwtToken = jwtService.generateToken(user);

        UserResponse userResponse = userService.findById(user.getId())
                .orElseThrow(() -> new IllegalStateException("Errore nel recupero utente"));

        return AuthResponse.builder()
                .token(jwtToken)
                .type("Bearer")
                .user(userResponse)
                .build();
    }

    /**
     * Valida un token JWT ricevuto dal client e restituisce l'utente corrispondente.
     *
     * Questo metodo viene usato dagli endpoint protetti per sapere chi sta
     * facendo la richiesta. Se il token non è valido, lancia un'eccezione.
     */
    public User validateTokenAndGetUser(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Token non valido");
        }

        // Rimuove il prefisso "Bearer " per ottenere solo il token JWT
        String jwtToken = token.substring(7);

        // Estrae l'email dal token JWT
        String email = jwtService.extractUsername(jwtToken);
        if (email == null) {
            throw new IllegalArgumentException("Token non valido");
        }

        // Trova l'utente nel database usando l'email
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));

        // Valida che il token sia ancora valido per questo utente
        if (!jwtService.isTokenValid(jwtToken, user)) {
            throw new IllegalArgumentException("Token scaduto o non valido");
        }

        return user;
    }
}