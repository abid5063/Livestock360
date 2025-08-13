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

export default function FarmerMessaging() {
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
  const [farmer, setFarmer] = useState(null);

  // const API_BASE_URL = "http://localhost:3000/api";

  useEffect(() => {
    loadFarmerData();
  }, []);

  useEffect(() => {
    if (farmer) {
      fetchConversations();
    }
  }, [farmer]);

  const loadFarmerData = async () => {
    try {
      const storedData = await AsyncStorage.getItem('userData');
      if (storedData) {
        setFarmer(JSON.parse(storedData));
      }
    } catch (error) {
      console.error("Error loading farmer data:", error);
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

  const searchVets = async (query = '') => {
    try {
      const response = await axios.get(`${API_BASE_URL}/api/vets/search`, {
        params: query.trim() !== '' ? { search: query } : {}
      });
      
      setSearchResults(response.data.vets || []);
    } catch (error) {
      console.error("Error searching vets:", error);
      Alert.alert(t('farmerMessaging.error'), t('farmerMessaging.failedToSearch'));
    }
  };

  const startNewConversation = async (vet) => {
    try {
      // Close the search modal first
      setShowSearch(false);
      
      // First, check if there's an existing conversation with this vet
      const existingConversation = conversations.find(conv => 
        conv.participant.id === vet._id || conv.participant._id === vet._id
      );

      if (existingConversation) {
        // Open existing conversation
        await openExistingConversation(existingConversation);
      } else {
        // Create new conversation
        const conversation = {
          participant: {
            id: vet._id,
            name: vet.name,
            specialty: vet.specialty,
            avatar: vet.profileImage
          },
          messages: []
        };
        setSelectedConversation(conversation);
        setMessages([]);
        setShowChat(true);
      }
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
        Alert.alert(t('farmerMessaging.error'), t('farmerMessaging.invalidConversationData'));
        return;
      }
      
      const response = await axios.get(
        `${API_BASE_URL}/api/messages/conversation/${participantId}/vet`,
        { headers: { Authorization: `Bearer ${token}` } }
      );
      
      setMessages(response.data.messages || []);
      setShowChat(true);
    } catch (error) {
      console.error("Error fetching conversation:", error);
      Alert.alert(t('farmerMessaging.error'), t('farmerMessaging.failedToLoadConversation'));
    }
  };

  const sendMessage = async () => {
    if (!newMessage.trim() || !selectedConversation) return;

    const messageText = newMessage.trim();
    const tempMessage = {
      _id: Date.now().toString(), // Temporary ID
      content: messageText,
      senderType: 'farmer',
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
        Alert.alert(t('farmerMessaging.error'), t('farmerMessaging.invalidConversationData'));
        return;
      }
      
      const response = await axios.post(
        `${API_BASE_URL}/api/messages`,
        {
          receiverId: participantId,
          receiverType: 'vet',
          content: messageText
        },
        { headers: { Authorization: `Bearer ${token}` } }
      );

      // Refresh messages to get the actual saved message
      const messagesResponse = await axios.get(
        `${API_BASE_URL}/api/messages/conversation/${participantId}/vet`,
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
      Alert.alert(t('farmerMessaging.error'), t('farmerMessaging.failedToSendMessage'));
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
      return t('farmerMessaging.yesterday');
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
        {(item.participant.avatar || item.participant.profileImage) ? (
          <Image source={{ uri: item.participant.avatar || item.participant.profileImage }} style={styles.avatar} />
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
            Dr. {item.participant.name}
          </Text>
          <Text style={styles.messageTime}>
            {formatMessageTime(item.lastMessage?.timestamp)}
          </Text>
        </View>
        
        <Text style={styles.participantSpecialty} numberOfLines={1}>
          {item.participant.specialty || t('farmerMessaging.veterinarian')}
        </Text>
        
        <Text style={styles.lastMessage} numberOfLines={2}>
          {item.lastMessage?.content || t('farmerMessaging.noMessagesYet')}
        </Text>
      </View>
      
      {item.unreadCount > 0 && (
        <View style={styles.unreadBadge}>
          <Text style={styles.unreadText}>{item.unreadCount}</Text>
        </View>
      )}
    </TouchableOpacity>
  );

  const renderVetSearchItem = ({ item }) => (
    <TouchableOpacity
      style={styles.vetSearchItem}
      onPress={() => startNewConversation(item)}
    >
      <View style={styles.avatarContainer}>
        {item.profileImage ? (
          <Image source={{ uri: item.profileImage }} style={styles.avatar} />
        ) : (
          <View style={styles.avatarPlaceholder}>
            <Ionicons name="medical" size={24} color="#27ae60" />
          </View>
        )}
      </View>
      
      <View style={styles.vetInfo}>
        <Text style={styles.vetName}>Dr. {item.name}</Text>
        <Text style={styles.vetSpecialty}>{item.specialty}</Text>
        <Text style={styles.vetLocation}>{item.location}</Text>
        <View style={styles.ratingContainer}>
          <Ionicons name="star" size={12} color="#f39c12" />
          <Text style={styles.ratingText}>
            {item.rating ? `${item.rating}/5` : t('farmerMessaging.new')} 
            {item.totalReviews ? ` (${item.totalReviews})` : ''}
          </Text>
        </View>
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
      item.senderType === 'farmer' ? styles.sentMessage : styles.receivedMessage
    ]}>
      <Text style={[
        styles.messageText,
        item.senderType === 'farmer' ? styles.sentMessageText : styles.receivedMessageText
      ]}>
        {item.content}
      </Text>
      <Text style={[
        styles.messageTimestamp,
        item.senderType === 'farmer' ? styles.sentTimestamp : styles.receivedTimestamp
      ]}>
        {new Date(item.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
      </Text>
    </View>
  );

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <Text>{t('farmerMessaging.loadingMessages')}</Text>
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
        <Text style={styles.headerTitle}>{t('farmerMessaging.headerTitle')}</Text>
        <TouchableOpacity onPress={() => {
          setShowSearch(true);
          setSearchQuery(''); // Clear search query
          searchVets(); // Load all vets initially
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
              <Text style={styles.emptyTitle}>{t('farmerMessaging.noConversationsYet')}</Text>
              <Text style={styles.emptySubtitle}>
                {t('farmerMessaging.tapSearchToFind')}
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
                Dr. {selectedConversation.participant.name}
              </Text>
              <Text style={styles.chatHeaderSpecialty}>
                {selectedConversation.participant.specialty}
              </Text>
            </View>
            <View style={styles.chatHeaderActions}>
              <TouchableOpacity 
                style={styles.appointmentButton}
                onPress={() => {
                  const vetId = selectedConversation.participant._id || selectedConversation.participant.id;
                  const vetName = selectedConversation.participant.name;
                  router.push({
                    pathname: '/addAppointment',
                    params: {
                      vetId: vetId,
                      vetName: vetName,
                      fromChat: 'true',
                      farmerId: farmer?._id,
                      farmerName: farmer?.name
                    }
                  });
                }}
              >
                <Ionicons name="calendar" size={20} color="#3498db" />
              </TouchableOpacity>
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
          // removed inverted so latest message is at the bottom
          />

          {/* Message Input */}
          <View style={styles.messageInputContainer}>
            <TextInput
              style={styles.messageInput}
              value={newMessage}
              onChangeText={setNewMessage}
              placeholder={t('farmerMessaging.typeMessage')}
              multiline
              maxLength={500}
            />
            <TouchableOpacity style={styles.sendButton} onPress={sendMessage}>
              <Ionicons name="send" size={20} color="#fff" />
            </TouchableOpacity>
          </View>
        </View>
      )}

      {/* Search Vets Modal */}
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
            <Text style={styles.searchTitle}>{t('farmerMessaging.findVeterinarians')}</Text>
            <View style={{ width: 24 }} />
          </View>

          <View style={styles.searchInputContainer}>
            <Ionicons name="search" size={20} color="#7f8c8d" />
            <TextInput
              style={styles.searchInput}
              value={searchQuery}
              onChangeText={(text) => {
                setSearchQuery(text);
                searchVets(text);
              }}
              placeholder={t('farmerMessaging.searchPlaceholder')}
            />
          </View>

          <FlatList
            data={searchResults}
            renderItem={renderVetSearchItem}
            keyExtractor={(item) => item._id}
            contentContainerStyle={styles.searchResults}
            ListEmptyComponent={
              <View style={styles.emptySearch}>
                <Ionicons name="search-outline" size={48} color="#bdc3c7" />
                <Text style={styles.emptySearchText}>
                  {searchQuery ? t('farmerMessaging.noVetsFound') : t('farmerMessaging.noVetsAvailable')}
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
  participantSpecialty: {
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
  chatHeaderName: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#2c3e50',
  },
  chatHeaderSpecialty: {
    fontSize: 12,
    color: '#27ae60',
  },
  chatHeaderActions: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  appointmentButton: {
    marginRight: 16,
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
    backgroundColor: '#3498db',
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
    backgroundColor: '#3498db',
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
  vetSearchItem: {
    flexDirection: 'row',
    paddingHorizontal: 20,
    paddingVertical: 16,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#ecf0f1',
    alignItems: 'center',
  },
  vetInfo: {
    flex: 1,
    marginLeft: 16,
  },
  vetName: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#2c3e50',
    marginBottom: 4,
  },
  vetSpecialty: {
    fontSize: 14,
    color: '#27ae60',
    marginBottom: 2,
  },
  vetLocation: {
    fontSize: 12,
    color: '#7f8c8d',
    marginBottom: 4,
  },
  ratingContainer: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  ratingText: {
    fontSize: 12,
    color: '#f39c12',
    marginLeft: 4,
  },
  chatButton: {
    backgroundColor: '#3498db',
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
