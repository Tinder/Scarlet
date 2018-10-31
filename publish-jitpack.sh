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

./gradlew scarlet-protocol-mqtt:build scarlet-protocol-mqtt:publishToMavenLocal

./gradlew scarlet-protocol-socketio-client:build scarlet-protocol-socketio-client:publishToMavenLocal

./gradlew scarlet-protocol-socketio-mockserver:build scarlet-protocol-socketio-mockserver:publishToMavenLocal

./gradlew scarlet-protocol-sse-okhttp:build scarlet-protocol-sse-okhttp:publishToMavenLocal

./gradlew scarlet-protocol-stomp:build scarlet-protocol-stomp:publishToMavenLocal

./gradlew scarlet-protocol-websocket-mockserver:build scarlet-protocol-websocket-mockserver:publishToMavenLocal

./gradlew scarlet-protocol-websocket-okhttp:build scarlet-protocol-websocket-okhttp:publishToMavenLocal

./gradlew scarlet-lifecycle-android:assemble scarlet-lifecycle-android:generatePomFileForAarPublication
./gradlew scarlet-lifecycle-android:publishToMavenLocal
