# Marketplace (LeadTrack Industrial) – Android app

## Git rules

- Push code **only** to https://github.com/jimm529/marketplace-android.git (branch `main`).
- Never push to any other repository. The `backend/` folder is a separate repository and must not be pushed from here.
- Stage with `git add app` (not `git add -A`) so `backend` is never included.
- Commit and push directly to `main`: `git push origin main`.

## Cost rules

- Use AWS as little as possible, because it costs money.
- Do not upload files to S3, add AWS services, or run anything that calls AWS (for example document or photo uploads) just to test or seed data. Use existing data, public sample links or local mocks instead.
- If a task would need AWS, say so first and ask before doing it.
