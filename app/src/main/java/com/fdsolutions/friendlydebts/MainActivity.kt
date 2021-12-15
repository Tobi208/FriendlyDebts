package com.fdsolutions.friendlydebts

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * Handles control elements, view logic, and model.
 */
class MainActivity : AppCompatActivity() {

    // control
    lateinit var dbHandler: DBHandler
    lateinit var actionHandler: ActionHandler
    lateinit var dialogHandler: DialogHandler

    // model
    lateinit var profiles: List<Profile>
    lateinit var activeProfile: Profile
    lateinit var debts: ArrayList<Debt>

    // view
    private lateinit var optionsMenu: Menu
    lateinit var tableScrollView: ScrollView
    lateinit var debtsTableView: TableLayout
    lateinit var totalDebtNumberView: TextView
    private lateinit var fab: FloatingActionButton

    // model/view aux binders
    lateinit var debtViewIdToDId: HashMap<Int, Int>
    lateinit var selectedDebtViews: ArrayList<TableRow>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // instantiate control elements
        dbHandler = DBHandler(this)
        actionHandler = ActionHandler(this)
        dialogHandler = DialogHandler(this)

        // delayed init
        actionHandler.init()
        dialogHandler.init()

        // hook relevant view elements
        tableScrollView = findViewById(R.id.tableScrollView)
        debtsTableView = findViewById(R.id.debtsTableView)
        totalDebtNumberView = findViewById(R.id.totalDebtNumberView)
        fab = findViewById(R.id.fab)

