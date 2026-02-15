# Offline Sync Quick Reference

## Quick Start

### Check Connectivity Status
```kotlin
// Inject use case
@Inject lateinit var observeConnectivityUseCase: ObserveConnectivityUseCase

// Observe in ViewModel
viewModelScope.launch {
    observeConnectivityUseCase()
        .collect { status ->
            when (status) {
                is ConnectivityStatus.Connected -> {
                    if (status.isMetered) {
                        // On cellular
                    } else {
                        // On WiFi
                    }
                }
                ConnectivityStatus.Disconnected -> // Offline
                ConnectivityStatus.Unknown -> // Unknown
            }
        }
}
```

### Trigger Manual Sync
```kotlin
// Inject scheduler
@Inject lateinit var syncScheduler: SyncScheduler

// Trigger immediate sync
syncScheduler.triggerImmediateSync(manual = true)
```

### Get Pending Receipts Count
```kotlin
// Inject use case
@Inject lateinit var getSyncStatusUseCase: GetSyncStatusUseCase

// Observe pending receipts
viewModelScope.launch {
    getSyncStatusUseCase.getPendingReceipts()
        .collect { pendingReceipts ->
            _pendingCount.value = pendingReceipts.size
        }
}
```

### Retry Failed Sync
```kotlin
// Inject use case
@Inject lateinit var retrySyncUseCase: RetrySyncUseCase

// Retry all failed syncs
viewModelScope.launch {
    val result = retrySyncUseCase.retryAll()
    when (result) {
        is Result.Success -> // Show success
        is Result.Error -> // Show error
    }
}
```

## Key Classes

### Domain Layer
- `ConnectivityStatus` - Network connectivity state
- `ObserveConnectivityUseCase` - Monitor connectivity
- `GetSyncStatusUseCase` - Get sync status
- `RetrySyncUseCase` - Retry failed syncs
- `TriggerSyncOnConnectivityUseCase` - Auto-sync on connectivity

### Data Layer
- `ConnectivityMonitor` - Connectivity monitoring implementation
- `SyncStatusTracker` - Sync status management
- `TransactionSyncManager` - Core sync logic (existing)

### App Layer
- `SyncWorker` - Background sync worker
- `SyncScheduler` - Work scheduling
- `NotificationHelper` - Sync notifications
- `SyncStatusView` - UI component for sync status

## Configuration

### Sync Interval Settings
```xml
<!-- preferences.xml -->
<ListPreference
    android:key="sync_interval"
    android:entries="@array/sync_interval_entries"
    android:entryValues="@array/sync_interval_values" />
```

Values (minutes):
- `15` - Every 15 minutes
- `60` - Every hour (default)
- `240` - Every 4 hours
- `720` - Every 12 hours
- `0` - Manual only

### Update Sync Interval Programmatically
```kotlin
// In ViewModel or Repository
syncScheduler.updateSyncInterval(intervalMinutes = 240)

// Cancel periodic sync (manual only)
syncScheduler.cancelPeriodicSync()

// Re-enable with default
syncScheduler.schedulePeriodicSync()
```

### Metered Network Control
```kotlin
// Allow sync on cellular data
syncScheduler.schedulePeriodicSync(
    intervalMinutes = 60,
    requireUnmetered = false // Allow cellular
)

// WiFi only
syncScheduler.schedulePeriodicSync(
    intervalMinutes = 60,
    requireUnmetered = true // WiFi only
)
```

## UI Integration

### Add SyncStatusView to Layout
```xml
<com.ynab.receiptscanner.ui.sync.SyncStatusView
    android:id="@+id/sync_status_view"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

### Update SyncStatusView
```kotlin
// In Fragment
binding.syncStatusView.apply {
    setSyncStatus(SyncStatus.SYNCING)
    setConnectivityStatus(ConnectivityStatus.Connected(false))
    setPendingCount(5)
    setOnRetryClickListener {
        viewModel.retrySync()
    }
}
```

## Notifications

### Manual Notification Control
```kotlin
// Inject helper
@Inject lateinit var notificationHelper: NotificationHelper

// Show sync in progress
notificationHelper.showSyncInProgress(pendingCount = 3)

// Show completion
notificationHelper.showSyncComplete(successCount = 3, failedCount = 0)

// Show error
notificationHelper.showSyncError("Network error")

