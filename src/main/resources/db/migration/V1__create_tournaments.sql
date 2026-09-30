create table tournaments
(
    id                 uuid            primary key,
    slug               varchar(255)    not null,
    name               varchar(255)    not null,
    description        text,
    handicap_mode      varchar(20)     not null default 'NO_HANDICAP',
    participation_mode varchar(20)     not null default 'WALK_IN',
    participant_type   varchar(20)     not null default 'PLAYER',
    created_at         timestamptz     not null default current_timestamp,
    updated_at         timestamptz     not null default current_timestamp,

    constraint tournaments_slug_unique unique (slug),
    constraint tournaments_handicap_mode_check
        check (handicap_mode in ('NO_HANDICAP', 'RESET_EACH_SEASON', 'CARRY_FORWARD')),
    constraint tournaments_participation_mode_check
        check (participation_mode in ('WALK_IN', 'REGISTRATION')),
    constraint tournaments_participant_type_check
        check (participant_type in ('PLAYER', 'TEAM'))
);
