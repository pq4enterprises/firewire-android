package com.fire.wire.utils

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.fire.wire.R
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Draws the approved "Classic Pin" firehouse marker entirely in code:
 * a fw_red teardrop pin with a 2.5dp white border and a white firehouse
 * glyph (house silhouette whose lower half is a garage door), composed
 * into a single bitmap with the firehouse name underneath in the
 * design-system chip typography (poppins_bold, uppercase, letter-spaced)
 * with a halo stroke for map legibility.
 *
 * Android twin of iOS `FirehouseMarker` (MapManager.swift) — keep the
 * geometry and label treatment in sync with that implementation.
 */
object FirehouseMarker {

    // Metrics (dp) — parity with the iOS point values.

    /** Head-circle radius of the teardrop. */
    private const val HEAD_RADIUS = 11.5f
    /** White border stroke width. */
    private const val BORDER_WIDTH = 2.5f
    /** Vertical gap between the pin tip and the label. */
    private const val LABEL_GAP = 3f
    /** Extra transparent padding around the composition (keeps the halo
     *  stroke and the pin border from clipping at the bitmap edge). */
    private const val PADDING = 2f
    /** Label font size (dp, matches iOS 11pt) and kerning (iOS kern 0.8). */
    private const val FONT_SIZE = 11f
    private const val KERN = 0.8f

    /** Pin bounding width/height in dp (excluding label). ~26 x 34dp. */
    private val PIN_WIDTH = HEAD_RADIUS * 2 + BORDER_WIDTH
    private val PIN_HEIGHT = HEAD_RADIUS * 2.5f + BORDER_WIDTH + 3f

    /**
     * The composed marker image plus the anchor that places the pin tip
     * (not the bitmap bottom — the label hangs below the tip) on the
     * marker coordinate.
     */
    data class ComposedIcon(
        val descriptor: BitmapDescriptor,
        val anchorU: Float,
        val anchorV: Float
    )

