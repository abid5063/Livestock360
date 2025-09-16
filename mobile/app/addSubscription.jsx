import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  TextInput,
  Alert,
  ActivityIndicator,
  Image,
} from 'react-native';
import { router } from 'expo-router';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Ionicons } from '@expo/vector-icons';
import axios from 'axios';
import { API_BASE_URL } from '../utils/apiConfig';

const subscriptionPackages = [
  {
    id: 'basic',
    name: 'Basic Package',
    amount: 50,
    tokens: 10,
    description: '50 TK for 10 Tokens',
    color: '#4CAF50',
    popular: false,
  },
  {
    id: 'standard',
    name: 'Standard Package',
    amount: 100,
    tokens: 25,
    description: '100 TK for 25 Tokens',
    color: '#2196F3',
    popular: true,
  },
  {
    id: 'premium',
    name: 'Premium Package',
    amount: 500,
    tokens: 150,
    description: '500 TK for 150 Tokens',
    color: '#FF9800',
    popular: false,
  },
];

export default function AddSubscription() {
  const [selectedPackage, setSelectedPackage] = useState(null);
  const [transactionId, setTransactionId] = useState('');
  const [loading, setLoading] = useState(false);

  const bkashNumber = '01XXXXXXXXX'; // Replace with actual bKash merchant number

  const handlePackageSelection = (packageId) => {
    setSelectedPackage(packageId);
  };

  const handleSubmitSubscription = async () => {
    if (!selectedPackage) {
      Alert.alert('Error', 'Please select a subscription package');
      return;
    }

    if (!transactionId.trim()) {
      Alert.alert('Error', 'Please enter the bKash transaction ID');
      return;
    }

    if (transactionId.length < 8) {
      Alert.alert('Error', 'Please enter a valid transaction ID (minimum 8 characters)');
      return;
    }

    setLoading(true);

    try {
      const token = await AsyncStorage.getItem('authToken');
      if (!token) {
        Alert.alert('Error', 'Authentication token not found. Please login again.');
        router.replace('/');
        return;
      }

      const response = await axios.post(
        `${API_BASE_URL}/api/subscriptions/submit`,
        {
          subscriptionPackage: selectedPackage,
          transactionId: transactionId.trim(),
        },
        {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json',
          },
        }
      );

      if (response.data.success) {
        Alert.alert(
          'Success',
          'Your subscription request has been submitted successfully! It will be reviewed by an admin and you will receive tokens once approved.',
          [
            {
              text: 'OK',
              onPress: () => router.back(),
            },
          ]
        );
      } else {
        Alert.alert('Error', response.data.message || 'Failed to submit subscription request');
      }
    } catch (error) {
      console.error('Subscription submission error:', error);
      if (error.response?.status === 401) {
        Alert.alert('Session Expired', 'Please login again.');
        await AsyncStorage.multiRemove(['authToken', 'userData']);
        router.replace('/');
      } else if (error.response?.status === 409) {
        Alert.alert('Error', 'This transaction ID has already been used. Please use a different transaction ID.');
      } else {
        Alert.alert('Error', error.response?.data?.message || 'Network error. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  const getSelectedPackage = () => {
    return subscriptionPackages.find(pkg => pkg.id === selectedPackage);
  };

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.contentContainer}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity 
          style={styles.backButton} 
          onPress={() => router.back()}
        >
          <Ionicons name="arrow-back" size={24} color="#fff" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Add Subscription</Text>
        <View style={styles.placeholder} />
      </View>

      {/* bKash Payment Instructions */}
      <View style={styles.instructionsContainer}>
        <View style={styles.instructionsHeader}>
          <Image 
            source={{ uri: 'https://logos-world.net/wp-content/uploads/2022/01/bKash-Logo.png' }}
            style={styles.bkashLogo}
            resizeMode="contain"
          />
          <Text style={styles.instructionsTitle}>Payment Instructions</Text>
        </View>
        
        <View style={styles.instructionCard}>
          <Text style={styles.instructionStep}>Step 1: Send money to this bKash number:</Text>
          <View style={styles.bkashNumberContainer}>
            <Ionicons name="call" size={20} color="#E2136E" />
            <Text style={styles.bkashNumber}>{bkashNumber}</Text>
          </View>
        </View>

        <View style={styles.instructionCard}>
          <Text style={styles.instructionStep}>Step 2: Select your package below</Text>
        </View>

        <View style={styles.instructionCard}>
          <Text style={styles.instructionStep}>Step 3: Enter the transaction ID you received</Text>
        </View>

        <View style={styles.instructionCard}>
          <Text style={styles.instructionStep}>Step 4: Submit for admin approval</Text>
        </View>
      </View>

      {/* Package Selection */}
      <View style={styles.packagesContainer}>
        <Text style={styles.sectionTitle}>Select Subscription Package</Text>
        
        {subscriptionPackages.map((pkg) => (
          <TouchableOpacity
            key={pkg.id}
            style={[
              styles.packageCard,
              selectedPackage === pkg.id && styles.selectedPackageCard,
              pkg.popular && styles.popularPackageCard,
            ]}
            onPress={() => handlePackageSelection(pkg.id)}
          >
            {pkg.popular && (
              <View style={styles.popularBadge}>
                <Text style={styles.popularBadgeText}>POPULAR</Text>
              </View>
            )}
            
            <View style={styles.packageHeader}>
              <View style={[styles.packageIcon, { backgroundColor: pkg.color }]}>
                <Ionicons name="diamond" size={24} color="#fff" />
              </View>
              <View style={styles.packageInfo}>
                <Text style={styles.packageName}>{pkg.name}</Text>
                <Text style={styles.packageDescription}>{pkg.description}</Text>
              </View>
              <View style={styles.packagePricing}>
                <Text style={styles.packageAmount}>{pkg.amount} TK</Text>
                <Text style={styles.packageTokens}>{pkg.tokens} Tokens</Text>
              </View>
            </View>
            
            <View style={styles.packageValue}>
              <Text style={styles.packageValueText}>
                Value: {(pkg.amount / pkg.tokens).toFixed(1)} TK per token
              </Text>
            </View>
            
            {selectedPackage === pkg.id && (
              <View style={styles.selectedIndicator}>
                <Ionicons name="checkmark-circle" size={24} color="#4CAF50" />
              </View>
            )}
          </TouchableOpacity>
        ))}
      </View>

      {/* Transaction ID Input */}
      <View style={styles.transactionContainer}>
        <Text style={styles.sectionTitle}>Transaction Details</Text>
        
        <View style={styles.inputContainer}>
          <Text style={styles.inputLabel}>bKash Transaction ID *</Text>
          <TextInput
            style={styles.transactionInput}
            placeholder="Enter your bKash transaction ID"
            value={transactionId}
            onChangeText={setTransactionId}
            autoCapitalize="characters"
            placeholderTextColor="#999"
          />
          <Text style={styles.inputHelper}>
            You will receive this ID after completing the bKash payment
          </Text>
        </View>
      </View>

      {/* Summary */}
      {selectedPackage && (
        <View style={styles.summaryContainer}>
          <Text style={styles.sectionTitle}>Order Summary</Text>
          <View style={styles.summaryCard}>
            <View style={styles.summaryRow}>
              <Text style={styles.summaryLabel}>Package:</Text>
              <Text style={styles.summaryValue}>{getSelectedPackage()?.name}</Text>
            </View>
            <View style={styles.summaryRow}>
              <Text style={styles.summaryLabel}>Amount:</Text>
              <Text style={styles.summaryValue}>{getSelectedPackage()?.amount} TK</Text>
            </View>
            <View style={styles.summaryRow}>
              <Text style={styles.summaryLabel}>Tokens:</Text>
              <Text style={styles.summaryValue}>{getSelectedPackage()?.tokens} Tokens</Text>
            </View>
            <View style={styles.summaryDivider} />
            <View style={styles.summaryRow}>
              <Text style={styles.summaryTotalLabel}>Total Payment:</Text>
              <Text style={styles.summaryTotalValue}>{getSelectedPackage()?.amount} TK</Text>
            </View>
          </View>
        </View>
      )}

      {/* Submit Button */}
      <TouchableOpacity
        style={[styles.submitButton, (!selectedPackage || !transactionId.trim() || loading) && styles.submitButtonDisabled]}
        onPress={handleSubmitSubscription}
        disabled={!selectedPackage || !transactionId.trim() || loading}
      >
        {loading ? (
          <ActivityIndicator size="small" color="#fff" />
        ) : (
          <>
            <Ionicons name="send" size={20} color="#fff" />
            <Text style={styles.submitButtonText}>Submit Subscription Request</Text>
          </>
        )}
      </TouchableOpacity>

      {/* Warning Note */}
      <View style={styles.warningContainer}>
        <Ionicons name="warning" size={20} color="#FF9800" />
        <Text style={styles.warningText}>
          Please ensure you have completed the bKash payment before submitting. 
          Your subscription will be processed after admin verification.
        </Text>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  contentContainer: {
    paddingBottom: 30,
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
  placeholder: {
    width: 40,
  },
  instructionsContainer: {
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
  instructionsHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 16,
  },
  bkashLogo: {
    width: 40,
    height: 40,
    marginRight: 12,
  },
  instructionsTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
  },
  instructionCard: {
    backgroundColor: '#f8f9fa',
    padding: 12,
    borderRadius: 8,
    marginBottom: 12,
  },
  instructionStep: {
    fontSize: 14,
    color: '#555',
    marginBottom: 8,
  },
  bkashNumberContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#fff',
    padding: 12,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#E2136E',
  },
  bkashNumber: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#E2136E',
    marginLeft: 8,
  },
  packagesContainer: {
    margin: 16,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 16,
  },
  packageCard: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
    borderWidth: 2,
    borderColor: '#e0e0e0',
    position: 'relative',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  selectedPackageCard: {
    borderColor: '#4CAF50',
    backgroundColor: '#f8fff8',
  },
  popularPackageCard: {
    borderColor: '#2196F3',
  },
  popularBadge: {
    position: 'absolute',
    top: -8,
    right: 16,
    backgroundColor: '#2196F3',
    paddingHorizontal: 12,
    paddingVertical: 4,
    borderRadius: 12,
  },
  popularBadgeText: {
    color: '#fff',
    fontSize: 10,
    fontWeight: 'bold',
  },
  packageHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 8,
  },
  packageIcon: {
    width: 48,
    height: 48,
    borderRadius: 24,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 12,
  },
  packageInfo: {
    flex: 1,
  },
  packageName: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333',
  },
  packageDescription: {
    fontSize: 14,
    color: '#666',
  },
  packagePricing: {
    alignItems: 'flex-end',
  },
  packageAmount: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#4CAF50',
  },
  packageTokens: {
    fontSize: 14,
    color: '#666',
  },
  packageValue: {
    alignItems: 'center',
  },
  packageValueText: {
    fontSize: 12,
    color: '#999',
    fontStyle: 'italic',
  },
  selectedIndicator: {
    position: 'absolute',
    top: 16,
    right: 16,
  },
  transactionContainer: {
    margin: 16,
  },
  inputContainer: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  inputLabel: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 8,
  },
  transactionInput: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
    backgroundColor: '#f9f9f9',
  },
  inputHelper: {
    fontSize: 12,
    color: '#666',
    marginTop: 8,
    fontStyle: 'italic',
  },
  summaryContainer: {
    margin: 16,
  },
  summaryCard: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  summaryRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  summaryLabel: {
    fontSize: 14,
    color: '#666',
  },
  summaryValue: {
    fontSize: 14,
    color: '#333',
    fontWeight: '500',
  },
  summaryDivider: {
    height: 1,
    backgroundColor: '#e0e0e0',
    marginVertical: 8,
  },
  summaryTotalLabel: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333',
  },
  summaryTotalValue: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#4CAF50',
  },
  submitButton: {
    backgroundColor: '#4CAF50',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    margin: 16,
    padding: 16,
    borderRadius: 12,
    gap: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.2,
    shadowRadius: 4,
    elevation: 3,
  },
  submitButtonDisabled: {
    backgroundColor: '#ccc',
  },
  submitButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
  },
  warningContainer: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    backgroundColor: '#fff3cd',
    margin: 16,
    padding: 16,
    borderRadius: 8,
    borderLeftWidth: 4,
    borderLeftColor: '#FF9800',
  },
  warningText: {
    flex: 1,
    fontSize: 14,
    color: '#856404',
    marginLeft: 8,
    lineHeight: 20,
  },
});