ALTER TABLE document_file
    ADD CONSTRAINT file_hash UNIQUE (file_hash);