import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  Alert,
  RefreshControl,
  FlatList,
  Modal,
  Dimensions,
  ActivityIndicator
} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { FontAwesome5, MaterialIcons } from '@expo/vector-icons';
import { useTranslation } from 'react-i18next';
import { router } from 'expo-router';
import { API_BASE_URL } from '../utils/apiConfig';

const { width, height } = Dimensions.get('window');

const FarmerOrderManagement = () => {
  const { t } = useTranslation();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [detailsModalVisible, setDetailsModalVisible] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [farmerId, setFarmerId] = useState(null);
  const [farmerName, setFarmerName] = useState('');

  useEffect(() => {
    initializeComponent();
  }, []);

  const initializeComponent = async () => {
    try {
      const storedUserData = await AsyncStorage.getItem('userData');
      
      if (storedUserData) {
        const farmerData = JSON.parse(storedUserData);
        console.log('FarmerOrderManagement: Loaded farmer data:', farmerData);
        console.log('FarmerOrderManagement: Farmer ID (id):', farmerData.id);
        console.log('FarmerOrderManagement: Farmer ID (_id):', farmerData._id);
        
        const farmerId = farmerData.id || farmerData._id;
        setFarmerId(farmerId);
        setFarmerName(farmerData.name || 'Farmer');
        await fetchOrders(farmerId);
      } else {
        Alert.alert(t('error'), t('pleaseLoginFirst'));
        router.replace('/farmerAuth');
      }
    } catch (error) {
      console.error('Error initializing component:', error);
      setLoading(false);
    }
  };

  const fetchOrders = async (id = farmerId) => {
    if (!id) return;
    
    try {
      setLoading(true);
      console.log('FarmerOrderManagement: Fetching orders for farmer ID:', id);
      const token = await AsyncStorage.getItem('authToken'); // Changed from 'token' to 'authToken'
      console.log('FarmerOrderManagement: Using token:', token ? 'Token found' : 'No token');
      
      console.log('FarmerOrderManagement: Making API call to:', `${API_BASE_URL}/api/orders/farmer/${id}`);
      const response = await fetch(`${API_BASE_URL}/api/orders/farmer/${id}`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      console.log('FarmerOrderManagement: Response status:', response.status);
      if (response.ok) {
        const data = await response.json();
        console.log('FarmerOrderManagement: API response:', data);
        
        // Handle both direct array response and response with orders property
        const ordersArray = data.orders || data;
        console.log('FarmerOrderManagement: Orders loaded:', ordersArray?.length || 0, 'orders');
        if (ordersArray && ordersArray.length > 0) {
          console.log('FarmerOrderManagement: First order structure:', ordersArray[0]);
          console.log('FarmerOrderManagement: Order ID properties - id:', ordersArray[0].id, '_id:', ordersArray[0]._id, 'orderId:', ordersArray[0].orderId);
          console.log('FarmerOrderManagement: Products in first order:', ordersArray[0].products);
          if (ordersArray[0].products) {
            console.log('FarmerOrderManagement: Product keys:', Object.keys(ordersArray[0].products));
          }
        }
        
        // Sort orders by creation date (newest first)
        const sortedOrders = ordersArray.sort((a, b) => new Date(b.createdAt || b.orderDate) - new Date(a.createdAt || a.orderDate));
        setOrders(sortedOrders);
      } else if (response.status === 404) {
        console.log('FarmerOrderManagement: No orders found (404)');
        setOrders([]);
      } else {
        console.log('FarmerOrderManagement: Error response:', response.status);
        throw new Error('Failed to fetch orders');
      }
    } catch (error) {
      console.error('Error fetching orders:', error);
      Alert.alert(t('error'), t('failedToLoadOrders'));
    } finally {
      setLoading(false);
    }
  };

  const onRefresh = async () => {
    setRefreshing(true);
    await fetchOrders();
    setRefreshing(false);
  };

  const updateOrderStatus = async (orderId, newStatus) => {
    try {
      setActionLoading(true);
      const token = await AsyncStorage.getItem('authToken'); // Changed from 'token' to 'authToken'
      
      let endpoint;
      let requestBody = {};
      
      if (newStatus === 'CONFIRMED') {
        endpoint = `${API_BASE_URL}/api/orders/${orderId}/confirm`;
        requestBody.farmerNotes = ''; // Empty notes, as it's optional
      } else if (newStatus === 'DELIVERED') {
        endpoint = `${API_BASE_URL}/api/orders/${orderId}/deliver`;
        requestBody.farmerNotes = ''; // Empty notes, as it's optional
      } else {
        throw new Error('Invalid status update');
      }

      console.log('FarmerOrderManagement: Making API call to:', endpoint, 'with body:', requestBody);
      const response = await fetch(endpoint, {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestBody)
      });

      console.log('FarmerOrderManagement: Update response status:', response.status);
      if (response.ok) {
        const result = await response.json();
        console.log('FarmerOrderManagement: Update response:', result);
        
        // Get the updated order from the response
        const updatedOrder = result.order || result;
        
        // Update the orders list
        setOrders(prevOrders => 
          prevOrders.map(order => {
            const orderIdToCheck = order.id || order._id || order.orderId;
            return orderIdToCheck === orderId ? updatedOrder : order;
          })
        );

        // Update selected order if it's the current one
        if (selectedOrder) {
          const selectedOrderId = selectedOrder.id || selectedOrder._id || selectedOrder.orderId;
          if (selectedOrderId === orderId) {
            setSelectedOrder(updatedOrder);
          }
        }

        const statusMessage = newStatus === 'CONFIRMED' ? 
          t('orderConfirmedSuccessfully') : t('orderMarkedAsDelivered');
        Alert.alert(t('success'), statusMessage);
      } else {
        const errorResponse = await response.text();
        console.log('FarmerOrderManagement: Error response:', errorResponse);
        throw new Error('Failed to update order status: ' + errorResponse);
      }
    } catch (error) {
      console.error('Error updating order status:', error);
      Alert.alert(t('error'), t('failedToUpdateOrder'));
    } finally {
      setActionLoading(false);
    }
  };



  const getStatusColor = (status) => {
    switch (status) {
      case 'PENDING': return '#FF9800';
      case 'CONFIRMED': return '#2196F3';
      case 'DELIVERED': return '#4CAF50';
      case 'RECEIVED': return '#8BC34A';
      default: return '#757575';
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'PENDING': return 'clock';
      case 'CONFIRMED': return 'check';
      case 'DELIVERED': return 'truck';
      case 'RECEIVED': return 'check-circle';
      default: return 'help';
    }
  };

  const formatProductList = (products) => {
    if (!products || Object.keys(products).length === 0) return t('noProducts');
    
    return Object.entries(products).map(([product, quantity]) => 
      `${t(product)}: ${quantity}`
    ).join(', ');
  };

  const calculateOrderTotal = (products) => {
    if (!products) return 0;
    
    return Object.entries(products).reduce((total, [product, quantity]) => {
      const price = getProductPrice(product);
      console.log(`Product: ${product}, Quantity: ${quantity}, Price: ${price}, Subtotal: ${price * quantity}`);
      return total + price * quantity;
    }, 0);
  };

  const getProductPrice = (productKey) => {
    // Centralized price mapping to handle different product key formats
    const prices = {
      'MILK_COW': 80,
      'milkCow': 80,
      'MILK_BUFFALO': 90,
      'milkBuffalo': 90,
      'MILK_GOAT': 120,
      'milkGoat': 120,
      'BUTTER': 150,
      'butter': 150,
      'EGGS_HEN': 12,
      'henEggs': 12,
      'EGGS_DUCK': 15,
      'duckEggs': 15
    };
    
    return prices[productKey] || 0;
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const renderOrderCard = ({ item }) => (
    <TouchableOpacity 
      style={styles.orderCard}
      onPress={() => {
        setSelectedOrder(item);
        setDetailsModalVisible(true);
      }}
    >
      <View style={styles.orderHeader}>
        <View style={styles.orderInfo}>
          <Text style={styles.orderId}>#{(item.id || item._id || item.orderId || 'N/A').slice(-8)}</Text>
          <Text style={styles.customerName}>{item.customerName}</Text>
        </View>
        <View style={[styles.statusBadge, { backgroundColor: getStatusColor(item.status) }]}>
          <FontAwesome5 
            name={getStatusIcon(item.status)} 
            size={12} 
            color="white" 
            style={styles.statusIcon}
          />
          <Text style={styles.statusText}>{t(item.status.toLowerCase())}</Text>
        </View>
      </View>
      
      <Text style={styles.productList} numberOfLines={2}>
        {formatProductList(item.products)}
      </Text>
      
      <View style={styles.orderFooter}>
        <Text style={styles.orderTotal}>
          {t('total')}: ৳{calculateOrderTotal(item.products)}
        </Text>
        <Text style={styles.orderDate}>
          {formatDate(item.createdAt)}
        </Text>
      </View>
      
      {item.status === 'PENDING' && (
        <TouchableOpacity 
          style={styles.quickActionButton}
          onPress={(e) => {
            e.stopPropagation();
            updateOrderStatus(item.id || item._id || item.orderId, 'CONFIRMED');
          }}
          disabled={actionLoading}
        >
          <FontAwesome5 name="check" size={14} color="white" />
          <Text style={styles.quickActionText}>{t('confirm')}</Text>
        </TouchableOpacity>
      )}
    </TouchableOpacity>
  );

  const renderOrderDetails = () => (
    <Modal
      visible={detailsModalVisible}
      animationType="slide"
      transparent={false}
      onRequestClose={() => setDetailsModalVisible(false)}
    >
      <View style={styles.modalContainer}>
        <View style={styles.modalHeader}>
          <Text style={styles.modalTitle}>{t('orderDetails')}</Text>
          <TouchableOpacity 
            onPress={() => setDetailsModalVisible(false)}
            style={styles.closeButton}
          >
            <MaterialIcons name="close" size={24} color="#333" />
          </TouchableOpacity>
        </View>
        
        <ScrollView style={styles.modalContent}>
          {selectedOrder && (
            <>
              <View style={styles.detailSection}>
                <Text style={styles.detailLabel}>{t('orderNumber')}</Text>
                <Text style={styles.detailValue}>#{(selectedOrder.id || selectedOrder._id || selectedOrder.orderId || 'N/A').slice(-8)}</Text>
              </View>
              
              <View style={styles.detailSection}>
                <Text style={styles.detailLabel}>{t('customer')}</Text>
                <Text style={styles.detailValue}>{selectedOrder.customerName}</Text>
              </View>
              
              <View style={styles.detailSection}>
                <Text style={styles.detailLabel}>{t('status')}</Text>
                <View style={[styles.statusBadge, { backgroundColor: getStatusColor(selectedOrder.status) }]}>
                  <FontAwesome5 
                    name={getStatusIcon(selectedOrder.status)} 
                    size={12} 
                    color="white" 
                    style={styles.statusIcon}
                  />
                  <Text style={styles.statusText}>{t(selectedOrder.status.toLowerCase())}</Text>
                </View>
              </View>
              
              <View style={styles.detailSection}>
                <Text style={styles.detailLabel}>{t('orderedProducts')}</Text>
                <View style={styles.productDetails}>
                  {Object.entries(selectedOrder.products || {}).map(([product, quantity]) => (
                    <View key={product} style={styles.productRow}>
                      <Text style={styles.productName}>{t(product)}</Text>
                      <Text style={styles.productQuantity}>x{quantity}</Text>
                      <Text style={styles.productPrice}>
                        ৳{quantity * getProductPrice(product)}
                      </Text>
                    </View>
                  ))}
                </View>
              </View>
              
              <View style={styles.detailSection}>
                <Text style={styles.detailLabel}>{t('totalAmount')}</Text>
                <Text style={styles.totalAmount}>৳{calculateOrderTotal(selectedOrder.products)}</Text>
              </View>
              
              <View style={styles.detailSection}>
                <Text style={styles.detailLabel}>{t('orderDate')}</Text>
                <Text style={styles.detailValue}>{formatDate(selectedOrder.createdAt)}</Text>
              </View>
              
              {selectedOrder.confirmedAt && (
                <View style={styles.detailSection}>
                  <Text style={styles.detailLabel}>{t('confirmedAt')}</Text>
                  <Text style={styles.detailValue}>{formatDate(selectedOrder.confirmedAt)}</Text>
                </View>
              )}
              
              {selectedOrder.deliveredAt && (
                <View style={styles.detailSection}>
                  <Text style={styles.detailLabel}>{t('deliveredAt')}</Text>
                  <Text style={styles.detailValue}>{formatDate(selectedOrder.deliveredAt)}</Text>
                </View>
              )}
              
              {selectedOrder.receivedAt && (
                <View style={styles.detailSection}>
                  <Text style={styles.detailLabel}>{t('receivedAt')}</Text>
                  <Text style={styles.detailValue}>{formatDate(selectedOrder.receivedAt)}</Text>
                </View>
              )}
            </>
          )}
        </ScrollView>
        
        <View style={styles.modalActions}>
          {selectedOrder && selectedOrder.status === 'PENDING' && (
            <TouchableOpacity 
              style={[styles.actionButton, styles.confirmButton]}
              onPress={() => {
                setDetailsModalVisible(false);
                setTimeout(() => {
                  updateOrderStatus(selectedOrder.id || selectedOrder._id || selectedOrder.orderId, 'CONFIRMED');
                }, 300);
              }}
              disabled={actionLoading}
            >
              {actionLoading ? (
                <ActivityIndicator size="small" color="white" />
              ) : (
                <>
                  <FontAwesome5 name="check" size={16} color="white" />
                  <Text style={styles.actionButtonText}>{t('confirmOrder')}</Text>
                </>
              )}
            </TouchableOpacity>
          )}
          
          {selectedOrder && selectedOrder.status === 'CONFIRMED' && (
            <TouchableOpacity 
              style={[styles.actionButton, styles.deliverButton]}
              onPress={() => {
                setDetailsModalVisible(false);
                setTimeout(() => {
                  updateOrderStatus(selectedOrder.id || selectedOrder._id || selectedOrder.orderId, 'DELIVERED');
                }, 300);
              }}
              disabled={actionLoading}
            >
              {actionLoading ? (
                <ActivityIndicator size="small" color="white" />
              ) : (
                <>
                  <FontAwesome5 name="truck" size={16} color="white" />
                  <Text style={styles.actionButtonText}>{t('markAsDelivered')}</Text>
                </>
              )}
            </TouchableOpacity>
          )}
        </View>
      </View>
    </Modal>
  );

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#2E7D32" />
        <Text style={styles.loadingText}>{t('loadingOrders')}</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity 
          onPress={() => router.back()}
          style={styles.backButton}
        >
          <FontAwesome5 name="arrow-left" size={20} color="#333" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>{t('myOrders')}</Text>
      </View>
      
      <View style={styles.statsContainer}>
        <View style={styles.statCard}>
          <Text style={styles.statNumber}>{orders.filter(o => o.status === 'PENDING').length}</Text>
          <Text style={styles.statLabel}>{t('pending')}</Text>
        </View>
        <View style={styles.statCard}>
          <Text style={styles.statNumber}>{orders.filter(o => o.status === 'CONFIRMED').length}</Text>
          <Text style={styles.statLabel}>{t('confirmed')}</Text>
        </View>
        <View style={styles.statCard}>
          <Text style={styles.statNumber}>{orders.filter(o => o.status === 'DELIVERED').length}</Text>
          <Text style={styles.statLabel}>{t('delivered')}</Text>
        </View>
        <View style={styles.statCard}>
          <Text style={styles.statNumber}>{orders.filter(o => o.status === 'RECEIVED').length}</Text>
          <Text style={styles.statLabel}>{t('received')}</Text>
        </View>
      </View>

      {orders.length === 0 ? (
        <View style={styles.emptyContainer}>
          <FontAwesome5 name="shopping-cart" size={64} color="#ccc" />
          <Text style={styles.emptyTitle}>{t('noOrdersYet')}</Text>
          <Text style={styles.emptySubtitle}>{t('noOrdersMessage')}</Text>
        </View>
      ) : (
        <FlatList
          data={orders}
          renderItem={renderOrderCard}
          keyExtractor={(item) => item.id || item._id || item.orderId}
          style={styles.ordersList}
          refreshControl={
            <RefreshControl
              refreshing={refreshing}
              onRefresh={onRefresh}
              colors={['#2E7D32']}
            />
          }
          showsVerticalScrollIndicator={false}
        />
      )}
      
      {renderOrderDetails()}
    </View>
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
    backgroundColor: '#f5f5f5',
  },
  loadingText: {
    marginTop: 10,
    fontSize: 16,
    color: '#666',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingVertical: 15,
    backgroundColor: 'white',
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  backButton: {
    marginRight: 15,
  },
  headerTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333',
  },
  statsContainer: {
    flexDirection: 'row',
    paddingHorizontal: 20,
    paddingVertical: 15,
    backgroundColor: 'white',
    marginBottom: 10,
  },
  statCard: {
    flex: 1,
    alignItems: 'center',
    paddingVertical: 10,
  },
  statNumber: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#2E7D32',
  },
  statLabel: {
    fontSize: 12,
    color: '#666',
    marginTop: 4,
  },
  ordersList: {
    flex: 1,
    paddingHorizontal: 20,
  },
  orderCard: {
    backgroundColor: 'white',
    borderRadius: 10,
    padding: 15,
    marginBottom: 10,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
  },
  orderHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 10,
  },
  orderInfo: {
    flex: 1,
  },
  orderId: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333',
  },
  customerName: {
    fontSize: 14,
    color: '#666',
    marginTop: 2,
  },
  statusBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 12,
  },
  statusIcon: {
    marginRight: 4,
  },
  statusText: {
    color: 'white',
    fontSize: 12,
    fontWeight: 'bold',
  },
  productList: {
    fontSize: 14,
    color: '#555',
    marginBottom: 10,
  },
  orderFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  orderTotal: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#2E7D32',
  },
  orderDate: {
    fontSize: 12,
    color: '#666',
  },
  quickActionButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#2196F3',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 15,
    marginTop: 10,
    alignSelf: 'flex-start',
  },
  quickActionText: {
    color: 'white',
    fontSize: 12,
    fontWeight: 'bold',
    marginLeft: 4,
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 40,
  },
  emptyTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#666',
    marginTop: 20,
    textAlign: 'center',
  },
  emptySubtitle: {
    fontSize: 14,
    color: '#999',
    marginTop: 10,
    textAlign: 'center',
    lineHeight: 20,
  },
  modalContainer: {
    flex: 1,
    backgroundColor: 'white',
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingVertical: 15,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  modalTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333',
  },
  closeButton: {
    padding: 5,
  },
  modalContent: {
    flex: 1,
    paddingHorizontal: 20,
    paddingVertical: 15,
  },
  detailSection: {
    marginBottom: 20,
  },
  detailLabel: {
    fontSize: 14,
    color: '#666',
    marginBottom: 5,
  },
  detailValue: {
    fontSize: 16,
    color: '#333',
    fontWeight: '500',
  },
  productDetails: {
    marginTop: 5,
  },
  productRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
  },
  productName: {
    flex: 2,
    fontSize: 14,
    color: '#333',
  },
  productQuantity: {
    flex: 1,
    fontSize: 14,
    color: '#666',
    textAlign: 'center',
  },
  productPrice: {
    flex: 1,
    fontSize: 14,
    color: '#2E7D32',
    fontWeight: 'bold',
    textAlign: 'right',
  },
  totalAmount: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#2E7D32',
  },
  modalActions: {
    paddingHorizontal: 20,
    paddingVertical: 15,
    borderTopWidth: 1,
    borderTopColor: '#eee',
  },
  actionButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 15,
    borderRadius: 10,
    marginBottom: 10,
  },
  confirmButton: {
    backgroundColor: '#2196F3',
  },
  deliverButton: {
    backgroundColor: '#4CAF50',
  },
  actionButtonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: 'bold',
    marginLeft: 8,
  },
});

export default FarmerOrderManagement;