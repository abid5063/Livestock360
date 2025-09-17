import React, { useState, useEffect } from 'react';
import { 
  View, 
  Text, 
  ScrollView, 
  TouchableOpacity, 
  StyleSheet, 
  SafeAreaView, 
  Alert,
  RefreshControl
} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { router } from 'expo-router';
import { MaterialIcons, Ionicons } from '@expo/vector-icons';
import { API_BASE_URL, ALL_ALERTS } from '../utils/apiConfig';

const CustomerDashboard = () => {
  const [customerData, setCustomerData] = useState(null);
  const [refreshing, setRefreshing] = useState(false);
  const [stats, setStats] = useState({
    totalAnimalsAvailable: 0,
    recentlyAdded: 0,
    favoriteTypes: []
  });

  useEffect(() => {
    loadCustomerData();
    fetchStats();
  }, []);

  const loadCustomerData = async () => {
    try {
      const token = await AsyncStorage.getItem('customerToken');
      const storedData = await AsyncStorage.getItem('customerData');
      
      console.log('🔑 Customer token exists:', !!token);
      console.log('💾 Customer data exists:', !!storedData);
      
      if (token && storedData) {
        // Skip backend validation for now and just use stored data
        console.log('✅ Loading customer data from storage');
        setCustomerData(JSON.parse(storedData));
      } else {
        // No token or data, redirect to auth
        console.log('❌ No customer token or data found, redirecting to login');
        router.replace('/');
      }
    } catch (error) {
      console.error('❌ Error loading customer data:', error);
      // Clear potentially corrupted data and redirect
      await AsyncStorage.multiRemove(['customerToken', 'customerData', 'userType']);
      router.replace('/');
    }
  };

  const fetchStats = async () => {
    try {
      const token = await AsyncStorage.getItem('customerToken');
      if (!token) return;

      // Fetch marketplace stats
      const response = await fetch(`${API_BASE_URL}/api/marketplace/animals`, {
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (response.status === 401 || response.status === 403) {
        // Token is invalid, log out user
        await AsyncStorage.multiRemove(['customerToken', 'customerData', 'userType']);
        router.replace('/');
        return;
      }

      const data = await response.json();
      if (data.success) {
        setStats(prev => ({
          ...prev,
          totalAnimalsAvailable: data.totalAnimals || 0
        }));
      }
    } catch (error) {
      console.error('Error fetching stats:', error);
    }
  };

  const onRefresh = async () => {
    setRefreshing(true);
    await loadCustomerData();
    await fetchStats();
    setRefreshing(false);
  };

  const performLogout = async () => {
    try {
      // Clear all customer-related data
      await AsyncStorage.multiRemove(['customerToken', 'customerData', 'userType']);
      
      // Clear any cached data
      setCustomerData(null);
      setStats({
        totalAnimalsAvailable: 0,
        recentlyAdded: 0,
        favoriteTypes: []
      });
      
      // Navigate to the welcome screen and reset navigation stack
      router.replace('/');
    } catch (error) {
      console.error('Error during logout:', error);
      // Force navigation even if there's an error clearing storage
      router.replace('/');
    }
  };

  const handleLogout = () => {
    if (ALL_ALERTS) {
      Alert.alert(
        'Logout',
        'Are you sure you want to logout?',
        [
          { text: 'Cancel', style: 'cancel' },
          { 
            text: 'Logout', 
            style: 'destructive',
            onPress: performLogout
          }
        ]
      );
    } else {
      performLogout();
    }
  };

  if (!customerData) {
    return (
      <SafeAreaView style={styles.container}>
        <View style={styles.loadingContainer}>
          <Text>Loading...</Text>
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView 
        style={styles.scrollView}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
        }
      >
        {/* Header */}
        <View style={styles.header}>
          <View>
            <Text style={styles.welcomeText}>Welcome back,</Text>
            <Text style={styles.nameText}>{customerData.name}</Text>
            <Text style={styles.subtitleText}>
              {customerData.customerType === 'business' ? 'Business Customer' : 'Individual Customer'}
            </Text>
          </View>
          <TouchableOpacity onPress={handleLogout} style={styles.logoutButton}>
            <MaterialIcons name="logout" size={24} color="#fff" />
          </TouchableOpacity>
        </View>

        {/* Quick Stats */}
        <View style={styles.statsContainer}>
          <Text style={styles.sectionTitle}>Marketplace Overview</Text>
          <View style={styles.statsRow}>
            <View style={styles.statCard}>
              <Ionicons name="animals" size={30} color="#2E7D32" />
              <Text style={styles.statNumber}>{stats.totalAnimalsAvailable}</Text>
              <Text style={styles.statLabel}>Animals Available</Text>
            </View>
            <View style={styles.statCard}>
              <MaterialIcons name="account-balance-wallet" size={30} color="#2E7D32" />
              <Text style={styles.statNumber}>
                ${customerData.maxBudget ? customerData.maxBudget.toLocaleString() : '0'}
              </Text>
              <Text style={styles.statLabel}>Max Budget</Text>
            </View>
          </View>
        </View>

        {/* Quick Actions */}
        <View style={styles.actionsContainer}>
          <Text style={styles.sectionTitle}>Browse & Shop</Text>
          
          <TouchableOpacity 
            style={styles.actionCard}
            onPress={() => router.push('/animalMarketplace')}
          >
            <View style={styles.actionIcon}>
              <Ionicons name="storefront" size={30} color="#2E7D32" />
            </View>
            <View style={styles.actionContent}>
              <Text style={styles.actionTitle}>Animal Marketplace</Text>
              <Text style={styles.actionDescription}>Browse and search available animals</Text>
            </View>
            <MaterialIcons name="chevron-right" size={24} color="#ccc" />
          </TouchableOpacity>

          <TouchableOpacity 
            style={[styles.actionCard, styles.buyProductsCard]}
            onPress={() => router.push('/buyProducts')}
          >
            <View style={styles.actionIcon}>
              <Ionicons name="basket" size={30} color="#FF6B35" />
            </View>
            <View style={styles.actionContent}>
              <Text style={styles.actionTitle}>Buy Products</Text>
              <Text style={styles.actionDescription}>Order milk, butter, and eggs from local farmers</Text>
            </View>
            <MaterialIcons name="chevron-right" size={24} color="#ccc" />
          </TouchableOpacity>

          <TouchableOpacity 
            style={styles.actionCard}
            onPress={() => router.push('/customerProfile')}
          >
            <View style={styles.actionIcon}>
              <MaterialIcons name="person" size={30} color="#2E7D32" />
            </View>
            <View style={styles.actionContent}>
              <Text style={styles.actionTitle}>My Profile</Text>
              <Text style={styles.actionDescription}>View and edit your profile information</Text>
            </View>
            <MaterialIcons name="chevron-right" size={24} color="#ccc" />
          </TouchableOpacity>

          {/* <TouchableOpacity 
            style={styles.actionCard}
            onPress={() => router.push('/customerSettings')}
          >
            <View style={styles.actionIcon}>
              <MaterialIcons name="settings" size={30} color="#2E7D32" />
            </View>
            <View style={styles.actionContent}>
              <Text style={styles.actionTitle}>Settings</Text>
              <Text style={styles.actionDescription}>Manage preferences and account settings</Text>
            </View>
            <MaterialIcons name="chevron-right" size={24} color="#ccc" />
          </TouchableOpacity> */}
        </View>

        {/* Customer Info */}
        <View style={styles.infoContainer}>
          <Text style={styles.sectionTitle}>Account Information</Text>
          
          <View style={styles.infoRow}>
            <Text style={styles.infoLabel}>Email:</Text>
            <Text style={styles.infoValue}>{customerData.email}</Text>
          </View>
          
          {customerData.phone && (
            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>Phone:</Text>
              <Text style={styles.infoValue}>{customerData.phone}</Text>
            </View>
          )}
          
          {customerData.location && (
            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>Location:</Text>
              <Text style={styles.infoValue}>{customerData.location}</Text>
            </View>
          )}

          {customerData.customerType === 'business' && customerData.businessName && (
            <View style={styles.infoRow}>
              <Text style={styles.infoLabel}>Business:</Text>
              <Text style={styles.infoValue}>{customerData.businessName}</Text>
            </View>
          )}

          <View style={styles.infoRow}>
            <Text style={styles.infoLabel}>Member Since:</Text>
            <Text style={styles.infoValue}>
              {customerData.dateJoined ? new Date(customerData.dateJoined).toLocaleDateString() : 'Recently'}
            </Text>
          </View>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  scrollView: {
    flex: 1,
  },
  header: {
    backgroundColor: '#2E7D32',
    padding: 20,
    paddingTop: 40,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  welcomeText: {
    color: '#fff',
    fontSize: 16,
    opacity: 0.9,
  },
  nameText: {
    color: '#fff',
    fontSize: 24,
    fontWeight: 'bold',
    marginTop: 5,
  },
  subtitleText: {
    color: '#fff',
    fontSize: 14,
    opacity: 0.8,
    marginTop: 2,
  },
  logoutButton: {
    padding: 10,
    backgroundColor: 'rgba(255,255,255,0.2)',
    borderRadius: 50,
  },
  statsContainer: {
    padding: 20,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    marginBottom: 15,
    color: '#333',
  },
  statsRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  statCard: {
    backgroundColor: '#fff',
    padding: 20,
    borderRadius: 10,
    alignItems: 'center',
    flex: 1,
    marginHorizontal: 5,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 3.84,
    elevation: 5,
  },
  statNumber: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#2E7D32',
    marginTop: 10,
  },
  statLabel: {
    fontSize: 12,
    color: '#666',
    textAlign: 'center',
    marginTop: 5,
  },
  actionsContainer: {
    padding: 20,
    paddingTop: 0,
  },
  actionCard: {
    backgroundColor: '#fff',
    padding: 15,
    borderRadius: 10,
    marginBottom: 10,
    flexDirection: 'row',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 3.84,
    elevation: 5,
  },
  actionIcon: {
    marginRight: 15,
  },
  actionContent: {
    flex: 1,
  },
  actionTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333',
  },
  actionDescription: {
    fontSize: 14,
    color: '#666',
    marginTop: 2,
  },
  infoContainer: {
    padding: 20,
    paddingTop: 0,
  },
  infoRow: {
    backgroundColor: '#fff',
    padding: 15,
    borderRadius: 8,
    marginBottom: 10,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  infoLabel: {
    fontSize: 14,
    color: '#666',
    fontWeight: '500',
  },
  infoValue: {
    fontSize: 14,
    color: '#333',
    flex: 1,
    textAlign: 'right',
  },
  buyProductsCard: {
    borderLeftWidth: 4,
    borderLeftColor: '#FF6B35',
  },
});

export default CustomerDashboard;