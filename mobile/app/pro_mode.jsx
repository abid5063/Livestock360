import React, { useState, useEffect } from 'react';
import { 
  View, 
  Text, 
  StyleSheet, 
  ScrollView, 
  TouchableOpacity, 
  Alert,
  ActivityIndicator,
  Platform,
  Share,
  Linking
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import axios from 'axios';
import { useLanguage } from '../utils/LanguageContext';
import { useTranslation } from 'react-i18next';
import { deductTokens, checkTokenSufficiency, TOKEN_COSTS } from '../utils/tokenUtils';
import * as FileSystem from 'expo-file-system';

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
      // Step 1: Check if user has sufficient tokens
      const tokenCheck = await checkTokenSufficiency(TOKEN_COSTS.PRO_MODE);
      
      if (!tokenCheck.sufficient) {
        Alert.alert(
          t('proMode.insufficientTokens'), 
          tokenCheck.message + '. Please purchase more tokens to continue.'
        );
        setLoading(false);
        return;
      }

      // Step 2: Deduct tokens before making the ML API call
      const deductionResult = await deductTokens(TOKEN_COSTS.PRO_MODE, 'Pro Mode Disease Prediction');
      
      if (!deductionResult.success) {
        Alert.alert(t('proMode.tokenError'), deductionResult.message);
        setLoading(false);
        return;
      }

      console.log(`✅ Tokens deducted successfully. New balance: ${deductionResult.newBalance}`);

      // Step 3: Create symptoms object with all important symptoms
      const symptomsObject = {};
      IMPORTANT_SYMPTOMS.forEach(symptom => {
        symptomsObject[symptom] = selectedSymptoms[symptom] || 0;
      });

      const symptomsData = {
        symptoms: symptomsObject
      };

      console.log('Sending symptoms data:', JSON.stringify(symptomsData, null, 2));

      // Step 4: Call the ML API
      const response = await axios.post(
        'http://52.184.80.117:80/predict',
        symptomsData,
        {
          headers: {
            'Content-Type': 'application/json'
          }
        }
      );

      console.log('API Response:', response.data);
      setPrediction(response.data);
      
      // Show success message with new token balance
      Alert.alert(
        t('proMode.predictionComplete'), 
        `${t('proMode.tokensUsed')}: ${TOKEN_COSTS.PRO_MODE}. ${t('proMode.remainingTokens')}: ${deductionResult.newBalance}`
      );
      
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

  const generatePDFReport = async () => {
    if (!prediction) {
      Alert.alert(t('proMode.error'), 'No prediction data available to export');
      return;
    }

    try {
      // Get selected symptoms for the report
      const selectedSymptomsArray = Object.entries(selectedSymptoms)
        .filter(([_, value]) => value === 1)
        .map(([symptom, _]) => symptom.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase()));

      const currentDate = new Date().toLocaleDateString();
      const currentTime = new Date().toLocaleTimeString();

      // Check if we're running on web platform
      const isWeb = Platform.OS === 'web';

      // Create professional HTML content for PDF conversion
      const htmlContent = `
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Livestock Disease Prediction Report</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: 'Arial', sans-serif;
            line-height: 1.6;
            color: #333;
            max-width: 800px;
            margin: 0 auto;
            padding: 20px;
            background-color: #fff;
        }
        
        .header {
            text-align: center;
            border-bottom: 3px solid #4a89dc;
            padding-bottom: 20px;
            margin-bottom: 30px;
            background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
            padding: 30px;
            border-radius: 10px;
        }
        
        .logo {
            font-size: 28px;
            font-weight: bold;
            color: #4a89dc;
            margin-bottom: 10px;
        }
        
        .title {
            font-size: 22px;
            font-weight: bold;
            color: #2c3e50;
            margin-bottom: 10px;
        }
        
        .subtitle {
            color: #7f8c8d;
            font-size: 14px;
            margin-bottom: 5px;
        }
        
        .section {
            margin-bottom: 30px;
            page-break-inside: avoid;
        }
        
        .section-title {
            font-size: 18px;
            font-weight: bold;
            color: #2c3e50;
            border-bottom: 2px solid #3498db;
            padding-bottom: 8px;
            margin-bottom: 15px;
            background-color: #ecf0f1;
            padding-left: 10px;
            padding-right: 10px;
            padding-top: 10px;
        }
        
        .symptoms-container {
            background-color: #f8f9fa;
            padding: 20px;
            border-radius: 8px;
            border-left: 4px solid #3498db;
        }
        
        .symptoms-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 12px;
            margin-top: 10px;
        }
        
        .symptom-item {
            background-color: #fff;
            padding: 8px 12px;
            border-radius: 20px;
            border: 1px solid #bdc3c7;
            font-size: 14px;
            text-align: center;
            box-shadow: 0 1px 3px rgba(0,0,0,0.1);
        }
        
        .prediction-container {
            background: linear-gradient(135deg, #e8f5e8 0%, #d5e8d5 100%);
            border: 2px solid #27ae60;
            border-radius: 12px;
            padding: 25px;
            text-align: center;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
        }
        
        .diagnosis {
            font-size: 24px;
            font-weight: bold;
            color: #27ae60;
            margin-bottom: 15px;
            text-transform: capitalize;
            text-shadow: 1px 1px 2px rgba(0,0,0,0.1);
        }
        
        .status {
            font-size: 18px;
            color: #2c3e50;
            background-color: #fff;
            padding: 8px 16px;
            border-radius: 20px;
            display: inline-block;
            border: 1px solid #27ae60;
            font-weight: 500;
        }
        
        .disclaimer {
            background-color: #fff3cd;
            border: 2px solid #f39c12;
            border-radius: 10px;
            padding: 20px;
            margin: 20px 0;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        
        .disclaimer-title {
            font-weight: bold;
            color: #e67e22;
            font-size: 16px;
            margin-bottom: 10px;
            display: flex;
            align-items: center;
        }
        
        .disclaimer-text {
            color: #8b4513;
            line-height: 1.7;
            font-size: 14px;
        }
        
        .next-steps {
            background-color: #e3f2fd;
            border-left: 4px solid #2196f3;
            padding: 20px;
            border-radius: 8px;
        }
        
        .steps-list {
            list-style-type: none;
            padding-left: 0;
        }
        
        .steps-list li {
            padding: 5px 0;
            position: relative;
            padding-left: 25px;
        }
        
        .steps-list li:before {
            content: "✓";
            position: absolute;
            left: 0;
            color: #27ae60;
            font-weight: bold;
        }
        
        .footer {
            margin-top: 40px;
            text-align: center;
            padding: 20px;
            background-color: #34495e;
            color: #fff;
            border-radius: 8px;
        }
        
        .footer-title {
            font-size: 16px;
            font-weight: bold;
            margin-bottom: 10px;
        }
        
        .footer-text {
            font-size: 12px;
            opacity: 0.9;
        }
        
        @media print {
            body { 
                margin: 0;
                box-shadow: none;
            }
            .header {
                background: #f5f7fa !important;
                -webkit-print-color-adjust: exact;
            }
        }
    </style>
</head>
<body>
    <div class="header">
        <div class="logo">🏥 LIVESTOCK360</div>
        <div class="title">Disease Prediction Report</div>
        <div class="subtitle">Generated on ${currentDate} at ${currentTime}</div>
        <div class="subtitle">Professional AI-Assisted Diagnosis</div>
    </div>

    <div class="section">
        <div class="section-title">📋 Selected Symptoms</div>
        <div class="symptoms-container">
            <p><strong>Total Symptoms Analyzed:</strong> ${selectedSymptomsArray.length}</p>
            <div class="symptoms-grid">
                ${selectedSymptomsArray.map(symptom => `<div class="symptom-item">• ${symptom}</div>`).join('')}
            </div>
        </div>
    </div>

    <div class="section">
        <div class="section-title">🔍 AI Prediction Result</div>
        <div class="prediction-container">
            <div class="diagnosis">${prediction.prognosis}</div>
            <div class="status">Status: ${prediction.status}</div>
        </div>
    </div>

    <div class="section">
        <div class="section-title">📞 Recommended Next Steps</div>
        <div class="next-steps">
            <ul class="steps-list">
                <li>Contact a licensed veterinarian immediately for professional consultation</li>
                <li>Provide this report to your veterinarian along with additional observations</li>
                <li>Follow all professional medical guidance and treatment recommendations</li>
                <li>Monitor your livestock closely and document any changes in condition</li>
                <li>Keep detailed records of symptoms and treatments for future reference</li>
                <li>Consider isolating affected animals if recommended by your veterinarian</li>
            </ul>
        </div>
    </div>

    <div class="disclaimer">
        <div class="disclaimer-title">⚠️ Important Medical Disclaimer</div>
        <div class="disclaimer-text">
            This prediction is generated by an artificial intelligence system and is provided for reference purposes only. 
            It should not be considered as a substitute for professional veterinary diagnosis and treatment. 
            Always consult with a qualified and licensed veterinarian for proper diagnosis, treatment recommendations, and ongoing care of your livestock. 
            The accuracy of AI predictions may vary, and professional veterinary examination remains essential for proper animal healthcare.
        </div>
    </div>

    <div class="footer">
        <div class="footer-title">Livestock360 - Pro Mode Analysis</div>
        <div class="footer-text">
            This report was generated using advanced AI technology for livestock health assessment.<br>
            For technical support or questions about this report, please contact our support team.<br>
            Emergency veterinary services: Contact your local veterinary clinic immediately.
        </div>
    </div>
</body>
</html>`;

      // Generate timestamp for filename
      const timestamp = new Date().toISOString().replace(/:/g, '-').split('.')[0];
      const filename = `Livestock_Diagnosis_Report_${timestamp}.html`;

      if (isWeb) {
        // Web platform: Create downloadable file
        const blob = new Blob([htmlContent], { type: 'text/html' });
        const url = URL.createObjectURL(blob);
        
        // Create a temporary link element and trigger download
        const link = document.createElement('a');
        link.href = url;
        link.download = filename;
        link.style.display = 'none';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);

        // Show success message for web
        Alert.alert(
          'PDF Report Downloaded Successfully! 📄',
          `Your professional livestock diagnosis report has been downloaded as:\n\n${filename}\n\nTo convert to PDF:\n1. Open the downloaded HTML file in your browser\n2. Use browser's "Print" option (Ctrl+P)\n3. Select "Save as PDF" as destination\n4. Save to your desired location`
        );

      } else {
        // Mobile platform: Use FileSystem
        const fileUri = FileSystem.documentDirectory + filename;
        await FileSystem.writeAsStringAsync(fileUri, htmlContent, {
          encoding: FileSystem.EncodingType.UTF8,
        });

        // Show options for accessing the report on mobile
      Alert.alert(
        'PDF Report Generated Successfully! 📄',
        `Your professional livestock diagnosis report has been created and saved.\n\nFilename: ${filename}\n\nChoose how you'd like to access it:`,
        [
          { text: 'View File Location', onPress: () => {
              Alert.alert(
                'File Location 📁', 
                `Your report is saved at:\n\n${fileUri}\n\nTo convert to PDF:\n\n1. Open this file in any browser\n2. Use browser's "Print" option\n3. Select "Save as PDF"\n4. Choose your desired location\n\nThe file is also accessible through your device's file manager.`
              );
            }
          },
          { 
            text: 'Share Report 📤', 
            onPress: async () => {
              try {
                // Share the HTML file
                await Share.share({
                  url: fileUri,
                  title: 'Livestock Disease Diagnosis Report',
                  message: 'Livestock disease diagnosis report generated by Livestock360 Pro Mode. Open in browser and save as PDF for best results.'
                });
              } catch (shareError) {
                console.error('Error sharing file:', shareError);
                Alert.alert('Share Options', 'File saved to device storage. You can find it in your file manager and share from there.');
              }
            }
          },
          { 
            text: 'Open in Browser 🌐', 
            onPress: async () => {
              try {
                // Try to open the HTML file in default browser
                const canOpen = await Linking.canOpenURL(fileUri);
                if (canOpen) {
                  await Linking.openURL(fileUri);
                } else {
                  // If direct opening fails, provide instructions
                  Alert.alert(
                    'Manual Opening Required',
                    `Please manually open the file:\n\n${fileUri}\n\nUsing your device's file manager, then select "Open with Browser" to view and save as PDF.`
                  );
                }
              } catch (openError) {
                console.error('Error opening file:', openError);
                Alert.alert(
                  'Opening Instructions',
                  `File saved successfully!\n\nTo open:\n1. Go to your file manager\n2. Navigate to Downloads or Documents\n3. Find: ${filename}\n4. Open with any browser\n5. Use Print > Save as PDF`
                );
              }
            }
          }
        ]
      );
      }

    } catch (error) {
      console.error('Error generating PDF report:', error);
      Alert.alert(
        t('proMode.error'), 
        'Failed to generate PDF report. Please try again.'
      );
    }
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
            
            <TouchableOpacity
              style={styles.downloadButton}
              onPress={generatePDFReport}
              testID="download-report-button"
            >
              <Ionicons name="document-text-outline" size={20} color="#fff" />
              <Text style={styles.downloadButtonText}>
                Download PDF Report
              </Text>
            </TouchableOpacity>
            
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
  downloadButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#4a89dc',
    padding: 12,
    borderRadius: 8,
    marginBottom: 12,
    gap: 8,
  },
  downloadButtonText: {
    color: '#fff',
    fontWeight: 'bold',
    fontSize: 16,
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