        // populate model and view
        start()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.options_menu, menu)
        optionsMenu = menu!!
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        // hook menu items to actions
        return when (item.itemId) {
            R.id.action_new_profile -> {
                dialogHandler.createNewProfile.show()
                true
            }
            R.id.action_change_profile -> {
                dialogHandler.changeProfile.show()
                true
            }
            R.id.action_rename_profile -> {
                dialogHandler.renameProfile.show()
                true
            }
            R.id.action_delete_profile -> {
                actionHandler.deleteProfile()
                true
            }
            R.id.action_summarize_debts -> {
                actionHandler.summarizeAllDebts()
                true
            }
            R.id.action_clear_debts -> {
                actionHandler.clearDebts()
                true
            }
            R.id.action_about -> {
                dialogHandler.about.show()
                true
            }
            R.id.action_selection_delete -> {
                actionHandler.deleteSelectedDebts()
                true
            }
            R.id.action_selection_summarize -> {
                actionHandler.summarizeSelectedDebts()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Set up the app for the first time.
     */
    private fun start() {
        // load model from database
        profiles = dbHandler.getProfiles()
        activeProfile = profiles.find { it.focus == 1 }!!
        debts = dbHandler.getDebtsByPId(activeProfile.pId!!) as ArrayList<Debt>

        // init (aux) controls
        dialogHandler.buildDialogs()
        selectedDebtViews = ArrayList()
        debtViewIdToDId = HashMap()

        // set up view
        title = activeProfile.name
        totalDebtNumberView.text = activeProfile.total.toString()
        fab.setOnClickListener { dialogHandler.addDebt.show() }
        debts.forEach { addDebtToTable(it) }
        scrollDebtsTableToBottom()
    }

    /**
     * Set up the app again after a significant change to the model.
     */
    fun restart() {
        // reload model from database
        profiles = dbHandler.getProfiles()
        activeProfile = profiles.find { it.focus == 1 }!!
        debts = dbHandler.getDebtsByPId(activeProfile.pId!!) as ArrayList<Debt>

        // update (aux) controls
        dialogHandler.updateDialogs()
        selectedDebtViews.clear()

        // update view
        title = activeProfile.name
        totalDebtNumberView.text = activeProfile.total.toString()
        updateDebtsTable()
    }

    /**
     * Create a new row in the debts table and populate it with data from the model.
     * The row is created in the last place of the table.
     */
    private fun addDebtToTable(debt: Debt) {
        // create new row, inserted in last place
        layoutInflater.inflate(R.layout.debt_row, debtsTableView)

        // set up new row
        val row = debtsTableView.getChildAt(debtsTableView.childCount - 1) as TableRow
        row.id = View.generateViewId()
        debtViewIdToDId[row.id] = debt.dId!!
        row.setOnClickListener { actionHandler.toggleSelectDebt(row) }
        row.setOnLongClickListener { actionHandler.openEditDebt(row, debt) }

        // scrollview for note consumes clicks, set up listener separately
        val noteLayout = row.findViewWithTag<LinearLayout>("note_layout")
        noteLayout.setOnClickListener { actionHandler.toggleSelectDebt(row) }
        noteLayout.setOnLongClickListener { actionHandler.openEditDebt(row, debt) }

        // populate new row with data
        row.findViewWithTag<TextView>("amount").text = debt.amount.toString()
        row.findViewWithTag<TextView>("date").text = dateDBToApp(debt.date)
        row.findViewWithTag<TextView>("note").text = debt.note ?: ""
    }

    /**
     * Create a new row in the debts table and populate it with data from the model.
     * The row will be placed after the first row with an equal or lesser date.
     */
    fun insertDebtToTable(debt: Debt) {
        addDebtToTable(debt)

        // if debt is the only entry, no adjustment necessary
        if (debtsTableView.childCount == 1)
            return

        // else check if adjustment is necessary
        // by comparing the dates in reverse order
        // WARNING: hacky index logic ahead
        // i -> index of first row with lower or equal date
        // insert new debt one above of i
        // or at 0 if no date lower or equal exists
        val lastIdx = debtsTableView.childCount - 1
        var i = lastIdx - 1
        var date = debtsTableView.getChildAt(i).findViewWithTag<TextView>("date").text as String
        while (dateAppToDB(date) > debt.date) {
            i--
            if (i < 0) break
            date = debtsTableView.getChildAt(i).findViewWithTag<TextView>("date").text as String
        }

        // adjust index of new debt if necessary
        if (i < lastIdx - 1) {
            val newDebtRow = debtsTableView.getChildAt(lastIdx) as TableRow
            debtsTableView.removeViewAt(lastIdx)
            debtsTableView.addView(newDebtRow, i + 1)
        }
        scrollDebtsTableToBottom()
    }

    /**
     * Clear the table and add all debts again.
     */
    fun updateDebtsTable() {
        debtsTableView.removeAllViews()
        debtViewIdToDId.clear()
        debts.forEach { addDebtToTable(it) }
        scrollDebtsTableToBottom()
    }

    /**
     * Scrolls the debts table view to its bottom.
     */
    private fun scrollDebtsTableToBottom() {
        tableScrollView.post { tableScrollView.fullScroll(View.FOCUS_DOWN) }
    }

    /**
     * Enter a mode to select debt rows to be deleted.
     * Suspends all other activity until selection is cleared.
     */
    fun enterSelectionMode() {
        // toggle visibility of menu items
        // only selection mode options will show
        optionsMenu.children.forEach { it.isVisible = !it.isVisible }

        // disable debt editing
        debtsTableView.children.forEach {
            it.isLongClickable = false
            it.findViewWithTag<LinearLayout>("note_layout").isLongClickable = false
        }

        // transform fab to cancel selection mode
        fab.setImageResource(R.drawable.fab_cancel_selection_svg)
        fab.setOnClickListener { actionHandler.cancelSelection() }
    }

    /**
     * Exit mode to select debt rows to be deleted.
     */
    fun exitSelectionMode() {
        selectedDebtViews.clear()
        // toggle visibility of menu items
        // all but selection mode options will show
        optionsMenu.children.forEach { it.isVisible = !it.isVisible }

        // enable debt editing
        debtsTableView.children.forEach {
            it.isLongClickable = true
            it.findViewWithTag<LinearLayout>("note_layout").isLongClickable = true
        }

        // transform fab back to normal state
        fab.setImageResource(R.drawable.fab_add_debt_svg)
        fab.setOnClickListener { dialogHandler.addDebt.show() }
    }

}
