package dev.kiritoxd.miaopu.data

import android.content.Context

class EsportSubscriptionStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun orderedSubscriptions(): List<Esport> = EsportCatalog.orderedSubscriptions(
        preferences.getStringSet(KEY_SUBSCRIPTIONS, null),
        preferences.getString(KEY_ORDER, null)?.split(',').orEmpty(),
    )

    fun subscriptions(): Set<Esport> = orderedSubscriptions().toCollection(linkedSetOf())

    fun selected(subscriptions: Collection<Esport>): Esport {
        val stored = EsportCatalog.byBusinessId(preferences.getString(KEY_SELECTED, null))
        return stored?.takeIf { it in subscriptions } ?: subscriptions.first()
    }

    fun saveSubscriptions(subscriptions: Collection<Esport>) {
        preferences.edit()
            .putStringSet(KEY_SUBSCRIPTIONS, subscriptions.mapTo(linkedSetOf()) { it.businessId })
            .putString(KEY_ORDER, subscriptions.joinToString(",") { it.businessId })
            .apply()
    }

    fun saveSelected(esport: Esport) {
        preferences.edit().putString(KEY_SELECTED, esport.businessId).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "miaopu_esport_subscriptions"
        const val KEY_SUBSCRIPTIONS = "business_ids"
        const val KEY_ORDER = "business_id_order"
        const val KEY_SELECTED = "selected_business_id"
    }
}
