# MyFit-Android Backlog

Единственный источник истины о том, что нужно сделать. Никакой Jira больше не используется.

**Формат:** `[ ]` не сделано, `[~]` в работе, `[x]` сделано.

**Workflow:** Agent читает этот файл, берёт первую `[ ]` задачу с выполненными зависимостями, согласует план с Lead, делает, открывает PR, ждёт apprоve Lead, обновляет статус.

---

## Закрыто ранее (для справки)

- [x] **B-0a** Setup Android Studio + Hello World (бывший KAN-5, закрыт 2026-05-17)
- [x] **B-0b** Room + справочник продуктов (бывший KAN-6, закрыт 2026-05-17)
- [x] **B-0c** Дневник: дашборд + еда + bottom nav 5 вкл. (бывший KAN-14, закрыт 2026-05-18)
- [x] **B-0d** Справочник активностей (бывший KAN-16, закрыт 2026-05-18)
- [x] **B-0e** Блок активностей в Дневнике + ActivityLog (бывший KAN-17, закрыт 2026-05-19)
- [x] **B-1** Дневник — переключатель даты и редактирование записей (закрыт 2026-05-20)
- [x] **B-tech-3** .gitignore для Android Studio артефактов (закрыт 2026-05-20)
- [x] **B-2a** План питания — экран План, CRUD приёмов, интеграция с Дневником (закрыт 2026-05-20)
- [x] **B-2b** История с графиками — KcalBarChart + MacroStackChart + click-through (закрыт 2026-05-20)
- [x] **B-3** Настройки — тема, цели КБЖУ, управление активностями, Об приложении (закрыт 2026-05-20)
- [x] **B-tech-1** UI Polish — format util, semantic split, rememberSaveable, orphan cleanup (закрыт 2026-05-20)
- [x] **B-4** Напоминания — AlarmManager + Notification + Settings UI (закрыт 2026-05-20)
- [x] **B-5a** Health Connect — TotalCalories в Дневнике (SDK + permissions + Android 14+ activity-alias + dashboard breakdown) (закрыт 2026-05-23)

**Этап 2 эпика "Rewrite на Kotlin/Compose" полностью закрыт.** Дневник функционален в MVP: учёт еды и активностей с реальным балансом калорий.

**Этап 3 эпика "Rewrite на Kotlin/Compose" полностью закрыт.** Дневник, План, История — все три core-экрана функциональны. Осталось: Настройки, Напоминания, Health Connect, Release.

---

## Активный backlog

### B-5: Health Connect — KILLER FEATURE  `[~]`

**Категория:** Главная цель эпика  
**Зависимости:** B-0e (нужен ActivityLog для интеграции данных)  
**Оценка:** большая (декомпозирована на B-5a / B-5b)

**Декомпозиция:**
- `[x]` **B-5a** — SDK + permissions + чтение TotalCaloriesBurned + интеграция с дашбордом + Android 14+ activity-alias `VIEW_PERMISSION_USAGE` (закрыт 2026-05-23)
- `[ ]` **B-5b** — Steps в UI / Privacy Policy text + GitHub Pages / "Открыть настройки HC" / debug helper / **UX-полировка надписи "Сожжено"** с учётом что цифра — Total (включая базовый обмен), не только активность; возможно ввести явное визуальное разделение "BMR + активность" или сделать pill/tooltip с пояснением

**Важное архитектурное решение (изменение плана B-5):** перешли с `ActiveCaloriesBurnedRecord` на `TotalCaloriesBurnedRecord`. Samsung Health в Health Connect пишет только Total (без выделения Active). Подтверждено на устройстве Lead'а (OnePlus + Galaxy Watch, 4 тренировки ходьбы → ActiveCalories=null, TotalCalories=полная цифра с BMR). Цифра "Сожжено" теперь содержит базовый метаболизм (~1700-2200 ккал/сутки на покое плюс активность). Психологически нужна UX-полировка — в B-5b.

**Что нужно (для B-5b):**
- Steps вывести в UI (читаются и кешируются с B-5a)
- Privacy Policy реальный текст + публикация на GitHub Pages (требование Play Store)
- "Открыть настройки Health Connect" intent-кнопка в Settings
- Debug helper `debugReadAllToLogcat()` под `BuildConfig.DEBUG`
- UX-полировка "Сожжено" с учётом Total + BMR
- Опционально (растягиваемый scope): пульс, сон

