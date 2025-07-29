pipeline {
    agent any

    // Configura gli strumenti necessari
    tools {
        maven 'Maven-3.9'  // Nome da configurare in Jenkins
        jdk 'JDK-17'       // Nome da configurare in Jenkins
    }

    stages {
        // Fase 1: Scarica il codice da GitLab
        stage('Checkout') {
            steps {
                echo 'Scaricamento codice sorgente da GitLab...'
                checkout scm
            }
        }

        // Fase 2: Compila il progetto
        stage('Clean and Compile') {
            steps {
                echo 'Pulizia e compilazione del progetto...'
                sh 'mvn clean compile'
            }
        }

        // Fase 3: Esegue tutti i test
        stage('Test') {
            steps {
                echo 'Esecuzione test unitari e di integrazione...'
                sh 'mvn test'
            }
            post {
                always {
                    // Pubblica i risultati dei test in Jenkins
                    publishTestResults testResultsPattern: 'target/surefire-reports/*.xml'
                }
            }
        }

        // Fase 4: Crea il file JAR eseguibile
        stage('Package') {
            steps {
                echo 'Creazione del file JAR...'
                sh 'mvn package -DskipTests'

                // Salva il JAR come artifact in Jenkins
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        // Fase 5: Crea immagine Docker (solo per branch main)
        stage('Docker Build') {
            when {
                branch 'main'  // Esegue solo quando si fa push su main
            }
            steps {
                echo 'Creazione immagine Docker...'
                script {
                    // Crea immagine con numero build e tag latest
                    docker.build("ecommerce-spring:${BUILD_NUMBER}")
                    docker.build("ecommerce-spring:latest")
                }
            }
        }

        // Fase 6: Deploy dell'applicazione (solo per branch main)
        stage('Deploy') {
            when {
                branch 'main'  // Deploy solo da main branch
            }
            steps {
                echo 'Deploy dell applicazione...'
                sh '''
                    # Ferma e rimuove il container precedente se esiste
                    docker stop ecommerce-app || true
                    docker rm ecommerce-app || true

                    # Avvia il nuovo container con l'applicazione
                    docker run -d \
                        --name ecommerce-app \
                        -p 8080:8080 \
                        --restart unless-stopped \
                        ecommerce-spring:latest

                    # Attende che l'applicazione si avvii
                    sleep 30
                    echo "Applicazione disponibile su http://localhost:8080"
                '''
            }
        }
    }

    // Azioni da eseguire alla fine della pipeline
    post {
        success {
            echo 'Pipeline completata con successo!'
        }
        failure {
            echo 'Pipeline fallita! Controlla i log per i dettagli.'
        }
        always {
            echo 'Pulizia del workspace...'
            cleanWs()  // Pulisce i file temporanei
        }
    }
}