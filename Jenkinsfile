// Hospital Management System - CI/CD pipeline
//
// Flow: GitHub -> Jenkins -> Maven (build + test) -> Apache Tomcat.
//
// The pipeline runs on either a Windows or a Linux agent: every shell step goes
// through the runCmd/capture helpers below, which pick `bat` or `sh` at runtime.
//
// Required Jenkins configuration:
//
//   Manage Jenkins > Tools
//     - JDK installation        named 'jdk17'
//     - Maven installation      named 'maven3'
//
//   Manage Jenkins > Credentials  ("Username with password")
//     - ID 'tomcat-manager' : a user holding the manager-script role in tomcat-users.xml
//
//   Manage Jenkins > Plugins
//     - Pipeline Utility / HTML Publisher (for the coverage report), JUnit, Git
//
//   Pipeline job > Build Triggers
//     - "GitHub hook trigger for GITScm polling", with a matching webhook on the
//       GitHub repository pointing at http://<jenkins-host>/github-webhook/

/** Runs a command on whichever platform the agent is, failing the build on a non-zero exit. */
def runCmd(String command) {
    if (isUnix()) {
        sh command
    } else {
        bat command
    }
}

/** Runs a command and returns its stdout, or an empty string if it failed. */
def capture(String command) {
    try {
        if (isUnix()) {
            return sh(script: command, returnStdout: true).trim()
        }
        // '@' stops cmd echoing the command itself, leaving only the real output.
        return bat(script: "@${command}", returnStdout: true).trim()
    } catch (ignored) {
        return ''
    }
}

