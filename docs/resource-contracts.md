# CRUD composition and resource contracts

Concrete season and tournament controllers own explicit Spring MVC routes and typed
write DTOs. Each composes `CrudDelegate` with ordinary callbacks to its transactional
service. There is no generic route controller, routing registry, handler superclass,
or per-resource handler subclass.

Controllers use Spring MVC validation; services validate direct calls. The delegate supplies shared record/list/form response orchestration,
404 handling, creation status/Location handling, and audit enrichment through the
existing response composer. Persistence callbacks run in the existing service
transactions; the manually composed delegate does not depend on Spring transaction
proxying. Domain rules and mapping stay explicit in the services.

## Contracts and manifests

- Write DTOs own Bean Validation constraints and write policies. The managed metadata
  reader uses the same application Validator as runtime validation.
- DTO policy/default helpers live with validation metadata. They do not register routes.
- Each resource has one manifest file combining its presentation, views, request
  contracts, access predicates, and UI registration bean. No central registration file
  must be edited when another resource supplies its own manifest bean.
- `UiResourceRegistry` validates catalogue identities and UI references. It is the only
  resource catalogue, and is not a dynamic HTTP dispatcher.
- The manifest resolver projects validation hints and applies published layouts.
  The response composer builds envelopes and reads audit data after writes commit.

Manifest `required` describes a value after binding/defaults, rather than JSON
property presence. Create defaults come from the request DTO factory. Read/list views
continue to omit form constraints. Client suggestions remain independent from server
validation, and constraints without a client translation still run on the server.

## Authorization and API compatibility

Spring Security enforces authentication and CSRF. `ResourceAccess` on concrete routes
selects the catalogue policy; its interceptor authorizes before argument conversion
or JSON parsing. Standard CRUD and specialised endpoints use the same policy source.

Response envelopes, errors, reference selectors, layouts, and audit visibility
are retained. Existing nested season routes remain supported with public locators.
Public resource IDs are now `slug-nanoID` locators. Their opaque four-character
suffix from `23456789BCDFGHJKLMNPQRSTVWXYZ` is stored independently from the internal UUIDv7 key. Lookups use the
suffix, so an old readable slug remains resolvable after a rename. UUID and bare-slug
resource URLs have no legacy UUID or slug lookup fallback. Parent references, lookup results and generated
links use public locators; internal audit keys are never serialized. Lookup cursor
version 2 contains public NanoIDs rather than UUIDs. Season slugs may repeat
within a tournament; season names remain unique within their tournament. Migration
V6 removes the former season slug uniqueness constraint.

Create forms return an unreserved four-character NanoID in `item.nanoId`.
The frontend previews `slug-nanoId` and submits that same `nanoId`. Abandoned forms
need no release. A database uniqueness collision fails with HTTP 409 and an
`errors.nanoId` message; the server never retries or substitutes another ID. Reopen
the create form to obtain a new candidate. Direct create requests may omit `nanoId`,
in which case the server generates one once. NanoIDs are immutable after creation.
Uniqueness is enforced in each resource table of the tenant database.

The resource catalogue declares the locator components:

```json
{"field":"id","type":"slug-nano-id","slugField":"slug","nanoIdField":"nanoId","slugContext":[]}
```

For create forms, read the values named by `slugField` and `nanoIdField` from `item`.
Preview `${item.slug}-${item.nanoId}` as the slug changes. `nanoId` is transport data,
not an editable field in `manifest.fields` or the layout. Preserve it when building
the POST payload, even when selecting editable fields from the manifest. For example:

```json
{"name":"Winter league","slug":"winter-league","fromYear":2026,"tournamentId":"winter-open-BCDF","nanoId":"GHJK"}
```

After creation, use the returned `item.id` for links rather than the preview.
Generation, request validation, locator parsing and lookup cursors all use the same
four-character alphabet. The frontend does not generate or edit NanoIDs.

Migration V5 deliberately drops and recreates tournaments, seasons, and their two
audit tables, as requested. It discards their records and history. Shared audit
revisions, UI layouts and the tenant catalogue are retained. V5 retains its original
12-character `short_id` schema and checksum for databases that already applied it.
V7 clears the resource tables again and replaces those columns with four-character
`nano_id` columns. Apply the pending migrations by starting with the `dev` profile;
no Flyway repair is needed for the original V5 checksum (`-919477706`).

In `dev`, tenant migration startup restores runtime CRUD grants on application tables
and sequence access after migrations, excluding the Flyway history table. It also
sets defaults for tables and sequences subsequently created by the migration role,
so recreating tables does not leave tenant runtime roles without access.

## Adding a resource

Add its entity/migration, repository, separate read/write DTOs, transactional domain
operations, and a concrete controller that composes the delegate. Supply its manifest
bean and mapping/permission configuration. No shared dispatch infrastructure changes
are required. Specialised domain operations remain ordinary service/controller methods.

## Remaining follow-ups

- Version the UI catalogue if create/update contracts need separate catalogue entries;
  form manifests already use the correct operation contract.
- Generalise domain error translation when additional resources actually need it.
