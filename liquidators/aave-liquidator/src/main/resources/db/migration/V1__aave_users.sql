create table aave_user
(
    address   text primary key,
    latest_hf double precision default 2,
    ignored   boolean          default false
);