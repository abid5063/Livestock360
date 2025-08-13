import React, { useState, useEffect } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet, ScrollView, ActivityIndicator, KeyboardAvoidingView, Platform, Image, Alert } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import * as ImagePicker from 'expo-image-picker';
import { useLanguage } from '../utils/LanguageContext';
import { useTranslation } from 'react-i18next';

const API_KEY = "AIzaSyCrYK2JHpleJxGT3TtneVT6hZHZY8KC1Vc";
const GEMINI_URL = `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${API_KEY}`;
const GEMINI_VISION_URL = `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${API_KEY}`;

export default function DiseaseDetection() {
  const router = useRouter();
  const { language } = useLanguage();
  const { t, i18n } = useTranslation();

  // Update i18n language when language changes
  useEffect(() => {
    i18n.changeLanguage(language);
  }, [language, i18n]);

  const [symptoms, setSymptoms] = useState('');
  const [image, setImage] = useState(null);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);

  const pickImage = async () => {
    const { status } = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (status !== 'granted') {
      Alert.alert(t('symptomChecker.permissionRequired'), t('symptomChecker.cameraRollPermission'));
      return;
    }
    let result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      allowsEditing: true,
      aspect: [4, 3],
      quality: 0.7,
      base64: true,
    });
    if (!result.canceled) {
      setImage(result.assets[0]);
    }
  };

  const handleAnalyze = async () => {
    if (!symptoms.trim() && !image) {
      setResult({ error: t('symptomChecker.enterSymptomsOrPhoto') });
      return;
    }
    setLoading(true);
    setResult(null);
    try {
      const languageInstruction = language === 'bn' ? 'Respond ONLY in Bangla (Bengali) language. All text must be in Bangla.' : 'Respond in English language. All text must be in English.';
      
      const parts = [];
      let promptText = '';
      
      if (symptoms.trim()) {
        promptText = `Symptoms: ${symptoms}\n\n${languageInstruction}\n\nAnalyze the symptoms and provide:\n- Possible diseases or conditions\n- Recommended actions\n- When to consult a veterinarian\n\nFormat your response with bullet points (•) and avoid using bold text. Keep it clear and concise.`;
      } else {
        promptText = `${languageInstruction}\n\nAnalyze the image and provide:\n- Possible diseases or conditions\n- Recommended actions\n- When to consult a veterinarian\n\nFormat your response with bullet points (•) and avoid using bold text. Keep it clear and concise.`;
      }
      
      parts.push({ text: promptText });
      
      if (image) {
        parts.push({
          inlineData: {
            mimeType: image.type || 'image/jpeg',
            data: image.base64
          }
        });
      }
      
      const response = await fetch(GEMINI_VISION_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ contents: [{ parts }] }),
      });
      const data = await response.json();
      const aiText = data.candidates?.[0]?.content?.parts?.[0]?.text;
      if (aiText) {
        // Format the response to ensure proper bullet points
        const formattedText = formatResponse(aiText);
        setResult({ text: formattedText });
      } else {
        setResult({ error: t('symptomChecker.failedToGetResponse') });
      }
    } catch (err) {
      setResult({ error: t('symptomChecker.aiServiceError') });
    } finally {
      setLoading(false);
    }
  };

  const formatResponse = (text) => {
    // Replace various bullet point formats with consistent bullet points
    return text
      .replace(/\*\s/g, '• ') // Replace * with •
      .replace(/-\s/g, '• ') // Replace - with •
      .replace(/\d+\.\s/g, '• ') // Replace numbered lists with •
      .replace(/\*\*/g, '') // Remove bold markers
      .replace(/__/g, '') // Remove underline markers
      .replace(/#{1,6}\s/g, '') // Remove markdown headers
      .trim();
  };

  return (
    <KeyboardAvoidingView
      style={{ flex: 1, backgroundColor: '#f7f9fa' }}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()}>
          <Ionicons name="arrow-back" size={28} color="#073f8aff" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>{t('symptomChecker.headerTitle')}</Text>
        <TouchableOpacity 
          style={styles.proButton}
          onPress={() => router.push('/pro_mode')}
          testID="pro-button"
        >
          <Text style={styles.proButtonText}>{t('symptomChecker.goPro')}</Text>
        </TouchableOpacity>
      </View>
      <ScrollView contentContainerStyle={styles.container}>
        <Text style={styles.label}>{t('symptomChecker.describeSymptoms')}</Text>
        <TextInput
          style={styles.input}
          placeholder={t('symptomChecker.symptomsPlaceholder')}
          value={symptoms}
          onChangeText={setSymptoms}
          multiline
          editable={!loading}
          testID="symptoms-input"
        />
        <TouchableOpacity style={styles.imagePickerButton} onPress={pickImage} disabled={loading} testID="image-picker-button">
          <Text style={styles.imagePickerText}>{image ? t('symptomChecker.changePhoto') : t('symptomChecker.selectPhoto')}</Text>
        </TouchableOpacity>
        {image && (
          <Image source={{ uri: image.uri }} style={styles.previewImage} />
        )}
        <TouchableOpacity
          style={[styles.button, loading && styles.buttonDisabled]}
          onPress={handleAnalyze}
          disabled={loading}
          testID="analyze-button"
        >
          {loading ? (
            <ActivityIndicator color="#fff" />
          ) : (
            <Text style={styles.buttonText}>{t('symptomChecker.analyze')}</Text>
          )}
        </TouchableOpacity>
        {result && (
          <View style={styles.resultContainer} testID="result-container">
            <Text style={styles.resultTitle}>{t('symptomChecker.analysisResult')}</Text>
            {result.text ? (
              <Text style={styles.resultText} testID="result-text">{result.text}</Text>
            ) : (
              <Text style={[styles.resultText, styles.errorText]} testID="error-text">{result.error}</Text>
            )}
          </View>
        )}
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
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
    marginLeft: 16,
    flex: 1,
  },
  proButton: {
    backgroundColor: '#025926ff',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 15,
  },
  proButtonText: {
    color: '#fff',
    fontWeight: 'bold',
    fontSize: 14,
  },
  container: {
    padding: 20,
  },
  label: {
    fontSize: 16,
    fontWeight: '500',
    marginBottom: 10,
    color: '#333',
  },
  input: {
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
    minHeight: 80,
    textAlignVertical: 'top',
    marginBottom: 20,
  },
  imagePickerButton: {
    borderWidth: 1,
    borderColor: '#094390ff',
    borderRadius: 8,
    padding: 12,
    marginBottom: 12,
    alignItems: 'center',
  },
  imagePickerText: {
    color: '#08428fff',
    fontWeight: 'bold',
  },
  previewImage: {
    width: '100%',
    height: 180,
    borderRadius: 8,
    marginBottom: 12,
  },
  button: {
    backgroundColor: '#0f5e30ff',
    paddingVertical: 14,
    borderRadius: 8,
    alignItems: 'center',
  },
  buttonDisabled: {
    backgroundColor: '#95a5a6',
  },
  buttonText: {
    color: '#fff',
    fontSize: 18,
    fontWeight: 'bold',
  },
  resultContainer: {
    marginTop: 30,
    padding: 15,
    backgroundColor: '#fff',
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#eee',
  },
  resultTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    marginBottom: 10,
    color: '#2c3e50',
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
    paddingBottom: 10,
  },
  resultText: {
    fontSize: 16,
    lineHeight: 24,
    color: '#34495e',
  },
  errorText: {
    color: '#801206ff',
    fontWeight: 'bold',
  }
}); 