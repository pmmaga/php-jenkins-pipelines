node() {
    def shortCommit;

    stage('Checkout') {
        checkout(
            [
                $class: 'GitSCM', 
                branches: [[name: BRANCH]], 
                doGenerateSubmoduleConfigurations: false, 
                extensions: [[$class: 'WipeWorkspace']], 
                submoduleCfg: [], 
                userRemoteConfigs: [[url: 'https://github.com/php/php-src.git']]
            ]
        );
        sh 'git rev-parse HEAD > GIT_COMMIT'
        shortCommit = readFile('GIT_COMMIT').take(6)
    }

    stage('Configure') {
        sh('./buildconf --force');
        def debugConfigure = '--enable-debug';
        if(DEBUG != 'true') {
            debugConfigure = '';
        }
        def ztsConfigure = '--enable-maintainer-zts';
        if(MAINTAINERZTS != 'true') {
            ztsConfigure = '';
        }
        sh("./configure --prefix=${WORKSPACE}/php-install ${debugConfigure} ${ztsConfigure}");
    }

    stage('Build') {
        sh('make -j2');
        sh('make install');
    }

    stage('Save Artifact') {
        def debugZipName = 'DEBUG';
        if(DEBUG != 'true') {
            debugZipName = 'RELEASE';
        }
        def ztsZipName = 'MAINTAINERZTS';
        if(MAINTAINERZTS != 'true') {
            ztsZipName = 'NTS';
        }
        zip([zipFile:"php-${BRANCH}-${shortCommit}-${debugZipName}-${ztsZipName}.zip", dir:'php-install', archive: true]);
        fingerprint("php-${BRANCH}-${shortCommit}-${debugZipName}-${ztsZipName}.zip");
    }
}
