-- Пользователи (если хранятся в БД)
-- Таблица: users(id, username, password_hash, role)

insert into users (username, password_hash, role)
values ('admin', '$2a$10$7pR6aV9OQfS9z2lD7q6O3u3dCG9y2kM9M1x2QbY7l2i2Qe2Z4xk2G', 'ROLE_ADMIN');

insert into users (username, password_hash, role)
values ('user',  '$2a$10$7pR6aV9OQfS9z2lD7q6O3u3dCG9y2kM9M1x2QbY7l2i2Qe2Z4xk2G', 'ROLE_USER');

-- Бронирования (минимум — пусто; тесты сами создадут, чтобы проверять идемпотентность)
-- Таблица: bookings(id, user_id, room_id, status, start_date, end_date, request_id)
-- insert into bookings (...) values (...);
