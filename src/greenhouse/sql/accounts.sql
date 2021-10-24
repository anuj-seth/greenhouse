-- :name insert-account :? :1
insert into bank.accounts (account_name)
values (:account-name)
       returning *;

-- :name open-balance :? :1
insert into bank.account_opening_balances (account_id, amount)
values (:account-id, :amount)
       returning *;

-- :name view-account-and-balance :? :1
with current_balance as (
  select aoc.account_id,
         aoc.amount + coalesce(sum(case when transaction_type = 'credit' then trx.amount
                                   else -trx.amount end), 0) as balance_amount
    from bank.account_opening_balances aoc
         left join bank.account_transactions trx
             on trx.account_id = aoc.account_id
             and trx.transaction_date > aoc.balance_date 
   where aoc.account_id = :account-id
         and aoc.latest = true
   group by aoc.account_id, aoc.amount)
select acc.account_name,
       cb.balance_amount
  from bank.accounts acc
       join current_balance cb
           on cb.account_id = acc.account_id
 where acc.account_id = :account-id;



-- insert into account_transactions (account_id, transaction_type, amount, transaction_date)
-- values (11, 'credit', 100, now() + '1 day'::interval);

-- insert into account_transactions (account_id, transaction_type, amount, transaction_date)
-- values (11, 'debit', 10, now() + '1 day'::interval);
-- insert into account_transactions (account_id, transaction_type, amount, transaction_date)
-- values (11, 'debit', 10, now() + '7 day'::interval);

-- insert into account_transactions (account_id, transaction_type, amount, transaction_date)
-- values (11, 'debit', 10, now() - '7 day'::interval);
