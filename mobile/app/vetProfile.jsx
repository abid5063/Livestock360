import { useState, useEffect } from "react";
import { 
  Text, 
  View, 
  TouchableOpacity, 
  StyleSheet, 
  ScrollView, 
  Dimensions, 
  Alert,
  Modal,
  FlatList,
  TextInput,
  RefreshControl
} from "react-native";
import { Ionicons } from '@expo/vector-icons';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { useRouter, useLocalSearchParams } from "expo-router";
import axios from "axios";
import { API_BASE_URL } from "../utils/apiConfig"; // Adjust the import path as needed
import { useLanguage } from '../utils/LanguageContext';
import { useTranslation } from 'react-i18next';
const { width } = Dimensions.get('window');

export default function VetProfile() {
  const router = useRouter();
  const params = useLocalSearchParams();
  const { language } = useLanguage();
  const { t, i18n } = useTranslation();

  // Update i18n language when language changes
  useEffect(() => {
    i18n.changeLanguage(language);
  }, [language, i18n]);

  const [vetData, setVetData] = useState(null);
  const [appointments, setAppointments] = useState([]);
  const [messages, setMessages] = useState([]);
  const [conversations, setConversations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [showAppointments, setShowAppointments] = useState(false);
  const [showMessages, setShowMessages] = useState(false);
  const [selectedConversation, setSelectedConversation] = useState(null);
  const [newMessage, setNewMessage] = useState('');
  const [conversationMessages, setConversationMessages] = useState([]);

  // const API_BASE_URL = "http://localhost:3000/api";

  useEffect(() => {
    loadVetData();
  }, []);

  useEffect(() => {
    if (vetData) {
      fetchDashboardData();
    }
  }, [vetData]);

  const loadVetData = async () => {
    try {
      const token = await AsyncStorage.getItem('authToken');
      if (!token) {
        router.replace('/');
        return;
      }

      let vet = null;
      
      // Try to get from params first
      if (params.vet) {
        vet = JSON.parse(params.vet);
      } else {
        // Fallback to AsyncStorage
        const userData = await AsyncStorage.getItem('userData');
        if (userData) {
          vet = JSON.parse(userData);
        }
      }

      if (vet) {
        setVetData(vet);
      } else {
        // No profile data found
        setLoading(false);
        return;
      }

      setLoading(false);
    } catch (error) {
      console.error("Error loading vet data:", error);
      setLoading(false);
    }
  };

  const fetchDashboardData = async () => {
    try {
      const token = await AsyncStorage.getItem('authToken');
      
      // Fetch appointments
      const appointmentsResponse = await axios.get(`${API_BASE_URL}/api/appointments/vet`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setAppointments(appointmentsResponse.data.appointments || []);

      // Fetch conversations
      const conversationsResponse = await axios.get(`${API_BASE_URL}/api/messages/conversations`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setConversations(conversationsResponse.data.conversations || []);

      // Fetch recent messages for dashboard
      const messagesResponse = await axios.get(`${API_BASE_URL}/api/messages/vet`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setMessages(messagesResponse.data || []);

    } catch (error) {
      console.error("Error fetching dashboard data:", error);
      if (error.response?.status === 401) {
        Alert.alert("Session Expired", "Please login again");
        await AsyncStorage.multiRemove(['authToken', 'userData']);
        router.replace('/');
      }
    }
  };

  const onRefresh = async () => {
    setRefreshing(true);
    await fetchDashboardData();
    setRefreshing(false);
  };

  const handleLogout = async () => {
    Alert.alert(
      t('vetProfile.signOut'),
      t('vetProfile.signOutMessage'),
      [
        { text: t('vetProfile.cancel'), style: 'cancel' },
        {
          text: t('vetProfile.logOut'),
          style: 'destructive',
          onPress: async () => {
            try {
              await AsyncStorage.multiRemove(['authToken', 'userData']);
              router.replace('/');
            } catch (error) {
              console.error('Error during logout:', error);
            }
          }
        }
      ]
    );
  };

  const handleAppointmentAction = async (appointmentId, action) => {
    try {
      const token = await AsyncStorage.getItem('authToken');
      await axios.put(
        `${API_BASE_URL}/api/appointments/${appointmentId}`,
        { status: action },
        { headers: { Authorization: `Bearer ${token}` } }
      );
      fetchDashboardData(); // Refresh data
      Alert.alert(t('vetProfile.success'), t('vetProfile.appointmentActionSuccess', { action }));
    } catch (error) {
      console.error("Error updating appointment:", error);
      Alert.alert(t('vetProfile.error'), t('vetProfile.failedToUpdateAppointment'));
    }
  };

  const openConversation = async (conversation) => {
    try {
      setSelectedConversation(conversation);
      const token = await AsyncStorage.getItem('authToken');
      
      const response = await axios.get(
        `${API_BASE_URL}/api/messages/conversation/${conversation.participant.id}/farmer`,
        { headers: { Authorization: `Bearer ${token}` } }
      );
      
      setConversationMessages(response.data.messages || []);
      setShowMessages(true);
    } catch (error) {
      console.error("Error fetching conversation:", error);
      Alert.alert(t('vetProfile.error'), t('vetProfile.failedToLoadConversation'));
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
          receiverType: 'farmer',
          content: newMessage.trim()
        },
        { headers: { Authorization: `Bearer ${token}` } }
      );

      setNewMessage('');
      
      // Refresh conversation
      const response = await axios.get(
        `${API_BASE_URL}/api/messages/conversation/${selectedConversation.participant.id}/farmer`,
        { headers: { Authorization: `Bearer ${token}` } }
      );
      
      setConversationMessages(response.data.messages || []);
      
    } catch (error) {
      console.error("Error sending message:", error);
      Alert.alert(t('vetProfile.error'), t('vetProfile.failedToSendMessage'));
    }
  };

  const featureCards = [
    {
      title: "Appointments",
      subtitle: "Manage appointments",
      icon: "calendar",
      color: "#044c7cff",
      count: appointments.filter(a => a.status === 'pending').length,
      onPress: () => router.push('/appointmentManagement'),
      testID: "appointments-card"
    },
    {
      title: "Messages",
      subtitle: "Chat with farmers",
      icon: "chatbubbles",
      color: "#026029ff",
      count: conversations.filter(c => c.unreadCount > 0).length,
      onPress: () => router.push('/vetMessaging'),
      testID: "messages-card"
    },
    {
      title: "Emergency Cases",
      subtitle: "Urgent consultations",
      icon: "medical",
      color: "#821105ff",
      count: appointments.filter(a => a.priority === 'emergency').length,
      onPress: () => Alert.alert("Emergency", "Emergency cases feature coming soon"),
      testID: "emergency-card"
    },
    {
      title: "Edit Profile",
      subtitle: "Update your details",
      icon: "person",
      color: "#577344ff",
      onPress: () => router.push({
        pathname: '/vetEditProfile',
        params: { vet: JSON.stringify(vetData) }
      }),
      testID: "edit-profile-card"
    }
  ];

  const renderAppointmentItem = ({ item }) => (
    <View style={styles.appointmentItem} testID={`appointment-${item.id}`}>
      <View style={styles.appointmentHeader}>
        <Text style={styles.appointmentFarmer}>{item.farmerName}</Text>
        <Text style={[styles.appointmentStatus, { color: getStatusColor(item.status) }]}>
          {item.status}
        </Text>
      </View>
      <Text style={styles.appointmentAnimal}>{item.animalType} - {item.animalName}</Text>
      <Text style={styles.appointmentDescription}>{item.description}</Text>
      <Text style={styles.appointmentDate}>{new Date(item.date).toLocaleDateString()}</Text>
      
      {item.status === 'pending' && (
        <View style={styles.appointmentActions}>
          <TouchableOpacity
            style={[styles.actionButton, styles.acceptButton]}
            onPress={() => handleAppointmentAction(item.id, 'accepted')}
            testID={`accept-appointment-${item.id}`}
          >
            <Text style={styles.actionButtonText}>Accept</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.actionButton, styles.rejectButton]}
            onPress={() => handleAppointmentAction(item.id, 'rejected')}
            testID={`reject-appointment-${item.id}`}
          >
            <Text style={styles.actionButtonText}>Reject</Text>
          </TouchableOpacity>
        </View>
      )}
    </View>
  );

  const renderConversationItem = ({ item }) => (
    <TouchableOpacity
      style={styles.conversationItem}
      onPress={() => openConversation(item)}
      testID={`conversation-${item.participant.id}`}
    >
      <View style={styles.conversationContent}>
        <View style={styles.conversationAvatar}>
          <Text style={styles.avatarText}>
            {item.participant.name.charAt(0).toUpperCase()}
          </Text>
        </View>
        
        <View style={styles.conversationDetails}>
          <View style={styles.conversationHeader}>
            <Text style={styles.conversationName}>{item.participant.name}</Text>
            <Text style={styles.conversationTime}>
              {new Date(item.lastMessage.timestamp).toLocaleDateString()}
            </Text>
          </View>
          <Text style={styles.conversationPreview} numberOfLines={2}>
            {item.lastMessage.content}
          </Text>
        </View>
        
        {item.unreadCount > 0 && (
          <View style={styles.unreadBadge}>
            <Text style={styles.unreadText}>{item.unreadCount}</Text>
          </View>
        )}
      </View>
    </TouchableOpacity>
  );

  const renderMessageItem = ({ item }) => (
    <View style={[
      styles.messageItem,
      item.senderType === 'vet' ? styles.sentMessage : styles.receivedMessage
    ]}>
      <Text style={[
        styles.messageContent,
        { color: item.senderType === 'vet' ? '#fff' : '#2c3e50' }
      ]}>
        {item.content}
      </Text>
      <Text style={[
        styles.messageTime,
        { color: item.senderType === 'vet' ? 'rgba(255,255,255,0.8)' : '#7f8c8d' }
      ]}>
        {new Date(item.createdAt).toLocaleTimeString()}
      </Text>
    </View>
  );

  const getStatusColor = (status) => {
    switch (status) {
      case 'pending': return '#a76906ff';
      case 'accepted': return '#0c6702ff';
      case 'rejected': return '#8a1103ff';
      case 'completed': return '#7b989aff';
      default: return '#34495e';
    }
  };

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <Text style={styles.loadingText}>{t('vetProfile.loadingProfile')}</Text>
      </View>
    );
  }

  if (!vetData) {
    return (
      <View style={styles.errorContainer}>
        <Ionicons name="person-circle-outline" size={100} color="#bdc3c7" />
        <Text style={styles.errorText}>{t('vetProfile.noProfileData')}</Text>
        <TouchableOpacity
          style={styles.errorButton}
          onPress={() => router.replace('/')}
          testID="go-to-login-button"
        >
          <Text style={styles.errorButtonText}>{t('vetProfile.goToLogin')}</Text>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <ScrollView 
      style={styles.container}
      refreshControl={
        <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
      }
    >
      {/* Header */}
      <View style={styles.header}>
        <View style={styles.headerContent}>
          <View>
            <Text style={styles.greeting}>{t('vetProfile.goodDay')}</Text>
            <Text style={styles.vetName}>{vetData.name}</Text>
            <Text style={styles.specialty}>{vetData.specialty}</Text>
                          <Text style={styles.licenseNumber}>{t('vetProfile.license')} {vetData.licenseNumber}</Text>
          </View>
          <TouchableOpacity 
            style={styles.logoutButton}
            onPress={handleLogout}
            testID="logout-button"
          >
            <Ionicons name="log-out-outline" size={24} color="#e74c3c" />
          </TouchableOpacity>
        </View>
      </View>

      {/* Stats Cards */}
      <View style={styles.statsContainer}>
        <View style={styles.statCard}>
          <Text style={styles.statNumber}>{appointments.length}</Text>
                      <Text style={styles.statLabel}>{t('vetProfile.totalAppointments')}</Text>
        </View>
        <View style={styles.statCard}>
          <Text style={styles.statNumber}>{conversations.length}</Text>
                      <Text style={styles.statLabel}>{t('vetProfile.conversations')}</Text>
        </View>
        <View style={styles.statCard}>
          <Text style={styles.statNumber}>
            {appointments.filter(a => a.status === 'completed').length}
          </Text>
                      <Text style={styles.statLabel}>{t('vetProfile.completed')}</Text>
        </View>
      </View>

      {/* Feature Cards */}
              <Text style={styles.sectionTitle}>{t('vetProfile.dashboard')}</Text>
      <View style={styles.featureGrid}>
        {featureCards.map((card, index) => (
          <TouchableOpacity
            key={index}
            style={[styles.featureCard, { backgroundColor: card.color }]}
            onPress={card.onPress}
            testID={card.testID}
          >
            <View style={styles.cardHeader}>
              <Ionicons name={card.icon} size={32} color="#fff" />
              {card.count > 0 && (
                <View style={styles.badge}>
                  <Text style={styles.badgeText}>{card.count}</Text>
                </View>
              )}
            </View>
            <Text style={styles.cardTitle}>{card.title}</Text>
            <Text style={styles.cardSubtitle}>{card.subtitle}</Text>
          </TouchableOpacity>
        ))}
      </View>

      {/* Appointments Modal */}
      <Modal
        visible={showAppointments}
        animationType="slide"
        presentationStyle="pageSheet"
      >
        <View style={styles.modalContainer}>
          <View style={styles.modalHeader}>
            <Text style={styles.modalTitle}>{t('vetProfile.appointments')}</Text>
            <TouchableOpacity
              onPress={() => setShowAppointments(false)}
              testID="close-appointments-modal"
            >
              <Ionicons name="close" size={24} color="#34495e" />
            </TouchableOpacity>
          </View>
          <FlatList
            data={appointments}
            renderItem={renderAppointmentItem}
            keyExtractor={(item) => item.id.toString()}
            contentContainerStyle={styles.modalContent}
            showsVerticalScrollIndicator={false}
          />
        </View>
      </Modal>

      {/* Messages Modal */}
      <Modal
        visible={showMessages}
        animationType="slide"
        presentationStyle="fullScreen"
      >
        <View style={styles.fullScreenModal}>
          <View style={styles.modalHeader}>
            <TouchableOpacity
              onPress={() => {
                if (selectedConversation) {
                  setSelectedConversation(null);
                  setConversationMessages([]);
                } else {
                  setShowMessages(false);
                }
              }}
              style={styles.headerButton}
              testID="close-messages-modal"
            >
              <Ionicons 
                name={selectedConversation ? "arrow-back" : "close"} 
                size={24} 
                color="#fff" 
              />
            </TouchableOpacity>
            
            <Text style={styles.modalTitle}>
              {selectedConversation ? selectedConversation.participant.name : "Messages"}
            </Text>
            
            {!selectedConversation && (
              <TouchableOpacity
                style={styles.headerButton}
                onPress={() => {/* Add search functionality */}}
              >
                <Ionicons name="search" size={24} color="#fff" />
              </TouchableOpacity>
            )}
            
            {selectedConversation && <View style={styles.headerButton} />}
          </View>
          
          {selectedConversation ? (
            <View style={styles.chatContainer}>
              <FlatList
                data={conversationMessages}
                renderItem={renderMessageItem}
                keyExtractor={(item) => item._id}
                contentContainerStyle={styles.messagesContainer}
                showsVerticalScrollIndicator={false}
              />
              <View style={styles.messageInputContainer}>
                <TextInput
                  style={styles.messageInput}
                  value={newMessage}
                  onChangeText={setNewMessage}
                  placeholder="Type a message..."
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
            <View style={styles.conversationsContainer}>
              {conversations.length > 0 ? (
                <FlatList
                  data={conversations}
                  renderItem={renderConversationItem}
                  keyExtractor={(item) => item.conversationId}
                  contentContainerStyle={styles.conversationsList}
                  showsVerticalScrollIndicator={false}
                />
              ) : (
                <View style={styles.emptyConversations}>
                  <Ionicons name="chatbubbles-outline" size={64} color="#bdc3c7" />
                                <Text style={styles.emptyConversationsTitle}>{t('vetProfile.noConversations')}</Text>
              <Text style={styles.emptyConversationsText}>
                    {t('vetProfile.farmerConsultationsWillAppear')}
                  </Text>
                </View>
              )}
            </View>
          )}
        </View>
      </Modal>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#f5f6fa",
  },
  loadingContainer: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    backgroundColor: "#f5f6fa",
  },
  loadingText: {
    fontSize: 18,
    color: "#7f8c8d",
    marginTop: 20,
  },
  errorContainer: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    backgroundColor: "#f5f6fa",
    padding: 20,
  },
  errorText: {
    fontSize: 18,
    color: "#7f8c8d",
    marginTop: 20,
    marginBottom: 30,
  },
  errorButton: {
    backgroundColor: "#3498db",
    paddingHorizontal: 30,
    paddingVertical: 12,
    borderRadius: 8,
  },
  errorButtonText: {
    color: "#fff",
    fontSize: 16,
    fontWeight: "bold",
  },
  header: {
    backgroundColor: "#fff",
    paddingTop: 60,
    paddingHorizontal: 20,
    paddingBottom: 20,
    borderBottomLeftRadius: 24,
    borderBottomRightRadius: 24,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 8,
    elevation: 5,
  },
  headerContent: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },
  greeting: {
    fontSize: 16,
    color: "#7f8c8d",
  },
  vetName: {
    fontSize: 24,
    fontWeight: "bold",
    color: "#2c3e50",
    marginTop: 4,
  },
  specialty: {
    fontSize: 14,
    color: "#27ae60",
    marginTop: 2,
  },
  licenseNumber: {
    fontSize: 12,
    color: "#95a5a6",
    marginTop: 2,
  },
  logoutButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: "#fff",
    justifyContent: "center",
    alignItems: "center",
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  statsContainer: {
    flexDirection: "row",
    justifyContent: "space-around",
    paddingHorizontal: 20,
    paddingVertical: 20,
  },
  statCard: {
    backgroundColor: "#fff",
    padding: 16,
    borderRadius: 12,
    alignItems: "center",
    flex: 1,
    marginHorizontal: 4,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  statNumber: {
    fontSize: 24,
    fontWeight: "bold",
    color: "#2c3e50",
  },
  statLabel: {
    fontSize: 12,
    color: "#7f8c8d",
    marginTop: 4,
  },
  sectionTitle: {
    fontSize: 20,
    fontWeight: "bold",
    color: "#2c3e50",
    marginLeft: 20,
    marginBottom: 16,
  },
  featureGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    paddingHorizontal: 20,
    justifyContent: "space-between",
  },
  featureCard: {
    width: "48%",
    padding: 20,
    borderRadius: 16,
    marginBottom: 12,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.1,
    shadowRadius: 8,
    elevation: 5,
  },
  cardHeader: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 12,
  },
  badge: {
    backgroundColor: "rgba(255, 255, 255, 0.9)",
    borderRadius: 10,
    paddingHorizontal: 6,
    paddingVertical: 2,
    minWidth: 20,
    alignItems: "center",
  },
  badgeText: {
    fontSize: 12,
    fontWeight: "bold",
    color: "#e74c3c",
  },
  cardTitle: {
    fontSize: 16,
    fontWeight: "bold",
    color: "#fff",
    marginBottom: 4,
  },
  cardSubtitle: {
    fontSize: 12,
    color: "rgba(255, 255, 255, 0.8)",
  },
  modalContainer: {
    flex: 1,
    backgroundColor: "#f5f6fa",
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
  modalTitle: {
    fontSize: 20,
    fontWeight: "bold",
    color: "#2c3e50",
  },
  modalContent: {
    padding: 20,
  },
  appointmentItem: {
    backgroundColor: "#fff",
    padding: 16,
    borderRadius: 12,
    marginBottom: 12,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  appointmentHeader: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 8,
  },
  appointmentFarmer: {
    fontSize: 16,
    fontWeight: "bold",
    color: "#2c3e50",
  },
  appointmentStatus: {
    fontSize: 12,
    fontWeight: "bold",
    textTransform: "uppercase",
  },
  appointmentAnimal: {
    fontSize: 14,
    color: "#34495e",
    marginBottom: 4,
  },
  appointmentDescription: {
    fontSize: 14,
    color: "#7f8c8d",
    marginBottom: 8,
  },
  appointmentDate: {
    fontSize: 12,
    color: "#95a5a6",
    marginBottom: 12,
  },
  appointmentActions: {
    flexDirection: "row",
    gap: 8,
  },
  actionButton: {
    flex: 1,
    padding: 8,
    borderRadius: 8,
    alignItems: "center",
  },
  acceptButton: {
    backgroundColor: "#27ae60",
  },
  rejectButton: {
    backgroundColor: "#e74c3c",
  },
  actionButtonText: {
    color: "#fff",
    fontWeight: "bold",
    fontSize: 12,
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
  },
  messageTime: {
    fontSize: 10,
    marginTop: 4,
    opacity: 0.7,
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
});
