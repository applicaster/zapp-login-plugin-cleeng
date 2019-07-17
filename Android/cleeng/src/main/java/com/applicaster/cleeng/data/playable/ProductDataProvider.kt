package com.applicaster.cleeng.data.playable

import com.applicaster.atom.model.APAtomEntry
import com.applicaster.model.APChannel
import com.applicaster.model.APModel

interface ProductDataProvider {
    fun getLegacyProviderIds(): List<String>?
    fun isAuthRequired(): Boolean
    fun getProductIds(): ArrayList<String>?

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
        return playable.getExtension(ProductDataProvider.KEY_LEGACY_AUTH_PROVIDER_IDS) as? ArrayList<String>
    }

    override fun isAuthRequired(): Boolean {
        return playable.getExtension(ProductDataProvider.KEY_REQUIRE_AUTH).toString().toBoolean()
    }

    override fun getProductIds(): ArrayList<String>? {
        return playable.getExtension(ProductDataProvider.KEY_DS_PRODUCT_ID) as? ArrayList<String>
    }
}

class ProductAPAtomEntryPlayableItem(private val playable: APAtomEntry.APAtomEntryPlayable) : ProductDataProvider {
    override fun getLegacyProviderIds(): List<String>? {
        return playable.entry?.getExtension(
            ProductDataProvider.KEY_LEGACY_AUTH_PROVIDER_IDS,
            List::class.java
        ) as? List<String>
    }

    override fun isAuthRequired(): Boolean {
        return playable.entry?.getExtension(ProductDataProvider.KEY_REQUIRE_AUTH, Boolean::class.java) ?: false
    }

    override fun getProductIds(): ArrayList<String>? {
        return playable.entry?.getExtension(
            ProductDataProvider.KEY_DS_PRODUCT_ID,
            List::class.java
        ) as? ArrayList<String>
    }
}

class ProductAPModelItem(private val apModel: APModel) : ProductDataProvider {
    override fun getLegacyProviderIds(): List<String>? {
        var providerIds: List<String>? = apModel.authorization_providers_ids.toList()
        if (providerIds == null || providerIds.isEmpty()) {
            val providersStr = (apModel.getExtension(ProductDataProvider.KEY_LEGACY_AUTH_PROVIDER_IDS) as? String)
            providerIds = providersStr?.split(",")?.map { it.trim() }
        }
        return providerIds
    }

    override fun isAuthRequired(): Boolean {
        return apModel.getExtension(ProductDataProvider.KEY_REQUIRE_AUTH).toString().toBoolean()
    }

    override fun getProductIds(): ArrayList<String>? {
        return apModel.getExtension(ProductDataProvider.KEY_DS_PRODUCT_ID) as? ArrayList<String>
    }
}

class ProductAPAtomEntryItem(private val playable: APAtomEntry) : ProductDataProvider {
    override fun getLegacyProviderIds(): List<String>? {
        return playable.getExtension(
            ProductDataProvider.KEY_LEGACY_AUTH_PROVIDER_IDS,
            List::class.java
        ) as? List<String>
    }

    override fun isAuthRequired(): Boolean {
        return playable.getExtension(ProductDataProvider.KEY_REQUIRE_AUTH, Boolean::class.java) ?: false
    }

    override fun getProductIds(): ArrayList<String>? {
        return playable.getExtension(
            ProductDataProvider.KEY_DS_PRODUCT_ID,
            List::class.java
        ) as? ArrayList<String>
    }
}
