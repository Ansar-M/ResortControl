# План подготовки проекта для GitHub (Портфолио)

Чтобы проект выглядел профессионально в портфолио, нам нужно подготовить качественную документацию и убедиться, что в репозиторий не попадет лишний «мусор» или конфиденциальные данные.

## Требуется обзор пользователя

> [!IMPORTANT]
> **Конфиденциальность**: Файл `google-services.json` содержит конфигурацию вашего проекта Firebase. Обычно его не выкладывают в открытый доступ. Я предлагаю добавить его в `.gitignore`, а в инструкции написать, как пользователю создать свой файл.
> **README**: Я создам структуру описания проекта на русском и английском языках, чтобы он был понятен широкой аудитории.

## Предлагаемые изменения

### Документация

#### [NEW] [README.md](file:///C:/Users/ocloc/AndroidStudioProjects/ResortControl2/README.md)
- Создать подробный файл с описанием:
    - Название и скриншот (заглушка для скриншота).
    - Описание функций (синхронизация, управление базами/домиками, авторизация Google).
    - Стек технологий (Jetpack Compose, Room, Firebase, WorkManager, Material 3 Adaptive).
    - Инструкция по запуску.

### Конфигурация Git

#### [MODIFY] [.gitignore](file:///C:/Users/ocloc/AndroidStudioProjects/ResortControl2/.gitignore)
- Добавить `google-services.json` в список игнорируемых файлов.
- Добавить игнорирование папки `.artifacts` (где лежат наши рабочие планы).
- Добавить стандартные правила для Android Studio и Gradle.

### Код

#### [MODIFY] [MainActivity.kt](file:///C:/Users/ocloc/AndroidStudioProjects/ResortControl2/app/src/main/java/com/example/resortcontrol/MainActivity.kt)
- Вынести `webClientId` в строковые ресурсы или `BuildConfig`, чтобы его было проще заменить другому разработчику.

## План действий для пользователя (вне кода)

1. **GitHub**: Создать новый публичный репозиторий на github.com.
2. **Инициализация**:
    ```bash
    git init
    git add .
    git commit -m "Initial commit: Resort Control App"
    git branch -M main
    git remote add origin https://github.com/ВАШ_НИК/ResortControl.git
    git push -u origin main
    ```
3. **Скриншоты**: Сделать 2-3 красивых скриншота и положить их в папку `screenshots` (я создам её в README).

## Верификация

### Ручная проверка
- Убедиться, что `git status` не показывает лишних файлов.
- Проверить, что `README.md` отображается корректно и содержит все ключевые технологии.
