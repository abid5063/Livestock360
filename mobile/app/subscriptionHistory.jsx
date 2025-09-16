import React, { useState, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  Alert,
  ActivityIndicator,
  RefreshControl,
} from 'react-native';
import { router } from 'expo-router';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Ionicons } from '@expo/vector-icons';
import axios from 'axios';
import { API_BASE_URL } from '../utils/apiConfig';

export default function SubscriptionHistory() {
  const [subscriptions, setSubscriptions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [farmer, setFarmer] = useState(null);

  const loadData = useCallback(async () => {
    try {
      const storedData = await AsyncStorage.getItem('userData');
      if (storedData) {
        const farmerData = JSON.parse(storedData);
        setFarmer(farmerData);
        await fetchSubscriptionHistory(farmerData.id);
      }
    } catch (error) {
      console.error('Error loading data:', error);
      Alert.alert('Error', 'Failed to load data');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const fetchSubscriptionHistory = async (farmerId) => {
    try {
      const token = await AsyncStorage.getItem('authToken');
      if (!token) {
        Alert.alert('Authentication Error', 'Please login again');
        router.replace('/');
        return;
      }

      const response = await axios.get(
        `${API_BASE_URL}/api/subscriptions/user/${farmerId}`,
        {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json',
          },
        }
      );

      if (response.data.success) {
        setSubscriptions(response.data.subscriptions);
      } else {
        Alert.alert('Error', response.data.message || 'Failed to fetch subscription history');
      }
    } catch (error) {
      console.error('Error fetching subscription history:', error);
      if (error.response?.status === 401) {
        Alert.alert('Session Expired', 'Please login again');
        await AsyncStorage.multiRemove(['authToken', 'userData']);
        router.replace('/');
      } else {
        Alert.alert('Error', 'Failed to fetch subscription history');
      }
    }
  };

  const onRefresh = async () => {
    setRefreshing(true);
    if (farmer) {
      await fetchSubscriptionHistory(farmer.id);
    }
    setRefreshing(false);
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'approved':
        return '#4CAF50';
      case 'rejected':
        return '#F44336';
      case 'pending':
        return '#FF9800';
      default:
        return '#666';
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'approved':
        return 'checkmark-circle';
      case 'rejected':
        return 'close-circle';
      case 'pending':
        return 'time';
      default:
        return 'help-circle';
    }
  };

  const getPackageInfo = (packageType) => {
    switch (packageType) {
      case 'basic':
        return { name: 'Basic Package', color: '#4CAF50' };
      case 'standard':
        return { name: 'Standard Package', color: '#2196F3' };
      case 'premium':
        return { name: 'Premium Package', color: '#FF9800' };
      default:
        return { name: 'Unknown Package', color: '#666' };
    }
  };

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#4CAF50" />
        <Text style={styles.loadingText}>Loading subscription history...</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity 
          style={styles.backButton} 
          onPress={() => router.back()}
        >
          <Ionicons name="arrow-back" size={24} color="#fff" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Subscription History</Text>
        <TouchableOpacity 
          style={styles.addButton}
          onPress={() => router.push('/addSubscription')}
        >
          <Ionicons name="add" size={24} color="#fff" />
        </TouchableOpacity>
      </View>

      <ScrollView 
        style={styles.content}
        contentContainerStyle={styles.contentContainer}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={onRefresh}
            colors={['#4CAF50']}
          />
        }
      >
        {subscriptions.length === 0 ? (
          <View style={styles.emptyContainer}>
            <Ionicons name="document-text-outline" size={64} color="#ccc" />
            <Text style={styles.emptyTitle}>No Subscriptions Yet</Text>
            <Text style={styles.emptySubtitle}>
              You haven&apos;t made any subscription requests yet.
            </Text>
            <TouchableOpacity 
              style={styles.addSubscriptionButton}
              onPress={() => router.push('/addSubscription')}
            >
              <Ionicons name="add-circle" size={20} color="#fff" />
              <Text style={styles.addSubscriptionButtonText}>Add Subscription</Text>
            </TouchableOpacity>
          </View>
        ) : (
          <>
            {/* Summary Stats */}
            <View style={styles.statsContainer}>
              <View style={styles.statCard}>
                <Text style={styles.statNumber}>
                  {subscriptions.filter(sub => sub.status === 'approved').length}
                </Text>
                <Text style={styles.statLabel}>Approved</Text>
                <Ionicons name="checkmark-circle" size={20} color="#4CAF50" />
              </View>
              <View style={styles.statCard}>
                <Text style={styles.statNumber}>
                  {subscriptions.filter(sub => sub.status === 'pending').length}
                </Text>
                <Text style={styles.statLabel}>Pending</Text>
                <Ionicons name="time" size={20} color="#FF9800" />
              </View>
              <View style={styles.statCard}>
                <Text style={styles.statNumber}>
                  {subscriptions.filter(sub => sub.status === 'rejected').length}
                </Text>
                <Text style={styles.statLabel}>Rejected</Text>
                <Ionicons name="close-circle" size={20} color="#F44336" />
              </View>
            </View>

            {/* Subscription List */}
            <View style={styles.subscriptionsList}>
              {subscriptions.map((subscription) => {
                const packageInfo = getPackageInfo(subscription.subscriptionPackage);
                return (
                  <View key={subscription.id} style={styles.subscriptionCard}>
                    {/* Card Header */}
                    <View style={styles.cardHeader}>
                      <View style={styles.packageBadge}>
                        <View 
                          style={[styles.packageIcon, { backgroundColor: packageInfo.color }]}
                        >
                          <Ionicons name="diamond" size={16} color="#fff" />
                        </View>
                        <Text style={styles.packageName}>{packageInfo.name}</Text>
                      </View>
                      <View style={styles.statusContainer}>
                        <Ionicons 
                          name={getStatusIcon(subscription.status)} 
                          size={20} 
                          color={getStatusColor(subscription.status)} 
                        />
                        <Text 
                          style={[
                            styles.statusText, 
                            { color: getStatusColor(subscription.status) }
                          ]}
                        >
                          {subscription.status.toUpperCase()}
                        </Text>
                      </View>
                    </View>

                    {/* Card Content */}
                    <View style={styles.cardContent}>
                      <View style={styles.contentRow}>
                        <View style={styles.contentItem}>
                          <Text style={styles.contentLabel}>Amount</Text>
                          <Text style={styles.contentValue}>{subscription.amount} TK</Text>
                        </View>
                        <View style={styles.contentItem}>
                          <Text style={styles.contentLabel}>Tokens</Text>
                          <Text style={styles.contentValue}>{subscription.tokens}</Text>
                        </View>
                      </View>
                      
                      <View style={styles.contentRow}>
                        <View style={styles.contentItem}>
                          <Text style={styles.contentLabel}>Transaction ID</Text>
                          <Text style={styles.transactionId}>{subscription.transactionId}</Text>
                        </View>
                      </View>
                      
                      <View style={styles.contentRow}>
                        <View style={styles.contentItem}>
                          <Text style={styles.contentLabel}>Submitted</Text>
                          <Text style={styles.contentValue}>{formatDate(subscription.createdAt)}</Text>
                        </View>
                      </View>

                      {/* Admin Notes */}
                      {subscription.adminNotes && (
                        <View style={styles.adminNotesContainer}>
                          <Text style={styles.adminNotesLabel}>Admin Notes:</Text>
                          <Text style={styles.adminNotesText}>{subscription.adminNotes}</Text>
                        </View>
                      )}
                    </View>
                  </View>
                );
              })}
            </View>
          </>
        )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#f5f5f5',
  },
  loadingText: {
    marginTop: 16,
    fontSize: 16,
    color: '#666',
  },
  header: {
    backgroundColor: '#4CAF50',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingTop: 50,
    paddingBottom: 20,
    paddingHorizontal: 16,
  },
  backButton: {
    padding: 8,
  },
  headerTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#fff',
  },
  addButton: {
    padding: 8,
  },
  content: {
    flex: 1,
  },
  contentContainer: {
    paddingBottom: 30,
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 32,
    marginTop: 100,
  },
  emptyTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333',
    marginTop: 16,
    marginBottom: 8,
  },
  emptySubtitle: {
    fontSize: 16,
    color: '#666',
    textAlign: 'center',
    marginBottom: 32,
    lineHeight: 24,
  },
  addSubscriptionButton: {
    backgroundColor: '#4CAF50',
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 24,
    paddingVertical: 12,
    borderRadius: 12,
    gap: 8,
  },
  addSubscriptionButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
  },
  statsContainer: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    margin: 16,
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 20,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  statCard: {
    alignItems: 'center',
  },
  statNumber: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 4,
  },
  statLabel: {
    fontSize: 12,
    color: '#666',
    marginBottom: 8,
  },
  subscriptionsList: {
    paddingHorizontal: 16,
  },
  subscriptionCard: {
    backgroundColor: '#fff',
    borderRadius: 12,
    marginBottom: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
    overflow: 'hidden',
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 16,
    backgroundColor: '#f8f9fa',
    borderBottomWidth: 1,
    borderBottomColor: '#e9ecef',
  },
  packageBadge: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  packageIcon: {
    width: 32,
    height: 32,
    borderRadius: 16,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 8,
  },
  packageName: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333',
  },
  statusContainer: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  statusText: {
    fontSize: 12,
    fontWeight: 'bold',
    marginLeft: 4,
  },
  cardContent: {
    padding: 16,
  },
  contentRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 12,
  },
  contentItem: {
    flex: 1,
  },
  contentLabel: {
    fontSize: 12,
    color: '#666',
    marginBottom: 4,
  },
  contentValue: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
  },
  transactionId: {
    fontSize: 14,
    fontWeight: '600',
    color: '#2196F3',
    fontFamily: 'monospace',
  },
  adminNotesContainer: {
    backgroundColor: '#f8f9fa',
    padding: 12,
    borderRadius: 8,
    marginTop: 8,
    borderLeftWidth: 3,
    borderLeftColor: '#4CAF50',
  },
  adminNotesLabel: {
    fontSize: 12,
    fontWeight: 'bold',
    color: '#666',
    marginBottom: 4,
  },
  adminNotesText: {
    fontSize: 14,
    color: '#333',
    lineHeight: 20,
  },
});