-- a fund transfer consists of a debit from the source and a credit to the target account,
-- both of which are stored in the `bank.account_transactions` table.
-- this table links the two legs of the fund transfer
create table bank.fund_transfers (
  debit_transaction_id bigint not null references bank.account_transactions(transaction_id),
  credit_transaction_id bigint not null references bank.account_transactions(transaction_id),
  from_account bigint not null references bank.accounts(account_id),
  to_account bigint not null references bank.accounts(account_id),
  primary key (debit_transaction_id, credit_transaction_id)
);

create unique index bank_fund_transfers_credit_transaction_id_uidx on bank.fund_transfers(credit_transaction_id);
