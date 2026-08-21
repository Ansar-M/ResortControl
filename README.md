# ResortControl

ResortControl is a comprehensive Android application designed for recreation center administrators to manage properties, cabins, and guest bookings. The project demonstrates modern Android development practices, including full cloud synchronization and an adaptive interface for various form factors.

## Overview

The application provides a centralized platform to manage multiple recreation centers. Administrators can track cabin availability, manage property details, and handle reservations through an intuitive calendar interface. All data is synchronized in real-time across devices, ensuring that the management team always has access to the most up-to-date information.

## Key Features

*   **Multi-Center Management**: Ability to create and organize multiple recreation areas within a single account.
*   **Property Details**: Full control over cabin specifications, including room counts, amenities (showers, toilets), and pricing.
*   **Interactive Calendar**: A custom booking system that visualizes stays and prevents scheduling conflicts.
*   **Media Gallery**: Integrated photo management with automatic image optimization for cloud storage efficiency.
*   **Cloud Infrastructure**: Built-in synchronization using Firebase, providing reliable data persistence and cross-device updates.
*   **Adaptive UI**: A responsive design that scales seamlessly from compact smartphones to large tablets.
*   **Secure Access**: User authentication powered by Google Account integration.

## Technology Stack

*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose with Material 3 (including Adaptive layouts)
*   **Local Database**: Room for offline persistence
*   **Backend Services**: Firebase (Auth, Firestore, Storage)
*   **Background Processing**: WorkManager for reliable data synchronization
*   **Image Loading**: Coil
*   **Authentication**: Google Credentials API

## Setup Instructions

1.  Clone the repository to your local machine.
2.  Set up a new project in the [Firebase Console](https://console.firebase.google.com/).
3.  Enable Google Authentication, Firestore Database, and Cloud Storage.
4.  Download the `google-services.json` configuration file and place it in the `app/` directory.
5.  In `MainActivity.kt`, update the `webClientId` variable with your Google Web Client ID from the Firebase settings.
6.  Build and run the project using Android Studio.

---

# ResortControl (RU)

ResortControl — это полнофункциональное Android-приложение для администраторов баз отдыха, позволяющее эффективно управлять объектами недвижимости и бронированиями. Проект служит демонстрацией современных практик разработки, включая облачную синхронизацию и адаптивный интерфейс.

## Описание

Приложение предоставляет единую платформу для управления несколькими базами отдыха. Администраторы могут отслеживать доступность домиков, редактировать характеристики объектов и управлять резервированиями через интерактивный календарь. Все данные синхронизируются в реальном времени, что гарантирует актуальность информации на всех устройствах команды.

## Основные возможности

*   **Управление базами**: Создание и организация нескольких рекреационных зон в одном аккаунте.
*   **Детальные характеристики**: Полный контроль над описанием домиков (количество комнат, удобства, ценообразование).
*   **Интерактивный календарь**: Наглядная система бронирования, предотвращающая накладки по датам.
*   **Работа с медиа**: Управление фотографиями с автоматической оптимизацией размера для экономии места в облаке.
*   **Облачная инфраструктура**: Синхронизация на базе Firebase для надежного хранения и обмена данными.
*   **Адаптивный UI**: Интерфейс, который одинаково удобен на смартфонах и планшетах благодаря Material 3 Adaptive.
*   **Безопасность**: Авторизация через Google Account.

## Технологический стек

*   **Язык**: Kotlin
*   **UI**: Jetpack Compose (Material 3 Adaptive)
*   **Локальная база данных**: Room
*   **Backend**: Firebase (Auth, Firestore, Storage)
*   **Фоновые задачи**: WorkManager
*   **Загрузка изображений**: Coil
*   **Авторизация**: Google Credentials API

## Инструкция по запуску

1.  Склонируйте репозиторий.
2.  Создайте проект в [Firebase Console](https://console.firebase.google.com/).
3.  Включите Google Auth, Firestore и Cloud Storage.
4.  Разместите файл `google-services.json` в папке `app/`.
5.  В файле `MainActivity.kt` замените `webClientId` на ваш идентификатор веб-клиента.
6.  Соберите проект в Android Studio.
