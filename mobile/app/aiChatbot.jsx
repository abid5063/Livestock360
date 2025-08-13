import React, { useState, useEffect } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet, ScrollView, KeyboardAvoidingView, Platform, ActivityIndicator } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { useLanguage } from '../utils/LanguageContext';
import { useTranslation } from 'react-i18next';

const API_KEY = "AIzaSyCrYK2JHpleJxGT3TtneVT6hZHZY8KC1Vc";
const GEMINI_URL = `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${API_KEY}`;

export default function AIChatbot() {
  const router = useRouter();
  const { language } = useLanguage();
  const { t, i18n } = useTranslation();

  // Update i18n language when language changes
  useEffect(() => {
    i18n.changeLanguage(language);
  }, [language, i18n]);

  const [messages, setMessages] = useState([
    { from: 'bot', text: t('aiChatbot.welcomeMessage'), isFormatted: false }
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);

  // Update welcome message when language changes
  useEffect(() => {
    setMessages(prev => {
      if (prev.length > 0 && prev[0].from === 'bot') {
        const newMessages = [...prev];
        newMessages[0] = { from: 'bot', text: t('aiChatbot.welcomeMessage'), isFormatted: false };
        return newMessages;
      }
      return prev;
    });
  }, [language, t]);

  // Function to parse and format JSON response
  const formatResponse = (jsonText) => {
    try {
      // Try to extract JSON from the response (in case AI includes extra text)
      const jsonMatch = jsonText.match(/\{[\s\S]*\}/);
      if (!jsonMatch) {
        return { text: jsonText, isFormatted: false };
      }

      const jsonData = JSON.parse(jsonMatch[0]);
      
      if (jsonData.response) {
        let formattedText = '';
        
        // Handle different response formats
        if (Array.isArray(jsonData.response)) {
          // If response is an array, format as bullet points
          formattedText = jsonData.response.map(item => `• ${item}`).join('\n');
        } else if (typeof jsonData.response === 'string') {
          // If response is a string, check if it contains bullet points
          if (jsonData.response.includes('•') || jsonData.response.includes('-')) {
            formattedText = jsonData.response;
          } else {
            // Split by sentences and add bullet points
            const sentences = jsonData.response.split(/[.!?]+/).filter(s => s.trim());
            formattedText = sentences.map(sentence => `• ${sentence.trim()}`).join('\n');
          }
        } else if (typeof jsonData.response === 'object') {
          // If response is an object, format key-value pairs
          formattedText = Object.entries(jsonData.response)
            .map(([key, value]) => `• ${key}: ${value}`)
            .join('\n');
        }
        
        return { text: formattedText, isFormatted: true };
      }
      
      return { text: jsonText, isFormatted: false };
    } catch (error) {
      console.log('JSON parsing failed, returning raw text:', error);
      return { text: jsonText, isFormatted: false };
    }
  };

  const sendMessage = async () => {
    if (!input.trim()) return;
    const userMessage = { from: 'user', text: input };
    setMessages(prev => [...prev, userMessage]);
    setInput('');
    setLoading(true);
    try {
      // Choose prompt based on language with JSON format instruction
      const basePrompt = language === 'bn' ? t('aiChatbot.bengaliPrompt') : t('aiChatbot.englishPrompt');
      
      const prompt = `
${basePrompt}

IMPORTANT: Please respond in the following JSON format:
{
  "response": "Your detailed response here. If you have multiple points, separate them with bullet points (•) or provide them as an array."
}

User: ${userMessage.text}
AI:
`;
      const response = await fetch(GEMINI_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          contents: [{ parts: [{ text: prompt }] }]
        }),
      });
      const data = await response.json();
      const aiText = data.candidates?.[0]?.content?.parts?.[0]?.text || t('aiChatbot.noResponseError');
      
      // Format the response
      const formattedResponse = formatResponse(aiText);
      
      setMessages(prev => [...prev, { 
        from: 'bot', 
        text: formattedResponse.text, 
        isFormatted: formattedResponse.isFormatted 
      }]);
    } catch (_err) {
      setMessages(prev => [...prev, { 
        from: 'bot', 
        text: t('aiChatbot.tryAgainError'), 
        isFormatted: false 
      }]);
    } finally {
      setLoading(false);
    }
  };

  const renderMessage = (msg, idx) => {
    if (msg.isFormatted) {
      // Split the formatted text by newlines and render each line
      const lines = msg.text.split('\n').filter(line => line.trim());
      
      return (
        <View
          key={idx}
          style={[
            styles.message,
            msg.from === 'user' ? styles.userMessage : styles.botMessage
          ]}
        >
          {lines.map((line, lineIdx) => (
            <Text key={lineIdx} style={[
              styles.messageText,
              line.startsWith('•') && styles.bulletPoint
            ]}>
              {line}
            </Text>
          ))}
        </View>
      );
    } else {
      // Regular message rendering
      return (
        <View
          key={idx}
          style={[
            styles.message,
            msg.from === 'user' ? styles.userMessage : styles.botMessage
          ]}
        >
          <Text style={styles.messageText}>{msg.text}</Text>
        </View>
      );
    }
  };

  return (
    <KeyboardAvoidingView
      style={{ flex: 1, backgroundColor: '#f7f9fa' }}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()}>
          <Ionicons name="arrow-back" size={28} color="#4a89dc" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>{t('aiChatbot.headerTitle')}</Text>
      </View>
      <ScrollView
        style={styles.chatContainer}
        contentContainerStyle={{ padding: 16 }}
        ref={ref => { if (ref) ref.scrollToEnd({ animated: true }); }}
      >
        {messages.map((msg, idx) => renderMessage(msg, idx))}
      </ScrollView>
      <View style={styles.inputContainer}>
        <TextInput
          style={styles.input}
          placeholder={t('aiChatbot.inputPlaceholder')}
          value={input}
          onChangeText={setInput}
          editable={!loading}
          testID="message-input"
        />
        <TouchableOpacity
          style={styles.sendButton}
          onPress={sendMessage}
          disabled={loading || !input.trim()}
          testID="send-button"
        >
          {loading ? <ActivityIndicator color="#fff" /> : <Ionicons name="send" size={24} color="#fff" />}
        </TouchableOpacity>
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
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
    fontSize: 20,
    fontWeight: 'bold',
    color: '#4a89dc',
    marginLeft: 16,
  },
  chatContainer: {
    flex: 1,
    backgroundColor: '#f7f9fa',
  },
  message: {
    marginBottom: 12,
    padding: 12,
    borderRadius: 10,
    maxWidth: '80%',
  },
  userMessage: {
    backgroundColor: '#4a89dc',
    alignSelf: 'flex-end',
  },
  botMessage: {
    backgroundColor: '#e0e0e0',
    alignSelf: 'flex-start',
  },
  messageText: {
    color: '#222',
    fontSize: 16,
    lineHeight: 22,
  },
  bulletPoint: {
    marginBottom: 4,
    paddingLeft: 8,
  },
  inputContainer: {
    flexDirection: 'row',
    padding: 12,
    backgroundColor: '#fff',
    borderTopWidth: 1,
    borderTopColor: '#eee',
    alignItems: 'center',
  },
  input: {
    flex: 1,
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 25,
    paddingHorizontal: 16,
    paddingVertical: 8,
    fontSize: 16,
    backgroundColor: '#fafbfc',
    marginRight: 8,
  },
  sendButton: {
    backgroundColor: '#4a89dc',
    borderRadius: 25,
    padding: 10,
    justifyContent: 'center',
    alignItems: 'center',
  },
}); 