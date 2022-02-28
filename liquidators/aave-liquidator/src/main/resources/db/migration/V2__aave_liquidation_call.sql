create table aave_liquidation_call
(
    id            bigserial primary key,
    submitted_at  timestamp        default null,
    submitted     boolean          default false,
    unsigned_data text             default null,
    signed_data   text             default null,
    user_id       text not null,
    netProfit     double precision default 2,
    foreign key (user_id) references aave_user (address)
);