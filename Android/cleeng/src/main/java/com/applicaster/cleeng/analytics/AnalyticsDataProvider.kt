package com.applicaster.cleeng.analytics

import com.applicaster.app.CustomApplication
import com.applicaster.cam.IAnalyticsDataProvider
import com.applicaster.cam.PurchaseData
import com.applicaster.cam.Trigger
import com.applicaster.cleeng.Session

class AnalyticsDataProvider : IAnalyticsDataProvider {

    private val DEFAULT_ENTITY_TYPE = "App"
    private val DEFAULT_ENTITY_NAME =
            CustomApplication.getApplication().applicationContext.applicationInfo.nonLocalizedLabel.toString()

    override var entityType: String = DEFAULT_ENTITY_TYPE
        get() = field
        set(value) { field = value.capitalize() }

    override var entityName: String = DEFAULT_ENTITY_NAME
        get() = field
        set(value) { field = value }

    override var trigger: Trigger = Trigger.OTHER
        get() = field
        set(value) { field = value }

    override val isUserSubscribed: Boolean
        get() = Session.isAccessGranted()

    override var purchaseData: MutableList<PurchaseData> = mutableListOf()
        get() = field
        set(value) { field = value }

    fun dropAllData() {
        entityType = DEFAULT_ENTITY_TYPE
        entityName = DEFAULT_ENTITY_NAME
        trigger = Trigger.OTHER
        purchaseData = arrayListOf()
    }
}