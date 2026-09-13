-- Older transfers predate reconciliation_status. Derive their final outcome from
-- the already persisted transfer lifecycle instead of showing them as pending.
update money_transfers
set reconciliation_status = case status
    when 'COMPLETED' then 'SETTLED'
    when 'COMPENSATED' then 'REVERSED'
    when 'FAILED' then 'FAILED'
    else 'PENDING'
end;
