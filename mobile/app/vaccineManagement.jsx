import React, { useState, useEffect, useCallback } from 'react';
import { 
  View, 
  Text, 
  StyleSheet, 
  TouchableOpacity, 
  ScrollView, 
  Alert,
  ActivityIndicator,
  RefreshControl
} from 'react-native';
import { useRouter, useFocusEffect } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import AsyncStorage from '@react-native-async-storage/async-storage';
import axios from 'axios';
import { API_BASE_URL } from '../utils/apiConfig'; // Adjust the import path as needed
import { useLanguage } from '../utils/LanguageContext';
import { useTranslation } from 'react-i18next';
// const API_BASE_URL = "http://localhost:3000/api";

export default function VaccineManagement() {
  const router = useRouter();
  const { language } = useLanguage();
  const { t, i18n } = useTranslation();

  // Update i18n language when language changes
  useEffect(() => {
    i18n.changeLanguage(language);
  }, [language, i18n]);

  const [vaccines, setVaccines] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  useEffect(() => {
    fetchVaccines();
  }, []);

  // Refresh vaccines when screen comes into focus
  useFocusEffect(
    useCallback(() => {
      fetchVaccines();
    }, [])
  );

  const fetchVaccines = async () => {
    try {
      const token = await AsyncStorage.getItem('authToken');
      const response = await axios.get(`${API_BASE_URL}/api/vaccines`, {
        headers: {
          Authorization: `Bearer ${token}`
        }
      });
      setVaccines(response.data);
    } catch (error) {
      console.error('Error fetching vaccines:', error);
      Alert.alert(t('vaccineManagement.error'), t('vaccineManagement.failedToFetchVaccines'));
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  const onRefresh = () => {
    setRefreshing(true);
    fetchVaccines();
  };

  const handleDeleteVaccine = async (vaccineId) => {
    Alert.alert(
      t('vaccineManagement.deleteVaccineRecord'),
      t('vaccineManagement.deleteVaccineMessage'),
      [
        { text: t('vaccineManagement.cancel'), style: 'cancel' },
        {
          text: t('vaccineManagement.delete'),
          style: 'destructive',
          onPress: async () => {
            try {
              const token = await AsyncStorage.getItem('authToken');
              await axios.delete(`${API_BASE_URL}/api/vaccines/${vaccineId}`, {
                headers: {
                  Authorization: `Bearer ${token}`
                }
              });
              fetchVaccines(); // Refresh the list immediately
            } catch (error) {
              console.error('Error deleting vaccine:', error);
              Alert.alert(t('vaccineManagement.error'), t('vaccineManagement.failedToDeleteVaccine'));
            }
          }
        }
      ]
    );
  };

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  };

  const isOverdue = (nextDueDate) => {
    if (!nextDueDate) return false;
    return new Date(nextDueDate) < new Date();
  };

  const isDueSoon = (nextDueDate) => {
    if (!nextDueDate) return false;
    const today = new Date();
    const dueDate = new Date(nextDueDate);
    const diffTime = dueDate.getTime() - today.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays > 0 && diffDays <= 7;
  };

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#4a89dc" />
        <Text style={styles.loadingText}>{t('vaccineManagement.loadingVaccines')}</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.canGoBack() ? router.back() : router.push('/profile')}>
          <Ionicons name="arrow-back" size={28} color="#4a89dc" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>{t('vaccineManagement.headerTitle')}</Text>
        <TouchableOpacity 
          onPress={() => router.push('/addVaccine')}
          style={styles.addButton}
        >
          <Ionicons name="add" size={24} color="#fff" />
        </TouchableOpacity>
      </View>

      <ScrollView 
        style={styles.content}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
        }
      >
        {vaccines.length === 0 ? (
          <View style={styles.emptyContainer}>
            <Ionicons name="medical" size={64} color="#ccc" />
            <Text style={styles.emptyText}>{t('vaccineManagement.noVaccineRecords')}</Text>
            <TouchableOpacity 
              style={styles.addFirstButton}
              onPress={() => router.push('/addVaccine')}
            >
              <Text style={styles.addFirstButtonText}>{t('vaccineManagement.addFirstVaccine')}</Text>
            </TouchableOpacity>
          </View>
        ) : (
          vaccines.map((vaccine) => (
            <View key={vaccine._id} style={styles.vaccineCard}>
              <View style={styles.vaccineHeader}>
                <Text style={styles.vaccineName}>{vaccine.vaccine_name}</Text>
                <View style={styles.vaccineActions}>
                  <TouchableOpacity
                    onPress={() => router.push({
                      pathname: '/editVaccine',
                      params: { vaccineId: vaccine._id }
                    })}
                    style={styles.editButton}
                  >
                    <Ionicons name="create" size={20} color="#4a89dc" />
                  </TouchableOpacity>
                  <TouchableOpacity
                    onPress={() => handleDeleteVaccine(vaccine._id)}
                    style={styles.deleteButton}
                  >
                    <Ionicons name="trash" size={20} color="#e74c3c" />
                  </TouchableOpacity>
                </View>
              </View>
              
              <View style={styles.vaccineDetails}>
                <View style={styles.detailRow}>
                  <Ionicons name="paw" size={16} color="#666" />
                  <Text style={styles.detailText}>{t('vaccineManagement.animal')} {vaccine.animal_name}</Text>
                </View>
                
                <View style={styles.detailRow}>
                  <Ionicons name="calendar" size={16} color="#666" />
                  <Text style={styles.detailText}>
                    {t('vaccineManagement.vaccinated')} {formatDate(vaccine.vaccine_date)}
                  </Text>
                </View>
                
                {vaccine.next_due_date && (
                  <View style={styles.detailRow}>
                    <Ionicons name="alarm" size={16} color="#666" />
                    <Text style={[
                      styles.detailText,
                      isOverdue(vaccine.next_due_date) && styles.overdueText,
                      isDueSoon(vaccine.next_due_date) && styles.dueSoonText
                    ]}>
                      {t('vaccineManagement.nextDue')} {formatDate(vaccine.next_due_date)}
                      {isOverdue(vaccine.next_due_date) && ` (${t('vaccineManagement.overdue')})`}
                      {isDueSoon(vaccine.next_due_date) && ` (${t('vaccineManagement.dueSoon')})`}
                    </Text>
                  </View>
                )}
                
                {vaccine.notes && (
                  <View style={styles.detailRow}>
                    <Ionicons name="document-text" size={16} color="#666" />
                    <Text style={styles.detailText}>Notes: {vaccine.notes}</Text>
                  </View>
                )}
              </View>
            </View>
          ))
        )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8f9fa',
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
    color: '#333',
  },
  addButton: {
    backgroundColor: '#4a89dc',
    borderRadius: 20,
    width: 40,
    height: 40,
    justifyContent: 'center',
    alignItems: 'center',
  },
  content: {
    flex: 1,
    padding: 16,
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#f8f9fa',
  },
  loadingText: {
    marginTop: 16,
    fontSize: 16,
    color: '#666',
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: 100,
  },
  emptyText: {
    fontSize: 18,
    color: '#666',
    marginTop: 16,
    marginBottom: 24,
  },
  addFirstButton: {
    backgroundColor: '#4a89dc',
    paddingVertical: 12,
    paddingHorizontal: 24,
    borderRadius: 8,
  },
  addFirstButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  vaccineCard: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  vaccineHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  vaccineName: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
    flex: 1,
  },
  vaccineActions: {
    flexDirection: 'row',
    gap: 8,
  },
  editButton: {
    padding: 8,
  },
  deleteButton: {
    padding: 8,
  },
  vaccineDetails: {
    gap: 8,
  },
  detailRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  detailText: {
    fontSize: 14,
    color: '#666',
    flex: 1,
  },
  overdueText: {
    color: '#e74c3c',
    fontWeight: '600',
  },
  dueSoonText: {
    color: '#f39c12',
    fontWeight: '600',
  },
});
