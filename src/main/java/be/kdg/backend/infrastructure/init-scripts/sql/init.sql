create schema ordering;
create schema restaurant;
create schema delivery;

create table if not exists scheduled_publish (
    id uuid primary key,
    restaurant_id uuid not null,
    publish_at timestamp not null,
    status varchar(16) not null,
    attempts int not null default 0,
    last_error varchar(500),
    created_at timestamp not null,
    updated_at timestamp not null
);
create index if not exists idx_scheduled_publish_due
    on scheduled_publish (status, publish_at);

