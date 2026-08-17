package com.blue.app

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

private const val SUPPORT_PHONE = "08030001122"

private enum class Screen(val flipperIndex: Int) {
    ONBOARDING(0), REQUEST(1), TRACK(2), PAY(3), DONE(4)
}

class MainActivity : AppCompatActivity() {

    private lateinit var flipper: ViewFlipper
    private lateinit var bottomNav: LinearLayout

    // Order state — in-memory only, no backend yet.
    private var kg: Double = 12.5
    private var scheduled: Boolean = false
    private var scheduledTimeLabel: String? = null
    private var orderStarted = false
    private var paid = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        flipper = findViewById(R.id.flipper)
        bottomNav = findViewById(R.id.bottomNav)

        setupOnboarding()
        setupFeatureList()
        setupRequestScreen()
        setupTrackScreen()
        setupPayScreen()
        setupDoneScreen()
        setupBottomNav()
    }

    // ---- Onboarding ----------------------------------------------------

    private fun setupOnboarding() {
        findViewById<View>(R.id.btnGetStarted).setOnClickListener {
            bottomNav.visibility = View.VISIBLE
            goTo(Screen.REQUEST)
        }
        findViewById<View>(R.id.tvCallOnboarding).setOnClickListener { placeCall() }
    }

    private fun setupFeatureList() {
        fillFeatureRow(R.id.featureVerified, R.drawable.ic_shield_check, "Verified stations only", "No diluted, no shortchanged")
        fillFeatureRow(R.id.featureWeighed, R.drawable.ic_scale, "Weighed before and after", "See the numbers yourself")
        fillFeatureRow(R.id.featureLoaner, R.drawable.ic_tank, "Free loaner cylinder", "Never without gas")
    }

    private fun fillFeatureRow(includeId: Int, iconRes: Int, title: String, subtitle: String) {
        val row = findViewById<View>(includeId)
        row.findViewById<ImageView>(R.id.featureIcon).setImageResource(iconRes)
        row.findViewById<TextView>(R.id.featureTitle).text = title
        row.findViewById<TextView>(R.id.featureSubtitle).text = subtitle
    }

    // ---- Request ---------------------------------------------------------

    private fun setupRequestScreen() {
        val etKg = findViewById<EditText>(R.id.etKg)
        etKg.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                kg = etKg.text.toString().toDoubleOrNull() ?: kg
            }
        }

        val chipNow = findViewById<TextView>(R.id.chipNow)
        val chipSchedule = findViewById<TextView>(R.id.chipSchedule)
        val tvScheduledTime = findViewById<TextView>(R.id.tvScheduledTime)

        chipNow.setOnClickListener {
            scheduled = false
            chipNow.setBackgroundResource(R.drawable.chip_active)
            chipNow.setTextColor(getColor(R.color.accent))
            chipSchedule.setBackgroundResource(R.drawable.chip_inactive)
            chipSchedule.setTextColor(getColor(R.color.muted))
            tvScheduledTime.visibility = View.GONE
        }

        chipSchedule.setOnClickListener {
            scheduled = true
            chipSchedule.setBackgroundResource(R.drawable.chip_active)
            chipSchedule.setTextColor(getColor(R.color.accent))
            chipNow.setBackgroundResource(R.drawable.chip_inactive)
            chipNow.setTextColor(getColor(R.color.muted))

            val now = Calendar.getInstance()
            TimePickerDialog(
                this,
                { _, hour, minute ->
                    val label = String.format("%02d:%02d", hour, minute)
                    scheduledTimeLabel = label
                    tvScheduledTime.text = label
                    tvScheduledTime.visibility = View.VISIBLE
                },
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                true
            ).show()
        }

        findViewById<View>(R.id.btnRequestPickup).setOnClickListener {
            kg = etKg.text.toString().toDoubleOrNull() ?: kg
            orderStarted = true
            setNavEnabled(R.id.navTrack, true)
            setNavEnabled(R.id.navPay, true)
            goTo(Screen.TRACK)
        }
    }

    // ---- Track ----------------------------------------------------------

    private fun setupTrackScreen() {
        findViewById<View>(R.id.btnPing).setOnClickListener {
            findViewById<View>(R.id.notifBanner).visibility = View.VISIBLE
        }
    }

    // ---- Pay --------------------------------------------------------------

    private fun setupPayScreen() {
        val btnPaid = findViewById<View>(R.id.btnPaid)
        val tvChecking = findViewById<View>(R.id.tvChecking)
        val rowPaidDone = findViewById<View>(R.id.rowPaidDone)

        btnPaid.setOnClickListener {
            btnPaid.visibility = View.GONE
            tvChecking.visibility = View.VISIBLE
            // Simulated verification delay — swap for a real payment check later.
            Handler(Looper.getMainLooper()).postDelayed({
                tvChecking.visibility = View.GONE
                rowPaidDone.visibility = View.VISIBLE
                paid = true
                setNavEnabled(R.id.navDone, true)
            }, 1100)
        }
    }

    // ---- Done -------------------------------------------------------------

    private fun setupDoneScreen() {
        findViewById<View>(R.id.btnConfirmReceived).setOnClickListener {
            findViewById<TextView>(R.id.tvWeightDetail).text = "${kg}kg confirmed full"
            // Real app: log this confirmation to the rider/ops tablet here.
        }
    }

    // ---- Bottom nav ---------------------------------------------------------

    private fun setupBottomNav() {
        configureNavItem(R.id.navRequest, R.drawable.ic_flame, "Request") { goTo(Screen.REQUEST) }
        configureNavItem(R.id.navTrack, R.drawable.ic_pin, "Track") { goTo(Screen.TRACK) }
        configureNavItem(R.id.navPay, R.drawable.ic_bank, "Pay") { goTo(Screen.PAY) }
        configureNavItem(R.id.navDone, R.drawable.ic_check, "Done") { goTo(Screen.DONE) }
        configureNavItem(R.id.navCall, R.drawable.ic_phone, "Call") { placeCall() }

        setNavEnabled(R.id.navTrack, false)
        setNavEnabled(R.id.navPay, false)
        setNavEnabled(R.id.navDone, false)
    }

    private fun configureNavItem(includeId: Int, iconRes: Int, label: String, onClick: () -> Unit) {
        val item = findViewById<View>(includeId)
        item.findViewById<ImageView>(R.id.navIcon).setImageResource(iconRes)
        item.findViewById<TextView>(R.id.navLabel).text = label
        item.setOnClickListener { onClick() }
    }

    private fun setNavEnabled(includeId: Int, enabled: Boolean) {
        val item = findViewById<View>(includeId)
        item.isEnabled = enabled
        item.isClickable = enabled
        item.alpha = if (enabled) 1f else 0.35f
    }

    private fun goTo(screen: Screen) {
        // Guard against jumping into screens that need an order first.
        if (screen != Screen.ONBOARDING && screen != Screen.REQUEST && !orderStarted) return
        if (screen == Screen.DONE && !paid) return
        flipper.displayedChild = screen.flipperIndex
        highlightNav(screen)
    }

    private fun highlightNav(screen: Screen) {
        val map = mapOf(
            Screen.REQUEST to R.id.navRequest,
            Screen.TRACK to R.id.navTrack,
            Screen.PAY to R.id.navPay,
            Screen.DONE to R.id.navDone,
        )
        listOf(R.id.navRequest, R.id.navTrack, R.id.navPay, R.id.navDone, R.id.navCall).forEach { id ->
            findViewById<View>(id).setBackgroundResource(0)
        }
        map[screen]?.let { findViewById<View>(it).setBackgroundResource(R.drawable.bg_nav_active) }
    }

    private fun placeCall() {
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$SUPPORT_PHONE")))
    }
}
