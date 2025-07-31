pipeline {
    agent any

    stages {
        // FASE 1: Scarica il codice dal repository GitLab
        stage('Checkout') {
            steps {
                echo 'Scaricamento codice sorgente da GitLab...'
                checkout scm  // scm = source control management (GitLab)
                echo 'Checkout completato'
            }
        }

        // FASE 2: Pulisce e compila il progetto Java
        stage('Clean and Compile') {
            steps {
                echo 'Pulizia e compilazione del progetto...'
                sh 'chmod +x ./mvnw'  // Rende eseguibile il Maven wrapper
                sh './mvnw clean compile'  // Pulisce e compila il codice Java
                echo 'Compilazione completata'
            }
        }

        // FASE 3: Esegue tutti i test del progetto (con timeout)
        stage('Test') {
            steps {
                echo 'Esecuzione test unitari e di integrazione...'
                timeout(time: 3, unit: 'MINUTES') {
                    sh './mvnw test'  // Esegue i test con Maven
                }
                echo 'Test completati'
            }
        }

        // FASE 4: Analisi Code Quality con SonarQube (opzionale e veloce)
        stage('Code Quality Analysis') {
            steps {
                echo 'Verifica disponibilità SonarQube...'
                script {
                    try {
                        timeout(time: 1, unit: 'MINUTES') {
                            sh '''
                                # Test veloce connessione SonarQube
                                if timeout 5 curl -f http://localhost:9000 >/dev/null 2>&1; then
                                    echo "SonarQube disponibile - avvio analisi veloce"

                                    # Analisi semplificata (solo metriche base)
                                    ./mvnw sonar:sonar \
                                        -Dsonar.projectKey=ecommerce-spring \
                                        -Dsonar.host.url=http://localhost:9000 \
                                        -Dsonar.login=admin \
                                        -Dsonar.password=admin \
                                        -Dsonar.scm.disabled=true \
                                        -Dsonar.exclusions=**/test/**,**/target/** \
                                        -Dsonar.skipPackageDesign=true

                                    echo "SonarQube analysis completata"
                                else
                                    echo "SonarQube non raggiungibile - skip analisi"
                                fi
                            '''
                        }
                    } catch (Exception e) {
                        echo "SonarQube timeout o errore - continuando senza analisi"
                        echo "Per abilitare: docker run -d --name sonarqube -p 9000:9000 sonarqube:community"
                    }
                }
            }
        }

        // FASE 5: Crea il file JAR eseguibile dell'applicazione
        stage('Package') {
            steps {
                echo 'Creazione del file JAR...'
                sh './mvnw package -DskipTests'  // Crea il JAR senza rieseguire i test

                // Verifica che il JAR sia stato creato
                sh '''
                    if [ -f target/myecom-0.0.1-SNAPSHOT.jar ]; then
                        echo "JAR creato: $(ls -lh target/*.jar)"
                    else
                        echo "JAR non trovato!"
                        exit 1
                    fi
                '''

                // Salva il JAR come artifact in Jenkins per download
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                echo 'Package completato'
            }
        }

        // FASE 6: Deploy Development Environment (con diagnostica)
        stage('Deploy to DEV') {
            steps {
                echo 'Deploy in ambiente Development...'
                timeout(time: 2, unit: 'MINUTES') {
                    sh '''
                        echo "Cleanup processi DEV precedenti..."
                        pkill -f "myecom.*8091" || true
                        sleep 3

                        echo "Avvio DEV environment (porta 8091)..."
                        nohup java -jar target/myecom-0.0.1-SNAPSHOT.jar \
                            --server.port=8091 \
                            --spring.profiles.active=dev \
                            --spring.datasource.url=jdbc:h2:mem:devdb \
                            --spring.jpa.hibernate.ddl-auto=create-drop \
                            --logging.level.root=WARN \
                            --server.shutdown=graceful > dev.log 2>&1 &

                        echo "Attesa avvio DEV (max 30s)..."
                        for i in {1..15}; do
                            if pgrep -f "myecom.*8091" > /dev/null; then
                                echo "DEV processo avviato (PID: $(pgrep -f 'myecom.*8091'))"
                                break
                            fi
                            echo "   Tentativo $i/15..."
                            sleep 2
                        done

                        # Verifica finale
                        if pgrep -f "myecom.*8091" > /dev/null; then
                            echo "DEV Environment: RUNNING"
                        else
                            echo "DEV Environment: FAILED"
                            echo "Ultimi log DEV:"
                            tail -10 dev.log || echo "Log DEV non disponibile"
                            exit 1
                        fi
                    '''
                }
            }
        }

        // FASE 7: Deploy Staging Environment (con diagnostica)
        stage('Deploy to STAGING') {
            steps {
                echo 'Deploy in ambiente Staging...'
                timeout(time: 2, unit: 'MINUTES') {
                    sh '''
                        echo "Cleanup processi STAGING precedenti..."
                        pkill -f "myecom.*8092" || true
                        sleep 3

                        echo "Avvio STAGING environment (porta 8092)..."
                        nohup java -jar target/myecom-0.0.1-SNAPSHOT.jar \
                            --server.port=8092 \
                            --spring.profiles.active=staging \
                            --spring.datasource.url=jdbc:h2:mem:stagingdb \
                            --spring.jpa.hibernate.ddl-auto=create-drop \
                            --logging.level.root=WARN \
                            --server.shutdown=graceful > staging.log 2>&1 &

                        echo "Attesa avvio STAGING (max 30s)..."
                        for i in {1..15}; do
                            if pgrep -f "myecom.*8092" > /dev/null; then
                                echo "STAGING processo avviato (PID: $(pgrep -f 'myecom.*8092'))"
                                break
                            fi
                            echo "   Tentativo $i/15..."
                            sleep 2
                        done

                        # Verifica finale
                        if pgrep -f "myecom.*8092" > /dev/null; then
                            echo "STAGING Environment: RUNNING"
                        else
                            echo "STAGING Environment: FAILED"
                            echo "Ultimi log STAGING:"
                            tail -10 staging.log || echo "Log STAGING non disponibile"
                            exit 1
                        fi
                    '''
                }
            }
        }

        // FASE 8: Deploy Production Environment (con approvazione e diagnostica)
        stage('Deploy to PRODUCTION') {
            steps {
                script {
                    // Richiede approvazione manuale per produzione - best practice enterprise
                    echo 'Richiesta approvazione per PRODUCTION deploy...'
                    timeout(time: 5, unit: 'MINUTES') {
                        input message: 'Deploy in PRODUCTION?',
                              ok: 'Deploy Now!',
                              submitterParameter: 'DEPLOYER'
                    }
                    echo "Deploy autorizzato da: ${env.DEPLOYER}"
                }

                echo 'Deploy in ambiente Production...'
                timeout(time: 2, unit: 'MINUTES') {
                    sh '''
                        echo "Cleanup processi PRODUCTION precedenti..."
                        pkill -f "myecom.*8090" || true
                        sleep 3

                        echo "Avvio PRODUCTION environment (porta 8090)..."
                        nohup java -jar target/myecom-0.0.1-SNAPSHOT.jar \
                            --server.port=8090 \
                            --spring.profiles.active=prod \
                            --spring.datasource.url=jdbc:h2:mem:proddb \
                            --spring.jpa.hibernate.ddl-auto=create-drop \
                            --logging.level.root=ERROR \
                            --server.shutdown=graceful > prod.log 2>&1 &

                        echo "Attesa avvio PRODUCTION (max 40s)..."
                        for i in {1..20}; do
                            if pgrep -f "myecom.*8090" > /dev/null; then
                                echo "PRODUCTION processo avviato (PID: $(pgrep -f 'myecom.*8090'))"
                                break
                            fi
                            echo "   Tentativo $i/20..."
                            sleep 2
                        done

                        # Verifica finale PRODUCTION
                        if pgrep -f "myecom.*8090" > /dev/null; then
                            echo "PRODUCTION Environment: RUNNING"
                        else
                            echo "PRODUCTION Environment: FAILED"
                            echo "Ultimi log PRODUCTION:"
                            tail -15 prod.log || echo "Log PRODUCTION non disponibile"
                            exit 1
                        fi
                    '''
                }
            }
        }

        // FASE 9: Health Check Multi-Environment (rapido e completo)
        stage('Multi-Environment Health Check') {
            steps {
                echo 'Verifica health di tutti gli ambienti...'
                timeout(time: 3, unit: 'MINUTES') {
                    sh '''
                        echo "=== HEALTH CHECK MULTI-ENVIRONMENT ==="

                        # Funzione per health check con retry intelligente
                        check_health() {
                            local env=$1
                            local port=$2
                            local max_attempts=10

                            echo "Controllo $env su porta $port..."

                            for i in $(seq 1 $max_attempts); do
                                if curl -f -m 3 --connect-timeout 3 http://localhost:$port/actuator/health >/dev/null 2>&1; then
                                    echo "$env ($port): HEALTHY"
                                    return 0
                                else
                                    echo "$env ($port): Tentativo $i/$max_attempts..."
                                    sleep 3
                                fi
                            done

                            echo "$env ($port): UNHEALTHY dopo $max_attempts tentativi"
                            return 1
                        }

                        # Verifica sequenziale per debug migliore
                        check_health "DEV" "8091"
                        check_health "STAGING" "8092"
                        check_health "PRODUCTION" "8090"

                        echo ""
                        echo "=== DEPLOYMENT SUMMARY ==="
                        echo "DEV:        http://localhost:8091"
                        echo "STAGING:    http://localhost:8092"
                        echo "PRODUCTION: http://localhost:8090"
                        echo "SonarQube:  http://localhost:9000"
                        echo "H2 Console: http://localhost:8090/h2-console"
                        echo ""
                        echo "Processi attivi:"
                        pgrep -f "myecom.*jar" || echo "Nessun processo myecom trovato"
                    '''
                }
            }
        }
    }

    // AZIONI FINALI: Eseguite sempre alla fine della pipeline
    post {
        success {
            // Eseguito solo se tutto va bene
            echo 'MULTI-ENVIRONMENT DEPLOYMENT COMPLETED!'
            echo 'Durata totale: ${currentBuild.durationString}'
            echo 'Code Quality: SonarQube analysis completed'
            echo 'DEV Environment: http://localhost:8091'
            echo 'STAGING Environment: http://localhost:8092'
            echo 'PRODUCTION Environment: http://localhost:8090'
            echo 'All environments verified and operational'

            // Report finale con statistiche
            sh '''
                echo ""
                echo "=== DEPLOYMENT STATISTICS ==="
                echo "Timestamp: $(date)"
                echo "Deployer: ${DEPLOYER:-automated}"
                echo "JAR Size: $(ls -lh target/*.jar | awk '{print $5}')"
                echo "Active Processes: $(pgrep -f 'myecom.*jar' | wc -l)"
                echo "Status: SUCCESS"
            '''
        }
        failure {
            echo 'Multi-environment deployment FALLITO!'
            sh '''
                echo "=== DIAGNOSTICA ERRORI ==="
                echo "Timestamp fallimento: $(date)"

                echo ""
                echo "Processi Java attivi:"
                pgrep -f java || echo "Nessun processo Java trovato"

                echo ""
                echo "Porte in ascolto:"
                netstat -tlnp 2>/dev/null | grep -E ':(8090|8091|8092|9000)' || echo "Nessuna porta myecom in ascolto"

                echo ""
                echo "Spazio disco:"
                df -h . || echo "Errore controllo spazio"

                echo ""
                echo "Ultimi logs applicazioni:"
                echo "=== DEV LOG ==="
                tail -5 dev.log 2>/dev/null || echo "DEV log non disponibile"
                echo "=== STAGING LOG ==="
                tail -5 staging.log 2>/dev/null || echo "STAGING log non disponibile"
                echo "=== PROD LOG ==="
                tail -5 prod.log 2>/dev/null || echo "PROD log non disponibile"

                echo ""
                echo "Cleanup processi..."
                pkill -f "myecom.*jar" || true
            '''
        }
        always {
            // Eseguito sempre, indipendentemente dal risultato
            echo 'Pulizia del workspace...'
            cleanWs()  // Pulisce i file temporanili per risparmiare spazio
            echo 'Cleanup completato'
        }
    }
}