import React, { useState, useEffect } from 'react';
import { 
  View, 
  Text, 
  ScrollView, 
  TouchableOpacity, 
  StyleSheet, 
  SafeAreaView, 
  Image,
  TextInput,
  RefreshControl,
  Alert,
  ActivityIndicator
} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { router } from 'expo-router';
import { MaterialIcons, Ionicons } from '@expo/vector-icons';
import { API_BASE_URL } from '../utils/apiConfig';

const AnimalMarketplace = () => {
  const [animals, setAnimals] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedType, setSelectedType] = useState('');
  const [selectedBreed, setSelectedBreed] = useState('');
  const [animalTypes, setAnimalTypes] = useState(['All', 'Cattle', 'Goat', 'Sheep', 'Poultry', 'Pig']);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(true);

  useEffect(() => {
    checkAuth();
    fetchAnimals();
  }, [selectedType, selectedBreed, searchQuery]);

  const checkAuth = async () => {
    try {
      const token = await AsyncStorage.getItem('customerToken');
      if (!token) {
        router.replace('/customerAuth');
      }
    } catch (error) {
      console.error('Auth check error:', error);
      router.replace('/customerAuth');
    }
  };

  const fetchAnimals = async (loadMore = false) => {
    try {
      const currentPage = loadMore ? page + 1 : 1;
      const token = await AsyncStorage.getItem('customerToken');
      
      let url = `${API_BASE_URL}/api/marketplace/animals?page=${currentPage}&limit=20`;
      
      if (selectedType && selectedType !== 'All') {
        url += `&type=${encodeURIComponent(selectedType)}`;
      }
      if (selectedBreed) {
        url += `&breed=${encodeURIComponent(selectedBreed)}`;
      }

      const response = await fetch(url, {
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      const data = await response.json();
      console.log('Marketplace API response:', data);

      if (data.success) {
        // Log the first animal to see what data we're getting
        if (data.animals && data.animals.length > 0) {
          console.log('First animal data:', data.animals[0]);
        }

        if (loadMore) {
          setAnimals(prev => [...prev, ...data.animals]);
          setPage(currentPage);
        } else {
          setAnimals(data.animals || []);
          setPage(1);
        }
        setHasMore(data.animals && data.animals.length === 20);
      } else {
        Alert.alert('Error', data.message || 'Failed to fetch animals');
      }
    } catch (error) {
      console.error('Error fetching animals:', error);
      Alert.alert('Error', 'Network error. Please try again.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  const onRefresh = async () => {
    setRefreshing(true);
    await fetchAnimals();
  };

  const loadMoreAnimals = () => {
    if (!loading && hasMore) {
      fetchAnimals(true);
    }
  };

  const handleAnimalPress = (animal) => {
    // Navigate to animal details with animal data
    router.push({
      pathname: '/animalMarketplaceDetails',
      params: { animalId: animal.id, animalData: JSON.stringify(animal) }
    });
  };

  const messageFarmer = async (animal) => {
    try {
      console.log('Message farmer pressed for animal:', animal);
      
      const token = await AsyncStorage.getItem('customerToken');
      if (!token) {
        Alert.alert('Error', 'Please login again');
        return;
      }

      // Navigate to customer messaging with farmer info
      router.push({
        pathname: '/customerMessaging',
        params: { 
          farmerId: animal.farmerId,
          farmerName: animal.farmerName || 'Farmer',
          animalContext: `Interested in ${animal.name} (${animal.type})`
        }
      });
    } catch (error) {
      console.error('Error starting message:', error);
      Alert.alert('Error', 'Failed to start messaging. Please try again.');
    }
  };

  const renderAnimalCard = (animal) => (
    <TouchableOpacity 
      key={animal.id} 
      style={styles.animalCard}
      onPress={() => handleAnimalPress(animal)}
    >
      <View style={styles.cardHeader}>
        {animal.photo_url ? (
          <Image source={{ uri: animal.photo_url }} style={styles.animalImage} />
        ) : (
          <View style={styles.placeholderImage}>
            <Ionicons name="camera-outline" size={40} color="#ccc" />
          </View>
        )}
        
        <View style={styles.animalInfo}>
          <Text style={styles.animalName}>{animal.name}</Text>
          <Text style={styles.animalType}>{animal.type} - {animal.breed}</Text>
          {animal.age && <Text style={styles.animalAge}>Age: {animal.age} years</Text>}
          <Text style={styles.animalGender}>Gender: {animal.gender || 'Not specified'}</Text>
        </View>
      </View>

      {animal.details && (
        <Text style={styles.animalDetails} numberOfLines={2}>
          {animal.details}
        </Text>
      )}

      <View style={styles.farmerInfo}>
        <View style={styles.farmerDetails}>
          <MaterialIcons name="person" size={16} color="#666" />
          <Text style={styles.farmerName}>
            {animal.farmerName || 'Unknown Farmer'}
          </Text>
          {animal.farmerPhone && (
            <Text style={styles.phoneIndicator}> • Phone available</Text>
          )}
        </View>
        
        {animal.farmerLocation && (
          <View style={styles.locationInfo}>
            <MaterialIcons name="location-on" size={16} color="#666" />
            <Text style={styles.locationText}>{animal.farmerLocation}</Text>
          </View>
        )}
      </View>

      <View style={styles.cardActions}>
        <TouchableOpacity 
          style={styles.messageButton}
          onPress={() => messageFarmer(animal)}
        >
          <MaterialIcons name="message" size={18} color="#2E7D32" />
          <Text style={styles.messageButtonText}>Message Seller</Text>
        </TouchableOpacity>
        
        <Text style={styles.dateAdded}>
          Added: {animal.createdAt ? new Date(animal.createdAt).toLocaleDateString() : 'Recently'}
        </Text>
      </View>
    </TouchableOpacity>
  );

  const renderFilters = () => (
    <View style={styles.filtersContainer}>
      <TextInput
        style={styles.searchInput}
        placeholder="Search animals..."
        value={searchQuery}
        onChangeText={setSearchQuery}
      />
      
      <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.filterScroll}>
        {animalTypes.map(type => (
          <TouchableOpacity
            key={type}
            style={[
              styles.filterButton,
              selectedType === type && styles.selectedFilter
            ]}
            onPress={() => setSelectedType(selectedType === type ? '' : type)}
          >
            <Text style={[
              styles.filterText,
              selectedType === type && styles.selectedFilterText
            ]}>
              {type}
            </Text>
          </TouchableOpacity>
        ))}
      </ScrollView>
    </View>
  );

  return (
    <SafeAreaView style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backButton}>
          <MaterialIcons name="arrow-back" size={24} color="#fff" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Animal Marketplace</Text>
        <TouchableOpacity onPress={() => router.push('/customerDashboard')} style={styles.homeButton}>
          <MaterialIcons name="home" size={24} color="#fff" />
        </TouchableOpacity>
      </View>

      {renderFilters()}

      {loading && animals.length === 0 ? (
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color="#2E7D32" />
          <Text style={styles.loadingText}>Loading animals...</Text>
        </View>
      ) : (
        <ScrollView 
          style={styles.scrollView}
          refreshControl={
            <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
          }
          onMomentumScrollEnd={(e) => {
            const { layoutMeasurement, contentOffset, contentSize } = e.nativeEvent;
            const paddingToBottom = 20;
            if (layoutMeasurement.height + contentOffset.y >= contentSize.height - paddingToBottom) {
              loadMoreAnimals();
            }
          }}
        >
          <View style={styles.resultsHeader}>
            <Text style={styles.resultsText}>
              {animals.length} animals available
            </Text>
          </View>

          {animals.length === 0 ? (
            <View style={styles.emptyContainer}>
              <Ionicons name="search-outline" size={60} color="#ccc" />
              <Text style={styles.emptyText}>No animals found</Text>
              <Text style={styles.emptySubtext}>Try adjusting your search filters</Text>
            </View>
          ) : (
            <View style={styles.animalsContainer}>
              {animals.map(renderAnimalCard)}
              
              {loading && animals.length > 0 && (
                <View style={styles.loadMoreContainer}>
                  <ActivityIndicator size="small" color="#2E7D32" />
                </View>
              )}
            </View>
          )}
        </ScrollView>
      )}
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  header: {
    backgroundColor: '#2E7D32',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 15,
    paddingVertical: 15,
    paddingTop: 40,
  },
  backButton: {
    padding: 5,
  },
  headerTitle: {
    color: '#fff',
    fontSize: 18,
    fontWeight: 'bold',
    flex: 1,
    textAlign: 'center',
    marginHorizontal: 10,
  },
  homeButton: {
    padding: 5,
  },
  filtersContainer: {
    backgroundColor: '#fff',
    padding: 15,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  searchInput: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    paddingHorizontal: 15,
    paddingVertical: 10,
    fontSize: 16,
    marginBottom: 10,
  },
  filterScroll: {
    flexDirection: 'row',
  },
  filterButton: {
    backgroundColor: '#f0f0f0',
    paddingHorizontal: 15,
    paddingVertical: 8,
    borderRadius: 20,
    marginRight: 10,
  },
  selectedFilter: {
    backgroundColor: '#2E7D32',
  },
  filterText: {
    color: '#333',
    fontSize: 14,
  },
  selectedFilterText: {
    color: '#fff',
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingText: {
    marginTop: 10,
    color: '#666',
    fontSize: 16,
  },
  scrollView: {
    flex: 1,
  },
  resultsHeader: {
    padding: 15,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  resultsText: {
    fontSize: 16,
    fontWeight: '500',
    color: '#333',
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 50,
  },
  emptyText: {
    fontSize: 18,
    color: '#666',
    marginTop: 10,
  },
  emptySubtext: {
    fontSize: 14,
    color: '#999',
    marginTop: 5,
  },
  animalsContainer: {
    padding: 15,
  },
  animalCard: {
    backgroundColor: '#fff',
    borderRadius: 10,
    padding: 15,
    marginBottom: 15,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 3.84,
    elevation: 5,
  },
  cardHeader: {
    flexDirection: 'row',
    marginBottom: 10,
  },
  animalImage: {
    width: 80,
    height: 80,
    borderRadius: 10,
    marginRight: 15,
  },
  placeholderImage: {
    width: 80,
    height: 80,
    borderRadius: 10,
    backgroundColor: '#f0f0f0',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 15,
  },
  animalInfo: {
    flex: 1,
  },
  animalName: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 5,
  },
  animalType: {
    fontSize: 14,
    color: '#2E7D32',
    fontWeight: '500',
    marginBottom: 3,
  },
  animalAge: {
    fontSize: 14,
    color: '#666',
    marginBottom: 3,
  },
  animalGender: {
    fontSize: 14,
    color: '#666',
  },
  animalDetails: {
    fontSize: 14,
    color: '#666',
    marginBottom: 10,
    lineHeight: 20,
  },
  farmerInfo: {
    marginBottom: 10,
  },
  farmerDetails: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 5,
  },
  farmerName: {
    fontSize: 14,
    color: '#666',
    marginLeft: 5,
  },
  phoneIndicator: {
    fontSize: 12,
    color: '#2E7D32',
    fontWeight: '500',
  },
  locationInfo: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  locationText: {
    fontSize: 14,
    color: '#666',
    marginLeft: 5,
  },
  cardActions: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    borderTopWidth: 1,
    borderTopColor: '#eee',
    paddingTop: 10,
  },
  messageButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#f0f8f0',
    paddingHorizontal: 15,
    paddingVertical: 8,
    borderRadius: 20,
  },
  messageButtonText: {
    color: '#2E7D32',
    fontSize: 14,
    fontWeight: '500',
    marginLeft: 5,
  },
  dateAdded: {
    fontSize: 12,
    color: '#999',
  },
  loadMoreContainer: {
    padding: 20,
    alignItems: 'center',
  },
});

export default AnimalMarketplace;