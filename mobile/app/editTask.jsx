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
import { router, useLocalSearchParams } from 'expo-router';
import AsyncStorage from '@react-native-async-storage/async-storage';
import axios from 'axios';
import { API_BASE_URL } from '../utils/apiConfig';
import { Ionicons } from '@expo/vector-icons';
import { useLanguage } from '../utils/LanguageContext';
import { useTranslation } from 'react-i18next';

const EditTask = () => {
  const { id } = useLocalSearchParams();
  const { language } = useLanguage();
  const { t, i18n } = useTranslation();

  // Update i18n language when language changes
  useEffect(() => {
    i18n.changeLanguage(language);
  }, [language, i18n]);

  const [formData, setFormData] = useState({
    title: '',
    description: '',
    dueDate: new Date(),
    dueTime: '09:00',
    estimatedCost: '',
    priority: 'medium',
    category: 'other',
    animal: '',
    notes: '',
    status: 'pending',
    isCompleted: false
  });
  const [showDatePicker, setShowDatePicker] = useState(false);
  const [showTimePicker, setShowTimePicker] = useState(false);
  // ...existing code...

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

  const [animals, setAnimals] = useState([]);
  const [loading, setLoading] = useState(false);
  const [initialLoading, setInitialLoading] = useState(true);

  const priorities = [
    { key: 'low', label: t('editTask.priorities.low'), color: '#155b15ff' },
    { key: 'medium', label: t('editTask.priorities.medium'), color: '#a06c04ff' },
    { key: 'high', label: t('editTask.priorities.high'), color: '#860303ff' }
  ];

  const categories = [
    { key: 'feeding', label: t('editTask.categories.feeding'), icon: 'restaurant' },
    { key: 'vaccination', label: t('editTask.categories.vaccination'), icon: 'medical' },
    { key: 'health-check', label: t('editTask.categories.healthCheck'), icon: 'heart' },
    { key: 'breeding', label: t('editTask.categories.breeding'), icon: 'heart-circle' },
    { key: 'maintenance', label: t('editTask.categories.maintenance'), icon: 'construct' },
    { key: 'other', label: t('editTask.categories.other'), icon: 'clipboard' }
  ];

  const statuses = [
    { key: 'pending', label: t('editTask.statuses.pending'), color: '#805705ff' },
    { key: 'in-progress', label: t('editTask.statuses.inProgress'), color: '#103f71ff' },
    { key: 'completed', label: t('editTask.statuses.completed'), color: '#105b10ff' },
    { key: 'cancelled', label: t('editTask.statuses.cancelled'), color: '#770505ff' }
  ];

  // Fetch task data and animals when component mounts
  useEffect(() => {
    fetchTaskData();
    fetchAnimals();
  }, []);

  const fetchTaskData = async () => {
    try {
      const token = await AsyncStorage.getItem('authToken');
      if (!token) {
        router.replace('/');
        return;
      }

      const response = await axios.get(`${API_BASE_URL}/api/tasks/${id}`, {
        headers: { Authorization: `Bearer ${token}` }
      });

      const task = response.data;
      setFormData({
        title: task.title || '',
        description: task.description || '',
        dueDate: new Date(task.dueDate).toISOString().split('T')[0], // Convert to YYYY-MM-DD format
        dueTime: task.dueTime || '09:00',
        estimatedCost: task.estimatedCost?.toString() || '',
        priority: task.priority || 'medium',
        category: task.category || 'other',
        animal: task.animal?._id || '',
        notes: task.notes || '',
        status: task.status || 'pending',
        isCompleted: task.isCompleted || false
      });

    } catch (error) {
      console.error('Error fetching task:', error);
      Alert.alert(t('editTask.error'), t('editTask.failedToLoad'));
      router.back();
    } finally {
      setInitialLoading(false);
    }
  };

  const fetchAnimals = async () => {
    try {
      const token = await AsyncStorage.getItem('authToken');
      if (!token) return;

      const response = await axios.get(`${API_BASE_URL}/api/animals`, {
        headers: { Authorization: `Bearer ${token}` }
      });

      setAnimals(response.data);
    } catch (error) {
      console.error('Error fetching animals:', error);
    }
  };

  const handleInputChange = (field, value) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));
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

  const validateForm = () => {
    if (!formData.title.trim()) {
      Alert.alert(t('editTask.validationError'), t('editTask.enterTaskTitle'));
      return false;
    }
    
    // Validate date format (YYYY-MM-DD)
    const datePattern = /^\d{4}-\d{2}-\d{2}$/;
    if (!datePattern.test(formData.dueDate)) {
      Alert.alert(t('editTask.validationError'), t('editTask.enterValidDate'));
      return false;
    }
    
    // Validate time format (HH:MM)
    if (!formData.dueTime.match(/^([01]?[0-9]|2[0-3]):[0-5][0-9]$/)) {
      Alert.alert(t('editTask.validationError'), t('editTask.enterValidTime'));
      return false;
    }

    if (formData.estimatedCost && isNaN(parseFloat(formData.estimatedCost))) {
      Alert.alert(t('editTask.validationError'), t('editTask.enterValidCost'));
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

      const taskData = {
        ...formData,
        estimatedCost: formData.estimatedCost ? parseFloat(formData.estimatedCost) : 0,
        animal: formData.animal || null
      };

      await axios.put(`${API_BASE_URL}/api/tasks/${id}`, taskData, {
        headers: { Authorization: `Bearer ${token}` }
      });

      router.replace('/taskManagement');
    } catch (error) {
      console.error('Error updating task:', error);
      Alert.alert(t('editTask.error'), error.response?.data?.message || t('editTask.failedToUpdate'));
    } finally {
      setLoading(false);
    }
  };

  const renderPrioritySelector = () => (
    <View style={styles.selectorContainer}>
      <Text style={styles.label}>{t('editTask.priority')}</Text>
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

  const renderStatusSelector = () => (
    <View style={styles.selectorContainer}>
      <Text style={styles.label}>{t('editTask.status')}</Text>
      <View style={styles.statusContainer}>
        {statuses.map(status => (
          <TouchableOpacity
            key={status.key}
            style={[
              styles.statusButton,
              { borderColor: status.color },
              formData.status === status.key && { backgroundColor: status.color }
            ]}
            onPress={() => handleInputChange('status', status.key)}
          >
            <Text style={[
              styles.statusText,
              formData.status === status.key && { color: '#fff' }
            ]}>
              {status.label}
            </Text>
          </TouchableOpacity>
        ))}
      </View>
    </View>
  );

  const renderCategorySelector = () => (
    <View style={styles.selectorContainer}>
      <Text style={styles.label}>{t('editTask.category')}</Text>
      <ScrollView horizontal showsHorizontalScrollIndicator={false}>
        <View style={styles.categoryContainer}>
          {categories.map(category => (
            <TouchableOpacity
              key={category.key}
              style={[
                styles.categoryButton,
                formData.category === category.key && styles.selectedCategory
              ]}
              onPress={() => handleInputChange('category', category.key)}
            >
              <Ionicons 
                name={category.icon} 
                size={20} 
                color={formData.category === category.key ? '#fff' : '#666'} 
              />
              <Text style={[
                styles.categoryText,
                formData.category === category.key && styles.selectedCategoryText
              ]}>
                {category.label}
              </Text>
            </TouchableOpacity>
          ))}
        </View>
      </ScrollView>
    </View>
  );

  const renderAnimalSelector = () => (
    <View style={styles.selectorContainer}>
      <Text style={styles.label}>{t('editTask.animalOptional')}</Text>
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
            {t('editTask.noSpecificAnimal')}
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

  if (initialLoading) {
    return (
      <View style={styles.loadingContainer}>
        <Text>{t('editTask.loadingTask')}</Text>
      </View>
    );
  }

  return (
    <ScrollView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()}>
          <Ionicons name="arrow-back" size={24} color="#073668ff" />
        </TouchableOpacity>
        <Text style={styles.title}>{t('editTask.headerTitle')}</Text>
        <View style={{ width: 24 }} />
      </View>

      <View style={styles.form}>
        <View style={styles.inputContainer}>
          <Text style={styles.label}>{t('editTask.taskTitle')}</Text>
          <TextInput
            style={styles.input}
            value={formData.title}
            onChangeText={(value) => handleInputChange('title', value)}
            placeholder={t('editTask.taskTitlePlaceholder')}
            maxLength={100}
          />
        </View>

        <View style={styles.inputContainer}>
          <Text style={styles.label}>{t('editTask.description')}</Text>
          <TextInput
            style={[styles.input, styles.textArea]}
            value={formData.description}
            onChangeText={(value) => handleInputChange('description', value)}
            placeholder={t('editTask.descriptionPlaceholder')}
            multiline
            numberOfLines={3}
            maxLength={500}
          />
        </View>

        <View style={styles.dateTimeContainer}>
          <View style={styles.dateContainer}>
            <Text style={styles.label}>{t('editTask.dueDate')}</Text>
            <TouchableOpacity style={styles.dateButton} onPress={() => setShowDatePicker(true)}>
              <Text style={styles.dateText}>{formatDate(formData.dueDate)}</Text>
              <Ionicons name="calendar" size={20} color="#053a73ff" style={{ marginLeft: 8 }} />
            </TouchableOpacity>
          </View>

          <View style={styles.timeContainer}>
            <Text style={styles.label}>{t('editTask.dueTime')}</Text>
            <TouchableOpacity style={styles.timeButton} onPress={() => setShowTimePicker(true)}>
              <Text style={styles.timeText}>{formatTime(formData.dueTime)}</Text>
              <Ionicons name="time" size={20} color="#064487ff" style={{ marginLeft: 8 }} />
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
        {/* ...existing code... */}
          <Text style={styles.label}>{t('editTask.estimatedCost')}</Text>
          <TextInput
            style={styles.input}
            value={formData.estimatedCost}
            onChangeText={(value) => handleInputChange('estimatedCost', value)}
            placeholder={t('editTask.estimatedCostPlaceholder')}
            keyboardType="decimal-pad"
          />
        </View>

        {renderPrioritySelector()}
        {renderStatusSelector()}
        {renderCategorySelector()}
        {renderAnimalSelector()}

        <View style={styles.inputContainer}>
          <Text style={styles.label}>{t('editTask.notes')}</Text>
          <TextInput
            style={[styles.input, styles.textArea]}
            value={formData.notes}
            onChangeText={(value) => handleInputChange('notes', value)}
            placeholder={t('editTask.notesPlaceholder')}
            multiline
            numberOfLines={3}
            maxLength={300}
          />
        </View>

        <View style={styles.completionContainer}>
          <TouchableOpacity
            style={styles.completionButton}
            onPress={() => handleInputChange('isCompleted', !formData.isCompleted)}
          >
            <View style={[
              styles.checkbox,
              formData.isCompleted && styles.checkedCheckbox
            ]}>
              {formData.isCompleted && (
                <Ionicons name="checkmark" size={16} color="#fff" />
              )}
            </View>
            <Text style={styles.completionText}>
              {formData.isCompleted ? t('editTask.markAsIncomplete') : t('editTask.markAsCompleted')}
            </Text>
          </TouchableOpacity>
        </View>

        <TouchableOpacity
          style={[styles.submitButton, loading && styles.disabledButton]}
          onPress={handleSubmit}
          disabled={loading}
        >
          <Text style={styles.submitText}>
            {loading ? t('editTask.updatingTask') : t('editTask.updateTask')}
          </Text>
        </TouchableOpacity>
    </ScrollView>
  );
};

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
  statusContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
  },
  statusButton: {
    flex: 1,
    paddingVertical: 12,
    paddingHorizontal: 8,
    borderWidth: 2,
    borderRadius: 8,
    marginHorizontal: 2,
    marginBottom: 8,
    alignItems: 'center',
    minWidth: '48%',
  },
  statusText: {
    fontSize: 12,
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
    backgroundColor: '#073464ff',
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
    backgroundColor: '#043466ff',
    borderColor: '#007AFF',
  },
  animalText: {
    fontSize: 12,
    color: '#666',
  },
  selectedAnimalText: {
    color: '#fff',
  },
  completionContainer: {
    marginBottom: 20,
  },
  completionButton: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
  },
  checkbox: {
    width: 24,
    height: 24,
    borderRadius: 12,
    borderWidth: 2,
    borderColor: '#093e77ff',
    marginRight: 12,
    justifyContent: 'center',
    alignItems: 'center',
  },
  checkedCheckbox: {
    backgroundColor: '#083564ff',
  },
  completionText: {
    fontSize: 16,
    color: '#333',
  },
  submitButton: {
    backgroundColor: '#0a5324ff',
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
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
});

export default EditTask;
