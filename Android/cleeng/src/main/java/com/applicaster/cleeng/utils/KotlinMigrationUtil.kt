package com.applicaster.cleeng.utils

fun <T> Collection<T>?.isNullOrEmpty(): Boolean {
    return this == null || this.isEmpty()
}

fun <K, V> Map<out K, V>?.isNullOrEmpty(): Boolean {
    return this == null || isEmpty()
}

fun CharSequence?.isNullOrEmpty(): Boolean {
    return this == null || this.isEmpty()
}
