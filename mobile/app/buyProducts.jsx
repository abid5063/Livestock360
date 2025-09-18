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
  TextInput,
  Modal,
  FlatList
} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { router } from 'expo-router';
import { Ionicons, MaterialIcons } from '@expo/vector-icons';
import { API_BASE_URL, ALL_ALERTS } from '../utils/apiConfig';

const BuyProducts = () => {
  const [loading, setLoading] = useState(true);
  const [sellers, setSellers] = useState([]);
  const [customerData, setCustomerData] = useState(null);
  const [showOrderModal, setShowOrderModal] = useState(false);
  const [selectedFarmer, setSelectedFarmer] = useState(null);
  const [orderItems, setOrderItems] = useState({});
  const [deliveryAddress, setDeliveryAddress] = useState('');
  const [customerNotes, setCustomerNotes] = useState('');
  const [submittingOrder, setSubmittingOrder] = useState(false);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      
      // Load customer data
      const storedData = await AsyncStorage.getItem('customerData');
      if (storedData) {
        const parsedData = JSON.parse(storedData);
        setCustomerData(parsedData);
        setDeliveryAddress(parsedData.address || parsedData.location || '');
      }

      // Fetch available sellers
      await fetchSellers();
    } catch (error) {
      console.error('Error loading data:', error);
      if (ALL_ALERTS) {
        Alert.alert('Error', 'Failed to load marketplace data');
      }
    } finally {
      setLoading(false);
    }
  };

  const fetchSellers = async () => {
    try {
      const token = await AsyncStorage.getItem('customerToken');
      if (!token) {
        router.replace('/');
        return;
      }

      const response = await fetch(`${API_BASE_URL}/api/products/available-sellers`, {
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
      if (data.success) {
        setSellers(data.sellers || []);
      } else {
        if (ALL_ALERTS) {
          Alert.alert('Error', data.message || 'Failed to load sellers');
        }
      }
    } catch (error) {
      console.error('Error fetching sellers:', error);
      if (ALL_ALERTS) {
        Alert.alert('Error', 'Failed to connect to server');
      }
    }
  };

  const openOrderModal = (farmer) => {
    setSelectedFarmer(farmer);
    setOrderItems({});
    setCustomerNotes('');
    setShowOrderModal(true);
  };

  const updateOrderQuantity = (productType, change) => {
    setOrderItems(prev => {
      const currentQty = prev[productType] || 0;
      const newQty = Math.max(0, currentQty + change);
      
      if (newQty === 0) {
        const { [productType]: removed, ...rest } = prev;
        return rest;
      }
      
      return {
        ...prev,
        [productType]: newQty
      };
    });
  };

  const calculateTotal = () => {
    let total = 0;
    Object.entries(orderItems).forEach(([product, quantity]) => {
      total += getProductPrice(product) * quantity;
    });
    return total;
  };

  const submitOrder = async () => {
    if (!selectedFarmer || Object.keys(orderItems).length === 0) {
      if (ALL_ALERTS) {
        Alert.alert('Invalid Order', 'Please select at least one product');
      }
      return;
    }

    if (!deliveryAddress.trim()) {
      if (ALL_ALERTS) {
        Alert.alert('Missing Information', 'Please provide a delivery address');
      }
      return;
    }

    if (!customerData || (!customerData.id && !customerData._id)) {
      if (ALL_ALERTS) {
        Alert.alert('Error', 'Customer information is missing. Please log in again.');
      }
      return;
    }

    if (!selectedFarmer.id && !selectedFarmer._id) {
      if (ALL_ALERTS) {
        Alert.alert('Error', 'Farmer information is missing. Please try again.');
      }
      return;
    }

    setSubmittingOrder(true);
    try {
      const token = await AsyncStorage.getItem('customerToken');
      
      const orderData = {
        customerId: customerData.id || customerData._id,
        farmerId: selectedFarmer.id || selectedFarmer._id,
        products: orderItems,
        deliveryAddress: deliveryAddress.trim(),
        customerNotes: customerNotes.trim()
      };
      
      console.log('🛒 Submitting order:', orderData);
      console.log('👤 Customer data:', customerData);
      console.log('👨‍🌾 Selected farmer:', selectedFarmer);
      console.log('📦 Order items:', orderItems);
      console.log('🏷️ Available products from farmer:', selectedFarmer.availableProducts);
      
      // Test: Check what products farmer actually has configured
      try {
        const farmerProductsResponse = await fetch(`${API_BASE_URL}/api/products/farmer/${selectedFarmer.id || selectedFarmer._id}/products`, {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        });
        const farmerProductsResult = await farmerProductsResponse.json();
        console.log('🧑‍🌾 Farmer actual products config:', farmerProductsResult);
      } catch (farmerError) {
        console.log('❌ Farmer products check error:', farmerError);
      }

      // Test: Validate farmer products before creating order
      try {
        const validateResponse = await fetch(`${API_BASE_URL}/api/products/farmer/validate`, {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            farmerId: selectedFarmer.id || selectedFarmer._id,
            products: orderItems
          })
        });
        const validateResult = await validateResponse.json();
        console.log('✅ Product validation response:', validateResult);
      } catch (validateError) {
        console.log('❌ Product validation error:', validateError);
      }
      
      const response = await fetch(`${API_BASE_URL}/api/orders/create`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(orderData)
      });

      const result = await response.json();
      console.log('📡 Order creation response:', result);
      
      if (result.success) {
        setShowOrderModal(false);
        if (ALL_ALERTS) {
          Alert.alert(
            'Order Placed Successfully!',
            'Your order has been sent to the farmer for confirmation.',
            [
              {
                text: 'View Orders',
                onPress: () => router.push('/orderHistory')
              },
              {
                text: 'Continue Shopping',
                style: 'cancel'
              }
            ]
          );
        }
        // Refresh sellers to get updated data
        fetchSellers();
      } else {
        if (ALL_ALERTS) {
          Alert.alert('Order Failed', result.message || 'Failed to place order');
        }
      }
    } catch (error) {
      console.error('Error submitting order:', error);
      if (ALL_ALERTS) {
        Alert.alert('Error', 'Failed to submit order. Please try again.');
      }
    } finally {
      setSubmittingOrder(false);
    }
  };

  const getProductDisplayName = (productType) => {
    const names = {
      MILK_COW: 'Cow Milk',
      MILK_BUFFALO: 'Buffalo Milk',
      MILK_GOAT: 'Goat Milk',
      BUTTER: 'Fresh Butter',
      HEN_EGGS: 'Hen Eggs',
      DUCK_EGGS: 'Duck Eggs',
      // Legacy support for old format
      EGGS_HEN: 'Hen Eggs',
      EGGS_DUCK: 'Duck Eggs'
    };
    return names[productType] || productType;
  };

  const getProductUnit = (productType) => {
    const units = {
      MILK_COW: 'liter',
      MILK_BUFFALO: 'liter',
      MILK_GOAT: 'liter',
      BUTTER: 'kg',
      HEN_EGGS: 'dozen',
      DUCK_EGGS: 'dozen',
      // Legacy support for old format
      EGGS_HEN: 'dozen',
      EGGS_DUCK: 'dozen'
    };
    return units[productType] || 'unit';
  };

  const getProductPrice = (productType) => {
    const prices = {
      MILK_COW: 50,
      MILK_BUFFALO: 60,
      MILK_GOAT: 70,
      BUTTER: 200,
      HEN_EGGS: 120,
      DUCK_EGGS: 150,
      // Legacy support for old format
      EGGS_HEN: 120,
      EGGS_DUCK: 150
    };
    return prices[productType] || 0;
  };

  const getProductIcon = (productType) => {
    const icons = {
      MILK_COW: 'water',
      MILK_BUFFALO: 'water',
      MILK_GOAT: 'water',
      BUTTER: 'cube',
      HEN_EGGS: 'ellipse',
      DUCK_EGGS: 'ellipse',
      // Legacy support for old format
      EGGS_HEN: 'ellipse',
      EGGS_DUCK: 'ellipse'
    };
    return icons[productType] || 'help';
  };

  const getProductColor = (productType) => {
    const colors = {
      MILK_COW: '#8B4513',
      MILK_BUFFALO: '#4B4B4D',
      MILK_GOAT: '#F5DEB3',
      BUTTER: '#FFD700',
      HEN_EGGS: '#F4A460',
      DUCK_EGGS: '#FFFACD',
      // Legacy support for old format
      EGGS_HEN: '#F4A460',
      EGGS_DUCK: '#FFFACD'
    };
    return colors[productType] || '#666';
  };

  if (loading) {
    return (
      <SafeAreaView style={styles.container}>
        <View style={styles.header}>
          <TouchableOpacity onPress={() => router.back()} style={styles.backButton}>
            <Ionicons name="arrow-back" size={24} color="#fff" />
          </TouchableOpacity>
          <Text style={styles.headerTitle}>Buy Products</Text>
          <View style={styles.placeholder} />
        </View>
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color="#2E7D32" />
          <Text style={styles.loadingText}>Loading marketplace...</Text>
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backButton}>
          <Ionicons name="arrow-back" size={24} color="#fff" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Buy Products</Text>
        <TouchableOpacity onPress={() => router.push('/orderHistory')} style={styles.ordersButton}>
          <Ionicons name="receipt" size={20} color="#fff" />
        </TouchableOpacity>
      </View>

      {/* Content */}
      <ScrollView style={styles.content}>
        {sellers.length === 0 ? (
          <View style={styles.emptyState}>
            <Ionicons name="storefront-outline" size={64} color="#ccc" />
            <Text style={styles.emptyTitle}>No Sellers Available</Text>
            <Text style={styles.emptySubtitle}>
              There are currently no farmers selling products in your area.
              Check back later or contact farmers directly.
            </Text>
            <TouchableOpacity style={styles.refreshButton} onPress={fetchSellers}>
              <Text style={styles.refreshButtonText}>Refresh</Text>
            </TouchableOpacity>
          </View>
        ) : (
          <>
            <Text style={styles.sectionTitle}>Available Sellers ({sellers.length})</Text>
            {sellers.map((seller) => (
              <View key={seller.id} style={styles.sellerCard}>
                <View style={styles.sellerHeader}>
                  <View style={styles.sellerInfo}>
                    <Text style={styles.sellerName}>{seller.name}</Text>
                    <Text style={styles.sellerLocation}>
                      <Ionicons name="location" size={12} color="#666" /> {seller.location}
                    </Text>
                    <Text style={styles.sellerPhone}>
                      <Ionicons name="call" size={12} color="#666" /> {seller.phone}
                    </Text>
                  </View>
                  <TouchableOpacity
                    style={styles.orderButton}
                    onPress={() => openOrderModal(seller)}
                  >
                    <Ionicons name="basket" size={16} color="#fff" />
                    <Text style={styles.orderButtonText}>Order</Text>
                  </TouchableOpacity>
                </View>

                <View style={styles.productsContainer}>
                  <Text style={styles.productsTitle}>Available Products:</Text>
                  <View style={styles.productsList}>
                    {seller.availableProducts?.map((product) => (
                      <View key={product.type} style={styles.productTag}>
                        <Ionicons 
                          name={getProductIcon(product.type)} 
                          size={14} 
                          color={getProductColor(product.type)} 
                        />
                        <View style={styles.productTagInfo}>
                          <Text style={styles.productTagText}>
                            {product.name} ({getProductUnit(product.type)})
                          </Text>
                          <Text style={styles.productTagPrice}>
                            ৳{getProductPrice(product.type)}
                          </Text>
                        </View>
                      </View>
                    ))}
                  </View>
                </View>
              </View>
            ))}
          </>
        )}
      </ScrollView>

      {/* Order Modal */}
      <Modal
        visible={showOrderModal}
        animationType="slide"
        transparent={true}
        onRequestClose={() => setShowOrderModal(false)}
      >
        <View style={styles.modalContainer}>
          <View style={styles.modalContent}>
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>Order from {selectedFarmer?.name}</Text>
              <TouchableOpacity onPress={() => setShowOrderModal(false)}>
                <Ionicons name="close" size={24} color="#333" />
              </TouchableOpacity>
            </View>

            <ScrollView style={styles.modalBody}>
              {/* Delivery Address */}
              <View style={styles.formGroup}>
                <Text style={styles.formLabel}>Delivery Address *</Text>
                <TextInput
                  style={styles.textInput}
                  value={deliveryAddress}
                  onChangeText={setDeliveryAddress}
                  placeholder="Enter your delivery address"
                  multiline
                />
              </View>

              {/* Product Selection */}
              <View style={styles.formGroup}>
                <Text style={styles.formLabel}>Select Products</Text>
                {selectedFarmer?.availableProducts?.map((product) => (
                  <View key={product.type} style={styles.productOrderItem}>
                    <View style={styles.productOrderInfo}>
                      <Ionicons 
                        name={getProductIcon(product.type)} 
                        size={20} 
                        color={getProductColor(product.type)} 
                      />
                      <View style={styles.productNameContainer}>
                        <Text style={styles.productOrderName}>{product.name}</Text>
                        <Text style={styles.productUnit}>
                          ৳{getProductPrice(product.type)} per {getProductUnit(product.type)}
                        </Text>
                      </View>
                    </View>
                    <View style={styles.quantityControls}>
                      <TouchableOpacity
                        style={styles.quantityButton}
                        onPress={() => updateOrderQuantity(product.type, -1)}
                      >
                        <Ionicons name="remove" size={16} color="#666" />
                      </TouchableOpacity>
                      <Text style={styles.quantityText}>{orderItems[product.type] || 0}</Text>
                      <TouchableOpacity
                        style={styles.quantityButton}
                        onPress={() => updateOrderQuantity(product.type, 1)}
                      >
                        <Ionicons name="add" size={16} color="#666" />
                      </TouchableOpacity>
                    </View>
                  </View>
                ))}
              </View>

              {/* Customer Notes */}
              <View style={styles.formGroup}>
                <Text style={styles.formLabel}>Notes (Optional)</Text>
                <TextInput
                  style={styles.textInput}
                  value={customerNotes}
                  onChangeText={setCustomerNotes}
                  placeholder="Any special instructions or preferences..."
                  multiline
                />
              </View>

              {/* Order Summary */}
              {Object.keys(orderItems).length > 0 && (
                <View style={styles.orderSummary}>
                  <Text style={styles.summaryTitle}>Order Summary</Text>
                  {Object.entries(orderItems).map(([product, quantity]) => (
                    <View key={product} style={styles.summaryItem}>
                      <Text style={styles.summaryProduct}>
                        {getProductDisplayName(product)} x {quantity} {getProductUnit(product)}
                      </Text>
                      <Text style={styles.summaryPrice}>
                        ৳{getProductPrice(product) * quantity}
                      </Text>
                    </View>
                  ))}
                  <View style={styles.summaryTotal}>
                    <Text style={styles.totalText}>Estimated Total: ৳{calculateTotal()}</Text>
                    <Text style={styles.totalNote}>*Final price will be confirmed by farmer</Text>
                  </View>
                </View>
              )}
            </ScrollView>

            {/* Modal Footer */}
            <View style={styles.modalFooter}>
              <TouchableOpacity
                style={styles.cancelButton}
                onPress={() => setShowOrderModal(false)}
              >
                <Text style={styles.cancelButtonText}>Cancel</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[styles.submitButton, submittingOrder && styles.submitButtonDisabled]}
                onPress={submitOrder}
                disabled={submittingOrder}
              >
                {submittingOrder ? (
                  <ActivityIndicator size="small" color="#fff" />
                ) : (
                  <Text style={styles.submitButtonText}>Place Order</Text>
                )}
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
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
  ordersButton: {
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
  refreshButton: {
    backgroundColor: '#2E7D32',
    padding: 12,
    borderRadius: 8,
  },
  refreshButtonText: {
    color: '#fff',
    fontWeight: 'bold',
  },
  sellerCard: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  sellerHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 12,
  },
  sellerInfo: {
    flex: 1,
  },
  sellerName: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 4,
  },
  sellerLocation: {
    fontSize: 14,
    color: '#666',
    marginBottom: 2,
  },
  sellerPhone: {
    fontSize: 14,
    color: '#666',
  },
  orderButton: {
    backgroundColor: '#FF6B35',
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
    gap: 4,
  },
  orderButtonText: {
    color: '#fff',
    fontWeight: 'bold',
    fontSize: 14,
  },
  productsContainer: {
    borderTopWidth: 1,
    borderTopColor: '#f0f0f0',
    paddingTop: 12,
  },
  productsTitle: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333',
    marginBottom: 8,
  },
  productsList: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  productTag: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#f8f9fa',
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 16,
    gap: 4,
  },
  productTagInfo: {
    flexDirection: 'column',
    alignItems: 'flex-start',
  },
  productTagText: {
    fontSize: 12,
    color: '#333',
  },
  productTagPrice: {
    fontSize: 11,
    color: '#2E7D32',
    fontWeight: 'bold',
  },
  // Modal Styles
  modalContainer: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'flex-end',
  },
  modalContent: {
    backgroundColor: '#fff',
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    maxHeight: '90%',
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 20,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
  },
  modalBody: {
    padding: 20,
  },
  formGroup: {
    marginBottom: 20,
  },
  formLabel: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
    marginBottom: 8,
  },
  textInput: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
    backgroundColor: '#fff',
  },
  productOrderItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 12,
    backgroundColor: '#f8f9fa',
    borderRadius: 8,
    marginBottom: 8,
  },
  productOrderInfo: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
    gap: 8,
  },
  productNameContainer: {
    flex: 1,
  },
  productOrderName: {
    fontSize: 16,
    color: '#333',
  },
  productUnit: {
    fontSize: 12,
    color: '#666',
    marginTop: 2,
  },
  quantityControls: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  quantityButton: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: '#fff',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#ddd',
  },
  quantityText: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333',
    minWidth: 24,
    textAlign: 'center',
  },
  orderSummary: {
    backgroundColor: '#f8f9fa',
    padding: 16,
    borderRadius: 8,
    marginTop: 8,
  },
  summaryTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 12,
  },
  summaryItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 6,
  },
  summaryProduct: {
    fontSize: 14,
    color: '#666',
    flex: 1,
  },
  summaryPrice: {
    fontSize: 14,
    color: '#2E7D32',
    fontWeight: 'bold',
  },
  summaryTotal: {
    borderTopWidth: 1,
    borderTopColor: '#ddd',
    paddingTop: 12,
    marginTop: 8,
  },
  totalText: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#2E7D32',
  },
  totalNote: {
    fontSize: 12,
    color: '#999',
    marginTop: 4,
  },
  modalFooter: {
    flexDirection: 'row',
    padding: 20,
    borderTopWidth: 1,
    borderTopColor: '#f0f0f0',
    gap: 12,
  },
  cancelButton: {
    flex: 1,
    padding: 14,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#ddd',
    alignItems: 'center',
  },
  cancelButtonText: {
    color: '#666',
    fontWeight: 'bold',
    fontSize: 16,
  },
  submitButton: {
    flex: 1,
    backgroundColor: '#2E7D32',
    padding: 14,
    borderRadius: 8,
    alignItems: 'center',
  },
  submitButtonDisabled: {
    backgroundColor: '#ccc',
  },
  submitButtonText: {
    color: '#fff',
    fontWeight: 'bold',
    fontSize: 16,
  },
});

export default BuyProducts;