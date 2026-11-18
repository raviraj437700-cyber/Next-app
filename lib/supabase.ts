import { Platform } from 'react-native';
import { createClient } from '@supabase/supabase-js';

const url = process.env.EXPO_PUBLIC_SUPABASE_URL;
const key = process.env.EXPO_PUBLIC_SUPABASE_ANON_KEY;

// AsyncStorage touches browser globals during module evaluation, which breaks
// Expo Router's server-side static rendering on Vercel. Use browser storage on
// web (guarded for SSR) and load AsyncStorage only on native platforms.
const webStorage = {
  getItem: async (key: string) =>
    typeof window === 'undefined' ? null : window.localStorage.getItem(key),
  setItem: async (key: string, value: string) => {
    if (typeof window !== 'undefined') window.localStorage.setItem(key, value);
  },
  removeItem: async (key: string) => {
    if (typeof window !== 'undefined') window.localStorage.removeItem(key);
  },
};

const storage =
  Platform.OS === 'web'
    ? webStorage
    : require('@react-native-async-storage/async-storage').default;

export const supabase =
  url && key && !url.includes('YOUR_PROJECT')
    ? createClient(url, key, {
        auth: {
          storage,
          persistSession: true,
          autoRefreshToken: true,
          detectSessionInUrl: false,
        },
      })
    : null;
