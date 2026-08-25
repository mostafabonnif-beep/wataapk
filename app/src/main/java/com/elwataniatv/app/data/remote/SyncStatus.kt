package com.elwataniatv.app.data.remote

/** Public sync state shared by FirebaseSync, the repository, and the UI. */
data class SyncStatus(
    val isConnected: Boolean = false,
    val lastUpdatedAt: Long? = null,
    val lastCollection: String? = null,
    val errorMessage: String? = null
)
