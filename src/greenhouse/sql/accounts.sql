-- :name insert-account :? :1
insert into bank.accounts (account_name)
values (:account-name)
       returning *;

-- :name open-balance :? :1
insert into bank.account_opening_balances (account_id, amount)
values (:account-id, :amount)
       returning *;
