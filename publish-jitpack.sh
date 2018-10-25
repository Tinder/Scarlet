#!/usr/bin/env bash

./gradlew clean

./gradlew scarlet-core:build scarlet-core:publishToMavenLocal

./gradlew scarlet-core-internal:build scarlet-core-internal:publishToMavenLocal

./gradlew scarlet:build scarlet:publishToMavenLocal

./gradlew scarlet-test-utils:build scarlet-test-utils:publishToMavenLocal

./gradlew scarlet-message-adapter-builtin:build scarlet-message-adapter-builtin:publishToMavenLocal

./gradlew scarlet-message-adapter-protobuf:build scarlet-message-adapter-protobuf:publishToMavenLocal

./gradlew scarlet-message-adapter-moshi:build scarlet-message-adapter-moshi:publishToMavenLocal

./gradlew scarlet-message-adapter-gson:build scarlet-message-adapter-gson:publishToMavenLocal

./gradlew scarlet-message-adapter-jackson:build scarlet-message-adapter-jackson:publishToMavenLocal

./gradlew scarlet-stream-adapter-builtin:build scarlet-stream-adapter-builtin:publishToMavenLocal

./gradlew scarlet-stream-adapter-rxjava2:build scarlet-stream-adapter-rxjava2:publishToMavenLocal

./gradlew scarlet-stream-adapter-rxjava:build scarlet-stream-adapter-rxjava:publishToMavenLocal

./gradlew scarlet-stream-adapter-coroutines:build scarlet-stream-adapter-coroutines:publishToMavenLocal

./gradlew scarlet-websocket-okhttp:build scarlet-websocket-okhttp:publishToMavenLocal

./gradlew scarlet-websocket-mockwebserver:build scarlet-websocket-mockwebserver:publishToMavenLocal

./gradlew scarlet-lifecycle-android:assemble scarlet-lifecycle-android:generatePomFileForAarPublication
./gradlew scarlet-lifecycle-android:publishToMavenLocal
