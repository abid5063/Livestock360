import React, { useEffect, useState } from 'react';
import { View, Text, FlatList, TouchableOpacity, Alert, StyleSheet, ActivityIndicator } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Ionicons } from '@expo/vector-icons';
import { API_BASE_URL } from '../utils/apiConfig';
import axios from 'axios';
import { router } from 'expo-router';
import { useLanguage } from '../utils/LanguageContext';
import { useTranslation } from 'react-i18next';

export default function FarmerAppointmentManagement() {
  const { language } = useLanguage();
  const { t, i18n } = useTranslation();

  // Update i18n language when language changes
  useEffect(() => {
    i18n.changeLanguage(language);
  }, [language, i18n]);

  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchAppointments();
  }, []);

  const fetchAppointments = async () => {
    setLoading(true);
    try {
      const token = await AsyncStorage.getItem('authToken');
      const response = await fetch(`${API_BASE_URL}/api/appointments/farmer`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      const data = await response.json();
      // If response has appointments array, use it
      setAppointments(Array.isArray(data.appointments) ? data.appointments : data);
    } catch (error) {
      Alert.alert(t('farmerAppointmentManagement.error'), t('farmerAppointmentManagement.failedToLoad'));
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (appointmentId) => {
    Alert.alert(
      t('farmerAppointmentManagement.deleteAppointment'),
      t('farmerAppointmentManagement.deleteAppointmentMessage'),
      [
        { text: t('farmerAppointmentManagement.cancel'), style: 'cancel' },
        {
          text: t('farmerAppointmentManagement.delete'),
          style: 'destructive',
          onPress: async () => {
            try {
              const token = await AsyncStorage.getItem('authToken');
              await axios.delete(`${API_BASE_URL}/api/appointments/remove/${appointmentId}`, {
                headers: { Authorization: `Bearer ${token}` }
              });
              setAppointments(appointments.filter(a => a._id !== appointmentId));
            } catch (error) {
              Alert.alert(t('farmerAppointmentManagement.error'), t('farmerAppointmentManagement.failedToDelete'));
            }
          }
        }
      ]
    );
  };

  const renderItem = ({ item }) => (
    <View style={styles.card}>
      <View style={styles.info}>
        <Text style={styles.title}>{item.animalName || item.animalId?.name || t('farmerAppointmentManagement.animal')}</Text>
        <Text style={styles.detail}>{t('farmerAppointmentManagement.vet')} {item.vetId?.name || 'N/A'}</Text>
        <Text style={styles.detail}>{t('farmerAppointmentManagement.date')} {item.scheduledDate?.slice(0, 10)}</Text>
        <Text style={styles.detail}>{t('farmerAppointmentManagement.time')} {item.scheduledTime}</Text>
        <Text style={styles.detail}>{t('farmerAppointmentManagement.status')} {item.status}</Text>
      </View>
      <TouchableOpacity onPress={() => handleDelete(item._id)} style={styles.deleteButton}>
        <Ionicons name="trash" size={22} color="#7c1004ff" />
      </TouchableOpacity>
    </View>
  );

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#084c79ff" />
        <Text>{t('farmerAppointmentManagement.loadingAppointments')}</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Text style={styles.header}>{t('farmerAppointmentManagement.header')}</Text>
      <FlatList
        data={appointments}
        keyExtractor={item => item._id}
        renderItem={renderItem}
        ListEmptyComponent={<Text style={styles.emptyText}>{t('farmerAppointmentManagement.noAppointmentsFound')}</Text>}
        contentContainerStyle={appointments.length === 0 && { flex: 1, justifyContent: 'center', alignItems: 'center' }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8f9fa',
    padding: 16,
  },
  header: {
    fontSize: 22,
    fontWeight: 'bold',
    color: '#2c3e50',
    marginBottom: 20,
    textAlign: 'center',
  },
  card: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.08,
    shadowRadius: 2,
    elevation: 2,
  },
  info: {
    flex: 1,
  },
  title: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#3498db',
    marginBottom: 4,
  },
  detail: {
    fontSize: 15,
    color: '#555',
    marginBottom: 2,
  },
  deleteButton: {
    marginLeft: 16,
    padding: 8,
    borderRadius: 8,
    backgroundColor: '#fbeaea',
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  emptyText: {
    fontSize: 16,
    color: '#888',
    textAlign: 'center',
    marginTop: 40,
  },
});
