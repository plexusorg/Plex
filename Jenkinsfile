pipeline {
    agent any
    tools {
        jdk 'jdk-25-latest'
    }
    stages {
        stage("build") {
            steps {
                withGradle {
                    sh "./gradlew build javadoc --no-daemon --refresh-dependencies"
                }
            }
        }
        // Telesphorepo takes this job's identity token only from Plex/master, so a branch or pull request
        // that edits this stage cannot publish. An identity token mints one publish session, so each upload binds its own.
        stage("publish") {
            when {
                branch "master"
            }
            steps {
                sh "curl -fsSL https://telesphorepo.com/install.sh | sh"
                // A release publishes once, the first time its version reaches master.
                withCredentials([string(credentialsId: "telesphorepo-id-token", variable: "OIDC_TOKEN")]) {
                    sh '''
                        export PATH="$HOME/.local/bin:$PATH"
                        version="$(./gradlew -q --no-daemon properties --property version | sed -n 's/^version: //p')"
                        pom="https://telesphorepo.com/maven/plex-releases/dev/plex/api/$version/api-$version.pom"
                        if [ "${version%-SNAPSHOT}" = "$version" ] && curl -fsSI "$pom" > /dev/null; then
                            echo "Release $version is already published."
                        else
                            telesphorepo publish
                        fi
                    '''
                }
                withCredentials([string(credentialsId: "telesphorepo-id-token", variable: "OIDC_TOKEN")]) {
                    sh "./gradlew :server:publishUpdaterMetadata --no-daemon"
                }
            }
        }
    }
    post {
        always {
            archiveArtifacts artifacts: "build/libs/*.jar", fingerprint: true
            javadoc javadocDir: "api/build/docs/javadoc", keepAll: false
            discordSend description: "**Build:** ${env.BUILD_NUMBER}\n**Status:** ${currentBuild.currentResult}", enableArtifactsList: true, footer: "Built with Jenkins", link: env.BUILD_URL, result: currentBuild.currentResult, scmWebUrl: "https://github.com/plexusorg/Plex", showChangeset: true, title: env.JOB_NAME, webhookURL: env.WEBHOOK_URL
            discordSend description: "**Build:** ${env.BUILD_NUMBER}\n**Status:** ${currentBuild.currentResult}", enableArtifactsList: true, footer: "Built with Jenkins", link: env.BUILD_URL, result: currentBuild.currentResult, scmWebUrl: "https://github.com/plexusorg/Plex", showChangeset: true, title: env.JOB_NAME, webhookURL: env.TF_WEBHOOK_URL
            cleanWs()
        }
    }
}
