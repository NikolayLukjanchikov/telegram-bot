--liquibase formatted sql

--changeset Nikolay:1
CREATE TABLE notification_task
(
    id       SERIAL,
    chatId   INT,
    message  CHARACTER,
    timeDate CHARACTER
)