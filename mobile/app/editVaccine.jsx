import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TextInput,
  TouchableOpacity,
  ScrollView,
  Alert,
  ActivityIndicator,
  Platform
} from 'react-native';
import { useRouter, useLocalSearchParams } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import DateTimePicker from '@react-native-community/datetimepicker';
import AsyncStorage from '@react-native-async-storage/async-storage';
import axios from 'axios';
import { API_BASE_URL } from '../utils/apiConfig'; // Adjust the import path as needed
import { useLanguage } from '../utils/LanguageContext';
import { useTranslation } from 'react-i18next';

export default function EditVaccine() {
  const router = useRouter();
  const { vaccineId } = useLocalSearchParams();
  const { language } = useLanguage();
  const { t, i18n } = useTranslation();

  // Update i18n language when language changes
  useEffect(() => {
    i18n.changeLanguage(language);
  }, [language, i18n]);

  const [animals, setAnimals] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const [formData, setFormData] = useState({
    vaccine_name: '',
    animal_id: '',
    vaccine_date: new Date(),
    next_due_date: '',
    notes: ''
  });
  const [showVaccineDatePicker, setShowVaccineDatePicker] = useState(false);
  const [showNextDueDatePicker, setShowNextDueDatePicker] = useState(false);

  useEffect(() => {
    fetchData();
  }, [vaccineId]);

  const fetchData = async () => {
    try {
      const token = await AsyncStorage.getItem('authToken');
      
      // Fetch both animals and vaccine details
      const [animalsResponse, vaccineResponse] = await Promise.all([
        axios.get(`${API_BASE_URL}/api/animals`, {
          headers: { Authorization: `Bearer ${token}` }
        }),
        axios.get(`${API_BASE_URL}/api/vaccines/${vaccineId}`, {
          headers: { Authorization: `Bearer ${token}` }
        })
      ]);

      setAnimals(animalsResponse.data);
      
      const vaccine = vaccineResponse.data;
      setFormData({
        vaccine_name: vaccine.vaccine_name,
        animal_id: vaccine.animal._id,
        vaccine_date: vaccine.vaccine_date ? new Date(vaccine.vaccine_date) : new Date(),
        next_due_date: vaccine.next_due_date ? new Date(vaccine.next_due_date) : '',
        notes: vaccine.notes || ''
      });
    } catch (error) {
      console.error('Error fetching data:', error);
      Alert.alert(t('editVaccine.error'), t('editVaccine.failedToLoad'));
      router.back();
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (field, value) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));
  };

  const formatDate = (date) => {
    if (!date) return '';
    if (typeof date === 'string') date = new Date(date);
    return date.toLocaleDateString('en-US', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  const validateForm = () => {
    if (!formData.vaccine_name.trim()) {
      Alert.alert(t('editVaccine.validationError'), t('editVaccine.enterVaccineName'));
      return false;
    }
    if (!formData.animal_id) {
      Alert.alert(t('editVaccine.validationError'), t('editVaccine.selectAnimal'));
      return false;
    }
    // Validate vaccine_date
    if (!(formData.vaccine_date instanceof Date) || isNaN(formData.vaccine_date)) {
      Alert.alert(t('editVaccine.validationError'), t('editVaccine.selectValidVaccineDate'));
      return false;
    }
    // Validate next_due_date if provided
    if (formData.next_due_date && (!(formData.next_due_date instanceof Date) || isNaN(formData.next_due_date))) {
      Alert.alert(t('editVaccine.validationError'), t('editVaccine.selectValidNextDueDate'));
      return false;
    }
    return true;
  };

  const handleSubmit = async () => {
    if (!validateForm()) return;

    try {
      setSaving(true);
      const token = await AsyncStorage.getItem('authToken');
      
      const requestData = {
        vaccine_name: formData.vaccine_name.trim(),
        animal_id: formData.animal_id,
        vaccine_date: formData.vaccine_date instanceof Date ? formData.vaccine_date.toISOString().split('T')[0] : formData.vaccine_date,
        notes: formData.notes.trim()
      };
      if (formData.next_due_date) {
        requestData.next_due_date = formData.next_due_date instanceof Date ? formData.next_due_date.toISOString().split('T')[0] : formData.next_due_date;
      }

      await axios.put(`${API_BASE_URL}/api/vaccines/${vaccineId}`, requestData, {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      // Navigate directly to vaccine management without alert
      router.replace('/vaccineManagement');
    } catch (error) {
      console.error('Error updating vaccine:', error);
      Alert.alert(t('editVaccine.error'), error.response?.data?.message || t('editVaccine.failedToUpdate'));
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#4a89dc" />
        <Text style={styles.loadingText}>{t('editVaccine.loadingVaccineDetails')}</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      {/* Header with logo */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.canGoBack() ? router.back() : router.push('/vaccineManagement')}>
          <Ionicons name="arrow-back" size={28} color="#3498db" />
        </TouchableOpacity>
        <View style={styles.logoContainer}>
          <Ionicons name="medkit" size={28} color="#3498db" />
        </View>
        <Text style={styles.headerTitle}>{t('editVaccine.headerTitle')}</Text>
        <View style={{ width: 28 }} />
      </View>

      <ScrollView style={styles.content}>
        {/* Vaccine Name */}
        <View style={styles.inputGroup}>
          <Text style={styles.label}>{t('editVaccine.vaccineName')}</Text>
          <TextInput
            style={styles.input}
            placeholder={t('editVaccine.vaccineNamePlaceholder')}
            value={formData.vaccine_name}
            onChangeText={(value) => handleInputChange('vaccine_name', value)}
          />
        </View>

        {/* Animal Selection */}
        <View style={styles.inputGroup}>
          <Text style={styles.label}>{t('editVaccine.selectAnimal')}</Text>
          <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.animalSelector}>
            {animals.map((animal) => (
              <TouchableOpacity
                key={animal._id}
                style={[
                  styles.animalCard,
                  formData.animal_id === animal._id && styles.selectedAnimalCard
                ]}
                onPress={() => handleInputChange('animal_id', animal._id)}
              >
                <Text style={[
                  styles.animalName,
                  formData.animal_id === animal._id && styles.selectedAnimalName
                ]}>
                  {animal.name}
                </Text>
                <Text style={[
                  styles.animalType,
                  formData.animal_id === animal._id && styles.selectedAnimalType
                ]}>
                  {animal.type}
                </Text>
              </TouchableOpacity>
            ))}
          </ScrollView>
        </View>

        {/* Vaccine Date */}
        <View style={styles.inputGroup}>
          <Text style={styles.label}>{t('editVaccine.vaccineDate')}</Text>
          <TouchableOpacity
            style={styles.dateButton}
            onPress={() => setShowVaccineDatePicker(true)}
          >
            <Text style={styles.dateText}>{formatDate(formData.vaccine_date)}</Text>
            <Ionicons name="calendar" size={20} color="#3498db" style={{ marginLeft: 8 }} />
          </TouchableOpacity>
        </View>

        {/* Next Due Date */}
        <View style={styles.inputGroup}>
          <Text style={styles.label}>{t('editVaccine.nextDueDate')}</Text>
          <TouchableOpacity
            style={styles.dateButton}
            onPress={() => setShowNextDueDatePicker(true)}
          >
            <Text style={styles.dateText}>{formData.next_due_date ? formatDate(formData.next_due_date) : t('editVaccine.selectNextDueDate')}</Text>
            <Ionicons name="calendar" size={20} color="#3498db" style={{ marginLeft: 8 }} />
          </TouchableOpacity>
          {formData.next_due_date && (
            <TouchableOpacity
              style={styles.clearButton}
              onPress={() => handleInputChange('next_due_date', '')}
            >
              <Text style={styles.clearButtonText}>{t('editVaccine.clearDate')}</Text>
            </TouchableOpacity>
          )}
        </View>

        {/* Notes */}
        <View style={styles.inputGroup}>
          <Text style={styles.label}>{t('editVaccine.notes')}</Text>
          <TextInput
            style={[styles.input, styles.textArea]}
            placeholder={t('editVaccine.notesPlaceholder')}
            value={formData.notes}
            onChangeText={(value) => handleInputChange('notes', value)}
            multiline
            numberOfLines={4}
          />
        </View>

        {/* Submit Button */}
        <TouchableOpacity
          style={[styles.submitButton, saving && styles.submitButtonDisabled]}
          onPress={handleSubmit}
          disabled={saving}
        >
          <Ionicons name="medkit" size={20} color="#fff" style={styles.submitButtonIcon} />
          {saving ? (
            <ActivityIndicator color="#fff" />
          ) : (
            <Text style={styles.submitButtonText}>{t('editVaccine.updateVaccineRecord')}</Text>
          )}
        </TouchableOpacity>
      </ScrollView>
      {/* Vaccine Date Picker */}
      {showVaccineDatePicker && (
        <DateTimePicker
          value={formData.vaccine_date instanceof Date ? formData.vaccine_date : new Date()}
          mode="date"
          display="default"
          onChange={(event, selectedDate) => {
            setShowVaccineDatePicker(false);
            if (selectedDate) handleInputChange('vaccine_date', selectedDate);
          }}
          minimumDate={new Date('2000-01-01')}
        />
      )}
      {/* Next Due Date Picker */}
      {showNextDueDatePicker && (
        <DateTimePicker
          value={formData.next_due_date instanceof Date ? formData.next_due_date : new Date()}
          mode="date"
          display="default"
          onChange={(event, selectedDate) => {
            setShowNextDueDatePicker(false);
            if (selectedDate) handleInputChange('next_due_date', selectedDate);
          }}
          minimumDate={new Date('2000-01-01')}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8f9fa',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingTop: 50,
    paddingBottom: 16,
    paddingHorizontal: 16,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  logoContainer: {
    marginRight: 8,
  },
  headerTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#3498db',
    flex: 1,
    textAlign: 'center',
  },
  content: {
    flex: 1,
    padding: 16,
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#f8f9fa',
  },
  loadingText: {
    marginTop: 16,
    fontSize: 16,
    color: '#666',
  },
  inputGroup: {
    marginBottom: 20,
  },
  label: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
    marginBottom: 8,
  },
  input: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
    backgroundColor: '#fff',
  },
  dateInput: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
    backgroundColor: '#fff',
  },
  textArea: {
    height: 100,
    textAlignVertical: 'top',
  },
  animalSelector: {
    flexDirection: 'row',
  },
  animalCard: {
    backgroundColor: '#fff',
    borderRadius: 8,
    padding: 12,
    marginRight: 12,
    minWidth: 100,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#ddd',
  },
  selectedAnimalCard: {
    backgroundColor: '#4a89dc',
    borderColor: '#4a89dc',
  },
  animalName: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333',
  },
  selectedAnimalName: {
    color: '#fff',
  },
  animalType: {
    fontSize: 12,
    color: '#666',
    marginTop: 4,
  },
  selectedAnimalType: {
    color: '#fff',
  },
  dateButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    gap: 8,
  },
  dateText: {
    fontSize: 16,
    color: '#333',
  },
  clearButton: {
    marginTop: 8,
    alignSelf: 'flex-start',
  },
  clearButtonText: {
    color: '#e74c3c',
    fontSize: 14,
  },
  submitButton: {
    backgroundColor: '#3498db',
    borderRadius: 12,
    padding: 16,
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: 20,
    marginBottom: 40,
  },
  submitButtonIcon: {
    marginRight: 8,
  },
  submitButtonDisabled: {
    backgroundColor: '#ccc',
  },
  submitButtonText: {
    color: '#fff',
    fontSize: 18,
    fontWeight: 'bold',
  },
});
