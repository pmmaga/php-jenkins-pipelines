node() {
    stage('Checkout') {
        checkout(
            [
                $class: 'GitSCM', 
                branches: [[name: BRANCH]], 
                doGenerateSubmoduleConfigurations: false, 
                extensions: [[$class: 'WipeWorkspace']], 
                submoduleCfg: [], 
                userRemoteConfigs: [[url: 'https://github.com/php/php-langspec.git']]
            ]
        );
    }

    stage('Unpack Build') {
        def selector = [$class: 'LastCompletedBuildSelector'];
        step ([$class: 'CopyArtifact', projectName: "php-src/${BRANCH}/build", selector: selector, parameters: "BRANCH=${BRANCH}"]);
        def zipName = sh([script:"find -name php-*.zip", returnStdout:true]).trim();
        fingerprint("php-*.zip");
        unzip([dir: 'php-install', zipFile: zipName]);
        sh("chmod +x ./php-install/bin/php");
    }

    stage('Run Tests') {
        try {
            sh('SKIP_IO_CAPTURE_TESTS=1 NO_INTERACTION=1 TEST_PHP_JUNIT=langspec-junit.xml REPORT_EXIT_STATUS=1 ./php-install/bin/php ./php-install/lib/php/build/run-tests.php -P -g "FAIL,XFAIL,BORK,WARN,LEAK,SKIP" --show-diff');
        }
        finally {
            junit('*junit.xml');
        }
    }
}
