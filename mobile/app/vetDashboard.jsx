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
  FlatList
} from "react-native";
import { Ionicons } from '@expo/vector-icons';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { useRouter } from "expo-router";
import axios from "axios";
import { API_BASE_URL } from '../utils/apiConfig'; // Adjust the import path as needed
import { useLanguage } from '../utils/LanguageContext';
import { useTranslation } from 'react-i18next';
const { width } = Dimensions.get('window');

export default function VetDashboard() {
  const router = useRouter();
  const { language } = useLanguage();
  const { t, i18n } = useTranslation();

  // Update i18n language when language changes
  useEffect(() => {
    i18n.changeLanguage(language);
  }, [language, i18n]);

  const [vetData, setVetData] = useState(null);
  const [appointments, setAppointments] = useState([]);
  const [messages, setMessages] = useState([]);
  const [showAppointments, setShowAppointments] = useState(false);
  const [showMessages, setShowMessages] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  // const API_BASE_URL = "http://localhost:3000/api";

  useEffect(() => {
    loadVetData();
    fetchAppointments();
    fetchMessages();
  }, []);

  const loadVetData = async () => {
    try {
      const userData = await AsyncStorage.getItem('userData');
      if (userData) {
        setVetData(JSON.parse(userData));
      }
    } catch (error) {
      console.error("Error loading vet data:", error);
    }
  };

  const fetchAppointments = async () => {
    try {
      const token = await AsyncStorage.getItem('authToken');
      const response = await axios.get(`${API_BASE_URL}/api/appointments/vet`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setAppointments(response.data);
    } catch (error) {
      console.error("Error fetching appointments:", error);
    }
  };

  const fetchMessages = async () => {
    try {
      const token = await AsyncStorage.getItem('authToken');
      const response = await axios.get(`${API_BASE_URL}/api/messages/vet`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setMessages(response.data);
    } catch (error) {
      console.error("Error fetching messages:", error);
    }
  };

  const handleLogout = async () => {
    Alert.alert(
      t('vetDashboard.logout'),
      t('vetDashboard.logoutMessage'),
      [
        { text: t('vetDashboard.cancel'), style: "cancel" },
        {
          text: t('vetDashboard.logout'),
          style: "destructive",
          onPress: async () => {
            try {
              await AsyncStorage.multiRemove(['authToken', 'userData']);
              router.replace('/');
            } catch (error) {
              console.error("Error during logout:", error);
            }
          }
        }
      ]
    );
  };

  const handleAppointmentAction = async (appointmentId, action) => {
    try {
      setIsLoading(true);
      const token = await AsyncStorage.getItem('authToken');
      await axios.put(
        `${API_BASE_URL}/api/appointments/${appointmentId}`,
        { status: action },
        { headers: { Authorization: `Bearer ${token}` } }
      );
      fetchAppointments(); // Refresh appointments
      Alert.alert("Success", `Appointment ${action} successfully`);
    } catch (error) {
      console.error("Error updating appointment:", error);
      Alert.alert("Error", "Failed to update appointment");
    } finally {
      setIsLoading(false);
    }
  };

  const featureCards = [
    {
      title: "Appointments",
      subtitle: "Manage your appointments",
      icon: "calendar",
      color: "#05609dff",
      count: appointments.filter(a => a.status === 'pending').length,
      onPress: () => setShowAppointments(true),
      testID: "appointments-card"
    },
    {
      title: "Messages",
      subtitle: "Chat with farmers",
      icon: "chatbubbles",
      color: "#037934ff",
      count: messages.filter(m => !m.read).length,
      onPress: () => setShowMessages(true),
      testID: "messages-card"
    },
    {
      title: "Emergency Cases",
      subtitle: "Urgent consultations",
      icon: "medical",
      color: "#6c0d03ff",
      count: appointments.filter(a => a.priority === 'emergency').length,
      onPress: () => Alert.alert("Emergency", "Emergency cases feature coming soon"),
      testID: "emergency-card"
    },
    {
      title: "Profile",
      subtitle: "Manage your profile",
      icon: "person",
      color: "#6e9a60ff",
      onPress: () => Alert.alert("Profile", "Profile management coming soon"),
      testID: "profile-card"
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

  const renderMessageItem = ({ item }) => (
    <View style={styles.messageItem} testID={`message-${item.id}`}>
      <View style={styles.messageHeader}>
        <Text style={styles.messageFarmer}>{item.farmerName}</Text>
        <Text style={styles.messageTime}>
          {new Date(item.timestamp).toLocaleDateString()}
        </Text>
      </View>
      <Text style={styles.messageContent}>{item.content}</Text>
      {!item.read && <View style={styles.unreadIndicator} />}
    </View>
  );

  const getStatusColor = (status) => {
    switch (status) {
      case 'pending': return '#f39c12';
      case 'accepted': return '#27ae60';
      case 'rejected': return '#e74c3c';
      case 'completed': return '#95a5a6';
      default: return '#34495e';
    }
  };

  return (
    <ScrollView style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <View style={styles.headerContent}>
          <View>
            <Text style={styles.greeting}>{t('vetDashboard.goodDay')}</Text>
            <Text style={styles.vetName}>{vetData?.name || t('vetDashboard.veterinarian')}</Text>
            <Text style={styles.specialty}>{vetData?.specialty}</Text>
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
          <Text style={styles.statLabel}>{t('vetDashboard.totalAppointments')}</Text>
        </View>
        <View style={styles.statCard}>
          <Text style={styles.statNumber}>{messages.length}</Text>
          <Text style={styles.statLabel}>{t('vetDashboard.messages')}</Text>
        </View>
        <View style={styles.statCard}>
          <Text style={styles.statNumber}>
            {appointments.filter(a => a.status === 'completed').length}
          </Text>
          <Text style={styles.statLabel}>Completed</Text>
        </View>
      </View>

      {/* Feature Cards */}
      <Text style={styles.sectionTitle}>Dashboard</Text>
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
            <Text style={styles.modalTitle}>Appointments</Text>
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
        presentationStyle="pageSheet"
      >
        <View style={styles.modalContainer}>
          <View style={styles.modalHeader}>
            <Text style={styles.modalTitle}>Messages</Text>
            <TouchableOpacity
              onPress={() => setShowMessages(false)}
              testID="close-messages-modal"
            >
              <Ionicons name="close" size={24} color="#34495e" />
            </TouchableOpacity>
          </View>
          <FlatList
            data={messages}
            renderItem={renderMessageItem}
            keyExtractor={(item) => item.id.toString()}
            contentContainerStyle={styles.modalContent}
            showsVerticalScrollIndicator={false}
          />
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
    paddingBottom: 30,
  },
  featureCard: {
    width: (width - 60) / 2,
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
  messageItem: {
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
  messageHeader: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 8,
  },
  messageFarmer: {
    fontSize: 16,
    fontWeight: "bold",
    color: "#2c3e50",
  },
  messageTime: {
    fontSize: 12,
    color: "#95a5a6",
  },
  messageContent: {
    fontSize: 14,
    color: "#34495e",
    lineHeight: 20,
  },
  unreadIndicator: {
    position: "absolute",
    top: 8,
    right: 8,
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: "#e74c3c",
  },
});
