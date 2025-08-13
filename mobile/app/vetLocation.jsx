import React, { useState, useEffect } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet, ScrollView, ActivityIndicator, Alert } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { useLanguage } from '../utils/LanguageContext';
import { useTranslation } from 'react-i18next';

const API_KEY = "AIzaSyCrYK2JHpleJxGT3TtneVT6hZHZY8KC1Vc";
const GEMINI_URL = `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${API_KEY}`;

// Component for displaying veterinary locations in a table format
const VeterinaryTable = ({ data, t }) => {
  if (!data || !Array.isArray(data)) return null;

  return (
    <View style={styles.tableContainer}>
      <Text style={styles.tableTitle}>{t('vetLocation.nearbyClinics')}</Text>
      <Text style={styles.tableSubtitle}>{t('vetLocation.sortedByDistance')}</Text>
      <View style={styles.tableHeader}>
        <Text style={[styles.tableHeaderCell, { flex: 2 }]}>{t('vetLocation.name')}</Text>
        <Text style={[styles.tableHeaderCell, { flex: 1 }]}>{t('vetLocation.distance')}</Text>
        <Text style={[styles.tableHeaderCell, { flex: 1 }]}>{t('vetLocation.rating')}</Text>
      </View>
      {data.map((clinic, index) => {
        const distance = parseFloat(clinic.distance.replace(/[^\d.]/g, ''));
        const isNearest = index === 0;
        const isNearby = distance <= 5;
        
        return (
          <View key={index} style={[
            styles.tableRow,
            isNearest && styles.nearestRow,
            isNearby && styles.nearbyRow
          ]}>
            <View style={[styles.tableCell, { flex: 2 }]}>
              <View style={styles.clinicInfo}>
                <Text style={styles.clinicName}>{clinic.name}</Text>
                {isNearest && (
                  <View style={styles.nearestBadge}>
                    <Text style={styles.nearestBadgeText}>{t('vetLocation.nearest')}</Text>
                  </View>
                )}
              </View>
              <Text style={styles.clinicAddress}>{clinic.address}</Text>
              <Text style={styles.clinicPhone}>{clinic.phone}</Text>
            </View>
            <View style={[styles.tableCell, { flex: 1, alignItems: 'center' }]}>
              <Text style={[
                styles.distanceText,
                isNearest && styles.nearestDistance,
                isNearby && styles.nearbyDistance
              ]}>
                {clinic.distance}
              </Text>
              {isNearby && (
                <Text style={styles.nearbyText}>{t('vetLocation.nearby')}</Text>
              )}
            </View>
            <Text style={[styles.tableCell, { flex: 1, textAlign: 'center' }]}>
              {clinic.rating} ⭐
            </Text>
          </View>
        );
      })}
    </View>
  );
};

// Component for location input form
const LocationForm = ({ locationData, setLocationData, onSearch, loading, t }) => {
  return (
    <View style={styles.formContainer}>
      <Text style={styles.formTitle}>{t('vetLocation.enterLocationDetails')}</Text>
      <Text style={styles.formSubtitle}>{t('vetLocation.provideLocation')}</Text>
      
      <TextInput
        style={styles.input}
        placeholder={t('vetLocation.villagePlaceholder')}
        value={locationData.village}
        onChangeText={(text) => setLocationData(prev => ({ ...prev, village: text }))}
        editable={!loading}
      />
      
      <TextInput
        style={styles.input}
        placeholder={t('vetLocation.thanaPlaceholder')}
        value={locationData.thana}
        onChangeText={(text) => setLocationData(prev => ({ ...prev, thana: text }))}
        editable={!loading}
      />
      
      <TextInput
        style={styles.input}
        placeholder={t('vetLocation.districtPlaceholder')}
        value={locationData.district}
        onChangeText={(text) => setLocationData(prev => ({ ...prev, district: text }))}
        editable={!loading}
      />
      
      <TouchableOpacity
        style={[styles.searchButton, loading && styles.searchButtonDisabled]}
        onPress={onSearch}
        disabled={loading}
      >
        {loading ? (
          <ActivityIndicator size="small" color="#fff" />
        ) : (
          <Text style={styles.searchButtonText}>{t('vetLocation.searchClinics')}</Text>
        )}
      </TouchableOpacity>
    </View>
  );
};

