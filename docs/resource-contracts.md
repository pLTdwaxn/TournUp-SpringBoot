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

The existing URLs, DTOs, response envelopes, errors, reference selectors, layouts,
and audit visibility are retained. Existing nested season URLs remain supported.
The proposed slug-plus-short-ID locator is a separate change and is not introduced
by this structural refactor.

## Adding a resource

Add its entity/migration, repository, separate read/write DTOs, transactional domain
operations, and a concrete controller that composes the delegate. Supply its manifest
bean and mapping/permission configuration. No shared dispatch infrastructure changes
are required. Specialised domain operations remain ordinary service/controller methods.

## Remaining follow-ups

- Version the UI catalogue if create/update contracts need separate catalogue entries;
  form manifests already use the correct operation contract.
- Generalise domain error translation when additional resources actually need it.
