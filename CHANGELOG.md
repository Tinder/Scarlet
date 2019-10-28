Change Log
==========

## Version 0.1.9

_2019_07_15

**This release upgrades the project's JVM target to 1.8.** If you are building Android libraries or apps, adding this to your `build.gradle` to have [Java 8 language support](https://developer.android.com/studio/write/java8-support).

```Gradle
android {
  compileOptions {
    sourceCompatibility JavaVersion.VERSION_1_8
    targetCompatibility JavaVersion.VERSION_1_8
  }
}
```

* Fix: Target Java 8 bytecode.
* Fix: Use [StateMachine](https://github.com/Tinder/StateMachine) artifact from Maven Central. 

## Version 0.1.8

_2019_06_20

* Released Scarlet in Maven Central. `groupId` is now `com.tinder`. See all artifacts [here](https://search.maven.org/search?q=g:com.tinder.scarlet). 