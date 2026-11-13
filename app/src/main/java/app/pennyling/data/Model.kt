package app.pennyling.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Money is kept in minor units (kopecks, cents) everywhere. */

/** Where money lives: a wallet, a card, a savings pot. [start] is the balance it was added with. */
@Entity
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: String = CARD,          // cash · card · save
    val start: Long = 0,
    val archived: Boolean = false,
    val order: Int = 0,
) { companion object { const val CASH = "cash"; const val CARD = "card"; const val SAVE = "save" } }

/**
 * A category. Spending ones have a keeper: the pet that looks after them and grows on their records.
 * [limit] is a monthly cap (0 = none). [key] names a built-in one, so it keeps its pet and icon.
 */
@Entity
data class Cat(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val income: Boolean = false,
    val keeper: String = "",
    val icon: String = "",
    val limit: Long = 0,
    val archived: Boolean = false,
    val order: Int = 0,
    val key: String = "",
)

/**
 * One record. [kind] 0 spent, 1 received, 2 moved between accounts. [worth] is the answer to
 * «was it worth it?» a week later: 0 not asked yet, 1 yes, 2 so-so, 3 no, -1 not to be asked.
 * [source]: "" typed, "crow" from a bank notification, "bill" a regular payment, "import".
 */
@Entity(indices = [Index("day"), Index("catId")])
data class Tx(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Long,
    val kind: Int = SPEND,
    val catId: Long = 0,
    val accountId: Long = 0,
    val toAccountId: Long = 0,
    val day: Long,
    val at: Long = System.currentTimeMillis(),
    val note: String = "",
    val worth: Int = 0,
    val billId: Long = 0,
    val source: String = "",
) { companion object { const val SPEND = 0; const val INCOME = 1; const val MOVE = 2 } }

/** A regular payment: rent, a subscription, a salary. Posted by itself on [next]. [checked] is the last review day. */
@Entity
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Long,
    val catId: Long = 0,
    val accountId: Long = 0,
    val income: Boolean = false,
    val every: String = MONTH,        // m · w · y, or a count before the unit: 2m, 10d, 3w (Bills.every)
    val next: Long,
    val sub: Boolean = false,         // a subscription: lives in the cellar as a barrel
    val active: Boolean = true,
    val checked: Long = 0,
    val keep: Int = 0,                // the last review: 1 keep, 2 doubt
) { companion object { const val MONTH = "m"; const val WEEK = "w"; const val YEAR = "y" } }

/** A jar on the shelf: something saved up for. A full jar turns into an egg. */
@Entity
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val target: Long,
    val created: Long,
    val until: Long = 0,              // wished-for day, 0 none
    val done: Long = 0,               // the day it filled
    val egg: String = "",             // the species it hatched
    val color: Int = 0,
    val archived: Boolean = false,
)

/** A boulder at the door: a debt rolled away payment by payment. [rate] is yearly %. */
@Entity
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val total: Long,
    val rate: Double = 0.0,
    val minPay: Long = 0,
    val created: Long,
    val done: Long = 0,
)

/** Money put into a jar or paid on a debt ([kind] "goal" or "debt"). Negative takes from a jar. */
@Entity(indices = [Index("refId")])
data class Move(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val refId: Long,
    val amount: Long,
    val day: Long,
    val at: Long = System.currentTimeMillis(),
)

/** A wish cooling in the attic. [state] 0 waiting, 1 bought, 2 let go. */
@Entity
data class Wish(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val price: Long,
    val added: Long,
    val days: Int = 30,
    val state: Int = 0,
    val decided: Long = 0,
    val note: String = "",
)

/**
 * A pet. [slot] 0 leads, 1–2 come along, -1 rests in the collection. [growth] counts towards hatching
 * while it's an egg ([hatched] 0), then [xp] grows it.
 */
@Entity
data class Pet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val species: String,
    val name: String,
    val xp: Int = 0,
    val growth: Int = 0,
    val found: Long,
    val hatched: Long = 0,
    val slot: Int = -1,
    val shiny: Boolean = false,
    val fed: Int = 0,                 // records it was fed by
)

/** What the crow heard from a bank: a record waiting to be taken. [state] 0 new, 1 taken, 2 dropped. */
@Entity
data class Heard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pkg: String,
    val amount: Long,
    val income: Boolean,
    val place: String,
    val text: String,
    val at: Long,
    val cash: Boolean = false,
    val state: Int = 0,
)

@Dao
interface Dao0 {
    @Query("SELECT * FROM Account ORDER BY `order`, id") fun accounts(): Flow<List<Account>>
    @Query("SELECT * FROM Account ORDER BY `order`, id") suspend fun accountList(): List<Account>
    @Insert suspend fun insert(a: Account): Long
    @Update suspend fun update(a: Account)

