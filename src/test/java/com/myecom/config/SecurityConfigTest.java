package com.myecom.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test semplificato per verificare solo che PasswordEncoder sia configurato correttamente.
 *
 * Questo test verifica la parte più importante della sicurezza:
 * che le password vengano hashate correttamente con BCrypt.
 *
 * Gli altri componenti JWT li testiamo negli integration test.
 */
@SpringBootTest
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldConfigurePasswordEncoder() {
        // When & Then - verifica che PasswordEncoder sia configurato
        assertThat(passwordEncoder).isNotNull();

        // Verifica che usi BCrypt (algoritmo sicuro per password)
        String plainPassword = "testPassword123";
        String hashedPassword = passwordEncoder.encode(plainPassword);

        assertThat(hashedPassword).isNotEqualTo(plainPassword); // Password deve essere hashata
        assertThat(hashedPassword).startsWith("$2a$"); // BCrypt inizia sempre così
        assertThat(passwordEncoder.matches(plainPassword, hashedPassword)).isTrue(); // Verifica funziona

        System.out.println("PasswordEncoder configurato correttamente: BCrypt");
    }

    @Test
    void shouldHashPasswordsSecurely() {
        // Given - password di test
        String password1 = "myPassword123";
        String password2 = "myPassword123";
        String differentPassword = "differentPassword456";

        // When - genera hash
        String hash1 = passwordEncoder.encode(password1);
        String hash2 = passwordEncoder.encode(password2);
        String hash3 = passwordEncoder.encode(differentPassword);

        // Then - verifica proprietà di sicurezza di BCrypt
        assertThat(hash1).isNotEqualTo(hash2); // Stesso input -> hash diversi (salt random)
        assertThat(hash1).isNotEqualTo(password1); // Hash != password originale
        assertThat(passwordEncoder.matches(password1, hash1)).isTrue(); // Verifica funziona
        assertThat(passwordEncoder.matches(password1, hash3)).isFalse(); // Password diverse -> false
        assertThat(passwordEncoder.matches("wrongPassword", hash1)).isFalse(); // Password sbagliata -> false

        System.out.println("Hash sicuri BCrypt: VERIFICATO");
        System.out.println("Esempio hash: " + hash1.substring(0, 20) + "...");
    }

    @Test
    void shouldValidatePasswordSecurity() {
        // Given - test vari scenari di password
        String[] passwords = {"simple123", "Complex!Password@123", "test", "verylongpasswordwithmanycharacters"};

        // When & Then - verifica che tutte le password vengano hashate correttamente
        for (String password : passwords) {
            String hash = passwordEncoder.encode(password);

            assertThat(hash).startsWith("$2a$"); // Formato BCrypt
            assertThat(hash.length()).isGreaterThan(50); // Hash BCrypt sono lunghi
            assertThat(passwordEncoder.matches(password, hash)).isTrue(); // Verifica corretta
            assertThat(passwordEncoder.matches(password + "wrong", hash)).isFalse(); // Rifiuta password errate
        }

        System.out.println("Sicurezza password per vari input: VERIFICATA");
    }

}