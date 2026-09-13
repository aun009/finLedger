alter table accounts add column owner_username varchar(80);

update accounts set owner_username = lower(trim(owner_name)) where owner_username is null;

alter table accounts alter column owner_username set not null;
