create table seasons
(
    id              uuid            primary key,
    tournament_id   uuid            not null,
    name            varchar(255)    not null,
    slug            varchar(255)    not null,
    from_year       integer         not null,

    constraint seasons_tournament_fk
        foreign key (tournament_id) references tournaments (id),
    constraint seasons_tournament_name_unique
        unique (tournament_id, name),
    constraint seasons_tournament_slug_unique
        unique (tournament_id, slug)
);

create table seasons_audit
(
    id              uuid            not null,
    rev             integer         not null,
    revtype         smallint,
    tournament_id   uuid,
    name            varchar(255),
    slug            varchar(255),
    from_year       integer,

    primary key (id, rev),

    constraint seasons_aud_revision_fk
        foreign key (rev) references audit_revisions (id)
);
