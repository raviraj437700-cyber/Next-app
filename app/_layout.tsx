import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { useEffect } from 'react';
import { View, Text } from 'react-native';
import { isSupabaseConfigured } from '../lib/supabase';

export default function RootLayout() {
  useEffect(() => {}, []);
  return <View style={{flex:1,backgroundColor:'#080B18'}}><StatusBar style="light" /><Stack screenOptions={{headerShown:false,contentStyle:{backgroundColor:'#080B18'},animation:'fade_from_bottom'}} />{!isSupabaseConfigured && <View style={{padding:7,backgroundColor:'#3B2A12'}}><Text style={{color:'#FFD58A',textAlign:'center',fontSize:11}}>Backend setup pending — Supabase credentials not configured yet.</Text></View>}</View>;
}
