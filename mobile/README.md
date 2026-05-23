# Lebhas Creative Maker - Mobile App

A multi-tenant AI Creative SaaS platform where users can create marketing creatives from raw product images.

## Day 1: Foundation

This project is built using:
- **Flutter** latest stable
- **Riverpod** for State Management
- **GoRouter** for Routing
- **Dio** for API Communication
- **Freezed & JsonSerializable** for Data Modeling
- **Material 3** with Dark-Mode-First design

### Project Structure

- `lib/app`: App-wide configuration, bootstrap, and routing.
- `lib/core`: Core utilities, theme, API client, and storage services.
- `lib/features`: Feature-based modules (Public Home, Auth, etc.).
- `lib/shared`: Reusable widgets and extensions.

### Getting Started

1. **Install Dependencies**
   ```bash
   flutter pub get
   ```

2. **Generate Code** (Freezed and JsonSerializable)
   ```bash
   flutter pub run build_runner build --delete-conflicting-outputs
   ```

3. **Run the App**
   ```bash
   flutter run
   ```

### Architecture Highlights
- **Clean Architecture Principles**: Separation of concerns between presentation, domain, and data layers.
- **Theme Persistence**: Dark/Light mode preference is saved in secure storage.
- **JWT-ready API Client**: Dio client with interceptors for auth tokens and correlation IDs.
- **Responsive UI**: Built to support phones and tablets.

### Implemented Features
- [x] Public Home/Onboarding Screen
- [x] Login Bottom Sheet Foundation
- [x] Theme Switching (Dark/Light)
- [x] Routing with Auth Guard (GoRouter)
- [x] Auth Foundation (Repository, Api, Controller)
