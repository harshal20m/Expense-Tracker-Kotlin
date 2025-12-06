# PaisaTracker

A simple and intuitive expense tracker app for Android, built with modern Android development practices. This app helps you manage your finances by organizing expenses into projects, providing clear visualizations and easy data export options.

<p align="center">
  <img src="https://github.com/harshal20m/Expense-Tracker-Kotlin/raw/master/src/main/res/drawable/expenses_5501391.png" width="300" alt="PaisaTracker Banner">
</p>

## ✨ Features

- **Project-Based Tracking**: Create different projects to segregate your expenses (e.g., "Vacation," "Monthly Budget," "Home Renovation").
- **Expense Management**: Easily add, edit, and view expenses within each project.
- **Categorization**: Organize expenses into categories for better financial insights.
- **Visualizations**: View your expense distribution with interactive pie charts.
- **Data Export**: Export your financial data to CSV or PDF for reporting and backup.
- **Asset Management**: Attach images or other assets to your expenses for detailed records.
- **Modern & Aesthetic UI**: A clean and responsive user interface built entirely with Jetpack Compose, featuring a smooth, animated bottom navigation bar and custom dialogs.

## 🛠️ Tech Stack

- **UI**: 100% Kotlin with [Jetpack Compose](https://developer.android.com/jetpack/compose) for a declarative and modern UI.
- **Asynchronous Programming**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) for managing background threads and asynchronous operations.
- **Database**: [Room](https://developer.android.com/training/data-storage/room) for robust and persistent local data storage.
- **Navigation**: [Jetpack Navigation](https://developer.android.com/guide/navigation) for Compose to handle all in-app navigation.
- **Image Loading**: [Coil](https://coil-kt.github.io/coil/) for efficiently loading and displaying images.
- **Charting**: [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) for creating beautiful and interactive charts.
- **Data Export**: Utilizes [OpenCSV](http://opencsv.sourceforge.net/) and [iTextPDF](https://itextpdf.com/) for data export functionality.

## 🏛️ Architecture

This app follows the **MVVM (Model-View-ViewModel)** architecture pattern. This design separates the UI from the business logic, making the code cleaner, easier to maintain, and more testable.

```mermaid
graph TD
    A["UI / View (Compose)"] -- User Actions --> B[ViewModel];
    B -- Observes State --> A;
    B -- Calls --> C[Repository];
    C -- Returns Data --> B;
    C -- Accesses --> D["Data Source (Room DB)"];
    D -- Provides Data --> C;
```

## 🚀 Getting Started

1.  **Clone the repository**:
    ```sh
    git clone https://github.com/harshal20m/Expense-Tracker-Kotlin.git
    ```
2.  **Open in Android Studio**:
    - Open Android Studio and select `File > Open`.
    - Navigate to the cloned repository folder and select it.
3.  **Sync Dependencies**:
    - Let Gradle sync and download all the required project dependencies.
4.  **Build & Run**:
    - Build the project and run it on an Android emulator or a physical device.

## 🤝 Contributing

Contributions are welcome! If you have suggestions for improvements or want to fix a bug, please feel free to open an issue or submit a pull request.

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
