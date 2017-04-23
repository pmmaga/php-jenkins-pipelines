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

    stage('Setup Composer') {
        def expectedSignature = sh([script:"wget -q -O - https://composer.github.io/installer.sig", returnStdout:true]).trim();
        sh("wget -q -O - https://getcomposer.org/installer > composer-setup.php");
        def actualSignature = sh([script:"php-install/bin/php -r \"echo hash_file('SHA384', 'composer-setup.php');\"", returnStdout:true]).trim();

        if (expectedSignature != actualSignature) {
            error("Invalid composer installer signature");
        }

        sh("php-install/bin/php composer-setup.php --quiet");
    }

    stage('Run Composer Install') {
        def composerRootVersion = sh([script:"php-install/bin/php -r 'echo json_decode(file_get_contents(\"./composer.json\"), true)[\"extra\"][\"branch-alias\"][\"dev-master\"];'", returnStdout:true]).trim();
        sh("touch php.ini");
        sh("COMPOSER_ROOT_VERSION=${composerRootVersion} php-install/bin/php -c php.ini composer.phar install --no-interaction");
    }

    stage('Run Tests') {
        try {
            sh("php-install/bin/php -c php.ini phpunit --log-junit=phpunit-junit.xml");
        }
        finally {
            junit('*junit.xml');
        }
    }
}