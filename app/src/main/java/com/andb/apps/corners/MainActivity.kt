package com.andb.apps.corners

import android.annotation.TargetApi
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.dialog_custom_value.view.*
import java.util.*

class MainActivity : AppCompatActivity(), ColorPickerDialogListener {

    var individualCollapse = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        loadValues()
        setupWindow()
        setupContent()
    }

    private fun setupWindow() {
        val toolbar = findViewById<View>(R.id.toolbar) as androidx.appcompat.widget.Toolbar
        setSupportActionBar(toolbar)
        toolbar.setTitleTextColor(Color.BLACK)
        toolbar.overflowIcon?.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP)
        window.navigationBarColor = resources.getColor(R.color.colorAccent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            window.statusBarColor = resources.getColor(R.color.colorAccent)
        }

    }

    private fun loadValues() {
        checkDrawOverlayPermission()
        Values.sizes = Persist.getIndividualSizes(this)
        Values.toggleState = Persist.getSavedToggleState(this)
        Values.cornerStates = Persist.getIndividualState(this)
        Values.cornerColor = Persist.getSavedCornerColor(this)
        Values.firstRun = Persist.getSavedFirstRun(this)
    }

    private fun setupContent() {
        overlay_toggle.isChecked = Values.toggleState
        currentVal.text = Values.commonSize().toString()
        sizeDialog(currentVal, -1)
        seekBar.progress = Values.commonSize()

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                Values.sizes = Values.listFromSize(progress)
                updateService()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })


        overlay_toggle.setOnCheckedChangeListener { _, isChecked ->
            val serviceIntent = Intent(this, CornerService::class.java)
            Values.toggleState = isChecked
            when (isChecked) {
                true -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (Settings.canDrawOverlays(this)) {
                            Log.d("serviceStart", "service started")
                            startService(serviceIntent)
                            if (Values.firstRun) {
                                showHelp()
                                Values.firstRun = false
                            }
                        } else {
                            checkDrawOverlayPermission()
                            overlay_toggle.isChecked = false
                        }
                    } else {
                        startService(serviceIntent)
                    }
                }
                false -> {
                    stopService(serviceIntent)
                }
            }
        }


        val scale = resources.displayMetrics.density
        val pixels = (52 * scale + 0.5f).toInt()
        val params = individual_card.layoutParams
        params.height = pixels
        collapseToggleSpace.setOnClickListener {
            if (individualCollapse) {

                TransitionManager.beginDelayedTransition(
                    individual_card, TransitionSet().addTransition(ChangeBounds())
                )
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT
                individual_card.layoutParams = params

                collapseButton.animate().setDuration(100).rotation(0f)

            } else {
                TransitionManager.beginDelayedTransition(
                    individual_card, TransitionSet()
                        .addTransition(ChangeBounds())
                )
                params.height = pixels
                individual_card.layoutParams = params
                collapseButton.animate().setDuration(100).rotation(180f)
            }
            individualCollapse = !individualCollapse
        }

        tagColorPreview.color = Values.cornerColor
        colorPreviewLayout.setOnClickListener {
            ColorPickerDialog.newBuilder()
                .setColor(Values.cornerColor)
                .setShowAlphaSlider(false)
                .setDialogId(DIALOG_ID)
                .show(this)
        }

        test_button.setOnClickListener {
            test_image.setImageBitmap(ModifyWallpaper.applyToLockscreenWallpaper(this, this, Values.commonSize()/*TODO: Use all sizes*/, Values.cornerColor))
        }

        setupIndividual()

    }

    fun setupIndividual() {
        switchTopL.isChecked = Values.cornerStates[0]
        switchTopR.isChecked = Values.cornerStates[1]
        switchBottomL.isChecked = Values.cornerStates[2]
        switchBottomR.isChecked = Values.cornerStates[3]

        switchTopL.setOnCheckedChangeListener { _, isChecked ->
            Values.cornerStates[0] = isChecked
            setIndividualVisibility()
        }
        switchTopR.setOnCheckedChangeListener { _, isChecked ->
            Values.cornerStates[1] = isChecked
            setIndividualVisibility()
        }
        switchBottomL.setOnCheckedChangeListener { _, isChecked ->
            Values.cornerStates[2] = isChecked
            setIndividualVisibility()
        }
        switchBottomR.setOnCheckedChangeListener { _, isChecked ->
            Values.cornerStates[3] = isChecked
            setIndividualVisibility()
        }

        sizeDialog(switchTopL, 0)
        sizeDialog(switchTopR, 1)
        sizeDialog(switchBottomL, 2)
        sizeDialog(switchBottomR, 3)

    }

    fun sizeDialog(view: View, index: Int) {
        view.setOnLongClickListener {
            val currentSize = if (index != -1) Values.sizes[index] else Values.commonSize()
            Log.d("currentSize", "currentSize: $currentSize")

            val dialogView = layoutInflater.inflate(R.layout.dialog_custom_value, null)
            dialogView.customSizeEditText.setText(currentSize.toString())
            AlertDialog.Builder(this).setView(dialogView)
                .setPositiveButton(R.string.dialog_ok) { dialog, _ ->
                    val size = dialogView.customSizeEditText.text.toString().toInt()
                    if (index != -1) {
                        Values.sizes[index] = size
                    } else {
                        Values.sizes = Values.listFromSize(size)
                    }
                    Log.d("newSizes", Values.sizes.toString())
                    updateService()
                    dialog.cancel()
                }.show()

            true
        }
    }

    fun updateService() {
        CornerService.sizes = Values.sizes
        currentVal.text = Values.commonSize().toString()
        seekBar.progress = Values.commonSize()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this@MainActivity)) {
                Log.d("change size", "change size")
                CornerService.setSize(this@MainActivity)
            } else {
                Toast.makeText(this@MainActivity, "Overlay permission not granted", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            CornerService.setSize(this@MainActivity)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId


        when(id){
            R.id.menuItemHelp->showHelp()
        }


        return super.onOptionsItemSelected(item)
    }

    private fun showHelp(){
        AlertDialog.Builder(this)
            .setView(R.layout.help_dialog)
            .setNegativeButton(R.string.dialog_ok) { dialog, _ ->
                dialog.cancel()
            }
            .create()
            .show()
    }

    //Request screenOverlay permission
    @TargetApi(Build.VERSION_CODES.M)
    fun checkDrawOverlayPermission() {
        /* check if we already  have permission to draw over other apps */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(applicationContext)) {
            /* if not construct intent to request permission */
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            /* request permission via start activity for result */
            startActivityForResult(intent, REQUEST_CODE)
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        /* check if received result code
         is equal our requested code for draw permission  */
        if (requestCode == REQUEST_CODE) {
            /* if so check once again if we have permission */
            if (!Settings.canDrawOverlays(applicationContext)) {
                Toast.makeText(this, "Overlay permision not granted!", Toast.LENGTH_SHORT).show()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onColorSelected(dialogId: Int, color: Int) {
        when (dialogId) {
            DIALOG_ID -> {
                Log.d("colorSelected", Integer.toHexString(color))
                // We got result from the dialog that is shown when clicking on the icon in the action bar.
                Values.cornerColor = color
                tagColorPreview.color = color
                CornerService.setColor(Values.cornerColor)
            }
        }
    }

    override fun onDialogDismissed(dialogId: Int) {

    }

    override fun onPause() {
        super.onPause()
        Persist.saveCornerSizes(this, Values.sizes)
        Persist.saveToggleState(this, Values.toggleState)
        Persist.saveIndivdualState(this, Values.cornerStates[0], Values.cornerStates[1], Values.cornerStates[2], Values.cornerStates[3])
        Persist.saveCornerColor(this, Values.cornerColor)
        Persist.saveFirstRun(this, Values.firstRun)
    }

    companion object {
        const val DIALOG_ID = 0
        const val REQUEST_CODE: Int = 34387

        fun setIndividualVisibility() {
            if (CornerService.mView != null) {
                val topLeft = CornerService.mView!!.findViewById<View>(R.id.topLeft) as TextView
                val topRight = CornerService.mView!!.findViewById<View>(R.id.topRight) as TextView
                val bottomLeft = CornerService.mView!!.findViewById<View>(R.id.bottomLeft) as TextView
                val bottomRight = CornerService.mView!!.findViewById<View>(R.id.bottomRight) as TextView

                val views = ArrayList(Arrays.asList(topLeft, topRight, bottomLeft, bottomRight))

                for (i in views.indices) {
                    if (Values.cornerStates[i]) {
                        views[i].visibility = View.VISIBLE
                    } else {
                        views[i].visibility = View.GONE
                    }
                }
            }
        }
    }
}