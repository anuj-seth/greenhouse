#!/bin/bash

MASTER_USER=postgres

sudo -u postgres psql -U $MASTER_USER -d postgres <<EOF

\connect postgres
drop database if exists greenhouse;
drop user if exists greendev;
drop role if exists greendev;
create role greendev with login superuser password 'green';
create database greenhouse with owner greendev;

\connect greenhouse

create schema bank authorization greendev;
grant usage on schema bank to greendev;
grant create on schema bank to greendev;

ALTER DATABASE greenhouse SET search_path TO bank, public;
EOF
