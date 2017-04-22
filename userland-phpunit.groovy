node() {
    stage('Checkout') {
        checkout(
            [
                $class: 'GitSCM', 
                branches: [[name: 'master']], 
                doGenerateSubmoduleConfigurations: false, 
                extensions: [[$class: 'WipeWorkspace']], 
                submoduleCfg: [], 
                userRemoteConfigs: [[url: 'https://github.com/sebastianbergmann/phpunit.git']]
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

    stage('Run Composer') {
        def expectedSignature = sh([script:"wget -q -O - https://composer.github.io/installer.sig", returnStdout:true]).trim();
        sh("wget -q -O - https://getcomposer.org/installer > composer-setup.php");
        def actualSignature = sh([script:"php-install/bin/php -r \"echo hash_file('SHA384', 'composer-setup.php');\"", returnStdout:true]).trim();

        if (expectedSignature != actualSignature) {
            error("Invalid composer installer signature");
        }

        sh("php-install/bin/php composer-setup.php --quiet");

        sh("php-install/bin/php composer.phar install");
    }

    stage('Run Tests') {
        try {
            sh("php-install/bin/php phpunit --log-junit=phpunit-junit.xml");
        }
        finally {
            junit('*junit.xml');
        }
    }
}