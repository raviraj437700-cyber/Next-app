# RX Test — Bihar Board MCQ App

Android-first Expo + React Native app backed by Supabase. The project uses the existing Supabase project **RX-MOD** with app-specific database objects prefixed by `rxt_`. Existing legacy RX-MOD objects are not modified by the RX Test app.

## Current app features

- Email sign-up and sign-in, student profile, and database-controlled admin roles.
- Subject → chapter → published tests → MCQ practice flow.
- Test attempt history and detailed result review.
- Admin subject/chapter/question/test authoring, question editing and publishing, test publishing, and announcements.
- Study groups, group messages, and student-to-admin support messages.
- Profile photo upload, private chapter PDF upload, and signed PDF access are implemented; physical Android testing is still required.

## Security model

- RLS policies protect app-owned tables.
- Students receive published question text/options through `rxt_student_questions`; the correct answer is not returned before submission.
- `rxt_submit_attempt` calculates scores in PostgreSQL. The client cannot directly insert or update scores.
- A student cannot update their own `role` column. The previously designated owner email is promoted to admin only after Supabase confirms that email address.
- No Supabase service-role or secret key belongs in the app. Use only the project's publishable/anon key.
- AI MCQ generation is intentionally disabled. Do not add Gemini API integration: its API age/usage terms are incompatible with this app's under-18 student audience. Admins can manually author and review MCQs.

## Local development

1. Install Node.js LTS and run `npm install`.
2. Copy `.env.example` to `.env`.
3. Set `EXPO_PUBLIC_SUPABASE_URL` and `EXPO_PUBLIC_SUPABASE_ANON_KEY` to the RX-MOD project URL and publishable key.
4. Run `npx expo start` and test on a physical Android device.

## Android APK

The EAS preview profile is configured for an installable APK. An Expo account and a successful EAS Android build/device test are still required before calling the app release-ready.

## Release checklist

- Verify realtime chat and all RLS/storage policies with separate student and admin accounts.
- Finish and device-test remaining admin CRUD and validation for subjects, chapters, tests, questions, and notes.
- Test authentication, password reset, offline/error states, test timing, answer submission, history, and chat on a physical Android device.
- Run security/performance advisors and resolve all RX Test-specific findings.
- Build and install the EAS APK, then complete end-to-end QA.
