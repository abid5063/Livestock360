import AsyncStorage from '@react-native-async-storage/async-storage';
import axios from 'axios';
import { API_BASE_URL } from './apiConfig';

/**
 * Deduct tokens from user's account
 * @param {number} amount - Number of tokens to deduct
 * @param {string} featureUsed - Name of the feature using tokens
 * @returns {Promise<{success: boolean, newBalance?: number, message?: string}>}
 */
export const deductTokens = async (amount, featureUsed) => {
  try {
    // Get user token and data
    const [authToken, userData] = await Promise.all([
      AsyncStorage.getItem('authToken'),
      AsyncStorage.getItem('userData')
    ]);

    if (!authToken || !userData) {
      return {
        success: false,
        message: 'Authentication required. Please login again.'
      };
    }

    const user = JSON.parse(userData);
    const userId = user.id || user._id;

    if (!userId) {
      return {
        success: false,
        message: 'User ID not found. Please login again.'
      };
    }

    // Call the Spring Boot backend to deduct tokens
    const response = await axios.post(
      `${API_BASE_URL}/api/auth/farmers/deduct-tokens`,
      {
        userId: userId,
        amount: amount,
        featureUsed: featureUsed
      },
      {
        headers: {
          'Authorization': `Bearer ${authToken}`,
          'Content-Type': 'application/json'
        }
      }
    );

    if (response.data.success) {
      return {
        success: true,
        newBalance: response.data.newBalance,
        message: `${amount} token(s) deducted successfully`
      };
    } else {
      return {
        success: false,
        message: response.data.message || 'Failed to deduct tokens'
      };
    }

  } catch (error) {
    console.error('Token deduction error:', error);
    
    if (error.response?.status === 401) {
      return {
        success: false,
        message: 'Session expired. Please login again.'
      };
    } else if (error.response?.status === 400) {
      return {
        success: false,
        message: error.response.data.message || 'Insufficient tokens'
      };
    } else {
      return {
        success: false,
        message: 'Network error. Please check your connection and try again.'
      };
    }
  }
};

/**
 * Get user's current token balance
 * @returns {Promise<{success: boolean, balance?: number, message?: string}>}
 */
export const getTokenBalance = async () => {
  try {
    const authToken = await AsyncStorage.getItem('authToken');
    
    if (!authToken) {
      return {
        success: false,
        message: 'Authentication required. Please login again.'
      };
    }

    const response = await axios.get(
      `${API_BASE_URL}/api/auth/farmers/token-balance`,
      {
        headers: {
          'Authorization': `Bearer ${authToken}`,
          'Content-Type': 'application/json'
        }
      }
    );

    if (response.data.success) {
      return {
        success: true,
        balance: response.data.tokenBalance
      };
    } else {
      return {
        success: false,
        message: response.data.message || 'Failed to get token balance'
      };
    }

  } catch (error) {
    console.error('Token balance error:', error);
    return {
      success: false,
      message: 'Failed to retrieve token balance'
    };
  }
};

/**
 * Check if user has sufficient tokens for a feature
 * @param {number} requiredTokens - Number of tokens required
 * @returns {Promise<{sufficient: boolean, currentBalance?: number, message?: string}>}
 */
export const checkTokenSufficiency = async (requiredTokens) => {
  const balanceResult = await getTokenBalance();
  
  if (!balanceResult.success) {
    return {
      sufficient: false,
      message: balanceResult.message
    };
  }

  const sufficient = balanceResult.balance >= requiredTokens;
  
  return {
    sufficient: sufficient,
    currentBalance: balanceResult.balance,
    message: sufficient 
      ? `You have ${balanceResult.balance} tokens available`
      : `Insufficient tokens. You need ${requiredTokens} tokens but only have ${balanceResult.balance}`
  };
};

/**
 * Token costs for different features
 */
export const TOKEN_COSTS = {
  AI_CHATBOT: 1,
  SYMPTOM_CHECKER: 2,
  FOOD_SUGGESTIONS: 2,
  MARKET_ANALYSIS: 3,
  PRO_MODE: 1  // As requested by user
};
