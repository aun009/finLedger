alter table money_transfers add column payment_reference varchar(20);
alter table money_transfers add column reconciliation_status varchar(12) not null default 'PENDING';

update money_transfers
set payment_reference = 'LEGACY' || substring(replace(id::text, '-', '') from 1 for 12)
where payment_reference is null;

alter table money_transfers alter column payment_reference set not null;
alter table money_transfers add constraint uq_money_transfers_payment_reference unique (payment_reference);
create index idx_money_transfers_initiator_created on money_transfers(initiated_by, created_at desc);
