CREATE TABLE tenants (
    id UUID PRIMARY KEY,
    slug VARCHAR(63) NOT NULL UNIQUE,
    name TEXT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PROVISIONING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT tenants_slug_format CHECK (
        slug ~ '^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?$'
    ),
    CONSTRAINT tenants_status_valid CHECK (
        status IN (
            'PROVISIONING', 'ACTIVE', 'SUSPENDED', 'DELETING', 'FAILED'
        )
    )
);

CREATE TABLE tenant_domains (
    hostname VARCHAR(253) PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),

    CONSTRAINT tenant_domains_hostname_lowercase CHECK (
        hostname = lower(hostname)
    )
);

CREATE INDEX tenant_domains_tenant_id_idx
    ON tenant_domains (tenant_id);

CREATE TABLE tenant_database_bindings (
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    service_name VARCHAR(63) NOT NULL,
    database_host TEXT NOT NULL,
    database_port INTEGER NOT NULL DEFAULT 5432,
    database_name TEXT NOT NULL,
    database_username TEXT NOT NULL,
    credential_reference TEXT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PROVISIONING',
    configuration_version BIGINT NOT NULL DEFAULT 1,

    PRIMARY KEY (tenant_id, service_name),

    CONSTRAINT tenant_database_bindings_port_valid CHECK (
        database_port BETWEEN 1 AND 65535
    ),
    CONSTRAINT tenant_database_bindings_status_valid CHECK (
        status IN ('PROVISIONING', 'ACTIVE', 'SUSPENDED', 'FAILED')
    ),
    CONSTRAINT tenant_database_bindings_version_valid CHECK (
        configuration_version > 0
    )
);
