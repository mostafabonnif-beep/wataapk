package com.elwataniatv.app.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
@Immutable
@Serializable
data class ArchiveProgram(
    val id: String,
    val title: String,
    val youtubeUrl: String,
    val thumbnailUrl: String = "",
    val category: String = "أخرى",
    val date: String = "",
    val duration: String = "",
    val description: String = "",
    val isActive: Boolean = true,
    val isFeatured: Boolean = true,
    val editorialPriority: Int = 0,
    val isBreaking: Boolean = false,
    val publishAt: Long? = null,
    val expiresAt: Long? = null
)

fun featuredArchivePreview(programs: List<ArchiveProgram>, limit: Int = 5): List<ArchiveProgram> {
    require(limit > 0) { "limit must be positive" }
    val featured = programs
        .filter { it.isFeatured }
        .sortedWith(compareByDescending<ArchiveProgram> { it.editorialPriority }.thenByDescending { it.date })
    val fallback = programs.sortedWith(compareByDescending<ArchiveProgram> { it.editorialPriority }.thenByDescending { it.date })
    return (if (featured.isNotEmpty()) featured else fallback).take(limit)
}
