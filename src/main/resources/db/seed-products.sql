-- =====================================================================
-- Asmitha Explores - Order Tracker : product catalogue
--
-- Mirrors the storefront (asmitha-explores-bookstore.onrender.com).
-- Inserts PRODUCTS ONLY - no customers, no orders.
-- Stock starts at 0; maintain it from the Inventory screen.
--
-- Idempotent: a product is skipped when its name already exists, so this
-- script can be re-run to add newly released products.
-- =====================================================================

-- ---------------------------------------------- Flash Cards
insert into products (name, category, price, quantity_in_hand, quantity_to_be_ordered)
select 'Tamil 247 Letters Flash Cards', 'Flash Cards', 1350.00, 0, 0
where not exists (select 1 from products where name = 'Tamil 247 Letters Flash Cards');
insert into products (name, category, price, quantity_in_hand, quantity_to_be_ordered)
select 'Alphabet Picture Flash Cards', 'Flash Cards', 600.00, 0, 0
where not exists (select 1 from products where name = 'Alphabet Picture Flash Cards');
insert into products (name, category, price, quantity_in_hand, quantity_to_be_ordered)
select 'Tamil Uyir Mei Eluthukkal Flash Cards', 'Flash Cards', 360.00, 0, 0
where not exists (select 1 from products where name = 'Tamil Uyir Mei Eluthukkal Flash Cards');
insert into products (name, category, price, quantity_in_hand, quantity_to_be_ordered)
select 'Consonant Blending & Digraph Flash Cards', 'Flash Cards', 360.00, 0, 0
where not exists (select 1 from products where name = 'Consonant Blending & Digraph Flash Cards');
insert into products (name, category, price, quantity_in_hand, quantity_to_be_ordered)
select 'Wild & Domestic Animals Flash Cards', 'Flash Cards', 360.00, 0, 0
where not exists (select 1 from products where name = 'Wild & Domestic Animals Flash Cards');
insert into products (name, category, price, quantity_in_hand, quantity_to_be_ordered)
select 'Flags Flash Cards A to Z', 'Flash Cards', 320.00, 0, 0
where not exists (select 1 from products where name = 'Flags Flash Cards A to Z');
insert into products (name, category, price, quantity_in_hand, quantity_to_be_ordered)
select 'Phonics Flash Cards', 'Flash Cards', 300.00, 0, 0
where not exists (select 1 from products where name = 'Phonics Flash Cards');
insert into products (name, category, price, quantity_in_hand, quantity_to_be_ordered)
select 'Parts of Body Flash Cards', 'Flash Cards', 300.00, 0, 0
where not exists (select 1 from products where name = 'Parts of Body Flash Cards');
insert into products (name, category, price, quantity_in_hand, quantity_to_be_ordered)
select 'Tamil Mei Eluthukkal Flash Cards', 'Flash Cards', 280.00, 0, 0
where not exists (select 1 from products where name = 'Tamil Mei Eluthukkal Flash Cards');
insert into products (name, category, price, quantity_in_hand, quantity_to_be_ordered)
select 'Birds Flash Cards', 'Flash Cards', 250.00, 0, 0
where not exists (select 1 from products where name = 'Birds Flash Cards');
insert into products (name, category, price, quantity_in_hand, quantity_to_be_ordered)
select 'Tamil Uyir Eluthukkal Flash Cards', 'Flash Cards', 220.00, 0, 0
where not exists (select 1 from products where name = 'Tamil Uyir Eluthukkal Flash Cards');
insert into products (name, category, price, quantity_in_hand, quantity_to_be_ordered)
select 'Flowers Flash Cards', 'Flash Cards', 180.00, 0, 0
where not exists (select 1 from products where name = 'Flowers Flash Cards');
-- ---------------------------------------------- Banners
insert into products (name, category, price, quantity_in_hand, quantity_to_be_ordered)
select 'Numbers & Tamil Names - Banner 1 to 100', 'Banners', 850.00, 0, 0
where not exists (select 1 from products where name = 'Numbers & Tamil Names - Banner 1 to 100');
insert into products (name, category, price, quantity_in_hand, quantity_to_be_ordered)
select 'Maths Multiplication Table 1 to 10 - Banner', 'Banners', 750.00, 0, 0
where not exists (select 1 from products where name = 'Maths Multiplication Table 1 to 10 - Banner');
insert into products (name, category, price, quantity_in_hand, quantity_to_be_ordered)
select 'Jolly Phonics - Banner', 'Banners', 680.00, 0, 0
where not exists (select 1 from products where name = 'Jolly Phonics - Banner');
insert into products (name, category, price, quantity_in_hand, quantity_to_be_ordered)
select 'Tamil Uyir Mei Eluthukkal - Banner', 'Banners', 650.00, 0, 0
where not exists (select 1 from products where name = 'Tamil Uyir Mei Eluthukkal - Banner');
insert into products (name, category, price, quantity_in_hand, quantity_to_be_ordered)
select 'Tamil Uyir Mei Eluthukkal Table - Banner', 'Banners', 650.00, 0, 0
where not exists (select 1 from products where name = 'Tamil Uyir Mei Eluthukkal Table - Banner');
insert into products (name, category, price, quantity_in_hand, quantity_to_be_ordered)
select 'Phonics Flash Cards - Banner', 'Banners', 650.00, 0, 0
where not exists (select 1 from products where name = 'Phonics Flash Cards - Banner');
insert into products (name, category, price, quantity_in_hand, quantity_to_be_ordered)
select 'Jolly Phonics - Sound Group Banner', 'Banners', 650.00, 0, 0
where not exists (select 1 from products where name = 'Jolly Phonics - Sound Group Banner');
insert into products (name, category, price, quantity_in_hand, quantity_to_be_ordered)
select 'Numbers - Banner 1 to 50', 'Banners', 650.00, 0, 0
where not exists (select 1 from products where name = 'Numbers - Banner 1 to 50');
insert into products (name, category, price, quantity_in_hand, quantity_to_be_ordered)
select 'Tamil Uyir Eluthukkal - Banner', 'Banners', 600.00, 0, 0
where not exists (select 1 from products where name = 'Tamil Uyir Eluthukkal - Banner');
insert into products (name, category, price, quantity_in_hand, quantity_to_be_ordered)
select 'Tamil Mei Eluthukkal - Banner', 'Banners', 600.00, 0, 0
where not exists (select 1 from products where name = 'Tamil Mei Eluthukkal - Banner');
-- ---------------------------------------------- Learning Books
insert into products (name, category, price, quantity_in_hand, quantity_to_be_ordered)
select 'Tamil Phonics Book', 'Learning Books', 599.00, 0, 0
where not exists (select 1 from products where name = 'Tamil Phonics Book');
insert into products (name, category, price, quantity_in_hand, quantity_to_be_ordered)
select 'Having Fun With Phonics', 'Learning Books', 199.00, 0, 0
where not exists (select 1 from products where name = 'Having Fun With Phonics');
insert into products (name, category, price, quantity_in_hand, quantity_to_be_ordered)
select 'Tamil Letters - உயிர் மெய் எழுத்துக்கள்', 'Learning Books', 189.00, 0, 0
where not exists (select 1 from products where name = 'Tamil Letters - உயிர் மெய் எழுத்துக்கள்');
insert into products (name, category, price, quantity_in_hand, quantity_to_be_ordered)
select 'Alphabets Writing Practice', 'Learning Books', 189.00, 0, 0
where not exists (select 1 from products where name = 'Alphabets Writing Practice');
-- ---------------------------------------------- Story Books
insert into products (name, category, price, quantity_in_hand, quantity_to_be_ordered)
select 'Bed Time Story Book - Penguin Theme', 'Story Books', 100.00, 0, 0
where not exists (select 1 from products where name = 'Bed Time Story Book - Penguin Theme');
insert into products (name, category, price, quantity_in_hand, quantity_to_be_ordered)
select 'Bed Time Story Book - Giraffe Theme', 'Story Books', 100.00, 0, 0
where not exists (select 1 from products where name = 'Bed Time Story Book - Giraffe Theme');
insert into products (name, category, price, quantity_in_hand, quantity_to_be_ordered)
select 'Bed Time Story Book - Rabbit Theme', 'Story Books', 100.00, 0, 0
where not exists (select 1 from products where name = 'Bed Time Story Book - Rabbit Theme');
-- ------------------------------------- added after the initial load (19 Oct 2026)
insert into products (name, category, price, quantity_in_hand, quantity_to_be_ordered)
select 'Chess Flash Cards', 'Flash Cards', 360.00, 0, 0
where not exists (select 1 from products where name = 'Chess Flash Cards');

insert into products (name, category, price, quantity_in_hand, quantity_to_be_ordered)
select 'Cursive Writing Level 1 Book', 'Learning Books', 199.00, 0, 0
where not exists (select 1 from products where name = 'Cursive Writing Level 1 Book');

insert into products (name, category, price, quantity_in_hand, quantity_to_be_ordered)
select 'Cursive Writing Level 2 Book', 'Learning Books', 199.00, 0, 0
where not exists (select 1 from products where name = 'Cursive Writing Level 2 Book');

-- ------------------------------------------------------------- summary
select category, count(*) as products, min(price) as cheapest, max(price) as dearest
from products group by category order by category;

