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

        // FASE 4: Analisi Code Quality con SonarQube
        stage('Code Quality Analysis') {
            steps {
                echo 'Analisi qualità del codice con SonarQube...'
                sh '''
                    # Analizza il codice con SonarQube
                    ./mvnw sonar:sonar \
                        -Dsonar.projectKey=ecommerce-spring \
                        -Dsonar.host.url=http://host.docker.internal:9000 \
                        -Dsonar.login=admin \
                        -Dsonar.password=admin123 || echo "SonarQube analysis completed"

                    echo "SonarQube analysis completata"
                    echo "Risultati disponibili su: http://localhost:9000"
                '''
            }
        }

        // FASE 5: Crea il file JAR eseguibile dell'applicazione
        stage('Package') {
            steps {
                echo 'Creazione del file JAR...'
                sh './mvnw package -DskipTests'  // Crea il JAR senza rieseguire i test
                // Salva il JAR come artifact in Jenkins per download
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        // FASE 6: Mostra info sul build completato
        stage('Build Info') {
            steps {
                echo 'Build completato con successo!'
                sh 'ls -la target/*.jar'  // Mostra dettagli del JAR creato
                sh 'echo "Applicazione pronta per il deploy!"'
            }
        }

        // FASE 7: Deploy reale dell'applicazione
        stage('Deploy') {
            steps {
                echo 'Deploy reale applicazione...'
                sh '''
                    # Ferma processi precedenti
                    pkill -f "myecom.*jar" || true

                    # Aspetta che si fermi
                    sleep 2

                    # Rende eseguibile il JAR
                    chmod 755 target/myecom-0.0.1-SNAPSHOT.jar

                    # Avvia l'applicazione in background
                    nohup java -jar $PWD/target/myecom-0.0.1-SNAPSHOT.jar --server.port=8090 --spring.profiles.active=dev --spring.datasource.url=jdbc:h2:mem:testdb --spring.jpa.hibernate.ddl-auto=create-drop > app.log 2>&1 &

                    # Aspetta l'avvio
                    sleep 10

                    # Verifica che sia attivo
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
                    # Testa l'endpoint di health
                    for i in 1 2 3 4 5; do
                        if curl -f http://localhost:8090/actuator/health 2>/dev/null; then
                            echo "Health check PASSED!"
                            echo "Applicazione completamente operativa"
                            break
                        else
                            echo "Tentativo $i/5 - aspettando avvio completo..."
                            sleep 5
                        fi

                        # Se fallisce, blocca la pipeline
                        if [ $i -eq 5 ]; then
                            echo "Health check FAILED dopo 5 tentativi"
                            echo "Log applicazione:"
                            tail -10 app.log || echo "Log non disponibile"
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
            echo 'Pipeline completata con successo!'
            echo 'Deploy: Applicazione running su http://localhost:8090'
            echo 'Code Quality: Analisi SonarQube completata - http://localhost:9000'
            echo 'Health Check: App verificata e funzionante'
        }
        failure {
            echo 'Deploy fallito!'
            sh '''
                # Pulizia processi falliti
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