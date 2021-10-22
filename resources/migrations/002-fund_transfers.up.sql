create table bank.fund_transfers (
  credit_transaction_id bigint not null references bank.account_transactions(transaction_id),
  debit_transaction_id bigint not null references bank.account_transactions(transaction_id),
  from_account bigint not null references bank.accounts(account_id),
  to_account bigint not null references bank.accounts(account_id),
  transaction_date timestamptz not null,
  primary key (debit_transaction_id, credit_transaction_id)
);
