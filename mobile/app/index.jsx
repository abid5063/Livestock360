import { Text, View, TouchableOpacity, StyleSheet, Image, SafeAreaView, StatusBar, Linking } from "react-native";
import { useRouter } from "expo-router";
import { Ionicons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import React, { useState, useEffect } from "react";
import { useLanguage } from "../utils/LanguageContext";
import { useTranslation } from 'react-i18next';

export default function WelcomeScreen() {
  const router = useRouter();
  const { language, changeLanguage, isLoading: languageLoading } = useLanguage();
  const { t, i18n } = useTranslation();

  // Update i18n language when language changes
  useEffect(() => {
    if (!languageLoading) {
      try {
        i18n.changeLanguage(language);
      } catch (error) {
        console.log('Error changing language:', error);
      }
    }
  }, [language, i18n, languageLoading]);

  const handleFarmerAuth = () => {
    router.push('/farmerAuth');
  };

  const handleVetAuth = () => {
    router.push('/vetAuth');
  };

  const handleLearnMore = () => {
    try {
      // You can replace this URL with your actual tutorial video link
      Linking.openURL('https://www.youtube.com/watch?v=your-tutorial-video-id');
    } catch (error) {
      console.log('Error opening tutorial link:', error);
    }
  };

  // Show loading state while language is initializing
  if (languageLoading) {
    return (
      <SafeAreaView style={styles.container}>
        <StatusBar barStyle="light-content" backgroundColor="#0d3b16" />
        <LinearGradient
          colors={['#02290aff', '#80c25aff', '#46c426ff', '#b0dd96ff']}
          style={styles.gradientBackground}
          start={{ x: 0, y: 0 }}
          end={{ x: 1, y: 1 }}
        />
        <View style={styles.loadingContainer}>
          <Text style={styles.loadingText}>Loading...</Text>
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container}> 
      <StatusBar barStyle="light-content" backgroundColor="#0d3b16" />
      
      {/* Green Gradient Background */}
      <LinearGradient
        colors={['#02290aff', '#80c25aff', '#46c426ff', '#b0dd96ff']}
        style={styles.gradientBackground}
        start={{ x: 0, y: 0 }}
        end={{ x: 1, y: 1 }}
      />

      {/* Language Toggle Button */}
      <TouchableOpacity
        style={styles.langButton}
        onPress={() => {
          try {
            changeLanguage(language === 'en' ? 'bn' : 'en');
          } catch (error) {
            console.log('Error changing language:', error);
          }
        }}
      >
        <Text style={styles.langButtonText}>{language === 'en' ? 'BN' : 'EN'}</Text>
      </TouchableOpacity>

      {/* Main Content */}
      <View style={styles.content}>
        {/* Logo Section - Blended with background */}
        <View style={styles.logoSection}>
          <View style={styles.logoContainer}>
            <Image 
              source={require('../assets/images/logo.jpeg')}
              style={styles.logo}
              resizeMode="cover"
            />
            {/* Logo blend overlay */}
            <LinearGradient
              colors={['rgba(202, 239, 138, 0.97)', 'rgba(4, 106, 35, 0.61)', 'rgba(45, 122, 74, 0.1)', 'transparent']}
              style={styles.logoBlendOverlay}
              start={{ x: 0, y: 0 }}
              end={{ x: 1, y: 1 }}
            />
            {/* Additional blend layers for seamless integration */}
            <View style={styles.logoBlendLayer1} />
            <View style={styles.logoBlendLayer2} />
          </View>
        </View>

        {/* Tagline Section - More farmer-focused messaging */}
        <View style={styles.taglineSection}>
          <Text style={styles.tagline}>{t('tagline') || 'Having trouble with livestock management?'}</Text>
          <Text style={styles.subTagline}>{t('subTagline1') || 'Stay with us'}</Text>
          <TouchableOpacity 
            style={styles.learnMoreButton}
            onPress={handleLearnMore}
            activeOpacity={0.7}
          >
            <Ionicons name="logo-youtube" size={20} color="#023402ff" />
            <Text style={styles.learnMoreText}>{t('subTagline2') || 'Learn how to use this app.'}</Text>
          </TouchableOpacity>
        </View>

        {/* Button Section - Enhanced for better UX */}
        <View style={styles.buttonSection}>
          <TouchableOpacity
            style={styles.optionButton}
            onPress={handleFarmerAuth}
            testID="farmer-option-button"
            activeOpacity={0.8}
          >
            <Ionicons name="leaf" size={28} color='#032301ff' />
            <Text style={styles.buttonText}>{t('farmer') || 'I\'m a Farmer'}</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.optionButton}
            onPress={handleVetAuth}
            testID="vet-option-button"
            activeOpacity={0.8}
          >
            <Ionicons name="medical" size={28} color='#032001ff'/>
            <Text style={styles.buttonText}>{t('vet') || 'I\'m a Veterinarian'}</Text>
          </TouchableOpacity>
        </View>
      </View>

      {/* Bottom gradient overlay */}
      <LinearGradient
        colors={['transparent','rgba(218, 238, 195, 1)',  'rgba(155, 209, 131, 1)']}
        style={styles.grassOverlay}
        start={{ x: 0, y: 0 }}
        end={{ x: 0, y: 1 }}
      />
      
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  gradientBackground: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
  },
  content: {
    flex: 1,
    paddingHorizontal: 24,
    justifyContent: 'space-between',
    zIndex: 1,
  },
  logoSection: {
    flex: 1, //don't change
    justifyContent: 'flex-start',
    alignItems: 'center',
    paddingTop: 12,
    marginTop: 2,
  },
  logoContainer: {
    width: '120%',
    aspectRatio: 1.5,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 2,
    position: 'relative',
  },
  logo: {
    width: '100%',
    height: '100%',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.2,
    shadowRadius: 6,
    elevation: 8,
    opacity: 0.98,
    borderRadius: 8,
  },
  logoBlendOverlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 20,
    borderRadius: 10,
  },
  logoBlendLayer1: {
    position: 'absolute',
    top: '10%',
    left: '10%',
    right: '10%',
    bottom: '10%',
    backgroundColor: 'rgba(13, 59, 22, 0.1)',
    borderRadius: 15,
  },
  logoBlendLayer2: {
    position: 'absolute',
    top: '20%',
    left: '20%',
    right: '20%',
    bottom: '20%',
    backgroundColor: 'rgba(26, 90, 45, 0.05)',
    borderRadius: 10,
  },
  appTitle: {
    fontSize: 32,
    fontWeight: '800',
    color: '#0d3b16',
    textAlign: 'center',
    letterSpacing: 1,
  },
  appSubtitle: {
    fontSize: 14,
    color: '#2d5a2d',
    textAlign: 'center',
    fontWeight: '600',
    letterSpacing: 2,
    textTransform: 'uppercase',
  },
  taglineSection: {
    alignItems: 'center',
    paddingVertical: 2,
    gap:6,
  },
  tagline: {
    fontSize: 28,
    fontWeight: '700',
    color: '#2f7112ff',
    textAlign: 'center',
    marginBottom: 5,
    lineHeight: 26,
    textShadowColor: 'rgba(0, 0, 0, 0.3)',
    textShadowOffset: { width: 0, height: 2 },
    textShadowRadius: 4,
  },
  subTagline: {
    fontSize: 20,
    color: '#2f7112ff',
    textAlign: 'center',
    fontWeight: '400',
    textShadowColor: 'rgba(0, 0, 0, 0.2)',
    textShadowOffset: { width: 0, height: 1 },
    textShadowRadius: 2,
    paddingBottom: 6,
    marginBottom: 2,
  },
  learnMoreButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 8,
    paddingHorizontal: 16,
    borderRadius: 20,
    backgroundColor: 'rgba(3, 43, 15, 0.1)',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.2)',
    gap: 8,
  },
  learnMoreText: {
    fontSize: 16,
    color:'#204a0eff',
    fontWeight: '500',
    textDecorationLine: 'underline',
  },
  buttonSection: {
    paddingBottom: 40,
    gap: 18,
  },
  optionButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'rgba(94, 163, 85, 0.15)',
    paddingVertical: 18,
    paddingHorizontal: 12,
    borderRadius: 10,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.15,
    shadowRadius: 8,
    elevation: 6,
    gap: 15,
    minHeight: 50,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.2)',
  },
  buttonText: {
    color: '#164204ff',
    fontSize: 20,
    fontWeight: '600',
    letterSpacing: 0.5,
  },
  grassBackground: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    height: 150,
    zIndex: 0,
  },
  grassImage: {
    width: '100%',
    height: '100%',
    opacity: 0.6,
  },
  grassOverlay: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    height: '100%',
  },
  langButton: {
    position: 'absolute',
    top: 18,
    right: 18,
    zIndex: 10,
    backgroundColor: 'rgba(255,255,255,0.2)',
    paddingVertical: 6,
    paddingHorizontal: 14,
    borderRadius: 16,
  },
  langButtonText: {
    color: '#032001ff',
    fontWeight: 'bold',
    fontSize: 16,
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingText: {
    fontSize: 18,
    color: '#fff',
    fontWeight: '600',
  },
});