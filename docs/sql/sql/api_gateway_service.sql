create table if not exists app_user (
                                        id uuid primary key,
                                        email varchar(255) not null unique,
    display_name varchar(255),
    provider varchar(50),
    created_at timestamptz not null,
    last_login_at timestamptz
);

create table app_role (
                          id uuid primary key,
                          code varchar(50) not null unique,      -- USER / ADMIN
                          created_at timestamptz not null default now()
);

create table app_user_role (
                               user_id uuid not null references app_user(id) on delete cascade,
                               role_id uuid not null references app_role(id) on delete cascade,
                               primary key (user_id, role_id)
);

-- 初始化角色
insert into app_role(id, code) values
                                   (gen_random_uuid(), 'USER'),
                                   (gen_random_uuid(), 'ADMIN');