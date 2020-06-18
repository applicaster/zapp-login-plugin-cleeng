package com.applicaster.cleeng.data.playable

import com.applicaster.atom.model.APAtomEntry
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
                is Map<*, *> -> ProductMapDataItem(dataItem)
                else -> null
            }
        }

        const val KEY_LEGACY_AUTH_PROVIDER_IDS = "authorization_providers_ids"
        const val KEY_REQUIRE_AUTH = "requires_authentication"
        const val KEY_DS_PRODUCT_ID = "ds_product_ids"
        const val KEY_EXTENSIONS = "extensions"
        const val KEY_TYPE = "type"
        const val KEY_TITLE = "title"
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
        val defaultValue = "None provided"
        return defaultValue
    }

    override fun getEntityName(): String {
        return getSafety { apModel.name }.orEmpty()
    }
}

class ProductAPAtomEntryItem(private val playable: APAtomEntry) : ProductDataProvider {
    override fun getLegacyProviderIds(): List<String>? {
        val data = getSafety {
            playable.getExtension(
                ProductDataProvider.KEY_LEGACY_AUTH_PROVIDER_IDS,
                List::class.java
            ) as? List<Double>
        }
        return data?.map {
            it.toInt().toString()
        }
    }

    override fun isAuthRequired(): Boolean {
        return getSafety {
            playable.getExtension(
                ProductDataProvider.KEY_REQUIRE_AUTH,
                java.lang.Boolean::class.java
            ) as Boolean
        } ?: false
    }

    override fun getProductIds(): ArrayList<String>? {
        return getSafety {
            playable.getExtension(
                ProductDataProvider.KEY_DS_PRODUCT_ID,
                List::class.java
            ) as? ArrayList<String>
        }
    }

    override fun getEntityType(): String {
        return getSafety { playable.type.toString() }.orEmpty()
    }

    override fun getEntityName(): String {
        return getSafety { playable.playable.playableName }.orEmpty()
    }
}

class ProductMapDataItem(private val dataSource: Map<*, *>) : ProductDataProvider {
    override fun getLegacyProviderIds(): List<String>? {
        return getSafety {
            val extensions: Map<String, Any>? =
                    dataSource[ProductDataProvider.KEY_EXTENSIONS] as? Map<String, Any>?
            val ids = extensions?.get(ProductDataProvider.KEY_LEGACY_AUTH_PROVIDER_IDS) as? List<Double>
            ids?.map { it.toInt().toString() }
        }
    }

    override fun isAuthRequired(): Boolean {
        return getSafety {
            val extensions: Map<String, Any>? =
                    dataSource[ProductDataProvider.KEY_EXTENSIONS] as? Map<String, Any>?
            extensions?.get(ProductDataProvider.KEY_REQUIRE_AUTH) as? Boolean
        } ?: false
    }

    override fun getProductIds(): ArrayList<String>? {
        return getSafety {
            val extensions: Map<String, Any>? =
                    dataSource[ProductDataProvider.KEY_EXTENSIONS] as? Map<String, Any>?
            extensions?.filterKeys {
                it == ProductDataProvider.KEY_DS_PRODUCT_ID
            }?.values as? ArrayList<String>
        }
    }

    override fun getEntityType(): String {
        return getSafety {
            val typeStr = dataSource[ProductDataProvider.KEY_TYPE] as? String
            APAtomEntry.Type.fromStr(typeStr).name
        }.orEmpty()
    }

    override fun getEntityName(): String {
        return getSafety {
            dataSource[ProductDataProvider.KEY_TITLE] as? String
        }.orEmpty()
    }
}
