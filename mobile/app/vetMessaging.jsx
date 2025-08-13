import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  TextInput,
  Modal,
  Alert,
  RefreshControl,
  Image,
  Dimensions
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import AsyncStorage from '@react-native-async-storage/async-storage';
import axios from 'axios';
import { API_BASE_URL } from '../utils/apiConfig'; // Adjust the import path as needed
import { useLanguage } from '../utils/LanguageContext';
import { useTranslation } from 'react-i18next';
const { width } = Dimensions.get('window');

export default function VetMessaging() {
  const router = useRouter();
  const { language } = useLanguage();
  const { t, i18n } = useTranslation();

  // Update i18n language when language changes
  useEffect(() => {
    i18n.changeLanguage(language);
  }, [language, i18n]);

  const [conversations, setConversations] = useState([]);
  const [searchResults, setSearchResults] = useState([]);
  const [showSearch, setShowSearch] = useState(false);
  const [showChat, setShowChat] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedConversation, setSelectedConversation] = useState(null);
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState('');
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [vet, setVet] = useState(null);

  // const API_BASE_URL = "http://localhost:3000/api";

  useEffect(() => {
    loadVetData();
  }, []);

  useEffect(() => {
    if (vet) {
      fetchConversations();
    }
  }, [vet]);

  const loadVetData = async () => {
    try {
      const storedData = await AsyncStorage.getItem('userData');
      if (storedData) {
        setVet(JSON.parse(storedData));
      }
    } catch (error) {
      console.error("Error loading vet data:", error);
    } finally {
      setLoading(false);
    }
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

  const searchFarmers = async (query = '') => {
    try {
      const response = await axios.get(`${API_BASE_URL}/api/auth/farmers/search`, {
        params: query.trim() !== '' ? { search: query } : {}
      });
      
      setSearchResults(response.data.farmers || []);
    } catch (error) {
      console.error("Error searching farmers:", error);
      Alert.alert(t('vetMessaging.error'), t('vetMessaging.failedToSearchFarmers'));
    }
  };

  const startNewConversation = async (farmer) => {
    try {
      const conversation = {
        participant: {
          id: farmer._id,
          name: farmer.name,
          location: farmer.location,
          avatar: farmer.profileImage
        },
        messages: []
      };
      setSelectedConversation(conversation);
      setMessages([]);
      setShowSearch(false);
      setShowChat(true);
    } catch (error) {
      console.error("Error starting conversation:", error);
    }
  };

  const openExistingConversation = async (conversation) => {
    try {
      setSelectedConversation(conversation);
      const token = await AsyncStorage.getItem('authToken');
      
      // Use the participant's _id or id field
      const participantId = conversation.participant._id || conversation.participant.id;
      
      if (!participantId) {
        console.error("No participant ID found in conversation:", conversation);
        Alert.alert("Error", "Invalid conversation data");
        return;
      }
      
      const response = await axios.get(
        `${API_BASE_URL}/api/messages/conversation/${participantId}/farmer`,
        { headers: { Authorization: `Bearer ${token}` } }
      );
      
      setMessages(response.data.messages || []);
      setShowChat(true);
    } catch (error) {
      console.error("Error fetching conversation:", error);
      Alert.alert("Error", "Failed to load conversation");
    }
  };

  const sendMessage = async () => {
    if (!newMessage.trim() || !selectedConversation) return;

    const messageText = newMessage.trim();
    const tempMessage = {
      _id: Date.now().toString(), // Temporary ID
      content: messageText,
      senderType: 'vet',
      createdAt: new Date().toISOString(),
      isTemp: true
    };

    try {
      const token = await AsyncStorage.getItem('authToken');
      
      // Add message to UI immediately for better UX
      setMessages(prev => [tempMessage, ...prev]);
      setNewMessage('');
      
      // Use the participant's _id or id field
      const participantId = selectedConversation.participant._id || selectedConversation.participant.id;
      
      if (!participantId) {
        console.error("No participant ID found in conversation:", selectedConversation);
        Alert.alert("Error", "Invalid conversation data");
        return;
      }
      
      const response = await axios.post(
        `${API_BASE_URL}/api/messages`,
        {
          receiverId: participantId,
          receiverType: 'farmer',
          content: messageText
        },
        { headers: { Authorization: `Bearer ${token}` } }
      );

      // Refresh messages to get the actual saved message
      const messagesResponse = await axios.get(
        `${API_BASE_URL}/api/messages/conversation/${participantId}/farmer`,
        { headers: { Authorization: `Bearer ${token}` } }
      );
      
      setMessages(messagesResponse.data.messages || []);
      
      // Refresh conversations list
      fetchConversations();
      
    } catch (error) {
      console.error("Error sending message:", error);
      // Remove the temporary message on error
      setMessages(prev => prev.filter(msg => msg._id !== tempMessage._id));
      setNewMessage(messageText); // Restore the message text
      Alert.alert("Error", "Failed to send message");
    }
  };

  const onRefresh = async () => {
    setRefreshing(true);
    await fetchConversations();
    setRefreshing(false);
  };

  const formatMessageTime = (timestamp) => {
    const date = new Date(timestamp);
    const now = new Date();
    const diffMs = now - date;
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

    if (diffDays === 0) {
      return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } else if (diffDays === 1) {
      return 'Yesterday';
    } else if (diffDays < 7) {
      return date.toLocaleDateString([], { weekday: 'short' });
    } else {
      return date.toLocaleDateString([], { month: 'short', day: 'numeric' });
    }
  };

  const renderConversationItem = ({ item }) => (
    <TouchableOpacity
      style={styles.conversationItem}
      onPress={() => openExistingConversation(item)}
    >
      <View style={styles.avatarContainer}>
        {item.participant.avatar ? (
          <Image source={{ uri: item.participant.avatar }} style={styles.avatar} />
        ) : (
          <View style={styles.avatarPlaceholder}>
            <Ionicons name="person" size={24} color="#7f8c8d" />
          </View>
        )}
        {item.isOnline && <View style={styles.onlineIndicator} />}
      </View>
      
      <View style={styles.conversationContent}>
        <View style={styles.conversationHeader}>
          <Text style={styles.participantName} numberOfLines={1}>
            {item.participant.name}
          </Text>
          <Text style={styles.messageTime}>
            {formatMessageTime(item.lastMessage?.timestamp)}
          </Text>
        </View>
        
        <Text style={styles.participantFarm} numberOfLines={1}>
          {item.participant.location || "Location not specified"}
        </Text>
        
        <Text style={styles.lastMessage} numberOfLines={2}>
          {item.lastMessage?.content || "No messages yet"}
        </Text>
      </View>
      
      {item.unreadCount > 0 && (
        <View style={styles.unreadBadge}>
          <Text style={styles.unreadText}>{item.unreadCount}</Text>
        </View>
      )}
    </TouchableOpacity>
  );

  const renderFarmerSearchItem = ({ item }) => (
    <TouchableOpacity
      style={styles.farmerSearchItem}
      onPress={() => startNewConversation(item)}
    >
      <View style={styles.avatarContainer}>
        {item.profileImage ? (
          <Image source={{ uri: item.profileImage }} style={styles.avatar} />
        ) : (
          <View style={styles.avatarPlaceholder}>
            <Ionicons name="leaf" size={24} color="#27ae60" />
          </View>
        )}
      </View>
      
      <View style={styles.farmerInfo}>
        <Text style={styles.farmerName}>{item.name}</Text>
        <Text style={styles.farmerLocation}>{item.location || "Location not specified"}</Text>
        <Text style={styles.farmerPhone}>{item.phoneNo || "Phone not provided"}</Text>
      </View>
      
      <TouchableOpacity
        style={styles.chatButton}
        onPress={() => startNewConversation(item)}
      >
        <Ionicons name="chatbubbles" size={16} color="#fff" />
      </TouchableOpacity>
    </TouchableOpacity>
  );

  const renderMessage = ({ item }) => (
    <View style={[
      styles.messageContainer,
      item.senderType === 'vet' ? styles.sentMessage : styles.receivedMessage
    ]}>
      <Text style={[
        styles.messageText,
        item.senderType === 'vet' ? styles.sentMessageText : styles.receivedMessageText
      ]}>
        {item.content}
      </Text>
      <Text style={[
        styles.messageTimestamp,
        item.senderType === 'vet' ? styles.sentTimestamp : styles.receivedTimestamp
      ]}>
        {new Date(item.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
      </Text>
    </View>
  );

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <Text>{t('vetMessaging.loadingMessages')}</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()}>
          <Ionicons name="arrow-back" size={24} color="#2c3e50" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>{t('vetMessaging.headerTitle')}</Text>
        <TouchableOpacity onPress={() => {
          setShowSearch(true);
          setSearchQuery(''); // Clear search query
          searchFarmers(); // Load all farmers initially
        }}>
          <Ionicons name="search" size={24} color="#2c3e50" />
        </TouchableOpacity>
      </View>

      {/* Conversations List */}
      {!showChat && (
        <FlatList
          data={conversations}
          renderItem={renderConversationItem}
          keyExtractor={(item) => item.conversationId}
          contentContainerStyle={styles.conversationsList}
          refreshControl={
            <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
          }
          ListEmptyComponent={
            <View style={styles.emptyState}>
              <Ionicons name="chatbubbles-outline" size={64} color="#bdc3c7" />
              <Text style={styles.emptyTitle}>{t('vetMessaging.noPatientConversations')}</Text>
              <Text style={styles.emptySubtitle}>
                {t('vetMessaging.tapSearchToFindFarmers')}
              </Text>
            </View>
          }
        />
      )}

      {/* Chat Screen */}
      {showChat && selectedConversation && (
        <View style={styles.chatScreen}>
          {/* Chat Header */}
          <View style={styles.chatHeader}>
            <TouchableOpacity onPress={() => setShowChat(false)}>
              <Ionicons name="arrow-back" size={24} color="#2c3e50" />
            </TouchableOpacity>
            <View style={styles.chatHeaderInfo}>
              <Text style={styles.chatHeaderName}>
                {selectedConversation.participant.name}
              </Text>
              <Text style={styles.chatHeaderFarm}>
                {selectedConversation.participant.location || t('vetMessaging.locationNotSpecified')}
              </Text>
            </View>
            <View style={styles.chatHeaderActions}>
              {/* Removed appointment button here */}
              <TouchableOpacity>
                <Ionicons name="call" size={24} color="#27ae60" />
              </TouchableOpacity>
            </View>
          </View>

          {/* Messages */}
          <FlatList
            data={messages}
            renderItem={renderMessage}
            keyExtractor={(item) => item._id}
            contentContainerStyle={styles.messagesList}
          />

          {/* Message Input */}
          <View style={styles.messageInputContainer}>
            <TextInput
              style={styles.messageInput}
              value={newMessage}
              onChangeText={setNewMessage}
              placeholder={t('vetMessaging.typeConsultationMessage')}
              multiline
              maxLength={500}
            />
            <TouchableOpacity style={styles.sendButton} onPress={sendMessage}>
              <Ionicons name="send" size={20} color="#fff" />
            </TouchableOpacity>
          </View>
        </View>
      )}

      {/* Search Farmers Modal */}
      <Modal
        visible={showSearch}
        animationType="slide"
        presentationStyle="pageSheet"
      >
        <View style={styles.searchContainer}>
          <View style={styles.searchHeader}>
            <TouchableOpacity onPress={() => setShowSearch(false)}>
              <Ionicons name="close" size={24} color="#2c3e50" />
            </TouchableOpacity>
            <Text style={styles.searchTitle}>Find Farmers</Text>
            <View style={{ width: 24 }} />
          </View>

          <View style={styles.searchInputContainer}>
            <Ionicons name="search" size={20} color="#7f8c8d" />
            <TextInput
              style={styles.searchInput}
              value={searchQuery}
              onChangeText={(text) => {
                setSearchQuery(text);
                searchFarmers(text);
              }}
              placeholder="Search by name, farm, location..."
            />
          </View>

          <FlatList
            data={searchResults}
            renderItem={renderFarmerSearchItem}
            keyExtractor={(item) => item._id}
            contentContainerStyle={styles.searchResults}
            ListEmptyComponent={
              <View style={styles.emptySearch}>
                <Ionicons name="search-outline" size={48} color="#bdc3c7" />
                <Text style={styles.emptySearchText}>
                  {searchQuery ? "No farmers found" : "No farmers available"}
                </Text>
              </View>
            }
          />
        </View>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8f9fa',
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingVertical: 16,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#ecf0f1',
  },
  headerTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#2c3e50',
  },
  conversationsList: {
    paddingVertical: 8,
  },
  conversationItem: {
    flexDirection: 'row',
    paddingHorizontal: 20,
    paddingVertical: 16,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#ecf0f1',
    alignItems: 'center',
  },
  avatarContainer: {
    position: 'relative',
    marginRight: 16,
  },
  avatar: {
    width: 50,
    height: 50,
    borderRadius: 25,
  },
  avatarPlaceholder: {
    width: 50,
    height: 50,
    borderRadius: 25,
    backgroundColor: '#ecf0f1',
    justifyContent: 'center',
    alignItems: 'center',
  },
  onlineIndicator: {
    position: 'absolute',
    bottom: 2,
    right: 2,
    width: 12,
    height: 12,
    borderRadius: 6,
    backgroundColor: '#27ae60',
    borderWidth: 2,
    borderColor: '#fff',
  },
  conversationContent: {
    flex: 1,
  },
  conversationHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 4,
  },
  participantName: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#2c3e50',
    flex: 1,
  },
  messageTime: {
    fontSize: 12,
    color: '#95a5a6',
  },
  participantFarm: {
    fontSize: 12,
    color: '#27ae60',
    marginBottom: 4,
  },
  lastMessage: {
    fontSize: 14,
    color: '#7f8c8d',
    lineHeight: 18,
  },
  unreadBadge: {
    backgroundColor: '#e74c3c',
    borderRadius: 10,
    paddingHorizontal: 6,
    paddingVertical: 2,
    minWidth: 20,
    alignItems: 'center',
    marginLeft: 8,
  },
  unreadText: {
    color: '#fff',
    fontSize: 10,
    fontWeight: 'bold',
  },
  emptyState: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 80,
    paddingHorizontal: 40,
  },
  emptyTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#34495e',
    marginTop: 16,
    marginBottom: 8,
  },
  emptySubtitle: {
    fontSize: 14,
    color: '#7f8c8d',
    textAlign: 'center',
    lineHeight: 20,
  },
  chatScreen: {
    flex: 1,
  },
  chatHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingVertical: 16,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#ecf0f1',
  },
  chatHeaderInfo: {
    flex: 1,
    marginLeft: 16,
  },
  chatHeaderActions: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  appointmentButton: {
    padding: 8,
    borderRadius: 20,
    backgroundColor: '#f8f9fa',
    borderWidth: 1,
    borderColor: '#3498db',
  },
  chatHeaderName: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#2c3e50',
  },
  chatHeaderFarm: {
    fontSize: 12,
    color: '#27ae60',
  },
  messagesList: {
    paddingVertical: 16,
    paddingHorizontal: 20,
  },
  messageContainer: {
    maxWidth: '80%',
    marginBottom: 12,
    padding: 12,
    borderRadius: 16,
  },
  sentMessage: {
    alignSelf: 'flex-end',
    backgroundColor: '#27ae60',
  },
  receivedMessage: {
    alignSelf: 'flex-start',
    backgroundColor: '#ecf0f1',
  },
  messageText: {
    fontSize: 14,
    lineHeight: 18,
  },
  sentMessageText: {
    color: '#fff',
  },
  receivedMessageText: {
    color: '#2c3e50',
  },
  messageTimestamp: {
    fontSize: 10,
    marginTop: 4,
  },
  sentTimestamp: {
    color: 'rgba(255, 255, 255, 0.7)',
    textAlign: 'right',
  },
  receivedTimestamp: {
    color: '#95a5a6',
  },
  messageInputContainer: {
    flexDirection: 'row',
    paddingHorizontal: 20,
    paddingVertical: 16,
    backgroundColor: '#fff',
    borderTopWidth: 1,
    borderTopColor: '#ecf0f1',
    alignItems: 'flex-end',
  },
  messageInput: {
    flex: 1,
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 20,
    paddingHorizontal: 16,
    paddingVertical: 8,
    marginRight: 8,
    maxHeight: 100,
    fontSize: 14,
  },
  sendButton: {
    backgroundColor: '#27ae60',
    borderRadius: 20,
    width: 40,
    height: 40,
    justifyContent: 'center',
    alignItems: 'center',
  },
  searchContainer: {
    flex: 1,
    backgroundColor: '#f8f9fa',
  },
  searchHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingVertical: 16,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#ecf0f1',
  },
  searchTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#2c3e50',
  },
  searchInputContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginHorizontal: 20,
    marginVertical: 16,
    paddingHorizontal: 16,
    paddingVertical: 12,
    backgroundColor: '#fff',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#ecf0f1',
  },
  searchInput: {
    flex: 1,
    marginLeft: 8,
    fontSize: 14,
  },
  searchResults: {
    paddingVertical: 8,
  },
  farmerSearchItem: {
    flexDirection: 'row',
    paddingHorizontal: 20,
    paddingVertical: 16,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#ecf0f1',
    alignItems: 'center',
  },
  farmerInfo: {
    flex: 1,
    marginLeft: 16,
  },
  farmerName: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#2c3e50',
    marginBottom: 4,
  },
  farmerLocation: {
    fontSize: 12,
    color: '#7f8c8d',
    marginBottom: 2,
  },
  farmerPhone: {
    fontSize: 12,
    color: '#95a5a6',
  },
  chatButton: {
    backgroundColor: '#27ae60',
    borderRadius: 20,
    width: 36,
    height: 36,
    justifyContent: 'center',
    alignItems: 'center',
  },
  emptySearch: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 80,
    paddingHorizontal: 40,
  },
  emptySearchText: {
    fontSize: 14,
    color: '#7f8c8d',
    textAlign: 'center',
    marginTop: 16,
    lineHeight: 20,
  },
});
