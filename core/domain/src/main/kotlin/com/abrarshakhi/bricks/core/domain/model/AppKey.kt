package com.abrarshakhi.bricks.core.domain.model

data class AppKey(
    val packageName: String,
    val className: String,
    val userSerial: Long,
) {
    val flattened: String
        get() = "$packageName/$className#$userSerial"

    fun samePackage(other: PackageUserKey): Boolean =
        packageName == other.packageName && userSerial == other.userSerial

    fun toPackageUserKey(): PackageUserKey = PackageUserKey(packageName, userSerial)

    companion object {
        fun parse(value: String): AppKey? {
            val slash = value.indexOf('/')
            val hash = value.lastIndexOf('#')
            if (slash <= 0 || hash <= slash + 1 || hash == value.lastIndex) return null
            val serial = value.substring(hash + 1).toLongOrNull() ?: return null
            return AppKey(
                packageName = value.substring(0, slash),
                className = value.substring(slash + 1, hash),
                userSerial = serial,
            )
        }
    }
}

data class PackageUserKey(
    val packageName: String,
    val userSerial: Long,
)
