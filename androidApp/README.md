# Telegram Drive — Jetpack Compose

Full Material 3 / Jetpack Compose translation of the TeleBox React UI.

## Package layout

```
android/app/src/main/java/com/telebox/app/
  MainActivity.kt
  TeleBoxApplication.kt
  data/
    Models.kt
    PreferencesStore.kt
    TelegramRepository.kt
  util/
    FileUtils.kt
  viewmodel/
    AppViewModel.kt
    AuthViewModel.kt
    ConfirmViewModel.kt
    DashboardViewModel.kt
    ThemeViewModel.kt
    UpdateViewModel.kt
    ViewModelFactory.kt
  ui/
    theme/
      Color.kt
      Type.kt
      Theme.kt
    components/
      ConfirmDialog.kt
      FileTypeIcon.kt
      ThemeToggle.kt
      ToastHost.kt
      UpdateBanner.kt
    screens/
      AuthWizard.kt
      DashboardScreen.kt
      ErrorScreen.kt
    dashboard/
      BandwidthWidget.kt
      ContextMenu.kt
      DownloadQueue.kt
      EmptyState.kt
      FileCard.kt
      FileExplorer.kt
      FileListItem.kt
      MediaPlayer.kt
      MoveToFolderModal.kt
      Overlays.kt
      PdfViewer.kt
      PreviewModal.kt
      Sidebar.kt
      SidebarItem.kt
      TopBar.kt
      UploadQueue.kt
```

## Mapping from React

| React | Compose |
| --- | --- |
| `ThemeContext` | `ThemeViewModel` + `TeleBoxTheme` |
| `ConfirmContext` | `ConfirmViewModel` + `ConfirmDialog` |
| `AuthWizard` | `AuthWizardScreen` + `AuthViewModel` |
| `Dashboard` | `DashboardScreen` + `DashboardViewModel` |
| `useTelegramConnection` / `useFile*` hooks | `DashboardViewModel` |
| `App.css` Telegram tokens | `Color.kt` / `Theme.kt` |

Open the `android/` folder in Android Studio and sync Gradle to build.
