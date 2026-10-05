# Sign in and sign up

> The `/login` and `/register` screens.
> Back to the [index](README.md).

```
┌──────────────────────────────────┐
│  Sign in                         │
│                                  │
│  Email                           │
│  [demo@pokedex.dev            ]  │
│                                  │
│  Password                        │
│  [••••••••                    ]  │
│                                  │
│  ┌ Invalid email or password   ┐ │  ← back-end 401, above the button
│  └─────────────────────────────┘ │
│                                  │
│  [          Sign in           ]  │
│                                  │
│  Don’t have an account? Sign up  │
│                                  │
│  Demo user: demo@pokedex.dev /   │
│             demo1234             │
└──────────────────────────────────┘
```

- Centered card, at most 400px wide. On mobile it takes the full width.
- Forms with React Hook Form + `loginSchema`/`registerSchema` (in `features/auth/schemas.ts`), with the contract's rules:
  - **Sign in:** `email` (valid email) and `password` (required).
  - **Sign up:** `name` (required, ≤ 100), `email` (valid, ≤ 254), `password` (8 to 72 characters) and "Confirm password", which only exists in the form.
- Each field (`TextField`) has a `<label>`, the error right below it (linked by `aria-describedby`, with `aria-invalid`) and the right `autoComplete`.
- **When it validates:** sign in on submit (on blur, an error would move the "Sign up" link away from the click); sign up when leaving a field (`onTouched`). The focus goes to the first field with an error. On load, sign in focuses the email, or the password when coming from sign up with the email filled in.
- The submit button shows a spinner and is disabled while submitting.
- **Back-end errors:**
  - 401 → the generic message at the top of the form.
  - 409 on sign up (email already used) → error on the `email` field.
  - 400 with `fieldErrors` → each error on its own field; anything else at the top.
- **After sign in:** the token and the user's name (from the login response, D-32) are saved, the name shows in the header, and the user goes back to the protected page of origin (or `/pokemon`). Sign out and an expired session remove both.
- **After sign up:** it goes to `/login` with the toast "Account created. Please sign in." and the email already filled in.
- The demo user hint shows **only** while the mock runs (`npm run dev` with `VITE_USE_MOCKS=true`): that user does not exist in the real back end.
