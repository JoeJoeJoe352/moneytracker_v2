create Table users (
    id int(11) unsigned NOT NULL AUTO_INCREMENT primary key,
    uuid varchar(255) not null unique,
    username varchar(255) not null,
    password varchar(255) not null,
    email varchar(255) not null unique,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp on update current_timestamp
);