    @Query("SELECT * FROM Cat ORDER BY `order`, id") fun cats(): Flow<List<Cat>>
    @Query("SELECT * FROM Cat ORDER BY `order`, id") suspend fun catList(): List<Cat>
    @Insert suspend fun insert(c: Cat): Long
    @Insert suspend fun insertCats(c: List<Cat>)
    @Update suspend fun update(c: Cat)
    @Update suspend fun updateCats(c: List<Cat>)

    @Query("SELECT * FROM Tx ORDER BY day DESC, at DESC") fun txs(): Flow<List<Tx>>
    @Query("SELECT * FROM Tx") suspend fun txList(): List<Tx>
    @Query("SELECT * FROM Tx WHERE id = :id") suspend fun tx(id: Long): Tx?
    @Insert suspend fun insert(t: Tx): Long
    @Insert suspend fun insertTxs(t: List<Tx>)
    @Update suspend fun update(t: Tx)
    @Query("DELETE FROM Tx WHERE id = :id") suspend fun deleteTx(id: Long)
    @Query("UPDATE Tx SET catId = :to WHERE catId = :from") suspend fun moveCat(from: Long, to: Long)

    @Query("SELECT * FROM Bill ORDER BY next") fun bills(): Flow<List<Bill>>
    @Query("SELECT * FROM Bill") suspend fun billList(): List<Bill>
    @Insert suspend fun insert(b: Bill): Long
    @Update suspend fun update(b: Bill)
    @Query("DELETE FROM Bill WHERE id = :id") suspend fun deleteBill(id: Long)

    @Query("SELECT * FROM Goal ORDER BY id") fun goals(): Flow<List<Goal>>
    @Query("SELECT * FROM Goal") suspend fun goalList(): List<Goal>
    @Insert suspend fun insert(g: Goal): Long
    @Update suspend fun update(g: Goal)
    @Query("DELETE FROM Goal WHERE id = :id") suspend fun deleteGoal(id: Long)

    @Query("SELECT * FROM Debt ORDER BY id") fun debts(): Flow<List<Debt>>
    @Query("SELECT * FROM Debt") suspend fun debtList(): List<Debt>
    @Insert suspend fun insert(d: Debt): Long
    @Update suspend fun update(d: Debt)
    @Query("DELETE FROM Debt WHERE id = :id") suspend fun deleteDebt(id: Long)

    @Query("SELECT * FROM Move ORDER BY day, at") fun moves(): Flow<List<Move>>
    @Query("SELECT * FROM Move") suspend fun moveList(): List<Move>
    @Insert suspend fun insert(m: Move): Long
    @Insert suspend fun insertMoves(m: List<Move>)
    @Query("DELETE FROM Move WHERE id = :id") suspend fun deleteMove(id: Long)
    @Query("DELETE FROM Move WHERE kind = :kind AND refId = :ref") suspend fun deleteMovesOf(kind: String, ref: Long)

    @Query("SELECT * FROM Wish ORDER BY added DESC") fun wishes(): Flow<List<Wish>>
    @Query("SELECT * FROM Wish") suspend fun wishList(): List<Wish>
    @Insert suspend fun insert(w: Wish): Long
    @Update suspend fun update(w: Wish)
    @Query("DELETE FROM Wish WHERE id = :id") suspend fun deleteWish(id: Long)

    @Query("SELECT * FROM Pet ORDER BY id") fun pets(): Flow<List<Pet>>
    @Query("SELECT * FROM Pet ORDER BY id") suspend fun petList(): List<Pet>
    @Insert suspend fun insert(p: Pet): Long
    @Update suspend fun update(p: Pet)
    @Update suspend fun updatePets(p: List<Pet>)

    @Query("SELECT * FROM Heard WHERE state = 0 ORDER BY at DESC") fun heard(): Flow<List<Heard>>
    @Query("SELECT * FROM Heard ORDER BY at DESC LIMIT 200") suspend fun heardList(): List<Heard>
    @Insert suspend fun insert(h: Heard): Long
    @Update suspend fun update(h: Heard)

    @Query("DELETE FROM Tx") suspend fun wipeTx()
    @Query("DELETE FROM Account") suspend fun wipeAccounts()
    @Query("DELETE FROM Cat") suspend fun wipeCats()
    @Query("DELETE FROM Bill") suspend fun wipeBills()
    @Query("DELETE FROM Goal") suspend fun wipeGoals()
    @Query("DELETE FROM Debt") suspend fun wipeDebts()
    @Query("DELETE FROM Move") suspend fun wipeMoves()
    @Query("DELETE FROM Wish") suspend fun wipeWishes()
    @Query("DELETE FROM Pet") suspend fun wipePets()
    @Query("DELETE FROM Heard") suspend fun wipeHeard()
}

@Database(entities = [Account::class, Cat::class, Tx::class, Bill::class, Goal::class, Debt::class, Move::class, Wish::class, Pet::class, Heard::class], version = 1)
abstract class Db : RoomDatabase() {
    abstract fun dao(): Dao0

    companion object {
        fun create(ctx: Context): Db = Room.databaseBuilder(ctx, Db::class.java, "pennyling.db").build()
    }
}
