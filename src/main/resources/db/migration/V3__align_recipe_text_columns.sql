ALTER TABLE recipe
    MODIFY COLUMN description LONGTEXT NULL;

ALTER TABLE recipe_source_document
    MODIFY COLUMN import_error LONGTEXT NULL;

ALTER TABLE recipe_step
    MODIFY COLUMN content LONGTEXT NOT NULL;
