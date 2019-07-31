package com.applicaster.cleeng

enum class AuthenticationRequirement {
    NEVER,
    ALWAYS,
    REQUIRE_ON_PURCHASABLE_ITEMS,
    REQUIRE_WHEN_SPECIFIED_IN_DATA_SOURCE,
    UNDEFINED;

    companion object {
        fun fromKey(key: String): AuthenticationRequirement {
            return when (key) {
                "never_require" -> NEVER
                "always_require" -> ALWAYS
                "require_on_all_purchasable_items" -> REQUIRE_ON_PURCHASABLE_ITEMS
                "require_when_specified_on_the_data_source" -> REQUIRE_WHEN_SPECIFIED_IN_DATA_SOURCE
                else -> UNDEFINED
            }
        }
    }
}