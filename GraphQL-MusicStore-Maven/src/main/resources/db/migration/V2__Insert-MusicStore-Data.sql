INSERT INTO tracks (movie_title, track_title, tempo_name, tempo_bpm, inventory, created)
values ('Starting Out Slow', 'Stillness of the mind', 'fast', 126, 12, '1977-03-28 00:00:00'),
       ('1-2 Guitar Hero!', 'Rhythm of the Night', 'medium', 85, 0, '1985-01-02 00:00:00'),
       ('American Reunion', 'The Slow Slowdown', 'medium', 90, 1, '2022-11-15 19:09:57'),
       ('Machiavelli Rises', 'The Piano in The Night', 'slow', 82, 0, '2022-12-03 19:09:57'),
       ('Fast and the Furious', 'Furious Abel Drum Solo', 'medium', 120, 2, '2022-12-15 19:09:57');

INSERT INTO composers (composer_id, composer_name)
values (1, 'Abel Korzenoiski'),
       (2, 'Fast Slow Stephanson'),
       (3, 'Josh Talbot'),
       (4, 'Medium Talbot');

INSERT INTO tracks_composers (track_id, composer_id)
values (1, 1),
       (2, 2),
       (3, 3),
       (4, 3),
       (5, 4);
