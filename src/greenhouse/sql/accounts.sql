-- :name insert-account :<! :1
insert into bank.accounts (account_name)
values (:account-name)
       returning *;

-- :name open-balance :<! :1
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
select acc.account_id,
       acc.account_name,
       cb.balance_amount
  from bank.accounts acc
       join current_balance cb
           on cb.account_id = acc.account_id
 where acc.account_id = :account-id;

-- :name insert-transaction :<! :1
insert into bank.account_transactions (account_id, transaction_type, amount)
values (:account-id, :transaction-type, :amount)
       returning transaction_id;

-- :name select-and-maybe-lock-account :? :1
select 1
  from bank.accounts
 where account_id = :account-id
  -- ~ (if (:lock params) "for update")
;

-- :name insert-fund-transfer :!
insert into bank.fund_transfers (debit_transaction_id, credit_transaction_id, from_account, to_account)
values (:debit-transaction-id, :credit-transaction-id, :sender-id, :receiver-id);


-- :name select-transactions :? :*
with credits as (
  select transaction_id,
         amount,
         transaction_type,
         (case when from_account is not null then 'receive from #' || from_account
         else 'deposit' end) as description
    from bank.account_transactions at
         left join bank.fund_transfers ft
                on ft.credit_transaction_id = at.transaction_id
   where account_id = :account-id
     and transaction_type = 'credit'),
  debits as (
    select transaction_id,
           amount as credit,
           transaction_type,
           (case when to_account is not null then 'send to #' || to_account
            else 'withdraw' end) as description
      from bank.account_transactions at
           left join bank.fund_transfers ft
               on ft.debit_transaction_id = at.transaction_id
     where account_id = :account-id
       and transaction_type = 'debit'),
  credits_and_debits as (
    select *
      from credits
     union all
    select *
      from debits)
select (row_number() over (order by transaction_id asc) - 1) as sequence,
       amount,
       transaction_type,
       description
  from credits_and_debits
 order by transaction_id desc;

-- insert into account_transactions (account_id, transaction_type, amount, transaction_date)
-- values (11, 'credit', 100, now() + '1 day'::interval);

-- insert into account_transactions (account_id, transaction_type, amount, transaction_date)
-- values (11, 'debit', 10, now() + '1 day'::interval);
-- insert into account_transactions (account_id, transaction_type, amount, transaction_date)
-- values (11, 'debit', 10, now() + '7 day'::interval);

-- insert into account_transactions (account_id, transaction_type, amount, transaction_date)
-- values (11, 'debit', 10, now() - '7 day'::interval);
