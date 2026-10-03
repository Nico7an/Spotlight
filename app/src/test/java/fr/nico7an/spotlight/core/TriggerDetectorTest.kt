package fr.nico7an.spotlight.core

import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.ACTION_UP
import android.view.KeyEvent.KEYCODE_A
import android.view.KeyEvent.KEYCODE_ALT_LEFT
import android.view.KeyEvent.KEYCODE_META_LEFT
import android.view.KeyEvent.KEYCODE_META_RIGHT
import android.view.KeyEvent.KEYCODE_SPACE
import android.view.KeyEvent.KEYCODE_TAB
import android.view.KeyEvent.META_ALT_ON
import android.view.KeyEvent.META_META_ON
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TriggerDetectorTest {

    private var triggers = 0
    private val detector = TriggerDetector { triggers++ }

    private fun key(action: Int, code: Int, meta: Int = 0, time: Long = 0L, repeat: Int = 0) =
        KeyInput(action, code, meta, repeat, time)

    @Test
    fun `meta tap triggers on release and only swallows the release`() {
        val sc = Shortcut.META_TAP
        assertFalse(detector.onKey(key(ACTION_DOWN, KEYCODE_META_LEFT, META_META_ON, 0), sc, false))
        assertTrue(detector.onKey(key(ACTION_UP, KEYCODE_META_LEFT, 0, 100), sc, false))
        assertEquals(1, triggers)
    }

    @Test
    fun `right meta key works too`() {
        val sc = Shortcut.META_TAP
        detector.onKey(key(ACTION_DOWN, KEYCODE_META_RIGHT, META_META_ON, 0), sc, false)
        detector.onKey(key(ACTION_UP, KEYCODE_META_RIGHT, 0, 100), sc, false)
        assertEquals(1, triggers)
    }

    @Test
    fun `meta combined with another key does not trigger`() {
        val sc = Shortcut.META_TAP
        detector.onKey(key(ACTION_DOWN, KEYCODE_META_LEFT, META_META_ON, 0), sc, false)
        assertFalse(detector.onKey(key(ACTION_DOWN, KEYCODE_TAB, META_META_ON, 10), sc, false))
        detector.onKey(key(ACTION_UP, KEYCODE_TAB, META_META_ON, 20), sc, false)
        assertFalse(detector.onKey(key(ACTION_UP, KEYCODE_META_LEFT, 0, 30), sc, false))
        assertEquals(0, triggers)
    }

    @Test
    fun `long meta press does not trigger`() {
        val sc = Shortcut.META_TAP
        detector.onKey(key(ACTION_DOWN, KEYCODE_META_LEFT, META_META_ON, 0), sc, false)
        detector.onKey(key(ACTION_UP, KEYCODE_META_LEFT, 0, 2_000), sc, false)
        assertEquals(0, triggers)
    }

    @Test
    fun `block mode swallows the press as well`() {
        val sc = Shortcut.META_TAP
        assertTrue(detector.onKey(key(ACTION_DOWN, KEYCODE_META_LEFT, META_META_ON, 0), sc, true))
        assertTrue(detector.onKey(key(ACTION_UP, KEYCODE_META_LEFT, 0, 50), sc, true))
        assertEquals(1, triggers)
    }

    @Test
    fun `alt space triggers on press and swallows both events`() {
        val sc = Shortcut.ALT_SPACE
        assertFalse(detector.onKey(key(ACTION_DOWN, KEYCODE_ALT_LEFT, META_ALT_ON), sc, false))
        assertTrue(detector.onKey(key(ACTION_DOWN, KEYCODE_SPACE, META_ALT_ON), sc, false))
        assertTrue(detector.onKey(key(ACTION_DOWN, KEYCODE_SPACE, META_ALT_ON, repeat = 1), sc, false))
        assertTrue(detector.onKey(key(ACTION_UP, KEYCODE_SPACE, META_ALT_ON), sc, false))
        assertFalse(detector.onKey(key(ACTION_UP, KEYCODE_ALT_LEFT), sc, false))
        assertEquals(1, triggers)
    }

    @Test
    fun `meta space also swallows the meta release`() {
        val sc = Shortcut.META_SPACE
        detector.onKey(key(ACTION_DOWN, KEYCODE_META_LEFT, META_META_ON), sc, false)
        assertTrue(detector.onKey(key(ACTION_DOWN, KEYCODE_SPACE, META_META_ON), sc, false))
        assertTrue(detector.onKey(key(ACTION_UP, KEYCODE_SPACE, META_META_ON), sc, false))
        assertTrue(detector.onKey(key(ACTION_UP, KEYCODE_META_LEFT), sc, false))
        assertEquals(1, triggers)
    }

    @Test
    fun `plain space is left alone`() {
        val sc = Shortcut.ALT_SPACE
        assertFalse(detector.onKey(key(ACTION_DOWN, KEYCODE_SPACE), sc, false))
        assertFalse(detector.onKey(key(ACTION_UP, KEYCODE_SPACE), sc, false))
        assertFalse(detector.onKey(key(ACTION_DOWN, KEYCODE_A, META_ALT_ON), sc, false))
        assertEquals(0, triggers)
    }

    @Test
    fun `shortcut encoding round trips`() {
        assertEquals(Shortcut.ALT_SPACE, Shortcut.decode(Shortcut.ALT_SPACE.encode()))
        assertEquals(null, Shortcut.decode("garbage"))
    }
}
