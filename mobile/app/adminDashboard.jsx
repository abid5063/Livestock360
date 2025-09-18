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
  Modal,
  TextInput,
  SafeAreaView,
} from 'react-native';
import { router } from 'expo-router';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Ionicons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import axios from 'axios';
import { API_BASE_URL, ALL_ALERTS } from '../utils/apiConfig';

export default function AdminDashboard() {
  const [subscriptions, setSubscriptions] = useState([]);
  // Stats are now calculated dynamically from subscriptions array
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [admin, setAdmin] = useState(null);
  const [modalVisible, setModalVisible] = useState(false);
  const [selectedSubscription, setSelectedSubscription] = useState(null);
  const [adminNotes, setAdminNotes] = useState('');
  const [processingId, setProcessingId] = useState(null);
  const [currentFilter, setCurrentFilter] = useState('pending'); // Default to pending

  const loadData = useCallback(async () => {
    try {
      const adminData = await AsyncStorage.getItem('adminData');
      if (adminData) {
        setAdmin(JSON.parse(adminData));
        await fetchSubscriptions(); // Only fetch subscriptions, stats are calculated dynamically
      }
    } catch (error) {
      console.error('Error loading admin data:', error);
      if (ALL_ALERTS) {
        Alert.alert('Error', 'Failed to load admin data');
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const fetchSubscriptions = async () => {
    try {
      const token = await AsyncStorage.getItem('adminToken');
      if (!token) {
        if (ALL_ALERTS) {
          Alert.alert('Authentication Error', 'Please login again');
        }
        router.replace('/');
        return;
      }

      const response = await axios.get(`${API_BASE_URL}/api/admin/subscriptions`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });

      if (response.data.success) {
        setSubscriptions(response.data.subscriptions);
      } else {
        if (ALL_ALERTS) {
          Alert.alert('Error', response.data.message || 'Failed to fetch subscriptions');
        }
      }
    } catch (error) {
      console.error('Error fetching subscriptions:', error);
      if (error.response?.status === 401) {
        if (ALL_ALERTS) {
          Alert.alert('Session Expired', 'Please login again');
        }
        await AsyncStorage.multiRemove(['adminToken', 'adminData']);
        router.replace('/');
      } else {
        if (ALL_ALERTS) {
          Alert.alert('Error', 'Failed to fetch subscriptions');
        }
      }
    }
  };

  // Stats are calculated dynamically from subscriptions, no separate API call needed

  const onRefresh = async () => {
    setRefreshing(true);
    await fetchSubscriptions(); // Only fetch subscriptions, stats are calculated dynamically
    setRefreshing(false);
  };

  const handleStatusUpdate = async (subscriptionId, newStatus) => {
    setProcessingId(subscriptionId);
    
    try {
      const token = await AsyncStorage.getItem('adminToken');
      const response = await axios.put(
        `${API_BASE_URL}/api/admin/subscriptions/${subscriptionId}/status`,
        {
          status: newStatus,
          adminNotes: adminNotes.trim() || null,
        },
        {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json',
          },
        }
      );

      if (response.data.success) {
        if (ALL_ALERTS) {
          Alert.alert('Success', `Subscription ${newStatus} successfully`);
        }
        setModalVisible(false);
        setAdminNotes('');
        setSelectedSubscription(null);
        await fetchSubscriptions(); // Only fetch subscriptions, stats are calculated dynamically
      } else {
        if (ALL_ALERTS) {
          Alert.alert('Error', response.data.message || 'Failed to update subscription');
        }
      }
    } catch (error) {
      console.error('Error updating subscription:', error);
      if (ALL_ALERTS) {
        Alert.alert('Error', 'Failed to update subscription status');
      }
    } finally {
      setProcessingId(null);
    }
  };

  const openStatusModal = (subscription) => {
    setSelectedSubscription(subscription);
    setAdminNotes(subscription.adminNotes || '');
    setModalVisible(true);
  };

  const handleLogout = async () => {
    if (ALL_ALERTS) {
      Alert.alert(
        'Logout',
        'Are you sure you want to logout?',
        [
          { text: 'Cancel', style: 'cancel' },
          {
            text: 'Logout',
            style: 'destructive',
            onPress: async () => {
              await AsyncStorage.multiRemove(['adminToken', 'adminData']);
              router.replace('/');
            },
          },
        ]
      );
    } else {
      await AsyncStorage.multiRemove(['adminToken', 'adminData']);
      router.replace('/');
    }
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
        return { name: 'Basic', color: '#4CAF50', badge: '50 TK' };
      case 'standard':
        return { name: 'Standard', color: '#2196F3', badge: '100 TK' };
      case 'premium':
        return { name: 'Premium', color: '#FF9800', badge: '500 TK' };
      default:
        return { name: 'Unknown', color: '#666', badge: '???' };
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

  // Calculate stats dynamically from subscriptions
  const calculateStats = () => {
    const pending = subscriptions.filter(s => s.status === 'pending').length;
    const approved = subscriptions.filter(s => s.status === 'approved').length; 
    const rejected = subscriptions.filter(s => s.status === 'rejected').length;
    const total = subscriptions.length;
    return { pending, approved, rejected, total };
  };

  // Get dynamic stats
  const dynamicStats = calculateStats();

  // Filter subscriptions based on current filter
  const filteredSubscriptions = subscriptions.filter(subscription => {
    if (currentFilter === 'all') return true;
    return subscription.status === currentFilter;
  });

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#dc3545" />
        <Text style={styles.loadingText}>Loading dashboard...</Text>
      </View>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <LinearGradient
        colors={['#ffffff', '#f8f9fa', '#ffffff']}
        style={styles.gradientBackground}
        start={{ x: 0, y: 0 }}
        end={{ x: 1, y: 1 }}
      />

      {/* Header */}
      <View style={styles.header}>
        <View style={styles.headerLeft}>
          <Ionicons name="shield-checkmark" size={24} color="#2E7D32" />
          <Text style={styles.headerTitle}>Admin Dashboard</Text>
        </View>
        <TouchableOpacity style={styles.logoutButton} onPress={handleLogout}>
          <Ionicons name="log-out" size={20} color="#2E7D32" />
        </TouchableOpacity>
      </View>

      {/* Admin Info */}
      {admin && (
        <View style={styles.adminInfoContainer}>
          <Text style={styles.adminWelcome}>Welcome back, {admin.email}</Text>
          <Text style={styles.adminLastLogin}>
            Last login: {formatDate(admin.lastLogin)}
          </Text>
        </View>
      )}

      <ScrollView
        style={styles.content}
        contentContainerStyle={styles.contentContainer}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={onRefresh}
            colors={['#2E7D32']}
            tintColor="#2E7D32"
          />
        }
      >
        {/* Stats Overview */}
        <View style={styles.statsContainer}>
          <Text style={styles.sectionTitle}>Subscription Overview</Text>
          <View style={styles.statsGrid}>
            <View style={[styles.statCard, { borderLeftColor: '#FF9800' }]}>
              <Text style={styles.statNumber}>{dynamicStats.pending}</Text>
              <Text style={styles.statLabel}>Pending</Text>
            </View>
            <View style={[styles.statCard, { borderLeftColor: '#4CAF50' }]}>
              <Text style={styles.statNumber}>{dynamicStats.approved}</Text>
              <Text style={styles.statLabel}>Approved</Text>
            </View>
            <View style={[styles.statCard, { borderLeftColor: '#F44336' }]}>
              <Text style={styles.statNumber}>{dynamicStats.rejected}</Text>
              <Text style={styles.statLabel}>Rejected</Text>
            </View>
            <View style={[styles.statCard, { borderLeftColor: '#2196F3' }]}>
              <Text style={styles.statNumber}>{dynamicStats.total}</Text>
              <Text style={styles.statLabel}>Total</Text>
            </View>
          </View>
        </View>

        {/* Filter Buttons */}
        <View style={styles.filterContainer}>
          <Text style={styles.sectionTitle}>Filter Subscriptions</Text>
          <View style={styles.filterButtons}>
            <TouchableOpacity
              style={[styles.filterButton, currentFilter === 'pending' && styles.activeFilterButton]}
              onPress={() => setCurrentFilter('pending')}
            >
              <Text style={[styles.filterButtonText, currentFilter === 'pending' && styles.activeFilterButtonText]}>
                Pending ({dynamicStats.pending})
              </Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.filterButton, currentFilter === 'approved' && styles.activeFilterButton]}
              onPress={() => setCurrentFilter('approved')}
            >
              <Text style={[styles.filterButtonText, currentFilter === 'approved' && styles.activeFilterButtonText]}>
                Approved ({dynamicStats.approved})
              </Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.filterButton, currentFilter === 'rejected' && styles.activeFilterButton]}
              onPress={() => setCurrentFilter('rejected')}
            >
              <Text style={[styles.filterButtonText, currentFilter === 'rejected' && styles.activeFilterButtonText]}>
                Rejected ({dynamicStats.rejected})
              </Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.filterButton, currentFilter === 'all' && styles.activeFilterButton]}
              onPress={() => setCurrentFilter('all')}
            >
              <Text style={[styles.filterButtonText, currentFilter === 'all' && styles.activeFilterButtonText]}>
                All ({dynamicStats.total})
              </Text>
            </TouchableOpacity>
          </View>
        </View>

        {/* Subscriptions List */}
        <View style={styles.subscriptionsSection}>
          <Text style={styles.sectionTitle}>
            {currentFilter === 'all' ? 'All Subscriptions' : 
             currentFilter === 'pending' ? 'Pending Subscriptions' :
             currentFilter === 'approved' ? 'Approved Subscriptions' : 
             'Rejected Subscriptions'}
          </Text>
          
          {filteredSubscriptions.length === 0 ? (
            <View style={styles.emptyContainer}>
              <Ionicons name="document-text-outline" size={48} color="#81C784" />
              <Text style={styles.emptyText}>
                {currentFilter === 'all' ? 'No subscriptions found' :
                 `No ${currentFilter} subscriptions found`}
              </Text>
            </View>
          ) : (
            filteredSubscriptions.map((subscription) => {
              const packageInfo = getPackageInfo(subscription.subscriptionPackage);
              return (
                <View key={subscription.id} style={styles.subscriptionCard}>
                  <View style={styles.cardHeader}>
                    <View style={styles.packageInfo}>
                      <View
                        style={[
                          styles.packageBadge,
                          { backgroundColor: packageInfo.color }
                        ]}
                      >
                        <Text style={styles.packageBadgeText}>{packageInfo.badge}</Text>
                      </View>
                      <View>
                        <Text style={styles.packageName}>{packageInfo.name} Package</Text>
                        <Text style={styles.farmerName}>Farmer ID: {subscription.userId}</Text>
                      </View>
                    </View>
                    <View style={styles.statusContainer}>
                      <Ionicons
                        name={getStatusIcon(subscription.status)}
                        size={16}
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

                  <View style={styles.cardContent}>
                    <View style={styles.detailRow}>
                      <Text style={styles.detailLabel}>Amount:</Text>
                      <Text style={styles.detailValue}>{subscription.amount} TK</Text>
                    </View>
                    <View style={styles.detailRow}>
                      <Text style={styles.detailLabel}>Tokens:</Text>
                      <Text style={styles.detailValue}>{subscription.tokens}</Text>
                    </View>
                    <View style={styles.detailRow}>
                      <Text style={styles.detailLabel}>Transaction ID:</Text>
                      <Text style={styles.transactionId}>{subscription.transactionId}</Text>
                    </View>
                    <View style={styles.detailRow}>
                      <Text style={styles.detailLabel}>Submitted:</Text>
                      <Text style={styles.detailValue}>{formatDate(subscription.createdAt)}</Text>
                    </View>
                  </View>

                  {subscription.status === 'pending' && (
                    <View style={styles.actionButtons}>
                      <TouchableOpacity
                        style={[styles.actionButton, styles.approveButton]}
                        onPress={() => openStatusModal(subscription)}
                        disabled={processingId === subscription.id}
                      >
                        {processingId === subscription.id ? (
                          <ActivityIndicator size="small" color="#fff" />
                        ) : (
                          <>
                            <Ionicons name="checkmark" size={16} color="#fff" />
                            <Text style={styles.actionButtonText}>Review</Text>
                          </>
                        )}
                      </TouchableOpacity>
                    </View>
                  )}
                </View>
              );
            })
          )}
        </View>
      </ScrollView>

      {/* Status Update Modal */}
      <Modal
        animationType="slide"
        transparent={true}
        visible={modalVisible}
        onRequestClose={() => setModalVisible(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>Update Subscription Status</Text>
              <TouchableOpacity
                style={styles.modalCloseButton}
                onPress={() => setModalVisible(false)}
              >
                <Ionicons name="close" size={24} color="#333" />
              </TouchableOpacity>
            </View>

            {selectedSubscription && (
              <View style={styles.modalBody}>
                <Text style={styles.modalSubtitle}>
                  {getPackageInfo(selectedSubscription.subscriptionPackage).name} Package - {selectedSubscription.amount} TK
                </Text>
                
                <View style={styles.notesInput}>
                  <Text style={styles.inputLabel}>Admin Notes (Optional):</Text>
                  <TextInput
                    style={styles.textArea}
                    placeholder="Add notes for the farmer..."
                    placeholderTextColor="#999"
                    value={adminNotes}
                    onChangeText={setAdminNotes}
                    multiline
                    numberOfLines={3}
                    textAlignVertical="top"
                  />
                </View>

                <View style={styles.modalActions}>
                  <TouchableOpacity
                    style={[styles.modalActionButton, styles.approveModalButton]}
                    onPress={() => handleStatusUpdate(selectedSubscription.id, 'approved')}
                    disabled={processingId === selectedSubscription.id}
                  >
                    <Ionicons name="checkmark-circle" size={20} color="#fff" />
                    <Text style={styles.modalActionButtonText}>Approve</Text>
                  </TouchableOpacity>

                  <TouchableOpacity
                    style={[styles.modalActionButton, styles.rejectModalButton]}
                    onPress={() => handleStatusUpdate(selectedSubscription.id, 'rejected')}
                    disabled={processingId === selectedSubscription.id}
                  >
                    <Ionicons name="close-circle" size={20} color="#fff" />
                    <Text style={styles.modalActionButtonText}>Reject</Text>
                  </TouchableOpacity>
                </View>
              </View>
            )}
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  gradientBackground: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#ffffff',
  },
  loadingText: {
    marginTop: 16,
    fontSize: 16,
    color: '#2E7D32',
    fontWeight: '600',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingTop: 20,
    paddingBottom: 16,
    paddingHorizontal: 20,
  },
  headerLeft: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  headerTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#2E7D32',
    marginLeft: 12,
  },
  logoutButton: {
    padding: 8,
  },
  adminInfoContainer: {
    paddingHorizontal: 20,
    paddingBottom: 16,
  },
  adminWelcome: {
    fontSize: 16,
    color: '#2E7D32',
    fontWeight: '600',
  },
  adminLastLogin: {
    fontSize: 12,
    color: '#4CAF50',
    marginTop: 4,
  },
  content: {
    flex: 1,
    backgroundColor: '#ffffff',
    borderTopLeftRadius: 25,
    borderTopRightRadius: 25,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: -2 },
    shadowOpacity: 0.1,
    shadowRadius: 8,
    elevation: 8,
  },
  contentContainer: {
    padding: 20,
    paddingBottom: 40,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#2E7D32',
    marginBottom: 16,
  },
  statsContainer: {
    marginBottom: 30,
  },
  statsGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
  },
  statCard: {
    backgroundColor: '#ffffff',
    borderRadius: 12,
    padding: 16,
    width: '48%',
    marginBottom: 12,
    borderLeftWidth: 4,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
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
    fontWeight: '600',
  },
  subscriptionsSection: {
    flex: 1,
  },
  filterContainer: {
    marginBottom: 20,
  },
  filterButtons: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginTop: 12,
  },
  filterButton: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
    backgroundColor: '#F5F5F5',
    borderWidth: 1,
    borderColor: '#E0E0E0',
    minWidth: 80,
    alignItems: 'center',
  },
  activeFilterButton: {
    backgroundColor: '#2E7D32',
    borderColor: '#2E7D32',
  },
  filterButtonText: {
    fontSize: 12,
    fontWeight: '600',
    color: '#666',
  },
  activeFilterButtonText: {
    color: '#ffffff',
  },
  emptyContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 40,
  },
  emptyText: {
    fontSize: 16,
    color: '#81C784',
    marginTop: 12,
  },
  subscriptionCard: {
    backgroundColor: '#ffffff',
    borderRadius: 12,
    marginBottom: 16,
    overflow: 'hidden',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
    borderWidth: 1,
    borderColor: '#E8F5E8',
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 16,
    backgroundColor: '#F8F9FA',
  },
  packageInfo: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  packageBadge: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 12,
    marginRight: 12,
  },
  packageBadgeText: {
    color: '#fff',
    fontSize: 12,
    fontWeight: 'bold',
  },
  packageName: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333',
  },
  farmerName: {
    fontSize: 12,
    color: '#666',
    marginTop: 2,
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
  detailRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 8,
  },
  detailLabel: {
    fontSize: 14,
    color: '#666',
  },
  detailValue: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333',
  },
  transactionId: {
    fontSize: 14,
    fontWeight: '600',
    color: '#2196F3',
    fontFamily: 'monospace',
  },
  actionButtons: {
    flexDirection: 'row',
    padding: 12,
    backgroundColor: '#F8F9FA',
  },
  actionButton: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 12,
    borderRadius: 8,
  },
  approveButton: {
    backgroundColor: '#4CAF50',
  },
  actionButtonText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: 'bold',
    marginLeft: 6,
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  modalContent: {
    backgroundColor: '#fff',
    borderRadius: 16,
    width: '90%',
    maxHeight: '80%',
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 20,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
  },
  modalCloseButton: {
    padding: 4,
  },
  modalBody: {
    padding: 20,
  },
  modalSubtitle: {
    fontSize: 16,
    color: '#666',
    marginBottom: 20,
    textAlign: 'center',
  },
  notesInput: {
    marginBottom: 24,
  },
  inputLabel: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333',
    marginBottom: 8,
  },
  textArea: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    fontSize: 14,
    color: '#333',
    minHeight: 80,
  },
  modalActions: {
    flexDirection: 'row',
    gap: 12,
  },
  modalActionButton: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 14,
    borderRadius: 8,
  },
  approveModalButton: {
    backgroundColor: '#4CAF50',
  },
  rejectModalButton: {
    backgroundColor: '#F44336',
  },
  modalActionButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
    marginLeft: 8,
  },
});