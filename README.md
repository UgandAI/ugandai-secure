# UgandAI

UgandAI is a mobile farming assistant designed to support rural farmers in Uganda by providing essential farming tools and information. The app integrates OpenAI's API for advanced language processing, offering personalized and context-aware guidance to help improve agricultural productivity in underserved communities like Mbale and Namatumba.

# Libraries used
- Koin for dependency injection
- Jetpack Compose and Material3 for UI design
- Languages: Java, Kotlin, Python, JSON

# Features
- AI-Powered Farming Assistant: Uses OpenAI's API to provide intelligent, real-time farming advice.

- User-Friendly Mobile Interface: Developed with Java and Kotlin for a smooth Android experience.

- Backend and API Integration: Efficiently handles data processing and user interactions.

# Demo
[device-2023-04-29-121149.webm](https://user-images.githubusercontent.com/5604165/235278948-49e01143-1090-4d79-8310-7449466faaab.webm)

# Local backend

The debug configuration connects an Android emulator to the backend running on
the development computer at `http://10.0.2.2:8000`. Start the canonical backend
first, then build and run the app from Android Studio or with
`./gradlew assembleDebug`. OpenAI credentials belong only in the backend
environment; the Android app does not contain an OpenAI API key.

For a physical Android device, change `NetworkConfig.BASE_URL` to the development
computer's reachable LAN address.

Enjoy!

# Contribution
Feel free to contact me at donatienthorez@gmail.com or make a PR to this repo.
