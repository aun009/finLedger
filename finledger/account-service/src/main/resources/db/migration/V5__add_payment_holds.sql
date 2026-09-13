alter table accounts add column held_balance numeric(19, 2) not null default 0;

create table balance_holds (
    id uuid primary key,
    hold_key varchar(100) not null unique,
    account_number varchar(20) not null,
    amount numeric(19, 2) not null,
    status varchar(12) not null,
    created_at timestamp with time zone not null,
    resolved_at timestamp with time zone
);

create index idx_balance_holds_account_status on balance_holds(account_number, status);
