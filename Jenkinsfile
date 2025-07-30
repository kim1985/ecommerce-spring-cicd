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

        // FASE 6: Deploy automatico
                stage('Deploy') {
                    steps {
                        echo 'Deploy automatico applicazione...'
                        sh '''
                            # Ferma container precedente
                            docker stop my-ecommerce || true
                            docker rm my-ecommerce || true

                            # Crea immagine Docker
                            docker build -t my-ecommerce .

                            # Avvia nuovo container
                            docker run -d --name my-ecommerce -p 8090:8080 my-ecommerce

                            echo "Applicazione disponibile su http://localhost:8090"
                        '''
                    }
                }
    }

    // AZIONI FINALI: Eseguite sempre alla fine della pipeline
    post {
        success {
            // Eseguito solo se tutto va bene
            echo 'Pipeline completata con successo!'
            echo 'JAR disponibile nella sezione Artifacts'
        }
        failure {
            // Eseguito solo se qualcosa fallisce
            echo 'Pipeline fallita! Controlla i log per i dettagli.'
        }
        always {
            // Eseguito sempre, indipendentemente dal risultato
            echo 'Pulizia del workspace...'
            cleanWs()  // Pulisce i file temporanei per risparmiare spazio
        }
    }
}