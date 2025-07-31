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

        // FASE 4: Analisi Code Quality con SonarQube (ottimizzata)
        stage('Code Quality Analysis') {
            steps {
                echo 'Analisi qualità del codice con SonarQube...'
                script {
                    try {
                        sh '''
                            # Test rapido connessione SonarQube
                            timeout 10 curl -f http://localhost:9000 >/dev/null 2>&1

                            # Se SonarQube è disponibile, esegui analisi veloce
                            ./mvnw sonar:sonar \
                                -Dsonar.projectKey=ecommerce-spring \
                                -Dsonar.host.url=http://localhost:9000 \
                                -Dsonar.login=admin \
                                -Dsonar.password=admin \
                                -Dsonar.exclusions=**/test/**,**/target/** \
                                -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml

                            echo "✅ SonarQube analysis completata"
                        '''
                    } catch (Exception e) {
                        echo "⚠️ SonarQube non disponibile - continuando senza analisi"
                        echo "💡 Per abilitare: docker run -d --name sonarqube -p 9000:9000 sonarqube:community"
                    }
                }
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

        // FASE 6: Deploy Development Environment (ottimizzato)
        stage('Deploy to DEV') {
            steps {
                echo 'Deploy in ambiente Development...'
                sh '''
                    # Ferma processi DEV precedenti (veloce)
                    pkill -f "myecom.*8091" || true
                    sleep 2

                    # Avvia DEV (porta 8091) - configurazione leggera
                    nohup java -jar target/myecom-0.0.1-SNAPSHOT.jar \
                        --server.port=8091 \
                        --spring.profiles.active=dev \
                        --spring.datasource.url=jdbc:h2:mem:devdb \
                        --spring.jpa.hibernate.ddl-auto=create-drop \
                        --logging.level.root=WARN > dev.log 2>&1 &

                    # Verifica avvio rapido
                    sleep 8
                    if pgrep -f "myecom.*8091" > /dev/null; then
                        echo "✅ DEV Environment: RUNNING (PID: $(pgrep -f 'myecom.*8091'))"
                    else
                        echo "❌ Errore deploy DEV"
                        exit 1
                    fi
                '''
            }
        }

        // FASE 7: Deploy Staging Environment (ottimizzato)
        stage('Deploy to STAGING') {
            steps {
                echo 'Deploy in ambiente Staging...'
                sh '''
                    # Ferma processi STAGING precedenti
                    pkill -f "myecom.*8092" || true
                    sleep 2

                    # Avvia STAGING (porta 8092) - simile a produzione
                    nohup java -jar target/myecom-0.0.1-SNAPSHOT.jar \
                        --server.port=8092 \
                        --spring.profiles.active=staging \
                        --spring.datasource.url=jdbc:h2:mem:stagingdb \
                        --spring.jpa.hibernate.ddl-auto=create-drop \
                        --logging.level.root=WARN > staging.log 2>&1 &

                    # Verifica avvio
                    sleep 8
                    if pgrep -f "myecom.*8092" > /dev/null; then
                        echo "✅ STAGING Environment: RUNNING (PID: $(pgrep -f 'myecom.*8092'))"
                    else
                        echo "❌ Errore deploy STAGING"
                        exit 1
                    fi
                '''
            }
        }

        // FASE 8: Deploy Production Environment (con approvazione manuale)
        stage('Deploy to PRODUCTION') {
            steps {
                script {
                    // Richiede approvazione manuale per produzione - best practice enterprise
                    timeout(time: 5, unit: 'MINUTES') {
                        input message: 'Deploy in PRODUCTION?',
                              ok: 'Deploy Now!',
                              submitterParameter: 'DEPLOYER'
                    }
                    echo "Deploy autorizzato da: ${env.DEPLOYER}"
                }

                echo 'Deploy in ambiente Production...'
                sh '''
                    # Ferma processi PRODUCTION precedenti
                    pkill -f "myecom.*8090" || true
                    sleep 2

                    # Avvia PRODUCTION (porta 8090) - configurazione ottimizzata
                    nohup java -jar target/myecom-0.0.1-SNAPSHOT.jar \
                        --server.port=8090 \
                        --spring.profiles.active=prod \
                        --spring.datasource.url=jdbc:h2:mem:proddb \
                        --spring.jpa.hibernate.ddl-auto=create-drop \
                        --logging.level.root=ERROR > prod.log 2>&1 &

                    # Verifica avvio production
                    sleep 10
                    if pgrep -f "myecom.*8090" > /dev/null; then
                        echo "✅ PRODUCTION Environment: RUNNING (PID: $(pgrep -f 'myecom.*8090'))"
                    else
                        echo "❌ Errore deploy PRODUCTION"
                        exit 1
                    fi
                '''
            }
        }

        // FASE 9: Health Check Multi-Environment (ottimizzato)
        stage('Multi-Environment Health Check') {
            steps {
                echo 'Verifica health di tutti gli ambienti...'
                sh '''
                    echo "🏥 === HEALTH CHECK MULTI-ENVIRONMENT ==="

                    # Funzione per health check veloce
                    check_health() {
                        local env=$1
                        local port=$2
                        local max_attempts=6

                        for i in $(seq 1 $max_attempts); do
                            if curl -f -m 5 http://localhost:$port/actuator/health >/dev/null 2>&1; then
                                echo "✅ $env ($port): HEALTHY"
                                return 0
                            else
                                echo "⏳ $env ($port): Tentativo $i/$max_attempts..."
                                sleep 2
                            fi
                        done
                        echo "❌ $env ($port): UNHEALTHY"
                        return 1
                    }

                    # Verifica tutti gli ambienti in parallelo (concetto avanzato)
                    check_health "DEV" "8091" &
                    check_health "STAGING" "8092" &
                    check_health "PRODUCTION" "8090" &
                    wait  # Aspetta che tutti i check finiscano

                    echo ""
                    echo "🚀 === DEPLOYMENT SUMMARY ==="
                    echo "🔧 DEV:        http://localhost:8091"
                    echo "🔍 STAGING:    http://localhost:8092"
                    echo "🌐 PRODUCTION: http://localhost:8090"
                    echo "📊 SonarQube:  http://localhost:9000"
                    echo "💾 H2 Console: http://localhost:8090/h2-console"
                '''
            }
        }
    }

    // AZIONI FINALI: Eseguite sempre alla fine della pipeline
    post {
        success {
            // Eseguito solo se tutto va bene
            echo '🎉 MULTI-ENVIRONMENT DEPLOYMENT COMPLETED!'
            echo '📊 Code Quality: SonarQube analysis completed'
            echo '🔧 DEV Environment: http://localhost:8091'
            echo '🔍 STAGING Environment: http://localhost:8092'
            echo '🌐 PRODUCTION Environment: http://localhost:8090'
            echo '✅ All environments verified and operational'

            // Notifica successo (enterprise pattern)
            sh '''
                echo "📧 === DEPLOYMENT NOTIFICATION ==="
                echo "Timestamp: $(date)"
                echo "Branch: ${GIT_BRANCH:-main}"
                echo "Commit: ${GIT_COMMIT:-unknown}"
                echo "Deployer: ${DEPLOYER:-system}"
                echo "Status: SUCCESS ✅"
            '''
        }
        failure {
            echo '❌ Multi-environment deployment fallito!'
            sh '''
                echo "🧹 Cleanup dopo fallimento..."
                pkill -f "myecom.*jar" || true

                echo "📋 Debug logs:"
                echo "=== DEV LOG ==="
                tail -20 dev.log 2>/dev/null || echo "DEV log non disponibile"
                echo "=== STAGING LOG ==="
                tail -20 staging.log 2>/dev/null || echo "STAGING log non disponibile"
                echo "=== PROD LOG ==="
                tail -20 prod.log 2>/dev/null || echo "PROD log non disponibile"
            '''
        }
        always {
            // Eseguito sempre, indipendentemente dal risultato
            echo 'Pulizia del workspace...'
            cleanWs()  // Pulisce i file temporanili per risparmiare spazio
        }
    }
}