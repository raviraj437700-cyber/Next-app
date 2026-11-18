# RX Test — Bihar Board MCQ Learning App

An Android-first React Native + Expo application with a crystal-glass interface. Planned modules: Supabase authentication and profiles, subject/chapter management, chapter-wise MCQ tests, results, leaderboard, student/admin chat, four groups, announcements, PDF notes and AI-assisted MCQ generation with admin review.

## Stack
- Expo + React Native + Expo Router
- Supabase Auth, Postgres, Storage and Realtime
- Gemini API via a server-side Edge Function (never expose the AI key in the app)
- EAS Build for Android APK

## Setup
1. Install Node.js 20.19.4+ and run `npm install`.
2. Copy `.env.example` to `.env` and add the Supabase project URL and publishable key.
3. Run `npx expo start`.
4. Configure EAS Build after backend setup.

Do not commit `.env`, Supabase secret keys, service-role keys or Gemini API keys. The first commit is the UI foundation; backend schema and secured AI processing are the next implementation stage.
