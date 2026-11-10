package com.pioneer.nycfirewire.activity

import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback
import com.google.android.gms.maps.StreetViewPanorama
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.StreetViewPanoramaCamera
import com.google.android.gms.maps.model.StreetViewSource
import com.pioneer.nycfirewire.R
import com.pioneer.nycfirewire.databinding.ActivityStreetViewBinding
import com.pioneer.nycfirewire.utils.StreetViewProbe
import com.pioneer.nycfirewire.utils.gone
import com.pioneer.nycfirewire.utils.visible

/**
 * Full-screen 360° street-level view of an incident address, opened from the
 * "360° VIEW" pill on the incident page. The panorama (the billable Dynamic
 * Street View event) is created only here, i.e. only on a tap.
 *
 * Shows "No street view here" when Google has no outdoor panorama within
 * [StreetViewProbe.RADIUS_METERS] of the coordinate.
 */
class StreetViewActivity : BaseActivity(), OnStreetViewPanoramaReadyCallback {

    private lateinit var binding: ActivityStreetViewBinding
    private var target: LatLng? = null
    /** The camera is turned towards the incident once, on the first panorama. */
    private var aimed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStreetViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val lat = intent.getDoubleExtra(EXTRA_LATITUDE, Double.NaN)
        val lng = intent.getDoubleExtra(EXTRA_LONGITUDE, Double.NaN)
        if (!lat.isNaN() && !lng.isNaN()) target = LatLng(lat, lng)
        aimed = savedInstanceState?.getBoolean(STATE_AIMED) ?: false

        binding.tvCaption.text = intent.getStringExtra(EXTRA_ADDRESS)?.trim()
            ?.takeIf { it.isNotEmpty() } ?: getString(R.string.street_view_caption_fallback)
        binding.ivClose.setOnClickListener { finish() }

        // The view keeps its state in its own bundle, as MapView does.
        binding.streetViewPanorama.onCreate(savedInstanceState?.getBundle(STATE_PANORAMA))
        if (target == null) {
            showNoStreetView()
        } else {
            binding.streetViewPanorama.getStreetViewPanoramaAsync(this)
        }
    }

    override fun onStreetViewPanoramaReady(panorama: StreetViewPanorama) {
        val position = target ?: return
        panorama.setOnStreetViewPanoramaChangeListener { location ->
            if (isFinishing || isDestroyed) return@setOnStreetViewPanoramaChangeListener
            // null (or a location without links) is how the SDK reports
            // "no panorama near this position"
            if (location == null || location.links == null) {
                showNoStreetView()
            } else {
                binding.tvNoStreetView.gone()
                if (!aimed) {
                    aimed = true
                    aimAtIncident(panorama, location.position, position)
                }
            }
        }
        panorama.setPosition(position, StreetViewProbe.RADIUS_METERS, StreetViewSource.OUTDOOR)
    }

    /** Street View opens facing north; turn it to face the incident address. */
    private fun aimAtIncident(panorama: StreetViewPanorama, from: LatLng, to: LatLng) {
        val results = FloatArray(2)
        Location.distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude, results)
        if (results[0] < 2f) return // standing on the point: no meaningful direction
        val bearing = ((results[1] % 360f) + 360f) % 360f
        panorama.animateTo(
            StreetViewPanoramaCamera.Builder().zoom(0f).tilt(0f).bearing(bearing).build(),
            0L
        )
    }

    private fun showNoStreetView() {
        binding.tvNoStreetView.visible()
    }

    override fun onStart() {
        super.onStart()
        binding.streetViewPanorama.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.streetViewPanorama.onResume()
    }

    override fun onPause() {
        binding.streetViewPanorama.onPause()
        super.onPause()
    }

    override fun onStop() {
        binding.streetViewPanorama.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        binding.streetViewPanorama.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.streetViewPanorama.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val panoramaState = Bundle()
        binding.streetViewPanorama.onSaveInstanceState(panoramaState)
        outState.putBundle(STATE_PANORAMA, panoramaState)
        outState.putBoolean(STATE_AIMED, aimed)
    }

    companion object {
        private const val EXTRA_LATITUDE = "street_view_latitude"
        private const val EXTRA_LONGITUDE = "street_view_longitude"
        private const val EXTRA_ADDRESS = "street_view_address"
        private const val STATE_PANORAMA = "street_view_panorama_state"
        private const val STATE_AIMED = "street_view_aimed"

        fun intent(context: Context, latitude: Double, longitude: Double, address: String?): Intent =
            Intent(context, StreetViewActivity::class.java)
                .putExtra(EXTRA_LATITUDE, latitude)
                .putExtra(EXTRA_LONGITUDE, longitude)
                .putExtra(EXTRA_ADDRESS, address)
    }
}