    /** Renders the pin + label into one marker bitmap. */
    fun composedIcon(context: Context, label: String): ComposedIcon {
        val density = context.resources.displayMetrics.density
        val name = label.trim().uppercase()

        // Design-system label treatment: same as unit chips / action labels.
        val textColor = ContextCompat.getColor(context, R.color.fw_text)
        // Halo inverts with the text so the label reads on any map imagery:
        // white halo around dark text (light mode), dark halo around the
        // near-white night-mode text.
        val isNight = (context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val haloColor = if (isNight) 0xFF15161B.toInt() else Color.WHITE

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = ResourcesCompat.getFont(context, R.font.poppins_bold)
            textSize = FONT_SIZE * density
            letterSpacing = KERN / FONT_SIZE // Paint spacing is in ems
        }
        val fontMetrics = textPaint.fontMetrics
        val labelWidth: Float
        val labelHeight: Float
        if (name.isEmpty()) {
            labelWidth = 0f
            labelHeight = 0f
        } else {
            labelWidth = ceil(textPaint.measureText(name))
            labelHeight = ceil(fontMetrics.descent - fontMetrics.ascent)
        }

        val padding = PADDING * density
        val imageWidth = max(PIN_WIDTH * density, labelWidth) + padding * 2
        val pinTipY = padding + PIN_HEIGHT * density
        val imageHeight = pinTipY +
                (if (name.isEmpty()) 0f else LABEL_GAP * density + labelHeight) + padding

        val bitmap = Bitmap.createBitmap(
            ceil(imageWidth).toInt(), ceil(imageHeight).toInt(), Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        val cx = imageWidth / 2

        drawPin(canvas, context, cx, padding, density)

        if (name.isNotEmpty()) {
            val labelX = cx - labelWidth / 2
            val baselineY = pinTipY + LABEL_GAP * density - fontMetrics.ascent
            // Halo pass: stroke-only draw underneath …
            textPaint.style = Paint.Style.STROKE
            textPaint.strokeWidth = textPaint.textSize * 0.09f // subtle, ~1dp
            textPaint.strokeJoin = Paint.Join.ROUND
            textPaint.color = haloColor
            canvas.drawText(name, labelX, baselineY, textPaint)
            // … then the fill on top.
            textPaint.style = Paint.Style.FILL
            textPaint.color = textColor
            canvas.drawText(name, labelX, baselineY, textPaint)
        }

        // Anchor the pin tip — not the label — to the coordinate.
        return ComposedIcon(
            descriptor = BitmapDescriptorFactory.fromBitmap(bitmap),
            anchorU = 0.5f,
            anchorV = pinTipY / imageHeight
        )
    }

    // Pin drawing

    private fun drawPin(canvas: Canvas, context: Context, cx: Float, topY: Float, density: Float) {
        val r = HEAD_RADIUS * density
        val borderWidth = BORDER_WIDTH * density
        val inset = borderWidth / 2 // stroke is centered on the path
        val headCx = cx
        val headCy = topY + inset + r
        val tipY = topY + PIN_HEIGHT * density - inset

        // Teardrop: arc over the head, tapering curves down to the tip.
        // (Android canvas: y down, arc angles clockwise from +x — same
        // convention as the UIKit path this mirrors.)
        val startAngle = Math.toRadians(150.0) // lower-left of head
        val endAngle = Math.toRadians(30.0)    // lower-right of head
        val startX = headCx + r * cos(startAngle).toFloat()
        val startY = headCy + r * sin(startAngle).toFloat()
        val endX = headCx + r * cos(endAngle).toFloat()

        val pin = Path()
        // Arc from 150° sweeping 240° over the top of the head to 30°.
        pin.arcTo(RectF(headCx - r, headCy - r, headCx + r, headCy + r), 150f, 240f)
        pin.quadTo(endX - r * 0.18f, headCy + r * 1.35f, headCx, tipY)
        pin.quadTo(startX + r * 0.18f, headCy + r * 1.35f, startX, startY)
        pin.close()

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = ContextCompat.getColor(context, R.color.fw_red)
        }
        canvas.drawPath(pin, fillPaint)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.WHITE
            strokeWidth = borderWidth
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(pin, strokePaint)

        drawFirehouseGlyph(canvas, context, headCx, headCy, density)
    }

    /**
     * White house silhouette whose lower half is a garage door — a red
     * cutout crossed by two thin white horizontal slat lines.
     */
    private fun drawFirehouseGlyph(canvas: Canvas, context: Context, cx: Float, cy: Float, density: Float) {
        // Glyph box ~13 x 12dp centered in the pin head.
        val apexY = cy - 6f * density
        val eaveY = cy - 0.8f * density
        val eaveHalf = 6.4f * density
        val wallHalf = 4.9f * density
        val baseY = cy + 6f * density

        val house = Path().apply {
            moveTo(cx, apexY)
            lineTo(cx + eaveHalf, eaveY)
            lineTo(cx + wallHalf, eaveY)
            lineTo(cx + wallHalf, baseY)
            lineTo(cx - wallHalf, baseY)
            lineTo(cx - wallHalf, eaveY)
            lineTo(cx - eaveHalf, eaveY)
            close()
        }
        val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }
        canvas.drawPath(house, whitePaint)

        // Garage door: red cutout in the lower half of the body.
        val doorHalf = 3.3f * density
        val doorTop = cy + 1.2f * density
        val redPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = ContextCompat.getColor(context, R.color.fw_red)
        }
        canvas.drawRect(cx - doorHalf, doorTop, cx + doorHalf, baseY, redPaint)

        // Two thin white slat lines across the door.
        for (lineOffset in floatArrayOf(1.4f, 3.0f)) {
            val lineY = doorTop + lineOffset * density
            canvas.drawRect(
                cx - doorHalf, lineY,
                cx + doorHalf, lineY + 0.6f * density,
                whitePaint
            )
        }
    }
}
