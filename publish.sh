#!/usr/bin/env bash
[ -z "$BUILD_NUMBER" ] && echo "Build Number must be set as an environment variable" && exit 1;
[ -z "$ARTIFACTORY_USER" ] && echo "Artifactory User must be set as an environment variable" && exit 1;
[ -z "$ARTIFACTORY_PASSWORD" ] && echo "Artifactory Password must be set as an environment variable" && exit 1;

./gradlew clean

./gradlew scarlet-core:build scarlet-core:artifactoryPublish

./gradlew scarlet-core-internal:build scarlet-core-internal:artifactoryPublish

./gradlew scarlet:build scarlet:artifactoryPublish

./gradlew scarlet-test-utils:build scarlet-test-utils:artifactoryPublish

./gradlew scarlet-message-adapter-builtin:build scarlet-message-adapter-builtin:artifactoryPublish

./gradlew scarlet-message-adapter-gson:build scarlet-message-adapter-gson:artifactoryPublish

./gradlew scarlet-message-adapter-moshi:build scarlet-message-adapter-moshi:artifactoryPublish

./gradlew scarlet-message-adapter-protobuf:build scarlet-message-adapter-protobuf:artifactoryPublish

./gradlew scarlet-stream-adapter-builtin:build scarlet-stream-adapter-builtin:artifactoryPublish

./gradlew scarlet-stream-adapter-rxjava:build scarlet-stream-adapter-rxjava:artifactoryPublish

./gradlew scarlet-stream-adapter-rxjava2:build scarlet-stream-adapter-rxjava2:artifactoryPublish

./gradlew scarlet-stream-adapter-coroutines:build scarlet-stream-adapter-coroutines:artifactoryPublish

./gradlew scarlet-websocket-okhttp:build scarlet-websocket-okhttp:artifactoryPublish

./gradlew scarlet-websocket-mockwebserver:build scarlet-websocket-mockwebserver:artifactoryPublish

./gradlew scarlet-lifecycle-android:assemble scarlet-lifecycle-android:generatePomFileForAarPublication
./gradlew scarlet-lifecycle-android:artifactoryPublish
