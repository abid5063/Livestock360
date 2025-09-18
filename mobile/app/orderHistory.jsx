import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  StyleSheet,
  SafeAreaView,
  Alert,
  ActivityIndicator,
  RefreshControl
} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { router } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { API_BASE_URL, ALL_ALERTS } from '../utils/apiConfig';

const OrderHistory = () => {
  const [loading, setLoading] = useState(true);
  const [orders, setOrders] = useState([]);
  const [customerData, setCustomerData] = useState(null);
  const [refreshing, setRefreshing] = useState(false);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const storedData = await AsyncStorage.getItem('customerData');
      if (storedData) {
        const parsedData = JSON.parse(storedData);
        console.log('OrderHistory: Loaded customer data:', parsedData);
        console.log('OrderHistory: Customer ID (id):', parsedData.id);
        console.log('OrderHistory: Customer ID (_id):', parsedData._id);
        setCustomerData(parsedData);
        await fetchOrders(parsedData.id || parsedData._id);
      } else {
        router.replace('/');
      }
    } catch (error) {
      console.error('Error loading data:', error);
      if (ALL_ALERTS) {
        Alert.alert('Error', 'Failed to load order history');
      }
    } finally {
      setLoading(false);
    }
  };

  const fetchOrders = async (customerId) => {
    try {
      console.log('OrderHistory: Fetching orders for customer ID:', customerId);
      const token = await AsyncStorage.getItem('customerToken');
      if (!token) {
        console.log('OrderHistory: No token found, redirecting to login');
        router.replace('/');
        return;
      }

      console.log('OrderHistory: Making API call to:', `${API_BASE_URL}/api/orders/customer/${customerId}`);
      const response = await fetch(`${API_BASE_URL}/api/orders/customer/${customerId}`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (response.status === 401) {
        await AsyncStorage.multiRemove(['customerToken', 'customerData']);
        router.replace('/');
        return;
      }

      const data = await response.json();
      console.log('OrderHistory: API response:', data);
      if (data.success) {
        console.log('OrderHistory: Orders loaded successfully:', data.orders?.length || 0, 'orders');
        setOrders(data.orders || []);
      } else {
        console.log('OrderHistory: API error:', data.message);
        if (ALL_ALERTS) {
          Alert.alert('Error', data.message || 'Failed to load orders');
        }
      }
    } catch (error) {
      console.error('Error fetching orders:', error);
      if (ALL_ALERTS) {
        Alert.alert('Error', 'Failed to connect to server');
      }
    }
  };

  const onRefresh = async () => {
    setRefreshing(true);
    if (customerData) {
      await fetchOrders(customerData.id || customerData._id);
    }
    setRefreshing(false);
  };

  const markOrderReceived = async (orderId) => {
    try {
      const token = await AsyncStorage.getItem('customerToken');
      const response = await fetch(`${API_BASE_URL}/api/orders/${orderId}/receive`, {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      const result = await response.json();
      if (result.success) {
        // Show success alert only if ALL_ALERTS is enabled
        if (ALL_ALERTS) {
          Alert.alert('Success', 'Order marked as received!');
        } else {
          // Silent success - no alert shown
          console.log('Order marked as received successfully (alert disabled)');
        }
        
        // Refresh orders
        if (customerData) {
          await fetchOrders(customerData.id || customerData._id);
        }
      } else {
        // Show error alert only if ALL_ALERTS is enabled
        if (ALL_ALERTS) {
          Alert.alert('Error', result.message || 'Failed to update order');
        } else {
          // Silent error - no alert shown
          console.log('Failed to update order:', result.message);
        }
      }
    } catch (error) {
      console.error('Error updating order:', error);
      if (ALL_ALERTS) {
        Alert.alert('Error', 'Failed to update order status');
      }
    }
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'PENDING': return '#FFA500';
      case 'CONFIRMED': return '#2196F3';
      case 'DELIVERED': return '#4CAF50';
      case 'RECEIVED': return '#8BC34A';
      case 'CANCELLED': return '#F44336';
      default: return '#999';
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'PENDING': return 'time';
      case 'CONFIRMED': return 'checkmark-circle';
      case 'DELIVERED': return 'car';
      case 'RECEIVED': return 'thumbs-up';
      case 'CANCELLED': return 'close-circle';
      default: return 'help';
    }
  };

  const messageFarmer = async (order) => {
    try {
      const token = await AsyncStorage.getItem('customerToken');
      if (!token) {
        Alert.alert('Error', 'Please login again');
        return;
      }

      // Navigate to customer messaging with farmer info
      router.push({
        pathname: '/customerMessaging',
        params: { 
          farmerId: order.farmerId,
          farmerName: order.farmerName || 'Farmer',
          orderContext: `Regarding Order #${(order.id || order._id || 'N/A').slice(-8)}`
        }
      });
    } catch (error) {
      console.error('Error starting message:', error);
      Alert.alert('Error', 'Failed to start messaging. Please try again.');
    }
  };

  const formatDate = (dateString) => {
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch {
      return 'N/A';
    }
  };

  if (loading) {
    return (
      <SafeAreaView style={styles.container}>
        <View style={styles.header}>
          <TouchableOpacity onPress={() => router.back()} style={styles.backButton}>
            <Ionicons name="arrow-back" size={24} color="#fff" />
          </TouchableOpacity>
          <Text style={styles.headerTitle}>Order History</Text>
          <View style={styles.placeholder} />
        </View>
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color="#2E7D32" />
          <Text style={styles.loadingText}>Loading orders...</Text>
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backButton}>
          <Ionicons name="arrow-back" size={24} color="#fff" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Order History</Text>
        <TouchableOpacity onPress={onRefresh} style={styles.refreshButton}>
          <Ionicons name="refresh" size={20} color="#fff" />
        </TouchableOpacity>
      </View>

      <ScrollView 
        style={styles.content}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
        }
      >
        {orders.length === 0 ? (
          <View style={styles.emptyState}>
            <Ionicons name="receipt-outline" size={64} color="#ccc" />
            <Text style={styles.emptyTitle}>No Orders Yet</Text>
            <Text style={styles.emptySubtitle}>
              Start shopping for fresh products from local farmers!
            </Text>
            <TouchableOpacity 
              style={styles.shopButton} 
              onPress={() => router.push('/buyProducts')}
            >
              <Ionicons name="basket" size={16} color="#fff" />
              <Text style={styles.shopButtonText}>Start Shopping</Text>
            </TouchableOpacity>
          </View>
        ) : (
          <>
            <Text style={styles.sectionTitle}>Your Orders ({orders.length})</Text>
            {orders.map((order) => (
              <View key={order.id} style={styles.orderCard}>
                <View style={styles.orderHeader}>
                  <View style={styles.orderInfo}>
                    <Text style={styles.orderNumber}>Order #{order.id.slice(-8)}</Text>
                    <Text style={styles.orderDate}>{formatDate(order.orderDate)}</Text>
                  </View>
                  <View style={[styles.statusBadge, { backgroundColor: getStatusColor(order.status) }]}>
                    <Ionicons name={getStatusIcon(order.status)} size={14} color="#fff" />
                    <Text style={styles.statusText}>{order.status}</Text>
                  </View>
                </View>

                <View style={styles.farmerInfo}>
                  <View style={styles.farmerDetails}>
                    <Text style={styles.farmerName}>
                      <Ionicons name="person" size={14} color="#666" /> {order.farmerName}
                    </Text>
                    <TouchableOpacity 
                      style={styles.messageButton}
                      onPress={() => messageFarmer(order)}
                    >
                      <Ionicons name="chatbubble-outline" size={16} color="#2E7D32" />
                      <Text style={styles.messageButtonText}>Message</Text>
                    </TouchableOpacity>
                  </View>
                  <Text style={styles.farmerLocation}>
                    <Ionicons name="location" size={14} color="#666" /> {order.farmerLocation}
                  </Text>
                  {order.farmerPhone && (
                    <Text style={styles.farmerPhone}>
                      <Ionicons name="call" size={14} color="#666" /> {order.farmerPhone}
                    </Text>
                  )}
                </View>

                <View style={styles.productsSection}>
                  <Text style={styles.productsTitle}>Products:</Text>
                  <View style={styles.productsList}>
                    {order.productDetails?.map((product, index) => (
                      <Text key={index} style={styles.productItem}>
                        • {product.name} x {product.quantity}
                      </Text>
                    ))}
                  </View>
                </View>

                <View style={styles.orderFooter}>
                  <View style={styles.totalSection}>
                    <Text style={styles.totalLabel}>Total Items: {order.totalItems}</Text>
                    <Text style={styles.totalAmount}>৳{order.totalAmount}</Text>
                  </View>
                  
                  {order.status === 'DELIVERED' && (
                    <TouchableOpacity
                      style={styles.receiveButton}
                      onPress={() => {
                        Alert.alert(
                          'Confirm Receipt',
                          'Have you received your order in good condition?',
                          [
                            { text: 'Cancel', style: 'cancel' },
                            { 
                              text: 'Yes, Received', 
                              onPress: () => markOrderReceived(order.id) 
                            }
                          ]
                        );
                      }}
                    >
                      <Ionicons name="thumbs-up" size={16} color="#fff" />
                      <Text style={styles.receiveButtonText}>Mark as Received</Text>
                    </TouchableOpacity>
                  )}
                </View>

                {order.deliveryAddress && (
                  <View style={styles.addressSection}>
                    <Text style={styles.addressLabel}>Delivery Address:</Text>
                    <Text style={styles.addressText}>{order.deliveryAddress}</Text>
                  </View>
                )}

                {order.customerNotes && (
                  <View style={styles.notesSection}>
                    <Text style={styles.notesLabel}>Your Notes:</Text>
                    <Text style={styles.notesText}>{order.customerNotes}</Text>
                  </View>
                )}

                {order.farmerNotes && (
                  <View style={styles.notesSection}>
                    <Text style={styles.notesLabel}>Notes:</Text>
                    <Text style={styles.notesText}>{order.farmerNotes}</Text>
                  </View>
                )}

                {/* Order Timeline */}
                <View style={styles.timeline}>
                  <Text style={styles.timelineTitle}>Order Timeline:</Text>
                  
                  <View style={styles.timelineItem}>
                    <View style={[styles.timelineDot, { backgroundColor: '#FFA500' }]} />
                    <View style={styles.timelineContent}>
                      <Text style={styles.timelineLabel}>Order Placed</Text>
                      <Text style={styles.timelineDate}>{formatDate(order.orderDate)}</Text>
                    </View>
                  </View>

                  {order.confirmedDate && (
                    <View style={styles.timelineItem}>
                      <View style={[styles.timelineDot, { backgroundColor: '#2196F3' }]} />
                      <View style={styles.timelineContent}>
                        <Text style={styles.timelineLabel}>Confirmed by Farmer</Text>
                        <Text style={styles.timelineDate}>{formatDate(order.confirmedDate)}</Text>
                      </View>
                    </View>
                  )}

                  {order.deliveredDate && (
                    <View style={styles.timelineItem}>
                      <View style={[styles.timelineDot, { backgroundColor: '#4CAF50' }]} />
                      <View style={styles.timelineContent}>
                        <Text style={styles.timelineLabel}>Delivered</Text>
                        <Text style={styles.timelineDate}>{formatDate(order.deliveredDate)}</Text>
                      </View>
                    </View>
                  )}

                  {order.receivedDate && (
                    <View style={styles.timelineItem}>
                      <View style={[styles.timelineDot, { backgroundColor: '#8BC34A' }]} />
                      <View style={styles.timelineContent}>
                        <Text style={styles.timelineLabel}>Received</Text>
                        <Text style={styles.timelineDate}>{formatDate(order.receivedDate)}</Text>
                      </View>
                    </View>
                  )}

                  {order.cancelledDate && (
                    <View style={styles.timelineItem}>
                      <View style={[styles.timelineDot, { backgroundColor: '#F44336' }]} />
                      <View style={styles.timelineContent}>
                        <Text style={styles.timelineLabel}>Cancelled</Text>
                        <Text style={styles.timelineDate}>{formatDate(order.cancelledDate)}</Text>
                      </View>
                    </View>
                  )}
                </View>
              </View>
            ))}
          </>
        )}
      </ScrollView>
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
    paddingHorizontal: 16,
    paddingVertical: 12,
    paddingTop: 40,
  },
  backButton: {
    padding: 8,
  },
  headerTitle: {
    color: '#fff',
    fontSize: 18,
    fontWeight: 'bold',
  },
  refreshButton: {
    padding: 8,
  },
  placeholder: {
    width: 40,
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
  content: {
    flex: 1,
    padding: 16,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 16,
  },
  emptyState: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 32,
    paddingVertical: 64,
  },
  emptyTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#666',
    marginTop: 16,
    marginBottom: 8,
  },
  emptySubtitle: {
    fontSize: 14,
    color: '#999',
    textAlign: 'center',
    lineHeight: 20,
    marginBottom: 24,
  },
  shopButton: {
    backgroundColor: '#FF6B35',
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingVertical: 12,
    borderRadius: 8,
    gap: 8,
  },
  shopButtonText: {
    color: '#fff',
    fontWeight: 'bold',
    fontSize: 16,
  },
  orderCard: {
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
  orderHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 12,
  },
  orderInfo: {
    flex: 1,
  },
  orderNumber: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333',
  },
  orderDate: {
    fontSize: 12,
    color: '#666',
    marginTop: 2,
  },
  statusBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 12,
    gap: 4,
  },
  statusText: {
    color: '#fff',
    fontSize: 11,
    fontWeight: 'bold',
  },
  farmerInfo: {
    backgroundColor: '#f8f9fa',
    padding: 12,
    borderRadius: 8,
    marginBottom: 12,
  },
  farmerDetails: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 4,
  },
  farmerName: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333',
    flex: 1,
  },
  messageButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#fff',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#2E7D32',
  },
  messageButtonText: {
    fontSize: 12,
    color: '#2E7D32',
    marginLeft: 4,
    fontWeight: '500',
  },
  farmerLocation: {
    fontSize: 12,
    color: '#666',
    marginBottom: 2,
  },
  farmerPhone: {
    fontSize: 12,
    color: '#666',
  },
  productsSection: {
    marginBottom: 12,
  },
  productsTitle: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333',
    marginBottom: 6,
  },
  productsList: {
    paddingLeft: 8,
  },
  productItem: {
    fontSize: 13,
    color: '#666',
    marginBottom: 2,
  },
  orderFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
    paddingTop: 12,
    borderTopWidth: 1,
    borderTopColor: '#f0f0f0',
  },
  totalSection: {
    flex: 1,
  },
  totalLabel: {
    fontSize: 12,
    color: '#666',
  },
  totalAmount: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#2E7D32',
  },
  receiveButton: {
    backgroundColor: '#4CAF50',
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 6,
    gap: 4,
  },
  receiveButtonText: {
    color: '#fff',
    fontSize: 12,
    fontWeight: 'bold',
  },
  addressSection: {
    backgroundColor: '#f8f9fa',
    padding: 10,
    borderRadius: 6,
    marginBottom: 8,
  },
  addressLabel: {
    fontSize: 12,
    fontWeight: '600',
    color: '#333',
    marginBottom: 4,
  },
  addressText: {
    fontSize: 13,
    color: '#666',
  },
  notesSection: {
    backgroundColor: '#f8f9fa',
    padding: 10,
    borderRadius: 6,
    marginBottom: 8,
  },
  notesLabel: {
    fontSize: 12,
    fontWeight: '600',
    color: '#333',
    marginBottom: 4,
  },
  notesText: {
    fontSize: 13,
    color: '#666',
    fontStyle: 'italic',
  },
  timeline: {
    borderTopWidth: 1,
    borderTopColor: '#f0f0f0',
    paddingTop: 12,
  },
  timelineTitle: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333',
    marginBottom: 8,
  },
  timelineItem: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 6,
  },
  timelineDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    marginRight: 12,
  },
  timelineContent: {
    flex: 1,
  },
  timelineLabel: {
    fontSize: 12,
    fontWeight: '500',
    color: '#333',
  },
  timelineDate: {
    fontSize: 11,
    color: '#999',
  },
});

export default OrderHistory;