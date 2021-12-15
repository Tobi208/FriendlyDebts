package com.fdsolutions.friendlydebts

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Color
import android.text.InputType
import android.view.LayoutInflater
import android.widget.*
import androidx.core.view.setPadding
import java.util.*

/**
 * Builds and handles all dialogs.
 */
class DialogHandler(private var app: MainActivity) {

    private lateinit var actions: ActionHandler

    // dialogs
    lateinit var createNewProfile: AlertDialog
    lateinit var changeProfile: AlertDialog
    lateinit var renameProfile: AlertDialog
    lateinit var addDebt: AlertDialog
    lateinit var editDebt: AlertDialog
    lateinit var about: AlertDialog
    private lateinit var invalidInput: AlertDialog

    // adjustable dialog children
    private lateinit var renameProfileInput: EditText
    private lateinit var createNewProfileInput: EditText
    private lateinit var addProfileSpinnerView: Spinner
    private lateinit var editProfilesSpinnerView: Spinner
    private lateinit var profileSpinnerAdapter: SpinnerAdapter
    private lateinit var changeProfileItems: LinearLayout

    // delay init
    fun init() {
        actions = app.actionHandler
    }

    /**
     * Build all dialogs.
     */
    fun buildDialogs() {
        createNewProfile = buildCreateNewProfileDialog()
        changeProfile = buildChangeProfileDialog()
        renameProfile = buildRenameProfileDialog()
        profileSpinnerAdapter = ArrayAdapter(
            app, R.layout.debt_profiles_item, app.profiles.map { it.name }
        )
        addDebt = buildAddDebtDialog()
        editDebt = buildEditDebtDialog()
        about = buildAboutDialog()
        invalidInput = buildInvalidInputDialog()
    }

    /**
     * Update dynamic dialogs according to model changes.
     */
    fun updateDialogs() {
        // set items of change profile dialog
        changeProfileItems.removeAllViews()
        addItemsToChangeProfile()

        // reset input of create / rename dialogs
        createNewProfileInput.setText("")
        renameProfileInput.setText(app.activeProfile.name)

        // set items of profile selection in add / edit debt dialogs
        profileSpinnerAdapter = ArrayAdapter(
            app, R.layout.debt_profiles_item, app.profiles.map { it.name }
        )
        addProfileSpinnerView.adapter = profileSpinnerAdapter
        editProfilesSpinnerView.adapter = profileSpinnerAdapter
        addProfileSpinnerView.setSelection(app.profiles.indexOf(app.activeProfile))
        editProfilesSpinnerView.setSelection(app.profiles.indexOf(app.activeProfile))
    }

    /**
     * Build dialog to create a new profile.
     */
    @SuppressLint("InflateParams")
    private fun buildCreateNewProfileDialog(): AlertDialog {
        // set up view
        val dialogView = app.layoutInflater.inflate(R.layout.create_new_profile_dialog, null)
        createNewProfileInput = dialogView.findViewById(R.id.create_new_profile_input)

        val builder = AlertDialog.Builder(app)
        builder.apply {
            setTitle(R.string.create_new_profile_dialog_title)
            setCancelable(true)
            setView(dialogView)
            setPositiveButton(R.string.confirm) { _, _ ->

                // verify input and proceed accordingly
                val text = createNewProfileInput.text.toString().trim()
                if (text.isEmpty())
                    invalidInput.show()
                else
                    actions.createNewProfile(createNewProfileInput.text.toString())
                createNewProfileInput.setText("")
            }
            setNegativeButton(R.string.cancel) { _, _ -> createNewProfileInput.setText("") }
        }
        return builder.create()
    }

    /**
     * Build dialog to change the active profile.
     */
    @SuppressLint("InflateParams")
    private fun buildChangeProfileDialog(): AlertDialog {
        // set up view
        val dialogView = app.layoutInflater.inflate(R.layout.change_profile_dialog, null)
        changeProfileItems = dialogView.findViewById(R.id.change_profile_items)
        addItemsToChangeProfile()

        val builder = AlertDialog.Builder(app)
        builder.apply {
            setTitle(R.string.change_dialog_title)
            setCancelable(true)
            setView(dialogView)
        }
        return builder.create()
    }


    /**
     * Build dialog to rename the active profile.
     */
    @SuppressLint("InflateParams")
    private fun buildRenameProfileDialog(): AlertDialog {
        // set up view
        val dialogView = app.layoutInflater.inflate(R.layout.rename_profile_dialog, null)
        renameProfileInput = dialogView.findViewById(R.id.rename_profile_input)
        renameProfileInput.setText(app.activeProfile.name)

        val builder = AlertDialog.Builder(app)
        builder.apply {
            setTitle(R.string.rename_profile_dialog_title)
            setCancelable(true)
            setView(dialogView)
            setPositiveButton(R.string.confirm) { _, _ ->

                // verify input and only rename if name is different
                val text = renameProfileInput.text.toString().trim()
                if (text.isEmpty()) {
                    invalidInput.show()
                    renameProfileInput.setText(app.activeProfile.name)
                }
                else if (text != app.activeProfile.name)
                    actions.renameProfile(renameProfileInput.text.toString())
            }
            setNegativeButton(R.string.cancel) { _, _ ->
                renameProfileInput.setText(app.activeProfile.name)
            }
        }
        return builder.create()
    }

