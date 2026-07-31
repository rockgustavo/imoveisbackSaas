-- ADR-03: necessária para o EXCLUDE USING gist que garante RN-06-05
-- (exclusividade de agenciamento), introduzido no Épico 06.
CREATE EXTENSION IF NOT EXISTS btree_gist;
