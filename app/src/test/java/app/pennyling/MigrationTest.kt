package app.pennyling

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import app.pennyling.data.Db
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** A burrow made by the first schema opens with the new one, its records kept (#3, #20). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class MigrationTest {
    @Test fun oneToTwo() {
        val ctx = org.robolectric.RuntimeEnvironment.getApplication()
        val schema = JSONObject(File("schemas/app.pennyling.data.Db/1.json").readText()).getJSONObject("database")
        val path = ctx.getDatabasePath("old.db").apply { parentFile?.mkdirs(); delete() }
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            val ents = schema.getJSONArray("entities")
            for (i in 0 until ents.length()) {
                val e = ents.getJSONObject(i)
                db.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", e.getString("tableName")))
                e.optJSONArray("indices")?.let { ix -> for (k in 0 until ix.length()) db.execSQL(ix.getJSONObject(k).getString("createSql").replace("\${TABLE_NAME}", e.getString("tableName"))) }
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            db.execSQL("INSERT INTO Account (id, name, kind, start, archived, `order`) VALUES (1, 'Card', 'card', 1000, 0, 0)")
            db.execSQL("INSERT INTO Tx (id, amount, kind, catId, accountId, toAccountId, day, at, note, worth, billId, source) VALUES (1, 250, 0, 1, 1, 0, 20000, 0, 'tea', 1, 0, '')")
            db.version = 1
        }
        val room = Room.databaseBuilder(ctx, Db::class.java, "old.db").addMigrations(Db.M1_2).allowMainThreadQueries().build()
        runBlocking {
            val t = room.dao().txList().single()
            assertEquals(250L, t.amount); assertEquals(0L, t.fx); assertEquals(0L, t.worthDay); assertEquals(250L, t.own())
            assertEquals("", room.dao().accountList().single().cur)
        }
        room.close()
    }
}
