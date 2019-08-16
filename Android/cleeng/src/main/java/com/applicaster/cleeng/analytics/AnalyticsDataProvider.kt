package com.applicaster.cleeng.analytics

import com.applicaster.cam.IAnalyticsDataProvider
import com.applicaster.cam.PurchaseData
import com.applicaster.cam.Trigger
import com.applicaster.cleeng.Session

class AnalyticsDataProvider : IAnalyticsDataProvider {

    override var entityType: String = ""
        get() = field
        set(value) { field = value }

    override var entityName: String = ""
        get() = field
        set(value) { field = value }

    override var trigger: Trigger = Trigger.OTHER
        get() = field
        set(value) { field = value }

    override val isUserSubscribed: Boolean
        get() = Session.isAccessGranted()

    override var purchaseData: MutableList<PurchaseData> = arrayListOf()
        get() = field
        set(value) { field = value }
}