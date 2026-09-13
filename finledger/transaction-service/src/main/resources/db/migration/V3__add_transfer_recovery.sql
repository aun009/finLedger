alter table money_transfers add column last_failure text;
alter table money_transfers add column recovery_attempts integer not null default 0;

create unique index uq_outbox_events_topic_key on outbox_events (topic, event_key);
