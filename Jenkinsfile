#!groovy

@Library("workflowlibs@1.0.0") _

pipeline {
    agent {
        kubernetes {
            yaml """
apiVersion: v1
kind: Pod
metadata:
  labels:
    type: deployment
spec:
  securityContext:
    runAsUser: 1000
  containers:
  - name: maven
    image: globaldevtools.bbva.com:5000/hub/bpmaas/jenkins/maven:3.6.1
    command:
    - cat
    tty: true
    resources:
      requests:
        cpu: '1'
        memory: '2048Mi'
      limits:
        cpu: '1'
        memory: '2048Mi'
  imagePullSecrets:
  - name: registrypullsecret
"""
}
    }
    options {
        ansiColor('xterm')
        timestamps()
    }
    environment {
        GOCACHE = "${WORKSPACE}"
    }
    stages {
        stage('Checkout Global Library') {
            steps {
                script{
                    globalBootstrap {
                        libraryName   = "bpmaas-workflowlibs"
                        libraryBranch = "develop"
                        entrypointParams = [
                            type : "mavenproject",
                            project : "wrapper"
                        ]
                    }
                }
            }
        }
    }

    post {
        always {
            echo "We have been through the entire pipeline"
        }
        changed {
            echo "There have been some changes from the last build"
        }
        success {
            echo "Build successful"
        }
        failure {
            echo "There have been some errors"
        }
        unstable {
            echo "Unstable"
        }
        aborted {
            echo "Aborted"
        }
    }
}
