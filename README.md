## Synopsis

An Android rowing app that beeps at a desired stroke rate per minute (SPM).

PROJECT IS UNDER DEVELOPMENT. Feel free to contribute or request new features.

![](https://i.imgur.com/3MQ42eM.gif)

Current features:
- Preset workouts
- Option to add new workouts, for example 4 legs of 50 strokes at SPM 22, 24, 26, 28.

Planned features:
- Workout history/logbook
- Background beeping service  - in order to combine with other sports apps.
- Workouts based on distance and time

Known bugs:
- Speed button shows speed but crashes the app on first use because of location permission missing. After location permission is given, speed shows fine.
- you tell me.


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