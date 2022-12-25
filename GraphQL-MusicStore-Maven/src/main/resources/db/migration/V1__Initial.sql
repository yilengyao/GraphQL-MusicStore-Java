CREATE TABLE user (
                      id BIGINT NOT NULL AUTO_INCREMENT,
                      name varchar(255) not null,
                      email varchar(255) not null,
                      PRIMARY KEY(id)
);

insert into user (name, email) values ('john doe', 'doe@gmail.com');