# NFC Habit Tracker

NFC Habit Tracker is an Android application designed to help users manage their daily habits. It allows users to create habits, set notifications, and snooze habits using NFC tags. The app leverages Android's notification system and NFC capabilities to provide a seamless habit-tracking experience.

## Features

- Create and manage daily habits.
- Set notifications for specific days and times.
- Mark habits as completed using NFC tag scans.
- View and delete habits from a list.
- Supports exact reminder scheduling for notifications.

## File Structure

The project is organized as follows:

```
NFC-Habit-Tracker/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/nfchabittracker/
│   │   │   │   ├── MainActivity.kt          # Main screen of the app
│   │   │   │   ├── CreateHabitActivity.kt   # Activity to create new habits
│   │   │   │   ├── Habit.kt                 # Data model for habits
│   │   │   │   ├── HabitBroadcastReceiver.kt # Handles habit notifications
│   │   │   │   ├── FirstFragment.kt         # Example fragment
│   │   │   │   ├── SecondFragment.kt        # Example fragment
│   │   │   ├── res/
│   │   │   │   ├── layout/                  # XML layout files
│   │   │   │   ├── values/                  # App themes, strings, and dimensions
│   │   │   │   ├── drawable/                # App icons and backgrounds
│   │   │   │   ├── navigation/              # Navigation graph
│   │   │   ├── AndroidManifest.xml          # App manifest file
│   ├── build.gradle.kts                     # Module-level Gradle configuration
├── build.gradle.kts                         # Project-level Gradle configuration
├── settings.gradle.kts                      # Gradle settings
├── gradle/                                  # Gradle wrapper files
```

## How to Run

### Prerequisites

- **Android Studio**: Download and install [Android Studio](https://developer.android.com/studio).
- **Git**: Ensure Git is installed to clone the repository.
- **Android Device or Emulator**: Use a physical Android device or an emulator with NFC support.

### Running in Android Studio

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/your-username/NFC-Habit-Tracker.git
   cd NFC-Habit-Tracker
   ```

2. **Open the Project**:
   - Launch Android Studio.
   - Select `File > Open` and navigate to the `NFC-Habit-Tracker` folder.

3. **Build the Project**:
   - Click on the `Build` menu and select `Make Project`.

4. **Run the App**:
   - Connect an Android device or start an emulator.
   - Click the green "Run" button or press `Shift + F10`.

5. **Grant Permissions**:
   - Ensure the app has permissions for notifications, NFC, and exact alarms.
