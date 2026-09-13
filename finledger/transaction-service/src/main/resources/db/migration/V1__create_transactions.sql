create table money_transfers (
    id uuid primary key,
    from_account varchar(20) not null,
    to_account varchar(20) not null,
    amount numeric(19, 2) not null,
    idempotency_key varchar(100) not null unique,
    status varchar(20) not null,
    created_at timestamp with time zone not null
);

create table outbox_events (
    id uuid primary key,
    topic varchar(100) not null,
    event_key varchar(100) not null,
    payload text not null,
    created_at timestamp with time zone not null,
    published_at timestamp with time zone
);

create index idx_outbox_unpublished on outbox_events (created_at) where published_at is null;