// Dismiss all
notificationHelper.dismissAllSyncNotifications()
```

## Testing

### Mock Connectivity Status
```kotlin
// In tests
val connectivityRepository = mock<ConnectivityRepository>()
whenever(connectivityRepository.observeConnectivity())
    .thenReturn(flowOf(ConnectivityStatus.Connected(false)))
```

### Mock Pending Transactions
```kotlin
val pendingTransactionDao = mock<PendingTransactionDao>()
whenever(pendingTransactionDao.getPendingTransactionsByStatus("PENDING"))
    .thenReturn(flowOf(listOf(/* test data */)))
```

### Test Worker
```kotlin
val worker = TestListenableWorkerBuilder<SyncWorker>(context)
    .setInputData(workDataOf(SyncWorker.KEY_MANUAL_TRIGGER to true))
    .build()

val result = worker.doWork()
assertTrue(result is ListenableWorker.Result.Success)
```

## Common Tasks

### Queue Transaction for Offline Sync
```kotlin
// Automatically handled in YnabRepository.createTransaction()
// When offline, transaction is queued in PendingTransactionDao
```

### Monitor Sync Work Status
```kotlin
// Get WorkManager LiveData
val workInfoLiveData = syncScheduler.getSyncWorkInfo()

// Observe in Fragment/Activity
workInfoLiveData.observe(viewLifecycleOwner) { workInfoList ->
    for (workInfo in workInfoList) {
        when (workInfo.state) {
            WorkInfo.State.RUNNING -> // Sync in progress
            WorkInfo.State.SUCCEEDED -> // Sync completed
            WorkInfo.State.FAILED -> // Sync failed
            else -> // Other states
        }
    }
}
```

### Handle Sync Completion
```kotlin
// Use WorkManager's Data output
val successCount = workInfo.outputData.getInt(SyncWorker.KEY_SUCCESS_COUNT, 0)
val failureCount = workInfo.outputData.getInt(SyncWorker.KEY_FAILURE_COUNT, 0)
```

## Troubleshooting

### Sync Not Running
1. Check if auto-sync is enabled in preferences
2. Verify network connectivity
3. Check WorkManager constraints (battery, network)
4. Look for WorkManager logs with tag "SyncWorker"

### Notifications Not Showing
1. Check notification permission (Android 13+)
2. Verify notification channels are created
3. Check Do Not Disturb settings
4. Look for NotificationHelper logs

### Connectivity Not Detected
1. Verify ACCESS_NETWORK_STATE permission
2. Check device network settings
3. Look for ConnectivityMonitor logs
4. Test with airplane mode on/off

## Permissions Required

```xml
<!-- Required -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Android 13+ -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- WorkManager -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

## Dependencies

```kotlin
// Already included in build.gradle.kts
implementation("androidx.work:work-runtime-ktx:2.9.0")
implementation("androidx.hilt:hilt-work:1.1.0")
ksp("androidx.hilt:hilt-compiler:1.1.0")
```

## Best Practices

### Do's ✅
- Use StateFlow for UI state in ViewModels
- Observe connectivity in ViewModel, not Fragment
- Show user feedback for connectivity changes
- Handle offline gracefully without blocking UI
- Use WorkManager constraints appropriately
- Test with various network conditions

### Don'ts ❌
- Don't block UI thread for network checks
- Don't sync too frequently (respect battery)
- Don't ignore metered network preferences
- Don't show too many sync notifications
- Don't retry indefinitely without backoff
- Don't assume network is always available

## Debugging

### Enable WorkManager Logging
```kotlin
// In Application.onCreate()
Configuration.Builder()
    .setMinimumLoggingLevel(Log.DEBUG)
    .build()
```

### LogCat Filters
```
tag:SyncWorker
tag:SyncScheduler
tag:ConnectivityMonitor
tag:TransactionSyncManager
tag:NotificationHelper
```

### Force Sync from ADB
```bash
# Trigger one-time sync
adb shell am broadcast -a androidx.work.diagnostics.REQUEST_DIAGNOSTICS \
    -p com.ynab.receiptscanner
```

## Resources

- [WorkManager Documentation](https://developer.android.com/topic/libraries/architecture/workmanager)
- [ConnectivityManager API](https://developer.android.com/reference/android/net/ConnectivityManager)
- [Notification Best Practices](https://developer.android.com/develop/ui/views/notifications)
- [Hilt Worker Integration](https://developer.android.com/training/dependency-injection/hilt-jetpack#workmanager)
