-- Resource identity reset: existing tournament/season records and their history are discarded.
-- Keep shared audit revisions, UI layouts and the tenant catalogue.
DROP TABLE seasons_audit;
DROP TABLE seasons;
DROP TABLE tournaments_audit;
DROP TABLE tournaments;

create table tournaments
(
    id                 uuid            primary key,
    short_id           varchar(12)     not null unique,
    slug               varchar(255)    not null,
    name               varchar(255)    not null,
    description        text,
    handicap_mode      varchar(20)     not null default 'NO_HANDICAP',
    participation_mode varchar(20)     not null default 'WALK_IN',
    participant_type   varchar(20)     not null default 'PLAYER',

    constraint tournaments_slug_unique unique (slug),
    constraint tournaments_handicap_mode_check
        check (handicap_mode in ('NO_HANDICAP', 'RESET_EACH_SEASON', 'CARRY_FORWARD')),
    constraint tournaments_participation_mode_check
        check (participation_mode in ('WALK_IN', 'REGISTRATION')),
    constraint tournaments_participant_type_check
        check (participant_type in ('PLAYER', 'TEAM'))
);

create table tournaments_audit
(
    id                  uuid                not null,
    short_id            varchar(12),
    rev                 integer             not null,
    revtype             smallint,
    slug                varchar(255),
    name                varchar(255),
    description         text,
    handicap_mode       varchar(20),
    participation_mode  varchar(20),
    participant_type    varchar(20),

    primary key (id, rev),

    constraint tournaments_aud_revision_fk
        foreign key (rev) references audit_revisions (id)
);

create table seasons
(
    id              uuid            primary key,
    short_id        varchar(12)     not null unique,
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
    short_id        varchar(12),
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

