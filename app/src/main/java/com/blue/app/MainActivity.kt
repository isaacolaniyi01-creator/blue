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
import android.widget.ScrollView
import android.widget.TextView
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

private const val SUPPORT_PHONE = "08030001122"

private enum class Screen(val flipperIndex: Int) {
    ONBOARDING(0), REQUEST(1), TRACK(2)
}

private data class PaymentAccount(val number: String, val label: String, val bank: String)

// Pool of accounts we rotate through for "Make payment" — reshuffled once all 4 are used.
private val PAYMENT_ACCOUNTS = listOf(
    PaymentAccount("8127 445 210", "Blue Logistics", "Moniepoint"),
    PaymentAccount("0234 981 663", "Blue Logistics Ops", "GTBank"),
    PaymentAccount("5610 772 044", "Blue Logistics Ltd", "Kuda"),
    PaymentAccount("3399 108 527", "Blue Gas Services", "Opay")
)

class MainActivity : AppCompatActivity() {

    private lateinit var flipper: ViewFlipper
    private lateinit var bottomNav: LinearLayout

    // Order state — in-memory only, no backend yet.
    private var kg: Double = 12.5
    private var scheduled: Boolean = false
    private var scheduledTimeLabel: String? = null
    private var orderStarted = false
    private var paid = false
    private var accountQueue: MutableList<PaymentAccount> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        flipper = findViewById(R.id.flipper)
        bottomNav = findViewById(R.id.bottomNav)

        setupOnboarding()
        setupFeatureList()
        setupRequestAndPayScreen()
        setupTrackAndDoneScreen()
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

    // ---- Request + Pay (one scrollable page) -------------------------------

    private fun setupRequestAndPayScreen() {
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

        val requestScroll = findViewById<View>(R.id.etKg).let { findScrollViewAncestor(it) }

        val btnMakePayment = findViewById<View>(R.id.btnMakePayment)
        val rowPaymentLoading = findViewById<View>(R.id.rowPaymentLoading)
        val sectionPaymentDetails = findViewById<View>(R.id.sectionPaymentDetails)

        findViewById<View>(R.id.btnRequestPickup).setOnClickListener {
            kg = etKg.text.toString().toDoubleOrNull() ?: kg
            orderStarted = true
            setNavEnabled(R.id.navTrack, true)
            // Scroll down within the page to reveal the Make payment button.
            requestScroll?.post {
                requestScroll.smoothScrollTo(0, btnMakePayment.top)
            }
        }

        btnMakePayment.setOnClickListener {
            btnMakePayment.visibility = View.GONE
            rowPaymentLoading.visibility = View.VISIBLE
            // Simulated lookup delay — swap for a real API call later.
            Handler(Looper.getMainLooper()).postDelayed({
                val account = nextAccount()
                findViewById<TextView>(R.id.tvAccountNumber).text = account.number
                findViewById<TextView>(R.id.tvAccountLabel).text = account.label
                findViewById<TextView>(R.id.tvAccountBank).text = account.bank

                rowPaymentLoading.visibility = View.GONE
                sectionPaymentDetails.visibility = View.VISIBLE
                requestScroll?.post {
                    requestScroll.smoothScrollTo(0, sectionPaymentDetails.top)
                }
            }, 1200)
        }

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
                setNavEnabled(R.id.navTrack, true)
                goTo(Screen.TRACK)
            }, 1100)
        }
    }

    // Hands out accounts one at a time from a shuffled pool of 4; reshuffles once exhausted
    // so no account repeats until every other one has been shown.
    private fun nextAccount(): PaymentAccount {
        if (accountQueue.isEmpty()) {
            accountQueue = PAYMENT_ACCOUNTS.shuffled().toMutableList()
        }
        return accountQueue.removeAt(0)
    }

    private fun findScrollViewAncestor(view: View): ScrollView? {
        var parent = view.parent
        while (parent != null) {
            if (parent is ScrollView) return parent
            parent = parent.parent
        }
        return null
    }

    // ---- Track + Done (one scrollable page) --------------------------------

    private val trackingNotifications = listOf(
        "Rider is at the filling station",
        "Rider is on the way back to you"
    )
    private var trackingNotifIndex = 0

    private fun setupTrackAndDoneScreen() {
        findViewById<View>(R.id.btnPing).setOnClickListener {
            findViewById<TextView>(R.id.tvNotifBannerText).text =
                trackingNotifications[trackingNotifIndex % trackingNotifications.size]
            trackingNotifIndex++
            findViewById<View>(R.id.notifBanner).visibility = View.VISIBLE
        }

        findViewById<View>(R.id.btnConfirmReceived).setOnClickListener {
            findViewById<TextView>(R.id.tvWeightDetail).text = "${kg}kg confirmed full"
            // Real app: log this confirmation to the rider/ops tablet here.
        }
    }

    // ---- Bottom nav ---------------------------------------------------------

    private fun setupBottomNav() {
        configureNavItem(R.id.navRequest, R.drawable.ic_flame, "Request") { goTo(Screen.REQUEST) }
        configureNavItem(R.id.navTrack, R.drawable.ic_pin, "Track") { goTo(Screen.TRACK) }
        configureNavItem(R.id.navCall, R.drawable.ic_phone, "Call") { placeCall() }

        setNavEnabled(R.id.navTrack, false)
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
        if (screen == Screen.TRACK && !orderStarted) return
        flipper.displayedChild = screen.flipperIndex
        highlightNav(screen)
    }

    private fun highlightNav(screen: Screen) {
        val map = mapOf(
            Screen.REQUEST to R.id.navRequest,
            Screen.TRACK to R.id.navTrack,
        )
        listOf(R.id.navRequest, R.id.navTrack, R.id.navCall).forEach { id ->
            findViewById<View>(id).setBackgroundResource(0)
        }
        map[screen]?.let { findViewById<View>(it).setBackgroundResource(R.drawable.bg_nav_active) }
    }

    private fun placeCall() {
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$SUPPORT_PHONE")))
    }
}
