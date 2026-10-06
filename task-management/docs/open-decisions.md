# Open Decisions 1 — questions raised by the Phase 1 plan

The questions the AI raised while planning, with the developer's answers. Four of them differ from the AI's proposal.

1. **Register response.** `POST /api/v1/auth/register`: which status code and which body — 201 with
   `{id, name, email}`, 200 with the same body, or 201 empty? Any `Location` header, given that no
   route returns a user by id?
   
   Answer: 201 with {id, name, email}, no Location header.

2. **`ownerId` in task responses.** Do the task response bodies include `ownerId`, or omit it because
   the owner is always the authenticated caller?

   Answer: Leave ownerId out, since the owner is always the logged-in user.

3. **Password length, characters or bytes.** The rule says "8 to 72 characters, because BCrypt only
   considers the first 72 bytes", and the two differ: 72 times `é` is 72 characters but 144 UTF-8
   bytes. Which one is validated, 72 characters or 72 UTF-8 bytes?

   Answer: Validate at least 8 characters and at most 72 UTF-8 bytes, since BCrypt ignores anything past byte 72.

4. **Length limits for the user fields.** No limits are given for `name` and `email`, but a column
   needs a size and `web` needs a bound. Which limits for `name`, `email` and `password_hash`?

   Answer: 100 characters for name, 254 for email and 60 for password_hash, the fixed length of a BCrypt hash.

5. **`description` normalisation.** `title` is explicitly trimmed; `description` is not. Is
   `description` stored exactly as sent, or trimmed with a blank value turned into `null`?

   Answer: Trim it too and store a blank value as null.

6. **Error body for read-only fields.** `id`, `ownerId`, `createdAt` or `updatedAt` in the body must
   respond 400, but the requirement does not ask for the field to be named. Is a single 400
   `"Malformed request body"` enough, or must the offending field be named in `errors[]`?

   Answer: Name the field in errors[] with a message like "must not be sent", using the usual "Validation failed" shape.

7. **Timezone of the "not in the past" boundary.** Turning the `Clock` instant into "today" needs a
   zone, and this machine is UTC-3: between 21:00 and midnight local time, UTC is already tomorrow.
   UTC, a named zone, or the JVM default?

   Answer: Use UTC.

8. **`size` above the allowed range.** `size` is 1 to 100 with a default of 20. Does `size=101`
   respond 400, or is it clamped to 100 with a 200?

   Answer: Return 400, the same as any other invalid value.
   
9. **Seed data.** "One demo user and a few tasks": which user and password, and how many tasks with
   which statuses and due dates?

   Answer: User demo@example.com with password demo1234 and five tasks across all three statuses, with due dates relative to the current date.
