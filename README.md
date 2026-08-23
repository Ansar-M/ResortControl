# ResortControl

ResortControl is a professional Android application designed for recreation center administrators to manage properties, cabins, and guest bookings. This project features full cloud synchronization, offline persistence, and a modern, adaptive user interface.

## Overview

The application provides a centralized platform to manage multiple recreation centers. Administrators can track cabin availability in real-time, manage property details, and handle reservations through an intuitive calendar interface. 

## Key Features

*   **Cloud Synchronization**: Built-in Firebase integration (Firestore, Storage) with a robust "Push-before-Pull" logic to ensure data consistency across multiple devices.
*   **Manual Refresh**: Support for "Pull-to-Refresh" gesture on the main screen to manually trigger synchronization.
*   **Multi-Center Management**: Create and organize multiple recreation areas within a single Google account.
*   **Interactive Booking Calendar**: A custom visualization system for bookings that helps prevent scheduling conflicts with smart color-coding.
*   **Automatic Image Optimization**: Integrated photo management that optimizes image size before uploading to the cloud to save bandwidth and storage.
*   **Offline First**: Uses Room database for full offline functionality; changes are automatically synced when the connection is restored via WorkManager.
*   **Adaptive Design**: A responsive interface built with Material 3 that scales perfectly from compact smartphones to large tablets.
*   **Dark Mode Protection**: Forced light theme to maintain a consistent brand identity and avoid visibility issues in system-wide dark mode.

## Technical Stack

*   **Language**: Kotlin (Idiomatic Coroutines & Flows)
*   **UI Framework**: Jetpack Compose with Material 3 (including Adaptive layouts and PullToRefresh)
*   **Architecture**: MVVM with Repository pattern
*   **Local Database**: Room with Cascade Deletions
*   **Backend**: Firebase (Authentication, Firestore, Cloud Storage)
*   **Background Tasks**: WorkManager
*   **Image Handling**: Coil
*   **Authentication**: Google Identity Services (Credentials API)
*   **Build System**: Gradle Kotlin DSL targeting stable Android 15 (SDK 35)

## Setup & Installation

1.  **Clone the Repository**:
    ```bash
    git clone https://github.com/your-repo/ResortControl.git
    ```
2.  **Firebase Configuration**:
    *   Create a project in the [Firebase Console](https://console.firebase.google.com/).
    *   Add an Android App with package name: `com.resort.control`.
    *   Enable **Google Auth**, **Firestore** (production or test mode), and **Cloud Storage**.
    *   Download the `google-services.json` and place it in the `app/` directory.
    *   Add your **SHA-1** release fingerprint to the Firebase project settings to enable Google Sign-in on real devices.
3.  **Client ID Update**:
    *   In `MainActivity.kt`, update the `webClientId` variable with your **Web Client ID** from the Firebase Google Auth settings.
4.  **Build**:
    *   Open the project in Android Studio (Ladybug or newer).
    *   Build a **Signed APK/Bundle** using your release key for production deployment.

---

# ResortControl (RU)

ResortControl — это профессиональное Android-приложение для администраторов баз отдыха, позволяющее эффективно управлять объектами недвижимости и бронированиями. Проект сочетает в себе надежную облачную синхронизацию, работу в офлайн-режиме и современный адаптивный интерфейс.

## Основные возможности

*   **Надежная синхронизация**: Интеграция с Firebase (Firestore, Storage) с логикой "Push-before-Pull", которая гарантирует, что локальные удаления и правки не будут затерты данными из облака.
*   **Ручное обновление**: Поддержка жеста «свайп вниз» (Pull-to-Refresh) для принудительного запуска обмена данными.
*   **Управление базами**: Возможность создавать и организовывать несколько рекреационных зон (баз) в одном аккаунте Google.
*   **Интерактивный календарь**: Умная система визуализации броней с цветовой кодировкой, предотвращающая накладки по датам.
*   **Оптимизация медиа**: Автоматическое сжатие фотографий перед загрузкой в облако для экономии места и трафика.
*   **Offline-First**: Использование базы данных Room для полноценной работы без интернета с последующей синхронизацией через WorkManager.
*   **Адаптивный UI**: Интерфейс на базе Material 3, который одинаково удобен как на смартфонах, так и на планшетах.
*   **Защита интерфейса**: Принудительная светлая тема для сохранения читаемости и дизайна вне зависимости от настроек системы.

## Технологический стек

*   **Язык**: Kotlin
*   **UI**: Jetpack Compose (Material 3 Adaptive, PullToRefresh)
*   **Архитектура**: MVVM + Repository
*   **Локальная БД**: Room (с поддержкой каскадных удалений)
*   **Backend**: Firebase (Auth, Firestore, Storage)
*   **Фоновые задачи**: WorkManager
*   **Загрузка изображений**: Coil
*   **Авторизация**: Google Credentials API
*   **Сборка**: Gradle Kotlin DSL, стабильный SDK 35 (Android 15)

## Инструкция по настройке

1.  **Склонируйте репозиторий**.
2.  **Настройка Firebase**:
    *   Создайте проект в [Firebase Console](https://console.firebase.google.com/).
    *   Добавьте Android-приложение с ID пакета: `com.resort.control`.
    *   Включите **Google Auth**, **Firestore** и **Cloud Storage**.
    *   Разместите свежий файл `google-services.json` в папке `app/`.
    *   Добавьте **SHA-1** вашего релизного ключа в настройки Firebase для работы входа через Google на реальных устройствах.
3.  **Обновление Client ID**:
    *   В файле `MainActivity.kt` замените значение `webClientId` на ваш **Web Client ID** из настроек Google Auth в Firebase.
4.  **Сборка**:
    *   Соберите **подписанный APK** (Signed APK) в Android Studio для корректной работы всех сервисов.
