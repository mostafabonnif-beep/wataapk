package com.elwataniatv.app

import com.elwataniatv.app.data.model.EpgItem
import com.elwataniatv.app.ui.components.currentEpgItem
import com.elwataniatv.app.ui.components.epgProgress
import com.elwataniatv.app.ui.components.epgDurationMinutes
import com.elwataniatv.app.ui.components.epgTimeMinutes
import com.elwataniatv.app.ui.components.minutesRemainingEpg
import com.elwataniatv.app.ui.components.minutesUntilEpg
import com.elwataniatv.app.ui.components.nextEpgItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EpgTimeTest {
    private val items = listOf(
        EpgItem(id = "late", startTime = "23:50", title = "Late"),
        EpgItem(id = "early", startTime = "00:10", title = "Early"),
        EpgItem(id = "prime", startTime = "20:30", title = "Prime")
    )

    @Test
    fun nextProgramHandlesUnsortedScheduleAndMidnightWrap() {
        assertEquals("early", nextEpgItem(items, 23 * 60 + 55)?.id)
        assertEquals("prime", nextEpgItem(items, 12 * 60)?.id)
    }

    @Test
    fun countdownWrapsToNextDay() {
        assertEquals(15, minutesUntilEpg(items[1], 23 * 60 + 55))
        assertEquals(510, minutesUntilEpg(items[2], 12 * 60))
    }

    @Test
    fun invalidTimeIsRejected() {
        assertNull(epgTimeMinutes("25:99"))
        assertEquals(75, epgTimeMinutes("01:15"))
    }

    @Test
    fun durationAndProgressAreCalculatedForCurrentProgram() {
        val current = EpgItem(
            id = "current",
            startTime = "10:00",
            title = "Current",
            duration = "60 دقيقة"
        )
        assertEquals(60, epgDurationMinutes(current.duration))
        assertEquals("current", currentEpgItem(listOf(current), 10 * 60 + 15)?.id)
        assertEquals(45, minutesRemainingEpg(current, 10 * 60 + 15))
        assertEquals(0.25f, epgProgress(current, 10 * 60 + 15))
    }

    @Test
    fun currentProgramSupportsCrossingMidnight() {
        val late = EpgItem(
            id = "late-show",
            startTime = "23:30",
            title = "Late show",
            duration = "90 min"
        )
        assertEquals("late-show", currentEpgItem(listOf(late), 30)?.id)
        assertEquals(30, minutesRemainingEpg(late, 30))
    }
}
