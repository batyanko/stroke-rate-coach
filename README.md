Android rowing app that beeps at a desired stroke rate per minute (SPM).

PROJECT IS IN DEVELOPMENT. Feel free to contribute or request new features.

Get it on Google Play:

[https://play.google.com/store/apps/details?id=com.batyanko.strokeratecoach](https://play.google.com/store/apps/details?id=com.batyanko.strokeratecoach)

![giphy](https://media.giphy.com/media/3o7WItSv8U3WpZ0Ums/giphy.gif)

New in 1.20:
- Workout preset edit/copy
- Backup of all presets
- Beep sound option

Current features:
- Workouts based on stroke count, distance (GPS Location) and time
- Preset workouts, for example 4 legs of 50 strokes at SPM 22, 24, 26, 28
- Workout history/logbook
- Background beeping service allowing combined use with other sports apps
- Speed monitor & control (low speed warning)

Planned features:
- Share workouts with friends?

Known bugs:
- Moto G (falcon) with Cyanogenmod 13. Unable to replicate on other devices or API versions.
- Location listener sometimes persists in the background thread SOLVED?
- you tell me

## Motivation

As opposed to measuring SPM, which is already offered by numerous other Android apps, this app is aimed to actually set it by beeping, i.e. acting as metronome.

This is meant to enable coaches and athletes to explore new ways of workout, possibly using auditory stimulus to increase concentration in training.

## Installation

This is an ordinary Android Studio project, it should work by simply downloading it and opening the build.gradle file in Android studio with default settings.

## Contributors

Just drop me a line if you have any feedback or desire to contribute.

## License

This project is being developed under the Apache 2.0 license ( http://www.apache.org/licenses/LICENSE-2.0 ).

Parts of code from other developers have been used and annotated where necessary. These parts are licensed under either Apache 2.0 or CC BY-SA 3.0 ( http://creativecommons.org/licenses/by-sa/3.0/ )
