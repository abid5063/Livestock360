import React, { useState } from "react";
import { View, Text, StyleSheet, Image, TouchableOpacity, ScrollView, Modal, TextInput, Alert, ActivityIndicator } from "react-native";
import { useLocalSearchParams, router } from "expo-router";
import AsyncStorage from '@react-native-async-storage/async-storage';
import axios from "axios";
import { API_BASE_URL } from '../utils/apiConfig';
import { useLanguage } from '../utils/LanguageContext';
import { useTranslation } from 'react-i18next';

export default function AnimalDetails() {
  const params = useLocalSearchParams();
  const animal = params.animal ? JSON.parse(params.animal) : null;
  const { language } = useLanguage();
  const { t, i18n } = useTranslation();

  // Update i18n language when language changes
  React.useEffect(() => {
    i18n.changeLanguage(language);
  }, [language, i18n]);

  const [editModalVisible, setEditModalVisible] = useState(false);
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    name: animal?.name || "",
    type: animal?.type || "",
    breed: animal?.breed || "",
    age: animal?.age ? animal.age.toString() : "",
    gender: animal?.gender || "",
    details: animal?.details || "",
  });

  const handleInputChange = (key, value) => {
    setFormData({ ...formData, [key]: value });
  };

  const handleEditAnimal = async () => {
    if (
      !formData.name.trim() ||
      !formData.type.trim() ||
      !formData.breed.trim() ||
      !formData.age.trim() ||
      !formData.gender.trim()
    ) {
      Alert.alert(t('alerts.validation'), t('alerts.fillRequiredFields'));
      return;
    }
    try {
      setLoading(true);
      const token = await AsyncStorage.getItem('authToken');      
      const response = await axios.put(
        `${API_BASE_URL}/api/animals/${animal._id}`,
        {
          ...formData,
          age: Number(formData.age),
        },
        {
          headers: {
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        }
      );
      Alert.alert(t('alerts.success'), t('alerts.animalUpdatedSuccess'));
      setEditModalVisible(false);
      router.replace({
        pathname: '/profile'
      });
    } catch (error) {
      Alert.alert(t('alerts.error'), error.response?.data?.message || t('alerts.failedToUpdateAnimal'));
    } finally {
      setLoading(false);
    }
  };

 const handleDeleteAnimal = () => {
  Alert.alert(
    t('animalDetails.deleteAnimal'),
    t('animalDetails.deleteConfirmation'),
    [
      { text: t('animalDetails.cancel'), style: "cancel" },
      {
        text: t('animalDetails.delete'),
        style: "destructive",
        onPress: () => {
          actuallyDeleteAnimal();
        }
      }
    ]
  );
};

const actuallyDeleteAnimal = async () => {
  try {
    setLoading(true);
    const token = await AsyncStorage.getItem('authToken');    
    await axios.delete(
      `${API_BASE_URL}/api/animals/${animal._id}`,
      {
        headers: {
          Authorization: `Bearer ${token}`
        }
      }
    );
    Alert.alert(t('animalDetails.deleted'), t('animalDetails.animalRemovedSuccess'));
    router.replace('/profile');
  } catch (error) {
    Alert.alert(t('alerts.error'), error.response?.data?.message || t('animalDetails.failedToDeleteAnimal'));
  } finally {
    setLoading(false);
  }
};

  if (!animal) {
    return (
      <View style={styles.container}>
        <Text style={styles.errorText}>{t('animalDetails.noAnimalData')}</Text>
        <TouchableOpacity onPress={() => router.back()}>
          <Text style={styles.backText} testID="error-back-button">{t('animalDetails.goBack')}</Text>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <ScrollView contentContainerStyle={styles.scrollContainer}>
      <View style={styles.container}>
        <TouchableOpacity onPress={() => router.back()}>
          <Text style={styles.backText} testID="back-button">{t('animalDetails.back')}</Text>
        </TouchableOpacity>
        <View style={styles.imageContainer}>
          {animal.photo_url ? (
            <Image source={{ uri: animal.photo_url }} style={styles.animalImage} />
          ) : (
            <View style={[styles.animalImage, { backgroundColor: "#e0e0e0", justifyContent: "center", alignItems: "center" }]}>
              <Text style={{ color: "#aaa" }}>{t('animalDetails.noImage')}</Text>
            </View>
          )}
        </View>
        <Text style={styles.animalName}>{animal.name}</Text>
        <Text style={styles.detailText}>{t('animalDetails.type')}: {animal.type}</Text>
        <Text style={styles.detailText}>{t('animalDetails.breed')}: {animal.breed}</Text>
        <Text style={styles.detailText}>{t('animalDetails.age')}: {animal.age} {t('animalDetails.years')}</Text>
        <Text style={styles.detailText}>{t('animalDetails.gender')}: {animal.gender}</Text>
        {animal.details ? (
          <Text style={styles.detailText}>{t('animalDetails.details')}: {animal.details}</Text>
        ) : null}

        <View style={{ flexDirection: "row", marginTop: 24, gap: 16 }}>
          <TouchableOpacity style={styles.editButton} onPress={() => setEditModalVisible(true)} testID="animal-edit-button">
            <Text style={styles.editButtonText}>{t('animalDetails.edit')}</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.deleteButton} onPress={handleDeleteAnimal} testID="animal-delete-button">
            <Text style={styles.deleteButtonText}>{t('animalDetails.delete')}</Text>
          </TouchableOpacity>
        </View>

        {/* Edit Modal */}
        <Modal
          animationType="slide"
          transparent={true}
          visible={editModalVisible}
          onRequestClose={() => setEditModalVisible(false)}
        >
          <View style={styles.modalContainer}>
            <View style={styles.modalContent}>
              <Text style={styles.modalTitle}>{t('animalDetails.editAnimal')}</Text>
              <TextInput
                style={styles.input}
                placeholder={t('animalDetails.name')}
                value={formData.name}
                onChangeText={text => handleInputChange('name', text)}
                testID="animal-name-input"
              />
              <TextInput
                style={styles.input}
                placeholder={t('animalDetails.type')}
                value={formData.type}
                onChangeText={text => handleInputChange('type', text)}
              />
              <TextInput
                style={styles.input}
                placeholder={t('animalDetails.breed')}
                value={formData.breed}
                onChangeText={text => handleInputChange('breed', text)}
              />
              <TextInput
                style={styles.input}
                placeholder={t('animalDetails.agePlaceholder')}
                value={formData.age}
                onChangeText={text => handleInputChange('age', text)}
                keyboardType="numeric"
              />
              <TextInput
                style={styles.input}
                placeholder={t('animalDetails.genderPlaceholder')}
                value={formData.gender}
                onChangeText={text => handleInputChange('gender', text)}
              />
              <TextInput
                style={[styles.input, { height: 80 }]}
                placeholder={t('animalDetails.detailsPlaceholder')}
                value={formData.details}
                onChangeText={text => handleInputChange('details', text)}
                multiline
              />
              {loading ? (
                <ActivityIndicator size="large" color="#4a89dc" style={{ marginVertical: 20 }} testID="animal-loading-indicator" />
              ) : (
                <View style={styles.modalButtons}>
                  <TouchableOpacity style={[styles.modalButton, styles.cancelButton]} onPress={() => setEditModalVisible(false)} testID="animal-cancel-button">
                    <Text style={styles.cancelButtonText}>{t('animalDetails.cancel')}</Text>
                  </TouchableOpacity>
                  <TouchableOpacity style={[styles.modalButton, styles.saveButton]} onPress={handleEditAnimal} testID="animal-save-button">
                    <Text style={styles.saveButtonText}>{t('animalDetails.save')}</Text>
                  </TouchableOpacity>
                </View>
              )}
            </View>
          </View>
        </Modal>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  scrollContainer: {
    flexGrow: 1,
    justifyContent: "center",
    alignItems: "center",
    paddingVertical: 30,
    backgroundColor: "#f7f9fa",
  },
  container: {
    width: "90%",
    alignSelf: "center",
    backgroundColor: "#fff",
    borderRadius: 12,
    padding: 20,
    alignItems: "center",
    elevation: 2,
  },
  imageContainer: {
    marginBottom: 20,
    alignItems: "center",
  },
  animalImage: {
    width: 160,
    height: 160,
    borderRadius: 16,
    marginBottom: 10,
    backgroundColor: "#e0e0e0",
  },
  animalName: {
    fontSize: 24,
    fontWeight: "bold",
    marginBottom: 10,
    color: "#333",
    textAlign: "center",
  },
  detailText: {
    fontSize: 16,
    color: "#555",
    marginBottom: 6,
    textAlign: "center",
  },
  errorText: {
    color: "#e74c3c",
    fontSize: 18,
    marginBottom: 20,
    textAlign: "center",
  },
  backText: {
    color: "#4a89dc",
    fontSize: 16,
    marginBottom: 20,
    textAlign: "left",
  },
  editButton: {
    backgroundColor: "#4a89dc",
    paddingVertical: 10,
    paddingHorizontal: 24,
    borderRadius: 8,
    alignItems: "center",
  },
  editButtonText: {
    color: "white",
    fontWeight: "bold",
    fontSize: 16,
  },
  deleteButton: {
    backgroundColor: "#e74c3c",
    paddingVertical: 10,
    paddingHorizontal: 24,
    borderRadius: 8,
    alignItems: "center",
  },
  deleteButtonText: {
    color: "white",
    fontWeight: "bold",
    fontSize: 16,
  },
  modalContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'rgba(0,0,0,0.5)',
  },
  modalContent: {
    width: '90%',
    backgroundColor: 'white',
    borderRadius: 10,
    padding: 20,
  },
  modalTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    marginBottom: 20,
    textAlign: 'center',
  },
  input: {
    borderWidth: 1,
    borderColor: "#ccc",
    borderRadius: 8,
    padding: 10,
    marginBottom: 12,
    fontSize: 15,
    backgroundColor: "#fafbfc",
  },
  modalButtons: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 20,
  },
  modalButton: {
    padding: 12,
    borderRadius: 8,
    width: '48%',
    alignItems: 'center',
  },
  cancelButton: {
    backgroundColor: '#e0e0e0',
  },
  saveButton: {
    backgroundColor: '#4a89dc',
  },
  saveButtonText: {
    color: 'white',
    fontWeight: 'bold',
  },
  cancelButtonText: {
    color: '#333',
    fontWeight: 'bold',
  },
});