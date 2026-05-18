# MyFit Android

Native Android-приложение MyFit на Kotlin + Jetpack Compose.

Это переписанная с нуля версия PWA-проекта (https://github.com/maxshplmax/MyFit), главная цель — интеграция с Samsung Health и Galaxy Watch через Health Connect.

## Стек

- Kotlin
- Jetpack Compose
- Room (локальная БД)
- WorkManager (фоновые задачи)
- Health Connect API (интеграция с Samsung Health)
- Material Design 3

## Минимальные требования

- Android 8.0+ (API 26)
- Установленный Health Connect (для фичей здоровья)

## Native Android build

### Открыть проект

1. Установить Android Studio (последняя stable)
2. Открыть папку проекта через File → Open
3. Дождаться окончания Gradle Sync

### Сборка debug-APK

\\\
.\gradlew assembleDebug
\\\

Готовый APK будет в \pp/build/outputs/apk/debug/\.

### Запуск на устройстве

С подключённым телефоном (USB Debugging on):

\\\
.\gradlew installDebug
\\\

Или нажми ▶ Run в Android Studio.

### Сборка release-APK

\\\
.\gradlew assembleRelease
\\\

Требует настроенный keystore — см. KAN-12 в Jira.

## Статус проекта

Эпик: [KAN-4](https://maksymshestovytskyi.atlassian.net/browse/KAN-4)

См. [Jira](https://maksymshestovytskyi.atlassian.net/projects/KAN) для всех Stories.
