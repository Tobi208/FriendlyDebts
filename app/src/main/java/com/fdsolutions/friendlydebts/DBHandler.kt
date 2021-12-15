package com.fdsolutions.friendlydebts

import androidx.room.*

@Entity
data class Profile(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "pId") val pId: Int?,
    @ColumnInfo(name = "name") var name: String,
    @ColumnInfo(name = "total") var total: Double,
    @ColumnInfo(name = "focus") var focus: Int
)

@Entity
data class Debt(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "dId") var dId: Int?,
    @ColumnInfo(name = "pId") var pId: Int,
    @ColumnInfo(name = "date") var date: String,
    @ColumnInfo(name = "amount") var amount: Double,
    @ColumnInfo(name = "note") var note: String?
)

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile")
    fun getAll(): List<Profile>

    @Update
    fun updateProfile(profile: Profile)

    @Insert
    fun addProfile(profile: Profile)

    @Delete
    fun deleteProfile(profile: Profile)
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM debt WHERE pId = :pId ORDER BY date")
    fun getDebtsByPId(pId: Int): List<Debt>

    @Insert
    fun addDebt(debt: Debt): Long

    @Query("DELETE FROM debt WHERE dId in (:dIds)")
    fun deleteDebtsByDIds(dIds: List<Int>)

    @Query("DELETE FROM debt WHERE pId == :pId")
    fun deleteDebtsByPId(pId: Int)
}

@Database(entities = [Profile::class, Debt::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun debtDao(): DebtDao
}


/**
 * Handles database interactions.
 */
class DBHandler(app: MainActivity) {

    private val db: AppDatabase = Room.databaseBuilder(
        app.applicationContext,
        AppDatabase::class.java, "friendlydebts.db"
    )
        .createFromAsset("database/friendlydebts.db")
        .allowMainThreadQueries()
        .build()

    private val profileDao: ProfileDao = db.profileDao()
    private val debtDao: DebtDao = db.debtDao()

    /**
     * Retrieve a list of all profiles.
     */
    fun getProfiles(): List<Profile> {
        return profileDao.getAll()
    }

    /**
     * Update a profile.
     */
    fun updateProfile(profile: Profile) {
        profileDao.updateProfile(profile)
    }

    /**
     * Add a new profile.
     */
    fun addProfile(profile: Profile) {
        profileDao.addProfile(profile)
    }

    /**
     * Delete a profile and its corresponding debts.
     */
    fun deleteProfile(profile: Profile) {
        profileDao.deleteProfile(profile)
        debtDao.deleteDebtsByPId(profile.pId!!)
    }

    /**
     * Retrieve a list of debts corresponding to a profile id.
     */
    fun getDebtsByPId(pId: Int): List<Debt> {
        return debtDao.getDebtsByPId(pId)
    }

    /**
     * Add a new debt.
     */
    fun addDebt(debt: Debt): Int {
        return debtDao.addDebt(debt).toInt()
    }

    /**
     * Delete all debts corresponding to a list of debt ids.
     */
    fun deleteDebtsByDIds(dIds: List<Int>) {
        debtDao.deleteDebtsByDIds(dIds)
    }

    /**
     * Delete all debts corresponding to a profile id.
     */
    fun deleteDebtsByPId(pId: Int) {
        debtDao.deleteDebtsByPId(pId)
    }

}