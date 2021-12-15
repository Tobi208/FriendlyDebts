package com.fdsolutions.friendlydebts

import android.graphics.Color
import android.widget.EditText
import android.widget.TableRow
import android.widget.TextView
import java.util.*

/**
 * Handles actions called by view elements or dialogs.
 */
class ActionHandler(private val app: MainActivity) {

    private lateinit var db: DBHandler
    private lateinit var dialogs: DialogHandler

    // aux
    private lateinit var editDebtRow: TableRow

    // delay init
    fun init() {
        db = app.dbHandler
        dialogs = app.dialogHandler
    }

    /**
     * Create a new profile and change to it.
     */
    fun createNewProfile(name: String) {
        // deactivate active profile
        app.activeProfile.focus = 0
        db.updateProfile(app.activeProfile)

        // create new active profile and restart
        val newProfile = Profile(pId = null, name, 0.0, 1)
        db.addProfile(newProfile)
        app.restart()
    }

    /**
     * Change to a specified profile.
     */
    fun changeProfile(pId: Int) {
        // deactivate active profile
        app.activeProfile.focus = 0
        db.updateProfile(app.activeProfile)

        // activate specified profile and restart
        val nextActiveProfile = app.profiles.find { it.pId == pId }!!
        nextActiveProfile.focus = 1
        db.updateProfile(nextActiveProfile)
        app.restart()
    }

    /**
     * Rename the active profile.
     */
    fun renameProfile(newName: String) {
        // rename active profile
        app.activeProfile.name = newName
        db.updateProfile(app.activeProfile)

        // update specific view/control elements instead of restarting
        dialogs.updateDialogs()
        app.title = newName
    }

    /**
     * Delete the active profile.
     * Change to arbitrary profile afterwards.
     * Create a default profile if no profiles exist.
     */
    fun deleteProfile() {
        // delete active profile
        db.deleteProfile(app.activeProfile)

        // check if arbitrary alternative profile exists
        val altProfile = app.profiles.find { it != app.activeProfile }
        if (altProfile != null) {

            // change to alternative profile
            altProfile.focus = 1
            db.updateProfile(altProfile)

            // else create default profile
        } else {
            val defaultName = app.applicationContext.getString(R.string.default_profile_name)
            val defaultFriend = Profile(pId = null, defaultName, 0.0, 1)
            db.addProfile(defaultFriend)
        }

        app.restart()
    }

    /**
     * Insert a new debt to any profile.
     */
    fun insertNewDebt(date: String, pId: Int, amount: Double, note: String?) {
        // instantiate a new debt and insert it to the database
        val debt = Debt(dId = null, pId, dateAppToDB(date), amount, note)
        app.debts.add(debt)
        debt.dId = db.addDebt(debt)

        // update the corresponding profile
        val profile = app.profiles.find { it.pId == pId }!!
        profile.total = round2Decimals(profile.total + amount)
        db.updateProfile(profile)

        // if it corresponds to the active profile, update the view
        if (pId == app.activeProfile.pId) {
            app.insertDebtToTable(debt)
            app.totalDebtNumberView.text = app.activeProfile.total.toString()
        }
    }

    /**
     * Populate the edit debt dialog and show it.
     */
    fun openEditDebt(row: TableRow, debt: Debt): Boolean {
        // temp save the debt row of interest
        editDebtRow = row
        val calendar = Calendar.getInstance()

        // show edit debt dialog
        val edd = dialogs.editDebt
        edd.show()

        // populate the dialog with data from the debt
        val dateView = edd.findViewById<TextView>(R.id.edit_debt_date_view)
        dateView.text = dateDBToApp(debt.date)
        calendar.time = sdfDB.parse(debt.date)!!
        dialogs.hookDatePickerDialog(calendar, dateView)

        edd.findViewById<EditText>(R.id.edit_debt_amount_view).setText(debt.amount.toString())
        edd.findViewById<EditText>(R.id.edit_debt_note_view).setText(debt.note)

        // OnLongClickListener needs to return a Boolean?
        return true
    }

