create table bank.accounts (
  account_id bigserial primary key,
  account_name text
);

create table bank.account_opening_balances (
  account_id bigint not null references bank.accounts(account_id),
  balance_date date not null,
  amount float not null,
  primary key (account_id, balance_date)
);

create table bank.transaction_types (
  transaction_type text primary key);

insert into bank.transaction_types
values ('credit'),
       ('debit');

create table bank.account_transactions (
  transaction_id bigserial primary key,
  account_id bigint not null references bank.accounts(account_id),
  transaction_type text not null references bank.transaction_types(transaction_type) 
);
