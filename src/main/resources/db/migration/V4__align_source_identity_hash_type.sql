ALTER TABLE recipe_source_document
    MODIFY COLUMN source_identity_hash VARCHAR(64) NOT NULL;
