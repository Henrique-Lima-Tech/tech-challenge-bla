ALTER TABLE local_pokemon ADD COLUMN user_id BIGINT;

UPDATE local_pokemon SET user_id = (SELECT MIN(id) FROM users);

DELETE FROM local_pokemon_abilities
WHERE local_pokemon_id IN (SELECT id FROM local_pokemon WHERE user_id IS NULL);
DELETE FROM local_pokemon_internal_tags
WHERE local_pokemon_id IN (SELECT id FROM local_pokemon WHERE user_id IS NULL);
DELETE FROM local_pokemon WHERE user_id IS NULL;

ALTER TABLE local_pokemon ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE local_pokemon DROP CONSTRAINT uk_local_pokemon_poke_api_id;
ALTER TABLE local_pokemon ADD CONSTRAINT uk_local_pokemon_user_poke_api_id UNIQUE (user_id, poke_api_id);
ALTER TABLE local_pokemon ADD CONSTRAINT fk_local_pokemon_users FOREIGN KEY (user_id) REFERENCES users (id);
