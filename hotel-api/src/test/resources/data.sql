-- Отели
insert into hotels (name, city) values ('Grand Oak', 'Berlin');
insert into hotels (name, city) values ('River Plaza', 'Munich');

-- Комнаты (разный times_booked и доступность)
insert into rooms (hotel_id, number, available, times_booked) values (1, '101', true, 0);
insert into rooms (hotel_id, number, available, times_booked) values (1, '102', true, 2);
insert into rooms (hotel_id, number, available, times_booked) values (1, '103', false, 5);
insert into rooms (hotel_id, number, available, times_booked) values (2, '201', true, 1);
insert into rooms (hotel_id, number, available, times_booked) values (2, '202', true, 1);

-- Холд-таблица (пустая на старте тестов)
-- NOTE: имена полей должны соответствовать вашим JPA:
-- room_holds(id, room_id, request_id, start_date, end_date, status)
-- insert into room_holds (...) values (...);  -- при необходимости