    /**
     * Create dialog from layout to add new debt.
     */
    @SuppressLint("InflateParams")
    private fun buildAddDebtDialog(): AlertDialog {
        // create form from layout
        val dialogView = LayoutInflater.from(app).inflate(R.layout.add_debt_dialog, null)

        // set up view
        val dateView = dialogView.findViewById<TextView>(R.id.add_debt_date_view)
        val currentDate = sdfApp.format(Date())
        val calendar = Calendar.getInstance()
        dateView.text = currentDate
        hookDatePickerDialog(calendar, dateView)

        addProfileSpinnerView = dialogView.findViewById(R.id.add_debt_profiles_view)
        addProfileSpinnerView.adapter = profileSpinnerAdapter
        addProfileSpinnerView.setSelection(app.profiles.indexOf(app.activeProfile))

        val amountView = dialogView.findViewById<EditText>(R.id.add_debt_amount_view)
        val noteView = dialogView.findViewById<EditText>(R.id.add_debt_note_view)

        val builder = AlertDialog.Builder(app)
        builder.apply {
            setView(dialogView)
            setTitle(R.string.add_debt_dialog_title)
            setPositiveButton(R.string.confirm) { _, _ ->

                // verify input
                val amountText = amountView.text.toString().trim()
                if (amountText.isEmpty())
                    invalidInput.show()
                else
                    actions.insertNewDebt(
                        dateView.text as String,
                        app.profiles[addProfileSpinnerView.selectedItemPosition].pId!!,
                        amountText.toDouble(),
                        noteView.text.toString()
                    )

                // reset selections
                dateView.text = currentDate
                hookDatePickerDialog(calendar, dateView)
                addProfileSpinnerView.setSelection(app.profiles.indexOf(app.activeProfile))
                amountView.setText("")
                noteView.setText("")
            }
            setNegativeButton(R.string.cancel) { _, _ ->
                dateView.text = currentDate
                hookDatePickerDialog(calendar, dateView)
                addProfileSpinnerView.setSelection(app.profiles.indexOf(app.activeProfile))
                amountView.setText("")
                noteView.setText("")
            }
        }
        return builder.create()
    }

    /**
     * Create dialog from layout to edit a debt.
     */
    @SuppressLint("InflateParams")
    private fun buildEditDebtDialog(): AlertDialog {
        // create form from layout
        val dialogView = LayoutInflater.from(app).inflate(R.layout.edit_debt_dialog, null)

        // set up view
        editProfilesSpinnerView = dialogView.findViewById(R.id.edit_debt_profiles_view)
        editProfilesSpinnerView.adapter = profileSpinnerAdapter
        editProfilesSpinnerView.setSelection(app.profiles.indexOf(app.activeProfile))

        val dateView = dialogView.findViewById<TextView>(R.id.edit_debt_date_view)
        val amountView = dialogView.findViewById<EditText>(R.id.edit_debt_amount_view)
        val noteView = dialogView.findViewById<EditText>(R.id.edit_debt_note_view)

        val builder = AlertDialog.Builder(app)
        builder.apply {
            setView(dialogView)
            setTitle(R.string.edit_debt_dialog_title)
            setPositiveButton(R.string.confirm) { _, _ ->

                // verify input
                val amountText = amountView.text.toString().trim()
                if (amountText.isEmpty())
                    invalidInput.show()
                else
                    actions.editDebt(
                        dateView.text as String,
                        app.profiles[editProfilesSpinnerView.selectedItemPosition].pId!!,
                        amountText.toDouble(),
                        noteView.text.toString()
                    )
            }
            setNegativeButton(R.string.cancel) { _, _ -> }
        }
        return builder.create()
    }

    /**
     * Build dialog to show extremely useful information.
     */
    private fun buildAboutDialog(): AlertDialog {
        val builder = AlertDialog.Builder(app)
        builder.apply {
            setTitle(R.string.about_dialog_title)
            setCancelable(true)
            setMessage("This is a beta test!")
        }
        return builder.create()
    }

    /**
     * Build dialog to alert the user of invalid input.
     */
    private fun buildInvalidInputDialog(): AlertDialog {
        val builder = AlertDialog.Builder(app)
        builder.apply {
            setTitle(R.string.error)
            setCancelable(true)
            setMessage(R.string.invalid_input)
        }
        return builder.create()
    }

    /**
     * Add items to the change profile dialog view programmatically.
     */
    private fun addItemsToChangeProfile() {
        app.profiles.forEach { p ->
            val textView = TextView(app)
            textView.apply {
                text = p.name
                setTextColor(Color.parseColor(
                    if (p == app.activeProfile) "#FFBB86FC" else "#FFC8C8C8"
                ))
                setPadding(20)
                textSize = 20F

                // change to the selected profile if it's inactive
                setOnClickListener {
                    changeProfile.dismiss()
                    if (p != app.activeProfile)
                        actions.changeProfile(p.pId!!)
                }
            }
            changeProfileItems.addView(textView)
        }
    }

    /**
     * Hook up a date picker dialog to a date text view with a specified date.
     */
    fun hookDatePickerDialog(calendar: Calendar, dateView: TextView) {
        val dateSetListener =
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, monthOfYear)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                dateView.text = sdfApp.format(calendar.time)
            }
        dateView.setOnClickListener {
            DatePickerDialog(
                app, dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

}