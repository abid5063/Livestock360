import React, { useState, useEffect } from "react";
import { View, Text, TextInput, StyleSheet, TouchableOpacity, Alert, ActivityIndicator } from "react-native";
import { useLocalSearchParams, router } from "expo-router";
import AsyncStorage from '@react-native-async-storage/async-storage';
import axios from "axios";
import { API_BASE_URL } from "../utils/apiConfig";
import { useLanguage } from '../utils/LanguageContext';
import { useTranslation } from 'react-i18next';
export default function VetEditProfile() {
  const params = useLocalSearchParams();
  const { language } = useLanguage();
  const { t, i18n } = useTranslation();

  // Update i18n language when language changes
  useEffect(() => {
    i18n.changeLanguage(language);
  }, [language, i18n]);

  const vet = params.vet ? JSON.parse(params.vet) : null;

  const [formData, setFormData] = useState({
    name: vet?.name || "",
    email: vet?.email || "",
    phoneNo: vet?.phoneNo || "",
    location: vet?.location || "",
    specialty: vet?.specialty || "",
    experience: vet?.experience || "",
    profileImage: vet?.profileImage || "",
  });
  const [loading, setLoading] = useState(false);

  const handleInputChange = (key, value) => {
    setFormData({ ...formData, [key]: value });
  };

  const handleEditProfile = async () => {
    if (
      !formData.name.trim() ||
      !formData.email.trim() ||
      !formData.specialty.trim()
    ) {
      Alert.alert(t('vetEditProfile.validation'), t('vetEditProfile.fillRequiredFields'));
      return;
    }
    Alert.alert(
      t('vetEditProfile.saveChanges'),
      t('vetEditProfile.saveChangesMessage'),
      [
        { text: t('vetEditProfile.cancel'), style: "cancel" },
        {
          text: t('vetEditProfile.save'),
          style: "default",
          onPress: async () => {
            try {
              setLoading(true);
              const token = await AsyncStorage.getItem('authToken');
              await axios.put(
                `${API_BASE_URL}/api/vets/edit/${vet._id}`,
                formData,
                {
                  headers: {
                    Authorization: `Bearer ${token}`,
                    'Content-Type': 'application/json'
                  }
                }
              );
              // Update the stored user data
              const updatedVetData = { ...vet, ...formData };
              await AsyncStorage.setItem('userData', JSON.stringify(updatedVetData));
              Alert.alert(t('vetEditProfile.success'), t('vetEditProfile.profileUpdated'));
              router.replace({
                pathname: '/vetProfile',
                params: { vet: JSON.stringify(updatedVetData) }
              });
            } catch (error) {
              Alert.alert(t('vetEditProfile.error'), error.response?.data?.message || t('vetEditProfile.failedToUpdate'));
            } finally {
              setLoading(false);
            }
          }
        }
      ]
    );
  };

  const handleDeleteProfile = async () => {
    Alert.alert(
      t('vetEditProfile.deleteProfile'),
      t('vetEditProfile.deleteProfileMessage'),
      [
        { text: t('vetEditProfile.cancel'), style: "cancel" },
        {
          text: t('vetEditProfile.delete'),
          style: "destructive",
          onPress: async () => {
            try {
              setLoading(true);
              const token = await AsyncStorage.getItem('authToken');
              await axios.delete(
                `${API_BASE_URL}/api/vets/delete/${vet._id}`,
                {
                  headers: {
                    Authorization: `Bearer ${token}`
                  }
                }
              );
              await AsyncStorage.multiRemove(['authToken', 'userData']);
              Alert.alert(t('vetEditProfile.deleted'), t('vetEditProfile.profileDeleted'));
              router.replace('/');
            } catch (error) {
              Alert.alert(t('vetEditProfile.error'), error.response?.data?.message || t('vetEditProfile.failedToDelete'));
            } finally {
              setLoading(false);
            }
          }
        }
      ]
    );
  };

  if (!vet) {
    return (
      <View style={styles.container}>
        <Text>{t('vetEditProfile.noVetData')}</Text>
        <TouchableOpacity onPress={() => router.replace('/')} testID="go-to-login">
          <Text style={styles.linkText}>{t('vetEditProfile.goToLogin')}</Text>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Text style={styles.title}>{t('vetEditProfile.title')}</Text>
      <TextInput
        style={styles.input}
        placeholder={t('vetEditProfile.namePlaceholder')}
        placeholderTextColor="#333"
        value={formData.name}
        onChangeText={text => handleInputChange('name', text)}
        testID="vet-name-input"
      />
      <TextInput
        style={styles.input}
        placeholder={t('vetEditProfile.emailPlaceholder')}
        placeholderTextColor="#333"
        value={formData.email}
        onChangeText={text => handleInputChange('email', text)}
        keyboardType="email-address"
        autoCapitalize="none"
        testID="vet-email-input"
      />
      <TextInput
        style={styles.input}
        placeholder={t('vetEditProfile.phoneNumberPlaceholder')}
        placeholderTextColor="#333"
        value={formData.phoneNo}
        onChangeText={text => handleInputChange('phoneNo', text)}
        keyboardType="phone-pad"
        testID="vet-phone-input"
      />
      <TextInput
        style={styles.input}
        placeholder={t('vetEditProfile.locationPlaceholder')}
        placeholderTextColor="#333"
        value={formData.location}
        onChangeText={text => handleInputChange('location', text)}
        testID="vet-location-input"
      />
      <TextInput
        style={styles.input}
        placeholder={t('vetEditProfile.specialtyPlaceholder')}
        placeholderTextColor="#333"
        value={formData.specialty}
        onChangeText={text => handleInputChange('specialty', text)}
        testID="vet-specialty-input"
      />
      <TextInput
        style={styles.input}
        placeholder={t('vetEditProfile.experiencePlaceholder')}
        placeholderTextColor="#333"
        value={formData.experience}
        onChangeText={text => handleInputChange('experience', text)}
        keyboardType="numeric"
        testID="vet-experience-input"
      />
      <TextInput
        style={styles.input}
        placeholder={t('vetEditProfile.profileImagePlaceholder')}
        placeholderTextColor="#333"
        value={formData.profileImage}
        onChangeText={text => handleInputChange('profileImage', text)}
        testID="vet-image-input"
      />

      {loading ? (
        <ActivityIndicator size="large" color="#4a89dc" style={{ marginVertical: 20 }} testID="vet-loading-indicator" />
      ) : (
        <>
          <TouchableOpacity style={styles.saveButton} onPress={handleEditProfile} testID="vet-save-button">
            <Text style={styles.saveButtonText}>{t('vetEditProfile.saveChanges')}</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.deleteButton} onPress={handleDeleteProfile} testID="vet-delete-button">
            <Text style={styles.deleteButtonText}>{t('vetEditProfile.deleteProfile')}</Text>
          </TouchableOpacity>
        </>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 24,
    backgroundColor: "#f7f9fa",
    justifyContent: "center",
  },
  title: {
    fontSize: 22,
    fontWeight: "bold",
    marginBottom: 24,
    textAlign: "center",
    color: "#333",
  },
  input: {
    borderWidth: 1,
    borderColor: "#ccc",
    borderRadius: 8,
    padding: 12,
    marginBottom: 14,
    fontSize: 16,
    backgroundColor: "#fafbfc",
  },
  saveButton: {
    backgroundColor: "#4a89dc",
    padding: 14,
    borderRadius: 8,
    alignItems: "center",
    marginBottom: 14,
  },
  saveButtonText: {
    color: "white",
    fontWeight: "bold",
    fontSize: 16,
  },
  deleteButton: {
    backgroundColor: "#e74c3c",
    padding: 14,
    borderRadius: 8,
    alignItems: "center",
  },
  deleteButtonText: {
    color: "white",
    fontWeight: "bold",
    fontSize: 16,
  },
  linkText: {
    color: "#4a89dc",
    marginTop: 20,
    textAlign: "center",
  },
});
