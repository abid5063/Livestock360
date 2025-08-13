import React, { useState, useEffect } from 'react';
import DateTimePicker from '@react-native-community/datetimepicker';
import { 
  View, 
  Text, 
  TextInput, 
  TouchableOpacity, 
  StyleSheet, 
  ScrollView, 
  Alert,
  Platform 
} from 'react-native';
import { router } from 'expo-router';
import AsyncStorage from '@react-native-async-storage/async-storage';
import axios from 'axios';
import { API_BASE_URL } from '../utils/apiConfig';
import { Ionicons } from '@expo/vector-icons';
import { useLanguage } from '../utils/LanguageContext';
import { useTranslation } from 'react-i18next';

const AddTask = () => {
  const { language } = useLanguage();
  const { t, i18n } = useTranslation();

  // Update i18n language when language changes
  React.useEffect(() => {
    i18n.changeLanguage(language);
  }, [language, i18n]);

  const [formData, setFormData] = useState({
    title: '',
    description: '',
    dueDate: new Date().toISOString().split('T')[0], // YYYY-MM-DD format
    dueTime: '09:00',
    estimatedCost: '',
    priority: 'medium',
    category: ['other'], // Changed to array to support multiple categories
    animal: '',
    notes: ''
  });

  const [showDatePicker, setShowDatePicker] = useState(false);
  const [showTimePicker, setShowTimePicker] = useState(false);

  const [animals, setAnimals] = useState([]);
  const [loading, setLoading] = useState(false);

  const priorities = [
    { key: 'low', label: t('addTask.low'), color: '#106310ff' },
    { key: 'medium', label: t('addTask.medium'), color: '#bb7f07ff' },
    { key: 'high', label: t('addTask.high'), color: '#9b0202ff' }
  ];

  const categories = [
    { key: 'feeding', label: t('addTask.feeding'), icon: 'restaurant' },
    { key: 'vaccination', label: t('addTask.vaccination'), icon: 'medical' },
    { key: 'health-check', label: t('addTask.healthCheck'), icon: 'heart' },
    { key: 'breeding', label: t('addTask.breeding'), icon: 'heart-circle' },
    { key: 'maintenance', label: t('addTask.maintenance'), icon: 'construct' },
    { key: 'other', label: t('addTask.other'), icon: 'clipboard' }
  ];

  // Fetch animals when component mounts
  useEffect(() => {
    fetchAnimals();
  }, []);

  const fetchAnimals = async () => {
    try {
      const token = await AsyncStorage.getItem('authToken');
      if (!token) {
        router.replace('/');
        return;
      }

      const response = await axios.get(`${API_BASE_URL}/api/animals`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setAnimals(response.data);
    } catch (error) {
      console.error('Error fetching animals:', error);
      console.error('Error response:', error.response?.data);
      console.error('Error status:', error.response?.status);
      // Show user-friendly error message
      if (error.response?.status === 401) {
        Alert.alert(t('addTask.authenticationError'), t('addTask.pleaseLoginAgain'));
        router.replace('/');
      } else if (error.response?.status === 500) {
        Alert.alert(t('addTask.serverError'), t('addTask.unableToFetchAnimals'));
      } else {
        Alert.alert(t('addTask.error'), t('addTask.failedToFetchAnimals'));
      }
    }
  };

  const handleInputChange = (field, value) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));
  };

  const handleCategoryToggle = (categoryKey) => {
    setFormData(prev => {
      const currentCategories = prev.category;
      if (currentCategories.includes(categoryKey)) {
        // Remove category if already selected
        const newCategories = currentCategories.filter(cat => cat !== categoryKey);
        // Ensure at least one category is selected
        return {
          ...prev,
          category: newCategories.length > 0 ? newCategories : ['other']
        };
      } else {
        // Add category if not selected
        return {
          ...prev,
          category: [...currentCategories, categoryKey]
        };
      }
    });
  };

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', { 
      weekday: 'long',
      year: 'numeric', 
      month: 'long', 
      day: 'numeric' 
    });
  };

  const formatTime = (timeString) => {
    const [hour, minute] = timeString.split(':');
    const date = new Date();
    date.setHours(hour, minute);
    return date.toLocaleTimeString('en-US', {
      hour: 'numeric',
      minute: '2-digit',
      hour12: true,
    });
  };

  const validateForm = () => {
    if (!formData.title.trim()) {
      Alert.alert(t('addTask.validationError'), t('addTask.enterTaskTitle'));
      return false;
    }
    
    // Validate date format (YYYY-MM-DD)
    const datePattern = /^\d{4}-\d{2}-\d{2}$/;
    if (!datePattern.test(formData.dueDate)) {
      Alert.alert(t('addTask.validationError'), t('addTask.enterValidDate'));
      return false;
    }
    
    // Validate time format (HH:MM)
    if (!formData.dueTime.match(/^([01]?[0-9]|2[0-3]):[0-5][0-9]$/)) {
      Alert.alert(t('addTask.validationError'), t('addTask.enterValidTime'));
      return false;
    }

    if (formData.estimatedCost && isNaN(parseFloat(formData.estimatedCost))) {
      Alert.alert(t('addTask.validationError'), t('addTask.enterValidCost'));
      return false;
    }

    return true;
  };

  const handleSubmit = async () => {
    if (!validateForm()) return;

    setLoading(true);
    try {
      const token = await AsyncStorage.getItem('authToken');
      if (!token) {
        router.replace('/');
        return;
      }

      // Send only the first selected category to backend (since backend expects single category)
      const primaryCategory = formData.category[0] || 'other';
      
      const taskData = {
        ...formData,
        category: primaryCategory, // Send only the first category to backend
        estimatedCost: formData.estimatedCost ? parseFloat(formData.estimatedCost) : 0,
        animal: formData.animal || null
      };

      await axios.post(`${API_BASE_URL}/api/tasks`, taskData, {
        headers: { Authorization: `Bearer ${token}` }
      });

      router.replace('/taskManagement');
    } catch (error) {
      console.error('Error creating task:', error);
      Alert.alert(t('addTask.error'), error.response?.data?.message || t('addTask.failedToCreateTask'));
    } finally {
      setLoading(false);
    }
  };

  const renderPrioritySelector = () => (
    <View style={styles.selectorContainer}>
      <Text style={styles.label}>{t('addTask.priority')}</Text>
      <View style={styles.priorityContainer}>
        {priorities.map(priority => (
          <TouchableOpacity
            key={priority.key}
            style={[
              styles.priorityButton,
              { borderColor: priority.color },
              formData.priority === priority.key && { backgroundColor: priority.color }
            ]}
            onPress={() => handleInputChange('priority', priority.key)}
          >
            <Text style={[
              styles.priorityText,
              formData.priority === priority.key && { color: '#fff' }
            ]}>
              {priority.label}
            </Text>
          </TouchableOpacity>
        ))}
      </View>
    </View>
  );

  const renderCategorySelector = () => (
    <View style={styles.selectorContainer}>
      <Text style={styles.label}>
        {t('addTask.category')} 
        {formData.category.length > 1 && ` (${t('addTask.primaryCategory')}: ${categories.find(cat => cat.key === formData.category[0])?.label})`}
      </Text>
      <ScrollView horizontal showsHorizontalScrollIndicator={false}>
        <View style={styles.categoryContainer}>
          {categories.map(category => (
            <TouchableOpacity
              key={category.key}
              style={[
                styles.categoryButton,
                formData.category.includes(category.key) && styles.selectedCategory
              ]}
              onPress={() => handleCategoryToggle(category.key)}
            >
              <Ionicons 
                name={category.icon} 
                size={20} 
                color={formData.category.includes(category.key) ? '#fff' : '#666'} 
              />
              <Text style={[
                styles.categoryText,
                formData.category.includes(category.key) && styles.selectedCategoryText
              ]}>
                {category.label}
              </Text>
              {formData.category.includes(category.key) && (
                <Ionicons 
                  name={formData.category[0] === category.key ? "star" : "checkmark-circle"} 
                  size={16} 
                  color="#fff" 
                  style={{ marginLeft: 4 }} 
                />
              )}
            </TouchableOpacity>
          ))}
        </View>
      </ScrollView>
    </View>
  );

  const renderAnimalSelector = () => (
    <View style={styles.selectorContainer}>
      <Text style={styles.label}>{t('addTask.animalOptional')}</Text>
      <View style={styles.animalContainer}>
        <TouchableOpacity
          style={[
            styles.animalButton,
            !formData.animal && styles.selectedAnimal
          ]}
          onPress={() => handleInputChange('animal', '')}
        >
          <Text style={[
            styles.animalText,
            !formData.animal && styles.selectedAnimalText
          ]}>
            {t('addTask.noSpecificAnimal')}
          </Text>
        </TouchableOpacity>
        
        {animals.map(animal => (
          <TouchableOpacity
            key={animal._id}
            style={[
              styles.animalButton,
              formData.animal === animal._id && styles.selectedAnimal
            ]}
            onPress={() => handleInputChange('animal', animal._id)}
          >
            <Text style={[
              styles.animalText,
              formData.animal === animal._id && styles.selectedAnimalText
            ]}>
              {animal.name} ({animal.type})
            </Text>
          </TouchableOpacity>
        ))}
      </View>
    </View>
  );

  return (
    <ScrollView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()}>
          <Ionicons name="arrow-back" size={24} color="#007AFF" />
        </TouchableOpacity>
        <Text style={styles.title}>{t('addTask.headerTitle')}</Text>
        <View style={{ width: 24 }} />
      </View>

      <View style={styles.form}>
        <View style={styles.inputContainer}>
          <Text style={styles.label}>{t('addTask.taskTitle')}</Text>
          <TextInput
            style={styles.input}
            value={formData.title}
            onChangeText={(value) => handleInputChange('title', value)}
            placeholder={t('addTask.taskTitlePlaceholder')}
            maxLength={100}
          />
        </View>

        <View style={styles.inputContainer}>
          <Text style={styles.label}>{t('addTask.description')}</Text>
          <TextInput
            style={[styles.input, styles.textArea]}
            value={formData.description}
            onChangeText={(value) => handleInputChange('description', value)}
            placeholder={t('addTask.descriptionPlaceholder')}
            multiline
            numberOfLines={3}
            maxLength={500}
          />
        </View>

        <View style={styles.dateTimeContainer}>
          <View style={styles.dateContainer}>
            <Text style={styles.label}>{t('addTask.dueDate')}</Text>
            <TouchableOpacity style={styles.dateButton} onPress={() => setShowDatePicker(true)}>
              <Text style={styles.dateText}>{formatDate(formData.dueDate)}</Text>
              <Ionicons name="calendar" size={20} color="#007AFF" style={{ marginLeft: 8 }} />
            </TouchableOpacity>
          </View>

          <View style={styles.timeContainer}>
            <Text style={styles.label}>{t('addTask.dueTime')}</Text>
            <TouchableOpacity style={styles.timeButton} onPress={() => setShowTimePicker(true)}>
              <Text style={styles.timeText}>{formatTime(formData.dueTime)}</Text>
              <Ionicons name="time" size={20} color="#007AFF" style={{ marginLeft: 8 }} />
            </TouchableOpacity>
          </View>

          {/* Date/Time pickers for mobile */}
          {showDatePicker && (
            <DateTimePicker
              value={new Date(formData.dueDate)}
              mode="date"
              display={Platform.OS === 'ios' ? 'spinner' : 'default'}
              onChange={(event, selectedDate) => {
                setShowDatePicker(false);
                if (selectedDate) {
                  const yyyy = selectedDate.getFullYear();
                  const mm = String(selectedDate.getMonth() + 1).padStart(2, '0');
                  const dd = String(selectedDate.getDate()).padStart(2, '0');
                  handleInputChange('dueDate', `${yyyy}-${mm}-${dd}`);
                }
              }}
            />
          )}
          {showTimePicker && (
            <DateTimePicker
              value={new Date(`1970-01-01T${formData.dueTime}`)}
              mode="time"
              display={Platform.OS === 'ios' ? 'spinner' : 'default'}
              onChange={(event, selectedTime) => {
                setShowTimePicker(false);
                if (selectedTime) {
                  const hh = String(selectedTime.getHours()).padStart(2, '0');
                  const min = String(selectedTime.getMinutes()).padStart(2, '0');
                  handleInputChange('dueTime', `${hh}:${min}`);
                }
              }}
            />
          )}
        </View>

        <View style={styles.inputContainer}>
          <Text style={styles.label}>{t('addTask.estimatedCost')}</Text>
          <TextInput
            style={styles.input}
            value={formData.estimatedCost}
            onChangeText={(value) => handleInputChange('estimatedCost', value)}
            placeholder={t('addTask.costPlaceholder')}
            keyboardType="decimal-pad"
          />
        </View>

        {renderPrioritySelector()}
        {renderCategorySelector()}
        {renderAnimalSelector()}

        <View style={styles.inputContainer}>
          <Text style={styles.label}>{t('addTask.notes')}</Text>
          <TextInput
            style={[styles.input, styles.textArea]}
            value={formData.notes}
            onChangeText={(value) => handleInputChange('notes', value)}
            placeholder={t('addTask.notesPlaceholder')}
            multiline
            numberOfLines={3}
            maxLength={300}
          />
        </View>

        <TouchableOpacity
          style={[styles.submitButton, loading && styles.disabledButton]}
          onPress={handleSubmit}
          disabled={loading}
        >
          <Text style={styles.submitText}>
            {loading ? t('addTask.creatingTask') : t('addTask.createTask')}
          </Text>
        </TouchableOpacity>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
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
  timeButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    gap: 8,
  },
  timeText: {
    fontSize: 16,
    color: '#333',
  },
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 20,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
  },
  title: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333',
  },
  form: {
    padding: 20,
  },
  inputContainer: {
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
  textArea: {
    height: 80,
    textAlignVertical: 'top',
  },
  dateTimeContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 20,
  },
  dateContainer: {
    flex: 1,
    marginRight: 10,
  },
  timeContainer: {
    flex: 1,
    marginLeft: 10,
  },
  dateInput: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
    backgroundColor: '#fff',
  },
  timeInput: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
    backgroundColor: '#fff',
  },
  selectorContainer: {
    marginBottom: 20,
  },
  priorityContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  priorityButton: {
    flex: 1,
    paddingVertical: 12,
    paddingHorizontal: 16,
    borderWidth: 2,
    borderRadius: 8,
    marginHorizontal: 4,
    alignItems: 'center',
  },
  priorityText: {
    fontSize: 14,
    fontWeight: '600',
  },
  categoryContainer: {
    flexDirection: 'row',
    paddingVertical: 8,
  },
  categoryButton: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 8,
    paddingHorizontal: 12,
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 20,
    marginRight: 8,
    backgroundColor: '#fff',
  },
  selectedCategory: {
    backgroundColor: '#104176ff',
    borderColor: '#007AFF',
  },
  categoryText: {
    fontSize: 12,
    color: '#666',
    marginLeft: 4,
  },
  selectedCategoryText: {
    color: '#fff',
  },
  animalContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
  },
  animalButton: {
    paddingVertical: 8,
    paddingHorizontal: 12,
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 20,
    marginRight: 8,
    marginBottom: 8,
    backgroundColor: '#fff',
  },
  selectedAnimal: {
    backgroundColor: '#1a4a7dff',
    borderColor: '#0b4482ff',
  },
  animalText: {
    fontSize: 12,
    color: '#666',
  },
  selectedAnimalText: {
    color: '#fff',
  },
  submitButton: {
    backgroundColor: '#08481fff',
    paddingVertical: 16,
    borderRadius: 8,
    alignItems: 'center',
    marginTop: 20,
  },
  disabledButton: {
    backgroundColor: '#ccc',
  },
  submitText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
});

export default AddTask;
