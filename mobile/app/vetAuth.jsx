import { useState, useEffect, useCallback } from "react";
import { Text, View, TextInput, TouchableOpacity, StyleSheet, Alert, Image } from "react-native";
import axios from "axios";
import AsyncStorage from '@react-native-async-storage/async-storage';
import { useRouter, useFocusEffect } from "expo-router";
import { API_BASE_URL } from '../utils/apiConfig'; // Adjust the import path as needed
import { useLanguage } from '../utils/LanguageContext';
import { useTranslation } from 'react-i18next';
export default function VetAuthScreen() {
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
    specialty: "",
    licenseNumber: "",
    phoneNo: "",
    location: "",
  });
  const [isLoading, setIsLoading] = useState(false);

  // const API_BASE_URL = "http://localhost:3000/api/vets";

  // Reset form when screen is focused (e.g., after logout)
  useFocusEffect(
    useCallback(() => {
      setIsLogin(true);
      setFormData({
        name: "",
        email: "",
        password: "",
        specialty: "",
        licenseNumber: "",
        phoneNo: "",
        location: "",
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

  const storeAuthData = async (token, vet) => {
    try {
      await AsyncStorage.multiSet([
        ['authToken', token],
        ['userData', JSON.stringify(vet)],
        ['userType', 'vet']
      ]);
    } catch (error) {
      console.error("Error storing auth data:", error);
      throw error;
    }
  };

  const handleSubmit = async () => {
    try {
      setIsLoading(true);
      const { name, email, password, specialty, licenseNumber, phoneNo, location } = formData;

      // Validation
      if (!email || !password) {
        Alert.alert(t('vetAuth.error'), t('vetAuth.emailPasswordRequired'));
        return;
      }

      if (!isLogin) {
        if (!name) {
          Alert.alert(t('vetAuth.error'), t('vetAuth.nameRequired'));
          return;
        }
        if (!specialty) {
          Alert.alert(t('vetAuth.error'), t('vetAuth.specialtyRequired'));
          return;
        }
        if (!licenseNumber) {
          Alert.alert(t('vetAuth.error'), t('vetAuth.licenseNumberRequired'));
          return;
        }
        if (!phoneNo) {
          Alert.alert(t('vetAuth.error'), t('vetAuth.phoneNumberRequired'));
          return;
        }
        if (!location) {
          Alert.alert(t('vetAuth.error'), t('vetAuth.locationRequired'));
          return;
        }
        if (password.length < 6) {
          Alert.alert(t('vetAuth.error'), t('vetAuth.passwordLength'));
          return;
        }
        if (name.length < 3) {
          Alert.alert(t('vetAuth.error'), t('vetAuth.nameLength'));
          return;
        }
      }

      const endpoint = isLogin ? "/login" : "/register";
      const response = await axios.post(`${API_BASE_URL}/api/vets${endpoint}`, formData);

      const { token, vet } = response.data;
      await storeAuthData(token, vet);
      
      // Navigate to vet profile
      router.push({
        pathname: "/vetProfile",
        params: { vet: JSON.stringify(vet) }
      });

    } catch (error) {
      console.error("Auth error:", error.response?.data || error.message);
      const errorMessage = error.response?.data?.message || 
                         error.message || 
                         t('vetAuth.somethingWentWrong');
      Alert.alert(t('vetAuth.error'), errorMessage);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <View style={styles.logoSection}>
        <View style={styles.logoContainer}>
          <Image 
            source={require('../assets/images/vet.avif')}
            style={styles.logo}
            resizeMode="contain"
          />
        </View>
      </View>
      <Text style={styles.title}>{t('vetAuth.title', { mode: isLogin ? t('vetAuth.login') : t('vetAuth.registration') })}</Text>
      
      {!isLogin && (
        <>
          <TextInput
            style={styles.input}
            placeholder={t('vetAuth.fullNamePlaceholder')}
            placeholderTextColor="#333"
            value={formData.name}
            onChangeText={(text) => handleChange("name", text)}
            autoCapitalize="words"
            testID="vet-name-input"
          />
          
          <TextInput
            style={styles.input}
            placeholder={t('vetAuth.specialtyPlaceholder')}
            placeholderTextColor="#333"
            value={formData.specialty}
            onChangeText={(text) => handleChange("specialty", text)}
            testID="vet-specialty-input"
          />
          
          <TextInput
            style={styles.input}
            placeholder={t('vetAuth.licenseNumberPlaceholder')}
            placeholderTextColor="#333"
            value={formData.licenseNumber}
            onChangeText={(text) => handleChange("licenseNumber", text)}
            testID="vet-license-input"
          />
          
          <TextInput
            style={styles.input}
            placeholder={t('vetAuth.phoneNumberPlaceholder')}
            placeholderTextColor="#333"
            value={formData.phoneNo}
            onChangeText={(text) => handleChange("phoneNo", text)}
            keyboardType="phone-pad"
            testID="vet-phone-input"
          />
          
          <TextInput
            style={styles.input}
            placeholder={t('vetAuth.locationPlaceholder')}
            placeholderTextColor="#333"
            value={formData.location}
            onChangeText={(text) => handleChange("location", text)}
            testID="vet-location-input"
          />
        </>
      )}
      
      <TextInput
        style={styles.input}
        placeholder={t('vetAuth.emailPlaceholder')}
        placeholderTextColor="#333"
        keyboardType="email-address"
        autoCapitalize="none"
        value={formData.email}
        onChangeText={(text) => handleChange("email", text)}
        testID="vet-email-input"
      />
      
      <TextInput
        style={styles.input}
        placeholder={t('vetAuth.passwordPlaceholder')}
        placeholderTextColor="#333"
        secureTextEntry
        value={formData.password}
        onChangeText={(text) => handleChange("password", text)}
        testID="vet-password-input"
      />
      
      <TouchableOpacity
        style={[styles.button, isLoading && styles.buttonDisabled]}
        onPress={handleSubmit}
        disabled={isLoading}
        testID="vet-submit-button"
      >
        <Text style={styles.buttonText}>
          {isLoading ? t('vetAuth.pleaseWait') : isLogin ? t('vetAuth.signIn') : t('vetAuth.signUp')}
        </Text>
      </TouchableOpacity>
      
      <TouchableOpacity 
        style={styles.switchButton}
        onPress={() => setIsLogin(!isLogin)}
        testID="vet-toggle-mode-button"
      >
        <Text style={styles.switchText}>
          {isLogin
            ? t('vetAuth.needAccount')
            : t('vetAuth.haveAccount')}
        </Text>
      </TouchableOpacity>

      <TouchableOpacity 
        style={styles.backButton}
        onPress={() => router.back()}
        testID="back-to-welcome-button"
      >
        <Text style={styles.backText}>{t('vetAuth.backToWelcome')}</Text>
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
    margintop: 40,
    marginBottom: 20,
  },
  logoContainer: {
    width: '70%',
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
    fontSize: 24,
    fontWeight: "bold",
    marginBottom: 30,
    textAlign: "center",
    color: "#333",
  },
  input: {
    backgroundColor: '#fff',
    color: '#333',
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
    marginBottom: 15,
  },
  button: {
    height: 50,
    borderRadius: 8,
    backgroundColor: "#27ae60",
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
    color: "#27ae60",
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
