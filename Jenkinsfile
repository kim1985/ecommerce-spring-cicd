pipeline {
    agent any

    stages {
        // Fase 1: Scarica il codice da GitLab
        stage('Checkout') {
            steps {
                echo 'Scaricamento codice sorgente da GitLab...'
                checkout scm
            }
        }

        // Fase 2: Compila il progetto usando il wrapper Maven
        stage('Clean and Compile') {
            steps {
                echo 'Pulizia e compilazione del progetto...'
                sh 'chmod +x ./mvnw'  // Rende eseguibile il file mvnw
                sh './mvnw clean compile'  // Usa il wrapper invece di mvn
            }
        }

        // Fase 3: Esegue tutti i test
        stage('Test') {
            steps {
                echo 'Esecuzione test unitari e di integrazione...'
                sh './mvnw test'  // Usa il wrapper invece di mvn
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
                sh './mvnw package -DskipTests'  // Usa il wrapper invece di mvn

                // Salva il JAR come artifact in Jenkins
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        // Fase 5: Solo messaggio di successo per ora
        stage('Build Info') {
            steps {
                echo 'Build completato con successo!'
                sh 'ls -la target/*.jar'  // Mostra il JAR creato
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