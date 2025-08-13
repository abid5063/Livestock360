import { 
  View, 
  Text, 
  StyleSheet, 
  Image, 
  ScrollView, 
  TouchableOpacity,
  ActivityIndicator,
  Alert,
  Modal,
  TextInput,
  Pressable,
  Dimensions,
  FlatList
} from "react-native";
import { useLocalSearchParams, router } from "expo-router";
import { useEffect, useState } from "react";
import AsyncStorage from '@react-native-async-storage/async-storage';
import { AntDesign, Feather, MaterialIcons, Ionicons } from '@expo/vector-icons';
import axios from "axios";
import * as ImagePicker from 'expo-image-picker';
import { API_BASE_URL } from '../utils/apiConfig'; // Adjust the import path as needed
import { useLanguage } from "../utils/LanguageContext";
import { useTranslation } from 'react-i18next';
const { width } = Dimensions.get('window');

export default function Profile() {
  const params = useLocalSearchParams();
  const { language, changeLanguage } = useLanguage();
  const { t, i18n } = useTranslation();
  const [farmer, setFarmer] = useState(null);
  const [loading, setLoading] = useState(true);
  const [animals, setAnimals] = useState([]);
  const [modalVisible, setModalVisible] = useState(false);
  const [editModalVisible, setEditModalVisible] = useState(false);
  const [currentAnimal, setCurrentAnimal] = useState(null);
  const [image, setImage] = useState(null);
  const [showMessages, setShowMessages] = useState(false);
  const [showVets, setShowVets] = useState(false);
  const [conversations, setConversations] = useState([]);
  const [vets, setVets] = useState([]);
  const [selectedConversation, setSelectedConversation] = useState(null);
  const [conversationMessages, setConversationMessages] = useState([]);
  const [newMessage, setNewMessage] = useState('');

  const [formData, setFormData] = useState({
    name: '',
    type: '',
    breed: '',
    age: '',
    gender: '',
    details: ''
  });

  // Update i18n language when language changes
  useEffect(() => {
    i18n.changeLanguage(language);
  }, [language, i18n]);

  useEffect(() => {
    const loadData = async () => {
      try {
        const token = await AsyncStorage.getItem('authToken');
        if (!token) {
          router.replace('/');
          return;
        }

        let parsedFarmer = null;
        if (params.farmer) {
          parsedFarmer = JSON.parse(params.farmer);
        } else {
          const storedData = await AsyncStorage.getItem('userData');
          if (storedData) {
            parsedFarmer = JSON.parse(storedData);
          }
        }
        if (!parsedFarmer) {
          router.replace('/');
          return;
        }
        setFarmer(parsedFarmer);
        await fetchAnimals(token);
        await fetchConversations();
        await fetchVets();
      } catch (error) {
        Alert.alert(t('alerts.error'), t('alerts.failedToLoadData'));
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, []);

  const fetchAnimals = async (token) => {
    try {
      const response = await axios.get(`${API_BASE_URL}/api/animals`, {
        headers: {
          Authorization: `Bearer ${token}`
        }
      });
      setAnimals(response.data);
    } catch (error) {
      if (error.response?.status === 401) {
        Alert.alert(t('alerts.sessionExpired'), t('alerts.pleaseLoginAgain'));
        await AsyncStorage.multiRemove(['authToken', 'userData']);
        router.replace('/');
      } else {
        Alert.alert(t('alerts.error'), t('alerts.failedToFetchAnimals'));
      }
    }
  };

  const pickImage = async () => {
    // Request permissions first
    const { status } = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (status !== 'granted') {
      Alert.alert(t('alerts.permissionRequired'), t('alerts.cameraRollPermission'));
      return;
    }

    let result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      allowsEditing: true,
      aspect: [4, 3],
      quality: 0.5,
      base64: true,
    });

    if (!result.canceled) {
      setImage(result.assets[0].base64);
    }
  };

  const handleLogout = async () => {
    Alert.alert(
      'Sign Out',
      'Are you sure you want to log out?',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Log Out',
          style: 'destructive',
          onPress: async () => {
            try {
              await AsyncStorage.multiRemove(['authToken', 'userData']);
              router.replace('/');
            } catch (error) {
              Alert.alert(t('alerts.error'), t('alerts.failedToLogout'));
            }
          }
        }
      ]
    );
  };

  const handleInputChange = (name, value) => {
    setFormData({
      ...formData,
      [name]: value
    });
  };

  const handleAddAnimal = async () => {
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
      const token = await AsyncStorage.getItem('authToken');
      const response = await axios.post(
        `${API_BASE_URL}/api/animals`,
        {
          ...formData,
          age: Number(formData.age),
          image: image ? `data:image/jpeg;base64,${image}` : null
        },
        {
          headers: {
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        }
      );
      setAnimals([...animals, response.data]);
      setModalVisible(false);
      setFormData({
        name: '',
        type: '',
        breed: '',
        age: '',
        gender: '',
        details: ''
      });
      setImage(null);
      Alert.alert(t('alerts.success'), t('alerts.animalAddedSuccess'));
    } catch (error) {
      Alert.alert(t('alerts.error'), error.response?.data?.message || t('alerts.failedToAddAnimal'));
    }
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
      const token = await AsyncStorage.getItem('authToken');
      const response = await axios.put(
        `${API_BASE_URL}/api/animals/${currentAnimal._id}`,
        {
          ...formData,
          age: Number(formData.age),
          image: image ? `data:image/jpeg;base64,${image}` : currentAnimal.photo_url
        },
        {
          headers: {
            Authorization: `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        }
      );
      setAnimals(animals.map(animal => 
        animal._id === currentAnimal._id ? response.data : animal
      ));
      setEditModalVisible(false);
      setCurrentAnimal(null);
      setImage(null);
      Alert.alert(t('alerts.success'), t('alerts.animalUpdatedSuccess'));
    } catch (error) {
      Alert.alert(t('alerts.error'), error.response?.data?.message || t('alerts.failedToUpdateAnimal'));
    }
  };



  const handleViewAnimal = (animal) => {
    router.push({
      pathname: '/animalDetails',
      params: { animal: JSON.stringify(animal) }
    });
  };

  const fetchConversations = async () => {
    try {
      const token = await AsyncStorage.getItem('authToken');
      const response = await axios.get(`${API_BASE_URL}/api/messages/conversations`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setConversations(response.data.conversations || []);
    } catch (error) {
      console.error("Error fetching conversations:", error);
    }
  };

  const fetchVets = async () => {
    try {
      const token = await AsyncStorage.getItem('authToken');
      const response = await axios.get(`${API_BASE_URL}/api/vets/search`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setVets(response.data.vets || []);
    } catch (error) {
      console.error("Error fetching vets:", error);
    }
  };

  const openConversation = async (conversation) => {
    try {
      setSelectedConversation(conversation);
      const token = await AsyncStorage.getItem('authToken');
      
      const response = await axios.get(
        `${API_BASE_URL}/api/messages/conversation/${conversation.participant.id}/vet`,
        { headers: { Authorization: `Bearer ${token}` } }
      );
      
      setConversationMessages(response.data.messages || []);
    } catch (error) {
      console.error("Error fetching conversation:", error);
      Alert.alert(t('alerts.error'), t('alerts.failedToLoadConversation'));
    }
  };

  const startConversationWithVet = async (vet) => {
    try {
      const conversation = {
        participant: {
          id: vet._id,
          name: vet.name,
          specialty: vet.specialty
        }
      };
      setSelectedConversation(conversation);
      setConversationMessages([]);
      setShowVets(false);
      setShowMessages(true);
    } catch (error) {
      console.error("Error starting conversation:", error);
    }
  };

  const sendMessage = async () => {
    if (!newMessage.trim() || !selectedConversation) return;

    try {
      const token = await AsyncStorage.getItem('authToken');
      
      await axios.post(
        `${API_BASE_URL}/api/messages`,
        {
          receiverId: selectedConversation.participant.id,
          receiverType: 'vet',
          content: newMessage.trim()
        },
        { headers: { Authorization: `Bearer ${token}` } }
      );

      setNewMessage('');
      
      // Refresh conversation
      const response = await axios.get(
        `${API_BASE_URL}/api/messages/conversation/${selectedConversation.participant.id}/vet`,
        { headers: { Authorization: `Bearer ${token}` } }
      );
      
      setConversationMessages(response.data.messages || []);
      
    } catch (error) {
      console.error("Error sending message:", error);
      Alert.alert(t('alerts.error'), t('alerts.failedToSendMessage'));
    }
  };

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#4a89dc" />
        <Text style={styles.loadingText}>{t('profile.loading')}</Text>
      </View>
    );
  }

  if (!farmer) {
    return (
      <View style={styles.container}>
        <Text>{t('profile.noProfileData')}</Text>
        <TouchableOpacity onPress={() => router.replace('/')}>
          <Text style={styles.linkText}>{t('profile.goToLogin')}</Text>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <ScrollView contentContainerStyle={styles.scrollContainer}>
      <View style={styles.container}>
        {/* Header Section */}
        <View style={styles.headerSection}>
          <View style={styles.headerContent}>
            <Text style={styles.welcomeText}>{t('profile.welcomeBack')}</Text>
            <Text style={styles.farmerName}>{farmer.name}</Text>
            <Text style={styles.farmerEmail}>{farmer.email}</Text>
          </View>
          <TouchableOpacity 
            style={styles.profileImageContainer}
            onPress={() => router.push({
              pathname: '/editProfile',
              params: { farmer: JSON.stringify(farmer) }
            })}
            testID="edit-profile-button"
          >
            <Image 
              source={farmer.profileImage ? { uri: farmer.profileImage } : require('../assets/images/icon.png')}
              style={styles.profileImage}
            />
            <View style={styles.editIndicator}>
              <Ionicons name="pencil" size={12} color="#f6E6bb" />
            </View>
          </TouchableOpacity>
        </View>

        {/* Quick Stats */}
        <View style={styles.statsContainer}>
          <View style={styles.statCard}>
            <View style={styles.statIconContainer}>
              <Ionicons name="paw" size={24} color="#124438" />
            </View>
            <Text style={styles.statNumber}>{animals.length}</Text>
            <Text style={styles.statLabel}>{t('profile.animals')}</Text>
          </View>
          <View style={styles.statCard}>
            <View style={styles.statIconContainer}>
              <Ionicons name="location" size={24} color="#27ae60" />
            </View>
            <Text style={styles.statNumber}>1</Text>
            <Text style={styles.statLabel}>{t('profile.farm')}</Text>
          </View>
          <View style={styles.statCard}>
            <View style={styles.statIconContainer}>
              <Ionicons name="shield-checkmark" size={24} color="#0db727ff" />
            </View>
            <Text style={styles.statNumber}>{t('profile.active')}</Text>
            <Text style={styles.statLabel}>{t('profile.status')}</Text>
          </View>
        </View>

        {/* Main Features Grid */}
        <View style={styles.featuresContainer}>
          <Text style={styles.sectionTitle}>{t('profile.farmManagement')}</Text>
          
          <View style={styles.featuresGrid}>
            {/* AI Chatbot */}
            <TouchableOpacity
              style={[styles.featureCard, styles.primaryCard]}
              onPress={() => router.push('/aiChatbot')}
              testID="ai-chatbot-button"
            >
              <View style={styles.featureIconContainer}>
                <Ionicons name="chatbubble-ellipses" size={28} color="#f1f6f5ff" />
              </View>
              <Text style={styles.featureTitle}>{t('profile.aiAssistant')}</Text>
              <Text style={styles.featureSubtitle}>{t('profile.getInstantHelp')}</Text>
            </TouchableOpacity>

            {/* Disease Detection */}
            <TouchableOpacity
              style={[styles.featureCard, styles.secondaryCard]}
              onPress={() => router.push('/symptomChecker')}
              testID="disease-detection-button"
            >
              <View style={styles.featureIconContainer}>
                <Ionicons name="medical" size={28} color="#fff" />
              </View>
              <Text style={styles.featureTitle}>{t('profile.healthCheck')}</Text>
              <Text style={styles.featureSubtitle}>{t('profile.diseaseDetection')}</Text>
            </TouchableOpacity>

            {/* Task Management */}
            <TouchableOpacity
              style={[styles.featureCard, styles.accentCard]}
              onPress={() => router.push('/taskManagement')}
              testID="task-management-button"
            >
              <View style={styles.featureIconContainer}>
                <Ionicons name="clipboard" size={28} color="#fff" />
              </View>
              <Text style={styles.featureTitle}>{t('profile.tasks')}</Text>
              <Text style={styles.featureSubtitle}>{t('profile.manageDailyTasks')}</Text>
            </TouchableOpacity>

            {/* Add Task */}
            <TouchableOpacity
              style={[styles.featureCard, styles.warningCard]}
              onPress={() => router.push('/farmerAppointmentManagement')}
              testID="add-task-button"
            >
              <View style={styles.featureIconContainer}>
                <Ionicons name="add-circle" size={28} color="#fff" />
              </View>
              <Text style={styles.featureTitle}>{t('profile.appointments')}</Text>
              <Text style={styles.featureSubtitle}>{t('profile.addNewTask')}</Text>
            </TouchableOpacity>

            {/* Vaccine Management */}
            <TouchableOpacity
              style={[styles.featureCard, styles.infoCard]}
              onPress={() => router.push('/vaccineManagement')}
              testID="vaccine-management-button"
            >
              <View style={styles.featureIconContainer}>
                <Ionicons name="shield-checkmark" size={28} color="#fff" />
              </View>
              <Text style={styles.featureTitle}>{t('profile.vaccines')}</Text>
              <Text style={styles.featureSubtitle}>{t('profile.manageVaccines')}</Text>
            </TouchableOpacity>

            {/* Add Vaccine */}
            <TouchableOpacity
              style={[styles.featureCard, styles.successCard]}
              onPress={() => router.push('/addVaccine')}
              testID="add-vaccine-button"
            >
              <View style={styles.featureIconContainer}>
                <Ionicons name="add-circle" size={28} color="#fff" />
              </View>
              <Text style={styles.featureTitle}>{t('profile.newVaccine')}</Text>
              <Text style={styles.featureSubtitle}>{t('profile.addVaccineRecord')}</Text>
            </TouchableOpacity>

            {/* Messages */}
            <TouchableOpacity
              style={[styles.featureCard, styles.messageCard]}
              onPress={() => router.push('/farmerMessaging')}
              testID="messages-button"
            >
              <View style={styles.featureIconContainer}>
                <Ionicons name="chatbubbles" size={28} color="#fff" />
              </View>
              <Text style={styles.featureTitle}>{t('profile.message')}</Text>
              <Text style={styles.featureSubtitle}>{t('profile.chatWithVets')}</Text>
            </TouchableOpacity>



            {/* Find Vet */}
            <TouchableOpacity
              style={[styles.featureCard, styles.warningCard]}
              onPress={() => router.push('/vetLocation')}
            >
              <View style={styles.featureIconContainer}>
                <Ionicons name="location" size={28} color="#fff" />
              </View>
              <Text style={styles.featureTitle}>{t('profile.findVet')}</Text>
              <Text style={styles.featureSubtitle}>{t('profile.nearestVeterinarian')}</Text>
            </TouchableOpacity>

            {/* Market Analysis */}
            <TouchableOpacity
              style={[styles.featureCard, styles.accentCard]}
              onPress={() => router.push('/marketAnalysis')}
            >
              <View style={styles.featureIconContainer}>
                <Ionicons name="trending-up" size={28} color="#fff" />
              </View>
              <Text style={styles.featureTitle}>{t('profile.market')}</Text>
              <Text style={styles.featureSubtitle}>{t('profile.priceAnalysis')}</Text>
            </TouchableOpacity>

            {/* Food Suggestions */}
            <TouchableOpacity
              style={[styles.featureCard, styles.primaryCard]}
              onPress={() => router.push('/foodSuggestions')}
            >
              <View style={styles.featureIconContainer}>
                <Ionicons name="nutrition" size={28} color="#fff" />
              </View>
              <Text style={styles.featureTitle}>{t('profile.nutrition')}</Text>
              <Text style={styles.featureSubtitle}>{t('profile.feedSuggestions')}</Text>
            </TouchableOpacity>
          </View>
        </View>

        {/* Animals Section */}
        <View style={styles.animalsSection}>
          <View style={styles.sectionHeader}>
            <Text style={styles.sectionTitle}>{t('profile.myAnimals')}</Text>
            <TouchableOpacity 
              style={styles.addAnimalButton}
              onPress={() => setModalVisible(true)}
              testID="add-animal-button"
            >
              <Ionicons name="add" size={20} color="#fff" />
            </TouchableOpacity>
          </View>

          {animals.length === 0 ? (
            <View style={styles.emptyState}>
              <Ionicons name="paw" size={48} color="#ccc" />
              <Text style={styles.emptyStateText}>{t('profile.noAnimalsYet')}</Text>
              <Text style={styles.emptyStateSubtext}>{t('profile.addFirstAnimal')}</Text>
            </View>
          ) : (
            <View style={styles.animalsGrid}>
              {animals.map(animal => (
                <TouchableOpacity 
                  key={animal._id} 
                  style={styles.animalCard}
                  onPress={() => handleViewAnimal(animal)}
                  testID={`view-animal-${animal._id}`}
                >
                  <View style={styles.animalImageContainer}>
                    {animal.photo_url ? (
                      <Image 
                        source={{ uri: animal.photo_url }} 
                        style={styles.animalImage}
                      />
                    ) : (
                      <View style={styles.animalImagePlaceholder}>
                        <Ionicons name="paw" size={24} color="#aaa" />
                      </View>
                    )}
                  </View>
                  <View style={styles.animalInfo}>
                    <Text style={styles.animalName}>{animal.name}</Text>
                    <Text style={styles.animalType}>{animal.type}</Text>
                    <Text style={styles.animalBreed}>{animal.breed}</Text>
                  </View>
                </TouchableOpacity>
              ))}
            </View>
          )}
        </View>

        {/* Farm Details */}
        <View style={styles.farmDetailsSection}>
          <Text style={styles.sectionTitle}>{t('profile.farmDetails')}</Text>
          <View style={styles.detailsCard}>
            <View style={styles.detailItem}>
              <View style={styles.detailIconContainer}>
                <Ionicons name="location" size={20} color="#4a89dc" />
              </View>
              <Text style={styles.detailText}>
                {farmer.location || "Location not specified"}
              </Text>
            </View>
            <View style={styles.detailItem}>
              <View style={styles.detailIconContainer}>
                <Ionicons name="call" size={20} color="#4a89dc" />
              </View>
              <Text style={styles.detailText}>
                {farmer.phoneNo || "Phone not specified"}
              </Text>
            </View>
          </View>
        </View>

        {/* Logout Button */}
        <TouchableOpacity 
          style={styles.logoutButton}
          onPress={handleLogout}
          testID="logout-button"
        >
          <Ionicons name="log-out" size={20} color="#fff" style={styles.logoutIcon} />
          <Text style={styles.logoutButtonText}>{t('profile.signOut')}</Text>
        </TouchableOpacity>

        {/* Add Animal Modal */}
        <Modal
          animationType="slide"
          transparent={true}
          visible={modalVisible}
          onRequestClose={() => setModalVisible(false)}
        >
          <View style={styles.modalContainer}>
            <View style={styles.modalContent}>
              <Text style={styles.modalTitle}>{t('profile.addNewAnimal')}</Text>
              
              <TextInput
                style={styles.input}
                placeholder={t('profile.addAnimal.namePlaceholder')}
                placeholderTextColor="#333"
                value={formData.name}
                onChangeText={(text) => handleInputChange('name', text)}
                testID="animal-name-input"
              />
              
              <TextInput
                style={styles.input}
                placeholder={t('profile.addAnimal.typePlaceholder')}
                placeholderTextColor="#333"
                value={formData.type}
                onChangeText={(text) => handleInputChange('type', text)}
                testID="animal-type-input"
              />
              
              <TextInput
                style={styles.input}
                placeholder={t('profile.addAnimal.breedPlaceholder')}
                placeholderTextColor="#333"
                value={formData.breed}
                onChangeText={(text) => handleInputChange('breed', text)}
                testID="animal-breed-input"
              />
              
              <TextInput
                style={styles.input}
                placeholder={t('profile.addAnimal.agePlaceholder')}
                placeholderTextColor="#333"
                value={formData.age}
                onChangeText={(text) => handleInputChange('age', text)}
                keyboardType="numeric"
                testID="animal-age-input"
              />
              
              <TextInput
                style={styles.input}
                placeholder={t('profile.addAnimal.genderPlaceholder')}
                placeholderTextColor="#333"
                value={formData.gender}
                onChangeText={(text) => handleInputChange('gender', text)}
                testID="animal-gender-input"
              />
              
              <TextInput
                style={[styles.input, { height: 80 }]}
                placeholder={t('profile.addAnimal.detailsPlaceholder')}
                placeholderTextColor="#333"
                value={formData.details}
                onChangeText={(text) => handleInputChange('details', text)}
                multiline
                testID="animal-details-input"
              />

              <TouchableOpacity 
                style={styles.imagePickerButton}
                onPress={pickImage}
                testID="image-picker-button"
              >
                <Text style={styles.imagePickerText}>
                  {image ? t('profile.addAnimal.imageSelected') : t('profile.addAnimal.selectImagePlaceholder')}
                </Text>
              </TouchableOpacity>

              {image && (
                <Image 
                  source={{ uri: `data:image/jpeg;base64,${image}` }}
                  style={styles.previewImage}
                />
              )}
              
              <View style={styles.modalButtons}>
                <Pressable
                  style={[styles.modalButton, styles.cancelButton]}
                  onPress={() => {
                    setModalVisible(false);
                    setImage(null);
                  }}
                  testID="modal-cancel-button"
                >
                  <Text style={styles.cancelButtonText}>{t('profile.addAnimal.cancelButton')}</Text>
                </Pressable>
                <Pressable
                  style={[styles.modalButton, styles.saveButton]}
                  onPress={handleAddAnimal}
                  testID="modal-save-button"
                >
                  <Text style={styles.saveButtonText}>{t('profile.addAnimal.saveButton')}</Text>
                </Pressable>
              </View>
            </View>
          </View>
        </Modal>

        {/* Edit Animal Modal */}
        <Modal
          animationType="slide"
          transparent={true}
          visible={editModalVisible}
          onRequestClose={() => setEditModalVisible(false)}
        >
          <View style={styles.modalContainer}>
            <View style={styles.modalContent}>
              <Text style={styles.modalTitle}>{t('profile.editAnimal')}</Text>
              
              <TextInput
                style={styles.input}
                placeholder={t('profile.editAnimal.namePlaceholder')}
                value={formData.name}
                onChangeText={(text) => handleInputChange('name', text)}
              />
              
              <TextInput
                style={styles.input}
                placeholder={t('profile.editAnimal.typePlaceholder')}
                value={formData.type}
                onChangeText={(text) => handleInputChange('type', text)}
              />
              
              <TextInput
                style={styles.input}
                placeholder={t('profile.editAnimal.breedPlaceholder')}
                value={formData.breed}
                onChangeText={(text) => handleInputChange('breed', text)}
              />
              
              <TextInput
                style={styles.input}
                placeholder={t('profile.editAnimal.agePlaceholder')}
                value={formData.age}
                onChangeText={(text) => handleInputChange('age', text)}
                keyboardType="numeric"
              />
              
              <TextInput
                style={styles.input}
                placeholder={t('profile.editAnimal.genderPlaceholder')}
                value={formData.gender}
                onChangeText={(text) => handleInputChange('gender', text)}
              />
              
              <TextInput
                style={[styles.input, { height: 80 }]}
                placeholder={t('profile.editAnimal.detailsPlaceholder')}
                value={formData.details}
                onChangeText={(text) => handleInputChange('details', text)}
                multiline
              />

              <TouchableOpacity 
                style={styles.imagePickerButton}
                onPress={pickImage}
              >
                <Text style={styles.imagePickerText}>
                  {image ? t('profile.editAnimal.changeImage') : currentAnimal?.photo_url ? t('profile.editAnimal.keepCurrentImage') : t('profile.editAnimal.addImagePlaceholder')}
                </Text>
              </TouchableOpacity>

              {(image || currentAnimal?.photo_url) && (
                <Image 
                  source={{ uri: image ? `data:image/jpeg;base64,${image}` : currentAnimal?.photo_url }}
                  style={styles.previewImage}
                />
              )}
              
              <View style={styles.modalButtons}>
                <Pressable
                  style={[styles.modalButton, styles.cancelButton]}
                  onPress={() => {
                    setEditModalVisible(false);
                    setImage(null);
                  }}
                >
                  <Text style={styles.cancelButtonText}>{t('profile.editAnimal.cancelButton')}</Text>
                </Pressable>
                <Pressable
                  style={[styles.modalButton, styles.saveButton]}
                  onPress={handleEditAnimal}
                >
                  <Text style={styles.saveButtonText}>{t('profile.editAnimal.saveChangesButton')}</Text>
                </Pressable>
              </View>
            </View>
          </View>
        </Modal>

        {/* Messages Modal */}
        <Modal
          visible={showMessages}
          animationType="slide"
          presentationStyle="pageSheet"
        >
          <View style={styles.modalContainer}>
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>
                {selectedConversation ? selectedConversation.participant.name : t('profile.messages.title')}
              </Text>
              <TouchableOpacity
                onPress={() => {
                  if (selectedConversation) {
                    setSelectedConversation(null);
                    setConversationMessages([]);
                  } else {
                    setShowMessages(false);
                  }
                }}
                testID="close-messages-modal"
              >
                <Ionicons name={selectedConversation ? "arrow-back" : "close"} size={24} color="#34495e" />
              </TouchableOpacity>
            </View>
            
            {selectedConversation ? (
              <View style={styles.chatContainer}>
                <FlatList
                  data={conversationMessages}
                  renderItem={({ item }) => (
                    <View style={[
                      styles.messageItem,
                      item.senderType === 'farmer' ? styles.sentMessage : styles.receivedMessage
                    ]}>
                      <Text style={styles.messageContent}>{item.content}</Text>
                      <Text style={styles.messageTime}>
                        {new Date(item.createdAt).toLocaleTimeString()}
                      </Text>
                    </View>
                  )}
                  keyExtractor={(item) => item._id}
                  contentContainerStyle={styles.messagesContainer}
                  showsVerticalScrollIndicator={false}
                />
                <View style={styles.messageInputContainer}>
                  <TextInput
                    style={styles.messageInput}
                    value={newMessage}
                    onChangeText={setNewMessage}
                    placeholder={t('profile.messages.messageInputPlaceholder')}
                    multiline
                    testID="message-input"
                  />
                  <TouchableOpacity
                    style={styles.sendButton}
                    onPress={sendMessage}
                    testID="send-message-button"
                  >
                    <Ionicons name="send" size={20} color="#fff" />
                  </TouchableOpacity>
                </View>
              </View>
            ) : (
              <FlatList
                data={conversations}
                renderItem={({ item }) => (
                  <TouchableOpacity
                    style={styles.conversationItem}
                    onPress={() => openConversation(item)}
                    testID={`conversation-${item.participant.id}`}
                  >
                    <View style={styles.conversationHeader}>
                      <Text style={styles.conversationName}>{item.participant.name}</Text>
                      <Text style={styles.conversationTime}>
                        {new Date(item.lastMessage.timestamp).toLocaleDateString()}
                      </Text>
                    </View>
                    <Text style={styles.conversationPreview} numberOfLines={2}>
                      {item.lastMessage.content}
                    </Text>
                    {item.unreadCount > 0 && (
                      <View style={styles.unreadBadge}>
                        <Text style={styles.unreadText}>{item.unreadCount}</Text>
                      </View>
                    )}
                  </TouchableOpacity>
                )}
                keyExtractor={(item) => item.conversationId}
                contentContainerStyle={styles.modalContent}
                showsVerticalScrollIndicator={false}
              />
            )}
          </View>
        </Modal>

        {/* Find Vets Modal */}
        <Modal
          visible={showVets}
          animationType="slide"
          presentationStyle="pageSheet"
        >
          <View style={styles.modalContainer}>
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>{t('profile.findVets.title')}</Text>
              <TouchableOpacity
                onPress={() => setShowVets(false)}
                testID="close-vets-modal"
              >
                <Ionicons name="close" size={24} color="#34495e" />
              </TouchableOpacity>
            </View>
            <FlatList
              data={vets}
              renderItem={({ item }) => (
                <View style={styles.vetItem}>
                  <View style={styles.vetInfo}>
                    <Text style={styles.vetName}>Dr. {item.name}</Text>
                    <Text style={styles.vetSpecialty}>{item.specialty}</Text>
                    <Text style={styles.vetLocation}>{item.location}</Text>
                    <Text style={styles.vetRating}>⭐ {item.rating}/5 ({item.totalReviews} reviews)</Text>
                  </View>
                  <TouchableOpacity
                    style={styles.chatButton}
                    onPress={() => startConversationWithVet(item)}
                    testID={`chat-with-vet-${item._id}`}
                  >
                    <Ionicons name="chatbubbles" size={16} color="#fff" />
                    <Text style={styles.chatButtonText}>{t('profile.findVets.chatButton')}</Text>
                  </TouchableOpacity>
                </View>
              )}
              keyExtractor={(item) => item._id}
              contentContainerStyle={styles.modalContent}
              showsVerticalScrollIndicator={false}
            />
          </View>
        </Modal>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  scrollContainer: {
    paddingBottom: 40,
  },
  container: {
    flex: 1,
    backgroundColor: "#f2fef7ff",
  },
  loadingContainer: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    backgroundColor: "#f5f6fa",
  },
  loadingText: {
    marginTop: 15,
    fontSize: 16,
    color: "#4a89dc",
  },
  headerSection: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    backgroundColor: "#fff",
    paddingHorizontal: 20,
    paddingVertical: 25,
    margintop: 8,
    borderBottomLeftRadius: 20,
    borderBottomRightRadius: 20,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 5,
  },
  headerContent: {
    flex: 1,
  },
  welcomeText: {
    fontSize: 18,
    color: "#044c17ff",
    marginBottom: 4,
  },
  farmerName: {
    fontSize: 24,
    fontWeight: "bold",
    color: "#2c3e50",
    marginBottom: 2,
  },
  farmerEmail: {
    fontSize: 14,
    color: "#4f6364ff",
  },
  profileImageContainer: {
    position: "relative",
  },
  profileImage: {
    width: 70,
    height: 70,
    borderRadius: 35,
    backgroundColor: "#e0e0e0",
    borderWidth: 3,
    borderColor: "#fff",
  },
  editIndicator: {
    position: "absolute",
    bottom: 0,
    right: 0,
    backgroundColor: "#4a89dc",
    width: 24,
    height: 24,
    borderRadius: 12,
    justifyContent: "center",
    alignItems: "center",
    borderWidth: 2,
    borderColor: "#fff",
  },
  statsContainer: {
    flexDirection: "row",
    justifyContent: "space-between",
    paddingHorizontal: 20,
    paddingVertical: 20,
    gap: 10,
  },
  statCard: {
    backgroundColor: "#fff",
    borderRadius: 12,
    padding: 16,
    flex: 1,
    alignItems: "center",
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  statIconContainer: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: "#f8f9fa",
    justifyContent: "center",
    alignItems: "center",
    marginBottom: 8,
  },
  statNumber: {
    fontSize: 20,
    fontWeight: "bold",
    color: "#2c3e50",
    marginBottom: 4,
  },
  statLabel: {
    fontSize: 12,
    color: "#7f8c8d",
  },
  featuresContainer: {
    paddingHorizontal: 20,
    paddingBottom: 20,
  },
  sectionTitle: {
    fontSize: 20,
    fontWeight: "bold",
    color: "#2c3e50",
    marginBottom: 16,
  },
  featuresGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    justifyContent: "space-between",
    paddingHorizontal: 4,
  },
  featureCard: {
    width: "48%",
    backgroundColor: "#fff",
    borderRadius: 16,
    padding: 16,
    alignItems: "center",
    justifyContent: "center",
    marginBottom: 12,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.1,
    shadowRadius: 8,
    elevation: 5,
    minHeight: 110,
  },
  primaryCard: {
    backgroundColor: "#5c7d3eff",
  },
  secondaryCard: {
    backgroundColor: "#2e544cff",
  },
 
  accentCard: {
    backgroundColor: "#477c59ff",
  },
  warningCard: {
    backgroundColor: "#7790a7ff",
  },
  infoCard: {
    backgroundColor: "#2e6977fe",
  },
  successCard: {
    backgroundColor: "#195d4fff",
  },
  featureIconContainer: {
    width: 48,
    height: 48,
    borderRadius: 24,
    backgroundColor: "rgba(241, 248, 245, 0.35)",
    justifyContent: "center",
    alignItems: "center",
    marginBottom: 12,
  },
  featureTitle: {
    color: "#fff",
    fontSize: 16,
    fontWeight: "bold",
    marginBottom: 4,
    textAlign: "center",
  },
  featureSubtitle: {
    color: "rgba(255, 255, 255, 0.8)",
    fontSize: 12,
    textAlign: "center",
  },
  animalsSection: {
    paddingHorizontal: 20,
    paddingBottom: 20,
  },
  sectionHeader: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 16,
  },
  addAnimalButton: {
    backgroundColor: "#0b5034ff",
    width: 36,
    height: 36,
    borderRadius: 18,
    justifyContent: "center",
    alignItems: "center",
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  emptyState: {
    alignItems: "center",
    paddingVertical: 40,
    backgroundColor: "#fff",
    borderRadius: 16,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  emptyStateText: {
    fontSize: 16,
    color: "#7f8c8d",
    marginTop: 12,
    marginBottom: 4,
  },
  emptyStateSubtext: {
    fontSize: 14,
    color: "#95a5a6",
    textAlign: "center",
    paddingHorizontal: 20,
  },
  animalsGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    justifyContent: "space-between",
    paddingHorizontal: 2,
  },
  animalCard: {
    width: (width - 64) / 2, // Adjusted for container padding (40) + gap (12) + extra spacing (12)
    backgroundColor: "#fff",
    borderRadius: 16,
    padding: 16,
    alignItems: "center",
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
    marginBottom: 10,
  },
  animalImageContainer: {
    marginBottom: 12,
  },
  animalImage: {
    width: 60,
    height: 60,
    borderRadius: 30,
    backgroundColor: "#e0e0e0",
  },
  animalImagePlaceholder: {
    width: 60,
    height: 60,
    borderRadius: 30,
    backgroundColor: "#f8f9fa",
    justifyContent: "center",
    alignItems: "center",
  },
  animalInfo: {
    alignItems: "center",
  },
  animalName: {
    fontSize: 16,
    fontWeight: "bold",
    color: "#2c3e50",
    marginBottom: 2,
  },
  animalType: {
    fontSize: 14,
    color: "#7f8c8d",
    marginBottom: 1,
  },
  animalBreed: {
    fontSize: 12,
    color: "#95a5a6",
  },
  farmDetailsSection: {
    paddingHorizontal: 20,
    paddingBottom: 20,
  },
  detailsCard: {
    backgroundColor: "#fff",
    borderRadius: 16,
    padding: 20,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  detailItem: {
    flexDirection: "row",
    alignItems: "center",
    marginBottom: 12,
  },
  detailIconContainer: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: "#f8f9fa",
    justifyContent: "center",
    alignItems: "center",
    marginRight: 12,
  },
  detailText: {
    fontSize: 16,
    color: "#2c3e50",
  },
  logoutButton: {
    backgroundColor: "#431c18ff",
    marginHorizontal: 20,
    marginBottom: 20,
    borderRadius: 12,
    paddingVertical: 16,
    paddingHorizontal: 20,
    flexDirection: "row",
    justifyContent: "center",
    alignItems: "center",
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  logoutIcon: {
    marginRight: 8,
  },
  logoutButtonText: {
    color: "#fff",
    fontSize: 16,
    fontWeight: "bold",
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
    borderRadius: 16,
    padding: 24,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.1,
    shadowRadius: 8,
    elevation: 5,
  },
  modalTitle: {
    fontSize: 22,
    fontWeight: 'bold',
    marginBottom: 20,
    textAlign: 'center',
    color: '#2c3e50',
  },
  input: {
    backgroundColor: '#fff',
    color: '#333',
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
    marginBottom: 16,
  },
  modalButtons: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 20,
    gap: 12,
  },
  modalButton: {
    padding: 16,
    borderRadius: 12,
    flex: 1,
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
    fontSize: 16,
  },
  cancelButtonText: {
    color: '#2c3e50',
    fontWeight: 'bold',
    fontSize: 16,
  },
  linkText: {
    color: "#4a89dc",
    marginTop: 10,
    textAlign: "center",
  },
  imagePickerButton: {
    borderWidth: 1,
    borderColor: '#4a89dc',
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
    alignItems: 'center',
  },
  imagePickerText: {
    color: '#4a89dc',
    fontWeight: 'bold',
    fontSize: 16,
  },
  previewImage: {
    width: '100%',
    height: 150,
    borderRadius: 12,
    marginBottom: 16,
  },
  messageCard: {
    backgroundColor: "#27ae60",
  },
  vetCard: {
    backgroundColor: "#8e44ad",
  },
  modalHeader: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    paddingHorizontal: 20,
    paddingVertical: 16,
    backgroundColor: "#fff",
    borderBottomWidth: 1,
    borderBottomColor: "#ecf0f1",
  },
  chatContainer: {
    flex: 1,
  },
  messagesContainer: {
    padding: 16,
  },
  messageItem: {
    maxWidth: "80%",
    marginBottom: 12,
    padding: 12,
    borderRadius: 12,
  },
  sentMessage: {
    alignSelf: "flex-end",
    backgroundColor: "#3498db",
  },
  receivedMessage: {
    alignSelf: "flex-start",
    backgroundColor: "#ecf0f1",
  },
  messageContent: {
    fontSize: 14,
    lineHeight: 20,
    color: "#2c3e50",
  },
  messageTime: {
    fontSize: 10,
    marginTop: 4,
    opacity: 0.7,
    color: "#7f8c8d",
  },
  messageInputContainer: {
    flexDirection: "row",
    padding: 16,
    backgroundColor: "#fff",
    borderTopWidth: 1,
    borderTopColor: "#ecf0f1",
    alignItems: "flex-end",
  },
  messageInput: {
    flex: 1,
    borderWidth: 1,
    borderColor: "#ddd",
    borderRadius: 20,
    paddingHorizontal: 16,
    paddingVertical: 8,
    marginRight: 8,
    maxHeight: 100,
    fontSize: 14,
  },
  sendButton: {
    backgroundColor: "#3498db",
    borderRadius: 20,
    width: 40,
    height: 40,
    justifyContent: "center",
    alignItems: "center",
  },
  conversationItem: {
    backgroundColor: "#fff",
    padding: 16,
    borderRadius: 12,
    marginBottom: 12,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
    position: "relative",
  },
  conversationHeader: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 8,
  },
  conversationName: {
    fontSize: 16,
    fontWeight: "bold",
    color: "#2c3e50",
  },
  conversationTime: {
    fontSize: 12,
    color: "#95a5a6",
  },
  conversationPreview: {
    fontSize: 14,
    color: "#34495e",
    lineHeight: 20,
  },
  unreadBadge: {
    position: "absolute",
    top: 8,
    right: 8,
    backgroundColor: "#e74c3c",
    borderRadius: 10,
    paddingHorizontal: 6,
    paddingVertical: 2,
    minWidth: 20,
    alignItems: "center",
  },
  unreadText: {
    color: "#fff",
    fontSize: 10,
    fontWeight: "bold",
  },
  vetItem: {
    backgroundColor: "#fff",
    padding: 16,
    borderRadius: 12,
    marginBottom: 12,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },
  vetInfo: {
    flex: 1,
  },
  vetName: {
    fontSize: 16,
    fontWeight: "bold",
    color: "#2c3e50",
    marginBottom: 4,
  },
  vetSpecialty: {
    fontSize: 14,
    color: "#27ae60",
    marginBottom: 2,
  },
  vetLocation: {
    fontSize: 12,
    color: "#7f8c8d",
    marginBottom: 2,
  },
  vetRating: {
    fontSize: 12,
    color: "#a16605ff",
  },
  chatButton: {
    backgroundColor: "#025c98ff",
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 8,
    gap: 4,
  },
  chatButtonText: {
    color: "#fff",
    fontSize: 12,
    fontWeight: "bold",
  },
});