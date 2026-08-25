package com.elwataniatv.app

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regression guards for the release-readiness fixes that are source-verifiable in JVM tests. */
class AuditReleaseReadinessTest {

    @Test
    fun reminderRescheduling_usesSuspendDaoFromViewModelCoroutine() {
        val dao = source("app/src/main/java/com/elwataniatv/app/data/local/Daos.kt")
        val repository = source("app/src/main/java/com/elwataniatv/app/data/repository/WataniaRepository.kt")
        val viewModel = source("app/src/main/java/com/elwataniatv/app/ui/viewmodel/MainViewModel.kt")

        assertTrue(dao.contains("suspend fun getAllReminders(): List<ProgramReminder>"))
        assertTrue(repository.contains("suspend fun rescheduleAllReminders()"))
        assertTrue(repository.contains("db.remindersDao().getAllReminders()"))
        assertFalse(repository.contains("db.remindersDao().getAllRemindersSync()"))
        assertTrue(viewModel.contains("viewModelScope.launch(Dispatchers.IO) {\n            repository.rescheduleAllReminders()\n        }"))
    }

    @Test
    fun globalErrorBoundary_doesNotInstallProcessExceptionHandler() {
        val boundary = source("app/src/main/java/com/elwataniatv/app/ui/components/GlobalErrorBoundary.kt")

        assertFalse(boundary.contains("Thread.setDefaultUncaughtExceptionHandler"))
        assertFalse(boundary.contains("capturedError"))
        assertFalse(boundary.contains("DisposableEffect(Unit)"))
        assertTrue(boundary.contains("does not intercept uncaught exceptions"))
    }

    private fun source(relativePath: String): String {
        val candidates = listOf(Paths.get(relativePath), Paths.get("..", relativePath))
        val path: Path = candidates.firstOrNull { Files.exists(it) }
            ?: error("Source file not found from ${Paths.get(".").toAbsolutePath()}: $relativePath")
        return Files.readAllBytes(path).toString(Charsets.UTF_8)
    }
}