pipeline {

    agent any

    environment {
        // Use absolute paths to JDK and Maven - no Jenkins tool config needed
        JAVA_HOME = 'C:\\Program Files\\Java\\jdk-25.0.4'
        MAVEN_HOME = 'C:\\Users\\saish\\AppData\\Local\\Temp\\claude\\c--Users-saish-OneDrive-Desktop-Hospital-management-system\\51c33365-fd54-45a1-bd3e-63d5fef5e512\\scratchpad\\maven\\apache-maven-3.9.16'
        PATH = "${env.MAVEN_HOME}\\bin;${env.JAVA_HOME}\\bin;${env.PATH}"
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '15'))
        timeout(time: 20, unit: 'MINUTES')
        disableConcurrentBuilds()
    }

    environment {
        APP_NAME   = 'hms'
        WAR_FILE   = 'target/hms.war'

        // Tomcat is on 9090, not its default 8080, because Jenkins itself defaults to
        // 8080 and the two cannot share a port. Set the connector port in
        // <tomcat>/conf/server.xml to match, or change both values here.
        TOMCAT_URL = 'http://localhost:9090'
        HEALTH_URL = 'http://localhost:9090/hms/health'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                script {
                    // Record the commit under test, so a build in the history can be
                    // traced back to a change without digging through logs.
                    def sha = env.GIT_COMMIT ? env.GIT_COMMIT.take(7) : 'unknown'
                    currentBuild.description = "commit ${sha}"
                }
            }
        }

        stage('Build') {
            steps {
                // Compile only. Packaging happens after the tests pass, so a failing
                // build never produces a WAR that could be deployed by mistake.
                runCmd 'mvn -B -ntp clean compile'
            }
        }

        stage('Test') {
            steps {
                runCmd 'mvn -B -ntp test'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml',
                          allowEmptyResults: false
                    publishHTML(target: [
                        reportDir            : 'target/site/jacoco',
                        reportFiles          : 'index.html',
                        reportName           : 'Coverage Report',
                        keepAll              : true,
                        alwaysLinkToLastBuild: true,
                        allowMissing         : true
                    ])
                }
            }
        }

        stage('Package') {
            steps {
                runCmd 'mvn -B -ntp package -DskipTests'
                archiveArtifacts artifacts: 'target/hms.war', fingerprint: true
            }
        }

        stage('Deploy to Tomcat') {
            // Only the mainline is deployed; feature branches stop after packaging.
            when {
                branch 'main'
            }
            steps {
                // Keep the WAR that is currently live before replacing it. Without
                // this there is nothing to roll back TO, which is why a rollback
                // stage bolted on at the end of a pipeline usually cannot work.
                script {
                    env.ROLLBACK_WAR = ''
                    def previous = "${env.JENKINS_HOME}/hms-last-good/hms.war"

                    if (fileExists(previous)) {
                        env.ROLLBACK_WAR = previous
                        echo "Previous known-good WAR available for rollback: ${previous}"
                    } else {
                        echo 'No previous known-good WAR yet; this build cannot roll back.'
                    }
                }

                withCredentials([
                    usernamePassword(credentialsId: 'tomcat-manager',
                                     usernameVariable: 'TOMCAT_USER',
                                     passwordVariable: 'TOMCAT_PASS')
                ]) {
                    script {
                        // The credentials are referenced with native shell syntax rather than
                        // Groovy interpolation, which would bake the password into the job log.
                        //
                        // Manager's "deploy" with update=true replaces any existing deployment,
                        // so the same call serves the first build and every one after it.
                        if (isUnix()) {
                            sh '''
                                curl --fail --silent --show-error \
                                     --upload-file "$WAR_FILE" \
                                     --user "$TOMCAT_USER:$TOMCAT_PASS" \
                                     "$TOMCAT_URL/manager/text/deploy?path=/$APP_NAME&update=true"
                            '''
                        } else {
                            bat 'curl --fail --silent --show-error ' +
                                '--upload-file "%WAR_FILE%" ' +
                                '--user "%TOMCAT_USER%:%TOMCAT_PASS%" ' +
                                '"%TOMCAT_URL%/manager/text/deploy?path=/%APP_NAME%&update=true"'
                        }
                    }
                }
            }
        }

        stage('Smoke test') {
            when {
                branch 'main'
            }
            steps {
                script {
                    // Tomcat returns from the deploy call before the context has finished
                    // starting, so poll the health endpoint instead of checking once.
                    def healthy = false

                    for (int attempt = 1; attempt <= 20 && !healthy; attempt++) {
                        def body = capture("curl --fail --silent ${env.HEALTH_URL}")

                        if (body.contains('"status":"UP"')) {
                            echo "Application reported healthy on attempt ${attempt}."
                            healthy = true
                        } else {
                            echo "Waiting for the application to start (attempt ${attempt}/20)..."
                            sleep(time: 3, unit: 'SECONDS')
                        }
                    }

                    if (!healthy) {
                        // A DOWN response usually means Tomcat started the app but it
                        // cannot reach MySQL, so show whatever the endpoint last returned.
                        def last = capture("curl --silent ${env.HEALTH_URL}")
                        error("Application did not report healthy within 60 seconds. " +
                              "Last response: ${last}")
                    }
                }
            }
        }

        stage('Promote') {
            when {
                branch 'main'
            }
            steps {
                // The smoke test passed, so this WAR becomes the one a future
                // failed deployment rolls back to.
                script {
                    def store = "${env.JENKINS_HOME}/hms-last-good"
                    if (isUnix()) {
                        sh "mkdir -p '${store}' && cp target/hms.war '${store}/hms.war'"
                    } else {
                        bat "if not exist \"${store}\" mkdir \"${store}\" & " +
                            "copy /Y target\\hms.war \"${store}\\hms.war\""
                    }
                    echo "Recorded build ${env.BUILD_NUMBER} as the known-good version."
                }
            }
        }
    }

    post {
        success {
            echo "Build ${env.BUILD_NUMBER} succeeded. Deployed to ${env.TOMCAT_URL}/${env.APP_NAME}/"
        }

        failure {
            script {
                // Roll back only when a deployment actually reached the server and
                // then failed its smoke test. A compile or test failure never got
                // that far, so redeploying over a healthy server would be wrong.
                boolean deployed = currentBuild.result == 'FAILURE' && env.ROLLBACK_WAR
                boolean onMain = env.BRANCH_NAME == null || env.BRANCH_NAME == 'main'

                if (deployed && onMain) {
                    echo "Deployment failed its smoke test. Rolling back to the last known-good WAR."

                    withCredentials([
                        usernamePassword(credentialsId: 'tomcat-manager',
                                         usernameVariable: 'TOMCAT_USER',
                                         passwordVariable: 'TOMCAT_PASS')
                    ]) {
                        if (isUnix()) {
                            sh '''
                                curl --fail --silent --show-error \
                                     --upload-file "$ROLLBACK_WAR" \
                                     --user "$TOMCAT_USER:$TOMCAT_PASS" \
                                     "$TOMCAT_URL/manager/text/deploy?path=/$APP_NAME&update=true"
                            '''
                        } else {
                            bat 'curl --fail --silent --show-error ' +
                                '--upload-file "%ROLLBACK_WAR%" ' +
                                '--user "%TOMCAT_USER%:%TOMCAT_PASS%" ' +
                                '"%TOMCAT_URL%/manager/text/deploy?path=/%APP_NAME%&update=true"'
                        }
                    }

                    def body = capture("curl --silent ${env.HEALTH_URL}")
                    if (body.contains('"status":"UP"')) {
                        echo 'Rollback succeeded: the previous version is serving again.'
                    } else {
                        echo 'ROLLBACK DID NOT RECOVER THE SERVICE - manual intervention needed.'
                    }
                } else {
                    echo "Build ${env.BUILD_NUMBER} failed before deployment; nothing to roll back."
                }
            }
        }

        cleanup {
            cleanWs()
        }
    }
}
