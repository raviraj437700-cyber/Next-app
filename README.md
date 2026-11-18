# RX Test — Bihar Board MCQ App

Android-first Expo + React Native app backed by Supabase. The current foundation includes email sign-up/sign-in, subject and chapter browsing, chapter test loading, MCQ answering, attempt saving, groups listing, announcements, history and role-gated admin content forms.

## Backend
Supabase project RX-MOD (ap-south-1). App-specific tables use the rxt_ prefix; existing RX-MOD tables are not modified by this app.

## Local setup
1. Install Node.js LTS and run npm install.
2. Copy .env.example to .env.
3. Set EXPO_PUBLIC_SUPABASE_URL and the Supabase publishable/anon key. Never put a service-role key or Gemini key in the app.
4. Run npx expo start and test on Android.

## Android APK
Install EAS CLI, authenticate with an Expo account, then run eas build --platform android --profile preview. The preview profile is configured to produce an APK.

## Remaining production work
- Assign the verified admin UUID in public.rxt_profiles.role='admin' only after the intended owner account registers. Never allow self-assigned admin role.
- Configure GEMINI_API_KEY as a Supabase Edge Function secret and implement/test secure PDF extraction + MCQ generation. AI questions must remain drafts until admin review.
- Complete realtime chat, group membership/message screens, avatar upload, question editing/publishing UI and detailed result review.
- Configure env values and run a real Android build/device test before release.

## AI integration status
AI question generation is currently disabled. Google's Gemini API terms require API users to be at least 18 and prohibit API clients directed to or likely accessed by under-18 users. Since RX Test is a student app, do not create or add a Gemini API key for this project. The deployed `rxt-generate-mcqs` endpoint is a disabled placeholder and returns HTTP 403. Manual admin MCQ creation remains available.

## Security
Do not commit .env, Supabase secret/service-role keys, Expo tokens or Gemini API keys. Supabase RLS is the authorization boundary; UI role checks alone are not security.