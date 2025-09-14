import React, { useState, useEffect } from 'react';
import { 
  View, 
  Text, 
  TextInput, 
  TouchableOpacity, 
  StyleSheet, 
  SafeAreaView, 
  Alert,
  ScrollView,
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform
} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { router } from 'expo-router';
import { MaterialIcons, Ionicons } from '@expo/vector-icons';
import { API_BASE_URL } from '../utils/apiConfig';

const CustomerProfile = () => {
  const [customerData, setCustomerData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [editMode, setEditMode] = useState(false);
  const [formData, setFormData] = useState({});

  useEffect(() => {
    loadCustomerData();
  }, []);

  const loadCustomerData = async () => {
    try {
      const token = await AsyncStorage.getItem('customerToken');
      const storedData = await AsyncStorage.getItem('customerData');
      
      if (token && storedData) {
        const data = JSON.parse(storedData);
        setCustomerData(data);
        setFormData(data);
      } else {
        // No token or data, redirect to root
        router.replace('/');
      }
    } catch (error) {
      console.error('Error loading customer data:', error);
      // Clear potentially corrupted data and redirect
      await AsyncStorage.multiRemove(['customerToken', 'customerData', 'userType']);
      router.replace('/');
    }
  };

  const handleInputChange = (field, value) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));
  };

  const handleSave = async () => {
    setLoading(true);
    try {
      const token = await AsyncStorage.getItem('customerToken');
      
      const response = await fetch(`${API_BASE_URL}/api/auth/profile`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify(formData),
      });

      const data = await response.json();

      if (data.success) {
        await AsyncStorage.setItem('customerData', JSON.stringify(data.customer));
        setCustomerData(data.customer);
        setEditMode(false);
        Alert.alert('Success', 'Profile updated successfully!');
      } else {
        Alert.alert('Error', data.message || 'Failed to update profile');
      }
    } catch (error) {
      console.error('Update profile error:', error);
      Alert.alert('Error', 'Network error. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    setFormData(customerData);
    setEditMode(false);
  };

  const handleLogout = async () => {
    try {
      // Clear all AsyncStorage data
      await AsyncStorage.clear();
      
      // Clear local state
      setCustomerData(null);
      setFormData({});
      
      // Navigate to welcome page
      router.replace('/');
    } catch (error) {
      console.error('Error during logout:', error);
      // Force navigation even if there's an error clearing storage
      router.replace('/');
    }
  };

  if (!customerData) {
    return (
      <SafeAreaView style={styles.container}>
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color="#2E7D32" />
          <Text style={styles.loadingText}>Loading profile...</Text>
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <KeyboardAvoidingView 
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        style={styles.keyboardAvoidingView}
      >
        {/* Header */}
        <View style={styles.header}>
          <TouchableOpacity onPress={() => router.push('/customerDashboard')} style={styles.backButton}>
            <MaterialIcons name="arrow-back" size={24} color="#fff" />
          </TouchableOpacity>
          <Text style={styles.headerTitle}>My Profile</Text>
          <TouchableOpacity onPress={handleLogout} style={styles.logoutButton}>
            <MaterialIcons name="logout" size={24} color="#fff" />
          </TouchableOpacity>
        </View>

        <ScrollView style={styles.scrollView}>
          <View style={styles.profileContainer}>
            {/* Profile Header */}
            <View style={styles.profileHeader}>
              <View style={styles.avatarContainer}>
                <MaterialIcons name="person" size={60} color="#2E7D32" />
              </View>
              <Text style={styles.nameText}>{customerData.name}</Text>
              <Text style={styles.emailText}>{customerData.email}</Text>
              <Text style={styles.customerTypeText}>
                {customerData.customerType === 'business' ? 'Business Customer' : 'Individual Customer'}
              </Text>
            </View>

            {/* Action Buttons */}
            <View style={styles.actionButtonsContainer}>
              {!editMode ? (
                <>
                  <TouchableOpacity 
                    style={styles.editButton}
                    onPress={() => setEditMode(true)}
                  >
                    <MaterialIcons name="edit" size={20} color="#fff" />
                    <Text style={styles.buttonText}>Edit Profile</Text>
                  </TouchableOpacity>
                  
                  <TouchableOpacity 
                    style={styles.dashboardButton}
                    onPress={() => router.push('/customerDashboard')}
                  >
                    <MaterialIcons name="dashboard" size={20} color="#2E7D32" />
                    <Text style={styles.dashboardButtonText}>Dashboard</Text>
                  </TouchableOpacity>
                </>
              ) : (
                <View style={styles.editButtonsRow}>
                  <TouchableOpacity 
                    style={[styles.saveButton, loading && styles.buttonDisabled]}
                    onPress={handleSave}
                    disabled={loading}
                  >
                    {loading ? (
                      <ActivityIndicator color="#fff" size="small" />
                    ) : (
                      <>
                        <MaterialIcons name="save" size={20} color="#fff" />
                        <Text style={styles.buttonText}>Save</Text>
                      </>
                    )}
                  </TouchableOpacity>
                  
                  <TouchableOpacity 
                    style={styles.cancelButton}
                    onPress={handleCancel}
                  >
                    <MaterialIcons name="cancel" size={20} color="#666" />
                    <Text style={styles.cancelButtonText}>Cancel</Text>
                  </TouchableOpacity>
                </View>
              )}
            </View>

            {/* Profile Information */}
            <View style={styles.infoContainer}>
              <Text style={styles.sectionTitle}>Personal Information</Text>
              
              <View style={styles.fieldContainer}>
                <Text style={styles.fieldLabel}>Full Name</Text>
                {editMode ? (
                  <TextInput
                    style={styles.input}
                    value={formData.name || ''}
                    onChangeText={(text) => handleInputChange('name', text)}
                    placeholder="Enter your full name"
                  />
                ) : (
                  <Text style={styles.fieldValue}>{customerData.name || 'Not provided'}</Text>
                )}
              </View>

              <View style={styles.fieldContainer}>
                <Text style={styles.fieldLabel}>Email</Text>
                <Text style={styles.fieldValue}>{customerData.email}</Text>
                {editMode && (
                  <Text style={styles.fieldNote}>Email cannot be changed</Text>
                )}
              </View>

              <View style={styles.fieldContainer}>
                <Text style={styles.fieldLabel}>Phone Number</Text>
                {editMode ? (
                  <TextInput
                    style={styles.input}
                    value={formData.phone || ''}
                    onChangeText={(text) => handleInputChange('phone', text)}
                    placeholder="Enter your phone number"
                    keyboardType="phone-pad"
                  />
                ) : (
                  <Text style={styles.fieldValue}>{customerData.phone || 'Not provided'}</Text>
                )}
              </View>

              <View style={styles.fieldContainer}>
                <Text style={styles.fieldLabel}>Location</Text>
                {editMode ? (
                  <TextInput
                    style={styles.input}
                    value={formData.location || ''}
                    onChangeText={(text) => handleInputChange('location', text)}
                    placeholder="Enter your location"
                  />
                ) : (
                  <Text style={styles.fieldValue}>{customerData.location || 'Not provided'}</Text>
                )}
              </View>

              <View style={styles.fieldContainer}>
                <Text style={styles.fieldLabel}>Address</Text>
                {editMode ? (
                  <TextInput
                    style={[styles.input, styles.textArea]}
                    value={formData.address || ''}
                    onChangeText={(text) => handleInputChange('address', text)}
                    placeholder="Enter your address"
                    multiline
                    numberOfLines={3}
                  />
                ) : (
                  <Text style={styles.fieldValue}>{customerData.address || 'Not provided'}</Text>
                )}
              </View>

              <View style={styles.fieldContainer}>
                <Text style={styles.fieldLabel}>Customer Type</Text>
                <Text style={styles.fieldValue}>
                  {customerData.customerType === 'business' ? 'Business Customer' : 'Individual Customer'}
                </Text>
              </View>

              {customerData.customerType === 'business' && (
                <>
                  <View style={styles.fieldContainer}>
                    <Text style={styles.fieldLabel}>Business Name</Text>
                    {editMode ? (
                      <TextInput
                        style={styles.input}
                        value={formData.businessName || ''}
                        onChangeText={(text) => handleInputChange('businessName', text)}
                        placeholder="Enter business name"
                      />
                    ) : (
                      <Text style={styles.fieldValue}>{customerData.businessName || 'Not provided'}</Text>
                    )}
                  </View>

                  <View style={styles.fieldContainer}>
                    <Text style={styles.fieldLabel}>Business License</Text>
                    {editMode ? (
                      <TextInput
                        style={styles.input}
                        value={formData.businessLicense || ''}
                        onChangeText={(text) => handleInputChange('businessLicense', text)}
                        placeholder="Enter business license number"
                      />
                    ) : (
                      <Text style={styles.fieldValue}>{customerData.businessLicense || 'Not provided'}</Text>
                    )}
                  </View>
                </>
              )}

              <View style={styles.fieldContainer}>
                <Text style={styles.fieldLabel}>Maximum Budget</Text>
                {editMode ? (
                  <TextInput
                    style={styles.input}
                    value={formData.maxBudget ? formData.maxBudget.toString() : ''}
                    onChangeText={(text) => handleInputChange('maxBudget', parseFloat(text) || 0)}
                    placeholder="Enter maximum budget"
                    keyboardType="numeric"
                  />
                ) : (
                  <Text style={styles.fieldValue}>
                    ${customerData.maxBudget ? customerData.maxBudget.toLocaleString() : '0'}
                  </Text>
                )}
              </View>

              <View style={styles.fieldContainer}>
                <Text style={styles.fieldLabel}>Member Since</Text>
                <Text style={styles.fieldValue}>
                  {customerData.dateJoined ? new Date(customerData.dateJoined).toLocaleDateString() : 'Recently'}
                </Text>
              </View>
            </View>

            {/* Quick Actions */}
            <View style={styles.quickActionsContainer}>
              <Text style={styles.sectionTitle}>Quick Actions</Text>
              
              <TouchableOpacity 
                style={styles.actionCard}
                onPress={() => router.push('/animalMarketplace')}
              >
                <View style={styles.actionIcon}>
                  <Ionicons name="storefront" size={25} color="#2E7D32" />
                </View>
                <Text style={styles.actionTitle}>Browse Marketplace</Text>
                <MaterialIcons name="chevron-right" size={24} color="#ccc" />
              </TouchableOpacity>

              <TouchableOpacity 
                style={styles.actionCard}
                onPress={() => router.push('/customerMessaging')}
              >
                <View style={styles.actionIcon}>
                  <MaterialIcons name="message" size={25} color="#2E7D32" />
                </View>
                <Text style={styles.actionTitle}>Messages</Text>
                <MaterialIcons name="chevron-right" size={24} color="#ccc" />
              </TouchableOpacity>

              <TouchableOpacity 
                style={styles.actionCard}
                onPress={() => router.push('/customerDashboard')}
              >
                <View style={styles.actionIcon}>
                  <MaterialIcons name="dashboard" size={25} color="#2E7D32" />
                </View>
                <Text style={styles.actionTitle}>Dashboard</Text>
                <MaterialIcons name="chevron-right" size={24} color="#ccc" />
              </TouchableOpacity>
            </View>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  keyboardAvoidingView: {
    flex: 1,
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingText: {
    marginTop: 10,
    fontSize: 16,
    color: '#666',
  },
  header: {
    backgroundColor: '#2E7D32',
    padding: 20,
    paddingTop: 40,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  backButton: {
    padding: 5,
  },
  headerTitle: {
    color: '#fff',
    fontSize: 20,
    fontWeight: 'bold',
  },
  logoutButton: {
    padding: 5,
  },
  scrollView: {
    flex: 1,
  },
  profileContainer: {
    padding: 20,
  },
  profileHeader: {
    backgroundColor: '#fff',
    padding: 20,
    borderRadius: 15,
    alignItems: 'center',
    marginBottom: 20,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 3.84,
    elevation: 5,
  },
  avatarContainer: {
    width: 100,
    height: 100,
    borderRadius: 50,
    backgroundColor: '#f0f0f0',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 15,
  },
  nameText: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 5,
  },
  emailText: {
    fontSize: 16,
    color: '#666',
    marginBottom: 5,
  },
  customerTypeText: {
    fontSize: 14,
    color: '#2E7D32',
    fontWeight: '500',
  },
  actionButtonsContainer: {
    marginBottom: 20,
  },
  editButton: {
    backgroundColor: '#2E7D32',
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 15,
    borderRadius: 10,
    marginBottom: 10,
  },
  dashboardButton: {
    backgroundColor: '#fff',
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 15,
    borderRadius: 10,
    borderWidth: 2,
    borderColor: '#2E7D32',
  },
  dashboardButtonText: {
    color: '#2E7D32',
    fontSize: 16,
    fontWeight: 'bold',
    marginLeft: 8,
  },
  editButtonsRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  saveButton: {
    backgroundColor: '#2E7D32',
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 15,
    borderRadius: 10,
    flex: 1,
    marginRight: 10,
  },
  cancelButton: {
    backgroundColor: '#fff',
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 15,
    borderRadius: 10,
    flex: 1,
    borderWidth: 1,
    borderColor: '#ddd',
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
    marginLeft: 8,
  },
  cancelButtonText: {
    color: '#666',
    fontSize: 16,
    fontWeight: 'bold',
    marginLeft: 8,
  },
  buttonDisabled: {
    backgroundColor: '#ccc',
  },
  infoContainer: {
    backgroundColor: '#fff',
    padding: 20,
    borderRadius: 15,
    marginBottom: 20,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 3.84,
    elevation: 5,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    marginBottom: 20,
    color: '#333',
  },
  fieldContainer: {
    marginBottom: 20,
  },
  fieldLabel: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333',
    marginBottom: 8,
  },
  fieldValue: {
    fontSize: 16,
    color: '#666',
    paddingVertical: 8,
  },
  fieldNote: {
    fontSize: 12,
    color: '#999',
    fontStyle: 'italic',
    marginTop: 5,
  },
  input: {
    borderWidth: 1,
    borderColor: '#ddd',
    padding: 12,
    borderRadius: 8,
    fontSize: 16,
    backgroundColor: '#fff',
  },
  textArea: {
    height: 80,
    textAlignVertical: 'top',
  },
  quickActionsContainer: {
    backgroundColor: '#fff',
    padding: 20,
    borderRadius: 15,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 3.84,
    elevation: 5,
  },
  actionCard: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 15,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
  },
  actionIcon: {
    marginRight: 15,
    width: 30,
  },
  actionTitle: {
    flex: 1,
    fontSize: 16,
    color: '#333',
    fontWeight: '500',
  },
});

export default CustomerProfile;