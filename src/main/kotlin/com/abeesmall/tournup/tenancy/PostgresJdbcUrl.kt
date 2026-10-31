package com.abeesmall.tournup.tenancy

import com.abeesmall.tournup.catalog.TenantDatabaseBinding
import java.net.URI

internal fun TenantDatabaseBinding.toPostgresJdbcUrl(): String =
    "jdbc:" + URI(
        "postgresql",
        null,
        databaseHost,
        databasePort,
        "/$databaseName",
        null,
        null,
    ).toASCIIString()
