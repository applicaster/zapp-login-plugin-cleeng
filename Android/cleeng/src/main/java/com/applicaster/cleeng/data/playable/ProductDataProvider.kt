package com.applicaster.cleeng.data.playable

import com.applicaster.atom.model.APAtomEntry
import com.applicaster.cam.analytics.AnalyticsUtil
import com.applicaster.model.APChannel
import com.applicaster.model.APModel

interface ProductDataProvider {
    fun getLegacyProviderIds(): List<String>?
    fun isAuthRequired(): Boolean
    fun getProductIds(): ArrayList<String>?
    fun getEntityType(): String
    fun getEntityName(): String

    fun <T> getSafety(function: () -> T): T? {
        return try {
            function()
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        fun fromPlayable(dataItem: Any?): ProductDataProvider? {
            return when (dataItem) {
                is APModel -> ProductAPModelItem(dataItem)
                is APAtomEntry.APAtomEntryPlayable -> ProductAPAtomEntryPlayableItem(dataItem)
                is APAtomEntry -> ProductAPAtomEntryItem(dataItem)
                is APChannel -> ProductAPChannelItem(dataItem)
                else -> null
            }
        }

        const val KEY_LEGACY_AUTH_PROVIDER_IDS = "authorization_providers_ids"
        const val KEY_REQUIRE_AUTH = "requires_authentication"
        const val KEY_DS_PRODUCT_ID = "ds_product_ids"
    }
}

class ProductAPChannelItem(private val playable: APChannel) : ProductDataProvider {
    override fun getLegacyProviderIds(): ArrayList<String>? {
        return getSafety { playable.getExtension(ProductDataProvider.KEY_LEGACY_AUTH_PROVIDER_IDS) as? ArrayList<String> }
    }

    override fun isAuthRequired(): Boolean {
        return getSafety { playable.getExtension(ProductDataProvider.KEY_REQUIRE_AUTH).toString().toBoolean() } ?: false
    }

    override fun getProductIds(): ArrayList<String>? {
        return getSafety { playable.getExtension(ProductDataProvider.KEY_DS_PRODUCT_ID) as? ArrayList<String> }
    }

    override fun getEntityType(): String {
       return getSafety { playable.playableType.name }.orEmpty()
    }

    override fun getEntityName(): String {
        return getSafety { playable.name }.orEmpty()
    }
}

class ProductAPAtomEntryPlayableItem(private val playable: APAtomEntry.APAtomEntryPlayable) : ProductDataProvider {
    override fun getLegacyProviderIds(): List<String>? {
        return getSafety { playable.entry?.getExtension(
            ProductDataProvider.KEY_LEGACY_AUTH_PROVIDER_IDS,
            List::class.java
        ) as? List<String> }
    }

    override fun isAuthRequired(): Boolean {
        return getSafety { playable.entry?.getExtension(ProductDataProvider.KEY_REQUIRE_AUTH, Boolean::class.java) } ?: false
    }

    override fun getProductIds(): ArrayList<String>? {
        return getSafety { playable.entry?.getExtension(
            ProductDataProvider.KEY_DS_PRODUCT_ID,
            List::class.java
        ) as? ArrayList<String> }
    }

    override fun getEntityType(): String {
        return getSafety { playable.playableType.name }.orEmpty()
    }

    override fun getEntityName(): String {
        return getSafety { playable.playableName }.orEmpty()
    }
}

class ProductAPModelItem(private val apModel: APModel) : ProductDataProvider {
    override fun getLegacyProviderIds(): List<String>? {
        var providerIds: List<String>? = apModel.authorization_providers_ids.toList()
        if (providerIds == null || providerIds.isEmpty()) {
            val providersStr = getSafety { (apModel.getExtension(ProductDataProvider.KEY_LEGACY_AUTH_PROVIDER_IDS) as? String) }
            providerIds = providersStr?.split(",")?.map { it.trim() }
        }
        return providerIds
    }

    override fun isAuthRequired(): Boolean {
        return getSafety { apModel.getExtension(ProductDataProvider.KEY_REQUIRE_AUTH).toString().toBoolean() } ?: false
    }

    override fun getProductIds(): ArrayList<String>? {
        return getSafety { apModel.getExtension(ProductDataProvider.KEY_DS_PRODUCT_ID) as? ArrayList<String> }
    }

    override fun getEntityType(): String {
        return AnalyticsUtil.KEY_NONE_PROVIDED
    }

    override fun getEntityName(): String {
        return getSafety { apModel.name }.orEmpty()
    }
}

class ProductAPAtomEntryItem(private val playable: APAtomEntry) : ProductDataProvider {
    override fun getLegacyProviderIds(): List<String>? {
        val data = getSafety { playable.getExtension(
                ProductDataProvider.KEY_LEGACY_AUTH_PROVIDER_IDS,
                List::class.java
        ) as? List<Double> }
        return data?.map {
            it.toInt().toString()
        }
    }

    override fun isAuthRequired(): Boolean {
        return getSafety { playable.getExtension(ProductDataProvider.KEY_REQUIRE_AUTH, Boolean::class.java) } ?: false
    }

    override fun getProductIds(): ArrayList<String>? {
        return getSafety { playable.getExtension(
            ProductDataProvider.KEY_DS_PRODUCT_ID,
            List::class.java
        ) as? ArrayList<String> }
    }

    override fun getEntityType(): String {
        return getSafety { playable.type.toString() }.orEmpty()
    }

    override fun getEntityName(): String {
        return getSafety { playable.playable.playableName }.orEmpty()
    }
}
