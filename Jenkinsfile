pipeline {
    agent any
    stages {
        stage('build') {
            steps {
                sh './gradlew build javadoc publish --no-daemon'
            }
        }
    }
    post {
        always {
            archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true
            deleteWs()
        }
    }
}