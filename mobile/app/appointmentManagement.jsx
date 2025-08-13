import React, { useState, useEffect } from 'react';
import {
  StyleSheet,
  Text,
  View,
  FlatList,
  TouchableOpacity,
  Alert,
  RefreshControl,
  StatusBar,
  Dimensions,
  SafeAreaView,
} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Ionicons } from '@expo/vector-icons';
import { router, useFocusEffect } from 'expo-router';
import axios from 'axios';
import { API_BASE_URL } from '../utils/apiConfig';
import { useCallback } from 'react';
import { useLanguage } from '../utils/LanguageContext';
import { useTranslation } from 'react-i18next';

const { width } = Dimensions.get('window');

const AppointmentManagement = () => {
  const { language } = useLanguage();
  const { t, i18n } = useTranslation();

  // Update i18n language when language changes
  useEffect(() => {
    i18n.changeLanguage(language);
  }, [language, i18n]);

  const [appointments, setAppointments] = useState([]);
  const [refreshing, setRefreshing] = useState(false);
  const [activeFilter, setActiveFilter] = useState('all');
  const [vetId, setVetId] = useState(null);

  const filterOptions = [
    { key: 'all', label: t('appointmentManagement.filterAll') },
    { key: 'pending', label: t('appointmentManagement.filterPending') },
    { key: 'accepted', label: t('appointmentManagement.filterConfirmed') },
    { key: 'completed', label: t('appointmentManagement.filterCompleted') },
    { key: 'cancelled', label: t('appointmentManagement.filterCancelled') },
  ];

  useEffect(() => {
    initializeData();
  }, []);

  // Refresh appointments when screen is focused (after creating new appointment)
  useFocusEffect(
    useCallback(() => {
      if (vetId) {
        loadAppointments(vetId);
      }
    }, [vetId])
  );

  const initializeData = async () => {
    try {
      const userData = await AsyncStorage.getItem('userData');
      if (userData) {
        const user = JSON.parse(userData);
        setVetId(user._id);
        loadAppointments(user._id);
      }
    } catch (error) {
      console.error('Error initializing data:', error);
    }
  };

  const loadAppointments = async (id = vetId) => {
    try {
      const token = await AsyncStorage.getItem('authToken');
      
      // Use the simpler route that gets appointments from token
      const response = await axios.get(`${API_BASE_URL}/api/appointments/vet`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      
      // Extract appointments from the response - the API returns {appointments: [...], pagination: {...}}
      const appointmentsData = response.data.appointments || response.data;
      
      // Ensure we always set an array
      if (Array.isArray(appointmentsData)) {
        setAppointments(appointmentsData);
      } else {
        console.error('API returned non-array appointments data:', appointmentsData);
        setAppointments([]);
      }
    } catch (error) {
      console.error('Error loading appointments:', error);
      console.error('Error response:', error.response?.data);
      setAppointments([]); // Set empty array on error
      Alert.alert(t('appointmentManagement.error'), t('appointmentManagement.failedToLoad'));
    }
  };

  const onRefresh = async () => {
    setRefreshing(true);
    await loadAppointments();
    setRefreshing(false);
  };

  const updateAppointmentStatus = async (appointmentId, newStatus) => {
    Alert.alert(
      newStatus === 'accepted' ? t('appointmentManagement.confirmAppointment') : t('appointmentManagement.completeAppointment'),
      newStatus === 'accepted'
        ? t('appointmentManagement.confirmAppointmentMessage')
        : t('appointmentManagement.completeAppointmentMessage'),
      [
        { text: t('appointmentManagement.cancel'), style: 'cancel' },
        {
          text: newStatus === 'accepted' ? t('appointmentManagement.confirmButton') : t('appointmentManagement.completeButton'),
          style: 'default',
          onPress: async () => {
            try {
              const token = await AsyncStorage.getItem('authToken');
              await axios.put(`${API_BASE_URL}/api/appointments/${appointmentId}`, 
                { status: newStatus },
                { headers: { Authorization: `Bearer ${token}` }}
              );
              loadAppointments();
            } catch (error) {
              console.error('Error updating appointment:', error);
              Alert.alert(t('appointmentManagement.error'), t('appointmentManagement.failedToUpdate'));
            }
          }
        }
      ]
    );
  };

  const cancelAndRemoveAppointment = async (appointmentId) => {
    Alert.alert(
      t('appointmentManagement.cancelAppointment'),
      t('appointmentManagement.cancelAppointmentMessage'),
      [
        { text: t('appointmentManagement.cancel'), style: 'cancel' },
        {
          text: t('appointmentManagement.yesCancel'),
          style: 'destructive',
          onPress: async () => {
            try {
              console.log('Attempting to cancel appointment:', appointmentId);
              const token = await AsyncStorage.getItem('authToken');
              console.log('Token exists:', !!token);
              // Try using PUT route to set status to cancelled (more reliable than DELETE)
              const response = await axios.put(`${API_BASE_URL}/api/appointments/${appointmentId}`, 
                { 
                  status: 'cancelled',
                  vetNotes: 'Cancelled by vet from appointment management'
                },
                { headers: { Authorization: `Bearer ${token}` }}
              );
              console.log('Cancel response:', response.data);
              loadAppointments();
              Alert.alert(t('appointmentManagement.success'), t('appointmentManagement.appointmentCancelled'));
            } catch (error) {
              console.error('Error cancelling appointment:', error);
              console.error('Error response:', error.response?.data);
              // If PUT fails, try the DELETE route as fallback
              try {
                console.log('Trying DELETE route as fallback...');
                const deleteResponse = await axios({
                  method: 'delete',
                  url: `${API_BASE_URL}/api/appointments/${appointmentId}`,
                  headers: { Authorization: `Bearer ${token}` },
                  data: { reason: 'Cancelled by vet from appointment management' }
                });
                console.log('Delete response:', deleteResponse.data);
                loadAppointments();
                Alert.alert(t('appointmentManagement.success'), t('appointmentManagement.appointmentCancelled'));
              } catch (deleteError) {
                console.error('Both PUT and DELETE failed:', deleteError);
                Alert.alert(t('appointmentManagement.error'), deleteError.response?.data?.message || t('appointmentManagement.failedToCancel'));
              }
            }
          }
        }
      ]
    );
  };

  const deleteAppointmentPermanently = async (appointmentId) => {
    Alert.alert(
      t('appointmentManagement.deleteAppointment'),
      t('appointmentManagement.deleteAppointmentMessage'),
      [
        { text: t('appointmentManagement.cancel'), style: 'cancel' },
        {
          text: t('appointmentManagement.delete'),
          style: 'destructive',
          onPress: async () => {
            try {
              console.log('Attempting to delete appointment permanently:', appointmentId);
              const token = await AsyncStorage.getItem('authToken');
              await axios.delete(`${API_BASE_URL}/api/appointments/remove/${appointmentId}`, {
                headers: { Authorization: `Bearer ${token}` }
              });
              console.log('Appointment deleted successfully');
              loadAppointments();
            } catch (error) {
              console.error('Error deleting appointment:', error);
              console.error('Error response:', error.response?.data);
              Alert.alert(t('appointmentManagement.error'), error.response?.data?.message || t('appointmentManagement.failedToDelete'));
            }
          }
        }
      ]
    );
  };

  const filteredAppointments = (Array.isArray(appointments) ? appointments : []).filter(appointment => {
    if (activeFilter === 'all') return true;
    return appointment.status === activeFilter;
  });

  const getStatusColor = (status) => {
    const colors = {
      pending: '#f39c12',
      accepted: '#3498db',
      confirmed: '#3498db',
      completed: '#27ae60',
      cancelled: '#e74c3c',
    };
    return colors[status] || '#95a5a6';
  };

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  };

  const formatTime = (timeString) => {
    return new Date(`1970-01-01T${timeString}`).toLocaleTimeString('en-US', {
      hour: 'numeric',
      minute: '2-digit',
      hour12: true,
    });
  };

  const renderAppointmentItem = ({ item }) => (
    <View style={styles.appointmentCard}>
      <View style={styles.appointmentHeader}>
        <View style={styles.appointmentInfo}>
          <Text style={styles.farmerName}>{item.farmerName || item.farmerId?.name || t('appointmentManagement.unknownFarmer')}</Text>
          <Text style={styles.animalName}>{item.animalName || item.animal?.name || t('appointmentManagement.noAnimal')}</Text>
          <View style={styles.statusContainer}>
            <View style={[styles.statusBadge, { backgroundColor: getStatusColor(item.status) }]}>
              <Text style={styles.statusText}>{item.status.toUpperCase()}</Text>
            </View>
          </View>
        </View>
        <View style={styles.dateTimeContainer}>
          <Text style={styles.dateText}>{formatDate(item.date || item.scheduledDate)}</Text>
          <Text style={styles.timeText}>{formatTime(item.time || item.scheduledTime)}</Text>
        </View>
      </View>

      {(item.reason || item.symptoms) && (
        <Text style={styles.reasonText}>{t('appointmentManagement.reasonLabel')} {item.reason || item.symptoms}</Text>
      )}

      <View style={styles.actionButtons}>
        {item.status === 'pending' && (
          <>
            <TouchableOpacity
              style={[styles.actionButton, styles.confirmButton]}
              onPress={() => updateAppointmentStatus(item._id, 'accepted')}
            >
              <Ionicons name="checkmark" size={16} color="white" />
              <Text style={styles.actionButtonText}>{t('appointmentManagement.confirmButton')}</Text>
            </TouchableOpacity>
          </>
        )}
        
        {(item.status === 'accepted' || item.status === 'confirmed') && (
          <TouchableOpacity
            style={[styles.actionButton, styles.completeButton]}
            onPress={() => updateAppointmentStatus(item._id, 'completed')}
          >
            <Ionicons name="checkmark-circle" size={16} color="white" />
            <Text style={styles.actionButtonText}>{t('appointmentManagement.completeButton')}</Text>
          </TouchableOpacity>
        )}

        {(item.status === 'pending' || item.status === 'accepted' || item.status === 'confirmed') && (
          <TouchableOpacity
            style={[styles.actionButton, styles.cancelButton]}
            onPress={() => cancelAndRemoveAppointment(item._id)}
          >
            <Ionicons name="close" size={16} color="white" />
            <Text style={styles.actionButtonText}>{t('appointmentManagement.cancelButton')}</Text>
          </TouchableOpacity>
        )}

        <TouchableOpacity
          style={[styles.actionButton, styles.deleteButton]}
          onPress={() => deleteAppointmentPermanently(item._id)}
        >
          <Ionicons name="trash" size={16} color="white" />
          <Text style={styles.actionButtonText}>{t('appointmentManagement.deleteButton')}</Text>
        </TouchableOpacity>
      </View>
    </View>
  );

  const renderEmptyState = () => (
    <View style={styles.emptyState}>
      <Ionicons name="calendar-outline" size={64} color="#bdc3c7" />
      <Text style={styles.emptyStateText}>{t('appointmentManagement.noAppointmentsFound')}</Text>
      <Text style={styles.emptyStateSubtext}>
        {activeFilter === 'all' 
          ? t('appointmentManagement.createFirstAppointment')
          : t('appointmentManagement.noFilteredAppointments', { filter: activeFilter })}
      </Text>
    </View>
  );

  const safeAppointments = Array.isArray(appointments) ? appointments : [];
  
  const statsCards = [
    {
      title: t('appointmentManagement.statsTotal'),
      count: safeAppointments.length,
      color: '#3498db',
      icon: 'calendar'
    },
    {
      title: t('appointmentManagement.statsPending'),
      count: safeAppointments.filter(a => a.status === 'pending').length,
      color: '#f39c12',
      icon: 'time'
    },
    {
      title: t('appointmentManagement.statsConfirmed'),
      count: safeAppointments.filter(a => a.status === 'accepted' || a.status === 'confirmed').length,
      color: '#3498db',
      icon: 'checkmark-circle'
    },
    {
      title: t('appointmentManagement.statsCompleted'),
      count: safeAppointments.filter(a => a.status === 'completed').length,
      color: '#27ae60',
      icon: 'checkmark-done-circle'
    },
  ];

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor="#2c3e50" />
      
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity 
          style={styles.backButton}
          onPress={() => router.back()}
        >
          <Ionicons name="arrow-back" size={24} color="white" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>{t('appointmentManagement.headerTitle')}</Text>
        <TouchableOpacity
          style={styles.addButton}
          onPress={() => router.push('/addAppointment')}
        >
          <Ionicons name="add" size={24} color="white" />
        </TouchableOpacity>
      </View>

      {/* Stats Cards */}
      <View style={styles.statsContainer}>
        {statsCards.map((stat, index) => (
          <View key={index} style={[styles.statCard, { borderLeftColor: stat.color }]}>
            <View style={styles.statContent}>
              <View style={styles.statInfo}>
                <Text style={styles.statCount}>{stat.count}</Text>
                <Text style={styles.statLabel}>{stat.title}</Text>
              </View>
              <Ionicons name={stat.icon} size={24} color={stat.color} />
            </View>
          </View>
        ))}
      </View>

      {/* Filter Buttons */}
      <View style={styles.filterContainer}>
        <FlatList
          horizontal
          showsHorizontalScrollIndicator={false}
          data={filterOptions}
          keyExtractor={(item) => item.key}
          renderItem={({ item }) => (
            <TouchableOpacity
              style={[
                styles.filterButton,
                activeFilter === item.key && styles.activeFilterButton
              ]}
              onPress={() => setActiveFilter(item.key)}
            >
              <Text style={[
                styles.filterButtonText,
                activeFilter === item.key && styles.activeFilterButtonText
              ]}>
                {item.label}
              </Text>
            </TouchableOpacity>
          )}
        />
      </View>

      {/* Appointments List */}
      <FlatList
        data={filteredAppointments}
        renderItem={renderAppointmentItem}
        keyExtractor={(item) => item._id}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
        }
        contentContainerStyle={[
          styles.listContainer,
          filteredAppointments.length === 0 && styles.emptyListContainer
        ]}
        ListEmptyComponent={renderEmptyState}
      />
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8f9fa',
  },
  header: {
    backgroundColor: '#2c3e50',
    paddingTop: 10,
    paddingBottom: 15,
    paddingHorizontal: 20,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  backButton: {
    padding: 5,
  },
  headerTitle: {
    color: 'white',
    fontSize: 20,
    fontWeight: 'bold',
    flex: 1,
    textAlign: 'center',
    marginHorizontal: 10,
  },
  addButton: {
    padding: 5,
  },
  statsContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    padding: 15,
    gap: 10,
  },
  statCard: {
    backgroundColor: 'white',
    borderRadius: 12,
    padding: 15,
    borderLeftWidth: 4,
    flex: 1,
    minWidth: (width - 50) / 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  statContent: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  statInfo: {
    flex: 1,
  },
  statCount: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#2c3e50',
  },
  statLabel: {
    fontSize: 14,
    color: '#7f8c8d',
    marginTop: 2,
  },
  filterContainer: {
    paddingHorizontal: 15,
    marginBottom: 10,
  },
  filterButton: {
    paddingHorizontal: 20,
    paddingVertical: 8,
    marginRight: 10,
    backgroundColor: '#ecf0f1',
    borderRadius: 20,
    minWidth: 80,
    alignItems: 'center',
  },
  activeFilterButton: {
    backgroundColor: '#3498db',
  },
  filterButtonText: {
    color: '#2c3e50',
    fontWeight: '500',
    fontSize: 14,
  },
  activeFilterButtonText: {
    color: 'white',
  },
  listContainer: {
    padding: 15,
  },
  emptyListContainer: {
    flex: 1,
    justifyContent: 'center',
  },
  appointmentCard: {
    backgroundColor: 'white',
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  appointmentHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 12,
  },
  appointmentInfo: {
    flex: 1,
  },
  farmerName: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#2c3e50',
    marginBottom: 4,
  },
  animalName: {
    fontSize: 14,
    color: '#7f8c8d',
    marginBottom: 8,
  },
  statusContainer: {
    alignSelf: 'flex-start',
  },
  statusBadge: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 12,
  },
  statusText: {
    color: 'white',
    fontSize: 10,
    fontWeight: 'bold',
  },
  dateTimeContainer: {
    alignItems: 'flex-end',
  },
  dateText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#2c3e50',
  },
  timeText: {
    fontSize: 12,
    color: '#7f8c8d',
    marginTop: 2,
  },
  reasonText: {
    fontSize: 14,
    color: '#34495e',
    marginBottom: 12,
    fontStyle: 'italic',
  },
  actionButtons: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  actionButton: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 6,
    gap: 4,
  },
  confirmButton: {
    backgroundColor: '#3498db',
  },
  completeButton: {
    backgroundColor: '#27ae60',
  },
  cancelButton: {
    backgroundColor: '#f39c12',
  },
  deleteButton: {
    backgroundColor: '#e74c3c',
  },
  actionButtonText: {
    color: 'white',
    fontSize: 12,
    fontWeight: '500',
  },
  emptyState: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 60,
  },
  emptyStateText: {
    fontSize: 18,
    fontWeight: '600',
    color: '#7f8c8d',
    marginTop: 16,
    marginBottom: 8,
  },
  emptyStateSubtext: {
    fontSize: 14,
    color: '#95a5a6',
    textAlign: 'center',
    lineHeight: 20,
  },
});

export default AppointmentManagement;
