package com.telebox.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "config")

class PreferencesStore(private val context: Context) {

    private val apiIdKey = stringPreferencesKey("api_id")
    private val apiHashKey = stringPreferencesKey("api_hash")
    private val foldersKey = stringPreferencesKey("folders")
    private val activeFolderKey = longPreferencesKey("activeFolderId")
    private val viewModeKey = stringPreferencesKey("viewMode")
    private val themeKey = stringPreferencesKey("theme")
    private val uploadQueueKey = stringPreferencesKey("uploadQueue")
    private val downloadQueueKey = stringPreferencesKey("downloadQueue")

    val themeFlow: Flow<AppThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[themeKey]) {
            "light" -> AppThemeMode.LIGHT
            "dark" -> AppThemeMode.DARK
            else -> AppThemeMode.DARK
        }
    }

    suspend fun getApiId(): String? = context.dataStore.data.first()[apiIdKey]

    suspend fun getApiHash(): String? = context.dataStore.data.first()[apiHashKey]

    suspend fun saveCredentials(apiId: String, apiHash: String) {
        context.dataStore.edit { prefs ->
            prefs[apiIdKey] = apiId
            prefs[apiHashKey] = apiHash
        }
    }

    suspend fun clearCredentials() {
        context.dataStore.edit { prefs ->
            prefs.remove(apiIdKey)
            prefs.remove(apiHashKey)
            prefs.remove(foldersKey)
        }
    }

    suspend fun getFolders(): List<TelegramFolder> {
        val raw = context.dataStore.data.first()[foldersKey] ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split("||").mapNotNull { part ->
            val bits = part.split("::")
            if (bits.size < 2) return@mapNotNull null
            TelegramFolder(
                id = bits[0].toLongOrNull() ?: return@mapNotNull null,
                name = bits[1],
                parentId = bits.getOrNull(2)?.toLongOrNull()
            )
        }
    }

    suspend fun saveFolders(folders: List<TelegramFolder>) {
        val encoded = folders.joinToString("||") { folder ->
            listOf(folder.id.toString(), folder.name, folder.parentId?.toString().orEmpty())
                .joinToString("::")
        }
        context.dataStore.edit { it[foldersKey] = encoded }
    }

    suspend fun getActiveFolderId(): Long? {
        val value = context.dataStore.data.first()[activeFolderKey] ?: return null
        return if (value == Long.MIN_VALUE) null else value
    }

    suspend fun saveActiveFolderId(id: Long?) {
        context.dataStore.edit { prefs ->
            prefs[activeFolderKey] = id ?: Long.MIN_VALUE
        }
    }

    suspend fun getViewMode(): ViewMode {
        return if (context.dataStore.data.first()[viewModeKey] == "list") ViewMode.LIST else ViewMode.GRID
    }

    suspend fun saveViewMode(mode: ViewMode) {
        context.dataStore.edit {
            it[viewModeKey] = if (mode == ViewMode.LIST) "list" else "grid"
        }
    }

    suspend fun saveTheme(mode: AppThemeMode) {
        context.dataStore.edit {
            it[themeKey] = if (mode == AppThemeMode.LIGHT) "light" else "dark"
        }
    }

    suspend fun getPendingUploads(): List<QueueItem> {
        val raw = context.dataStore.data.first()[uploadQueueKey] ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split("||").mapNotNull { part ->
            val bits = part.split("::")
            if (bits.size < 3) return@mapNotNull null
            QueueItem(
                id = bits[0],
                path = bits[1],
                folderId = bits[2].toLongOrNull(),
                status = TransferStatus.PENDING
            )
        }
    }

    suspend fun savePendingUploads(items: List<QueueItem>) {
        val pending = items.filter { it.status == TransferStatus.PENDING }
        val encoded = pending.joinToString("||") { item ->
            listOf(item.id, item.path, item.folderId?.toString().orEmpty()).joinToString("::")
        }
        context.dataStore.edit { it[uploadQueueKey] = encoded }
    }

    suspend fun getPendingDownloads(): List<DownloadItem> {
        val raw = context.dataStore.data.first()[downloadQueueKey] ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split("||").mapNotNull { part ->
            val bits = part.split("::")
            if (bits.size < 4) return@mapNotNull null
            DownloadItem(
                id = bits[0],
                messageId = bits[1].toLongOrNull() ?: return@mapNotNull null,
                filename = bits[2],
                folderId = bits[3].toLongOrNull(),
                status = TransferStatus.PENDING
            )
        }
    }

    suspend fun savePendingDownloads(items: List<DownloadItem>) {
        val pending = items.filter { it.status == TransferStatus.PENDING }
        val encoded = pending.joinToString("||") { item ->
            listOf(
                item.id,
                item.messageId.toString(),
                item.filename,
                item.folderId?.toString().orEmpty()
            ).joinToString("::")
        }
        context.dataStore.edit { it[downloadQueueKey] = encoded }
    }
}
