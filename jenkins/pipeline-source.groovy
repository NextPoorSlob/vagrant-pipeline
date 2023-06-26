pipeline {
  agent any
  tools {
    maven "MAVEN3"
    jdk "Oracle8JDK"
  }

  stages {
    stage('Fetch code') {
      steps {
        git branch: 'vp-rem', url: 'https://github.com/devopshydclub/vprofile-repo.git'
      }
    }

    stage('Build') {
      steps {
        sh 'mvn install -DskipTests'
      }

      post {
        success {
          echo 'Now archiving file...'
          archiveArtifacts artifacts: '**/target/*.war'
        }
      }
    }

    stage ('Unit Test') {
      steps {
        sh 'mvn test' 
      }
    }

    stage('Checkstyle Analysis') {
      steps {
        sh 'mvn checkstyle:checkstyle'
      }
    }

    stage('Sonar Analysis') {
      environment {
        scannerHome = tool 'sonar4.8'
      }
      steps {
        withSonarQubeEnv('sonar') {
          sh '''${scannerHome}/bin/sonar-scanner -Dsonar.projectKey=vprofile \
                   -Dsonar.projectName=vprofile \
                   -Dsonar.projectVersion=1.0 \
                   -Dsonar.sources=src/ \
                   -Dsonar.java.binaries=target/test-classes/com/visualpathit/account/controllerTest/ \
                   -Dsonar.junit.reportsPath=target/surefire-reports/ \
                   -Dsonar.jacoco.reportsPath=target/jacoco.exec \
                   -Dsonar.java.checkstyle.reportPaths=target/checkstyle-result.xml'''
        }
      }
    }

    stage('Quality Gate') {
      steps {
        timeout(time: 1, unit: 'HOURS') {
          waitForQualityGate abortPipeline: true
        }
      }
    }

    stage('Upload Artifact') {
        steps {
            nexusArtifactUploader(
                nexusVersion: 'nexus3',
                protocol: 'http',
                nexusUrl: '192.168.33.11:8081',
                groupId: 'QA',
                version: "${env.BUILD_ID}-${env.BUILD_TIMESTAMP}",
                repository: 'vprofile-repo',
                credentialsId: 'nexuslogin',
                artifacts: [
                    [artifactId: 'vproapp',
                    classifier: '',
                    file: 'target/vprofile-v2.war',
                    type: 'war']
                ]
            )
        }
    }
  }
  post {
    always {
        echo "Success notification: ${currentBuild.currentResult}"
    }
  }
}
