package com.elwataniatv.app.data.model

/**
 * A news article published by the institution, managed from the admin panel.
 *
 * Firestore collection: "news"
 */
data class NewsItem(
    val id: String = "",
    val title: String = "",
    val summary: String = "",
    val url: String = "",
    val imageUrl: String = "",
    val order: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long = 0L,
)
