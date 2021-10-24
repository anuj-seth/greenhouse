-- this is the primary account information table.
-- as a rule every activity on an account must lock the
-- row in this table before performing any activity on an account
-- even if that activity does not actually update data in this table.
create table bank.accounts (
  account_id bigserial primary key,
  account_name text
);

-- the opening balance is calculated at the start of every month
-- and is the sum total of all credits and debits recorded in the account_transactions
-- table after the last opening balance was computed.
-- note that this is not actual account balance at any given point in time.
-- for an updated account balance use the `amount` from this table and add/subtract all the
-- transactions that come after that `balance_date`
-- Note: a zero amount opening balance is created when a new account is created.
create table bank.account_opening_balances (
  account_id bigint not null references bank.accounts(account_id),
  balance_date timestamptz not null default now(),
  amount float not null,
  primary key (account_id, balance_date)
);

-- reference table for all the types of transactions the
-- system supports
create table bank.transaction_types (
  transaction_type text primary key);

insert into bank.transaction_types
values ('credit'),
       ('debit');

-- all the credits and debits against an account are logged in this table.
create table bank.account_transactions (
  transaction_id bigserial primary key,
  account_id bigint not null references bank.accounts(account_id),
  transaction_type text not null references bank.transaction_types(transaction_type),
  amount float not null,
  transaction_date timestamptz not null default now()
);

create index bank_account_transactions_account_id_idx on bank.account_transactions(account_id);
