package com.applicaster.cleeng.analytics

import com.applicaster.cam.IAnalyticsDataProvider
import com.applicaster.cam.PurchaseData
import com.applicaster.cam.Trigger
import com.applicaster.cleeng.Session

class AnalyticsDataProvider : IAnalyticsDataProvider {

    override fun getEntityType(): String  = ""

    override fun getEntityName(): String = ""

    override fun getTrigger(): Trigger {
        return Trigger.OTHER //TODO("Some mock stuff")
    }

    override fun isUserSubscribed(): Boolean = Session.isAccessGranted()

    override fun getPurchaseData(): List<PurchaseData> {
        return listOf()
    }
}