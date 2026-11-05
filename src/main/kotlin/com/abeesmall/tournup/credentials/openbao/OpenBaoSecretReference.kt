package com.abeesmall.tournup.credentials.openbao


class OpenBaoSecretReference private constructor(
    val mount: String,
    val secretPath: String,
) {
    val apiPath: String
        get() = "/v1/$mount/data/$secretPath"

    companion object {
        private val segmentPattern = Regex("[A-Za-z0-9_-]+")

        fun parse(reference: String): OpenBaoSecretReference {
            require(reference.startsWith("openbao:")) {
                "Unsupported database credential reference"
            }

            val segments = reference.removePrefix("openbao:").split('/')

            require(
                segments.size >= 2 &&
                        segments.all { segmentPattern.matches(it) },
            ) {
                "Invalid OpenBao credential reference"
            }

            return OpenBaoSecretReference(
                mount = segments.first(),
                secretPath = segments.drop(1).joinToString("/"),
            )
        }
    }
}
