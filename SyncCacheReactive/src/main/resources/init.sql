
create table public.range
(
    id          bigserial
        primary key,
    card_number varchar(255),
    from_date   timestamp(9),
    to_date     timestamp(9)
);


create table public.transaction
(
    id         bigserial
        primary key,
    added_date timestamp(6)   not null,
    amount     numeric(38, 1) not null,
    from_card  varchar(255)   not null,
    status     varchar,
    to_card    varchar(255)   not null,
    constraint ukedvvc6skwsvgigo1fyedsvt3d
        unique (from_card, to_card, added_date)
);

alter table public.transaction
    owner to postgres;

create sequence public.range_id_seq;

alter sequence public.range_id_seq owner to postgres;

alter sequence public.range_id_seq owned by public.range.id;

create sequence public.transaction_id_seq;

alter sequence public.transaction_id_seq owner to postgres;

alter sequence public.transaction_id_seq owned by public.transaction.id;

