import React, { useState, useEffect, useCallback, useRef } from 'react';
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
  SafeAreaView
} from 'react-native';
import { Ionicons, MaterialIcons } from '@expo/vector-icons';
import { useRouter, useLocalSearchParams } from 'expo-router';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { API_BASE_URL } from '../utils/apiConfig';

export default function CustomerMessaging() {
  const router = useRouter();
  const params = useLocalSearchParams();

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
  const [customer, setCustomer] = useState(null);
  const paramsProcessed = useRef(false);

  const loadCustomerData = useCallback(async () => {
    try {
      const storedData = await AsyncStorage.getItem('customerData');
      if (storedData) {
        setCustomer(JSON.parse(storedData));
      } else {
        router.replace('/customerAuth');
      }
    } catch (error) {
      console.error("Error loading customer data:", error);
      router.replace('/customerAuth');
    } finally {
      setLoading(false);
    }
  }, [router]);

  useEffect(() => {
    loadCustomerData();
  }, [loadCustomerData]);

  useEffect(() => {
    if (customer) {
      fetchConversations();
    }
  }, [customer]);

  useEffect(() => {
    // If we have farmer params, start a new conversation immediately (only once)
    if (params.farmerId && params.farmerName && customer && !paramsProcessed.current) {
      paramsProcessed.current = true;
      startNewConversationWithFarmer({
        _id: params.farmerId,
        id: params.farmerId,
        name: params.farmerName,
        animalContext: params.animalContext
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [params, customer]);

  const fetchConversations = async () => {
    try {
      const token = await AsyncStorage.getItem('customerToken');
      if (!token) return;

      const response = await fetch(`${API_BASE_URL}/api/messages/conversations`, {
        headers: { 
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (response.ok) {
        const data = await response.json();
        console.log('Customer conversations:', data);
        setConversations(data.conversations || []);
      } else {
        console.error('Failed to fetch conversations:', response.status);
      }
    } catch (error) {
      console.error("Error fetching conversations:", error);
    }
  };

  const searchFarmers = async (query = '') => {
    try {
      const token = await AsyncStorage.getItem('customerToken');
      const url = query.trim() !== '' 
        ? `${API_BASE_URL}/api/auth/farmers/search?search=${encodeURIComponent(query)}`
        : `${API_BASE_URL}/api/auth/farmers`;
      
      const response = await fetch(url, {
        headers: { 
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.ok) {
        const data = await response.json();
        console.log('Farmers search result:', data);
        setSearchResults(data.farmers || []);
      } else {
        console.error('Failed to search farmers:', response.status);
        Alert.alert('Error', 'Failed to search farmers');
      }
    } catch (error) {
      console.error("Error searching farmers:", error);
      Alert.alert('Error', 'Network error while searching farmers');
    }
  };

  const startNewConversationWithFarmer = async (farmer) => {
    try {
      setShowSearch(false);
      
      // Get current conversations from state
      setConversations(currentConversations => {
        // Check if there's an existing conversation with this farmer
        const existingConversation = currentConversations.find(conv => 
          conv.participant.id === farmer._id || 
          conv.participant._id === farmer._id ||
          conv.participant.id === farmer.id
        );

        if (existingConversation) {
          openExistingConversation(existingConversation);
        } else {
          // Create new conversation
          const conversation = {
            participant: {
              id: farmer._id || farmer.id,
              _id: farmer._id || farmer.id,
              name: farmer.name,
              location: farmer.location || '',
              avatar: farmer.profileImage || farmer.profilePicture || ''
            },
            messages: []
          };
          setSelectedConversation(conversation);
          setMessages([]);
          
          // If we have animal context, add it as initial message
          if (farmer.animalContext) {
            setNewMessage(farmer.animalContext);
          }
          
          setShowChat(true);
        }
        
        return currentConversations; // Don't actually change conversations state
      });
    } catch (error) {
      console.error("Error starting conversation:", error);
      Alert.alert('Error', 'Failed to start conversation');
    }
  };

  const openExistingConversation = async (conversation) => {
    try {
      setSelectedConversation(conversation);
      const token = await AsyncStorage.getItem('customerToken');
      
      const participantId = conversation.participant._id || conversation.participant.id;
      
      if (!participantId) {
        console.error("No participant ID found in conversation:", conversation);
        Alert.alert('Error', 'Invalid conversation data');
        return;
      }
      
      const response = await fetch(
        `${API_BASE_URL}/api/messages/conversation/${participantId}/farmer`,
        { 
          headers: { 
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        }
      );
      
      if (response.ok) {
        const data = await response.json();
        console.log('Conversation messages:', data);
        setMessages(data.messages || []);
        setShowChat(true);
      } else {
        console.error('Failed to load conversation:', response.status);
        Alert.alert('Error', 'Failed to load conversation');
      }
    } catch (error) {
      console.error("Error fetching conversation:", error);
      Alert.alert('Error', 'Failed to load conversation');
    }
  };

  const sendMessage = async () => {
    if (!newMessage.trim() || !selectedConversation) return;

    const messageText = newMessage.trim();
    const tempMessage = {
      _id: Date.now().toString(),
      content: messageText,
      senderType: 'customer',
      createdAt: new Date().toISOString(),
      isTemp: true
    };

    try {
      const token = await AsyncStorage.getItem('customerToken');
      
      // Add message to UI immediately for better UX
      setMessages(prev => [tempMessage, ...prev]);
      setNewMessage('');
      
      const participantId = selectedConversation.participant._id || selectedConversation.participant.id;
      
      if (!participantId) {
        console.error("No participant ID found in conversation:", selectedConversation);
        Alert.alert('Error', 'Invalid conversation data');
        return;
      }
      
      const response = await fetch(`${API_BASE_URL}/api/messages`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          receiverId: participantId,
          receiverType: 'farmer',
          content: messageText
        })
      });

      if (response.ok) {
        const data = await response.json();
        console.log('Message sent:', data);
        
        // Refresh messages to get the actual saved message
        const messagesResponse = await fetch(
          `${API_BASE_URL}/api/messages/conversation/${participantId}/farmer`,
          { 
            headers: { 
              'Authorization': `Bearer ${token}`,
              'Content-Type': 'application/json'
            }
          }
        );
        
        if (messagesResponse.ok) {
          const messagesData = await messagesResponse.json();
          setMessages(messagesData.messages || []);
        }
        
        // Refresh conversations list
        fetchConversations();
      } else {
        console.error('Failed to send message:', response.status);
        // Remove the temporary message on error
        setMessages(prev => prev.filter(msg => msg._id !== tempMessage._id));
        setNewMessage(messageText); // Restore the message text
        Alert.alert('Error', 'Failed to send message');
      }
      
    } catch (error) {
      console.error("Error sending message:", error);
      // Remove the temporary message on error
      setMessages(prev => prev.filter(msg => msg._id !== tempMessage._id));
      setNewMessage(messageText); // Restore the message text
      Alert.alert('Error', 'Network error. Please try again.');
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
            {item.participant.name}
          </Text>
          <Text style={styles.messageTime}>
            {formatMessageTime(item.lastMessage?.timestamp)}
          </Text>
        </View>
        
        <Text style={styles.participantType} numberOfLines={1}>
          Farmer • {item.participant.location || 'Location not specified'}
        </Text>
        
        <Text style={styles.lastMessage} numberOfLines={2}>
          {item.lastMessage?.content || 'No messages yet'}
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
      onPress={() => startNewConversationWithFarmer(item)}
    >
      <View style={styles.avatarContainer}>
        {item.profileImage ? (
          <Image source={{ uri: item.profileImage }} style={styles.avatar} />
        ) : (
          <View style={styles.avatarPlaceholder}>
            <Ionicons name="person" size={24} color="#27ae60" />
          </View>
        )}
      </View>
      
      <View style={styles.farmerInfo}>
        <Text style={styles.farmerName}>{item.name}</Text>
        <Text style={styles.farmerLocation}>{item.location || 'Location not specified'}</Text>
        {item.phoneNo && (
          <Text style={styles.farmerPhone}>📞 {item.phoneNo}</Text>
        )}
      </View>
      
      <TouchableOpacity
        style={styles.chatButton}
        onPress={() => startNewConversationWithFarmer(item)}
      >
        <Ionicons name="chatbubbles" size={16} color="#fff" />
      </TouchableOpacity>
    </TouchableOpacity>
  );

  const renderMessage = ({ item }) => (
    <View style={[
      styles.messageContainer,
      item.senderType === 'customer' ? styles.sentMessage : styles.receivedMessage
    ]}>
      <Text style={[
        styles.messageText,
        item.senderType === 'customer' ? styles.sentMessageText : styles.receivedMessageText
      ]}>
        {item.content}
      </Text>
      <Text style={[
        styles.messageTimestamp,
        item.senderType === 'customer' ? styles.sentTimestamp : styles.receivedTimestamp
      ]}>
        {new Date(item.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
      </Text>
    </View>
  );

  if (loading) {
    return (
      <SafeAreaView style={styles.loadingContainer}>
        <Text>Loading messages...</Text>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()}>
          <Ionicons name="arrow-back" size={24} color="#2c3e50" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Messages</Text>
        <TouchableOpacity onPress={() => {
          setShowSearch(true);
          setSearchQuery('');
          searchFarmers();
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
              <Text style={styles.emptyTitle}>No conversations yet</Text>
              <Text style={styles.emptySubtitle}>
                Tap the search icon to find farmers to message
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
              <Text style={styles.chatHeaderType}>
                Farmer • {selectedConversation.participant.location || 'Location not specified'}
              </Text>
            </View>
            <View style={styles.chatHeaderActions}>
              <TouchableOpacity>
                <MaterialIcons name="info" size={24} color="#2E7D32" />
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
              placeholder="Type a message..."
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
        <SafeAreaView style={styles.searchContainer}>
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
              placeholder="Search farmers by name or location..."
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
                  {searchQuery ? 'No farmers found' : 'Search for farmers to start messaging'}
                </Text>
              </View>
            }
          />
        </SafeAreaView>
      </Modal>
    </SafeAreaView>
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
  participantType: {
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
  chatHeaderType: {
    fontSize: 12,
    color: '#27ae60',
  },
  chatHeaderActions: {
    flexDirection: 'row',
    alignItems: 'center',
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
    backgroundColor: '#2E7D32',
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
    backgroundColor: '#2E7D32',
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
    fontSize: 14,
    color: '#7f8c8d',
    marginBottom: 2,
  },
  farmerPhone: {
    fontSize: 12,
    color: '#27ae60',
  },
  chatButton: {
    backgroundColor: '#2E7D32',
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