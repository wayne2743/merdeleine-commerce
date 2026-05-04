create table if not exists app_user (
    id uuid primary key,
    email varchar(255) not null unique,
    display_name varchar(255),
    provider varchar(50),
    contact_name varchar(100),
    contact_phone varchar(30),
    contact_email varchar(255),
    shipping_address text,
    profile_completed boolean not null default false,
    created_at timestamptz not null,
    last_login_at timestamptz
);

create table if not exists app_role (
    id uuid primary key,
    code varchar(50) not null unique,
    created_at timestamptz not null default now()
);

create table if not exists app_user_role (
    user_id uuid not null references app_user(id) on delete cascade,
    role_id uuid not null references app_role(id) on delete cascade,
    primary key (user_id, role_id)
);

insert into app_role(id, code)
values
    ('11111111-1111-1111-1111-111111111111', 'USER'),
    ('22222222-2222-2222-2222-222222222222', 'ADMIN')
on conflict (code) do nothing;


