CREATE TABLE user (
                      id BIGINT NOT NULL AUTO_INCREMENT,
                      name varchar(255) not null,
                      email varchar(255) not null,
                      PRIMARY KEY(id)
);

insert into user (name, email) values ('john doe', 'doe@gmail.com');

CREATE TABLE track (
    track_id BIGINT NOT NULL AUTO_INCREMENT,
    movie_title varchar(255) NOT NULL,
    track_title varchar(255) NOT NULL,
    tempo_name ENUM('fast', 'medium', 'slow') NOT NULL,
    tempo_bpm INT NOT NULL,
    composer_id BIGINT,
    inventory INT,
    created DATE,
    PRIMARY KEY (track_id)
);

CREATE TABLE composers (
    composer_id BIGINT NOT NULL,
    composer_name varchar(255) NOT NULL,
    PRIMARY KEY (composer_id)
);

CREATE TABLE instruments (
    instrument_id BIGINT,
    instrument_group varchar(255),
    instrument_name varchar(255),
    PRIMARY KEY (instrument_id)
);

CREATE TABLE track_instruments (
    track_id BIGINT NOT NULL,
    instrument_id BIGINT NOT NULL,
    PRIMARY KEY (track_id, instrument_id)
);