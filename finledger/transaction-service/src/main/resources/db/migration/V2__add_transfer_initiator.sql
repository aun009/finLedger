alter table money_transfers add column initiated_by varchar(80);

update money_transfers set initiated_by = 'legacy' where initiated_by is null;

alter table money_transfers alter column initiated_by set not null;
