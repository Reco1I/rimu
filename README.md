# rimu!

rimu! is a free-to-play circle clicking rhythm game for Android and desktop (Linux and Windows), built on
[Verktex](https://github.com/Reco1I/verktex), its own engine. It started as a fork of [osu!droid](https://github.com/osudroid/osu-droid),
the game hatched by the [osu!](https://osu.ppy.sh/home) community, and shares its folder with it: the beatmaps, skins and
replays in `osu!droid/` are rimu!'s too.

## Status

rimu! is under constant development. Features are being added and bugs fixed, but it's playable and fun!

Releases, with the Android APK and the desktop distributions, are on the [releases page](https://github.com/Reco1I/rimu/releases).

### Downloading the source code

Clone the repository with its submodules (Verktex is one):

```sh
git clone --recurse-submodules https://github.com/Reco1I/rimu.git
```

Open the folder in Android Studio or IntelliJ IDEA.

To update the source code to the latest commit, run the following commands inside the repository's directory:

```sh
git pull
git submodule update --init
```

### Building

rimu! requires Java 17, the Android NDK 22.1.7171670 and the android-36.1 SDK platform to build.

- Android: `./gradlew assembleDebug` builds the debug APK in `androidApp/build/outputs/apk/`.
- Desktop: `./gradlew :desktopApp:run` runs the game, and `./gradlew :desktopApp:distZip` builds a distribution
  (`bin/rimu`), which needs a Java 17 runtime.
- Tests: `./gradlew :shared:desktopTest`.

## Contributing

We welcome any sort of contributions, as long as they're helpful. Those who aren't able to contribute code may instead
suggest changes or report issues through [GitHub issues](https://github.com/Reco1I/rimu/issues).

## License

rimu! is licensed under the [Apache-2.0](https://opensource.org/licenses/Apache-2.0) License, as osu!droid is. Please see
the LICENSE file for more information.
