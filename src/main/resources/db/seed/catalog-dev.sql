BEGIN;

DO $$
BEGIN
    IF current_database() <> 'tournup_catalog_dev' THEN
        RAISE EXCEPTION 'Run this seed against tournup_catalog_dev';
    END IF;
END;
$$;

INSERT INTO public.tenants (id, slug, name, status)
VALUES
    (
        '00000000-0000-0000-0000-000000000001',
        'abeesmall', 'ABeeSmall', 'ACTIVE'
    ),
    (
        '00000000-0000-0000-0000-000000000002',
        'demo-club-1', 'Demo Club 1', 'ACTIVE'
    ),
    (
        '00000000-0000-0000-0000-000000000003',
        'demo-club-2', 'Demo Club 2', 'ACTIVE'
    )
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.tenant_database_bindings (
    tenant_id,
    service_name,
    database_host,
    database_port,
    database_name,
    database_username,
    credential_reference,
    status
)
VALUES
    (
        '00000000-0000-0000-0000-000000000001',
        'tournup-api',
        'localhost', 5432, 'abeesmall_tournup',
        current_user, 'env:DB_PASSWORD', 'ACTIVE'
    ),
    (
        '00000000-0000-0000-0000-000000000002',
        'tournup-api',
        'localhost', 5432, 'demo_club_1_tournup',
        'demo_club_1_app', 'env:DEMO_TENANT_1_DB_PASSWORD', 'ACTIVE'
    ),
    (
        '00000000-0000-0000-0000-000000000003',
        'tournup-api',
        'localhost', 5432, 'demo_club_2_tournup',
        'demo_club_2_app', 'env:DEMO_TENANT_2_DB_PASSWORD', 'ACTIVE'
    )
ON CONFLICT (tenant_id, service_name) DO NOTHING;

COMMIT;
