import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  Alert,
  Modal,
  Image,
  TextInput
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import AsyncStorage from '@react-native-async-storage/async-storage';
import axios from 'axios';
import { API_BASE_URL } from '../utils/apiConfig'; // Adjust the import path as needed
import { useLanguage } from '../utils/LanguageContext';
import { useTranslation } from 'react-i18next';
const API_KEY = "AIzaSyCrYK2JHpleJxGT3TtneVT6hZHZY8KC1Vc";
const GEMINI_URL = `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${API_KEY}`;

export default function MarketAnalysis() {
  const router = useRouter();
  const { language } = useLanguage();
  const { t, i18n } = useTranslation();

  // Update i18n language when language changes
  useEffect(() => {
    i18n.changeLanguage(language);
  }, [language, i18n]);

  const [animals, setAnimals] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedAnimal, setSelectedAnimal] = useState(null);
  const [analysisLoading, setAnalysisLoading] = useState(false);
  const [analysis, setAnalysis] = useState(null);
  const [showMainOptions, setShowMainOptions] = useState(false);
  const [showSellOptions, setShowSellOptions] = useState(false);
  const [showBuyOptions, setShowBuyOptions] = useState(false);
  const [showAnalysisModal, setShowAnalysisModal] = useState(false);
  const [buyFormData, setBuyFormData] = useState({
    breed: '',
    animalType: '',
    gender: '',
    weight: '',
    age: '',
    location: ''
  });
  const [analysisError, setAnalysisError] = useState(null);

  useEffect(() => {
    fetchAnimals();
  }, []);

  const fetchAnimals = async () => {
    try {
      const token = await AsyncStorage.getItem('authToken');
      if (!token) {
        router.replace('/');
        return;
      }

      const response = await axios.get(`${API_BASE_URL}/api/animals`, {
        headers: {
          Authorization: `Bearer ${token}`
        }
      });
      setAnimals(response.data);
    } catch (error) {
      Alert.alert(t('marketAnalysis.error'), t('marketAnalysis.failedToFetchAnimals'));
    } finally {
      setLoading(false);
    }
  };

  const getSellAnalysis = async (animal) => {
    setSelectedAnimal(animal);
    setAnalysisLoading(true);
    setAnalysis(null);
    setShowAnalysisModal(true);
    setAnalysisError(null);

    try {
      const languageInstruction = language === 'bn' ? 'Respond ONLY in Bangla (Bengali) language. All market data, prices, and analysis must be in Bangla.' : 'Respond in English language. All market data, prices, and analysis must be in English.';
      
      const prompt = `Create a market analysis for selling ${animal.name} (${animal.type}, ${animal.breed}, ${animal.age} years, ${animal.gender}):\n\n${languageInstruction}\n\nRespond ONLY with a JSON object with the following structure, no extra text or explanation.\n{\n  \"currentMarketPrice\": [\n    {\"factor\": \"string\", \"value\": \"string\", \"priceImpact\": \"number\"}\n  ],\n  \"sellingLocations\": [\n    {\"marketName\": \"string\", \"distance\": \"string\", \"bestPrice\": \"number\", \"peakTime\": \"string\"}\n  ],\n  \"marketTrends\": [\n    {\"trend\": \"string\", \"currentStatus\": \"string\", \"priceDirection\": \"string\", \"duration\": \"string\"}\n  ],\n  \"oneYearPrediction\": [\n    {\"metric\": \"string\", \"current\": \"number\", \"predicted\": \"number\", \"growth\": \"string\"}\n  ],\n  \"sellingRecommendations\": [\n    {\"factor\": \"string\", \"recommendation\": \"string\", \"reason\": \"string\"}\n  ]\n}\nAll prices must be in Bangladeshi Taka (BDT).`;

      const response = await fetch(GEMINI_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          contents: [{
            parts: [{ text: prompt }]
          }]
        }),
      });

      const data = await response.json();
      const aiText = data.candidates?.[0]?.content?.parts?.[0]?.text || t('marketAnalysis.noResponseError');
      let parsed;
      let jsonString = aiText.trim();
      // Remove markdown code blocks if present
      if (jsonString.startsWith('```json')) {
        jsonString = jsonString.replace(/^```json\n/, '').replace(/\n```$/, '');
      } else if (jsonString.startsWith('```')) {
        jsonString = jsonString.replace(/^```\n/, '').replace(/\n```$/, '');
      }
      try {
        parsed = JSON.parse(jsonString);
        setAnalysis(parsed);
      } catch (e) {
        console.error('Parse error:', e);
        console.error('Response text:', aiText);
        setAnalysisError(t('marketAnalysis.tryAgainError'));
        setAnalysis(null);
      }
    } catch (err) {
      setAnalysisError(t('marketAnalysis.tryAgainError'));
      setAnalysis(null);
    } finally {
      setAnalysisLoading(false);
    }
  };

  const getBuyAnalysis = async () => {
    if (!buyFormData.breed || !buyFormData.animalType || !buyFormData.gender || !buyFormData.weight || !buyFormData.age) {
      Alert.alert(t('marketAnalysis.error'), t('marketAnalysis.selectAnimal'));
      return;
    }

    setAnalysisLoading(true);
    setAnalysis(null);
    setShowAnalysisModal(true);
    setAnalysisError(null);

    try {
      const languageInstruction = language === 'bn' ? 'Respond ONLY in Bangla (Bengali) language. All market data, prices, and analysis must be in Bangla.' : 'Respond in English language. All market data, prices, and analysis must be in English.';
      
      const prompt = `Create a market analysis for buying ${buyFormData.animalType} (${buyFormData.breed}, ${buyFormData.age} years, ${buyFormData.gender}, ${buyFormData.weight}kg):\n\n${languageInstruction}\n\nRespond ONLY with a JSON object with the following structure, no extra text or explanation.\n{\n  \"marketPriceRange\": [\n    {\"quality\": \"string\", \"priceRange\": \"string\", \"availability\": \"string\", \"bestTime\": \"string\"}\n  ],\n  \"topBuyingOptions\": [\n    {\"rank\": \"number\", \"breed\": \"string\", \"age\": \"string\", \"weight\": \"string\", \"price\": \"number\", \"location\": \"string\", \"rating\": \"string\"}\n  ],\n  \"priceComparison\": [\n    {\"market\": \"string\", \"averagePrice\": \"number\", \"quality\": \"string\", \"distance\": \"string\", \"recommendation\": \"string\"}\n  ],\n  \"buyingTips\": [\n    {\"factor\": \"string\", \"tip\": \"string\", \"impact\": \"string\"}\n  ],\n  \"marketForecast\": [\n    {\"timePeriod\": \"string\", \"priceTrend\": \"string\", \"supply\": \"string\", \"demand\": \"string\", \"recommendation\": \"string\"}\n  ]\n}\nAll prices must be in Bangladeshi Taka (BDT).`;

      const response = await fetch(GEMINI_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          contents: [{
            parts: [{ text: prompt }]
          }]
        }),
      });

      const data = await response.json();
      const aiText = data.candidates?.[0]?.content?.parts?.[0]?.text || t('marketAnalysis.noResponseError');
      let parsed;
      let jsonString = aiText.trim();
      // Remove markdown code blocks if present
      if (jsonString.startsWith('```json')) {
        jsonString = jsonString.replace(/^```json\n/, '').replace(/\n```$/, '');
      } else if (jsonString.startsWith('```')) {
        jsonString = jsonString.replace(/^```\n/, '').replace(/\n```$/, '');
      }
      try {
        parsed = JSON.parse(jsonString);
        setAnalysis(parsed);
      } catch (e) {
        console.error('Parse error:', e);
        console.error('Response text:', aiText);
        setAnalysisError(t('marketAnalysis.tryAgainError'));
        setAnalysis(null);
      }
    } catch (err) {
      setAnalysisError(t('marketAnalysis.tryAgainError'));
      setAnalysis(null);
    } finally {
      setAnalysisLoading(false);
    }
  };

  const handleInputChange = (key, value) => {
    setBuyFormData({ ...buyFormData, [key]: value });
  };

  // Helper to render a table from JSON array
  // columns: Array<{ key: string, label: string }>, data: Array<Object>
  const renderTable = (columns, data) => (
    <View style={{ marginBottom: 24 }}>
      {/* Header */}
      <View style={{ flexDirection: 'row', backgroundColor: '#e6f0fa', borderTopLeftRadius: 8, borderTopRightRadius: 8 }}>
        {columns.map((col) => (
          <Text key={col.key} style={{ flex: 1, fontWeight: 'bold', padding: 8, color: '#333' }}>{col.label}</Text>
        ))}
      </View>
      {/* Rows */}
      {data.map((row, idx) => (
        <View key={idx} style={{ flexDirection: 'row', backgroundColor: idx % 2 === 0 ? '#fff' : '#f7f9fa' }}>
          {columns.map((col) => (
            <Text key={col.key} style={{ flex: 1, padding: 8, color: '#333' }}>{row[col.key]?.toString()}</Text>
          ))}
        </View>
      ))}
    </View>
  );

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#4a89dc" />
        <Text style={styles.loadingText}>{t('marketAnalysis.loadingAnimals')}</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()}>
          <Ionicons name="arrow-back" size={28} color="#4a89dc" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>{t('marketAnalysis.headerTitle')}</Text>
        <View style={{ width: 28 }} />
      </View>

      <ScrollView contentContainerStyle={styles.scrollContainer}>
        <Text style={styles.description}>
          {t('marketAnalysis.selectAnimal')}
        </Text>

        <View style={styles.mainButtons}>
          <TouchableOpacity
            style={styles.mainButton}
            onPress={() => setShowMainOptions(true)}
          >
            <Ionicons name="trending-up" size={32} color="#27ae60" />
            <Text style={styles.mainButtonText}>{t('marketAnalysis.headerTitle')}</Text>
            <Text style={styles.mainButtonSubtext}>{t('marketAnalysis.selectAnimal')}</Text>
          </TouchableOpacity>
        </View>
      </ScrollView>

      {/* Main Options Modal */}
      <Modal
        animationType="slide"
        transparent={true}
        visible={showMainOptions}
        onRequestClose={() => setShowMainOptions(false)}
      >
        <View style={styles.modalContainer}>
          <View style={styles.modalContent}>
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>{t('marketAnalysis.mainOptions')}</Text>
              <TouchableOpacity
                onPress={() => setShowMainOptions(false)}
                style={styles.closeButton}
              >
                <Ionicons name="close" size={24} color="#666" />
              </TouchableOpacity>
            </View>

            <View style={styles.optionsContainer}>
              <TouchableOpacity
                style={styles.optionButton}
                onPress={() => {
                  setShowMainOptions(false);
                  setShowSellOptions(true);
                }}
              >
                <Ionicons name="cash" size={24} color="#27ae60" />
                <Text style={styles.optionButtonText}>{t('marketAnalysis.sellAnalysis')}</Text>
                <Text style={styles.optionButtonSubtext}>{t('marketAnalysis.selectAnimal')}</Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={styles.optionButton}
                onPress={() => {
                  setShowMainOptions(false);
                  setShowBuyOptions(true);
                }}
              >
                <Ionicons name="cart" size={24} color="#e74c3c" />
                <Text style={styles.optionButtonText}>{t('marketAnalysis.buyAnalysis')}</Text>
                <Text style={styles.optionButtonSubtext}>{t('marketAnalysis.selectAnimal')}</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>

      {/* Sell Options Modal */}
      <Modal
        animationType="slide"
        transparent={true}
        visible={showSellOptions}
        onRequestClose={() => setShowSellOptions(false)}
      >
        <View style={styles.modalContainer}>
          <View style={styles.modalContent}>
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>{t('marketAnalysis.selectAnimalToSell')}</Text>
              <TouchableOpacity
                onPress={() => setShowSellOptions(false)}
                style={styles.closeButton}
              >
                <Ionicons name="close" size={24} color="#666" />
              </TouchableOpacity>
            </View>

            <ScrollView style={styles.animalsList}>
              {animals.length === 0 ? (
                <Text style={styles.noAnimalsText}>{t('marketAnalysis.noAnimalsToSell')}</Text>
              ) : (
                animals.map((animal) => (
                  <TouchableOpacity
                    key={animal._id}
                    style={styles.animalCard}
                    onPress={() => {
                      setShowSellOptions(false);
                      getSellAnalysis(animal);
                    }}
                  >
                    <View style={styles.animalImageContainer}>
                      {animal.photo_url ? (
                        <Image
                          source={{ uri: animal.photo_url }}
                          style={styles.animalImage}
                        />
                      ) : (
                        <View style={[styles.animalImage, styles.noImage]}>
                          <Ionicons name="paw" size={32} color="#ccc" />
                        </View>
                      )}
                    </View>
                    <View style={styles.animalInfo}>
                      <Text style={styles.animalName}>{animal.name}</Text>
                      <Text style={styles.animalDetails}>
                        {animal.type} • {animal.breed} • {animal.age} years
                      </Text>
                      <Text style={styles.animalGender}>{animal.gender}</Text>
                    </View>
                    <View style={styles.arrowContainer}>
                      <Ionicons name="chevron-forward" size={24} color="#4a89dc" />
                    </View>
                  </TouchableOpacity>
                ))
              )}
            </ScrollView>
          </View>
        </View>
      </Modal>

      {/* Buy Options Modal */}
      <Modal
        animationType="slide"
        transparent={true}
        visible={showBuyOptions}
        onRequestClose={() => setShowBuyOptions(false)}
      >
        <View style={styles.modalContainer}>
          <View style={styles.modalContent}>
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>{t('marketAnalysis.buyAnalysis')}</Text>
              <TouchableOpacity
                onPress={() => setShowBuyOptions(false)}
                style={styles.closeButton}
              >
                <Ionicons name="close" size={24} color="#666" />
              </TouchableOpacity>
            </View>

            <ScrollView style={styles.formContainer}>
              <TextInput
                style={styles.input}
                placeholder={t('marketAnalysis.animalTypePlaceholder')}
                value={buyFormData.animalType}
                onChangeText={(text) => handleInputChange('animalType', text)}
              />

              <TextInput
                style={styles.input}
                placeholder={t('marketAnalysis.breedPlaceholder')}
                value={buyFormData.breed}
                onChangeText={(text) => handleInputChange('breed', text)}
              />

              <TextInput
                style={styles.input}
                placeholder={t('marketAnalysis.genderPlaceholder')}
                value={buyFormData.gender}
                onChangeText={(text) => handleInputChange('gender', text)}
              />

              <TextInput
                style={styles.input}
                placeholder={t('marketAnalysis.weightPlaceholder')}
                value={buyFormData.weight}
                onChangeText={(text) => handleInputChange('weight', text)}
                keyboardType="numeric"
              />

              <TextInput
                style={styles.input}
                placeholder={t('marketAnalysis.agePlaceholder')}
                value={buyFormData.age}
                onChangeText={(text) => handleInputChange('age', text)}
                keyboardType="numeric"
              />

              <TextInput
                style={styles.input}
                placeholder={t('marketAnalysis.locationPlaceholder')}
                value={buyFormData.location}
                onChangeText={(text) => handleInputChange('location', text)}
              />

              <TouchableOpacity
                style={styles.analyzeButton}
                onPress={() => {
                  setShowBuyOptions(false);
                  getBuyAnalysis();
                }}
              >
                <Text style={styles.analyzeButtonText}>{t('marketAnalysis.analyze')}</Text>
              </TouchableOpacity>
            </ScrollView>
          </View>
        </View>
      </Modal>

      {/* Analysis Results Modal */}
      <Modal
        animationType="slide"
        transparent={true}
        visible={showAnalysisModal}
        onRequestClose={() => setShowAnalysisModal(false)}
      >
        <View style={styles.modalContainer}>
          <View style={styles.modalContent}>
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>
                {t('marketAnalysis.headerTitle')} {selectedAnimal ? `${t('marketAnalysis.for')} ${selectedAnimal.name}` : ''}
              </Text>
              <TouchableOpacity
                onPress={() => setShowAnalysisModal(false)}
                style={styles.closeButton}
              >
                <Ionicons name="close" size={24} color="#666" />
              </TouchableOpacity>
            </View>

            <ScrollView style={styles.analysisContainer}>
              {analysisLoading ? (
                <View style={styles.loadingAnalysis}>
                  <ActivityIndicator size="large" color="#4a89dc" />
                  <Text style={styles.loadingAnalysisText}>
                    Analyzing market data...
                  </Text>
                </View>
              ) : analysisError ? (
                <View style={{ alignItems: 'center', padding: 20 }}>
                  <Text style={{ color: 'red', marginBottom: 10, fontWeight: 'bold' }}>Error</Text>
                  <Text style={{ color: '#333', marginBottom: 10 }}>{analysisError}</Text>
                  <TouchableOpacity style={{ backgroundColor: '#4a89dc', padding: 10, borderRadius: 6 }} onPress={() => {
                    setAnalysisError(null);
                    if (selectedAnimal) getSellAnalysis(selectedAnimal);
                    else getBuyAnalysis();
                  }}>
                    <Text style={{ color: '#fff', fontWeight: 'bold' }}>Retry</Text>
                  </TouchableOpacity>
                </View>
              ) : analysis && typeof analysis === 'object' ? (
                // Render tables for each section
                <View>
                  {/* Sell Analysis Tables */}
                  {analysis.currentMarketPrice && renderTable([
                    { key: 'factor', label: 'কারণ' },
                    { key: 'value', label: 'মান' },
                    { key: 'priceImpact', label: 'মূল্য প্রভাব (৳)' },
                  ], analysis.currentMarketPrice)}
                  {analysis.sellingLocations && renderTable([
                    { key: 'marketName', label: 'বাজারের নাম' },
                    { key: 'distance', label: 'দূরত্ব' },
                    { key: 'bestPrice', label: 'সেরা মূল্য (৳)' },
                    { key: 'peakTime', label: 'শীর্ষ সময়' },
                  ], analysis.sellingLocations)}
                  {analysis.marketTrends && renderTable([
                    { key: 'trend', label: 'প্রবণতা' },
                    { key: 'currentStatus', label: 'বর্তমান অবস্থা' },
                    { key: 'priceDirection', label: 'মূল্য প্রবাহ' },
                    { key: 'duration', label: 'সময়কাল' },
                  ], analysis.marketTrends)}
                  {analysis.oneYearPrediction && renderTable([
                    { key: 'metric', label: 'মেট্রিক' },
                    { key: 'current', label: 'বর্তমান (৳)' },
                    { key: 'predicted', label: 'পূর্বাভাস (৳)' },
                    { key: 'growth', label: 'বৃদ্ধি' },
                  ], analysis.oneYearPrediction)}
                  {analysis.sellingRecommendations && renderTable([
                    { key: 'factor', label: 'কারণ' },
                    { key: 'recommendation', label: 'সুপারিশ' },
                    { key: 'reason', label: 'কারণ' },
                  ], analysis.sellingRecommendations)}
                  {/* Buy Analysis Tables */}
                  {analysis.marketPriceRange && renderTable([
                    { key: 'quality', label: 'গুণমান' },
                    { key: 'priceRange', label: 'মূল্য সীমা (৳)' },
                    { key: 'availability', label: 'উপলব্ধতা' },
                    { key: 'bestTime', label: 'সেরা সময়' },
                  ], analysis.marketPriceRange)}
                  {analysis.topBuyingOptions && renderTable([
                    { key: 'rank', label: 'ক্রম' },
                    { key: 'breed', label: 'বংশ' },
                    { key: 'age', label: 'বয়স' },
                    { key: 'weight', label: 'ওজন' },
                    { key: 'price', label: 'মূল্য (৳)' },
                    { key: 'location', label: 'অবস্থান' },
                    { key: 'rating', label: 'রেটিং' },
                  ], analysis.topBuyingOptions)}
                  {analysis.priceComparison && renderTable([
                    { key: 'market', label: 'বাজার' },
                    { key: 'averagePrice', label: 'গড় মূল্য (৳)' },
                    { key: 'quality', label: 'গুণমান' },
                    { key: 'distance', label: 'দূরত্ব' },
                    { key: 'recommendation', label: 'সুপারিশ' },
                  ], analysis.priceComparison)}
                  {analysis.buyingTips && renderTable([
                    { key: 'factor', label: 'কারণ' },
                    { key: 'tip', label: 'পরামর্শ' },
                    { key: 'impact', label: 'প্রভাব' },
                  ], analysis.buyingTips)}
                  {analysis.marketForecast && renderTable([
                    { key: 'timePeriod', label: 'সময়কাল' },
                    { key: 'priceTrend', label: 'মূল্য প্রবণতা' },
                    { key: 'supply', label: 'সরবরাহ' },
                    { key: 'demand', label: 'চাহিদা' },
                    { key: 'recommendation', label: 'সুপারিশ' },
                  ], analysis.marketForecast)}
                </View>
              ) : analysis ? (
                <Text style={styles.analysisText}>{analysis}</Text>
              ) : null}
            </ScrollView>
          </View>
        </View>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f7f9fa',
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#f7f9fa',
  },
  loadingText: {
    marginTop: 15,
    fontSize: 16,
    color: '#4a89dc',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingTop: 50,
    paddingBottom: 16,
    paddingHorizontal: 16,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  headerTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333',
  },
  scrollContainer: {
    padding: 20,
  },
  description: {
    fontSize: 16,
    color: '#666',
    marginBottom: 24,
    lineHeight: 22,
  },
  mainButtons: {
    gap: 16,
  },
  mainButton: {
    backgroundColor: '#fff',
    padding: 24,
    borderRadius: 16,
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  mainButtonText: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
    marginTop: 12,
  },
  mainButtonSubtext: {
    fontSize: 14,
    color: '#666',
    marginTop: 4,
    textAlign: 'center',
  },
  modalContainer: {
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
    padding: 0,
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
    flex: 1,
  },
  closeButton: {
    padding: 4,
  },
  optionsContainer: {
    padding: 20,
    gap: 16,
  },
  optionButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#fff',
    padding: 16,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#eee',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  optionButtonText: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333',
    marginLeft: 12,
    flex: 1,
  },
  optionButtonSubtext: {
    fontSize: 12,
    color: '#666',
    marginLeft: 12,
    marginTop: 2,
  },
  animalsList: {
    padding: 20,
    gap: 12,
  },
  animalCard: {
    flexDirection: 'row',
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  animalImageContainer: {
    marginRight: 16,
  },
  animalImage: {
    width: 60,
    height: 60,
    borderRadius: 30,
  },
  noImage: {
    backgroundColor: '#f0f0f0',
    justifyContent: 'center',
    alignItems: 'center',
  },
  animalInfo: {
    flex: 1,
  },
  animalName: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 4,
  },
  animalDetails: {
    fontSize: 14,
    color: '#666',
    marginBottom: 2,
  },
  animalGender: {
    fontSize: 14,
    color: '#4a89dc',
    fontWeight: '500',
  },
  arrowContainer: {
    marginLeft: 8,
  },
  noAnimalsText: {
    textAlign: 'center',
    fontSize: 16,
    color: '#666',
    marginTop: 20,
  },
  formContainer: {
    padding: 20,
  },
  input: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
    marginBottom: 16,
    backgroundColor: '#fff',
  },
  analyzeButton: {
    backgroundColor: '#4a89dc',
    paddingVertical: 14,
    borderRadius: 8,
    alignItems: 'center',
    marginTop: 8,
  },
  analyzeButtonText: {
    color: '#fff',
    fontSize: 18,
    fontWeight: 'bold',
  },
  analysisContainer: {
    padding: 20,
  },
  loadingAnalysis: {
    alignItems: 'center',
    paddingVertical: 40,
  },
  loadingAnalysisText: {
    marginTop: 16,
    fontSize: 16,
    color: '#666',
    textAlign: 'center',
  },
  analysisText: {
    fontSize: 16,
    lineHeight: 24,
    color: '#333',
  },
}); 