create table balance_operations (
    id uuid primary key,
    operation_key varchar(100) not null unique,
    account_number varchar(20) not null,
    operation_type varchar(10) not null,
    amount numeric(19, 2) not null,
    created_at timestamp with time zone not null
);
