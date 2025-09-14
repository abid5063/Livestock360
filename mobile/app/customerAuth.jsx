import React, { useState, useEffect } from 'react';
import { 
  View, 
  Text, 
  TextInput, 
  TouchableOpacity, 
  StyleSheet, 
  SafeAreaView, 
  Alert, 
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  ActivityIndicator
} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { router } from 'expo-router';
import { API_BASE_URL } from '../utils/apiConfig';
import { useLanguage } from '../utils/LanguageContext';

const CustomerAuth = () => {
  const [isLogin, setIsLogin] = useState(true);
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    password: '',
    phone: '',
    location: '',
    address: '',
    customerType: 'individual',
    maxBudget: '',
    interestedAnimalTypes: [],
    businessName: '',
    businessLicense: ''
  });

  const { language, t } = useLanguage();

  useEffect(() => {
    checkAuthStatus();
  }, []);

  const checkAuthStatus = async () => {
    try {
      const token = await AsyncStorage.getItem('customerToken');
      if (token) {
        // Validate token by making a request to the backend
        const response = await fetch(`${API_BASE_URL}/api/customers/profile`, {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        });
        
        if (response.ok) {
          // Token is valid, redirect to dashboard
          router.replace('/customerDashboard');
        } else {
          // Token is invalid, clear storage
          await AsyncStorage.multiRemove(['customerToken', 'customerData', 'userType']);
        }
      }
    } catch (error) {
      console.log('Error checking auth status:', error);
      // Clear potentially corrupted data
      await AsyncStorage.multiRemove(['customerToken', 'customerData', 'userType']);
    }
  };

  const handleInputChange = (field, value) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));
  };

  const validateForm = () => {
    if (!formData.email || !formData.password) {
      Alert.alert('Error', 'Email and password are required');
      return false;
    }

    if (!isLogin && !formData.name) {
      Alert.alert('Error', 'Name is required for registration');
      return false;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(formData.email)) {
      Alert.alert('Error', 'Please enter a valid email address');
      return false;
    }

    if (formData.password.length < 6) {
      Alert.alert('Error', 'Password must be at least 6 characters long');
      return false;
    }

    return true;
  };

  const handleLogin = async () => {
    if (!validateForm()) return;

    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/login?userType=customer`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          email: formData.email,
          password: formData.password,
        }),
      });

      const data = await response.json();

      if (data.success) {
        await AsyncStorage.setItem('customerToken', data.token);
        await AsyncStorage.setItem('customerData', JSON.stringify(data.customer));
        await AsyncStorage.setItem('userType', 'customer');
        
        // Navigate directly to customer profile page
        router.replace('/customerProfile');
      } else {
        Alert.alert('Login Failed', data.message || 'Invalid credentials');
      }
    } catch (error) {
      console.error('Login error:', error);
      Alert.alert('Error', 'Network error. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async () => {
    if (!validateForm()) return;

    setLoading(true);
    try {
      const registrationData = {
        name: formData.name,
        email: formData.email,
        password: formData.password,
        phone: formData.phone,
        location: formData.location,
        address: formData.address,
        customerType: formData.customerType,
        maxBudget: parseFloat(formData.maxBudget) || 0,
        interestedAnimalTypes: formData.interestedAnimalTypes,
        businessName: formData.customerType === 'business' ? formData.businessName : '',
        businessLicense: formData.customerType === 'business' ? formData.businessLicense : ''
      };

      const response = await fetch(`${API_BASE_URL}/api/auth/register?userType=customer`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(registrationData),
      });

      const data = await response.json();

      if (data.success) {
        await AsyncStorage.setItem('customerToken', data.token);
        await AsyncStorage.setItem('customerData', JSON.stringify(data.customer));
        await AsyncStorage.setItem('userType', 'customer');
        
        // Navigate directly to customer profile page
        router.replace('/customerProfile');
      } else {
        Alert.alert('Registration Failed', data.message || 'Registration failed');
      }
    } catch (error) {
      console.error('Registration error:', error);
      Alert.alert('Error', 'Network error. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const toggleAuthMode = () => {
    setIsLogin(!isLogin);
    setFormData({
      name: '',
      email: '',
      password: '',
      phone: '',
      location: '',
      address: '',
      customerType: 'individual',
      maxBudget: '',
      interestedAnimalTypes: [],
      businessName: '',
      businessLicense: ''
    });
  };

  const renderLoginForm = () => (
    <View style={styles.formContainer}>
      <Text style={styles.title}>Customer Login</Text>
      
      <TextInput
        style={styles.input}
        placeholder="Email"
        value={formData.email}
        onChangeText={(text) => handleInputChange('email', text)}
        keyboardType="email-address"
        autoCapitalize="none"
      />
      
      <TextInput
        style={styles.input}
        placeholder="Password"
        value={formData.password}
        onChangeText={(text) => handleInputChange('password', text)}
        secureTextEntry
      />
      
      <TouchableOpacity 
        style={[styles.button, loading && styles.buttonDisabled]} 
        onPress={handleLogin}
        disabled={loading}
      >
        {loading ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <Text style={styles.buttonText}>Login</Text>
        )}
      </TouchableOpacity>
    </View>
  );

  const renderRegisterForm = () => (
    <View style={styles.formContainer}>
      <Text style={styles.title}>Customer Registration</Text>
      
      <TextInput
        style={styles.input}
        placeholder="Full Name *"
        value={formData.name}
        onChangeText={(text) => handleInputChange('name', text)}
      />
      
      <TextInput
        style={styles.input}
        placeholder="Email *"
        value={formData.email}
        onChangeText={(text) => handleInputChange('email', text)}
        keyboardType="email-address"
        autoCapitalize="none"
      />
      
      <TextInput
        style={styles.input}
        placeholder="Password *"
        value={formData.password}
        onChangeText={(text) => handleInputChange('password', text)}
        secureTextEntry
      />
      
      <TextInput
        style={styles.input}
        placeholder="Phone Number"
        value={formData.phone}
        onChangeText={(text) => handleInputChange('phone', text)}
        keyboardType="phone-pad"
      />
      
      <TextInput
        style={styles.input}
        placeholder="Location"
        value={formData.location}
        onChangeText={(text) => handleInputChange('location', text)}
      />
      
      <TextInput
        style={styles.input}
        placeholder="Address"
        value={formData.address}
        onChangeText={(text) => handleInputChange('address', text)}
        multiline
      />
      
      <View style={styles.pickerContainer}>
        <Text style={styles.label}>Customer Type:</Text>
        <View style={styles.radioContainer}>
          <TouchableOpacity 
            style={styles.radioButton}
            onPress={() => handleInputChange('customerType', 'individual')}
          >
            <View style={[styles.radioCircle, formData.customerType === 'individual' && styles.selectedRadio]} />
            <Text style={styles.radioText}>Individual</Text>
          </TouchableOpacity>
          
          <TouchableOpacity 
            style={styles.radioButton}
            onPress={() => handleInputChange('customerType', 'business')}
          >
            <View style={[styles.radioCircle, formData.customerType === 'business' && styles.selectedRadio]} />
            <Text style={styles.radioText}>Business</Text>
          </TouchableOpacity>
        </View>
      </View>
      
      {formData.customerType === 'business' && (
        <>
          <TextInput
            style={styles.input}
            placeholder="Business Name"
            value={formData.businessName}
            onChangeText={(text) => handleInputChange('businessName', text)}
          />
          
          <TextInput
            style={styles.input}
            placeholder="Business License Number"
            value={formData.businessLicense}
            onChangeText={(text) => handleInputChange('businessLicense', text)}
          />
        </>
      )}
      
      <TextInput
        style={styles.input}
        placeholder="Maximum Budget (Optional)"
        value={formData.maxBudget}
        onChangeText={(text) => handleInputChange('maxBudget', text)}
        keyboardType="numeric"
      />
      
      <TouchableOpacity 
        style={[styles.button, loading && styles.buttonDisabled]} 
        onPress={handleRegister}
        disabled={loading}
      >
        {loading ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <Text style={styles.buttonText}>Register</Text>
        )}
      </TouchableOpacity>
    </View>
  );

  return (
    <SafeAreaView style={styles.container}>
      <KeyboardAvoidingView 
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        style={styles.keyboardAvoidingView}
      >
        <ScrollView contentContainerStyle={styles.scrollContainer}>
          {isLogin ? renderLoginForm() : renderRegisterForm()}
          
          <TouchableOpacity 
            style={styles.switchButton} 
            onPress={toggleAuthMode}
          >
            <Text style={styles.switchText}>
              {isLogin 
                ? "Don't have an account? Register here" 
                : "Already have an account? Login here"
              }
            </Text>
          </TouchableOpacity>
          
          <TouchableOpacity 
            style={styles.backButton} 
            onPress={() => router.back()}
          >
            <Text style={styles.backText}>← Back to Home</Text>
          </TouchableOpacity>
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
  scrollContainer: {
    flexGrow: 1,
    justifyContent: 'center',
    padding: 20,
  },
  formContainer: {
    backgroundColor: '#fff',
    padding: 20,
    borderRadius: 10,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 3.84,
    elevation: 5,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 20,
    textAlign: 'center',
    color: '#2E7D32',
  },
  input: {
    borderWidth: 1,
    borderColor: '#ddd',
    padding: 12,
    marginBottom: 15,
    borderRadius: 8,
    fontSize: 16,
  },
  label: {
    fontSize: 16,
    fontWeight: '500',
    marginBottom: 10,
    color: '#333',
  },
  pickerContainer: {
    marginBottom: 15,
  },
  radioContainer: {
    flexDirection: 'row',
    justifyContent: 'space-around',
  },
  radioButton: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  radioCircle: {
    height: 20,
    width: 20,
    borderRadius: 10,
    borderWidth: 2,
    borderColor: '#2E7D32',
    marginRight: 8,
  },
  selectedRadio: {
    backgroundColor: '#2E7D32',
  },
  radioText: {
    fontSize: 16,
    color: '#333',
  },
  button: {
    backgroundColor: '#2E7D32',
    padding: 15,
    borderRadius: 8,
    alignItems: 'center',
    marginTop: 10,
  },
  buttonDisabled: {
    backgroundColor: '#ccc',
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
  },
  switchButton: {
    marginTop: 20,
    alignItems: 'center',
  },
  switchText: {
    color: '#2E7D32',
    fontSize: 16,
    textDecorationLine: 'underline',
  },
  backButton: {
    marginTop: 15,
    alignItems: 'center',
  },
  backText: {
    color: '#666',
    fontSize: 16,
  },
});

export default CustomerAuth;