**AC оставшиеся для B-5b:**
- [ ] Privacy Policy опубликована на GitHub Pages (требование Health Connect)
- [ ] Steps отображаются в Дневнике или Истории

**AC закрытые в B-5a:**
- [x] Запрос permissions Health Connect при первом запуске функции
- [x] Чтение Steps за выбранную дату (читается, в UI пока не показываем)
- [x] Чтение TotalCaloriesBurned за выбранную дату (вместо ActiveCalories — Samsung не пишет Active в HC)
- [x] Дашборд показывает реальное "Сожжено" из Health Connect (плюс ручные ActivityLog из B-0e)
- [x] Опция в Настройках: вкл/выкл интеграцию с Health Connect
- [x] Корректная работа при отсутствии Samsung Health или отказе в permissions

**Технические заметки:**
- API: `androidx.health.connect.client:connect-client:1.1.0` (stable)
- Тест проводится на устройстве Lead'а (Samsung Health → Galaxy Watch уже синхронизированы).

**Известные ограничения (документировано 2026-05-24):**
- **Sliding window Samsung Health:** исторические данные за 7-30 дней в HC могут содержать только BMR baseline (без реальной активности). В Дневнике для таких дней может отображаться baseline ~1500-1700 ккал как "Сожжено". Текущий фикс `dataOrigins.isEmpty()` отсекает только даты ВНЕ окна Samsung Health (~30 дней назад и старше). Внутри окна Samsung Health прописывает себя в origins даже за пустые дни. Не критично для главного use case (сегодня + ближайшие 7 дней). UX может быть улучшен в [B-tech-5](#b-tech-5-hc-empty-day-detection-через-readrecords) через `readRecords(ActiveCaloriesBurnedRecord)` проверку наличия реальных записей активности.

---

### B-6: Release APK + распространение

**Категория:** Release  
**Зависимости:** B-5 (последняя фича MVP)  
**Оценка:** малая

**Что нужно:**
- Подписанный release APK
- ProGuard правила (если нужны)
- Версионирование (semver, v1.0.0 для первого релиза)
- Релиз на GitHub Releases с описанием
- README с инструкцией установки

**AC:**
- [ ] `./gradlew assembleRelease` собирает подписанный APK без ошибок
- [ ] ProGuard правила для Room/Compose/Health Connect не ломают runtime
- [ ] APK устанавливается на тестовое устройство, все фичи работают
- [ ] Tag v1.0.0 + GitHub Release с APK и changelog
- [ ] README обновлён с инструкцией для друзей: "скачайте APK, разрешите установку из неизвестных источников"

---

## Технический долг (приоритет ниже фич)

### B-tech-2: Case-insensitive UNIQUE для имён

**Что:** добавить `COLLATE NOCASE`-like решение для UNIQUE индексов на name в activities и products. Учесть, что NOCASE работает только для ASCII — нужен fallback через колонку `name_lower`.

**Источник:** ex-KAN-20.

**Технические заметки:** требует миграции БД.

---

### B-tech-CI: тестовая инфраструктура (заглушка)

**Что:** настроить unit-тесты и/или instrumentation tests для критических repository/flow паттернов.

**Известные сценарии для покрытия:**
- `HistoryRepository.observeRange` когда `activity_log` пуст за период → должен вернуть list с `kcalBurned=0` для каждого дня (не падать, не пропускать дни). Источник: B-5b диагностика 2026-05-24, Lead тестировал dashboard без записи ручных активностей, History показывала "Сжёг: 0" во всех днях — это было правильно, но потребовало pull БД + ручной SQL для подтверждения.
- `HealthConnectRepository.getOrRead(date.isAfter(today))` → должен вернуть `Empty` без HC запроса (clamp future, B-5b фикс).
- Cache invalidation/getOrRead с одинаковой датой из двух корутин одновременно (Mutex поведение).

**Технические заметки:** требует решения по test framework (junit4 уже подключён, нужны корутиновые тесты с `runTest` + fake DAO). Объём ≥ нескольких дней.

---

### B-tech-4: HC permission auto-sync

**Что:** при mismatch между `granted` и `required` permission set'ами автоматически вызывать `HealthConnectPreferencesRepository.setEnabled(false)`, чтобы Switch в Settings не врал юзеру.

**Сценарии где сейчас Switch остаётся в ON визуально, но фича не работает:**
1. Юзер revoke'нул permission в системных настройках HC → вернулся в MyFit
2. Мы расширили `HealthConnectRepository.PERMISSIONS` в новой версии приложения (например, добавили READ_HEART_RATE в B-5b или позже) — старые юзеры с partial grants попадут в этот кейс автоматически

**Где править:** `SettingsViewModel` — на ON_RESUME (рядом с `refreshAvailability`) добавить suspend-check `hasHealthConnectPermissions()` и, если enabled=true && granted=false → `setEnabled(false)`. Учесть race: после launcher → grant → setEnabled(true), ON_RESUME сразу после может опередить DataStore write. Простое решение — debounce 500мс или флаг "just granted".

**Сейчас mitigated:** Dashboard корректно показывает legacy формат через `hcBurnedKcal=null`. То есть фича не ломается, просто UI inconsistency.

**Источник:** обсуждение closure B-5a (2026-05-23).

---

### B-tech-5: HC empty day detection через readRecords

**Категория:** UX polish  
**Приоритет:** после v1.0.0  
**Источник:** B-5b диагностика на устройстве Lead'а 2026-05-24

**Что:** В `HealthConnectRepository.getOrRead` — для определения "пустого дня" (только BMR baseline без активности) использовать `readRecords(ActiveCaloriesBurnedRecord)` или `readRecords(ExerciseSessionRecord)` вместо текущей проверки `dataOrigins.isEmpty()`.

**Почему текущая логика недостаточна:** Samsung Health синхронизирует исторические шаги в HC даже за дни без реальной активности (внутри своего ~30-дневного sliding window). В таких случаях:
- `dataOrigins` НЕ пустой (Samsung Health прописывает себя как источник),
- `StepsRecord.COUNT_TOTAL` > 0 (фантомные исторические шаги),
- `TotalCaloriesBurnedRecord.ENERGY_TOTAL` ≈ 1564 ккал (только BMR baseline, без реальных Active calories).

Подтверждено на устройстве Lead'а: 10 мая 2026 — steps=6781, totalEnergy=1564.5, при этом Lead в тот день HC не использовал и часы не носил.

**План:**
1. Добавить permission `android.permission.health.READ_ACTIVE_CALORIES_BURNED` в AndroidManifest.xml + в `PERMISSIONS` set в `HealthConnectRepository`. Это НОВЫЙ permission, не равен `READ_TOTAL_CALORIES_BURNED` — Lead должен будет заново открыть HC dialog (UI это умеет через `hasAllPermissions()`).
2. В `getOrRead`: перед `aggregate()` сделать `readRecords(ActiveCaloriesBurnedRecord)` за то же время. Если пустой список → return `Empty` (только BMR fallback, реальной активности нет).
3. Альтернатива без новых permissions (проверить первой через диагностику): `readRecords(TotalCaloriesBurnedRecord)` — есть гипотеза, что HC возвращает 1564 как computed BMR fallback, а readRecords отдаёт только реально сохранённые записи. Если sum readRecords ≈ 0 для phantom day — используем этот вариант и permission не нужен.
4. Убрать проверку `dataOrigins.isEmpty()` (она остаётся как safety net или удаляется — решается по факту).
5. Прогнать debug helper `debugReadAllToLogcat()` на 4 разнесённых окнах, убедиться что phantom days корректно → Empty, а реальные дни → правильные значения.

**Технические заметки:** требует решения по permission (новый или нет) на основе диагностики через extended debug helper. Объём — 1-2 дня.

---

## Идеи на будущее (не приоритет)

- Кастомные иконки для активностей
- Категории продуктов (мясо, овощи, крупы...)
- Импорт/экспорт данных в JSON или CSV
- Виджет на главный экран Android с текущим балансом
- Wear OS компаньон для записи активности с часов
- Графики веса с интеграцией Health Connect

---

## Принципы работы с backlog'ом

1. **Один источник правды** — этот файл. Никакой Jira.
2. **Только agent редактирует статусы `[ ]/[~]/[x]`.** Lead добавляет новые задачи или меняет приоритет.
3. **Order matters** — порядок задач в активном backlog'е = приоритет.
4. **Новые задачи** добавляются в конец активного backlog'а с обоснованием.
5. **Завершённые** перемещаются в "Закрыто ранее" с датой.
