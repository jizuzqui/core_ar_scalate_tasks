#!groovy
// vim: ft=groovy
// These 2 need to be authorized using jenkins script approval
// http://your.jenkins.host/scriptApprovald/
import groovy.json.JsonOutput
import java.util.Optional
import java.util.LinkedHashMap

////////////////////////////////////////
// MODIFY YOUR PROJECT VARIABLES
////////////////////////////////////////

/********** User defined variables ****/
uuaa = "events"							// example: qnos
countryRepoPrefix = "bgba" // example: bgba for Spain 
repoName = "core_ar_scalate_tasks"					// example: BPMS_QNOS_QNOS
component = "core-libs"						 // example: process
architectureVersion = null // example: 1.0.0 - Only applys to process component
dockerImageVersion = null // example: 0.0.1 - Only applys to process component
////////////////////////////////////////
// DO NOT MODIFY FURTHER THIS POINT
////////////////////////////////////////

/********** Environment variables **********/
node ('bpm'){
  globalrepo = "${env.globalrepo}"
  globalrepo_credentials_id = "${env.globalrepo_credentials_id}"
  projVerRepo = "${env.proj_ver_repo}"
  projVerRepoBucket = "${env.proj_ver_repo_bucket}"
  jenkinsProjVerCredentials = "${env.jenkins_proj_ver_credentials}"
  jenkinsSshCredentials = "${env.jenkins_ssh_credentials}"
  jenkinsArtifactoryCredentials = "${env.jenkins_artifactory_credentials}"
  artifactoryServer = "${env.artifactory_server}"
  workspace = "${env.WORKSPACE}"
}

/********** Global variables **********/
globalrepoBranch = "develop"
bpm_node = "bpm"

/********** Project variables **********/
def repoParams = [
        "uuaa" : uuaa,
        "countryRepoPrefix": countryRepoPrefix,
        "component" : component,
        "repoName" : repoName,
		"architectureVersion" : architectureVersion,
        "dockVer" : dockerImageVersion,
        "repoBranch" : "${env.BRANCH_NAME}",
        "workspace" : "${workspace}"
]

/************ CI Flow ************/

stage("Checkout Global Library") {
  fileLoader.withGit(globalrepo, globalrepoBranch, globalrepo_credentials_id, bpm_node) {
    bpm_ci = fileLoader.load('src/bpm/ci/ci');
  }
}

bpm_ci.bpm_ci_flow(repoParams)
