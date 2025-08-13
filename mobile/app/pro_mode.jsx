import React, { useState, useEffect } from 'react';
import { 
  View, 
  Text, 
  StyleSheet, 
  ScrollView, 
  TouchableOpacity, 
  Alert,
  ActivityIndicator
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import axios from 'axios';
import { useLanguage } from '../utils/LanguageContext';
import { useTranslation } from 'react-i18next';

const IMPORTANT_SYMPTOMS = [
  "fever",
  "loss_of_appetite", 
  "lethargy",
  "coughing",
  "diarrhoea",
  "dehydration",
  "lameness",
  "milk_fever",
  "pneumonia",
  "weight_loss"
];

export default function ProMode() {
  const router = useRouter();
  const { language } = useLanguage();
  const { t, i18n } = useTranslation();

  // Update i18n language when language changes
  useEffect(() => {
    i18n.changeLanguage(language);
  }, [language, i18n]);

  const [selectedSymptoms, setSelectedSymptoms] = useState({});
  const [loading, setLoading] = useState(false);
  const [prediction, setPrediction] = useState(null);

  const toggleSymptom = (symptom) => {
    setSelectedSymptoms(prev => ({
      ...prev,
      [symptom]: prev[symptom] ? 0 : 1
    }));
  };

  const predictDisease = async () => {
    const hasSelectedSymptoms = Object.values(selectedSymptoms).some(value => value === 1);
    
    if (!hasSelectedSymptoms) {
      Alert.alert(t('proMode.noSymptomsSelected'), t('proMode.selectAtLeastOneSymptom'));
      return;
    }

    setLoading(true);
    setPrediction(null);

    try {
      // Create symptoms object with all important symptoms
      const symptomsObject = {};
      IMPORTANT_SYMPTOMS.forEach(symptom => {
        symptomsObject[symptom] = selectedSymptoms[symptom] || 0;
      });

      const symptomsData = {
        symptoms: symptomsObject
      };

      console.log('Sending symptoms data:', JSON.stringify(symptomsData, null, 2));

      const response = await axios.post(
        'http://104.214.178.145:80/predict',
        symptomsData,
        {
          headers: {
            'Content-Type': 'application/json'
          }
        }
      );

      console.log('API Response:', response.data);
      setPrediction(response.data);
    } catch (error) {
      console.error('Prediction error:', error);
      Alert.alert(
        t('proMode.error'), 
        error.response?.data?.message || t('proMode.failedToGetPrediction')
      );
    } finally {
      setLoading(false);
    }
  };

  const clearSelection = () => {
    setSelectedSymptoms({});
    setPrediction(null);
  };

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} testID="back-button">
          <Ionicons name="arrow-back" size={28} color="#4a89dc" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>{t('proMode.headerTitle')}</Text>
      </View>

      <ScrollView style={styles.content} contentContainerStyle={styles.scrollContent}>
        <Text style={styles.instructionText}>
          {t('proMode.instructionText')}
        </Text>

        <View style={styles.symptomsContainer}>
          {IMPORTANT_SYMPTOMS.map((symptom) => (
            <TouchableOpacity
              key={symptom}
              style={[
                styles.symptomItem,
                selectedSymptoms[symptom] === 1 && styles.selectedSymptom
              ]}
              onPress={() => toggleSymptom(symptom)}
              testID={`symptom-${symptom}`}
            >
              <View style={styles.checkbox}>
                {selectedSymptoms[symptom] === 1 && (
                  <Ionicons name="checkmark" size={16} color="#fff" />
                )}
              </View>
              <Text style={[
                styles.symptomText,
                selectedSymptoms[symptom] === 1 && styles.selectedSymptomText
              ]}>
                {symptom.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase())}
              </Text>
            </TouchableOpacity>
          ))}
        </View>

        <View style={styles.buttonContainer}>
          <TouchableOpacity
            style={styles.clearButton}
            onPress={clearSelection}
            testID="clear-button"
          >
            <Text style={styles.clearButtonText}>{t('proMode.clearAll')}</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.predictButton, loading && styles.disabledButton]}
            onPress={predictDisease}
            disabled={loading}
            testID="predict-button"
          >
            {loading ? (
              <ActivityIndicator color="#fff" size="small" />
            ) : (
              <Text style={styles.predictButtonText}>{t('proMode.predictDisease')}</Text>
            )}
          </TouchableOpacity>
        </View>

        {prediction && (
          <View style={styles.resultContainer}>
            <Text style={styles.resultTitle}>{t('proMode.predictionResult')}</Text>
            <View style={styles.resultCard}>
              <Text style={styles.diagnosisLabel}>{t('proMode.diagnosis')}</Text>
              <Text style={styles.diagnosisText}>{prediction.prognosis}</Text>
              <Text style={styles.statusLabel}>{t('proMode.status')}</Text>
              <Text style={[
                styles.statusText,
                prediction.status === 'success' ? styles.successStatus : styles.errorStatus
              ]}>
                {prediction.status}
              </Text>
            </View>
            <Text style={styles.recommendationText}>
              {t('proMode.recommendationText')}
            </Text>
          </View>
        )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f7f9fa',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingTop: 50,
    paddingBottom: 16,
    paddingHorizontal: 16,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  headerTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#043f8dff',
    marginLeft: 16,
    flex: 1,
  },
  content: {
    flex: 1,
  },
  scrollContent: {
    padding: 16,
  },
  instructionText: {
    fontSize: 16,
    color: '#333',
    marginBottom: 20,
    textAlign: 'center',
    fontWeight: '500',
  },
  symptomsContainer: {
    marginBottom: 20,
  },
  symptomItem: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#fff',
    padding: 12,
    marginBottom: 8,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#e0e0e0',
  },
  selectedSymptom: {
    backgroundColor: '#4a89dc',
    borderColor: '#4a89dc',
  },
  checkbox: {
    width: 20,
    height: 20,
    borderRadius: 4,
    borderWidth: 2,
    borderColor: '#ccc',
    marginRight: 12,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'transparent',
  },
  selectedSymptomText: {
    color: '#fff',
    fontWeight: '600',
  },
  symptomText: {
    fontSize: 15,
    color: '#333',
    flex: 1,
  },
  buttonContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 20,
    gap: 10,
  },
  clearButton: {
    flex: 1,
    backgroundColor: '#780f04ff',
    padding: 12,
    borderRadius: 8,
    alignItems: 'center',
  },
  clearButtonText: {
    color: '#fff',
    fontWeight: 'bold',
    fontSize: 16,
  },
  predictButton: {
    flex: 1,
    backgroundColor: '#065f2bff',
    padding: 12,
    borderRadius: 8,
    alignItems: 'center',
  },
  disabledButton: {
    backgroundColor: '#95a5a6',
  },
  predictButtonText: {
    color: '#fff',
    fontWeight: 'bold',
    fontSize: 16,
  },
  resultContainer: {
    marginTop: 20,
  },
  resultTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 12,
    textAlign: 'center',
  },
  resultCard: {
    backgroundColor: '#fff',
    padding: 16,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: '#e0e0e0',
    marginBottom: 12,
  },
  diagnosisLabel: {
    fontSize: 14,
    color: '#666',
    marginBottom: 4,
  },
  diagnosisText: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#2c3e50',
    marginBottom: 12,
    textTransform: 'capitalize',
  },
  statusLabel: {
    fontSize: 14,
    color: '#666',
    marginBottom: 4,
  },
  statusText: {
    fontSize: 16,
    fontWeight: 'bold',
    textTransform: 'capitalize',
  },
  successStatus: {
    color: '#025a27ff',
  },
  errorStatus: {
    color: '#9b1304ff',
  },
  recommendationText: {
    fontSize: 14,
    color: '#964a07ff',
    textAlign: 'center',
    fontStyle: 'italic',
    backgroundColor: '#fff3cd',
    padding: 12,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#ffeaa7',
  },
});
