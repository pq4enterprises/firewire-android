package com.pioneer.nycfirewire.model.alertfilters

/**
 * Alert picker (iteration 1, item #2) — per-locality incident-type and unit
 * filters for push alerts.
 *
 * GET api/app/alert-filters/{localityId}
 *   -> { code, message, data: AlertFiltersData }
 * PUT api/app/alert-filters/{localityId}
 *   body AlertFiltersUpdateRequest; a null list means ALL (the default, stored
 *   as no rows on the server so new types/units are included automatically).
 *   -> { code: "updated", data: AlertFiltersUpdateResult }
 */
data class AlertFiltersResponse(
    val code: String? = "",
    val message: String? = "",
    val data: AlertFiltersData? = null
)

data class AlertFiltersData(
    val locality: AlertFiltersLocality? = null,
    val typesAll: Boolean = true,
    val unitsAll: Boolean = true,
    val types: List<AlertFilterOption>? = null,
    val units: List<AlertFilterOption>? = null
)

data class AlertFiltersLocality(
    val _id: String? = "",
    val name: String? = ""
)

data class AlertFilterOption(
    val _id: String? = "",
    val name: String? = "",
    val checked: Boolean = false
)

/** Gson drops null fields; the server treats an absent list the same as null (= all). */
data class AlertFiltersUpdateRequest(
    val typeIds: List<String>?,
    val unitIds: List<String>?
)

data class AlertFiltersUpdateResponse(
    val code: String? = "",
    val message: String? = "",
    val data: AlertFiltersUpdateResult? = null
)

data class AlertFiltersUpdateResult(
    val typesAll: Boolean = true,
    val unitsAll: Boolean = true,
    val typeCount: Int = 0,
    val unitCount: Int = 0
)
