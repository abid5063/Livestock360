import React, { useState, useEffect } from "react";
import { View, Text, TextInput, StyleSheet, TouchableOpacity, Alert, ActivityIndicator } from "react-native";
import { useLocalSearchParams, router } from "expo-router";
import AsyncStorage from '@react-native-async-storage/async-storage';
import axios from "axios";
import { API_BASE_URL } from '../utils/apiConfig'; // Adjust the import path as needed
import { useLanguage } from '../utils/LanguageContext';
import { useTranslation } from 'react-i18next';

export default function EditProfile() {
  const params = useLocalSearchParams();
  const farmer = params.farmer ? JSON.parse(params.farmer) : null;
  const { language } = useLanguage();
  const { t, i18n } = useTranslation();

  // Update i18n language when language changes
  useEffect(() => {
    i18n.changeLanguage(language);
  }, [language, i18n]);

  const [formData, setFormData] = useState({
    name: farmer?.name || "",
    email: farmer?.email || "",
    phoneNo: farmer?.phoneNo || "",
    location: farmer?.location || "",
    profileImage: farmer?.profileImage || "",
  });
  const [loading, setLoading] = useState(false);

  const handleInputChange = (key, value) => {
    setFormData({ ...formData, [key]: value });
  };

  const handleEditProfile = async () => {
    if (
      !formData.name.trim() ||
      !formData.email.trim()
    ) {
      Alert.alert(t('editProfile.validation'), t('editProfile.fillRequiredFields'));
      return;
    }
    Alert.alert(
      t('editProfile.saveChangesTitle'),
      t('editProfile.saveChangesMessage'),
      [
        { text: t('editProfile.cancel'), style: 'cancel' },
        {
          text: t('editProfile.save'),
          style: 'default',
          onPress: async () => {
            try {
              setLoading(true);
              const token = await AsyncStorage.getItem('authToken');
              await axios.put(
                `${API_BASE_URL}/api/auth/edit/${farmer._id}`,
                formData,
                {
                  headers: {
                    Authorization: `Bearer ${token}`,
                    'Content-Type': 'application/json'
                  }
                }
              );
              Alert.alert(t('editProfile.success'), t('editProfile.profileUpdated'));
              router.replace({
                pathname: '/profile',
                params: { farmer: JSON.stringify({ ...farmer, ...formData }) }
              });
            } catch (error) {
              Alert.alert(t('editProfile.error'), error.response?.data?.message || t('editProfile.failedToUpdate'));
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
      t('editProfile.deleteProfileTitle'),
      t('editProfile.deleteProfileMessage'),
      [
        { text: t('editProfile.cancel'), style: 'cancel' },
        {
          text: t('editProfile.delete'),
          style: 'destructive',
          onPress: async () => {
            try {
              setLoading(true);
              const token = await AsyncStorage.getItem('authToken');
              console.log(token);
              await axios.delete(
                `${API_BASE_URL}/api/auth/delete/${farmer._id}`,
                {
                  headers: {
                    Authorization: `Bearer ${token}`
                  }
                }
              );
              await AsyncStorage.multiRemove(['authToken', 'userData']);
              Alert.alert(t('editProfile.deleted'), t('editProfile.profileDeleted'));
              router.replace('/');
            } catch (error) {
              Alert.alert(t('editProfile.error'), error.response?.data?.message || t('editProfile.failedToDelete'));
            } finally {
              setLoading(false);
            }
          }
        }
      ]
    );
  };

  if (!farmer) {
    return (
      <View style={styles.container}>
        <Text>{t('editProfile.noFarmerData')}</Text>
        <TouchableOpacity onPress={() => router.replace('/')} testID="go-to-login">
          <Text style={styles.linkText}>{t('editProfile.goToLogin')}</Text>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Text style={styles.title}>{t('editProfile.title')}</Text>
      <TextInput
        style={styles.input}
        placeholder={t('editProfile.namePlaceholder')}
        placeholderTextColor="#333"
        value={formData.name}
        onChangeText={text => handleInputChange('name', text)}
        testID="profile-name-input"
      />
      <TextInput
        style={styles.input}
        placeholder={t('editProfile.emailPlaceholder')}
        placeholderTextColor="#333"
        value={formData.email}
        onChangeText={text => handleInputChange('email', text)}
        keyboardType="email-address"
        autoCapitalize="none"
        testID="profile-email-input"
      />
      <TextInput
        style={styles.input}
        placeholder={t('editProfile.phonePlaceholder')}
        placeholderTextColor="#333"
        value={formData.phoneNo}
        onChangeText={text => handleInputChange('phoneNo', text)}
        keyboardType="phone-pad"
        testID="profile-phone-input"
      />
      <TextInput
        style={styles.input}
        placeholder={t('editProfile.locationPlaceholder')}
        placeholderTextColor="#333"
        value={formData.location}
        onChangeText={text => handleInputChange('location', text)}
        testID="profile-location-input"
      />
      <TextInput
        style={styles.input}
        placeholder={t('editProfile.imageUrlPlaceholder')}
        placeholderTextColor="#333"
        value={formData.profileImage}
        onChangeText={text => handleInputChange('profileImage', text)}
        testID="profile-image-input"
      />

      {loading ? (
        <ActivityIndicator size="large" color="#4a89dc" style={{ marginVertical: 20 }} testID="profile-loading-indicator" />
      ) : (
        <>
          <TouchableOpacity style={styles.saveButton} onPress={handleEditProfile} testID="profile-save-button">
            <Text style={styles.saveButtonText}>{t('editProfile.saveChanges')}</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.deleteButton} onPress={handleDeleteProfile} testID="profile-delete-button">
            <Text style={styles.deleteButtonText}>{t('editProfile.deleteProfile')}</Text>
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