E-commerce Spring Boot - CI/CD Pipeline
Panoramica del Progetto
Applicazione e-commerce enterprise sviluppata con Spring Boot e integrata con pipeline CI/CD automatizzata utilizzando Jenkins e GitLab.
Tecnologie Utilizzate
Backend

Java 17
Spring Boot 3.5.3
Spring Security per autenticazione
Spring Data JPA per persistenza
H2 Database (sviluppo)
Maven per build automation

DevOps e CI/CD

GitLab per source control
Jenkins per continuous integration
Docker per containerizzazione Jenkins
Maven Wrapper per build consistency

Testing

JUnit 5 per unit testing
Spring Boot Test per integration testing
Mockito per mocking

Architettura Applicazione
Il progetto segue il pattern MVC con una chiara separazione delle responsabilità:
Controllers -> Services -> Repositories -> Models
Moduli Principali

Gestione Utenti - Registrazione, login, profili
Catalogo Prodotti - CRUD prodotti e categorie
Carrello Acquisti - Gestione carrello con persistenza
Gestione Ordini - Creazione e tracking ordini
Autenticazione - Sistema di sicurezza integrato

Pipeline CI/CD
La pipeline automatizzata esegue le seguenti fasi:

Checkout - Scarica il codice da GitLab
Clean and Compile - Pulisce e compila il progetto
Test - Esegue tutti i test automatizzati
Package - Crea il JAR eseguibile
Build Info - Verifica e documenta il build

Jenkinsfile
Il file di configurazione della pipeline è versionato insieme al codice, seguendo il principio "Pipeline as Code".
Setup e Installazione
Prerequisiti

Java 17 o superiore
Docker Desktop
Git
Account GitLab

Avvio Locale
git clone git@gitlab.com:kim19851/ecommerce-spring.git
cd ecommerce-spring
./mvnw spring-boot:run

Setup Jenkins
docker run -d --name jenkins -p 8080:8080 jenkins/jenkins:lts
Dopo l'avvio, configurare:

Credenziali GitLab
Connessione al repository
Pipeline automatizzata

Funzionalità Implementate
Gestione Utenti

Registrazione con validazione
Login sicuro
Profili utente completi
Sistema di ruoli (USER/ADMIN)

Catalogo Prodotti

CRUD completo per prodotti
Gestione categorie
Ricerca e filtri
Paginazione

Sistema Carrello

Aggiunta/rimozione prodotti
Calcolo automatico totali
Gestione quantità
Persistenza durante la sessione

Gestione Ordini

Creazione ordini dal carrello
Tracking con numero ordine univoco
Stati dell'ordine
Storico ordini per utente

Testing
Il progetto include test completi a tutti i livelli:

Unit test per services e controllers
Integration test per repository
Test della business logic
Validation test

Esecuzione test:
bash./mvnw test
Risultati Pipeline

Build Success Rate: 100%
Tempo di esecuzione: meno di 3 minuti
Test automatizzati: completa copertura business logic
Artifact generation: JAR pronto per deployment

Repository

GitLab: https://gitlab.com/kim19851/ecommerce-spring
Jenkins Pipeline: Configurata e funzionante
Documentazione: Inclusa nel repository