ALTER TABLE nextbook.keywords_to_book ADD CONSTRAINT unique_couple UNIQUE(book_id, keyword_id);