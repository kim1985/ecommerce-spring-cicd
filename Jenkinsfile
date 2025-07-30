pipeline {
    agent any

    stages {
        // FASE 1: Scarica il codice dal repository GitLab
        stage('Checkout') {
            steps {
                echo 'Scaricamento codice sorgente da GitLab...'
                checkout scm  // scm = source control management (GitLab)
            }
        }

        // FASE 2: Pulisce e compila il progetto Java
        stage('Clean and Compile') {
            steps {
                echo 'Pulizia e compilazione del progetto...'
                sh 'chmod +x ./mvnw'  // Rende eseguibile il Maven wrapper
                sh './mvnw clean compile'  // Pulisce e compila il codice Java
            }
        }

        // FASE 3: Esegue tutti i test del progetto
        stage('Test') {
            steps {
                echo 'Esecuzione test unitari e di integrazione...'
                sh './mvnw test'  // Esegue i test con Maven
                echo 'Test completati - controlla i log sopra per i risultati'
            }
        }

        // FASE 4: Crea il file JAR eseguibile dell'applicazione
        stage('Package') {
            steps {
                echo 'Creazione del file JAR...'
                sh './mvnw package -DskipTests'  // Crea il JAR senza rieseguire i test
                // Salva il JAR come artifact in Jenkins per download
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        // FASE 5: Mostra info sul build completato
        stage('Build Info') {
            steps {
                echo 'Build completato con successo!'
                sh 'ls -la target/*.jar'  // Mostra dettagli del JAR creato
                sh 'echo "Applicazione pronta per il deploy!"'
            }
        }

        // FASE 6: Deploy reale dell'applicazione
        stage('Deploy') {
            steps {
                echo 'Deploy reale applicazione...'
                sh '''
                    # Ferma processo precedente se esiste
                    pkill -f "myecom.*jar" || true

                    # Aspetta che si fermi
                    sleep 5

                    # Fix permessi JAR
                    chmod 755 target/myecom-0.0.1-SNAPSHOT.jar

                    # Avvia con path assoluto e configurazione semplificata
                    nohup java -jar $PWD/target/myecom-0.0.1-SNAPSHOT.jar --server.port=8090 --spring.profiles.active=dev --spring.datasource.url=jdbc:h2:mem:testdb --spring.jpa.hibernate.ddl-auto=create-drop > app.log 2>&1 &

                    # Aspetta avvio
                    sleep 20

                    # Verifica che sia started
                    if pgrep -f "myecom.*jar" > /dev/null; then
                        echo "Applicazione avviata con successo!"
                        echo "Disponibile su: http://localhost:8090"
                        echo "PID processo: $(pgrep -f 'myecom.*jar')"
                    else
                        echo "Errore nell avvio dell applicazione"
                        cat app.log || echo "Log non disponibile"
                        exit 1
                    fi
                '''
            }
        }

        // FASE 7: Verifica che l'applicazione funzioni correttamente
        stage('Health Check') {
            steps {
                echo 'Verifica health dell applicazione...'
                sh '''
                    # Attendi che l'app sia completamente avviata
                    for i in 1 2 3 4 5 6 7 8 9 10; do
                        if curl -f http://localhost:8090/actuator/health 2>/dev/null; then
                            echo "Health check PASSED!"
                            echo "Applicazione completamente operativa"
                            break
                        else
                            echo "Tentativo $i/10 - aspettando avvio completo..."
                            sleep 10
                        fi

                        if [ $i -eq 10 ]; then
                            echo "Health check FAILED dopo 10 tentativi"
                            echo "Log applicazione:"
                            tail -20 app.log || echo "Log non disponibile"
                            exit 1
                        fi
                    done
                '''
            }
        }
    }

    // AZIONI FINALI: Eseguite sempre alla fine della pipeline
    post {
        success {
            // Eseguito solo se tutto va bene
            echo 'Deploy completato! Applicazione running su http://localhost:8090'
        }
        failure {
            echo 'Deploy fallito!'
            sh '''
                echo "Cleanup processo fallito:"
                pkill -f "myecom.*jar" || true
            '''
        }
        always {
            // Eseguito sempre, indipendentemente dal risultato
            echo 'Pulizia del workspace...'
            cleanWs()  // Pulisce i file temporanei per risparmiare spazio
        }
    }
}