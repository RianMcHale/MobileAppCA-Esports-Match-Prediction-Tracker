# Mobile App Development - CA1

## Esports Match Prediction Tracker

A simple Android app (Kotlin) for viewing CS2 matches and predicting which team will win. Match data and official results come from the [PandaScore API](https://pandascore.co/).

## Current Features

- Home screen with buttons for **Matches** and **Prediction History**.
- Match list split into **Upcoming**, **Live** and **Completed** tabs, loaded live from PandaScore.
  - Start times are shown in the device's local time.
  - Completed matches show the winner (or "result unavailable" if none is given).
- Tap an upcoming match to choose a predicted winner.
  - A team must be selected before saving.
  - One prediction per match; the choice is locked once saved.
- Prediction history screen:
  - Lists saved picks, newest first.
  - Checks PandaScore for official winners each time the screen opens.
  - Marks each pick as correct, incorrect or pending.
  - Shows overall accuracy (correct / completed, as a percentage). Pending matches are not counted.

## Project Structure

```
app/src/main/java/org/setu/esportstracker/
├── main/
│   └── MainActivity.kt               # Home screen and navigation
├── matches/
│   ├── EsportsMatch.kt               # Match data model
│   ├── MatchCategory.kt              # Upcoming / Live / Completed tabs and their API paths
│   ├── MatchStore.kt                 # Match store interface
│   ├── PandaScoreMatchStore.kt       # Fetches and parses matches from PandaScore
│   ├── PandaScoreResultService.kt    # Fetches official winners for saved predictions
│   ├── InMemoryMatchStore.kt         # Original fixed HLTV sample data (no longer used by the UI)
│   ├── MatchAdapter.kt               # RecyclerView adapter for match cards
│   └── MatchListActivity.kt          # Tabbed match list screen
└── predictions/
    ├── Prediction.kt                 # Prediction data model
    ├── PredictionStore.kt            # Prediction store interface
    ├── InMemoryPredictionStore.kt    # Stores predictions while the app is running
    ├── PredictionAccuracy.kt         # Calculates accuracy from completed predictions
    ├── PredictionActivity.kt         # Pick-a-winner form
    └── PredictionHistoryActivity.kt  # History and accuracy screen

app/src/main/res/layout/
├── activity_main.xml
├── activity_match_list.xml
├── activity_prediction.xml
├── activity_prediction_history.xml
├── item_match.xml                    # Match card
└── item_prediction.xml               # History card
```

## Setup

1. Clone the repository and open it in Android Studio.
2. Create a free account at [PandaScore](https://pandascore.co/) to get an API token.
3. Create a `.env` file in the project root (next to `settings.gradle.kts`):

   ```
   PANDASCORE_TOKEN=your_token_here
   ```

4. Sync Gradle and run the **debug** build on an emulator or device (min SDK 24).

The token is read at build time into `BuildConfig.PANDASCORE_TOKEN`. `.env` is listed in `.gitignore`, so the token is not committed. Without a token, the app shows a "token missing" message instead of matches.

## Notes

- Predictions are stored in memory only, so they are lost when the app process stops.
- Only debug builds receive the API token; release builds currently have an empty token.
- The Completed tab loads the 50 most recent finished matches.
- The token is compiled into the debug APK, which is fine for a college project but not for a published app.

## AI Usage Disclaimer

AI tools were used in the following parts of this project:

- **Code comments:** Codex was used to write simplified comments across the entire project, so the code is clearer for anyone reading it.
- **GitHub issues:** Codex was used to help write issue names and descriptions.
- **GitHub Releases:** Codex was used to help write release descriptions.
- **Planning:** Codex was used to help plan the project out step by step.
- **README.md:** Claude was used to analyze the project repository and create this README.

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