    /**
     * Edit a debt of the active profile.
     */
    fun editDebt(date: String, fId: Int, amount: Double, note: String?) {
        // delete the debt
        toggleSelectDebt(editDebtRow)
        deleteSelectedDebts()

        // create a new debt with the specified values and insert it
        insertNewDebt(date, fId, amount, note)
    }

    /**
     * Summarize all debts of the active profile into a single debt.
     */
    fun summarizeAllDebts() {
        // create new debt
        val fId = app.activeProfile.pId!!
        val date = sdfDB.format(Date())
        val amount = app.activeProfile.total
        val note = app.applicationContext.getString(R.string.summarize_debts_note)
        val sumDebt = Debt(dId = null, fId, date, amount, note)

        // update model
        app.debts.clear()
        app.debts.add(sumDebt)

        // update database
        db.deleteDebtsByPId(fId)
        sumDebt.dId = db.addDebt(sumDebt)

        // update view
        app.updateDebtsTable()
    }

    /**
     * Clear all debts of the active profile.
     */
    fun clearDebts() {
        // update model
        app.debts.clear()
        app.activeProfile.total = 0.0

        // update database
        db.updateProfile(app.activeProfile)
        db.deleteDebtsByPId(app.activeProfile.pId!!)

        // update view
        app.totalDebtNumberView.text = "0.0"
        app.updateDebtsTable()
    }


    /**
     * Toggle selected status of a debt row.
     * Handles toggle of selection mode.
     */
    fun toggleSelectDebt(debtRow: TableRow) {
        val sdv = app.selectedDebtViews

        // unselect
        if (sdv.contains(debtRow)) {
            debtRow.setBackgroundColor(Color.parseColor("#FF202020"))
            sdv.remove(debtRow)
            if (sdv.isEmpty())
                app.exitSelectionMode()

            // select
        } else {
            debtRow.setBackgroundColor(Color.parseColor("#FF303030"))
            if (sdv.isEmpty())
                app.enterSelectionMode()
            sdv.add(debtRow)
        }
    }

    /**
     * Unselect all debt rows.
     */
    fun cancelSelection() {
        while (app.selectedDebtViews.isNotEmpty())
            toggleSelectDebt(app.selectedDebtViews.last())
    }

    /**
     * Delete all selected debt rows.
     */
    fun deleteSelectedDebts() {
        // gather corresponding debts
        val selectedDebtDIds = app.selectedDebtViews.map { app.debtViewIdToDId[it.id]!! }

        // remove from model
        val selDebts = app.debts.filter { selectedDebtDIds.contains(it.dId) }
        selDebts.forEach {
            app.debts.remove(it)
            app.activeProfile.total = round2Decimals(app.activeProfile.total - it.amount)
        }

        // remove from database
        db.deleteDebtsByDIds(selectedDebtDIds)
        db.updateProfile(app.activeProfile)

        // remove from view
        app.selectedDebtViews.forEach {
            app.debtsTableView.removeView(it)
            app.debtViewIdToDId.remove(it.id)
        }

        // update view
        app.totalDebtNumberView.text = app.activeProfile.total.toString()
        app.exitSelectionMode()
    }

    /**
     * Summarize all selected debts into a single new debt.
     */
    fun summarizeSelectedDebts() {
        // gather corresponding debts
        val selectedDebtDIds = app.selectedDebtViews.map { app.debtViewIdToDId[it.id]!! }

        // create new debt
        val selDebts = app.debts.filter { selectedDebtDIds.contains(it.dId) }
        val fId = app.activeProfile.pId!!
        val date = sdfDB.format(Date())
        val amount = round2Decimals(selDebts.sumOf { it.amount })
        val note = app.applicationContext.getString(R.string.summarize_debts_note)
        val sumDebt = Debt(dId = null, fId, date, amount, note)

        // update model
        selDebts.forEach { app.debts.remove(it) }
        app.debts.add(sumDebt)

        // update database
        db.deleteDebtsByDIds(selectedDebtDIds)
        sumDebt.dId = db.addDebt(sumDebt)

        // remove from and add to view
        app.selectedDebtViews.forEach {
            app.debtsTableView.removeView(it)
            app.debtViewIdToDId.remove(it.id)
        }
        app.insertDebtToTable(sumDebt)

        // update view
        app.exitSelectionMode()
    }

}
