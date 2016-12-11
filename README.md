#Sunshine weather app
Synchronizes weather information from OpenWeatherMap on Android Phones and Tablets. It also contains custom watch face for android wear.

##Installation Instuctions
1. Download `google-services.json` for this project and place it in app module. You can generate it [here](https://support.google.com/firebase/answer/7015592).
2. Add Open weather map api key in `app/build.gradle and sunshinewear/build.gradle` files.
3. Also, add google places api key in `app/src/main/res/values/strings.xml` file.
4. Install the watchface by running this command in terminal in the project directory : `./gradlew :sunshinewear:installDebug`
5. Set the watch face manually in emulator.
6. Make sure that the android device is properly paired with the android wear. You can pair your device with android wear (emulator) using [these instructions](https://developer.android.com/training/wearables/apps/creating.html#SetupEmulator).
7. Install the android app in your device using `./gradlew :app:installDebug` command in terminal in the main directory of the project.
8. Now whenever the android application updates its weather data, it will send the current weather info to the android wear via a dataItem and notify the android wear to update its weather data asap. This will show latest weather info on the android wear watch face.

##Watch face screenshot
<img src="https://github.com/crazyhitty/sunshine/blob/master/screenshots/sunsine_watch_face.png" alt="alt text" width="400">

Initially forked from : https://github.com/udacity/Advanced_Android_Development