export default function VetLocation() {
  const router = useRouter();
  const { language } = useLanguage();
  const { t, i18n } = useTranslation();

  // Update i18n language when language changes
  useEffect(() => {
    i18n.changeLanguage(language);
  }, [language, i18n]);

  const [locationData, setLocationData] = useState({
    village: '',
    thana: '',
    district: ''
  });
  const [clinics, setClinics] = useState([]);
  const [loading, setLoading] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);

  useEffect(() => {
    const loadDefaultLocation = async () => {
      try {
        const storedLocation = await AsyncStorage.getItem('userLocation');
        if (storedLocation) {
          const parsedLocation = JSON.parse(storedLocation);
          setLocationData(parsedLocation);
        }
      } catch (error) {
        console.error('Error loading default location:', error);
      }
    };

    loadDefaultLocation();
  }, []);

  const sortByDistance = (clinics) => {
    return clinics.sort((a, b) => {
      const distanceA = parseFloat(a.distance.replace(/[^\d.]/g, ''));
      const distanceB = parseFloat(b.distance.replace(/[^\d.]/g, ''));
      return distanceA - distanceB;
    });
  };

  const fetchVeterinaryLocations = async () => {
    if (!locationData.village || !locationData.thana || !locationData.district) {
      Alert.alert(t('vetLocation.error'), 'Please fill in all location fields');
      return;
    }

    setLoading(true);
    setHasSearched(true);

    try {
      const languageInstruction = language === 'bn' ? 'Respond ONLY in Bangla (Bengali) language. All clinic names, addresses, and details must be in Bangla.' : 'Respond in English language. All clinic names, addresses, and details must be in English.';
      
      const prompt = `Find veterinary clinics near ${locationData.village}, ${locationData.thana}, ${locationData.district} in Bangladesh.\n\n${languageInstruction}\n\nReturn ONLY a JSON array of veterinary clinics with the following structure:\n[\n  {\n    "name": "Clinic Name",\n    "address": "Full Address",\n    "phone": "Phone Number",\n    "distance": "X.X km",\n    "rating": "X.X"\n  }\n]\n\nInclude at least 5-8 clinics with realistic names, addresses, and phone numbers. Distances should be between 0.5 to 15 km. Ratings should be between 3.0 to 5.0.`;

      const response = await fetch(GEMINI_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          contents: [{
            parts: [{ text: prompt }]
          }]
        }),
      });

      const data = await response.json();
      const aiText = data.candidates?.[0]?.content?.parts?.[0]?.text || t('vetLocation.noClinicsFound');

      try {
        // Try to extract JSON from the response
        const jsonMatch = aiText.match(/\[[\s\S]*\]/);
        if (jsonMatch) {
          const parsedClinics = JSON.parse(jsonMatch[0]);
          const sortedClinics = sortByDistance(parsedClinics);
          setClinics(sortedClinics);
        } else {
          throw new Error('No valid JSON found');
        }
      } catch (parseError) {
        console.error('Error parsing AI response:', parseError);
        Alert.alert(t('vetLocation.error'), t('vetLocation.failedToSearch'));
        setClinics([]);
      }

      // Save location data
      try {
        await AsyncStorage.setItem('userLocation', JSON.stringify(locationData));
      } catch (error) {
        console.error('Error saving location:', error);
      }

    } catch (error) {
      console.error('Error fetching veterinary locations:', error);
      Alert.alert(t('vetLocation.error'), t('vetLocation.failedToSearch'));
      setClinics([]);
    } finally {
      setLoading(false);
    }
  };

  const handleNewSearch = () => {
    setHasSearched(false);
    setClinics([]);
  };

  const handleBackPress = () => {
    try {
      router.back();
    } catch (error) {
      router.replace('/');
    }
  };

  const handleCallClinic = (phone) => {
    Alert.alert(
      t('vetLocation.callClinic'),
      `Call ${phone}?`,
      [
        { text: 'Cancel', style: 'cancel' },
        { text: 'Call', onPress: () => console.log('Calling:', phone) }
      ]
    );
  };

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={handleBackPress}>
          <Ionicons name="arrow-back" size={24} color="#4a89dc" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>{t('vetLocation.headerTitle')}</Text>
        <View style={{ width: 24 }} />
      </View>

      <ScrollView style={styles.content}>
        {!hasSearched ? (
          <LocationForm
            locationData={locationData}
            setLocationData={setLocationData}
            onSearch={fetchVeterinaryLocations}
            loading={loading}
            t={t}
          />
        ) : (
          <View style={styles.resultsContainer}>
            <View style={styles.resultsHeader}>
              <Text style={styles.resultsTitle}>{t('vetLocation.nearbyClinics')}</Text>
              <TouchableOpacity style={styles.newSearchButton} onPress={handleNewSearch}>
                <Ionicons name="refresh" size={20} color="#4a89dc" />
                <Text style={styles.newSearchButtonText}>{t('vetLocation.newSearch')}</Text>
              </TouchableOpacity>
            </View>

            {clinics.length > 0 ? (
              <VeterinaryTable data={clinics} t={t} />
            ) : (
              <View style={styles.noResultsContainer}>
                <Ionicons name="location-outline" size={64} color="#ccc" />
                <Text style={styles.noResultsText}>{t('vetLocation.noClinicsFound')}</Text>
                <TouchableOpacity style={styles.tryAgainButton} onPress={handleNewSearch}>
                  <Text style={styles.tryAgainButtonText}>{t('vetLocation.newSearch')}</Text>
                </TouchableOpacity>
              </View>
            )}
          </View>
        )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f7f9fa',
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
  headerTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#4a89dc',
  },
  content: {
    flex: 1,
    padding: 16,
  },
  // Form styles
  formContainer: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 20,
    marginTop: 20,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  formTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 8,
    textAlign: 'center',
  },
  formSubtitle: {
    fontSize: 14,
    color: '#666',
    marginBottom: 24,
    textAlign: 'center',
    lineHeight: 20,
  },
  input: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    marginBottom: 16,
    fontSize: 16,
    backgroundColor: '#fafbfc',
  },
  searchButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#4a89dc',
    paddingVertical: 14,
    borderRadius: 8,
    marginTop: 8,
  },
  searchButtonDisabled: {
    backgroundColor: '#ccc',
  },
  searchButtonText: {
    color: 'white',
    fontWeight: 'bold',
    fontSize: 16,
    marginLeft: 8,
  },
  loadingContainer: {
    alignItems: 'center',
    paddingVertical: 40,
  },
  loadingText: {
    marginTop: 15,
    fontSize: 16,
    color: '#4a89dc',
  },
  locationText: {
    marginTop: 8,
    fontSize: 14,
    color: '#666',
    textAlign: 'center',
  },
  locationContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#fff',
    padding: 12,
    borderRadius: 8,
    marginBottom: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1,
    shadowRadius: 2,
    elevation: 2,
  },
  welcomeText: {
    fontSize: 16,
    color: '#333',
    marginBottom: 20,
    textAlign: 'center',
    lineHeight: 24,
  },
  // Table styles for veterinary locations
  tableContainer: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 20,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  tableTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#032451ff',
    marginBottom: 4,
    textAlign: 'center',
  },
  tableSubtitle: {
    fontSize: 12,
    color: '#666',
    marginBottom: 12,
    textAlign: 'center',
  },
  tableHeader: {
    flexDirection: 'row',
    backgroundColor: '#f8f9fa',
    paddingVertical: 12,
    paddingHorizontal: 8,
    borderRadius: 8,
    marginBottom: 8,
  },
  tableHeaderCell: {
    fontWeight: 'bold',
    fontSize: 14,
    color: '#495057',
  },
  tableRow: {
    flexDirection: 'row',
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
    paddingVertical: 12,
    paddingHorizontal: 8,
  },
  nearestRow: {
    backgroundColor: '#e8f5e8',
    borderLeftWidth: 4,
    borderLeftColor: '#095b2bff',
  },
  nearbyRow: {
    backgroundColor: '#f8f9fa',
  },
  tableCell: {
    fontSize: 13,
    color: '#333',
  },
  clinicInfo: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  clinicName: {
    fontWeight: 'bold',
    fontSize: 14,
    color: '#2c3e50',
    flex: 1,
  },
  nearestBadge: {
    backgroundColor: '#04652cff',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
    marginLeft: 8,
  },
  nearestBadgeText: {
    color: 'white',
    fontSize: 10,
    fontWeight: 'bold',
  },
  clinicAddress: {
    fontSize: 12,
    color: '#666',
    marginTop: 2,
  },
  clinicPhone: {
    fontSize: 12,
    color: '#04459aff',
    marginTop: 2,
  },
  distanceText: {
    fontSize: 13,
    fontWeight: '500',
    color: '#333',
  },
  nearestDistance: {
    color: '#045c29ff',
    fontWeight: 'bold',
  },
  nearbyDistance: {
    color: '#053471ff',
    fontWeight: '500',
  },
  nearbyText: {
    fontSize: 10,
    color: '#4a89dc',
    marginTop: 2,
  },
  infoContainer: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 20,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  infoTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 12,
  },
  actionButton: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
    paddingHorizontal: 16,
    backgroundColor: '#f8f9fa',
    borderRadius: 8,
    marginBottom: 8,
  },
  actionButtonText: {
    marginLeft: 12,
    fontSize: 14,
    color: '#333',
    fontWeight: '500',
  },
  errorContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 40,
  },
  errorText: {
    fontSize: 16,
    color: '#666',
    marginTop: 16,
    marginBottom: 20,
    textAlign: 'center',
  },
  retryButton: {
    backgroundColor: '#023372ff',
    paddingVertical: 12,
    paddingHorizontal: 24,
    borderRadius: 8,
  },
  retryButtonText: {
    color: 'white',
    fontWeight: 'bold',
    fontSize: 16,
  },
  resultsContainer: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 20,
    marginTop: 20,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  resultsHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  resultsTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#024211ff',
  },
  newSearchButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#f8f9fa',
    paddingVertical: 8,
    paddingHorizontal: 12,
    borderRadius: 6,
  },
  newSearchButtonText: {
    marginLeft: 8,
    fontSize: 14,
    color: '#04326dff',
    fontWeight: '500',
  },
  noResultsContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 40,
  },
  noResultsText: {
    fontSize: 16,
    color: '#666',
    marginTop: 16,
    marginBottom: 20,
    textAlign: 'center',
  },
  tryAgainButton: {
    backgroundColor: '#4a89dc',
    paddingVertical: 12,
    paddingHorizontal: 24,
    borderRadius: 8,
  },
  tryAgainButtonText: {
    color: 'white',
    fontWeight: 'bold',
    fontSize: 16,
  },
});