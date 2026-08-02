ALTER TABLE ai_plan_run
    MODIFY COLUMN error_message LONGTEXT NULL;

ALTER TABLE meal_plan_item
    MODIFY COLUMN reason LONGTEXT NULL;
