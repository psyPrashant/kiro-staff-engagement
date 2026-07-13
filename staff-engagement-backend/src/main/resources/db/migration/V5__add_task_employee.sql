-- V5__add_task_employee.sql
-- Adds a direct employee_id foreign key to the tasks table, allowing tasks
-- to be linked to an employee independently of an interaction.

ALTER TABLE tasks ADD COLUMN employee_id BIGINT;

ALTER TABLE tasks ADD CONSTRAINT fk_tasks_employee FOREIGN KEY (employee_id) REFERENCES employees(id);

CREATE INDEX idx_tasks_employee_id ON tasks(employee_id);
