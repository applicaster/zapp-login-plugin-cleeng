package com.applicaster.cleeng.data.playable

import com.applicaster.atom.model.APAtomEntry
import com.applicaster.model.APChannel
import com.applicaster.plugin_manager.playersmanager.Playable

interface ProductDataProvider {
    fun getLegacyProviderIds(): ArrayList<String>?
    fun isAuthRequired(): Boolean
    fun getProductIds(): ArrayList<String>?

    companion object {
        fun fromPlayable(playable: Playable?): ProductDataProvider? {
            return when (playable) {
                is APAtomEntry.APAtomEntryPlayable -> ProductAPAtomEntryItem(playable)
                is APChannel -> ProductAPChannelItem(playable)
                else -> null
            }
        }

        const val KEY_LEGACY_AUTH_PROVIDER_IDS = "auth_provider_ids"
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

class ProductAPAtomEntryItem(private val playable: APAtomEntry.APAtomEntryPlayable) : ProductDataProvider {
    override fun getLegacyProviderIds(): ArrayList<String>? {
        return playable.entry?.getExtension(
            ProductDataProvider.KEY_LEGACY_AUTH_PROVIDER_IDS,
            List::class.java
        ) as? ArrayList<String>
    }

    override fun isAuthRequired(): Boolean {
        return playable.entry?.getExtension(ProductDataProvider.KEY_REQUIRE_AUTH, Boolean::class.java) ?: false
    }

    override fun getProductIds(): ArrayList<String>? {
        return playable.entry?.getExtension(ProductDataProvider.KEY_DS_PRODUCT_ID, List::class.java) as? ArrayList<String>
    }
}
