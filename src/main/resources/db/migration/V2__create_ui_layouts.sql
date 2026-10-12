create table ui_layouts
(
    id                 uuid         primary key,
    application        varchar(100) not null,
    resource           varchar(100) not null,
    operation          varchar(30)  not null,
    published_revision integer,

    constraint ui_layouts_key_unique unique (application, resource, operation)
);

create table ui_layout_revisions
(
    layout_id        uuid        not null references ui_layouts (id),
    revision         integer     not null check (revision > 0),
    contract_version integer     not null check (contract_version > 0),
    definition       jsonb       not null,

    primary key (layout_id, revision)
);

alter table ui_layouts
    add constraint ui_layouts_published_revision_fk
    foreign key (id, published_revision)
    references ui_layout_revisions (layout_id, revision);
