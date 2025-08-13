import { useState, useEffect, useCallback } from "react";
import { Text, View, TextInput, TouchableOpacity, StyleSheet, Alert, Image } from "react-native";
import axios from "axios";
import AsyncStorage from '@react-native-async-storage/async-storage';
import { useRouter, useFocusEffect } from "expo-router";
import { API_BASE_URL } from '../utils/apiConfig'; // Adjust the import path as needed
import { useLanguage } from '../utils/LanguageContext';
import { useTranslation } from 'react-i18next';

export default function FarmerAuthScreen() {
  const router = useRouter();
  const { language } = useLanguage();
  const { t, i18n } = useTranslation();

  // Update i18n language when language changes
  useEffect(() => {
    i18n.changeLanguage(language);
  }, [language, i18n]);

  const [isLogin, setIsLogin] = useState(true);
  const [formData, setFormData] = useState({
    name: "",
    email: "",
    password: "",
  });
  const [isLoading, setIsLoading] = useState(false);

  //  const API_BASE_URL = "http://localhost:3000/api/auth";

  // Reset form when screen is focused (e.g., after logout)
  useFocusEffect(
    useCallback(() => {
      setIsLogin(true);
      setFormData({
        name: "",
        email: "",
        password: "",
      });
      setIsLoading(false);
    }, [])
  );

  const handleChange = (name, value) => {
    setFormData({
      ...formData,
      [name]: value,
    });
  };

  const storeAuthData = async (token, farmer) => {
    try {
      await AsyncStorage.multiSet([
        ['authToken', token],
        ['userData', JSON.stringify(farmer)],
        ['userType', 'farmer']
      ]);
    } catch (error) {
      console.error("Error storing auth data:", error);
      throw error;
    }
  };

  const handleSubmit = async () => {
    try {
      setIsLoading(true);
      const { name, email, password } = formData;

      // Validation
      if (!email || !password) {
        Alert.alert(t('farmerAuth.error'), t('farmerAuth.emailPasswordRequired'));
        return;
      }

      if (!isLogin) {
        if (!name) {
          Alert.alert(t('farmerAuth.error'), t('farmerAuth.nameRequired'));
          return;
        }
        if (password.length < 6) {
          Alert.alert(t('farmerAuth.error'), t('farmerAuth.passwordLength'));
          return;
        }
        if (name.length < 3) {
          Alert.alert(t('farmerAuth.error'), t('farmerAuth.nameLength'));
          return;
        }
      }

      const endpoint = isLogin ? "/login" : "/register";
      const response = await axios.post(`${API_BASE_URL}/api/auth${endpoint}`, formData);

      const { token, farmer } = response.data;
      await storeAuthData(token, farmer);
      
      // Navigate to profile page with farmer data
      router.push({
        pathname: "/profile",
        params: { farmer: JSON.stringify(farmer) }
      });

    } catch (error) {
      console.error("Auth error:", error.response?.data || error.message);
      const errorMessage = error.response?.data?.message || 
                         error.message || 
                         t('farmerAuth.somethingWentWrong');
      Alert.alert(t('farmerAuth.error'), errorMessage);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <View style={styles.logoSection}>
        <View style={styles.logoContainer}>
          <Image 
            source={require('../assets/images/farmer.jpeg')}
            style={styles.logo}
            resizeMode="contain"
          />
        </View>
      </View>
      <Text style={styles.title}>{t('farmerAuth.title', { mode: isLogin ? t('farmerAuth.login') : t('farmerAuth.registration') })}</Text>
      
      {!isLogin && (
        <TextInput
          style={styles.input}
          placeholder={t('farmerAuth.fullNamePlaceholder')}
          placeholderTextColor="#333"
          value={formData.name}
          onChangeText={(text) => handleChange("name", text)}
          autoCapitalize="words"
          testID="auth-name-input"
        />
      )}
      
      <TextInput
        style={styles.input}
        placeholder={t('farmerAuth.emailPlaceholder')}
        placeholderTextColor="#333"
        keyboardType="email-address"
        autoCapitalize="none"
        value={formData.email}
        onChangeText={(text) => handleChange("email", text)}
        testID="auth-email-input"
      />
      
      <TextInput
        style={styles.input}
        placeholder={t('farmerAuth.passwordPlaceholder')}
        placeholderTextColor="#333"
        secureTextEntry
        value={formData.password}
        onChangeText={(text) => handleChange("password", text)}
        testID="auth-password-input"
      />
      
      <TouchableOpacity
        style={[styles.button, isLoading && styles.buttonDisabled]}
        onPress={handleSubmit}
        disabled={isLoading}
        testID="auth-submit-button"
      >
        <Text style={styles.buttonText}>
          {isLoading ? t('farmerAuth.pleaseWait') : isLogin ? t('farmerAuth.signIn') : t('farmerAuth.signUp')}
        </Text>
      </TouchableOpacity>
      
      <TouchableOpacity 
        style={styles.switchButton}
        onPress={() => setIsLogin(!isLogin)}
        testID="auth-toggle-mode-button"
      >
        <Text style={styles.switchText}>
          {isLogin
            ? t('farmerAuth.needAccount')
            : t('farmerAuth.haveAccount')}
        </Text>
      </TouchableOpacity>

      <TouchableOpacity 
        style={styles.backButton}
        onPress={() => router.back()}
        testID="back-to-welcome-button"
      >
        <Text style={styles.backText}>{t('farmerAuth.backToWelcome')}</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: "flex-start",
    padding: 20,
    backgroundColor: "#ffffff",
  },
  logoSection: {
    flex: 0.4,
    justifyContent: 'center',
    alignItems: 'center',
    paddingTop: 40,
    paddingBottom: 20,
    margintop:40,
    marginBottom: 20,
  },
  logoContainer: {
    width: '80%',
    height: 200,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 20,
  },
  logo: {
    width: '80%',
    height: '80%',
    borderRadius: 10,
  },
  title: {
    fontSize: 28,
    fontWeight: "bold",
    marginBottom: 30,
    textAlign: "center",
    color: "#333",
  },
  input: {
    height: 50,
    borderWidth: 1,
    borderColor: "#ddd",
    borderRadius: 8,
    paddingHorizontal: 15,
    marginBottom: 15,
    backgroundColor: "#fff",
    fontSize: 16,
  },
  button: {
    height: 50,
    borderRadius: 8,
    backgroundColor: "#05408dff",
    justifyContent: "center",
    alignItems: "center",
    marginTop: 10,
  },
  buttonDisabled: {
    backgroundColor: "#cccccc",
  },
  buttonText: {
    color: "#fff",
    fontSize: 18,
    fontWeight: "600",
  },
  switchButton: {
    marginTop: 20,
    padding: 10,
  },
  switchText: {
    color: "#4a89dc",
    textAlign: "center",
    fontSize: 16,
  },
  backButton: {
    marginTop: 15,
    padding: 10,
  },
  backText: {
    color: "#666",
    textAlign: "center",
    fontSize: 14,
  },
});
