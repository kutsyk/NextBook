ALTER TABLE nextbook.orders_with_book ADD CONSTRAINT unique_couple UNIQUE(orders_id, book_id);