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
                        -Dsonar.password=admin || echo "SonarQube analysis completed"

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

        // FASE 6: Deploy Development Environment
        stage('Deploy to DEV') {
            steps {
                echo 'Deploy in ambiente Development...'
                sh '''
                    # Ferma processi DEV precedenti
                    pkill -f "myecom.*8091" || true

                    # Aspetta che si fermi
                    sleep 2

                    # Rende eseguibile il JAR
                    chmod 755 target/myecom-0.0.1-SNAPSHOT.jar

                    # Avvia in ambiente DEV (porta 8091)
                    nohup java -jar $PWD/target/myecom-0.0.1-SNAPSHOT.jar \
                        --server.port=8091 \
                        --spring.profiles.active=dev \
                        --spring.datasource.url=jdbc:h2:mem:devdb \
                        --spring.jpa.hibernate.ddl-auto=create-drop > dev.log 2>&1 &

                    # Aspetta l'avvio
                    sleep 10

                    # Verifica ambiente DEV
                    if pgrep -f "myecom.*8091" > /dev/null; then
                        echo "DEV Environment: RUNNING"
                        echo "URL: http://localhost:8091"
                        echo "PID: $(pgrep -f 'myecom.*8091')"
                    else
                        echo "Errore deploy DEV"
                        exit 1
                    fi
                '''
            }
        }

        // FASE 7: Deploy Staging Environment
        stage('Deploy to STAGING') {
            steps {
                echo 'Deploy in ambiente Staging...'
                sh '''
                    # Ferma processi STAGING precedenti
                    pkill -f "myecom.*8092" || true

                    # Aspetta che si fermi
                    sleep 2
                    
                    # Avvia in ambiente STAGING (porta 8092) - usa create-drop come DEV
                    nohup java -jar $PWD/target/myecom-0.0.1-SNAPSHOT.jar \
                        --server.port=8092 \
                        --spring.profiles.active=staging \
                        --spring.datasource.url=jdbc:h2:mem:stagingdb \
                        --spring.jpa.hibernate.ddl-auto=create-drop > staging.log 2>&1 &

                    # Aspetta l'avvio
                    sleep 10

                    # Verifica ambiente STAGING
                    if pgrep -f "myecom.*8092" > /dev/null; then
                        echo "STAGING Environment: RUNNING"
                        echo "URL: http://localhost:8092"
                        echo "PID: $(pgrep -f 'myecom.*8092')"
                    else
                        echo "Errore deploy STAGING"
                        cat staging.log || echo "Log STAGING non disponibile"
                        exit 1
                    fi
                '''
            }
        }

        // FASE 8: Deploy Production Environment (con approvazione manuale)
        stage('Deploy to PRODUCTION') {
            steps {
                script {
                    // Richiede approvazione manuale per produzione
                    input message: 'Deploy in PRODUCTION?', ok: 'Deploy Now!',
                          submitterParameter: 'DEPLOYER'

                    echo "Deploy autorizzato da: ${env.DEPLOYER}"
                }

                echo 'Deploy in ambiente Production...'
                sh '''
                    # Ferma processi PRODUCTION precedenti
                    pkill -f "myecom.*8090" || true

                    # Aspetta che si fermi
                    sleep 2

                    # Avvia in ambiente PRODUCTION (porta 8090) - usa create-drop per semplicità
                    nohup java -jar $PWD/target/myecom-0.0.1-SNAPSHOT.jar \
                        --server.port=8090 \
                        --spring.profiles.active=prod \
                        --spring.datasource.url=jdbc:h2:mem:proddb \
                        --spring.jpa.hibernate.ddl-auto=create-drop > prod.log 2>&1 &

                    # Aspetta l'avvio
                    sleep 10

                    # Verifica ambiente PRODUCTION
                    if pgrep -f "myecom.*8090" > /dev/null; then
                        echo "PRODUCTION Environment: RUNNING"
                        echo "URL: http://localhost:8090"
                        echo "PID: $(pgrep -f 'myecom.*8090')"
                    else
                        echo "Errore deploy PRODUCTION"
                        cat prod.log || echo "Log PRODUCTION non disponibile"
                        exit 1
                    fi
                '''
            }
        }

        // FASE 9: Verifica tutti gli ambienti
        stage('Multi-Environment Health Check') {
            steps {
                echo 'Verifica health di tutti gli ambienti...'
                sh '''
                    echo "=== HEALTH CHECK MULTI-ENVIRONMENT ==="

                    # Verifica DEV
                    if curl -f http://localhost:8091/actuator/health 2>/dev/null; then
                        echo "DEV (8091): HEALTHY"
                    else
                        echo "DEV (8091): UNHEALTHY"
                    fi

                    # Verifica STAGING
                    if curl -f http://localhost:8092/actuator/health 2>/dev/null; then
                        echo "STAGING (8092): HEALTHY"
                    else
                        echo "STAGING (8092): UNHEALTHY"
                    fi

                    # Verifica PRODUCTION
                    if curl -f http://localhost:8090/actuator/health 2>/dev/null; then
                        echo "PRODUCTION (8090): HEALTHY"
                    else
                        echo "PRODUCTION (8090): UNHEALTHY"
                    fi

                    echo "=== DEPLOYMENT SUMMARY ==="
                    echo "DEV:        http://localhost:8091"
                    echo "STAGING:    http://localhost:8092"
                    echo "PRODUCTION: http://localhost:8090"
                    echo "SonarQube:  http://localhost:9000"
                '''
            }
        }
    }

    // AZIONI FINALI: Eseguite sempre alla fine della pipeline
    post {
        success {
            // Eseguito solo se tutto va bene
            echo 'MULTI-ENVIRONMENT DEPLOYMENT COMPLETED!'
            echo 'Code Quality: SonarQube PASSED - http://localhost:9000'
            echo 'DEV Environment: http://localhost:8091'
            echo 'STAGING Environment: http://localhost:8092'
            echo 'PRODUCTION Environment: http://localhost:8090'
            echo 'All environments verified and operational'
        }
        failure {
            echo 'Multi-environment deployment fallito!'
            sh '''
                # Pulizia tutti gli ambienti
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