package com.jmrc.jaboard.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomPersistenceTest {
    private lateinit var database: JaBoardDatabase

    @Before fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, JaBoardDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After fun closeDatabase() = database.close()

    @Test fun notesCanBeInsertedPinnedAndDeleted() = runBlocking {
        val id = database.notes().insert(Note(title = "Local note", body = "Saved offline"))
        assertEquals("Local note", database.notes().observeAll().first().single().title)
        val note = database.notes().observeAll().first().single()
        database.notes().update(note.copy(pinned = true))
        assertTrue(database.notes().observeAll().first().single().pinned)
        database.notes().delete(note.copy(pinned = true))
        assertTrue(database.notes().observeAll().first().isEmpty())
        assertTrue(id > 0)
    }

    @Test fun tasksPersistCompletionState() = runBlocking {
        val id = database.todos().insert(TodoTask(title = "Build dashboard"))
        val task = database.todos().observeAll().first().single()
        database.todos().update(task.copy(completed = true))
        assertTrue(database.todos().observeAll().first().single().completed)
        assertTrue(id > 0)
    }
}
